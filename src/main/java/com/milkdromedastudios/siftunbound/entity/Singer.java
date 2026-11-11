package com.milkdromedastudios.siftunbound.entity;

import com.milkdromedastudios.siftunbound.portal.NoteBlockMachine;
import com.milkdromedastudios.siftunbound.registry.ModEntities;
import com.milkdromedastudios.siftunbound.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * The Singer (Minecraft Dungeons II): a healthy warden, the peaceful keeper of the Singer's Portal in Singer's Meadow.
 * It speaks only in song: now and then it sings the Sift's melody, it sings it for anyone who asks (right-click), and it
 * creates echo golems to look after the Sift. Like the blub it cannot be hurt (only /kill gets through).
 */
public class Singer extends PathfinderMob {
	private int songCooldown = 200;
	private int golemCooldown = 2400;
	/** Client-side singing animation timer. */
	public int singTicks;

	public Singer(EntityType<? extends Singer> type, Level level) {
		super(type, level);
		setPersistenceRequired();
	}

	public static AttributeSupplier.Builder createAttributes() {
		return PathfinderMob.createMobAttributes()
			.add(Attributes.MAX_HEALTH, 200.0)
			.add(Attributes.MOVEMENT_SPEED, 0.18)
			.add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(2, new MoveTowardsRestrictionGoal(this, 0.7));
		goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.5));
		goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 12.0F));
		goalSelector.addGoal(7, new RandomLookAroundGoal(this));
	}

	/** Keeps the Singer close to its portal. */
	public void setHome(BlockPos pos) {
		setHomeTo(pos, 8);
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		if (hasHome()) {
			output.store("SingerHome", BlockPos.CODEC, getHomePosition());
		}
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		input.read("SingerHome", BlockPos.CODEC).ifPresent(this::setHome);
	}

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		if (--songCooldown <= 0) {
			sing(level);
			songCooldown = 900 + random.nextInt(900);
		}
		if (--golemCooldown <= 0) {
			golemCooldown = 4800 + random.nextInt(2400);
			if (EchoGolem.near(level, blockPosition(), 24).size() < 2) {
				createGolem(level);
			}
		}
	}

	private void sing(ServerLevel level) {
		NoteBlockMachine.hum(level, position().add(0, 1.2, 0));
		level.broadcastEntityEvent(this, (byte) 4);
	}

	/** A soul gathers and an echo golem steps out of it. */
	public void createGolem(ServerLevel level) {
		EchoGolem golem = ModEntities.ECHO_GOLEM.create(level, EntitySpawnReason.MOB_SUMMONED);
		if (golem == null) {
			return;
		}
		double a = random.nextDouble() * Math.PI * 2;
		golem.snapTo(getX() + Math.cos(a) * 3, getY(), getZ() + Math.sin(a) * 3, random.nextFloat() * 360F, 0);
		level.addFreshEntity(golem);
		level.sendParticles(ParticleTypes.SCULK_SOUL, golem.getX(), golem.getY() + 1.0, golem.getZ(), 30, 0.4, 0.6, 0.4, 0.02);
		level.playSound(null, golem.blockPosition(), ModSounds.SIFT_PORTAL_ACTIVATE, net.minecraft.sounds.SoundSource.NEUTRAL, 0.5F, 1.6F);
	}

	@Override
	public InteractionResult mobInteract(Player player, InteractionHand hand) {
		if (level() instanceof ServerLevel server) {
			sing(server);
			player.sendOverlayMessage(Component.translatable("message.sift_unbound.singer", NoteBlockMachine.melodyText()));
			songCooldown = Math.max(songCooldown, 200);
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public void handleEntityEvent(byte id) {
		if (id == 4) {
			singTicks = 60;
		} else {
			super.handleEntityEvent(id);
		}
	}

	@Override
	public void aiStep() {
		super.aiStep();
		if (level().isClientSide()) {
			if (singTicks > 0) {
				singTicks--;
			}
			if (random.nextInt(40) == 0) { // the soul in its chest
				level().addParticle(ParticleTypes.SCULK_SOUL, getRandomX(0.3), getY() + 1.6, getRandomZ(0.3), 0.0, 0.02, 0.0);
			}
		}
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		return source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) && super.hurtServer(level, source, damage);
	}

	@Override
	public boolean isPushable() {
		return false;
	}

	@Override
	public boolean removeWhenFarAway(double distSqr) {
		return false;
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return ModSounds.SINGER_AMBIENT;
	}

	@Override
	public int getAmbientSoundInterval() {
		return 200;
	}
}
