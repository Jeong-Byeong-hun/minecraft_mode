package com.minecraftmode.registry;

import com.minecraftmode.MinecraftMode;
import com.minecraftmode.worldgen.lair.LairChestBlockEntity;
import java.util.Set;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;

public final class ModBlockEntities {
	/** Lair treasure chests and supply caches (personal contents, lair lords). */
	public static final BlockEntityType<LairChestBlockEntity> LAIR_CHEST = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, MinecraftMode.id("lair_chest"),
		new BlockEntityType<>(LairChestBlockEntity::new, Set.of(ModBlocks.LAIR_CHEST, ModBlocks.LAIR_CACHE)));

	public static void init() {
	}

	private ModBlockEntities() {
	}
}
