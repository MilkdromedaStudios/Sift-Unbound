package com.milkdromedastudios.therift.registry;

import com.milkdromedastudios.therift.TheRift;
import com.milkdromedastudios.therift.block.SiftGrassBlock;
import com.milkdromedastudios.therift.block.SiftPlantBlock;
import com.milkdromedastudios.therift.block.SiftPortalBlock;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.ColorRGBA;
import net.minecraft.util.random.WeightedList;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.block.sounds.AmbientLeavesBlockSoundPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.FlowerBlock;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SandBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.UntintedParticleLeavesBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

public final class ModBlocks {
	/** Blocks that get a BlockItem, in creative-tab order. */
	public static final List<Block> WITH_ITEMS = new ArrayList<>();

	// ---------------------------------------------------------------- terrain
	public static final Block SIFT_GRASS_BLOCK = reg("sift_grass_block", SiftGrassBlock::new,
		BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).randomTicks().strength(0.6F).sound(SoundType.GRASS), true);
	public static final Block SIFT_SOIL = reg("sift_soil", Block::new,
		BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_PINK).strength(0.5F).sound(SoundType.ROOTED_DIRT), true);
	public static final Block SIFT_SCULK = reg("sift_sculk", Block::new,
		BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(0.6F).sound(SoundType.SCULK).lightLevel(s -> 1), true);
	public static final Block DARK_SIFT_SCULK = reg("dark_sift_sculk", Block::new,
		BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(0.6F).sound(SoundType.SCULK).lightLevel(s -> 2), true);
	public static final Block SIFT_SAND = reg("sift_sand", p -> new SandBlock(new ColorRGBA(0xFFD7D5BA), p),
		BlockBehaviour.Properties.of().mapColor(MapColor.SAND).instrument(NoteBlockInstrument.SNARE).strength(0.5F).sound(SoundType.SAND), true);
	public static final Block HUSK_SHELL = reg("husk_shell", RotatedPillarBlock::new,
		BlockBehaviour.Properties.of().mapColor(MapColor.SAND).instrument(NoteBlockInstrument.XYLOPHONE).requiresCorrectToolForDrops().strength(2.0F).sound(SoundType.BONE_BLOCK), true);

	// ---------------------------------------------------------------- soulshale (stone)
	public static final Block SOULSHALE = reg("soulshale", Block::new, stone(3.0F), true);
	public static final Block COBBLED_SOULSHALE = reg("cobbled_soulshale", Block::new, stone(3.5F), true);
	public static final Block COBBLED_SOULSHALE_STAIRS = stairs("cobbled_soulshale_stairs", COBBLED_SOULSHALE);
	public static final Block COBBLED_SOULSHALE_SLAB = reg("cobbled_soulshale_slab", SlabBlock::new, stone(3.5F), true);
	public static final Block COBBLED_SOULSHALE_WALL = reg("cobbled_soulshale_wall", WallBlock::new, stone(3.5F).forceSolidOn(), true);
	public static final Block POLISHED_SOULSHALE = reg("polished_soulshale", Block::new, stone(3.5F), true);
	public static final Block POLISHED_SOULSHALE_STAIRS = stairs("polished_soulshale_stairs", POLISHED_SOULSHALE);
	public static final Block POLISHED_SOULSHALE_SLAB = reg("polished_soulshale_slab", SlabBlock::new, stone(3.5F), true);
	public static final Block POLISHED_SOULSHALE_WALL = reg("polished_soulshale_wall", WallBlock::new, stone(3.5F).forceSolidOn(), true);
	public static final Block SOULSHALE_BRICKS = reg("soulshale_bricks", Block::new, stone(3.5F), true);
	public static final Block SOULSHALE_BRICK_STAIRS = stairs("soulshale_brick_stairs", SOULSHALE_BRICKS);
	public static final Block SOULSHALE_BRICK_SLAB = reg("soulshale_brick_slab", SlabBlock::new, stone(3.5F), true);
	public static final Block SOULSHALE_BRICK_WALL = reg("soulshale_brick_wall", WallBlock::new, stone(3.5F).forceSolidOn(), true);
	public static final Block CHISELED_SOULSHALE = reg("chiseled_soulshale", Block::new, stone(3.5F).lightLevel(s -> 3), true);
	public static final Block SOUL_SHARD_ORE = reg("soul_shard_ore", p -> new DropExperienceBlock(UniformInt.of(2, 5), p),
		stone(4.0F).lightLevel(s -> 4), true);
	public static final Block SOUL_SHARD_BLOCK = reg("soul_shard_block", Block::new,
		BlockBehaviour.Properties.of().mapColor(MapColor.DIAMOND).requiresCorrectToolForDrops().strength(3.0F, 6.0F).sound(SoundType.AMETHYST).lightLevel(s -> 10), true);

	// ---------------------------------------------------------------- plants
	public static final Block SHORT_SIFT_GRASS = reg("short_sift_grass", p -> new SiftPlantBlock(true, p), plant(MapColor.COLOR_PINK).replaceable(), true);
	public static final Block TALL_SIFT_GRASS = reg("tall_sift_grass", DoublePlantBlock::new, plant(MapColor.COLOR_PINK).replaceable(), true);
	public static final Block TEAL_SPROUTS = reg("teal_sprouts", p -> new SiftPlantBlock(false, p), plant(MapColor.COLOR_CYAN).replaceable(), true);
	public static final Block LULLABY_BLOOM = reg("lullaby_bloom", p -> new FlowerBlock(MobEffects.NIGHT_VISION, 5.0F, p),
		plant(MapColor.COLOR_LIGHT_BLUE).lightLevel(s -> 6), true);
	public static final Block POTTED_LULLABY_BLOOM = reg("potted_lullaby_bloom", p -> new FlowerPotBlock(LULLABY_BLOOM, p), Blocks.flowerPotProperties().lightLevel(s -> 6), false);

	// ---------------------------------------------------------------- pallid wood
	public static final ResourceKey<Feature> PALLID_TREE = ResourceKey.create(Registries.FEATURE, TheRift.id("pallid_tree"));
	public static final ResourceKey<Feature> TALL_PALLID_TREE = ResourceKey.create(Registries.FEATURE, TheRift.id("tall_pallid_tree"));
	public static final TreeGrower PALLID_GROWER = new TreeGrower("the_rift_pallid",
		WeightedList.<ResourceKey<Feature>>builder().add(PALLID_TREE, 4).add(TALL_PALLID_TREE, 1).build(), WeightedList.of(), WeightedList.of(), PALLID_TREE);

	public static final Block PALLID_LOG = reg("pallid_log", RotatedPillarBlock::new, wood(MapColor.COLOR_LIGHT_BLUE), true);
	public static final Block PALLID_WOOD = reg("pallid_wood", RotatedPillarBlock::new, wood(MapColor.COLOR_LIGHT_BLUE), true);
	public static final Block STRIPPED_PALLID_LOG = reg("stripped_pallid_log", RotatedPillarBlock::new, wood(MapColor.SNOW), true);
	public static final Block STRIPPED_PALLID_WOOD = reg("stripped_pallid_wood", RotatedPillarBlock::new, wood(MapColor.SNOW), true);
	public static final Block PALLID_PLANKS = reg("pallid_planks", Block::new, wood(MapColor.SNOW).strength(2.0F, 3.0F), true);
	public static final Block PALLID_LEAVES = reg("pallid_leaves",
		p -> new UntintedParticleLeavesBlock(0.06F, ParticleTypes.WHITE_ASH, AmbientLeavesBlockSoundPlayer.noAmbientSound(), p),
		BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE).strength(0.2F).randomTicks().sound(SoundType.CHERRY_LEAVES).noOcclusion()
			.isValidSpawn(Blocks::ocelotOrParrot).isSuffocating(Blocks::never).ignitedByLava()
			.pushReaction(PushReaction.POPPED).isRedstoneConductor(Blocks::never), true);
	public static final Block PALLID_SAPLING = reg("pallid_sapling", p -> new SaplingBlock(PALLID_GROWER, p),
		BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE).noCollision().randomTicks().instabreak().sound(SoundType.CHERRY_SAPLING)
			.pushReaction(PushReaction.POPPED), true);
	public static final Block POTTED_PALLID_SAPLING = reg("potted_pallid_sapling", p -> new FlowerPotBlock(PALLID_SAPLING, p), Blocks.flowerPotProperties(), false);
	public static final Block PALLID_STAIRS = stairs("pallid_stairs", PALLID_PLANKS);
	public static final Block PALLID_SLAB = reg("pallid_slab", SlabBlock::new, wood(MapColor.SNOW).strength(2.0F, 3.0F), true);
	public static final Block PALLID_FENCE = reg("pallid_fence", FenceBlock::new, wood(MapColor.SNOW).strength(2.0F, 3.0F), true);
	public static final Block PALLID_FENCE_GATE = reg("pallid_fence_gate", p -> new FenceGateBlock(WoodType.CHERRY, p),
		wood(MapColor.SNOW).strength(2.0F, 3.0F).forceSolidOn(), true);
	public static final Block PALLID_DOOR = reg("pallid_door", p -> new DoorBlock(BlockSetType.CHERRY, p),
		wood(MapColor.SNOW).strength(3.0F).noOcclusion().pushReaction(PushReaction.POPPED), true);
	public static final Block PALLID_TRAPDOOR = reg("pallid_trapdoor", p -> new TrapDoorBlock(BlockSetType.CHERRY, p),
		wood(MapColor.SNOW).strength(3.0F).noOcclusion().isValidSpawn(Blocks::never), true);
	public static final Block PALLID_BUTTON = reg("pallid_button", p -> new ButtonBlock(BlockSetType.CHERRY, 30, p),
		BlockBehaviour.Properties.of().noCollision().strength(0.5F).pushReaction(PushReaction.POPPED), true);
	public static final Block PALLID_PRESSURE_PLATE = reg("pallid_pressure_plate", p -> new PressurePlateBlock(BlockSetType.CHERRY, p),
		wood(MapColor.SNOW).forceSolidOn().noCollision().strength(0.5F).pushReaction(PushReaction.POPPED), true);

	// ---------------------------------------------------------------- soul water + portal
	public static final Block SOUL_WATER = reg("soul_water", p -> new LiquidBlock(ModFluids.SOUL_WATER, p),
		BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN).replaceable().noCollision().strength(100.0F).pushReaction(PushReaction.POPPED)
			.noLootTable().liquid().sound(SoundType.EMPTY).lightLevel(s -> 5), false);
	public static final Block SIFT_PORTAL = reg("sift_portal", SiftPortalBlock::new,
		BlockBehaviour.Properties.of().noCollision().randomTicks().strength(-1.0F).sound(SoundType.GLASS).lightLevel(s -> 12)
			.pushReaction(PushReaction.IMMOVEABLE).noLootTable(), false);

	private static BlockBehaviour.Properties stone(float hardness) {
		return BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops()
			.strength(hardness, 6.0F).sound(SoundType.DEEPSLATE);
	}

	private static BlockBehaviour.Properties wood(MapColor color) {
		return BlockBehaviour.Properties.of().mapColor(color).instrument(NoteBlockInstrument.BASS).strength(2.0F).sound(SoundType.CHERRY_WOOD).ignitedByLava();
	}

	private static BlockBehaviour.Properties plant(MapColor color) {
		return BlockBehaviour.Properties.of().mapColor(color).noCollision().instabreak().sound(SoundType.GRASS)
			.offsetType(BlockBehaviour.OffsetType.XZ).ignitedByLava().pushReaction(PushReaction.POPPED);
	}

	private static Block stairs(String name, Block base) {
		return reg(name, p -> new StairBlock(base.defaultBlockState(), p), BlockBehaviour.Properties.ofFullCopy(base), true);
	}

	private static Block reg(String name, Function<BlockBehaviour.Properties, Block> factory, BlockBehaviour.Properties properties, boolean item) {
		ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, TheRift.id(name));
		Block block = Registry.register(BuiltInRegistries.BLOCK, key, factory.apply(properties.setId(key)));
		if (item) {
			WITH_ITEMS.add(block);
		}
		return block;
	}

	public static void init() {
	}

	private ModBlocks() {
	}
}
