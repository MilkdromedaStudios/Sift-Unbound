package com.milkdromedastudios.siftunbound.portal;

import com.milkdromedastudios.siftunbound.SiftUnbound;
import com.milkdromedastudios.siftunbound.registry.ModBlocks;
import com.milkdromedastudios.siftunbound.registry.ModPoi;
import com.milkdromedastudios.siftunbound.registry.ModSounds;
import java.util.Comparator;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiRecord;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.portal.PortalShape;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** Works out where a Sift portal sends an entity, building a return portal when none exists. */
public final class SiftTravel {
	public static final ResourceKey<Level> THE_SIFT = ResourceKey.create(Registries.DIMENSION, SiftUnbound.id("the_sift"));
	private static final int SEARCH_RADIUS = 64;

	public static @Nullable TeleportTransition destination(ServerLevel from, Entity entity, BlockPos entry) {
		ResourceKey<Level> targetKey = from.dimension() == THE_SIFT ? Level.OVERWORLD : THE_SIFT;
		ServerLevel to = from.getServer().getLevel(targetKey);
		if (to == null) {
			SiftUnbound.LOGGER.warn("Dimension {} is missing; is the mod's datapack disabled?", targetKey.identifier());
			return null;
		}
		BlockPos approx = to.getWorldBorder().clampToBounds(entity.getX(), entity.getY(), entity.getZ());
		Direction.Axis axis = from.getBlockState(entry).getOptionalValue(BlockStateProperties.HORIZONTAL_AXIS).orElse(Direction.Axis.X);

		Optional<BlockPos> existing = findPortal(to, approx);
		BlockPos target;
		Direction.Axis targetAxis;
		if (existing.isPresent()) {
			target = lowestPortalBlock(to, existing.get());
			targetAxis = to.getBlockState(target).getValue(BlockStateProperties.HORIZONTAL_AXIS);
		} else {
			if (entity.isSpectator()) {
				return null;
			}
			target = createPortal(to, approx, axis).bottomLeft();
			targetAxis = axis;
		}
		Direction right = targetAxis == Direction.Axis.X ? Direction.EAST : Direction.SOUTH;
		Vec3 standAt = Vec3.atBottomCenterOf(target).add(right.getStepX() * 0.5, 0, right.getStepZ() * 0.5);
		EntityDimensions dims = entity.getDimensions(entity.getPose());
		Vec3 safe = PortalShape.findCollisionFreePosition(standAt, to, entity, dims);
		BlockPos ticketPos = target;
		return new TeleportTransition(to, safe, Vec3.ZERO, targetAxis == axis ? 0.0F : 90.0F, 0.0F, Relative.union(Relative.DELTA, Relative.ROTATION),
			e -> {
				e.placePortalTicket(ticketPos);
				e.level().playSound(null, e.getX(), e.getY(), e.getZ(), ModSounds.SIFT_PORTAL_TRAVEL, SoundSource.BLOCKS, 0.7F, 1.0F);
			});
	}

	public static Optional<BlockPos> findPortal(ServerLevel level, BlockPos near) {
		PoiManager poi = level.getPoiManager();
		poi.ensureLoadedAndValid(level, near, SEARCH_RADIUS);
		return poi.getInSquare(h -> h.is(ModPoi.SIFT_PORTAL_KEY), near, SEARCH_RADIUS, PoiManager.Occupancy.ANY)
			.map(PoiRecord::getPos)
			.filter(p -> level.getBlockState(p).is(ModBlocks.SIFT_PORTAL))
			.min(Comparator.<BlockPos>comparingDouble(p -> p.distSqr(near)).thenComparingInt(BlockPos::getY));
	}

	private static BlockPos lowestPortalBlock(ServerLevel level, BlockPos pos) {
		BlockPos p = pos;
		while (level.getBlockState(p.below()).is(ModBlocks.SIFT_PORTAL)) {
			p = p.below();
		}
		return p;
	}

	private static WardenPortal.Shape createPortal(ServerLevel level, BlockPos approx, Direction.Axis axis) {
		BlockPos base = findDryLand(level, approx).orElseGet(() -> {
			int y = surfaceY(level, approx.getX(), approx.getZ());
			return new BlockPos(approx.getX(), y, approx.getZ());
		});
		BlockState platform = level.dimension() == THE_SIFT ? ModBlocks.POLISHED_SOULSHALE.defaultBlockState() : Blocks.POLISHED_DEEPSLATE.defaultBlockState();
		SiftUnbound.LOGGER.debug("Building a Sift portal in {} at {}", level.dimension().identifier(), base);
		WardenPortal.Shape shape = WardenPortal.buildSmallPortal(level, base, axis, platform);
		if (level.dimension() == THE_SIFT) {
			SingersPortal.decorate(level, shape);
		}
		return shape;
	}

	/**
	 * Surface height read straight from the (loaded or freshly generated) chunk. Level#getHeight can't be used here:
	 * it returns the bottom of the world for chunks that aren't in the loaded-chunk map yet, which would bury the portal.
	 */
	static int surfaceY(ServerLevel level, int x, int z) {
		int y = level.getChunk(SectionPos.blockToSectionCoord(x), SectionPos.blockToSectionCoord(z))
			.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x & 15, z & 15) + 1;
		return Math.max(level.getMinY() + 6, Math.min(y, level.getMaxY() - 8));
	}

	/**
	 * Looks for solid, dry ground near {@code approx} (rings every 4 blocks out to 48) so a new return portal isn't
	 * dropped into the middle of a soul water sea or the ocean. Empty if the whole area is liquid.
	 */
	static Optional<BlockPos> findDryLand(ServerLevel level, BlockPos approx) {
		for (int r = 0; r <= 48; r += 4) {
			for (int dx = -r; dx <= r; dx += 4) {
				for (int dz = -r; dz <= r; dz += 4) {
					if (Math.max(Math.abs(dx), Math.abs(dz)) != r) {
						continue;
					}
					int x = approx.getX() + dx;
					int z = approx.getZ() + dz;
					if (!level.getWorldBorder().isWithinBounds(new BlockPos(x, 0, z))) {
						continue;
					}
					int y = surfaceY(level, x, z);
					BlockPos ground = new BlockPos(x, y - 1, z);
					BlockState g = level.getBlockState(ground);
					if (g.getFluidState().isEmpty() && g.isSolidRender() && level.getFluidState(ground.above()).isEmpty()) {
						return Optional.of(ground.above());
					}
				}
			}
		}
		return Optional.empty();
	}

	private SiftTravel() {
	}
}
