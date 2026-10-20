package com.gabbymaybelle.mindyourownblock;

import static com.gabbymaybelle.mindyourownblock.ClientTest.check;
import static com.gabbymaybelle.mindyourownblock.ClientTest.createWorld;
import static com.gabbymaybelle.mindyourownblock.ClientTest.run;
import static com.gabbymaybelle.mindyourownblock.ClientTest.waitTicks;

import com.gabbymaybelle.mindyourownblock.client.ExactKey;
import java.util.Arrays;
import net.minecraft.client.Minecraft;
import net.minecraft.client.server.IntegratedServer;
import net.neoforged.fml.ModList;

/**
 * The NeoForge jar's in-game test: the same checks as the Fabric game test (fabric/src/gametest),
 * so both loaders are held to the same behaviour.
 */
final class MindYourOwnBlockClientTest {
	private MindYourOwnBlockClientTest() {
	}

	static void steps() {
		createWorld("mind-your-own-block-clienttest");
		run(mc -> check(ModList.get().isLoaded(MindYourOwnBlock.MOD_ID), "the mod is loaded in a new world"));
		key();
	}

	/** The key, the same checks as the Fabric test's {@code key}. */
	private static void key() {
		run(mc -> {
			check(Arrays.asList(mc.options.keyMappings).contains(ExactKey.KEY), "the key is in Controls");
			check(!serverOn(mc), "exact placing is off before the key is held");
			ExactKey.KEY.setDown(true);
		});
		waitTicks(5);
		run(mc -> {
			check(serverOn(mc), "holding the key turns exact placing on, on the server");
			check(ExactMode.isOn(mc.player), "holding the key turns exact placing on, in the player's game");
			ExactKey.KEY.setDown(false);
		});
		waitTicks(5);
		run(mc -> {
			check(!serverOn(mc), "letting go of the key turns exact placing off, on the server");
			check(!ExactMode.isOn(mc.player), "letting go of the key turns exact placing off, in the player's game");
			mc.options.keyShift.setDown(true);
		});
		waitTicks(5);
		run(mc -> {
			check(!serverOn(mc), "sneaking alone doesn't turn exact placing on");
			ExactKey.KEY.setDown(true);
		});
		waitTicks(5);
		run(mc -> {
			check(serverOn(mc), "the key works while sneaking");
			ExactKey.KEY.setDown(false);
			mc.options.keyShift.setDown(false);
		});
		waitTicks(5);
	}

	private static boolean serverOn(Minecraft mc) {
		IntegratedServer server = mc.getSingleplayerServer();
		if (server == null) {
			throw new AssertionError("No singleplayer server");
		}
		boolean[] on = new boolean[1];
		server.executeBlocking(() -> on[0] = ExactMode.isOn(server.getPlayerList().getPlayers().getFirst()));
		return on[0];
	}
}
