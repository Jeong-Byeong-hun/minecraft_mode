package com.minecraftmode.job.gear;

import com.minecraftmode.companion.Companions;
import com.minecraftmode.consumable.BuffEffects;
import com.minecraftmode.enhance.Enhancement;
import com.minecraftmode.job.JobData;
import com.minecraftmode.job.JobProgression;
import com.minecraftmode.job.Paragon;
import com.minecraftmode.job.engrave.EngraveStat;
import com.minecraftmode.job.engrave.EngraveTotals;
import com.minecraftmode.job.engrave.Engraving;
import com.minecraftmode.job.engrave.Engravings;
import com.minecraftmode.job.weapon.JobWeapons;
import com.minecraftmode.job.weapon.WeaponDef;
import com.minecraftmode.progress.CollectionBonuses;
import com.minecraftmode.progress.Progress;
import com.minecraftmode.registry.ModDataComponents;
import com.minecraftmode.talent.Talents;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Everything a player's gear and level add up to: engravings of the active class weapon, the
 * options and engravings of class armor they may wear, enhancement, set bonuses, level rewards, stat passives, buffs, talents and
 * codex collection bonuses. Combat,
 * stats and skills all read this one total. Cached per player for the current game tick.
 */
public final class GearStats {
	/** One cache per side: in single player the client and server threads both ask. */
	private static final Map<Player, Cached> SERVER_CACHE = new WeakHashMap<>();
	private static final Map<Player, Cached> CLIENT_CACHE = new WeakHashMap<>();

	/** Totals for one tick and one set of worn gear and class data (changes within a tick recompute). */
	private record Cached(long time, JobData data, int gear, EngraveTotals totals) {
	}

	public static EngraveTotals of(final Player player) {
		Map<Player, Cached> cache = player.level().isClientSide() ? CLIENT_CACHE : SERVER_CACHE;
		long time = player.level().getGameTime();
		JobData data = JobProgression.get(player);
		int gear = gearHash(player);
		Cached cached = cache.get(player);
		if (cached != null && cached.time == time && cached.gear == gear && cached.data.equals(data)) {
			return cached.totals;
		}
		EngraveTotals totals = compute(player);
		cache.put(player, new Cached(time, data, gear, totals));
		return totals;
	}

	private static int gearHash(final Player player) {
		int hash = ItemStack.hashItemAndComponents(player.getMainHandItem()) * 31 + BuffEffects.fingerprint(player);
		for (GearSlot slot : GearSlot.ARMOR) {
			hash = hash * 31 + ItemStack.hashItemAndComponents(player.getItemBySlot(slot.equipmentSlot()));
		}
		return hash;
	}

	/** Drops the cached total, e.g. right after equipment changed in the same tick. */
	public static void invalidate(final Player player) {
		(player.level().isClientSide() ? CLIENT_CACHE : SERVER_CACHE).remove(player);
	}

	/** Where a stat line comes from (the character screen lists totals by source). */
	public enum Source {
		WEAPON, ARMOR, ENHANCEMENT, SET, LEVEL, PASSIVE, BUFF, TALENT, COLLECTION, PARAGON, COMPANION;

		public String key() {
			return "screen.minecraft_mode.character.source." + this.name().toLowerCase(Locale.ROOT);
		}
	}

	@FunctionalInterface
	private interface Sink {
		void add(Source source, EngraveStat stat, float value);
	}

	private static EngraveTotals compute(final Player player) {
		EngraveTotals.Builder builder = EngraveTotals.builder();
		collect(player, (source, stat, value) -> builder.add(stat, value));
		return builder.build();
	}

	/** Every stat's uncapped value per source (only sources that add something); the caps apply to the sum ({@link #of}). */
	public static Map<EngraveStat, Map<Source, Float>> breakdown(final Player player) {
		Map<EngraveStat, Map<Source, Float>> out = new EnumMap<>(EngraveStat.class);
		collect(player, (source, stat, value) -> {
			if (value != 0.0F) {
				out.computeIfAbsent(stat, s -> new EnumMap<>(Source.class)).merge(source, value, Float::sum);
			}
		});
		return out;
	}

	private static void collect(final Player player, final Sink sink) {
		JobData data = JobProgression.get(player);
		ItemStack main = player.getMainHandItem();
		WeaponDef weapon = JobWeapons.def(main);
		if (weapon != null && JobWeapons.isActive(data, weapon)) {
			for (Engraving e : JobWeapons.engravings(main).resolved()) {
				sink.add(Source.WEAPON, e.stat(), e.value());
			}
			ClassGear gear = ClassGear.of(main);
			if (gear != null) {
				lines(sink, Source.ENHANCEMENT, Enhancement.lines(gear, Enhancement.level(main), Enhancement.of(main).awaken()));
			}
		}
		Map<String, Integer> setCounts = new HashMap<>();
		for (GearSlot slot : GearSlot.ARMOR) {
			ItemStack stack = player.getItemBySlot(slot.equipmentSlot());
			ArmorPieceDef piece = ClassArmor.def(stack);
			if (piece == null || !GearRules.canUse(data, piece.job(), piece.level())) {
				continue;
			}
			lines(sink, Source.ARMOR, List.of(piece.baseOption()));
			lines(sink, Source.ARMOR, piece.defenseLines());
			lines(sink, Source.ARMOR, stack.getOrDefault(ModDataComponents.GEAR_ROLLS, GearRolls.EMPTY).lines());
			for (Engraving e : stack.getOrDefault(ModDataComponents.ENGRAVINGS, Engravings.EMPTY).resolved()) {
				sink.add(Source.ARMOR, e.stat(), e.value());
			}
			ClassGear gear = ClassGear.of(stack);
			if (gear != null) {
				lines(sink, Source.ENHANCEMENT, Enhancement.lines(gear, Enhancement.level(stack), Enhancement.of(stack).awaken()));
			}
			setCounts.merge(piece.set().id(), 1, Integer::sum);
		}
		for (Map.Entry<String, Integer> entry : setCounts.entrySet()) {
			lines(sink, Source.SET, activeSetLines(ClassArmor.set(entry.getKey()), entry.getValue()));
		}
		lines(sink, Source.LEVEL, LevelRewards.of(data));
		lines(sink, Source.PASSIVE, ClassPassives.of(data));
		lines(sink, Source.BUFF, BuffEffects.active(player));
		lines(sink, Source.TALENT, Talents.lines(player));
		lines(sink, Source.COLLECTION, CollectionBonuses.lines(Progress.get(player)));
		lines(sink, Source.PARAGON, Paragon.lines(player));
		lines(sink, Source.COMPANION, Companions.lines(player));
	}

	private static void lines(final Sink sink, final Source source, final Iterable<StatLine> lines) {
		for (StatLine line : lines) {
			sink.add(source, line.stat(), line.value());
		}
	}

	/** Bonus lines of {@code set} that {@code worn} pieces unlock. */
	public static List<StatLine> activeSetLines(final ArmorSetDef set, final int worn) {
		return set.bonuses().stream().filter(b -> worn >= b.pieces()).flatMap(b -> b.lines().stream()).toList();
	}

	/** Worn, usable pieces per set id (for tooltips and the class screen). */
	public static Map<String, Integer> setCounts(final Player player) {
		JobData data = JobProgression.get(player);
		Map<String, Integer> counts = new LinkedHashMap<>();
		for (GearSlot slot : GearSlot.ARMOR) {
			ArmorPieceDef piece = ClassArmor.def(player.getItemBySlot(slot.equipmentSlot()));
			if (piece != null && GearRules.canUse(data, piece.job(), piece.level())) {
				counts.merge(piece.set().id(), 1, Integer::sum);
			}
		}
		return counts;
	}

	private GearStats() {
	}
}
