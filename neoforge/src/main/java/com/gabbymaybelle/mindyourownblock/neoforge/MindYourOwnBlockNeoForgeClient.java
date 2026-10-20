package com.gabbymaybelle.mindyourownblock.neoforge;

import com.gabbymaybelle.mindyourownblock.MindYourOwnBlock;
import com.gabbymaybelle.mindyourownblock.Platform;
import com.gabbymaybelle.mindyourownblock.client.ExactKey;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.common.NeoForge;

/** The NeoForge entrypoint on the player's game: the key, read every tick. A dedicated server never loads it. */
@Mod(value = MindYourOwnBlock.MOD_ID, dist = Dist.CLIENT)
public class MindYourOwnBlockNeoForgeClient {
	public MindYourOwnBlockNeoForgeClient(IEventBus modBus) {
		Platform.setClient(new Platform.Client() {
			@Override
			public boolean canSendToServer(CustomPacketPayload.Type<?> type) {
				ClientPacketListener connection = Minecraft.getInstance().getConnection();
				return connection != null && connection.hasChannel(type);
			}

			@Override
			public void sendToServer(CustomPacketPayload payload) {
				ClientPacketDistributor.sendToServer(payload);
			}
		});
		modBus.addListener((RegisterKeyMappingsEvent event) -> {
			event.registerCategory(ExactKey.CATEGORY);
			event.register(ExactKey.KEY);
		});
		NeoForge.EVENT_BUS.addListener((ClientTickEvent.Pre event) -> ExactKey.onTick(Minecraft.getInstance()));
		NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut event) -> ExactKey.onLeave());
	}
}
