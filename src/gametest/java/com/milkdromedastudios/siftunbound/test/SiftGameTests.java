package com.milkdromedastudios.siftunbound.test;

import com.milkdromedastudios.siftunbound.entity.Blub;
import com.milkdromedastudios.siftunbound.entity.BlubVariant;
import com.milkdromedastudios.siftunbound.portal.NoteBlockMachine;
import com.milkdromedastudios.siftunbound.portal.WardenPortal;
import com.milkdromedastudios.siftunbound.registry.ModBlocks;
import com.milkdromedastudios.siftunbound.registry.ModEffects;
import com.milkdromedastudios.siftunbound.registry.ModEntities;
import com.milkdromedastudios.siftunbound.registry.ModItems;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.NoteBlock;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public class SiftGameTests {
	/** Frame position inside minecraft:ancient_city/city_center/city_center_1 (found by reading the template). */
	private static final int FRAME_X = 13;

	private static void frame(GameTestHelper h, BlockPos bottomLeftOpening, Direction right, int w, int hgt) {
		for (int a = -1; a <= w; a++) {
			h.setBlock(bottomLeftOpening.relative(right, a).below(), Blocks.REINFORCED_DEEPSLATE);
			h.setBlock(bottomLeftOpening.relative(right, a).above(hgt), Blocks.REINFORCED_DEEPSLATE);
		}
		for (int y = 0; y < hgt; y++) {
			h.setBlock(bottomLeftOpening.relative(right, -1).above(y), Blocks.REINFORCED_DEEPSLATE);
			h.setBlock(bottomLeftOpening.relative(right, w).above(y), Blocks.REINFORCED_DEEPSLATE);
		}
	}

	private static Player tunerPlayer(GameTestHelper h) {
		Player player = h.makeMockPlayer(GameType.SURVIVAL);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.ECHO_TUNER));
		return player;
	}

	/** The real ancient city portal frame (20x6 opening) is woken by the Echo Tuner. */
	@GameTest(structure = "minecraft:ancient_city/city_center/city_center_1", maxTicks = 100)
	public void ancientCityFrameAwakens(GameTestHelper h) {
		// worldgen turns this jigsaw into reinforced deepslate (its final_state); the raw template keeps the jigsaw
		h.setBlock(new BlockPos(FRAME_X, 24, 20), Blocks.REINFORCED_DEEPSLATE);
		h.useBlock(new BlockPos(FRAME_X, 17, 20), tunerPlayer(h));
		h.succeedIf(() -> {
			int count = 0;
			for (int y = 18; y <= 23; y++) {
				for (int z = 11; z <= 30; z++) {
					BlockPos p = new BlockPos(FRAME_X, y, z);
					h.assertBlockPresent(ModBlocks.SIFT_PORTAL, p);
					h.assertBlockProperty(p, BlockStateProperties.HORIZONTAL_AXIS, Direction.Axis.Z);
					count++;
				}
			}
			h.assertValueEqual(count, 120, Component.literal("portal blocks"));
		});
	}

	/** A small frame lights, and the membrane collapses when the frame is broken. */
	@GameTest(maxTicks = 40)
	public void smallFrameLightsAndCollapses(GameTestHelper h) {
		BlockPos base = new BlockPos(3, 2, 3);
		frame(h, base, Direction.EAST, 2, 3);
		h.useBlock(base, tunerPlayer(h));
		h.assertBlockPresent(ModBlocks.SIFT_PORTAL, base);
		h.assertBlockPresent(ModBlocks.SIFT_PORTAL, base.east().above(2));
		h.setBlock(base.above(3), Blocks.AIR);
		h.succeedWhen(() -> {
			h.assertBlockNotPresent(ModBlocks.SIFT_PORTAL, base);
			h.assertBlockNotPresent(ModBlocks.SIFT_PORTAL, base.east().above(2));
		});
	}

	/**
	 * The note block machine: eight note blocks near a frame, tuned to the Sift's melody and played one after another (here
	 * by redstone, through the real NoteBlock code path the mixin hooks), wake the portal.
	 */
	@GameTest(maxTicks = 200)
	public void noteBlockMachineWakesThePortal(GameTestHelper h) {
		BlockPos base = new BlockPos(3, 2, 3);
		frame(h, base, Direction.EAST, 2, 3);
		BlockPos[] blocks = new BlockPos[NoteBlockMachine.MELODY.length];
		for (int i = 0; i < blocks.length; i++) {
			blocks[i] = new BlockPos(i, 2, 6);  // a row of eight inside the 8x8 test area
			h.setBlock(blocks[i], Blocks.NOTE_BLOCK.defaultBlockState().setValue(NoteBlock.NOTE, NoteBlockMachine.MELODY[i]));
		}
		for (int i = 0; i < blocks.length; i++) {
			BlockPos power = blocks[i].south();
			h.runAfterDelay(4 + i * 6L, () -> h.setBlock(power, Blocks.REDSTONE_BLOCK));
		}
		h.succeedWhen(() -> {
			h.assertBlockPresent(ModBlocks.SIFT_PORTAL, base);
			h.assertBlockPresent(ModBlocks.SIFT_PORTAL, base.east().above(2));
		});
	}

	/** A wrong note (or the right notes in the wrong order) leaves the portal dormant. */
	@GameTest(maxTicks = 120)
	public void wrongMelodyLeavesThePortalDormant(GameTestHelper h) {
		BlockPos base = new BlockPos(3, 2, 3);
		frame(h, base, Direction.EAST, 2, 3);
		for (int i = 0; i < NoteBlockMachine.MELODY.length; i++) {
			int note = NoteBlockMachine.MELODY[NoteBlockMachine.MELODY.length - 1 - i];  // backwards
			BlockPos pos = new BlockPos(i, 2, 6);
			h.setBlock(pos, Blocks.NOTE_BLOCK.defaultBlockState().setValue(NoteBlock.NOTE, note));
			h.runAfterDelay(4 + i * 6L, () -> h.setBlock(pos.south(), Blocks.REDSTONE_BLOCK));
		}
		h.runAfterDelay(100, () -> {
			h.assertBlockNotPresent(ModBlocks.SIFT_PORTAL, base);
			h.succeed();
		});
	}

	/** Like the official blub, it cannot be hurt or pushed; only /kill-type damage gets through. */
	@GameTest(maxTicks = 20)
	public void blubsCannotBeHurtOrPushed(GameTestHelper h) {
		Blub blub = h.spawn(ModEntities.BLUB, new BlockPos(3, 2, 3));
		ServerLevel level = h.getLevel();
		blub.hurtServer(level, level.damageSources().generic(), 10.0F);
		blub.hurtServer(level, level.damageSources().playerAttack(h.makeMockPlayer(GameType.SURVIVAL)), 10.0F);
		if (blub.getHealth() < blub.getMaxHealth()) {
			h.fail(Component.literal("blub took damage"));
		}
		blub.push(2.0, 0.0, 2.0);
		if (blub.getDeltaMovement().horizontalDistanceSqr() > 0.01) {
			h.fail(Component.literal("blub was pushed"));
		}
		if (blub.isPushable()) {
			h.fail(Component.literal("blub should not be pushable"));
		}
		blub.hurtServer(level, level.damageSources().genericKill(), 100.0F);
		h.succeedIf(() -> {
			if (blub.isAlive()) {
				h.fail(Component.literal("/kill damage should still work"));
			}
		});
	}

	/** Using the tuner on plain blocks does nothing. */
	@GameTest(maxTicks = 20)
	public void noFrameNoPortal(GameTestHelper h) {
		h.setBlock(new BlockPos(3, 1, 3), Blocks.STONE);
		h.useBlock(new BlockPos(3, 1, 3), tunerPlayer(h));
		h.succeedIf(() -> h.assertBlockNotPresent(ModBlocks.SIFT_PORTAL, new BlockPos(3, 2, 3)));
	}

	/** Ichor drains Overworld mobs, but blubs bathe in it happily. */
	@GameTest(maxTicks = 120)
	public void soulWaterDrainsButNotBlubs(GameTestHelper h) {
		for (int x = 1; x <= 5; x++) {
			for (int z = 1; z <= 5; z++) {
				h.setBlock(new BlockPos(x, 0, z), Blocks.GLASS);
				h.setBlock(new BlockPos(x, 1, z), ModBlocks.SOUL_WATER);
			}
		}
		Pig pig = h.spawn(EntityTypes.PIG, new BlockPos(2, 1, 2));
		Blub blub = h.spawn(ModEntities.BLUB, new BlockPos(4, 1, 4));
		float start = pig.getHealth();
		h.runAfterDelay(80, () -> {
			if (!pig.hasEffect(ModEffects.SOULSICK)) {
				h.fail(Component.literal("pig should be Soulsick"));
			}
			if (!(pig.getHealth() < start)) {
				h.fail(Component.literal("pig health should drain"));
			}
			if (blub.hasEffect(ModEffects.SOULSICK) || blub.getHealth() < blub.getMaxHealth()) {
				h.fail(Component.literal("blub should be immune"));
			}
			h.succeed();
		});
	}

	/**
	 * The GameTest server loads no datapack dimensions, so the Sift is absent here: walking into a portal must then be
	 * harmless (no crash, the mob stays put). Real travel is covered by SiftClientGameTest in a full world.
	 */
	@GameTest(maxTicks = 120)
	public void portalWithoutSiftIsHarmless(GameTestHelper h) {
		BlockPos base = new BlockPos(3, 2, 3);
		frame(h, base, Direction.EAST, 2, 3);
		WardenPortal.tryLight(h.getLevel(), h.absolutePos(base));
		Pig pig = h.spawn(EntityTypes.PIG, base);
		h.runAfterDelay(100, () -> {
			h.assertTrue(pig.isAlive() && pig.level() == h.getLevel(), Component.literal("pig should still be here"));
			h.assertBlockPresent(ModBlocks.SIFT_PORTAL, base);
			h.succeed();
		});
	}

	/**
	 * 26.x reads heightmaps, fluid blocking and suffocation from block tags. Sift blocks must be in them, or mobs spawn
	 * inside the ground, features float, portals get buried and soul water washes through soulshale.
	 */
	@GameTest(maxTicks = 80)
	public void siftBlocksAreSolidForTheGame(GameTestHelper h) {
		BlockPos[] columns = {new BlockPos(1, 1, 1), new BlockPos(3, 1, 1), new BlockPos(5, 1, 1)};
		net.minecraft.world.level.block.Block[] tops = {ModBlocks.SOULSHALE, ModBlocks.SIFT_GRASS_BLOCK, ModBlocks.SIFT_SCULK};
		for (int i = 0; i < columns.length; i++) {
			for (int y = 0; y < 4; y++) {
				h.setBlock(columns[i].above(y), tops[i]);
			}
		}
		for (int i = 0; i < columns.length; i++) {
			var state = h.getBlockState(columns[i].above(3));
			for (var t : java.util.List.of(net.minecraft.tags.BlockTags.BLOCKS_MOTION_IN_HEIGHTMAP_NO_LEAVES,
				net.minecraft.tags.BlockTags.BLOCKS_MOTION_IN_HEIGHTMAP, net.minecraft.tags.BlockTags.BLOCKS_FLUID_FLOW)) {
				h.assertTrue(state.is(t), Component.literal(state + " should be in #" + t.location()));
			}
		}
		// soul water next to a soulshale wall must not wash it away
		h.setBlock(new BlockPos(1, 1, 4), ModBlocks.SOUL_WATER);
		h.setBlock(new BlockPos(2, 1, 4), ModBlocks.SOULSHALE);
		h.runAfterDelay(60, () -> {
			h.assertBlockPresent(ModBlocks.SOULSHALE, new BlockPos(2, 1, 4));
			h.assertTrue(h.getBlockState(new BlockPos(2, 1, 4)).getFluidState().isEmpty(), Component.literal("soulshale must stay dry"));
			h.succeed();
		});
	}

	/** A blub keeps its look when carried in a Soul Water Bucket. */
	@GameTest(maxTicks = 20)
	public void blubBucketKeepsVariant(GameTestHelper h) {
		Blub blub = h.spawn(ModEntities.BLUB, new BlockPos(2, 1, 2));
		blub.setVariant(BlubVariant.CARAPACE);
		ItemStack bucket = blub.getBucketItemStack();
		blub.saveToBucketTag(bucket);
		Blub copy = h.spawn(ModEntities.BLUB, new BlockPos(4, 1, 4));
		copy.loadFromBucketTag(bucket.getOrDefault(DataComponents.BUCKET_ENTITY_DATA, CustomData.EMPTY).copyTag());
		h.assertValueEqual(copy.getVariant(), BlubVariant.CARAPACE, Component.literal("variant"));
		h.assertTrue(bucket.is(ModItems.BLUB_BUCKET), Component.literal("bucket item"));
		// blubs are the Sift's resident animals: like other animals they must not despawn when the player walks away
		h.assertTrue(!blub.removeWhenFarAway(1.0E6), Component.literal("blubs should not despawn"));
		h.succeed();
	}

	/** Bone meal on Sift grass grows Sift plants. */
	@GameTest(maxTicks = 20)
	public void siftGrassGrowsPlants(GameTestHelper h) {
		for (int x = 0; x < 7; x++) {
			for (int z = 0; z < 7; z++) {
				h.setBlock(new BlockPos(x, 1, z), ModBlocks.SIFT_GRASS_BLOCK);
			}
		}
		var level = h.getLevel();
		BlockPos center = h.absolutePos(new BlockPos(3, 1, 3));
		((net.minecraft.world.level.block.BonemealableBlock) ModBlocks.SIFT_GRASS_BLOCK).performBonemeal(level, level.getRandom(), center,
			level.getBlockState(center), net.minecraft.world.level.block.BonemealSource.INTERACTION);
		h.succeedIf(() -> {
			int plants = 0;
			for (int x = 0; x < 7; x++) {
				for (int z = 0; z < 7; z++) {
					if (!h.getBlockState(new BlockPos(x, 2, z)).isAir()) {
						plants++;
					}
				}
			}
			h.assertTrue(plants > 0, Component.literal("bone meal should grow at least one plant"));
		});
	}
}
