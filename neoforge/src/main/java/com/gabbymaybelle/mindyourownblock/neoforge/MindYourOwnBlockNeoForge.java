package com.gabbymaybelle.mindyourownblock.neoforge;

import com.gabbymaybelle.mindyourownblock.ExactMode;
import com.gabbymaybelle.mindyourownblock.Locks;
import com.gabbymaybelle.mindyourownblock.MindYourOwnBlock;
import com.gabbymaybelle.mindyourownblock.Platform;
import java.util.function.Supplier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/** The NeoForge entrypoint on both sides: the key's message and who holds it, and the chunks' locks. */
@Mod(MindYourOwnBlock.MOD_ID)
public class MindYourOwnBlockNeoForge {
	private static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, MindYourOwnBlock.MOD_ID);
	/** Each chunk's locks, saved with it (only when it has some) and sent to every player who can see it. */
	private static final Supplier<AttachmentType<Locks>> LOCKS = ATTACHMENTS.register("locks",
			() -> AttachmentType.builder(Locks::empty).serialize(Locks.MAP_CODEC, locks -> !locks.isEmpty()).sync(Locks.STREAM_CODEC).build());

	public MindYourOwnBlockNeoForge(IEventBus modBus) {
		Platform.setChunks(new Platform.Chunks() {
			@Override
			public Locks locks(LevelChunk chunk) {
				return chunk.getExistingDataOrNull(LOCKS.get());
			}

			@Override
			public void setLocks(LevelChunk chunk, Locks locks) {
				if (locks == null) {
					chunk.removeData(LOCKS.get());
				} else {
					chunk.setData(LOCKS.get(), locks);
				}
			}
		});
		ATTACHMENTS.register(modBus);
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
