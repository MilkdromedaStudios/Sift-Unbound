package com.milkdromedastudios.siftunbound.registry;

import com.google.common.collect.ImmutableSet;
import com.milkdromedastudios.siftunbound.SiftUnbound;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;

public final class ModPoi {
	public static final ResourceKey<PoiType> SIFT_PORTAL_KEY = ResourceKey.create(Registries.POINT_OF_INTEREST_TYPE, SiftUnbound.id("sift_portal"));
	public static PoiType SIFT_PORTAL;

	public static void init() {
		SIFT_PORTAL = PoiTypes.register(BuiltInRegistries.POINT_OF_INTEREST_TYPE, SIFT_PORTAL_KEY,
			ImmutableSet.copyOf(ModBlocks.SIFT_PORTAL.getStateDefinition().getPossibleStates()), 0, 1);
	}

	private ModPoi() {
	}
}
