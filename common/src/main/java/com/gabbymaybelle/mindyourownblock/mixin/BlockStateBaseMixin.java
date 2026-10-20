package com.gabbymaybelle.mindyourownblock.mixin;

import com.gabbymaybelle.mindyourownblock.ExactPlacing;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Where a block reshapes itself because a neighbour changed. A block being placed exactly keeps
 * the state it was placed in, so the neighbour reaching toward it can't pull it back into joining.
 * This runs on every neighbour update in the game, so the check is a single read when nothing is
 * being placed.
 */
@Mixin(BlockBehaviour.BlockStateBase.class)
abstract class BlockStateBaseMixin {
	@Inject(method = "updateShape", at = @At("HEAD"), cancellable = true)
	private void mindYourOwnBlock$keepShape(LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction directionToNeighbour,
			BlockPos neighbourPos, BlockState neighbourState, RandomSource random, CallbackInfoReturnable<BlockState> cir) {
		if (ExactPlacing.keeps(pos)) {
			cir.setReturnValue((BlockState) (Object) this);
		}
	}
}
