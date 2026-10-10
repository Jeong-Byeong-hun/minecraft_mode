package com.minecraftmode.client.hud;

import com.minecraftmode.MinecraftMode;
import com.minecraftmode.client.job.JobHud;
import com.minecraftmode.client.map.MapSettings;
import com.minecraftmode.client.mixin.BossHealthOverlayAccessor;
import com.minecraftmode.network.TargetHealthPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/**
 * The health bar of the monster the player hit last (melee, shots or skills; the server names it with {@link TargetHealthPayload}):
 * top centre (kept between the class panel and the minimap), under any boss bars, with its name and health. It stays {@link #SHOW_TICKS} after the last hit and goes when the
 * target dies or is far away.
 */
public final class TargetHealthHud {
	public static final int SHOW_TICKS = 100;
	public static final int WIDTH = 182;
	private static final double RANGE = 48.0;
	private static int targetId = -1;
	private static long shownAt;
	/** Health a moment ago, drawn as a fading chunk behind the bar. */
	private static float trail = -1.0F;

	public static void init() {
		ClientPlayNetworking.registerGlobalReceiver(TargetHealthPayload.TYPE, (payload, context) -> context.client().execute(() -> track(payload.entityId())));
		HudElementRegistry.attachElementAfter(VanillaHudElements.BOSS_BAR, MinecraftMode.id("target_health"), TargetHealthHud::extract);
	}

	public static void track(final int entityId) {
		Minecraft minecraft = Minecraft.getInstance();
		if (entityId != targetId) {
			trail = -1.0F;
		}
		targetId = entityId;
		shownAt = minecraft.level == null ? 0L : minecraft.level.getGameTime();
	}

	/** The entity whose bar is shown, or null when there is none to show. */
	public static LivingEntity target() {
		Minecraft minecraft = Minecraft.getInstance();
		if (targetId < 0 || minecraft.level == null || minecraft.player == null) {
			return null;
		}
		Entity entity = minecraft.level.getEntity(targetId);
		if (!(entity instanceof LivingEntity living) || !living.isAlive() || living.isRemoved()
			|| minecraft.level.getGameTime() - shownAt > SHOW_TICKS || living.distanceTo(minecraft.player) > RANGE) {
			return null;
		}
		return living;
	}

	private static void extract(final GuiGraphicsExtractor g, final DeltaTracker delta) {
		Minecraft minecraft = Minecraft.getInstance();
		LivingEntity target = target();
		if (target == null) {
			targetId = -1;
			return;
		}
		Font font = minecraft.font;
		int bosses = ((BossHealthOverlayAccessor)minecraft.gui.hud.getBossOverlay()).minecraftMode$events().size();
		// centred, but between the class panel (top left) and the minimap (top right) when they come close
		int left = JobHud.panelRight() + 6;
		int right = g.guiWidth() - 4 - (MapSettings.minimap ? MapSettings.sizePx() + 12 : 0);
		int width = Math.max(120, Math.min(WIDTH, right - left));
		int x = Mth.clamp(g.guiWidth() / 2 - width / 2, left, Math.max(left, right - width));
		int y = bosses == 0 ? 4 : 12 + bosses * 19;
		float max = Math.max(1.0F, target.getMaxHealth());
		float health = Mth.clamp(target.getHealth(), 0.0F, max);
		trail = trail < 0.0F ? health : Math.max(health, trail - max * 0.01F);
		Component name = target.getDisplayName();
		String numbers = "❤ " + whole(health) + " / " + whole(max);
		g.fill(x - 2, y - 2, x + width + 2, y + 17, 0x90000000);
		g.text(font, font.plainSubstrByWidth(name.getString(), width - font.width(numbers) - 8), x + 1, y, 0xFFFFFFFF, true);
		g.text(font, numbers, x + width - font.width(numbers) - 1, y, 0xFFFF8A8A, true);
		int barY = y + 10;
		g.fill(x, barY, x + width, barY + 5, 0xFF2A0A0A);
		g.fill(x, barY, x + Math.round(width * trail / max), barY + 5, 0xFFE8C080);
		int color = health / max > 0.5F ? 0xFF4CC24C : health / max > 0.25F ? 0xFFE0B030 : 0xFFE04040;
		g.fill(x, barY, x + Math.round(width * health / max), barY + 5, color);
		g.fill(x, barY, x + Math.round(width * health / max), barY + 1, 0x50FFFFFF);
	}

	private static String whole(final float value) {
		return value > 0.0F && value < 1.0F ? "1" : Integer.toString(Math.round(value));
	}

	private TargetHealthHud() {
	}
}
