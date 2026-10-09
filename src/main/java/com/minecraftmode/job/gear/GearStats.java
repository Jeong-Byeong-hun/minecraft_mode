package com.minecraftmode.job.gear;

import com.minecraftmode.consumable.BuffEffects;
import com.minecraftmode.enhance.Enhancement;
import com.minecraftmode.job.JobData;
import com.minecraftmode.job.JobProgression;
import com.minecraftmode.job.engrave.EngraveTotals;
import com.minecraftmode.job.engrave.Engravings;
import com.minecraftmode.job.weapon.JobWeapons;
import com.minecraftmode.job.weapon.WeaponDef;
import com.minecraftmode.progress.CollectionBonuses;
import com.minecraftmode.progress.Progress;
import com.minecraftmode.registry.ModDataComponents;
import com.minecraftmode.talent.Talents;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
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

	private static EngraveTotals compute(final Player player) {
		JobData data = JobProgression.get(player);
		EngraveTotals.Builder builder = EngraveTotals.builder();
		ItemStack main = player.getMainHandItem();
		WeaponDef weapon = JobWeapons.def(main);
		if (weapon != null && JobWeapons.isActive(data, weapon)) {
			builder.addAll(JobWeapons.engravings(main).resolved());
			ClassGear gear = ClassGear.of(main);
			if (gear != null) {
				for (StatLine line : Enhancement.lines(gear, Enhancement.level(main))) {
					add(builder, line);
				}
			}
		}
		Map<String, Integer> setCounts = new HashMap<>();
		for (GearSlot slot : GearSlot.ARMOR) {
			ItemStack stack = player.getItemBySlot(slot.equipmentSlot());
			ArmorPieceDef piece = ClassArmor.def(stack);
			if (piece == null || !GearRules.canUse(data, piece.job(), piece.level())) {
				continue;
			}
			add(builder, piece.baseOption());
			for (StatLine line : stack.getOrDefault(ModDataComponents.GEAR_ROLLS, GearRolls.EMPTY).lines()) {
				add(builder, line);
			}
			builder.addAll(stack.getOrDefault(ModDataComponents.ENGRAVINGS, Engravings.EMPTY).resolved());
			ClassGear gear = ClassGear.of(stack);
			if (gear != null) {
				for (StatLine line : Enhancement.lines(gear, Enhancement.level(stack))) {
					add(builder, line);
				}
			}
			setCounts.merge(piece.set().id(), 1, Integer::sum);
		}
		for (Map.Entry<String, Integer> entry : setCounts.entrySet()) {
			ArmorSetDef set = ClassArmor.set(entry.getKey());
			for (StatLine line : activeSetLines(set, entry.getValue())) {
				add(builder, line);
			}
		}
		for (StatLine line : LevelRewards.of(data)) {
			add(builder, line);
		}
		for (StatLine line : ClassPassives.of(data)) {
			add(builder, line);
		}
		for (StatLine line : BuffEffects.active(player)) {
			add(builder, line);
		}
		for (StatLine line : Talents.lines(player)) {
			add(builder, line);
		}
		for (StatLine line : CollectionBonuses.lines(Progress.get(player))) {
			add(builder, line);
		}
		return builder.build();
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

	private static void add(final EngraveTotals.Builder builder, final StatLine line) {
		builder.add(line.stat(), line.value());
	}

	private GearStats() {
	}
}
