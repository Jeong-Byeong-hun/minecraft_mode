package com.minecraftmode.client.datagen;

import com.minecraftmode.MinecraftMode;
import com.minecraftmode.bag.Bags;
import com.minecraftmode.bounty.Bounties;
import com.minecraftmode.city.CityFixtures;
import com.minecraftmode.city.DailyBread;
import com.minecraftmode.city.StarterKit;
import com.minecraftmode.city.Homestead;
import com.minecraftmode.city.TrainingGrounds;
import com.minecraftmode.client.hud.TargetHealthHud;
import com.minecraftmode.client.map.MapSettings;
import com.minecraftmode.companion.Companions;
import com.minecraftmode.consumable.BuffEffects;
import com.minecraftmode.consumable.ConsumableDef;
import com.minecraftmode.consumable.Consumables;
import com.minecraftmode.craft.CraftRecipes;
import com.minecraftmode.craft.Profession;
import com.minecraftmode.dungeon.DungeonAffix;
import com.minecraftmode.dungeon.DungeonDef;
import com.minecraftmode.dungeon.DungeonLayout;
import com.minecraftmode.dungeon.Dungeons;
import com.minecraftmode.dungeon.Keystone;
import com.minecraftmode.economy.Buyback;
import com.minecraftmode.economy.ShopOffers;
import com.minecraftmode.enhance.EnhanceMenu;
import com.minecraftmode.enhance.Enhancement;
import com.minecraftmode.entity.boss.RaidBoss;
import com.minecraftmode.entity.named.Ability;
import com.minecraftmode.entity.named.Habitat;
import com.minecraftmode.entity.named.NamedDef;
import com.minecraftmode.entity.named.NamedMob;
import com.minecraftmode.entity.named.NamedMobs;
import com.minecraftmode.event.WorldEvents;
import com.minecraftmode.job.JobClass;
import com.minecraftmode.job.JobEvents;
import com.minecraftmode.job.JobProgression;
import com.minecraftmode.job.Paragon;
import com.minecraftmode.job.engrave.EngraveStat;
import com.minecraftmode.job.gear.ArmorOptions;
import com.minecraftmode.job.gear.ArmorPieceDef;
import com.minecraftmode.job.gear.ArmorSetDef;
import com.minecraftmode.job.gear.ClassArmor;
import com.minecraftmode.job.gear.ClassDefense;
import com.minecraftmode.job.gear.ClassGear;
import com.minecraftmode.job.gear.GearSlot;
import com.minecraftmode.job.gear.ItemLevels;
import com.minecraftmode.job.gear.StatLine;
import com.minecraftmode.job.quest.TrialHunts;
import com.minecraftmode.job.weapon.JobWeapons;
import com.minecraftmode.loot.Coins;
import com.minecraftmode.loot.EvolutionEtherItem;
import com.minecraftmode.loot.GearIndex;
import com.minecraftmode.loot.GearShop;
import com.minecraftmode.loot.GearUpgrades;
import com.minecraftmode.market.AuctionService;
import com.minecraftmode.progress.Achievements;
import com.minecraftmode.progress.Codex;
import com.minecraftmode.progress.CollectionBonuses;
import com.minecraftmode.progress.Contribution;
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
import com.minecraftmode.story.Story;
import com.minecraftmode.talent.TalentTree;
import com.minecraftmode.talent.Talents;
import com.minecraftmode.worldgen.lair.LairChestBlockEntity;
import com.minecraftmode.worldgen.lair.LairDef;
import com.minecraftmode.worldgen.lair.LairExp;
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
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Writes docs/GEAR.md (shop per bracket, drop rates, every armor set with its pieces and set
 * bonuses, armor options), docs/MONSTERS.md (named monsters, their lairs, raid bosses and their
 * mechanics), docs/CONSUMABLES.md, docs/ENDGAME.md and docs/CONTENT.md (paragon, awakening, professions, pets and mounts,
 * dungeons, world events, the main story) in Korean, from the same definitions the game uses.
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
		this.write("docs/CONTENT.md", this.content());
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
		md.append("- 길드는 **모든 직업 장비**(자기 직업 포함)를 길드 가격의 ").append(Math.round(GearShop.BUYBACK_SHARE * 100))
			.append("%에 사들입니다. 상점을 열 때 인벤토리에 있는 종류마다 판매 줄이 붙고(입고 있는 방어구 제외), 강화·각인·옵션은 값에 반영되지 않습니다.\n");
		md.append("- 방어구는 해당 직업만, 차수·레벨이 맞아야 입을 수 있습니다. 각인은 무기 3줄, 방어구 4줄.\n");
		md.append("- 강화: 도시 대장장이 \"명장 볼룬드\"가 장비를 같은 직업·종류의 다음 단계로 바꿔 줍니다(목표 구간 등급의 진화의 에테르 + 목표 구간 길드 가격의 절반). 강화를 이어받을 때는 **재담금 수수료**가 붙습니다: 그 +N을 목표 구간에서 올렸다면 더 들었을 동전의 절반(낮은 구간에서 싸게 +15를 만들어 올리는 우회 방지).\n");
		md.append("- 진화 에테르 비용(목표 구간):");
		for (int bracket = ItemLevels.MIN_BRACKET; bracket <= ItemLevels.MAX_BRACKET; bracket += 10) {
			md.append(bracket == ItemLevels.MIN_BRACKET ? " " : " · ").append("Lv").append(bracket).append(' ').append(GearUpgrades.etherCost(bracket)).append("개");
		}
		md.append(".\n- 에테르 합치기·나누기: 손에 들고 우클릭하면 같은 등급 ").append(EvolutionEtherItem.FUSE).append("개가 한 단계 위 1개로, 웅크리고 우클릭하면 1개가 한 단계 아래 ")
			.append(EvolutionEtherItem.FUSE).append("개로 바뀝니다(손실 없음).\n\n");

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

		md.append("\n## 직업 방어구 방어 성능\n\n");
		md.append("- 한 벌(4부위) 기준. 방어 점수 = (").append(exact(ClassDefense.ARMOR_BASE)).append(" + 레벨 × ")
			.append(exact(ClassDefense.ARMOR_PER_LEVEL)).append(") × 직업 배율, 방어 강도 = 레벨 ÷ 50 × ").append(exact(ClassDefense.TOUGHNESS_PER_50))
			.append(" × 직업 배율, **물리 보호**(모든 피해 감소, 보호 인챈트 역할) = min(").append(exact(ClassDefense.PROTECTION_MAX)).append("%, 레벨 × ")
			.append(exact(ClassDefense.PROTECTION_PER_LEVEL)).append("%) × 직업 배율, **마법 방어**(마법 공격에 추가 감소) = min(")
			.append(exact(ClassDefense.MAGIC_MAX)).append("%, 레벨 × ").append(exact(ClassDefense.MAGIC_PER_LEVEL)).append("%) × 마법 배율. 부위마다 1/4씩.\n");
		md.append("- 마법 공격: 네임드의 브레스·가시 분출·마력탄(칼·총알·돌·금 파편 같은 물체는 물리), 바닐라 마법(물약·소환사 송곳니·가디언 광선·위더·드래곤 브레스·워든 음파·화염구). 마법 공격에도 방어 점수는 적용됩니다.\n");
		md.append("- 바닐라 비교: 철 15 / 0, 다이아 20 / 8, 다이아 + 보호 IV 20 / 8 / 피해 -64%. 직업 방어구는 바닐라 인챈트를 받지 않습니다.\n\n");
		md.append("| 직업 | 방어 배율 | 마법 배율 | 고유 |\n|---|---|---|---|\n");
		for (JobClass job : JobClass.PLAYABLE) {
			ClassDefense d = ClassDefense.of(job);
			String own = d.manaShield() > 0 ? "마나 보호막(받는 피해의 " + exact(d.manaShield()) + "%를 MP로)"
				: d.setDodge(0) > 0 || d.dodgePerLevel() > 0 ? "회피 " + exact(d.dodgeBase()) + "% + 레벨 × " + exact(d.dodgePerLevel()) + "%" : "-";
			md.append("| ").append(this.ko.get(job.nameKey())).append(" | ").append(exact(d.armor())).append(" | ").append(exact(d.magic()))
				.append(" | ").append(own).append(" |\n");
		}
		md.append("\n| Lv | ");
		for (JobClass job : JobClass.PLAYABLE) {
			md.append(this.ko.get(job.nameKey())).append(" | ");
		}
		md.append("\n|---|").append("---|".repeat(JobClass.PLAYABLE.size())).append("\n");
		for (int level : new int[] {10, 30, 50, 70, 80, 100}) {
			md.append("| ").append(level).append(" | ");
			for (JobClass job : JobClass.PLAYABLE) {
				final int lv = level;
				ArmorSetDef set = ClassArmor.sets(job).stream().filter(s -> s.level() <= lv).max(java.util.Comparator.comparingInt(ArmorSetDef::level)).orElse(null);
				if (set == null) {
					md.append("- | ");
					continue;
				}
				int armor = 0;
				float toughness = 0.0F;
				for (ArmorPieceDef piece : ClassArmor.piecesOf(set)) {
					armor += piece.armor();
					toughness += piece.toughness();
				}
				ClassDefense d = ClassDefense.of(job);
				md.append(Math.min(30, armor)).append(" / ").append(exact(toughness)).append(" / ").append(exact(d.setProtection(set.level())))
					.append("% / 마법 ").append(exact(d.setMagicDefense(set.level()))).append("%");
				if (d.setDodge(set.level()) > 0) {
					md.append(" / 회피 ").append(exact(d.setDodge(set.level()))).append("%");
				}
				md.append(" | ");
			}
			md.append("\n");
		}
		md.append("\n(방어 점수 / 방어 강도 / 물리 보호 / 마법 방어 / 회피, 그 레벨까지 입을 수 있는 가장 높은 세트)\n");

		md.append("\n## 방어구 기본 옵션과 추가 옵션\n\n");
		md.append("- 부위마다 고정 기본 옵션 1줄 + 무작위 추가 옵션(Lv 30 미만 1줄, Lv 60 미만 2줄, 그 이상 3줄). 추가 옵션 수치는 레벨 최대치의 60–100%.\n");
		md.append("- 대장장이의 **추가 옵션** 탭에서 다시 굴립니다: 응축된 정수 ").append(GearUpgrades.REROLL_CONDENSED)
			.append("개(정수 9개 = 1개) + 구간 길드 가격의 절반. 새 옵션은 기존 옵션 옆에 보관되고, ✔(적용) 또는 ✖(기존 유지)를 고를 때까지 기존 옵션이 그대로 적용됩니다.\n\n");
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
			.append("초마다 바닥 경고 뒤 **소굴의 분노**(반경 5블록, 최대 체력의 40%). 상자에서 ").append((int)NamedMob.LORD_LEASH)
			.append("블록 넘게 끌려 나가면 추격을 멈추고 상자 옆으로 돌아갑니다.\n");
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
			.append(Enhancement.RISKY_FROM).append("부터는 실패하면 한 단계 하락 — **보호 강화**는 하락을 막을 때만 보호 주문서 1장을 씁니다. 주문서가 없으면 장비 구간 등급 이상의 진화의 에테르 ")
			.append(EnhanceMenu.PROTECTION_ETHER).append("개를 대신 씁니다(낮은 등급부터).\n");
		md.append("- +").append(Enhancement.GLOW_FROM).append("부터 장비가 빛납니다: 광택, 슬롯 테두리(+").append(Enhancement.GLOW_FROM).append(" 파랑, +")
			.append(Enhancement.GLOW_BRIGHT).append(" 보라, +").append(Enhancement.MAX).append(" 금색, 각성 붉은 금색과 ✦), 든 무기 주변 파티클, 직업 방어구 4부위가 모두 +")
			.append(Enhancement.GLOW_FROM).append(" 이상이면 발밑의 고리. `/enhanceeffects false`로 파티클만 끕니다.\n");
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
		md.append("## 상점 재구매\n\n- 상점에 판 물건은 최근 ").append(Buyback.LIMIT).append("건까지(강화·각인 그대로) 어느 상점에서든 판 값의 ")
			.append(Buyback.MARKUP).append("배로 되살 수 있습니다. 상점 창 오른쪽의 **재구매** 칸을 누르세요. 같은 물건을 연달아 팔면 한 줄로 합쳐지고, 재구매는 시세에 영향을 주지 않습니다.\n\n");

		md.append("## 업적 (").append(Achievements.all().size()).append("개)\n\n| 업적 | 조건 | 공적 | 칭호 |\n|---|---|---|---|\n");
		for (Achievements.Achievement a : Achievements.all()) {
			md.append("| ").append(a.ko()).append(" | ").append(a.descKo()).append(" | ").append(a.merit()).append(" | ").append(a.hasTitle() ? a.titleKo() : "").append(" |\n");
		}
		md.append("\n- 칭호는 도감(J)의 칭호 탭에서 착용하며 이름 앞에 붙습니다(머리 위·채팅·탭 목록).\n");
		Map<Codex.Category, List<Item>> codexItems = Codex.items();
		md.append("- 도감(J) 탭: **네임드**(네임드 ").append(NamedMobs.all().size()).append("종 숙련, 소굴 ").append(NamedLairs.all().size())
			.append("곳의 발견·정복), **몬스터**(일반 몬스터 ").append(Codex.monsters().size()).append("종의 처치 수와 ")
			.append(Codex.MILESTONES[0]).append("/").append(Codex.MILESTONES[1]).append("/").append(Codex.MILESTONES[2])
			.append("회 테두리, 레이드 보스·던전 보스·거신), **아이템**(무기 ").append(codexItems.get(Codex.Category.WEAPON).size()).append(" · 방어구 ")
			.append(codexItems.get(Codex.Category.ARMOR).size()).append(" · 소모품 ").append(codexItems.get(Codex.Category.CONSUMABLE).size()).append(" · 재료·기타 ")
			.append(codexItems.get(Codex.Category.MATERIAL).size()).append("의 획득 기록, 직업·레벨 필터와 검색, 얻는 곳), 업적, 칭호, 스토리. 몬스터·아이템 기록은 능력치를 주지 않습니다.\n");
		md.append("- 처치 기록(도감·업적·처치 의뢰)은 처치에 기여한 사람과 그 ").append((int)Progress.SHARE_RANGE).append("블록 안의 같은 파티원 모두에게 올라갑니다.\n");
		md.append("\n## 처치 기여도\n\n");
		md.append("- 몬스터가 실제로 잃은 체력을 때린 사람별로 기록합니다(방어구·흡수·넘친 피해 제외, 길들인 소환수의 피해는 주인 몫). 마지막 공격 뒤 ")
			.append(Contribution.FORGET_TICKS / 20).append("초가 지나면 그 사람의 기여는 잊힙니다.\n");
		md.append("- **지원도 기여입니다**: 다른 플레이어를 치유한 양(넘친 치유 제외)은 그 사람이 최근 ").append(Contribution.COMBAT_TICKS / 20)
			.append("초 안에 싸운 몬스터들에게 피해로 나눠 계산합니다. 아군 강화를 걸어 주면 그 사람이 효과 동안 넣는 피해의 ").append(Math.round(Contribution.BUFF_SHARE * 100))
			.append("%, 적에게 약화를 걸면 다른 사람이 그 적에게 넣는 피해의 ").append(Math.round(Contribution.DEBUFF_SHARE * 100))
			.append("%가 내 기여로 더해집니다(때린 사람의 몫은 줄지 않고 함께 나눕니다).\n");
		md.append("- 전체 기여의 ").append(Math.round(Contribution.MIN_SHARE * 100)).append("% 이상인 사람만 몫을 받습니다(1위는 항상). 막타가 아니어도 됩니다.\n");
		md.append("- **직업 경험치**: 혼자 잡을 때의 경험치 × (1 + ").append(Math.round(Contribution.GROUP_BONUS * 100)).append("% × (기여자 수 - 1), 최대 ")
			.append(Contribution.MAX_BONUS_MEMBERS).append("명분)을 기여 비율대로 나눕니다. 예: 둘이 6:4로 잡으면 각자 혼자 잡을 때의 72%·48%.\n");
		md.append("- **최다 기여자(1위)**: 정수·동전 발견(해적·약탈) 판정, 네임드 장비 판정(1위의 직업 기준, 여럿이면 1위 인벤토리로 바로), 펫·탈것 드롭 판정.\n");
		md.append("- **네임드 동전**: 여럿이 잡으면 바닥에 떨어지지 않고 경험치와 같은 비율로 각자의 지갑에. 1위가 아닌 기여자도 ")
			.append(Math.round(Contribution.ASSIST_SHARE * 100)).append("% 이상이면 진화의 에테르를 하나 받습니다.\n");
		md.append("- 기여자 모두: 전직 시험 처치 목표와 시험 토큰 판정(원거리 처치 조건은 막타만), 펫 성장. 처치 시 회복·MP 같은 각인 효과는 막타 친 사람만.\n");
		md.append("- 독·불 같은 지속 피해로 죽어도 직전에 플레이어가 때렸다면 똑같이 나눕니다.\n");
		md.append("- **수집 보너스**(영구): 네임드 종마다 ").append(CollectionBonuses.KILLS).append("마리 처치하면 보스 피해 +0.5%, 정복한 소굴 종류마다 아이템 발견 +0.5%, 업적 ")
			.append(CollectionBonuses.ACHIEVEMENT_STEP).append("개마다 경험치 +1%.\n\n");

		md.append("## 특성\n\n");
		md.append("- 특성 창: N (직업 창 K에도 버튼). 포인트 = (레벨 - 10) × 2 ÷ 3 (Lv 100에 ").append(Talents.points(JobProgression.MAX_LEVEL)).append("점).\n");
		md.append("- 직업마다 3계열 × ").append(TalentTree.TIERS).append("단계. 1–4단계는 최대 ").append(TalentTree.RANKS).append("랭크, 5단계는 1랭크 핵심 특성. 다음 단계는 그 계열에 ")
			.append(TalentTree.PER_TIER).append("점씩 넣어야 열립니다. 초기화는 동전(레벨 구간 가격). 직업별 특성표는 `docs/CLASSES.md`.\n");
		return md.toString();
	}

	// ------------------------------------------------------------------ CONTENT.md

	private static final String[][] VANILLA_KO = {
		{"item.minecraft.dried_kelp", "말린 켈프"}, {"item.minecraft.wheat", "밀"}, {"item.minecraft.carrot", "당근"}, {"item.minecraft.sweet_berries", "달콤한 열매"},
		{"item.minecraft.baked_potato", "구운 감자"}, {"item.minecraft.sugar", "설탕"}, {"item.minecraft.cod", "생대구"}, {"item.minecraft.cooked_salmon", "익힌 연어"},
		{"item.minecraft.chicken", "생닭고기"}, {"item.minecraft.bowl", "그릇"}, {"item.minecraft.cooked_beef", "스테이크"}, {"item.minecraft.bone", "뼈"},
		{"item.minecraft.paper", "종이"}, {"item.minecraft.bread", "빵"}, {"block.minecraft.red_mushroom", "빨간 버섯"}, {"item.minecraft.golden_carrot", "황금 당근"},
		{"item.minecraft.glass_bottle", "유리병"}, {"item.minecraft.spider_eye", "거미 눈"}, {"item.minecraft.honey_bottle", "꿀이 든 병"},
		{"item.minecraft.iron_ingot", "철 주괴"}, {"item.minecraft.cocoa_beans", "코코아 콩"}, {"item.minecraft.gold_ingot", "금 주괴"},
		{"item.minecraft.lapis_lazuli", "청금석"}, {"item.minecraft.ender_pearl", "엔더 진주"}, {"item.minecraft.compass", "나침반"}, {"item.minecraft.diamond", "다이아몬드"},
		{"item.minecraft_mode.mythril_ingot", "미스릴 주괴"}, {"item.minecraft_mode.raw_mythril", "미스릴 원석"}, {"item.minecraft_mode.mythril_nugget", "미스릴 조각"},
		{"entity.minecraft.pillager", "약탈자"}, {"entity.minecraft.vindicator", "변명자"}, {"entity.minecraft.zombie", "좀비"}, {"entity.minecraft.spider", "거미"},
		{"entity.minecraft.cave_spider", "동굴 거미"}, {"entity.minecraft.witch", "마녀"}, {"entity.minecraft.drowned", "드라운드"},
		{"entity.minecraft.skeleton", "스켈레톤"}, {"entity.minecraft.stray", "스트레이"}, {"entity.minecraft.blaze", "블레이즈"},
		{"entity.minecraft.magma_cube", "마그마 큐브"}, {"entity.minecraft.wither_skeleton", "위더 스켈레톤"}, {"entity.minecraft.phantom", "팬텀"},
		{"entity.minecraft.bogged", "보그드"}, {"entity.minecraft.zombie_villager", "좀비 주민"}, {"entity.minecraft.evoker", "소환사"}
	};

	private String itemName(final Item item) {
		return this.ko.getOrDefault(item.getDescriptionId(), BuiltInRegistries.ITEM.getKey(item).getPath());
	}

	private String content() {
		for (String[] e : VANILLA_KO) {
			this.ko.putIfAbsent(e[0], e[1]);
		}
		StringBuilder md = new StringBuilder();
		md.append("# 성장 이후 콘텐츠 (초월·각성·생활 기술·펫과 탈것·던전·월드 이벤트·메인 스토리)\n\n");
		md.append("> 이 문서는 `./gradlew runDatagen`이 코드 정의에서 생성합니다(`GearDocProvider`). 직접 고치지 마세요.\n\n");

		md.append("## 광장의 안내원·보급관·제빵사와 수련장\n\n");
		md.append("- **안내원 넬라**(광장, 시작 지점 왼쪽 앞): 우클릭하면 주제별 안내 창 — 처음 시작, 빠른 성장(레벨 10까지 필요한 경험치와 남은 양, 추천 사냥법), 직업, 키와 명령어, 길 안내(모든 NPC·교관과 수련장 입구 좌표), 돈, 장비, 네임드, 레이드·던전, 의뢰·스토리, 펫·생활 기술, 이벤트.\n");
		md.append("- **보급관 브람**(넬라 뒤): 모험가마다 한 번, 인챈트 없이 내구도가 닳지 않는 \"보급관의\" 장비 ").append(StarterKit.ITEMS.size())
			.append("개 — 철 투구·흉갑·레깅스·부츠, 철 검·곡괭이·도끼, 방패(왼손이 비어 있으면 바로 들려 줌). 방패가 생기기 전에 보급품을 받은 모험가는 다시 찾아가면 방패만 받습니다.\n");
		md.append("- **제빵사 한나**(브람 맞은편): 모험가마다 마인크래프트 하루(낮과 밤 한 바퀴, 잠자면 넘어감)에 한 번 빵 ").append(DailyBread.COUNT).append("개.\n");
		BlockPos stairs = TrainingGrounds.entrance(0);
		int toTen = 0;
		for (int level = 1; level < 10; level++) {
			toTen += JobProgression.expToNext(level);
		}
		md.append("- **수련장**(도시 지하, 분수 남동쪽 공원의 정자 계단 x ").append(stairs.getX()).append(", z ").append(stairs.getZ())
			.append("): 직업을 고르기 전의 모험가가 홀 안에 있으면 벽의 감실 ").append(TrainingGrounds.SPAWNS.size())
			.append("곳에서 좀비·스켈레톤·거미·드라운드가 계속 나옵니다(동시에 ").append(TrainingGrounds.cap(1)).append("마리, 수련생이 한 명 늘 때마다 +")
			.append(TrainingGrounds.PER_TRAINEE).append(", 최대 ").append(TrainingGrounds.MAX_CAP)
			.append("마리, 플레이어 바로 옆 감실에서는 나오지 않음). 직업이 없는 모험가는 수련장 몬스터에게서 직업 경험치를 ")
			.append(TrainingGrounds.EXP_MULTIPLIER).append("배로 얻어, 레벨 10까지(경험치 ").append(toTen).append(") 좀비 약 ")
			.append((toTen + 20 * TrainingGrounds.EXP_MULTIPLIER - 1) / (20 * TrainingGrounds.EXP_MULTIPLIER))
			.append("마리면 됩니다. 아무도 수련하지 않으면 몬스터는 사라지고, 계단으로 따라 올라온 몬스터는 경비병이 쫓아냅니다. 직업이 있는 플레이어만 있으면 몬스터가 나오지 않습니다.\n\n");
		md.append("## 마을 편의 시설\n\n");
		BlockPos nether = CityFixtures.netherPortal(0);
		BlockPos end = CityFixtures.endPortal(0);
		md.append("- **차원문**: 성 안뜰의 레이드·던전 문 사이에 켜진 네더 차원문(x ").append(nether.getX()).append(", z ").append(nether.getZ())
			.append(")과 엔드 차원문(x ").append(end.getX()).append(", z ").append(end.getZ()).append(").\n");
		md.append("- **엔더 상자** ").append(CityFixtures.ENDER_CHESTS.size()).append("개: 광장(스폰 뒤·분수 북쪽), 성 안뜰, 모험가 길드 안, 시장, 남쪽 광장.\n");
		md.append("- **건축 평야**: 동문 밖 x ").append(Homestead.X0).append("–").append(Homestead.X1).append(", z ").append(-Homestead.HALF_Z).append("–").append(Homestead.HALF_Z)
			.append("의 평지(도시 바닥 높이, 나무·구조물 없음, 지상에 적대 몹이 나오지 않음). 누구나 건축할 수 있습니다. 이 평야가 생기기 전에 만든 월드는 서버가 처음 켜질 때 한 번 평야 구역을 평지로 깎습니다(그 자리의 지형·나무·건물은 사라지고, 이후 지은 건물은 그대로).\n");
		md.append("- **이동 마법진**(자석석): 광장 스폰 바로 뒤(x 0, z ").append(CityFixtures.WAYSTONE.getZ()).append(")와 건축 평야 한가운데(x ").append(Homestead.CENTER_X)
			.append(", z ").append(Homestead.CENTER_Z).append("). 우클릭하면 서로 이동합니다(3초 재사용 대기). 귀환 주문서로도 광장에 돌아올 수 있습니다.\n");
		md.append("- **부활**: 평소에는 광장, 침대에서 자거나 침대를 우클릭하면 그 침대에서 부활합니다(침대가 부서지면 다시 광장).\n");
		md.append("- **가방**: 보급관 브람이 장비·소비·재료 가방을 한 번씩 줍니다(잡화점에서 은화 ").append(ShopOffers.BAG_PRICE / Coins.SILVER).append("개에 추가 구매). 가방마다 ")
			.append(Bags.SIZE).append("칸이고, 주운 물건이 종류에 맞는 가방에 먼저 들어갑니다 — 장비 가방: 직업 장비·무기·방어구·도구, 소비 가방: 음식·물약·소모품, 재료 가방: 광석·원석·주괴·보석·돌·흙과 강화석·보호 주문서·각성 결정·거신 파편(강화대·제작대는 가방 속 재료도 씀). 레이드·던전·의뢰 보상과 제작품도 가방에 먼저 들어가고, 던전·레이드 보상이 넘치면 우편함으로 갑니다. ")
			.append("동전·정수·진화의 에테르·시련 증표·쐐기돌·화살·불사의 토템은 가방에 들어가지 않습니다(상점과 교관이 인벤토리에서 찾음). 들고 우클릭하거나 인벤토리에서 우클릭하면 열리고, 아이템을 든 채 가방을 우클릭하면 넣습니다. 가방에는 가방·셜커 상자를 넣을 수 없습니다.\n");
		md.append("- **쓰레기통**: 인벤토리 오른쪽의 \"버림\" 버튼. 커서에 든 아이템을 없애고, 빈 커서로 누르면 마지막에 버린 것을 한 번 되돌립니다. 잡템(막대기·씨앗·켈프·네더랙·석영 등)은 잡화점이 싸게 사 줍니다.\n");
		md.append("- **대상 체력 바**: 마지막으로 때린 몬스터의 이름과 체력이 화면 위(보스 바 아래)에 ").append(TargetHealthHud.SHOW_TICKS / 20).append("초 동안 보입니다.\n");
		md.append("- **시련 사냥터**: 광장의 **사냥터지기 개릭**에게 말을 걸면, 진행 중인 시련에 아직 필요한 몬스터만 나오는 개인 사냥터(던전 차원)로 갑니다. 동시에 최대 ")
			.append(TrialHunts.CAP).append("마리(보스는 한 마리씩, 엔더 드래곤은 엔드에서), 필요한 처치·증표를 다 채우면 ").append("몬스터가 더 나오지 않고 잠시 뒤 돌아옵니다. 입구의 자석석을 우클릭하면 언제든 나갑니다. 쓰러지면 광장으로 돌아옵니다.\n");
		md.append("- **전직 장비**: 전직할 때마다(1차 포함) 지금 레벨 구간에 맞는 직업 무기 1개와 방어구 한 벌(투구·흉갑·레깅스·부츠)을 받습니다. 직업·차수마다 한 번뿐입니다(직업 초기화 후 다시 받지 않음).\n\n");
		md.append("## 지도·미니맵과 손에 든 광원\n\n");
		md.append("- **미니맵**(오른쪽 위, 쉼표 키로 켜고 끔): 돌아다닌 지형이 바닐라 지도 색으로 그려집니다(북쪽이 위). 자기 위치와 바라보는 방향, 다른 플레이어, 수도, 웨이포인트(화면 밖이면 가장자리에 고정)와 좌표가 표시됩니다. 크기 ")
			.append(MapSettings.SIZES[0]).append("/").append(MapSettings.SIZES[1]).append("/").append(MapSettings.SIZES[2]).append(" px, 배율 x0.5/x1/x2.\n");
		md.append("- **월드 지도**(M): 이 차원에서 본 모든 지형. 드래그로 이동, 휠로 확대/축소, 우클릭으로 그 자리에 웨이포인트(이름·색상). 오른쪽 목록은 거리순이 아닌 추가순이며 클릭하면 그곳을 보여 주고 우클릭하면 편집·삭제합니다. 죽은 자리는 \"마지막 사망 지점\"으로 자동 표시됩니다.\n");
		md.append("- 지형과 웨이포인트는 클라이언트의 `minecraft_mode/map/<월드>/<차원>.bin`, `minecraft_mode/waypoints/<월드>.json`에 저장됩니다(싱글은 월드 이름, 서버는 주소 기준).\n");
		md.append("- **손에 든 광원**: 횃불·영혼 횃불·랜턴·발광석·바다 랜턴·슈룸라이트·개구리불·용암 양동이처럼 빛나는 블록 아이템을 어느 손에든 들면 그 블록의 밝기로 주변이 밝아집니다. 서버가 머리(또는 발) 위치의 공기·물 칸에 보이지 않는 광원 블록을 두고 따라 옮기며, 내려놓거나 죽거나 접속을 끊으면 사라집니다.\n\n");
		md.append("## 칭호 이름표\n\n");
		md.append("- 착용한 칭호는 스코어보드 팀을 쓰지 않고 모드가 직접 이름 앞에 붙입니다(머리 위 이름·채팅·탭 목록). 다른 플러그인·데이터팩의 팀 설정과 충돌하지 않습니다.\n\n");

		md.append("## 초월 (레벨 ").append(JobProgression.MAX_LEVEL).append(" 이후)\n\n");
		md.append("- 레벨 ").append(JobProgression.MAX_LEVEL).append("부터 경험치가 **초월 레벨**로 쌓입니다. 다음 초월 레벨까지 4000 + 40 × 초월 레벨.\n");
		md.append("- 초월 레벨마다 포인트 1개를 능력치 하나에 넣습니다(능력치마다 최대 ").append(Paragon.MAX_RANK)
			.append("랭크). 특성 창(N)의 초월 탭. 초기화는 동전(Lv 100 구간 가격).\n\n");
		md.append("| 능력치 | 랭크당 |\n|---|---|\n");
		for (Paragon.Stat stat : Paragon.Stat.values()) {
			md.append("| ").append(stat.ko).append(" | ").append(stat.lines(1).stream().map(this::stat).collect(Collectors.joining(", "))).append(" |\n");
		}

		md.append("\n## 각성 (+").append(Enhancement.MAX).append(" 이후)\n\n");
		md.append("- +").append(Enhancement.MAX).append(" 장비를 강화 장인에게서 ✦1–✦").append(Enhancement.MAX_AWAKEN)
			.append("까지 각성합니다. **실패하지 않습니다.** 비용: 각성의 결정(목표 단계만큼)과 동전(장비 구간 가격 × (1 + 목표 단계)). 서버 전체에 알립니다.\n");
		md.append("- 각성의 결정: +5 이상 쐐기돌 던전 보상과 메인 스토리. 거신의 파편: 월드 보스(거신) 보상, 최상급 요리·연금 재료.\n\n");
		ClassGear weapon = ClassGear.of(JobWeapons.of(JobClass.WARRIOR).getFirst());
		ClassGear armor = ClassGear.of(ClassArmor.pieces().iterator().next());
		md.append("| 단계 | 결정 | 무기 (누적) | 방어구 (누적) |\n|---|---|---|---|\n");
		for (int a = 1; a <= Enhancement.MAX_AWAKEN; a++) {
			md.append("| ✦").append(a).append(" | ").append(Enhancement.crystals(a)).append(" | ")
				.append(Enhancement.lines(weapon, 0, a).stream().map(this::stat).collect(Collectors.joining(", "))).append(" | ")
				.append(Enhancement.lines(armor, 0, a).stream().map(this::stat).collect(Collectors.joining(", "))).append(" |\n");
		}

		md.append("\n## 생활 기술\n\n");
		md.append("- 요리(조리대), 연금술(연금술 작업대), 대장 기술(대장 작업대). 수도에 하나씩(시장 식료품 노점, 연금술사 노점, 대장간) 있고 직접 만들 수도 있습니다.\n");
		md.append("- 레벨 1–").append(Profession.MAX_LEVEL).append(". 다음 레벨까지 20 + 6 × 레벨. (레벨 ÷ 2)% 확률로 두 배 제작. 레시피 레벨 + 15 이상이면 숙련도 1/4.\n");
		md.append("- **약초**: 풀·꽃을 부수면 (12 + 연금술 레벨 ÷ 2)% 확률로 떨어지고 연금술 숙련도 +1. 햇살잎(기본), 달꽃잎(밤의 꽃), 서리뿌리(눈 내리는 생물군계), ")
			.append("빛버섯(버섯·발광 이끼·깊은 지하의 풀), 잿불꽃(네더 식물), 공허버섯(코러스).\n");
		for (Profession profession : Profession.values()) {
			md.append("\n### ").append(profession.ko).append("\n\n| 레벨 | 결과 | 재료 | 숙련도 |\n|---|---|---|---|\n");
			for (CraftRecipes.Recipe recipe : CraftRecipes.of(profession)) {
				String output = recipe.output() instanceof CraftRecipes.Output out
					? this.itemName(out.item()) + (out.count() > 1 ? " ×" + out.count() : "")
					: this.ko.getOrDefault("item.minecraft_mode." + recipe.id(), recipe.id()) + " ×3 (제작자 레벨 구간)";
				String ingredients = recipe.ingredients().stream().map(in -> this.itemName(in.item()) + " ×" + in.count()).collect(Collectors.joining(", "));
				md.append("| ").append(recipe.level()).append(" | ").append(output).append(" | ").append(ingredients).append(" | ").append(recipe.exp()).append(" |\n");
			}
		}

		md.append("\n## 펫과 탈것\n\n");
		md.append("- 펫 부적·탈것 호루라기를 쓰면 수집품에 추가되고(아이템 소모) 바로 소환됩니다. 수집품: P. 탈것 호출/내리기: H(마지막 탈것).\n");
		md.append("- 펫은 주인을 따라다니며(공격받지 않음) 소환 중일 때만 보너스를 줍니다. 레벨 1–").append(Companions.MAX_PET_LEVEL)
			.append(": 소환한 채 몬스터 처치 1, 네임드 10, 보스 50 경험치. 레벨 L까지 누적 25 × L × (L − 1).\n");
		md.append("- 탈것은 주인만 타고, 다치지 않으며, 내린 뒤 3초가 지나면 사라집니다. 레이드·던전·물속에서는 부를 수 없습니다. 비행 탈것은 공중에서 시선 방향으로 날아갑니다.\n");
		md.append("- 획득: 무작위 고급 펫 부적·질풍마 호루라기(공적 상점), 희귀(네임드 처치 ").append(ClassDocProvider.num(Companions.NAMED_DROP * 100)).append("%, 소굴 보물 ")
			.append(ClassDocProvider.num(Companions.LAIR_DROP * 100)).append("%, 던전 2%), 영웅(레이드 일반 3%·영웅 6%·악몽 10%, 거신 10%, +10 이상 던전), 메인 스토리. ")
			.append("이미 가진 것은 떨어지지 않습니다.\n\n");
		md.append("| 펫 | 등급 | 레벨당 보너스 | 비행 |\n|---|---|---|---|\n");
		for (Companions.PetDef def : Companions.pets()) {
			md.append("| ").append(def.ko()).append(" | ").append(rarity(def.rarity())).append(" | ").append(def.lines(1).stream().map(this::stat).collect(Collectors.joining(", ")))
				.append(" | ").append(def.flying() ? "예" : "").append(" |\n");
		}
		md.append("\n| 탈것 | 등급 | 속도(블록/초) | 점프 | 비행 |\n|---|---|---|---|---|\n");
		for (Companions.MountDef def : Companions.mounts()) {
			md.append("| ").append(def.ko()).append(" | ").append(rarity(def.rarity())).append(" | ").append(Math.round(def.speed() * 43.17)).append(" | ")
				.append(ClassDocProvider.num((float)def.jump())).append(" | ").append(def.flying() ? "예" : "").append(" |\n");
		}

		this.growth(md);

		md.append("\n## 던전 (2–4인)\n\n");
		md.append("- 성 안뜰 서쪽 던전 문의 **던전 관리인 카엘**. 파티장이 입장시키며(").append((int)Dungeons.GATHER_RANGE).append("블록 안의 파티원, 최대 ")
			.append(Dungeons.MAX_PARTY).append("명) 혼자도 됩니다.\n");
		md.append("- 구성: 입구 → 홀 ").append(DungeonLayout.HALLS).append("개(들어서면 몬스터가 깨어나고, 모두 쓰러뜨리면 다음 문이 열림, 홀마다 정예 1마리) → 보스 방(네임드의 **챔피언**: 최고 레벨, 체력 ×")
			.append(ClassDocProvider.num(Dungeons.CHAMPION_HEALTH)).append(", 피해 ×").append(ClassDocProvider.num(Dungeons.CHAMPION_DAMAGE)).append(", 분노 패턴).\n");
		md.append("- 쓰러지면 죽지 않고 입구에서 깨어나며 ").append(Dungeons.DEATH_PENALTY_SECONDS).append("초를 잃습니다. 건축·PvP 불가. `/dungeon leave`로 나갑니다.\n");
		md.append("- 홀 몬스터 체력: 기본 × (1 + 던전 힘/10) × 파티(1인 ").append(ClassDocProvider.num(Dungeons.partyScale(1))).append(" … 4인 ")
			.append(ClassDocProvider.num(Dungeons.partyScale(4))).append(", 인원마다 2마리씩 더 나옴) × 쐐기돌(1 + 0.08 × 단계), 정예 ×").append(ClassDocProvider.num(Dungeons.ELITE_HEALTH))
			.append(". 피해: × (1 + 던전 힘/50) × (1 + 0.08 × 단계).\n");
		md.append("- 보스 체력: 네임드 최고 레벨 체력 × ").append(ClassDocProvider.num(Dungeons.CHAMPION_HEALTH)).append(" × 파티(1인 ")
			.append(ClassDocProvider.num(Dungeons.bossPartyScale(1))).append(" … 4인 ").append(ClassDocProvider.num(Dungeons.bossPartyScale(4)))
			.append(") × 쐐기돌(1 + 0.15 × 단계), 피해 × ").append(ClassDocProvider.num(Dungeons.CHAMPION_DAMAGE)).append(" × (1 + 0.10 × 단계). 방 가운데서 ")
			.append((int)Dungeons.BOSS_LEASH).append("블록 넘게 끌려 나가면 돌아갑니다.\n");
		md.append("- 체력이 1024(바닐라 한도)를 넘어야 하는 몬스터는 ").append((int)com.minecraftmode.entity.MobPower.HEALTH_CAP)
			.append(" 체력에 받는 피해를 나눠(레이드 보스처럼) 실제 체력을 냅니다.\n");
		md.append("- 홀 밖(벽 속·지붕 위)으로 나간 홀 몬스터는 홀로 돌아오고, ").append(Dungeons.STUCK_TICKS / 20).append("초 동안 아무것도 맞지 않으면 남은 몬스터를 불러냅니다. 남은 수는 보스바에, ")
			.append(Dungeons.GLOW_LEFT).append("마리 이하가 되면 빛납니다. 홀 밖의 소환수·지원군은 사라집니다.\n\n");
		md.append("| 던전 | 입장 | 보스 | 몬스터 | 제한 시간 |\n|---|---|---|---|---|\n");
		for (DungeonDef def : Dungeons.all()) {
			NamedDef boss = NamedMobs.byId(def.boss());
			String trash = def.trash().stream().map(t -> this.ko.getOrDefault(t.getDescriptionId(), BuiltInRegistries.ENTITY_TYPE.getKey(t).getPath()))
				.collect(Collectors.joining(", "));
			md.append("| ").append(def.ko()).append(" | Lv ").append(def.minLevel()).append(" | ").append(boss == null ? def.boss() : boss.ko()).append(" (Lv ")
				.append(boss == null ? "?" : boss.hi()).append(") | ").append(trash).append(" | ").append(def.timeLimit() / 60).append("분 |\n");
		}
		md.append("\n### 쐐기돌\n\n");
		md.append("- 쐐기돌 없이 클리어하면 쐐기돌이 없는 사람마다 **+").append(Keystone.MIN_LEVEL).append(" 쐐기돌**을 받습니다. 쐐기돌 입장은 파티장의 가장 높은 쐐기돌을 씁니다.\n");
		md.append("- 제한 시간 안에 클리어하면 +1(60% 안이면 +2)되어 레벨에 맞는 던전(열린 레벨이 내 레벨 - ").append(Dungeons.KEYSTONE_REACH).append(" 이상, 가능하면 다른 곳)으로 바뀌고, 늦거나 포기·실패하면 1 내려갑니다(최소 +").append(Keystone.MIN_LEVEL).append(").\n\n");
		md.append("| 속성 | 단계 | 효과 |\n|---|---|---|\n");
		for (DungeonAffix a : DungeonAffix.values()) {
			md.append("| ").append(this.ko(a.nameKey())).append(" | +").append(a.level()).append(" | ").append(this.ko(a.descKey())).append(" |\n");
		}
		md.append("\n- 견고/폭군은 주기마다, 강화/분노는 두 주기마다 번갈아 나옵니다.\n");
		md.append("- 보상(클리어한 사람마다, 단계는 +").append(Dungeons.REWARD_SOFT_CAP).append("부터 절반 속도로 셈): 동전(던전 힘 구간 가격 × (0.6 + 0.15 × 단계)), 진화의 에테르 2 + 단계/2, 강화석 1 + 단계/3, 응축된 정수 1 + 단계/4, ")
			.append("장비 (40 + 4 × 단계)% (+7부터 강화된 채), +5부터 각성의 결정 (15 + 3 × 단계)%, 희귀 펫·탈것 2% (+10부터 영웅 2% + 단계당 0.5%). 제한 시간을 넘기면 ")
			.append(Math.round(Dungeons.LATE_SHARE * 100)).append("%만 받습니다.\n");
		md.append("- 직업 경험치: 한 판에 레벨의 ").append(Math.round(Dungeons.RUN_EXP * 100)).append("% (늦으면 절반, 단계·인원과 무관). 던전 몬스터는 처치 경험치가 없습니다. 레벨 ")
			.append(JobProgression.MAX_LEVEL).append(" 이후(파라곤)에는 단계당 2%p씩 늘어 최대 ").append(Math.round(Dungeons.PARAGON_EXP_MAX * 100)).append("%.\n");

		md.append("\n## 월드 이벤트\n\n");
		md.append("- 오버월드 하루에 한 번, 해 질 녘. 사흘 중 이틀은 **거신**, 하루는 **수도 침공**(몹 스폰 게임 규칙 `spawn_mobs`가 꺼져 있으면 열리지 않음). 관리자: `/worldevent titan [네임드]|invasion|stop`.\n");
		md.append("- **거신**: 도시 밖에 있는 무작위 플레이어 근처에서, 그 레벨에 맞는 네임드의 거대 챔피언(크기 ×").append(ClassDocProvider.num(WorldEvents.TITAN_SCALE))
			.append(", 체력 ×").append(ClassDocProvider.num(WorldEvents.TITAN_HEALTH)).append(", 근처 플레이어 1명당 +50%). ").append(WorldEvents.TITAN_LIFETIME / 1200)
			.append("분 안에 쓰러뜨려야 합니다. 최대 체력의 ").append(Math.round(WorldEvents.CONTRIBUTION * 100))
			.append("% 이상 피해를 준 모두에게: 거신의 파편 1–2개(상위 3명 +1), 진화의 에테르 4, 강화석 2, 동전, 영웅 펫·탈것 10%.\n");
		md.append("- **수도 침공**: 동·서·남 성문으로 ").append(WorldEvents.WAVES)
			.append("웨이브가 몰려옵니다(마지막 웨이브에 침공 군주). 경비병은 침공군을 막지 않습니다. 침공군을 공격한 모두에게: 동전, 진화의 에테르 3, 강화석 2, 공적 10. ")
			.append(WorldEvents.INVASION_LIFETIME / 1200).append("분이 지나면 물러갑니다.\n");
		md.append("- 침공군 세기: 수도 근처 수비대(직업 있는 플레이어) 평균 레벨 - ").append(WorldEvents.INVASION_LEVEL_OFFSET)
			.append(". 체력 배율 = (1 + 레벨/20) × 인원 보정(1명 ").append(ClassDocProvider.num(WorldEvents.partyScale(1))).append(", 2명 ").append(ClassDocProvider.num(WorldEvents.partyScale(2)))
			.append(", 3명 ").append(ClassDocProvider.num(WorldEvents.partyScale(3))).append(", ").append(WorldEvents.INVASION_PARTY_CAP).append("명 이상 ")
			.append(ClassDocProvider.num(WorldEvents.partyScale(WorldEvents.INVASION_PARTY_CAP))).append("). 침공군은 약화되어 주는 피해 ×").append(ClassDocProvider.num(WorldEvents.INVADER_DAMAGE))
			.append(", 침공 군주 ×").append(ClassDocProvider.num(WorldEvents.WARLORD_DAMAGE)).append(". 웨이브당 ").append(WorldEvents.invaderCount(1, 1)).append("–")
			.append(WorldEvents.invaderCount(WorldEvents.WAVES, WorldEvents.INVASION_PARTY_CAP)).append("마리.\n");

		md.append("\n## 메인 스토리\n\n");
		md.append("- 광장의 **왕실 전령 엘릭**에게서 장을 받고, 목표를 이루면 돌아가 보상을 받습니다. 도감(J)의 스토리 탭과 `/story`.\n\n");
		md.append("| 장 | 제목 | 목표 | 보상 |\n|---|---|---|---|\n");
		List<Story.Chapter> chapters = Story.chapters();
		for (int i = 0; i < chapters.size(); i++) {
			Story.Chapter c = chapters.get(i);
			md.append("| ").append(i + 1).append(" | ").append(c.titleKo()).append(" | ").append(c.goalKo()).append(" | ").append(Coins.format(Story.coins(i))).append(", ")
				.append(c.rewardKo()).append(" |\n");
		}
		return md.toString();
	}

	/** Where class experience comes from at three levels: amount and share of that level's requirement. */
	private void growth(final StringBuilder md) {
		int[] levels = {15, 45, 90};
		md.append("\n## 성장 경험치\n\n");
		md.append("- 레벨을 올리는 주된 길은 **소굴 탐험**입니다. 소굴·레이드·던전·네임드는 \"레벨분\"(다음 레벨까지 필요한 경험치의 비율)으로 주므로 레벨이 올라도 비중이 같고, ")
			.append("내 레벨이 더 높으면 그 콘텐츠의 최고 레벨 기준으로 줍니다. 일반 몬스터는 기본 체력만큼(던전·이벤트로 늘어난 체력은 치지 않음), 보스는 두 배입니다.\n");
		md.append("- 레벨 ").append(JobProgression.MAX_LEVEL).append(" 이후에는 경험치가 파라곤 레벨로 들어갑니다.\n\n");
		md.append("| 활동 | Lv ").append(levels[0]).append(" | Lv ").append(levels[1]).append(" | Lv ").append(levels[2]).append(" |\n|---|---|---|---|\n");
		this.growthRow(md, "다음 레벨까지", levels, l -> String.valueOf(JobProgression.expToNext(l)));
		this.growthRow(md, "좀비 (체력 20)", levels, l -> exp(20, l));
		this.growthRow(md, "철광석 / 다이아몬드 광석", levels, l -> exp(JobEvents.IRON.exp(l), l) + " / " + exp(JobEvents.DIAMOND.exp(l), l));
		this.growthRow(md, "네임드 처치", levels, l -> exp(JobProgression.levelExp(l, JobEvents.NAMED_SHARE), l));
		this.growthRow(md, "소굴 첫 발견 (소굴마다 1번)", levels, l -> exp(JobProgression.levelExp(l, LairExp.DISCOVER), l));
		this.growthRow(md, "소굴 군주 처치", levels, l -> exp(JobProgression.levelExp(l, JobEvents.LORD_SHARE), l));
		this.growthRow(md, "소굴 보물 상자 (주기마다)", levels, l -> exp(JobProgression.levelExp(l, LairExp.CHEST), l));
		this.growthRow(md, "소굴 첫 공략 (상자에 더해, 1번)", levels, l -> exp(JobProgression.levelExp(l, LairExp.FIRST_CLEAR), l));
		this.growthRow(md, "막다른 길 보물함 (주기마다)", levels, l -> exp(JobProgression.levelExp(l, LairExp.CACHE), l));
		for (RaidDifficulty d : RaidDifficulty.values()) {
			this.growthRow(md, "레이드 클리어 (" + this.ko(d.nameKey()) + ")", levels, l -> exp(JobProgression.levelExp(l, d.exp), l));
		}
		this.growthRow(md, "던전 한 판 (시간 안)", levels, l -> exp(JobProgression.levelExp(l, Dungeons.RUN_EXP), l));
	}

	private void growthRow(final StringBuilder md, final String label, final int[] levels, final java.util.function.IntFunction<String> cell) {
		md.append("| ").append(label);
		for (int level : levels) {
			md.append(" | ").append(cell.apply(level));
		}
		md.append(" |\n");
	}

	/** "{@code exp} (n%)": an amount and its share of the level's requirement. */
	private static String exp(final int exp, final int level) {
		return exp + " (" + Math.round(exp * 100.0F / JobProgression.expToNext(level)) + "%)";
	}

	private static String rarity(final net.minecraft.world.item.Rarity rarity) {
		return switch (rarity) {
			case EPIC -> "영웅";
			case RARE -> "희귀";
			case UNCOMMON -> "고급";
			default -> "일반";
		};
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

	/** Up to two decimals (0.16, 1.15, 43.2). */
	private static String exact(final double value) {
		return java.math.BigDecimal.valueOf(Math.round(value * 100.0) / 100.0).stripTrailingZeros().toPlainString();
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
