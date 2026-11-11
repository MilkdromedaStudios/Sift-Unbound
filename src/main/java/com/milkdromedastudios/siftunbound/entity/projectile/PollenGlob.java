package com.milkdromedastudios.siftunbound.entity.projectile;

import com.milkdromedastudios.siftunbound.entity.sifter.AbstractSifter;
import com.milkdromedastudios.siftunbound.registry.ModEntities;
import com.milkdromedastudios.siftunbound.registry.ModItems;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * A glob of pollen, spat by a Pollinator (or thrown by a player). It sticks where it lands and bursts three seconds after
 * it was spat, splattering a patch of sticky yellow goo that slows whoever wades through it. A direct hit bursts at once.
 */
public class PollenGlob extends ThrowableItemProjectile {
	public static final DustParticleOptions GOO = new DustParticleOptions(0xE8C83A, 1.1F);
	private int fuse = 60;

	public PollenGlob(EntityType<? extends PollenGlob> type, Level level) {
		super(type, level);
	}

	public PollenGlob(Level level, LivingEntity owner) {
		super(ModEntities.POLLEN_GLOB, owner, level, new ItemStack(ModItems.POLLEN_GLOB));
	}

	@Override
	protected Item getDefaultItem() {
		return ModItems.POLLEN_GLOB;
	}

	@Override
	public void tick() {
		super.tick();
		if (level() instanceof ServerLevel server) {
			if (--fuse <= 0 && isAlive()) {
				burst(server);
			}
		} else if (random.nextInt(3) == 0) {
			level().addParticle(ParticleTypes.FALLING_HONEY, getX(), getY(), getZ(), 0.0, 0.0, 0.0);
		}
	}

	@Override
	protected void onHitEntity(EntityHitResult hit) {
		super.onHitEntity(hit);
		if (hit.getEntity() instanceof AbstractSifter && getOwner() instanceof AbstractSifter) {
			return;
		}
		hit.getEntity().hurt(damageSources().thrown(this, getOwner()), 2.0F);
		if (level() instanceof ServerLevel server) {
			burst(server);
		}
	}

	@Override
	protected void onHitBlock(BlockHitResult hit) {
		super.onHitBlock(hit);
		Vec3 at = hit.getLocation();
		setPos(at.x, at.y, at.z);
		setDeltaMovement(Vec3.ZERO);
		setNoGravity(true); // stuck in place until it bursts
	}

	private void burst(ServerLevel level) {
		level.playSound(null, getX(), getY(), getZ(), SoundEvents.HONEY_BLOCK_BREAK, SoundSource.HOSTILE, 1.0F, 0.7F);
		level.playSound(null, getX(), getY(), getZ(), SoundEvents.SLIME_SQUISH, SoundSource.HOSTILE, 0.8F, 1.3F);
		level.sendParticles(GOO, getX(), getY() + 0.2, getZ(), 30, 0.6, 0.3, 0.6, 0.05);
		for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(1.8),
			e -> !(e instanceof AbstractSifter) && e.isAlive())) {
			victim.hurt(damageSources().thrown(this, getOwner()), 2.0F);
		}
		AreaEffectCloud goo = new AreaEffectCloud(level, getX(), getY(), getZ());
		goo.setRadius(2.2F);
		goo.setRadiusPerTick(-0.01F);
		goo.setDuration(100);
		goo.setCustomParticle(GOO);
		goo.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40, 1));
		level.addFreshEntity(goo);
		discard();
	}
}
