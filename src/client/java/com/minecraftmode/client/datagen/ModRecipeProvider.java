package com.minecraftmode.client.datagen;

import com.minecraftmode.registry.ModItems;
import com.minecraftmode.registry.ModTags;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider;
import net.minecraft.advancements.Advancement;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CookingBookCategory;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.ItemLike;

public class ModRecipeProvider extends FabricRecipeProvider {
	public ModRecipeProvider(final FabricPackOutput output, final CompletableFuture<HolderLookup.Provider> registries) {
		super(output, registries);
	}

	@Override
	protected RecipeProvider createRecipeProvider(
		final HolderLookup.Provider registries, final BootstrapContext<Recipe<?>> recipes, final BootstrapContext<Advancement> advancements
	) {
		return new RecipeProvider(recipes, advancements) {
			@Override
			public void buildRecipes() {
				this.metal(
					ModItems.RAW_MYTHRIL, ModItems.MYTHRIL_INGOT, ModItems.MYTHRIL_NUGGET, ModItems.MYTHRIL_BLOCK, ModItems.RAW_MYTHRIL_BLOCK,
					List.of(ModItems.MYTHRIL_ORE, ModItems.DEEPSLATE_MYTHRIL_ORE, ModItems.RAW_MYTHRIL), 1.0F
				);
				this.metal(
					ModItems.RAW_ALUMINUM, ModItems.ALUMINUM_INGOT, ModItems.ALUMINUM_NUGGET, ModItems.ALUMINUM_BLOCK, ModItems.RAW_ALUMINUM_BLOCK,
					List.of(ModItems.ALUMINUM_ORE, ModItems.DEEPSLATE_ALUMINUM_ORE, ModItems.RAW_ALUMINUM), 0.7F
				);
				this.mythrilGear();
				this.plastic();
				this.coins();
				this.shopBlock();
			}

			private void metal(
				final Item raw, final Item ingot, final Item nugget, final Item block, final Item rawBlock, final List<ItemLike> smeltables, final float xp
			) {
				String ingotName = getItemName(ingot);
				this.oreSmelting(smeltables, RecipeCategory.MISC, CookingBookCategory.MISC, ingot, xp, 200, ingotName);
				this.oreBlasting(smeltables, RecipeCategory.MISC, CookingBookCategory.MISC, ingot, xp, 100, ingotName);
				this.nineBlockStorageRecipesRecipesWithCustomUnpacking(
					RecipeCategory.MISC, ingot, RecipeCategory.BUILDING_BLOCKS, block, ingotName + "_from_" + getItemName(block), ingotName
				);
				this.nineBlockStorageRecipesWithCustomPacking(
					RecipeCategory.MISC, nugget, RecipeCategory.MISC, ingot, ingotName + "_from_nuggets", ingotName
				);
				this.nineBlockStorageRecipes(RecipeCategory.MISC, raw, RecipeCategory.BUILDING_BLOCKS, rawBlock);
			}

			private void mythrilGear() {
				String hasMythril = getHasName(ModItems.MYTHRIL_INGOT);
				this.shaped(RecipeCategory.COMBAT, ModItems.MYTHRIL_SWORD)
					.define('#', Items.STICK).define('X', ModTags.MYTHRIL_TOOL_MATERIALS)
					.pattern("X").pattern("X").pattern("#")
					.unlockedBy(hasMythril, this.has(ModTags.MYTHRIL_TOOL_MATERIALS))
					.save(this.output);
				this.shaped(RecipeCategory.TOOLS, ModItems.MYTHRIL_PICKAXE)
					.define('#', Items.STICK).define('X', ModTags.MYTHRIL_TOOL_MATERIALS)
					.pattern("XXX").pattern(" # ").pattern(" # ")
					.unlockedBy(hasMythril, this.has(ModTags.MYTHRIL_TOOL_MATERIALS))
					.save(this.output);
				this.shaped(RecipeCategory.TOOLS, ModItems.MYTHRIL_AXE)
					.define('#', Items.STICK).define('X', ModTags.MYTHRIL_TOOL_MATERIALS)
					.pattern("XX").pattern("X#").pattern(" #")
					.unlockedBy(hasMythril, this.has(ModTags.MYTHRIL_TOOL_MATERIALS))
					.save(this.output);
				this.shaped(RecipeCategory.TOOLS, ModItems.MYTHRIL_SHOVEL)
					.define('#', Items.STICK).define('X', ModTags.MYTHRIL_TOOL_MATERIALS)
					.pattern("X").pattern("#").pattern("#")
					.unlockedBy(hasMythril, this.has(ModTags.MYTHRIL_TOOL_MATERIALS))
					.save(this.output);
				this.shaped(RecipeCategory.TOOLS, ModItems.MYTHRIL_HOE)
					.define('#', Items.STICK).define('X', ModTags.MYTHRIL_TOOL_MATERIALS)
					.pattern("XX").pattern(" #").pattern(" #")
					.unlockedBy(hasMythril, this.has(ModTags.MYTHRIL_TOOL_MATERIALS))
					.save(this.output);

				this.shaped(RecipeCategory.COMBAT, ModItems.MYTHRIL_HELMET)
					.define('X', ModItems.MYTHRIL_INGOT)
					.pattern("XXX").pattern("X X")
					.unlockedBy(hasMythril, this.has(ModItems.MYTHRIL_INGOT))
					.save(this.output);
				this.shaped(RecipeCategory.COMBAT, ModItems.MYTHRIL_CHESTPLATE)
					.define('X', ModItems.MYTHRIL_INGOT)
					.pattern("X X").pattern("XXX").pattern("XXX")
					.unlockedBy(hasMythril, this.has(ModItems.MYTHRIL_INGOT))
					.save(this.output);
				this.shaped(RecipeCategory.COMBAT, ModItems.MYTHRIL_LEGGINGS)
					.define('X', ModItems.MYTHRIL_INGOT)
					.pattern("XXX").pattern("X X").pattern("X X")
					.unlockedBy(hasMythril, this.has(ModItems.MYTHRIL_INGOT))
					.save(this.output);
				this.shaped(RecipeCategory.COMBAT, ModItems.MYTHRIL_BOOTS)
					.define('X', ModItems.MYTHRIL_INGOT)
					.pattern("X X").pattern("X X")
					.unlockedBy(hasMythril, this.has(ModItems.MYTHRIL_INGOT))
					.save(this.output);
			}

			private void plastic() {
				// 2 coal/charcoal + 1 slime ball -> 4 plastic sheets
				this.shapeless(RecipeCategory.MISC, ModItems.PLASTIC_SHEET, 4)
					.requires(ItemTags.COALS)
					.requires(ItemTags.COALS)
					.requires(Items.SLIME_BALL)
					.unlockedBy(getHasName(Items.SLIME_BALL), this.has(Items.SLIME_BALL))
					.save(this.output, "plastic_sheet_from_slime_ball");
				// Bioplastic: smelt a dried kelp block into a single sheet
				this.oreSmelting(
					List.of(Items.DRIED_KELP_BLOCK), RecipeCategory.MISC, CookingBookCategory.MISC, ModItems.PLASTIC_SHEET, 0.1F, 200, getItemName(ModItems.PLASTIC_SHEET)
				);
				this.nineBlockStorageRecipes(RecipeCategory.MISC, ModItems.PLASTIC_SHEET, RecipeCategory.BUILDING_BLOCKS, ModItems.PLASTIC_BLOCK);
			}

			private void coins() {
				this.nineBlockStorageRecipes(
					RecipeCategory.MISC, ModItems.COPPER_COIN, RecipeCategory.MISC, ModItems.SILVER_COIN,
					"silver_coin_from_copper_coins", null, "copper_coins_from_silver_coin", null
				);
				this.nineBlockStorageRecipes(
					RecipeCategory.MISC, ModItems.SILVER_COIN, RecipeCategory.MISC, ModItems.GOLD_COIN,
					"gold_coin_from_silver_coins", null, "silver_coins_from_gold_coin", null
				);
			}

			private void shopBlock() {
				this.shaped(RecipeCategory.DECORATIONS, ModItems.SHOP_BLOCK)
					.define('A', ModItems.ALUMINUM_INGOT)
					.define('P', ModItems.PLASTIC_SHEET)
					.define('E', Items.EMERALD)
					.define('C', Items.CHEST)
					.pattern("AAA")
					.pattern("PEP")
					.pattern("PCP")
					.unlockedBy(getHasName(ModItems.ALUMINUM_INGOT), this.has(ModItems.ALUMINUM_INGOT))
					.save(this.output);
			}
		};
	}

	@Override
	public String getName() {
		return "Minecraft Mode recipes";
	}
}
