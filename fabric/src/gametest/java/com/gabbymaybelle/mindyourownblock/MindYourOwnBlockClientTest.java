package com.gabbymaybelle.mindyourownblock;

import com.gabbymaybelle.mindyourownblock.client.ExactHint;
import com.gabbymaybelle.mindyourownblock.client.ExactKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import com.gabbymaybelle.mindyourownblock.harness.ClientGameTest;
import com.gabbymaybelle.mindyourownblock.harness.ClientGameTestContext;
import com.gabbymaybelle.mindyourownblock.harness.TestSingleplayerContext;
import com.gabbymaybelle.mindyourownblock.harness.TestWorldSave;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.gui.screens.Screen;

/**
 * The mod in a real game. Run with {@code ./gradlew :fabric:runClientGameTest}; screenshots land
 * in fabric/build/run/clientGameTest/screenshots. Each feature adds its own checks here as it
 * lands, and each check is proven by running it once with the feature switched off. The NeoForge
 * jar runs the same checks (neoforge/src/clienttest).
 */
public class MindYourOwnBlockClientTest implements ClientGameTest {
	private final List<String> failures = new ArrayList<>();

	@Override
	public void runTest(ClientGameTestContext ctx) {
		TestWorldSave save;
		try (TestSingleplayerContext world = ctx.worldBuilder().create()) {
			world.waitForChunksRender();
			closeOtherModsScreens(ctx);
			boolean loaded = world.getServer().computeOnServer(server -> FabricLoader.getInstance().isModLoaded(MindYourOwnBlock.MOD_ID));
			check(loaded, "the mod is loaded in a new world");
			key(ctx, world);
			placing(ctx, world);
			staysPut(ctx, world);
			save = world.getWorldSave();
		}
		// Saved and closed: open it again and check the locks came back with it.
		try (TestSingleplayerContext world = save.open()) {
			world.waitForChunksRender();
			closeOtherModsScreens(ctx);
			world.getServer().runOnServer(server -> StaysPutChecks.afterRejoin(server.getPlayerList().getPlayers().getFirst(), this::check));
			ctx.waitTicks(10);
			ctx.runOnClient(mc -> StaysPutChecks.clientAgrees(mc.level, "after reopening", this::check));
		}
		if (!failures.isEmpty()) {
			throw new AssertionError(failures.size() + " check(s) failed:\n - " + String.join("\n - ", failures));
		}
	}

	/**
	 * The key: it's in Controls with the mouse side button as its default, holding it turns exact placing on for this player on the server
	 * (and in the player's own game), letting go turns it off, sneaking alone doesn't, and it works
	 * while sneaking.
	 */
	private void key(ClientGameTestContext ctx, TestSingleplayerContext world) {
		boolean listed = ctx.computeOnClient(mc -> Arrays.asList(mc.options.keyMappings).contains(ExactKey.KEY));
		check(listed, "the key is in Controls");
		check(ExactKey.KEY.getDefaultKey().getName().equals("key.mouse.4"), "the key's default is the mouse side button (key.mouse.4), not " + ExactKey.KEY.getDefaultKey().getName());
		check(!serverOn(world), "exact placing is off before the key is held");

		ctx.getInput().holdKey(ExactKey.KEY);
		ctx.waitTicks(5);
		check(serverOn(world), "holding the key turns exact placing on, on the server");
		check(ctx.computeOnClient(mc -> ExactMode.isOn(mc.player)), "holding the key turns exact placing on, in the player's game");

		ctx.getInput().releaseKey(ExactKey.KEY);
		ctx.waitTicks(5);
		check(!serverOn(world), "letting go of the key turns exact placing off, on the server");
		check(!ctx.computeOnClient(mc -> ExactMode.isOn(mc.player)), "letting go of the key turns exact placing off, in the player's game");

		ctx.getInput().holdKey(options -> options.keyShift);
		ctx.waitTicks(5);
		check(!serverOn(world), "sneaking alone doesn't turn exact placing on");
		ctx.getInput().holdKey(ExactKey.KEY);
		ctx.waitTicks(5);
		check(serverOn(world), "the key works while sneaking");
		ctx.getInput().releaseKey(ExactKey.KEY);
		ctx.getInput().releaseKey(options -> options.keyShift);
		ctx.waitTicks(5);
	}

	/**
	 * Exact placing, with the key held and then without it (PlacingChecks, shared with NeoForge): no
	 * joining, straight stairs, dry blocks. The crosshair mark shows only while the key is held.
	 */
	private void placing(ClientGameTestContext ctx, TestSingleplayerContext world) {
		ctx.getInput().holdKey(ExactKey.KEY);
		ctx.waitTicks(5);
		check(ctx.computeOnClient(ExactHint::showing), "the crosshair mark shows while the key is held");
		ctx.takeScreenshot("exact-hint");
		world.getServer().runOnServer(server -> PlacingChecks.run(server.getPlayerList().getPlayers().getFirst(), true, this::check));
		ctx.getInput().releaseKey(ExactKey.KEY);
		ctx.waitTicks(5);
		check(!ctx.computeOnClient(ExactHint::showing), "the crosshair mark is gone once the key is let go");
		world.getServer().runOnServer(server -> PlacingChecks.run(server.getPlayerList().getPlayers().getFirst(), false, this::check));
		ctx.waitTicks(10);
		ctx.takeScreenshot("placed-without-the-key");
	}

	/**
	 * Stays put (StaysPutChecks, shared with NeoForge): locked blocks keep their shape as neighbours
	 * come and go, a bucket still fills them, breaking or a piston ends the lock, and the player's own
	 * game agrees with the server.
	 */
	private void staysPut(ClientGameTestContext ctx, TestSingleplayerContext world) {
		ctx.getInput().holdKey(ExactKey.KEY);
		ctx.waitTicks(5);
		world.getServer().runOnServer(server -> StaysPutChecks.placeLocked(server.getPlayerList().getPlayers().getFirst(), this::check));
		ctx.getInput().releaseKey(ExactKey.KEY);
		ctx.waitTicks(5);
		world.getServer().runOnServer(server -> StaysPutChecks.neighbours(server.getPlayerList().getPlayers().getFirst(), this::check));
		// The piston takes a few ticks to push, and the locks a moment to reach the player's game.
		ctx.waitTicks(20);
		world.getServer().runOnServer(server -> StaysPutChecks.afterPiston(server.getPlayerList().getPlayers().getFirst(), this::check));
		ctx.runOnClient(mc -> StaysPutChecks.clientAgrees(mc.level, "while playing", this::check));
		ctx.takeScreenshot("stays-put");
	}

	/**
	 * Closes screens other mods open when a world first loads (Build Better's library shows welcome
	 * cards), which would otherwise sit over the test and catch its key presses. Done once no screen
	 * has opened for two seconds; the loading screen is left to finish by itself.
	 */
	private static void closeOtherModsScreens(ClientGameTestContext ctx) {
		int quiet = 0;
		for (int tick = 0; tick < 600 && quiet < 40; tick++) {
			boolean open = ctx.computeOnClient(mc -> {
				Screen screen = mc.screen;
				if (screen == null) {
					return false;
				}
				if (!(screen instanceof LevelLoadingScreen)) {
					MindYourOwnBlock.LOGGER.info("[test] closing a screen another mod opened: {}", screen.getClass().getName());
					mc.setScreen(null);
				}
				return true;
			});
			quiet = open ? 0 : quiet + 1;
			ctx.waitTick();
		}
	}

	private static boolean serverOn(TestSingleplayerContext world) {
		return world.getServer().computeOnServer(server -> ExactMode.isOn(server.getPlayerList().getPlayers().getFirst()));
	}

	private void check(boolean condition, String what) {
		if (!condition) {
			MindYourOwnBlock.LOGGER.error("[test] FAILED: {}", what);
			failures.add(what);
		}
	}
}
