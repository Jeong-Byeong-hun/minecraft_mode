package com.minecraftmode.story;

import com.minecraftmode.companion.Companions;
import com.minecraftmode.craft.Profession;
import com.minecraftmode.dungeon.Dungeons;
import com.minecraftmode.entity.CityNpc;
import com.minecraftmode.job.JobClass;
import com.minecraftmode.job.JobProgression;
import com.minecraftmode.loot.Coins;
import com.minecraftmode.loot.EvolutionEtherItem;
import com.minecraftmode.loot.GearShop;
import com.minecraftmode.progress.Progress;
import com.minecraftmode.registry.ModAttachments;
import com.minecraftmode.registry.ModItems;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.ToIntFunction;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;

/**
 * The main story: chapters handed out by the royal herald on the plaza. Each chapter has a briefing, one goal read from what the
 * player has already done (class, levels, named kills, lairs, dungeons, professions, raids, keystones, world bosses) and a reward
 * collected from the herald. When a goal is reached the player is told to come back. Chapter ids are saved by position: append
 * new chapters at the end.
 */
public final class Story {
	/**
	 * @param counter  progress toward {@code goal}
	 * @param rewards  items for the player (coins are paid on top, growing with the chapter)
	 */
	public record Chapter(String id, String titleEn, String titleKo, String textEn, String textKo, String goalEn, String goalKo, int goal,
		ToIntFunction<Player> counter, Function<ServerPlayer, List<ItemStack>> rewards, String rewardEn, String rewardKo) {
		public String key() {
			return "story.minecraft_mode." + this.id;
		}

		public int progress(final Player player) {
			return Math.min(this.goal, this.counter.applyAsInt(player));
		}

		public boolean done(final Player player) {
			return this.counter.applyAsInt(player) >= this.goal;
		}
	}

	private static final List<Chapter> CHAPTERS = new ArrayList<>();

	static {
		chapter("call_to_arms", "A Call to Arms", "소집령",
			"Darkness stirs beyond the walls, and Stormhold needs fighters. Find a class trainer in the city and take up a path.",
			"성벽 너머에서 어둠이 꿈틀거리고 있소. 스톰홀드에는 싸울 이가 필요하오. 도시의 직업 교관을 찾아가 길을 정하시오.",
			"Choose a class", "직업 선택", 1, p -> JobProgression.get(p).job() != JobClass.NONE ? 1 : 0,
			p -> List.of(new ItemStack(ModItems.CONDENSED_ESSENCE, 2)), "2 condensed essence", "응축된 정수 2개");
		chapter("first_steps", "First Steps", "첫걸음",
			"Every hero starts small. Hunt beyond the walls and grow stronger; come back when you have some experience behind you.",
			"모든 영웅은 작게 시작하오. 성벽 밖에서 사냥하며 강해지시오. 경험을 쌓으면 다시 오시오.",
			"Reach level 10", "레벨 10 달성", 10, p -> JobProgression.get(p).level(),
			p -> List.of(new ItemStack(ModItems.ENHANCEMENT_STONE, 2)), "2 enhancement stones", "강화석 2개");
		chapter("named_threats", "Named Threats", "이름 있는 위협",
			"Some monsters have grown cunning and earned names. Scouts report them across the wilds. Prove you can bring them down.",
			"어떤 마물은 교활해져 이름까지 얻었소. 정찰병들이 황야 곳곳에서 그들을 보았다 하오. 그들을 쓰러뜨릴 수 있음을 증명하시오.",
			"Defeat 3 named monsters", "네임드 몬스터 3마리 처치", 3, p -> Progress.get(p).totalNamedKills(),
			p -> companion(p, false, "swift_stallion"), "Swift Stallion whistle (coins if owned)", "질풍마 호루라기(이미 있으면 동전)");
		chapter("lair_secrets", "Secrets of the Lairs", "소굴의 비밀",
			"The named ones hoard treasure in their lairs. Find one, defeat its guardians and open the chest at its heart.",
			"이름 있는 마물들은 소굴에 보물을 쌓아 두오. 소굴을 찾아 수호자를 물리치고 그 중심의 상자를 여시오.",
			"Clear a named lair", "네임드 소굴 정복", 1, p -> Progress.get(p).lairClears().values().stream().mapToInt(Integer::intValue).sum(),
			p -> List.of(new ItemStack(ModItems.LAIR_MAP, 2)), "2 lair maps", "소굴 지도 2장");
		chapter("second_path", "The Second Path", "두 번째 길",
			"Your trainer says you are ready for more. Complete their trial and advance your class.",
			"교관이 그대가 더 큰 힘을 다룰 준비가 되었다 하오. 시련을 마치고 직업을 전직하시오.",
			"Reach the second class tier", "2차 전직", 2, p -> JobProgression.get(p).tier(),
			p -> List.of(EvolutionEtherItem.of(Math.max(10, JobProgression.get(p).level() / 10 * 10), 5)), "5 Evolution Ether", "진화의 에테르 5개");
		chapter("below_the_keep", "Below the Keep", "성 아래로",
			"Beneath the keep the dungeon warden keeps the gate to dark places. Gather companions and clear a dungeon.",
			"성 아래에는 던전 관리인이 어둠으로 향하는 문을 지키고 있소. 동료를 모아 던전을 정복하시오.",
			"Clear a dungeon", "던전 클리어", 1, p -> Dungeons.data(p).totalClears(),
			p -> petCharm(p), "a pet charm (coins if you own them all)", "펫 부적 1개(모두 있으면 동전)");
		chapter("craftsmanship", "Craftsmanship", "장인의 길",
			"An army marches on food, potions and good steel. Learn a profession at the market or the forge and master its basics.",
			"군대는 음식과 물약, 좋은 강철로 움직이오. 시장이나 대장간에서 생활 기술을 익혀 기본을 다지시오.",
			"Reach level 10 in a profession", "생활 기술 레벨 10 달성", 10, Story::bestProfession,
			p -> List.of(new ItemStack(ModItems.PROTECTION_SCROLL)), "a protection scroll", "보호 주문서 1장");
		chapter("marshals_call", "The Marshal's Call", "사령관의 부름",
			"Six great monsters threaten the realm. The raid marshal is gathering parties. Answer his call and fell one of them.",
			"여섯 거대 마물이 왕국을 위협하고 있소. 토벌 사령관이 파티를 모으고 있으니 부름에 응해 그중 하나를 쓰러뜨리시오.",
			"Clear a raid", "레이드 클리어", 1, p -> Progress.get(p).raidClears().values().stream().mapToInt(Integer::intValue).sum(),
			p -> List.of(new ItemStack(ModItems.ENHANCEMENT_STONE, 3), new ItemStack(ModItems.CONDENSED_ESSENCE, 5)),
			"3 enhancement stones, 5 condensed essence", "강화석 3개, 응축된 정수 5개");
		chapter("trial_of_keys", "Trial of Keys", "쐐기돌의 시험",
			"Keystones lead to harder dungeons. Beat a +5 keystone within its time to show the warden what you are made of.",
			"쐐기돌은 더 어려운 던전으로 이끄오. +5 쐐기돌을 제한 시간 안에 돌파해 관리인에게 실력을 보이시오.",
			"Time a +5 keystone", "+5 쐐기돌 시간 내 돌파", 5, p -> Dungeons.data(p).bestOverall(),
			p -> List.of(new ItemStack(ModItems.AWAKENING_CRYSTAL)), "an awakening crystal", "각성의 결정 1개");
		chapter("titan_wakes", "The Titan Wakes", "거신의 각성",
			"At dusk the land shakes: titans rise from the wilds. When one appears, join the others and bring it down.",
			"해 질 녘이면 땅이 흔들리고 거신이 황야에서 일어나오. 거신이 나타나면 다른 이들과 함께 쓰러뜨리시오.",
			"Defeat a world boss", "월드 보스 처치", 1, p -> data(p).worldBosses(),
			p -> List.of(new ItemStack(ModItems.TITAN_SHARD, 2)), "2 titan shards", "거신의 파편 2개");
		chapter("hero_of_stormhold", "Hero of Stormhold", "스톰홀드의 영웅",
			"Only one step remains: reach the pinnacle of your strength. The realm will remember your name.",
			"남은 것은 한 걸음뿐이오. 힘의 정점에 오르시오. 왕국은 그대의 이름을 기억할 것이오.",
			"Reach level 100", "레벨 100 달성", 100, p -> JobProgression.get(p).level(),
			p -> companion(p, false, "storm_griffin"), "Storm Griffin whistle (coins if owned)", "폭풍 그리핀 호루라기(이미 있으면 동전)");
	}

	private static void chapter(final String id, final String titleEn, final String titleKo, final String textEn, final String textKo, final String goalEn,
		final String goalKo, final int goal, final ToIntFunction<Player> counter, final Function<ServerPlayer, List<ItemStack>> rewards, final String rewardEn,
		final String rewardKo) {
		CHAPTERS.add(new Chapter(id, titleEn, titleKo, textEn, textKo, goalEn, goalKo, goal, counter, rewards, rewardEn, rewardKo));
	}

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTickCount() % 40 == 0) {
				for (ServerPlayer player : server.getPlayerList().getPlayers()) {
					check(player);
				}
			}
		});
	}

	public static List<Chapter> chapters() {
		return List.copyOf(CHAPTERS);
	}

	public static StoryData data(final Player player) {
		return player.getAttachedOrElse(ModAttachments.STORY, StoryData.DEFAULT);
	}

	/** True once every chapter is finished. */
	public static boolean finished(final Player player) {
		return data(player).chapter() >= CHAPTERS.size();
	}

	private static int bestProfession(final Player player) {
		int best = 0;
		for (Profession profession : Profession.values()) {
			best = Math.max(best, profession.level(player));
		}
		return best;
	}

	/** A pet charm the player does not own yet, or (all owned) coins instead. */
	private static List<ItemStack> petCharm(final ServerPlayer player) {
		ItemStack charm = Companions.randomPet(Rarity.UNCOMMON, player.getRandom(), Companions.data(player));
		return charm.isEmpty() ? Coins.asItems(GearShop.bracketPrice(Math.max(10, JobProgression.get(player).level())) * 4) : List.of(charm);
	}

	/** The whistle or charm, or (already owned) its merit-shop worth in coins instead. */
	private static List<ItemStack> companion(final ServerPlayer player, final boolean pet, final String id) {
		Companions.Data data = Companions.data(player);
		if (pet ? data.hasPet(id) : data.hasMount(id)) {
			return Coins.asItems(GearShop.bracketPrice(Math.max(10, JobProgression.get(player).level())) * 4);
		}
		return List.of(new ItemStack(pet ? Companions.petItem(id) : Companions.mountItem(id)));
	}

	/** Coins for finishing chapter {@code index}. */
	public static int coins(final int index) {
		return GearShop.bracketPrice(10 + index * 9) * 2;
	}

	/** Tells the player once when the current chapter's goal is reached. */
	public static void check(final ServerPlayer player) {
		StoryData data = data(player);
		if (data.chapter() >= CHAPTERS.size() || data.notified()) {
			return;
		}
		Chapter chapter = CHAPTERS.get(data.chapter());
		if (chapter.done(player)) {
			player.setAttached(ModAttachments.STORY, data.announced());
			player.sendSystemMessage(Component.translatable("message.minecraft_mode.story.goal_done", Component.translatable(chapter.key()))
				.withStyle(ChatFormatting.GOLD));
		}
	}

	/** Talking to the herald: collect a finished chapter (and hear the next), or hear the current one again. */
	public static void talk(final ServerPlayer player, final CityNpc herald) {
		StoryData data = data(player);
		if (data.chapter() < CHAPTERS.size() && CHAPTERS.get(data.chapter()).done(player)) {
			Chapter done = CHAPTERS.get(data.chapter());
			player.setAttached(ModAttachments.STORY, data.next());
			int coins = coins(data.chapter());
			Coins.give(player, coins);
			for (ItemStack stack : done.rewards().apply(player)) {
				if (!stack.isEmpty()) {
					player.getInventory().placeItemBackInInventory(stack, net.minecraft.util.Prediction.SERVER_ONLY);
				}
			}
			player.sendSystemMessage(Component.translatable("message.minecraft_mode.story.chapter_done", Component.translatable(done.key())).withStyle(ChatFormatting.GOLD,
				ChatFormatting.BOLD));
			player.sendSystemMessage(Component.translatable("message.minecraft_mode.story.reward", Coins.component(coins), Component.translatable(done.key() + ".reward"))
				.withStyle(ChatFormatting.YELLOW));
			player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 0.8F, 1.0F);
			Progress.check(player);
		}
		brief(player);
	}

	/** The current chapter's briefing and goal (or the epilogue). */
	public static void brief(final ServerPlayer player) {
		StoryData data = data(player);
		if (data.chapter() >= CHAPTERS.size()) {
			player.sendSystemMessage(Component.translatable("message.minecraft_mode.story.finished").withStyle(ChatFormatting.LIGHT_PURPLE));
			return;
		}
		Chapter chapter = CHAPTERS.get(data.chapter());
		player.sendSystemMessage(Component.translatable("message.minecraft_mode.story.chapter", data.chapter() + 1, Component.translatable(chapter.key()))
			.withStyle(ChatFormatting.GOLD));
		player.sendSystemMessage(Component.translatable(chapter.key() + ".text").withStyle(ChatFormatting.GRAY));
		player.sendSystemMessage(Component.translatable("message.minecraft_mode.story.goal", Component.translatable(chapter.key() + ".goal"), chapter.progress(player),
			chapter.goal()).withStyle(chapter.done(player) ? ChatFormatting.GREEN : ChatFormatting.AQUA));
	}

	private Story() {
	}
}
