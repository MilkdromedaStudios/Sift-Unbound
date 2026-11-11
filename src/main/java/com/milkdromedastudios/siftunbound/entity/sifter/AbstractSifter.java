package com.milkdromedastudios.siftunbound.entity.sifter;

import com.milkdromedastudios.siftunbound.SiftUnbound;
import com.milkdromedastudios.siftunbound.registry.ModCriteria;
import com.milkdromedastudios.siftunbound.registry.ModSounds;
import com.milkdromedastudios.siftunbound.registry.ModTags;
import com.milkdromedastudios.siftunbound.world.SiftTides;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The Sifters (Minecraft Dungeons II): the Sift's own plant-like natives, wild creatures living in nests that were driven
 * mad by the illager invasion and now attack Overworlders on sight. They fight as a band: hurt one and those nearby join
 * in. When a Sprout (or a Harmonizer) dies, every sifter around it is <b>enraged</b> for good: faster, stronger and
 * tougher, with glowing purple details. Sifters are at home in ichor, and the sculkers hunt them.
 */
public abstract class AbstractSifter extends Monster {
	private static final EntityDataAccessor<Boolean> ENRAGED = SynchedEntityData.defineId(AbstractSifter.class, EntityDataSerializers.BOOLEAN);
	private static final Identifier RAGE = SiftUnbound.id("enraged");
	public static final double ENRAGE_RADIUS = 16.0;
	/** Purple, as on an enraged sifter. */
	public static final DustParticleOptions RAGE_DUST = new DustParticleOptions(0xB45CFF, 1.3F);

	protected AbstractSifter(EntityType<? extends AbstractSifter> type, Level level) {
		super(type, level);
	}

	public static boolean checkSifterSpawnRules(EntityType<? extends AbstractSifter> type, ServerLevelAccessor level, EntitySpawnReason reason, BlockPos pos,
		RandomSource random) {
		return level.getDifficulty() != Difficulty.PEACEFUL && level.getBlockState(pos.below()).is(ModTags.SIFT_GROUND);
	}

	@Override
	public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason,
		@Nullable SpawnGroupData data) {
		SpawnGroupData result = super.finalizeSpawn(level, difficulty, reason, data);
		SiftTides.empowerIfEndure(this);
		return result;
	}

	/** Shared goals; subclasses add their attacks (priorities 1-4) before or after calling this. */
	@Override
	protected void registerGoals() {
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.8));
		goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 10.0F));
		goalSelector.addGoal(8, new RandomLookAroundGoal(this));
		targetSelector.addGoal(1, new HurtByTargetGoal(this, AbstractSifter.class));
		targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(ENRAGED, false);
	}

	public boolean isEnraged() {
		return entityData.get(ENRAGED);
	}

	/** Enraged for good: +30% speed, +50% damage and +50% health, and it goes for whoever caused it. */
	public void enrage(@Nullable LivingEntity culprit) {
		if (!isEnraged()) {
			entityData.set(ENRAGED, true);
			boost(Attributes.MOVEMENT_SPEED, 0.3);
			boost(Attributes.ATTACK_DAMAGE, 0.5);
			float before = getHealth() / getMaxHealth();
			boost(Attributes.MAX_HEALTH, 0.5);
			setHealth(getMaxHealth() * before);
			playSound(ModSounds.SIFTER_ENRAGE, 1.2F, 0.8F + random.nextFloat() * 0.3F);
			if (level() instanceof ServerLevel server) {
				server.sendParticles(RAGE_DUST, getX(), getY() + getBbHeight() * 0.6, getZ(), 24, getBbWidth() * 0.6, getBbHeight() * 0.4, getBbWidth() * 0.6, 0.0);
			}
		}
		if (culprit != null && culprit.isAlive() && !(culprit instanceof AbstractSifter) && canAttack(culprit)) {
			setTarget(culprit);
		}
	}

	private void boost(Holder<Attribute> attribute, double amount) {
		AttributeInstance instance = getAttribute(attribute);
		if (instance != null) {
			instance.addOrReplacePermanentModifier(new AttributeModifier(RAGE, amount, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
		}
	}

	/** A Sprout or Harmonizer has died: every sifter within {@link #ENRAGE_RADIUS} blocks is enraged. */
	public static void enrageAround(ServerLevel level, LivingEntity fallen, @Nullable LivingEntity killer) {
		int count = 0;
		for (AbstractSifter sifter : level.getEntitiesOfClass(AbstractSifter.class, fallen.getBoundingBox().inflate(ENRAGE_RADIUS),
			s -> s != fallen && s.isAlive())) {
			sifter.enrage(killer);
			count++;
		}
		if (count > 0 && killer instanceof ServerPlayer player) {
			ModCriteria.award(player, ModCriteria.SIFTERS_ENRAGED);
		}
		level.playSound(null, fallen.getX(), fallen.getY(), fallen.getZ(), ModSounds.SIFTER_ENRAGE, SoundSource.HOSTILE, 2.0F, 0.6F);
	}

	/** Hurting one sifter brings the rest of its band within 12 blocks down on the attacker. */
	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		boolean hurt = super.hurtServer(level, source, damage);
		if (hurt && source.getEntity() instanceof LivingEntity attacker && !(attacker instanceof AbstractSifter) && attacker.isAlive()) {
			for (AbstractSifter ally : level.getEntitiesOfClass(AbstractSifter.class, getBoundingBox().inflate(12.0),
				s -> s != this && s.getTarget() == null && s.canAttack(attacker))) {
				ally.setTarget(attacker);
			}
		}
		return hurt;
	}

	@Override
	public void aiStep() {
		super.aiStep();
		if (level().isClientSide() && isEnraged() && random.nextInt(8) == 0) {
			Vec3 at = position().add((random.nextDouble() - 0.5) * getBbWidth(), random.nextDouble() * getBbHeight(), (random.nextDouble() - 0.5) * getBbWidth());
			level().addParticle(RAGE_DUST, at.x, at.y, at.z, 0.0, 0.02, 0.0);
		}
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putBoolean("Enraged", isEnraged());
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		entityData.set(ENRAGED, input.getBooleanOr("Enraged", false)); // the attribute boosts are saved with the attributes
	}

	/** A short burst of particles every sifter uses for its "tell" before an attack. */
	protected void tell(ServerLevel level, net.minecraft.core.particles.ParticleOptions particle, int count) {
		level.sendParticles(particle, getX(), getY() + getBbHeight() * 0.8, getZ(), count, getBbWidth() * 0.4, 0.2, getBbWidth() * 0.4, 0.02);
	}
}
