package com.minecraftmode.client.endgame;

import com.minecraftmode.consumable.ConsumableDef;
import com.minecraftmode.consumable.Consumables;
import com.minecraftmode.craft.CraftRecipes;
import com.minecraftmode.economy.ShopOffers;
import com.minecraftmode.economy.ShopType;
import com.minecraftmode.economy.Wallet;
import com.minecraftmode.job.gear.ClassGear;
import com.minecraftmode.loot.GearIndex;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * Where an item of the codex comes from, read from the same definitions the game uses: shops that sell it for coins, profession
 * recipes that make it, the guild and drop brackets of class gear, raid-only consumables, and a hand-written line for materials that
 * only drop ({@code screen.minecraft_mode.codex.source.<item>}).
 */
final class CodexSources {
	private static final String KEY = "screen.minecraft_mode.codex.source.";
	private static @Nullable Map<Item, Set<Component>> fixed;

	/** Shops and recipes, collected once. */
	private static Map<Item, Set<Component>> fixed(final Player player) {
		if (fixed == null) {
			Map<Item, Set<Component>> map = new HashMap<>();
			for (ShopType type : ShopType.values()) {
				for (ShopOffers.Trade trade : ShopOffers.trades(type)) {
					// bought with coins (sales to the shop pay coins instead)
					if (Wallet.value(trade.cost().asItem()) > 0 && Wallet.value(trade.result().asItem()) <= 0) {
						map.computeIfAbsent(trade.result().asItem(), i -> new LinkedHashSet<>())
							.add(Component.translatable(KEY + "shop", Component.translatable(type.titleKey())));
					}
				}
			}
			for (CraftRecipes.Recipe recipe : CraftRecipes.all()) {
				ItemStack out = recipe.output() instanceof CraftRecipes.Output fixedOut ? new ItemStack(fixedOut.item()) : recipe.preview(player);
				if (!out.isEmpty()) {
					map.computeIfAbsent(out.getItem(), i -> new LinkedHashSet<>())
						.add(Component.translatable(KEY + "craft", Component.translatable(recipe.profession().nameKey()), recipe.level()));
				}
			}
			fixed = map;
		}
		return fixed;
	}

	static List<Component> of(final Item item, final Player player) {
		List<Component> out = new ArrayList<>(fixed(player).getOrDefault(item, Set.of()));
		ClassGear gear = ClassGear.of(new ItemStack(item));
		if (gear != null) {
			if (GearIndex.shopItems(gear.job(), gear.bracket()).contains(gear)) {
				out.add(Component.translatable(KEY + "guild", gear.bracket()));
			}
			out.add(Component.translatable(KEY + "drops", gear.bracket()));
			out.add(Component.translatable(KEY + "evolve"));
		}
		String path = BuiltInRegistries.ITEM.getKey(item).getPath();
		ConsumableDef def = Consumables.def(path);
		if (def != null && def.tier() >= 5) {
			out.add(Component.translatable(KEY + "raid"));
		}
		if (Language.getInstance().has(KEY + path)) {
			out.add(Component.translatable(KEY + path));
		}
		if (out.isEmpty()) {
			out.add(Component.translatable(KEY + "adventure"));
		}
		return out;
	}

	private CodexSources() {
	}
}
