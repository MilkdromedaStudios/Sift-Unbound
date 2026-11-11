package com.milkdromedastudios.siftunbound.entity.projectile;

import com.milkdromedastudios.siftunbound.entity.sifter.AbstractSifter;
import com.milkdromedastudios.siftunbound.registry.ModEntities;
import com.milkdromedastudios.siftunbound.registry.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
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
 * A Sentinel's homing quill. For its first half second it swings round toward its target, then it stops tracking and
 * flies straight, so it can be sidestepped once it has lined up.
 */
public class SentinelQuill extends ThrowableItemProjectile {
	private static final double SPEED = 0.9;
	private @Nullable LivingEntity target;
	private int homing = 10;
	private int life;

	public SentinelQuill(EntityType<? extends SentinelQuill> type, Level level) {
		super(type, level);
	}

	public SentinelQuill(Level level, LivingEntity owner, @Nullable LivingEntity target) {
		super(ModEntities.SENTINEL_QUILL, owner, level, new ItemStack(ModItems.SENTINEL_QUILL));
		this.target = target;
	}

	@Override
	protected Item getDefaultItem() {
		return ModItems.SENTINEL_QUILL;
	}

	@Override
	protected double getDefaultGravity() {
		return 0.0;
	}

	@Override
	public void tick() {
		if (!level().isClientSide()) {
			if (++life > 100) {
				discard();
				return;
			}
			if (homing > 0 && target != null && target.isAlive()) {
				homing--;
				Vec3 want = target.getEyePosition().subtract(position()).normalize().scale(SPEED);
				setDeltaMovement(getDeltaMovement().lerp(want, 0.3));
			}
		} else {
			level().addParticle(ParticleTypes.CRIT, getX(), getY(), getZ(), 0.0, 0.0, 0.0);
		}
		super.tick();
	}

	@Override
	protected boolean canHitEntity(Entity entity) {
		return super.canHitEntity(entity) && !(entity instanceof AbstractSifter);
	}

	@Override
	protected void onHitEntity(EntityHitResult hit) {
		super.onHitEntity(hit);
		hit.getEntity().hurt(damageSources().thrown(this, getOwner()), 3.0F);
	}

	@Override
	protected void onHit(HitResult hit) {
		super.onHit(hit);
		if (level() instanceof ServerLevel server) {
			server.sendParticles(ParticleTypes.CRIT, getX(), getY(), getZ(), 6, 0.1, 0.1, 0.1, 0.1);
			discard();
		}
	}
}
