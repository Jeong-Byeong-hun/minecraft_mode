package com.minecraftmode.craft;

import com.minecraftmode.consumable.Consumables;
import com.minecraftmode.job.JobProgression;
import com.minecraftmode.loot.EvolutionEtherItem;
import com.minecraftmode.registry.ModItems;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;

/**
 * What each profession can make. A recipe has a profession level, ingredients taken from the inventory, an output (some
 * depend on the crafter, like Evolution Ether of their level bracket) and the experience it teaches. Ids are only used for
 * lang and docs.
 */
public final class CraftRecipes {
	public record Ingredient(Item item, int count) {
	}

	/** A fixed output (most recipes); docs read the item and count without making a stack. */
	public record Output(Item item, int count) implements Function<Player, ItemStack> {
		@Override
		public ItemStack apply(final @Nullable Player player) {
			return new ItemStack(this.item, this.count);
		}
	}

	public record Recipe(String id, Profession profession, int level, List<Ingredient> ingredients, Function<Player, ItemStack> output, int exp) {
		public ItemStack preview(final @Nullable Player player) {
			return player == null ? ItemStack.EMPTY : this.output.apply(player);
		}

		/** Experience for one craft; recipes far below the crafter's level teach little. */
		public int expFor(final int professionLevel) {
			return professionLevel >= this.level + 15 ? Math.max(1, this.exp / 4) : this.exp;
		}
	}

	private static @Nullable Map<Profession, List<Recipe>> recipes;

	private static Map<Profession, List<Recipe>> recipes() {
		if (recipes == null) {
			Map<Profession, List<Recipe>> map = new EnumMap<>(Profession.class);
			map.put(Profession.COOKING, cooking());
			map.put(Profession.ALCHEMY, alchemy());
			map.put(Profession.SMITHING, smithing());
			recipes = map;
		}
		return recipes;
	}

	public static List<Recipe> of(final Profession profession) {
		return recipes().get(profession);
	}

	public static List<Recipe> all() {
		List<Recipe> out = new ArrayList<>();
		for (Profession p : Profession.values()) {
			out.addAll(of(p));
		}
		return out;
	}

	private static Ingredient in(final Item item, final int count) {
		return new Ingredient(item, count);
	}

	private static Function<Player, ItemStack> consumable(final String id, final int count) {
		return new Output(Consumables.item(id), count);
	}

	private static Function<Player, ItemStack> item(final Item item, final int count) {
		return new Output(item, count);
	}

	private static Recipe r(final String id, final Profession p, final int level, final Function<Player, ItemStack> output, final int exp, final Ingredient... in) {
		return new Recipe(id, p, level, List.of(in), output, exp);
	}

	private static List<Recipe> cooking() {
		Profession p = Profession.COOKING;
		return List.of(
			r("gimbap", p, 1, consumable("gimbap", 2), 6, in(Items.DRIED_KELP, 3), in(Items.WHEAT, 1), in(Items.CARROT, 1)),
			r("tteokbokki", p, 1, consumable("tteokbokki", 2), 6, in(Items.WHEAT, 2), in(Items.SWEET_BERRIES, 2)),
			r("roasted_sweet_potato", p, 3, consumable("roasted_sweet_potato", 2), 7, in(Items.BAKED_POTATO, 2), in(Items.SUGAR, 1)),
			r("bungeoppang", p, 5, consumable("bungeoppang", 3), 8, in(Items.WHEAT, 2), in(Items.SUGAR, 1), in(Items.COD, 1)),
			r("onigiri", p, 10, consumable("onigiri", 2), 10, in(Items.WHEAT, 2), in(Items.DRIED_KELP, 1), in(Items.COOKED_SALMON, 1)),
			r("samgyetang", p, 12, consumable("samgyetang", 1), 12, in(Items.CHICKEN, 1), in(ModItems.SUNLEAF, 2), in(Items.BOWL, 1)),
			r("meat_on_the_bone", p, 15, consumable("meat_on_the_bone", 1), 13, in(Items.COOKED_BEEF, 2), in(Items.BONE, 1)),
			r("fortune_cookie", p, 18, consumable("fortune_cookie", 3), 14, in(Items.WHEAT, 2), in(Items.SUGAR, 1), in(Items.PAPER, 1)),
			r("heros_bento", p, 25, consumable("heros_bento", 1), 18, in(Items.COOKED_BEEF, 1), in(Items.COOKED_SALMON, 1), in(Items.BREAD, 1),
				in(ModItems.SUNLEAF, 2)),
			r("hunters_jerky", p, 30, consumable("hunters_jerky", 2), 20, in(Items.COOKED_BEEF, 3), in(ModItems.FROSTROOT, 2)),
			r("super_mushroom", p, 38, consumable("super_mushroom", 1), 24, in(Items.RED_MUSHROOM, 2), in(ModItems.GLOWCAP, 2), in(Items.GOLDEN_CARROT, 1)),
			r("dragon_heart_steak", p, 50, consumable("dragon_heart_steak", 1), 40, in(ModItems.TITAN_SHARD, 1), in(Items.COOKED_BEEF, 4),
				in(ModItems.EMBERBLOOM, 3), in(ModItems.VOIDCAP, 2))
		);
	}

	private static List<Recipe> alchemy() {
		Profession p = Profession.ALCHEMY;
		return List.of(
			r("healing_draught", p, 1, consumable("healing_draught", 2), 6, in(Items.GLASS_BOTTLE, 2), in(ModItems.SUNLEAF, 2)),
			r("mana_draught", p, 1, consumable("mana_draught", 2), 6, in(Items.GLASS_BOTTLE, 2), in(ModItems.MOONPETAL, 2)),
			r("antidote", p, 4, consumable("antidote", 2), 7, in(Items.GLASS_BOTTLE, 2), in(ModItems.SUNLEAF, 1), in(Items.SPIDER_EYE, 1)),
			r("green_tea", p, 8, consumable("green_tea", 2), 9, in(Items.GLASS_BOTTLE, 2), in(ModItems.SUNLEAF, 3)),
			r("greater_healing_draught", p, 10, consumable("greater_healing_draught", 1), 10, in(Consumables.item("healing_draught"), 2), in(ModItems.GLOWCAP, 1)),
			r("greater_mana_draught", p, 10, consumable("greater_mana_draught", 1), 10, in(Consumables.item("mana_draught"), 2), in(ModItems.MOONPETAL, 2)),
			r("red_ginseng_extract", p, 15, consumable("red_ginseng_extract", 1), 12, in(Items.HONEY_BOTTLE, 1), in(ModItems.SUNLEAF, 2), in(Items.CARROT, 2)),
			r("ironskin_tonic", p, 18, consumable("ironskin_tonic", 1), 13, in(Items.GLASS_BOTTLE, 1), in(Items.IRON_INGOT, 2), in(ModItems.FROSTROOT, 1)),
			r("scholars_coffee", p, 22, consumable("scholars_coffee", 2), 15, in(Items.COCOA_BEANS, 3), in(ModItems.MOONPETAL, 1), in(Items.GLASS_BOTTLE, 2)),
			r("thunderbolt", p, 26, consumable("thunderbolt", 1), 17, in(Items.GLASS_BOTTLE, 1), in(ModItems.EMBERBLOOM, 1), in(Items.GOLD_INGOT, 1)),
			r("hawkeye_draught", p, 28, consumable("hawkeye_draught", 1), 18, in(Items.GLASS_BOTTLE, 1), in(ModItems.FROSTROOT, 2), in(Items.SPIDER_EYE, 1)),
			r("arcane_draught", p, 30, consumable("arcane_draught", 1), 19, in(Items.GLASS_BOTTLE, 1), in(ModItems.MOONPETAL, 2), in(Items.LAPIS_LAZULI, 3)),
			r("estus_flask", p, 35, consumable("estus_flask", 1), 22, in(Consumables.item("greater_healing_draught"), 2), in(ModItems.EMBERBLOOM, 2)),
			r("hi_ether", p, 40, consumable("hi_ether", 1), 25, in(Consumables.item("greater_mana_draught"), 2), in(ModItems.VOIDCAP, 2)),
			r("elixir", p, 45, consumable("elixir", 1), 30, in(Consumables.item("estus_flask"), 1), in(Consumables.item("hi_ether"), 1), in(ModItems.VOIDCAP, 1)),
			r("philosophers_shard", p, 50, consumable("philosophers_shard", 1), 40, in(ModItems.TITAN_SHARD, 1), in(ModItems.CONDENSED_ESSENCE, 2),
				in(ModItems.GLOWCAP, 3))
		);
	}

	private static List<Recipe> smithing() {
		Profession p = Profession.SMITHING;
		return List.of(
			r("enhancement_stone", p, 1, item(ModItems.ENHANCEMENT_STONE, 1), 8, in(Items.IRON_INGOT, 2), in(ModItems.ESSENCE, 2)),
			r("return_scroll", p, 5, item(ModItems.RETURN_SCROLL, 2), 8, in(Items.PAPER, 2), in(Items.ENDER_PEARL, 1)),
			r("lair_map", p, 10, item(ModItems.LAIR_MAP, 1), 10, in(Items.PAPER, 3), in(Items.COMPASS, 1), in(ModItems.ESSENCE, 2)),
			r("enhancement_stones", p, 20, item(ModItems.ENHANCEMENT_STONE, 2), 14, in(ModItems.MYTHRIL_INGOT, 1), in(ModItems.ESSENCE, 3)),
			r("protection_scroll", p, 25, item(ModItems.PROTECTION_SCROLL, 1), 20, in(Items.PAPER, 3), in(Items.GOLD_INGOT, 2), in(ModItems.CONDENSED_ESSENCE, 2),
				in(ModItems.ENHANCEMENT_STONE, 2)),
			r("evolution_ether", p, 32, player -> EvolutionEtherItem.of(Math.max(10, JobProgression.get(player).level()), 3), 22, in(ModItems.MYTHRIL_INGOT, 2),
				in(ModItems.CONDENSED_ESSENCE, 1)),
			r("awakening_crystal", p, 40, item(ModItems.AWAKENING_CRYSTAL, 1), 35, in(ModItems.TITAN_SHARD, 1), in(ModItems.CONDENSED_ESSENCE, 4),
				in(Items.DIAMOND, 2))
		);
	}

	private CraftRecipes() {
	}
}
