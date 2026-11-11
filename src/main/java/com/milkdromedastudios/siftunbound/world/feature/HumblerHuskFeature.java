package com.milkdromedastudios.siftunbound.world.feature;

import com.milkdromedastudios.siftunbound.registry.ModBlocks;
import com.milkdromedastudios.siftunbound.registry.ModTags;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;

/**
 * A humbler husk ({@code sift_unbound:humbler_husk}): the colossal fossil remains of a humbler, the giant creatures whose
 * carapaces litter the Humbler Huskland (Minecraft Dungeons II). One of three remnants, all of husk shell:
 * <ul>
 * <li>a rib cage: a spine arching over pairs of curved ribs, with a tapering tail trailing off behind;</li>
 * <li>a carapace: a great domed shell, half buried, broken open in places;</li>
 * <li>a head remnant: a hollow skull with empty eye sockets and a jaw full of teeth.</li>
 * </ul>
 *
 * @param size overall size of the remains (they stay within 16 blocks of their origin, as features must)
 */
public record HumblerHuskFeature(int size) implements Feature {
	public static final MapCodec<HumblerHuskFeature> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.intRange(6, 24).fieldOf("size").forGetter(HumblerHuskFeature::size))
		.apply(i, HumblerHuskFeature::new));

	@Override
	public MapCodec<HumblerHuskFeature> codec() {
		return CODEC;
	}

	@Override
	public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
		if (!level.getBlockState(origin.below()).is(ModTags.SIFT_GROUND) || !level.getFluidState(origin).isEmpty()) {
			return false;
		}
		Direction dir = Direction.Plane.HORIZONTAL.getRandomDirection(random);
		switch (random.nextInt(3)) {
			case 0 -> ribs(level, random, origin.below(), dir);
			case 1 -> carapace(level, random, origin.below(2));
			default -> skull(level, random, origin, dir);
		}
		return true;
	}

	private static BlockState shell(Direction.Axis axis) {
		return ModBlocks.HUSK_SHELL.defaultBlockState().setValue(RotatedPillarBlock.AXIS, axis);
	}

	private static void put(WorldGenLevel level, BlockPos p, BlockState state) {
		BlockState old = level.getBlockState(p);
		if ((old.isAir() || old.canBeReplaced() || old.is(ModTags.SIFT_GROUND)) && old.getFluidState().isEmpty()) {
			level.setBlock(p, state, 2);
		}
	}

	/** Pairs of ribs arching up to a spine, the cage tallest in the middle, and a tail curling away behind. */
	private void ribs(WorldGenLevel level, RandomSource random, BlockPos base, Direction dir) {
		Direction side = dir.getClockWise();
		int length = Math.min(size, 12); // features must stay within 16 blocks of where they start
		for (int i = 0; i <= length; i++) {
			double t = i / (double) length;
			double radius = 2.5 + 3.0 * Mth.sin((float) (t * Math.PI));
			BlockPos spine = base.relative(dir, i - length / 2).above(Mth.floor(radius * 1.15));
			put(level, spine, shell(dir.getAxis()));
			if (i % 3 != 1 || random.nextInt(6) == 0) {
				continue; // a rib every three blocks; the odd one is missing
			}
			for (int s = -1; s <= 1; s += 2) {
				for (double a = 0.15; a < Math.PI / 2; a += 0.12) {
					if (random.nextInt(14) == 0) {
						break; // broken off
					}
					int across = Mth.floor(Math.cos(a) * radius * s + 0.5);
					int up = Mth.floor(Math.sin(a) * radius * 1.15);
					put(level, base.relative(dir, i - length / 2).relative(side, across).above(up),
						shell(a > 1.0 ? side.getAxis() : Direction.Axis.Y));
				}
			}
		}
		// the tail: a curling line of shrinking vertebrae on the ground behind the cage
		double x = 0, z = 0, heading = 0, turn = (random.nextDouble() - 0.5) * 0.25;
		BlockPos tailStart = base.relative(dir.getOpposite(), length / 2 + 1);
		for (int i = 0; i < Math.min(size / 2, 8); i++) {
			heading += turn;
			x += Math.sin(heading);
			z += Math.cos(heading);
			BlockPos p = tailStart.relative(dir.getOpposite(), Mth.floor(z)).relative(side, Mth.floor(x)).above(1);
			p = SoulshaleMonolithFeature.surface(level, p);
			if (p != null && i % 2 == 0) {
				put(level, p, shell(dir.getAxis()));
				if (i < size / 4) {
					put(level, p.above(), shell(Direction.Axis.Y));
				}
			}
		}
	}

	/** A domed shell, half sunk into the ground and broken open in places. */
	private void carapace(WorldGenLevel level, RandomSource random, BlockPos centre) {
		int r = Math.max(5, Math.min(size / 2, 11));
		for (int dx = -r; dx <= r; dx++) {
			for (int dz = -r; dz <= r; dz++) {
				for (int dy = 0; dy <= r; dy++) {
					double d = Math.sqrt(dx * dx + dz * dz + dy * dy * 1.4);
					if (d > r || d < r - 1.2) {
						continue;
					}
					boolean hole = Mth.sin(dx * 0.9F + dz * 0.4F) + Mth.cos(dz * 0.8F - dy * 0.7F) > 1.25;
					if (!hole) {
						put(level, centre.offset(dx, dy, dz), shell(dy > r * 0.6 ? Direction.Axis.Y : Math.abs(dx) > Math.abs(dz) ? Direction.Axis.Z : Direction.Axis.X));
					}
				}
			}
		}
		// ridges along the top of the shell
		for (int dx = -r + 2; dx <= r - 2; dx++) {
			put(level, centre.offset(dx, Mth.floor(Math.sqrt(Math.max(0, r * r - dx * dx) / 1.4)) + 1, 0), shell(Direction.Axis.X));
		}
	}

	/** A hollow skull lying on the ground: eye sockets, a heavy brow and a jaw of teeth. */
	private void skull(WorldGenLevel level, RandomSource random, BlockPos base, Direction dir) {
		Direction side = dir.getClockWise();
		int w = 4, h = 6, l = 5;
		for (int a = -w; a <= w; a++) {
			for (int y = -1; y < h; y++) {
				for (int f = -l; f <= l; f++) {
					boolean wall = Math.abs(a) == w || y == -1 || y == h - 1 || Math.abs(f) == l;
					if (!wall) {
						continue;
					}
					boolean eye = f == l && y >= 2 && y <= 3 && (a == -2 || a == -1 || a == 1 || a == 2);
					boolean broken = random.nextInt(9) == 0 && y > 0;
					if (!eye && !broken) {
						put(level, base.relative(side, a).relative(dir, f).above(y), shell(Direction.Axis.Y));
					}
				}
			}
		}
		// the jaw: a slab of shell jutting forward with teeth along its edge
		for (int a = -w + 1; a <= w - 1; a++) {
			for (int f = l + 1; f <= l + 4; f++) {
				put(level, base.relative(side, a).relative(dir, f).below(), shell(dir.getAxis()));
			}
			if (a % 2 == 0) {
				put(level, base.relative(side, a).relative(dir, l + 4), shell(Direction.Axis.Y));
			}
		}
	}
}
