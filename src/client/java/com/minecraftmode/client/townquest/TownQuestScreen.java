package com.minecraftmode.client.townquest;

import com.minecraftmode.entity.CityNpc;
import com.minecraftmode.loot.Coins;
import com.minecraftmode.network.OpenTownQuestPayload;
import com.minecraftmode.network.TownQuestActionPayload;
import com.minecraftmode.progress.RealReset;
import com.minecraftmode.registry.ModItems;
import com.minecraftmode.townquest.TownQuestData;
import com.minecraftmode.townquest.TownQuestService;
import com.minecraftmode.townquest.TownQuestService.Status;
import com.minecraftmode.townquest.TownQuests;
import com.minecraftmode.townquest.TownQuests.Quest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * A resident's town quests: the list of the giver's quests with where each stands (new, in progress, ready, done today) on the left,
 * the picked quest on the right (the giver's words, the objective with its progress, the reward) with accept / turn in / abandon, and
 * for the jump quest a button into the course. Quests and progress come from the synced {@link TownQuestData}; the resident sends
 * today's real day and the time to the daily reset.
 */
public class TownQuestScreen extends Screen {
	private static final int W = 340;
	private static final int H = 226;
	private static final int LIST_X = 8;
	private static final int LIST_Y = 34;
	private static final int LIST_W = 128;
	private static final int ROW = 24;
	private static final int PANE_X = LIST_X + LIST_W + 8;
	private static final int PANE_W = W - PANE_X - 8;
	private static final int REWARD_Y = H - 66;
	/** The quest picked last at each resident (by role id), kept while the game runs. */
	private static final Map<String, String> PICKED = new HashMap<>();

	private OpenTownQuestPayload info;
	private CityNpc.Role role;
	private List<Quest> quests;
	/** What the buttons were built for: every quest's status (a delivery turns ready when the items arrive, which the quest data does not show). */
	private String shown = "";
	private boolean abandonArmed;
	private int left;
	private int top;
	private int age;

	public TownQuestScreen(final OpenTownQuestPayload info) {
		super(Component.translatable("screen.minecraft_mode.townquest.title"));
		this.setInfo(info);
	}

	private void setInfo(final OpenTownQuestPayload info) {
		this.info = info;
		this.role = CityNpc.Role.byId(info.role());
		this.quests = TownQuests.of(this.role);
	}

	public int entityId() {
		return this.info.entityId();
	}

	public void update(final OpenTownQuestPayload info) {
		this.setInfo(info);
		this.age = 0;
		this.abandonArmed = false;
		this.rebuildWidgets();
	}

	/** Game tests: the quest the next screen of its giver shows. */
	public static void pick(final String questId) {
		Quest quest = TownQuests.get(questId);
		if (quest != null) {
			PICKED.put(quest.giver().id(), questId);
		}
	}

	private Quest selected() {
		String id = PICKED.get(this.role.id());
		for (Quest quest : this.quests) {
			if (quest.id().equals(id)) {
				return quest;
			}
		}
		return this.quests.isEmpty() ? null : this.quests.getFirst();
	}

	private Status status(final LocalPlayer player, final Quest quest) {
		return TownQuestService.status(player, quest, this.info.day());
	}

	@Override
	protected void init() {
		this.left = (this.width - W) / 2;
		this.top = (this.height - H) / 2;
		LocalPlayer player = this.minecraft.player;
		if (player == null) {
			return;
		}
		this.shown = this.signature(player);
		Quest quest = this.selected();
		int by = this.top + H - 24;
		int right = this.left + W - 8;
		if (quest != null) {
			Status status = this.status(player, quest);
			if (status == Status.AVAILABLE) {
				this.addRenderableWidget(Button.builder(Component.translatable("screen.minecraft_mode.townquest.accept"), b -> this.send(TownQuestActionPayload.ACCEPT, quest))
					.bounds(right - 76, by, 76, 18).build());
			}
			if (status == Status.READY) {
				this.addRenderableWidget(Button.builder(Component.translatable("screen.minecraft_mode.townquest.turn_in").withStyle(ChatFormatting.GREEN),
					b -> this.send(TownQuestActionPayload.TURN_IN, quest)).bounds(right - 76, by, 76, 18).build());
			}
			if (status == Status.ACTIVE || status == Status.READY) {
				int x = status == Status.READY ? right - 76 - 4 - 64 : right - 64;
				this.addRenderableWidget(Button.builder(this.abandonArmed ? Component.translatable("screen.minecraft_mode.townquest.abandon_confirm").withStyle(ChatFormatting.RED)
					: Component.translatable("screen.minecraft_mode.townquest.abandon"), b -> {
						if (this.abandonArmed) {
							this.abandonArmed = false;
							this.send(TownQuestActionPayload.ABANDON, quest);
						} else {
							this.abandonArmed = true;
							this.rebuildWidgets();
						}
					}).bounds(x, by, 64, 18).build());
			}
			if (quest.goal() == TownQuests.Goal.JUMP && status != Status.LOCKED && status != Status.READY) {
				int x = status == Status.AVAILABLE ? right - 76 - 4 - 100 : status == Status.ACTIVE ? right - 64 - 4 - 100 : right - 100;
				this.addRenderableWidget(Button.builder(Component.translatable("screen.minecraft_mode.townquest.enter"), b -> {
					this.send(TownQuestActionPayload.ENTER, quest);
					this.onClose();
				}).bounds(x, by, 100, 18).build());
			}
		}
		this.addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> this.onClose())
			.bounds(this.left + LIST_X, by, 60, 18).build());
	}

	private void send(final int action, final Quest quest) {
		if (ClientPlayNetworking.canSend(TownQuestActionPayload.TYPE)) {
			ClientPlayNetworking.send(new TownQuestActionPayload(this.info.entityId(), action, quest.id()));
		}
	}

	@Override
	public void tick() {
		this.age++;
		LocalPlayer player = this.minecraft.player;
		if (player != null && !this.signature(player).equals(this.shown)) {
			this.rebuildWidgets();
		}
	}

	private String signature(final LocalPlayer player) {
		StringBuilder out = new StringBuilder();
		for (Quest quest : this.quests) {
			out.append(this.status(player, quest).ordinal());
		}
		return out.toString();
	}

	private int rowAt(final double mouseX, final double mouseY) {
		int x = this.left + LIST_X;
		int y = this.top + LIST_Y;
		if (mouseX < x || mouseX >= x + LIST_W || mouseY < y) {
			return -1;
		}
		int index = (int)((mouseY - y) / ROW);
		return index < this.quests.size() ? index : -1;
	}

	@Override
	public boolean mouseClicked(final MouseButtonEvent event, final boolean doubleClick) {
		int row = this.rowAt(event.x(), event.y());
		if (row >= 0) {
			PICKED.put(this.role.id(), this.quests.get(row).id());
			this.abandonArmed = false;
			this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
			this.rebuildWidgets();
			return true;
		}
		return super.mouseClicked(event, doubleClick);
	}

	private static ItemStack icon(final Quest quest) {
		return switch (quest.goal()) {
			case KILL -> new ItemStack(Items.IRON_SWORD);
			case KILL_NAMED -> new ItemStack(Items.WITHER_SKELETON_SKULL);
			case KILL_LORD -> new ItemStack(Items.GOLDEN_HELMET);
			case DELIVER -> new ItemStack(TownQuestService.item(quest.target()));
			case MINE_ORE -> new ItemStack(Items.IRON_PICKAXE);
			case FISH -> new ItemStack(Items.FISHING_ROD);
			case HARVEST -> new ItemStack(Items.IRON_HOE);
			case CRAFT -> new ItemStack(Items.CAMPFIRE);
			case CLEAR_LAIR -> new ItemStack(ModItems.LAIR_MAP);
			case DUNGEON -> new ItemStack(Items.TRIAL_KEY);
			case JUMP -> new ItemStack(Items.FEATHER);
		};
	}

	private Component statusLabel(final LocalPlayer player, final Quest quest, final Status status) {
		return switch (status) {
			case LOCKED -> Component.translatable("screen.minecraft_mode.townquest.status.locked", quest.minLevel()).withStyle(ChatFormatting.DARK_GRAY);
			case AVAILABLE -> Component.translatable("screen.minecraft_mode.townquest.status.available").withStyle(ChatFormatting.YELLOW);
			case ACTIVE -> Component.translatable("screen.minecraft_mode.townquest.status.active", TownQuestService.progress(player, quest), quest.need())
				.withStyle(ChatFormatting.WHITE);
			case READY -> Component.translatable("screen.minecraft_mode.townquest.status.ready").withStyle(ChatFormatting.GREEN);
			case DONE -> Component.translatable(quest.daily() ? "screen.minecraft_mode.townquest.status.done" : "screen.minecraft_mode.townquest.status.done_once")
				.withStyle(ChatFormatting.DARK_GRAY);
		};
	}

	@Override
	public void extractRenderState(final GuiGraphicsExtractor g, final int mouseX, final int mouseY, final float a) {
		LocalPlayer player = this.minecraft.player;
		if (player == null) {
			return;
		}
		int x = this.left;
		int y = this.top;
		g.fill(x, y, x + W, y + H, 0xE8101018);
		g.outline(x, y, W, H, 0xFF000000 | this.role.color());
		Component name = Component.translatable(this.role.nameKey()).withColor(this.role.color());
		g.text(this.font, name, x + 10, y + 8, 0xFFFFFFFF, true);
		Component title = this.title.copy().withStyle(ChatFormatting.GRAY);
		g.text(this.font, title, x + W - 10 - this.font.width(title), y + 8, 0xFFFFFFFF, false);
		// the greeting on one line, cut with an ellipsis (the whole of it is in chat when the resident is clicked)
		String greeting = Component.translatable(this.role.greetingKey()).getString();
		if (this.font.width(greeting) > W - 20) {
			greeting = this.font.plainSubstrByWidth(greeting, W - 20 - this.font.width("...")).stripTrailing() + "...";
		}
		g.text(this.font, Component.literal(greeting).withStyle(ChatFormatting.ITALIC), x + 10, y + 20, 0xFF9AA8B8, false);

		// the list
		Quest picked = this.selected();
		for (int i = 0; i < this.quests.size(); i++) {
			Quest quest = this.quests.get(i);
			Status status = this.status(player, quest);
			int rx = x + LIST_X;
			int ry = y + LIST_Y + i * ROW;
			boolean hover = this.rowAt(mouseX, mouseY) == i;
			g.fill(rx, ry, rx + LIST_W, ry + ROW - 2, quest == picked ? 0xA0283040 : hover ? 0x30FFFFFF : 0x40000000);
			if (quest == picked) {
				g.outline(rx, ry, LIST_W, ROW - 2, 0xFF000000 | this.role.color());
			}
			g.fakeItem(icon(quest), rx + 2, ry + 3);
			int textX = rx + 21;
			int textW = LIST_W - 23;
			int color = status == Status.DONE || status == Status.LOCKED ? 0xFF808080 : 0xFFFFFFFF;
			String label = (quest.daily() ? "" : "★ ") + Component.translatable(quest.titleKey()).getString();
			g.text(this.font, this.font.plainSubstrByWidth(label, textW), textX, ry + 2, color, false);
			g.text(this.font, this.statusLabel(player, quest, status), textX, ry + 12, 0xFFFFFFFF, false);
		}

		// the picked quest
		if (picked != null) {
			this.pane(g, player, picked);
		}

		Component reset = Component.translatable("screen.minecraft_mode.townquest.reset", RealReset.remaining(Math.max(0L, this.info.dayLeft() - this.age)));
		// under the list, above the Done button (the bottom row belongs to the buttons)
		List<FormattedCharSequence> resetLines = this.font.split(reset, LIST_W);
		for (int i = 0; i < resetLines.size(); i++) {
			g.text(this.font, resetLines.get(i), x + LIST_X, y + H - 28 - 10 * (resetLines.size() - i), 0xFF707070, false);
		}
		super.extractRenderState(g, mouseX, mouseY, a);
	}

	private void pane(final GuiGraphicsExtractor g, final LocalPlayer player, final Quest quest) {
		Status status = this.status(player, quest);
		int px = this.left + PANE_X;
		int py = this.top + LIST_Y;
		g.fill(px - 2, py, px + PANE_W, this.top + H - 28, 0x40000000);
		int tx = px + 4;
		int tw = PANE_W - 8;
		int ty = py + 4;
		g.text(this.font, this.font.split(Component.translatable(quest.titleKey()), tw).getFirst(), tx, ty, 0xFFFFD27F, true);
		ty += 11;
		Component kind = Component.translatable(quest.daily() ? "screen.minecraft_mode.townquest.daily" : "screen.minecraft_mode.townquest.once");
		if (quest.minLevel() > 1) {
			kind = kind.copy().append(" · ").append(Component.translatable("screen.minecraft_mode.townquest.level", quest.minLevel()));
		}
		g.text(this.font, kind, tx, ty, 0xFF8A8A8A, false);
		ty += 12;
		// the objective and its progress
		int done = status == Status.ACTIVE || status == Status.READY ? TownQuestService.progress(player, quest) : status == Status.DONE ? quest.need() : 0;
		String count = done + "/" + quest.need();
		int countW = this.font.width(count);
		List<FormattedCharSequence> objective = this.font.split(Component.literal("▶ ").append(TownQuestService.describe(quest)), tw - countW - 4);
		for (FormattedCharSequence line : objective) {
			g.text(this.font, line, tx, ty, 0xFFFFFFFF, false);
			if (line == objective.getFirst()) {
				g.text(this.font, count, tx + tw - countW, ty, done >= quest.need() ? 0xFF7CFC7C : 0xFFBBBBBB, false);
			}
			ty += 10;
		}
		ty += 3;
		// what the giver says, then where the quest stands
		Component note = switch (status) {
			case LOCKED -> Component.translatable("screen.minecraft_mode.townquest.locked", quest.minLevel()).withStyle(ChatFormatting.RED);
			case DONE -> quest.daily()
				? Component.translatable("screen.minecraft_mode.townquest.done_today", RealReset.remaining(Math.max(0L, this.info.dayLeft() - this.age)))
				: Component.translatable("screen.minecraft_mode.townquest.done_once");
			case READY -> Component.translatable("screen.minecraft_mode.townquest.ready").withStyle(ChatFormatting.GREEN);
			default -> quest.goal() == TownQuests.Goal.DELIVER ? Component.translatable("screen.minecraft_mode.townquest.deliver_hint") : null;
		};
		int noteLines = note == null ? 0 : this.font.split(note, tw).size();
		int room = Math.max(1, (this.top + REWARD_Y - 4 - ty) / 10 - noteLines);
		List<FormattedCharSequence> text = this.font.split(Component.translatable(quest.textKey()).withStyle(ChatFormatting.ITALIC), tw);
		for (int i = 0; i < text.size() && i < room; i++) {
			g.text(this.font, text.get(i), tx, ty, 0xFFC8C8C8, false);
			ty += 10;
		}
		if (note != null) {
			for (FormattedCharSequence line : this.font.split(note, tw)) {
				g.text(this.font, line, tx, ty, 0xFF9A9A9A, false);
				ty += 10;
			}
		}
		// the reward
		int ry = this.top + REWARD_Y;
		g.fill(tx, ry - 2, tx + tw, ry - 1, 0x30FFFFFF);
		TownQuestService.Reward reward = TownQuestService.reward(player, quest);
		g.text(this.font, Component.translatable("screen.minecraft_mode.townquest.reward").withStyle(ChatFormatting.YELLOW), tx, ry + 2, 0xFFFFFFFF, false);
		Component exp = Component.translatable("screen.minecraft_mode.townquest.reward_exp", reward.exp()).withStyle(ChatFormatting.GREEN);
		g.text(this.font, exp, tx, ry + 14, 0xFFFFFFFF, false);
		Component coins = Component.literal("◎ ").append(Coins.component(reward.coins()));
		g.text(this.font, coins, tx, ry + 25, 0xFFFFD27F, false);
		// the items, right-aligned beside the lines
		int ix = tx + tw - 18;
		if (reward.bonus() != null) {
			ItemStack bonus = new ItemStack(reward.bonus(), reward.bonusCount());
			g.fakeItem(bonus, ix, ry + 12);
			g.itemDecorations(this.font, bonus, ix, ry + 12);
			ix -= 20;
		}
		if (reward.stones() > 0) {
			ItemStack stones = new ItemStack(ModItems.ENHANCEMENT_STONE, reward.stones());
			g.fakeItem(stones, ix, ry + 12);
			g.itemDecorations(this.font, stones, ix, ry + 12);
		}
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
