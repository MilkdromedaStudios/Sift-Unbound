package com.milkdromedastudios.siftunbound.portal;

import com.milkdromedastudios.siftunbound.entity.EchoGolem;
import com.milkdromedastudios.siftunbound.entity.Singer;
import com.milkdromedastudios.siftunbound.registry.ModBlocks;
import com.milkdromedastudios.siftunbound.registry.ModEntities;
import com.milkdromedastudios.siftunbound.registry.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The Singer's Portal (Minecraft Dungeons II): the Sift end of the Warden Portal, in Singer's Meadow, ringed by red sculk
 * and tall grass, with the Singer and its echo golems standing by. The return portal the mod builds in the Sift is
 * dressed this way the first time, and the Singer makes its home there.
 */
public final class SingersPortal {
	public static void decorate(ServerLevel level, WardenPortal.Shape shape) {
		RandomSource random = level.getRandom();
		Direction right = shape.right();
		Direction front = right.getClockWise();
		BlockPos centre = shape.bottomLeft().relative(right, shape.width() / 2);
		// red sculk and grass in a rough ring around the platform
		for (int dx = -6; dx <= 6; dx++) {
			for (int dz = -6; dz <= 6; dz++) {
				int d2 = dx * dx + dz * dz;
				if (d2 < 9 || d2 > 36 || random.nextInt(3) == 0) {
					continue;
				}
				BlockPos top = surface(level, centre.offset(dx, 0, dz));
				if (top == null) {
					continue;
				}
				BlockState ground = level.getBlockState(top.below());
				if (ground.is(ModTags.SIFT_GROUND) && !ground.is(ModBlocks.POLISHED_SOULSHALE)) {
					level.setBlock(top.below(), ModBlocks.SIFT_SCULK.defaultBlockState(), Block.UPDATE_CLIENTS);
					if (random.nextInt(3) == 0 && level.getBlockState(top).isAir() && level.getBlockState(top.above()).isAir()) {
						DoublePlantBlock.placeAt(level, ModBlocks.TALL_SIFT_GRASS.defaultBlockState(), top, Block.UPDATE_CLIENTS);
					} else if (random.nextInt(3) == 0 && level.getBlockState(top).isAir()) {
						level.setBlock(top, ModBlocks.TEAL_SPROUTS.defaultBlockState(), Block.UPDATE_CLIENTS);
					}
				}
			}
		}
		// the Singer, three blocks in front of the portal, and two echo golems
		BlockPos singerPos = surface(level, centre.relative(front, 4));
		if (singerPos == null) {
			return;
		}
		Singer singer = ModEntities.SINGER.create(level, EntitySpawnReason.STRUCTURE);
		if (singer != null) {
			singer.snapTo(singerPos.getX() + 0.5, singerPos.getY(), singerPos.getZ() + 0.5, front.toYRot() + 180F, 0F);
			singer.setHome(singerPos);
			level.addFreshEntity(singer);
		}
		for (int side : new int[]{-3, 3}) {
			BlockPos p = surface(level, centre.relative(front, 3).relative(right, side));
			EchoGolem golem = p == null ? null : ModEntities.ECHO_GOLEM.create(level, EntitySpawnReason.STRUCTURE);
			if (golem != null) {
				golem.snapTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, random.nextFloat() * 360F, 0F);
				level.addFreshEntity(golem);
			}
		}
	}

	/** The first air block above solid ground near {@code pos} (searching 6 up and 6 down), or null. */
	private static BlockPos surface(ServerLevel level, BlockPos pos) {
		for (int dy = 6; dy >= -6; dy--) {
			BlockPos p = pos.above(dy);
			if (level.getBlockState(p).isAir() && level.getBlockState(p.below()).isSolidRender()) {
				return p;
			}
		}
		return null;
	}

	private SingersPortal() {
	}
}
