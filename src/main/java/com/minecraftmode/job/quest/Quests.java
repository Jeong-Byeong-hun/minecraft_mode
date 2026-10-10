package com.minecraftmode.job.quest;

import com.minecraftmode.MinecraftMode;
import com.minecraftmode.job.JobClass;
import com.minecraftmode.job.quest.QuestDef.KillGoal;
import com.minecraftmode.job.quest.QuestDef.Material;
import com.minecraftmode.job.quest.QuestDef.TokenSource;
import com.minecraftmode.registry.ModEntities;
import com.minecraftmode.registry.ModItems;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import org.jspecify.annotations.Nullable;

/**
 * The 28 advancement trials (7 classes x 4 tiers) and their trial tokens. Tier 1 is the free choice
 * of class at level 10 ({@link QuestDef#instant()}); tiers 2-4 are real trials. Tokens only drop for a
 * player whose trial needs them and go straight into that player's inventory, so other players
 * cannot pick them up. Bosses credit every player with the trial within 64 blocks.
 */
public final class Quests {
	public static final Set<EntityType<?>> BOSSES = Set.of(
		EntityTypes.WITHER, EntityTypes.ENDER_DRAGON, EntityTypes.WARDEN, EntityTypes.ELDER_GUARDIAN, ModEntities.MYTHRIL_GOLEM
	);

	private static final Map<String, QuestDef> BY_ID = new LinkedHashMap<>();
	private static final List<Item> TOKENS = new ArrayList<>();

	private static final Set<EntityType<?>> ZOMBIES = Set.of(EntityTypes.ZOMBIE, EntityTypes.HUSK, EntityTypes.DROWNED, EntityTypes.ZOMBIE_VILLAGER);
	private static final Set<EntityType<?>> SKELETONS = Set.of(EntityTypes.SKELETON, EntityTypes.STRAY, EntityTypes.BOGGED);
	private static final Set<EntityType<?>> SPIDERS = Set.of(EntityTypes.SPIDER, EntityTypes.CAVE_SPIDER);
	private static final Set<EntityType<?>> ILLAGERS = Set.of(EntityTypes.PILLAGER, EntityTypes.VINDICATOR, EntityTypes.EVOKER, EntityTypes.RAVAGER);
	private static final Set<EntityType<?>> HOSTILES = Set.of(
		EntityTypes.ZOMBIE, EntityTypes.HUSK, EntityTypes.DROWNED, EntityTypes.SKELETON, EntityTypes.STRAY, EntityTypes.BOGGED, EntityTypes.SPIDER,
		EntityTypes.CAVE_SPIDER, EntityTypes.WITCH, EntityTypes.ENDERMAN, EntityTypes.PHANTOM, EntityTypes.PILLAGER,
		EntityTypes.VINDICATOR, EntityTypes.BLAZE, EntityTypes.GHAST, EntityTypes.WITHER_SKELETON, EntityTypes.MAGMA_CUBE, EntityTypes.HOGLIN,
		EntityTypes.PIGLIN_BRUTE, EntityTypes.GUARDIAN, EntityTypes.SHULKER, ModEntities.MINE_RAIDER
	);

	// Trial tokens (one per trial)
	public static final Item RUSTED_MEDAL = token("rusted_medal", Rarity.UNCOMMON);
	public static final Item CHAMPIONS_LAUREL = token("champions_laurel", Rarity.UNCOMMON);
	public static final Item FRENZY_BLOOD = token("frenzy_blood", Rarity.RARE);
	public static final Item GRAIL_SHARD = token("grail_shard", Rarity.EPIC);
	public static final Item THIEVES_TOKEN = token("thieves_token", Rarity.UNCOMMON);
	public static final Item NINJA_SCROLL = token("ninja_scroll", Rarity.UNCOMMON);
	public static final Item ASSASSIN_SEAL = token("assassin_seal", Rarity.RARE);
	public static final Item MONARCHS_SHADOW = token("monarchs_shadow", Rarity.EPIC);
	public static final Item ARCANE_DUST = token("arcane_dust", Rarity.UNCOMMON);
	public static final Item GRIMOIRE_PAGE = token("grimoire_page", Rarity.UNCOMMON);
	public static final Item EMBER_CORE = token("ember_core", Rarity.RARE);
	public static final Item AKASHIC_FRAGMENT = token("akashic_fragment", Rarity.EPIC);
	public static final Item STEEL_ARROWHEAD = token("steel_arrowhead", Rarity.UNCOMMON);
	public static final Item PHANTOM_PLUME = token("phantom_plume", Rarity.UNCOMMON);
	public static final Item SPIRIT_ARROW = token("spirit_arrow", Rarity.RARE);
	public static final Item GOLDEN_KEY = token("golden_key", Rarity.EPIC);
	public static final Item MAP_SCRAP = token("map_scrap", Rarity.UNCOMMON);
	public static final Item BOUNTY_POSTER = token("bounty_poster", Rarity.UNCOMMON);
	public static final Item HAKI_CRYSTAL = token("haki_crystal", Rarity.RARE);
	public static final Item SEA_KINGS_TREASURE = token("sea_kings_treasure", Rarity.EPIC);
	public static final Item HOLLOW_MASK_SHARD = token("hollow_mask_shard", Rarity.UNCOMMON);
	public static final Item HELL_BUTTERFLY = token("hell_butterfly", Rarity.UNCOMMON);
	public static final Item TENSHINTAI = token("tenshintai", Rarity.RARE);
	public static final Item HOGYOKU_FRAGMENT = token("hogyoku_fragment", Rarity.EPIC);
	public static final Item EXAM_BADGE = token("exam_badge", Rarity.UNCOMMON);
	public static final Item DIVINATION_GLASS = token("divination_glass", Rarity.UNCOMMON);
	public static final Item CHIMERA_CARAPACE = token("chimera_carapace", Rarity.RARE);
	public static final Item DARK_CONTINENT_RELIC = token("dark_continent_relic", Rarity.EPIC);

	private static Item token(final String name, final Rarity rarity) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, MinecraftMode.id(name));
		Item item = Registry.register(BuiltInRegistries.ITEM, key, new QuestTokenItem(new Item.Properties().setId(key).rarity(rarity).stacksTo(16)));
		TOKENS.add(item);
		return item;
	}

	public static void init() {
		// ---------------------------------------------------------------- Warrior
		choice("warrior_1", JobClass.WARRIOR, "Taking Up the Sword", "검을 드는 자",
			"Take up the sword and stand in the front line. Every Warrior starts here.",
			"검을 들고 최전선에 서라. 모든 전사는 여기서 시작한다.",
			RUSTED_MEDAL);
		add("warrior_2", JobClass.WARRIOR, 2, "Trial of the Arena", "투기장의 시련",
			"The arena masters want illager blood. Win their laurels in real battle.",
			"투기장의 주인들은 일리저의 피를 원한다. 실전에서 월계관을 쟁취하라.",
			List.of(kill("Illagers", "일리저", ILLAGERS, 6)),
			CHAMPIONS_LAUREL, 4, List.of(source(ILLAGERS, 0.7F, 1), source(Set.of(EntityTypes.RAVAGER), 1.0F, 2)),
			List.of(mat(ModItems.ESSENCE, 8)));
		add("warrior_3", JobClass.WARRIOR, 3, "Berserker's Awakening", "광전사의 각성",
			"Shatter a Mythril Golem and drink the frenzy of the Nether's skeleton knights.",
			"미스릴 골렘을 부수고, 네더 해골 기사들의 광기를 마셔라.",
			List.of(kill("Mythril Golem", "미스릴 골렘", Set.of(ModEntities.MYTHRIL_GOLEM), 1),
				kill("Wither Skeletons", "위더 스켈레톤", Set.of(EntityTypes.WITHER_SKELETON), 8)),
			FRENZY_BLOOD, 3, List.of(source(Set.of(EntityTypes.WITHER_SKELETON), 0.5F, 1), source(Set.of(EntityTypes.PIGLIN_BRUTE), 1.0F, 1)),
			List.of(mat(ModItems.CONDENSED_ESSENCE, 2)));
		add("warrior_4", JobClass.WARRIOR, 4, "Sword of Selection", "선정의 검",
			"Only one who has felled the Wither may draw the sword of kings.",
			"위더를 쓰러뜨린 자만이 왕의 검을 뽑을 수 있다.",
			List.of(kill("The Wither", "위더", Set.of(EntityTypes.WITHER), 1), kill("Blazes", "블레이즈", Set.of(EntityTypes.BLAZE), 10)),
			GRAIL_SHARD, 3, List.of(source(Set.of(EntityTypes.WITHER), 1.0F, 3), source(Set.of(EntityTypes.BLAZE), 0.3F, 1)),
			List.of(mat(ModItems.CONDENSED_ESSENCE, 4)));

		// ---------------------------------------------------------------- Rogue
		choice("rogue_1", JobClass.ROGUE, "Into the Shadows", "그림자 속으로",
			"The Thieves' Guild opens its doors to you. Step into the shadows.",
			"도적 길드가 너에게 문을 연다. 그림자 속으로 들어와라.",
			THIEVES_TOKEN);
		add("rogue_2", JobClass.ROGUE, 2, "The Way of the Ninja", "닌자의 길",
			"A ninja strikes before the bowstring is drawn. Hunt skeletons and recover the stolen scrolls.",
			"닌자는 시위가 당겨지기 전에 벤다. 스켈레톤을 사냥하고 빼앗긴 두루마리를 되찾아라.",
			List.of(kill("Skeletons", "스켈레톤", SKELETONS, 8), kill("Witches", "마녀", Set.of(EntityTypes.WITCH), 2)),
			NINJA_SCROLL, 4, List.of(source(SKELETONS, 0.6F, 1), source(Set.of(EntityTypes.WITCH), 1.0F, 2)),
			List.of(mat(ModItems.ESSENCE, 8)));
		add("rogue_3", JobClass.ROGUE, 3, "Creed of Assassins", "어쌔신의 신조",
			"Nothing is true, everything is permitted. Bring down a golem and the endermen who watch from the dark.",
			"진실은 없고, 모든 것이 허용된다. 골렘을 쓰러뜨리고 어둠 속에서 지켜보는 엔더맨을 처단하라.",
			List.of(kill("Mythril Golem", "미스릴 골렘", Set.of(ModEntities.MYTHRIL_GOLEM), 1), kill("Endermen", "엔더맨", Set.of(EntityTypes.ENDERMAN), 8)),
			ASSASSIN_SEAL, 3, List.of(source(Set.of(EntityTypes.ENDERMAN), 0.5F, 1)),
			List.of(mat(ModItems.CONDENSED_ESSENCE, 2)));
		add("rogue_4", JobClass.ROGUE, 4, "Arise", "일어나라",
			"Defeat the Warden of the deep dark. Its shadow will kneel before its new monarch.",
			"깊은 어둠의 워든을 쓰러뜨려라. 그 그림자가 새 군주 앞에 무릎 꿇을 것이다.",
			List.of(kill("The Warden", "워든", Set.of(EntityTypes.WARDEN), 1),
				kill("Wither Skeletons", "위더 스켈레톤", Set.of(EntityTypes.WITHER_SKELETON), 10)),
			MONARCHS_SHADOW, 3, List.of(source(Set.of(EntityTypes.WARDEN), 1.0F, 3), source(Set.of(EntityTypes.WITHER_SKELETON), 0.25F, 1)),
			List.of(mat(ModItems.CONDENSED_ESSENCE, 4)));

		// ---------------------------------------------------------------- Mage
		choice("mage_1", JobClass.MAGE, "The First Spell", "첫 번째 주문",
			"You can already feel the faint mana every apprentice learns to sense. Speak your first spell.",
			"모든 견습생이 처음 느끼는 희미한 마력이 벌써 느껴질 것이다. 첫 주문을 외워라.",
			ARCANE_DUST);
		add("mage_2", JobClass.MAGE, 2, "Caster's Covenant", "캐스터의 서약",
			"Witches and illusioners stole pages of the great grimoire. Take them back.",
			"마녀와 환술사들이 위대한 마도서의 페이지를 훔쳤다. 되찾아 와라.",
			List.of(kill("Witches", "마녀", Set.of(EntityTypes.WITCH), 3), kill("Endermen", "엔더맨", Set.of(EntityTypes.ENDERMAN), 5)),
			GRIMOIRE_PAGE, 4, List.of(source(Set.of(EntityTypes.WITCH), 1.0F, 1), source(Set.of(EntityTypes.ENDERMAN), 0.5F, 1),
				source(Set.of(EntityTypes.EVOKER), 1.0F, 2)),
			List.of(mat(ModItems.ESSENCE, 8)));
		add("mage_3", JobClass.MAGE, 3, "Tower of the Archmage", "대마도사의 탑",
			"The tower's hearth needs blaze embers and the heart of a mythril golem.",
			"탑의 화로에는 블레이즈의 불씨와 미스릴 골렘의 심장이 필요하다.",
			List.of(kill("Mythril Golem", "미스릴 골렘", Set.of(ModEntities.MYTHRIL_GOLEM), 1), kill("Blazes", "블레이즈", Set.of(EntityTypes.BLAZE), 8)),
			EMBER_CORE, 3, List.of(source(Set.of(EntityTypes.BLAZE), 0.5F, 1)),
			List.of(mat(ModItems.CONDENSED_ESSENCE, 2)));
		add("mage_4", JobClass.MAGE, 4, "Root of the World", "근원의 문",
			"Beyond the End lies the Akashic Record. Slay the dragon that guards its door.",
			"엔드 너머에 아카식 레코드가 있다. 그 문을 지키는 용을 쓰러뜨려라.",
			List.of(kill("Ender Dragon", "엔더 드래곤", Set.of(EntityTypes.ENDER_DRAGON), 1), kill("Shulkers", "셜커", Set.of(EntityTypes.SHULKER), 5)),
			AKASHIC_FRAGMENT, 3, List.of(source(Set.of(EntityTypes.ENDER_DRAGON), 1.0F, 3), source(Set.of(EntityTypes.SHULKER), 0.4F, 1)),
			List.of(mat(ModItems.CONDENSED_ESSENCE, 4)));

		// ---------------------------------------------------------------- Archer
		choice("archer_1", JobClass.ARCHER, "The Hunter's Path", "사냥꾼의 길",
			"Pick up the bow and keep your eyes on the far horizon.",
			"활을 들고 먼 지평선에서 눈을 떼지 마라.",
			STEEL_ARROWHEAD);
		add("archer_2", JobClass.ARCHER, 2, "Ranger's Watch", "레인저의 감시",
			"Rangers guard the night sky. Down phantoms and keep your aim true from afar.",
			"레인저는 밤하늘을 지킨다. 팬텀을 떨어뜨리고, 멀리서도 빗나가지 마라.",
			List.of(kill("Phantoms", "팬텀", Set.of(EntityTypes.PHANTOM), 3), rangedKill("Monsters (ranged kills)", "몬스터 (원거리 처치)", 8)),
			PHANTOM_PLUME, 4, List.of(source(Set.of(EntityTypes.PHANTOM), 1.0F, 1), source(SKELETONS, 0.3F, 1)),
			List.of(mat(ModItems.ESSENCE, 8)));
		add("archer_3", JobClass.ARCHER, 3, "Heroic Spirit", "영웅의 영령",
			"A heroic archer can hit a ghast across a lava sea. Prove it, and fell a golem.",
			"영웅의 궁수는 용암 바다 건너의 가스트도 맞힌다. 증명하고, 골렘을 쓰러뜨려라.",
			List.of(kill("Mythril Golem", "미스릴 골렘", Set.of(ModEntities.MYTHRIL_GOLEM), 1), kill("Ghasts", "가스트", Set.of(EntityTypes.GHAST), 4)),
			SPIRIT_ARROW, 3, List.of(source(Set.of(EntityTypes.GHAST), 0.8F, 1)),
			List.of(mat(ModItems.CONDENSED_ESSENCE, 2)));
		add("archer_4", JobClass.ARCHER, 4, "Treasury of the King", "왕의 보물고",
			"The King of Heroes keeps his treasury locked with keys of gold. Defeat the Wither to claim them.",
			"영웅왕의 보물고는 황금 열쇠로 잠겨 있다. 위더를 쓰러뜨리고 열쇠를 손에 넣어라.",
			List.of(kill("The Wither", "위더", Set.of(EntityTypes.WITHER), 1), rangedKill("Monsters (ranged kills)", "몬스터 (원거리 처치)", 20)),
			GOLDEN_KEY, 3, List.of(source(Set.of(EntityTypes.WITHER), 1.0F, 3), source(HOSTILES, 0.1F, 1)),
			List.of(mat(ModItems.CONDENSED_ESSENCE, 4)));

		// ---------------------------------------------------------------- Pirate
		choice("pirate_1", JobClass.PIRATE, "Raise the Flag", "깃발을 올려라",
			"Raise your own flag. The sea belongs to whoever dares to sail it.",
			"너만의 깃발을 올려라. 바다는 감히 항해하는 자의 것이다.",
			MAP_SCRAP);
		add("pirate_2", JobClass.PIRATE, 2, "Captain's Bounty", "선장의 현상금",
			"Every captain needs a bounty. Raid the guardians of the monuments and the pillagers' outposts.",
			"모든 선장에게는 현상금이 필요하다. 해저 유적의 가디언과 약탈자 전초기지를 털어라.",
			List.of(kill("Guardians", "가디언", Set.of(EntityTypes.GUARDIAN), 4), kill("Pillagers", "약탈자", Set.of(EntityTypes.PILLAGER), 3)),
			BOUNTY_POSTER, 4, List.of(source(Set.of(EntityTypes.GUARDIAN), 0.6F, 1), source(Set.of(EntityTypes.PILLAGER), 0.6F, 1)),
			List.of(mat(ModItems.ESSENCE, 8)));
		add("pirate_3", JobClass.PIRATE, 3, "Emperor of the Sea", "사황의 바다",
			"An Emperor's haki breaks beasts. Crush a golem and the hoglins of the Nether.",
			"사황의 패기는 짐승을 꺾는다. 골렘과 네더의 호글린을 굴복시켜라.",
			List.of(kill("Mythril Golem", "미스릴 골렘", Set.of(ModEntities.MYTHRIL_GOLEM), 1), kill("Hoglins", "호글린", Set.of(EntityTypes.HOGLIN), 5)),
			HAKI_CRYSTAL, 3, List.of(source(Set.of(EntityTypes.HOGLIN), 0.6F, 1), source(Set.of(EntityTypes.PIGLIN_BRUTE), 1.0F, 1)),
			List.of(mat(ModItems.CONDENSED_ESSENCE, 2)));
		add("pirate_4", JobClass.PIRATE, 4, "One Piece", "원피스",
			"The great treasure waits at the end of the sea. Defeat the Elder Guardian and the drowned who guard it.",
			"위대한 보물은 바다의 끝에서 기다린다. 엘더 가디언과 그 보물을 지키는 드라운드를 쓰러뜨려라.",
			List.of(kill("Elder Guardian", "엘더 가디언", Set.of(EntityTypes.ELDER_GUARDIAN), 1), kill("Drowned", "드라운드", Set.of(EntityTypes.DROWNED), 15)),
			SEA_KINGS_TREASURE, 3, List.of(source(Set.of(EntityTypes.ELDER_GUARDIAN), 1.0F, 3), source(Set.of(EntityTypes.DROWNED), 0.2F, 1)),
			List.of(mat(ModItems.CONDENSED_ESSENCE, 4)));

		// ---------------------------------------------------------------- Soul Reaper
		choice("shinigami_1", JobClass.SHINIGAMI, "Konso", "혼장",
			"Take up the zanpakuto and guide the lingering dead to rest before they turn into Hollows.",
			"참백도를 들고, 떠도는 망자가 호로가 되기 전에 저편으로 보내 주어라.",
			HOLLOW_MASK_SHARD);
		add("shinigami_2", JobClass.SHINIGAMI, 2, "The Name of Your Blade", "참백도의 이름",
			"Your zanpakuto will only tell you its name in the dark between worlds. Hunt the endermen and follow the hell butterflies.",
			"참백도는 세계 사이의 어둠 속에서만 이름을 알려 준다. 엔더맨을 사냥하고 지옥나비를 따라가라.",
			List.of(kill("Endermen", "엔더맨", Set.of(EntityTypes.ENDERMAN), 4), kill("Spiders", "거미", SPIDERS, 6)),
			HELL_BUTTERFLY, 4, List.of(source(Set.of(EntityTypes.ENDERMAN), 0.8F, 1), source(SPIDERS, 0.3F, 1)),
			List.of(mat(ModItems.ESSENCE, 8)));
		add("shinigami_3", JobClass.SHINIGAMI, 3, "Bankai Training", "만해 수행",
			"Bankai takes ten years - or three days with a Tenshintai. Break a golem and the frozen dead to earn one.",
			"만해는 십 년이 걸린다. 전신체가 있다면 사흘이다. 골렘과 얼어붙은 망자를 쓰러뜨려 하나를 얻어라.",
			List.of(kill("Mythril Golem", "미스릴 골렘", Set.of(ModEntities.MYTHRIL_GOLEM), 1), kill("Strays", "스트레이", Set.of(EntityTypes.STRAY), 8)),
			TENSHINTAI, 3, List.of(source(Set.of(EntityTypes.STRAY), 0.5F, 1), source(Set.of(ModEntities.MYTHRIL_GOLEM), 1.0F, 2)),
			List.of(mat(ModItems.CONDENSED_ESSENCE, 2)));
		add("shinigami_4", JobClass.SHINIGAMI, 4, "Hueco Mundo", "후에코 문도",
			"Beyond the white desert, a traitor waits with the Hogyoku. Destroy the Wither and the husks of the sands.",
			"하얀 사막 너머에서 배신자가 붕옥과 함께 기다린다. 위더와 모래의 허스크를 쓰러뜨려라.",
			List.of(kill("The Wither", "위더", Set.of(EntityTypes.WITHER), 1), kill("Husks", "허스크", Set.of(EntityTypes.HUSK), 12)),
			HOGYOKU_FRAGMENT, 3, List.of(source(Set.of(EntityTypes.WITHER), 1.0F, 3), source(Set.of(EntityTypes.HUSK), 0.2F, 1)),
			List.of(mat(ModItems.CONDENSED_ESSENCE, 4)));

		// ---------------------------------------------------------------- Hunter
		choice("hunter_1", JobClass.HUNTER, "The Hunter Exam", "헌터 시험",
			"You passed the Hunter Exam. Here is your license - what you hunt is up to you.",
			"헌터 시험 합격이다. 라이선스를 받아라. 무엇을 사냥할지는 네가 정해.",
			EXAM_BADGE);
		add("hunter_2", JobClass.HUNTER, 2, "Water Divination", "수견식",
			"A glass of water and a leaf will tell what kind of Nen you have. Train hard enough to make it move.",
			"물이 든 유리잔과 잎 한 장이 네 념의 계통을 알려 줄 거야. 잎이 움직일 만큼 수련해.",
			List.of(kill("Skeletons", "스켈레톤", SKELETONS, 8), kill("Endermen", "엔더맨", Set.of(EntityTypes.ENDERMAN), 3)),
			DIVINATION_GLASS, 4, List.of(source(SKELETONS, 0.5F, 1), source(Set.of(EntityTypes.ENDERMAN), 0.6F, 1)),
			List.of(mat(ModItems.ESSENCE, 8)));
		add("hunter_3", JobClass.HUNTER, 3, "Chimera Ant Extermination", "키메라 앤트 토벌",
			"The Chimera Ants are spreading. Break their golem guard and wipe out the nests.",
			"키메라 앤트가 퍼지고 있다. 골렘 수호자를 부수고 둥지를 쓸어버려라.",
			List.of(kill("Mythril Golem", "미스릴 골렘", Set.of(ModEntities.MYTHRIL_GOLEM), 1), kill("Spiders", "거미", SPIDERS, 10)),
			CHIMERA_CARAPACE, 3, List.of(source(SPIDERS, 0.4F, 1), source(Set.of(ModEntities.MYTHRIL_GOLEM), 1.0F, 2)),
			List.of(mat(ModItems.CONDENSED_ESSENCE, 2)));
		add("hunter_4", JobClass.HUNTER, 4, "The Dark Continent", "암흑대륙",
			"Only a Triple-Star Hunter may join the expedition. Bring back a relic from the deep dark and its Warden.",
			"트리플 헌터만이 원정에 참가할 수 있다. 깊은 어둠과 그 워든에게서 유물을 가져와라.",
			List.of(kill("The Warden", "워든", Set.of(EntityTypes.WARDEN), 1), kill("Endermen", "엔더맨", Set.of(EntityTypes.ENDERMAN), 12)),
			DARK_CONTINENT_RELIC, 3, List.of(source(Set.of(EntityTypes.WARDEN), 1.0F, 3), source(Set.of(EntityTypes.ENDERMAN), 0.2F, 1)),
			List.of(mat(ModItems.CONDENSED_ESSENCE, 4)));
	}

	/** The first choice of class: no trial, the trainer grants tier 1 at level 10. Its token no longer drops but stays registered. */
	private static void choice(final String id, final JobClass job, final String en, final String ko, final String storyEn, final String storyKo, final Item token) {
		add(id, job, 1, en, ko, storyEn, storyKo, List.of(), token, 0, List.of(), List.of());
	}
	private static void add(
		final String id, final JobClass job, final int tier, final String en, final String ko, final String storyEn, final String storyKo, final List<KillGoal> kills,
		final Item token, final int tokenCount, final List<TokenSource> sources, final List<Material> materials
	) {
		BY_ID.put(id, new QuestDef(id, job, tier, en, ko, storyEn, storyKo, kills, token, tokenCount, sources, materials));
	}

	private static KillGoal kill(final String en, final String ko, final Set<EntityType<?>> types, final int count) {
		return new KillGoal(en, ko, types, count, false);
	}

	private static KillGoal rangedKill(final String en, final String ko, final int count) {
		return new KillGoal(en, ko, HOSTILES, count, true);
	}

	private static TokenSource source(final Set<EntityType<?>> types, final float chance, final int amount) {
		return new TokenSource(types, chance, amount);
	}

	private static Material mat(final Item item, final int count) {
		return new Material(item, count);
	}

	public static @Nullable QuestDef get(final String id) {
		return BY_ID.get(id);
	}

	/** The trial that leads to {@code tier} of {@code job}. */
	public static QuestDef forTier(final JobClass job, final int tier) {
		return BY_ID.get(job.id() + "_" + tier);
	}

	public static List<QuestDef> all() {
		return List.copyOf(BY_ID.values());
	}

	public static List<Item> tokens() {
		return Collections.unmodifiableList(TOKENS);
	}

	private Quests() {
	}
}
