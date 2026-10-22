package com.minecraftmode.job.skill;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Where a skill step really hits, read from its own parameters ({@link SkillAction#area}): the class docs' look table
 * ({@code docs/SKILL-LOOKS.md}) and the game test that draws each area under its look use it. Report only: nothing in play reads it.
 *
 * @param step the action's name (nova, slash, beam, ...)
 * @param shape what is hit
 * @param at where it is centred
 * @param radius circle radius, cone range, a path's or line's half-width, a projectile's hit radius
 * @param range how far away it may be placed (aimed spots, targets), a line's or path's length
 * @param arc a cone's full horizontal arc in degrees
 * @param blast a projectile's (or rain projectile's) blast radius, 0 for none
 * @param scaled the Expansion engraving grows {@code radius} ({@link SkillContext#area})
 * @param ally it reaches allies (heals, boosts), not enemies
 * @param harms it damages (false: knockback, debuffs, taunts)
 */
public record HitArea(String step, Shape shape, Anchor at, double radius, double range, double arc, double blast, boolean scaled, boolean ally, boolean harms) {
	public enum Shape {
		CIRCLE,
		CONE,
		LINE,
		PATH,
		TARGET,
		PROJECTILE
	}

	public enum Anchor {
		/** Round the caster's feet (a cone or line: from the eyes). */
		CASTER,
		/** On the ground where the caster aims ({@link SkillContext#groundPoint}). */
		AIM,
		/** Where the caster lands (leap). */
		LANDING,
		/** On the enemy in the crosshair. */
		TARGET,
		/** Where a projectile flies. */
		FLIGHT
	}

	static HitArea circle(final String step, final Anchor at, final double radius, final double range, final boolean harms) {
		return new HitArea(step, Shape.CIRCLE, at, radius, range, 360.0, 0.0, true, false, harms);
	}

	static HitArea allies(final String step, final double radius) {
		return new HitArea(step, Shape.CIRCLE, Anchor.CASTER, radius, 0.0, 360.0, 0.0, false, true, false);
	}

	static HitArea target(final String step, final double range, final boolean harms) {
		return new HitArea(step, Shape.TARGET, Anchor.TARGET, 0.0, range, 0.0, 0.0, false, false, harms);
	}

	/** One line for the docs: "nova r6", "slash 5 / 200°", "beam 26 x 0.9", ... */
	public String describe() {
		String scale = this.scaled ? "" : " (fixed)";
		return switch (this.shape) {
			case CIRCLE -> {
				String where = switch (this.at) {
					case AIM -> " at " + num(this.range);
					case LANDING -> " on landing";
					default -> "";
				};
				yield this.step + " r" + num(this.radius) + where + scale;
			}
			case CONE -> this.step + " " + num(this.radius) + " / " + num(this.arc) + "°";
			case LINE -> this.step + " " + num(this.range) + " x ±" + num(this.radius);
			case PATH -> this.step + " " + num(this.range) + " x ±" + num(this.radius);
			case TARGET -> this.step + " 1 target ≤" + num(this.range);
			case PROJECTILE -> this.step + " hit ±" + num(this.radius) + (this.blast > 0 ? ", blast r" + num(this.blast) : "");
		};
	}

	static String num(final double v) {
		double r = Math.round(v * 100.0) / 100.0;
		return r == Math.rint(r) ? Long.toString((long)r) : String.format(Locale.ROOT, "%s", r);
	}

	/** Every area of {@code action} and of the steps inside it (repeat, delay, look), in order. */
	public static List<HitArea> of(final SkillAction action) {
		List<HitArea> out = new ArrayList<>();
		collect(action, out);
		return out;
	}

	private static void collect(final SkillAction action, final List<HitArea> out) {
		out.addAll(action.area());
		for (SkillAction part : action.parts()) {
			collect(part, out);
		}
	}

	// ------------------------------------------------------------------ where it landed (game tests)

	/**
	 * One area as it was really placed when the step ran: centre (a cone's or line's start), {@code dir} (a cone's facing, a line's
	 * or path's whole length), {@code radius} after the Expansion engraving.
	 */
	public record Placed(HitArea area, Vec3 center, Vec3 dir, double radius) {
	}

	private static volatile @Nullable Consumer<Placed> tracer;

	/** Game tests only: hears every area a skill step places from now on (null stops). */
	public static void trace(final @Nullable Consumer<Placed> listener) {
		tracer = listener;
	}

	static void placed(final HitArea area, final Vec3 center, final Vec3 dir, final double radius) {
		Consumer<Placed> listener = tracer;
		if (listener != null) {
			listener.accept(new Placed(area, center, dir, radius));
		}
	}
}
