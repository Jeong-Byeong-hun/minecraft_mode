package com.minecraftmode.client.guide;

import com.minecraftmode.city.CityZone;
import com.minecraftmode.entity.CityNpc;
import com.minecraftmode.entity.ClassTrainer;
import com.minecraftmode.job.JobClass;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

/**
 * Guide Nella's screen: topics on the left (getting started, classes, keys, directions, money, gear, named monsters, raids and
 * dungeons, bounties and the story, companions and crafts, events), the answer on the right. Directions list every service NPC
 * and trainer with its coordinates, read from {@link CityZone}.
 */
public class GuideScreen extends Screen {
	private static final int W = 340;
	private static final int H = 232;
	private static final int LIST_W = 104;
	private static final int ROW = 17;
	private static final int TEXT_X = LIST_W + 16;

	public enum Topic {
		START, CLASSES, KEYS, PLACES, MONEY, GEAR, NAMED, DUNGEONS, BOUNTIES, COMPANIONS, EVENTS;

		public String key() {
			return "guide.minecraft_mode." + this.name().toLowerCase(Locale.ROOT);
		}
	}

	private static Topic topic = Topic.START;
	private int left;
	private int top;

	public GuideScreen() {
		super(Component.translatable("screen.minecraft_mode.guide.title"));
	}

	/** Opens on {@code name} next time (also used by tests). */
	public static void showTopic(final Topic next) {
		topic = next;
	}

	@Override
	protected void init() {
		this.left = (this.width - W) / 2;
		this.top = (this.height - H) / 2;
		Topic[] topics = Topic.values();
		for (int i = 0; i < topics.length; i++) {
			Topic t = topics[i];
			Button b = Button.builder(Component.translatable(t.key()), button -> {
				topic = t;
				this.rebuildWidgets();
			}).bounds(this.left + 8, this.top + 24 + i * ROW, LIST_W, 16).build();
			b.active = t != topic;
			this.addRenderableWidget(b);
		}
		this.addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> this.onClose()).bounds(this.left + W - 68, this.top + H - 22, 60, 18).build());
	}

	@Override
	public void extractRenderState(final GuiGraphicsExtractor g, final int mouseX, final int mouseY, final float a) {
		int x = this.left;
		int y = this.top;
		g.fill(x, y, x + W, y + H, 0xE8101018);
		g.outline(x, y, W, H, 0xFF4AA8E8);
		g.text(this.font, this.title.copy().withStyle(ChatFormatting.AQUA), x + 10, y + 9, 0xFFFFFFFF, true);
		int tx = x + TEXT_X;
		int width = W - TEXT_X - 10;
		g.fill(tx - 4, y + 24, x + W - 6, y + H - 26, 0x40000000);
		g.text(this.font, Component.translatable(topic.key()).withStyle(ChatFormatting.GOLD), tx, y + 28, 0xFFFFFFFF, false);
		if (topic == Topic.PLACES) {
			int ly = y + 41;
			for (Component line : places()) {
				g.text(this.font, this.font.plainSubstrByWidth(line.getString(), width), tx, ly, 0xFFDDDDDD, false);
				ly += 9;
			}
		} else {
			g.textWithWordWrap(this.font, Component.translatable(topic.key() + ".text"), tx, y + 41, width, 0xFFDDDDDD);
		}
		super.extractRenderState(g, mouseX, mouseY, a);
	}

	/** "Name: x, z" for the plaza, every service NPC and every trainer. */
	private static List<Component> places() {
		List<Component> out = new ArrayList<>();
		BlockPos spawn = CityZone.spawn(0);
		out.add(line(Component.translatable("screen.minecraft_mode.guide.plaza"), spawn));
		for (CityNpc.Role role : CityNpc.Role.values()) {
			out.add(line(Component.translatable(role.nameKey()), CityZone.npcHome(role, 0)));
		}
		for (JobClass job : JobClass.PLAYABLE) {
			out.add(line(Component.translatable(ClassTrainer.nameKey(job)), CityZone.trainerHome(job, 0)));
		}
		return out;
	}

	private static Component line(final Component name, final BlockPos pos) {
		return Component.empty().append(name).append(": " + pos.getX() + ", " + pos.getZ());
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
