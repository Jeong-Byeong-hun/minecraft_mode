package com.minecraftmode.client.datagen;

import com.minecraftmode.MinecraftMode;
import com.minecraftmode.consumable.BuffEffects;
import com.minecraftmode.consumable.ConsumableDef;
import com.minecraftmode.consumable.Consumables;
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
import com.minecraftmode.worldgen.lair.LairDef;
import com.minecraftmode.worldgen.lair.LairLoot;
import com.minecraftmode.worldgen.lair.LairPiece;
import com.minecraftmode.worldgen.lair.LairStructure;
import com.minecraftmode.worldgen.lair.NamedLairs;
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
 * bonuses, armor options), docs/MONSTERS.md (named monsters, their lairs, raid bosses and their
 * mechanics) and docs/CONSUMABLES.md in Korean, from the same definitions the game uses.
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

	private static final Map<LairDef.Shape, String> SHAPES = Map.of(LairDef.Shape.PYRAMID, "피라미드", LairDef.Shape.ZIGGURAT, "지구라트",
		LairDef.Shape.FORTRESS, "요새", LairDef.Shape.DOME, "돔", LairDef.Shape.SPIRE, "첨탑", LairDef.Shape.TREE, "거목", LairDef.Shape.NONE, "지하 미궁");
	private static final Map<ConsumableDef.Shop, String> SHOPS = Map.of(ConsumableDef.Shop.GENERAL, "잡화점", ConsumableDef.Shop.GROCER, "식료품점",
		ConsumableDef.Shop.ALCHEMIST, "연금술사");
	private static final Map<String, String> VANILLA_EFFECTS = Map.of("effect.minecraft.absorption", "흡수", "effect.minecraft.fire_resistance", "화염 저항",
		"effect.minecraft.haste", "성급함", "effect.minecraft.health_boost", "생명력 강화", "effect.minecraft.resistance", "저항", "effect.minecraft.speed", "신속",
		"effect.minecraft.regeneration", "재생", "effect.minecraft.strength", "힘", "effect.minecraft.night_vision", "야간 투시", "effect.minecraft.water_breathing", "수중 호흡");
	private static final Map<String, String> BIOMES = Map.ofEntries(
		Map.entry("plains", "평원"), Map.entry("sunflower_plains", "해바라기 평원"), Map.entry("meadow", "초원"), Map.entry("flower_forest", "꽃 숲"),
		Map.entry("forest", "숲"), Map.entry("birch_forest", "자작나무 숲"), Map.entry("savanna", "사바나"), Map.entry("savanna_plateau", "사바나 고원"),
		Map.entry("windswept_savanna", "바람이 부는 사바나"), Map.entry("taiga", "타이가"), Map.entry("windswept_hills", "바람이 부는 언덕"),
		Map.entry("snowy_plains", "눈 덮인 평원"), Map.entry("snowy_taiga", "눈 덮인 타이가"), Map.entry("grove", "산림"), Map.entry("snowy_slopes", "눈 덮인 비탈"),
		Map.entry("ice_spikes", "역고드름"), Map.entry("desert", "사막"), Map.entry("swamp", "늪"), Map.entry("mangrove_swamp", "맹그로브 늪"),
		Map.entry("dark_forest", "어두운 숲"), Map.entry("mushroom_fields", "버섯 들판"), Map.entry("pale_garden", "창백한 정원"), Map.entry("jungle", "정글"),
		Map.entry("sparse_jungle", "듬성듬성한 정글"), Map.entry("bamboo_jungle", "대나무 정글"), Map.entry("beach", "해변"), Map.entry("stony_shore", "돌 해안"),
		Map.entry("snowy_beach", "눈 덮인 해변"), Map.entry("badlands", "악지"), Map.entry("eroded_badlands", "침식된 악지"), Map.entry("wooded_badlands", "나무가 우거진 악지"),
		Map.entry("old_growth_pine_taiga", "원시 소나무 타이가"), Map.entry("old_growth_spruce_taiga", "원시 가문비나무 타이가"),
		Map.entry("old_growth_birch_forest", "원시 자작나무 숲"), Map.entry("stony_peaks", "돌 봉우리"), Map.entry("dripstone_caves", "점적석 동굴"),
		Map.entry("lush_caves", "무성한 동굴"), Map.entry("nether_wastes", "네더 황무지"), Map.entry("basalt_deltas", "현무암 삼각주"),
		Map.entry("soul_sand_valley", "영혼 모래 골짜기"), Map.entry("crimson_forest", "진홍빛 숲"), Map.entry("warped_forest", "뒤틀린 숲"), Map.entry("deep_dark", "딥 다크"),
		Map.entry("end_highlands", "엔드 고지대"), Map.entry("end_midlands", "엔드 중지대"), Map.entry("end_barrens", "엔드 불모지"));

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
		this.write("docs/CONSUMABLES.md", this.consumables());
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

		md.append("\n## 네임드 소굴 (").append(NamedLairs.all().size()).append("곳)\n\n");
		md.append("- 네임드마다 그 몬스터를 닮은 거대한 인공 구조물이 서식지 바이옴에 생깁니다(사막의 피라미드처럼). 한 칸(약 ")
			.append(NamedLairs.SPACING * 16).append("블록)마다 많아야 하나, 마을과 수도 근처에는 생기지 않습니다. 찾기: `/locate structure minecraft_mode:lair_<네임드 id>`\n");
		md.append("- 안은 3칸 폭 통로의 **미로**이고 남쪽 벽 가운데에 입구가 있습니다. **보물 상자 위치는 소굴마다 다릅니다**: 입구 바로 옆(20%), 가장 깊은 곳(35%), 먼 막다른 길(45%). 상자 옆에는 그 네임드가 지키고 있고, 다른 막다른 길에 작은 보급 통이 최대 3개 있습니다.\n");
		md.append("- 보물 상자: 동전(소굴 최고 레벨 구간 무기값의 50–100%) **항상** + 보상 1개 **항상**(60% 확률로 1개 더, 그중 25%로 또 1개). 보상은 소모품 ")
			.append(Math.round((1 - LairLoot.GEAR_SHARE) * 100)).append("% / 그 네임드 레벨 범위의 장비 ").append(Math.round(LairLoot.GEAR_SHARE * 100))
			.append("%. 진화의 에테르와 정수도 들어 있습니다.\n");
		md.append("- 소굴 구역(구조물 둘레 ").append(LairPiece.GROUNDS).append("블록 포함)에서는 일반 몬스터와 함께 그 네임드가 평소의 2배 비율(스폰의 6–20%)로 나타나고, 네임드끼리 거리 제한도 96 → 20블록으로 줄어듭니다. 들어서면 소굴 이름이 화면에 뜹니다.\n\n");
		md.append("| 소굴 | 네임드 | 레벨 | 모양 | 미로 | 위치 | 바이옴 |\n|---|---|---|---|---|---|---|\n");
		for (LairDef lair : NamedLairs.all()) {
			NamedDef named = lair.named();
			String where = switch (lair.setting()) {
				case SURFACE -> "지상";
				case UNDERGROUND -> "지하 Y " + lair.depth() + " (지상에 등대 탑)";
				case NETHER -> "네더 Y " + LairStructure.NETHER_FLOOR;
				case END -> "엔드 섬";
			};
			md.append("| **").append(lair.ko()).append("** | ").append(named.ko()).append(" | ").append(named.lo()).append("–").append(named.hi()).append(" | ")
				.append(SHAPES.get(lair.shape())).append(" | ").append(lair.size()).append("×").append(lair.size()).append(" | ").append(where).append(" | ")
				.append(lair.biomes().stream().map(b -> BIOMES.getOrDefault(b.identifier().getPath(), b.identifier().getPath())).collect(Collectors.joining(", ")))
				.append(" |\n");
		}

		md.append("\n## 레이드 보스 (").append(RaidBosses.all().size()).append("종)\n\n");
		md.append("- 도시 왕성 앞 \"토벌 사령관 알드릭\"에게서 입장합니다. 파티장이 보스를 고르면 ").append((int)Raids.GATHER_RANGE)
			.append("블록 안의 파티원(최대 ").append(Parties.MAX_SIZE).append("명)이 전용 차원의 경기장으로 이동하고, ").append(Raids.COUNTDOWN_TICKS / 20)
			.append("초 뒤 보스가 나타납니다. 모든 파티원이 보스 레벨(드롭 범위의 최저 레벨) 이상이어야 합니다.\n");
		md.append("- 체력은 1인 기준 × (1 + ").append(ClassDocProvider.num(RaidBoss.PARTY_SCALE)).append(" × (인원 - 1)). 페이즈마다 제목과 함께 패턴이 추가되고, 모든 광역기는 바닥 경고가 먼저 나옵니다.\n");
		md.append("- 입장료: 파티원 각자 보스 레벨 구간의 무기값만큼 동전을 냅니다(지갑에서). 레이드에서 죽어도 경험치와 아이템을 잃지 않고 입장한 곳으로 돌아갑니다(그 전투에는 복귀 불가). 전원 이탈 시 실패.\n");
		md.append("- **즉사 기믹**: 보스마다 1–3개. 정해진 체력 이하가 되면 처음 발동하고 이후 일정 간격으로 반복되며, 기믹과 기믹 사이는 최소 45초입니다. 실패한 사람은 즉사급 피해(방어·저항 무시)를 받고, 일부 협동 기믹은 실패하면 전멸합니다. 협동 기믹의 필요 인원은 파티 규모에 맞춰 줄어들어 혼자서도 깰 수 있습니다.\n");
		md.append("- 처치 보상: 참가자마다 진화의 에테르 5–8개 · 응축된 정수 2–4개 · 직업 경험치. 장비 3개(+2명마다 1개)는 **경매**(기본) 또는 **주사위**로 나눕니다.\n");
		md.append("  경매: 시작가 = 구간 상점가 × 1.5, 입찰 단위 ≈ 시작가의 5%, ").append(LootSessions.AUCTION_TICKS / 20).append("초(마지막 ")
			.append(LootSessions.SNIPE_TICKS / 20).append("초 입찰 시 연장), 낙찰금은 나머지 참가자에게 분배, 입찰이 없으면 주사위. 주사위: ")
			.append(LootSessions.DICE_TICKS / 20).append("초, 1–100, 동점은 재굴림, 전원 포기 시 파티장.\n\n");
		md.append("| 보스 | 칭호 | 레벨 | 1인 체력 | 공격력 | 입장료 | 무대 | 페이즈 |\n|---|---|---|---|---|---|---|---|\n");
		for (BossDef def : RaidBosses.all()) {
			StringBuilder phases = new StringBuilder("1");
			for (int i = 0; i < def.phases().size(); i++) {
				phases.append(" → ").append(i + 2).append(" (").append(Math.round(def.phases().get(i) * 100)).append("%)");
			}
			md.append("| **").append(def.ko()).append("** | ").append(def.epithetKo()).append(" | ").append(def.lo()).append("–").append(def.hi()).append(" | ")
				.append(ClassDocProvider.num(def.health())).append(" | ").append(ClassDocProvider.num(def.damage())).append(" | ").append(Coins.format(def.fee())).append(" | ")
				.append(this.ko.getOrDefault("screen.minecraft_mode.raid.arena." + def.arena().name().toLowerCase(Locale.ROOT), def.arena().name())).append(" | ")
				.append(phases).append(" |\n");
		}
		for (BossDef def : RaidBosses.all()) {
			md.append("\n### ").append(def.ko()).append(" — ").append(def.epithetKo()).append("\n\n").append(def.descKo()).append("\n\n");
			for (int phase = 2; phase <= def.phaseCount(); phase++) {
				md.append("- ").append(phase).append(" 페이즈 (체력 ").append(Math.round(def.phases().get(phase - 2) * 100)).append("% 이하): ")
					.append(this.ko.getOrDefault(def.nameKey() + ".phase" + phase, "")).append("\n");
			}
			for (String[] mechanic : RaidLang.MECHANICS) {
				if (mechanic[0].startsWith(def.id() + "_")) {
					md.append("- 기믹 **").append(mechanic[2]).append("**: ").append(mechanic[4]).append("\n");
				}
			}
		}
		return md.toString();
	}

	// ------------------------------------------------------------------ CONSUMABLES.md

	private String consumables() {
		StringBuilder md = new StringBuilder();
		md.append("# 소모품 (").append(Consumables.all().size()).append("종)\n\n");
		md.append("> `./gradlew runDatagen`이 코드 정의에서 생성합니다(`GearDocProvider`). 직접 고치지 마세요.\n\n");
		md.append("- 지속 효과는 모두 **10분**입니다. 효과 수치는 직업 장비 옵션과 같은 방식으로 더해집니다.\n");
		md.append("- 같은 그룹(회복약·마나약 등)은 재사용 대기시간을 공유합니다.\n");
		md.append("- 좋은 것일수록 구하기 어렵습니다: 1–2등급은 조합·상점, 3등급은 연금술사(금화)·네임드·소굴, 4등급은 비싼 상점가나 드물게 네임드·소굴·레이드, 5등급은 레이드 보스에게서만 나옵니다.\n\n");
		md.append("| 이름 | 등급 | 효과 | 재사용 | 구하는 곳 |\n|---|---|---|---|---|\n");
		for (ConsumableDef def : Consumables.all()) {
			List<String> effects = new java.util.ArrayList<>();
			if (def.heal() > 0) {
				effects.add("체력 " + Math.round(def.heal() * 100) + "% 회복");
			}
			if (def.mana() > 0) {
				effects.add("MP " + Math.round(def.mana() * 100) + "% 회복");
			}
			if (def.cleanse()) {
				effects.add("해로운 효과 제거");
			}
			for (ConsumableDef.Buff buff : def.effects()) {
				String name = this.ko.getOrDefault(buff.effect().value().getDescriptionId(),
					VANILLA_EFFECTS.getOrDefault(buff.effect().value().getDescriptionId(), buff.effect().value().getDescriptionId()));
				String stats = BuffEffects.lines(buff.effect(), buff.amplifier()).stream().map(this::stat).collect(Collectors.joining(", "));
				effects.add(name + " " + (buff.amplifier() + 1) + " (" + buff.ticks() / 1200 + "분" + (stats.isEmpty() ? "" : ": " + stats) + ")");
			}
			if (def.special() != ConsumableDef.Special.NONE) {
				effects.add(this.ko.getOrDefault("consumable.minecraft_mode." + def.special().name().toLowerCase(Locale.ROOT), def.special().name()));
			}
			List<String> sources = new java.util.ArrayList<>();
			if (def.shop() != ConsumableDef.Shop.NONE) {
				sources.add(SHOPS.get(def.shop()) + " " + Coins.format(def.price()));
			}
			if (ModRecipeProvider.COOKED.contains(def.id())) {
				sources.add("조합");
			}
			if (def.tier() <= 4) {
				sources.add(def.tier() == 4 ? "네임드·소굴(드묾)" : "네임드·소굴");
			}
			if (def.tier() >= 3) {
				sources.add("레이드");
			}
			md.append("| **").append(def.ko()).append("** | ").append("★".repeat(def.tier())).append(" | ").append(String.join(" · ", effects)).append(" | ")
				.append(def.cooldown() > 0 ? def.cooldown() + "초" : "-").append(" | ").append(String.join(", ", sources)).append(" |\n");
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
