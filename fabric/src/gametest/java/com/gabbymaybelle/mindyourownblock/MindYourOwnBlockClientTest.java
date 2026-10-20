package com.gabbymaybelle.mindyourownblock;

import com.gabbymaybelle.mindyourownblock.client.ExactKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.gui.screens.Screen;

/**
 * The mod in a real game. Run with {@code ./gradlew :fabric:runClientGameTest}; screenshots land
 * in fabric/build/run/clientGameTest/screenshots. Each feature adds its own checks here as it
 * lands, and each check is proven by running it once with the feature switched off. The NeoForge
 * jar runs the same checks (neoforge/src/clienttest).
 */
public class MindYourOwnBlockClientTest implements FabricClientGameTest {
	private final List<String> failures = new ArrayList<>();

	@Override
	public void runTest(ClientGameTestContext ctx) {
		try (TestSingleplayerContext world = ctx.worldBuilder().create()) {
			world.getConnection().waitForChunksRender();
			closeOtherModsScreens(ctx);
			boolean loaded = world.getServer().computeOnServer(server -> FabricLoader.getInstance().isModLoaded(MindYourOwnBlock.MOD_ID));
			check(loaded, "the mod is loaded in a new world");
			key(ctx, world);
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
	 * Closes screens other mods open when a world first loads (Build Better's library shows welcome
	 * cards), which would otherwise sit over the test and catch its key presses. Done once no screen
	 * has opened for two seconds; the loading screen is left to finish by itself.
	 */
	private static void closeOtherModsScreens(ClientGameTestContext ctx) {
		int quiet = 0;
		for (int tick = 0; tick < 600 && quiet < 40; tick++) {
			boolean open = ctx.computeOnClient(mc -> {
				Screen screen = mc.gui.screen();
				if (screen == null) {
					return false;
				}
				if (!(screen instanceof LevelLoadingScreen)) {
					MindYourOwnBlock.LOGGER.info("[test] closing a screen another mod opened: {}", screen.getClass().getName());
					mc.gui.setScreen(null);
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
