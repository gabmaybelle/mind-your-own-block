package com.gabbymaybelle.mindyourownblock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CrossCollisionBlock;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.StairsShape;

/**
 * The stays-put checks, shared by the Fabric and NeoForge in-game tests like {@link PlacingChecks}.
 * They run in steps, with game ticks in between (a piston needs a few to move):
 * {@link #placeLocked} with the key held, then {@link #neighbours} without it, {@link #afterPiston},
 * {@link #clientAgrees} on the player's own game, and {@link #afterRejoin} once the world has been
 * saved, closed and opened again.
 *
 * <p>Five spots on their own stone patch, west of the player: post A (neighbours, a bucket, then
 * broken), the stair, post B (pushed by a piston), post C (kept, for the rejoin) and the pond post (a
 * dry post in the middle of a 3 by 3 pond, which vanilla would fill: two water sources beside a
 * spot make it a source too).
 */
final class StaysPutChecks {
	private static BlockPos postA;
	private static BlockPos stair;
	private static BlockPos postB;
	private static BlockPos postC;
	private static BlockPos pondPost;
	private static BlockState postCState;

	private StaysPutChecks() {
	}

	/** With the key held: the four blocks placed exactly, which locks them. */
	static void placeLocked(ServerPlayer player, PlacingChecks.Check check) {
		BlockPos base = player.blockPosition().offset(-8, 0, -10);
		postA = PlacingChecks.clear(player.serverLevel(), base);
		stair = PlacingChecks.clear(player.serverLevel(), base.offset(-6, 0, 0));
		postB = PlacingChecks.clear(player.serverLevel(), base.offset(-12, 0, 0));
		postC = PlacingChecks.clear(player.serverLevel(), base.offset(-18, 0, 0));
		for (BlockPos post : new BlockPos[] { postA, postB, postC }) {
			PlacingChecks.place(player, post, Items.OAK_FENCE, 0F);
		}
		// The pond: sunk into the patch floor, with stone all round, so the water has nowhere to go.
		pondPost = PlacingChecks.clear(player.serverLevel(), base.offset(-24, 0, 0)).below();
		for (BlockPos at : BlockPos.betweenClosed(pondPost.offset(-1, 0, -1), pondPost.offset(1, 0, 1))) {
			player.serverLevel().setBlock(at, Blocks.WATER.defaultBlockState(), 3);
		}
		PlacingChecks.place(player, pondPost, Items.OAK_FENCE, 0F);
		check.check(!player.serverLevel().getBlockState(pondPost).getValue(BlockStateProperties.WATERLOGGED),
				"a post placed with the key in the middle of a pond is dry (" + player.serverLevel().getBlockState(pondPost) + ")");
		// Facing north, so a stair placed in front of it later (crossways) would bend it.
		PlacingChecks.place(player, stair, Items.OAK_STAIRS, 180F);
		ServerLevel level = player.serverLevel();
		for (BlockPos at : new BlockPos[] { postA, stair, postB, postC, pondPost }) {
			check.check(StaysPut.isLocked(level, at), "a block placed with the key is locked (" + level.getBlockState(at) + ")");
		}
	}

	/** Without the key: neighbours come and go, a bucket fills post A, it's broken; the piston is set off. */
	static void neighbours(ServerPlayer player, PlacingChecks.Check check) {
		ServerLevel level = player.serverLevel();
		check.check(!StaysPut.isLocked(level, postA.north(3)), "an empty spot isn't locked");

		// Post A: a normal fence on every side, placed the vanilla way.
		for (Direction side : Direction.Plane.HORIZONTAL) {
			PlacingChecks.place(player, postA.relative(side), Items.OAK_FENCE, 0F);
		}
		check.check(alone(level.getBlockState(postA)), "a locked post stays alone with a fence on every side (" + level.getBlockState(postA) + ")");
		// Gabby, 2026-10-21: a normal fence beside a locked post ends its line instead of reaching toward it.
		for (Direction side : Direction.Plane.HORIZONTAL) {
			BlockState beside = level.getBlockState(postA.relative(side));
			check.check(beside.is(Blocks.OAK_FENCE) && !beside.getValue(PipeBlock.PROPERTY_BY_DIRECTION.get(side.getOpposite())),
					"a normal fence placed " + side.getName() + " of a locked post doesn't reach toward it (" + beside + ")");
		}
		for (Direction side : Direction.Plane.HORIZONTAL) {
			level.setBlock(postA.relative(side), Blocks.AIR.defaultBlockState(), 3);
		}
		check.check(alone(level.getBlockState(postA)) && StaysPut.isLocked(level, postA),
				"a locked post is unchanged and still locked once its neighbours are gone (" + level.getBlockState(postA) + ")");

		// A water bucket still fills it, and it stays locked and alone.
		level.setBlock(postA.east(), Blocks.OAK_FENCE.defaultBlockState(), 3);
		((BucketItem) Items.WATER_BUCKET).emptyContents(player, level, postA, null);
		BlockState wet = level.getBlockState(postA);
		check.check(wet.getValue(BlockStateProperties.WATERLOGGED) && alone(wet) && StaysPut.isLocked(level, postA),
				"a water bucket fills a locked post, which stays locked and alone (" + wet + ")");

		// Broken: the lock ends, and a normal fence placed there joins like vanilla.
		level.destroyBlock(postA, false);
		level.setBlock(postA, Blocks.AIR.defaultBlockState(), 3);
		check.check(!StaysPut.isLocked(level, postA), "breaking a locked post ends its lock");
		BlockState replaced = PlacingChecks.place(player, postA, Items.OAK_FENCE, 0F);
		check.check(replaced.is(Blocks.OAK_FENCE) && replaced.getValue(CrossCollisionBlock.EAST),
				"a normal fence placed where a locked post was joins like vanilla (" + replaced + ")");

		// The stair: a crossways stair placed in front of it would bend it into a corner.
		level.setBlock(stair.north(), Blocks.OAK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.EAST), 3);
		BlockState stairNow = level.getBlockState(stair);
		check.check(stairNow.getValue(StairBlock.SHAPE) == StairsShape.STRAIGHT,
				"a locked stair stays straight when a stair is placed in front of it (" + stairNow + ")");
		// A normal stair placed facing east just west of the locked stair would bend into a corner
		// because of it (the locked stair is behind it, crossways); it stays straight instead.
		BlockState beside = PlacingChecks.place(player, stair.west(), Items.OAK_STAIRS, -90F);
		check.check(beside.is(Blocks.OAK_STAIRS) && beside.getValue(StairBlock.SHAPE) == StairsShape.STRAIGHT,
				"a normal stair beside a locked stair doesn't bend because of it (" + beside + ")");

		// Post C: a neighbour that stays, for the client and rejoin checks.
		PlacingChecks.place(player, postC.east(), Items.OAK_FENCE, 0F);
		postCState = level.getBlockState(postC);
		check.check(alone(postCState), "post C stays alone beside a fence (" + postCState + ")");

		// Post B: a piston on its west side, pushing east, set off by a block of redstone.
		level.setBlock(postB.west(), Blocks.PISTON.defaultBlockState().setValue(PistonBaseBlock.FACING, Direction.EAST), 3);
		level.setBlock(postB.west(2), Blocks.REDSTONE_BLOCK.defaultBlockState(), 3);
	}

	/** A few ticks after the piston fired: the pushed post has lost its lock and acts like vanilla. */
	static void afterPiston(ServerPlayer player, PlacingChecks.Check check) {
		ServerLevel level = player.serverLevel();
		BlockPos moved = postB.east();
		BlockState there = level.getBlockState(moved);
		check.check(there.is(Blocks.OAK_FENCE), "the piston pushed the post one block east (found " + there + ")");
		check.check(!StaysPut.isLocked(level, postB) && !StaysPut.isLocked(level, moved), "a post pushed by a piston isn't locked any more");
		level.setBlock(moved.east(), Blocks.OAK_FENCE.defaultBlockState(), 3);
		BlockState after = level.getBlockState(moved);
		check.check(after.is(Blocks.OAK_FENCE) && after.getValue(CrossCollisionBlock.EAST),
				"a pushed post joins a new neighbour like vanilla (" + after + ")");

		// The pond post, a second of game time later: the pond hasn't poured into it, and a bucket still fills it.
		BlockState pond = level.getBlockState(pondPost);
		check.check(pond.is(Blocks.OAK_FENCE) && !pond.getValue(BlockStateProperties.WATERLOGGED),
				"a dry post in the middle of a pond is still dry after the water has had time to flow (" + pond + ")");
		((BucketItem) Items.WATER_BUCKET).emptyContents(player, level, pondPost, null);
		BlockState poured = level.getBlockState(pondPost);
		check.check(poured.getValue(BlockStateProperties.WATERLOGGED) && StaysPut.isLocked(level, pondPost),
				"a water bucket still fills the dry pond post, which stays locked (" + poured + ")");
		for (BlockPos at : BlockPos.betweenClosed(pondPost.offset(-1, 0, -1), pondPost.offset(1, 0, 1))) {
			level.setBlock(at, Blocks.STONE.defaultBlockState(), 3);
		}
	}

	/** On the player's own game: it knows post C is locked, and shows it as the server has it. */
	static void clientAgrees(Level clientLevel, String when, PlacingChecks.Check check) {
		check.check(StaysPut.isLocked(clientLevel, postC), "the player's game knows post C is locked " + when);
		BlockState seen = clientLevel.getBlockState(postC);
		check.check(seen.equals(postCState), "the player's game shows post C as the server has it " + when + " (" + seen + ", server " + postCState + ")");
	}

	/** After the world was saved, closed and opened again: post C is still locked and still alone. */
	static void afterRejoin(ServerPlayer player, PlacingChecks.Check check) {
		ServerLevel level = player.serverLevel();
		check.check(StaysPut.isLocked(level, postC), "post C is still locked after the world is closed and opened again");
		check.check(level.getBlockState(postC).equals(postCState), "post C is unchanged after reopening (" + level.getBlockState(postC) + ")");
		PlacingChecks.place(player, postC.west(), Items.OAK_FENCE, 0F);
		check.check(alone(level.getBlockState(postC)), "post C stays alone when a fence is placed beside it after reopening (" + level.getBlockState(postC) + ")");
		postCState = level.getBlockState(postC);
	}

	private static boolean alone(BlockState state) {
		for (Direction side : Direction.Plane.HORIZONTAL) {
			if (state.getValue(PipeBlock.PROPERTY_BY_DIRECTION.get(side))) {
				return false;
			}
		}
		return true;
	}
}
