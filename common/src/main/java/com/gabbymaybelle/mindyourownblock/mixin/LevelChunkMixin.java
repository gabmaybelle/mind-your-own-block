package com.gabbymaybelle.mindyourownblock.mixin;

import com.gabbymaybelle.mindyourownblock.StaysPut;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Every block change in a loaded chunk, right where the old block is swapped for the new one. When
 * the block at a locked spot becomes a different block (broken, replaced, or turned into a moving
 * block by a piston), the lock ends, so whatever is placed there next acts like vanilla.
 */
@Mixin(LevelChunk.class)
abstract class LevelChunkMixin {
	@ModifyExpressionValue(method = "setBlockState", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/level/chunk/LevelChunkSection;setBlockState(IIILnet/minecraft/world/level/block/state/BlockState;)Lnet/minecraft/world/level/block/state/BlockState;"))
	private BlockState mindYourOwnBlock$endLock(BlockState before, @Local(argsOnly = true) BlockPos pos, @Local(argsOnly = true) BlockState after) {
		StaysPut.onChanged((LevelChunk) (Object) this, pos, before, after);
		return before;
	}
}
