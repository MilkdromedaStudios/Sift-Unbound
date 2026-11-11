package com.milkdromedastudios.siftunbound.entity.sifter;

import com.milkdromedastudios.siftunbound.registry.ModEntities;
import com.milkdromedastudios.siftunbound.registry.ModSounds;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Harmonizer: the massive sprout, a sifter mini-boss (Minecraft Dungeons II). It <b>roars</b>, a shockwave that throws
 * you back and calls a band of sifters (sentinels and nesters mostly), and binds each of them to itself with a beam
 * that strengthens them until it or they die. It slaps with a great tentacle, and conjures a <b>vortex</b>, an orb
 * under itself that drags in anyone standing on the ground (keep jumping to escape) before it slams down. Its death
 * enrages every sifter around.
 */
public class Harmonizer extends AbstractSifter {
	private static final DustParticleOptions BEAM = new DustParticleOptions(0xC07CFF, 1.1F);
	private static final int ROAR = 1, VORTEX = 2;
	private final ServerBossEvent bossEvent = new ServerBossEvent(getUUID(), getDisplayName(), BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.PROGRESS);
	private final List<AbstractSifter> summons = new ArrayList<>();
	private int action;
	private int actionTicks;
	private int roarCooldown = 20;
	private int vortexCooldown = 160;
	/** Client-side animation timers. */
	public int roarTicks;
	public int slapTicks;
	public int vortexTicks;

	public Harmonizer(EntityType<? extends Harmonizer> type, Level level) {
		super(type, level);
		xpReward = 60;
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
			.add(Attributes.MAX_HEALTH, 150.0)
			.add(Attributes.ATTACK_DAMAGE, 8.0)
			.add(Attributes.ATTACK_KNOCKBACK, 1.2)
			.add(Attributes.ARMOR, 6.0)
			.add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
			.add(Attributes.MOVEMENT_SPEED, 0.22)
			.add(Attributes.FOLLOW_RANGE, 32.0);
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(3, new MeleeAttackGoal(this, 0.9, true));
		super.registerGoals();
	}

	@Override
	public void setCustomName(@Nullable Component name) {
		super.setCustomName(name);
		bossEvent.setName(getDisplayName());
	}

	@Override
	public void startSeenByPlayer(ServerPlayer player) {
		super.startSeenByPlayer(player);
		bossEvent.addPlayer(player);
	}

	@Override
	public void stopSeenByPlayer(ServerPlayer player) {
		super.stopSeenByPlayer(player);
		bossEvent.removePlayer(player);
	}

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		bossEvent.setProgress(getHealth() / getMaxHealth());
		summons.removeIf(s -> !s.isAlive());
		bindSummons(level);
		LivingEntity target = getTarget();
		if (action != 0) {
			getNavigation().stop();
			actionTicks++;
			if (action == ROAR) {
				roar(level);
			} else {
				vortex(level);
			}
			return;
		}
		roarCooldown--;
		vortexCooldown--;
		if (target == null) {
			return;
		}
		if (roarCooldown <= 0 && summons.size() < 4) {
			begin(ROAR, (byte) 5);
			playSound(ModSounds.HARMONIZER_ROAR, 3.0F, 1.0F);
		} else if (vortexCooldown <= 0 && distanceToSqr(target) < 8 * 8) {
			begin(VORTEX, (byte) 7);
			playSound(SoundEvents.PORTAL_TRIGGER, 2.0F, 0.6F);
		}
	}

	private void begin(int what, byte event) {
		action = what;
		actionTicks = 0;
		level().broadcastEntityEvent(this, event);
	}

	/** The roar: a shockwave at its peak, and a band of sifters answers. */
	private void roar(ServerLevel level) {
		if (actionTicks == 20) {
			for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(7.0),
				e -> !(e instanceof AbstractSifter) && e.isAlive())) {
				victim.hurtServer(level, damageSources().mobAttack(this), 5.0F);
				Vec3 away = victim.position().subtract(position()).normalize();
				victim.push(away.x * 1.2, 0.4, away.z * 1.2);
				victim.needsSync = true;
			}
			level.sendParticles(ParticleTypes.EXPLOSION, getX(), getY() + 1.0, getZ(), 6, 2.5, 0.5, 2.5, 0.0);
			for (int i = 0; i < 48; i++) {
				double a = i * Math.PI * 2 / 48;
				level.sendParticles(RAGE_DUST, getX() + Math.cos(a) * 4, getY() + 0.3, getZ() + Math.sin(a) * 4, 1, 0, 0, 0, 0);
			}
			int count = 3 + random.nextInt(2);
			for (int i = 0; i < count && summons.size() < 6; i++) {
				summon(level);
			}
		}
		if (actionTicks >= 40) {
			action = 0;
			roarCooldown = 400 + random.nextInt(100);
		}
	}

	private void summon(ServerLevel level) {
		int roll = random.nextInt(20);
		EntityType<? extends AbstractSifter> type = roll < 7 ? ModEntities.SENTINEL : roll < 14 ? ModEntities.NESTER
			: roll < 18 ? ModEntities.SEEDLING : ModEntities.POLLINATOR;
		AbstractSifter sifter = type.create(level, EntitySpawnReason.MOB_SUMMONED);
		if (sifter == null) {
			return;
		}
		double a = random.nextDouble() * Math.PI * 2;
		sifter.snapTo(getX() + Math.cos(a) * 3.5, getY(), getZ() + Math.sin(a) * 3.5, random.nextFloat() * 360F, 0F);
		sifter.finalizeSpawn(level, level.getCurrentDifficultyAt(sifter.blockPosition()), EntitySpawnReason.MOB_SUMMONED, null);
		if (getTarget() != null) {
			sifter.setTarget(getTarget());
		}
		level.addFreshEntityWithPassengers(sifter);
		level.sendParticles(ParticleTypes.GLOW, sifter.getX(), sifter.getY() + 1.0, sifter.getZ(), 12, 0.3, 0.5, 0.3, 0.02);
		summons.add(sifter);
	}

	/** Beams to every sifter it summoned, strengthening them for as long as both live. */
	private void bindSummons(ServerLevel level) {
		if (tickCount % 10 == 0) {
			for (AbstractSifter s : summons) {
				for (var effect : List.of(MobEffects.REGENERATION, MobEffects.STRENGTH, MobEffects.SPEED)) {
					s.addEffect(new MobEffectInstance(effect, 30, 0, false, false), this);
				}
			}
		}
		if (tickCount % 4 == 0) {
			Vec3 from = position().add(0, getBbHeight() * 0.7, 0);
			for (AbstractSifter s : summons) {
				Vec3 to = s.position().add(0, s.getBbHeight() * 0.6, 0);
				int steps = (int) Math.ceil(from.distanceTo(to) * 1.5);
				for (int i = 0; i <= steps; i++) {
					Vec3 p = from.lerp(to, i / (double) steps);
					level.sendParticles(BEAM, p.x, p.y, p.z, 1, 0.0, 0.0, 0.0, 0.0);
				}
			}
		}
	}

	/** The vortex: three seconds of pull toward an orb at its feet (only on the ground), then the slam. */
	private void vortex(ServerLevel level) {
		Vec3 centre = position();
		if (actionTicks < 60) {
			for (int i = 0; i < 3; i++) {
				double a = (actionTicks * 0.35 + i * 2.1);
				double r = 3.0 - (actionTicks % 20) * 0.12;
				level.sendParticles(ParticleTypes.REVERSE_PORTAL, centre.x + Math.cos(a) * r, centre.y + 0.4, centre.z + Math.sin(a) * r, 1, 0, 0, 0, 0);
			}
			level.sendParticles(BEAM, centre.x, centre.y + 0.5, centre.z, 3, 0.3, 0.3, 0.3, 0.0);
			for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(9.0),
				e -> !(e instanceof AbstractSifter) && e.isAlive() && e.onGround())) {
				Vec3 pull = centre.subtract(victim.position()).multiply(1, 0, 1).normalize().scale(0.12);
				victim.push(pull.x, 0.0, pull.z);
				victim.needsSync = true;
			}
		} else if (actionTicks == 60) {
			for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(3.5),
				e -> !(e instanceof AbstractSifter) && e.isAlive())) {
				victim.hurtServer(level, damageSources().mobAttack(this), 9.0F);
			}
			level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, centre.x, centre.y + 0.5, centre.z, 1, 0, 0, 0, 0);
			level.playSound(null, getX(), getY(), getZ(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 1.2F, 0.8F);
		} else if (actionTicks >= 70) {
			action = 0;
			vortexCooldown = 280 + random.nextInt(100);
		}
	}

	@Override
	public boolean doHurtTarget(ServerLevel level, Entity target) {
		level.broadcastEntityEvent(this, (byte) 6); // the tentacle slap
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
		switch (id) {
			case 5 -> roarTicks = 40;
			case 6 -> slapTicks = 12;
			case 7 -> vortexTicks = 70;
			default -> super.handleEntityEvent(id);
		}
	}

	@Override
	public void aiStep() {
		super.aiStep();
		if (level().isClientSide()) {
			roarTicks = Math.max(0, roarTicks - 1);
			slapTicks = Math.max(0, slapTicks - 1);
			vortexTicks = Math.max(0, vortexTicks - 1);
		}
	}

	@Override
	public boolean removeWhenFarAway(double distSqr) {
		return false;
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return ModSounds.HARMONIZER_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return ModSounds.HARMONIZER_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return ModSounds.HARMONIZER_DEATH;
	}
}
