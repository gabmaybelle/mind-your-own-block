package com.gabbymaybelle.mindyourownblock;

import java.util.function.BooleanSupplier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CrossCollisionBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.StairsShape;
import net.minecraft.world.level.block.state.properties.WallSide;

/**
 * The placing rules: what a block placed with the key looks like. They go by the kind of block
 * (vanilla's fence, pane, wall and stair classes) and its properties, never by block names, so
 * modded fences, walls, panes and stairs built on vanilla's follow the same rules.
 */
public final class ExactPlacing {
	/**
	 * The spot being placed exactly right now, on this thread (the server's, or the player's own game
	 * predicting it). Setting the block makes its neighbours reshape, and a neighbour that grows an
	 * arm toward it would reshape it back; while this is set, that spot keeps its state.
	 */
	private static final ThreadLocal<BlockPos> PLACING = new ThreadLocal<>();
	/** How many threads are placing exactly, so every other neighbour update skips the lookup. */
	private static volatile int placingCount;

	private ExactPlacing() {
	}

	/** Runs the placing with {@code pos} kept as placed until it's done. */
	public static boolean keeping(BlockPos pos, BooleanSupplier place) {
		BlockPos before = PLACING.get();
		PLACING.set(pos.immutable());
		synchronized (ExactPlacing.class) {
			placingCount++;
		}
		try {
			return place.getAsBoolean();
		} finally {
			synchronized (ExactPlacing.class) {
				placingCount--;
			}
			if (before == null) {
				PLACING.remove();
			} else {
				PLACING.set(before);
			}
		}
	}

	/** Whether any thread is placing exactly right now. */
	public static boolean placingAnything() {
		return placingCount != 0;
	}

	/** Whether the block at {@code pos} keeps its state instead of reshaping to a neighbour. */
	public static boolean keeps(BlockPos pos) {
		if (placingCount == 0) {
			return false;
		}
		return pos.equals(PLACING.get());
	}

	/** The state to place instead of the one the game worked out from the neighbours. */
	public static BlockState exact(BlockState state) {
		Block block = state.getBlock();
		// Fences, glass panes and iron bars: no arms toward the neighbours.
		if (block instanceof CrossCollisionBlock) {
			for (BooleanProperty side : CrossCollisionBlock.PROPERTY_BY_DIRECTION.values()) {
				if (state.hasProperty(side)) {
					state = state.setValue(side, false);
				}
			}
		}
		// Walls: no arms, and the post stands up as it does on a wall on its own.
		if (block instanceof WallBlock) {
			for (EnumProperty<WallSide> side : WallBlock.PROPERTY_BY_DIRECTION.values()) {
				if (state.hasProperty(side)) {
					state = state.setValue(side, WallSide.NONE);
				}
			}
			if (state.hasProperty(WallBlock.UP)) {
				state = state.setValue(WallBlock.UP, true);
			}
		}
		// Stairs: straight, never an inner or outer corner.
		if (block instanceof StairBlock && state.hasProperty(StairBlock.SHAPE)) {
			state = state.setValue(StairBlock.SHAPE, StairsShape.STRAIGHT);
		}
		// Any block placed into water: dry. Buckets are never touched, so one still fills it later.
		if (state.hasProperty(BlockStateProperties.WATERLOGGED)) {
			state = state.setValue(BlockStateProperties.WATERLOGGED, false);
		}
		return state;
	}
}
