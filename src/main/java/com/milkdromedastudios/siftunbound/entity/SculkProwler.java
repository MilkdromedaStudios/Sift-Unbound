package com.milkdromedastudios.siftunbound.entity;

import com.milkdromedastudios.siftunbound.entity.sifter.AbstractSifter;
import com.milkdromedastudios.siftunbound.world.SiftTides;
import java.util.EnumSet;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import com.milkdromedastudios.siftunbound.registry.ModEffects;
import com.milkdromedastudios.siftunbound.registry.ModSounds;
import com.milkdromedastudios.siftunbound.registry.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Stalker: the clawed melee sculker of the Sift (Minecraft Dungeons II), out in the dark of the Endure Tide and common in
 * the Humbler Huskland. It strikes twice in quick succession, and from a distance it lunges at its target, leaving a
 * trail of souls. Its claws make you Soulsick. Those that spawn during the Endure Tide are stronger.
 * (Registered as sift_unbound:sculk_prowler, its name before 0.3.0, so existing worlds keep their stalkers.)
 */
public class SculkProwler extends Monster {
	private int followUpSlash = -1;
	private @Nullable LivingEntity followUpTarget;

	public SculkProwler(EntityType<? extends SculkProwler> type, Level level) {
		super(type, level);
		xpReward = 8;
	}

	@Override
	public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason,
		@Nullable SpawnGroupData data) {
		SpawnGroupData result = super.finalizeSpawn(level, difficulty, reason, data);
		SiftTides.empowerIfEndure(this);
		return result;
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
			.add(Attributes.MAX_HEALTH, 30.0)
			.add(Attributes.ATTACK_DAMAGE, 5.0)
			.add(Attributes.MOVEMENT_SPEED, 0.28)
			.add(Attributes.FOLLOW_RANGE, 24.0)
			.add(Attributes.ARMOR, 2.0);
	}

	public static boolean checkProwlerSpawnRules(EntityType<SculkProwler> type, ServerLevelAccessor level, EntitySpawnReason reason, BlockPos pos, RandomSource random) {
		if (level.getDifficulty() == Difficulty.PEACEFUL) {
			return false;
		}
		return Monster.checkMonsterSpawnRules(type, level, reason, pos, random)
			|| (level.getBlockState(pos.below()).is(ModTags.SIFT_GROUND) && level.getRawBrightness(pos, 0) < 4);
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(1, new LungeGoal(this));
		goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2, false));
		goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.9));
		goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
		goalSelector.addGoal(7, new RandomLookAroundGoal(this));
		targetSelector.addGoal(1, new HurtByTargetGoal(this));
		targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
		targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, AbstractSifter.class, true)); // sculkers hunt sifters
	}

	@Override
	public boolean doHurtTarget(ServerLevel level, Entity target) {
		boolean hit = super.doHurtTarget(level, target);
		if (hit && target instanceof LivingEntity living) {
			if (!living.is(ModTags.SOUL_WATER_IMMUNE)) {
				living.addEffect(new MobEffectInstance(ModEffects.SOULSICK, 80, 0), this);
			}
			if (followUpSlash < 0) { // the second claw follows a moment later
				followUpSlash = 6;
				followUpTarget = living;
			}
		}
		return hit;
	}

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		if (followUpSlash > 0) {
			followUpSlash--;
		} else if (followUpSlash == 0) {
			followUpSlash = -2; // not a new combo from this hit
			if (followUpTarget != null && followUpTarget.isAlive() && isWithinMeleeAttackRange(followUpTarget)) {
				doHurtTarget(level, followUpTarget);
			}
			followUpTarget = null;
			followUpSlash = -1;
		}
	}

	/** From 4 to 10 blocks away, the stalker leaps at its target and leaves a trail of souls. */
	static final class LungeGoal extends Goal {
		private final SculkProwler stalker;
		private int cooldown;
		private int trail;

		LungeGoal(SculkProwler stalker) {
			this.stalker = stalker;
			setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.JUMP));
		}

		@Override
		public boolean canUse() {
			if (cooldown > 0) {
				cooldown--;
				return false;
			}
			LivingEntity target = stalker.getTarget();
			if (target == null || !stalker.onGround() || !stalker.hasLineOfSight(target)) {
				return false;
			}
			double d = stalker.distanceToSqr(target);
			return d > 16.0 && d < 100.0 && stalker.getRandom().nextInt(10) == 0;
		}

		@Override
		public boolean canContinueToUse() {
			return trail > 0;
		}

		@Override
		public void start() {
			LivingEntity target = stalker.getTarget();
			if (target == null) {
				return;
			}
			Vec3 to = target.position().subtract(stalker.position());
			Vec3 flat = new Vec3(to.x, 0.0, to.z).normalize().scale(Math.min(1.6, to.horizontalDistance() * 0.22));
			stalker.setDeltaMovement(flat.x, 0.45, flat.z);
			stalker.lookAt(target, 30.0F, 30.0F);
			stalker.playSound(ModSounds.SCULK_PROWLER_AMBIENT, 1.0F, 0.7F);
			trail = 12;
			cooldown = 80;
		}

		@Override
		public void tick() {
			trail--;
			if (stalker.level() instanceof ServerLevel server) { // the "afterimage": souls left behind mid-air
				server.sendParticles(ParticleTypes.SCULK_SOUL, stalker.getX(), stalker.getY() + 0.4, stalker.getZ(), 2, 0.15, 0.15, 0.15, 0.0);
			}
		}
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return ModSounds.SCULK_PROWLER_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return ModSounds.SCULK_PROWLER_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return ModSounds.SCULK_PROWLER_DEATH;
	}

	@Override
	protected void playStepSound(BlockPos pos, BlockState state) {
		playSound(SoundEvents.SCULK_BLOCK_STEP, 0.4F, 1.2F);
	}
}
