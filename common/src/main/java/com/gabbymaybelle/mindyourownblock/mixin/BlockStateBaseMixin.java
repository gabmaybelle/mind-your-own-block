package com.gabbymaybelle.mindyourownblock.mixin;

import com.gabbymaybelle.mindyourownblock.StaysPut;
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
 * Where a block reshapes itself because a neighbour changed. A block placed exactly (being placed
 * right now, or locked) lets vanilla do everything else it does here (water, breaking when it can't
 * survive) and then gets its arms, post and stair shape back. This runs on every neighbour update in
 * the game, so the first check is whether the block is a kind that reshapes at all.
 */
@Mixin(BlockBehaviour.BlockStateBase.class)
abstract class BlockStateBaseMixin {
	@Inject(method = "updateShape", at = @At("RETURN"), cancellable = true)
	private void mindYourOwnBlock$keepShape(LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction directionToNeighbour,
			BlockPos neighbourPos, BlockState neighbourState, RandomSource random, CallbackInfoReturnable<BlockState> cir) {
		BlockState before = (BlockState) (Object) this;
		if (StaysPut.isShaped(before) && StaysPut.keeps(level, pos)) {
			cir.setReturnValue(StaysPut.keepShape(before, cir.getReturnValue()));
		}
	}
}
