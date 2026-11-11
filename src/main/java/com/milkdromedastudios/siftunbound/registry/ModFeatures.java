package com.milkdromedastudios.siftunbound.registry;

import com.milkdromedastudios.siftunbound.SiftUnbound;
import com.milkdromedastudios.siftunbound.world.feature.SculkFormationFeature;
import com.milkdromedastudios.siftunbound.world.feature.SoulshaleMonolithFeature;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;

/** The mod's own world generation feature types (configured in data/sift_unbound/worldgen/feature). */
public final class ModFeatures {
	public static void init() {
		Registry.register(BuiltInRegistries.FEATURE_TYPE, SiftUnbound.id("monolith"), SoulshaleMonolithFeature.CODEC);
		Registry.register(BuiltInRegistries.FEATURE_TYPE, SiftUnbound.id("sculk_formation"), SculkFormationFeature.CODEC);
	}

	private ModFeatures() {
	}
}
