package com.milkdromedastudios.therift.entity;

import com.milkdromedastudios.therift.registry.ModBlocks;
import com.milkdromedastudios.therift.registry.ModEntities;
import com.milkdromedastudios.therift.registry.ModItems;
import com.milkdromedastudios.therift.registry.ModSounds;
import com.milkdromedastudios.therift.registry.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowParentGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.Bucketable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * Blub: a small, squishy, rabbit-like creature that hops around the Sift and squeaks every few seconds.
 * (Mojang revealed the blub for Minecraft Dungeons II and for the Sift coming to Minecraft; this is a fan recreation
 * with original model, textures and sounds.)
 * It loves soul water, can be carried in a Soul Water Bucket, and is bred with Lullaby Blooms or Teal Sprouts.
 */
public class Blub extends Animal implements Bucketable {
	private static final EntityDataAccessor<Integer> DATA_VARIANT = SynchedEntityData.defineId(Blub.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Boolean> FROM_BUCKET = SynchedEntityData.defineId(Blub.class, EntityDataSerializers.BOOLEAN);
	private int hopCooldown;

	public Blub(EntityType<? extends Blub> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Animal.createAnimalAttributes().add(Attributes.MAX_HEALTH, 6.0).add(Attributes.MOVEMENT_SPEED, 0.22);
	}

	public static boolean checkBlubSpawnRules(EntityType<Blub> type, LevelAccessor level, EntitySpawnReason reason, BlockPos pos, RandomSource random) {
		return level.getBlockState(pos.below()).is(ModTags.SIFT_GROUND) || level.getFluidState(pos).is(ModTags.SOUL_WATER);
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(1, new PanicGoal(this, 1.6));
		goalSelector.addGoal(2, new BreedGoal(this, 0.9));
		goalSelector.addGoal(3, new TemptGoal(this, 1.0, s -> s.is(ModTags.BLUB_FOOD), false));
		goalSelector.addGoal(4, new FollowParentGoal(this, 1.1));
		goalSelector.addGoal(5, new RandomStrollGoal(this, 0.8));
		goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 6.0F));
		goalSelector.addGoal(7, new RandomLookAroundGoal(this));
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DATA_VARIANT, BlubVariant.SOUL.id);
		builder.define(FROM_BUCKET, false);
	}

	public BlubVariant getVariant() {
		return BlubVariant.byId(entityData.get(DATA_VARIANT));
	}

	public void setVariant(BlubVariant variant) {
		entityData.set(DATA_VARIANT, variant.id);
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.store("Variant", BlubVariant.CODEC, getVariant());
		output.putBoolean("FromBucket", fromBucket());
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		setVariant(input.read("Variant", BlubVariant.CODEC).orElse(BlubVariant.SOUL));
		setFromBucket(input.getBooleanOr("FromBucket", false));
	}

	@Override
	public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason, @Nullable SpawnGroupData data) {
		BlockPos below = blockPosition().below();
		if (level.getBlockState(below).is(ModBlocks.SIFT_SAND) || level.getBlockState(below).is(ModBlocks.DARK_SIFT_SCULK)) {
			setVariant(BlubVariant.CARAPACE);
		} else if (random.nextInt(4) == 0) {
			setVariant(BlubVariant.MEADOWS);
		} else {
			setVariant(BlubVariant.SOUL);
		}
		return super.finalizeSpawn(level, difficulty, reason, data);
	}

	@Override
	public void aiStep() {
		super.aiStep();
		if (level().isClientSide()) {
			if (getVariant() == BlubVariant.SOUL && random.nextInt(30) == 0) {
				level().addParticle(ParticleTypes.SCULK_SOUL, getRandomX(0.4), getY() + 0.4, getRandomZ(0.4), 0.0, 0.02, 0.0);
			}
			return;
		}
		// bouncy hop while wandering
		if (hopCooldown > 0) {
			hopCooldown--;
		} else if (onGround() && getDeltaMovement().horizontalDistanceSqr() > 1.0E-4 && random.nextInt(6) == 0) {
			jumpFromGround();
			playSound(ModSounds.BLUB_JUMP, 0.4F, getVoicePitch());
			hopCooldown = 12;
		}
	}

	@Override
	public boolean canBreatheUnderwater() {
		return true;
	}

	@Override
	public int getAmbientSoundInterval() {
		return 100;
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return ModSounds.BLUB_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return ModSounds.BLUB_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return ModSounds.BLUB_DEATH;
	}

	@Override
	public boolean isFood(ItemStack stack) {
		return stack.is(ModTags.BLUB_FOOD);
	}

	@Override
	public @Nullable Blub getBreedOffspring(ServerLevel level, AgeableMob partner) {
		Blub baby = ModEntities.BLUB.create(level, EntitySpawnReason.BREEDING);
		if (baby != null) {
			baby.setVariant(random.nextBoolean() || !(partner instanceof Blub other) ? getVariant() : other.getVariant());
		}
		return baby;
	}

	@Override
	public InteractionResult mobInteract(Player player, InteractionHand hand) {
		return Bucketable.bucketMobPickup(player, hand, this).orElse(super.mobInteract(player, hand));
	}

	// ------------------------------------------------------------------ Bucketable (carried in a Soul Water Bucket)
	@Override
	public boolean canBePickedUpWithBucket(ItemStack stack) {
		return stack.is(ModItems.SOUL_WATER_BUCKET);
	}

	@Override
	public boolean fromBucket() {
		return entityData.get(FROM_BUCKET);
	}

	@Override
	public void setFromBucket(boolean fromBucket) {
		entityData.set(FROM_BUCKET, fromBucket);
	}

	@Override
	public boolean requiresCustomPersistence() {
		return super.requiresCustomPersistence() || fromBucket();
	}

	@Override
	public void saveToBucketTag(ItemStack bucket) {
		Bucketable.saveDefaultDataToBucketTag(this, bucket);
		CustomData.update(DataComponents.BUCKET_ENTITY_DATA, bucket, tag -> {
			tag.putInt("Variant", getVariant().id);
			tag.putInt("Age", getAge());
		});
	}

	@Override
	public void loadFromBucketTag(CompoundTag tag) {
		Bucketable.loadDefaultDataFromBucketTag(this, tag);
		tag.getInt("Variant").ifPresent(v -> setVariant(BlubVariant.byId(v)));
		tag.getInt("Age").ifPresent(this::setAge);
	}

	@Override
	public ItemStack getBucketItemStack() {
		return new ItemStack(ModItems.BLUB_BUCKET);
	}

	@Override
	public SoundEvent getPickupSound() {
		return SoundEvents.BUCKET_FILL_AXOLOTL;
	}
}
