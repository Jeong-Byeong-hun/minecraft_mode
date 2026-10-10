package com.minecraftmode.client.guide;

import com.minecraftmode.city.CityZone;
import com.minecraftmode.city.TrainingGrounds;
import com.minecraftmode.entity.CityNpc;
import com.minecraftmode.entity.ClassTrainer;
import com.minecraftmode.job.JobClass;
import com.minecraftmode.job.JobData;
import com.minecraftmode.job.JobProgression;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

/**
 * Guide Nella's screen: topics on the left (getting started, leveling to 10, classes, keys, directions, money, gear, named monsters,
 * raids and dungeons, bounties and the story, companions and crafts, events), the answer on the right. Directions list every service
 * NPC and trainer (and the Training Grounds stairs) with its coordinates, read from {@link CityZone}; leveling shows the experience the
 * curve asks for and how much the player still needs.
 */
public class GuideScreen extends Screen {
	private static final int W = 340;
	private static final int H = 232;
	private static final int LIST_W = 104;
	private static final int ROW = 16;
	/** The level a class can be chosen at (the leveling topic counts up to it). */
	private static final int CLASS_LEVEL = 10;
	/** Experience of a zombie (= its max health), the leveling topic's yardstick. */
	private static final int ZOMBIE_EXP = 20;
	private static final int TEXT_X = LIST_W + 16;
	/** Line height of the directions list (one line per NPC, trainer and landmark). */
	private static final int PLACE_ROW = 8;

	public enum Topic {
		START, LEVELING, CLASSES, KEYS, PLACES, MONEY, GEAR, NAMED, DUNGEONS, BOUNTIES, COMPANIONS, EVENTS;

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
			}).bounds(this.left + 8, this.top + 24 + i * ROW, LIST_W, 15).build();
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
				ly += PLACE_ROW;
			}
		} else if (topic == Topic.LEVELING) {
			int total = expBetween(1, CLASS_LEVEL);
			int trainingExp = ZOMBIE_EXP * TrainingGrounds.EXP_MULTIPLIER;
			g.textWithWordWrap(this.font, Component.translatable(topic.key() + ".text", total, (total + ZOMBIE_EXP - 1) / ZOMBIE_EXP, (total + trainingExp - 1) / trainingExp),
				tx, y + 41, width, 0xFFDDDDDD);
			LocalPlayer player = this.minecraft.player;
			if (player != null) {
				JobData data = JobProgression.get(player);
				Component progress = data.level() >= CLASS_LEVEL
					? Component.translatable("screen.minecraft_mode.guide.leveling_done")
					: Component.translatable("screen.minecraft_mode.guide.leveling_left", data.level(), expBetween(data.level(), CLASS_LEVEL) - data.exp(), CLASS_LEVEL);
				g.textWithWordWrap(this.font, progress, tx, y + H - 46, width, 0xFF7CFC7C);
			}
		} else {
			g.textWithWordWrap(this.font, Component.translatable(topic.key() + ".text"), tx, y + 41, width, 0xFFDDDDDD);
		}
		super.extractRenderState(g, mouseX, mouseY, a);
	}

	/** Experience from the start of level {@code from} to level {@code to}. */
	private static int expBetween(final int from, final int to) {
		int total = 0;
		for (int level = from; level < to; level++) {
			total += JobProgression.expToNext(level);
		}
		return total;
	}

	/** "Name: x, z" for the plaza, the Training Grounds stairs, every service NPC and every trainer. */
	private static List<Component> places() {
		List<Component> out = new ArrayList<>();
		BlockPos spawn = CityZone.spawn(0);
		out.add(line(Component.translatable("screen.minecraft_mode.guide.plaza"), spawn));
		out.add(line(Component.translatable("screen.minecraft_mode.guide.training"), TrainingGrounds.entrance(0)));
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
