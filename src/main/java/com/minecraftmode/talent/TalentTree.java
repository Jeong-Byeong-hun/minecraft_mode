package com.minecraftmode.talent;

import static com.minecraftmode.job.engrave.EngraveStat.*;

import com.minecraftmode.job.JobClass;
import com.minecraftmode.job.engrave.EngraveStat;
import com.minecraftmode.job.gear.StatLine;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * Talent trees: three branches per class, five tiers each. The first four tiers take up to
 * {@link #RANKS} points, the fifth is a single-point capstone; a tier opens once the branch holds
 * {@link #PER_TIER} points per tier below it. Node ids ({@code class.branch.tier}) are saved keys.
 */
public final class TalentTree {
	public static final int RANKS = 5;
	public static final int PER_TIER = 4;
	public static final int TIERS = 5;

	/** A talent: what one rank adds, and how many ranks it takes. */
	public record Node(String id, String branch, int tier, String en, String ko, int maxRank, List<StatLine> perRank) {
		public String nameKey() {
			return "talent.minecraft_mode." + this.id;
		}

		public boolean capstone() {
			return this.tier == TIERS - 1;
		}

		public List<StatLine> lines(final int rank) {
			List<StatLine> out = new ArrayList<>();
			for (StatLine line : this.perRank) {
				out.add(StatLine.of(line.stat(), Math.round(line.value() * rank * 100.0F) / 100.0F));
			}
			return out;
		}
	}

	public record Branch(String id, JobClass job, String en, String ko, int color, List<Node> nodes) {
		public String nameKey() {
			return "talent.minecraft_mode." + this.job.id() + "." + this.id;
		}
	}

	private static final Map<JobClass, List<Branch>> TREES = new EnumMap<>(JobClass.class);
	private static final Map<String, Node> NODES = new LinkedHashMap<>();

	static {
		tree(JobClass.WARRIOR,
			branch("fury", "Fury", "투쟁", 0xE0533D,
				n("Heavy Blows", "강타", l(BASIC_DAMAGE, 2)), n("Flurry of Steel", "연속 베기", l(ATTACK_SPEED, 1.5F)),
				n("Executioner", "처형", l(EXECUTE, 4)), n("Bloodlust", "피의 갈망", l(LIFESTEAL, 0.6F)),
				n("Soul of the Berserker", "광전사의 혼", l(BASIC_DAMAGE, 10), l(CRIT_DAMAGE, 20))),
			branch("guard", "Guardian", "수호", 0x8AA0C0,
				n("Iron Skin", "강철 피부", l(DAMAGE_REDUCTION, 1)), n("Giant's Vigor", "거인의 체력", l(MAX_HEALTH, 1)),
				n("Unbowed", "불굴", l(KNOCKBACK_RES, 6)), n("Spiked Plate", "가시 갑주", l(THORNS, 5)),
				n("Rampart", "성벽", l(DAMAGE_REDUCTION, 5), l(LAST_STAND, 15))),
			branch("tactics", "Tactics", "전술", 0xE8B730,
				n("Battle Focus", "전투 집중", l(COOLDOWN, 1.5F)), n("Momentum", "기세", l(SKILL_DAMAGE, 2)),
				n("Wrath", "분노", l(MANA_ON_HIT, 0.4F)), n("Sweeping Blows", "휩쓸기", l(SPLASH, 0.3F)),
				n("Warlord's Presence", "군주의 위엄", l(BOSS_DAMAGE, 12), l(SKILL_AREA, 10))));
		tree(JobClass.ROGUE,
			branch("assassination", "Assassination", "암살", 0x9B59D0,
				n("Keen Edge", "예리한 칼날", l(CRIT_CHANCE, 1.5F)), n("Vital Points", "급소", l(CRIT_DAMAGE, 4)),
				n("Ambush", "암습", l(BACKSTAB, 4)), n("Opening Strike", "선제 일격", l(FIRST_STRIKE, 5)),
				n("Shadow Strike", "그림자 일격", l(CRIT_DAMAGE, 25), l(EXECUTE, 10))),
			branch("venom", "Venom", "독술", 0x6AA84F,
				n("Toxic Blade", "독 묻은 칼날", l(POISON, 0.4F)), n("Quick Cuts", "재빠른 베기", l(BASIC_DAMAGE, 2)),
				n("Flurry", "연격", l(ATTACK_SPEED, 1.5F)), n("Twin Fangs", "쌍아", l(DOUBLE_STRIKE, 1.5F)),
				n("Master of Venom", "맹독의 대가", l(DOUBLE_STRIKE, 8), l(POISON, 2))),
			branch("shadow", "Shadow", "은신", 0x404058,
				n("Light Feet", "가벼운 발", l(MOVE_SPEED, 1.5F)), n("Evasion", "회피", l(DODGE, 1)),
				n("Vanish", "사라지기", l(STEALTH_ON_KILL, 0.4F)), n("Shadow Arts", "그림자 술법", l(COOLDOWN, 1.5F)),
				n("Shadow Legion", "그림자 군단", l(DODGE, 6), l(SPEED_ON_KILL, 3))));
		tree(JobClass.MAGE,
			branch("destruction", "Destruction", "파괴", 0xFF7A3A,
				n("Amplify", "증폭", l(SKILL_DAMAGE, 2.5F)), n("Ignite", "점화", l(BURN, 0.4F)),
				n("Expansion", "광역화", l(SKILL_AREA, 2)), n("Storm Call", "폭풍 소환", l(CHAIN_LIGHTNING, 1)),
				n("Grand Magic", "대마법", l(SKILL_DAMAGE, 12), l(CRIT_CHANCE, 5))),
			branch("arcane", "Arcane", "비전", 0x4A8CFF,
				n("Mana Well", "마나 샘", l(MAX_MANA, 6)), n("Flowing Mana", "흐르는 마나", l(MANA_REGEN, 0.2F)),
				n("Efficiency", "절약", l(MANA_COST, 1.5F)), n("Echo", "메아리", l(ECHO, 1)),
				n("Endless Mana", "무한의 마력", l(MANA_REGEN, 2), l(ECHO, 6))),
			branch("ward", "Ward", "결계", 0x9FE2FF,
				n("Barrier", "방벽", l(DAMAGE_REDUCTION, 1)), n("Mana Ward", "마나 보호막", l(MANA_SHIELD, 2)),
				n("Frost", "서리", l(SLOW, 0.4F)), n("Renewal", "재생", l(HEALTH_REGEN, 0.2F)),
				n("Absolute Barrier", "절대 결계", l(MANA_SHIELD, 10), l(DAMAGE_REDUCTION, 4))));
		tree(JobClass.ARCHER,
			branch("marksman", "Marksman", "저격", 0x58C04A,
				n("Heavy Draw", "강궁", l(SHOT_DAMAGE, 2.5F)), n("Long Shot", "원거리 사격", l(RANGE_BONUS, 0.6F)),
				n("Keen Eye", "예리한 눈", l(CRIT_CHANCE, 1.5F)), n("Headshot", "헤드샷", l(CRIT_DAMAGE, 4)),
				n("Eye That Never Misses", "필중의 눈", l(SHOT_DAMAGE, 12), l(BOSS_DAMAGE, 8))),
			branch("volley", "Volley", "속사", 0xC8E070,
				n("Quick Draw", "속사", l(DRAW_SPEED, 3)), n("Nimble Hands", "재빠른 손", l(ATTACK_SPEED, 1.5F)),
				n("Focus", "집중", l(SKILL_DAMAGE, 2)), n("Tactics", "전술", l(COOLDOWN, 1.5F)),
				n("Rain of Arrows", "화살비", l(EXTRA_SHOT, 1))),
			branch("survival", "Survival", "생존", 0x8A6A3A,
				n("Windstep", "바람의 발", l(MOVE_SPEED, 1.5F)), n("Evasive", "날렵함", l(DODGE, 1)),
				n("Field Dressing", "응급 처치", l(KILL_HEAL, 0.3F)), n("Trophy Hunter", "전리품 사냥꾼", l(ITEM_FIND, 2)),
				n("Hunter's Instinct", "사냥꾼의 본능", l(SPEED_ON_KILL, 3), l(KILL_HEAL, 2))));
		tree(JobClass.PIRATE,
			branch("plunder", "Plunder", "약탈", 0xE8B730,
				n("Gold Tooth", "금니", l(GOLD_FIND, 1)), n("Treasure Nose", "보물 냄새", l(ITEM_FIND, 2)),
				n("Grog", "그로그주", l(KILL_MANA, 0.4F)), n("Rum Ration", "럼주", l(KILL_HEAL, 0.3F)),
				n("Pirate King's Hoard", "해적왕의 보물", l(GOLD_FIND, 6), l(ITEM_FIND, 10))),
			branch("gunnery", "Gunnery", "포격", 0xA0A0A8,
				n("Gunpowder", "화약", l(SHOT_DAMAGE, 2.5F)), n("Quick Reload", "빠른 장전", l(DRAW_SPEED, 3)),
				n("Incendiary", "소이탄", l(BURN, 0.4F)), n("Grapeshot", "산탄", l(SKILL_AREA, 2)),
				n("Full Broadside", "일제 사격", l(EXTRA_SHOT, 1), l(SHOT_DAMAGE, 8))),
			branch("haki", "Haki", "패기", 0xA8282E,
				n("Armament", "무장색", l(BASIC_DAMAGE, 2)), n("Observation", "견문색", l(CRIT_CHANCE, 1.5F)),
				n("Iron Will", "강철 의지", l(DAMAGE_REDUCTION, 1)), n("Crackle", "전율", l(CHAIN_LIGHTNING, 1)),
				n("Conqueror's Might", "패왕의 기세", l(BOSS_DAMAGE, 12), l(CHAIN_LIGHTNING, 5))));
		tree(JobClass.SHINIGAMI,
			branch("zanjutsu", "Zanjutsu", "참술", 0x9FD8E8,
				n("Clean Cut", "깔끔한 베기", l(BASIC_DAMAGE, 2)), n("Swift Blade", "빠른 칼날", l(ATTACK_SPEED, 1.5F)),
				n("Precise Edge", "정밀한 칼날", l(CRIT_DAMAGE, 4)), n("Wide Arc", "넓은 참격", l(SPLASH, 0.3F)),
				n("Bankai Released", "만해 개방", l(BASIC_DAMAGE, 10), l(DOUBLE_STRIKE, 6))),
			branch("kido", "Kido", "귀도", 0x8A5AFF,
				n("Hado", "파도", l(SKILL_DAMAGE, 2.5F)), n("Incantation", "영창", l(COOLDOWN, 1.5F)),
				n("Reiryoku", "영력", l(MAX_MANA, 6)), n("Bakudo", "박도", l(SKILL_AREA, 2)),
				n("Hado #90", "파도 90번", l(SKILL_DAMAGE, 12), l(ECHO, 5))),
			branch("hoho", "Hoho", "보법", 0xE8E8F0,
				n("Light Step", "가벼운 걸음", l(MOVE_SPEED, 1.5F)), n("Afterimage", "잔상", l(DODGE, 1)),
				n("Quickened Mind", "가속된 정신", l(COOLDOWN_FLAT, 0.06F)), n("Pursuit", "추격", l(SPEED_ON_KILL, 0.4F)),
				n("Flash Step Mastery", "순보의 극의", l(DODGE, 6), l(MOVE_SPEED, 6))));
		tree(JobClass.HUNTER,
			branch("enhancer", "Enhancer", "강화계", 0xF5862B,
				n("Reinforced Fists", "강화된 주먹", l(BASIC_DAMAGE, 2)), n("Tough Body", "단련된 몸", l(MAX_HEALTH, 1)),
				n("Ko", "경", l(DOUBLE_STRIKE, 1.5F)), n("Ken", "견", l(DAMAGE_REDUCTION, 1)),
				n("Rock!", "바위!", l(BASIC_DAMAGE, 12), l(CRIT_DAMAGE, 20))),
			branch("emitter", "Emitter", "방출계", 0x5AA0FF,
				n("Nen Bullet", "념탄", l(SHOT_DAMAGE, 2.5F)), n("Release", "방출", l(SKILL_DAMAGE, 2)),
				n("Scatter", "확산", l(SKILL_AREA, 2)), n("Lightning Palm", "낙뢰", l(CHAIN_LIGHTNING, 1)),
				n("Nen Storm", "념탄 폭풍", l(SKILL_DAMAGE, 10), l(SHOT_DAMAGE, 10))),
			branch("manipulator", "Manipulator", "조작계", 0x8A2AC8,
				n("Gyo", "응", l(CRIT_CHANCE, 1.5F)), n("Thread Snare", "실 올가미", l(SLOW, 0.4F)),
				n("Zetsu", "절", l(MANA_REGEN, 0.2F)), n("Hatsu", "발", l(COOLDOWN, 1.5F)),
				n("Limitation and Vow", "제약과 서약", l(COOLDOWN, 6), l(CRIT_REFUND, 0.6F))));
	}

	private record Draft(String en, String ko, List<StatLine> lines) {
	}

	private static Draft n(final String en, final String ko, final StatLine... lines) {
		return new Draft(en, ko, List.of(lines));
	}

	private static StatLine l(final EngraveStat stat, final float value) {
		return StatLine.of(stat, value);
	}

	private record BranchDraft(String id, String en, String ko, int color, Draft[] nodes) {
	}

	private static BranchDraft branch(final String id, final String en, final String ko, final int color, final Draft... nodes) {
		return new BranchDraft(id, en, ko, color, nodes);
	}

	private static void tree(final JobClass job, final BranchDraft... drafts) {
		List<Branch> branches = new ArrayList<>();
		for (BranchDraft d : drafts) {
			List<Node> nodes = new ArrayList<>();
			for (int t = 0; t < d.nodes().length; t++) {
				Draft n = d.nodes()[t];
				String id = job.id() + "." + d.id() + "." + t;
				Node node = new Node(id, d.id(), t, n.en(), n.ko(), t == TIERS - 1 ? 1 : RANKS, n.lines());
				nodes.add(node);
				NODES.put(id, node);
			}
			branches.add(new Branch(d.id(), job, d.en(), d.ko(), d.color(), List.copyOf(nodes)));
		}
		TREES.put(job, List.copyOf(branches));
	}

	public static List<Branch> of(final JobClass job) {
		return TREES.getOrDefault(job, List.of());
	}

	public static @Nullable Node node(final String id) {
		return NODES.get(id);
	}

	public static List<Node> all() {
		return List.copyOf(NODES.values());
	}

	private TalentTree() {
	}
}
