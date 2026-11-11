package com.milkdromedastudios.siftunbound.entity.sifter;

import com.milkdromedastudios.siftunbound.registry.ModSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Nester: the tall melee sifter (Minecraft Dungeons II). It gallops at you on long legs, lunges and bites, and is quick
 * enough to sidestep one sword swing in four.
 */
public class Nester extends AbstractSifter {
	public Nester(EntityType<? extends Nester> type, Level level) {
		super(type, level);
		xpReward = 8;
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
			.add(Attributes.MAX_HEALTH, 26.0)
			.add(Attributes.ATTACK_DAMAGE, 6.0)
			.add(Attributes.ATTACK_KNOCKBACK, 0.6)
			.add(Attributes.MOVEMENT_SPEED, 0.3)
			.add(Attributes.FOLLOW_RANGE, 24.0)
			.add(Attributes.STEP_HEIGHT, 1.0);
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(2, new LeapAtTargetGoal(this, 0.45F));
		goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.35, false)); // a gallop
		super.registerGoals();
	}

	/** One melee blow in four is dodged with a quick hop to the side. */
	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		if (source.getDirectEntity() instanceof LivingEntity attacker && source.getEntity() == attacker && onGround() && random.nextInt(4) == 0) {
			Vec3 away = position().subtract(attacker.position()).normalize();
			Vec3 side = new Vec3(-away.z, 0.0, away.x).scale(random.nextBoolean() ? 0.7 : -0.7);
			setDeltaMovement(side.x + away.x * 0.2, 0.3, side.z + away.z * 0.2);
			needsSync = true;
			playSound(ModSounds.SIFTER_DODGE, 1.0F, 1.0F);
			return false;
		}
		return super.hurtServer(level, source, damage);
	}

	@Override
	public boolean doHurtTarget(ServerLevel level, Entity target) {
		level.broadcastEntityEvent(this, (byte) 4);
		return super.doHurtTarget(level, target);
	}

	/** Client-side bite animation timer. */
	public int biteTicks;

	@Override
	public void handleEntityEvent(byte id) {
		if (id == 4) {
			biteTicks = 10;
		} else {
			super.handleEntityEvent(id);
		}
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
		return ModSounds.NESTER_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return ModSounds.NESTER_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return ModSounds.NESTER_DEATH;
	}
}
