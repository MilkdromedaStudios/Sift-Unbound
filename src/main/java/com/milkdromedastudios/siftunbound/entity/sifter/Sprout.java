package com.milkdromedastudios.siftunbound.entity.sifter;

import com.milkdromedastudios.siftunbound.registry.ModSounds;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Sprout: the squid-like support sifter (Minecraft Dungeons II). It drifts above the ground on dangling tendrils and
 * glows turquoise when roused. Its <b>Life Link</b>: it draws in its tendrils, holds still, flashing through every hue,
 * and binds itself to an ally with a beam (pollinators first), which then runs faster, hits harder, shrugs off blows and
 * heals fast for as long as the link holds; hitting the sprout breaks it. With no one to link it whips with its
 * tendrils. When a sprout dies, every sifter around it is enraged.
 */
public class Sprout extends AbstractSifter {
	private static final EntityDataAccessor<Boolean> CASTING = SynchedEntityData.defineId(Sprout.class, EntityDataSerializers.BOOLEAN);
	private static final DustParticleOptions BEAM = new DustParticleOptions(0x46E8D2, 0.9F);
	private static final DustParticleOptions BOON = new DustParticleOptions(0xFFE14A, 1.0F);
	private boolean linkBroken;
	/** Client-side whip animation timer. */
	public int whipTicks;

	public Sprout(EntityType<? extends Sprout> type, Level level) {
		super(type, level);
		xpReward = 10;
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
			.add(Attributes.MAX_HEALTH, 16.0)
			.add(Attributes.ATTACK_DAMAGE, 3.0)
			.add(Attributes.MOVEMENT_SPEED, 0.24)
			.add(Attributes.FOLLOW_RANGE, 24.0);
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(2, new LifeLinkGoal(this));
		goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.0, false));
		super.registerGoals();
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(CASTING, false);
	}

	public boolean isCasting() {
		return entityData.get(CASTING);
	}

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		setAggressive(getTarget() != null);
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		if (isCasting()) {
			linkBroken = true;
		}
		return super.hurtServer(level, source, damage);
	}

	@Override
	public boolean doHurtTarget(ServerLevel level, net.minecraft.world.entity.Entity target) {
		level.broadcastEntityEvent(this, (byte) 4);
		return super.doHurtTarget(level, target);
	}

	@Override
	public void die(DamageSource source) {
		super.die(source);
		if (level() instanceof ServerLevel server) {
			enrageAround(server, this, source.getEntity() instanceof LivingEntity killer ? killer : getKillCredit());
		}
	}

	@Override
	public void handleEntityEvent(byte id) {
		if (id == 4) {
			whipTicks = 10;
		} else {
			super.handleEntityEvent(id);
		}
	}

	@Override
	public void aiStep() {
		super.aiStep();
		if (level().isClientSide()) {
			whipTicks = Math.max(0, whipTicks - 1);
			if (isCasting() && random.nextInt(2) == 0) {
				level().addParticle(ParticleTypes.GLOW, getRandomX(0.6), getY() + random.nextDouble() * 1.4, getRandomZ(0.6), 0.0, 0.03, 0.0);
			}
		}
	}

	/** Life Link: charge (30 ticks), then hold a beam to an ally for up to 8 seconds. */
	static final class LifeLinkGoal extends Goal {
		private final Sprout sprout;
		private @Nullable AbstractSifter ally;
		private int charge;
		private int link;
		private int cooldown = 40;

		LifeLinkGoal(Sprout sprout) {
			this.sprout = sprout;
			setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
		}

		@Override
		public boolean canUse() {
			if (cooldown > 0) {
				cooldown--;
				return false;
			}
			if (sprout.getTarget() == null) {
				return false;
			}
			List<AbstractSifter> allies = sprout.level().getEntitiesOfClass(AbstractSifter.class, sprout.getBoundingBox().inflate(10.0),
				s -> s != sprout && s.isAlive() && !(s instanceof Sprout) && sprout.hasLineOfSight(s));
			ally = allies.stream().min(Comparator.comparingInt((AbstractSifter s) -> s instanceof Pollinator ? 0 : 1)
				.thenComparingDouble(sprout::distanceToSqr)).orElse(null);
			return ally != null;
		}

		@Override
		public boolean canContinueToUse() {
			return ally != null && ally.isAlive() && !sprout.linkBroken && sprout.distanceToSqr(ally) < 14 * 14 && (charge > 0 || link > 0);
		}

		@Override
		public void start() {
			charge = 30;
			link = 160;
			sprout.linkBroken = false;
			sprout.entityData.set(CASTING, true);
			sprout.getNavigation().stop();
			sprout.playSound(ModSounds.SPROUT_LINK, 1.0F, 1.0F);
		}

		@Override
		public void stop() {
			sprout.entityData.set(CASTING, false);
			sprout.linkBroken = false;
			ally = null;
			cooldown = 100 + sprout.getRandom().nextInt(60);
		}

		@Override
		public boolean requiresUpdateEveryTick() {
			return true;
		}

		@Override
		public void tick() {
			if (ally == null || !(sprout.level() instanceof ServerLevel level)) {
				return;
			}
			sprout.getNavigation().stop();
			sprout.getLookControl().setLookAt(ally);
			if (charge > 0) {
				charge--;
				return;
			}
			link--;
			if (link % 10 == 0) {
				for (var effect : List.of(MobEffects.REGENERATION, MobEffects.SPEED, MobEffects.STRENGTH, MobEffects.RESISTANCE)) {
					ally.addEffect(new MobEffectInstance(effect, 30, effect == MobEffects.REGENERATION ? 2 : 0, false, false), sprout);
				}
				level.sendParticles(BOON, ally.getX(), ally.getY() + ally.getBbHeight() * 0.6, ally.getZ(), 6, 0.4, 0.4, 0.4, 0.0);
			}
			if (link % 2 == 0) { // the beam
				Vec3 from = sprout.position().add(0, sprout.getBbHeight() * 0.6, 0);
				Vec3 to = ally.position().add(0, ally.getBbHeight() * 0.6, 0);
				int steps = (int) Math.ceil(from.distanceTo(to) * 2.5);
				for (int i = 0; i <= steps; i++) {
					Vec3 p = from.lerp(to, i / (double) steps);
					level.sendParticles(BEAM, p.x, p.y, p.z, 1, 0.0, 0.0, 0.0, 0.0);
				}
			}
		}
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return ModSounds.SPROUT_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return ModSounds.SPROUT_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return ModSounds.SPROUT_DEATH;
	}
}
