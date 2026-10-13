package com.minecraftmode.accessory;

import static com.minecraftmode.job.engrave.EngraveStat.*;

import com.minecraftmode.MinecraftMode;
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
 * {@link AccessoryMenu}) and kept per character in {@link ModAttachments#ACCESSORIES}. Every raid boss has a set of five that only it
 * drops, in its raid ({@code Raids.onBossDefeated} adds the lots), so accessories are the raid-only reward: no shop, lair, dungeon or
 * named monster gives them. A piece needs the boss's level, has its slot's fixed line and 1-3 random options (Normal, Heroic,
 * Nightmare), and the set pays 2-, 3- and 5-piece bonuses. Everything adds up in {@code GearStats} under its own source.
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
		// one set per raid boss, in the bosses' level order (the 5-piece bonus always adds damage dealt)
		set(RaidBosses.VERDESSA, "Thornbloom", "가시꽃", 0x7FA05A, 0xE0457B, bonus(2, l(HEALTH_REGEN, 0.5F)), bonus(3, l(THORNS, 10)),
			bonus(5, l(MAX_HEALTH, 2), l(DAMAGE, 2)));
		set(RaidBosses.ARACHNE, "Silkweaver", "비단거미", 0xC8C8D0, 0x6A2A8A, bonus(2, l(POISON, 1)), bonus(3, l(CRIT_CHANCE, 2)),
			bonus(5, l(EXECUTE, 8), l(DAMAGE, 3)));
		set(RaidBosses.GORVATH, "Peakstone", "산정석", 0x8A8478, 0x6FB7E0, bonus(2, l(MAX_HEALTH, 3)), bonus(3, l(KNOCKBACK_RES, 20)),
			bonus(5, l(DAMAGE_REDUCTION, 4), l(DAMAGE, 3)));
		set(RaidBosses.NEFREKHET, "Pharaoh's", "파라오의", 0xE0B23A, 0x2A6FD0, bonus(2, l(MAGIC_DEFENSE, 3)), bonus(3, l(SLOW, 1)),
			bonus(5, l(BOSS_DAMAGE, 8), l(DAMAGE, 4)));
		set(RaidBosses.KRAKEN, "Abyssal", "심해의", 0x3A6A8A, 0x3AE0C8, bonus(2, l(LIFESTEAL, 2)), bonus(3, l(SKILL_AREA, 8)),
			bonus(5, l(MAX_MANA, 20), l(DAMAGE, 4)));
		set(RaidBosses.IGNIS, "Phoenix", "불사조", 0xE07A2A, 0xFFD23A, bonus(2, l(BURN, 1)), bonus(3, l(HEALTH_REGEN, 1.5F)),
			bonus(5, l(LAST_STAND, 10), l(DAMAGE, 5)));
		set(RaidBosses.RAIJIN, "Thunderdrum", "뇌고", 0x5A6AB0, 0xFFF04A, bonus(2, l(ATTACK_SPEED, 4)), bonus(3, l(CHAIN_LIGHTNING, 8)),
			bonus(5, l(CRIT_DAMAGE, 15), l(DAMAGE, 5)));
		set(RaidBosses.MALACHAR, "Lich King's", "리치 왕의", 0x6A5A7A, 0x8AFF6A, bonus(2, l(MANA_REGEN, 2)), bonus(3, l(COOLDOWN, 5)),
			bonus(5, l(SKILL_DAMAGE, 10), l(DAMAGE, 6)));
		set(RaidBosses.AETHRYX, "Voidscale", "공허비늘", 0x2A2440, 0xC07AFF, bonus(2, l(BOSS_DAMAGE, 6)), bonus(3, l(CRIT_CHANCE, 4)),
			bonus(5, l(DAMAGE, 8), l(PROTECTION, 3)));
	}

	private static void set(final BossDef boss, final String adjEn, final String adjKo, final int metal, final int gem, final ArmorSetDef.SetBonus... bonuses) {
		AccessorySet set = new AccessorySet(boss, adjEn, adjKo, metal, gem, List.of(bonuses));
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

	/** One piece of {@code boss}'s set (random slot) as a raid lot, with the difficulty's option count; empty for a boss with no set. */
	public static ItemStack raidDrop(final BossDef boss, final RaidDifficulty difficulty, final RandomSource random) {
		AccessorySet set = setOf(boss);
		if (set == null) {
			return ItemStack.EMPTY;
		}
		AccessorySlot slot = AccessorySlot.values()[random.nextInt(SLOTS)];
		return create(def(set, slot), optionCount(difficulty), random);
	}

	/** Accessory lots of a raid win: one per rewarded member plus the difficulty's extra lots (solo: 1 / 2 / 3). */
	public static int raidLots(final int members, final RaidDifficulty difficulty) {
		return members <= 0 ? 0 : members + difficulty.extraLots;
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
			out.add(def.baseLine());
			out.addAll(stack.getOrDefault(ModDataComponents.GEAR_ROLLS, GearRolls.EMPTY).lines());
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
