package com.minecraftmode.enchantment;

import com.minecraftmode.MinecraftMode;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.enchantment.Enchantment;

/**
 * Key plus display texts of one enchantment. Datagen writes the names and the {@code .desc} keys
 * (the format read by description mods such as Enchantment Descriptions) for en_us and ko_kr.
 */
public record EnchantInfo(ResourceKey<Enchantment> key, String en, String ko, String enDesc, String koDesc) {
	public static EnchantInfo of(final String id, final String en, final String ko, final String enDesc, final String koDesc) {
		return new EnchantInfo(ResourceKey.create(Registries.ENCHANTMENT, MinecraftMode.id(id)), en, ko, enDesc, koDesc);
	}

	public String descriptionKey() {
		return "enchantment." + this.key.identifier().getNamespace() + "." + this.key.identifier().getPath() + ".desc";
	}
}
