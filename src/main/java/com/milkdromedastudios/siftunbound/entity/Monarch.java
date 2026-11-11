package com.milkdromedastudios.siftunbound.entity;

import com.milkdromedastudios.siftunbound.entity.projectile.MonarchFeather;
import com.milkdromedastudios.siftunbound.entity.sifter.AbstractSifter;
import com.milkdromedastudios.siftunbound.registry.ModEntities;
import com.milkdromedastudios.siftunbound.registry.ModSounds;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
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
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The Monarch: the huge, long-clawed sculker boss that rules the sculker nests in the Ravines (Minecraft Dungeons II).
 * It strides slowly through its arena, and every melee hit it lands heals it. Its attacks:
 * <ul>
 * <li><b>Heel stomp</b>: up close, a short twirl that hurts everything around it.</li>
 * <li><b>Summon</b>: a roar that calls two stalkers and two scavengers (not again while they live).</li>
 * <li><b>Dash strike</b>: a lunging, twirling slash; feathers form along its path and, five seconds later, fly at you.</li>
 * <li><b>Hover loop</b>: it spins at you, and eight feathers form in a circle around it, flying in after five seconds.</li>
 * <li><b>Monarch Echo</b>: once it has lost a quarter of its health a spectral clone joins the fight; it dies with the
 * Monarch, and if killed first another forms a little later.</li>
 * </ul>
 * The Monarch Echo is this same class with its own entity type: no summons and no echoes of its own.
 */
public class Monarch extends Monster {
	private static final int STOMP = 1, SUMMON = 2, DASH = 3, HOVER = 4;
	private final @Nullable ServerBossEvent bossEvent;
	private final List<Mob> summons = new ArrayList<>();
	private final List<Vec3> dashPath = new ArrayList<>();
	private @Nullable UUID echo;
	private int echoCooldown;
	private int action;
	private int actionTicks;
	private int stompCooldown = 40;
	private int summonCooldown = 60;
	private int dashCooldown = 100;
	private int hoverCooldown = 200;
	/** Client-side animation timers. */
	public int twirlTicks;
	public int roarTicks;
	public int dashTicks;

	public Monarch(EntityType<? extends Monarch> type, Level level) {
		super(type, level);
		xpReward = 150;
		bossEvent = isEcho() ? null : new ServerBossEvent(getUUID(), getDisplayName(), BossEvent.BossBarColor.YELLOW, BossEvent.BossBarOverlay.NOTCHED_10);
		setPersistenceRequired();
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
			.add(Attributes.MAX_HEALTH, 300.0)
			.add(Attributes.ATTACK_DAMAGE, 9.0)
			.add(Attributes.ATTACK_KNOCKBACK, 1.0)
			.add(Attributes.ARMOR, 10.0)
			.add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
			.add(Attributes.MOVEMENT_SPEED, 0.23)
			.add(Attributes.FOLLOW_RANGE, 40.0)
			.add(Attributes.STEP_HEIGHT, 1.5);
	}

	public static AttributeSupplier.Builder createEchoAttributes() {
		return createAttributes().add(Attributes.MAX_HEALTH, 60.0).add(Attributes.ARMOR, 4.0).add(Attributes.MOVEMENT_SPEED, 0.26);
	}

	public boolean isEcho() {
		return getType() == ModEntities.MONARCH_ECHO;
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, true));
		goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.7));
		goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 16.0F));
		goalSelector.addGoal(8, new RandomLookAroundGoal(this));
		targetSelector.addGoal(1, new HurtByTargetGoal(this, Monarch.class, SculkProwler.class, Scavenger.class));
		targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
		targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, AbstractSifter.class, true));
	}

	/** Every melee hit it lands heals it. */
	@Override
	public boolean doHurtTarget(ServerLevel level, Entity target) {
		boolean hit = super.doHurtTarget(level, target);
		if (hit) {
			heal(6.0F);
			level.sendParticles(ParticleTypes.SCULK_SOUL, getX(), getY() + getBbHeight() * 0.7, getZ(), 6, 0.4, 0.4, 0.4, 0.02);
		}
		return hit;
	}

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		if (bossEvent != null) {
			bossEvent.setProgress(getHealth() / getMaxHealth());
		}
		summons.removeIf(m -> !m.isAlive());
		LivingEntity target = getTarget();
		if (!isEcho()) {
			tendEcho(level, target);
		}
		if (action != 0) {
			actionTicks++;
			switch (action) {
				case STOMP -> stomp(level);
				case SUMMON -> summon(level);
				case DASH -> dash(level, target);
				default -> hover(level, target);
			}
			return;
		}
		stompCooldown--;
		summonCooldown--;
		dashCooldown--;
		hoverCooldown--;
		if (target == null) {
			return;
		}
		double d2 = distanceToSqr(target);
		if (!isEcho() && summonCooldown <= 0 && summons.isEmpty()) {
			begin(SUMMON, (byte) 6, ModSounds.MONARCH_ROAR);
		} else if (stompCooldown <= 0 && d2 < 3.5 * 3.5) {
			begin(STOMP, (byte) 5, null);
		} else if (dashCooldown <= 0 && d2 > 5 * 5 && d2 < 16 * 16 && hasLineOfSight(target)) {
			begin(DASH, (byte) 7, ModSounds.MONARCH_DASH);
		} else if (hoverCooldown <= 0 && d2 < 10 * 10) {
			begin(HOVER, (byte) 5, ModSounds.MONARCH_DASH);
		}
	}

	private void begin(int what, byte event, @Nullable SoundEvent sound) {
		action = what;
		actionTicks = 0;
		level().broadcastEntityEvent(this, event);
		if (sound != null) {
			playSound(sound, 2.0F, isEcho() ? 1.3F : 1.0F);
		}
	}

	private void end(int cooldownAction, int cooldown) {
		switch (cooldownAction) {
			case STOMP -> stompCooldown = cooldown;
			case SUMMON -> summonCooldown = cooldown;
			case DASH -> dashCooldown = cooldown;
			default -> hoverCooldown = cooldown;
		}
		action = 0;
	}

	private void hurtAround(ServerLevel level, double radius, float damage) {
		for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(radius),
			e -> e != this && e.isAlive() && !(e instanceof Monarch) && !(e instanceof SculkProwler) && !(e instanceof Scavenger))) {
			if (victim.hurtServer(level, damageSources().mobAttack(this), damage)) {
				Vec3 away = victim.position().subtract(position()).multiply(1, 0, 1).normalize();
				victim.push(away.x * 0.8, 0.3, away.z * 0.8);
				victim.needsSync = true;
			}
		}
	}

	/** The heel stomp: a short twirl, then everything close by is struck. */
	private void stomp(ServerLevel level) {
		getNavigation().stop();
		if (actionTicks == 8) {
			hurtAround(level, 2.5, 7.0F);
			level.sendParticles(ParticleTypes.SWEEP_ATTACK, getX(), getY() + 1.0, getZ(), 6, 1.5, 0.2, 1.5, 0.0);
		} else if (actionTicks >= 16) {
			end(STOMP, 50);
		}
	}

	/** The roar: two stalkers and two scavengers answer. */
	private void summon(ServerLevel level) {
		getNavigation().stop();
		if (actionTicks == 15) {
			for (int i = 0; i < 4; i++) {
				EntityType<? extends Mob> type = i < 2 ? ModEntities.SCULK_PROWLER : ModEntities.SCAVENGER;
				Mob mob = type.create(level, EntitySpawnReason.MOB_SUMMONED);
				if (mob == null) {
					continue;
				}
				double a = i * Math.PI / 2 + random.nextDouble() * 0.5;
				mob.snapTo(getX() + Math.cos(a) * 4, getY(), getZ() + Math.sin(a) * 4, random.nextFloat() * 360F, 0F);
				mob.finalizeSpawn(level, level.getCurrentDifficultyAt(mob.blockPosition()), EntitySpawnReason.MOB_SUMMONED, null);
				mob.setTarget(getTarget());
				level.addFreshEntityWithPassengers(mob);
				level.sendParticles(ParticleTypes.SCULK_SOUL, mob.getX(), mob.getY() + 1.0, mob.getZ(), 20, 0.4, 0.6, 0.4, 0.03);
				summons.add(mob);
			}
		} else if (actionTicks >= 30) {
			end(SUMMON, 400);
		}
	}

	/** The dash strike: a wind-up, a lunge that cuts through everything in its path, and feathers left hanging there. */
	private void dash(ServerLevel level, @Nullable LivingEntity target) {
		if (actionTicks < 10) {
			getNavigation().stop();
			if (target != null) {
				getLookControl().setLookAt(target);
			}
			if (actionTicks == 1) {
				dashPath.clear();
			}
		} else if (actionTicks == 10 && target != null) {
			Vec3 to = target.position().subtract(position()).multiply(1, 0, 1).normalize().scale(1.7);
			setDeltaMovement(to.x, 0.15, to.z);
		} else if (actionTicks < 20) {
			hurtAround(level, 0.6, 8.0F);
			if (actionTicks % 2 == 0) {
				dashPath.add(position().add(0, 1.4, 0));
			}
			level.sendParticles(ParticleTypes.SCULK_SOUL, getX(), getY() + 1.0, getZ(), 3, 0.3, 0.5, 0.3, 0.0);
		} else {
			for (Vec3 at : dashPath) {
				level.addFreshEntity(new MonarchFeather(level, this, at, 100));
			}
			dashPath.clear();
			end(DASH, 140);
		}
	}

	/** The hover loop: a rapid spin toward its target, then a ring of eight feathers. */
	private void hover(ServerLevel level, @Nullable LivingEntity target) {
		if (actionTicks <= 30) {
			setYRot(getYRot() + 40F);
			yBodyRot = getYRot();
			if (target != null) {
				Vec3 to = target.position().subtract(position()).multiply(1, 0, 1).normalize().scale(0.35);
				setDeltaMovement(to.x, getDeltaMovement().y, to.z);
			}
			if (actionTicks % 5 == 0) {
				hurtAround(level, 1.0, 5.0F);
			}
			level.sendParticles(ParticleTypes.SCULK_SOUL, getX(), getY() + 1.5, getZ(), 2, 0.8, 0.3, 0.8, 0.0);
		} else {
			for (int i = 0; i < 8; i++) {
				double a = i * Math.PI / 4;
				level.addFreshEntity(new MonarchFeather(level, this, position().add(Math.cos(a) * 3, 1.6, Math.sin(a) * 3), 100));
			}
			end(HOVER, 220);
		}
	}

	/** Once it has lost a quarter of its health, keep a Monarch Echo at its side. */
	private void tendEcho(ServerLevel level, @Nullable LivingEntity target) {
		Entity current = echo == null ? null : level.getEntity(echo);
		if (current != null && current.isAlive()) {
			return;
		}
		if (current == null && echo != null) {
			echo = null;
			echoCooldown = 300; // killed: another forms a little later
		}
		if (echoCooldown > 0) {
			echoCooldown--;
			return;
		}
		if (target == null || getHealth() > getMaxHealth() * 0.75F) {
			return;
		}
		Monarch clone = ModEntities.MONARCH_ECHO.create(level, EntitySpawnReason.MOB_SUMMONED);
		if (clone == null) {
			return;
		}
		clone.snapTo(getX() + 2.0, getY(), getZ() + 2.0, getYRot(), 0F);
		clone.setTarget(target);
		level.addFreshEntity(clone);
		level.sendParticles(ParticleTypes.SCULK_SOUL, clone.getX(), clone.getY() + 1.5, clone.getZ(), 40, 0.5, 1.0, 0.5, 0.04);
		clone.playSound(ModSounds.MONARCH_ECHO, 2.0F, 1.0F);
		echo = clone.getUUID();
	}

	@Override
	public void die(DamageSource source) {
		super.die(source);
		if (level() instanceof ServerLevel level && echo != null && level.getEntity(echo) instanceof Monarch clone && clone.isAlive()) {
			level.sendParticles(ParticleTypes.SCULK_SOUL, clone.getX(), clone.getY() + 1.5, clone.getZ(), 40, 0.5, 1.0, 0.5, 0.04);
			clone.discard(); // the echo fades with its monarch
		}
	}

	@Override
	public void setCustomName(@Nullable Component name) {
		super.setCustomName(name);
		if (bossEvent != null) {
			bossEvent.setName(getDisplayName());
		}
	}

	@Override
	public void startSeenByPlayer(ServerPlayer player) {
		super.startSeenByPlayer(player);
		if (bossEvent != null) {
			bossEvent.addPlayer(player);
		}
	}

	@Override
	public void stopSeenByPlayer(ServerPlayer player) {
		super.stopSeenByPlayer(player);
		if (bossEvent != null) {
			bossEvent.removePlayer(player);
		}
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		if (echo != null) {
			output.store("Echo", UUIDUtil.CODEC, echo);
		}
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		echo = input.read("Echo", UUIDUtil.CODEC).orElse(null);
		if (bossEvent != null) {
			bossEvent.setName(getDisplayName());
		}
	}

	@Override
	public void handleEntityEvent(byte id) {
		switch (id) {
			case 5 -> twirlTicks = 16;
			case 6 -> roarTicks = 30;
			case 7 -> dashTicks = 20;
			default -> super.handleEntityEvent(id);
		}
	}

	@Override
	public void aiStep() {
		super.aiStep();
		if (level().isClientSide()) {
			twirlTicks = Math.max(0, twirlTicks - 1);
			roarTicks = Math.max(0, roarTicks - 1);
			dashTicks = Math.max(0, dashTicks - 1);
		}
	}

	@Override
	public boolean removeWhenFarAway(double distSqr) {
		return false;
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return ModSounds.MONARCH_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return ModSounds.MONARCH_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return ModSounds.MONARCH_DEATH;
	}
}
