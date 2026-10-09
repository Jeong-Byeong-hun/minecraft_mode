package com.minecraftmode.registry;

import com.minecraftmode.MinecraftMode;
import com.minecraftmode.job.JobClass;
import com.minecraftmode.job.gear.ArmorPieceDef;
import com.minecraftmode.job.gear.ClassArmor;
import com.minecraftmode.job.gear.ItemLevels;
import com.minecraftmode.job.quest.Quests;
import com.minecraftmode.loot.EvolutionEtherItem;
import com.minecraftmode.job.weapon.JobWeapons;
import com.minecraftmode.job.weapon.WeaponDef;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class ModCreativeTabs {
	public static final ResourceKey<CreativeModeTab> MAIN = ResourceKey.create(Registries.CREATIVE_MODE_TAB, MinecraftMode.id("main"));
	public static final ResourceKey<CreativeModeTab> CLASSES = ResourceKey.create(Registries.CREATIVE_MODE_TAB, MinecraftMode.id("classes"));

	public static void init() {
		Registry.register(
			BuiltInRegistries.CREATIVE_MODE_TAB,
			MAIN,
			FabricCreativeModeTab.builder()
				.title(Component.translatable("itemGroup.minecraft_mode.main"))
				.icon(() -> new ItemStack(ModItems.MYTHRIL_INGOT))
				.displayItems((parameters, output) -> {
					output.accept(ModItems.MYTHRIL_ORE);
					output.accept(ModItems.DEEPSLATE_MYTHRIL_ORE);
					output.accept(ModItems.RAW_MYTHRIL);
					output.accept(ModItems.RAW_MYTHRIL_BLOCK);
					output.accept(ModItems.MYTHRIL_NUGGET);
					output.accept(ModItems.MYTHRIL_INGOT);
					output.accept(ModItems.MYTHRIL_BLOCK);
					output.accept(ModItems.MYTHRIL_SWORD);
					output.accept(ModItems.MYTHRIL_PICKAXE);
					output.accept(ModItems.MYTHRIL_AXE);
					output.accept(ModItems.MYTHRIL_SHOVEL);
					output.accept(ModItems.MYTHRIL_HOE);
					output.accept(ModItems.MYTHRIL_HELMET);
					output.accept(ModItems.MYTHRIL_CHESTPLATE);
					output.accept(ModItems.MYTHRIL_LEGGINGS);
					output.accept(ModItems.MYTHRIL_BOOTS);
					output.accept(ModItems.ALUMINUM_ORE);
					output.accept(ModItems.DEEPSLATE_ALUMINUM_ORE);
					output.accept(ModItems.RAW_ALUMINUM);
					output.accept(ModItems.RAW_ALUMINUM_BLOCK);
					output.accept(ModItems.ALUMINUM_NUGGET);
					output.accept(ModItems.ALUMINUM_INGOT);
					output.accept(ModItems.ALUMINUM_BLOCK);
					output.accept(ModItems.PLASTIC_SHEET);
					output.accept(ModItems.PLASTIC_BLOCK);
					output.accept(ModItems.COPPER_COIN);
					output.accept(ModItems.SILVER_COIN);
					output.accept(ModItems.GOLD_COIN);
					output.accept(ModItems.SHOP_BLOCK);
					output.accept(ModItems.BLACKSMITH_SHOP);
					output.accept(ModItems.GROCER_SHOP);
					output.accept(ModItems.JEWELER_SHOP);
					output.accept(ModItems.GUILD_SHOP);
					output.accept(ModItems.MINE_RAIDER_SPAWN_EGG);
					output.accept(ModItems.MYTHRIL_GOLEM_SPAWN_EGG);
				})
				.build()
		);
		// Classes: essence, tables and every class weapon by class and tier
		Registry.register(
			BuiltInRegistries.CREATIVE_MODE_TAB,
			CLASSES,
			FabricCreativeModeTab.builder()
				.title(Component.translatable("itemGroup.minecraft_mode.classes"))
				.icon(() -> new ItemStack(ModItems.ESSENCE))
				.displayItems((parameters, output) -> {
					output.accept(ModItems.ESSENCE);
					output.accept(ModItems.CONDENSED_ESSENCE);
					output.accept(ModItems.GOLEM_CORE);
					output.accept(ModItems.CLASS_RESET_SCROLL);
					output.accept(ModItems.ENGRAVING_TABLE);
					for (int grade = ItemLevels.MIN_BRACKET; grade <= ItemLevels.MAX_BRACKET; grade += 10) {
						output.accept(EvolutionEtherItem.of(grade, 1));
					}
					for (Item token : Quests.tokens()) {
						output.accept(token);
					}
					output.accept(ModItems.GUILD_SHOP);
					for (JobClass job : JobClass.PLAYABLE) {
						for (WeaponDef def : JobWeapons.of(job)) {
							output.accept(JobWeapons.item(def));
						}
						for (ArmorPieceDef piece : ClassArmor.pieces(job)) {
							output.accept(ClassArmor.item(piece));
						}
					}
				})
				.build()
		);
	}

	private ModCreativeTabs() {
	}
}
