package com.gabbymaybelle.mindyourownblock;

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
	private ExactPlacing() {
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
