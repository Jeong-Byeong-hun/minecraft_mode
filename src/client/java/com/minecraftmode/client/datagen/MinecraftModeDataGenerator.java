package com.minecraftmode.client.datagen;

import com.minecraftmode.enchantment.ModEnchantments;
import com.minecraftmode.worldgen.ModOreGeneration;
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
		pack.addProvider(ClassDocProvider::new);
		pack.addProvider(ModRecipeProvider::new);
		pack.addProvider(ModLootProviders.Blocks::new);
		pack.addProvider(ModLootProviders.Entities::new);
		ModTagProviders.Blocks blockTags = pack.addProvider(ModTagProviders.Blocks::new);
		pack.addProvider((output, registries) -> new ModTagProviders.Items(output, registries, blockTags));
		pack.addProvider(ModTagProviders.Enchantments::new);
		pack.addProvider(ModDynamicRegistryProvider::new);
		pack.addProvider(ModLanguageProviders.English::new);
		pack.addProvider(ModLanguageProviders.Korean::new);
	}

	@Override
	public void buildRegistry(final RegistrySetBuilder registryBuilder) {
		registryBuilder.add(Registries.FEATURE, ModOreGeneration::bootstrapFeatures);
		registryBuilder.add(Registries.PLACED_FEATURE, ModOreGeneration::bootstrapPlacedFeatures);
		registryBuilder.add(Registries.ENCHANTMENT, ModEnchantments::bootstrap);
	}
}
