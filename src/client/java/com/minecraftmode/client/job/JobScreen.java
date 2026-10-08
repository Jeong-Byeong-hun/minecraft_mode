package com.minecraftmode.client.job;

import com.minecraftmode.job.JobClass;
import com.minecraftmode.job.JobData;
import com.minecraftmode.job.JobProgression;
import com.minecraftmode.job.JobStats;
import com.minecraftmode.network.AdvanceJobPayload;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;

/**
 * Class screen (K): level, experience, MP, the four tiers with their passives, and the advancement
 * button (class choice at the first advancement). Rebuilds itself when the synced data changes.
 */
public class JobScreen extends Screen {
	private static final int W = 340;
	private static final int H = 214;

	private JobData shown;
	private int left;
	private int top;

	public JobScreen() {
		super(Component.translatable("screen.minecraft_mode.job"));
	}

	private LocalPlayer player() {
		return this.minecraft.player;
	}

	@Override
	protected void init() {
		this.left = (this.width - W) / 2;
		this.top = (this.height - H) / 2;
		LocalPlayer player = this.player();
		if (player == null) {
			return;
		}
		this.shown = JobProgression.get(player);
		int x = this.left + 180;
		int y = this.top + 40;
		if (this.shown.tier() == 0) {
			boolean levelOk = this.shown.level() >= JobProgression.levelForTier(1);
			for (JobClass job : JobClass.PLAYABLE) {
				MutableComponent label = Component.translatable(job.nameKey()).withColor(levelOk ? job.color() : 0x808080);
				Button button = Button.builder(label, b -> this.advance(job)).bounds(x, y, 150, 18).tooltip(Tooltip.create(classTooltip(job))).build();
				button.active = levelOk;
				this.addRenderableWidget(button);
				y += 21;
			}
		} else if (this.shown.tier() < 4) {
			Button button = Button.builder(Component.translatable("screen.minecraft_mode.job.advance"), b -> this.advance(this.shown.job()))
				.bounds(x, this.top + H - 30, 150, 20)
				.build();
			button.active = JobProgression.canAdvance(player, this.shown.job()) == JobProgression.AdvanceResult.OK;
			this.addRenderableWidget(button);
		}
		this.addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> this.onClose()).bounds(this.left + 12, this.top + H - 30, 80, 20).build());
	}

	private static Component classTooltip(final JobClass job) {
		MutableComponent text = Component.empty();
		for (int tier = 1; tier <= 4; tier++) {
			if (tier > 1) {
				text.append(" → ");
			}
			text.append(Component.translatable(job.tierKey(tier)));
		}
		text.append("\n\n").append(Component.translatable(job.passiveKey(1)).withStyle(ChatFormatting.GOLD))
			.append(": ").append(Component.translatable(job.passiveDescKey(1)));
		return text;
	}

	private void advance(final JobClass choice) {
		if (ClientPlayNetworking.canSend(AdvanceJobPayload.TYPE)) {
			ClientPlayNetworking.send(new AdvanceJobPayload(choice));
		}
	}

	@Override
	public void tick() {
		LocalPlayer player = this.player();
		if (player != null && !JobProgression.get(player).equals(this.shown)) {
			this.rebuildWidgets();
		}
	}

	@Override
	public void extractRenderState(final GuiGraphicsExtractor g, final int mouseX, final int mouseY, final float a) {
		LocalPlayer player = this.player();
		if (player == null) {
			return;
		}
		JobData data = JobProgression.get(player);
		int x = this.left;
		int y = this.top;
		g.fill(x, y, x + W, y + H, 0xE0101018);
		g.outline(x, y, W, H, 0xFF000000 | data.job().color());
		g.fill(x + 170, y + 26, x + 171, y + H - 36, 0x40FFFFFF);

		// header
		Component title = Component.translatable(data.job().tierKey(data.tier())).withColor(data.job().color());
		g.text(this.font, title, x + 12, y + 10, 0xFFFFFFFF, true);
		String level = "Lv " + data.level();
		g.text(this.font, level, x + 160 - this.font.width(level), y + 10, 0xFFFFE08A, true);

		// left: experience, MP, tiers
		int need = JobProgression.expToNext(data.level());
		String expText = data.level() >= JobProgression.MAX_LEVEL ? "MAX" : data.exp() + " / " + need;
		g.text(this.font, Component.translatable("screen.minecraft_mode.job.exp", expText), x + 12, y + 28, 0xFFCCCCCC, false);
		float fraction = data.level() >= JobProgression.MAX_LEVEL ? 1.0F : Math.min(1.0F, (float)data.exp() / need);
		g.fill(x + 12, y + 39, x + 160, y + 42, 0xFF2A2A2A);
		g.fill(x + 12, y + 39, x + 12 + Math.round(148 * fraction), y + 42, 0xFFE8C547);
		g.text(this.font, Component.translatable("screen.minecraft_mode.job.mana", data.mana(), JobStats.maxMana(player), JobStats.manaRegen(player)), x + 12, y + 47, 0xFF9CC3FF, false);

		int ty = y + 62;
		if (data.job() == JobClass.NONE) {
			g.textWithWordWrap(this.font, Component.translatable("screen.minecraft_mode.job.no_class", JobProgression.levelForTier(1)), x + 12, ty, 150, 0xFFBBBBBB);
		} else {
			for (int tier = 1; tier <= 4; tier++) {
				boolean unlocked = data.tier() >= tier;
				int color = unlocked ? 0xFF000000 | data.job().color() : 0xFF777777;
				String mark = unlocked ? "✔ " : "✕ ";
				g.text(this.font, Component.literal(mark).append(Component.translatable("screen.minecraft_mode.job.tier_line", tier, Component.translatable(data.job().tierKey(tier)))),
					x + 12, ty, color, false);
				Component passive = Component.translatable(data.job().passiveKey(tier));
				g.text(this.font, Component.literal("  ").append(passive), x + 12, ty + 10, unlocked ? 0xFFFFD27F : 0xFF666666, false);
				if (mouseX >= x + 12 && mouseX < x + 165 && mouseY >= ty && mouseY < ty + 20) {
					List<Component> tip = new ArrayList<>();
					tip.add(passive.copy().withStyle(ChatFormatting.GOLD));
					tip.add(Component.translatable(data.job().passiveDescKey(tier)));
					tip.add(Component.translatable("screen.minecraft_mode.job.requires_level", JobProgression.levelForTier(tier)).withStyle(ChatFormatting.DARK_GRAY));
					g.setComponentTooltipForNextFrame(this.font, tip, mouseX, mouseY);
				}
				ty += 24;
			}
		}

		// right: advancement
		int rx = x + 180;
		int ry = y + 26;
		int hintY;
		if (data.tier() == 0) {
			g.text(this.font, Component.translatable("screen.minecraft_mode.job.choose", JobProgression.levelForTier(1)), rx, ry, 0xFFFFFFFF, false);
			hintY = y + 40 + JobClass.PLAYABLE.size() * 21 + 2;
		} else if (data.tier() < 4) {
			int next = data.tier() + 1;
			g.text(this.font, Component.translatable("screen.minecraft_mode.job.next", next, Component.translatable(data.job().tierKey(next))).withColor(data.job().color()),
				rx, ry, 0xFFFFFFFF, false);
			int ly = ry + 14;
			int needLevel = JobProgression.levelForTier(next);
			boolean levelOk = data.level() >= needLevel;
			g.text(this.font, Component.translatable("screen.minecraft_mode.job.requires_level", needLevel), rx, ly, levelOk ? 0xFF7CFC7C : 0xFFFF6B6B, false);
			ly += 12;
			for (ItemStack cost : JobProgression.costForTier(next)) {
				int have = JobProgression.count(player.getInventory(), cost.getItem());
				boolean ok = have >= cost.getCount() || player.isCreative();
				g.item(cost, rx, ly - 4);
				g.text(this.font, Component.empty().append(cost.getHoverName()).append(" " + Math.min(have, cost.getCount()) + "/" + cost.getCount()),
					rx + 20, ly, ok ? 0xFF7CFC7C : 0xFFFF6B6B, false);
				ly += 18;
			}
			hintY = g.textWithWordWrap(this.font, Component.translatable("screen.minecraft_mode.job.next_passive",
				Component.translatable(data.job().passiveKey(next)), Component.translatable(data.job().passiveDescKey(next))), rx, ly + 4, 150, 0xFFBBBBBB) + 6;
		} else {
			hintY = g.textWithWordWrap(this.font, Component.translatable("screen.minecraft_mode.job.final"), rx, ry, 150, 0xFFFFD27F) + 6;
		}
		g.textWithWordWrap(this.font, Component.translatable("screen.minecraft_mode.job.hint",
			JobKeys.SKILLS[0].getTranslatedKeyMessage(), JobKeys.SKILLS[1].getTranslatedKeyMessage(), JobKeys.SKILLS[2].getTranslatedKeyMessage(),
			JobKeys.SKILLS[3].getTranslatedKeyMessage()), rx, hintY, 152, 0xFF8A8A8A);

		super.extractRenderState(g, mouseX, mouseY, a);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
