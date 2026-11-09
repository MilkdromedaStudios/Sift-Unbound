package com.milkdromedastudios.therift.fluid;

import com.milkdromedastudios.therift.registry.ModBlocks;
import com.milkdromedastudios.therift.registry.ModEffects;
import com.milkdromedastudios.therift.registry.ModFluids;
import com.milkdromedastudios.therift.registry.ModItems;
import com.milkdromedastudios.therift.registry.ModSounds;
import com.milkdromedastudios.therift.registry.ModTags;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.InsideBlockEffectType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import org.jspecify.annotations.Nullable;

/**
 * Soul Water: the shimmering, oil-on-water liquid of the Sift (inspired by Minecraft Dungeons II's ichor, "ouch water").
 * Creatures of the Sift bathe in it happily; anything from the Overworld becomes Soulsick and has its health drained.
 * It flows like water but never forms new sources, so it can't be farmed infinitely.
 */
public abstract class SoulWaterFluid extends FlowingFluid {
	@Override
	public Fluid getFlowing() {
		return ModFluids.FLOWING_SOUL_WATER;
	}

	@Override
	public Fluid getSource() {
		return ModFluids.SOUL_WATER;
	}

	@Override
	public Item getBucket() {
		return ModItems.SOUL_WATER_BUCKET;
	}

	@Override
	public void animateTick(Level level, BlockPos pos, FluidState fluidState, RandomSource random) {
		if (random.nextInt(48) == 0) {
			level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, ModSounds.SOUL_WATER_AMBIENT, SoundSource.AMBIENT,
				random.nextFloat() * 0.25F + 0.4F, random.nextFloat() * 0.4F + 0.8F, false);
		}
		if (fluidState.isSource() && random.nextInt(14) == 0 && level.getBlockState(pos.above()).isAir()) {
			level.addParticle(random.nextInt(4) == 0 ? ParticleTypes.SOUL : ParticleTypes.SCULK_SOUL,
				pos.getX() + random.nextDouble(), pos.getY() + 0.9, pos.getZ() + random.nextDouble(), 0.0, 0.03, 0.0);
		}
	}

	@Override
	public @Nullable ParticleOptions getDripParticle() {
		return ParticleTypes.DRIPPING_WATER;
	}

	@Override
	protected boolean canConvertToSource(ServerLevel level) {
		return false;
	}

	@Override
	protected void beforeDestroyingBlock(LevelAccessor level, BlockPos pos, BlockState state) {
		BlockEntity blockEntity = state.hasBlockEntity() ? level.getBlockEntity(pos) : null;
		Block.dropResources(state, level, pos, blockEntity);
	}

	@Override
	protected void entityInside(Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effectApplier) {
		effectApplier.apply(InsideBlockEffectType.EXTINGUISH);
		if (level.isClientSide() || !(entity instanceof LivingEntity living) || living.is(ModTags.SOUL_WATER_IMMUNE)) {
			return;
		}
		if (living instanceof Player player && (player.isCreative() || player.isSpectator())) {
			return;
		}
		MobEffectInstance current = living.getEffect(ModEffects.SOULSICK);
		if (current == null || current.getDuration() < 40) {
			if (current == null) {
				level.playSound(null, living.getX(), living.getY(), living.getZ(), ModSounds.SOUL_WATER_BURN, SoundSource.NEUTRAL, 0.8F, 1.0F);
			}
			living.addEffect(new MobEffectInstance(ModEffects.SOULSICK, 100, 0, false, true, true));
		}
	}

	@Override
	public int getSlopeFindDistance(LevelReader level) {
		return 4;
	}

	@Override
	public BlockState createLegacyBlock(FluidState fluidState) {
		return ModBlocks.SOUL_WATER.defaultBlockState().setValue(LiquidBlock.LEVEL, getLegacyLevel(fluidState));
	}

	@Override
	public boolean isSame(Fluid other) {
		return other == ModFluids.SOUL_WATER || other == ModFluids.FLOWING_SOUL_WATER;
	}

	@Override
	public int getDropOff(LevelReader level) {
		return 1;
	}

	@Override
	public int getTickDelay(LevelReader level) {
		return 10;
	}

	@Override
	public boolean canBeReplacedWith(FluidState state, BlockGetter level, BlockPos pos, Fluid other, Direction direction) {
		return direction == Direction.DOWN && !other.is(ModTags.SOUL_WATER);
	}

	@Override
	protected float getExplosionResistance() {
		return 100.0F;
	}

	@Override
	public Optional<SoundEvent> getPickupSound() {
		return Optional.of(SoundEvents.BUCKET_FILL);
	}

	public static class Flowing extends SoulWaterFluid {
		@Override
		protected void createFluidStateDefinition(StateDefinition.Builder<Fluid, FluidState> builder) {
			super.createFluidStateDefinition(builder);
			builder.add(LEVEL);
		}

		@Override
		public int getAmount(FluidState fluidState) {
			return fluidState.getValue(LEVEL);
		}

		@Override
		public boolean isSource(FluidState fluidState) {
			return false;
		}
	}

	public static class Source extends SoulWaterFluid {
		@Override
		public int getAmount(FluidState fluidState) {
			return 8;
		}

		@Override
		public boolean isSource(FluidState fluidState) {
			return true;
		}
	}
}
