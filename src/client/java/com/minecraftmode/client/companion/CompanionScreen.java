package com.minecraftmode.client.companion;

import com.minecraftmode.client.job.JobTooltips;
import com.minecraftmode.companion.Companions;
import com.minecraftmode.job.gear.StatLine;
import com.minecraftmode.network.ProgressActionPayload;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;

/**
 * The companion collection (P): every pet with its level and bonus (summon or dismiss), every mount (ride). Unknown ones show
 * where they are found.
 */
public class CompanionScreen extends Screen {
	private static final int W = 340;
	private static final int H = 226;
	private static final int ROW = 20;
	private static final int ROWS_Y = 38;
	private static final int MOUNT_X = 182;

	private Companions.Data shown = Companions.Data.DEFAULT;
	private int left;
	private int top;

	public CompanionScreen() {
		super(Component.translatable("screen.minecraft_mode.companion.title"));
	}

	@Override
	protected void init() {
		this.left = (this.width - W) / 2;
		this.top = (this.height - H) / 2;
		LocalPlayer player = this.minecraft.player;
		if (player == null) {
			return;
		}
		Companions.Data data = Companions.data(player);
		this.shown = data;
		List<Companions.PetDef> pets = new ArrayList<>(Companions.pets());
		for (int i = 0; i < pets.size(); i++) {
			Companions.PetDef def = pets.get(i);
			boolean active = data.activePet().equals(def.id());
			Button b = Button.builder(Component.translatable(active ? "screen.minecraft_mode.companion.dismiss" : "screen.minecraft_mode.companion.summon"),
				button -> CompanionClient.send(ProgressActionPayload.PET, active ? "" : def.id()))
				.bounds(this.left + MOUNT_X - 54, this.top + ROWS_Y + i * ROW + 1, 48, 17).build();
			b.active = data.hasPet(def.id());
			this.addRenderableWidget(b);
		}
		List<Companions.MountDef> mounts = new ArrayList<>(Companions.mounts());
		for (int i = 0; i < mounts.size(); i++) {
			Companions.MountDef def = mounts.get(i);
			Button b = Button.builder(Component.translatable("screen.minecraft_mode.companion.ride"), button -> {
				CompanionClient.send(ProgressActionPayload.MOUNT, def.id());
				this.onClose();
			}).bounds(this.left + W - 52, this.top + ROWS_Y + i * ROW + 1, 44, 17).build();
			b.active = data.hasMount(def.id()) && !player.isPassenger();
			this.addRenderableWidget(b);
		}
		this.addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> this.onClose()).bounds(this.left + W - 68, this.top + H - 22, 60, 18).build());
	}

	@Override
	public void tick() {
		LocalPlayer player = this.minecraft.player;
		if (player != null && !Companions.data(player).equals(this.shown)) {
			this.rebuildWidgets();
		}
	}

	@Override
	public void extractRenderState(final GuiGraphicsExtractor g, final int mouseX, final int mouseY, final float a) {
		LocalPlayer player = this.minecraft.player;
		if (player == null) {
			return;
		}
		int x = this.left;
		int y = this.top;
		Companions.Data data = Companions.data(player);
		g.fill(x, y, x + W, y + H, 0xE8101018);
		g.outline(x, y, W, H, 0xFFE8C24A);
		g.text(this.font, this.title.copy().withStyle(ChatFormatting.GOLD), x + 10, y + 9, 0xFFFFFFFF, true);
		List<Component> tip = null;

		List<Companions.PetDef> pets = new ArrayList<>(Companions.pets());
		g.text(this.font, Component.translatable("screen.minecraft_mode.companion.pets", data.pets().size(), pets.size()), x + 10, y + 25, 0xFFFFD27F, false);
		for (int i = 0; i < pets.size(); i++) {
			Companions.PetDef def = pets.get(i);
			int ry = y + ROWS_Y + i * ROW;
			boolean owned = data.hasPet(def.id());
			boolean active = data.activePet().equals(def.id());
			g.fill(x + 8, ry, x + MOUNT_X - 4, ry + ROW - 1, active ? 0x40E8C24A : owned ? 0x30FFFFFF : 0x40000000);
			g.fakeItem(new ItemStack(Companions.petItem(def.id())), x + 10, ry + 1);
			if (!owned) {
				g.fill(x + 10, ry + 1, x + 26, ry + 17, 0xB0101018);
			}
			Component name = owned ? Component.translatable(def.nameKey()) : Component.literal("???");
			g.text(this.font, this.font.plainSubstrByWidth(name.getString(), 92), x + 30, ry + 2, owned ? color(def.rarity()) : 0xFF6A6A6A, false);
			if (owned) {
				int exp = data.pets().get(def.id());
				int level = Companions.petLevel(exp);
				g.text(this.font, "Lv " + level, x + 30, ry + 11, 0xFFBBBBBB, false);
				int barX = x + 56;
				int barW = 64;
				int from = Companions.petExpFor(level);
				int to = Companions.petExpFor(Math.min(Companions.MAX_PET_LEVEL, level + 1));
				int fill = level >= Companions.MAX_PET_LEVEL ? barW : barW * (exp - from) / Math.max(1, to - from);
				g.fill(barX, ry + 13, barX + barW, ry + 16, 0xFF2A2A30);
				g.fill(barX, ry + 13, barX + fill, ry + 16, 0xFF5AB0E8);
			}
			if (mouseX >= x + 8 && mouseX < x + MOUNT_X - 58 && mouseY >= ry && mouseY < ry + ROW - 1) {
				tip = this.petTip(def, data);
			}
		}

		List<Companions.MountDef> mounts = new ArrayList<>(Companions.mounts());
		g.text(this.font, Component.translatable("screen.minecraft_mode.companion.mounts", data.mounts().size(), mounts.size()), x + MOUNT_X + 2, y + 25, 0xFFFFD27F, false);
		for (int i = 0; i < mounts.size(); i++) {
			Companions.MountDef def = mounts.get(i);
			int ry = y + ROWS_Y + i * ROW;
			boolean owned = data.hasMount(def.id());
			g.fill(x + MOUNT_X, ry, x + W - 8, ry + ROW - 1, owned ? 0x30FFFFFF : 0x40000000);
			g.fakeItem(new ItemStack(Companions.mountItem(def.id())), x + MOUNT_X + 2, ry + 1);
			if (!owned) {
				g.fill(x + MOUNT_X + 2, ry + 1, x + MOUNT_X + 18, ry + 17, 0xB0101018);
			}
			Component name = owned ? Component.translatable(def.nameKey()) : Component.literal("???");
			g.text(this.font, this.font.plainSubstrByWidth(name.getString(), 96), x + MOUNT_X + 22, ry + 2, owned ? color(def.rarity()) : 0xFF6A6A6A, false);
			if (owned) {
				Component speed = Component.translatable(def.flying() ? "screen.minecraft_mode.companion.flying" : "screen.minecraft_mode.companion.speed", speed(def));
				g.text(this.font, this.font.plainSubstrByWidth(speed.getString(), W - 56 - MOUNT_X - 22), x + MOUNT_X + 22, ry + 11, 0xFF9AD0FF, false);
			}
			if (mouseX >= x + MOUNT_X && mouseX < x + W - 54 && mouseY >= ry && mouseY < ry + ROW - 1) {
				tip = this.mountTip(def, owned);
			}
		}
		int hintY = y + ROWS_Y + mounts.size() * ROW + 6;
		g.text(this.font, Component.translatable("screen.minecraft_mode.companion.mount_key"), x + MOUNT_X + 2, hintY, 0xFF8A8A8A, false);

		// the summoned pet's bonus
		List<StatLine> bonus = Companions.lines(player);
		Component footer = bonus.isEmpty() ? Component.translatable("screen.minecraft_mode.companion.no_pet")
			: Component.translatable("screen.minecraft_mode.companion.bonus", describe(bonus));
		g.text(this.font, this.font.plainSubstrByWidth(footer.getString(), W - 90), x + 10, y + H - 17, bonus.isEmpty() ? 0xFF6A6A6A : 0xFF7CFC7C, false);

		super.extractRenderState(g, mouseX, mouseY, a);
		if (tip != null) {
			g.setComponentTooltipForNextFrame(this.font, tip, mouseX, mouseY);
		}
	}

	private List<Component> petTip(final Companions.PetDef def, final Companions.Data data) {
		List<Component> lines = new ArrayList<>();
		if (!data.hasPet(def.id())) {
			lines.add(Component.translatable("screen.minecraft_mode.companion.unknown").withStyle(ChatFormatting.GRAY));
			lines.add(Component.translatable("screen.minecraft_mode.companion.source." + source(def.rarity(), true)).withStyle(ChatFormatting.DARK_GRAY));
			return lines;
		}
		int level = Companions.petLevel(data.pets().get(def.id()));
		lines.add(Component.translatable(def.nameKey()).withStyle(style(def.rarity())));
		lines.add(Component.translatable(def.nameKey() + ".desc").withStyle(ChatFormatting.GRAY));
		lines.add(Component.translatable("screen.minecraft_mode.companion.level", level, Companions.MAX_PET_LEVEL).withStyle(ChatFormatting.YELLOW));
		lines.add(Component.translatable("screen.minecraft_mode.companion.bonus", describe(def.lines(level))).withStyle(ChatFormatting.AQUA));
		if (level < Companions.MAX_PET_LEVEL) {
			lines.add(Component.translatable("screen.minecraft_mode.companion.max_bonus", describe(def.lines(Companions.MAX_PET_LEVEL))).withStyle(ChatFormatting.DARK_AQUA));
			lines.add(Component.translatable("screen.minecraft_mode.companion.grow").withStyle(ChatFormatting.DARK_GRAY));
		}
		return lines;
	}

	private List<Component> mountTip(final Companions.MountDef def, final boolean owned) {
		List<Component> lines = new ArrayList<>();
		if (!owned) {
			lines.add(Component.translatable("screen.minecraft_mode.companion.unknown").withStyle(ChatFormatting.GRAY));
			lines.add(Component.translatable("screen.minecraft_mode.companion.source." + source(def.rarity(), false)).withStyle(ChatFormatting.DARK_GRAY));
			return lines;
		}
		lines.add(Component.translatable(def.nameKey()).withStyle(style(def.rarity())));
		lines.add(Component.translatable(def.nameKey() + ".desc").withStyle(ChatFormatting.GRAY));
		lines.add(Component.translatable(def.flying() ? "screen.minecraft_mode.companion.flying" : "screen.minecraft_mode.companion.speed", speed(def))
			.withStyle(ChatFormatting.AQUA));
		return lines;
	}

	/** Where companions of a rarity come from: uncommon pets and the stallion from the merit shop, rare from named monsters and lairs, epic from raids and world bosses. */
	private static String source(final Rarity rarity, final boolean pet) {
		return switch (rarity) {
			case EPIC -> "epic";
			case RARE -> "rare";
			default -> pet ? "merit_pet" : "merit_mount";
		};
	}

	private static Component describe(final List<StatLine> lines) {
		Component out = Component.empty();
		for (int i = 0; i < lines.size(); i++) {
			if (i > 0) {
				out = out.copy().append(", ");
			}
			out = out.copy().append(Component.translatable(lines.get(i).stat().key(), JobTooltips.num(lines.get(i).value())));
		}
		return out;
	}

	private static long speed(final Companions.MountDef def) {
		return Math.round(def.speed() * 43.17);
	}

	private static int color(final Rarity rarity) {
		return switch (rarity) {
			case EPIC -> 0xFFE070FF;
			case RARE -> 0xFF6FD0FF;
			case UNCOMMON -> 0xFFFFE070;
			default -> 0xFFFFFFFF;
		};
	}

	private static ChatFormatting style(final Rarity rarity) {
		return switch (rarity) {
			case EPIC -> ChatFormatting.LIGHT_PURPLE;
			case RARE -> ChatFormatting.AQUA;
			case UNCOMMON -> ChatFormatting.YELLOW;
			default -> ChatFormatting.WHITE;
		};
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
