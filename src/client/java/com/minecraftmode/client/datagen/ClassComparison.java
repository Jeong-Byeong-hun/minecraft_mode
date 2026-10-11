package com.minecraftmode.client.datagen;

import static com.minecraftmode.job.engrave.EngraveStat.*;

import com.minecraftmode.entity.combat.PlayerArmor;
import com.minecraftmode.job.JobClass;
import com.minecraftmode.job.JobData;
import com.minecraftmode.job.JobProgression;
import com.minecraftmode.job.JobStats;
import com.minecraftmode.job.engrave.EngraveTotals;
import com.minecraftmode.job.gear.ArmorPieceDef;
import com.minecraftmode.job.gear.ArmorSetDef;
import com.minecraftmode.job.gear.ClassArmor;
import com.minecraftmode.job.gear.ClassPassives;
import com.minecraftmode.job.gear.LevelRewards;
import com.minecraftmode.job.gear.StatLine;
import com.minecraftmode.job.skill.CombatHooks;
import com.minecraftmode.job.skill.Skill;
import com.minecraftmode.job.skill.SkillCaster;
import com.minecraftmode.job.weapon.Archetype;
import com.minecraftmode.job.weapon.BasicAttacks;
import com.minecraftmode.job.weapon.JobWeapons;
import com.minecraftmode.job.weapon.ProjectileStyle;
import com.minecraftmode.job.weapon.WeaponDef;
import com.minecraftmode.talent.TalentTree;
import com.minecraftmode.talent.Talents;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * The "직업 비교" section of CLASSES.md: what each class deals and survives at the last level of every tier, with its own passives,
 * level rewards and melee vitality, the highest armor set it can wear (four pieces: base options, set bonuses, defense) and talents
 * spent for single-target damage. Engravings, random armor options, enhancement, consumables and the support side of skills (buffs,
 * debuffs, heals, shields) are left out: the player picks them, or every class has them alike. Estimates to compare classes with.
 */
final class ClassComparison {
	/** The last level of each tier: every weapon of the tier is out by then. */
	static final int[] LEVELS = {24, 44, 69, 100};
	/** Seconds of fighting the MP pool plus regeneration has to pay for. */
	static final double FIGHT_SECONDS = 60.0;
	/** Blocks between an archer and the target (Deadeye, range bonus). */
	static final double SHOT_DISTANCE = 12.0;
	/** Share of a target's life below 30% health (execute) and above 90% (first strike). */
	static final double EXECUTE_SHARE = 0.3;
	static final double FIRST_STRIKE_SHARE = 0.1;
	/** Of five grouped monsters, how many a basic cannonball's blast catches. */
	static final double CANNON_TARGETS = 3.0;

	record Offense(double basic, double basic5, double skill1, double skill5, double mpLoad) {
		double total1() {
			return this.basic + this.skill1;
		}

		double total5() {
			return this.basic5 + this.skill5;
		}
	}

	record Result(
		JobClass job, int level, String talents, double basic, double skill1, double total1, double total5, double boss, double best, String bestWeapon,
		double mpLoad, double health, double armor, double toughness, double taken, double takenMagic, double dodge, double ehp, double ehpMagic
	) {
	}

	static int tierAt(final int level) {
		int tier = 1;
		for (int t = 2; t <= 4; t++) {
			if (level >= JobProgression.levelForTier(t)) {
				tier = t;
			}
		}
		return tier;
	}

	/** One monster hit at {@code level}, like the named monsters of that level (5 at Lv 10, 10 at 45, 20 at 100). */
	static double referenceHit(final int level) {
		return 3.0 + 0.17 * level;
	}

	static Result evaluate(final JobClass job, final int level) {
		int tier = tierAt(level);
		JobData data = new JobData(job, tier, level, 0, 0, Map.of());
		List<WeaponDef> weapons = JobWeapons.of(job).stream().filter(w -> w.tier() == tier && w.level() <= level).toList();
		ArmorSetDef set = ClassArmor.sets(job).stream().filter(s -> s.level() <= level).max(Comparator.comparingInt(ArmorSetDef::level)).orElse(null);
		List<StatLine> base = new ArrayList<>();
		base.addAll(LevelRewards.of(data));
		base.addAll(ClassPassives.of(data));
		if (set != null) {
			for (ArmorPieceDef piece : ClassArmor.piecesOf(set)) {
				base.add(piece.baseOption());
				base.addAll(piece.defenseLines());
			}
			for (ArmorSetDef.SetBonus bonus : set.bonuses()) {
				base.addAll(bonus.lines());
			}
		}
		// talents: try every split of the points over the three branches (each filled from its first talent) and keep the one that hits hardest
		List<TalentTree.Branch> branches = TalentTree.of(job);
		int points = Talents.points(level);
		int max = TalentTree.RANKS * (TalentTree.TIERS - 1) + 1;
		int[] bestSplit = new int[branches.size()];
		double bestScore = -1.0;
		for (int a = 0; a <= Math.min(max, points); a++) {
			for (int b = 0; b <= Math.min(max, points - a); b++) {
				int c = Math.min(max, points - a - b);
				int[] split = {a, b, c};
				EngraveTotals mods = totals(base, branches, split);
				double score = weapons.stream().mapToDouble(w -> offense(data, w, mods).total1()).sum();
				if (score > bestScore + 1.0E-9) {
					bestScore = score;
					bestSplit = split;
				}
			}
		}
		EngraveTotals mods = totals(base, branches, bestSplit);
		double basic = 0.0;
		double skill1 = 0.0;
		double total1 = 0.0;
		double total5 = 0.0;
		double mp = 0.0;
		double best = 0.0;
		String bestWeapon = "";
		for (WeaponDef w : weapons) {
			Offense o = offense(data, w, mods);
			basic += o.basic();
			skill1 += o.skill1();
			total1 += o.total1();
			total5 += o.total5();
			mp += o.mpLoad();
			if (o.total1() > best) {
				best = o.total1();
				bestWeapon = w.ko();
			}
		}
		int n = Math.max(1, weapons.size());
		StringBuilder talents = new StringBuilder();
		for (int i = 0; i < branches.size(); i++) {
			if (bestSplit[i] > 0) {
				talents.append(talents.isEmpty() ? "" : " · ").append(branches.get(i).ko()).append(' ').append(bestSplit[i]);
			}
		}
		// defense
		double health = 20 + level / 10 + job.vitalityHealth() * tier + (CombatHooks.has(data, JobClass.WARRIOR, 1) ? 4 : 0) + mods.get(MAX_HEALTH);
		double armor = job.vitalityArmor() * tier + mods.get(ARMOR);
		double toughness = 0.0;
		if (set != null) {
			for (ArmorPieceDef piece : ClassArmor.piecesOf(set)) {
				armor += piece.armor();
				toughness += piece.toughness();
			}
		}
		double hit = referenceHit(level);
		double taken = taken(hit, false, mods, armor, toughness) / hit;
		double takenMagic = taken(hit, true, mods, armor, toughness) / hit;
		double dodge = Math.min(0.75, mods.fraction(DODGE) + (CombatHooks.has(data, JobClass.ROGUE, 4) ? 0.15 : 0.0));
		double boss = total1 / n * (1.0 + mods.fraction(BOSS_DAMAGE));
		return new Result(job, level, talents.toString(), basic / n, skill1 / n, total1 / n, total5 / n, boss, best, bestWeapon, mp / n, health, armor, toughness,
			taken, takenMagic, dodge, health / taken / (1.0 - dodge), health / takenMagic / (1.0 - dodge));
	}

	private static EngraveTotals totals(final List<StatLine> base, final List<TalentTree.Branch> branches, final int[] split) {
		EngraveTotals.Builder builder = EngraveTotals.builder();
		for (StatLine line : base) {
			builder.add(line.stat(), line.value());
		}
		for (int i = 0; i < branches.size(); i++) {
			List<TalentTree.Node> nodes = branches.get(i).nodes();
			int left = split[i];
			// the capstone needs every earlier tier unlocked (4 points per tier), the rest fill from the first talent on
			if (left >= TalentTree.PER_TIER * (TalentTree.TIERS - 1) + 1) {
				nodes.get(TalentTree.TIERS - 1).lines(1).forEach(l -> builder.add(l.stat(), l.value()));
				left--;
			}
			for (int t = 0; t < TalentTree.TIERS - 1 && left > 0; t++) {
				int ranks = Math.min(nodes.get(t).maxRank(), left);
				nodes.get(t).lines(ranks).forEach(l -> builder.add(l.stat(), l.value()));
				left -= ranks;
			}
		}
		return builder.build();
	}

	static Offense offense(final JobData data, final WeaponDef w, final EngraveTotals mods) {
		Archetype type = w.archetype();
		double critChance = Math.min(1.0, mods.fraction(CRIT_CHANCE) + (CombatHooks.has(data, JobClass.ROGUE, 2) ? 0.15 : 0.0));
		double crit = 1.0 + critChance * (0.5 + mods.fraction(CRIT_DAMAGE));
		double first = mods.fraction(FIRST_STRIKE) * FIRST_STRIKE_SHARE;
		double execute = mods.fraction(EXECUTE) * EXECUTE_SHARE;
		// melee: fully charged swings, which land through the hurt cooldown (CombatHooks)
		double swings = type.attackSpeed() * (1.0 + mods.fraction(ATTACK_SPEED));
		double meleeHit = w.attackDamage() * (1.0 + mods.fraction(BASIC_DAMAGE) + (CombatHooks.has(data, JobClass.WARRIOR, 2) ? 0.10 : 0.0) + execute + first)
			* crit * (1.0 + mods.fraction(DOUBLE_STRIKE));
		double melee = meleeHit * swings;
		double melee5 = melee * (1.0 + 4 * 0.5 * Math.min(1.0, mods.get(SPLASH)));
		// shots: one per draw or reload, the extra ones of a fan only reach the other monsters of a group (BasicAttacks.Fan; the stat
		// stops at its cap)
		double shot = 0.0;
		double shot5 = 0.0;
		double shotRate = 0.0;
		if (type.isRanged()) {
			int ticks = Math.max(type.mode() == Archetype.BasicMode.DRAW ? 5 : 4, Math.round(type.shotCooldown() / (1.0F + mods.fraction(DRAW_SPEED))));
			shotRate = 20.0 / ticks;
			double distance = mods.fraction(RANGE_BONUS) * SHOT_DISTANCE / 10.0
				+ (CombatHooks.has(data, JobClass.ARCHER, 3) ? Math.min(0.40, 0.02 * (int)(SHOT_DISTANCE / 4.0)) : 0.0);
			double hit = w.shotDamage() * (1.0 + BasicAttacks.shotBonus(data, mods)) * (CombatHooks.has(data, JobClass.ARCHER, 1) ? 1.15 : 1.0)
				* (1.0 + execute + first + distance) * crit * (CombatHooks.has(data, JobClass.ARCHER, 4) ? 1.0 + BasicAttacks.BABYLON_CHANCE * 2 : 1.0);
			shot = hit * shotRate;
			double extra = mods.get(EXTRA_SHOT) * 0.5;
			shot5 = shot * (type.shot() == ProjectileStyle.CANNONBALL ? CANNON_TARGETS : 1.0 + extra);
		}
		boolean shoots = shot > melee;
		double basicHits = shoots ? shotRate : swings;
		double basic = shoots ? shot : melee;
		// chain lightning jumps to three more monsters for half the hit
		double basic5 = (shoots ? shot5 : melee5) + basic * Math.min(0.5, mods.fraction(CHAIN_LIGHTNING)) * 3 * 0.5;
		// skills: cast on cooldown; critical basic hits wind cooldowns down (crit refund); an echo skips the cooldown
		double skillPower = w.skillPower() * (1.0 + mods.fraction(SKILL_DAMAGE) + (CombatHooks.has(data, JobClass.MAGE, 3) ? SkillCaster.ARCANE_MASTERY : 0.0) + first) * crit;
		double speedUp = 1.0 + basicHits * critChance * mods.get(CRIT_REFUND);
		double echo = mods.fraction(ECHO);
		record Use(double dps1, double dps5, double mpPerSecond) {
		}
		List<Use> uses = new ArrayList<>();
		for (Skill skill : w.skills()) {
			double casts = speedUp / (SkillCaster.cooldown(data, skill, mods) / 20.0) / (1.0 - echo);
			uses.add(new Use(skill.estimate(1) * skillPower * casts, skill.estimate(5) * skillPower * casts, SkillCaster.manaCost(skill, mods) * casts));
		}
		// MP for a minute of fighting: the pool plus regeneration, spent on the skills that deal the most per point first
		double pool = JobProgression.BASE_MANA + 2 * data.level() + (CombatHooks.has(data, JobClass.MAGE, 1) ? 30 : 0) + mods.get(MAX_MANA);
		double regen = (1.0 + data.level() * JobStats.MANA_REGEN_PER_LEVEL) * (CombatHooks.has(data, JobClass.MAGE, 2) ? 2 : 1) + mods.get(MANA_REGEN)
			+ mods.get(MANA_ON_HIT) * basicHits;
		double available = pool + regen * FIGHT_SECONDS;
		double budget = available;
		uses.sort(Comparator.comparingDouble((Use u) -> u.mpPerSecond() <= 0.0 ? Double.MAX_VALUE : u.dps1() / u.mpPerSecond()).reversed());
		double skill1 = 0.0;
		double skill5 = 0.0;
		double wanted = 0.0;
		for (Use u : uses) {
			double need = u.mpPerSecond() * FIGHT_SECONDS;
			double share = need <= 0.0 ? 1.0 : Math.max(0.0, Math.min(1.0, budget / need));
			budget -= need * share;
			skill1 += u.dps1() * share;
			skill5 += u.dps5() * share;
			wanted += need;
		}
		return new Offense(basic, basic5, skill1, skill5, wanted / available);
	}

	/** Damage left of a {@code hit} after the class's reductions (CombatHooks.incoming) and then armor ({@code PlayerArmor}). */
	private static double taken(final double hit, final boolean magic, final EngraveTotals mods, final double armor, final double toughness) {
		double d = hit * (1.0 - Math.min(0.8, mods.fraction(DAMAGE_REDUCTION))) * (1.0 - mods.fraction(PROTECTION));
		if (magic) {
			d *= 1.0 - mods.fraction(MAGIC_DEFENSE);
		}
		d *= 1.0 - mods.fraction(MANA_SHIELD);
		return d * (1.0 - PlayerArmor.fraction((float)d, (float)armor, (float)toughness));
	}

	static void write(final StringBuilder md) {
		md.append("\n## 직업 비교\n\n");
		md.append("- 각 차수의 마지막 레벨(").append(java.util.Arrays.stream(LEVELS).mapToObj(Integer::toString).collect(Collectors.joining("·")))
			.append(")에서 그 차수 무기를 하나씩 들었을 때의 초당 피해 추정치입니다. 피해 열은 그 차수 무기 전부의 평균, \"최고 무기\"는 1마리 합계가 가장 높은 무기입니다.\n");
		md.append("- 넣은 것: 직업 패시브, 레벨 보너스, 근접 체질, 입을 수 있는 가장 높은 방어구 세트 4부위(기본 옵션·세트 효과·방어 성능), 1마리 피해가 가장 높아지게 찍은 특성(계열마다 첫 특성부터).\n");
		md.append("- 뺀 것: 각인, 방어구 추가 옵션, 강화·각성, 소모품, 스킬의 지원 효과(버프·디버프·회복·보호막·표식), 상태 이상 피해, 뒤치기와 체력 조건 보너스.\n");
		md.append("- 평타: 근접은 공격 속도마다 꽉 찬 공격 1번, 사격은 장전·시위마다 1발(거리 ").append(ClassDocProvider.num(SHOT_DISTANCE))
			.append("블록), 사격 무기는 근접과 사격 중 높은 쪽입니다.\n");
		md.append("- 스킬: 대기 시간마다 씁니다. MP는 ").append(ClassDocProvider.num(FIGHT_SECONDS))
			.append("초 전투(최대 MP + 그동안의 회복)로 계산해 MP당 피해가 좋은 스킬부터 쓰고, 모자라면 덜 씁니다. \"MP 소모\"는 모든 스킬을 대기 시간마다 쓸 때 필요한 MP ÷ 쓸 수 있는 MP입니다(100% 넘으면 스킬을 덜 씀).\n");
		md.append("- 치명타는 기대값, 처형은 대상 체력의 30% 구간, 선제 일격은 10% 구간에만 붙는다고 봤습니다. 5마리는 모여 있는 적 5마리에게 들어가는 합입니다.\n");
		md.append("- 생존: 유효 체력 = 최대 체력 ÷ 받는 피해 비율 ÷ (1 - 회피), 곧 쓰러지기 전까지 받아낼 수 있는 원래 피해량입니다. 기준 공격은 그 레벨 네임드 몬스터의 한 방(3 + 0.17 × 레벨)입니다.\n");
		for (int level : LEVELS) {
			List<Result> results = JobClass.PLAYABLE.stream().map(job -> evaluate(job, level)).toList();
			double mean1 = results.stream().mapToDouble(Result::total1).average().orElse(1.0);
			double meanEhp = results.stream().mapToDouble(Result::ehp).average().orElse(1.0);
			md.append("\n### Lv ").append(level).append(" (").append(tierAt(level)).append("차)\n\n");
			md.append("| 직업 | 평타 | 스킬 1마리 | 스킬 비중 | 합계 1마리 | 평균 대비 | 합계 5마리 | 보스 상대 | 최고 무기 | MP 소모 | 특성 |\n|---|---|---|---|---|---|---|---|---|---|---|\n");
			for (Result r : results) {
				md.append("| ").append(r.job().ko()).append(" | ").append(ClassDocProvider.num(r.basic())).append(" | ").append(ClassDocProvider.num(r.skill1()))
					.append(" | ").append(Math.round(r.skill1() / r.total1() * 100)).append("% | ")
					.append(ClassDocProvider.num(r.total1())).append(" | ").append(Math.round(r.total1() / mean1 * 100)).append("% | ")
					.append(ClassDocProvider.num(r.total5())).append(" | ").append(ClassDocProvider.num(r.boss())).append(" | ").append(r.bestWeapon()).append(' ')
					.append(ClassDocProvider.num(r.best())).append(" | ").append(Math.round(r.mpLoad() * 100)).append("% | ").append(r.talents().isEmpty() ? "-" : r.talents())
					.append(" |\n");
			}
			md.append("\n| 직업 | 최대 체력 | 방어 / 강도 | 받는 피해 (물리 / 마법) | 회피 | 유효 체력 (물리 / 마법) | 평균 대비 |\n|---|---|---|---|---|---|---|\n");
			for (Result r : results) {
				md.append("| ").append(r.job().ko()).append(" | ").append(ClassDocProvider.num(r.health())).append(" | ").append(ClassDocProvider.num(r.armor())).append(" / ")
					.append(ClassDocProvider.num(r.toughness())).append(" | ").append(Math.round(r.taken() * 100)).append("% / ").append(Math.round(r.takenMagic() * 100))
					.append("% | ").append(Math.round(r.dodge() * 100)).append("% | ").append(ClassDocProvider.num(r.ehp())).append(" / ")
					.append(ClassDocProvider.num(r.ehpMagic())).append(" | ").append(Math.round(r.ehp() / meanEhp * 100)).append("% |\n");
			}
		}
	}

	private ClassComparison() {
	}
}
