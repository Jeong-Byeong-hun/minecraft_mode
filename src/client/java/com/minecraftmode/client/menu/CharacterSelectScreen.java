package com.minecraftmode.client.menu;

import com.minecraftmode.character.Characters;
import com.minecraftmode.job.JobClass;
import com.minecraftmode.network.CharacterListPayload;
import com.minecraftmode.network.MenuActionPayload;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * The select screen in the lobby after joining with more than one character slot ({@code character/Characters#choosing}): a card
 * per slot with the character's figure (the account's skin in that character's gear, following the mouse), its level and class,
 * and a button to play it. It stays open until the server lets the player in; Esc does nothing and "Done" goes on with the current
 * character.
 */
public class CharacterSelectScreen extends MenuScreenBase {
	private static final String K = "screen.minecraft_mode.menu.";
	private static final int W = 384;
	private static final int H = 224;
	private static final int CARD_W = 72;
	private static final int CARD_GAP = 4;
	private static final int FIGURE_H = 70;
	private static final int FIGURE_SIZE = 30;
	/** Card height: the figure, three lines of text and the button. */
	private static final int CARD_H = FIGURE_H + 55;
	private static final int FIGURE_ID = -1000;

	/** One figure per slot (null for an empty slot), rebuilt whenever new slots arrive. */
	private final List<@Nullable RemotePlayer> figures = new ArrayList<>();
	private int cardsX;
	private int cardsY;

	public CharacterSelectScreen() {
		super(Component.translatable(K + "character.select"), W, H);
	}

	/** "Lv.N class title" for a filled slot, "Empty" otherwise (also used by the adventurer's characters tab). */
	static Component describe(final CharacterListPayload.Entry e) {
		if (e.empty()) {
			return Component.translatable(K + "character.empty").withStyle(ChatFormatting.GRAY);
		}
		JobClass job = JobClass.values()[Math.floorMod(e.job(), JobClass.values().length)];
		return Component.literal("Lv." + e.level() + " ").withStyle(ChatFormatting.GRAY).append(Component.translatable(job.tierKey(e.tier())).withColor(job.color()));
	}

	@Override
	protected List<Component> tabs() {
		return List.of(Component.translatable(K + "characters"));
	}

	@Override
	protected void initTab(final int x, final int y) {
		CharacterListPayload list = MenuClient.characters();
		this.figures.clear();
		if (list == null) {
			this.label(x, y + 3, Component.translatable(K + "character.loading").withStyle(ChatFormatting.GRAY));
			return;
		}
		int count = list.entries().size();
		this.cardsX = this.left + (this.w - count * CARD_W - (count - 1) * CARD_GAP) / 2;
		this.cardsY = y;
		for (int slot = 0; slot < count; slot++) {
			CharacterListPayload.Entry e = list.entries().get(slot);
			this.figures.add(e.empty() ? null : this.figure(slot, e.gear()));
			boolean current = slot == list.active();
			Component text = Component.translatable(K + (current ? "character.continue" : e.empty() ? "character.new" : "character.play"));
			int index = slot;
			this.button(this.cardX(slot) + 2, y + FIGURE_H + 37, CARD_W - 4, current ? text.copy().withStyle(ChatFormatting.GREEN) : text,
				() -> MenuClient.send(MenuActionPayload.CHARACTER_ENTER, index));
		}
		int below = y + CARD_H + 4;
		if (count < list.maxSlots()) {
			this.confirm(x, below, 160, "character:buy", Component.translatable(K + "character.buy", list.price()).withStyle(ChatFormatting.GOLD),
				() -> MenuClient.send(MenuActionPayload.CHARACTER_BUY, 0));
		}
		this.label(x, below + 20, Component.translatable(K + "character.choose").withStyle(ChatFormatting.GRAY));
		this.label(x, below + 32, Component.translatable(K + "character.shared").withStyle(ChatFormatting.GRAY));
	}

	/**
	 * The account's figure (the local player's profile, so its skin) wearing {@code gear}. It is never added to the level, but the
	 * held-item renderer reads its entity id, so it gets one no real entity has.
	 */
	private @Nullable RemotePlayer figure(final int slot, final List<ItemStack> gear) {
		if (this.minecraft.level == null || this.minecraft.player == null) {
			return null;
		}
		RemotePlayer figure = new RemotePlayer(this.minecraft.level, this.minecraft.player.getGameProfile());
		figure.setId(FIGURE_ID - slot);
		for (int i = 0; i < Math.min(gear.size(), Characters.GEAR_SLOTS.size()); i++) {
			figure.setItemSlot(Characters.GEAR_SLOTS.get(i), gear.get(i));
		}
		return figure;
	}

	private int cardX(final int slot) {
		return this.cardsX + slot * (CARD_W + CARD_GAP);
	}

	@Override
	protected void extractExtra(final GuiGraphicsExtractor g, final int mouseX, final int mouseY) {
		CharacterListPayload list = MenuClient.characters();
		if (list == null) {
			return;
		}
		for (int slot = 0; slot < this.figures.size() && slot < list.entries().size(); slot++) {
			int x0 = this.cardX(slot);
			boolean current = slot == list.active();
			g.fill(x0, this.cardsY, x0 + CARD_W, this.cardsY + CARD_H, current ? 0x40507F3F : 0x30FFFFFF);
			g.outline(x0, this.cardsY, CARD_W, CARD_H, current ? 0xFF6FBF5F : 0x60FFFFFF);
			RemotePlayer figure = this.figures.get(slot);
			if (figure != null) {
				InventoryScreen.extractEntityInInventoryFollowsMouse(g, x0 + 2, this.cardsY + 2, x0 + CARD_W - 2, this.cardsY + FIGURE_H, FIGURE_SIZE,
					0.0625F, mouseX, mouseY, figure);
			}
			CharacterListPayload.Entry e = list.entries().get(slot);
			this.centered(g, Component.translatable(K + "character.slot", slot + 1), x0, this.cardsY + FIGURE_H + 2, 0xFFFFFFFF);
			if (e.empty()) {
				this.centered(g, describe(e), x0, this.cardsY + FIGURE_H + 13, 0xFFFFFFFF);
			} else {
				JobClass job = JobClass.values()[Math.floorMod(e.job(), JobClass.values().length)];
				this.centered(g, Component.literal("Lv." + e.level()).withStyle(ChatFormatting.GRAY), x0, this.cardsY + FIGURE_H + 13, 0xFFFFFFFF);
				this.centered(g, Component.translatable(job.tierKey(e.tier())), x0, this.cardsY + FIGURE_H + 24, 0xFF000000 | job.color());
			}
		}
	}

	/** {@code text} centred on the card, cut to the card's width (long class titles). */
	private void centered(final GuiGraphicsExtractor g, final Component text, final int cardX, final int y, final int color) {
		FormattedText fitted = this.font.substrByWidth(text, CARD_W - 4);
		int width = this.font.width(fitted);
		g.text(this.font, Language.getInstance().getVisualOrder(fitted), cardX + (CARD_W - width) / 2, y, color, false);
	}

	/** New character slots arrived from the server. */
	void refresh() {
		this.rebuildWidgets();
	}

	@Override
	public boolean shouldCloseOnEsc() {
		return false;
	}

	/** "Done" goes on with the current character; the server closes the screen once it lets the player in. */
	@Override
	public void onClose() {
		CharacterListPayload list = MenuClient.characters();
		if (list != null) {
			MenuClient.send(MenuActionPayload.CHARACTER_ENTER, list.active());
		}
	}
}
