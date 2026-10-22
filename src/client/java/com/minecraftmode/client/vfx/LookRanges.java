package com.minecraftmode.client.vfx;

import com.minecraftmode.job.skill.Actions;
import com.minecraftmode.job.skill.HitArea;
import com.minecraftmode.job.skill.Skill;
import com.minecraftmode.job.skill.SkillAction;
import com.minecraftmode.job.skill.SkillLook;
import com.minecraftmode.job.weapon.JobWeapons;
import com.minecraftmode.job.weapon.WeaponDef;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Compares where each tier 4 skill's looks draw their danger edge ({@link LookEdges}, sized by the content's {@link Actions.LookAction})
 * with where the skill really hits ({@link HitArea}, from the actions' own numbers): {@code docs/SKILL-LOOKS.md} and
 * {@code JobClientGameTest.checkLookRanges}. A drawn edge matches within {@link #TOLERANCE}.
 */
public final class LookRanges {
	/** How far a drawn edge may sit from the real one (a share of it). */
	public static final double TOLERANCE = 0.10;
	/** A beam may be drawn up to this much wider than its hit width (half a body: the beam hits a hitbox that far off). */
	private static final double BEAM_BODY = 0.45;
	/** A projectile's body may be this much wider than its hit radius before it counts as wider (glow round a thin shot). */
	private static final double PROJECTILE_SLACK = 0.15;

	public enum Verdict {
		/** The edge sits on the real area. */
		MATCH,
		/** The edge is off by more than {@link #TOLERANCE}. */
		MISMATCH,
		/** A projectile body wider than what it touches (its glow or its figure; the blast is drawn on its own). */
		WIDER,
		/** No danger edge to compare (a target, a path, an aura). */
		NO_EDGE,
		/** The look has no {@link LookEdges} entry. */
		UNAUDITED
	}

	/** One drawn look of a skill and how its edge compares. */
	public record Drawn(SkillLook look, LookEdges.@Nullable Edge edge, String drawn, @Nullable HitArea matched, double error, Verdict verdict) {
	}

	public record Row(WeaponDef weapon, Skill skill, List<HitArea> areas, List<Drawn> drawn, List<HitArea> undrawn) {
		public boolean mismatched() {
			return this.drawn.stream().anyMatch(d -> d.verdict() == Verdict.MISMATCH || d.verdict() == Verdict.UNAUDITED);
		}
	}

	/** Every tier 4 skill, class by class in content order. */
	public static List<Row> tier4() {
		List<Row> rows = new ArrayList<>();
		for (WeaponDef weapon : JobWeapons.all()) {
			if (weapon.tier() != 4) {
				continue;
			}
			for (Skill skill : weapon.skills()) {
				rows.add(of(weapon, skill));
			}
		}
		return rows;
	}

	public static Row of(final WeaponDef weapon, final Skill skill) {
		List<HitArea> areas = new ArrayList<>();
		for (SkillAction action : skill.actions()) {
			areas.addAll(HitArea.of(action));
		}
		List<Drawn> drawn = new ArrayList<>();
		for (SkillAction action : skill.actions()) {
			walk(action, areas, drawn);
		}
		List<HitArea> undrawn = new ArrayList<>();
		for (HitArea area : areas) {
			boolean shown = drawn.stream().anyMatch(d -> d.verdict() != Verdict.MISMATCH && covers(d.matched(), area));
			// a path is shown by any look drawn along the caster's path
			shown |= area.shape() == HitArea.Shape.PATH && drawn.stream().anyMatch(d -> d.look().hold() == SkillLook.Hold.TRAIL);
			// shots draw themselves; single targets are shown by the look on them, if any
			if (!shown && area.shape() != HitArea.Shape.PROJECTILE && area.shape() != HitArea.Shape.TARGET && (area.harms() || area.ally())) {
				undrawn.add(area);
			}
		}
		return new Row(weapon, skill, List.copyOf(areas), List.copyOf(drawn), List.copyOf(undrawn));
	}

	private static void walk(final SkillAction action, final List<HitArea> skillAreas, final List<Drawn> out) {
		if (action instanceof Actions.LookAction look && look.look() != SkillLook.NONE) {
			out.add(judge(look, HitArea.of(look), skillAreas));
		}
		if (action instanceof Actions.ProjectileAction shot) {
			HitArea flight = shot.area().getFirst();
			if (shot.look() != SkillLook.NONE) {
				out.add(projectile(shot.look(), flight));
			}
			Actions.ImpactLook impact = shot.impactLook();
			if (impact != null) {
				out.add(impact(impact, flight));
			}
		}
		for (SkillAction part : action.parts()) {
			walk(part, skillAreas, out);
		}
	}

	private static Drawn judge(final Actions.LookAction look, final List<HitArea> wrapped, final List<HitArea> skillAreas) {
		LookEdges.Edge edge = LookEdges.of(look.look());
		if (edge == null) {
			return new Drawn(look.look(), null, "?", null, 0.0, Verdict.UNAUDITED);
		}
		boolean scaled = !look.isUnscaled();
		return switch (edge.kind()) {
			case RING -> {
				double r = edge.k() * look.drawnSize();
				yield best(look.look(), edge, "ring r" + HitAreaText.num(r) + (scaled ? "" : " (fixed)"), wrapped, skillAreas,
					a -> circle(a) ? ringError(r, scaled, a) : Double.NaN);
			}
			case CONE -> {
				boolean placed = look.place() == Actions.LookAction.Place.CONE;
				double range = edge.k() * (placed ? look.range() : look.drawnSize());
				double arc = placed ? look.arc() : 360.0;
				yield best(look.look(), edge, "cone " + HitAreaText.num(range) + " / " + HitAreaText.num(arc) + "°", wrapped, skillAreas, a -> {
					if (a.shape() == HitArea.Shape.CONE) {
						return Math.max(rel(range, a.radius()), rel(arc, a.arc()));
					}
					return arc >= 359.0 && circle(a) ? rel(range, a.radius()) : Double.NaN;
				});
			}
			case BEAM -> {
				double length = look.place() == Actions.LookAction.Place.BEAM ? look.range() : look.drawnSize();
				double w = edge.k();
				yield best(look.look(), edge, "beam " + HitAreaText.num(length) + " x ±" + HitAreaText.num(w), wrapped, skillAreas, a -> {
					if (a.shape() != HitArea.Shape.LINE) {
						return Double.NaN;
					}
					double width = w < a.radius() ? rel(w, a.radius()) : w > a.radius() + BEAM_BODY ? rel(w, a.radius() + BEAM_BODY) : 0.0;
					return Math.max(rel(length, a.range()), width);
				});
			}
			case BODY -> {
				HitArea target = first(wrapped, skillAreas, a -> a.shape() == HitArea.Shape.TARGET);
				yield new Drawn(look.look(), edge, "on the target", target, 0.0, Verdict.NO_EDGE);
			}
			case TRAIL -> {
				HitArea path = first(wrapped, skillAreas, a -> a.shape() == HitArea.Shape.PATH);
				yield new Drawn(look.look(), edge, "along the path", path, 0.0, Verdict.NO_EDGE);
			}
			case AURA, PROJECTILE -> new Drawn(look.look(), edge, "on the caster", null, 0.0, Verdict.NO_EDGE);
		};
	}

	private static Drawn projectile(final SkillLook look, final HitArea flight) {
		LookEdges.Edge edge = LookEdges.of(look);
		if (edge == null) {
			return new Drawn(look, null, "?", flight, 0.0, Verdict.UNAUDITED);
		}
		double r = edge.k();
		double error = rel(r, flight.radius());
		Verdict verdict = error <= TOLERANCE || Math.abs(r - flight.radius()) <= PROJECTILE_SLACK ? Verdict.MATCH : r > flight.radius() ? Verdict.WIDER : Verdict.MISMATCH;
		return new Drawn(look, edge, "shot ±" + HitAreaText.num(r), flight, error, verdict);
	}

	private static Drawn impact(final Actions.ImpactLook impact, final HitArea flight) {
		LookEdges.Edge edge = LookEdges.of(impact.look());
		if (edge == null) {
			return new Drawn(impact.look(), null, "?", flight, 0.0, Verdict.UNAUDITED);
		}
		double r = edge.k() * impact.size();
		String text = "burst r" + HitAreaText.num(r) + (impact.scaled() ? "" : " (fixed)");
		if (flight.blast() <= 0.0) {
			return new Drawn(impact.look(), edge, text, flight, 0.0, Verdict.NO_EDGE);
		}
		// a blast is never grown by the Expansion engraving
		double error = impact.scaled() ? Math.max(rel(r, flight.blast()), TOLERANCE * 2) : rel(r, flight.blast());
		return new Drawn(impact.look(), edge, text, flight, error, error <= TOLERANCE ? Verdict.MATCH : Verdict.MISMATCH);
	}

	private interface Error {
		double of(HitArea area);
	}

	/** The area the edge fits best: one of the steps the look wraps when one fits, else any of the skill's. */
	private static Drawn best(
		final SkillLook look, final LookEdges.Edge edge, final String text, final List<HitArea> wrapped, final List<HitArea> skillAreas, final Error error
	) {
		HitArea pick = pick(wrapped, error);
		if (pick == null || error.of(pick) > TOLERANCE) {
			HitArea any = pick(skillAreas, error);
			if (any != null && (pick == null || error.of(any) < error.of(pick))) {
				pick = any;
			}
		}
		if (pick == null) {
			// nothing of that shape anywhere in the skill: the drawing marks no area (a blink's ripple)
			return new Drawn(look, edge, text + ", no area", null, 0.0, Verdict.NO_EDGE);
		}
		double e = error.of(pick);
		return new Drawn(look, edge, text, pick, e, e <= TOLERANCE + 1.0E-9 ? Verdict.MATCH : Verdict.MISMATCH);
	}

	private static @Nullable HitArea pick(final List<HitArea> areas, final Error error) {
		return areas.stream().filter(a -> !Double.isNaN(error.of(a))).min(Comparator.comparingDouble(error::of)).orElse(null);
	}

	private static @Nullable HitArea first(final List<HitArea> wrapped, final List<HitArea> skillAreas, final java.util.function.Predicate<HitArea> test) {
		return wrapped.stream().filter(test).findFirst().orElse(skillAreas.stream().filter(test).findFirst().orElse(null));
	}

	/**
	 * Whether the drawn edge fitted to {@code matched} also shows {@code area}: the same area, or a circle round the same spot that is as
	 * wide (a debuff and a nova of one radius, an ally boost and heal); scattered strikes count as shown by a ring out to where they reach.
	 */
	private static boolean covers(final @Nullable HitArea matched, final HitArea area) {
		if (matched == null) {
			return false;
		}
		if (matched.equals(area)) {
			return true;
		}
		// allies are shown by a ring of their radius whether or not the ring grows with the Expansion engraving (they meet without it)
		if (!circle(matched) || !circle(area) || matched.scaled() != area.scaled() && !area.ally() || !sameSpot(matched.at(), area.at())) {
			return false;
		}
		double reach = area.step().equals("strike") ? area.radius() * (1.0 + Math.sqrt(2.0) / 3.0) : area.radius();
		return matched.radius() >= area.radius() * (1.0 - TOLERANCE) && matched.radius() <= reach * (1.0 + TOLERANCE);
	}

	private static boolean sameSpot(final HitArea.Anchor a, final HitArea.Anchor b) {
		boolean feetA = a == HitArea.Anchor.CASTER || a == HitArea.Anchor.LANDING;
		boolean feetB = b == HitArea.Anchor.CASTER || b == HitArea.Anchor.LANDING;
		return a == b || feetA && feetB;
	}

	private static boolean circle(final HitArea a) {
		return a.shape() == HitArea.Shape.CIRCLE || a.shape() == HitArea.Shape.CONE && a.arc() >= 359.0;
	}

	/** A ring against a circle; an edge that grows with the Expansion engraving while the area does not (or the other way) never fits. */
	private static double ringError(final double r, final boolean scaled, final HitArea a) {
		double e = rel(r, a.radius());
		return scaled == a.scaled() ? e : Math.max(e, TOLERANCE * 2);
	}

	private static double rel(final double drawn, final double real) {
		return real <= 0.0 ? (drawn <= 0.0 ? 0.0 : 1.0) : Math.abs(drawn - real) / real;
	}

	/** Number formatting shared with {@link HitArea#describe}. */
	public static final class HitAreaText {
		public static String num(final double v) {
			double r = Math.round(v * 100.0) / 100.0;
			return r == Math.rint(r) ? Long.toString((long)r) : Double.toString(r);
		}

		private HitAreaText() {
		}
	}

	private LookRanges() {
	}
}
