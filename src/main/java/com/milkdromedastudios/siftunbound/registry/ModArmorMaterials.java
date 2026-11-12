package com.milkdromedastudios.siftunbound.registry;

import com.milkdromedastudios.siftunbound.SiftUnbound;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;

/**
 * Original survival-mode adaptation of equipment from Minecraft Dungeons II.
 * Mojang has NOT confirmed armor or these recipes for vanilla's upcoming Sift.
 */
public final class ModArmorMaterials {
	private static ResourceKey<EquipmentAsset> asset(String name) {
		return ResourceKey.create(EquipmentAssets.ROOT_ID, SiftUnbound.id(name));
	}

	private static TagKey<Item> repairItems(String name) {
		return TagKey.create(BuiltInRegistries.ITEM.key(), SiftUnbound.id(name));
	}

	public static final ArmorMaterial SIFTER = new ArmorMaterial(
		16, Map.of(ArmorType.HELMET, 2, ArmorType.CHESTPLATE, 5, ArmorType.LEGGINGS, 4, ArmorType.BOOTS, 1),
		18, SoundEvents.ARMOR_EQUIP_CHAIN, 0.0F, 0.0F, repairItems("repairs_sifter_armor"), asset("sifter"));

	public static final ArmorMaterial HUMBLER = new ArmorMaterial(
		24, Map.of(ArmorType.HELMET, 3, ArmorType.CHESTPLATE, 6, ArmorType.LEGGINGS, 5, ArmorType.BOOTS, 2),
		12, SoundEvents.ARMOR_EQUIP_IRON, 1.0F, 0.0F, repairItems("repairs_humbler_armor"), asset("humbler"));

	public static final ArmorMaterial MONARCH = new ArmorMaterial(
		33, Map.of(ArmorType.HELMET, 3, ArmorType.CHESTPLATE, 8, ArmorType.LEGGINGS, 6, ArmorType.BOOTS, 3),
		15, SoundEvents.ARMOR_EQUIP_NETHERITE, 2.0F, 0.1F, repairItems("repairs_monarch_armor"), asset("monarch"));

	private ModArmorMaterials() {}
}
