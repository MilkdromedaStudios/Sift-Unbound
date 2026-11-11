package com.milkdromedastudios.siftunbound.entity.projectile;

import com.milkdromedastudios.siftunbound.registry.ModEntities;
import com.milkdromedastudios.siftunbound.registry.ModItems;
import com.milkdromedastudios.siftunbound.registry.ModTags;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * A Monarch's feather. It forms where the Monarch has been (along a dash, or in a circle after a spin), hangs there for
 * five seconds, then flies straight at the nearest player.
 */
public class MonarchFeather extends ThrowableItemProjectile {
	private int delay = 100;
	private boolean launched;
	private int life;

	public MonarchFeather(EntityType<? extends MonarchFeather> type, Level level) {
		super(type, level);
	}

	public MonarchFeather(Level level, LivingEntity owner, Vec3 at, int delay) {
		super(ModEntities.MONARCH_FEATHER, at.x, at.y, at.z, level, new ItemStack(ModItems.MONARCH_FEATHER));
		setOwner(owner);
		this.delay = delay;
	}

	@Override
	protected Item getDefaultItem() {
		return ModItems.MONARCH_FEATHER;
	}

	@Override
	protected double getDefaultGravity() {
		return 0.0;
	}

	@Override
	public void tick() {
		if (level() instanceof ServerLevel server) {
			life++;
			if (!launched) {
				setDeltaMovement(Vec3.ZERO);
				if (--delay <= 0) {
					launched = true;
					Player target = server.getNearestPlayer(this, 32.0);
					if (target == null || target.isSpectator()) {
						discard();
						return;
					}
					Vec3 to = target.position().add(0, target.getBbHeight() * 0.5, 0).subtract(position());
					shoot(to.x, to.y, to.z, 1.1F, 0.5F);
				} else if (life % 5 == 0) {
					server.sendParticles(ParticleTypes.SCULK_SOUL, getX(), getY(), getZ(), 1, 0.05, 0.05, 0.05, 0.0);
				}
			} else if (life > 400) {
				discard();
				return;
			}
		}
		super.tick();
	}

	@Override
	protected boolean canHitEntity(Entity entity) {
		return launched && super.canHitEntity(entity) && !entity.is(ModTags.SCULKERS);
	}

	@Override
	protected void onHitEntity(EntityHitResult hit) {
		super.onHitEntity(hit);
		hit.getEntity().hurt(damageSources().mobProjectile(this, getOwner() instanceof LivingEntity owner ? owner : null), 4.0F);
	}

	@Override
	protected void onHit(HitResult hit) {
		if (!launched) {
			return; // hanging in the air, waiting
		}
		super.onHit(hit);
		if (level() instanceof ServerLevel server) {
			server.sendParticles(ParticleTypes.SCULK_SOUL, getX(), getY(), getZ(), 4, 0.1, 0.1, 0.1, 0.02);
			discard();
		}
	}
}
