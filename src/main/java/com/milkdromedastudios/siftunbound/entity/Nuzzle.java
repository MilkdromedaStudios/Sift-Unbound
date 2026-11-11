package com.milkdromedastudios.siftunbound.entity;

import com.milkdromedastudios.siftunbound.SiftUnbound;
import com.milkdromedastudios.siftunbound.registry.ModBlocks;
import com.milkdromedastudios.siftunbound.registry.ModCriteria;
import com.milkdromedastudios.siftunbound.registry.ModEntities;
import com.milkdromedastudios.siftunbound.registry.ModSounds;
import com.milkdromedastudios.siftunbound.registry.ModTags;
import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Shearable;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowParentGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.loot.LootTable;
import org.jspecify.annotations.Nullable;

/**
 * Nuzzle: the sheep-like grazer of the Sift (Minecraft Dungeons II), seen grazing in Singer's Meadow. Its salmon fleece
 * can be sheared for pink wool and grows back when it grazes Sift grass. Bred with short or tall Sift grass.
 */
public class Nuzzle extends Animal implements Shearable {
	public static final ResourceKey<LootTable> SHEARING = ResourceKey.create(Registries.LOOT_TABLE, SiftUnbound.id("shearing/nuzzle"));
	private static final EntityDataAccessor<Boolean> SHEARED = SynchedEntityData.defineId(Nuzzle.class, EntityDataSerializers.BOOLEAN);
	private int eatAnimationTick;

	public Nuzzle(EntityType<? extends Nuzzle> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Animal.createAnimalAttributes().add(Attributes.MAX_HEALTH, 10.0).add(Attributes.MOVEMENT_SPEED, 0.23);
	}

	public static boolean checkNuzzleSpawnRules(EntityType<Nuzzle> type, LevelAccessor level, EntitySpawnReason reason, BlockPos pos, RandomSource random) {
		return level.getBlockState(pos.below()).is(ModTags.SIFT_GROUND) && level.getRawBrightness(pos, 0) > 6;
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(1, new PanicGoal(this, 1.3));
		goalSelector.addGoal(2, new BreedGoal(this, 1.0));
		goalSelector.addGoal(3, new TemptGoal(this, 1.1, s -> s.is(ModTags.NUZZLE_FOOD), false));
		goalSelector.addGoal(4, new FollowParentGoal(this, 1.1));
		goalSelector.addGoal(5, new GrazeGoal(this));
		goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 1.0));
		goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 6.0F));
		goalSelector.addGoal(8, new RandomLookAroundGoal(this));
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(SHEARED, false);
	}

	public boolean isSheared() {
		return entityData.get(SHEARED);
	}

	public void setSheared(boolean sheared) {
		entityData.set(SHEARED, sheared);
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putBoolean("Sheared", isSheared());
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		setSheared(input.getBooleanOr("Sheared", false));
	}

	@Override
	public void aiStep() {
		if (level().isClientSide()) {
			eatAnimationTick = Math.max(0, eatAnimationTick - 1);
		}
		super.aiStep();
	}

	@Override
	public void handleEntityEvent(byte id) {
		if (id == 10) {
			eatAnimationTick = 40;
		} else {
			super.handleEntityEvent(id);
		}
	}

	/** 0..1: how far the head is lowered to graze (client side, for the model). */
	public float grazeProgress(float partialTicks) {
		if (eatAnimationTick <= 0) {
			return 0.0F;
		}
		float t = eatAnimationTick - partialTicks;
		return t >= 36 ? (40 - t) / 4.0F : t <= 4 ? t / 4.0F : 1.0F;
	}

	@Override
	public InteractionResult mobInteract(Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (stack.is(Items.SHEARS)) {
			if (level() instanceof ServerLevel server && readyForShearing()) {
				shear(server, SoundSource.PLAYERS, stack);
				gameEvent(GameEvent.SHEAR, player);
				stack.hurtAndBreak(1, player, hand.asEquipmentSlot());
				if (player instanceof ServerPlayer serverPlayer) {
					ModCriteria.award(serverPlayer, ModCriteria.SHEARED_NUZZLE);
				}
				return InteractionResult.SUCCESS_SERVER;
			}
			return InteractionResult.CONSUME;
		}
		return super.mobInteract(player, hand);
	}

	@Override
	public void shear(ServerLevel level, SoundSource source, ItemStack tool) {
		level.playSound(null, this, SoundEvents.SHEEP_SHEAR, source, 1.0F, 1.2F);
		dropFromShearingLootTable(level, SHEARING, tool, (l, drop) -> {
			for (int i = 0; i < drop.getCount(); i++) {
				ItemEntity item = spawnAtLocation(l, drop.copyWithCount(1), 1.0F);
				if (item != null) {
					item.setDeltaMovement(item.getDeltaMovement().add((random.nextFloat() - random.nextFloat()) * 0.1F, random.nextFloat() * 0.05F,
						(random.nextFloat() - random.nextFloat()) * 0.1F));
				}
			}
		});
		setSheared(true);
	}

	@Override
	public boolean readyForShearing() {
		return !isSheared() && !isBaby();
	}

	@Override
	public void ate() {
		super.ate();
		setSheared(false);
		if (canAgeUp()) {
			ageUp(60);
		}
	}

	@Override
	public boolean isFood(ItemStack stack) {
		return stack.is(ModTags.NUZZLE_FOOD);
	}

	@Override
	public @Nullable Nuzzle getBreedOffspring(ServerLevel level, AgeableMob partner) {
		return ModEntities.NUZZLE.create(level, EntitySpawnReason.BREEDING);
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return ModSounds.NUZZLE_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return ModSounds.NUZZLE_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return ModSounds.NUZZLE_DEATH;
	}

	@Override
	protected void playStepSound(BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
		playSound(SoundEvents.WOOL_STEP, 0.15F, 1.0F);
	}

	/** Grazes Sift grass: eats a tuft of short grass, or nibbles a grass block down to soil, then its fleece regrows. */
	static final class GrazeGoal extends Goal {
		private final Nuzzle nuzzle;
		private int timer;

		GrazeGoal(Nuzzle nuzzle) {
			this.nuzzle = nuzzle;
			setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
		}

		@Override
		public boolean canUse() {
			if (nuzzle.getRandom().nextInt(nuzzle.isBaby() ? 50 : (nuzzle.isSheared() ? 300 : 1000)) != 0) {
				return false;
			}
			BlockPos pos = nuzzle.blockPosition();
			return nuzzle.level().getBlockState(pos).is(ModBlocks.SHORT_SIFT_GRASS) || nuzzle.level().getBlockState(pos.below()).is(ModBlocks.SIFT_GRASS_BLOCK);
		}

		@Override
		public void start() {
			timer = 40;
			nuzzle.level().broadcastEntityEvent(nuzzle, (byte) 10);
			nuzzle.getNavigation().stop();
		}

		@Override
		public boolean canContinueToUse() {
			return timer > 0;
		}

		@Override
		public void tick() {
			timer = Math.max(0, timer - 1);
			if (timer != 4 || !(nuzzle.level() instanceof ServerLevel level)) {
				return;
			}
			BlockPos pos = nuzzle.blockPosition();
			boolean griefing = level.getGameRules().get(GameRules.MOB_GRIEFING);
			if (level.getBlockState(pos).is(ModBlocks.SHORT_SIFT_GRASS)) {
				if (griefing) {
					level.destroyBlock(pos, false);
				}
				nuzzle.ate();
			} else if (level.getBlockState(pos.below()).is(ModBlocks.SIFT_GRASS_BLOCK)) {
				if (griefing) {
					level.levelEvent(2001, pos.below(), Block.getId(ModBlocks.SIFT_GRASS_BLOCK.defaultBlockState()));
					level.setBlock(pos.below(), ModBlocks.SIFT_SOIL.defaultBlockState(), Block.UPDATE_CLIENTS);
				}
				nuzzle.ate();
			}
		}
	}
}
