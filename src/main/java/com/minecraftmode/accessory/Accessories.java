package com.minecraftmode.accessory;

import static com.minecraftmode.job.engrave.EngraveStat.*;

import com.minecraftmode.MinecraftMode;
import com.minecraftmode.enhance.Enhancement;
import com.minecraftmode.job.JobData;
import com.minecraftmode.job.JobProgression;
import com.minecraftmode.job.engrave.EngraveStat;
import com.minecraftmode.job.gear.ArmorOptions;
import com.minecraftmode.job.gear.ArmorSetDef;
import com.minecraftmode.job.gear.GearRolls;
import com.minecraftmode.job.gear.ItemLevels;
import com.minecraftmode.job.gear.StatLine;
import com.minecraftmode.loot.GearShop;
import com.minecraftmode.raid.BossDef;
import com.minecraftmode.raid.RaidBosses;
import com.minecraftmode.raid.RaidDifficulty;
import com.minecraftmode.registry.ModAttachments;
import com.minecraftmode.registry.ModDataComponents;
import com.minecraftmode.registry.ModItems;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import org.jspecify.annotations.Nullable;

/**
 * Accessories (0.5.24): necklace, earring, ring, bracelet and talisman, worn in five slots of their own (the accessory window,
 * {@link AccessoryMenu}) and kept per character in {@link ModAttachments#ACCESSORIES}. Every raid boss has a set of five, and only
 * raids drop them ({@code Raids.onBossDefeated} rolls {@link #raidDrops} for each rewarded member): no shop, lair, dungeon or named
 * monster gives them. The sets come in three tiers; on Normal a boss drops its own set (tier 1 15%, tier 2 8%, tier 3 1.5%), from
 * Heroic on every boss drops tier 3 pieces only, more often the harder the boss and the difficulty, so the ladder runs Normal
 * Verdessa ... Normal Aethryx, Heroic Verdessa ... Nightmare Aethryx. A piece needs the boss's level, has its slot's fixed line and 1-3
 * random options (Normal, Heroic, Nightmare; the blacksmith rolls them again), can be enhanced to +{@value AccessoryEnhance#MAX} with
 * accessory orbs (Heroic and Nightmare clears), and the set pays 2-, 3- and 5-piece bonuses. Tier 2 bosses drop tier 1 inheritance
 * tickets and tier 3 bosses tier 2 ones: at the blacksmith a ticket turns a piece into the same slot of a next-tier set of the
 * player's choice, keeping its enhancement and options ({@link #inherit}). Everything adds up in {@code GearStats} under its own
 * source.
 */
public final class Accessories {
	public static final int SLOTS = AccessorySlot.values().length;
	/** Options a piece rolls when it was made without any (creative, commands): one, like a Normal drop. */
	private static final int DEFAULT_OPTIONS = 1;

	private static final List<AccessorySet> SETS = new ArrayList<>();
	private static final Map<String, AccessoryDef> DEFS = new LinkedHashMap<>();
	private static final Map<String, Item> ITEMS = new LinkedHashMap<>();
	private static final Map<Item, AccessoryDef> BY_ITEM = new LinkedHashMap<>();

	static {
		// one set per raid boss, in the bosses' level order, tier 1-3 by three (the 5-piece bonus always adds damage dealt)
		set(RaidBosses.VERDESSA, 1, "Thornbloom", "가시꽃", 0x7FA05A, 0xE0457B, bonus(2, l(HEALTH_REGEN, 0.5F)), bonus(3, l(THORNS, 10)),
			bonus(5, l(MAX_HEALTH, 2), l(DAMAGE, 2)));
		set(RaidBosses.ARACHNE, 1, "Silkweaver", "비단거미", 0xC8C8D0, 0x6A2A8A, bonus(2, l(POISON, 1)), bonus(3, l(CRIT_CHANCE, 2)),
			bonus(5, l(EXECUTE, 8), l(DAMAGE, 3)));
		set(RaidBosses.GORVATH, 1, "Peakstone", "산정석", 0x8A8478, 0x6FB7E0, bonus(2, l(MAX_HEALTH, 3)), bonus(3, l(KNOCKBACK_RES, 20)),
			bonus(5, l(DAMAGE_REDUCTION, 4), l(DAMAGE, 3)));
		set(RaidBosses.NEFREKHET, 2, "Pharaoh's", "파라오의", 0xE0B23A, 0x2A6FD0, bonus(2, l(MAGIC_DEFENSE, 3)), bonus(3, l(SLOW, 1)),
			bonus(5, l(BOSS_DAMAGE, 8), l(DAMAGE, 4)));
		set(RaidBosses.KRAKEN, 2, "Abyssal", "심해의", 0x3A6A8A, 0x3AE0C8, bonus(2, l(LIFESTEAL, 2)), bonus(3, l(SKILL_AREA, 8)),
			bonus(5, l(MAX_MANA, 20), l(DAMAGE, 4)));
		set(RaidBosses.IGNIS, 2, "Phoenix", "불사조", 0xE07A2A, 0xFFD23A, bonus(2, l(BURN, 1)), bonus(3, l(HEALTH_REGEN, 1.5F)),
			bonus(5, l(LAST_STAND, 10), l(DAMAGE, 5)));
		set(RaidBosses.RAIJIN, 3, "Thunderdrum", "뇌고", 0x5A6AB0, 0xFFF04A, bonus(2, l(ATTACK_SPEED, 4)), bonus(3, l(CHAIN_LIGHTNING, 8)),
			bonus(5, l(CRIT_DAMAGE, 15), l(DAMAGE, 5)));
		set(RaidBosses.MALACHAR, 3, "Lich King's", "리치 왕의", 0x6A5A7A, 0x8AFF6A, bonus(2, l(MANA_REGEN, 2)), bonus(3, l(COOLDOWN, 5)),
			bonus(5, l(SKILL_DAMAGE, 10), l(DAMAGE, 6)));
		set(RaidBosses.AETHRYX, 3, "Voidscale", "공허비늘", 0x2A2440, 0xC07AFF, bonus(2, l(BOSS_DAMAGE, 6)), bonus(3, l(CRIT_CHANCE, 4)),
			bonus(5, l(DAMAGE, 8), l(PROTECTION, 3)));
	}

	private static void set(
		final BossDef boss, final int tier, final String adjEn, final String adjKo, final int metal, final int gem, final ArmorSetDef.SetBonus... bonuses
	) {
		AccessorySet set = new AccessorySet(boss, tier, adjEn, adjKo, metal, gem, List.of(bonuses));
		SETS.add(set);
		for (AccessorySlot slot : AccessorySlot.values()) {
			AccessoryDef def = new AccessoryDef(set, slot);
			DEFS.put(def.id(), def);
		}
	}

	private static ArmorSetDef.SetBonus bonus(final int pieces, final StatLine... lines) {
		return new ArmorSetDef.SetBonus(pieces, List.of(lines));
	}

	private static StatLine l(final EngraveStat stat, final float value) {
		return StatLine.of(stat, value);
	}

	public static void init() {
		for (AccessoryDef def : DEFS.values()) {
			ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, MinecraftMode.id(def.id()));
			Item item = Registry.register(BuiltInRegistries.ITEM, key, new AccessoryItem(def, new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).setId(key)));
			ITEMS.put(def.id(), item);
			BY_ITEM.put(item, def);
		}
		MinecraftMode.LOGGER.info("Registered {} raid accessories in {} sets", DEFS.size(), SETS.size());
	}

	// ------------------------------------------------------------------ definitions

	public static List<AccessorySet> sets() {
		return Collections.unmodifiableList(SETS);
	}

	public static List<AccessoryDef> all() {
		return List.copyOf(DEFS.values());
	}

	public static @Nullable AccessorySet setOf(final BossDef boss) {
		for (AccessorySet set : SETS) {
			if (set.boss() == boss) {
				return set;
			}
		}
		return null;
	}

	public static AccessoryDef def(final AccessorySet set, final AccessorySlot slot) {
		return DEFS.get(set.id() + "_" + slot.id());
	}

	public static @Nullable AccessoryDef def(final ItemStack stack) {
		return stack.isEmpty() ? null : BY_ITEM.get(stack.getItem());
	}

	public static Item item(final AccessoryDef def) {
		return ITEMS.get(def.id());
	}

	public static List<Item> items() {
		return List.copyOf(ITEMS.values());
	}

	/** Anyone of the piece's level may wear it, whatever their class. */
	public static boolean canWear(final JobData data, final AccessoryDef def) {
		return data.level() >= def.level();
	}

	// ------------------------------------------------------------------ options and drops

	/** Random options a raid piece rolls: one on Normal, two on Heroic, three on Nightmare. */
	public static int optionCount(final RaidDifficulty difficulty) {
		return 1 + difficulty.ordinal();
	}

	/** {@code count} different stats of the slot's pool, each at 60-100% of the slot's maximum for the piece's level. */
	public static GearRolls roll(final AccessoryDef def, final int count, final RandomSource random) {
		List<EngraveStat> stats = new ArrayList<>(def.slot().pool().keySet());
		List<StatLine> lines = new ArrayList<>();
		for (int i = 0; i < count && !stats.isEmpty(); i++) {
			EngraveStat stat = stats.remove(random.nextInt(stats.size()));
			float top = ArmorOptions.max(def.slot().pool().get(stat), def.level());
			lines.add(StatLine.of(stat, ArmorOptions.round(top * (0.6F + 0.4F * random.nextFloat()), top)));
		}
		return new GearRolls(lines);
	}

	public static ItemStack create(final AccessoryDef def, final int options, final RandomSource random) {
		ItemStack stack = new ItemStack(item(def));
		stack.set(ModDataComponents.GEAR_ROLLS, roll(def, options, random));
		return stack;
	}

	/** The tier every boss drops from Heroic on. */
	public static final int TOP_TIER = 3;
	/** Normal-difficulty chance per rewarded member that a boss of tier 1, 2, 3 drops a piece of its own set. */
	private static final float[] NORMAL_CHANCE = {0.15F, 0.08F, 0.015F};
	/** Heroic and Nightmare: tier 3 pieces only, from the first boss's chance up to the last boss's (Heroic Verdessa beats Normal Aethryx). */
	private static final float HEROIC_FIRST = 0.025F;
	private static final float HEROIC_LAST = 0.05F;
	private static final float NIGHTMARE_FIRST = 0.06F;
	private static final float NIGHTMARE_LAST = 0.09F;

	/** Chance per rewarded member that a clear of {@code boss} on {@code difficulty} drops an accessory. */
	public static float dropChance(final BossDef boss, final RaidDifficulty difficulty) {
		AccessorySet set = setOf(boss);
		if (set == null) {
			return 0.0F;
		}
		if (difficulty == RaidDifficulty.NORMAL) {
			return NORMAL_CHANCE[set.tier() - 1];
		}
		// harder bosses drop more often, evenly from the first set to the last
		float along = SETS.size() <= 1 ? 1.0F : SETS.indexOf(set) / (float)(SETS.size() - 1);
		return difficulty == RaidDifficulty.HEROIC ? HEROIC_FIRST + (HEROIC_LAST - HEROIC_FIRST) * along
			: NIGHTMARE_FIRST + (NIGHTMARE_LAST - NIGHTMARE_FIRST) * along;
	}

	/** The sets a clear of {@code boss} on {@code difficulty} drops from: its own on Normal (and for tier 3 bosses), else every tier 3 set. */
	public static List<AccessorySet> dropSets(final BossDef boss, final RaidDifficulty difficulty) {
		AccessorySet own = setOf(boss);
		if (own == null) {
			return List.of();
		}
		if (difficulty == RaidDifficulty.NORMAL || own.tier() == TOP_TIER) {
			return List.of(own);
		}
		return SETS.stream().filter(set -> set.tier() == TOP_TIER).toList();
	}

	/** One piece from {@link #dropSets} (random set and slot) with the difficulty's option count; empty for a boss with no set. */
	public static ItemStack raidDrop(final BossDef boss, final RaidDifficulty difficulty, final RandomSource random) {
		List<AccessorySet> sets = dropSets(boss, difficulty);
		if (sets.isEmpty()) {
			return ItemStack.EMPTY;
		}
		AccessorySet set = sets.get(random.nextInt(sets.size()));
		AccessorySlot slot = AccessorySlot.values()[random.nextInt(SLOTS)];
		return create(def(set, slot), optionCount(difficulty), random);
	}

	/** The accessory lots of a raid win: each rewarded member rolls {@link #dropChance} once. */
	public static List<ItemStack> raidDrops(final BossDef boss, final RaidDifficulty difficulty, final int members, final RandomSource random) {
		List<ItemStack> out = new ArrayList<>();
		float chance = dropChance(boss, difficulty);
		for (int i = 0; i < members; i++) {
			if (random.nextFloat() < chance) {
				ItemStack piece = raidDrop(boss, difficulty, random);
				if (!piece.isEmpty()) {
					out.add(piece);
				}
			}
		}
		return out;
	}

	/** Accessory orbs every rewarded member gets: none on Normal, one on Heroic, two on Nightmare. */
	public static int orbs(final RaidDifficulty difficulty) {
		return difficulty.ordinal();
	}

	// ------------------------------------------------------------------ inheritance

	/** Chance per rewarded member that a tier 2 or 3 boss also gives an inheritance ticket, by difficulty (Normal, Heroic, Nightmare). */
	private static final float[] TICKET_CHANCE = {0.25F, 0.35F, 0.50F};

	/** The ticket that inherits a piece of tier {@code tier} into the next tier, or null at the top tier. */
	public static @Nullable Item inheritTicket(final int tier) {
		return tier == 1 ? ModItems.ACCESSORY_INHERIT_1 : tier == 2 ? ModItems.ACCESSORY_INHERIT_2 : null;
	}

	/** The ticket a clear of {@code boss} may give: tier 2 bosses the tier 1 ticket, tier 3 bosses the tier 2 one, tier 1 bosses none. */
	public static @Nullable Item ticketDrop(final BossDef boss) {
		AccessorySet set = setOf(boss);
		return set == null || set.tier() <= 1 ? null : inheritTicket(set.tier() - 1);
	}

	public static float ticketChance(final RaidDifficulty difficulty) {
		return TICKET_CHANCE[difficulty.ordinal()];
	}

	/** What a piece of {@code def} can be inherited into: the same slot of every set one tier up (none at the top tier). */
	public static List<AccessoryDef> inheritTargets(final AccessoryDef def) {
		List<AccessoryDef> out = new ArrayList<>();
		for (AccessorySet set : SETS) {
			if (set.tier() == def.set().tier() + 1) {
				out.add(def(set, def.slot()));
			}
		}
		return out;
	}

	/** Coins for inheriting into {@code target}: half the guild weapon price of its bracket, like gear evolution. */
	public static int inheritCoins(final AccessoryDef target) {
		return Math.max(1, GearShop.bracketPrice(ItemLevels.bracket(target.level())) / 2);
	}

	/**
	 * {@code from} turned into {@code target}: the enhancement and artisan's spirit carry over, and every option keeps its stat and its
	 * share of the slot's maximum, measured again at the new level (a 90% roll stays a 90% roll). Options waiting at the blacksmith
	 * are dropped.
	 */
	public static ItemStack inherit(final ItemStack from, final AccessoryDef target) {
		AccessoryDef source = def(from);
		ItemStack result = new ItemStack(item(target));
		result.set(ModDataComponents.ENHANCEMENT, Enhancement.of(from));
		GearRolls rolls = from.get(ModDataComponents.GEAR_ROLLS);
		if (rolls != null && source != null) {
			List<StatLine> lines = new ArrayList<>();
			for (StatLine line : rolls.lines()) {
				Float top = target.slot().pool().get(line.stat());
				if (top == null) {
					lines.add(line);
					continue;
				}
				float before = ArmorOptions.max(top, source.level());
				float after = ArmorOptions.max(top, target.level());
				lines.add(StatLine.of(line.stat(), ArmorOptions.round(line.value() * after / before, after)));
			}
			result.set(ModDataComponents.GEAR_ROLLS, new GearRolls(lines));
		}
		return result;
	}

	/** Opening bid of a raid loot auction: twice the guild weapon price of the piece's bracket. */
	public static int auctionStart(final AccessoryDef def) {
		return GearShop.bracketPrice(ItemLevels.bracket(def.level())) * 2;
	}

	// ------------------------------------------------------------------ worn

	/** The five worn pieces (empty stacks for free slots), in {@link AccessorySlot} order. */
	public static List<ItemStack> worn(final Player player) {
		List<ItemStack> stored = player.getAttachedOrElse(ModAttachments.ACCESSORIES, List.of());
		List<ItemStack> out = new ArrayList<>(SLOTS);
		for (int i = 0; i < SLOTS; i++) {
			out.add(i < stored.size() ? stored.get(i) : ItemStack.EMPTY);
		}
		return out;
	}

	public static void setWorn(final Player player, final List<ItemStack> stacks) {
		List<ItemStack> copy = new ArrayList<>(SLOTS);
		for (int i = 0; i < SLOTS; i++) {
			copy.add(i < stacks.size() ? stacks.get(i).copy() : ItemStack.EMPTY);
		}
		player.setAttached(ModAttachments.ACCESSORIES, List.copyOf(copy));
	}

	/** The worn pieces {@code player} may wear (level), by slot order; others are skipped. */
	public static List<AccessoryDef> active(final Player player) {
		JobData data = JobProgression.get(player);
		List<AccessoryDef> out = new ArrayList<>();
		for (ItemStack stack : worn(player)) {
			AccessoryDef def = def(stack);
			if (def != null && canWear(data, def)) {
				out.add(def);
			}
		}
		return out;
	}

	/** Worn, usable pieces per set id. */
	public static Map<String, Integer> setCounts(final Player player) {
		Map<String, Integer> counts = new LinkedHashMap<>();
		for (AccessoryDef def : active(player)) {
			counts.merge(def.set().id(), 1, Integer::sum);
		}
		return counts;
	}

	public static @Nullable AccessorySet set(final String id) {
		for (AccessorySet set : SETS) {
			if (set.id().equals(id)) {
				return set;
			}
		}
		return null;
	}

	/** Every stat line the worn accessories add: fixed lines, rolled options and set bonuses. */
	public static List<StatLine> lines(final Player player) {
		JobData data = JobProgression.get(player);
		List<StatLine> out = new ArrayList<>();
		Map<String, Integer> counts = new LinkedHashMap<>();
		for (ItemStack stack : worn(player)) {
			AccessoryDef def = def(stack);
			if (def == null || !canWear(data, def)) {
				continue;
			}
			// enhancement raises the piece's own lines (fixed and rolled), and +10 adds damage dealt
			int enhanced = Enhancement.level(stack);
			out.add(AccessoryEnhance.scaled(def.baseLine(), enhanced));
			for (StatLine line : stack.getOrDefault(ModDataComponents.GEAR_ROLLS, GearRolls.EMPTY).lines()) {
				out.add(AccessoryEnhance.scaled(line, enhanced));
			}
			out.addAll(AccessoryEnhance.bonus(enhanced));
			counts.merge(def.set().id(), 1, Integer::sum);
		}
		counts.forEach((id, worn) -> {
			AccessorySet set = set(id);
			if (set != null) {
				set.bonuses().stream().filter(b -> worn >= b.pieces()).forEach(b -> out.addAll(b.lines()));
			}
		});
		return out;
	}

	/** Hash of the worn stacks, for the per-tick stat cache. */
	public static int hash(final Player player) {
		int hash = 0;
		for (ItemStack stack : player.getAttachedOrElse(ModAttachments.ACCESSORIES, List.<ItemStack>of())) {
			hash = hash * 31 + ItemStack.hashItemAndComponents(stack);
		}
		return hash;
	}

	/** Rolls the default options of a piece made without any. */
	static GearRolls defaultRolls(final AccessoryDef def, final RandomSource random) {
		return roll(def, DEFAULT_OPTIONS, random);
	}

	private Accessories() {
	}
}
