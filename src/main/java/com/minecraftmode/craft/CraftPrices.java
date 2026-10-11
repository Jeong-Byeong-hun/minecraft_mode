package com.minecraftmode.craft;

import com.minecraftmode.economy.ShopOffers;
import com.minecraftmode.economy.ShopType;
import com.minecraftmode.loot.Coins;
import com.minecraftmode.registry.ModItems;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;

/**
 * What the shops pay for the things professions make: cooking at the grocer, alchemy (and loose herbs) at the alchemist, smithing at
 * the blacksmith. A made item is worth its raw materials - what a shop pays for each ({@link ShopOffers#basePaid}), herbs and titan
 * shards by {@link #RAW}, made ingredients by their own cheapest recipe, vanilla made things (bread, paper, ...) by {@link #VANILLA} -
 * times a mastery factor that grows with the recipe's level,
 * so crafting always adds to what the materials would fetch and the high recipes add the most. With several recipes for one item
 * the cheapest decides. Outputs that depend on the crafter (Evolution Ether) are not bought.
 */
public final class CraftPrices {
	/** Mastery factor: {@code MASTERY_BASE + level * MASTERY_PER_LEVEL} (1.27 at recipe level 1, 2.25 at 50). */
	public static final float MASTERY_BASE = 1.25F;
	public static final float MASTERY_PER_LEVEL = 0.02F;

	/** Raw worth (copper) of what no shop buys: herbs by how hard they are to gather, and titan shards. */
	private static final Map<Item, Float> RAW = new LinkedHashMap<>();
	/** Vanilla things the recipes use that no shop buys, by what one of them is made of (smelted food from the raw food, ...). */
	private static final Map<Item, Map<Item, Float>> VANILLA = new LinkedHashMap<>();
	private static @Nullable Map<Item, Float> values;

	public static float mastery(final int level) {
		return MASTERY_BASE + level * MASTERY_PER_LEVEL;
	}

	/** The shop that buys what {@code profession} makes. */
	public static ShopType shop(final Profession profession) {
		return switch (profession) {
			case COOKING -> ShopType.GROCER;
			case ALCHEMY -> ShopType.ALCHEMIST;
			case SMITHING -> ShopType.BLACKSMITH;
		};
	}

	/** Copper a shop pays for one {@code item} made by a profession (unrounded), 0 for anything else. */
	public static float value(final Item item) {
		return values().getOrDefault(item, 0.0F);
	}

	/** The herbs and their worth, for the alchemist's list and the docs. */
	public static Map<Item, Float> herbs() {
		raw();
		Map<Item, Float> herbs = new LinkedHashMap<>(RAW);
		herbs.remove(ModItems.TITAN_SHARD);
		return herbs;
	}

	/** The sell lines of {@code type}: its profession's goods (and herbs at the alchemist), in recipe order. */
	public static List<ShopOffers.Trade> trades(final ShopType type) {
		List<ShopOffers.Trade> out = new ArrayList<>();
		if (type == ShopType.ALCHEMIST) {
			for (Map.Entry<Item, Float> herb : herbs().entrySet()) {
				ShopOffers.Trade trade = line(herb.getKey(), herb.getValue());
				if (trade != null) {
					out.add(trade);
				}
			}
		}
		Set<Item> seen = new HashSet<>();
		for (Profession profession : Profession.values()) {
			if (shop(profession) != type) {
				continue;
			}
			for (CraftRecipes.Recipe recipe : CraftRecipes.of(profession)) {
				if (recipe.output() instanceof CraftRecipes.Output output && seen.add(output.item())) {
					ShopOffers.Trade trade = line(output.item(), value(output.item()));
					if (trade != null) {
						out.add(trade);
					}
				}
			}
		}
		return out;
	}

	/**
	 * One sell line paying about {@code copper} per item: whole copper coins up to 63, silver up to 63 silver, gold above; below one
	 * copper, as many items as make a copper. Null when an item is worth too little to be bought at all (more than a stack a copper).
	 */
	public static ShopOffers.@Nullable Trade line(final Item item, final float copper) {
		if (copper <= 0.0F) {
			return null;
		}
		if (copper < 1.0F) {
			int count = (int)Math.ceil(1.0F / copper);
			return count > 64 ? null : new ShopOffers.Trade(item, count, ModItems.COPPER_COIN, 1);
		}
		if (copper < 63.5F) {
			return new ShopOffers.Trade(item, 1, ModItems.COPPER_COIN, Math.round(copper));
		}
		if (copper < 63.5F * Coins.SILVER) {
			return new ShopOffers.Trade(item, 1, ModItems.SILVER_COIN, Math.round(copper / Coins.SILVER));
		}
		return new ShopOffers.Trade(item, 1, ModItems.GOLD_COIN, Math.min(64, Math.round(copper / Coins.GOLD)));
	}

	private static synchronized Map<Item, Float> raw() {
		if (RAW.isEmpty()) {
			RAW.put(ModItems.SUNLEAF, 1.0F);
			RAW.put(ModItems.MOONPETAL, 2.0F);
			RAW.put(ModItems.FROSTROOT, 2.0F);
			RAW.put(ModItems.GLOWCAP, 2.0F);
			RAW.put(ModItems.EMBERBLOOM, 3.0F);
			RAW.put(ModItems.VOIDCAP, 3.0F);
			RAW.put(ModItems.TITAN_SHARD, (float)Coins.GOLD);
			VANILLA.put(Items.BAKED_POTATO, Map.of(Items.POTATO, 1.0F));
			VANILLA.put(Items.COOKED_BEEF, Map.of(Items.BEEF, 1.0F));
			VANILLA.put(Items.COOKED_SALMON, Map.of(Items.SALMON, 1.0F));
			VANILLA.put(Items.DRIED_KELP, Map.of(Items.KELP, 1.0F));
			VANILLA.put(Items.BREAD, Map.of(Items.WHEAT, 3.0F));
			VANILLA.put(Items.SUGAR, Map.of(Items.SUGAR_CANE, 1.0F));
			VANILLA.put(Items.PAPER, Map.of(Items.SUGAR_CANE, 1.0F));
			VANILLA.put(Items.GLASS_BOTTLE, Map.of(Items.SAND, 1.0F));
			VANILLA.put(Items.HONEY_BOTTLE, Map.of(Items.SAND, 1.0F));
			VANILLA.put(Items.BOWL, Map.of(Items.OAK_LOG, 0.1875F));
			VANILLA.put(Items.COMPASS, Map.of(Items.IRON_INGOT, 4.0F, Items.REDSTONE, 1.0F));
			VANILLA.put(Items.GOLDEN_CARROT, Map.of(Items.GOLD_NUGGET, 8.0F, Items.CARROT, 1.0F));
		}
		return RAW;
	}

	private static synchronized Map<Item, Float> values() {
		if (values == null) {
			Map<Item, Float> map = new LinkedHashMap<>();
			for (CraftRecipes.Recipe recipe : CraftRecipes.all()) {
				if (recipe.output() instanceof CraftRecipes.Output output) {
					float worth = materials(recipe, new HashSet<>()) * mastery(recipe.level()) / output.count();
					map.merge(output.item(), worth, Math::min);
				}
			}
			values = map;
		}
		return values;
	}

	/**
	 * Raw worth of one {@code item}: the table, a made item's cheapest materials per piece, a vanilla made thing's materials, or what a
	 * shop pays for it.
	 */
	private static float rawWorth(final Item item, final Set<Item> visiting) {
		Float fixed = raw().get(item);
		if (fixed != null) {
			return fixed;
		}
		Map<Item, Float> parts = VANILLA.get(item);
		if (parts != null) {
			float sum = 0.0F;
			for (Map.Entry<Item, Float> part : parts.entrySet()) {
				sum += ShopOffers.basePaid(part.getKey()) * part.getValue();
			}
			return sum;
		}
		float best = Float.MAX_VALUE;
		if (visiting.add(item)) {
			for (CraftRecipes.Recipe recipe : CraftRecipes.all()) {
				if (recipe.output() instanceof CraftRecipes.Output output && output.item() == item) {
					best = Math.min(best, materials(recipe, visiting) / output.count());
				}
			}
			visiting.remove(item);
		}
		return best == Float.MAX_VALUE ? ShopOffers.basePaid(item) : best;
	}

	/** Raw worth of everything one craft of {@code recipe} takes. */
	private static float materials(final CraftRecipes.Recipe recipe, final Set<Item> visiting) {
		float sum = 0.0F;
		for (CraftRecipes.Ingredient in : recipe.ingredients()) {
			sum += rawWorth(in.item(), visiting) * in.count();
		}
		return sum;
	}

	private CraftPrices() {
	}
}
