package com.minecraftmode.townquest;

import com.minecraftmode.entity.CityNpc.Role;
import com.minecraftmode.jump.JumpForest;
import com.minecraftmode.registry.ModItems;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;

/**
 * The town quests Stormhold's residents hand out (0.5.36): the farmer's wife, the old miner, the fisherman, the retired adventurer,
 * the child and the lumberjack ({@link Role#isResident}). Each quest has one goal; most repeat every real day (the guild bounties'
 * {@code RealReset} day), a few are one-time. Rewards are class EXP as a share of a level at the player's own level, coins on the bracket
 * curve and enhancement materials ({@link Tier}). Quest ids are saved keys ({@link TownQuestData}): never rename them; append new
 * quests anywhere (the list order is only the order on the giver's screen).
 */
public final class TownQuests {
	/** What a quest asks for. Only the quest id is saved, so these may change freely. */
	public enum Goal {
		/** Defeat monsters of one type (target = entity id), shared with party members nearby like bounties. */
		KILL("kill"),
		/** Defeat any named monsters. */
		KILL_NAMED("kill_named"),
		/** Defeat lair lords. */
		KILL_LORD("kill_lord"),
		/** Hand in items when turning the quest in (target = item id; inventory first, then the bags). */
		DELIVER("deliver"),
		/** Mine ores (as the guild bounty counts them). */
		MINE_ORE("mine_ore"),
		/** Land fish with a rod. */
		FISH("fish"),
		/** Harvest ripe crops of the mod. */
		HARVEST("harvest"),
		/** Craft at a profession station (target = profession id, "" = any). */
		CRAFT("craft"),
		/** Clear a named lair (open its treasure after the lord fell). */
		CLEAR_LAIR("clear_lair"),
		/** Clear a dungeon run. */
		DUNGEON("dungeon"),
		/** Reach the end of a jump course (target = {@link JumpForest} course id). */
		JUMP("jump");

		public final String id;

		Goal(final String id) {
			this.id = id;
		}

		public String key() {
			return "townquest.minecraft_mode.goal." + this.id;
		}
	}

	/**
	 * How much a quest pays: EXP as a share of a level at the player's level, coins as a share of {@code GearShop.bracketPrice} of the
	 * player's level and enhancement stones. A guild daily bounty pays 1.2x the bracket price and 3 stones; a town quest pays less each,
	 * but there are about twenty of them a day.
	 */
	public enum Tier {
		EASY(0.03F, 0.2F, 1),
		NORMAL(0.05F, 0.3F, 1),
		HARD(0.10F, 0.6F, 2);

		public final float exp;
		public final float coins;
		public final int stones;

		Tier(final float exp, final float coins, final int stones) {
			this.exp = exp;
			this.coins = coins;
			this.stones = stones;
		}
	}

	/**
	 * One quest.
	 *
	 * @param id       saved key, also the lang key ({@link #titleKey}, {@link #textKey})
	 * @param target   entity, item, profession or course id for goals that have one, else ""
	 * @param minLevel class level needed to take it
	 * @param daily    true: again every real day; false: once per character
	 * @param bonus    an extra reward item (one-time quests), or null
	 */
	public record Quest(String id, Role giver, Goal goal, String target, int need, int minLevel, boolean daily, Tier tier, @Nullable Item bonus,
		int bonusCount, String titleEn, String titleKo, String textEn, String textKo) {
		public String titleKey() {
			return "townquest.minecraft_mode." + this.id;
		}

		public String textKey() {
			return this.titleKey() + ".text";
		}
	}

	private static final Map<String, Quest> BY_ID = new LinkedHashMap<>();

	static {
		// ---- Farmer's Wife Martha
		add(daily("martha_wheat", Role.FARMER_WIFE, Goal.DELIVER, "minecraft:wheat", 32, 1, Tier.EASY,
			"Flour for Hanna", "한나네 밀가루",
			"Hanna bakes bread for every adventurer in town, and her flour bin is empty again. Bring me wheat and I'll have it milled by morning.",
			"한나가 모험가마다 빵을 구워 주느라 밀가루 통이 또 비었지 뭐예요. 밀을 가져다주면 아침까지 빻아 놓을게요."));
		add(daily("martha_harvest", Role.FARMER_WIFE, Goal.HARVEST, "", 16, 1, Tier.NORMAL,
			"A Hand at Harvest", "수확 일손",
			"My husband's back gave out again. Harvest ripe crops for me: rice, cabbage, anything grown from the grocer's seeds. You'll learn a thing or two about farming.",
			"남편 허리가 또 말썽이에요. 잘 익은 작물을 대신 거둬 줄래요? 쌀이든 배추든 식료품점 씨앗으로 키운 거면 다 돼요. 농사 공부도 되고요."));
		add(daily("martha_cabbage", Role.FARMER_WIFE, Goal.DELIVER, "minecraft_mode:napa_cabbage", 12, 1, Tier.NORMAL,
			"Kimchi Season", "김장철",
			"It's kimchi season and half the street is coming over to help. All I'm missing is napa cabbage. The grocer sells the seeds.",
			"김장철이라 동네 사람 절반이 도우러 온대요. 배추만 있으면 되는데 말이죠. 씨앗은 식료품점에서 팔아요."));
		add(daily("martha_supper", Role.FARMER_WIFE, Goal.CRAFT, "cooking", 5, 1, Tier.NORMAL,
			"Supper for the Field Hands", "일꾼들 저녁상",
			"The field hands work till dark and come home starving. Cook a few dishes at the kitchen station in the market. Anything warm will do.",
			"일꾼들이 해 질 때까지 일하고 배를 곯고 와요. 시장의 조리대에서 요리 몇 접시만 해 줄래요? 따뜻한 거면 뭐든 좋아요."));

		// ---- Old Miner Dugan
		add(daily("dugan_seam", Role.OLD_MINER, Goal.MINE_ORE, "", 24, 1, Tier.NORMAL,
			"Back to the Seam", "다시 광맥으로",
			"Forty years I swung a pick down there. My knees won't take the ladders any more, but yours will. Dig some ore and tell me how the seams look.",
			"사십 년을 저 아래서 곡괭이질을 했지. 이젠 무릎이 사다리를 못 버티지만 자네 무릎은 멀쩡하잖나. 광석 좀 캐 보고 광맥이 어떤지 들려주게."));
		add(daily("dugan_iron", Role.OLD_MINER, Goal.DELIVER, "minecraft:raw_iron", 24, 1, Tier.EASY,
			"Iron for the Forge", "대장간에 보낼 철",
			"Volund at the forge says shop iron is all slag. He wants it straight from the rock. Dig me some raw iron and I'll carry it over. He still owes me a favour.",
			"대장간의 볼룬드 말로는 가게 철은 죄다 찌꺼기래. 바위에서 막 캔 걸 원하지. 철 원석을 좀 캐 오게. 내가 갖다주겠네. 그 친구 나한테 빚진 게 있거든."));
		add(daily("dugan_bones", Role.OLD_MINER, Goal.KILL, "minecraft:skeleton", 15, 1, Tier.NORMAL,
			"Rattling in the Tunnels", "갱도의 달그락 소리",
			"Skeletons have taken over the old tunnels again. Clear them out so the young miners can work in peace.",
			"옛 갱도에 또 스켈레톤이 들끓는다네. 젊은 광부들이 마음 놓고 일하게 좀 쓸어 주게."));
		add(daily("dugan_mythril", Role.OLD_MINER, Goal.DELIVER, "minecraft_mode:raw_mythril", 4, 25, Tier.HARD,
			"Mythril Fever", "미스릴 열병",
			"Blue ore, deep down, harder to find than diamond. I found a vein once, long ago. Bring me raw mythril, fresh from the vein, and let an old man hold it again.",
			"깊은 곳의 푸른 광석, 다이아몬드보다 찾기 어렵지. 오래전에 딱 한 번 광맥을 찾았었다네. 광맥에서 막 캔 미스릴 원석을 가져와서 이 늙은이 손에 다시 쥐여 주게."));
		add(once("dugan_diamonds", Role.OLD_MINER, Goal.DELIVER, "minecraft:diamond", 3, 30, Tier.HARD, ModItems.PROTECTION_SCROLL, 1,
			"The Old Man's Last Pick", "노광부의 마지막 곡괭이",
			"Before I hang up my helmet I want to forge one last pick, a diamond one. Bring me the stones and I'll give you something I've kept for years.",
			"투구를 벗기 전에 마지막 곡괭이를 하나 벼리고 싶네. 다이아몬드로 말이야. 보석을 가져오면 오래 아껴 둔 물건을 주지."));

		// ---- Fisherman Pell
		add(daily("pell_morning", Role.FISHERMAN, Goal.FISH, "", 10, 1, Tier.NORMAL,
			"A Morning on the Pier", "부두의 아침",
			"Grab a rod and fish with me a while. Any water will do: the river, the sea, even a pond.",
			"낚싯대 하나 들고 나랑 좀 앉아 있자고. 강이든 바다든 연못이든 물만 있으면 돼."));
		add(daily("pell_carp", Role.FISHERMAN, Goal.DELIVER, "minecraft_mode:crucian_carp", 5, 1, Tier.EASY,
			"Carp Stew", "붕어찜",
			"My wife makes the best crucian carp stew in Stormhold, but all I've caught today is boots. Crucian carp bite in any water.",
			"우리 집사람 붕어찜이 스톰홀드에서 제일이거든. 그런데 오늘은 장화만 낚였어. 붕어는 아무 물에서나 물어."));
		add(daily("pell_drowned", Role.FISHERMAN, Goal.KILL, "minecraft:drowned", 8, 10, Tier.NORMAL,
			"Trouble Below the Waves", "물밑의 골칫거리",
			"Drowned keep tangling my nets and pinching my tridents. Thin them out in the rivers and the sea.",
			"드라운드 놈들이 자꾸 그물을 엉키게 하고 삼지창을 훔쳐 가. 강이랑 바다에서 좀 줄여 줘."));

		// ---- Retired Adventurer Rowan
		add(daily("rowan_named", Role.VETERAN, Goal.KILL_NAMED, "", 1, 10, Tier.NORMAL,
			"Names Worth Knowing", "이름 있는 놈들",
			"In my day we kept a list of every monster with a name. Hunt one down, any of them, and tell me how it went.",
			"내 시절엔 이름 붙은 괴물을 하나하나 적어 두고 다녔지. 아무 놈이나 하나 잡고 와서 어땠는지 들려주게."));
		add(daily("rowan_lair", Role.VETERAN, Goal.CLEAR_LAIR, "", 1, 10, Tier.HARD,
			"Into the Lair", "소굴 속으로",
			"Every named beast keeps a lair with a treasure, guarded by its lord. Beat the lord and open the treasure. A lair map from the guild shows you the way.",
			"네임드마다 소굴이 있고, 소굴의 군주가 보물을 지키고 있지. 군주를 쓰러뜨리고 보물을 열어 보게. 길드의 소굴 지도가 길을 알려 줄 걸세."));
		add(once("rowan_lord", Role.VETERAN, Goal.KILL_LORD, "", 1, 10, Tier.HARD, ModItems.AWAKENING_CRYSTAL, 1,
			"My First Lord", "나의 첫 군주",
			"I'll never forget my first lair lord. Shaking hands, a broken shield, and the best night of my life. Bring one down and I'll give you the crystal I never got to use.",
			"첫 소굴의 군주는 평생 못 잊지. 떨리는 손, 깨진 방패, 그리고 인생 최고의 밤. 군주 하나를 쓰러뜨리면 내가 끝내 못 쓴 각성의 결정을 주겠네."));
		add(daily("rowan_dungeon", Role.VETERAN, Goal.DUNGEON, "", 1, 10, Tier.HARD,
			"Old Halls, New Blood", "옛 회랑, 새 피",
			"Kael at the keep sends parties down into the dungeons. Clear a run, any dungeon, with or without a keystone, and come back with a story.",
			"성 앞의 카엘이 파티를 던전으로 보내 주지. 어느 던전이든, 쐐기돌이 있든 없든 한 번 돌파하고 무용담을 들려주게."));
		add(daily("rowan_endermen", Role.VETERAN, Goal.KILL, "minecraft:enderman", 5, 15, Tier.NORMAL,
			"Eyes in the Dark", "어둠 속의 눈",
			"Endermen. Never look them in the eye, unless you mean to fight. Bring a few down and you'll have earned a drink.",
			"엔더맨 말이야. 싸울 생각이 아니면 절대 눈을 마주치지 말게. 몇 마리 쓰러뜨리면 한잔 살 만하지."));

		// ---- Little Pip
		add(daily("pip_tenacity", Role.CHILD, Goal.JUMP, JumpForest.TENACITY, 1, 1, Tier.NORMAL,
			"The Forest of Tenacity", "끈기의 숲",
			"Bet you can't reach the end of the Forest of Tenacity! It's all jumping, branch to branch, and if you fall you start again. No mounts or flying either, they don't work in there. No cheating!",
			"끈기의 숲 끝까지 못 갈걸요? 나뭇가지에서 나뭇가지로 계속 점프해야 하고, 떨어지면 처음부터예요. 탈것도 비행도 거기선 안 돼요. 반칙 금지!"));
		add(daily("pip_spiders", Role.CHILD, Goal.KILL, "minecraft:spider", 10, 1, Tier.EASY,
			"Scary Spiders", "무서운 거미",
			"There are spiders outside the walls and they have EIGHT eyes. Can you make them go away? Please?",
			"성벽 밖에 거미가 있는데 눈이 여덟 개래요! 다 쫓아내 줄 수 있어요? 네?"));
		add(daily("pip_berries", Role.CHILD, Goal.DELIVER, "minecraft:sweet_berries", 16, 1, Tier.EASY,
			"Something Sweet", "달콤한 거",
			"Mum says no sweets before dinner, but berries aren't sweets, right? Sweet berries grow in the spruce forests. Get me some!",
			"엄마가 저녁 전엔 단 거 안 된대요. 근데 열매는 단 거 아니죠? 달콤한 열매는 가문비나무 숲에 있어요. 좀 따다 줘요!"));

		// ---- Lumberjack Bjorn
		add(daily("bjorn_oak", Role.LUMBERJACK, Goal.DELIVER, "minecraft:oak_sapling", 16, 1, Tier.EASY,
			"Replant the Grove", "숲 다시 심기",
			"Every tree I fell for the harbor piers leaves a hole in the grove. Bring me oak saplings from the leaves you clear and I'll plant them back.",
			"부두 놓느라 나무를 벨 때마다 숲에 구멍이 나. 나뭇잎을 치우다 나오는 참나무 묘목을 가져다줘. 내가 다시 심을게."));
		add(daily("bjorn_spruce", Role.LUMBERJACK, Goal.DELIVER, "minecraft:spruce_sapling", 12, 1, Tier.EASY,
			"Masts for Tomorrow", "내일의 돛대",
			"The shipwrights will want tall spruce for masts long after I'm gone. Bring me spruce saplings from the cold forests and we'll plant the masts of tomorrow.",
			"내가 떠난 뒤에도 배 목수들은 돛대로 쓸 높은 가문비나무를 찾을 거야. 추운 숲에서 가문비나무 묘목을 가져다줘. 내일의 돛대를 심자고."));
		add(daily("bjorn_night", Role.LUMBERJACK, Goal.KILL, "minecraft:zombie", 15, 1, Tier.NORMAL,
			"The Night Shift", "밤 근무",
			"Zombies come out of the woods every night and chase my crew off. Teach them some manners.",
			"밤마다 숲에서 좀비가 나와서 우리 일꾼들을 쫓아내. 따끔하게 혼 좀 내 줘."));
	}

	private static Quest daily(final String id, final Role giver, final Goal goal, final String target, final int need, final int minLevel, final Tier tier,
		final String titleEn, final String titleKo, final String textEn, final String textKo) {
		return new Quest(id, giver, goal, target, need, minLevel, true, tier, null, 0, titleEn, titleKo, textEn, textKo);
	}

	private static Quest once(final String id, final Role giver, final Goal goal, final String target, final int need, final int minLevel, final Tier tier,
		final Item bonus, final int bonusCount, final String titleEn, final String titleKo, final String textEn, final String textKo) {
		return new Quest(id, giver, goal, target, need, minLevel, false, tier, bonus, bonusCount, titleEn, titleKo, textEn, textKo);
	}

	private static void add(final Quest quest) {
		if (BY_ID.put(quest.id(), quest) != null) {
			throw new IllegalStateException("duplicate town quest " + quest.id());
		}
		if (!quest.giver().isResident()) {
			throw new IllegalStateException("town quest " + quest.id() + " is given by " + quest.giver() + ", not a resident");
		}
		if (quest.need() <= 0 || quest.bonus() == Items.AIR) {
			throw new IllegalStateException("town quest " + quest.id() + " is malformed");
		}
	}

	public static List<Quest> all() {
		return List.copyOf(BY_ID.values());
	}

	public static @Nullable Quest get(final String id) {
		return BY_ID.get(id);
	}

	/** The quests {@code giver} hands out, in screen order. */
	public static List<Quest> of(final Role giver) {
		List<Quest> out = new ArrayList<>();
		for (Quest quest : BY_ID.values()) {
			if (quest.giver() == giver) {
				out.add(quest);
			}
		}
		return out;
	}

	private TownQuests() {
	}
}
