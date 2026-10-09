package com.minecraftmode.client.endgame;

import com.minecraftmode.bounty.Bounties;
import com.minecraftmode.bounty.BountyData;
import com.minecraftmode.bounty.BountyKind;
import com.minecraftmode.job.JobProgression;
import com.minecraftmode.loot.Coins;
import com.minecraftmode.network.BountyActionPayload;
import com.minecraftmode.network.OpenBountyPayload;
import com.minecraftmode.progress.ResetCycle;
import com.minecraftmode.registry.ModItems;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * The guild clerk's board: the three daily bounties and the cycle bounty (progress, reward, hand-in button) with the time until
 * they renew, then the merit shop below. Bounties and merit come from the synced player data; the clerk sends the timers.
 */
public class BountyScreen extends Screen {
	private static final int W = 320;
	private static final int H = 236;
	private static final int ROW = 23;
	private static final int ROWS_Y = 24;
	private static final int SHOP_Y = 134;
	/** The merit shop is one row of offers: icon, then the buy button with the price. */
	private static final int CELL_W = 27;

	private OpenBountyPayload info;
	private BountyData shown = BountyData.DEFAULT;
	private int left;
	private int top;
	private int age;

	public BountyScreen(final OpenBountyPayload info) {
		super(Component.translatable("screen.minecraft_mode.bounty.title"));
		this.info = info;
	}

	public int clerk() {
		return this.info.entityId();
	}

	public void update(final OpenBountyPayload info) {
		this.info = info;
		this.age = 0;
		this.rebuildWidgets();
	}

	private int level() {
		LocalPlayer player = this.minecraft == null ? null : this.minecraft.player;
		return player == null ? 1 : JobProgression.get(player).level();
	}

	@Override
	protected void init() {
		this.left = (this.width - W) / 2;
		this.top = (this.height - H) / 2;
		LocalPlayer player = this.minecraft.player;
		if (player == null) {
			return;
		}
		BountyData data = Bounties.get(player);
		this.shown = data;
		for (int i = 0; i <= Bounties.SPECIAL; i++) {
			BountyData.Bounty bounty = data.get(i);
			final int index = i;
			Button claim = Button.builder(Component.translatable(bounty.claimed() ? "screen.minecraft_mode.bounty.done" : "screen.minecraft_mode.bounty.claim"),
				b -> send(BountyActionPayload.CLAIM, index)).bounds(this.left + W - 62, this.top + ROWS_Y + i * ROW + 1, 54, 18).build();
			claim.active = !bounty.isEmpty() && !bounty.claimed() && Bounties.ready(player, bounty);
			this.addRenderableWidget(claim);
		}
		for (int i = 0; i < Bounties.SHOP.size(); i++) {
			Bounties.Offer offer = Bounties.SHOP.get(i);
			final int index = i;
			Button buy = Button.builder(Component.literal(offer.cost() + "★"), b -> send(BountyActionPayload.BUY, index))
				.bounds(this.left + 9 + i * CELL_W, this.top + SHOP_Y + 34, CELL_W - 2, 16).build();
			buy.active = data.merit() >= offer.cost();
			this.addRenderableWidget(buy);
		}
		this.addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> this.onClose()).bounds(this.left + W - 68, this.top + H - 22, 60, 18).build());
	}

	private void send(final int action, final int index) {
		if (ClientPlayNetworking.canSend(BountyActionPayload.TYPE)) {
			ClientPlayNetworking.send(new BountyActionPayload(this.info.entityId(), action, index));
		}
	}

	@Override
	public void tick() {
		this.age++;
		LocalPlayer player = this.minecraft.player;
		if (player != null && (!Bounties.get(player).equals(this.shown) || this.age % 20 == 0)) {
			this.rebuildWidgets();
		}
	}

	private static ItemStack icon(final BountyData.Bounty bounty) {
		return switch (bounty.type()) {
			case KILL_ANY -> new ItemStack(Items.IRON_SWORD);
			case KILL_TYPE -> new ItemStack(Items.SKELETON_SKULL);
			case KILL_NAMED -> new ItemStack(Items.WITHER_SKELETON_SKULL);
			case CLEAR_LAIR -> new ItemStack(ModItems.LAIR_MAP);
			case MINE_ORE -> new ItemStack(Items.IRON_PICKAXE);
			case RAID -> new ItemStack(Items.NETHERITE_SWORD);
			case DELIVER -> new ItemStack(Bounties.item(bounty.target()));
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
		g.outline(x, y, W, H, 0xFF3A6ED8);
		g.text(this.font, this.title.copy().withStyle(ChatFormatting.GOLD), x + 10, y + 9, 0xFFFFFFFF, true);
		BountyData data = Bounties.get(player);
		String merit = Component.translatable("screen.minecraft_mode.bounty.merit", data.merit()).getString();
		g.text(this.font, merit, x + W - 10 - this.font.width(merit), y + 9, 0xFFFFD27F, false);

		List<Component> tip = null;
		int level = this.level();
		for (int i = 0; i <= Bounties.SPECIAL; i++) {
			BountyData.Bounty bounty = data.get(i);
			boolean special = i == Bounties.SPECIAL;
			int ry = y + ROWS_Y + i * ROW;
			g.fill(x + 8, ry, x + W - 8, ry + ROW - 2, special ? 0x50C08A2A : 0x40000000);
			if (bounty.isEmpty()) {
				g.text(this.font, Component.translatable("screen.minecraft_mode.bounty.none"), x + 30, ry + 6, 0xFF707070, false);
				continue;
			}
			g.fakeItem(icon(bounty), x + 10, ry + 2);
			int done = bounty.type() == BountyKind.DELIVER ? Math.min(bounty.need(), Bounties.delivered(player, bounty)) : Math.min(bounty.need(), bounty.progress());
			Component name = Bounties.describe(bounty);
			int textW = W - 108;
			String label = (special ? "★ " : "") + name.getString();
			g.text(this.font, this.font.plainSubstrByWidth(label, textW - 34), x + 30, ry + 2, bounty.claimed() ? 0xFF707070 : special ? 0xFFFFD27F : 0xFFFFFFFF, false);
			String count = done + "/" + bounty.need();
			g.text(this.font, count, x + 30 + textW - this.font.width(count), ry + 2, done >= bounty.need() ? 0xFF7CFC7C : 0xFFBBBBBB, false);
			Bounties.Reward reward = Bounties.reward(special, level);
			String pay = "◎" + Coins.format(reward.coins()) + "  ★" + reward.merit() + "  ✦" + reward.ether() + "  ◆" + reward.stones();
			g.text(this.font, pay, x + 30, ry + 12, 0xFF9A9A9A, false);
			if (mouseX >= x + 8 && mouseX < x + W - 66 && mouseY >= ry && mouseY < ry + ROW - 2) {
				tip = List.of(name.copy().withStyle(ChatFormatting.WHITE),
					Component.translatable("screen.minecraft_mode.bounty.reward").withStyle(ChatFormatting.GRAY),
					Component.literal(" ◎ ").append(Coins.component(reward.coins())),
					Component.translatable("screen.minecraft_mode.bounty.reward_merit", reward.merit()).withStyle(ChatFormatting.YELLOW),
					Component.translatable("screen.minecraft_mode.bounty.reward_ether", reward.ether()).withStyle(ChatFormatting.LIGHT_PURPLE),
					Component.translatable("screen.minecraft_mode.bounty.reward_stones", reward.stones()).withStyle(ChatFormatting.AQUA),
					Component.translatable(bounty.type() == BountyKind.DELIVER ? "screen.minecraft_mode.bounty.deliver_hint" : "screen.minecraft_mode.bounty.kind_hint")
						.withStyle(ChatFormatting.DARK_GRAY));
			}
		}
		int ty = y + ROWS_Y + 4 * ROW;
		long dayLeft = Math.max(0L, this.info.dayLeft() - this.age);
		long cycleLeft = Math.max(0L, this.info.cycleLeft() - this.age);
		g.text(this.font, Component.translatable("screen.minecraft_mode.bounty.daily_reset", ResetCycle.remaining(dayLeft)), x + 10, ty + 1, 0xFF8A8A8A, false);
		Component cycle = Component.translatable("screen.minecraft_mode.bounty.cycle_reset", ResetCycle.remaining(cycleLeft));
		g.text(this.font, cycle, x + W - 10 - this.font.width(cycle), ty + 1, 0xFF8A8A8A, false);

		g.fill(x + 8, y + SHOP_Y - 4, x + W - 8, y + SHOP_Y - 3, 0x40FFFFFF);
		g.text(this.font, Component.translatable("screen.minecraft_mode.bounty.shop").withStyle(ChatFormatting.YELLOW), x + 10, y + SHOP_Y, 0xFFFFFFFF, false);
		RandomSource preview = RandomSource.create(7L);
		for (int i = 0; i < Bounties.SHOP.size(); i++) {
			Bounties.Offer offer = Bounties.SHOP.get(i);
			int cx = x + 9 + i * CELL_W;
			int cy = y + SHOP_Y + 12;
			g.fill(cx, cy, cx + CELL_W - 2, cy + 20, 0x40000000);
			ItemStack stack = offer.item().make(this.minecraft.player, level, preview);
			if (stack.isEmpty()) {
				stack = new ItemStack(Items.BARRIER); // nothing left to give (pets or mount already owned)
			}
			g.fakeItem(stack, cx + (CELL_W - 2 - 16) / 2, cy + 2);
			g.itemDecorations(this.font, stack, cx + (CELL_W - 2 - 16) / 2, cy + 2);
			if (mouseX >= cx && mouseX < cx + CELL_W - 2 && mouseY >= cy && mouseY < cy + 20) {
				List<Component> lines = new ArrayList<>();
				lines.add(Component.translatable(offer.nameKey()).withStyle(ChatFormatting.WHITE));
				lines.addAll(getTooltipFromItem(this.minecraft, stack).stream().skip(1).toList());
				lines.add(Component.translatable("screen.minecraft_mode.bounty.cost", offer.cost()).withStyle(ChatFormatting.YELLOW));
				tip = lines;
			}
		}
		g.text(this.font, Component.translatable("screen.minecraft_mode.bounty.legend"), x + 10, y + H - 16, 0xFF6A6A6A, false);
		super.extractRenderState(g, mouseX, mouseY, a);
		if (tip != null) {
			g.setComponentTooltipForNextFrame(this.font, tip, mouseX, mouseY);
		}
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
