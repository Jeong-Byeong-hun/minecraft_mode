package com.minecraftmode.job.skill;

import com.minecraftmode.job.engrave.EngraveStat;
import com.minecraftmode.registry.ModEffects;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;

/**
 * The damage buffs of support skills ({@link Actions#boost}, {@link Actions#allyBoost}). Each is a marker effect whose stat line, times
 * the effect level, joins the player's totals like gear ({@code BuffEffects}); they are not the consumables' effects, so a skill boost
 * and a 10-minute consumable both count. The same boost from two skills does not stack: the higher level wins, as with any effect.
 * They replaced the guard, counter and evasion stances and the Resistance/Strength buffs in 0.5.20: players were tough enough that
 * those barely mattered (and Strength only ever reached melee basic hits).
 */
public enum Boost {
	/** Every hit: basic attacks, shots, skills and summons. */
	DAMAGE(ModEffects.VALOR, EngraveStat.DAMAGE, 5),
	CRIT_CHANCE(ModEffects.INSIGHT, EngraveStat.CRIT_CHANCE, 5),
	CRIT_DAMAGE(ModEffects.FEROCITY, EngraveStat.CRIT_DAMAGE, 10);

	private final Holder<MobEffect> effect;
	private final EngraveStat stat;
	private final int step;

	Boost(final Holder<MobEffect> effect, final EngraveStat stat, final int step) {
		this.effect = effect;
		this.stat = stat;
		this.step = step;
	}

	public Holder<MobEffect> effect() {
		return this.effect;
	}

	public EngraveStat stat() {
		return this.stat;
	}

	/** Percent per effect level. */
	public int step() {
		return this.step;
	}

	/** The effect amplifier that gives {@code pct}; refuses values between the steps, which the effect could not show. */
	public int amplifier(final int pct) {
		if (pct < this.step || pct % this.step != 0) {
			throw new IllegalArgumentException(this + " boosts go in steps of " + this.step + "%, not " + pct + "%");
		}
		return pct / this.step - 1;
	}
}
