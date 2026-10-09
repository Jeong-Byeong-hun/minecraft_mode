package com.minecraftmode.client.datagen;

import com.minecraftmode.dungeon.DungeonDimension;
import com.minecraftmode.enchantment.ModEnchantments;
import com.minecraftmode.raid.RaidDamage;
import com.minecraftmode.raid.RaidDimension;
import com.minecraftmode.worldgen.ModOreGeneration;
import com.minecraftmode.worldgen.lair.NamedLairs;
import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;

public class MinecraftModeDataGenerator implements DataGeneratorEntrypoint {
	@Override
	public void onInitializeDataGenerator(final FabricDataGenerator generator) {
		FabricDataGenerator.Pack pack = generator.createPack();

		pack.addProvider(ModModelProvider::new);
		pack.addProvider(WeaponTextureProvider::new);
		pack.addProvider(ArmorAssetProvider::new);
		pack.addProvider(CreatureTextureProvider::new);
		pack.addProvider(ConsumableAssetProvider::new);
		pack.addProvider(CompanionAssetProvider::new);
		pack.addProvider(ClassDocProvider::new);
		pack.addProvider(GearDocProvider::new);
		pack.addProvider(ModRecipeProvider::new);
		pack.addProvider(ModLootProviders.Blocks::new);
		pack.addProvider(ModLootProviders.Entities::new);
		ModTagProviders.Blocks blockTags = pack.addProvider(ModTagProviders.Blocks::new);
		pack.addProvider((output, registries) -> new ModTagProviders.Items(output, registries, blockTags));
		pack.addProvider(ModTagProviders.Enchantments::new);
		pack.addProvider(ModTagProviders.DamageTypes::new);
		pack.addProvider(ModDynamicRegistryProvider::new);
		pack.addProvider(RaidDimensionProvider::new);
		pack.addProvider(ModLanguageProviders.English::new);
		pack.addProvider(ModLanguageProviders.Korean::new);
	}

	@Override
	public void buildRegistry(final RegistrySetBuilder registryBuilder) {
		registryBuilder.add(Registries.FEATURE, ModOreGeneration::bootstrapFeatures);
		registryBuilder.add(Registries.PLACED_FEATURE, ModOreGeneration::bootstrapPlacedFeatures);
		registryBuilder.add(Registries.ENCHANTMENT, ModEnchantments::bootstrap);
		registryBuilder.add(Registries.DIMENSION_TYPE, context -> {
			RaidDimension.bootstrapType(context);
			DungeonDimension.bootstrapType(context);
		});
		registryBuilder.add(Registries.DAMAGE_TYPE, RaidDamage::bootstrap);
		registryBuilder.add(Registries.STRUCTURE, NamedLairs::bootstrapStructures);
		registryBuilder.add(Registries.STRUCTURE_SET, NamedLairs::bootstrapSets);
	}
}
