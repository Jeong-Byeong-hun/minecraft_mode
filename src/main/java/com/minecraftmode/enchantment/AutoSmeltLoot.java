package com.minecraftmode.enchantment;

import java.util.List;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.advancements.predicates.DataComponentMatchers;
import net.minecraft.advancements.predicates.EnchantmentPredicate;
import net.minecraft.advancements.predicates.ItemPredicate;
import net.minecraft.advancements.predicates.MinMaxBounds;
import net.minecraft.core.component.predicates.DataComponentPredicates;
import net.minecraft.core.component.predicates.EnchantmentsPredicate;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.storage.loot.functions.SmeltItemFunction;
import net.minecraft.world.level.storage.loot.predicates.MatchTool;

/**
 * Adds "smelt the drop if the tool has Auto Smelt" to every block loot table (vanilla and modded).
 * Items without a smelting recipe pass through unchanged.
 */
public final class AutoSmeltLoot {
	public static void init() {
		LootTableEvents.MODIFY.register((key, tableBuilder, source, registries) -> {
			if (!key.identifier().getPath().startsWith("blocks/")) {
				return;
			}
			registries.lookupOrThrow(Registries.ENCHANTMENT).get(ModEnchantments.AUTO_SMELT).ifPresent(autoSmelt -> {
				var hasAutoSmelt = MatchTool.toolMatches(
					ItemPredicate.Builder.item()
						.withComponents(
							DataComponentMatchers.Builder.components()
								.partial(
									DataComponentPredicates.ENCHANTMENTS,
									EnchantmentsPredicate.enchantments(List.of(new EnchantmentPredicate(autoSmelt, MinMaxBounds.Ints.atLeast(1))))
								)
								.build()
						)
				);
				tableBuilder.modifyPools(pool -> pool.apply(SmeltItemFunction.smelted().when(hasAutoSmelt)));
			});
		});
	}

	private AutoSmeltLoot() {
	}
}
