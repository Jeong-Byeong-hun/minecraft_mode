package com.minecraftmode.consumable;

import com.minecraftmode.job.engrave.EngraveStat;
import com.minecraftmode.job.gear.StatLine;
import com.minecraftmode.registry.ModEffects;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;

/**
 * The consumable buffs (fury, ironskin, ...) are marker effects; while one is active its stat lines,
 * times the effect level, join the player's totals in {@code GearStats} like gear does.
 */
public final class BuffEffects {
	private static final Map<Holder<MobEffect>, List<StatLine>> PER_LEVEL = new LinkedHashMap<>();

	static {
		PER_LEVEL.put(ModEffects.FURY, List.of(StatLine.of(EngraveStat.BASIC_DAMAGE, 5), StatLine.of(EngraveStat.SHOT_DAMAGE, 5)));
		PER_LEVEL.put(ModEffects.IRONSKIN, List.of(StatLine.of(EngraveStat.DAMAGE_REDUCTION, 4)));
		PER_LEVEL.put(ModEffects.PRECISION, List.of(StatLine.of(EngraveStat.CRIT_CHANCE, 5)));
		PER_LEVEL.put(ModEffects.ARCANA, List.of(StatLine.of(EngraveStat.SKILL_DAMAGE, 8)));
		PER_LEVEL.put(ModEffects.FOCUS, List.of(StatLine.of(EngraveStat.COOLDOWN, 6)));
		PER_LEVEL.put(ModEffects.CLARITY, List.of(StatLine.of(EngraveStat.MANA_REGEN, 1)));
		PER_LEVEL.put(ModEffects.REJUVENATION, List.of(StatLine.of(EngraveStat.HEALTH_REGEN, 1)));
		PER_LEVEL.put(ModEffects.FORTUNE, List.of(StatLine.of(EngraveStat.ITEM_FIND, 8), StatLine.of(EngraveStat.GOLD_FIND, 4)));
		PER_LEVEL.put(ModEffects.WISDOM, List.of(StatLine.of(EngraveStat.EXP_BONUS, 15)));
		PER_LEVEL.put(ModEffects.SLAYER, List.of(StatLine.of(EngraveStat.BOSS_DAMAGE, 8)));
	}

	/** Stat lines of {@code effect} at {@code amplifier} (empty for other effects). */
	public static List<StatLine> lines(final Holder<MobEffect> effect, final int amplifier) {
		List<StatLine> base = PER_LEVEL.get(effect);
		if (base == null) {
			return List.of();
		}
		List<StatLine> out = new ArrayList<>(base.size());
		for (StatLine line : base) {
			out.add(StatLine.of(line.stat(), line.value() * (amplifier + 1)));
		}
		return out;
	}

	/** Every buff line active on {@code player}. */
	public static List<StatLine> active(final Player player) {
		List<StatLine> out = new ArrayList<>();
		for (MobEffectInstance instance : player.getActiveEffects()) {
			out.addAll(lines(instance.getEffect(), instance.getAmplifier()));
		}
		return out;
	}

	/** A number that changes whenever the player's buffs (or their levels) change. */
	public static int fingerprint(final Player player) {
		int hash = 0;
		for (MobEffectInstance instance : player.getActiveEffects()) {
			if (PER_LEVEL.containsKey(instance.getEffect())) {
				hash += instance.getEffect().hashCode() * 31 + instance.getAmplifier();
			}
		}
		return hash;
	}

	public static boolean isBuff(final Holder<MobEffect> effect) {
		return PER_LEVEL.containsKey(effect);
	}

	public static List<Holder<MobEffect>> all() {
		return List.copyOf(PER_LEVEL.keySet());
	}

	private BuffEffects() {
	}
}
