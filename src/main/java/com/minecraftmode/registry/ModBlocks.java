package com.minecraftmode.registry;

import com.minecraftmode.MinecraftMode;
import com.minecraftmode.block.ShopBlock;
import java.util.function.Function;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;

public final class ModBlocks {
	// Mythril: deep, rare ore. Needs an iron pickaxe.
	public static final Block MYTHRIL_ORE = register(
		"mythril_ore",
		p -> new DropExperienceBlock(UniformInt.of(2, 5), p),
		BlockBehaviour.Properties.of().mapColor(MapColor.STONE).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(3.0F, 3.0F)
	);
	public static final Block DEEPSLATE_MYTHRIL_ORE = register(
		"deepslate_mythril_ore",
		p -> new DropExperienceBlock(UniformInt.of(2, 5), p),
		BlockBehaviour.Properties.ofLegacyCopy(MYTHRIL_ORE).mapColor(MapColor.DEEPSLATE).strength(4.5F, 3.0F).sound(SoundType.DEEPSLATE)
	);
	public static final Block MYTHRIL_BLOCK = register(
		"mythril_block",
		Block::new,
		BlockBehaviour.Properties.of()
			.mapColor(MapColor.COLOR_CYAN)
			.instrument(NoteBlockInstrument.IRON_XYLOPHONE)
			.requiresCorrectToolForDrops()
			.strength(5.0F, 6.0F)
			.sound(SoundType.METAL)
	);
	public static final Block RAW_MYTHRIL_BLOCK = register(
		"raw_mythril_block",
		Block::new,
		BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(5.0F, 6.0F)
	);

	// Aluminum: common ore (bauxite). Needs a stone pickaxe.
	public static final Block ALUMINUM_ORE = register(
		"aluminum_ore",
		p -> new DropExperienceBlock(ConstantInt.of(0), p),
		BlockBehaviour.Properties.of().mapColor(MapColor.STONE).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(3.0F, 3.0F)
	);
	public static final Block DEEPSLATE_ALUMINUM_ORE = register(
		"deepslate_aluminum_ore",
		p -> new DropExperienceBlock(ConstantInt.of(0), p),
		BlockBehaviour.Properties.ofLegacyCopy(ALUMINUM_ORE).mapColor(MapColor.DEEPSLATE).strength(4.5F, 3.0F).sound(SoundType.DEEPSLATE)
	);
	public static final Block ALUMINUM_BLOCK = register(
		"aluminum_block",
		Block::new,
		BlockBehaviour.Properties.of()
			.mapColor(MapColor.METAL)
			.instrument(NoteBlockInstrument.IRON_XYLOPHONE)
			.requiresCorrectToolForDrops()
			.strength(4.0F, 6.0F)
			.sound(SoundType.METAL)
	);
	public static final Block RAW_ALUMINUM_BLOCK = register(
		"raw_aluminum_block",
		Block::new,
		BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_ORANGE).instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops().strength(5.0F, 6.0F)
	);

	// Plastic: crafted material, no ore.
	public static final Block PLASTIC_BLOCK = register(
		"plastic_block", Block::new, BlockBehaviour.Properties.of().mapColor(MapColor.SNOW).strength(1.5F, 3.0F)
	);

	// Economy: right-click to open the coin shop.
	public static final Block SHOP_BLOCK = register(
		"shop_block", ShopBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).instrument(NoteBlockInstrument.BASS).strength(2.5F).sound(SoundType.WOOD)
	);

	private static Block register(final String name, final Function<BlockBehaviour.Properties, Block> factory, final BlockBehaviour.Properties properties) {
		return Blocks.register(ResourceKey.create(Registries.BLOCK, MinecraftMode.id(name)), factory, properties);
	}

	public static void init() {
	}

	private ModBlocks() {
	}
}
