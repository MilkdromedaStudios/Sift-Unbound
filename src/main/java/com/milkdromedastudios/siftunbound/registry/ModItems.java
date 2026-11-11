package com.milkdromedastudios.siftunbound.registry;

import com.milkdromedastudios.siftunbound.SiftUnbound;
import com.milkdromedastudios.siftunbound.item.EchoTunerItem;
import com.milkdromedastudios.siftunbound.item.PollenGlobItem;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
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
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;
import net.minecraft.world.level.block.Block;

public final class ModItems {
	public static final List<Item> CREATIVE_ORDER = new ArrayList<>();

	public static final Item ECHO_TUNER = register("echo_tuner", EchoTunerItem::new, new Item.Properties().stacksTo(1).durability(16).rarity(Rarity.RARE)
		.component(DataComponents.LORE, new ItemLore(List.of(
			Component.translatable("item.sift_unbound.echo_tuner.desc").withStyle(ChatFormatting.GRAY),
			Component.translatable("item.sift_unbound.echo_tuner.desc2").withStyle(ChatFormatting.GRAY)))));
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
	public static final Item NUZZLE_SPAWN_EGG = register("nuzzle_spawn_egg", SpawnEggItem::new, new Item.Properties().spawnEgg(ModEntities.NUZZLE));
	public static final Item SLABBER_SPAWN_EGG = register("slabber_spawn_egg", SpawnEggItem::new, new Item.Properties().spawnEgg(ModEntities.SLABBER));
	public static final Item ECHO_GOLEM_SPAWN_EGG = register("echo_golem_spawn_egg", SpawnEggItem::new, new Item.Properties().spawnEgg(ModEntities.ECHO_GOLEM));
	public static final Item SINGER_SPAWN_EGG = register("singer_spawn_egg", SpawnEggItem::new, new Item.Properties().spawnEgg(ModEntities.SINGER));
	public static final Item POLLEN_GLOB = register("pollen_glob", PollenGlobItem::new, new Item.Properties().stacksTo(16)
		.component(DataComponents.LORE, new ItemLore(List.of(Component.translatable("item.sift_unbound.pollen_glob.desc").withStyle(ChatFormatting.GRAY)))));
	public static final Item SENTINEL_QUILL = register("sentinel_quill", Item::new, new Item.Properties());
	public static final Item SEEDLING_SPAWN_EGG = register("seedling_spawn_egg", SpawnEggItem::new, new Item.Properties().spawnEgg(ModEntities.SEEDLING));
	public static final Item NESTER_SPAWN_EGG = register("nester_spawn_egg", SpawnEggItem::new, new Item.Properties().spawnEgg(ModEntities.NESTER));
	public static final Item POLLINATOR_SPAWN_EGG = register("pollinator_spawn_egg", SpawnEggItem::new, new Item.Properties().spawnEgg(ModEntities.POLLINATOR));
	public static final Item SENTINEL_SPAWN_EGG = register("sentinel_spawn_egg", SpawnEggItem::new, new Item.Properties().spawnEgg(ModEntities.SENTINEL));
	public static final Item SPROUT_SPAWN_EGG = register("sprout_spawn_egg", SpawnEggItem::new, new Item.Properties().spawnEgg(ModEntities.SPROUT));
	public static final Item HARMONIZER_SPAWN_EGG = register("harmonizer_spawn_egg", SpawnEggItem::new, new Item.Properties().spawnEgg(ModEntities.HARMONIZER));
	public static final Item SCAVENGER_SPAWN_EGG = register("scavenger_spawn_egg", SpawnEggItem::new, new Item.Properties().spawnEgg(ModEntities.SCAVENGER));
	public static final Item MONARCH_FEATHER = register("monarch_feather", Item::new, new Item.Properties().rarity(Rarity.UNCOMMON));
	public static final Item MONARCH_SPAWN_EGG = register("monarch_spawn_egg", SpawnEggItem::new, new Item.Properties().spawnEgg(ModEntities.MONARCH));
	/** Only the look of a Scavenger's soul bolt in flight; not in the creative tab. */
	public static final Item SOUL_BOLT = registerHidden("soul_bolt");

	private static Item register(String name, Function<Item.Properties, Item> factory, Item.Properties properties) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, SiftUnbound.id(name));
		Item item = Registry.register(BuiltInRegistries.ITEM, key, factory.apply(properties.setId(key)));
		CREATIVE_ORDER.add(item);
		return item;
	}

	private static Item registerHidden(String name) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, SiftUnbound.id(name));
		return Registry.register(BuiltInRegistries.ITEM, key, new Item(new Item.Properties().setId(key)));
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
