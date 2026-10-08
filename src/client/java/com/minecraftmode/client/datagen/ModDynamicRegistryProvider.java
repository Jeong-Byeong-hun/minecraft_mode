package com.minecraftmode.client.datagen;

import java.util.concurrent.CompletableFuture;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricDynamicRegistryProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;

/**
 * Writes the mod's ore features, placed features and enchantments (see {@code buildRegistry}).
 */
public class ModDynamicRegistryProvider extends FabricDynamicRegistryProvider {
	public ModDynamicRegistryProvider(final FabricPackOutput output, final CompletableFuture<HolderLookup.Provider> registries) {
		super(output, registries);
	}

	@Override
	protected void configure(final HolderLookup.Provider registries, final Entries entries) {
		entries.addAll(registries.lookupOrThrow(Registries.FEATURE));
		entries.addAll(registries.lookupOrThrow(Registries.PLACED_FEATURE));
		entries.addAll(registries.lookupOrThrow(Registries.ENCHANTMENT));
	}

	@Override
	public String getName() {
		return "Minecraft Mode dynamic registries";
	}
}
