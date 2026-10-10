package com.minecraftmode.job.skill;

import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * A weapon skill. {@code id} is "&lt;weapon id&gt;.&lt;slug&gt;" and doubles as the cooldown key and
 * the lang key suffix.
 *
 * @param fx overrides the weapon's effect style when set
 */
public record Skill(String id, String en, String ko, SkillKind kind, int cooldownTicks, int manaCost, List<SkillAction> actions, @Nullable Fx fx) {
	public Skill {
		actions = List.copyOf(actions);
	}

	public String nameKey() {
		return "skill.minecraft_mode." + this.id;
	}

	/** Expected damage of one cast on {@code targets} grouped monsters, in multiples of the skill power (see {@link SkillAction#estimate}). */
	public double estimate(final int targets) {
		return this.actions.stream().mapToDouble(a -> a.estimate(targets)).sum();
	}

	public Skill withFx(final Fx.Kind kind, final int color) {
		return new Skill(this.id, this.en, this.ko, this.kind, this.cooldownTicks, this.manaCost, this.actions, new Fx(kind, color));
	}

	public Skill withFx(final Fx.Kind kind) {
		return new Skill(this.id, this.en, this.ko, this.kind, this.cooldownTicks, this.manaCost, this.actions, new Fx(kind, this.fx == null ? -1 : this.fx.color()));
	}

	/**
	 * Moves the caster and hurts enemies (dash strikes, leaps, steps behind a target, blink and slash): such a skill lands its caster
	 * next to monsters, so the move leaves a short guard ({@code Engage.GUARD_TICKS}) and its hits stagger ({@code Engage.stagger}).
	 */
	public boolean engages() {
		return this.actions.stream().anyMatch(SkillAction::moves) && this.actions.stream().anyMatch(SkillAction::damages);
	}

	void cast(final SkillContext ctx) {
		for (SkillAction action : this.actions) {
			if (action.isModifier()) {
				action.run(ctx);
			}
		}
		for (SkillAction action : this.actions) {
			if (!action.isModifier()) {
				action.run(ctx);
			}
		}
	}
}
