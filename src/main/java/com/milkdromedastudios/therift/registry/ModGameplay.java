package com.milkdromedastudios.therift.registry;

import net.fabricmc.fabric.api.item.v1.BlockTransformerHelper;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.fabricmc.fabric.api.registry.FlammableBlockRegistry;
import net.fabricmc.fabric.api.registry.fluid.EntityFluidInteractionRegistry;
import net.fabricmc.fabric.api.registry.fluid.FluidBehavior;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;

/** Hooks into vanilla systems: stripping, flammability, fluid physics and loot. */
public final class ModGameplay {
	public static void init() {
		BlockTransformerHelper.registerStripping(ModBlocks.PALLID_LOG, ModBlocks.STRIPPED_PALLID_LOG);
		BlockTransformerHelper.registerStripping(ModBlocks.PALLID_WOOD, ModBlocks.STRIPPED_PALLID_WOOD);

		FlammableBlockRegistry flammable = FlammableBlockRegistry.getDefaultInstance();
		for (Block b : new Block[]{ModBlocks.PALLID_LOG, ModBlocks.PALLID_WOOD, ModBlocks.STRIPPED_PALLID_LOG, ModBlocks.STRIPPED_PALLID_WOOD}) {
			flammable.add(b, 5, 5);
		}
		for (Block b : new Block[]{ModBlocks.PALLID_PLANKS, ModBlocks.PALLID_STAIRS, ModBlocks.PALLID_SLAB, ModBlocks.PALLID_FENCE, ModBlocks.PALLID_FENCE_GATE}) {
			flammable.add(b, 5, 20);
		}
		flammable.add(ModBlocks.PALLID_LEAVES, 30, 60);
		flammable.add(ModBlocks.SHORT_SIFT_GRASS, 60, 100);
		flammable.add(ModBlocks.TALL_SIFT_GRASS, 60, 100);
		flammable.add(ModBlocks.TEAL_SPROUTS, 60, 100);
		flammable.add(ModBlocks.LULLABY_BLOOM, 60, 100);

		// Soul water is swimmable like water (Sift creatures bathe in it; everyone else just gets Soulsick).
		EntityFluidInteractionRegistry.register(ModTags.SOUL_WATER, FluidBehavior.WATER_LIKE);

		// Ancient city chests occasionally hold a ready-made Echo Tuner.
		LootTableEvents.MODIFY.register((key, builder, source, registries) -> {
			if (source.isBuiltin() && key.equals(BuiltInLootTables.ANCIENT_CITY)) {
				builder.withPool(LootPool.lootPool()
					.add(LootItem.lootTableItem(ModItems.ECHO_TUNER))
					.when(LootItemRandomChanceCondition.randomChance(0.12F)));
			}
		});
	}

	private ModGameplay() {
	}
}
