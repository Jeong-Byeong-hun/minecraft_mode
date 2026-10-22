package com.minecraftmode.client.domain;

import com.minecraftmode.MinecraftMode;
import com.minecraftmode.client.BossBarLayout;
import com.minecraftmode.domain.DomainStyle;
import com.minecraftmode.network.DomainPayload;
import java.util.HashMap;
import java.util.Map;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The open domains the server told about ({@link DomainPayload}) and what standing in one looks like: the world darkens and takes the
 * style's light ({@code LightmapRenderStateExtractorMixin}), fog closes in in its color ({@code FogRendererMixin}), the screen
 * tints with a vignette and the domain's name and "sure hit" show, its edge glows on the ground and embers fall. All of it eases in
 * and out over a second ({@link #strength}).
 */
public final class DomainClient {
	private record Active(Vec3 center, float radius, DomainStyle style, long until) {
		boolean contains(final LocalPlayer p) {
			return p.position().subtract(this.center).horizontalDistance() <= this.radius && Math.abs(p.getY() - this.center.y) < this.radius;
		}
	}

	private static final Map<Integer, Active> ACTIVE = new HashMap<>();
	private static final float EASE = 0.06F;
	private static float strength;
	private static float previous;
	private static DomainStyle style = DomainStyle.MALEVOLENT_SHRINE;
	private static @Nullable Active current;

	public static void init() {
		ClientPlayNetworking.registerGlobalReceiver(DomainPayload.TYPE, (payload, context) -> context.client().execute(() -> receive(payload)));
		ClientTickEvents.END_CLIENT_TICK.register(DomainClient::tick);
		HudElementRegistry.attachElementBefore(VanillaHudElements.CROSSHAIR, MinecraftMode.id("domain"), DomainClient::extract);
	}

	private static void receive(final DomainPayload payload) {
		ClientLevel level = Minecraft.getInstance().level;
		if (payload.ticks() <= 0 || level == null) {
			ACTIVE.remove(payload.id());
			return;
		}
		Vec3 center = new Vec3(payload.center().x(), payload.center().y(), payload.center().z());
		ACTIVE.put(payload.id(), new Active(center, payload.radius(), DomainStyle.byId(payload.style()), level.getGameTime() + payload.ticks()));
	}

	private static void tick(final Minecraft minecraft) {
		previous = strength;
		ClientLevel level = minecraft.level;
		LocalPlayer player = minecraft.player;
		if (level == null || player == null) {
			ACTIVE.clear();
			current = null;
			strength = 0.0F;
			return;
		}
		ACTIVE.values().removeIf(d -> level.getGameTime() >= d.until());
		current = null;
		for (Active d : ACTIVE.values()) {
			if (d.contains(player)) {
				current = d;
			}
		}
		if (current != null) {
			style = current.style();
		}
		strength = Mth.approach(strength, current != null ? 1.0F : 0.0F, EASE);
		for (Active d : ACTIVE.values()) {
			edge(level, player, d);
		}
		if (current != null && level.getGameTime() % 2 == 0) {
			// embers drifting down around the player
			for (int i = 0; i < 3; i++) {
				double x = player.getX() + (level.getRandom().nextDouble() - 0.5) * 16;
				double z = player.getZ() + (level.getRandom().nextDouble() - 0.5) * 16;
				level.addParticle(ParticleTypes.ASH, x, player.getY() + 3 + level.getRandom().nextDouble() * 4, z, 0, -0.05, 0);
				if (level.getRandom().nextInt(4) == 0) {
					level.addParticle(ParticleTypes.SMALL_FLAME, x, player.getY() + 4, z, 0, -0.02, 0);
				}
			}
		}
	}

	/** The edge on the ground, near the player only: a ring of glowing dust, some of it drifting up. */
	private static void edge(final ClientLevel level, final LocalPlayer player, final Active d) {
		if (level.getGameTime() % 3 != 0 || player.position().distanceTo(d.center()) > d.radius() + 32) {
			return;
		}
		int color = d.style().edge;
		DustParticleOptions dust = new DustParticleOptions(color, 2.0F);
		int points = Math.max(24, Math.round(d.radius() * 5));
		double turn = level.getGameTime() * 0.01;
		for (int i = 0; i < points; i++) {
			double a = turn + Math.PI * 2 * i / points;
			double x = d.center().x + Math.cos(a) * d.radius();
			double z = d.center().z + Math.sin(a) * d.radius();
			level.addParticle(dust, x, d.center().y + 0.15, z, 0, 0, 0);
			if (level.getRandom().nextInt(8) == 0) {
				level.addParticle(dust, x, d.center().y + 0.6 + level.getRandom().nextDouble() * 1.4, z, 0, 0.02, 0);
			}
		}
	}

	/** How much the player is in a domain right now, 0..1, eased. */
	public static float strength(final float partialTicks) {
		return Mth.lerp(partialTicks, previous, strength);
	}

	/** The style of the domain the player is in (or was in last). */
	public static DomainStyle style() {
		return style;
	}

	/** Tint, vignette and the domain's name with "sure hit" under the boss bars. */
	private static void extract(final GuiGraphicsExtractor g, final DeltaTracker delta) {
		float s = strength(delta.getGameTimeDeltaPartialTick(false));
		if (s <= 0.01F) {
			return;
		}
		int w = g.guiWidth();
		int h = g.guiHeight();
		int fog = style.fog & 0xFFFFFF;
		g.fill(0, 0, w, h, alpha(0.12F * s) | fog);
		int edge = Math.max(24, h / 4);
		g.fillGradient(0, 0, w, edge, alpha(0.5F * s) | fog, fog);
		g.fillGradient(0, h - edge, w, h, fog, alpha(0.5F * s) | fog);
		for (int i = 0; i < edge; i += 2) {
			int a = alpha(0.5F * s * (1.0F - i / (float)edge));
			g.fill(i, 0, i + 2, h, a | fog);
			g.fill(w - i - 2, 0, w - i, h, a | fog);
		}
		if (current != null) {
			Minecraft minecraft = Minecraft.getInstance();
			int bars = BossBarLayout.bottom();
			int y = (bars < 0 ? 8 : bars + 30);
			float pulse = 0.75F + 0.25F * Mth.sin((minecraft.level == null ? 0 : minecraft.level.getGameTime()) * 0.3F);
			int text = alpha(Math.min(1.0F, s * pulse)) | 0xFF4A4A;
			g.centeredText(minecraft.font, Component.translatable(style.nameKey()).withStyle(ChatFormatting.BOLD), w / 2, y, text);
			g.centeredText(minecraft.font, Component.translatable("domain.minecraft_mode.sure_hit"), w / 2, y + 11, alpha(s) | 0xFFC0C0);
		}
	}

	private static int alpha(final float a) {
		return Math.round(Mth.clamp(a, 0.0F, 1.0F) * 255) << 24;
	}

	private DomainClient() {
	}
}
