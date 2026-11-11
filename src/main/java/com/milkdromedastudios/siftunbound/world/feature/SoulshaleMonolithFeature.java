package com.milkdromedastudios.siftunbound.world.feature;

import com.milkdromedastudios.siftunbound.registry.ModBlocks;
import com.milkdromedastudios.siftunbound.registry.ModTags;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.IntProviders;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;

/**
 * A soulshale monolith ({@code sift_unbound:monolith}): an old carved stele of the Sift, three blocks wide and two thick,
 * standing on a polished plinth with a Chiseled Soulshale singer set into its faces. Weathered blocks and the odd
 * glowing soul shard vein show its age; some have toppled and lie broken in front of their stumps, among rubble and
 * creeping red sculk.
 *
 * @param height        blocks the stele rises above its plinth
 * @param toppledChance chance that it has fallen over
 */
public record SoulshaleMonolithFeature(IntProvider height, float toppledChance) implements Feature {
	public static final MapCodec<SoulshaleMonolithFeature> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			IntProviders.codec(3, 20).fieldOf("height").forGetter(SoulshaleMonolithFeature::height),
			Codec.floatRange(0.0F, 1.0F).optionalFieldOf("toppled_chance", 0.25F).forGetter(SoulshaleMonolithFeature::toppledChance))
		.apply(i, SoulshaleMonolithFeature::new));

	@Override
	public MapCodec<SoulshaleMonolithFeature> codec() {
		return CODEC;
	}

	@Override
	public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
		if (!level.getBlockState(origin.below()).is(ModTags.SIFT_GROUND) || !level.getFluidState(origin).isEmpty()) {
			return false;
		}
		Direction front = Direction.Plane.HORIZONTAL.getRandomDirection(random);
		Direction right = front.getClockWise();
		int h = height.sample(random);
		boolean toppled = random.nextFloat() < toppledChance;

		// the plinth: 5 wide and 4 deep, flush with the ground, on brick footings where the ground falls away
		for (int r = -2; r <= 2; r++) {
			for (int d = -2; d <= 1; d++) {
				BlockPos p = origin.relative(right, r).relative(front, d);
				boolean corner = Math.abs(r) == 2 && (d == -2 || d == 1);
				put(level, p, corner ? slab(ModBlocks.POLISHED_SOULSHALE_SLAB) : ModBlocks.POLISHED_SOULSHALE.defaultBlockState());
				footing(level, p.below());
			}
		}

		// the stele: 3 wide (r) and 2 thick (d = 0 is its front), rounded off at the top
		int standing = toppled ? 1 + random.nextInt(2) : h;
		for (int y = 1; y <= standing; y++) {
			for (int r = -1; r <= 1; r++) {
				for (int d = -1; d <= 0; d++) {
					BlockPos p = origin.above(y).relative(right, r).relative(front, d);
					if (!toppled && y == h && r != 0) {
						continue;
					}
					put(level, p, !toppled && y == h - 1 && r != 0 ? slab(ModBlocks.SOULSHALE_BRICK_SLAB) : weathered(random));
				}
			}
		}
		if (!toppled) {
			int reliefY = Math.max(2, h / 2 + 1);
			for (int d = -1; d <= 0; d++) {
				level.setBlock(origin.above(reliefY).relative(front, d), ModBlocks.CHISELED_SOULSHALE.defaultBlockState(), 2);
			}
		} else {
			fallen(level, random, origin, front, right, h - standing);
		}

		// rubble and creeping sculk around it
		for (int i = 0; i < 10; i++) {
			BlockPos p = surface(level, origin.offset(random.nextInt(9) - 4, 0, random.nextInt(9) - 4));
			if (p == null) {
				continue;
			}
			if (random.nextInt(3) == 0) {
				if (level.getBlockState(p.below()).is(ModTags.SIFT_GROUND)) {
					level.setBlock(p.below(), ModBlocks.SIFT_SCULK.defaultBlockState(), 2);
				}
			} else if (level.getBlockState(p).isAir()) {
				level.setBlock(p, random.nextBoolean() ? ModBlocks.COBBLED_SOULSHALE.defaultBlockState() : slab(ModBlocks.COBBLED_SOULSHALE_SLAB), 2);
			}
		}
		return true;
	}

	/** The fallen top of a toppled stele: lying on its back in front of the stump, broken in two, singer facing the sky. */
	private static void fallen(WorldGenLevel level, RandomSource random, BlockPos origin, Direction front, Direction right, int length) {
		int gap = 1 + random.nextInt(2);
		int breakAt = length / 2 + random.nextInt(2);
		for (int i = 0; i < length; i++) {
			int along = 2 + gap + i + (i >= breakAt ? 1 : 0);
			for (int r = -1; r <= 1; r++) {
				BlockPos column = origin.relative(front, along).relative(right, r);
				BlockPos p = surface(level, column);
				if (p == null) {
					continue;
				}
				put(level, p, weathered(random));
				boolean relief = r == 0 && i == length / 2;
				put(level, p.above(), relief ? ModBlocks.CHISELED_SOULSHALE.defaultBlockState() : weathered(random));
			}
		}
	}

	/** Bricks, worn in places to rough soulshale, with now and then a glowing vein of soul shard. */
	private static BlockState weathered(RandomSource random) {
		int roll = random.nextInt(40);
		if (roll == 0) {
			return ModBlocks.SOUL_SHARD_ORE.defaultBlockState();
		}
		return roll < 6 ? ModBlocks.COBBLED_SOULSHALE.defaultBlockState() : roll < 10 ? ModBlocks.SOULSHALE.defaultBlockState()
			: ModBlocks.SOULSHALE_BRICKS.defaultBlockState();
	}

	private static BlockState slab(net.minecraft.world.level.block.Block block) {
		return block.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM);
	}

	private static void footing(WorldGenLevel level, BlockPos top) {
		BlockPos.MutableBlockPos p = top.mutable();
		for (int i = 0; i < 6 && replaceable(level.getBlockState(p)); i++) {
			level.setBlock(p, ModBlocks.SOULSHALE_BRICKS.defaultBlockState(), 2);
			p.move(Direction.DOWN);
		}
	}

	private static void put(WorldGenLevel level, BlockPos pos, BlockState state) {
		BlockState old = level.getBlockState(pos);
		if (replaceable(old) || old.is(ModTags.SIFT_GROUND) || old.is(ModBlocks.SOULSHALE)) {
			level.setBlock(pos, state, 2);
		}
	}

	private static boolean replaceable(BlockState state) {
		return (state.isAir() || state.canBeReplaced()) && state.getFluidState().isEmpty();
	}

	/**
	 * The first open block above solid ground in this column, within 6 blocks of {@code pos}'s height, or null. (No
	 * heightmaps: they aren't kept for neighbouring chunks while features generate.)
	 */
	static BlockPos surface(WorldGenLevel level, BlockPos pos) {
		for (int dy = 6; dy >= -6; dy--) {
			BlockPos p = pos.above(dy);
			BlockState below = level.getBlockState(p.below());
			if (replaceable(level.getBlockState(p)) && !replaceable(below) && below.getFluidState().isEmpty()) {
				return p;
			}
		}
		return null;
	}
}
