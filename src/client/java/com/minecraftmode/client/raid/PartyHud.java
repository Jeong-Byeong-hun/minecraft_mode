package com.minecraftmode.client.raid;

import com.minecraftmode.client.job.JobHud;
import com.minecraftmode.job.JobClass;
import com.minecraftmode.network.PartySyncPayload;
import java.util.List;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Party frames on the left edge: one row per member with class color, name (★ = leader), level and
 * health bar; grey when offline, a sword mark while in a raid.
 */
final class PartyHud {
	private static final int W = 104;
	private static final int ROW = 17;

	static void extract(final GuiGraphicsExtractor g, final DeltaTracker delta) {
		Minecraft minecraft = Minecraft.getInstance();
		List<PartySyncPayload.Member> party = RaidClient.party();
		if (party.isEmpty() || minecraft.player == null) {
			return;
		}
		Font font = minecraft.font;
		int x = 4;
		// centered on the left edge, but below the class panels and above the hotbar and health
		int y = Math.max(JobHud.leftBottom() + 6, g.guiHeight() / 2 - party.size() * ROW / 2);
		y = Math.max(0, Math.min(y, g.guiHeight() - 40 - party.size() * ROW));
		for (int i = 0; i < party.size(); i++) {
			PartySyncPayload.Member m = party.get(i);
			JobClass job = JobClass.values()[Math.floorMod(m.job(), JobClass.values().length)];
			int top = y + i * ROW;
			g.fill(x, top, x + W, top + ROW - 2, 0x90000000);
			g.fill(x, top, x + 2, top + ROW - 2, 0xFF000000 | job.color());
			String name = (i == 0 ? "★ " : "") + m.name();
			int nameColor = !m.online() ? 0xFF707070 : m.id().equals(minecraft.player.getUUID()) ? 0xFFFFE08A : 0xFFFFFFFF;
			g.text(font, font.plainSubstrByWidth(name, W - 34), x + 5, top + 1, nameColor, true);
			String level = m.online() ? "Lv" + m.level() : "-";
			g.text(font, level, x + W - 3 - font.width(level), top + 1, 0xFFBBBBBB, true);
			float fraction = m.online() && m.maxHealth() > 0 ? Math.min(1.0F, m.health() / m.maxHealth()) : 0.0F;
			int barW = W - 8;
			g.fill(x + 5, top + 10, x + 5 + barW, top + 13, 0xFF3A1010);
			int healthColor = fraction > 0.5F ? 0xFF4CD06A : fraction > 0.25F ? 0xFFE8C547 : 0xFFE04848;
			g.fill(x + 5, top + 10, x + 5 + Math.round(barW * fraction), top + 13, healthColor);
			if (m.inRaid()) {
				g.text(font, "⚔", x + W - 3 - font.width(level) - 10, top + 1, 0xFFFF6B6B, true);
			}
		}
	}

	private PartyHud() {
	}
}
