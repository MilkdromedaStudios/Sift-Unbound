package com.milkdromedastudios.siftunbound.entity;

import com.milkdromedastudios.siftunbound.portal.NoteBlockMachine;
import com.milkdromedastudios.siftunbound.registry.ModSounds;
import com.milkdromedastudios.siftunbound.registry.ModTags;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;
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
import net.minecraft.world.entity.ai.goal.MoveToBlockGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * Echo Golem: the gentle caretaker of the Sift (Minecraft Dungeons II). A squishy grey-green body powered by the soul
 * block on its head, which slowly runs down; it recharges by bathing in ichor. Echo golems are the ones who run the
 * note block machine: hand one an Echo Shard near a dormant Warden Portal and it sings the Sift's melody to wake it
 * (elsewhere it just hums the tune for you). The Singer creates them.
 */
public class EchoGolem extends PathfinderMob {
	public static final int MAX_CHARGE = 100;
	private static final EntityDataAccessor<Integer> CHARGE = SynchedEntityData.defineId(EchoGolem.class, EntityDataSerializers.INT);
	private int wakeIn = -1;

	public EchoGolem(EntityType<? extends EchoGolem> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return PathfinderMob.createMobAttributes()
			.add(Attributes.MAX_HEALTH, 40.0)
			.add(Attributes.MOVEMENT_SPEED, 0.22)
			.add(Attributes.KNOCKBACK_RESISTANCE, 0.4);
	}

	public static boolean checkGolemSpawnRules(EntityType<EchoGolem> type, LevelAccessor level, EntitySpawnReason reason, BlockPos pos, RandomSource random) {
		return level.getBlockState(pos.below()).is(ModTags.SIFT_GROUND);
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(1, new PanicGoal(this, 1.3));
		goalSelector.addGoal(2, new RechargeGoal(this));
		goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.7));
		goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
		goalSelector.addGoal(7, new LookAtPlayerGoal(this, Blub.class, 6.0F));
		goalSelector.addGoal(8, new RandomLookAroundGoal(this));
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(CHARGE, MAX_CHARGE);
	}

	public int getCharge() {
		return entityData.get(CHARGE);
	}

	private void setCharge(int charge) {
		entityData.set(CHARGE, Math.max(0, Math.min(MAX_CHARGE, charge)));
	}

	public boolean isInIchor() {
		return level().getFluidState(blockPosition()).is(ModTags.SOUL_WATER);
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putInt("Charge", getCharge());
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		setCharge(input.getIntOr("Charge", MAX_CHARGE));
	}

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		if (isInIchor()) {
			if (tickCount % 4 == 0) {
				setCharge(getCharge() + 1);
			}
			if (tickCount % 30 == 0) {
				level.sendParticles(ParticleTypes.SOUL, getX(), getY() + 1.6, getZ(), 1, 0.2, 0.1, 0.2, 0.01);
			}
		} else if (tickCount % 60 == 0) {
			setCharge(getCharge() - 1);
		}
		if (wakeIn > 0) {
			wakeIn--;
		} else if (wakeIn == 0) {
			wakeIn = -1;
			NoteBlockMachine.wakePortalNear(level, blockPosition());
		}
	}

	@Override
	public InteractionResult mobInteract(Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (!stack.is(Items.ECHO_SHARD)) {
			return super.mobInteract(player, hand);
		}
		if (level() instanceof ServerLevel server && wakeIn < 0) {
			NoteBlockMachine.hum(server, position());
			if (NoteBlockMachine.dormantPortalNear(server, blockPosition())) {
				wakeIn = NoteBlockMachine.MELODY.length * 6 + 10; // wake it as the last note fades
				stack.consume(1, player);
			} else {
				player.sendOverlayMessage(Component.translatable("message.sift_unbound.melody", NoteBlockMachine.melodyText()));
			}
			level().broadcastEntityEvent(this, (byte) 4);
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public void handleEntityEvent(byte id) {
		if (id == 4) {
			singTicks = 50;
		} else {
			super.handleEntityEvent(id);
		}
	}

	/** Client-side singing animation timer. */
	public int singTicks;

	@Override
	public void aiStep() {
		super.aiStep();
		if (level().isClientSide() && singTicks > 0) {
			singTicks--;
		}
	}

	@Override
	public boolean removeWhenFarAway(double distSqr) {
		return false;
	}

	@Override
	protected @Nullable SoundEvent getAmbientSound() {
		return ModSounds.ECHO_GOLEM_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return ModSounds.ECHO_GOLEM_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return ModSounds.ECHO_GOLEM_DEATH;
	}

	/** When its soul block runs low, the golem walks to an ichor pool and bathes until it is full again. */
	static final class RechargeGoal extends MoveToBlockGoal {
		private final EchoGolem golem;

		RechargeGoal(EchoGolem golem) {
			super(golem, 0.8, 16, 4);
			this.golem = golem;
		}

		@Override
		public boolean canUse() {
			return golem.getCharge() < 40 && !golem.isInIchor() && super.canUse();
		}

		@Override
		public boolean canContinueToUse() {
			return golem.getCharge() < MAX_CHARGE && (golem.isInIchor() || super.canContinueToUse());
		}

		@Override
		public void tick() {
			super.tick();
			if (!golem.isInIchor() && isReachedTarget()) {
				for (Direction d : Direction.Plane.HORIZONTAL) {
					BlockPos pool = blockPos.relative(d);
					if (golem.level().getFluidState(pool).is(ModTags.SOUL_WATER)) {
						golem.getMoveControl().setWantedPosition(pool.getX() + 0.5, pool.getY(), pool.getZ() + 0.5, 0.8);
						break;
					}
				}
			}
		}

		@Override
		protected boolean isValidTarget(LevelReader level, BlockPos pos) {
			if (!level.getBlockState(pos).isFaceSturdy(level, pos, Direction.UP) || !level.getBlockState(pos.above()).isAir()) {
				return false;
			}
			for (Direction d : Direction.Plane.HORIZONTAL) {
				if (level.getFluidState(pos.relative(d)).is(ModTags.SOUL_WATER)) {
					return true;
				}
			}
			return false;
		}
	}

	/** Echo golems within {@code radius} of a position. */
	public static List<EchoGolem> near(ServerLevel level, BlockPos pos, double radius) {
		return level.getEntitiesOfClass(EchoGolem.class, new net.minecraft.world.phys.AABB(pos).inflate(radius));
	}
}
