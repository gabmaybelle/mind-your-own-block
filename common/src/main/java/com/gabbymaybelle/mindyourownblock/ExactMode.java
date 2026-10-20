package com.gabbymaybelle.mindyourownblock;

import io.netty.buffer.ByteBuf;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * Who is holding the key right now. The player's game sends {@link Held} when the key goes down and
 * when it comes up; the server keeps the players who hold it, in memory only (nothing is saved).
 * Messages on one connection arrive in order, so the server already knows by the time a block
 * from that player is placed.
 */
public final class ExactMode {
	/** The key went down ({@code true}) or came up ({@code false}). Sent from the player's game to the server. */
	public record Held(boolean held) implements CustomPacketPayload {
		public static final Type<Held> TYPE = new Type<>(MindYourOwnBlock.id("exact_key"));
		public static final StreamCodec<ByteBuf, Held> CODEC = StreamCodec.composite(ByteBufCodecs.BOOL, Held::held, Held::new);

		@Override
		public Type<Held> type() {
			return TYPE;
		}
	}

	/** On the server: players holding the key, by id (the player object is replaced on respawn). */
	private static final Set<UUID> HOLDING = ConcurrentHashMap.newKeySet();
	/** On the player's own game: whether the key is held and the server will hear about it. */
	private static volatile boolean clientHolding;

	private ExactMode() {
	}

	/** Whether this player is placing exactly, on whichever side asks. */
	public static boolean isOn(Player player) {
		if (player.level().isClientSide()) {
			return clientHolding;
		}
		return HOLDING.contains(player.getUUID());
	}

	/** On the server: a {@link Held} message arrived from this player. */
	public static void onHeld(ServerPlayer player, boolean held) {
		if (held) {
			HOLDING.add(player.getUUID());
		} else {
			HOLDING.remove(player.getUUID());
		}
	}

	/** On the server: the player left, so the key can't be held any more. */
	public static void onLeave(ServerPlayer player) {
		HOLDING.remove(player.getUUID());
	}

	/** On the player's game: set by {@code client.ExactKey} each tick. */
	public static void setClientHolding(boolean holding) {
		clientHolding = holding;
	}
}
