package com.milkdromedastudios.siftunbound.entity.sifter;

import com.milkdromedastudios.siftunbound.entity.projectile.PollenGlob;
import com.milkdromedastudios.siftunbound.registry.ModSounds;
import java.util.EnumSet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * Pollinator: the frog-like ranged sifter (Minecraft Dungeons II). It keeps its distance (though it won't back away),
 * puffs up its throat and spits a glob of pollen that bursts into sticky yellow goo; come too close and it lashes out
 * with its tongue.
 */
public class Pollinator extends AbstractSifter implements RangedAttackMob {
	private int puff = -1;
	private @Nullable LivingEntity spitAt;
	/** Client-side animation timers. */
	public int puffTicks;
	public int tongueTicks;

	public Pollinator(EntityType<? extends Pollinator> type, Level level) {
		super(type, level);
		xpReward = 7;
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
			.add(Attributes.MAX_HEALTH, 18.0)
			.add(Attributes.ATTACK_DAMAGE, 3.0)
			.add(Attributes.MOVEMENT_SPEED, 0.24)
			.add(Attributes.FOLLOW_RANGE, 24.0);
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(2, new TongueGoal(this));
		goalSelector.addGoal(3, new RangedAttackGoal(this, 1.0, 50, 70, 13.0F));
		super.registerGoals();
	}

	/** The throat puffs up first; the glob comes up a moment later. */
	@Override
	public void performRangedAttack(LivingEntity target, float power) {
		if (puff < 0) {
			puff = 14;
			spitAt = target;
			level().broadcastEntityEvent(this, (byte) 5);
			playSound(ModSounds.POLLINATOR_PUFF, 1.0F, 0.9F + random.nextFloat() * 0.2F);
		}
	}

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		if (puff > 0) {
			puff--;
		} else if (puff == 0) {
			puff = -1;
			if (spitAt != null && spitAt.isAlive()) {
				PollenGlob glob = new PollenGlob(level, this);
				double dx = spitAt.getX() - getX();
				double dz = spitAt.getZ() - getZ();
				double dy = spitAt.getY() - glob.getY();
				glob.shoot(dx, dy + Math.sqrt(dx * dx + dz * dz) * 0.2, dz, 0.85F, 5.0F);
				level.addFreshEntity(glob);
				playSound(SoundEvents.LLAMA_SPIT, 1.0F, 0.7F);
			}
			spitAt = null;
		}
	}

	@Override
	public void handleEntityEvent(byte id) {
		if (id == 5) {
			puffTicks = 18;
		} else if (id == 6) {
			tongueTicks = 8;
		} else {
			super.handleEntityEvent(id);
		}
	}

	@Override
	public void aiStep() {
		super.aiStep();
		if (level().isClientSide()) {
			puffTicks = Math.max(0, puffTicks - 1);
			tongueTicks = Math.max(0, tongueTicks - 1);
		}
	}

	/** Too close: a lash of the tongue that knocks you back. */
	static final class TongueGoal extends Goal {
		private final Pollinator frog;
		private int cooldown;

		TongueGoal(Pollinator frog) {
			this.frog = frog;
			setFlags(EnumSet.of(Flag.LOOK));
		}

		@Override
		public boolean canUse() {
			if (cooldown > 0) {
				cooldown--;
				return false;
			}
			LivingEntity target = frog.getTarget();
			return target != null && frog.distanceToSqr(target) < 3.2 * 3.2 && frog.hasLineOfSight(target);
		}

		@Override
		public boolean canContinueToUse() {
			return false;
		}

		@Override
		public void start() {
			LivingEntity target = frog.getTarget();
			if (target != null && frog.level() instanceof ServerLevel level) {
				frog.getLookControl().setLookAt(target);
				level.broadcastEntityEvent(frog, (byte) 6);
				frog.playSound(SoundEvents.FROG_TONGUE, 1.0F, 0.8F);
				if (frog.doHurtTarget(level, target)) {
					net.minecraft.world.phys.Vec3 away = target.position().subtract(frog.position()).multiply(1, 0, 1).normalize().scale(0.6);
					target.push(away.x, 0.2, away.z);
					target.needsSync = true;
				}
			}
			cooldown = 30;
		}
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return ModSounds.POLLINATOR_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return ModSounds.POLLINATOR_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return ModSounds.POLLINATOR_DEATH;
	}
}
