package com.milkdromedastudios.siftunbound.entity;

import com.milkdromedastudios.siftunbound.registry.ModEntities;
import com.milkdromedastudios.siftunbound.registry.ModSounds;
import com.milkdromedastudios.siftunbound.registry.ModTags;
import com.milkdromedastudios.siftunbound.world.SiftTides;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowParentGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * Slabber: the slow, turtle-like grazer of the Sift (Minecraft Dungeons II; its codename was "Camo"). Its flat shell is
 * a slab of mossy soulshale: when it settles down to sleep through the Endure Tide (or the night elsewhere) it tucks
 * everything in and looks just like part of the ground. Tough-shelled; bred with Teal Sprouts.
 */
public class Slabber extends Animal {
	private static final EntityDataAccessor<Boolean> SLEEPING = SynchedEntityData.defineId(Slabber.class, EntityDataSerializers.BOOLEAN);
	private int wakeTimer;

	public Slabber(EntityType<? extends Slabber> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Animal.createAnimalAttributes()
			.add(Attributes.MAX_HEALTH, 20.0)
			.add(Attributes.MOVEMENT_SPEED, 0.14)
			.add(Attributes.ARMOR, 6.0)
			.add(Attributes.KNOCKBACK_RESISTANCE, 0.6);
	}

	public static boolean checkSlabberSpawnRules(EntityType<Slabber> type, LevelAccessor level, EntitySpawnReason reason, BlockPos pos, RandomSource random) {
		return level.getBlockState(pos.below()).is(ModTags.SIFT_GROUND);
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(1, new PanicGoal(this, 1.6));
		goalSelector.addGoal(2, new BreedGoal(this, 1.0));
		goalSelector.addGoal(3, new TemptGoal(this, 1.2, s -> s.is(ModTags.SLABBER_FOOD), false));
		goalSelector.addGoal(4, new FollowParentGoal(this, 1.1));
		goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 1.0, 0.0001F));
		goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 6.0F));
		goalSelector.addGoal(7, new RandomLookAroundGoal(this));
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(SLEEPING, false);
	}

	public boolean isSlabberSleeping() {
		return entityData.get(SLEEPING);
	}

	private boolean sleepyTime() {
		Level level = level();
		return SiftTides.inSift(level) ? SiftTides.current(level) == SiftTides.Tide.ENDURE : level.isDarkOutside();
	}

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		if (wakeTimer > 0) {
			wakeTimer--;
		}
		boolean sleep = wakeTimer == 0 && sleepyTime() && onGround() && !isInWater() && !isInLove() && getLastHurtByMob() == null;
		if (sleep != isSlabberSleeping()) {
			entityData.set(SLEEPING, sleep);
		}
		if (sleep) {
			getNavigation().stop();
		}
	}

	@Override
	public boolean isImmobile() {
		return super.isImmobile() || isSlabberSleeping();
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		wakeTimer = 600; // startled awake
		entityData.set(SLEEPING, false);
		return super.hurtServer(level, source, damage);
	}

	@Override
	public boolean isFood(ItemStack stack) {
		return stack.is(ModTags.SLABBER_FOOD);
	}

	@Override
	public @Nullable Slabber getBreedOffspring(ServerLevel level, AgeableMob partner) {
		return ModEntities.SLABBER.create(level, EntitySpawnReason.BREEDING);
	}

	@Override
	protected @Nullable SoundEvent getAmbientSound() {
		return isSlabberSleeping() ? null : ModSounds.SLABBER_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return ModSounds.SLABBER_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return ModSounds.SLABBER_DEATH;
	}

	@Override
	protected void playStepSound(BlockPos pos, BlockState state) {
		playSound(SoundEvents.DEEPSLATE_STEP, 0.25F, 0.8F);
	}
}
