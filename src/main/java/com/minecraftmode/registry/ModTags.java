package com.minecraftmode.registry;

import com.minecraftmode.MinecraftMode;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public final class ModTags {
	public static final TagKey<Item> MYTHRIL_TOOL_MATERIALS = TagKey.create(Registries.ITEM, MinecraftMode.id("mythril_tool_materials"));
	public static final TagKey<Item> REPAIRS_MYTHRIL_ARMOR = TagKey.create(Registries.ITEM, MinecraftMode.id("repairs_mythril_armor"));
	public static final TagKey<Item> COINS = TagKey.create(Registries.ITEM, MinecraftMode.id("coins"));

	private ModTags() {
	}
}
