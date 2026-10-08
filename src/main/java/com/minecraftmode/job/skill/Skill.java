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

	public Skill withFx(final Fx.Kind kind, final int color) {
		return new Skill(this.id, this.en, this.ko, this.kind, this.cooldownTicks, this.manaCost, this.actions, new Fx(kind, color));
	}

	public Skill withFx(final Fx.Kind kind) {
		return new Skill(this.id, this.en, this.ko, this.kind, this.cooldownTicks, this.manaCost, this.actions, new Fx(kind, this.fx == null ? -1 : this.fx.color()));
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
