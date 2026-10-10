package com.minecraftmode.bag;

import com.minecraftmode.job.gear.ClassArmor;
import com.minecraftmode.job.quest.QuestTokenItem;
import com.minecraftmode.job.weapon.JobWeapons;
import com.minecraftmode.registry.ModItems;
import com.minecraftmode.registry.ModTags;
import java.util.Set;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalItemTags;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * The three bags and what each one picks up by itself: gear (class gear, weapons, tools, armor), supplies (food, potions,
 * consumables) and ores (ores, raw metal, ingots, gems and the stone a pickaxe digs up). Currency and quest items (coins, essence,
 * trial tokens, keystones, ether) never go into a bag by themselves, since shops, trainers and benches read them from the
 * inventory. Ids are saved item keys - never rename them.
 */
public enum BagKind {
	GEAR("gear_bag", 0x8A5A3C),
	SUPPLY("supply_bag", 0x3C8A4E),
	ORE("ore_bag", 0x5A6A80);

	/** Stone and dirt a pickaxe or shovel brings home that the conventional tags do not list. */
	private static final Set<Item> DIGGINGS = Set.of(Items.DIRT, Items.COARSE_DIRT, Items.FLINT, Items.TUFF, Items.CALCITE, Items.DEEPSLATE,
		Items.COBBLED_DEEPSLATE, Items.ANDESITE, Items.DIORITE, Items.GRANITE, Items.BLACKSTONE, Items.BASALT, Items.SMOOTH_BASALT, Items.END_STONE,
		Items.CLAY_BALL, Items.AMETHYST_SHARD, Items.QUARTZ, Items.GLOWSTONE_DUST, Items.REDSTONE, Items.LAPIS_LAZULI, Items.COAL, Items.CHARCOAL);
	/** The mod's own ores and metals (read when needed: the bags are registered while ModItems is still filling in). */
	private static boolean modOre(final ItemStack stack) {
		return stack.is(ModItems.MYTHRIL_ORE) || stack.is(ModItems.DEEPSLATE_MYTHRIL_ORE) || stack.is(ModItems.RAW_MYTHRIL) || stack.is(ModItems.MYTHRIL_NUGGET)
			|| stack.is(ModItems.MYTHRIL_INGOT) || stack.is(ModItems.ALUMINUM_ORE) || stack.is(ModItems.DEEPSLATE_ALUMINUM_ORE) || stack.is(ModItems.RAW_ALUMINUM)
			|| stack.is(ModItems.ALUMINUM_NUGGET) || stack.is(ModItems.ALUMINUM_INGOT);
	}

	private final String id;
	private final int color;

	BagKind(final String id, final int color) {
		this.id = id;
		this.color = color;
	}

	public String id() {
		return this.id;
	}

	/** Cloth color of the icon. */
	public int color() {
		return this.color;
	}

	/** Whether this bag picks {@code stack} up by itself. */
	public boolean accepts(final ItemStack stack) {
		if (stack.isEmpty() || stack.getItem() instanceof BagItem || !stack.getItem().canFitInsideContainerItems() || reserved(stack)) {
			return false;
		}
		return switch (this) {
			case GEAR -> isGear(stack);
			case SUPPLY -> !isGear(stack) && isSupply(stack);
			case ORE -> !isGear(stack) && !isSupply(stack) && isOre(stack);
		};
	}

	/** Currency and quest items: shops, trainers and benches read them from the inventory. */
	public static boolean reserved(final ItemStack stack) {
		return stack.is(ModTags.COINS) || stack.is(ModItems.ESSENCE) || stack.is(ModItems.CONDENSED_ESSENCE) || stack.is(ModItems.EVOLUTION_ETHER)
			|| stack.is(ModItems.DUNGEON_KEYSTONE) || stack.getItem() instanceof QuestTokenItem || stack.is(Items.TOTEM_OF_UNDYING)
			|| stack.is(ItemTags.ARROWS);
	}

	public static boolean isGear(final ItemStack stack) {
		return JobWeapons.def(stack) != null || ClassArmor.def(stack) != null || stack.isDamageableItem() || stack.has(DataComponents.EQUIPPABLE)
			|| stack.is(ConventionalItemTags.TOOLS) || stack.is(ConventionalItemTags.ARMORS);
	}

	public static boolean isSupply(final ItemStack stack) {
		return stack.has(DataComponents.CONSUMABLE) || stack.has(DataComponents.POTION_CONTENTS) || stack.is(ConventionalItemTags.FOODS)
			|| stack.is(ConventionalItemTags.POTIONS) || stack.is(ModItems.RETURN_SCROLL);
	}

	public static boolean isOre(final ItemStack stack) {
		return stack.is(ConventionalItemTags.ORES) || stack.is(ConventionalItemTags.RAW_MATERIALS) || stack.is(ConventionalItemTags.INGOTS)
			|| stack.is(ConventionalItemTags.GEMS) || stack.is(ConventionalItemTags.NUGGETS) || stack.is(ConventionalItemTags.DUSTS)
			|| stack.is(ConventionalItemTags.STONES) || stack.is(ConventionalItemTags.COBBLESTONES) || stack.is(ConventionalItemTags.NETHERRACKS)
			|| stack.is(ConventionalItemTags.GRAVELS) || DIGGINGS.contains(stack.getItem()) || modOre(stack);
	}
}
