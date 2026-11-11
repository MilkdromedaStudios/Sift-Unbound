package com.milkdromedastudios.siftunbound.registry;

import com.milkdromedastudios.siftunbound.SiftUnbound;
import com.milkdromedastudios.siftunbound.entity.Blub;
import com.milkdromedastudios.siftunbound.entity.EchoGolem;
import com.milkdromedastudios.siftunbound.entity.Nuzzle;
import com.milkdromedastudios.siftunbound.entity.SculkProwler;
import com.milkdromedastudios.siftunbound.entity.Monarch;
import com.milkdromedastudios.siftunbound.entity.Scavenger;
import com.milkdromedastudios.siftunbound.entity.projectile.MonarchFeather;
import com.milkdromedastudios.siftunbound.entity.projectile.PollenGlob;
import com.milkdromedastudios.siftunbound.entity.projectile.SentinelQuill;
import com.milkdromedastudios.siftunbound.entity.projectile.SoulBolt;
import com.milkdromedastudios.siftunbound.entity.sifter.AbstractSifter;
import com.milkdromedastudios.siftunbound.entity.sifter.Harmonizer;
import com.milkdromedastudios.siftunbound.entity.sifter.Nester;
import com.milkdromedastudios.siftunbound.entity.sifter.Pollinator;
import com.milkdromedastudios.siftunbound.entity.sifter.Seedling;
import com.milkdromedastudios.siftunbound.entity.sifter.Sentinel;
import com.milkdromedastudios.siftunbound.entity.sifter.Sprout;
import com.milkdromedastudios.siftunbound.entity.Singer;
import com.milkdromedastudios.siftunbound.entity.Slabber;
import com.milkdromedastudios.siftunbound.entity.SoulWisp;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import java.util.List;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.levelgen.Heightmap;

public final class ModEntities {
	public static final EntityType<Blub> BLUB = register("blub",
		EntityType.Builder.of(Blub::new, MobCategory.CREATURE).sized(0.55F, 0.55F).eyeHeight(0.4F).clientTrackingRange(8));
	public static final EntityType<SoulWisp> SOUL_WISP = register("soul_wisp",
		EntityType.Builder.of(SoulWisp::new, MobCategory.AMBIENT).sized(0.4F, 0.4F).eyeHeight(0.2F).clientTrackingRange(6).fireImmune());
	public static final EntityType<SculkProwler> SCULK_PROWLER = register("sculk_prowler",
		EntityType.Builder.of(SculkProwler::new, MobCategory.MONSTER).sized(0.9F, 0.9F).eyeHeight(0.6F).clientTrackingRange(8));
	public static final EntityType<Nuzzle> NUZZLE = register("nuzzle",
		EntityType.Builder.of(Nuzzle::new, MobCategory.CREATURE).sized(0.9F, 1.2F).eyeHeight(1.0F).clientTrackingRange(10));
	public static final EntityType<Slabber> SLABBER = register("slabber",
		EntityType.Builder.of(Slabber::new, MobCategory.CREATURE).sized(1.0F, 0.6F).eyeHeight(0.35F).clientTrackingRange(10));
	public static final EntityType<EchoGolem> ECHO_GOLEM = register("echo_golem",
		EntityType.Builder.of(EchoGolem::new, MobCategory.CREATURE).sized(0.9F, 1.5F).eyeHeight(1.15F).clientTrackingRange(10));
	public static final EntityType<Singer> SINGER = register("singer",
		EntityType.Builder.of(Singer::new, MobCategory.MISC).sized(1.0F, 2.8F).eyeHeight(2.3F).clientTrackingRange(12).fireImmune());

	public static final EntityType<Seedling> SEEDLING = register("seedling",
		EntityType.Builder.of(Seedling::new, MobCategory.MONSTER).sized(0.5F, 0.8F).eyeHeight(0.6F).clientTrackingRange(8));
	public static final EntityType<Nester> NESTER = register("nester",
		EntityType.Builder.of(Nester::new, MobCategory.MONSTER).sized(0.8F, 2.3F).eyeHeight(2.0F).clientTrackingRange(8));
	public static final EntityType<Pollinator> POLLINATOR = register("pollinator",
		EntityType.Builder.of(Pollinator::new, MobCategory.MONSTER).sized(0.9F, 0.9F).eyeHeight(0.7F).clientTrackingRange(8));
	public static final EntityType<Sentinel> SENTINEL = register("sentinel",
		EntityType.Builder.of(Sentinel::new, MobCategory.MONSTER).sized(0.7F, 1.9F).eyeHeight(1.65F).clientTrackingRange(8));
	public static final EntityType<Sprout> SPROUT = register("sprout",
		EntityType.Builder.of(Sprout::new, MobCategory.MONSTER).sized(0.8F, 1.8F).eyeHeight(1.4F).clientTrackingRange(8));
	public static final EntityType<Harmonizer> HARMONIZER = register("harmonizer",
		EntityType.Builder.of(Harmonizer::new, MobCategory.MONSTER).sized(1.8F, 3.6F).eyeHeight(2.9F).clientTrackingRange(10));
	public static final EntityType<Scavenger> SCAVENGER = register("scavenger",
		EntityType.Builder.of(Scavenger::new, MobCategory.MONSTER).sized(0.9F, 2.2F).eyeHeight(1.9F).clientTrackingRange(8));
	public static final EntityType<Monarch> MONARCH = register("monarch",
		EntityType.Builder.of(Monarch::new, MobCategory.MONSTER).sized(1.4F, 3.6F).eyeHeight(3.1F).clientTrackingRange(12).fireImmune());
	public static final EntityType<Monarch> MONARCH_ECHO = register("monarch_echo",
		EntityType.Builder.of(Monarch::new, MobCategory.MONSTER).sized(1.4F, 3.6F).eyeHeight(3.1F).clientTrackingRange(12).fireImmune());
	public static final EntityType<MonarchFeather> MONARCH_FEATHER = register("monarch_feather",
		EntityType.Builder.<MonarchFeather>of(MonarchFeather::new, MobCategory.MISC).sized(0.3F, 0.3F).clientTrackingRange(6).updateInterval(2));
	public static final EntityType<PollenGlob> POLLEN_GLOB = register("pollen_glob",
		EntityType.Builder.<PollenGlob>of(PollenGlob::new, MobCategory.MISC).sized(0.35F, 0.35F).clientTrackingRange(4).updateInterval(10));
	public static final EntityType<SentinelQuill> SENTINEL_QUILL = register("sentinel_quill",
		EntityType.Builder.<SentinelQuill>of(SentinelQuill::new, MobCategory.MISC).sized(0.25F, 0.25F).clientTrackingRange(4).updateInterval(2));
	public static final EntityType<SoulBolt> SOUL_BOLT = register("soul_bolt",
		EntityType.Builder.<SoulBolt>of(SoulBolt::new, MobCategory.MISC).sized(0.4F, 0.4F).clientTrackingRange(6).updateInterval(2));

	private static <T extends Entity> EntityType<T> register(String name, EntityType.Builder<T> builder) {
		ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, SiftUnbound.id(name));
		return Registry.register(BuiltInRegistries.ENTITY_TYPE, key, builder.build(key));
	}

	public static void init() {
		FabricDefaultAttributeRegistry.register(BLUB, Blub.createAttributes());
		FabricDefaultAttributeRegistry.register(SOUL_WISP, SoulWisp.createAttributes());
		FabricDefaultAttributeRegistry.register(SCULK_PROWLER, SculkProwler.createAttributes());
		FabricDefaultAttributeRegistry.register(NUZZLE, Nuzzle.createAttributes());
		FabricDefaultAttributeRegistry.register(SLABBER, Slabber.createAttributes());
		FabricDefaultAttributeRegistry.register(ECHO_GOLEM, EchoGolem.createAttributes());
		FabricDefaultAttributeRegistry.register(SINGER, Singer.createAttributes());
		SpawnPlacements.register(NUZZLE, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Nuzzle::checkNuzzleSpawnRules);
		SpawnPlacements.register(SLABBER, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Slabber::checkSlabberSpawnRules);
		SpawnPlacements.register(ECHO_GOLEM, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, EchoGolem::checkGolemSpawnRules);
		SpawnPlacements.register(BLUB, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Blub::checkBlubSpawnRules);
		SpawnPlacements.register(SOUL_WISP, SpawnPlacementTypes.NO_RESTRICTIONS, Heightmap.Types.MOTION_BLOCKING, SoulWisp::checkWispSpawnRules);
		FabricDefaultAttributeRegistry.register(SEEDLING, Seedling.createAttributes());
		FabricDefaultAttributeRegistry.register(NESTER, Nester.createAttributes());
		FabricDefaultAttributeRegistry.register(POLLINATOR, Pollinator.createAttributes());
		FabricDefaultAttributeRegistry.register(SENTINEL, Sentinel.createAttributes());
		FabricDefaultAttributeRegistry.register(SPROUT, Sprout.createAttributes());
		FabricDefaultAttributeRegistry.register(HARMONIZER, Harmonizer.createAttributes());
		FabricDefaultAttributeRegistry.register(SCAVENGER, Scavenger.createAttributes());
		FabricDefaultAttributeRegistry.register(MONARCH, Monarch.createAttributes());
		FabricDefaultAttributeRegistry.register(MONARCH_ECHO, Monarch.createEchoAttributes());
		for (EntityType<? extends AbstractSifter> sifter : List.of(SEEDLING, NESTER, POLLINATOR, SENTINEL, SPROUT, HARMONIZER)) {
			sifterPlacement(sifter);
		}
		SpawnPlacements.register(SCAVENGER, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Scavenger::checkScavengerSpawnRules);
		SpawnPlacements.register(SCULK_PROWLER, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, SculkProwler::checkProwlerSpawnRules);
	}

	private static <T extends AbstractSifter> void sifterPlacement(EntityType<T> type) {
		SpawnPlacements.register(type, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, AbstractSifter::checkSifterSpawnRules);
	}

	private ModEntities() {
	}
}
