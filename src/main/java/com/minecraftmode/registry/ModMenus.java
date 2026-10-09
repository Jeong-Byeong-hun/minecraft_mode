package com.minecraftmode.registry;

import com.minecraftmode.MinecraftMode;
import com.minecraftmode.craft.CraftMenu;
import com.minecraftmode.craft.Profession;
import com.minecraftmode.enhance.EnhanceMenu;
import com.minecraftmode.job.engrave.EngravingMenu;
import com.minecraftmode.loot.UpgradeMenu;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;

public final class ModMenus {
	public static final MenuType<EngravingMenu> ENGRAVING = Registry.register(
		BuiltInRegistries.MENU, MinecraftMode.id("engraving_table"), new MenuType<>(EngravingMenu::new, FeatureFlags.VANILLA_SET)
	);

	public static final MenuType<UpgradeMenu> UPGRADE = Registry.register(
		BuiltInRegistries.MENU, MinecraftMode.id("upgrade"), new MenuType<>(UpgradeMenu::new, FeatureFlags.VANILLA_SET)
	);

	public static final MenuType<EnhanceMenu> ENHANCE = Registry.register(
		BuiltInRegistries.MENU, MinecraftMode.id("enhance"), new MenuType<>(EnhanceMenu::new, FeatureFlags.VANILLA_SET)
	);

	public static final MenuType<CraftMenu> CRAFT_COOKING = Registry.register(
		BuiltInRegistries.MENU, MinecraftMode.id("craft_cooking"), new MenuType<>((id, inventory) -> new CraftMenu(ModMenus.CRAFT_COOKING, id, inventory, Profession.COOKING, null),
			FeatureFlags.VANILLA_SET)
	);
	public static final MenuType<CraftMenu> CRAFT_ALCHEMY = Registry.register(
		BuiltInRegistries.MENU, MinecraftMode.id("craft_alchemy"), new MenuType<>((id, inventory) -> new CraftMenu(ModMenus.CRAFT_ALCHEMY, id, inventory, Profession.ALCHEMY, null),
			FeatureFlags.VANILLA_SET)
	);
	public static final MenuType<CraftMenu> CRAFT_SMITHING = Registry.register(
		BuiltInRegistries.MENU, MinecraftMode.id("craft_smithing"), new MenuType<>((id, inventory) -> new CraftMenu(ModMenus.CRAFT_SMITHING, id, inventory, Profession.SMITHING, null),
			FeatureFlags.VANILLA_SET)
	);

	public static void init() {
	}

	private ModMenus() {
	}
}
