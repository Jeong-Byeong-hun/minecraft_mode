package com.minecraftmode.item;

import com.minecraftmode.MinecraftMode;
import com.minecraftmode.registry.ModTags;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorMaterials;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;

/**
 * Mythril sits between iron and diamond: more durable and enchantable than iron,
 * can mine obsidian, but weaker than diamond in raw damage and armor.
 */
public final class ModMaterials {
	public static final ToolMaterial MYTHRIL_TOOL = new ToolMaterial(
		BlockTags.INCORRECT_FOR_DIAMOND_TOOL, 900, 7.0F, 2.5F, 18, ModTags.MYTHRIL_TOOL_MATERIALS
	);

	public static final ResourceKey<EquipmentAsset> MYTHRIL_ASSET = ResourceKey.create(EquipmentAssets.ROOT_ID, MinecraftMode.id("mythril"));

	public static final ArmorMaterial MYTHRIL_ARMOR = new ArmorMaterial(
		25, ArmorMaterials.makeDefense(3, 6, 7, 3, 9), 16, SoundEvents.ARMOR_EQUIP_IRON, 1.0F, 0.0F, ModTags.REPAIRS_MYTHRIL_ARMOR, MYTHRIL_ASSET
	);

	private ModMaterials() {
	}
}
