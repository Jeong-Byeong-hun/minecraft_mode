package com.minecraftmode.client.datagen;

import com.minecraftmode.MinecraftMode;
import com.minecraftmode.job.JobClass;
import com.minecraftmode.job.JobProgression;
import com.minecraftmode.job.engrave.EngraveStat;
import com.minecraftmode.job.engrave.Engraving;
import com.minecraftmode.job.skill.Skill;
import com.minecraftmode.job.skill.SkillAction;
import com.minecraftmode.job.weapon.Archetype;
import com.minecraftmode.job.weapon.JobWeapons;
import com.minecraftmode.job.weapon.WeaponDef;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
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
		md.append("| 차수 | 요구 레벨 | 전직 재료 |\n|---|---|---|\n");
		for (int tier = 1; tier <= 4; tier++) {
			String cost = JobProgression.costsForTier(tier).stream().map(c -> this.itemName(c.item()) + " ×" + c.count()).collect(Collectors.joining(", "));
			md.append("| ").append(tier).append("차 | ").append(JobProgression.levelForTier(tier)).append(" | ").append(cost.isEmpty() ? "없음 (직업 선택)" : cost).append(" |\n");
		}
		md.append("\n- 경험치: 적대적 몹 처치(최대 체력만큼, 보스는 2배), 철 이상 광석 채굴. 최대 레벨 ").append(JobProgression.MAX_LEVEL).append(".\n");
		md.append("- 레벨 10마다 최대 체력 +1. 최대 MP = 30 + 2 × 레벨. 사망 시 현재 레벨 진행도의 10%를 잃습니다(레벨은 유지).\n");
		md.append("- 직업 초기화 주문서(직업 길드, 금화 4)로 직업을 다시 고를 수 있습니다. 레벨은 유지됩니다.\n\n");

		md.append("## 정수와 각인\n\n");
		md.append("- 정수: 적대적 몹이 무작위로 떨어뜨리고(체력이 높을수록 확률↑), 철 이상 광석(섬세한 손길 제외)에서 나옵니다. 보스(최대 체력 100 이상)는 응축된 정수를 반드시 떨어뜨립니다.\n");
		md.append("- 응축된 정수 = 정수 9개 (조합대에서 서로 변환).\n");
		md.append("- 정수 각인대에서 직업 무기에 각인을 최대 3줄 붙입니다. 같은 각인을 여러 줄 붙이면 수치가 더해집니다(예: 평타 반경 1.5블록 × 3 = 4.5블록).\n");
		md.append("- 비용: 차수 × 4 × (현재 줄 수 + 1) 정수. 후보 새로 고침·줄 제거: 차수 × 2 정수.\n\n");

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
