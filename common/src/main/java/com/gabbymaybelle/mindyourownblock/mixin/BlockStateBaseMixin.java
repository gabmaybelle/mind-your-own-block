package com.gabbymaybelle.mindyourownblock.mixin;

import com.gabbymaybelle.mindyourownblock.ExactPlacing;
import com.gabbymaybelle.mindyourownblock.StaysPut;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelAccessor;
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
 * the game, so the first check is whether the block is a kind that reshapes at all. A normal block's
 * new shape leaves out any locked neighbour.
 *
 * <p>1.21.1's {@code updateShape} takes the direction and neighbour first and the level third (26.x
 * reordered them and added the tick access and random); the hook's body is the same.
 */
@Mixin(BlockBehaviour.BlockStateBase.class)
abstract class BlockStateBaseMixin {
	@Inject(method = "updateShape", at = @At("RETURN"), cancellable = true)
	private void mindYourOwnBlock$keepShape(Direction directionToNeighbour, BlockState neighbourState, LevelAccessor level, BlockPos pos,
			BlockPos neighbourPos, CallbackInfoReturnable<BlockState> cir) {
		BlockState before = (BlockState) (Object) this;
		if (!StaysPut.isShaped(before)) {
			return;
		}
		// No exact placing going on and no lock near: nothing to keep or leave out (almost every update).
		if (!ExactPlacing.placingAnything() && !StaysPut.locksNear(level, pos)) {
			return;
		}
		if (StaysPut.keeps(level, pos)) {
			cir.setReturnValue(StaysPut.keepShape(before, cir.getReturnValue()));
		} else if (StaysPut.isShaped(cir.getReturnValue())) {
			// A normal block beside a locked one doesn't reach toward it or bend because of it.
			cir.setReturnValue(StaysPut.awayFromKept(level, pos, cir.getReturnValue()));
		}
	}
}
