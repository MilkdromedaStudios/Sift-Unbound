package com.milkdromedastudios.siftunbound.registry;

import com.milkdromedastudios.siftunbound.SiftUnbound;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;

public final class ModTags {
	public static final TagKey<Block> WARDEN_PORTAL_FRAME = TagKey.create(Registries.BLOCK, SiftUnbound.id("warden_portal_frame"));
	public static final TagKey<Block> SIFT_GROUND = TagKey.create(Registries.BLOCK, SiftUnbound.id("sift_ground"));
	public static final TagKey<Block> PALLID_LOGS = TagKey.create(Registries.BLOCK, SiftUnbound.id("pallid_logs"));
	public static final TagKey<Item> BLUB_FOOD = TagKey.create(Registries.ITEM, SiftUnbound.id("blub_food"));
	public static final TagKey<Fluid> SOUL_WATER = TagKey.create(Registries.FLUID, SiftUnbound.id("soul_water"));
	public static final TagKey<EntityType<?>> SOUL_WATER_IMMUNE = TagKey.create(Registries.ENTITY_TYPE, SiftUnbound.id("soul_water_immune"));

	private ModTags() {
	}
}
