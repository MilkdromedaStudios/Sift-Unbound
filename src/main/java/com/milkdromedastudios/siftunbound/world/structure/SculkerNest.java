package com.milkdromedastudios.siftunbound.world.structure;

import com.milkdromedastudios.siftunbound.SiftUnbound;
import com.milkdromedastudios.siftunbound.entity.Monarch;
import com.milkdromedastudios.siftunbound.registry.ModBlocks;
import com.milkdromedastudios.siftunbound.registry.ModEntities;
import com.milkdromedastudios.siftunbound.registry.ModStructures;
import com.mojang.serialization.MapCodec;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * A sculker nest in the Ravines (Minecraft Dungeons II): a dim cavern of dark red sculk deep under the ground, its floor
 * broken by ichor pools and sculk spires, where the Monarch waits beside a chest on an obsidian plinth. A tunnel climbs
 * from the cavern to a doorway in the hillside. Found under Singer's Meadow, the Humbler Huskland and the Lullaby Hills;
 * {@code /locate structure sift_unbound:sculker_nest} finds one.
 */
public class SculkerNest extends Structure {
	public static final MapCodec<SculkerNest> CODEC = simpleCodec(SculkerNest::new);
	public static final ResourceKey<LootTable> CHEST = ResourceKey.create(Registries.LOOT_TABLE, SiftUnbound.id("chests/sculker_nest"));
	static final int RX = 15, RY = 8, DEPTH = 26;

	public SculkerNest(StructureSettings settings) {
		super(settings);
	}

	@Override
	public Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
		ChunkPos chunk = context.chunkPos();
		int x = chunk.getMiddleBlockX(), z = chunk.getMiddleBlockZ();
		int surface = context.chunkGenerator().getFirstOccupiedHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(), context.randomState());
		int cy = surface - DEPTH;
		if (cy - RY < context.heightAccessor().getMinY() + 8) {
			return Optional.empty();
		}
		Direction door = Direction.Plane.HORIZONTAL.getRandomDirection(context.random());
		// the tunnel climbs one block per block from the cavern's edge until it reaches the surface
		int run = DEPTH + 4;
		BlockPos exit = new BlockPos(x, cy, z).relative(door, RX + run).atY(cy + run - 2);
		int exitSurface = context.chunkGenerator().getFirstOccupiedHeight(exit.getX(), exit.getZ(), Heightmap.Types.WORLD_SURFACE_WG,
			context.heightAccessor(), context.randomState());
		BlockPos centre = new BlockPos(x, cy, z);
		return Optional.of(new GenerationStub(centre, builder -> builder.addPiece(new Piece(centre, door, Math.max(cy + 4, exitSurface)))));
	}

	@Override
	public StructureType<?> type() {
		return ModStructures.SCULKER_NEST;
	}

	public static class Piece extends StructurePiece {
		private final BlockPos centre;
		private final Direction door;
		private final int exitY;
		private boolean monarchPlaced;

		Piece(BlockPos centre, Direction door, int exitY) {
			super(ModStructures.SCULKER_NEST_PIECE, 0, box(centre, door, exitY));
			this.centre = centre;
			this.door = door;
			this.exitY = exitY;
		}

		public Piece(CompoundTag tag) {
			super(ModStructures.SCULKER_NEST_PIECE, tag);
			centre = new BlockPos(tag.getIntOr("CX", 0), tag.getIntOr("CY", 0), tag.getIntOr("CZ", 0));
			door = Direction.from2DDataValue(tag.getIntOr("Door", 0));
			exitY = tag.getIntOr("ExitY", centre.getY() + 20);
			monarchPlaced = tag.getBooleanOr("Monarch", false);
		}

		private static BoundingBox box(BlockPos c, Direction door, int exitY) {
			int reach = RX + (exitY - c.getY()) + 4;
			BlockPos far = c.relative(door, reach);
			return BoundingBox.fromCorners(c.offset(-RX - 2, -RY - 2, -RX - 2), c.offset(RX + 2, RY + 2, RX + 2))
				.encapsulate(BoundingBox.fromCorners(far.offset(-3, 0, -3), far.atY(exitY + 6).offset(3, 0, 3)));
		}

		@Override
		protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
			tag.putInt("CX", centre.getX());
			tag.putInt("CY", centre.getY());
			tag.putInt("CZ", centre.getZ());
			tag.putInt("Door", door.get2DDataValue());
			tag.putInt("ExitY", exitY);
			tag.putBoolean("Monarch", monarchPlaced);
		}

		@Override
		public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random, BoundingBox chunkBox,
			ChunkPos chunkPos, BlockPos reference) {
			BlockState air = Blocks.CAVE_AIR.defaultBlockState();
			BlockState sculk = ModBlocks.SIFT_SCULK.defaultBlockState();
			BlockState dark = ModBlocks.DARK_SIFT_SCULK.defaultBlockState();
			// the cavern: a flattened dome, sculk-walled, with a dark floor
			for (int dx = -RX - 2; dx <= RX + 2; dx++) {
				for (int dz = -RX - 2; dz <= RX + 2; dz++) {
					for (int dy = -RY - 2; dy <= RY + 2; dy++) {
						BlockPos p = centre.offset(dx, dy, dz);
						if (!chunkBox.isInside(p)) {
							continue;
						}
						double wobble = Mth.sin(dx * 0.7F) * 0.06 + Mth.cos(dz * 0.6F) * 0.06;
						double e = (dx * dx + dz * dz) / (double) (RX * RX) + (dy > 0 ? dy * dy / (double) (RY * RY) : dy * dy / 9.0) + wobble;
						if (dy >= -3 && e < 1.0) {
							level.setBlock(p, air, 2);
						} else if (e < 1.35 || (dy < -3 && dy >= -5 && dx * dx + dz * dz < RX * RX)) {
							level.setBlock(p, hash(p) % 5 == 0 ? dark : dy <= -3 ? dark : sculk, 2);
						}
					}
				}
			}
			// ichor pools and sculk spires on the floor, and a few glowing veins in the walls
			RandomSource layout = RandomSource.create(centre.asLong());
			for (int i = 0; i < 4; i++) {
				BlockPos pool = centre.offset(layout.nextInt(17) - 8, -4, layout.nextInt(17) - 8);
				for (int dx = -2; dx <= 2; dx++) {
					for (int dz = -2; dz <= 2; dz++) {
						BlockPos p = pool.offset(dx, 0, dz);
						if (dx * dx + dz * dz <= 4 && chunkBox.isInside(p) && p.distSqr(centre.below(4)) > 16) {
							level.setBlock(p, ModBlocks.SOUL_WATER.defaultBlockState(), 2);
						}
					}
				}
			}
			for (int i = 0; i < 7; i++) {
				double a = layout.nextDouble() * Math.PI * 2;
				double r = 6 + layout.nextDouble() * 7;
				BlockPos base = centre.offset(Mth.floor(Math.cos(a) * r), -3, Mth.floor(Math.sin(a) * r));
				int h = 3 + layout.nextInt(6);
				for (int y = 0; y < h; y++) {
					BlockPos p = base.above(y);
					if (chunkBox.isInside(p)) {
						level.setBlock(p, y > h - 3 ? dark : sculk, 2);
					}
				}
			}
			for (int i = 0; i < 24; i++) {
				BlockPos p = centre.offset(layout.nextInt(2 * RX + 1) - RX, layout.nextInt(RY + 4) - 3, layout.nextInt(2 * RX + 1) - RX);
				if (chunkBox.isInside(p) && level.getBlockState(p).is(ModBlocks.SIFT_SCULK)) {
					level.setBlock(p, ModBlocks.SOUL_SHARD_ORE.defaultBlockState(), 2);
				}
			}
			// the reward: a chest on an obsidian plinth at the far side from the door
			BlockPos plinth = centre.relative(door.getOpposite(), RX - 4).below(3);
			for (int dx = -1; dx <= 1; dx++) {
				for (int dz = -1; dz <= 1; dz++) {
					placeIn(level, chunkBox, plinth.offset(dx, 0, dz), Blocks.OBSIDIAN.defaultBlockState());
				}
			}
			if (chunkBox.isInside(plinth.above())) {
				createChest(level, chunkBox, random, plinth.above(), CHEST, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, door));
			}
			tunnel(level, chunkBox, air, sculk);
			// the Monarch, once
			if (!monarchPlaced && chunkBox.isInside(centre)) {
				Monarch monarch = ModEntities.MONARCH.create(level.getLevel(), EntitySpawnReason.STRUCTURE);
				if (monarch != null) {
					monarch.snapTo(centre.getX() + 0.5, centre.getY() - 3, centre.getZ() + 0.5, door.toYRot(), 0F);
					monarch.finalizeSpawn(level, level.getCurrentDifficultyAt(centre), EntitySpawnReason.STRUCTURE, null);
					level.addFreshEntityWithPassengers(monarch);
					monarchPlaced = true;
				}
			}
		}

		/** A 3-wide, 4-high tunnel climbing from the cavern to a soulshale doorway on the surface. */
		private void tunnel(WorldGenLevel level, BoundingBox chunkBox, BlockState air, BlockState sculk) {
			Direction side = door.getClockWise();
			int steps = exitY - (centre.getY() - 3);
			for (int i = 0; i <= steps + 2; i++) {
				BlockPos floor = centre.relative(door, RX - 3 + i).atY(centre.getY() - 3 + Math.min(i, steps));
				for (int s = -2; s <= 2; s++) {
					for (int y = -1; y <= 5; y++) {
						BlockPos p = floor.relative(side, s).above(y);
						if (!chunkBox.isInside(p)) {
							continue;
						}
						boolean inside = Math.abs(s) <= 1 && y >= 0 && y <= 3;
						if (inside) {
							level.setBlock(p, air, 2);
						} else if (!level.getBlockState(p).isAir() || y == -1) {
							level.setBlock(p, sculk, 2);
						}
					}
				}
			}
			// the doorway: a polished soulshale frame at the top of the climb
			BlockPos top = centre.relative(door, RX - 3 + steps + 1).atY(exitY);
			for (int s = -2; s <= 2; s++) {
				for (int y = 0; y <= 4; y++) {
					boolean frame = Math.abs(s) == 2 || y == 4;
					if (frame) {
						placeIn(level, chunkBox, top.relative(side, s).above(y), y == 4 && s == 0
							? ModBlocks.CHISELED_SOULSHALE.defaultBlockState() : ModBlocks.POLISHED_SOULSHALE.defaultBlockState());
					}
				}
			}
		}

		private static void placeIn(WorldGenLevel level, BoundingBox chunkBox, BlockPos p, BlockState state) {
			if (chunkBox.isInside(p)) {
				level.setBlock(p, state, 2);
			}
		}

		private static int hash(BlockPos p) {
			return Math.floorMod(p.getX() * 73856093 ^ p.getY() * 19349663 ^ p.getZ() * 83492791, 1000);
		}
	}
}
