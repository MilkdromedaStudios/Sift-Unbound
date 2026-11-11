package com.milkdromedastudios.siftunbound.entity.sifter;

import com.milkdromedastudios.siftunbound.registry.ModSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.level.Level;

/**
 * Seedling: the small, quick melee sifter (Minecraft Dungeons II). It scurries at you, lunges and bites, like a baby
 * zombie; between bites it waits. Now and then it catches an arrow in its mouth and eats it.
 */
public class Seedling extends AbstractSifter {
	public Seedling(EntityType<? extends Seedling> type, Level level) {
		super(type, level);
		xpReward = 4;
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
			.add(Attributes.MAX_HEALTH, 12.0)
			.add(Attributes.ATTACK_DAMAGE, 3.0)
			.add(Attributes.MOVEMENT_SPEED, 0.33)
			.add(Attributes.FOLLOW_RANGE, 20.0);
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(2, new LeapAtTargetGoal(this, 0.35F));
		goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.15, false));
		super.registerGoals();
	}

	/** One arrow in five is caught and eaten. */
	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		if (source.getDirectEntity() instanceof AbstractArrow arrow && random.nextInt(5) == 0) {
			arrow.discard();
			playSound(SoundEvents.GENERIC_EAT.value(), 1.0F, 1.4F);
			heal(2.0F);
			level.broadcastEntityEvent(this, (byte) 4); // chomp
			return false;
		}
		return super.hurtServer(level, source, damage);
	}

	/** Client-side bite animation timer. */
	public int biteTicks;

	@Override
	public void handleEntityEvent(byte id) {
		if (id == 4) {
			biteTicks = 8;
		} else {
			super.handleEntityEvent(id);
		}
	}

	@Override
	public boolean doHurtTarget(ServerLevel level, net.minecraft.world.entity.Entity target) {
		level.broadcastEntityEvent(this, (byte) 4);
		return super.doHurtTarget(level, target);
	}

	@Override
	public void aiStep() {
		super.aiStep();
		if (level().isClientSide() && biteTicks > 0) {
			biteTicks--;
		}
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return ModSounds.SEEDLING_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return ModSounds.SEEDLING_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return ModSounds.SEEDLING_DEATH;
	}
}
