package com.milkdromedastudios.therift.registry;

import com.milkdromedastudios.therift.TheRift;
import com.milkdromedastudios.therift.item.EchoTunerItem;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.DoubleHighBlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MobBucketItem;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;
import net.minecraft.world.level.block.Block;

public final class ModItems {
	public static final List<Item> CREATIVE_ORDER = new ArrayList<>();

	public static final Item ECHO_TUNER = register("echo_tuner", EchoTunerItem::new, new Item.Properties().stacksTo(1).durability(16).rarity(Rarity.RARE));
	public static final Item SOUL_SHARD = register("soul_shard", Item::new, new Item.Properties());
	public static final Item BLUB_JELLY = register("blub_jelly", Item::new, new Item.Properties()
		.food(new FoodProperties.Builder().nutrition(3).saturationModifier(0.4F).alwaysEdible().build(),
			Consumables.defaultFood().onConsume(new ApplyStatusEffectsConsumeEffect(new MobEffectInstance(MobEffects.GLOWING, 200, 0))).build()));
	public static final Item SOUL_WATER_BUCKET = register("soul_water_bucket", p -> new BucketItem(ModFluids.SOUL_WATER, p),
		new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1));
	public static final Item BLUB_BUCKET = register("blub_bucket", p -> new MobBucketItem(ModEntities.BLUB, ModFluids.SOUL_WATER, SoundEvents.BUCKET_EMPTY_AXOLOTL, p),
		new Item.Properties().stacksTo(1).component(DataComponents.BUCKET_ENTITY_DATA, CustomData.EMPTY));
	public static final Item BLUB_SPAWN_EGG = register("blub_spawn_egg", SpawnEggItem::new, new Item.Properties().spawnEgg(ModEntities.BLUB));
	public static final Item SOUL_WISP_SPAWN_EGG = register("soul_wisp_spawn_egg", SpawnEggItem::new, new Item.Properties().spawnEgg(ModEntities.SOUL_WISP));
	public static final Item SCULK_PROWLER_SPAWN_EGG = register("sculk_prowler_spawn_egg", SpawnEggItem::new,
		new Item.Properties().spawnEgg(ModEntities.SCULK_PROWLER));

	private static Item register(String name, Function<Item.Properties, Item> factory, Item.Properties properties) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, TheRift.id(name));
		Item item = Registry.register(BuiltInRegistries.ITEM, key, factory.apply(properties.setId(key)));
		CREATIVE_ORDER.add(item);
		return item;
	}

	private static void registerBlockItem(Block block) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, BuiltInRegistries.BLOCK.getKey(block));
		Item.Properties props = new Item.Properties().setId(key).useBlockDescriptionPrefix();
		Item item = block == ModBlocks.TALL_SIFT_GRASS || block == ModBlocks.PALLID_DOOR ? new DoubleHighBlockItem(block, props) : new BlockItem(block, props);
		if (item instanceof BlockItem bi) {
			bi.registerBlocks(Item.BY_BLOCK, item);
		}
		Registry.register(BuiltInRegistries.ITEM, key, item);
		CREATIVE_ORDER.add(item);
	}

	public static void init() {
		for (Block block : ModBlocks.WITH_ITEMS) {
			registerBlockItem(block);
		}
	}

	private ModItems() {
	}
}
