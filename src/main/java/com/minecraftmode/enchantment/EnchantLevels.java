package com.minecraftmode.enchantment;

import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;

/** Reads the level of one of the mod's (data-driven) enchantments on an item. */
public final class EnchantLevels {
	public static Optional<Holder.Reference<Enchantment>> holder(final Level level, final ResourceKey<Enchantment> key) {
		return level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).get(key);
	}

	public static int get(final Level level, final ResourceKey<Enchantment> key, final ItemInstance item) {
		return holder(level, key).map(holder -> EnchantmentHelper.getItemEnchantmentLevel(holder, item)).orElse(0);
	}

	public static int get(final Level level, final EnchantInfo info, final ItemInstance item) {
		return get(level, info.key(), item);
	}

	private EnchantLevels() {
	}
}
