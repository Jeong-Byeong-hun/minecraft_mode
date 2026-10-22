package com.minecraftmode.test;

import com.minecraftmode.job.skill.HitArea;
import com.minecraftmode.job.skill.Skill;
import com.minecraftmode.job.skill.SkillAction;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.gizmos.SimpleGizmoCollector;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Test only: records every area the skill steps really place ({@link HitArea#trace}) and draws them as thin dashed gizmo lines on top
 * of everything (one fixed lime/magenta dash, readable on any look), so a top-down screenshot shows the drawn look and the real area
 * together ({@code JobClientGameTest} {@code look_area_*}). The shots are taken under {@code tick freeze}, when the client keeps
 * drawing the last tick's gizmos and collects no new ones, so {@link #show} puts its own lines in that list directly.
 */
final class LookAreaOverlay {
	private static final int LIME = 0xFF3CFF3C;
	private static final int MAGENTA = 0xFFFF2CF0;
	private static final float WIDTH = 3.0F;
	/** Dashes per full circle. */
	private static final int SEGMENTS = 72;
	/** How high over the floor (or the caster's feet) the lines lie. */
	private static final double LIFT = 0.25;
	private static final List<HitArea.Placed> PLACED = new CopyOnWriteArrayList<>();

	/** Starts recording the areas of {@code skill} afresh (steps of a skill cast before, still landing, are left out). */
	static void record(final Skill skill) {
		List<HitArea> own = new ArrayList<>();
		for (SkillAction action : skill.actions()) {
			own.addAll(HitArea.of(action));
		}
		boolean blasts = own.stream().anyMatch(a -> a.blast() > 0.0);
		PLACED.clear();
		HitArea.trace(p -> {
			if (own.contains(p.area()) || blasts && p.area().step().equals("blast")) {
				PLACED.add(p);
			}
		});
	}

	/** Stops recording. */
	static void stop() {
		HitArea.trace(null);
		PLACED.clear();
	}

	/** Client thread: shows the recorded areas until the next game tick (or {@code on} false). */
	static void show(final Minecraft minecraft, final boolean on) {
		SimpleGizmoCollector lines = new SimpleGizmoCollector();
		if (on) {
			try (Gizmos.TemporaryCollection ignored = Gizmos.withCollector(lines)) {
				draw();
			}
		}
		try {
			Field latest = Minecraft.class.getDeclaredField("drainedLatestTickGizmos");
			latest.setAccessible(true);
			latest.set(minecraft, lines.drainGizmos());
		} catch (ReflectiveOperationException e) {
			throw new AssertionError("cannot show the area overlay", e);
		}
	}

	/** Every area recorded so far that has a size on the ground (single targets left out). */
	static List<HitArea.Placed> areas() {
		List<HitArea.Placed> out = new ArrayList<>();
		for (HitArea.Placed p : PLACED) {
			if (p.radius() > 0.0 && p.area().shape() != HitArea.Shape.TARGET && p.area().shape() != HitArea.Shape.PROJECTILE) {
				out.add(p);
			}
		}
		return out;
	}

	/** The box round everything recorded, or null when nothing was. */
	static @Nullable AABB bounds() {
		AABB box = null;
		for (HitArea.Placed p : areas()) {
			AABB one = switch (p.area().shape()) {
				case LINE, PATH -> new AABB(p.center(), p.center().add(p.dir())).inflate(p.radius());
				default -> new AABB(p.center(), p.center()).inflate(p.radius(), 0.5, p.radius());
			};
			box = box == null ? one : box.minmax(one);
		}
		return box;
	}

	private static void draw() {
		for (HitArea.Placed p : PLACED) {
			if (p.radius() <= 0.0) {
				continue;
			}
			switch (p.area().shape()) {
				case CIRCLE -> arc(feet(p), p.radius(), 0.0, 360.0, false);
				case CONE -> {
					Vec3 at = p.center().add(0, -1.62 + LIFT, 0);
					double yaw = Math.toDegrees(Math.atan2(p.dir().z, p.dir().x));
					double arc = p.area().arc();
					arc(at, p.radius(), yaw - arc / 2, arc, arc < 359.0);
				}
				case LINE -> strip(p.center(), p.center().add(p.dir()), p.radius());
				case PATH -> strip(p.center().add(0, -1.0 + LIFT, 0), p.center().add(p.dir()).add(0, -1.0 + LIFT, 0), p.radius());
				default -> {
				}
			}
		}
	}

	private static Vec3 feet(final HitArea.Placed p) {
		return p.center().add(0, LIFT, 0);
	}

	/** A dashed arc of {@code radius} round {@code c} from {@code from} degrees over {@code span}, with its two sides when {@code sides}. */
	private static void arc(final Vec3 c, final double radius, final double from, final double span, final boolean sides) {
		int n = Math.max(8, (int)Math.ceil(SEGMENTS * span / 360.0));
		Vec3 prev = null;
		for (int i = 0; i <= n; i++) {
			double a = Math.toRadians(from + span * i / n);
			Vec3 q = c.add(Math.cos(a) * radius, 0, Math.sin(a) * radius);
			if (prev != null) {
				Gizmos.line(prev, q, i % 2 == 0 ? LIME : MAGENTA, WIDTH).setAlwaysOnTop();
			}
			if (sides && (i == 0 || i == n)) {
				dashed(c, q);
			}
			prev = q;
		}
	}

	/** A rectangle {@code half} either side of the segment, flat across, with its centre line. */
	private static void strip(final Vec3 from, final Vec3 to, final double half) {
		Vec3 d = to.subtract(from);
		Vec3 flat = new Vec3(d.x, 0, d.z);
		Vec3 side = flat.lengthSqr() < 1.0E-6 ? new Vec3(1, 0, 0) : new Vec3(-flat.z, 0, flat.x).normalize().scale(half);
		dashed(from.add(side), to.add(side));
		dashed(from.subtract(side), to.subtract(side));
		dashed(from.add(side), from.subtract(side));
		dashed(to.add(side), to.subtract(side));
		Gizmos.line(from, to, MAGENTA, 1.5F).setAlwaysOnTop();
	}

	private static void dashed(final Vec3 a, final Vec3 b) {
		int n = Math.max(2, (int)Math.ceil(a.distanceTo(b) / 0.5));
		for (int i = 0; i < n; i++) {
			Vec3 p = a.lerp(b, (double)i / n);
			Vec3 q = a.lerp(b, (double)(i + 1) / n);
			Gizmos.line(p, q, i % 2 == 0 ? LIME : MAGENTA, WIDTH).setAlwaysOnTop();
		}
	}

	private LookAreaOverlay() {
	}
}
