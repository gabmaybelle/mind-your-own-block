package com.gabbymaybelle.mindyourownblock.fabric;

import com.gabbymaybelle.mindyourownblock.Platform;
import com.gabbymaybelle.mindyourownblock.client.ExactHint;
import com.gabbymaybelle.mindyourownblock.client.ExactKey;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** The Fabric entrypoint on the player's game: the key, read every tick, and the crosshair mark. */
public class MindYourOwnBlockFabricClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		Platform.setClient(new Platform.Client() {
			@Override
			public boolean canSendToServer(CustomPacketPayload.Type<?> type) {
				return ClientPlayNetworking.canSend(type);
			}

			@Override
			public void sendToServer(CustomPacketPayload payload) {
				ClientPlayNetworking.send(payload);
			}
		});
		KeyBindingHelper.registerKeyBinding(ExactKey.KEY);
		ClientTickEvents.START_CLIENT_TICK.register(ExactKey::onTick);
		ClientPlayConnectionEvents.DISCONNECT.register((handler, mc) -> ExactKey.onLeave());
		HudRenderCallback.EVENT.register(ExactHint::render);
	}
}
