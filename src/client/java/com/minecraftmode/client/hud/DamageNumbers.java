package com.minecraftmode.client.hud;

import com.minecraftmode.MinecraftMode;
import com.minecraftmode.client.ClientSettings;
import com.minecraftmode.network.DamageNumberPayload;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector4f;

/**
 * Floating damage numbers over what the player (or their summon) hits: white for a normal hit, a bigger gold number with a "!" for a
 * critical one. They rise and fade over about a second; hits on the same target within {@link #MERGE_MS} add up into one number so area
 * skills do not paper the screen, and at most {@link #MAX} are shown. Drawn on the HUD at the target's projected position.
 * {@code /damagenumbers} turns them off ({@link ClientSettings}).
 */
public final class DamageNumbers {
	private static final int MAX = 40;
	private static final long LIFE_MS = 900L;
	private static final long CRIT_LIFE_MS = 1300L;
	private static final long MERGE_MS = 150L;
	private static final float RISE = 22.0F;

	private static final class Entry {
		final int entityId;
		final Vec3 pos;
		final boolean crit;
		final long born;
		final float drift;
		float amount;

		Entry(final int entityId, final Vec3 pos, final float amount, final boolean crit, final long born, final float drift) {
			this.entityId = entityId;
			this.pos = pos;
			this.amount = amount;
			this.crit = crit;
			this.born = born;
			this.drift = drift;
		}
	}

	private static final List<Entry> ENTRIES = new ArrayList<>();

	/** Numbers floating right now (for tests). */
	public static int count() {
		return ENTRIES.size();
	}

	public static void init() {
		ClientPlayNetworking.registerGlobalReceiver(DamageNumberPayload.TYPE, (payload, context) -> context.client().execute(() -> add(payload)));
		HudElementRegistry.attachElementBefore(VanillaHudElements.CROSSHAIR, MinecraftMode.id("damage_numbers"), DamageNumbers::extract);
	}

	private static void add(final DamageNumberPayload payload) {
		Minecraft minecraft = Minecraft.getInstance();
		if (!ClientSettings.damageNumbers || minecraft.level == null) {
			return;
		}
		Entity entity = minecraft.level.getEntity(payload.entityId());
		if (entity == null) {
			return;
		}
		long now = System.currentTimeMillis();
		for (Entry e : ENTRIES) {
			if (e.entityId == payload.entityId() && e.crit == payload.crit() && now - e.born < MERGE_MS) {
				e.amount += payload.amount();
				return;
			}
		}
		if (ENTRIES.size() >= MAX) {
			ENTRIES.removeFirst();
		}
		float drift = (minecraft.level.getRandom().nextFloat() - 0.5F) * 16.0F;
		ENTRIES.add(new Entry(payload.entityId(), new Vec3(entity.getX(), entity.getY() + entity.getBbHeight() * 0.85, entity.getZ()), payload.amount(), payload.crit(), now,
			drift));
	}

	private static void extract(final GuiGraphicsExtractor g, final DeltaTracker delta) {
		Minecraft minecraft = Minecraft.getInstance();
		if (ENTRIES.isEmpty() || minecraft.level == null) {
			ENTRIES.clear();
			return;
		}
		Camera camera = minecraft.gameRenderer.mainCamera();
		Matrix4f view = camera.getViewRotationProjectionMatrix(new Matrix4f());
		Vec3 eye = camera.position();
		Font font = minecraft.font;
		long now = System.currentTimeMillis();
		for (Iterator<Entry> it = ENTRIES.iterator(); it.hasNext();) {
			Entry e = it.next();
			long life = e.crit ? CRIT_LIFE_MS : LIFE_MS;
			float t = (now - e.born) / (float)life;
			if (t >= 1.0F) {
				it.remove();
				continue;
			}
			Vector4f clip = new Vector4f((float)(e.pos.x - eye.x), (float)(e.pos.y - eye.y), (float)(e.pos.z - eye.z), 1.0F);
			view.transform(clip);
			if (clip.w <= 0.05F) {
				continue;
			}
			float x = (clip.x / clip.w + 1.0F) / 2.0F * g.guiWidth() + e.drift * t;
			float y = (1.0F - clip.y / clip.w) / 2.0F * g.guiHeight() - RISE * t;
			int alpha = Mth.clamp((int)(255 * (t < 0.7F ? 1.0F : (1.0F - t) / 0.3F)), 8, 255);
			String text = e.amount >= 100.0F ? String.valueOf(Math.round(e.amount)) : String.format("%.1f", e.amount);
			if (e.crit) {
				text += "!";
			}
			float scale = e.crit ? 1.5F + 0.4F * Math.max(0.0F, 1.0F - t * 5.0F) : 1.0F;
			int color = (alpha << 24) | (e.crit ? 0xFFC83A : 0xFFFFFF);
			g.pose().pushMatrix();
			g.pose().translate(x, y);
			g.pose().scale(scale, scale);
			g.text(font, text, -font.width(text) / 2, -4, color, true);
			g.pose().popMatrix();
		}
	}

	private DamageNumbers() {
	}
}
