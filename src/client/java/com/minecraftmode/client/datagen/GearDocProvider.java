package com.minecraftmode.client.datagen;

import com.minecraftmode.MinecraftMode;
import com.minecraftmode.entity.boss.RaidBoss;
import com.minecraftmode.entity.named.Ability;
import com.minecraftmode.entity.named.Habitat;
import com.minecraftmode.entity.named.NamedDef;
import com.minecraftmode.entity.named.NamedMobs;
import com.minecraftmode.job.JobClass;
import com.minecraftmode.job.engrave.EngraveStat;
import com.minecraftmode.job.gear.ArmorOptions;
import com.minecraftmode.job.gear.ArmorPieceDef;
import com.minecraftmode.job.gear.ArmorSetDef;
import com.minecraftmode.job.gear.ClassArmor;
import com.minecraftmode.job.gear.ClassGear;
import com.minecraftmode.job.gear.GearSlot;
import com.minecraftmode.job.gear.ItemLevels;
import com.minecraftmode.job.gear.StatLine;
import com.minecraftmode.loot.Coins;
import com.minecraftmode.loot.GearIndex;
import com.minecraftmode.loot.GearShop;
import com.minecraftmode.loot.GearUpgrades;
import com.minecraftmode.raid.BossDef;
import com.minecraftmode.raid.Parties;
import com.minecraftmode.raid.RaidBosses;
import com.minecraftmode.raid.Raids;
import com.minecraftmode.raid.loot.LootSessions;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricLanguageProvider.TranslationBuilder;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;

/**
 * Writes docs/GEAR.md (shop per bracket, drop rates, every armor set with its pieces and set
 * bonuses, armor options) and docs/MONSTERS.md (named monsters and raid bosses) in Korean, from the
 * same definitions the game uses.
 */
public class GearDocProvider implements DataProvider {
	private static final Map<Habitat, String> HABITATS = Map.ofEntries(
		Map.entry(Habitat.ANY_OVERWORLD, "오버월드 어디나"), Map.entry(Habitat.GRASSLAND, "평원·숲"), Map.entry(Habitat.DRYLAND, "사바나·평원·언덕"),
		Map.entry(Habitat.SWAMP, "늪"), Map.entry(Habitat.SNOW, "설원·설산"), Map.entry(Habitat.DESERT, "사막·악지"), Map.entry(Habitat.CAVES, "동굴(Y 40 이하)"),
		Map.entry(Habitat.GLOOM, "어두운 숲·버섯 들판·창백한 정원"), Map.entry(Habitat.JUNGLE, "정글"), Map.entry(Habitat.COAST, "해안"),
		Map.entry(Habitat.BADLANDS, "악지"), Map.entry(Habitat.TAIGA, "타이가"), Map.entry(Habitat.DEEP_CAVES, "점적석·무성한 동굴"),
		Map.entry(Habitat.OLD_FOREST, "오래된 숲"), Map.entry(Habitat.NETHER_WASTES, "네더 황무지·현무암 삼각주"), Map.entry(Habitat.SOUL_VALLEY, "영혼 모래 골짜기"),
		Map.entry(Habitat.CRIMSON, "진홍빛 숲"), Map.entry(Habitat.WARPED, "뒤틀린 숲"), Map.entry(Habitat.DEEP_DARK, "딥 다크"),
		Map.entry(Habitat.END_HIGHLANDS, "엔드 고지대"), Map.entry(Habitat.END_ISLANDS, "엔드 외곽 섬"));
	private static final Map<Ability.Type, String> ABILITIES = Map.ofEntries(
		Map.entry(Ability.Type.LEAP, "도약 강타"), Map.entry(Ability.Type.CHARGE, "돌진"), Map.entry(Ability.Type.BOLT, "투사체"), Map.entry(Ability.Type.SUMMON, "소환"),
		Map.entry(Ability.Type.TELEPORT, "순간이동"), Map.entry(Ability.Type.AURA, "오라"), Map.entry(Ability.Type.ROAR, "포효"), Map.entry(Ability.Type.SPIKES, "가시 분출"),
		Map.entry(Ability.Type.HEAL, "재생"), Map.entry(Ability.Type.BREATH, "브레스"), Map.entry(Ability.Type.STEALTH, "은신 기습"), Map.entry(Ability.Type.SLAM, "내려찍기"));

	private final FabricPackOutput output;
	private final Map<String, String> ko = new HashMap<>();

	public GearDocProvider(final FabricPackOutput output) {
		this.output = output;
	}

	@Override
	public CompletableFuture<?> run(final CachedOutput cache) {
		JobLang.add(new TranslationBuilder() {
			@Override
			public boolean has(final String key) {
				return GearDocProvider.this.ko.containsKey(key);
			}

			@Override
			public String overwrite(final String key, final String value) {
				return GearDocProvider.this.ko.put(key, value);
			}
		}, true);
		this.write("docs/GEAR.md", this.gear());
		this.write("docs/MONSTERS.md", this.monsters());
		return CompletableFuture.completedFuture(null);
	}

	// ------------------------------------------------------------------ GEAR.md

	private String gear() {
		StringBuilder md = new StringBuilder();
		md.append("# 장비 (직업 무기 · 직업 방어구)\n\n");
		md.append("> `./gradlew runDatagen`이 코드 정의에서 생성합니다(`GearDocProvider`). 직접 고치지 마세요. 설계 의도는 [DESIGN-gear-raids.md](DESIGN-gear-raids.md).\n\n");
		md.append("- 모든 직업 장비는 요구 레벨이 있고 10레벨 단위 **구간**에 속합니다. 무기 ").append(GearIndex.list().stream().filter(ClassGear::isWeapon).count())
			.append("종, 방어구 ").append(ClassArmor.pieces().size()).append("부위(").append(ClassArmor.sets().size()).append("세트).\n");
		md.append("- 모험가 길드는 직업마다 구간당 **무기 1개 + 방어구 1부위**만 팝니다(자기 레벨 구간 + 10까지). 나머지는 네임드·보스 드롭 전용입니다.\n");
		md.append("- 방어구는 해당 직업만, 차수·레벨이 맞아야 입을 수 있습니다. 각인은 무기 3줄, 방어구 4줄.\n");
		md.append("- 강화: 도시 대장장이 \"명장 볼룬드\"가 장비를 같은 직업·종류의 다음 단계로 바꿔 줍니다(목표 구간 등급의 진화의 에테르 ")
			.append(GearUpgrades.ETHER_COST).append("개).\n\n");

		md.append("## 드롭 확률 (네임드 1마리당)\n\n| 레벨 | 장비 드롭 | 진화의 에테르 |\n|---|---|---|\n");
		for (int level = 10; level <= 100; level += 10) {
			md.append("| ").append(level).append(" | ").append(ClassDocProvider.num(ItemLevels.dropChance(level) * 100)).append("% | 반드시 ")
				.append(1 + level / 30).append("개 |\n");
		}
		md.append("\n장비 드롭 확률 스탯(해적 레벨 보너스, 일부 방어구 옵션)은 이 확률에 곱해집니다. 드롭의 60%는 처치자 직업 장비입니다.\n\n");

		md.append("## 길드 판매 목록\n\n| 직업 | 구간 | 무기 | 방어구 | 가격(무기 / 방어구) |\n|---|---|---|---|---|\n");
		for (JobClass job : JobClass.PLAYABLE) {
			for (int bracket = ItemLevels.MIN_BRACKET; bracket <= ItemLevels.MAX_BRACKET; bracket += 10) {
				List<ClassGear> shop = GearIndex.shopItems(job, bracket);
				ClassGear weapon = shop.stream().filter(ClassGear::isWeapon).findFirst().orElse(null);
				ClassGear armor = shop.stream().filter(g -> !g.isWeapon()).findFirst().orElse(null);
				md.append("| ").append(this.ko.get(job.nameKey())).append(" | Lv ").append(bracket).append(" | ")
					.append(weapon == null ? "-" : this.name(weapon) + " (Lv " + weapon.level() + ")").append(" | ")
					.append(armor == null ? "-" : this.name(armor) + " (Lv " + armor.level() + ")").append(" | ")
					.append(weapon == null ? "-" : Coins.format(GearShop.price(weapon))).append(" / ").append(armor == null ? "-" : Coins.format(GearShop.price(armor)))
					.append(" |\n");
			}
		}

		md.append("\n## 방어구 기본 옵션과 추가 옵션\n\n");
		md.append("- 부위마다 고정 기본 옵션 1줄 + 무작위 추가 옵션(Lv 30 미만 1줄, Lv 60 미만 2줄, 그 이상 3줄). 추가 옵션 수치는 레벨 최대치의 60–100%.\n\n");
		md.append("| 부위 | 기본 옵션 (Lv 50 기준) | 추가 옵션 풀 (Lv 100 최대치) |\n|---|---|---|\n");
		ArmorSetDef sample = ClassArmor.sets().stream().filter(s -> s.level() == 50).findFirst().orElseThrow();
		for (GearSlot slot : GearSlot.ARMOR) {
			ArmorPieceDef piece = ClassArmor.piecesOf(sample).stream().filter(p -> p.slot() == slot).findFirst().orElseThrow();
			String pool = ArmorOptions.pool(slot).entrySet().stream().map(e -> this.stat(e.getKey(), ArmorOptions.max(e.getValue(), 100)))
				.collect(Collectors.joining(", "));
			md.append("| ").append(slotName(slot)).append(" | ").append(this.stat(piece.baseOption())).append(" | ").append(pool).append(" |\n");
		}

		for (JobClass job : JobClass.PLAYABLE) {
			md.append("\n## ").append(this.ko.get(job.nameKey())).append(" 방어구 세트\n\n");
			md.append("| Lv | 세트 | 부위 | 2세트 | 3세트 | 4세트 |\n|---|---|---|---|---|---|\n");
			for (ArmorSetDef set : ClassArmor.sets(job)) {
				String pieces = ClassArmor.piecesOf(set).stream().map(p -> this.ko.getOrDefault(p.nameKey(), p.id())).collect(Collectors.joining(", "));
				md.append("| ").append(set.level()).append(" | **").append(set.ko()).append("** | ").append(pieces);
				for (ArmorSetDef.SetBonus bonus : set.bonuses()) {
					md.append(" | ").append(bonus.lines().stream().map(this::stat).collect(Collectors.joining(", ")));
				}
				md.append(" |\n");
			}
		}
		return md.toString();
	}

	// ------------------------------------------------------------------ MONSTERS.md

	private String monsters() {
		StringBuilder md = new StringBuilder();
		md.append("# 네임드 몬스터와 레이드 보스\n\n");
		md.append("> `./gradlew runDatagen`이 코드 정의에서 생성합니다(`GearDocProvider`). 직접 고치지 마세요.\n\n");
		md.append("## 네임드 몬스터 (").append(NamedMobs.all().size()).append("종)\n\n");
		md.append("- 서식지의 일반 몬스터 대신 드물게 나타납니다(출현율 = 그 서식지 몬스터 스폰 중 비율). 반경 96블록에 하나, 도시 안에는 없음.\n");
		md.append("- 처치 시 경험치·정수와 진화의 에테르(반드시), 레벨 범위의 드롭 전용 장비(확률)를 줍니다. 체력·공격력은 레벨 범위 안에서 최대 +30%.\n\n");
		md.append("| 이름 | 레벨 | 서식지 | 출현율 | 체력 | 공격력 | 패턴 | 설명 |\n|---|---|---|---|---|---|---|---|\n");
		for (NamedDef def : NamedMobs.all()) {
			String abilities = def.abilities().stream().map(a -> ABILITIES.getOrDefault(a.type(), a.type().name())).collect(Collectors.joining(", "));
			md.append("| **").append(def.ko()).append("** | ").append(def.lo()).append("–").append(def.hi()).append(" | ").append(HABITATS.getOrDefault(def.habitat(), def.habitat().name()))
				.append(def.daylight() ? " (낮에도)" : "").append(" | ").append(ClassDocProvider.num(def.rarity() * 100)).append("% | ").append(ClassDocProvider.num(def.health()))
				.append(" | ").append(ClassDocProvider.num(def.damage())).append(" | ").append(abilities).append(" | ").append(def.descKo()).append(" |\n");
		}

		md.append("\n## 레이드 보스 (").append(RaidBosses.all().size()).append("종)\n\n");
		md.append("- 도시 왕성 앞 \"토벌 사령관 알드릭\"에게서 입장합니다. 파티장이 보스를 고르면 ").append((int)Raids.GATHER_RANGE)
			.append("블록 안의 파티원(최대 ").append(Parties.MAX_SIZE).append("명)이 전용 차원의 경기장으로 이동하고, ").append(Raids.COUNTDOWN_TICKS / 20)
			.append("초 뒤 보스가 나타납니다. 모든 파티원이 보스 레벨(드롭 범위의 최저 레벨) 이상이어야 합니다.\n");
		md.append("- 체력은 1인 기준 × (1 + ").append(ClassDocProvider.num(RaidBoss.PARTY_SCALE)).append(" × (인원 - 1)). 페이즈마다 제목과 함께 패턴이 추가되고, 모든 광역기는 바닥 경고가 먼저 나옵니다.\n");
		md.append("- 레이드에서 죽으면 아이템을 잃지 않고 입장한 곳으로 돌아갑니다(그 전투에는 복귀 불가). 전원 이탈 시 실패.\n");
		md.append("- 처치 보상: 참가자마다 진화의 에테르 5–8개 · 응축된 정수 2–4개 · 직업 경험치. 장비 3개(+2명마다 1개)는 **경매**(기본) 또는 **주사위**로 나눕니다.\n");
		md.append("  경매: 시작가 = 구간 상점가 × 1.5, 입찰 단위 ≈ 시작가의 5%, ").append(LootSessions.AUCTION_TICKS / 20).append("초(마지막 ")
			.append(LootSessions.SNIPE_TICKS / 20).append("초 입찰 시 연장), 낙찰금은 나머지 참가자에게 분배, 입찰이 없으면 주사위. 주사위: ")
			.append(LootSessions.DICE_TICKS / 20).append("초, 1–100, 동점은 재굴림, 전원 포기 시 파티장.\n\n");
		md.append("| 보스 | 칭호 | 레벨 | 1인 체력 | 공격력 | 무대 | 페이즈 |\n|---|---|---|---|---|---|---|\n");
		for (BossDef def : RaidBosses.all()) {
			StringBuilder phases = new StringBuilder("1");
			for (int i = 0; i < def.phases().size(); i++) {
				phases.append(" → ").append(i + 2).append(" (").append(Math.round(def.phases().get(i) * 100)).append("%)");
			}
			md.append("| **").append(def.ko()).append("** | ").append(def.epithetKo()).append(" | ").append(def.lo()).append("–").append(def.hi()).append(" | ")
				.append(ClassDocProvider.num(def.health())).append(" | ").append(ClassDocProvider.num(def.damage())).append(" | ")
				.append(this.ko.getOrDefault("screen.minecraft_mode.raid.arena." + def.arena().name().toLowerCase(Locale.ROOT), def.arena().name())).append(" | ")
				.append(phases).append(" |\n");
		}
		for (BossDef def : RaidBosses.all()) {
			md.append("\n### ").append(def.ko()).append(" — ").append(def.epithetKo()).append("\n\n").append(def.descKo()).append("\n\n");
			for (int phase = 2; phase <= def.phaseCount(); phase++) {
				md.append("- ").append(phase).append(" 페이즈 (체력 ").append(Math.round(def.phases().get(phase - 2) * 100)).append("% 이하): ")
					.append(this.ko.getOrDefault(def.nameKey() + ".phase" + phase, "")).append("\n");
			}
		}
		return md.toString();
	}

	// ------------------------------------------------------------------ helpers

	private String name(final ClassGear gear) {
		return this.ko.getOrDefault("item.minecraft_mode." + gear.id(), gear.id());
	}

	private String stat(final StatLine line) {
		return this.stat(line.stat(), line.value());
	}

	private String stat(final EngraveStat stat, final float value) {
		return String.format(Locale.ROOT, stat.ko(), ClassDocProvider.num(value)).replace("%%", "%");
	}

	private static String slotName(final GearSlot slot) {
		return switch (slot) {
			case HEAD -> "투구";
			case CHEST -> "갑옷";
			case LEGS -> "각반";
			case FEET -> "장화";
			case WEAPON -> "무기";
		};
	}

	private void write(final String name, final String content) {
		try {
			Path root = this.output.getOutputFolder().getParent().getParent().getParent();
			Path file = root.resolve(name);
			Files.createDirectories(file.getParent());
			Files.writeString(file, content, StandardCharsets.UTF_8);
		} catch (IOException | NullPointerException e) {
			MinecraftMode.LOGGER.warn("Could not write {}", name, e);
		}
	}

	@Override
	public String getName() {
		return "Gear and monster docs";
	}
}
