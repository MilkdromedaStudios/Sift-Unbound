package com.milkdromedastudios.siftunbound.registry;

import com.milkdromedastudios.siftunbound.SiftUnbound;
import com.milkdromedastudios.siftunbound.entity.Blub;
import com.milkdromedastudios.siftunbound.entity.EchoGolem;
import com.milkdromedastudios.siftunbound.entity.Nuzzle;
import com.milkdromedastudios.siftunbound.entity.SculkProwler;
import com.milkdromedastudios.siftunbound.entity.Singer;
import com.milkdromedastudios.siftunbound.entity.Slabber;
import com.milkdromedastudios.siftunbound.entity.SoulWisp;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
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
		SpawnPlacements.register(SCULK_PROWLER, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, SculkProwler::checkProwlerSpawnRules);
	}

	private ModEntities() {
	}
}
