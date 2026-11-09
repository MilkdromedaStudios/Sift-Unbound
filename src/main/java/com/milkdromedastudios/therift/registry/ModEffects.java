package com.milkdromedastudios.therift.registry;

import com.milkdromedastudios.therift.TheRift;
import com.milkdromedastudios.therift.effect.SoulsickEffect;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

public final class ModEffects {
	/** Inflicted by Soul Water (and Sculk Prowler bites): slowly drains health and, for players, experience. */
	public static final Holder<MobEffect> SOULSICK = Registry.registerForHolder(
		BuiltInRegistries.MOB_EFFECT, TheRift.id("soulsick"), new SoulsickEffect(MobEffectCategory.HARMFUL, 0x4fcdd5)
	);
	public static final ResourceKey<DamageType> SOULSICK_DAMAGE = ResourceKey.create(Registries.DAMAGE_TYPE, TheRift.id("soulsick"));

	public static void init() {
	}

	private ModEffects() {
	}
}
