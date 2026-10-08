package com.minecraftmode;

import com.minecraftmode.economy.ModEconomy;
import com.minecraftmode.enchantment.AutoSmeltLoot;
import com.minecraftmode.registry.ModBlocks;
import com.minecraftmode.registry.ModCreativeTabs;
import com.minecraftmode.registry.ModEffects;
import com.minecraftmode.registry.ModEntities;
import com.minecraftmode.registry.ModItems;
import com.minecraftmode.worldgen.ModOreGeneration;
import net.fabricmc.api.ModInitializer;

import net.minecraft.resources.Identifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MinecraftMode implements ModInitializer {
	public static final String MOD_ID = "minecraft_mode";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		// Order matters: items reference blocks, the entity type (spawn egg) and effects.
		ModEffects.init();
		ModBlocks.init();
		ModEntities.init();
		ModItems.init();
		ModCreativeTabs.init();
		ModOreGeneration.init();
		ModEconomy.init();
		AutoSmeltLoot.init();

		LOGGER.info("Minecraft Mode initialized");
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
