package com.milkdromedastudios.siftunbound.test;

import com.milkdromedastudios.siftunbound.SiftUnbound;
import com.milkdromedastudios.siftunbound.entity.Blub;
import com.milkdromedastudios.siftunbound.entity.BlubVariant;
import com.milkdromedastudios.siftunbound.entity.EchoGolem;
import com.milkdromedastudios.siftunbound.entity.Monarch;
import com.milkdromedastudios.siftunbound.entity.Nuzzle;
import com.milkdromedastudios.siftunbound.entity.Scavenger;
import com.milkdromedastudios.siftunbound.entity.Singer;
import com.milkdromedastudios.siftunbound.entity.projectile.PollenGlob;
import com.milkdromedastudios.siftunbound.entity.projectile.SoulBolt;
import com.milkdromedastudios.siftunbound.entity.sifter.AbstractSifter;
import com.milkdromedastudios.siftunbound.entity.sifter.Harmonizer;
import com.milkdromedastudios.siftunbound.entity.sifter.Nester;
import com.milkdromedastudios.siftunbound.entity.sifter.Seedling;
import com.milkdromedastudios.siftunbound.entity.sifter.Sprout;
import com.milkdromedastudios.siftunbound.portal.NoteBlockMachine;
import com.milkdromedastudios.siftunbound.portal.WardenPortal;
import com.milkdromedastudios.siftunbound.registry.ModBlocks;
import com.milkdromedastudios.siftunbound.registry.ModCriteria;
import com.milkdromedastudios.siftunbound.registry.ModEffects;
import com.milkdromedastudios.siftunbound.registry.ModEntities;
import com.milkdromedastudios.siftunbound.registry.ModItems;
import com.milkdromedastudios.siftunbound.world.feature.SoulshaleMonolithFeature;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.ServerAdvancementManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.NoteBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.phys.AABB;

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

	/** Nuzzles are sheared for pink wool like sheep, and can't be sheared twice until they graze. */
	@GameTest(maxTicks = 40)
	public void nuzzleShearsForPinkWool(GameTestHelper h) {
		Nuzzle nuzzle = h.spawn(ModEntities.NUZZLE, new BlockPos(3, 2, 3));
		Player player = h.makeMockPlayer(GameType.SURVIVAL);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.SHEARS));
		nuzzle.mobInteract(player, InteractionHand.MAIN_HAND);
		if (!nuzzle.isSheared() || nuzzle.readyForShearing()) {
			h.fail(Component.literal("nuzzle should be sheared"));
		}
		h.succeedWhen(() -> {
			boolean wool = h.getLevel().getEntities(EntityTypes.ITEM, new AABB(h.absolutePos(new BlockPos(3, 2, 3))).inflate(4.0),
				e -> e.getItem().is(Items.WOOL.pick(DyeColor.PINK))).size() > 0;
			if (!wool) {
				h.fail(Component.literal("no pink wool dropped"));
			}
		});
	}

	/** Handed an echo shard near a dormant Warden Portal, an echo golem sings the melody and wakes it. */
	@GameTest(maxTicks = 160)
	public void echoGolemWakesThePortal(GameTestHelper h) {
		BlockPos base = new BlockPos(3, 2, 3);
		frame(h, base, Direction.EAST, 2, 3);
		EchoGolem golem = h.spawn(ModEntities.ECHO_GOLEM, new BlockPos(3, 2, 6));
		Player player = h.makeMockPlayer(GameType.SURVIVAL);
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.ECHO_SHARD, 2));
		golem.mobInteract(player, InteractionHand.MAIN_HAND);
		if (player.getItemInHand(InteractionHand.MAIN_HAND).getCount() != 1) {
			h.fail(Component.literal("the golem should take one echo shard"));
		}
		h.succeedWhen(() -> h.assertBlockPresent(ModBlocks.SIFT_PORTAL, base));
	}

	/** The Singer can't be hurt, and sings when asked. */
	@GameTest(maxTicks = 20)
	public void singerCannotBeHurt(GameTestHelper h) {
		Singer singer = h.spawn(ModEntities.SINGER, new BlockPos(3, 2, 3));
		ServerLevel level = h.getLevel();
		singer.hurtServer(level, level.damageSources().playerAttack(h.makeMockPlayer(GameType.SURVIVAL)), 50.0F);
		singer.mobInteract(h.makeMockPlayer(GameType.SURVIVAL), InteractionHand.MAIN_HAND);
		h.succeedIf(() -> {
			if (singer.getHealth() < singer.getMaxHealth()) {
				h.fail(Component.literal("the singer took damage"));
			}
		});
	}

	/** Every guide advancement loads, and the mod's sift_event trigger awards them. */
	@GameTest(maxTicks = 20)
	public void guideAdvancementsLoadAndAward(GameTestHelper h) {
		ServerAdvancementManager advancements = h.getLevel().getServer().getAdvancements();
		for (String name : List.of("root", "hum_melody", "note_block_machine", "chiseled_soulshale", "light_portal", "enter_sift",
			"soulsick", "soul_shard", "tide_endure", "tide_thrive", "explore_sift", "blub_jelly", "blub_friend", "shear_nuzzle",
			"feed_slabber", "singer", "echo_golem", "golem_portal", "prowler", "scavenger", "sifter", "sprout_enrage", "harmonizer",
			"all_sifters", "sculker_nest", "monarch")) {
			if (advancements.get(SiftUnbound.id(name)) == null) {
				h.fail(Component.literal("advancement " + name + " did not load"));
			}
		}
		ServerPlayer player = h.makeMockServerPlayerInLevel();
		AdvancementHolder hum = advancements.get(SiftUnbound.id("hum_melody"));
		AdvancementHolder thrive = advancements.get(SiftUnbound.id("tide_thrive"));
		ModCriteria.award(player, ModCriteria.HUMMED_MELODY);
		boolean humDone = player.getAdvancements().getOrStartProgress(hum).isDone();
		boolean thriveDone = player.getAdvancements().getOrStartProgress(thrive).isDone();
		h.getLevel().getServer().getPlayerList().remove(player);
		if (!humDone) {
			h.fail(Component.literal("the hummed_melody event did not award Hum Along"));
		}
		if (thriveDone) {
			h.fail(Component.literal("an unrelated event awarded Thrive"));
		}
		h.succeed();
	}

	/** The soulshale monolith configured in the datapack generates on Sift ground, plinth first. */
	@GameTest(maxTicks = 20)
	public void monolithGenerates(GameTestHelper h) {
		int[] counts = placeFeature(h, configured(h, "soulshale_monolith"), ModBlocks.SOULSHALE_BRICKS, ModBlocks.POLISHED_SOULSHALE);
		if (counts[1] < 10) {
			h.fail(Component.literal("no plinth: " + counts[1] + " polished soulshale"));
		}
		h.succeed();
	}

	/** A monolith left standing has its singer relief set into both faces, and a stele of bricks above the plinth. */
	@GameTest(maxTicks = 20)
	public void standingMonolithHasReliefs(GameTestHelper h) {
		int[] counts = placeFeature(h, new SoulshaleMonolithFeature(ConstantInt.of(7), 0.0F), ModBlocks.CHISELED_SOULSHALE, ModBlocks.POLISHED_SOULSHALE);
		if (counts[0] != 2) {
			h.fail(Component.literal("expected 2 singer reliefs, found " + counts[0]));
		}
		h.succeed();
	}

	/** A sculk formation turns the ground to sculk and grows dark-cored spires. */
	@GameTest(maxTicks = 20)
	public void sculkFormationGenerates(GameTestHelper h) {
		int[] counts = placeFeature(h, configured(h, "sculk_formation"), ModBlocks.SIFT_SCULK, ModBlocks.DARK_SIFT_SCULK);
		if (counts[0] + counts[1] < 20) {
			h.fail(Component.literal("too little sculk: " + counts[0] + " + " + counts[1]));
		}
		h.succeed();
	}

	private static Feature configured(GameTestHelper h, String name) {
		return h.getLevel().registryAccess().lookupOrThrow(Registries.FEATURE).getOptional(SiftUnbound.id(name)).orElseThrow();
	}

	/** Lays Sift grass, places a feature in the middle, and counts two kinds of block around it. */
	private static int[] placeFeature(GameTestHelper h, Feature feature, Block a, Block b) {
		for (int x = 0; x < 8; x++) {
			for (int z = 0; z < 8; z++) {
				h.setBlock(new BlockPos(x, 1, z), ModBlocks.SIFT_GRASS_BLOCK);
			}
		}
		ServerLevel level = h.getLevel();
		if (!feature.place(level, level.getChunkSource().getGenerator(), RandomSource.create(5), h.absolutePos(new BlockPos(4, 2, 4)))) {
			h.fail(Component.literal("the feature refused to place"));
		}
		int[] counts = new int[2];
		for (BlockPos p : BlockPos.betweenClosed(h.absolutePos(new BlockPos(-6, 0, -6)), h.absolutePos(new BlockPos(13, 18, 13)))) {
			BlockState state = level.getBlockState(p);
			counts[0] += state.is(a) ? 1 : 0;
			counts[1] += state.is(b) ? 1 : 0;
		}
		return counts;
	}

	/** Killing a Sprout enrages the sifters around it for good: +50% health, and they turn on the killer. */
	@GameTest(maxTicks = 20)
	public void sproutDeathEnragesSifters(GameTestHelper h) {
		ServerLevel level = h.getLevel();
		Sprout sprout = h.spawn(ModEntities.SPROUT, new BlockPos(4, 2, 4));
		Seedling a = h.spawn(ModEntities.SEEDLING, new BlockPos(2, 2, 2));
		Nester b = h.spawn(ModEntities.NESTER, new BlockPos(6, 2, 6));
		float before = a.getMaxHealth();
		Pig culprit = h.spawn(EntityTypes.PIG, new BlockPos(1, 2, 6));
		sprout.hurtServer(level, level.damageSources().mobAttack(culprit), 1000.0F);
		h.succeedIf(() -> {
			if (!a.isEnraged() || !b.isEnraged()) {
				h.fail(Component.literal("nearby sifters were not enraged"));
			}
			if (a.getMaxHealth() < before * 1.49F) {
				h.fail(Component.literal("enraged max health " + a.getMaxHealth() + " (was " + before + ")"));
			}
			if (a.getTarget() != culprit) {
				h.fail(Component.literal("the enraged seedling did not turn on the killer"));
			}
		});
	}

	/** A pollen glob sticks where it lands and bursts into a cloud of slowing yellow goo. */
	@GameTest(maxTicks = 100)
	public void pollenGlobBurstsIntoGoo(GameTestHelper h) {
		h.setBlock(new BlockPos(4, 1, 4), Blocks.STONE);
		PollenGlob glob = h.spawn(ModEntities.POLLEN_GLOB, new BlockPos(4, 3, 4));
		glob.setDeltaMovement(0, -0.2, 0);
		h.succeedWhen(() -> {
			boolean goo = !h.getEntities(EntityTypes.AREA_EFFECT_CLOUD, new BlockPos(4, 2, 4), 3.0).isEmpty();
			if (!goo || glob.isAlive()) {
				h.fail(Component.literal("no goo yet"));
			}
		});
	}

	/** A scavenger's soul bolt bursts into a pool of soulsickness. */
	@GameTest(maxTicks = 60)
	public void soulBoltLeavesASoulsicknessPool(GameTestHelper h) {
		h.setBlock(new BlockPos(4, 1, 4), Blocks.STONE);
		SoulBolt bolt = h.spawn(ModEntities.SOUL_BOLT, new BlockPos(4, 4, 4));
		bolt.setDeltaMovement(0, -0.4, 0);
		h.succeedWhen(() -> {
			var pools = h.getEntities(EntityTypes.AREA_EFFECT_CLOUD, new BlockPos(4, 2, 4), 3.0);
			if (pools.isEmpty() || pools.get(0).getParticle() != ParticleTypes.SCULK_SOUL) {
				h.fail(Component.literal("no soulsickness pool"));
			}
		});
	}

	/** Sculkers hunt sifters. */
	@GameTest(maxTicks = 100)
	public void sculkersHuntSifters(GameTestHelper h) {
		Scavenger scavenger = h.spawn(ModEntities.SCAVENGER, new BlockPos(1, 2, 1));
		Seedling seedling = h.spawn(ModEntities.SEEDLING, new BlockPos(6, 2, 6));
		seedling.setNoAi(true);
		h.succeedWhen(() -> {
			if (scavenger.getTarget() != seedling) {
				h.fail(Component.literal("the scavenger has not targeted the seedling"));
			}
		});
	}

	/** The Harmonizer's roar calls a band of sifters. */
	@GameTest(maxTicks = 120)
	public void harmonizerRoarSummonsSifters(GameTestHelper h) {
		Harmonizer harmonizer = h.spawn(ModEntities.HARMONIZER, new BlockPos(4, 2, 4));
		Pig target = h.spawn(EntityTypes.PIG, new BlockPos(1, 2, 1));
		target.setNoAi(true);
		harmonizer.setTarget(target);
		h.succeedWhen(() -> {
			int band = h.getLevel().getEntitiesOfClass(AbstractSifter.class, new AABB(h.absolutePos(new BlockPos(4, 2, 4))).inflate(8.0),
				s -> s != harmonizer).size();
			if (band < 3) {
				h.fail(Component.literal("only " + band + " sifters answered the roar"));
			}
		});
	}

	/** Every melee hit the Monarch lands heals it. */
	@GameTest(maxTicks = 20)
	public void monarchHealsOnMeleeHits(GameTestHelper h) {
		ServerLevel level = h.getLevel();
		Monarch monarch = h.spawn(ModEntities.MONARCH, new BlockPos(3, 2, 3));
		monarch.setNoAi(true);
		monarch.setHealth(100.0F);
		Pig pig = h.spawn(EntityTypes.PIG, new BlockPos(4, 2, 3));
		monarch.doHurtTarget(level, pig);
		h.succeedIf(() -> {
			if (monarch.getHealth() <= 100.0F) {
				h.fail(Component.literal("the Monarch did not heal (" + monarch.getHealth() + ")"));
			}
		});
	}

	/** Once the Monarch has lost a quarter of its health its Echo joins in, and the Echo fades when the Monarch dies. */
	@GameTest(maxTicks = 100)
	public void monarchEchoJoinsAndFades(GameTestHelper h) {
		ServerLevel level = h.getLevel();
		Monarch monarch = h.spawn(ModEntities.MONARCH, new BlockPos(3, 2, 3));
		Pig pig = h.spawn(EntityTypes.PIG, new BlockPos(6, 2, 6));
		pig.setNoAi(true);
		monarch.setTarget(pig);
		monarch.setHealth(monarch.getMaxHealth() * 0.6F);
		h.startSequence()
			.thenWaitUntil(() -> {
				if (h.getEntities(ModEntities.MONARCH_ECHO, new BlockPos(3, 2, 3), 8.0).isEmpty()) {
					h.fail(Component.literal("no Monarch Echo yet"));
				}
			})
			.thenExecute(() -> monarch.hurtServer(level, level.damageSources().genericKill(), Float.MAX_VALUE))
			.thenWaitUntil(() -> {
				if (!h.getEntities(ModEntities.MONARCH_ECHO, new BlockPos(3, 2, 3), 12.0).isEmpty()) {
					h.fail(Component.literal("the Echo outlived its Monarch"));
				}
			})
			.thenSucceed();
	}

	/** Humbler husk fossils and sifter nests (as configured in the datapack) generate on Sift ground. */
	@GameTest(maxTicks = 20)
	public void huskAndNestGenerate(GameTestHelper h) {
		int[] husk = placeFeature(h, configured(h, "humbler_husk"), ModBlocks.HUSK_SHELL, ModBlocks.HUSK_SHELL);
		if (husk[0] < 20) {
			h.fail(Component.literal("too little husk shell: " + husk[0]));
		}
		h.succeed();
	}

	@GameTest(maxTicks = 20)
	public void sifterNestGenerates(GameTestHelper h) {
		int[] nest = placeFeature(h, configured(h, "sifter_nest"), ModBlocks.PALLID_WOOD, ModBlocks.PALLID_LEAVES);
		if (nest[0] < 8 || nest[1] < 5) {
			h.fail(Component.literal("no proper nest: " + nest[0] + " wood, " + nest[1] + " leaves"));
		}
		h.succeed();
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

	/**
	 * Regression coverage for all twelve survival armor pieces.
	 * Previous CI exercised portal/world-gen but never checked the actual equipment components.
	 */
	@GameTest(maxTicks = 20)
	public void siftArmorPiecesHaveCorrectSlotsAndDurability(GameTestHelper h) {
		Item[][] sets = {
			{ModItems.SIFTER_HOOD, ModItems.SIFTER_ROBE, ModItems.SIFTER_LEGGINGS, ModItems.SIFTER_BOOTS},
			{ModItems.HUMBLER_ANTENNA, ModItems.HUMBLER_CARAPACE, ModItems.HUMBLER_GREAVES, ModItems.HUMBLER_TARSI},
			{ModItems.MONARCH_CROWN, ModItems.MONARCH_MANTLE, ModItems.MONARCH_SASH, ModItems.MONARCH_TALONS}
		};
		EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
		ArmorType[] types = {ArmorType.HELMET, ArmorType.CHESTPLATE, ArmorType.LEGGINGS, ArmorType.BOOTS};
		int[] durabilityBases = {16, 24, 33};
		for (int set = 0; set < sets.length; set++) {
			for (int piece = 0; piece < slots.length; piece++) {
				ItemStack stack = new ItemStack(sets[set][piece]);
				var equippable = stack.get(DataComponents.EQUIPPABLE);
				h.assertTrue(equippable != null,
					Component.literal("missing equippable component: " + stack));
				h.assertTrue(equippable.slot() == slots[piece],
					Component.literal("incorrect equipment slot on " + stack));
				h.assertValueEqual(stack.getMaxStackSize(), 1, Component.literal("armor stack size: " + stack));
				h.assertValueEqual(stack.getMaxDamage(), types[piece].getDurability(durabilityBases[set]),
					Component.literal("armor durability: " + stack));
				h.assertTrue(stack.get(DataComponents.ATTRIBUTE_MODIFIERS) != null,
					Component.literal("armor missing defense modifiers: " + stack));
				h.assertTrue(stack.get(DataComponents.REPAIRABLE) != null,
					Component.literal("armor missing repair component: " + stack));
			}
		}
		h.succeed();
	}

}
