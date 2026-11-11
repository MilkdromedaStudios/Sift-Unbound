package com.milkdromedastudios.siftunbound.entity.projectile;

import com.milkdromedastudios.siftunbound.registry.ModEffects;
import com.milkdromedastudios.siftunbound.registry.ModEntities;
import com.milkdromedastudios.siftunbound.registry.ModItems;
import com.milkdromedastudios.siftunbound.registry.ModTags;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * A Scavenger's soul bolt: it locks on and chases its target (turning only so fast, so it can be outrun or dodged round
 * a corner), then bursts into a pool of soulsickness that lingers on the ground.
 */
public class SoulBolt extends ThrowableItemProjectile {
	private static final double SPEED = 0.45;
	private @Nullable LivingEntity target;
	private int life;

	public SoulBolt(EntityType<? extends SoulBolt> type, Level level) {
		super(type, level);
	}

	public SoulBolt(Level level, LivingEntity owner, @Nullable LivingEntity target) {
		super(ModEntities.SOUL_BOLT, owner, level, new ItemStack(ModItems.SOUL_BOLT));
		this.target = target;
	}

	@Override
	protected Item getDefaultItem() {
		return ModItems.SOUL_BOLT;
	}

	@Override
	protected double getDefaultGravity() {
		return 0.0;
	}

	@Override
	public void tick() {
		if (level() instanceof ServerLevel server) {
			if (++life > 90) {
				burst(server);
				return;
			}
			if (life > 6 && target != null && target.isAlive()) {
				Vec3 want = target.position().add(0, target.getBbHeight() * 0.5, 0).subtract(position()).normalize().scale(SPEED);
				setDeltaMovement(getDeltaMovement().lerp(want, 0.12));
			}
		} else {
			level().addParticle(ParticleTypes.SOUL_FIRE_FLAME, getX(), getY(), getZ(), 0.0, 0.01, 0.0);
			if (random.nextInt(3) == 0) {
				level().addParticle(ParticleTypes.SCULK_SOUL, getX(), getY(), getZ(), 0.0, 0.02, 0.0);
			}
		}
		super.tick();
	}

	@Override
	protected boolean canHitEntity(Entity entity) {
		return super.canHitEntity(entity) && !entity.is(ModTags.SCULKERS);
	}

	@Override
	protected void onHitEntity(EntityHitResult hit) {
		super.onHitEntity(hit);
		hit.getEntity().hurt(damageSources().mobProjectile(this, getOwner() instanceof LivingEntity owner ? owner : null), 3.0F);
	}

	@Override
	protected void onHit(HitResult hit) {
		super.onHit(hit);
		if (level() instanceof ServerLevel server && isAlive()) {
			burst(server);
		}
	}

	private void burst(ServerLevel level) {
		level.playSound(null, getX(), getY(), getZ(), SoundEvents.SOUL_ESCAPE.value(), SoundSource.HOSTILE, 1.5F, 0.8F);
		level.sendParticles(ParticleTypes.SCULK_SOUL, getX(), getY(), getZ(), 16, 0.4, 0.3, 0.4, 0.05);
		AreaEffectCloud pool = new AreaEffectCloud(level, getX(), getY() - 0.4, getZ());
		pool.setRadius(2.0F);
		pool.setRadiusPerTick(-0.008F);
		pool.setDuration(120);
		pool.setCustomParticle(ParticleTypes.SCULK_SOUL);
		pool.addEffect(new MobEffectInstance(ModEffects.SOULSICK, 60, 0));
		level.addFreshEntity(pool);
		discard();
	}
}
