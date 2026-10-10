package com.minecraftmode.job;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import java.util.Arrays;
import java.util.List;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

/**
 * The seven classes (plus NONE for players who have not advanced yet). Each class advances through
 * four tiers with its own title and passive; the texts live here so datagen and the job screen
 * read the same table. Ids are saved in player data, so never rename them.
 */
public enum JobClass implements StringRepresentable {
	NONE("none", 0xB8B8B8, "Adventurer", "모험가", new Tier[0]),
	WARRIOR("warrior", 0xE0533D, "Warrior", "전사", new Tier[] {
		new Tier("Warrior", "전사", "Iron Body", "강철 육체", "Max health +4.", "최대 체력 +4."),
		new Tier("Gladiator", "검투사", "Arena Instinct", "투기장 본능", "Melee damage +10%.", "근접 피해 +10%."),
		new Tier("Berserker", "버서커", "Berserk Blood", "광전사의 피",
			"Below 40% health: damage +25% and 5% lifesteal.", "체력 40% 미만일 때 피해 +25%, 흡혈 5%."),
		new Tier("King of Knights", "기사왕", "Avalon", "아발론",
			"Once every 3 minutes, survive a lethal blow and recover 30% health.", "3분마다 한 번, 치명상을 버티고 체력 30%를 회복합니다.")
	}),
	ROGUE("rogue", 0x9B59D0, "Rogue", "도적", new Tier[] {
		new Tier("Thief", "도적", "Light Feet", "가벼운 발놀림", "Movement speed +10%.", "이동 속도 +10%."),
		new Tier("Ninja", "닌자", "Vital Strike", "급소 찌르기", "15% chance to land a critical hit (+50% damage).", "15% 확률로 치명타(피해 +50%)."),
		new Tier("Assassin", "어쌔신", "Assassination Arts", "암살술", "Damage from behind +30%.", "뒤에서 공격하면 피해 +30%."),
		new Tier("Shadow Monarch", "그림자 군주", "Shadow Veil", "그림자 장막", "15% chance to dodge an attack.", "15% 확률로 공격을 회피합니다.")
	}),
	MAGE("mage", 0x4A8CFF, "Mage", "법사", new Tier[] {
		new Tier("Mage", "마법사", "Mana Affinity", "마나 친화", "Max MP +30.", "최대 MP +30."),
		new Tier("Caster", "캐스터", "Mana Circulation", "마나 순환", "MP regeneration x2.", "MP 회복 속도 2배."),
		new Tier("Archmage", "대마도사", "Arcane Mastery", "비전 숙련", "Skill damage +15%.", "스킬 피해 +15%."),
		new Tier("Grand Caster", "그랜드 캐스터", "Akashic Record", "아카식 레코드", "Skill cooldowns -20%.", "스킬 재사용 대기시간 -20%.")
	}),
	ARCHER("archer", 0x58C04A, "Archer", "궁수", new Tier[] {
		new Tier("Bowman", "궁수", "Eagle Eye", "매의 눈", "Basic shot damage +15%.", "기본 사격 피해 +15%."),
		new Tier("Ranger", "레인저", "Wind Walker", "바람 걸음", "Movement speed +8%.", "이동 속도 +8%."),
		new Tier("Heroic Archer", "아처", "Deadeye", "필중", "+2% damage per 4 blocks of distance (max +40%).", "거리 4블록마다 피해 +2% (최대 +40%)."),
		new Tier("King of Heroes", "영웅왕", "Gate of Babylon", "왕의 재보",
			"Basic shots have a 20% chance to launch 2 treasure blades.", "기본 사격 시 20% 확률로 보구 2자루를 함께 발사합니다.")
	}),
	PIRATE("pirate", 0xE8B730, "Pirate", "해적", new Tier[] {
		new Tier("Pirate", "해적", "Plunderer", "약탈 본능", "Hostile kills have a 15% chance to drop coins.", "적대적 몹 처치 시 15% 확률로 동전을 떨어뜨립니다."),
		new Tier("Captain", "선장", "Sea Legs", "바다 사나이", "In water: Dolphin's Grace and Water Breathing.", "물속에서 돌고래의 우아함과 수중 호흡."),
		new Tier("Emperor of the Sea", "사황", "Conqueror's Haki", "패왕색 패기", "10% chance on hit to stun the enemy for 1.5s.", "공격 시 10% 확률로 1.5초 기절."),
		new Tier("Pirate King", "해적왕", "The One Piece", "원피스", "Shop prices -20% and Luck +2.", "상점 가격 -20%, 행운 +2.")
	}),
	SHINIGAMI("shinigami", 0x9FD8E8, "Soul Reaper", "사신", new Tier[] {
		new Tier("Soul Reaper", "사신", "Flash Step", "순보", "Movement speed +8% and 5% dodge chance.", "이동 속도 +8%, 회피 확률 5%."),
		new Tier("Lieutenant", "부대장", "Shikai", "시해", "Skill damage +12%.", "스킬 피해 +12%."),
		new Tier("Captain", "대장", "Bankai", "만해", "Basic attack damage +15% and attack speed +10%.", "기본 공격 피해 +15%, 공격 속도 +10%."),
		new Tier("Captain-Commander", "총대장", "Mugetsu", "무월",
			"Skill cooldowns -15% and +15% damage to bosses and named monsters.", "스킬 재사용 대기시간 -15%, 보스·네임드에게 주는 피해 +15%.")
	}),
	HUNTER("hunter", 0xF5862B, "Hunter", "헌터", new Tier[] {
		new Tier("Hunter", "헌터", "Ten", "전", "Damage taken -5% and max health +2.", "받는 피해 -5%, 최대 체력 +2."),
		new Tier("Nen User", "념능력자", "Gyo", "응", "Critical chance +10%.", "치명타 확률 +10%."),
		new Tier("Pro Hunter", "프로 헌터", "Ko", "경", "15% chance for basic attacks to hit twice.", "15% 확률로 기본 공격이 두 번 적중합니다."),
		new Tier("Triple-Star Hunter", "트리플 헌터", "Limitation and Vow", "제약과 서약",
			"Skill damage +20% and +2 MP per second.", "스킬 피해 +20%, 초당 MP 회복 +2.")
	});

	public static final Codec<JobClass> CODEC = StringRepresentable.fromEnum(JobClass::values);
	public static final StreamCodec<ByteBuf, JobClass> STREAM_CODEC = ByteBufCodecs.idMapper(i -> values()[i], JobClass::ordinal);
	public static final List<JobClass> PLAYABLE = Arrays.stream(values()).filter(c -> c != NONE).toList();

	private final String id;
	private final int color;
	private final String en;
	private final String ko;
	private final Tier[] tiers;

	JobClass(final String id, final int color, final String en, final String ko, final Tier[] tiers) {
		this.id = id;
		this.color = color;
		this.en = en;
		this.ko = ko;
		this.tiers = tiers;
	}

	public String id() {
		return this.id;
	}

	@Override
	public String getSerializedName() {
		return this.id;
	}

	/** RGB used for names, HUD bars and default skill effects. */
	public int color() {
		return this.color;
	}

	public String en() {
		return this.en;
	}

	public String ko() {
		return this.ko;
	}

	/** Tier 1..4. */
	public Tier tier(final int tier) {
		return this.tiers[tier - 1];
	}

	public int tierCount() {
		return this.tiers.length;
	}

	public String nameKey() {
		return "job.minecraft_mode." + this.id;
	}

	/** Max health each tier adds for the melee classes (the front line takes the hits); 0 for the ranged ones. */
	public int vitalityHealth() {
		return switch (this) {
			case WARRIOR -> 4;
			case PIRATE, SHINIGAMI, HUNTER -> 3;
			case ROGUE -> 2;
			default -> 0;
		};
	}

	/** Armor each tier adds for the melee classes; 0 for the ranged ones. */
	public double vitalityArmor() {
		return switch (this) {
			case WARRIOR -> 2.0;
			case PIRATE, SHINIGAMI, HUNTER -> 1.5;
			case ROGUE -> 1.0;
			default -> 0.0;
		};
	}

	/** Title for tier 1..4; tier 0 uses {@link #nameKey()}. */
	public String tierKey(final int tier) {
		return tier <= 0 ? this.nameKey() : this.nameKey() + ".tier" + tier;
	}

	public String passiveKey(final int tier) {
		return this.nameKey() + ".passive" + tier;
	}

	public String passiveDescKey(final int tier) {
		return this.passiveKey(tier) + ".desc";
	}

	public static JobClass byId(final String id) {
		for (JobClass job : values()) {
			if (job.id.equals(id)) {
				return job;
			}
		}
		return NONE;
	}

	/** Title and passive of one tier, in English and Korean. */
	public record Tier(String en, String ko, String passiveEn, String passiveKo, String passiveDescEn, String passiveDescKo) {
	}
}
