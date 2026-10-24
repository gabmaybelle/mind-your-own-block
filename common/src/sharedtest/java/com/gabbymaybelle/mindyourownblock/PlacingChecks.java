package com.gabbymaybelle.mindyourownblock;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CrossCollisionBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.StairsShape;
import net.minecraft.world.level.block.state.properties.WallSide;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The placing checks, shared by the Fabric and NeoForge in-game tests (both compile this folder in;
 * it never ships in a jar). They run on the server thread while the player's key is held, or not:
 * each block is placed the way a right-click places it ({@code ItemStack.useOn}), as the test player,
 * so whether it comes out exact depends only on the key the player's game reported.
 *
 * <p>Every case is set up so vanilla would reshape the block (a neighbour to join, a stair to bend
 * into, water to fill it), and the same case without the key must come out vanilla's way: that
 * proves the setup, so a pass with the key can't be luck.
 */
final class PlacingChecks {
	/** Records one check; each loader's test passes its own. */
	interface Check {
		void check(boolean ok, String what);
	}

	private PlacingChecks() {
	}

	/** Places every case beside the player, with the key held ({@code exact}) or not, and checks each. */
	static void run(ServerPlayer player, boolean exact, Check check) {
		String with = exact ? " with the key" : " without the key";
		List<BlockPos> water = new ArrayList<>();
		int[] slot = { 0 };

		// No joining: a neighbour on the east side that vanilla would join to.
		joins(player, next(player, slot), Items.OAK_FENCE, Blocks.OAK_FENCE, exact, "an oak fence" + with, check);
		joins(player, next(player, slot), Items.GLASS_PANE, Blocks.GLASS_PANE, exact, "a glass pane" + with, check);
		joins(player, next(player, slot), Items.IRON_BARS, Blocks.IRON_BARS, exact, "iron bars" + with, check);
		walls(player, next(player, slot), Items.COBBLESTONE_WALL, Blocks.COBBLESTONE_WALL, exact, "a cobblestone wall" + with, check);
		// Modded ones built on vanilla's, when Macaw's Fences and Walls is installed (one CI run).
		ResourceLocation picket = ResourceLocation.fromNamespaceAndPath("mcwfences", "oak_picket_fence");
		ResourceLocation pillar = ResourceLocation.fromNamespaceAndPath("mcwfences", "andesite_pillar_wall");
		if (BuiltInRegistries.BLOCK.containsKey(picket)) {
			joins(player, next(player, slot), BuiltInRegistries.ITEM.get(picket), BuiltInRegistries.BLOCK.get(picket), exact,
					"Macaw's oak picket fence" + with, check);
			// Macaw's pillar walls are fences underneath (arms are true or false, no post), so checked as one.
			joins(player, next(player, slot), BuiltInRegistries.ITEM.get(pillar), BuiltInRegistries.BLOCK.get(pillar), exact,
					"Macaw's andesite pillar wall" + with, check);
		}

		// Straight stairs: the stair in front faces across, so vanilla bends the new one into an outer corner.
		{
			BlockPos at = next(player, slot);
			player.serverLevel().setBlock(at.north(), Blocks.OAK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.EAST), 3);
			BlockState placed = place(player, at, Items.OAK_STAIRS, 180F);
			boolean straight = placed.is(Blocks.OAK_STAIRS) && placed.getValue(StairBlock.SHAPE) == StairsShape.STRAIGHT;
			check.check(placed.is(Blocks.OAK_STAIRS), "an oak stair" + with + " is placed (got " + placed + ")");
			check.check(straight == exact, "an oak stair" + with + (exact ? " stays straight" : " bends into a corner") + " (got " + placed + ")");
		}

		// Dry blocks: placed into a water source.
		dry(player, next(player, slot), Items.OAK_FENCE, Blocks.OAK_FENCE, exact, "an oak fence placed in water" + with, check, water);
		dry(player, next(player, slot), Items.OAK_STAIRS, Blocks.OAK_STAIRS, exact, "an oak stair placed in water" + with, check, water);

		// Tidy the water away before it can flow: nothing has ticked yet, since this all runs at once.
		for (BlockPos at : water) {
			player.serverLevel().setBlock(at, Blocks.AIR.defaultBlockState(), 3);
		}
	}

	private static void joins(ServerPlayer player, BlockPos at, Item item, Block block, boolean exact, String what, Check check) {
		player.serverLevel().setBlock(at.east(), block.defaultBlockState(), 3);
		BlockState placed = place(player, at, item, 0F);
		check.check(placed.is(block), what + " is placed (got " + placed + ")");
		boolean joined = placed.hasProperty(CrossCollisionBlock.EAST) && placed.getValue(CrossCollisionBlock.EAST);
		check.check(joined != exact, what + (exact ? " stands alone" : " joins its neighbour") + " (got " + placed + ")");
		// The neighbour doesn't reach toward an exact one either (Gabby, 2026-10-21), and does toward a normal one.
		BlockState neighbour = player.serverLevel().getBlockState(at.east());
		boolean reaches = neighbour.hasProperty(CrossCollisionBlock.WEST) && neighbour.getValue(CrossCollisionBlock.WEST);
		check.check(reaches != exact, what + ": its neighbour " + (exact ? "doesn't reach toward it" : "joins it") + " (got " + neighbour + ")");
	}

	private static void walls(ServerPlayer player, BlockPos at, Item item, Block block, boolean exact, String what, Check check) {
		player.serverLevel().setBlock(at.east(), block.defaultBlockState(), 3);
		BlockState placed = place(player, at, item, 0F);
		check.check(placed.is(block), what + " is placed (got " + placed + ")");
		boolean joined = placed.hasProperty(WallBlock.EAST_WALL) && placed.getValue(WallBlock.EAST_WALL) != WallSide.NONE;
		check.check(joined != exact, what + (exact ? " stands alone" : " joins its neighbour") + " (got " + placed + ")");
		BlockState neighbour = player.serverLevel().getBlockState(at.east());
		boolean reaches = neighbour.hasProperty(WallBlock.WEST_WALL) && neighbour.getValue(WallBlock.WEST_WALL) != WallSide.NONE;
		check.check(reaches != exact, what + ": its neighbour " + (exact ? "doesn't reach toward it" : "joins it") + " (got " + neighbour + ")");
		if (exact) {
			check.check(placed.hasProperty(WallBlock.UP) && placed.getValue(WallBlock.UP), what + " shows its post (got " + placed + ")");
		}
	}

	private static void dry(ServerPlayer player, BlockPos at, Item item, Block block, boolean exact, String what, Check check, List<BlockPos> water) {
		ServerLevel level = player.serverLevel();
		level.setBlock(at, Blocks.WATER.defaultBlockState(), 3);
		water.add(at);
		BlockState placed = place(player, at, item, 0F);
		check.check(placed.is(block), what + " is placed (got " + placed + ")");
		boolean wet = placed.hasProperty(BlockStateProperties.WATERLOGGED) && placed.getValue(BlockStateProperties.WATERLOGGED);
		check.check(wet != exact, what + (exact ? " is dry" : " is full of water") + " (got " + placed + ")");
		if (exact && placed.is(block)) {
			// Buckets are never touched: water poured on the dry block still fills it.
			((BucketItem) Items.WATER_BUCKET).emptyContents(player, level, at, null);
			BlockState filled = level.getBlockState(at);
			check.check(filled.hasProperty(BlockStateProperties.WATERLOGGED) && filled.getValue(BlockStateProperties.WATERLOGGED),
					what + " fills with a water bucket afterwards (got " + filled + ")");
		}
	}

	/** Places the item at {@code at} by clicking the top of the stone below it, the player facing {@code yaw}. */
	static BlockState place(ServerPlayer player, BlockPos at, Item item, float yaw) {
		player.setYRot(yaw);
		player.setXRot(0F);
		ItemStack stack = new ItemStack(item);
		player.setItemInHand(InteractionHand.MAIN_HAND, stack);
		BlockPos floor = at.below();
		BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(floor).add(0, 0.5, 0), Direction.UP, floor, false);
		stack.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
		player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		return player.serverLevel().getBlockState(at);
	}

	/** The next case's spot: its own clear patch on a stone floor, 6 blocks apart, east of the player. */
	private static BlockPos next(ServerPlayer player, int[] slot) {
		return clear(player.serverLevel(), player.blockPosition().offset(6 + 6 * slot[0]++, 0, 4));
	}

	/** Clears a 5 by 5 patch around {@code at}, three high, on a stone floor, and returns {@code at}. */
	static BlockPos clear(ServerLevel level, BlockPos at) {
		for (BlockPos pos : BlockPos.betweenClosed(at.offset(-2, -1, -2), at.offset(2, 2, 2))) {
			level.setBlock(pos, pos.getY() < at.getY() ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState(), 3);
		}
		return at.immutable();
	}
}
