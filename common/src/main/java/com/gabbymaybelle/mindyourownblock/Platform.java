package com.gabbymaybelle.mindyourownblock;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jetbrains.annotations.Nullable;

/**
 * The few things the mod asks of the loader. Each loader's main entrypoint sets {@link Chunks} first
 * thing, and its client entrypoint sets {@link Client}; a dedicated server never needs that one.
 */
public final class Platform {
	/** Where each loader keeps a chunk's locks (saved with the chunk and sent to the players who see it). */
	public interface Chunks {
		@Nullable Locks locks(LevelChunk chunk);

		/** Sets the chunk's locks; {@code null} takes them off. */
		void setLocks(LevelChunk chunk, @Nullable Locks locks);
	}

	/** What each loader answers on the player's game. */
	public interface Client {
		/** Whether the server understands this message (it has the mod too). */
		boolean canSendToServer(CustomPacketPayload.Type<?> type);

		void sendToServer(CustomPacketPayload payload);
	}

	private static Client client;
	private static Chunks chunks;

	private Platform() {
	}

	public static void setChunks(Chunks hooks) {
		chunks = hooks;
	}

	public static Chunks chunks() {
		return chunks;
	}

	public static void setClient(Client hooks) {
		client = hooks;
	}

	public static Client client() {
		return client;
	}
}
