package com.minecraftmode.economy;

import com.minecraftmode.consumable.ConsumableDef;
import com.minecraftmode.consumable.Consumables;
import com.minecraftmode.job.JobData;
import com.minecraftmode.job.JobProgression;
import com.minecraftmode.job.JobClass;
import com.minecraftmode.job.gear.ClassGear;
import com.minecraftmode.job.gear.ItemLevels;
import com.minecraftmode.loot.Coins;
import com.minecraftmode.loot.GearIndex;
import com.minecraftmode.loot.GearShop;
import com.minecraftmode.registry.ModItems;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.Fireworks;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.ItemLike;
import org.jspecify.annotations.Nullable;

/**
 * Base price lists. Prices are in copper (C), silver (S = 9C) and gold (G = 9S) coins; the market
 * raises them with pressure (see {@link MarketData}). Built on demand so item fields are initialized.
 */
public final class ShopOffers {
	private static final int UNLIMITED = Integer.MAX_VALUE;

	/**
	 * One line of a price list: pay {@code costCount} x {@code cost} (plus {@code extraCount} x
	 * {@code extra} when set), receive {@code resultCount} x {@code result}. Market pressure only
	 * raises the coin part.
	 */
	public record Trade(ItemLike cost, int costCount, ItemLike result, int resultCount, @Nullable ItemLike extra, int extraCount,
		@Nullable Consumer<ItemStack> finish) {
		public Trade(final ItemLike cost, final int costCount, final ItemLike result, final int resultCount) {
			this(cost, costCount, result, resultCount, null, 0);
		}

		public Trade(final ItemLike cost, final int costCount, final ItemLike result, final int resultCount, final @Nullable ItemLike extra, final int extraCount) {
			this(cost, costCount, result, resultCount, extra, extraCount, null);
		}

		/** The same trade with components set on the result (e.g. a rocket's flight duration); the market key stays the item ids. */
		public Trade finishing(final Consumer<ItemStack> finish) {
			return new Trade(this.cost, this.costCount, this.result, this.resultCount, this.extra, this.extraCount, finish);
		}

		/** Stable id used for market pressure. */
		public String key() {
			return BuiltInRegistries.ITEM.getKey(this.cost.asItem()) + "->" + BuiltInRegistries.ITEM.getKey(this.result.asItem());
		}

		public MerchantOffer toOffer() {
			Optional<ItemCost> second = this.extra == null ? Optional.empty() : Optional.of(new ItemCost(this.extra, this.extraCount));
			ItemStack out = new ItemStack(this.result, this.resultCount);
			if (this.finish != null) {
				this.finish.accept(out);
			}
			return new MerchantOffer(new ItemCost(this.cost, this.costCount), second, out, UNLIMITED, 0, 0.0F);
		}
	}

	/** Price list for {@code player}; only the guild depends on who is asking. */
	public static List<Trade> trades(final ShopType type, final Player player) {
		return type == ShopType.GUILD ? guild(player) : trades(type);
	}

	public static List<Trade> trades(final ShopType type) {
		List<Trade> list = new ArrayList<>(base(type));
		ConsumableDef.Shop shop = switch (type) {
			case GENERAL -> ConsumableDef.Shop.GENERAL;
			case GROCER -> ConsumableDef.Shop.GROCER;
			case ALCHEMIST -> ConsumableDef.Shop.ALCHEMIST;
			default -> ConsumableDef.Shop.NONE;
		};
		if (shop != ConsumableDef.Shop.NONE) {
			for (ConsumableDef def : Consumables.soldAt(shop)) {
				list.add(priced(def.price(), Consumables.item(def)));
			}
		}
		if (type == ShopType.GENERAL) {
			list.add(priced(3 * 9, ModItems.RETURN_SCROLL));
			list.add(priced(5 * 9, ModItems.LAIR_MAP));
		}
		return list;
	}

	/** One {@code goods} for {@code copper}, charged in the largest coin that keeps the price close. */
	public static Trade priced(final int copper, final ItemLike goods) {
		if (copper >= Coins.GOLD) {
			int gold = Math.min(64, copper / Coins.GOLD);
			int silver = gold >= 64 ? 0 : Math.round(copper % Coins.GOLD / (float)Coins.SILVER);
			if (silver >= Coins.SILVER) {
				gold = Math.min(64, gold + 1);
				silver = 0;
			}
			// gold plus a silver remainder (100 copper = 1G 2S), so prices between gold steps are not all rounded to the same gold
			return silver > 0 ? new Trade(ModItems.GOLD_COIN, gold, goods, 1, ModItems.SILVER_COIN, silver) : buy(ModItems.GOLD_COIN, gold, goods, 1);
		}
		if (copper >= Coins.SILVER) {
			return buy(ModItems.SILVER_COIN, Math.max(1, Math.round(copper / (float)Coins.SILVER)), goods, 1);
		}
		return buy(ModItems.COPPER_COIN, Math.max(1, copper), goods, 1);
	}

	private static List<Trade> base(final ShopType type) {
		return switch (type) {
			case ALCHEMIST -> List.of(sell(Items.NETHER_WART, 8, ModItems.COPPER_COIN, 3), sell(Items.BLAZE_POWDER, 2, ModItems.COPPER_COIN, 4),
				sell(Items.GHAST_TEAR, 1, ModItems.SILVER_COIN, 1), sell(Items.PHANTOM_MEMBRANE, 2, ModItems.COPPER_COIN, 5));
			case GUILD -> guild(null);
			case GENERAL -> List.of(
				// cobblestone stays first: the shop test trades the first offer
				sell(Items.COBBLESTONE, 32, ModItems.COPPER_COIN, 1),
				sell(Items.COBBLED_DEEPSLATE, 32, ModItems.COPPER_COIN, 1),
				sell(Items.ANDESITE, 32, ModItems.COPPER_COIN, 1),
				sell(Items.DIORITE, 32, ModItems.COPPER_COIN, 1),
				sell(Items.GRANITE, 32, ModItems.COPPER_COIN, 1),
				sell(Items.TUFF, 32, ModItems.COPPER_COIN, 1),
				sell(Items.DIRT, 64, ModItems.COPPER_COIN, 1),
				sell(Items.SAND, 32, ModItems.COPPER_COIN, 1),
				sell(Items.GRAVEL, 32, ModItems.COPPER_COIN, 1),
				sell(Items.FLINT, 16, ModItems.COPPER_COIN, 1),
				sell(Items.OAK_LOG, 24, ModItems.COPPER_COIN, 1),
				sell(Items.SPRUCE_LOG, 24, ModItems.COPPER_COIN, 1),
				sell(Items.BIRCH_LOG, 24, ModItems.COPPER_COIN, 1),
				sell(Items.ROTTEN_FLESH, 16, ModItems.COPPER_COIN, 1),
				sell(Items.BONE, 16, ModItems.COPPER_COIN, 1),
				sell(Items.STRING, 12, ModItems.COPPER_COIN, 1),
				sell(Items.SPIDER_EYE, 6, ModItems.COPPER_COIN, 1),
				sell(Items.ARROW, 32, ModItems.COPPER_COIN, 1),
				sell(Items.GUNPOWDER, 4, ModItems.COPPER_COIN, 1),
				sell(Items.LEATHER, 6, ModItems.COPPER_COIN, 1),
				sell(Items.FEATHER, 16, ModItems.COPPER_COIN, 1),
				sell(Items.SLIME_BALL, 4, ModItems.COPPER_COIN, 1),
				sell(Items.ENDER_PEARL, 2, ModItems.COPPER_COIN, 2),
				sell(Items.COAL, 16, ModItems.COPPER_COIN, 2),
				sell(ModItems.ALUMINUM_INGOT, 4, ModItems.COPPER_COIN, 3),
				sell(Items.IRON_INGOT, 4, ModItems.COPPER_COIN, 3),
				sell(ModItems.PLASTIC_SHEET, 8, ModItems.COPPER_COIN, 2),
				sell(Items.GOLD_INGOT, 1, ModItems.COPPER_COIN, 2),
				sell(Items.EMERALD, 1, ModItems.SILVER_COIN, 1),
				sell(ModItems.MYTHRIL_INGOT, 1, ModItems.SILVER_COIN, 1),
				sell(Items.DIAMOND, 1, ModItems.SILVER_COIN, 2),
				// the best rockets (flight 3, no stars, so they never hurt) for elytra, cheap
				buy(ModItems.COPPER_COIN, 2, Items.FIREWORK_ROCKET, 16).finishing(stack -> stack.set(DataComponents.FIREWORKS, new Fireworks(3, List.of()))),
				buy(ModItems.COPPER_COIN, 2, Items.TORCH, 16),
				// creepers are gone, so gunpowder (splash potions, TNT) comes from the shop
				buy(ModItems.COPPER_COIN, 3, Items.GUNPOWDER, 4),
				buy(ModItems.COPPER_COIN, 2, Items.GLASS, 16),
				buy(ModItems.COPPER_COIN, 2, Items.OAK_LOG, 16),
				buy(ModItems.COPPER_COIN, 3, Items.BED.white(), 1),
				buy(ModItems.COPPER_COIN, 4, Items.BUCKET, 1),
				buy(ModItems.COPPER_COIN, 3, Items.BREAD, 6),
				buy(ModItems.COPPER_COIN, 5, Items.COOKED_BEEF, 8),
				buy(ModItems.SILVER_COIN, 1, Items.ENDER_PEARL, 2),
				buy(ModItems.SILVER_COIN, 2, Items.NAME_TAG, 1),
				buy(ModItems.SILVER_COIN, 3, ModItems.MYTHRIL_INGOT, 1),
				buy(ModItems.SILVER_COIN, 4, Items.DIAMOND, 1),
				buy(ModItems.GOLD_COIN, 1, Items.TOTEM_OF_UNDYING, 1)
			);
			case BLACKSMITH -> List.of(
				sell(Items.IRON_INGOT, 4, ModItems.COPPER_COIN, 3),
				sell(ModItems.ALUMINUM_INGOT, 4, ModItems.COPPER_COIN, 3),
				sell(Items.COAL, 16, ModItems.COPPER_COIN, 2),
				sell(Items.RAW_IRON, 5, ModItems.COPPER_COIN, 3),
				sell(Items.RAW_COPPER, 16, ModItems.COPPER_COIN, 2),
				sell(Items.COPPER_INGOT, 12, ModItems.COPPER_COIN, 2),
				sell(Items.RAW_GOLD, 2, ModItems.COPPER_COIN, 3),
				sell(Items.IRON_NUGGET, 36, ModItems.COPPER_COIN, 3),
				sell(Items.CHARCOAL, 16, ModItems.COPPER_COIN, 2),
				sell(ModItems.RAW_MYTHRIL, 2, ModItems.SILVER_COIN, 1),
				sell(ModItems.MYTHRIL_INGOT, 1, ModItems.SILVER_COIN, 1),
				buy(ModItems.COPPER_COIN, 3, Items.ARROW, 16),
				buy(ModItems.COPPER_COIN, 3, Items.COAL, 16),
				buy(ModItems.COPPER_COIN, 6, Items.IRON_INGOT, 4),
				buy(ModItems.COPPER_COIN, 6, Items.SHIELD, 1),
				buy(ModItems.COPPER_COIN, 8, Items.IRON_AXE, 1),
				buy(ModItems.COPPER_COIN, 8, Items.IRON_SHOVEL, 1),
				buy(ModItems.SILVER_COIN, 1, Items.ANVIL, 1),
				buy(ModItems.COPPER_COIN, 8, Items.IRON_PICKAXE, 1),
				buy(ModItems.COPPER_COIN, 8, Items.IRON_SWORD, 1),
				buy(ModItems.SILVER_COIN, 2, Items.IRON_CHESTPLATE, 1),
				buy(ModItems.SILVER_COIN, 4, ModItems.MYTHRIL_PICKAXE, 1),
				buy(ModItems.SILVER_COIN, 4, ModItems.MYTHRIL_SWORD, 1),
				buy(ModItems.SILVER_COIN, 7, ModItems.MYTHRIL_CHESTPLATE, 1)
			);
			case GROCER -> List.of(
				sell(Items.WHEAT, 20, ModItems.COPPER_COIN, 1),
				sell(Items.CARROT, 24, ModItems.COPPER_COIN, 1),
				sell(Items.POTATO, 24, ModItems.COPPER_COIN, 1),
				sell(Items.BEETROOT, 20, ModItems.COPPER_COIN, 1),
				sell(Items.MELON_SLICE, 32, ModItems.COPPER_COIN, 1),
				sell(Items.PUMPKIN, 6, ModItems.COPPER_COIN, 1),
				sell(Items.SUGAR_CANE, 24, ModItems.COPPER_COIN, 1),
				sell(Items.APPLE, 12, ModItems.COPPER_COIN, 1),
				sell(Items.SWEET_BERRIES, 24, ModItems.COPPER_COIN, 1),
				sell(Items.COCOA_BEANS, 16, ModItems.COPPER_COIN, 1),
				sell(Items.EGG, 16, ModItems.COPPER_COIN, 1),
				sell(Items.KELP, 32, ModItems.COPPER_COIN, 1),
				sell(Items.CACTUS, 24, ModItems.COPPER_COIN, 1),
				sell(Items.BAMBOO, 32, ModItems.COPPER_COIN, 1),
				sell(Items.BEEF, 12, ModItems.COPPER_COIN, 1),
				sell(Items.PORKCHOP, 12, ModItems.COPPER_COIN, 1),
				sell(Items.CHICKEN, 12, ModItems.COPPER_COIN, 1),
				sell(Items.MUTTON, 12, ModItems.COPPER_COIN, 1),
				sell(Items.COD, 12, ModItems.COPPER_COIN, 1),
				sell(Items.SALMON, 12, ModItems.COPPER_COIN, 1),
				sell(Items.WOOL.white(), 12, ModItems.COPPER_COIN, 1),
				buy(ModItems.COPPER_COIN, 2, Items.WHEAT_SEEDS, 16),
				buy(ModItems.COPPER_COIN, 3, Items.BREAD, 6),
				buy(ModItems.COPPER_COIN, 3, Items.BAKED_POTATO, 8),
				buy(ModItems.COPPER_COIN, 5, Items.COOKED_BEEF, 8),
				buy(ModItems.COPPER_COIN, 4, Items.CAKE, 1),
				buy(ModItems.SILVER_COIN, 1, Items.GOLDEN_APPLE, 1),
				buy(ModItems.SILVER_COIN, 2, Items.GOLDEN_CARROT, 4)
			);
			case JEWELER -> List.of(
				sell(Items.GOLD_INGOT, 1, ModItems.COPPER_COIN, 2),
				sell(Items.LAPIS_LAZULI, 16, ModItems.COPPER_COIN, 2),
				sell(Items.QUARTZ, 16, ModItems.COPPER_COIN, 2),
				sell(Items.REDSTONE, 32, ModItems.COPPER_COIN, 2),
				sell(Items.AMETHYST_SHARD, 8, ModItems.COPPER_COIN, 1),
				sell(Items.RAW_GOLD, 2, ModItems.COPPER_COIN, 3),
				sell(Items.GOLD_NUGGET, 18, ModItems.COPPER_COIN, 3),
				sell(Items.COPPER_INGOT, 12, ModItems.COPPER_COIN, 2),
				sell(Items.GLOWSTONE_DUST, 16, ModItems.COPPER_COIN, 2),
				sell(Items.PRISMARINE_SHARD, 8, ModItems.COPPER_COIN, 1),
				sell(Items.ECHO_SHARD, 1, ModItems.COPPER_COIN, 4),
				sell(Items.EMERALD, 1, ModItems.SILVER_COIN, 1),
				sell(Items.DIAMOND, 1, ModItems.SILVER_COIN, 2),
				buy(ModItems.SILVER_COIN, 2, Items.EMERALD, 1),
				buy(ModItems.SILVER_COIN, 2, Items.GOLD_INGOT, 8),
				buy(ModItems.SILVER_COIN, 3, ModItems.MYTHRIL_INGOT, 1),
				buy(ModItems.SILVER_COIN, 4, Items.DIAMOND, 1),
				buy(ModItems.GOLD_COIN, 1, Items.TOTEM_OF_UNDYING, 1),
				buy(ModItems.GOLD_COIN, 2, Items.NETHERITE_SCRAP, 1)
			);
		};
	}

	/**
	 * Class gear for the visitor: for every 10-level bracket up to one above their own, the bracket's
	 * shop weapon and shop armor piece of their class (everything else only drops; see {@link GearIndex}).
	 * Players without a class see the Lv 10 items of every class. Plus essence and the class reset scroll.
	 */
	private static List<Trade> guild(final @Nullable Player player) {
		JobData data = player == null ? JobData.DEFAULT : JobProgression.get(player);
		List<Trade> list = new ArrayList<>();
		if (data.hasClass()) {
			int top = Math.min(ItemLevels.MAX_BRACKET, ItemLevels.bracket(data.level()) + 10);
			for (int bracket = ItemLevels.MIN_BRACKET; bracket <= top; bracket += 10) {
				for (ClassGear gear : GearIndex.shopItems(data.job(), bracket)) {
					list.add(GearShop.trade(gear));
				}
			}
		} else {
			for (JobClass job : JobClass.PLAYABLE) {
				for (ClassGear gear : GearIndex.shopItems(job, ItemLevels.MIN_BRACKET)) {
					list.add(GearShop.trade(gear));
				}
			}
		}
		list.add(sell(ModItems.ESSENCE, 6, ModItems.COPPER_COIN, 2));
		list.add(buy(ModItems.GOLD_COIN, 4, ModItems.CLASS_RESET_SCROLL, 1));
		return list;
	}

	private static Trade sell(final ItemLike goods, final int goodsCount, final ItemLike coin, final int coinCount) {
		return new Trade(goods, goodsCount, coin, coinCount);
	}

	private static Trade buy(final ItemLike coin, final int price, final ItemLike goods, final int goodsCount) {
		return new Trade(coin, price, goods, goodsCount);
	}

	private ShopOffers() {
	}
}
