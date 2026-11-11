package com.milkdromedastudios.siftunbound.world.feature;

import com.milkdromedastudios.siftunbound.registry.ModBlocks;
import com.milkdromedastudios.siftunbound.registry.ModTags;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.IntProviders;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;

/**
 * A sculk formation ({@code sift_unbound:sculk_formation}): a mound where the ground has turned to red Sift sculk,
 * darkest at its heart, with curved horn-like spires growing out of it, thick at the root and bending as they taper.
 *
 * @param spires      how many spires grow from the mound (the first is the tallest)
 * @param spireHeight height of the tallest spire
 * @param radius      radius of the sculk mound
 */
public record SculkFormationFeature(IntProvider spires, IntProvider spireHeight, int radius) implements Feature {
	public static final MapCodec<SculkFormationFeature> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			IntProviders.codec(0, 12).fieldOf("spires").forGetter(SculkFormationFeature::spires),
			IntProviders.codec(2, 24).fieldOf("spire_height").forGetter(SculkFormationFeature::spireHeight),
			Codec.intRange(2, 8).fieldOf("radius").forGetter(SculkFormationFeature::radius))
		.apply(i, SculkFormationFeature::new));

	@Override
	public MapCodec<SculkFormationFeature> codec() {
		return CODEC;
	}

	@Override
	public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
		if (!level.getBlockState(origin.below()).is(ModTags.SIFT_GROUND) || !level.getFluidState(origin).isEmpty()) {
			return false;
		}
		// the mound: ground turned to sculk, dark at the heart, a block higher in the middle
		for (int dx = -radius; dx <= radius; dx++) {
			for (int dz = -radius; dz <= radius; dz++) {
				double d = Math.sqrt(dx * dx + dz * dz) / radius + (random.nextFloat() - 0.5F) * 0.35F;
				if (d > 1.0) {
					continue;
				}
				BlockPos top = SoulshaleMonolithFeature.surface(level, origin.offset(dx, 0, dz));
				if (top == null || !level.getBlockState(top.below()).is(ModTags.SIFT_GROUND)) {
					continue;
				}
				level.setBlock(top.below(), (d < 0.45 ? ModBlocks.DARK_SIFT_SCULK : ModBlocks.SIFT_SCULK).defaultBlockState(), 2);
				if (d < 0.3) {
					level.setBlock(top, ModBlocks.SIFT_SCULK.defaultBlockState(), 2);
				}
			}
		}
		// the spires
		int count = spires.sample(random);
		int tallest = spireHeight.sample(random);
		for (int k = 0; k < count; k++) {
			double angle = random.nextDouble() * Math.PI * 2;
			double offset = k == 0 ? 0 : 1.5 + random.nextDouble() * radius * 0.5;
			BlockPos base = SoulshaleMonolithFeature.surface(level,
				origin.offset(Mth.floor(Math.cos(angle) * offset), 0, Mth.floor(Math.sin(angle) * offset)));
			if (base != null) {
				int h = k == 0 ? tallest : Math.max(3, tallest * (4 + random.nextInt(5)) / 10);
				spire(level, random, base, h);
			}
		}
		return true;
	}

	/** One spire: a tapering column that bends further off true the higher it gets, red skin over a dark core. */
	private static void spire(WorldGenLevel level, RandomSource random, BlockPos base, int h) {
		double lean = random.nextDouble() * Math.PI * 2;
		double bend = Math.min(7.0, h * (0.35 + random.nextDouble() * 0.35));
		double r0 = 1.2 + h / 8.0;
		for (int y = -2; y < h; y++) {
			double t = Math.max(0, y) / (double) h;
			double cx = base.getX() + 0.5 + Math.cos(lean) * bend * t * t;
			double cz = base.getZ() + 0.5 + Math.sin(lean) * bend * t * t;
			double r = r0 * (1.0 - t * 0.9) * (1.0 - t * 0.25) + 0.3;
			for (int bx = Mth.floor(cx - r); bx <= Mth.floor(cx + r); bx++) {
				for (int bz = Mth.floor(cz - r); bz <= Mth.floor(cz + r); bz++) {
					double dist = Math.sqrt((bx + 0.5 - cx) * (bx + 0.5 - cx) + (bz + 0.5 - cz) * (bz + 0.5 - cz));
					if (dist > r) {
						continue;
					}
					BlockPos p = new BlockPos(bx, base.getY() + y, bz);
					BlockState old = level.getBlockState(p);
					boolean open = (old.isAir() || old.canBeReplaced()) && old.getFluidState().isEmpty();
					if (open || old.is(ModTags.SIFT_GROUND)) {
						boolean core = dist < r - 1.3 || t > 0.78;
						level.setBlock(p, (core ? ModBlocks.DARK_SIFT_SCULK : ModBlocks.SIFT_SCULK).defaultBlockState(), 2);
					}
				}
			}
		}
	}
}
