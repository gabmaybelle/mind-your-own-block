package com.gabbymaybelle.mindyourownblock;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.gui.screens.ReceivingLevelScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * A small step-by-step in-game test for the NeoForge jar, since NeoForge has no client game test
 * framework like Fabric's. It only runs in the `clientTest` run (`./gradlew :neoforge:runClientTest`),
 * which sets the system property below; this source set is never in the jar players get.
 *
 * <p>Each step runs on the game's own thread, one or more ticks at a time. A failed check is noted
 * and the run carries on; at the end, if any failed, it throws with every failed check listed,
 * which crashes the game with a crash report and a non-zero exit code, so Gradle (and CI) fail.
 * When every check has passed, the game closes normally. A watchdog ends a run that hangs.
 */
@EventBusSubscriber(modid = MindYourOwnBlock.MOD_ID, value = Dist.CLIENT)
final class ClientTest {
	static final boolean ENABLED = Boolean.getBoolean(MindYourOwnBlock.MOD_ID + ".clienttest");
	/** A run that takes longer than this has hung (the game thread may be stuck, so a separate thread checks). */
	private static final long WATCHDOG_MILLIS = 10 * 60 * 1000;

	/** One step: returns true once it's done, and is called again next tick until then. */
	interface Step {
		boolean tick(Minecraft mc);
	}

	private static final Deque<Step> STEPS = new ArrayDeque<>();
	private static boolean started;
	private static boolean finished;
	/** Failed checks so far; checks run on both the game and the server thread. */
	private static final List<String> FAILURES = new ArrayList<>();
	/** The folder of the world {@link #createWorld} made, for {@link #rejoin}. */
	private static String world;

	private ClientTest() {
	}

	/** Runs once. */
	static void run(Consumer<Minecraft> action) {
		STEPS.add(mc -> {
			action.accept(mc);
			return true;
		});
	}

	/** Waits this many ticks. */
	static void waitTicks(int ticks) {
		int[] left = { ticks };
		STEPS.add(mc -> --left[0] <= 0);
	}

	/** Waits until the condition holds, failing after this many ticks. */
	static void waitUntil(String what, int timeoutTicks, Predicate<Minecraft> condition) {
		int[] waited = { 0 };
		STEPS.add(mc -> {
			if (condition.test(mc)) {
				return true;
			}
			if (++waited[0] > timeoutTicks) {
				throw new AssertionError("Timed out after " + timeoutTicks + " ticks waiting for: " + what);
			}
			return false;
		});
	}

	/** Runs a command on the singleplayer server as an operator, then gives it a few ticks to reach the client. */
	static void command(String command) {
		run(mc -> {
			IntegratedServer server = mc.getSingleplayerServer();
			if (server == null) {
				throw new AssertionError("No singleplayer server for the command: " + command);
			}
			server.execute(() -> server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), command));
		});
		waitTicks(4);
	}

	/** Saves a screenshot in the run's screenshots folder (named by the time it was taken). */
	static void screenshot() {
		run(mc -> Screenshot.grab(mc.gameDirectory, mc.getMainRenderTarget(), message -> {}));
		waitTicks(2);
	}

	/**
	 * Records a failed check and carries on, so one run reports every check that fails (the Fabric
	 * game test does the same). The run fails at the end with the whole list. Setup problems (a
	 * timeout, no server) still stop it at once: nothing after them could be trusted.
	 */
	static void check(boolean condition, String message) {
		if (condition) {
			return;
		}
		MindYourOwnBlock.LOGGER.error("[test] FAILED: {}", message);
		synchronized (FAILURES) {
			FAILURES.add(message);
		}
	}

	/**
	 * Creates a fresh flat creative world with commands on, and waits until the player is in it. The
	 * folder gets the time in its name, so a run never reopens a world an earlier run left behind
	 * (in the same game folder, perhaps on another Minecraft version).
	 */
	static void createWorld(String baseName) {
		String name = baseName + "-" + System.currentTimeMillis();
		world = name;
		waitUntil("the title screen", 2400, mc -> mc.getOverlay() == null && mc.screen != null);
		// The game runs in the background and loses focus when it is sent behind other windows;
		// a singleplayer game that pauses then stops the whole world (the Fabric harness does the same).
		run(mc -> mc.options.pauseOnLostFocus = false);
		run(mc -> mc.createWorldOpenFlows().createFreshLevel(name,
				new LevelSettings(name, GameType.CREATIVE, false, Difficulty.PEACEFUL, true, new GameRules(), WorldDataConfiguration.DEFAULT),
				new WorldOptions(1L, false, false),
				registries -> registries.registryOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.FLAT).createWorldDimensions(),
				mc.screen));
		waitUntil("the player in the new world", 2400, mc -> mc.player != null && mc.level != null && !(mc.screen instanceof LevelLoadingScreen));
		closeOtherModsScreens();
		// Time for the chunks around the player to arrive and draw.
		waitTicks(60);
	}

	/**
	 * Saves and quits to the title screen, as the pause menu does, then opens the same world again
	 * and waits until the player is back in it.
	 */
	static void rejoin() {
		run(mc -> {
			if (mc.level != null) {
				mc.level.disconnect();
			}
			mc.disconnect();
		});
		waitUntil("the world to close", 2400, mc -> mc.level == null && mc.getSingleplayerServer() == null && mc.screen != null);
		run(mc -> mc.createWorldOpenFlows().openWorld(world, () -> {
			throw new AssertionError("Opening the world again was cancelled");
		}));
		waitUntil("the player back in the world", 2400, mc -> mc.player != null && mc.level != null && !(mc.screen instanceof LevelLoadingScreen));
		closeOtherModsScreens();
		waitTicks(60);
	}

	/**
	 * Closes screens other mods open when a world first loads (Build Better's library shows welcome
	 * cards), which would otherwise sit over the test and catch its key presses. Done once no screen
	 * has opened for two seconds; the loading screen is left to finish by itself.
	 */
	static void closeOtherModsScreens() {
		int[] quiet = { 0 };
		int[] waited = { 0 };
		STEPS.add(mc -> {
			Screen screen = mc.screen;
			if (screen == null) {
				quiet[0]++;
			} else {
				quiet[0] = 0;
				if (!(screen instanceof LevelLoadingScreen) && !(screen instanceof ReceivingLevelScreen)) {
					MindYourOwnBlock.LOGGER.info("[test] closing a screen another mod opened: {}", screen.getClass().getName());
					mc.setScreen(null);
				}
			}
			if (++waited[0] > 600) {
				throw new AssertionError("Screens kept opening for 30 seconds after the world loaded");
			}
			return quiet[0] >= 40;
		});
	}

	@SubscribeEvent
	static void onTick(ClientTickEvent.Post event) {
		if (!ENABLED || finished) {
			return;
		}
		Minecraft mc = Minecraft.getInstance();
		if (!started) {
			started = true;
			MindYourOwnBlock.LOGGER.info("Mind Your Own Block: client test starting");
			Thread watchdog = new Thread(() -> {
				try {
					Thread.sleep(WATCHDOG_MILLIS);
				} catch (InterruptedException e) {
					return;
				}
				MindYourOwnBlock.LOGGER.error("Mind Your Own Block: client test hung for {} minutes; stopping", WATCHDOG_MILLIS / 60000);
				Runtime.getRuntime().halt(3);
			}, "Mind Your Own Block client test watchdog");
			watchdog.setDaemon(true);
			watchdog.start();
			MindYourOwnBlockClientTest.steps();
		}
		Step step = STEPS.peek();
		if (step == null) {
			finished = true;
			synchronized (FAILURES) {
				if (!FAILURES.isEmpty()) {
					throw new AssertionError(FAILURES.size() + " check(s) failed:\n - " + String.join("\n - ", FAILURES));
				}
			}
			MindYourOwnBlock.LOGGER.info("Mind Your Own Block: client test passed, every check");
			mc.stop();
			return;
		}
		if (step.tick(mc)) {
			STEPS.poll();
		}
	}
}
