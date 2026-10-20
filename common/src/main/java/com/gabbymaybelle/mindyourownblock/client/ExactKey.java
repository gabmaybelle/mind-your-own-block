package com.gabbymaybelle.mindyourownblock.client;

import com.gabbymaybelle.mindyourownblock.ExactMode;
import com.gabbymaybelle.mindyourownblock.MindYourOwnBlock;
import com.gabbymaybelle.mindyourownblock.Platform;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

/**
 * The key: held while placing, it makes blocks keep the shape they're placed in. It is its own
 * key, not sneak, so it works on scaffolding (where sneaking climbs down) and while sneaking on a
 * roof. Read at the start of every tick, before the game handles clicks, so a block placed in the
 * same tick as the key goes down is already exact.
 */
public final class ExactKey {
	public static final KeyMapping.Category CATEGORY = new KeyMapping.Category(MindYourOwnBlock.id("keys"));
	/**
	 * The default: the mouse's first side button ("Mouse 4"), so the hand on the mouse holds it while
	 * placing (Gabby's pick, 2026-10-20). Looked up while the game runs: 26.3 renumbered keys and
	 * mouse buttons, so a constant would be compiled in with one version's number.
	 */
	private static final InputConstants.Key DEFAULT = InputConstants.getKey("key.mouse.4");
	public static final KeyMapping KEY = new KeyMapping("key." + MindYourOwnBlock.MOD_ID + ".exact",
			DEFAULT.getType(), DEFAULT.getValue(), CATEGORY);

	/** What the server was last told. */
	private static boolean sent;

	private ExactKey() {
	}

	/** Called by the loaders at the start of every client tick. */
	public static void onTick(Minecraft mc) {
		Platform.Client platform = Platform.client();
		// On a server without the mod the key does nothing, so the game never shows a shape the
		// server would undo.
		boolean canSend = mc.player != null && mc.getConnection() != null && platform.canSendToServer(ExactMode.Held.TYPE);
		if (!canSend) {
			sent = false;
			ExactMode.setClientHolding(false);
			return;
		}
		boolean holding = KEY.isDown();
		ExactMode.setClientHolding(holding);
		if (holding != sent) {
			platform.sendToServer(new ExactMode.Held(holding));
			sent = holding;
		}
	}

	/** Called by the loaders when the player leaves a world or server. */
	public static void onLeave() {
		sent = false;
		ExactMode.setClientHolding(false);
	}
}
