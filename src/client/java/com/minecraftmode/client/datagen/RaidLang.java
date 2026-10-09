package com.minecraftmode.client.datagen;

import com.minecraftmode.raid.ArenaTheme;
import com.minecraftmode.raid.BossDef;
import com.minecraftmode.raid.RaidBosses;
import java.util.Map;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricLanguageProvider.TranslationBuilder;

/** Translations for raid bosses, raids, parties and loot sharing. */
final class RaidLang {
	/** Boss lines shown under "Phase N" when a phase starts: id -> {en2, ko2, en3, ko3, (en4, ko4)}. */
	private static final Map<String, String[]> PHASES = Map.of(
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
			"Starfall! The island collapses!", "별이 떨어진다! 섬이 무너진다!"}
	);

	static void add(final TranslationBuilder b, final boolean ko) {
		bosses(b, ko);
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
		b.add("entity.minecraft_mode.aethryx.collapse", ko ? "섬이 무너진다 - 중심으로!" : "The island collapses - get to the center!");
	}

	private static void raid(final TranslationBuilder b, final boolean ko) {
		b.add("raid.minecraft_mode.phase", ko ? "%s 페이즈" : "Phase %s");
		b.add("raid.minecraft_mode.victory", ko ? "토벌 성공!" : "VICTORY!");
		b.add("raid.minecraft_mode.fight", ko ? "전투 개시!" : "Fight!");
		b.add("key.minecraft_mode.loot_screen", ko ? "레이드 전리품 창" : "Raid Loot Screen");

		b.add("screen.minecraft_mode.raid.title", ko ? "레이드 토벌 게시판" : "Raid Board");
		b.add("screen.minecraft_mode.raid.levels", ko ? "드롭 Lv %s–%s" : "Drops Lv %s–%s");
		b.add("screen.minecraft_mode.raid.phases", ko ? "페이즈 %s" : "%s phases");
		b.add("screen.minecraft_mode.raid.arena", ko ? "무대: %s" : "Arena: %s");
		b.add("screen.minecraft_mode.raid.party", ko ? "파티 (%s/%s)" : "Party (%s/%s)");
		b.add("screen.minecraft_mode.raid.enter", ko ? "입장" : "Enter");
		b.add("screen.minecraft_mode.raid.not_leader", ko ? "파티장만 입장을 시작할 수 있습니다." : "Only the party leader can start the raid.");
		b.add("screen.minecraft_mode.raid.hint", ko
			? "%s블록 안의 파티원이 함께 입장합니다. /party invite <이름>"
			: "Party members within %s blocks come along. /party invite <name>");
		Map<ArenaTheme, String[]> arenas = Map.of(
			ArenaTheme.WEB_CAVE, new String[] {"Web Cave", "거미줄 동굴"},
			ArenaTheme.MOUNTAIN, new String[] {"Mountain Summit", "산 정상"},
			ArenaTheme.SUNKEN_SHIP, new String[] {"Sunken Flagship", "침몰한 기함"},
			ArenaTheme.VOLCANO, new String[] {"Volcanic Crater", "화산 분화구"},
			ArenaTheme.NECROPOLIS, new String[] {"Sanctum of the Dead", "망자의 성소"},
			ArenaTheme.VOID_ISLES, new String[] {"Void Isles", "공허의 부유섬"});
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
		b.add(p + "busy", ko ? "모든 경기장이 사용 중입니다. 잠시 후 다시 시도하세요." : "All arenas are in use. Try again shortly.");
		b.add(p + "entered", ko ? "%s 토벌 시작! %s초 뒤 보스가 나타납니다. 죽어도 아이템은 잃지 않습니다." : "Raid on %s! The boss appears in %s seconds. Dying here never costs items.");
		b.add(p + "not_in_raid", ko ? "레이드 중이 아닙니다." : "You are not in a raid.");
		b.add(p + "left", ko ? "%s님이 전투에서 이탈했습니다." : "%s left the fight.");
		b.add(p + "fallen", ko ? "%s님이 쓰러졌습니다!" : "%s has fallen!");
		b.add(p + "fallen_self", ko ? "쓰러져서 도시로 돌아왔습니다. 아이템은 그대로입니다." : "You fell and were carried back. You kept everything.");
		b.add(p + "failed", ko ? "%s 토벌 실패. 모두 전투에서 이탈했습니다." : "The raid on %s failed: nobody is left fighting.");
		b.add(p + "returning", ko ? "%s초 뒤 귀환 (/raid leave: 바로 귀환)" : "Returning in %ss (/raid leave to go now)");
		b.add(p + "victory", ko ? "%s 토벌 성공! 보상이 지급되었습니다. %s초 뒤 귀환합니다." : "%s is defeated! Rewards handed out. Returning in %s seconds.");
	}

	private static void party(final TranslationBuilder b, final boolean ko) {
		String p = "message.minecraft_mode.party.";
		b.add(p + "already_in", ko ? "이미 파티에 속해 있습니다." : "You are already in a party.");
		b.add(p + "created", ko ? "파티를 만들었습니다. /party invite <이름>으로 초대하세요." : "Party created. Invite with /party invite <name>.");
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

		String p = "message.minecraft_mode.loot.";
		b.add(p + "none", ko ? "진행 중인 전리품 분배가 없습니다." : "There is no loot to share right now.");
		b.add(p + "won_solo", ko ? "전리품: %s" : "Loot: %s");
		b.add(p + "started", ko ? "전리품 %s개 분배를 시작합니다 (L 키로 창 열기)." : "Sharing %s items (press L for the loot screen).");
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
		b.add(p + "delivered", ko ? "접속하지 않은 동안 받은 전리품: 아이템 %s개, %s" : "While you were away you received %s item(s) and %s.");
	}

	private RaidLang() {
	}
}
