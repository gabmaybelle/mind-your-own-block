package com.gabbymaybelle.mindyourownblock.fabric;

import com.gabbymaybelle.mindyourownblock.Platform;
import com.gabbymaybelle.mindyourownblock.client.ExactKey;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** The Fabric entrypoint on the player's game: the key, read every tick. */
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
		KeyMapping.Category.register(ExactKey.CATEGORY.id());
		KeyMappingHelper.registerKeyMapping(ExactKey.KEY);
		ClientTickEvents.START_CLIENT_TICK.register(ExactKey::onTick);
		ClientPlayConnectionEvents.DISCONNECT.register((handler, mc) -> ExactKey.onLeave());
	}
}
