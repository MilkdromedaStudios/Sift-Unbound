package com.milkdromedastudios.siftunbound.entity;

import com.milkdromedastudios.siftunbound.entity.projectile.SoulBolt;
import com.milkdromedastudios.siftunbound.entity.sifter.AbstractSifter;
import com.milkdromedastudios.siftunbound.registry.ModSounds;
import com.milkdromedastudios.siftunbound.registry.ModTags;
import com.milkdromedastudios.siftunbound.world.SiftTides;
import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Scavenger: the yellowish, magical sculker (Minecraft Dungeons II), the second largest of them, its body a cage of
 * ribs. From a distance its hands ignite and it hurls two soul bolts, one from each hand, that chase their target and
 * burst into pools of soulsickness; get close and it dashes away backwards, leaving golden afterimages. Like all
 * sculkers it hunts sifters too.
 */
public class Scavenger extends Monster implements RangedAttackMob {
	public static final DustParticleOptions AFTERIMAGE = new DustParticleOptions(0xF2C64B, 1.4F);
	private int cast = -1;
	private @Nullable LivingEntity castAt;
	/** Client-side animation timers. */
	public int castTicks;
	public int dashTicks;

	public Scavenger(EntityType<? extends Scavenger> type, Level level) {
		super(type, level);
		xpReward = 9;
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
			.add(Attributes.MAX_HEALTH, 24.0)
			.add(Attributes.ATTACK_DAMAGE, 3.0)
			.add(Attributes.MOVEMENT_SPEED, 0.27)
			.add(Attributes.FOLLOW_RANGE, 28.0)
			.add(Attributes.ARMOR, 2.0);
	}

	public static boolean checkScavengerSpawnRules(EntityType<Scavenger> type, ServerLevelAccessor level, EntitySpawnReason reason, BlockPos pos,
		RandomSource random) {
		if (level.getDifficulty() == Difficulty.PEACEFUL) {
			return false;
		}
		return Monster.checkMonsterSpawnRules(type, level, reason, pos, random)
			|| (level.getBlockState(pos.below()).is(ModTags.SIFT_GROUND) && level.getRawBrightness(pos, 0) < 4);
	}

	@Override
	public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason,
		@Nullable SpawnGroupData data) {
		SpawnGroupData result = super.finalizeSpawn(level, difficulty, reason, data);
		SiftTides.empowerIfEndure(this);
		return result;
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(1, new DashBackGoal(this));
		goalSelector.addGoal(2, new RangedAttackGoal(this, 1.0, 60, 80, 14.0F));
		goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.9));
		goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 10.0F));
		goalSelector.addGoal(7, new RandomLookAroundGoal(this));
		targetSelector.addGoal(1, new HurtByTargetGoal(this));
		targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
		targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, AbstractSifter.class, true)); // sculkers hunt sifters
	}

	/** Its hands ignite; the two bolts follow a moment apart. */
	@Override
	public void performRangedAttack(LivingEntity target, float power) {
		if (cast < 0) {
			cast = 16;
			castAt = target;
			level().broadcastEntityEvent(this, (byte) 5);
			playSound(ModSounds.SCAVENGER_CAST, 1.0F, 1.0F);
		}
	}

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		if (cast > 0) {
			cast--;
			if (cast == 8 || cast == 0) {
				throwBolt(level, cast == 8 ? 1 : -1);
			}
			if (cast == 0) {
				cast = -1;
				castAt = null;
			}
		}
	}

	private void throwBolt(ServerLevel level, int hand) {
		if (castAt == null || !castAt.isAlive()) {
			return;
		}
		SoulBolt bolt = new SoulBolt(level, this, castAt);
		Vec3 side = Vec3.directionFromRotation(0, getYRot() + 90).scale(0.6 * hand);
		bolt.setPos(getX() + side.x, getY() + getBbHeight() * 0.65, getZ() + side.z);
		Vec3 to = castAt.getEyePosition().subtract(bolt.position());
		bolt.shoot(to.x + side.x * 4, to.y + 2.0, to.z + side.z * 4, 0.45F, 4.0F); // flung out wide; they curve in
		level.addFreshEntity(bolt);
	}

	@Override
	public void handleEntityEvent(byte id) {
		if (id == 5) {
			castTicks = 20;
		} else if (id == 6) {
			dashTicks = 10;
		} else {
			super.handleEntityEvent(id);
		}
	}

	@Override
	public void aiStep() {
		super.aiStep();
		if (level().isClientSide()) {
			if (castTicks > 0) {
				castTicks--;
				Vec3 side = Vec3.directionFromRotation(0, yBodyRot + 90).scale(0.6);
				for (int hand = -1; hand <= 1; hand += 2) {
					level().addParticle(ParticleTypes.SOUL_FIRE_FLAME, getX() + side.x * hand, getY() + getBbHeight() * 0.6, getZ() + side.z * hand,
						0.0, 0.02, 0.0);
				}
			}
			if (dashTicks > 0) {
				dashTicks--;
			}
		}
	}

	/** Too close: a dash backwards, away from its target, trailing golden afterimages. */
	static final class DashBackGoal extends Goal {
		private final Scavenger scavenger;
		private int cooldown;
		private int trail;

		DashBackGoal(Scavenger scavenger) {
			this.scavenger = scavenger;
			setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP));
		}

		@Override
		public boolean canUse() {
			if (cooldown > 0) {
				cooldown--;
				return false;
			}
			LivingEntity target = scavenger.getTarget();
			return target != null && scavenger.onGround() && scavenger.distanceToSqr(target) < 4.5 * 4.5;
		}

		@Override
		public boolean canContinueToUse() {
			return trail > 0;
		}

		@Override
		public void start() {
			LivingEntity target = scavenger.getTarget();
			if (target == null) {
				return;
			}
			Vec3 away = scavenger.position().subtract(target.position()).multiply(1, 0, 1).normalize();
			scavenger.setDeltaMovement(away.x * 1.3, 0.35, away.z * 1.3);
			scavenger.needsSync = true;
			scavenger.level().broadcastEntityEvent(scavenger, (byte) 6);
			scavenger.playSound(ModSounds.SCAVENGER_DASH, 1.0F, 1.0F);
			trail = 10;
			cooldown = 70;
		}

		@Override
		public void tick() {
			trail--;
			if (scavenger.level() instanceof ServerLevel server) {
				server.sendParticles(AFTERIMAGE, scavenger.getX(), scavenger.getY() + scavenger.getBbHeight() * 0.5, scavenger.getZ(), 4,
					scavenger.getBbWidth() * 0.3, scavenger.getBbHeight() * 0.35, scavenger.getBbWidth() * 0.3, 0.0);
			}
		}
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return ModSounds.SCAVENGER_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return ModSounds.SCAVENGER_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return ModSounds.SCAVENGER_DEATH;
	}
}
