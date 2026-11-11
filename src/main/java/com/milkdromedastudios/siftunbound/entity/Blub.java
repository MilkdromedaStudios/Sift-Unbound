package com.milkdromedastudios.siftunbound.entity;

import com.milkdromedastudios.siftunbound.registry.ModBlocks;
import com.milkdromedastudios.siftunbound.registry.ModEntities;
import com.milkdromedastudios.siftunbound.registry.ModItems;
import com.milkdromedastudios.siftunbound.registry.ModSounds;
import com.milkdromedastudios.siftunbound.registry.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
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
import net.minecraft.world.entity.ai.goal.MoveToBlockGoal;
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
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * Blub: a small, squishy, rabbit-like creature that hops around the Sift and squeaks every few seconds.
 * (Mojang revealed the blub for Minecraft Dungeons II and for the Sift coming to Minecraft; this is a fan recreation
 * with original model, textures and sounds.)
 * Like the official blub it cannot be hurt or pushed (only /kill and the void get through), squeaks every few seconds and
 * loves to bathe in ichor: blubs wander to the nearest ichor pool for a soak and shed Blub Jelly when they climb out.
 * It can be carried in an Ichor Bucket and is bred with Lullaby Blooms or Teal Sprouts.
 */
public class Blub extends Animal implements Bucketable {
	private static final EntityDataAccessor<Integer> DATA_VARIANT = SynchedEntityData.defineId(Blub.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Boolean> FROM_BUCKET = SynchedEntityData.defineId(Blub.class, EntityDataSerializers.BOOLEAN);
	private int hopCooldown;
	private int soakTicks;

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
		goalSelector.addGoal(5, new BathingGoal(this));
		goalSelector.addGoal(6, new RandomStrollGoal(this, 0.8));
		goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 6.0F));
		goalSelector.addGoal(8, new RandomLookAroundGoal(this));
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
		// a good soak in ichor: count it, and shed a little jelly on the way out
		if (isInIchor()) {
			soakTicks++;
		} else if (soakTicks > 0) {
			if (soakTicks >= 200 && !isBaby() && random.nextInt(3) == 0 && level() instanceof ServerLevel server
				&& server.getGameRules().get(GameRules.MOB_DROPS)) {
				spawnAtLocation(server, new ItemStack(ModItems.BLUB_JELLY));
				playSound(ModSounds.BLUB_AMBIENT, 0.6F, getVoicePitch() * 1.2F);
			}
			soakTicks = 0;
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

	public boolean isInIchor() {
		return level().getFluidState(blockPosition()).is(ModTags.SOUL_WATER);
	}

	// ------------------------------------------------------------------ official blub: cannot be hurt or pushed
	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		return source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) && super.hurtServer(level, source, damage);
	}

	@Override
	public boolean isPushable() {
		return false;
	}

	@Override
	public void push(double xa, double ya, double za) {
	}

	@Override
	public boolean isPushedByFluid() {
		return false;
	}

	@Override
	protected void ageBoundaryReached() {
		super.ageBoundaryReached();
		if (!isBaby() && level() instanceof ServerLevel server && server.getGameRules().get(GameRules.MOB_DROPS)) {
			spawnAtLocation(server, new ItemStack(ModItems.BLUB_JELLY));
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

	// ------------------------------------------------------------------ Bucketable (carried in an Ichor Bucket)
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

	/** Walks to the shore of a nearby ichor pool, hops in and floats there for a while. */
	static final class BathingGoal extends MoveToBlockGoal {
		private final Blub blub;
		private int soak;

		BathingGoal(Blub blub) {
			super(blub, 0.9, 10, 4);
			this.blub = blub;
		}

		@Override
		public boolean canUse() {
			return !blub.isBaby() && !blub.isInIchor() && blub.getRandom().nextInt(4) == 0 && super.canUse();
		}

		@Override
		public boolean canContinueToUse() {
			return soak < 240 && (blub.isInIchor() || super.canContinueToUse());
		}

		@Override
		public void start() {
			super.start();
			soak = 0;
		}

		@Override
		public void tick() {
			super.tick();
			if (blub.isInIchor()) {
				soak++;
				if (soak % 40 == 0 && blub.level() instanceof ServerLevel server) {
					server.sendParticles(ParticleTypes.SCULK_SOUL, blub.getX(), blub.getY() + 0.5, blub.getZ(), 2, 0.2, 0.1, 0.2, 0.01);
				}
			} else if (isReachedTarget()) {
				// at the shore: hop into the ichor next to it
				BlockPos pool = poolNextTo(blub.level(), blockPos);
				if (pool != null) {
					blub.getJumpControl().jump();
					blub.setDeltaMovement(blub.getDeltaMovement().add((pool.getX() + 0.5 - blub.getX()) * 0.15, 0.35,
						(pool.getZ() + 0.5 - blub.getZ()) * 0.15));
				}
			}
		}

		@Override
		protected boolean isValidTarget(LevelReader level, BlockPos pos) {
			return level.getBlockState(pos).isFaceSturdy(level, pos, Direction.UP) && level.getBlockState(pos.above()).isAir()
				&& poolNextTo(level, pos) != null;
		}

		private static @Nullable BlockPos poolNextTo(LevelReader level, BlockPos shore) {
			for (Direction d : Direction.Plane.HORIZONTAL) {
				BlockPos p = shore.relative(d);
				if (level.getFluidState(p).is(ModTags.SOUL_WATER) && level.getFluidState(p).isSource()) {
					return p;
				}
			}
			return null;
		}
	}
}
