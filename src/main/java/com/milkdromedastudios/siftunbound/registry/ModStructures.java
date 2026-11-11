package com.milkdromedastudios.siftunbound.registry;

import com.milkdromedastudios.siftunbound.SiftUnbound;
import com.milkdromedastudios.siftunbound.world.structure.SculkerNest;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;

/** The mod's structure types and structure piece types (configured in data/sift_unbound/worldgen/structure). */
public final class ModStructures {
	public static final StructureType<SculkerNest> SCULKER_NEST = Registry.register(BuiltInRegistries.STRUCTURE_TYPE, SiftUnbound.id("sculker_nest"),
		() -> SculkerNest.CODEC);
	public static final StructurePieceType SCULKER_NEST_PIECE = Registry.register(BuiltInRegistries.STRUCTURE_PIECE, SiftUnbound.id("sculker_nest"),
		(StructurePieceType.ContextlessType) SculkerNest.Piece::new);

	public static void init() {
	}

	private ModStructures() {
	}
}
