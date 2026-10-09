package com.minecraftmode.client.datagen;

import com.minecraftmode.MinecraftMode;
import com.minecraftmode.bounty.Bounties;
import com.minecraftmode.consumable.BuffEffects;
import com.minecraftmode.consumable.ConsumableDef;
import com.minecraftmode.consumable.Consumables;
import com.minecraftmode.enhance.Enhancement;
import com.minecraftmode.entity.boss.RaidBoss;
import com.minecraftmode.entity.named.Ability;
import com.minecraftmode.entity.named.Habitat;
import com.minecraftmode.entity.named.NamedDef;
import com.minecraftmode.entity.named.NamedMob;
import com.minecraftmode.entity.named.NamedMobs;
import com.minecraftmode.job.JobClass;
import com.minecraftmode.job.JobProgression;
import com.minecraftmode.job.engrave.EngraveStat;
import com.minecraftmode.job.gear.ArmorOptions;
import com.minecraftmode.job.gear.ArmorPieceDef;
import com.minecraftmode.job.gear.ArmorSetDef;
import com.minecraftmode.job.gear.ClassArmor;
import com.minecraftmode.job.gear.ClassGear;
import com.minecraftmode.job.gear.GearSlot;
import com.minecraftmode.job.gear.ItemLevels;
import com.minecraftmode.job.gear.StatLine;
import com.minecraftmode.job.weapon.JobWeapons;
import com.minecraftmode.loot.Coins;
import com.minecraftmode.loot.GearIndex;
import com.minecraftmode.loot.GearShop;
import com.minecraftmode.loot.GearUpgrades;
import com.minecraftmode.market.AuctionService;
import com.minecraftmode.progress.Achievements;
import com.minecraftmode.progress.CollectionBonuses;
import com.minecraftmode.progress.Progress;
import com.minecraftmode.progress.ResetCycle;
import com.minecraftmode.raid.BossDef;
import com.minecraftmode.raid.Parties;
import com.minecraftmode.raid.RaidAffix;
import com.minecraftmode.raid.RaidBosses;
import com.minecraftmode.raid.RaidDifficulty;
import com.minecraftmode.raid.RaidRecordsData;
import com.minecraftmode.raid.Raids;
import com.minecraftmode.raid.loot.LootSessions;
import com.minecraftmode.talent.TalentTree;
import com.minecraftmode.talent.Talents;
import com.minecraftmode.worldgen.lair.LairChestBlockEntity;
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
		this.write("docs/ENDGAME.md", this.endgame());
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

	// ------------------------------------------------------------------ ENDGAME.md

	private String ko(final String key) {
		return this.ko.getOrDefault(key, key).replace("%%", "%");
	}

	private String endgame() {
		StringBuilder md = new StringBuilder();
		md.append("# 엔드게임 (반복 콘텐츠와 성장)\n\n");
		md.append("> 이 문서는 `./gradlew runDatagen`이 코드 정의에서 생성합니다(`GearDocProvider`). 직접 고치지 마세요. 설계 배경: `docs/DESIGN-endgame.md`.\n\n");

		md.append("## 초기화 주기\n\n");
		md.append("- 마인크래프트 날짜 기준: 하루 = 오버월드 시계 ").append(ResetCycle.DAY_TICKS).append("틱, **주기 = ").append(ResetCycle.DAYS)
			.append("일**. 잠을 자서 아침이 와도 날짜가 넘어갑니다.\n");
		md.append("- 주기마다: 소굴 개인 보상과 소굴의 군주, 레이드 보상 귀속, 주기 의뢰, 레이드 변형. 하루마다: 일일 의뢰.\n\n");

		md.append("## 소굴 — 개인 보상과 소굴의 군주\n\n");
		md.append("- 보물 상자와 보급품은 **개인 상자**입니다. 플레이어마다 내용물이 따로 굴려지고, 주기마다 다시 채워집니다. 가져가지 않은 물건은 그 주기 동안 남아 있습니다.\n");
		md.append("- 주기마다 처음으로 플레이어가 보물 상자 ").append((int)LairChestBlockEntity.WAKE_RANGE)
			.append("블록 안에 오면(또는 상자를 열면) **소굴의 군주**(그 네임드의 최고 레벨 강화판)가 깨어납니다: 체력 ×").append(ClassDocProvider.num(NamedMob.LORD_HEALTH))
			.append(", 공격 ×").append(ClassDocProvider.num(NamedMob.LORD_DAMAGE)).append(", 크기 ×").append(ClassDocProvider.num(NamedMob.LORD_SCALE)).append(", 보스 바, ").append(NamedMob.WRATH_INTERVAL / 20)
			.append("초마다 바닥 경고 뒤 **소굴의 분노**(반경 5블록, 최대 체력의 40%).\n");
		md.append("- 보물 상자는 그 주기에 군주를 쓰러뜨릴 때 ").append((int)LairChestBlockEntity.CREDIT_RANGE)
			.append("블록 안에 있던 사람(그리고 마지막 일격을 넣은 사람)에게만 열립니다. 아직 못 잡은 사람이 오면 그 사람을 위한 군주가 다시 깨어납니다. 군주는 강화석 1–2개, 10% 확률로 보호 주문서를 떨어뜨립니다.\n");
		md.append("- 보물 상자를 그 주기에 처음 열면 소굴 정복으로 기록됩니다(도감·업적·의뢰). 보물 상자에 강화석 1–2개, 보급품에 25% 확률로 강화석.\n");
		md.append("- **소굴 지도**: 사용하면 가장 가까운 네임드 소굴을 표시한 지도가 됩니다(잡화점 5S, 공적 상점).\n\n");

		md.append("## 레이드 난이도\n\n| 난이도 | 보스 체력 | 보스 피해 | 입장료 | 추가 분배 | 에테르 | 강화석 | 보호 주문서 | 장비 강화 |\n|---|---|---|---|---|---|---|---|---|\n");
		for (RaidDifficulty d : RaidDifficulty.values()) {
			md.append("| ").append(this.ko(d.nameKey())).append(" | ×").append(ClassDocProvider.num(d.health)).append(" | ×").append(ClassDocProvider.num(d.damage))
				.append(" | ×").append(ClassDocProvider.num(d.fee)).append(" | +").append(d.extraLots).append(" | ×").append(ClassDocProvider.num(d.ether))
				.append(" | ").append(d.stones).append(" | ").append(Math.round(d.scrollChance * 100)).append("% | ")
				.append(d.enhanceMax == 0 ? "-" : "+" + d.enhanceMin + "~+" + d.enhanceMax).append(" |\n");
		}
		md.append("\n- 영웅은 그 보스의 일반, 악몽은 영웅을 클리어해야 열립니다(파티원 모두).\n");
		md.append("- **보상 귀속**: 보스·난이도마다 주기에 한 번. 이미 받은 사람은 입장료 없이 **연습**으로 함께 들어가고 보상과 분배에서 빠집니다.\n");
		md.append("- **클리어 기록**: 보스·난이도별 최단 시간 상위 ").append(RaidRecordsData.KEEP).append(" 파티를 월드에 저장합니다(토벌 사령관 창의 기록 보기).\n\n");
		md.append("### 변형 (영웅·악몽, 주기마다 ").append(RaidAffix.PER_CYCLE).append("개)\n\n| 변형 | 효과 |\n|---|---|\n");
		for (RaidAffix a : RaidAffix.values()) {
			md.append("| ").append(this.ko(a.nameKey())).append(" | ").append(this.ko(a.descKey())).append(" |\n");
		}

		md.append("\n## 모험가 길드 의뢰\n\n");
		md.append("- 길드 접수원 리나(모험가 길드)에게서 **일일 의뢰 ").append(Bounties.DAILY).append("개**(하루마다)와 **주기 의뢰 1개**(").append(ResetCycle.DAYS)
			.append("일마다). 처치·채굴·소굴·레이드는 자동으로 집계되고, 납품은 보고할 때 인벤토리에서 가져갑니다.\n");
		md.append("- 종류: 적대 몬스터 처치, 특정 몬스터 처치, 네임드 처치, 소굴 정복, 광석 채굴, 레이드 클리어(Lv 20+ 주기 의뢰), 물품 납품.\n\n");
		md.append("| 레벨 | 일일 보상 | 주기 보상 |\n|---|---|---|\n");
		for (int level : new int[] {10, 30, 50, 70, 100}) {
			Bounties.Reward daily = Bounties.reward(false, level);
			Bounties.Reward special = Bounties.reward(true, level);
			md.append("| ").append(level).append(" | ").append(Coins.format(daily.coins())).append(", 공적 ").append(daily.merit()).append(", 에테르 ").append(daily.ether())
				.append(", 강화석 ").append(daily.stones()).append(" | ").append(Coins.format(special.coins())).append(", 공적 ").append(special.merit()).append(", 에테르 ")
				.append(special.ether()).append(", 강화석 ").append(special.stones()).append(" |\n");
		}
		md.append("\n### 공적 상점\n\n| 물품 | 공적 |\n|---|---|\n");
		for (Bounties.Offer offer : Bounties.SHOP) {
			md.append("| ").append(this.ko(offer.nameKey())).append(" | ").append(offer.cost()).append(" |\n");
		}

		md.append("\n## 장비 강화 (+1 ~ +").append(Enhancement.MAX).append(")\n\n");
		md.append("- 수도 대장간의 **강화 장인 브로크**. 직업 무기와 직업 방어구, 진화해도 강화 단계는 유지됩니다.\n");
		md.append("- 실패하면 **장인의 기운** +").append(Enhancement.PITY_STEP).append("%(다음 시도 확률에 더해짐, 성공하면 초기화). +")
			.append(Enhancement.RISKY_FROM).append("부터는 실패하면 한 단계 하락 — **보호 강화**는 하락을 막을 때만 보호 주문서 1장을 씁니다.\n");
		md.append("- 비용: 동전 = 장비 구간 가격 × (0.1 + 0.05 × 목표 단계), 정수(+5까지 정수, 이후 응축된 정수), +6부터 강화석. +10 이상 성공은 서버 전체에 알립니다.\n\n");
		md.append("| 단계 | 성공률 | 정수 | 강화석 | 실패 시 | 무기 (누적) | 방어구 (누적) |\n|---|---|---|---|---|---|---|\n");
		ClassGear weapon = ClassGear.of(JobWeapons.of(JobClass.WARRIOR).getFirst());
		ClassGear armor = ClassGear.of(ClassArmor.pieces().iterator().next());
		for (int t = 1; t <= Enhancement.MAX; t++) {
			String weaponLines = Enhancement.lines(weapon, t).stream().map(this::stat).collect(Collectors.joining(", "));
			String armorLines = Enhancement.lines(armor, t).stream().map(this::stat).collect(Collectors.joining(", "));
			md.append("| +").append(t).append(" | ").append(Enhancement.baseRate(t)).append("% | ").append(Enhancement.condensed(t) ? "응축 " : "")
				.append(Enhancement.essence(t)).append(" | ").append(Enhancement.stones(t)).append(" | ").append(Enhancement.risky(t) ? "1단계 하락" : "유지")
				.append(" | ").append(weaponLines).append(" | ").append(armorLines).append(" |\n");
		}

		md.append("\n## 거래소\n\n");
		md.append("- 시장 노점의 **중개인 모건**. 즉시 구매가로 사고팝니다. 한 사람당 ").append(AuctionService.MAX_LISTINGS).append("건, 등록 기간 ")
			.append(AuctionService.DURATION / ResetCycle.DAY_TICKS).append("일.\n");
		md.append("- 등록 수수료 ").append(AuctionService.LIST_FEE_PERCENT).append("%(등록할 때), 판매 수수료 ").append(AuctionService.SALE_FEE_PERCENT)
			.append("%(대금에서). 판매 대금과 기간이 끝났거나 취소한 물건은 **우편함**으로 가고 중개인에게서 받습니다. 코인은 팔 수 없습니다.\n");
		md.append("- 구매 탭: 검색, 분류(장비·소모품·재료), 정렬(가격·마감), 쪽 넘기기 — 서버가 찾아서 한 쪽(").append(AuctionService.PAGE_SIZE)
			.append("건)씩만 보냅니다. 판매 탭에서 물건을 고르면 지금 최저가가 자동으로 들어갑니다.\n");
		md.append("- 물건이 든 셜커 상자·꾸러미, 데이터가 ").append(AuctionService.MAX_ITEM_BYTES / 1024).append("KB를 넘는 물건(긴 책 등)은 등록할 수 없습니다.\n\n");

		md.append("## 업적 (").append(Achievements.all().size()).append("개)\n\n| 업적 | 조건 | 공적 | 칭호 |\n|---|---|---|---|\n");
		for (Achievements.Achievement a : Achievements.all()) {
			md.append("| ").append(a.ko()).append(" | ").append(a.descKo()).append(" | ").append(a.merit()).append(" | ").append(a.hasTitle() ? a.titleKo() : "").append(" |\n");
		}
		md.append("\n- 칭호는 도감(J)의 칭호 탭에서 착용하며 이름 앞에 붙습니다(머리 위·채팅·탭 목록).\n");
		md.append("- 처치 기록(도감·업적·처치 의뢰)은 마지막 일격을 넣은 사람과 ").append((int)Progress.SHARE_RANGE).append("블록 안의 같은 파티원 모두에게 올라갑니다.\n");
		md.append("- **수집 보너스**(영구): 네임드 종마다 ").append(CollectionBonuses.KILLS).append("마리 처치하면 보스 피해 +0.5%, 정복한 소굴 종류마다 아이템 발견 +0.5%, 업적 ")
			.append(CollectionBonuses.ACHIEVEMENT_STEP).append("개마다 경험치 +1%.\n\n");

		md.append("## 특성\n\n");
		md.append("- 특성 창: N (직업 창 K에도 버튼). 포인트 = (레벨 - 10) × 2 ÷ 3 (Lv 100에 ").append(Talents.points(JobProgression.MAX_LEVEL)).append("점).\n");
		md.append("- 직업마다 3계열 × ").append(TalentTree.TIERS).append("단계. 1–4단계는 최대 ").append(TalentTree.RANKS).append("랭크, 5단계는 1랭크 핵심 특성. 다음 단계는 그 계열에 ")
			.append(TalentTree.PER_TIER).append("점씩 넣어야 열립니다. 초기화는 동전(레벨 구간 가격). 직업별 특성표는 `docs/CLASSES.md`.\n");
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
