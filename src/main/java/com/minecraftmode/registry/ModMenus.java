package com.minecraftmode.registry;

import com.minecraftmode.MinecraftMode;
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

	public static void init() {
	}

	private ModMenus() {
	}
}
