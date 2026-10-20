package com.gabbymaybelle.mindyourownblock;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.CrossCollisionBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.chunk.LevelChunk;

/**
 * Stays put: a fence, wall, pane, bars or stair placed with the key keeps the shape it was placed in
 * when its neighbours change, until the block at that spot becomes a different block (broken,
 * replaced, or pushed by a piston, which turns it into a moving block first). Water and anything
 * else a block does when a neighbour changes still happen; only the shape is kept.
 */
public final class StaysPut {
	/** The properties that make up a block's shape: the arms, a wall's post and a stair's corner. */
	private static final List<Property<?>> SHAPE = List.of(
			CrossCollisionBlock.NORTH, CrossCollisionBlock.EAST, CrossCollisionBlock.SOUTH, CrossCollisionBlock.WEST,
			WallBlock.NORTH, WallBlock.EAST, WallBlock.SOUTH, WallBlock.WEST, WallBlock.UP,
			StairBlock.SHAPE);

	private StaysPut() {
	}

	/** Whether this kind of block reshapes itself to its neighbours, so a lock means something. */
	public static boolean isShaped(BlockState state) {
		return state.getBlock() instanceof CrossCollisionBlock || state.getBlock() instanceof WallBlock || state.getBlock() instanceof StairBlock;
	}

	/** Whether the block at {@code pos} keeps its shape: being placed exactly right now, or locked. */
	public static boolean keeps(LevelReader level, BlockPos pos) {
		return ExactPlacing.keeps(pos) || isLocked(level, pos);
	}

	/** The state vanilla reshaped {@code before} into, with the shape put back. */
	public static BlockState keepShape(BlockState before, BlockState updated) {
		if (updated.getBlock() != before.getBlock()) {
			return updated;
		}
		for (Property<?> property : SHAPE) {
			if (before.hasProperty(property) && updated.hasProperty(property)) {
				updated = copy(before, updated, property);
			}
		}
		return updated;
	}

	private static <T extends Comparable<T>> BlockState copy(BlockState from, BlockState to, Property<T> property) {
		return to.setValue(property, from.getValue(property));
	}

	/** Whether a lock is kept for {@code pos}, on whichever side asks. Only for a loaded, real world. */
	public static boolean isLocked(LevelReader level, BlockPos pos) {
		// World generation reshapes blocks in its own regions: never locked there.
		if (!(level instanceof Level world)) {
			return false;
		}
		LevelChunk chunk = world.getChunkSource().getChunkNow(SectionPos.blockToSectionCoord(pos.getX()), SectionPos.blockToSectionCoord(pos.getZ()));
		if (chunk == null) {
			return false;
		}
		Locks locks = Platform.chunks().locks(chunk);
		return locks != null && locks.contains(pos);
	}

	/** On the server: the block just placed exactly at {@code pos} stays put from now on. */
	public static void lock(Level level, BlockPos pos) {
		if (level.isClientSide()) {
			return;
		}
		LevelChunk chunk = level.getChunkAt(pos);
		Locks locks = Platform.chunks().locks(chunk);
		Platform.chunks().setLocks(chunk, (locks == null ? Locks.empty() : locks).with(pos));
	}

	/** On the server: the block at {@code pos} changed from {@code before} to {@code after}. */
	public static void onChanged(LevelChunk chunk, BlockPos pos, BlockState before, BlockState after) {
		// Only shaped blocks are ever locked, and only a change of block ends a lock (a bucket of
		// water poured into a locked fence keeps it).
		if (before.getBlock() == after.getBlock() || !isShaped(before) || chunk.getLevel().isClientSide()) {
			return;
		}
		Locks locks = Platform.chunks().locks(chunk);
		if (locks == null || !locks.contains(pos)) {
			return;
		}
		Locks left = locks.without(pos);
		Platform.chunks().setLocks(chunk, left.isEmpty() ? null : left);
	}
}
