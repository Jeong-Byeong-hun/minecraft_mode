package com.minecraftmode;

import com.minecraftmode.economy.ModEconomy;
import com.minecraftmode.enchantment.ArmorAuras;
import com.minecraftmode.enchantment.AutoSmeltLoot;
import com.minecraftmode.enchantment.CombatEnchantmentHandlers;
import com.minecraftmode.enchantment.ToolEnchantmentHandlers;
import com.minecraftmode.city.CityServices;
import com.minecraftmode.command.JobCommand;
import com.minecraftmode.command.RaidCommands;
import com.minecraftmode.raid.Parties;
import com.minecraftmode.raid.Raids;
import com.minecraftmode.raid.loot.LootSessions;
import com.minecraftmode.job.JobEvents;
import com.minecraftmode.job.quest.QuestService;
import com.minecraftmode.job.quest.Quests;
import com.minecraftmode.job.skill.CombatHooks;
import com.minecraftmode.job.skill.SkillScheduler;
import com.minecraftmode.job.weapon.JobWeapons;
import com.minecraftmode.network.ModNetworking;
import com.minecraftmode.registry.ModAttachments;
import com.minecraftmode.registry.ModDataComponents;
import com.minecraftmode.registry.ModMenus;
import com.minecraftmode.registry.ModParticles;
import com.minecraftmode.registry.ModBlocks;
import com.minecraftmode.registry.ModCreativeTabs;
import com.minecraftmode.registry.ModEffects;
import com.minecraftmode.registry.ModEntities;
import com.minecraftmode.job.gear.ClassArmor;
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
		ModParticles.init();
		ModDataComponents.init();
		ModAttachments.init();
		ModBlocks.init();
		ModEntities.init();
		ModItems.init();
		JobWeapons.init();
		ClassArmor.init();
		Quests.init();
		ModMenus.init();
		ModCreativeTabs.init();
		ModOreGeneration.init();
		ModEconomy.init();
		AutoSmeltLoot.init();
		ToolEnchantmentHandlers.init();
		CombatEnchantmentHandlers.init();
		ArmorAuras.init();
		SkillScheduler.init();
		CombatHooks.init();
		JobEvents.init();
		QuestService.init();
		CityServices.init();
		Parties.init();
		Raids.init();
		LootSessions.init();
		ModNetworking.init();
		JobCommand.init();
		RaidCommands.init();

		LOGGER.info("Minecraft Mode initialized");
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
