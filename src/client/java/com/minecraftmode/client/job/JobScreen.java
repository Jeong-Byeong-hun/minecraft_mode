package com.minecraftmode.client.job;

import com.minecraftmode.city.CityZone;
import com.minecraftmode.entity.ClassTrainer;
import com.minecraftmode.job.JobClass;
import com.minecraftmode.job.JobData;
import com.minecraftmode.job.JobProgression;
import com.minecraftmode.job.JobStats;
import com.minecraftmode.job.gear.ClassAbilities;
import com.minecraftmode.job.gear.LevelRewards;
import com.minecraftmode.job.gear.StatLine;
import com.minecraftmode.job.quest.QuestData;
import com.minecraftmode.job.quest.QuestDef;
import com.minecraftmode.job.quest.QuestService;
import com.minecraftmode.job.quest.Quests;
import com.minecraftmode.network.QuestActionPayload;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * Class screen (K): level, experience, MP and the four tiers with their passives on the left; the
 * active trial (or where to get the next one) on the right. Advancing happens at the class
 * trainers in Stormhold, not here.
 */
public class JobScreen extends Screen {
	/** 320 wide: the narrowest GUI Minecraft lays out (4:3 screens at auto GUI scale). */
	private static final int W = 320;
	/** Width of each column. */
	private static final int COL = 142;
	private static final int H = 236;

	private JobData shown;
	private QuestData shownQuest;
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
		this.shownQuest = QuestService.get(player);
		if (this.shownQuest.hasQuest()) {
			this.addRenderableWidget(Button.builder(Component.translatable("screen.minecraft_mode.trainer.abandon"), b -> {
				if (ClientPlayNetworking.canSend(QuestActionPayload.TYPE)) {
					ClientPlayNetworking.send(new QuestActionPayload(QuestActionPayload.Action.ABANDON, -1));
				}
			}).bounds(this.left + W - 92, this.top + H - 30, 80, 20).build());
		}
		this.addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> this.onClose()).bounds(this.left + 12, this.top + H - 30, 80, 20).build());
	}

	@Override
	public void tick() {
		LocalPlayer player = this.player();
		if (player != null && (!JobProgression.get(player).equals(this.shown) || !QuestService.get(player).equals(this.shownQuest))) {
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
		g.fill(x + 160, y + 26, x + 161, y + H - 36, 0x40FFFFFF);

		// header
		Component title = Component.translatable(data.job().tierKey(data.tier())).withColor(data.job().color());
		g.text(this.font, title, x + 12, y + 10, 0xFFFFFFFF, true);
		String level = "Lv " + data.level();
		g.text(this.font, level, x + 12 + COL - this.font.width(level), y + 10, 0xFFFFE08A, true);

		// left: experience, MP, tiers
		int need = JobProgression.expToNext(data.level());
		String expText = data.level() >= JobProgression.MAX_LEVEL ? "MAX" : data.exp() + " / " + need;
		g.text(this.font, Component.translatable("screen.minecraft_mode.job.exp", expText), x + 12, y + 28, 0xFFCCCCCC, false);
		float fraction = data.level() >= JobProgression.MAX_LEVEL ? 1.0F : Math.min(1.0F, (float)data.exp() / need);
		g.fill(x + 12, y + 39, x + 12 + COL, y + 42, 0xFF2A2A2A);
		g.fill(x + 12, y + 39, x + 12 + Math.round(COL * fraction), y + 42, 0xFFE8C547);
		g.text(this.font, Component.translatable("screen.minecraft_mode.job.mana", data.mana(), JobStats.maxMana(player), JobStats.manaRegen(player)), x + 12, y + 47, 0xFF9CC3FF, false);

		int ty = y + 62;
		if (data.job() == JobClass.NONE) {
			g.textWithWordWrap(this.font, Component.translatable("screen.minecraft_mode.job.no_class", JobProgression.levelForTier(1)), x + 12, ty, COL, 0xFFBBBBBB);
		} else {
			for (int tier = 1; tier <= 4; tier++) {
				boolean unlocked = data.tier() >= tier;
				int color = unlocked ? 0xFF000000 | data.job().color() : 0xFF777777;
				g.text(this.font, Component.literal(unlocked ? "✔ " : "✕ ").append(Component.translatable("screen.minecraft_mode.job.tier_line", tier, Component.translatable(data.job().tierKey(tier)))),
					x + 12, ty, color, false);
				Component passive = Component.translatable(data.job().passiveKey(tier));
				g.text(this.font, Component.literal("  ").append(passive), x + 12, ty + 10, unlocked ? 0xFFFFD27F : 0xFF666666, false);
				if (mouseX >= x + 12 && mouseX < x + 12 + COL && mouseY >= ty && mouseY < ty + 20) {
					List<Component> tip = new ArrayList<>();
					tip.add(passive.copy().withStyle(ChatFormatting.GOLD));
					tip.add(Component.translatable(data.job().passiveDescKey(tier)));
					tip.add(Component.translatable("screen.minecraft_mode.job.requires_level", JobProgression.levelForTier(tier)).withStyle(ChatFormatting.DARK_GRAY));
					g.setComponentTooltipForNextFrame(this.font, tip, mouseX, mouseY);
				}
				ty += 24;
			}
			this.levelBonus(g, data, x + 12, ty + 2);
		}

		// right: the trial
		int rx = x + 168;
		int ry = y + 26;
		QuestDef active = QuestService.active(player);
		if (active != null) {
			ry = this.activeQuest(g, player, active, rx, ry);
		} else if (data.tier() == 0) {
			ry = g.textWithWordWrap(this.font, Component.translatable("screen.minecraft_mode.job.visit_any"), rx, ry, COL, 0xFFFFFFFF) + 4;
			for (JobClass job : JobClass.PLAYABLE) {
				BlockPos home = CityZone.trainerHome(job, 0);
				g.text(this.font, Component.translatable(ClassTrainer.nameKey(job)).withColor(job.color()), rx, ry, 0xFFFFFFFF, false);
				String where = home.getX() + ", " + home.getZ();
				g.text(this.font, where, rx + COL - this.font.width(where), ry, 0xFF8A8A8A, false);
				ry += 11;
			}
		} else if (data.tier() < 4) {
			QuestDef next = Quests.forTier(data.job(), data.tier() + 1);
			BlockPos home = CityZone.trainerHome(data.job(), 0);
			ry = g.textWithWordWrap(this.font, Component.translatable("screen.minecraft_mode.job.next_trial", data.tier() + 1, Component.translatable(next.nameKey()),
				Component.translatable(ClassTrainer.nameKey(data.job())), home.getX(), home.getZ()).withColor(data.job().color()), rx, ry, COL, 0xFFFFFFFF) + 2;
			int needLevel = JobProgression.levelForTier(data.tier() + 1);
			g.text(this.font, Component.translatable("screen.minecraft_mode.job.requires_level", needLevel), rx, ry, data.level() >= needLevel ? 0xFF7CFC7C : 0xFFFF6B6B, false);
			ry = g.textWithWordWrap(this.font, Component.translatable("screen.minecraft_mode.job.next_passive",
				Component.translatable(data.job().passiveKey(data.tier() + 1)), Component.translatable(data.job().passiveDescKey(data.tier() + 1))), rx, ry + 12, COL, 0xFFBBBBBB);
		} else {
			ry = g.textWithWordWrap(this.font, Component.translatable("screen.minecraft_mode.job.final"), rx, ry, COL, 0xFFFFD27F);
		}
		g.textWithWordWrap(this.font, Component.translatable("screen.minecraft_mode.job.hint",
			JobKeys.SKILLS[0].getTranslatedKeyMessage(), JobKeys.SKILLS[1].getTranslatedKeyMessage(), JobKeys.SKILLS[2].getTranslatedKeyMessage(),
			JobKeys.SKILLS[3].getTranslatedKeyMessage()), rx, Math.max(ry + 8, y + 132), COL, 0xFF8A8A8A);

		super.extractRenderState(g, mouseX, mouseY, a);
	}

	/** Level rewards of the class and the innate ability (key B). */
	private void levelBonus(final GuiGraphicsExtractor g, final JobData data, final int x, final int y) {
		int ty = y;
		List<StatLine> rewards = LevelRewards.of(data);
		if (!rewards.isEmpty()) {
			MutableComponent text = Component.translatable("screen.minecraft_mode.job.level_bonus").append(": ");
			for (int i = 0; i < rewards.size(); i++) {
				if (i > 0) {
					text.append(", ");
				}
				text.append(Component.translatable(rewards.get(i).stat().key(), JobTooltips.num(rewards.get(i).value())));
			}
			ty = g.textWithWordWrap(this.font, text, x, ty, COL, 0xFF9CE89C) + 1;
		}
		ClassAbilities.Ability ability = ClassAbilities.Ability.of(data.job());
		if (ability != null) {
			Component line = LevelRewards.hasInnate(data)
				? Component.translatable("screen.minecraft_mode.job.innate", JobKeys.INNATE.getTranslatedKeyMessage(), Component.translatable(ability.nameKey()))
				: Component.translatable("screen.minecraft_mode.job.innate_locked", LevelRewards.INNATE_LEVEL);
			g.textWithWordWrap(this.font, line, x, ty, COL, LevelRewards.hasInnate(data) ? 0xFFFFD27F : 0xFF777777);
		}
	}

	private int activeQuest(final GuiGraphicsExtractor g, final LocalPlayer player, final QuestDef quest, final int x, final int y) {
		int ty = y;
		g.text(this.font, Component.translatable("screen.minecraft_mode.trainer.trial", quest.tier(), Component.translatable(quest.nameKey())).withColor(quest.job().color()), x, ty, 0xFFFFFFFF, false);
		ty += 12;
		QuestData data = QuestService.get(player);
		for (int i = 0; i < quest.kills().size(); i++) {
			ty = this.progress(g, x, ty, Component.translatable(quest.goalKey(i)), data.progress(i), quest.kills().get(i).count());
		}
		ty = this.progress(g, x, ty, Component.translatable(quest.token().getDescriptionId()), JobProgression.count(player.getInventory(), quest.token()), quest.tokenCount());
		for (QuestDef.Material material : quest.materials()) {
			ty = this.progress(g, x, ty, Component.translatable(material.item().getDescriptionId()), JobProgression.count(player.getInventory(), material.item()), material.count());
		}
		BlockPos home = CityZone.trainerHome(quest.job(), 0);
		return g.textWithWordWrap(this.font, Component.translatable("screen.minecraft_mode.job.return_to", Component.translatable(ClassTrainer.nameKey(quest.job())), home.getX(), home.getZ()),
			x, ty + 2, COL, 0xFFBBBBBB);
	}

	private int progress(final GuiGraphicsExtractor g, final int x, final int y, final Component label, final int have, final int need) {
		boolean done = have >= need;
		g.text(this.font, Component.literal(done ? "✔ " : "• ").append(label), x, y, done ? 0xFF7CFC7C : 0xFFE0E0E0, false);
		String count = Math.min(have, need) + "/" + need;
		g.text(this.font, count, x + COL - this.font.width(count), y, done ? 0xFF7CFC7C : 0xFFBBBBBB, false);
		return y + 10;
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
