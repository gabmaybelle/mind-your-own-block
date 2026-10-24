package com.gabbymaybelle.mindyourownblock.fabric;

import com.gabbymaybelle.mindyourownblock.ExactMode;
import com.gabbymaybelle.mindyourownblock.Locks;
import com.gabbymaybelle.mindyourownblock.MindYourOwnBlock;
import com.gabbymaybelle.mindyourownblock.Platform;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentTarget;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.world.level.chunk.LevelChunk;

/** The Fabric entrypoint on both sides: the key's message and who holds it, and the chunks' locks. */
public class MindYourOwnBlockFabric implements ModInitializer {
	/** Each chunk's locks, saved with it and sent to every player who can see it. */
	private static final AttachmentType<Locks> LOCKS = AttachmentRegistry.create(MindYourOwnBlock.id("locks"),
			builder -> builder.persistent(Locks.CODEC).syncWith(Locks.STREAM_CODEC, AttachmentSyncPredicate.all()));

	@Override
	public void onInitialize() {
		Platform.setChunks(new Platform.Chunks() {
			@Override
			public Locks locks(LevelChunk chunk) {
				return ((AttachmentTarget) chunk).getAttached(LOCKS);
			}

			@Override
			public void setLocks(LevelChunk chunk, Locks locks) {
				((AttachmentTarget) chunk).setAttached(LOCKS, locks);
			}
		});
		MindYourOwnBlock.init();
		PayloadTypeRegistry.playC2S().register(ExactMode.Held.TYPE, ExactMode.Held.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(ExactMode.Held.TYPE, (payload, context) -> ExactMode.onHeld(context.player(), payload.held()));
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> ExactMode.onLeave(handler.player));
	}
}
