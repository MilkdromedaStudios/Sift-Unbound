package com.milkdromedastudios.therift.effect;

import com.milkdromedastudios.therift.registry.ModEffects;
import com.milkdromedastudios.therift.registry.ModTags;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * Soulsick: the life (and, for players, experience) slowly drains out of you.
 * Deals 1 damage every 25 ticks at level I (faster at higher levels) and can kill, like Wither.
 */
public class SoulsickEffect extends MobEffect {
	public SoulsickEffect(MobEffectCategory category, int color) {
		super(category, color, ParticleTypes.SCULK_SOUL);
	}

	@Override
	public boolean applyEffectTick(ServerLevel level, LivingEntity mob, int amplification) {
		if (mob.is(ModTags.SOUL_WATER_IMMUNE)) {
			return false;
		}
		// falls back to magic damage if the mod's damage type is missing (e.g. a datapack override removed it)
		DamageSource source = level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).get(ModEffects.SOULSICK_DAMAGE)
			.map(DamageSource::new).orElseGet(() -> level.damageSources().magic());
		mob.hurtServer(level, source, 1.0F);
		if (mob instanceof Player player && player.totalExperience > 0) {
			player.giveExperiencePoints(-1);
		}
		return true;
	}

	@Override
	public boolean shouldApplyEffectTickThisTick(int tickCount, int amplification) {
		int interval = 25 >> amplification;
		return interval <= 0 || tickCount % interval == 0;
	}
}
