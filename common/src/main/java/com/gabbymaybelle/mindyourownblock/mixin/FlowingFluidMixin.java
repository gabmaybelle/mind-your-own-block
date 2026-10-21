package com.gabbymaybelle.mindyourownblock.mixin;

import com.gabbymaybelle.mindyourownblock.StaysPut;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Where spreading water asks whether it may fill a block. A block placed dry with the key says no,
 * so a pond doesn't pour back into it (vanilla turns a spot with two water sources beside it into a
 * source). Buckets and dispensers fill blocks another way, so poured water still fills it.
 */
@Mixin(FlowingFluid.class)
abstract class FlowingFluidMixin {
	@Inject(method = "canHoldSpecificFluid", at = @At("HEAD"), cancellable = true)
	private static void mindYourOwnBlock$keepDry(BlockGetter level, BlockPos pos, BlockState state, Fluid newFluid, CallbackInfoReturnable<Boolean> cir) {
		if (StaysPut.keepsDry(level, pos, state)) {
			cir.setReturnValue(false);
		}
	}
}
