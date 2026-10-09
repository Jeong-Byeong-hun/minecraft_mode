package com.minecraftmode.client.datagen;

import com.minecraftmode.MinecraftMode;
import com.minecraftmode.entity.ClassTrainer;
import com.minecraftmode.job.JobClass;
import com.minecraftmode.job.JobProgression;
import com.minecraftmode.job.engrave.EngraveStat;
import com.minecraftmode.job.engrave.Engraving;
import com.minecraftmode.job.quest.QuestDef;
import com.minecraftmode.job.quest.Quests;
import com.minecraftmode.job.skill.Skill;
import com.minecraftmode.job.skill.SkillAction;
import com.minecraftmode.job.weapon.Archetype;
import com.minecraftmode.job.weapon.JobWeapons;
import com.minecraftmode.job.weapon.WeaponDef;
import com.minecraftmode.talent.TalentTree;
import com.minecraftmode.talent.Talents;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricLanguageProvider.TranslationBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.PlainTextContents;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;

/**
 * Writes docs/CLASSES.md (Korean): tiers, passives, every weapon with its skills and their
 * generated descriptions, and the engraving pools. Rendered from the same definitions and lang
 * templates the game uses, so it cannot drift from the code.
 */
public class ClassDocProvider implements DataProvider {
	private final FabricPackOutput output;
	private final Map<String, String> ko = new HashMap<>();

	public ClassDocProvider(final FabricPackOutput output) {
		this.output = output;
	}

	@Override
	public CompletableFuture<?> run(final CachedOutput cache) {
		JobLang.add(new TranslationBuilder() {
			@Override
			public boolean has(final String key) {
				return ClassDocProvider.this.ko.containsKey(key);
			}

			@Override
			public String overwrite(final String key, final String value) {
				return ClassDocProvider.this.ko.put(key, value);
			}
		}, true);
		this.vanillaNames();
		StringBuilder md = new StringBuilder();
		md.append("# 직업 (클래스)\n\n");
		md.append("> 이 문서는 `./gradlew runDatagen`이 코드 정의에서 생성합니다(`ClassDocProvider`). 직접 고치지 마세요.\n\n");
		md.append("- 직업 5종 × 4차 전직, 직업마다 무기 23종(1차 3 · 2차 6 · 3차 6 · 4차 8), 무기마다 스킬 3개(4차는 4개).\n");
		md.append("- 스킬 키: R / G / V / Z (4번째는 4차 무기만), 직업 창: K. 키는 설정 > 조작에서 바꿀 수 있습니다.\n");
		md.append("- 직업 무기는 누구나 들 수 있지만, **직업·차수·레벨 조건을 모두 만족해야** 스킬과 각인이 작동합니다. 그 외에는 기본 공격만 됩니다.\n");
		md.append("- 피해 수치는 무기 위력(차수·레벨로 증가)에 대한 배율입니다. 위력 = 4 + 2.2 × 차수 + 0.06 × 요구 레벨.\n\n");

		md.append("## 레벨과 전직\n\n");
		md.append("| 차수 | 요구 레벨 |\n|---|---|\n");
		for (int tier = 1; tier <= 4; tier++) {
			md.append("| ").append(tier).append("차 | ").append(JobProgression.levelForTier(tier)).append(" |\n");
		}
		md.append("\n- 경험치: 적대적 몹 처치(최대 체력만큼, 보스는 2배), 철 이상 광석 채굴. 최대 레벨 ").append(JobProgression.MAX_LEVEL).append(".\n");
		md.append("- 레벨 10마다 최대 체력 +1. 최대 MP = 30 + 2 × 레벨. 사망 시 현재 레벨 진행도의 10%를 잃습니다(레벨은 유지).\n");
		md.append("- 직업 초기화 주문서(모험가 길드, 금화 4)로 직업을 다시 고를 수 있습니다. 레벨은 유지됩니다.\n\n");

		md.append("## 전직 시련\n\n");
		md.append("- 직업 선택과 모든 전직은 수도 **스톰홀드**(0, 0)의 직업 교관에게서 받는 시련으로 합니다. 시련은 한 번에 하나만 진행할 수 있습니다.\n");
		md.append("- 목표 몹을 처치하면 진행도가 오르고, 해당 몹이 **시련 증표**를 확률적으로 떨어뜨립니다. 증표는 시련 중인 플레이어의 인벤토리로 바로 들어옵니다.\n");
		md.append("- 보스(위더 · 엔더 드래곤 · 워든 · 엘더 가디언 · 미스릴 골렘)는 64블록 안에서 같은 시련을 진행 중인 모든 플레이어에게 인정됩니다.\n");
		md.append("- 목표 처치 + 증표 + 재료를 갖추고 교관에게 돌아가 \"시련 완료\"를 누르면 증표와 재료를 소모하고 전직합니다.\n\n");
		md.append("| 직업 | 차수 | 교관 | 시련 | 처치 목표 | 증표 (드롭) | 재료 |\n|---|---|---|---|---|---|---|\n");
		for (QuestDef quest : Quests.all()) {
			String goals = quest.kills().stream().map(k -> k.ko() + " ×" + k.count()).collect(Collectors.joining("<br>"));
			String drops = quest.sources().stream().map(s -> (s.types().size() > 8 ? "모든 적대적 몹" : s.types().stream().map(this::entityName).sorted().collect(Collectors.joining("·")))
				+ " " + Math.round(s.chance() * 100) + "%" + (s.amount() > 1 ? " ×" + s.amount() : "")).collect(Collectors.joining("<br>"));
			String materials = quest.materials().stream().map(m -> this.itemName(m.item()) + " ×" + m.count()).collect(Collectors.joining(", "));
			md.append("| ").append(quest.job().ko()).append(" | ").append(quest.tier()).append("차 | ").append(this.ko.get(ClassTrainer.nameKey(quest.job())))
				.append(" | ").append(quest.ko()).append(" | ").append(goals).append(" | ").append(this.itemName(quest.token())).append(" ×").append(quest.tokenCount())
				.append("<br><sub>").append(drops).append("</sub> | ").append(materials).append(" |\n");
		}
		md.append("\n");

		md.append("## 정수와 각인\n\n");
		md.append("- 정수: 적대적 몹이 무작위로 떨어뜨리고(체력이 높을수록 확률↑), 철 이상 광석(섬세한 손길 제외)에서 나옵니다. 보스(최대 체력 100 이상)는 응축된 정수를 반드시 떨어뜨립니다.\n");
		md.append("- 응축된 정수 = 정수 9개 (조합대에서 서로 변환).\n");
		md.append("- 정수 각인대에서 직업 무기에 각인을 최대 3줄 붙입니다. 같은 각인을 여러 줄 붙이면 수치가 더해집니다(예: 평타 반경 1.5블록 × 3 = 4.5블록).\n");
		md.append("- 비용: 차수 × 4 × (현재 줄 수 + 1) 정수. 후보 새로 고침·줄 제거: 차수 × 2 정수.\n\n");

		md.append("## 특성\n\n");
		md.append("- 특성 창: N. 포인트 = (레벨 - 10) × 2 ÷ 3 (Lv 100에 ").append(Talents.points(JobProgression.MAX_LEVEL)).append("점). 계열마다 1–4단계는 최대 ")
			.append(TalentTree.RANKS).append("랭크, 5단계는 1랭크 핵심 특성이며, 다음 단계는 그 계열에 ").append(TalentTree.PER_TIER).append("점씩 넣어야 열립니다.\n");
		md.append("- 표의 수치는 1랭크당 효과입니다(괄호 안은 최대 랭크). 직업별 특성표는 각 직업 절에 있습니다.\n\n");

		for (JobClass job : JobClass.PLAYABLE) {
			md.append("## ").append(job.ko()).append(" (").append(job.en()).append(")\n\n");
			md.append("| 차수 | 전직명 | 패시브 | 효과 |\n|---|---|---|---|\n");
			for (int tier = 1; tier <= 4; tier++) {
				JobClass.Tier t = job.tier(tier);
				md.append("| ").append(tier).append("차 | ").append(t.ko()).append(" | ").append(t.passiveKo()).append(" | ").append(t.passiveDescKo()).append(" |\n");
			}
			md.append("\n### 각인\n\n| 각인 | 효과 (1줄) | 장착 가능 무기 |\n|---|---|---|\n");
			for (Engraving e : Engraving.values()) {
				if (e.job() != job) {
					continue;
				}
				String only = Arrays.stream(Archetype.values()).filter(a -> e.fits(job, a)).count() == Archetype.values().length
					? "전체"
					: Arrays.stream(Archetype.values()).filter(a -> e.fits(job, a)).map(Archetype::ko).collect(Collectors.joining(", "));
				md.append("| ").append(e.ko()).append(" | ").append(statText(e.stat(), e.value())).append(" | ").append(only).append(" |\n");
			}
			md.append("\n### 특성\n\n| 단계 |");
			List<TalentTree.Branch> branches = TalentTree.of(job);
			for (TalentTree.Branch b : branches) {
				md.append(" ").append(b.ko()).append(" |");
			}
			md.append("\n|---|").append("---|".repeat(branches.size())).append("\n");
			for (int t = 0; t < TalentTree.TIERS; t++) {
				md.append("| ").append(t + 1).append(t == TalentTree.TIERS - 1 ? " (핵심)" : "").append(" |");
				for (TalentTree.Branch b : branches) {
					TalentTree.Node node = b.nodes().get(t);
					md.append(" **").append(node.ko()).append("** ").append(node.perRank().stream().map(l -> statText(l.stat(), l.value()))
						.collect(Collectors.joining(", "))).append(node.maxRank() > 1 ? " (×" + node.maxRank() + ")" : "").append(" |");
				}
				md.append("\n");
			}
			for (int tier = 1; tier <= 4; tier++) {
				md.append("\n### ").append(tier).append("차 무기 — ").append(job.tier(tier).ko()).append("\n");
				for (WeaponDef def : JobWeapons.of(job)) {
					if (def.tier() != tier) {
						continue;
					}
					md.append("\n#### ").append(def.ko()).append(" (").append(def.en()).append(")\n\n");
					md.append(def.archetype().ko()).append(" · 요구 Lv ").append(def.level()).append(" · 위력 ").append(num(def.power()))
						.append(" · 근접 피해 ").append(num(def.attackDamage()));
					if (def.archetype().isRanged()) {
						md.append(" · ").append(this.ko.get(def.archetype().shot().nameKey())).append(" 기본 사격 ").append(num(def.power() * def.archetype().shotMultiplier()));
					}
					md.append("\n\n| 키 | 스킬 | 종류 | MP | 대기 | 효과 |\n|---|---|---|---|---|---|\n");
					String[] keys = {"R", "G", "V", "Z"};
					for (int i = 0; i < def.skills().size(); i++) {
						Skill skill = def.skills().get(i);
						String effects = skill.actions().stream().map(SkillAction::describe).map(this::render).collect(Collectors.joining("<br>"));
						md.append("| ").append(keys[i]).append(" | ").append(skill.ko()).append(" | ").append(skill.kind().ko()).append(" | ").append(skill.manaCost())
							.append(" | ").append(num(skill.cooldownTicks() / 20.0)).append("초 | ").append(effects).append(" |\n");
					}
				}
			}
			md.append("\n");
		}
		try {
			Path root = this.output.getOutputFolder().getParent().getParent().getParent();
			Path file = root.resolve("docs/CLASSES.md");
			Files.createDirectories(file.getParent());
			Files.writeString(file, md.toString(), StandardCharsets.UTF_8);
		} catch (IOException | NullPointerException e) {
			MinecraftMode.LOGGER.warn("Could not write docs/CLASSES.md", e);
		}
		return CompletableFuture.completedFuture(null);
	}

	private String itemName(final Item item) {
		return this.ko.getOrDefault(item.getDescriptionId(), BuiltInRegistries.ITEM.getKey(item).getPath());
	}

	private String entityName(final EntityType<?> type) {
		return this.ko.getOrDefault(type.getDescriptionId(), BuiltInRegistries.ENTITY_TYPE.getKey(type).getPath());
	}

	private String statText(final EngraveStat stat, final float value) {
		return String.format(Locale.ROOT, stat.ko(), num(value)).replace("%%", "%");
	}

	static String num(final double v) {
		double r = Math.round(v * 10.0) / 10.0;
		return r == Math.rint(r) ? Long.toString((long)r) : String.format(Locale.ROOT, "%.1f", r);
	}

	/** Formats a component with the Korean strings collected from JobLang (plus a few vanilla names). */
	private String render(final Component component) {
		StringBuilder out = new StringBuilder();
		if (component.getContents() instanceof TranslatableContents t) {
			String template = this.ko.getOrDefault(t.getKey(), t.getKey());
			Object[] args = Arrays.stream(t.getArgs()).map(a -> a instanceof Component c ? this.render(c) : String.valueOf(a)).toArray();
			out.append(String.format(Locale.ROOT, template, args));
		} else if (component.getContents() instanceof PlainTextContents p) {
			out.append(p.text());
		}
		for (Component sibling : component.getSiblings()) {
			out.append(this.render(sibling));
		}
		return out.toString().replace("|", "\\|");
	}

	private void vanillaNames() {
		Object[][] effects = {
			{MobEffects.SPEED, "신속"}, {MobEffects.SLOWNESS, "구속"}, {MobEffects.HASTE, "성급함"}, {MobEffects.MINING_FATIGUE, "채굴 피로"},
			{MobEffects.STRENGTH, "힘"}, {MobEffects.JUMP_BOOST, "점프 강화"}, {MobEffects.NAUSEA, "멀미"}, {MobEffects.REGENERATION, "재생"},
			{MobEffects.RESISTANCE, "저항"}, {MobEffects.FIRE_RESISTANCE, "화염 저항"}, {MobEffects.WATER_BREATHING, "수중 호흡"},
			{MobEffects.INVISIBILITY, "투명화"}, {MobEffects.BLINDNESS, "실명"}, {MobEffects.NIGHT_VISION, "야간 투시"}, {MobEffects.HUNGER, "허기"},
			{MobEffects.WEAKNESS, "나약함"}, {MobEffects.POISON, "독"}, {MobEffects.WITHER, "시듦"}, {MobEffects.HEALTH_BOOST, "생명력 강화"},
			{MobEffects.ABSORPTION, "흡수"}, {MobEffects.GLOWING, "발광"}, {MobEffects.LEVITATION, "공중 부양"}, {MobEffects.LUCK, "행운"},
			{MobEffects.SLOW_FALLING, "느린 낙하"}, {MobEffects.DOLPHINS_GRACE, "돌고래의 우아함"}, {MobEffects.DARKNESS, "어둠"},
			{MobEffects.CONDUIT_POWER, "전달체의 힘"}, {com.minecraftmode.registry.ModEffects.BLEEDING, "출혈"}
		};
		for (Object[] e : effects) {
			@SuppressWarnings("unchecked")
			net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> holder = (net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect>)e[0];
			this.ko.putIfAbsent(holder.value().getDescriptionId(), (String)e[1]);
		}
		this.ko.put("item.minecraft.nether_star", "네더의 별");
		this.ko.put("item.minecraft.heart_of_the_sea", "바다의 심장");
		String[][] entities = {
			{"zombie", "좀비"}, {"husk", "허스크"}, {"drowned", "드라운드"}, {"zombie_villager", "좀비 주민"}, {"skeleton", "스켈레톤"}, {"stray", "스트레이"},
			{"bogged", "보그드"}, {"spider", "거미"}, {"cave_spider", "동굴 거미"}, {"pillager", "약탈자"}, {"vindicator", "변명자"}, {"evoker", "소환사"},
			{"ravager", "파괴수"}, {"wither_skeleton", "위더 스켈레톤"}, {"piglin_brute", "난폭한 피글린"}, {"enderman", "엔더맨"}, {"creeper", "크리퍼"},
			{"witch", "마녀"}, {"blaze", "블레이즈"}, {"wither", "위더"}, {"warden", "워든"}, {"shulker", "셜커"}, {"ender_dragon", "엔더 드래곤"},
			{"phantom", "팬텀"}, {"ghast", "가스트"}, {"hoglin", "호글린"}, {"guardian", "가디언"}, {"elder_guardian", "엘더 가디언"},
		};
		for (String[] e : entities) {
			this.ko.put("entity.minecraft." + e[0], e[1]);
		}
		this.ko.putIfAbsent("entity.minecraft_mode.mythril_golem", "미스릴 골렘");
		String[] roman = {"I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};
		for (int i = 0; i < roman.length; i++) {
			this.ko.put("enchantment.level." + (i + 1), roman[i]);
		}
	}

	@Override
	public String getName() {
		return "Class documentation";
	}
}
