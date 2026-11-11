package com.milkdromedastudios.siftunbound.entity.sifter;

import com.milkdromedastudios.siftunbound.entity.projectile.SentinelQuill;
import com.milkdromedastudios.siftunbound.registry.ModSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Sentinel: the bearded ranged sifter (Minecraft Dungeons II). It shakes its head, then looses a storm of three homing
 * quills that swing round toward you before flying straight, and afterwards stands still for a couple of seconds,
 * open to a counterattack.
 */
public class Sentinel extends AbstractSifter implements RangedAttackMob {
	private int shake = -1;
	private int rest;
	private @Nullable LivingEntity aim;
	/** Client-side head-shake timer. */
	public int shakeTicks;

	public Sentinel(EntityType<? extends Sentinel> type, Level level) {
		super(type, level);
		xpReward = 7;
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
			.add(Attributes.MAX_HEALTH, 18.0)
			.add(Attributes.ATTACK_DAMAGE, 2.0)
			.add(Attributes.MOVEMENT_SPEED, 0.25)
			.add(Attributes.FOLLOW_RANGE, 28.0);
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(3, new RangedAttackGoal(this, 1.0, 70, 90, 16.0F) {
			@Override
			public boolean canUse() {
				return rest <= 0 && shake < 0 && super.canUse();
			}

			@Override
			public boolean canContinueToUse() {
				return rest <= 0 && super.canContinueToUse();
			}
		});
		super.registerGoals();
	}

	@Override
	public void performRangedAttack(LivingEntity target, float power) {
		if (shake < 0) {
			shake = 16;
			aim = target;
			level().broadcastEntityEvent(this, (byte) 5);
			playSound(ModSounds.SENTINEL_SHAKE, 1.0F, 1.0F);
		}
	}

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		if (rest > 0) {
			rest--;
			getNavigation().stop();
		}
		if (shake > 0) {
			shake--;
			getNavigation().stop();
		} else if (shake == 0) {
			shake = -1;
			if (aim != null && aim.isAlive()) {
				Vec3 to = aim.getEyePosition().subtract(getEyePosition());
				for (int i = -1; i <= 1; i++) { // a fan of three; they home in on their own
					SentinelQuill quill = new SentinelQuill(level, this, aim);
					Vec3 dir = to.yRot(i * 0.35F);
					quill.shoot(dir.x, dir.y + 0.15, dir.z, 0.9F, 2.0F);
					level.addFreshEntity(quill);
				}
				playSound(ModSounds.SENTINEL_QUILLS, 1.0F, 1.0F);
			}
			aim = null;
			rest = 50;
		}
	}

	@Override
	public void handleEntityEvent(byte id) {
		if (id == 5) {
			shakeTicks = 16;
		} else {
			super.handleEntityEvent(id);
		}
	}

	@Override
	public void aiStep() {
		super.aiStep();
		if (level().isClientSide() && shakeTicks > 0) {
			shakeTicks--;
		}
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return ModSounds.SENTINEL_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return ModSounds.SENTINEL_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return ModSounds.SENTINEL_DEATH;
	}
}
