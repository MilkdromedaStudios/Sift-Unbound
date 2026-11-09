package com.milkdromedastudios.therift.entity;

import com.milkdromedastudios.therift.registry.ModSounds;
import com.milkdromedastudios.therift.registry.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ambient.AmbientCreature;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Soul Wisp: a harmless little soul drifting up out of the Sift's ravines and soul water pools.
 * Ambient like a bat - it just floats about, chimes, and leaves a trail of soul particles.
 */
public class SoulWisp extends AmbientCreature {
	private @Nullable BlockPos target;

	public SoulWisp(EntityType<? extends SoulWisp> type, Level level) {
		super(type, level);
		setNoGravity(true);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 4.0).add(Attributes.FLYING_SPEED, 0.4);
	}

	public static boolean checkWispSpawnRules(EntityType<SoulWisp> type, LevelAccessor level, EntitySpawnReason reason, BlockPos pos, RandomSource random) {
		return level.getBlockState(pos).isAir() || level.getFluidState(pos.below()).is(ModTags.SOUL_WATER);
	}

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		if (target != null && (!level.isEmptyBlock(target) || target.getY() <= level.getMinY())) {
			target = null;
		}
		if (target == null || random.nextInt(40) == 0 || target.closerToCenterThan(position(), 2.0)) {
			int ground = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, getBlockX(), getBlockZ());
			double y = Mth.clamp(getY() + random.nextInt(5) - 2, ground + 1.5, ground + 9);
			target = BlockPos.containing(getX() + random.nextInt(9) - 4, y, getZ() + random.nextInt(9) - 4);
		}
		Vec3 m = getDeltaMovement();
		double dx = target.getX() + 0.5 - getX();
		double dy = target.getY() + 0.5 - getY();
		double dz = target.getZ() + 0.5 - getZ();
		Vec3 nm = m.add((Math.signum(dx) * 0.25 - m.x) * 0.05, (Math.signum(dy) * 0.2 - m.y) * 0.05, (Math.signum(dz) * 0.25 - m.z) * 0.05);
		setDeltaMovement(nm);
		setYRot(getYRot() + Mth.wrapDegrees((float) (Mth.atan2(nm.z, nm.x) * Mth.RAD_TO_DEG) - 90.0F - getYRot()) * 0.2F);
		zza = 0.3F;
	}

	@Override
	public void aiStep() {
		super.aiStep();
		if (level().isClientSide() && random.nextInt(3) == 0) {
			level().addParticle(random.nextInt(5) == 0 ? ParticleTypes.SOUL : ParticleTypes.SCULK_SOUL,
				getRandomX(0.3), getRandomY(), getRandomZ(0.3), 0.0, 0.01, 0.0);
		}
	}

	@Override
	public boolean causeFallDamage(double fallDistance, float multiplier, DamageSource source) {
		return false;
	}

	@Override
	public boolean isPushable() {
		return false;
	}

	@Override
	public boolean canBreatheUnderwater() {
		return true;
	}

	@Override
	public int getAmbientSoundInterval() {
		return 160;
	}

	@Override
	protected @Nullable SoundEvent getAmbientSound() {
		return ModSounds.SOUL_WISP_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return ModSounds.SOUL_WISP_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return ModSounds.SOUL_WISP_DEATH;
	}

	@Override
	protected float getSoundVolume() {
		return 0.4F;
	}
}
