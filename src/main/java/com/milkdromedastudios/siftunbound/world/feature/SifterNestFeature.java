package com.milkdromedastudios.siftunbound.world.feature;

import com.milkdromedastudios.siftunbound.registry.ModBlocks;
import com.milkdromedastudios.siftunbound.registry.ModTags;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;

/**
 * A sifter nest ({@code sift_unbound:sifter_nest}): sifters are wild creatures that live in nests (Minecraft Dungeons II).
 * A wide bowl of woven pallid branches lined with leaves, a few lullaby blooms growing in it, in the Sifter Canyons.
 */
public record SifterNestFeature() implements Feature {
	public static final SifterNestFeature INSTANCE = new SifterNestFeature();
	public static final MapCodec<SifterNestFeature> CODEC = MapCodec.unit(INSTANCE);

	@Override
	public MapCodec<SifterNestFeature> codec() {
		return CODEC;
	}

	@Override
	public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
		if (!level.getBlockState(origin.below()).is(ModTags.SIFT_GROUND) || !level.getFluidState(origin).isEmpty()) {
			return false;
		}
		int r = 3 + random.nextInt(2);
		for (int dx = -r; dx <= r; dx++) {
			for (int dz = -r; dz <= r; dz++) {
				double d = Math.sqrt(dx * dx + dz * dz);
				BlockPos column = origin.offset(dx, 0, dz);
				if (d > r + 0.4) {
					continue;
				}
				BlockPos floor = SoulshaleMonolithFeature.surface(level, column);
				if (floor == null) {
					continue;
				}
				if (d > r - 1.0) { // the woven rim, two branches high
					Direction.Axis axis = Math.abs(dx) > Math.abs(dz) ? Direction.Axis.Z : Direction.Axis.X;
					level.setBlock(floor, ModBlocks.PALLID_WOOD.defaultBlockState().setValue(RotatedPillarBlock.AXIS, axis), 2);
					if (random.nextInt(3) != 0) {
						level.setBlock(floor.above(), ModBlocks.STRIPPED_PALLID_WOOD.defaultBlockState()
							.setValue(RotatedPillarBlock.AXIS, axis == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X), 2);
					}
				} else { // a lining of leaves, and a bloom or two
					level.setBlock(floor.below(), ModBlocks.PALLID_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true), 2);
					if (random.nextInt(6) == 0) {
						level.setBlock(floor, ModBlocks.LULLABY_BLOOM.defaultBlockState(), 2);
					}
				}
			}
		}
		return true;
	}
}
