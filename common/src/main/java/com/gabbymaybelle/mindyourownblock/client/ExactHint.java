package com.gabbymaybelle.mindyourownblock.client;

import com.gabbymaybelle.mindyourownblock.ExactMode;
import com.gabbymaybelle.mindyourownblock.MindYourOwnBlock;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/**
 * The small mark beside the crosshair while the key is held, so you know placing is exact. Shown
 * only where the crosshair is: first person, nothing open, the HUD not hidden.
 */
public final class ExactHint {
	public static final ResourceLocation ID = MindYourOwnBlock.id("exact_hint");
	private static final ResourceLocation SPRITE = MindYourOwnBlock.id("hud/exact_hint");
	private static final int WIDTH = 7;
	private static final int HEIGHT = 9;

	private ExactHint() {
	}

	/** Whether the mark shows right now. */
	public static boolean showing(Minecraft mc) {
		return mc.player != null && ExactMode.isOn(mc.player) && mc.screen == null && !mc.options.hideGui
				&& mc.options.getCameraType().isFirstPerson();
	}

	/** Called by the loaders when the HUD is drawn. */
	public static void render(GuiGraphics graphics, DeltaTracker delta) {
		Minecraft mc = Minecraft.getInstance();
		if (!showing(mc)) {
			return;
		}
		// Just below and right of the crosshair (15 px across, centred), clear of it.
		int x = graphics.guiWidth() / 2 + 5;
		int y = graphics.guiHeight() / 2 + 5;
		graphics.blitSprite(SPRITE, x, y, WIDTH, HEIGHT);
	}
}
