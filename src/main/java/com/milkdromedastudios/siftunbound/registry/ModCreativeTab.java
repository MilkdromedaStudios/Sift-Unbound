package com.milkdromedastudios.siftunbound.registry;

import com.milkdromedastudios.siftunbound.SiftUnbound;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class ModCreativeTab {
	public static CreativeModeTab TAB;

	public static void init() {
		TAB = Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, SiftUnbound.id("sift_unbound"), FabricCreativeModeTab.builder()
			.title(Component.translatable("itemGroup.sift_unbound"))
			.icon(() -> new ItemStack(ModItems.ECHO_TUNER))
			.displayItems((params, output) -> {
				for (Item item : ModItems.CREATIVE_ORDER) {
					output.accept(item);
				}
			})
			.build());
	}

	private ModCreativeTab() {
	}
}
