package com.minecraftmode.client.datagen;

import com.minecraftmode.domain.DomainStyle;
import com.minecraftmode.raid.ArenaTheme;
import com.minecraftmode.raid.BossDef;
import com.minecraftmode.raid.RaidBosses;
import com.minecraftmode.economy.ShopOffers;
import com.minecraftmode.economy.ShopType;
import com.minecraftmode.registry.ModEntities;
import com.minecraftmode.registry.ModItems;
import java.util.Map;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricLanguageProvider.TranslationBuilder;

/** Translations for raid bosses, raids, parties and loot sharing. */
final class RaidLang {
	/** Boss lines shown under "Phase N" when a phase starts: id -> {en2, ko2, en3, ko3, (en4, ko4)}. */
	private static final Map<String, String[]> PHASES = Map.of(
		"verdessa", new String[] {
			"Spores drift from her branches...", "그녀의 가지에서 포자가 흩날린다...",
			"Verdessa tears free of her roots - brambles everywhere!", "베르데사가 뿌리를 뜯어냈다 - 사방에 가시덤불!"},
		"nefrekhet", new String[] {
			"The sands rise and the guard wakes.", "모래가 일어나고 근위대가 깨어난다.",
			"The Judgment of Ra burns across the hall!", "라의 심판이 홀을 가로질러 불탄다!"},
		"raijin", new String[] {
			"Storm spirits ride the wind.", "폭풍의 정령들이 바람을 탄다.",
			"Raijin descends with the thunder!", "라이진이 천둥과 함께 내려온다!"},
		"arachne", new String[] {
			"The brood stirs in the walls...", "벽 속에서 새끼들이 꿈틀거린다...",
			"Arachne is frenzied! Beware the Silk Storm!", "아라크네가 광분했다! 비단 폭풍을 조심하라!"},
		"gorvath", new String[] {
			"The summit shakes - rocks begin to fall!", "정상이 흔들린다 - 바위가 쏟아진다!",
			"Gorvath tears loose: AVALANCHE!", "고르바스가 몸을 일으킨다: 눈사태!"},
		"kraken", new String[] {
			"The drowned crew answers the call.", "가라앉은 선원들이 부름에 답한다.",
			"The Crushing Tide rises!", "분쇄의 해일이 일어난다!"},
		"ignis", new String[] {
			"The crater fills with falling fire.", "분화구에 불비가 쏟아진다.",
			"Ignis is reborn in flame - get away!", "이그니스가 불꽃 속에서 다시 태어난다 - 멀리 피하라!"},
		"malachar", new String[] {
			"The dead rise to serve their king.", "망자들이 왕을 섬기려 일어난다.",
			"Requiem: only his feet are safe from the Death Nova.", "진혼곡: 죽음의 신성에서 안전한 곳은 그의 발치뿐."},
		"aethryx", new String[] {
			"The void opens - orbs seek you out.", "공허가 열린다 - 구체들이 너를 쫓는다.",
			"Rifts tear the island apart.", "균열이 섬을 찢는다.",
			"Starfall! The island collapses!", "별이 떨어진다! 섬이 무너진다!"},
		"sukuna", new String[] {
			"Slashes rain down - Kamutoke flashes!", "참격이 쏟아진다 - 카무토케가 번뜩인다!",
			"A flame is drawn between his hands...", "그의 두 손 사이에 불꽃이 당겨진다...",
			"The King of Curses stops holding back!", "저주의 왕이 봐주기를 그만둔다!"}
	);

	/** Mechanic id -> {en name, ko name, en hint, ko hint}. */
	static final String[][] MECHANICS = {
		{"verdessa_seed_trees", "Seed Trees", "씨앗나무", "Seed trees are growing. Cut every one down within 18 seconds before they bloom!",
			"씨앗나무가 자란다. 꽃피기 전에 18초 안에 모두 베어라!"},
		{"nefrekhet_quicksand", "Quicksand", "유사", "The floor sinks! Get onto a pedestal - two of the four will crumble (red).",
			"바닥이 꺼진다! 받침대 위로 - 넷 중 둘은 무너진다(빨강)."},
		{"raijin_storm_eye", "Eye of the Storm", "폭풍의 눈", "Stay inside the drifting calm (green circle). Lightning finds everyone outside it.",
			"움직이는 고요한 원(초록) 안에 머물러라. 원 밖은 번개가 내리친다."},
		{"raijin_thunder_drums", "Thunder Drums", "뇌고", "Silence every thunder drum within 18 seconds, or the thunderclap ends the fight.",
			"18초 안에 뇌고를 모두 잠재워라. 아니면 천둥소리가 싸움을 끝낸다."},
		{"arachne_venom_deluge", "Venom Deluge", "독의 홍수", "The floor floods with venom! Get inside a silk cocoon (green circle).", "바닥이 독으로 잠긴다! 거미줄 고치(초록 원) 안으로!"},
		{"gorvath_rockslide", "Rockslide", "산사태", "The peak comes down on everyone Gorvath can see. Hide behind a pillar!", "고르바스의 눈에 보이는 자에게 산이 무너진다. 기둥 뒤로 숨어라!"},
		{"gorvath_shoulder", "Shoulder the Mountain", "산을 떠받쳐라", "A peak is falling: 60% of the party must stand in the green circle together.",
			"봉우리가 떨어진다: 파티의 60% 이상이 초록 원 안에 함께 서야 한다."},
		{"kraken_ink_marks", "Ink Marks", "먹물 낙인", "Every mark bursts and kills anyone else within 6 blocks. Spread out!", "낙인이 터지면 6블록 안의 다른 사람이 죽는다. 흩어져라!"},
		{"kraken_anchors", "Anchor Chains", "닻을 지켜라", "Someone must hold every anchor (green circles) or the ship goes down.", "모든 닻(초록 원)에 한 명씩 서야 배가 버틴다."},
		{"ignis_solar_flare", "Solar Flare", "태양 섬광", "Turn your back on Ignis before the flash!", "섬광이 터지기 전에 이그니스에게 등을 돌려라!"},
		{"ignis_phoenix_eggs", "Phoenix Eggs", "불사조의 알", "Destroy every ember warden within 15 seconds or the eggs hatch in a supernova.",
			"15초 안에 불씨 수호 정령을 모두 쓰러뜨려라. 아니면 알이 깨어나 초신성이 된다."},
		{"malachar_stillness", "Requiem of Stillness", "정적의 진혼곡", "When the requiem begins, do not move a step.", "진혼곡이 시작되면 한 발짝도 움직이지 마라."},
		{"malachar_phylactery", "Phylactery", "성물함", "Malachar cannot be hurt while his guardians stand. Destroy them all in 20 seconds!",
			"수호자가 남아 있는 동안 말라카르는 무적이다. 20초 안에 모두 파괴하라!"},
		{"malachar_doom", "Doom", "파멸의 낙인", "The marked must reach the soul font (green circle) within 10 seconds.", "낙인이 찍힌 자는 10초 안에 영혼의 샘(초록 원)으로 가야 한다."},
		{"aethryx_event_horizon", "Event Horizon", "사건의 지평선", "A black hole pulls you in. Be more than 11 blocks from the center when it collapses!",
			"블랙홀이 끌어당긴다. 붕괴할 때 중심에서 11블록 밖에 있어라!"},
		{"aethryx_seals", "Starfall Seals", "별의 봉인", "Every seal needs someone standing on it when the stars fall.", "별이 떨어질 때 모든 봉인 위에 누군가 서 있어야 한다."},
		{"aethryx_judgment", "Void Judgment", "공허의 심판", "Gather on the marked player: 60% of the party must share the blow.", "표식이 찍힌 사람에게 모여라: 파티의 60% 이상이 함께 받아내야 한다."},
		{"sukuna_mahoraga", "Ten Shadows: Mahoraga", "십종영법술: 마허라",
			"Sukuna cannot be hurt while Mahoraga stands. It adapts to whatever hurts it most - fell it fast with many kinds of damage before its wheel turns eight times!",
			"마허라가 서 있는 동안 스쿠나는 피해를 받지 않는다. 마허라는 가장 아프게 한 것에 적응한다 - 법진이 여덟 번 돌기 전에 여러 종류의 피해로 빠르게 쓰러뜨려라!"},
		{"sukuna_domain", "Domain Expansion: Malevolent Shrine", "영역전개: 복마어주자",
			"Inside the red edge every slash is a sure hit. Run out past it, or shelter in a Simple Domain (blue circle) - the first ones break once the second ones appear!",
			"붉은 경계 안에서는 모든 참격이 필중이다. 경계 밖으로 달아나거나 간이영역(파란 원) 안으로 피하라 - 첫 원은 두 번째 원이 생기면 깨진다!"},
	};

	static void add(final TranslationBuilder b, final boolean ko) {
		bosses(b, ko);
		mechanics(b, ko);
		raid(b, ko);
		party(b, ko);
		loot(b, ko);
	}

	private static void bosses(final TranslationBuilder b, final boolean ko) {
		for (BossDef def : RaidBosses.all()) {
			b.add(RaidBosses.type(def), ko ? def.ko() : def.en());
			b.add(def.epithetKey(), ko ? def.epithetKo() : def.epithetEn());
			b.add(def.descKey(), ko ? def.descKo() : def.descEn());
			String[] lines = PHASES.get(def.id());
			for (int phase = 2; phase <= def.phaseCount(); phase++) {
				int i = (phase - 2) * 2 + (ko ? 1 : 0);
				b.add(def.nameKey() + ".phase" + phase, lines != null && i < lines.length ? lines[i] : "");
			}
		}
		b.add(ModEntities.MAHORAGA, ko ? "팔악검 이계신장 마허라" : "Eight-Handled Sword Divergent Sila Divine General Mahoraga");
		b.add(ModEntities.MALEVOLENT_SHRINE, ko ? "복마어주자" : "Malevolent Shrine");
		b.add(DomainStyle.MALEVOLENT_SHRINE.nameKey(), ko ? "영역전개 · 복마어주자" : "Domain Expansion · Malevolent Shrine");
		b.add("domain.minecraft_mode.sure_hit", ko ? "필중 범위 - 간이영역으로 피하거나 경계 밖으로 달아나라" : "Sure hit - shelter in a Simple Domain or run past the edge");
		String s = "raid.minecraft_mode.sukuna.";
		b.add(s + "guarded", ko ? "마허라가 서 있는 동안 스쿠나에게는 피해가 들어가지 않는다!" : "Sukuna cannot be hurt while Mahoraga stands!");
		b.add(s + "fuga", ko ? "■ 개 「푸가」" : "■ Open: Fuga");
		b.add(s + "world_slash", ko ? "세계를 가르는 참격" : "World-Cutting Slash");
		b.add(s + "world_slash.hint", ko ? "선 밖으로 피하라: 어떤 방어도 소용없다!" : "Get out of the line: no defense holds it!");
		b.add(s + "rct", ko ? "반전술식" : "Reverse Cursed Technique");
		b.add(s + "rct.hint", ko ? "체력의 %s%%만큼 피해를 주어 회복을 끊어라!" : "Deal %s%% of his health to break the heal!");
		b.add(s + "rct_broken", ko ? "반전술식 저지!" : "The heal is broken!");
		b.add(s + "rct_healed", ko ? "스쿠나의 상처가 아물었다" : "Sukuna's wounds close");
		b.add(ModItems.SUKUNA_FINGER, ko ? "스쿠나의 손가락" : "Sukuna's Finger");
		b.add(ModItems.SUKUNA_FINGER.getDescriptionId() + ".tooltip", ko
			? "특급 주물. 레이드 입구 옆 저주 유물 교환소에서 스쿠나의 장신구(" + ShopOffers.RELIC_PIECE_FINGERS + "개)나 3티어 장신구 계승권(4티어 스쿠나 세트로, " + ShopOffers.RELIC_TICKET_FINGERS + "개)으로 바꿉니다."
			: "A special grade cursed object. Trade it at the cursed relic exchange by the raid gate for one of Sukuna's accessories (" + ShopOffers.RELIC_PIECE_FINGERS
				+ ") or a Tier 3 Accessory Inheritance Ticket into his tier 4 set (" + ShopOffers.RELIC_TICKET_FINGERS + ").");
		b.add(ModItems.RELIC_SHOP, ko ? "저주 유물 교환소" : "Cursed Relic Exchange");
		b.add(ShopType.CURSED_RELICS.titleKey(), ko ? "저주 유물 교환소" : "Cursed Relic Exchange");
		String m = "raid.minecraft_mode.mahoraga.";
		b.add(m + "adapted", ko ? "법진이 돈다: 마허라가 %s에 적응했다 (내성 %s%%, %s/%s)" : "The wheel turns: Mahoraga adapted to %s (%s%% resistance, %s/%s)");
		b.add(m + "complete", ko ? "마허라가 완전히 적응했다" : "Mahoraga has fully adapted");
		b.add(m + "phenomenon.strike", ko ? "타격" : "strikes");
		b.add(m + "phenomenon.shot", ko ? "사격" : "shots");
		b.add(m + "phenomenon.fire", ko ? "불" : "fire");
		b.add(m + "phenomenon.lightning", ko ? "번개" : "lightning");
		b.add(m + "phenomenon.explosion", ko ? "폭발" : "explosions");
		b.add(m + "phenomenon.technique", ko ? "%s의 술식" : "the %s's technique");
		b.add("entity.minecraft_mode.aethryx.collapse",ko ? "섬이 무너진다 - 중심으로!" : "The island collapses - get to the center!");
	}

	private static void mechanics(final TranslationBuilder b, final boolean ko) {
		for (String[] m : MECHANICS) {
			b.add("raid.minecraft_mode.mechanic." + m[0], ko ? m[2] : m[1]);
			b.add("raid.minecraft_mode.mechanic." + m[0] + ".hint", ko ? m[4] : m[3]);
		}
		b.add("raid.minecraft_mode.mechanic.malachar_stillness.now", ko ? "지금! 움직이지 마라!" : "Now! Do not move!");
		b.add("raid.minecraft_mode.mechanic.malachar_doom.marked", ko ? "파멸의 낙인이 새겨졌다! 영혼의 샘으로!" : "You bear the mark of Doom! Get to the soul font!");
		b.add("raid.minecraft_mode.mechanic.malachar_doom.cleansed", ko ? "낙인이 정화되었다." : "The mark is cleansed.");
		b.add("raid.minecraft_mode.mechanic.count", "%s: %s / %s");
		b.add("raid.minecraft_mode.mechanic.failed", ko ? "%s 실패!" : "%s failed!");
		b.add("raid.minecraft_mode.mechanic.cleared", ko ? "%s 성공!" : "%s cleared!");
		b.add("raid.minecraft_mode.ember_warden", ko ? "불씨 수호 정령" : "Ember Warden");
		b.add("raid.minecraft_mode.phylactery_guard", ko ? "성물 수호자" : "Phylactery Guardian");
		b.add("raid.minecraft_mode.seed_tree", ko ? "가시 씨앗나무" : "Thorn Seed Tree");
		b.add("raid.minecraft_mode.thunder_drum", ko ? "뇌고 정령" : "Thunder Drum Spirit");
		b.add("death.attack.minecraft_mode.raid_mechanic", ko ? "%1$s은(는) 레이드 기믹에 휘말렸다" : "%1$s was caught by a raid mechanic");
		b.add("death.attack.minecraft_mode.monster_magic", ko ? "%1$s은(는) 마법에 쓰러졌다" : "%1$s was slain by magic");
		b.add("death.attack.minecraft_mode.monster_magic.player", ko ? "%1$s은(는) %2$s의 마법에 쓰러졌다" : "%1$s was slain by %2$s's magic");
		b.add("death.attack.minecraft_mode.raid_mechanic.player", ko ? "%1$s은(는) %2$s의 기믹에 휘말렸다" : "%1$s was caught by %2$s's mechanic");
	}

	private static void raid(final TranslationBuilder b, final boolean ko) {
		b.add("raid.minecraft_mode.phase", ko ? "%s 페이즈" : "Phase %s");
		b.add("raid.minecraft_mode.victory", ko ? "토벌 성공!" : "VICTORY!");
		b.add("raid.minecraft_mode.fight", ko ? "전투 개시!" : "Fight!");
		b.add("key.minecraft_mode.loot_screen", ko ? "레이드 전리품 창" : "Raid Loot Screen");

		b.add("screen.minecraft_mode.raid.title", ko ? "레이드 토벌 게시판" : "Raid Board");
		b.add("screen.minecraft_mode.raid.levels", ko ? "드롭 Lv %s–%s" : "Drops Lv %s–%s");
		b.add("screen.minecraft_mode.raid.phases", ko ? "페이즈 %s" : "%s phases");
		b.add("screen.minecraft_mode.raid.fee", ko ? "입장료 %s (1인)" : "Entry fee %s each");
		b.add("screen.minecraft_mode.raid.arena", ko ? "무대: %s" : "Arena: %s");
		b.add("screen.minecraft_mode.raid.party", ko ? "파티 (%s/%s)" : "Party (%s/%s)");
		b.add("screen.minecraft_mode.raid.enter", ko ? "입장" : "Enter");
		b.add("screen.minecraft_mode.raid.not_leader", ko ? "파티장만 입장을 시작할 수 있습니다." : "Only the party leader can start the raid.");
		b.add("screen.minecraft_mode.raid.hint", ko
			? "%s블록 안의 파티원이 함께 입장합니다. 초대는 모험가 메뉴의 파티 탭에서"
			: "Party members within %s blocks come along. Invite from the adventurer's menu, party tab");
		Map<ArenaTheme, String[]> arenas = Map.of(
			ArenaTheme.WEB_CAVE, new String[] {"Web Cave", "거미줄 동굴"},
			ArenaTheme.MOUNTAIN, new String[] {"Mountain Summit", "산 정상"},
			ArenaTheme.SUNKEN_SHIP, new String[] {"Sunken Flagship", "침몰한 기함"},
			ArenaTheme.VOLCANO, new String[] {"Volcanic Crater", "화산 분화구"},
			ArenaTheme.NECROPOLIS, new String[] {"Sanctum of the Dead", "망자의 성소"},
			ArenaTheme.VOID_ISLES, new String[] {"Void Isles", "공허의 부유섬"},
			ArenaTheme.FOREST_GROVE, new String[] {"Overgrown Grove", "우거진 숲의 유적"},
			ArenaTheme.DESERT_TOMB, new String[] {"Pharaoh's Tomb", "파라오의 무덤"},
			ArenaTheme.SKY_SHRINE, new String[] {"Shrine Above the Clouds", "구름 위 신전"},
			ArenaTheme.MALEVOLENT_SHRINE, new String[] {"Malevolent Shrine", "복마어주자"});
		for (ArenaTheme theme : ArenaTheme.values()) {
			b.add("screen.minecraft_mode.raid.arena." + theme.name().toLowerCase(), arenas.get(theme)[ko ? 1 : 0]);
		}

		String p = "message.minecraft_mode.raid.";
		b.add(p + "not_leader", ko ? "파티장만 레이드를 시작할 수 있습니다." : "Only the party leader can start a raid.");
		b.add(p + "not_ready", ko ? "입장할 수 없습니다:" : "The party is not ready:");
		b.add(p + "problem.in_raid", ko ? "이미 레이드 중" : "already in a raid");
		b.add(p + "problem.too_far", ko ? "너무 멀리 있음" : "too far away");
		b.add(p + "problem.level", ko ? "레벨 %s 필요" : "needs level %s");
		b.add(p + "problem.dead", ko ? "사망 상태" : "dead");
		b.add(p + "problem.past_cap", ko ? "악몽 난이도 %s 클리어 필요" : "needs a Nightmare %s clear");
		b.add(p + "problem.fee", ko ? "입장료 %s 부족" : "cannot pay the %s fee");
		b.add(p + "fee_paid", ko ? "입장료 %s을(를) 냈습니다." : "Paid the %s entry fee.");
		b.add(p + "busy", ko ? "모든 경기장이 사용 중입니다. 잠시 후 다시 시도하세요." : "All arenas are in use. Try again shortly.");
		b.add(p + "entered", ko ? "%s 토벌 시작! %s초 뒤 보스가 나타납니다. 죽어도 아이템은 잃지 않습니다." : "Raid on %s! The boss appears in %s seconds. Dying here never costs items.");
		b.add(p + "not_in_raid", ko ? "레이드 중이 아닙니다." : "You are not in a raid.");
		b.add(p + "left", ko ? "%s님이 전투에서 이탈했습니다." : "%s left the fight.");
		b.add(p + "fallen", ko ? "%s님이 쓰러졌습니다!" : "%s has fallen!");
		b.add(p + "fallen_self", ko ? "쓰러져서 도시로 돌아왔습니다. 아이템은 그대로입니다." : "You fell and were carried back. You kept everything.");
		b.add(p + "failed", ko ? "%s 토벌 실패. 모두 전투에서 이탈했습니다." : "The raid on %s failed: nobody is left fighting.");
		b.add(p + "returning", ko ? "%s초 뒤 귀환 (일시정지 메뉴의 '귀환': 바로 귀환)" : "Returning in %ss (\"Return home\" in the pause menu to go now)");
		b.add(p + "victory", ko ? "%s 토벌 성공! 보상이 지급되었습니다. %s초 뒤 귀환합니다." : "%s is defeated! Rewards handed out. Returning in %s seconds.");
		b.add(p + "mvp", ko ? "★ MVP: %s (기여도 %s%%)" : "★ MVP: %s (%s%% of the contribution)");

		String r = "screen.minecraft_mode.raid_report.";
		b.add(r + "title", ko ? "전투 결과" : "Fight Report");
		b.add(r + "time", ko ? "%s · 전투 시간 %s" : "%s · fight time %s");
		b.add(r + "total", ko ? "총 피해 %s · 초당 %s" : "Total %s · %s/s");
		b.add(r + "percent", ko ? "피해 %s%%" : "%s%% dmg");
		b.add(r + "dps", ko ? "초당 %s" : "%s/s");
		b.add(r + "help", ko ? "MVP: 피해+지원 기여도 1위 (2인 이상)"
			: "MVP: top contribution, damage + support (2+ players)");
		b.add(r + "loot", ko ? "전리품 분배 ▶" : "Loot ▶");
		b.add(r + "open", ko ? "전투 결과" : "Fight report");
	}

	private static void party(final TranslationBuilder b, final boolean ko) {
		String p = "message.minecraft_mode.party.";
		b.add(p + "already_in", ko ? "이미 파티에 속해 있습니다." : "You are already in a party.");
		b.add(p + "created", ko ? "파티를 만들었습니다. 모험가 메뉴의 파티 탭에서 초대하세요." : "Party created. Invite players from the adventurer's menu, party tab.");
		b.add(p + "not_online", ko ? "%s님은 접속해 있지 않습니다." : "%s is not online.");
		b.add(p + "not_leader", ko ? "파티장만 할 수 있습니다." : "Only the party leader can do that.");
		b.add(p + "self", ko ? "자기 자신은 초대할 수 없습니다." : "You cannot invite yourself.");
		b.add(p + "target_in_party", ko ? "%s님은 이미 다른 파티에 있습니다." : "%s is already in a party.");
		b.add(p + "full", ko ? "파티가 가득 찼습니다 (최대 %s명)." : "The party is full (%s max).");
		b.add(p + "invited", ko ? "%s님을 초대했습니다." : "Invited %s.");
		b.add(p + "accept_button", ko ? "[수락]" : "[Accept]");
		b.add(p + "invite_received", ko ? "%s님의 파티 초대 (1분)." : "%s invites you to a party (1 minute).");
		b.add(p + "no_invite", ko ? "받은 초대가 없습니다." : "You have no party invite.");
		b.add(p + "joined", ko ? "%s님이 파티에 들어왔습니다." : "%s joined the party.");
		b.add(p + "none", ko ? "파티에 속해 있지 않습니다." : "You are not in a party.");
		b.add(p + "left_self", ko ? "파티를 떠났습니다." : "You left the party.");
		b.add(p + "left", ko ? "%s님이 파티를 떠났습니다." : "%s left the party.");
		b.add(p + "kicked_self", ko ? "파티에서 추방되었습니다." : "You were removed from the party.");
		b.add(p + "kicked", ko ? "%s님이 추방되었습니다." : "%s was removed from the party.");
		b.add(p + "not_member", ko ? "%s님은 파티원이 아닙니다." : "%s is not in your party.");
		b.add(p + "promoted", ko ? "%s님이 파티장이 되었습니다." : "%s is now the party leader.");
		b.add(p + "disbanded", ko ? "파티가 해산되었습니다." : "The party was disbanded.");
		b.add(p + "list_header", ko ? "파티 (%s/%s):" : "Party (%s/%s):");
	}

	private static void loot(final TranslationBuilder b, final boolean ko) {
		String s = "screen.minecraft_mode.loot.";
		b.add(s + "title", ko ? "레이드 전리품" : "Raid Loot");
		b.add(s + "coins", ko ? "보유:" : "Coins:");
		b.add(s + "auction", ko ? "경매" : "Auction");
		b.add(s + "dice", ko ? "주사위" : "Dice");
		b.add(s + "waiting", ko ? "대기 중" : "Waiting");
		b.add(s + "finished", ko ? "모든 전리품의 주인이 정해졌습니다." : "Every item has found its owner.");
		b.add(s + "start", ko ? "시작가 %s · 입찰 단위 %s" : "Start %s · step %s");
		b.add(s + "no_bid", ko ? "아직 입찰 없음" : "No bids yet");
		b.add(s + "highest", ko ? "최고 입찰 %s (%s)" : "Highest bid %s (%s)");
		b.add(s + "auction_help", ko
			? "입찰하면 동전이 바로 맡겨지고, 더 높은 입찰이 나오면 돌려받습니다. 마지막 10초 안의 입찰은 시간을 연장합니다. 낙찰 금액은 나머지 파티원에게 나눠집니다."
			: "Bids take your coins right away and refund them if you are outbid. Bids in the last 10 seconds extend the auction. The price is shared among the others.");
		b.add(s + "roll", ko ? "주사위 굴리기" : "Roll");
		b.add(s + "pass", ko ? "포기" : "Pass");
		b.add(s + "my_roll", ko ? "내 주사위: %s" : "Your roll: %s");
		b.add(s + "passed", ko ? "포기함" : "You passed");
		b.add(s + "not_eligible", ko ? "동점자 재굴림 중" : "Tie re-roll in progress");
		b.add(s + "roll_help", ko ? "1–100을 굴립니다. 가장 높은 사람이 가져갑니다." : "Roll 1–100; the highest roll takes it.");
		b.add(s + "switch_dice", ko ? "주사위로 변경" : "Switch to dice");
		b.add(s + "switch_auction", ko ? "경매로 변경" : "Switch to auction");
		b.add(s + "won_for", ko ? "%s 낙찰 (%s)" : "Won by %s for %s");
		b.add(s + "won_by", ko ? "%s 획득" : "Won by %s");
		b.add(s + "give_up", ko ? "이 물건 포기" : "Give this up");
		b.add(s + "gave_up", ko ? "이 물건을 포기했습니다. 나머지가 정해지면 바로 넘어갑니다." : "You gave this up; it ends as soon as the others decide.");
		b.add(s + "leave", ko ? "경매 나가기" : "Leave loot");
		b.add(s + "leave_confirm", ko ? "정말 나가기?" : "Really leave?");
		b.add(s + "leave_tip", ko
			? "남은 전리품을 모두 포기하고 분배에서 빠집니다. 맡긴 입찰금은 돌려받지만, 이후 낙찰금 분배도 받지 않습니다."
			: "Give up every item still to come and leave the sharing. A bid you hold is refunded, but you get no share of later sales.");
		b.add(s + "left_count", ko ? "%s명 나감" : "%s left");

		String p = "message.minecraft_mode.loot.";
		b.add(p + "none", ko ? "진행 중인 전리품 분배가 없습니다." : "There is no loot to share right now.");
		b.add(p + "won_solo", ko ? "전리품: %s" : "Loot: %s");
		b.add(p + "started", ko ? "전리품 %s개 분배를 시작합니다 (Y 키로 창 열기)." : "Sharing %s items (press Y for the loot screen).");
		b.add(p + "already_highest", ko ? "이미 최고 입찰자입니다." : "You already have the highest bid.");
		b.add(p + "cannot_afford", ko ? "동전이 부족합니다 (%s 필요)." : "Not enough coins (%s needed).");
		b.add(p + "outbid", ko ? "%s님이 %s로 더 높게 입찰했습니다. 동전을 돌려받았습니다." : "%s outbid you with %s. Your coins were returned.");
		b.add(p + "bid", ko ? "%s님 입찰: %s (%s)" : "%s bids %s on %s");
		b.add(p + "rolled", ko ? "%s님의 주사위: %s" : "%s rolled %s");
		b.add(p + "mode_auction", ko ? "%s: 경매로 진행합니다." : "%s: auction.");
		b.add(p + "mode_dice", ko ? "%s: 주사위로 진행합니다." : "%s: dice.");
		b.add(p + "finished", ko ? "전리품 분배가 끝났습니다." : "Loot sharing is over.");
		b.add(p + "lot", ko ? "전리품 %s/%s: %s (시작가 %s)" : "Lot %s/%s: %s (starts at %s)");
		b.add(p + "no_bids", ko ? "%s: 입찰이 없어 주사위로 넘어갑니다." : "%s: no bids - rolling dice.");
		b.add(p + "share", ko ? "낙찰금 분배: %s" : "Your share of the sale: %s");
		b.add(p + "tie", ko ? "%s 동점! 동점자끼리 다시 굴립니다." : "Tie at %s! The tied players roll again.");
		b.add(p + "won_auction", ko ? "%s님이 %s을(를) %s에 낙찰받았습니다." : "%s won %s for %s.");
		b.add(p + "won_dice", ko ? "%s님이 %s을(를) 가져갑니다." : "%s takes %s.");
		b.add(p + "gave_up", ko ? "%s님이 %s을(를) 포기했습니다." : "%s gave up %s.");
		b.add(p + "left", ko ? "전리품 분배에서 나왔습니다." : "You left the loot sharing.");
		b.add(p + "left_new", ko ? "새 전리품 분배가 시작되어 이전 분배에서 나왔습니다." : "A new loot sharing started, so you left the previous one.");
		b.add(p + "participant_left", ko ? "%s님이 전리품 분배에서 나갔습니다." : "%s left the loot sharing.");
		b.add(p + "delivered", ko ? "접속하지 않은 동안 받은 전리품: 아이템 %s개, %s" : "While you were away you received %s item(s) and %s.");
	}

	private RaidLang() {
	}
}
