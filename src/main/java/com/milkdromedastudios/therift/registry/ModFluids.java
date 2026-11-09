package com.milkdromedastudios.therift.registry;

import com.milkdromedastudios.therift.TheRift;
import com.milkdromedastudios.therift.fluid.SoulWaterFluid;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.material.FlowingFluid;

public final class ModFluids {
	public static final FlowingFluid SOUL_WATER = Registry.register(BuiltInRegistries.FLUID, TheRift.id("soul_water"), new SoulWaterFluid.Source());
	public static final FlowingFluid FLOWING_SOUL_WATER = Registry.register(
		BuiltInRegistries.FLUID, TheRift.id("flowing_soul_water"), new SoulWaterFluid.Flowing()
	);

	public static void init() {
	}

	private ModFluids() {
	}
}
