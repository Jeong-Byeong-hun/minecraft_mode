package com.minecraftmode.client.residence;

import com.minecraftmode.network.LandBordersPayload;
import com.minecraftmode.residence.LandClaims;
import com.minecraftmode.residence.ResidenceDimension;
import java.util.List;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.phys.AABB;

/**
 * Draws the edges of every residence claim within {@link #RANGE} blocks as solid lines (green: yours, yellow: you are trusted,
 * red: someone else's), hidden behind blocks like any other line in the world. The claims come from the server
 * ({@link LandBordersPayload}); the lines are vanilla gizmos, emitted every client tick like the debug renderers do.
 */
public final class LandBorders {
	public static final int RANGE = 16;
	private static final int[] COLORS = {0xFF55FF55, 0xFFFFE040, 0xFFFF5050};
	private static final float WIDTH = 2.5F;
	/** Lifts the lines off block faces so they do not flicker against them. */
	private static final double INFLATE = 0.02;
	/** How far above and below the viewer a claim's column walls are drawn. */
	private static final double COLUMN_REACH = 12.0;
	private static volatile List<LandBordersPayload.Border> borders = List.of();

	public static void init() {
		ClientPlayNetworking.registerGlobalReceiver(LandBordersPayload.TYPE, (payload, context) -> borders = List.copyOf(payload.borders()));
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> borders = List.of());
		ClientTickEvents.END_CLIENT_TICK.register(LandBorders::draw);
	}

	public static int count() {
		return borders.size();
	}

	private static void draw(final Minecraft minecraft) {
		if (minecraft.level == null || minecraft.player == null || !ResidenceDimension.is(minecraft.level)) {
			return;
		}
		double px = minecraft.player.getX();
		double pz = minecraft.player.getZ();
		int size = LandClaims.SIZE;
		for (LandBordersPayload.Border border : borders) {
			double x0 = border.chunkX() << 4;
			double z0 = border.chunkZ() << 4;
			double dx = Math.max(0.0, Math.abs(px - (x0 + size / 2.0)) - size / 2.0);
			double dz = Math.max(0.0, Math.abs(pz - (z0 + size / 2.0)) - size / 2.0);
			if (dx * dx + dz * dz > RANGE * RANGE) {
				continue;
			}
			GizmoStyle style = GizmoStyle.stroke(COLORS[Math.clamp(border.relation(), 0, COLORS.length - 1)], WIDTH);
			// a claim is the whole column: its walls are drawn from the floor up past the viewer (and down to them when they dig)
			double py = minecraft.player.getY();
			double bottom = Math.min(border.minY(), py - COLUMN_REACH);
			double top = Math.max(border.minY() + size, py + COLUMN_REACH);
			Gizmos.cuboid(new AABB(x0, bottom, z0, x0 + size, top, z0 + size).inflate(INFLATE), style);
			// the bottom edges sit under the floor block of the claim; this outline lies on top of it so the border shows on the ground
			double floor = border.minY() + 1 + INFLATE;
			Gizmos.cuboid(new AABB(x0 - INFLATE, floor, z0 - INFLATE, x0 + size + INFLATE, floor, z0 + size + INFLATE), style);
		}
	}

	private LandBorders() {
	}
}
