package com.gabbymaybelle.mindyourownblock.mixin;

import com.gabbymaybelle.mindyourownblock.ExactMode;
import com.gabbymaybelle.mindyourownblock.ExactPlacing;
import com.gabbymaybelle.mindyourownblock.StaysPut;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * The one placing hook: where a block item has worked out the state it is about to place. Wrapping
 * the call inside {@code place} (rather than {@code getPlacementState} itself) also catches block
 * items that work the state out their own way. It runs on both sides, so the player's own game
 * shows the same shape the server places.
 */
@Mixin(BlockItem.class)
abstract class BlockItemMixin {
	@WrapOperation(method = "place", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/item/BlockItem;getPlacementState(Lnet/minecraft/world/item/context/BlockPlaceContext;)Lnet/minecraft/world/level/block/state/BlockState;"))
	private BlockState mindYourOwnBlock$placeExactly(BlockItem item, BlockPlaceContext context, Operation<BlockState> original) {
		BlockState state = original.call(item, context);
		Player player = context.getPlayer();
		if (state == null || player == null || !ExactMode.isOn(player)) {
			return state;
		}
		return ExactPlacing.exact(state);
	}

	/**
	 * Setting the block reshapes its neighbours, and they would reshape it back: keep it as placed.
	 * Once it's in, a block that reshapes to its neighbours, or that water could flow into, is locked
	 * so it stays that way.
	 */
	@WrapOperation(method = "place", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/item/BlockItem;placeBlock(Lnet/minecraft/world/item/context/BlockPlaceContext;Lnet/minecraft/world/level/block/state/BlockState;)Z"))
	private boolean mindYourOwnBlock$keepWhilePlacing(BlockItem item, BlockPlaceContext context, BlockState state, Operation<Boolean> original) {
		Player player = context.getPlayer();
		if (player == null || !ExactMode.isOn(player)) {
			return original.call(item, context, state);
		}
		boolean placed = ExactPlacing.keeping(context.getClickedPos(), () -> original.call(item, context, state));
		if (placed && StaysPut.isLockable(state)) {
			StaysPut.lock(context.getLevel(), context.getClickedPos());
		}
		return placed;
	}
}
