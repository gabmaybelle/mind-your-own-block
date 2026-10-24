package com.gabbymaybelle.mindyourownblock;

import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CrossCollisionBlock;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.StairsShape;
import net.minecraft.world.level.block.state.properties.WallSide;
import net.minecraft.world.level.chunk.LevelChunk;

/**
 * Stays put: a fence, wall, pane, bars or stair placed with the key keeps the shape it was placed in
 * when its neighbours change, and any block placed dry with the key stays dry when water flows
 * around it, until the block at that spot becomes a different block (broken, replaced, or pushed by
 * a piston, which turns it into a moving block first). Anything else a block does when a neighbour
 * changes still happens; only the shape and the dryness are kept. Water poured from a bucket still
 * fills it: only water spreading by itself is kept out. Normal blocks beside a locked one don't
 * reach toward it or bend because of it ({@link #awayFromKept}).
 */
public final class StaysPut {
	/** The properties that make up a block's shape: the arms, a wall's post and a stair's corner. */
	/** A wall's arm per side (1.21.1 keeps the map private, so this is the mod's own). */
	public static final Map<Direction, EnumProperty<WallSide>> WALL_ARMS = Map.of(
			Direction.NORTH, WallBlock.NORTH_WALL, Direction.EAST, WallBlock.EAST_WALL,
			Direction.SOUTH, WallBlock.SOUTH_WALL, Direction.WEST, WallBlock.WEST_WALL);
	private static final List<Property<?>> SHAPE = List.of(
			CrossCollisionBlock.NORTH, CrossCollisionBlock.EAST, CrossCollisionBlock.SOUTH, CrossCollisionBlock.WEST,
			WallBlock.NORTH_WALL, WallBlock.EAST_WALL, WallBlock.SOUTH_WALL, WallBlock.WEST_WALL, WallBlock.UP,
			StairBlock.SHAPE);

	private StaysPut() {
	}

	/** Whether this kind of block reshapes itself to its neighbours, so a lock means something. */
	public static boolean isShaped(BlockState state) {
		return state.getBlock() instanceof CrossCollisionBlock || state.getBlock() instanceof WallBlock || state.getBlock() instanceof StairBlock;
	}

	/**
	 * Whether a block placed with the key is locked: one that reshapes to its neighbours, or one water
	 * could flow into (in a pond, two water sources beside a dry block turn it into a source too).
	 */
	public static boolean isLockable(BlockState state) {
		return isShaped(state) || state.hasProperty(BlockStateProperties.WATERLOGGED);
	}

	/** Whether water spreading by itself may not fill the block at {@code pos}: a locked block placed dry. */
	public static boolean keepsDry(BlockGetter level, BlockPos pos, BlockState state) {
		return state.hasProperty(BlockStateProperties.WATERLOGGED) && !state.getValue(BlockStateProperties.WATERLOGGED)
				&& isLocked(level, pos);
	}

	/** Whether the block at {@code pos} keeps its shape: being placed exactly right now, or locked. */
	public static boolean keeps(BlockGetter level, BlockPos pos) {
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

	/**
	 * Whether a neighbour of a normal block counts as not there when that block works out its shape:
	 * a block that joins or bends, placed with the key (being placed right now, or locked). A block
	 * locked only to keep it dry (a slab, a lantern) is joined as usual.
	 */
	private static boolean ignored(BlockGetter level, BlockPos pos) {
		return keeps(level, pos) && isShaped(level.getBlockState(pos));
	}

	/**
	 * The shape a normal (not locked) block takes beside locked ones (Gabby, 2026-10-21): a fence,
	 * wall, pane or bars ends its line at its own post instead of reaching toward a locked block, and a
	 * stair doesn't bend into a corner because of a locked stair.
	 */
	public static BlockState awayFromKept(BlockGetter level, BlockPos pos, BlockState state) {
		// The common case first: no lock anywhere near, so nothing to look up per neighbour. This runs
		// on every reshape of a fence, wall, pane, bars or stair in the game.
		if (!ExactPlacing.placingAnything() && !locksNear(level, pos)) {
			return state;
		}
		Block block = state.getBlock();
		if (block instanceof StairBlock) {
			if (state.hasProperty(StairBlock.SHAPE) && state.getValue(StairBlock.SHAPE) != StairsShape.STRAIGHT) {
				return state.setValue(StairBlock.SHAPE, stairShape(state, level, pos));
			}
			return state;
		}
		boolean cut = false;
		for (Direction side : Direction.Plane.HORIZONTAL) {
			if (block instanceof CrossCollisionBlock) {
				BooleanProperty arm = PipeBlock.PROPERTY_BY_DIRECTION.get(side);
				if (state.hasProperty(arm) && state.getValue(arm) && ignored(level, pos.relative(side))) {
					state = state.setValue(arm, false);
				}
			} else if (block instanceof WallBlock) {
				EnumProperty<WallSide> arm = StaysPut.WALL_ARMS.get(side);
				if (state.hasProperty(arm) && state.getValue(arm) != WallSide.NONE && ignored(level, pos.relative(side))) {
					state = state.setValue(arm, WallSide.NONE);
					cut = true;
				}
			}
		}
		// A wall that lost an arm shows its post where vanilla would: at an end, a corner or alone.
		if (cut && state.hasProperty(WallBlock.UP) && endOrCorner(state)) {
			state = state.setValue(WallBlock.UP, true);
		}
		return state;
	}

	/**
	 * Whether the chunk holding {@code pos}, or for a spot on its edge the chunk beside it, keeps any
	 * locks: the neighbours a block looks at (one block away) are in one of those. One chunk lookup in
	 * the common case instead of one per neighbour.
	 */
	public static boolean locksNear(BlockGetter level, BlockPos pos) {
		if (!(level instanceof Level world)) {
			return false;
		}
		int cx = SectionPos.blockToSectionCoord(pos.getX()), cz = SectionPos.blockToSectionCoord(pos.getZ());
		int lx = pos.getX() & 15, lz = pos.getZ() & 15;
		int dx = lx <= 1 ? -1 : lx >= 14 ? 1 : 0, dz = lz <= 1 ? -1 : lz >= 14 ? 1 : 0;
		return hasLocks(world, cx, cz) || dx != 0 && hasLocks(world, cx + dx, cz) || dz != 0 && hasLocks(world, cx, cz + dz)
				|| dx != 0 && dz != 0 && hasLocks(world, cx + dx, cz + dz);
	}

	private static boolean hasLocks(Level world, int cx, int cz) {
		LevelChunk chunk = world.getChunkSource().getChunkNow(cx, cz);
		if (chunk == null) {
			return false;
		}
		Locks locks = Platform.chunks().locks(chunk);
		return locks != null && !locks.isEmpty();
	}

	private static boolean endOrCorner(BlockState wall) {
		boolean n = wall.getValue(WallBlock.NORTH_WALL) == WallSide.NONE, s = wall.getValue(WallBlock.SOUTH_WALL) == WallSide.NONE;
		boolean e = wall.getValue(WallBlock.EAST_WALL) == WallSide.NONE, w = wall.getValue(WallBlock.WEST_WALL) == WallSide.NONE;
		return n && s && e && w || n != s || e != w;
	}

	/** Vanilla's stair shape (StairBlock.getStairsShape), with ignored stairs counted as not there. */
	private static StairsShape stairShape(BlockState state, BlockGetter level, BlockPos pos) {
		Direction facing = state.getValue(StairBlock.FACING);
		BlockPos behind = pos.relative(facing);
		BlockState behindState = level.getBlockState(behind);
		if (plainStair(level, behind, behindState) && state.getValue(StairBlock.HALF) == behindState.getValue(StairBlock.HALF)) {
			Direction behindFacing = behindState.getValue(StairBlock.FACING);
			if (behindFacing.getAxis() != facing.getAxis() && canTakeShape(state, level, pos, behindFacing.getOpposite())) {
				return behindFacing == facing.getCounterClockWise() ? StairsShape.OUTER_LEFT : StairsShape.OUTER_RIGHT;
			}
		}
		BlockPos front = pos.relative(facing.getOpposite());
		BlockState frontState = level.getBlockState(front);
		if (plainStair(level, front, frontState) && state.getValue(StairBlock.HALF) == frontState.getValue(StairBlock.HALF)) {
			Direction frontFacing = frontState.getValue(StairBlock.FACING);
			if (frontFacing.getAxis() != facing.getAxis() && canTakeShape(state, level, pos, frontFacing)) {
				return frontFacing == facing.getCounterClockWise() ? StairsShape.INNER_LEFT : StairsShape.INNER_RIGHT;
			}
		}
		return StairsShape.STRAIGHT;
	}

	private static boolean canTakeShape(BlockState state, BlockGetter level, BlockPos pos, Direction side) {
		BlockPos next = pos.relative(side);
		BlockState neighbour = level.getBlockState(next);
		return !plainStair(level, next, neighbour) || neighbour.getValue(StairBlock.FACING) != state.getValue(StairBlock.FACING)
				|| neighbour.getValue(StairBlock.HALF) != state.getValue(StairBlock.HALF);
	}

	private static boolean plainStair(BlockGetter level, BlockPos pos, BlockState state) {
		return state.getBlock() instanceof StairBlock && !keeps(level, pos);
	}

	/** Whether a lock is kept for {@code pos}, on whichever side asks. Only for a loaded, real world. */
	public static boolean isLocked(BlockGetter level, BlockPos pos) {
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
		// Only lockable blocks are ever locked, and only a change of block ends a lock (a bucket of
		// water poured into a locked fence keeps it).
		if (before.getBlock() == after.getBlock() || !isLockable(before) || chunk.getLevel().isClientSide()) {
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
