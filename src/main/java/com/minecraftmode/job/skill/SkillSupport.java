package com.minecraftmode.job.skill;

/**
 * What a support step does for its caster's damage while it lasts ({@link SkillAction#supports}): the class docs' estimate
 * ({@code ClassComparison}) weighs it by how long it is up between casts.
 *
 * @param pct     the bonus in percent (vulnerability: 15 per effect level)
 * @param seconds how long it lasts
 * @param hits    empowered hits ({@link Kind#EMPOWER}), 1 for stealth, 0 otherwise
 */
public record SkillSupport(Kind kind, double pct, double seconds, int hits) {
	public enum Kind {
		/** Damage dealt ({@link Boost#DAMAGE}): every hit, basic or skill. */
		DAMAGE,
		CRIT_CHANCE,
		CRIT_DAMAGE,
		/** The marked target takes more from the caster ({@code CombatState.Mark}). */
		MARK,
		/** The next basic hits deal more ({@code Actions.empower}, {@code Actions.stealth}). */
		EMPOWER,
		/** The enemy takes more from everyone ({@code ModEffects.VULNERABLE}); training dummies (armor stands) never get it. */
		VULNERABLE,
		/** The weapon's other skills have {@code pct}% of their cooldown left taken off ({@code Actions.refresh}). */
		REFRESH
	}

	public static SkillSupport of(final Boost boost, final int pct, final double seconds) {
		return new SkillSupport(switch (boost) {
			case DAMAGE -> Kind.DAMAGE;
			case CRIT_CHANCE -> Kind.CRIT_CHANCE;
			case CRIT_DAMAGE -> Kind.CRIT_DAMAGE;
		}, pct, seconds, 0);
	}
}
