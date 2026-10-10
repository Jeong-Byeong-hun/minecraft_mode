package com.minecraftmode.client.job;

import com.minecraftmode.economy.Essence;
import com.minecraftmode.entity.ClassTrainer;
import com.minecraftmode.job.JobClass;
import com.minecraftmode.job.JobData;
import com.minecraftmode.job.JobProgression;
import com.minecraftmode.job.quest.QuestData;
import com.minecraftmode.job.quest.QuestDef;
import com.minecraftmode.job.quest.QuestService;
import com.minecraftmode.network.QuestActionPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.Item;
import org.jspecify.annotations.Nullable;

/**
 * Trainer dialog: greeting, then the trainer's trial (story, goals, materials) with Accept,
 * Complete and Abandon buttons depending on the player's state. Rebuilds when the synced job or
 * quest data changes.
 */
public class TrainerScreen extends Screen {
	private static final int W = 320;
	private static final int H = 220;

	private final int entityId;
	private final JobClass job;
	private JobData shownJob;
	private QuestData shownQuest;
	private int left;
	private int top;

	public TrainerScreen(final int entityId, final JobClass job) {
		super(Component.translatable(ClassTrainer.nameKey(job)));
		this.entityId = entityId;
		this.job = job;
	}

	private @Nullable LocalPlayer player() {
		return this.minecraft == null ? null : this.minecraft.player;
	}

	@Override
	protected void init() {
		this.left = (this.width - W) / 2;
		this.top = (this.height - H) / 2;
		LocalPlayer player = this.player();
		if (player == null) {
			return;
		}
		this.shownJob = JobProgression.get(player);
		this.shownQuest = QuestService.get(player);
		QuestService.Status status = QuestService.status(player, this.job);
		int by = this.top + H - 28;
		switch (status) {
			case AVAILABLE, LOW_LEVEL -> {
				QuestDef offered = QuestService.offered(player, this.job);
				String label = offered != null && offered.instant() ? "screen.minecraft_mode.trainer.choose" : "screen.minecraft_mode.trainer.accept";
				Button accept = Button.builder(Component.translatable(label), b -> this.send(QuestActionPayload.Action.ACCEPT))
					.bounds(this.left + W - 132, by, 120, 20).build();
				accept.active = status == QuestService.Status.AVAILABLE;
				this.addRenderableWidget(accept);
			}
			case IN_PROGRESS, READY -> {
				Button complete = Button.builder(Component.translatable("screen.minecraft_mode.trainer.complete"), b -> this.send(QuestActionPayload.Action.COMPLETE))
					.bounds(this.left + W - 132, by, 120, 20).build();
				complete.active = status == QuestService.Status.READY;
				this.addRenderableWidget(complete);
				this.addRenderableWidget(Button.builder(Component.translatable("screen.minecraft_mode.trainer.abandon"), b -> this.send(QuestActionPayload.Action.ABANDON))
					.bounds(this.left + W - 214, by, 76, 20).build());
			}
			case BUSY -> this.addRenderableWidget(Button.builder(Component.translatable("screen.minecraft_mode.trainer.abandon"), b -> this.send(QuestActionPayload.Action.ABANDON))
				.bounds(this.left + W - 132, by, 120, 20).build());
			default -> {
			}
		}
		this.addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> this.onClose()).bounds(this.left + 12, by, 70, 20).build());
	}

	private void send(final QuestActionPayload.Action action) {
		if (ClientPlayNetworking.canSend(QuestActionPayload.TYPE)) {
			ClientPlayNetworking.send(new QuestActionPayload(action, this.entityId));
		}
	}

	@Override
	public void tick() {
		LocalPlayer player = this.player();
		if (player == null) {
			return;
		}
		if (!JobProgression.get(player).equals(this.shownJob) || !QuestService.get(player).equals(this.shownQuest)) {
			this.rebuildWidgets();
		}
	}

	@Override
	public void extractRenderState(final GuiGraphicsExtractor g, final int mouseX, final int mouseY, final float a) {
		LocalPlayer player = this.player();
		if (player == null) {
			return;
		}
		int x = this.left;
		int y = this.top;
		int color = 0xFF000000 | this.job.color();
		g.fill(x, y, x + W, y + H, 0xE8101018);
		g.outline(x, y, W, H, color);
		g.text(this.font, this.title.copy().withColor(this.job.color()), x + 12, y + 10, 0xFFFFFFFF, true);
		g.text(this.font, Component.translatable("screen.minecraft_mode.trainer.of", Component.translatable(this.job.nameKey())), x + 12, y + 21, 0xFF9A9A9A, false);
		int ty = g.textWithWordWrap(this.font, Component.translatable(ClassTrainer.greetingKey(this.job)).withStyle(ChatFormatting.ITALIC), x + 12, y + 36, W - 24, 0xFFD8D8D8) + 6;
		g.fill(x + 12, ty, x + W - 12, ty + 1, 0x40FFFFFF);
		ty += 6;

		QuestService.Status status = QuestService.status(player, this.job);
		switch (status) {
			case OTHER_CLASS -> g.textWithWordWrap(this.font, Component.translatable("screen.minecraft_mode.trainer.other_class",
				Component.translatable(JobProgression.get(player).job().nameKey())), x + 12, ty, W - 24, 0xFFBBBBBB);
			case MAX_TIER -> g.textWithWordWrap(this.font, Component.translatable("screen.minecraft_mode.trainer.mastered"), x + 12, ty, W - 24, 0xFFFFD27F);
			case BUSY -> {
				QuestDef active = QuestService.active(player);
				g.textWithWordWrap(this.font, Component.translatable("screen.minecraft_mode.trainer.busy",
					active == null ? Component.empty() : Component.translatable(active.nameKey())), x + 12, ty, W - 24, 0xFFFF9F6B);
			}
			default -> {
				QuestDef quest = status == QuestService.Status.IN_PROGRESS || status == QuestService.Status.READY
					? QuestService.active(player)
					: QuestService.offered(player, this.job);
				if (quest != null) {
					this.quest(g, player, quest, status, x + 12, ty);
				}
			}
		}
		super.extractRenderState(g, mouseX, mouseY, a);
	}

	private void quest(final GuiGraphicsExtractor g, final LocalPlayer player, final QuestDef quest, final QuestService.Status status, final int x, final int y) {
		int ty = y;
		String titleKey = quest.instant() ? "screen.minecraft_mode.trainer.choice" : "screen.minecraft_mode.trainer.trial";
		MutableComponent title = Component.translatable(titleKey, quest.tier(), Component.translatable(quest.nameKey()));
		g.text(this.font, title.withColor(this.job.color()), x, ty, 0xFFFFFFFF, true);
		g.text(this.font, Component.translatable("screen.minecraft_mode.trainer.reward", Component.translatable(this.job.tierKey(quest.tier()))),
			x + 150, ty, 0xFFFFD27F, false);
		ty = g.textWithWordWrap(this.font, Component.translatable(quest.storyKey()), x, ty + 12, W - 24, 0xFFBBBBBB) + 4;
		boolean started = status == QuestService.Status.IN_PROGRESS || status == QuestService.Status.READY;
		QuestData data = QuestService.get(player);
		for (int i = 0; i < quest.kills().size(); i++) {
			int have = started ? data.progress(i) : 0;
			int need = quest.kills().get(i).count();
			ty = this.line(g, x, ty, Component.translatable("screen.minecraft_mode.trainer.goal_kill", Component.translatable(quest.goalKey(i))), have, need, started);
		}
		if (quest.instant()) {
			ty = g.textWithWordWrap(this.font, Component.translatable("screen.minecraft_mode.trainer.instant"), x, ty, W - 24, 0xFF7CFC7C) + 2;
		} else {
			ty = this.item(g, player, x, ty, quest.token(), quest.tokenCount(), started);
		}
		for (QuestDef.Material material : quest.materials()) {
			ty = this.item(g, player, x, ty, material.item(), material.count(), true);
		}
		if (status == QuestService.Status.LOW_LEVEL) {
			g.text(this.font, Component.translatable("screen.minecraft_mode.job.requires_level", JobProgression.levelForTier(quest.tier())), x, ty + 2, 0xFFFF6B6B, false);
		}
	}

	private int item(final GuiGraphicsExtractor g, final LocalPlayer player, final int x, final int y, final Item item, final int need, final boolean count) {
		int have = count ? Essence.held(player.getInventory(), item) : 0;
		return this.line(g, x, y, Component.translatable("screen.minecraft_mode.trainer.goal_item", Component.translatable(item.getDescriptionId())), have, need, count);
	}

	private int line(final GuiGraphicsExtractor g, final int x, final int y, final Component label, final int have, final int need, final boolean showCount) {
		boolean done = showCount && have >= need;
		g.text(this.font, Component.literal(done ? "✔ " : "• ").append(label), x, y, done ? 0xFF7CFC7C : 0xFFE0E0E0, false);
		String count = showCount ? Math.min(have, need) + " / " + need : "x" + need;
		g.text(this.font, count, x + W - 24 - this.font.width(count), y, done ? 0xFF7CFC7C : 0xFFBBBBBB, false);
		return y + 11;
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
