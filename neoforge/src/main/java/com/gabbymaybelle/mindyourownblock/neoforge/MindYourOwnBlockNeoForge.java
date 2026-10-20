package com.gabbymaybelle.mindyourownblock.neoforge;

import com.gabbymaybelle.mindyourownblock.ExactMode;
import com.gabbymaybelle.mindyourownblock.MindYourOwnBlock;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** The NeoForge entrypoint on both sides: the key's message and who holds it. */
@Mod(MindYourOwnBlock.MOD_ID)
public class MindYourOwnBlockNeoForge {
	public MindYourOwnBlockNeoForge(IEventBus modBus) {
		MindYourOwnBlock.init();
		modBus.addListener(MindYourOwnBlockNeoForge::payloads);
		NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent event) -> {
			if (event.getEntity() instanceof ServerPlayer player) {
				ExactMode.onLeave(player);
			}
		});
	}

	/** Optional, so players without the mod can still join; their placing is just vanilla. */
	private static void payloads(RegisterPayloadHandlersEvent event) {
		event.registrar("1").optional().playToServer(ExactMode.Held.TYPE, ExactMode.Held.CODEC,
				(payload, context) -> context.enqueueWork(() -> ExactMode.onHeld((ServerPlayer) context.player(), payload.held())));
	}
}
