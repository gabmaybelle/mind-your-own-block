package com.gabbymaybelle.mindyourownblock.fabric;

import com.gabbymaybelle.mindyourownblock.ExactMode;
import com.gabbymaybelle.mindyourownblock.MindYourOwnBlock;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

/** The Fabric entrypoint on both sides: the key's message and who holds it. */
public class MindYourOwnBlockFabric implements ModInitializer {
	@Override
	public void onInitialize() {
		MindYourOwnBlock.init();
		PayloadTypeRegistry.serverboundPlay().register(ExactMode.Held.TYPE, ExactMode.Held.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(ExactMode.Held.TYPE, (payload, context) -> ExactMode.onHeld(context.player(), payload.held()));
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> ExactMode.onLeave(handler.getPlayer()));
	}
}
