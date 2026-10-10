package com.minecraftmode.client.guide;

import com.minecraftmode.city.CityFixtures;
import com.minecraftmode.city.CityZone;
import com.minecraftmode.city.Homestead;
import com.minecraftmode.city.TrainingGrounds;
import com.minecraftmode.client.companion.CompanionClient;
import com.minecraftmode.client.endgame.EndgameClient;
import com.minecraftmode.client.job.JobKeys;
import com.minecraftmode.client.map.MapClient;
import com.minecraftmode.client.map.MapScreen;
import com.minecraftmode.client.raid.RaidClient;
import com.minecraftmode.entity.CityNpc;
import com.minecraftmode.entity.ClassTrainer;
import com.minecraftmode.job.JobClass;
import com.minecraftmode.job.JobData;
import com.minecraftmode.job.JobProgression;
import com.minecraftmode.network.GuideBookPayload;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import org.jspecify.annotations.Nullable;

/**
 * Guide Nella's screen: topics on the left (getting started, leveling to 10, classes, keys, directions, money, gear, named monsters,
 * raids and dungeons, bounties and the story, companions and crafts, events, town comforts), the answer on the right in a pane that
 * scrolls with the mouse wheel. Directions list every service NPC and trainer, the landmarks and the shops and work places with
 * their coordinates, read from {@link CityZone}; clicking a line shows it on the world map. Leveling shows the experience the curve
 * asks for and how much the player still needs. The handbook button asks Nella for a fresh copy of the adventurer's handbook.
 */
public class GuideScreen extends Screen {
	private static final int W = 380;
	private static final int H = 232;
	private static final int LIST_W = 116;
	private static final int ROW = 16;
	/** The level a class can be chosen at (the leveling topic counts up to it). */
	private static final int CLASS_LEVEL = 10;
	/** Experience of a zombie (= its max health), the leveling topic's yardstick. */
	private static final int ZOMBIE_EXP = 20;
	private static final int TEXT_X = LIST_W + 16;
	/** The answer pane: first line, line height and the space left for the buttons below it. */
	private static final int TEXT_TOP = 41;
	private static final int LINE = 10;
	private static final int TEXT_BOTTOM = 28;
	/** How close a marker is shown when a direction is clicked. */
	private static final float MAP_ZOOM = 2.0F;

	public enum Topic {
		START, LEVELING, CLASSES, KEYS, PLACES, MONEY, GEAR, NAMED, DUNGEONS, BOUNTIES, COMPANIONS, EVENTS, TOWN;

		public String key() {
			return "guide.minecraft_mode." + this.name().toLowerCase(Locale.ROOT);
		}
	}

	/** One line of the answer: text, color and (for directions) the map marker it opens. */
	private record Line(FormattedCharSequence text, int color, @Nullable String marker) {
	}

	private static Topic topic = Topic.START;
	private final int guideId;
	private int left;
	private int top;
	private int scroll;
	private List<Line> lines = List.of();

	public GuideScreen() {
		this(-1);
	}

	/** {@code guideId}: Nella's entity, so the handbook button can ask her (-1 hides it). */
	public GuideScreen(final int guideId) {
		super(Component.translatable("screen.minecraft_mode.guide.title"));
		this.guideId = guideId;
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
				this.scroll = 0;
				this.rebuildWidgets();
			}).bounds(this.left + 8, this.top + 24 + i * ROW, LIST_W, 15).build();
			b.active = t != topic;
			this.addRenderableWidget(b);
		}
		this.addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> this.onClose()).bounds(this.left + W - 68, this.top + H - 22, 60, 18).build());
		if (this.guideId >= 0) {
			this.addRenderableWidget(Button.builder(Component.translatable("screen.minecraft_mode.guide.handbook"), b -> {
				ClientPlayNetworking.send(new GuideBookPayload(this.guideId));
				b.active = false;
			}).bounds(this.left + TEXT_X - 4, this.top + H - 22, 110, 18).build());
		}
		this.lines = this.layout(W - TEXT_X - 16);
		this.scroll = Mth.clamp(this.scroll, 0, this.maxScroll());
	}

	/** The answer to the current topic, wrapped to {@code width}. */
	private List<Line> layout(final int width) {
		List<Line> out = new ArrayList<>();
		if (topic == Topic.PLACES) {
			out.add(new Line(Component.translatable("screen.minecraft_mode.guide.places_hint").withStyle(ChatFormatting.ITALIC).getVisualOrderText(), 0xFF9AA8B8, null));
			for (Place place : places()) {
				// one line each: a name too long for the pane is cut
				Component text = Component.empty().append(place.name).append(": " + place.pos.getX() + ", " + place.pos.getZ());
				out.add(new Line(this.font.split(text, width).getFirst(), 0xFFDDDDDD, place.marker));
			}
			return out;
		}
		Component text;
		if (topic == Topic.LEVELING) {
			int total = expBetween(1, CLASS_LEVEL);
			int trainingExp = ZOMBIE_EXP * TrainingGrounds.EXP_MULTIPLIER;
			text = Component.translatable(topic.key() + ".text", total, (total + ZOMBIE_EXP - 1) / ZOMBIE_EXP, (total + trainingExp - 1) / trainingExp);
		} else if (topic == Topic.KEYS) {
			// the keys as bound now, so rebinding shows here too
			KeyMapping[] keys = {JobKeys.OPEN_SCREEN, EndgameClient.TALENTS, EndgameClient.CODEX, JobKeys.SKILLS[0], JobKeys.SKILLS[1], JobKeys.SKILLS[2],
				JobKeys.SKILLS[3], JobKeys.INNATE, CompanionClient.COLLECTION, CompanionClient.MOUNT, RaidClient.LOOT, MapClient.OPEN_MAP, MapClient.TOGGLE_MINIMAP};
			Object[] args = new Object[keys.length];
			for (int i = 0; i < keys.length; i++) {
				args[i] = keys[i].getTranslatedKeyMessage().copy().withStyle(ChatFormatting.YELLOW);
			}
			text = Component.translatable(topic.key() + ".text", args);
		} else {
			text = Component.translatable(topic.key() + ".text");
		}
		for (FormattedCharSequence line : this.font.split(text, width)) {
			out.add(new Line(line, 0xFFDDDDDD, null));
		}
		LocalPlayer player = this.minecraft.player;
		if (topic == Topic.LEVELING && player != null) {
			JobData data = JobProgression.get(player);
			Component progress = data.level() >= CLASS_LEVEL
				? Component.translatable("screen.minecraft_mode.guide.leveling_done")
				: Component.translatable("screen.minecraft_mode.guide.leveling_left", data.level(), expBetween(data.level(), CLASS_LEVEL) - data.exp(), CLASS_LEVEL);
			out.add(new Line(FormattedCharSequence.EMPTY, 0, null));
			for (FormattedCharSequence line : this.font.split(progress, width)) {
				out.add(new Line(line, 0xFF7CFC7C, null));
			}
		}
		return out;
	}

	private int visibleLines() {
		return (H - TEXT_TOP - TEXT_BOTTOM) / LINE;
	}

	private int maxScroll() {
		return Math.max(0, this.lines.size() - this.visibleLines());
	}

	@Override
	public void extractRenderState(final GuiGraphicsExtractor g, final int mouseX, final int mouseY, final float a) {
		int x = this.left;
		int y = this.top;
		g.fill(x, y, x + W, y + H, 0xE8101018);
		g.outline(x, y, W, H, 0xFF4AA8E8);
		g.text(this.font, this.title.copy().withStyle(ChatFormatting.AQUA), x + 10, y + 9, 0xFFFFFFFF, true);
		int tx = x + TEXT_X;
		g.fill(tx - 4, y + 24, x + W - 6, y + H - 26, 0x40000000);
		g.text(this.font, Component.translatable(topic.key()).withStyle(ChatFormatting.GOLD), tx, y + 28, 0xFFFFFFFF, false);
		int hovered = this.lineAt(mouseX, mouseY);
		int shown = Math.min(this.visibleLines(), this.lines.size() - this.scroll);
		for (int i = 0; i < shown; i++) {
			Line line = this.lines.get(this.scroll + i);
			int ly = y + TEXT_TOP + i * LINE;
			if (line.marker() != null && this.scroll + i == hovered) {
				g.fill(tx - 2, ly - 1, x + W - 14, ly + LINE - 1, 0x304AA8E8);
			}
			g.text(this.font, line.text(), tx, ly, line.color(), false);
		}
		if (this.maxScroll() > 0) {
			// scroll bar on the pane's right edge
			int trackTop = y + TEXT_TOP;
			int track = this.visibleLines() * LINE;
			int thumb = Math.max(8, track * this.visibleLines() / this.lines.size());
			int thumbTop = trackTop + (track - thumb) * this.scroll / this.maxScroll();
			g.fill(x + W - 11, trackTop, x + W - 8, trackTop + track, 0x40FFFFFF);
			g.fill(x + W - 11, thumbTop, x + W - 8, thumbTop + thumb, 0xC0FFFFFF);
		}
		super.extractRenderState(g, mouseX, mouseY, a);
	}

	/** Index of the answer line under the mouse, or -1. */
	private int lineAt(final double mouseX, final double mouseY) {
		int tx = this.left + TEXT_X;
		if (mouseX < tx - 2 || mouseX > this.left + W - 14 || mouseY < this.top + TEXT_TOP) {
			return -1;
		}
		int row = (int)((mouseY - this.top - TEXT_TOP) / LINE);
		int index = this.scroll + row;
		return row < this.visibleLines() && index < this.lines.size() ? index : -1;
	}

	@Override
	public boolean mouseScrolled(final double mouseX, final double mouseY, final double scrollX, final double scrollY) {
		if (this.maxScroll() > 0 && scrollY != 0) {
			this.scroll = Mth.clamp(this.scroll - (int)Math.signum(scrollY) * 3, 0, this.maxScroll());
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}

	@Override
	public boolean mouseClicked(final MouseButtonEvent event, final boolean doubleClick) {
		int index = this.lineAt(event.x(), event.y());
		if (index >= 0 && this.lines.get(index).marker() != null && MapScreen.focus(this.lines.get(index).marker(), MAP_ZOOM)) {
			this.minecraft.gui.setScreen(new MapScreen());
			return true;
		}
		return super.mouseClicked(event, doubleClick);
	}

	/** Experience from the start of level {@code from} to level {@code to}. */
	private static int expBetween(final int from, final int to) {
		int total = 0;
		for (int level = from; level < to; level++) {
			total += JobProgression.expToNext(level);
		}
		return total;
	}

	private record Place(Component name, BlockPos pos, String marker) {
	}

	/** The plaza, the landmarks, every service NPC, every trainer and the shops and work places, with the map marker of each. */
	private static List<Place> places() {
		List<Place> out = new ArrayList<>();
		out.add(new Place(Component.translatable("screen.minecraft_mode.guide.plaza"), CityZone.spawn(0), "screen.minecraft_mode.guide.waystone"));
		out.add(place("screen.minecraft_mode.guide.training", TrainingGrounds.entrance(0)));
		out.add(new Place(Component.translatable("screen.minecraft_mode.guide.portals"), CityFixtures.netherPortal(0), "screen.minecraft_mode.guide.nether_portal"));
		out.add(place("screen.minecraft_mode.guide.homestead", Homestead.waystone(0)));
		for (CityNpc.Role role : CityNpc.Role.values()) {
			out.add(place(role.nameKey(), CityZone.npcHome(role, 0)));
		}
		for (JobClass job : JobClass.PLAYABLE) {
			out.add(place(ClassTrainer.nameKey(job), CityZone.trainerHome(job, 0)));
		}
		for (CityZone.Spot spot : CityZone.Spot.values()) {
			out.add(place(spot.nameKey, spot.pos(0)));
		}
		return out;
	}

	private static Place place(final String nameKey, final BlockPos pos) {
		return new Place(Component.translatable(nameKey), pos, nameKey);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
