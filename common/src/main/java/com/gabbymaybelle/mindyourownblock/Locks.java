package com.gabbymaybelle.mindyourownblock;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * The blocks in one chunk that stay as they were placed, as packed positions. Kept with the chunk by
 * the loader (saved with it and sent to every player who can see it). Never changed in place: each
 * change makes a new one and sets it, so the loader notices and sends it on.
 */
public record Locks(LongSet positions) {
	public static final Codec<Locks> CODEC = Codec.LONG.listOf().xmap(Locks::of, Locks::asList);
	public static final MapCodec<Locks> MAP_CODEC = CODEC.fieldOf("positions");
	public static final StreamCodec<ByteBuf, Locks> STREAM_CODEC = ByteBufCodecs.VAR_LONG.apply(ByteBufCodecs.list()).map(Locks::of, Locks::asList);

	public static Locks empty() {
		return new Locks(new LongOpenHashSet());
	}

	private static Locks of(List<Long> positions) {
		return new Locks(new LongOpenHashSet(positions));
	}

	private List<Long> asList() {
		return new ArrayList<>(positions);
	}

	public boolean contains(BlockPos pos) {
		return positions.contains(pos.asLong());
	}

	public boolean isEmpty() {
		return positions.isEmpty();
	}

	/** A copy with {@code pos} added. */
	public Locks with(BlockPos pos) {
		LongSet copy = new LongOpenHashSet(positions);
		copy.add(pos.asLong());
		return new Locks(copy);
	}

	/** A copy without {@code pos}. */
	public Locks without(BlockPos pos) {
		LongSet copy = new LongOpenHashSet(positions);
		copy.remove(pos.asLong());
		return new Locks(copy);
	}
}
