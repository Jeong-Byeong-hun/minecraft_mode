package com.minecraftmode.client.datagen;

import static com.minecraftmode.job.engrave.EngraveStat.*;

import com.minecraftmode.accessory.Accessories;
import com.minecraftmode.accessory.AccessoryEnhance;
import com.minecraftmode.accessory.AccessorySet;
import com.minecraftmode.accessory.AccessorySlot;
import com.minecraftmode.enhance.Enhancement;
import com.minecraftmode.entity.combat.PlayerArmor;
import com.minecraftmode.job.JobClass;
import com.minecraftmode.job.JobData;
import com.minecraftmode.job.JobProgression;
import com.minecraftmode.job.JobStats;
import com.minecraftmode.job.engrave.EngraveTotals;
import com.minecraftmode.job.gear.ArmorPieceDef;
import com.minecraftmode.job.gear.ArmorSetDef;
import com.minecraftmode.job.gear.ClassArmor;
import com.minecraftmode.job.gear.ClassGear;
import com.minecraftmode.job.gear.ClassPassives;
import com.minecraftmode.job.gear.LevelRewards;
import com.minecraftmode.job.gear.StatLine;
import com.minecraftmode.job.skill.CombatHooks;
import com.minecraftmode.job.skill.Skill;
import com.minecraftmode.job.skill.SkillAction;
import com.minecraftmode.job.skill.SkillCaster;
import com.minecraftmode.job.skill.SkillKind;
import com.minecraftmode.job.skill.SkillSupport;
import com.minecraftmode.job.weapon.Archetype;
import com.minecraftmode.job.weapon.BasicAttacks;
import com.minecraftmode.job.weapon.JobWeapons;
import com.minecraftmode.job.weapon.ProjectileStyle;
import com.minecraftmode.job.weapon.WeaponDef;
import com.minecraftmode.talent.TalentTree;
import com.minecraftmode.talent.Talents;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;

/**
 * The "직업 비교" section of CLASSES.md: what each class deals and survives at the last level of every tier, with its own passives,
 * level rewards and melee vitality, the highest armor set it can wear (four pieces: base options, set bonuses, defense) and talents
 * spent for single-target damage. Engravings, random armor options, enhancement, consumables and the support side of skills (boosts,
 * marks, empower, vulnerability) are left out: the player picks them, or every class has them alike. Estimates to compare classes with.
 *
 * <p>{@link #build} is the same estimate for any {@link Loadout} (enhancement, awakening, raid accessories) and {@link Scenario} (what
 * the hits land on): {@code DamageClientGameTest} compares it with what a full-gear character really deals on a training dummy.
 */
public final class ClassComparison {
	/** The last level of each tier: every weapon of the tier is out by then. */
	static final int[] LEVELS = {24, 44, 69, 100};
	/** Seconds of fighting the MP pool plus regeneration has to pay for. */
	public static final double FIGHT_SECONDS = 60.0;
	/** Blocks between an archer and the target (Deadeye, range bonus). */
	static final double SHOT_DISTANCE = 12.0;
	/** Share of a target's life below 30% health (execute) and above 90% (first strike). */
	static final double EXECUTE_SHARE = 0.3;
	static final double FIRST_STRIKE_SHARE = 0.1;
	/** Of five grouped monsters, how many a basic cannonball's blast catches. */
	static final double CANNON_TARGETS = 3.0;

	/**
	 * What the hits land on: the share of them on a target above 90% health (first strike) and below 30% (execute), how far shots fly
	 * (Deadeye, range bonus) and whether it is a boss (boss damage); whether the skills' support side counts ({@link SkillSupport}:
	 * boosts, marks and empowered hits weighed by how long they are up) and whether the target takes effects (vulnerability).
	 */
	public record Scenario(
		double firstStrikeShare, double executeShare, double shotDistance, boolean boss, boolean support, boolean debuffs, double fightSeconds
	) {
		/** A monster fought from full health to death, skills' support side left out, every skill at its steady rate (the class docs). */
		public static final Scenario DOCS = new Scenario(FIRST_STRIKE_SHARE, EXECUTE_SHARE, SHOT_DISTANCE, false, false, true, 0.0);

		/**
		 * A training dummy: it never loses health (first strike always counts, execute never) and, an armor stand, never takes an effect
		 * (no vulnerability); shot at from {@code distance}, with the skills' support side, in a {@link #FIGHT_SECONDS} fight that starts
		 * with every skill ready.
		 */
		public static Scenario dummy(final double distance, final boolean boss) {
			return new Scenario(1.0, 0.0, distance, boss, true, false, FIGHT_SECONDS);
		}

		/** The same target the class docs' way: no support side, every skill at its steady rate. */
		public Scenario steady() {
			return new Scenario(this.firstStrikeShare, this.executeShare, this.shotDistance, this.boss, false, this.debuffs, 0.0);
		}

		/**
		 * Casts a second of a skill that is ready again every {@code period} seconds: one per period at the steady rate, or in a fight
		 * of {@link #fightSeconds} that starts with it ready, every cast that begins before the end (a 50-second ultimate goes off twice
		 * in a minute).
		 */
		double casts(final double period) {
			return this.fightSeconds <= 0.0 ? 1.0 / period : (1.0 + Math.floor((this.fightSeconds - 1.0E-6) / period)) / this.fightSeconds;
		}
	}

	/**
	 * What a weapon's support skills add on average: {@code mark} to every hit's bonus, {@code vulnerable} as a multiplier,
	 * {@code empowered} basic hits a second at +100% (empower and stealth bonuses weighed by the hits they reach), and per skill the
	 * share of its remaining cooldown the weapon's refreshes take off a second ({@code refresh}).
	 */
	record Uplift(double mark, double vulnerable, double empowered, double[] refresh, double[] ownMark) {
		static final Uplift NONE = new Uplift(0.0, 0.0, 0.0, new double[0], new double[0]);

		/** The mark a skill puts on its target before it hits, which its own hits always get (instead of the average). */
		double ownMark(final int slot) {
			return slot < this.ownMark.length ? this.ownMark[slot] : 0.0;
		}

		/**
		 * {@code ticks} of cooldown with refreshes taking {@code k} of what is left off a second: the time left falls by one a second and
		 * by {@code k} of itself, so it runs out after ln(1 + k * cooldown) / k.
		 */
		double cooldownSeconds(final int slot, final int ticks) {
			double seconds = ticks / 20.0;
			double k = slot < this.refresh.length ? this.refresh[slot] : 0.0;
			return k <= 1.0E-9 ? seconds : Math.log(1.0 + k * seconds) / k;
		}
	}

	/**
	 * Enhancement on the weapon and the four armor pieces (level and awakening, {@link Enhancement#lines}) and five raid accessories of
	 * one set at one enhancement level ({@link Accessories#lines}); random options and engravings stay out like in the class docs.
	 */
	public record Loadout(int weapon, int weaponAwaken, int armor, int armorAwaken, @Nullable AccessorySet accessories, int accessoryLevel) {
		/** What the class docs count: no enhancement, no accessories. */
		public static final Loadout BARE = new Loadout(0, 0, 0, 0, null, 0);

		/** The lines this loadout adds to a character wearing {@code set} with any weapon of {@code tier}. */
		List<StatLine> lines(final @Nullable ArmorSetDef set, final @Nullable WeaponDef weapon) {
			List<StatLine> out = new ArrayList<>();
			if (weapon != null) {
				out.addAll(Enhancement.lines(ClassGear.of(weapon), this.weapon, this.weaponAwaken));
			}
			if (set != null) {
				for (ArmorPieceDef piece : ClassArmor.piecesOf(set)) {
					out.addAll(Enhancement.lines(ClassGear.of(piece), this.armor, this.armorAwaken));
				}
			}
			if (this.accessories != null) {
				for (AccessorySlot slot : AccessorySlot.values()) {
					out.add(AccessoryEnhance.scaled(Accessories.def(this.accessories, slot).baseLine(), this.accessoryLevel));
					out.addAll(AccessoryEnhance.bonus(this.accessoryLevel));
				}
				for (ArmorSetDef.SetBonus bonus : this.accessories.bonuses()) {
					if (bonus.pieces() <= AccessorySlot.values().length) {
						out.addAll(bonus.lines());
					}
				}
			}
			return out;
		}
	}

	/**
	 * Damage per second of one weapon on one target ({@code basic}, {@code skill1}) and on five, and the MP its skills want for a
	 * {@link #FIGHT_SECONDS} fight over what it has. {@code chain} is the part of {@code basic} that chain lightning adds on one target.
	 */
	public record Offense(
		double basic, double basic5, double skill1, double skill5, double mpLoad, double chain, boolean shoots, double basicHits, double[] casts
	) {
		public double total1() {
			return this.basic + this.skill1;
		}

		public double total5() {
			return this.basic5 + this.skill5;
		}
	}

	/**
	 * One class at one level with one loadout: the armor set it wears, the talent ranks that hit hardest on one target (node id -> rank),
	 * every stat it ends up with and the estimate of each weapon of its tier.
	 */
	public record Build(
		JobClass job, int level, @Nullable ArmorSetDef set, Map<String, Integer> talents, String talentText, EngraveTotals mods, Map<WeaponDef, Offense> weapons
	) {
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

	/** The highest armor set {@code job} can wear at {@code level} (the first one of that level). */
	public static @Nullable ArmorSetDef bestSet(final JobClass job, final int level) {
		return ClassArmor.sets(job).stream().filter(s -> s.level() <= level).max(Comparator.comparingInt(ArmorSetDef::level)).orElse(null);
	}

	/** The weapons of {@code job}'s tier at {@code level} that are out by then. */
	public static List<WeaponDef> weaponsAt(final JobClass job, final int level) {
		int tier = tierAt(level);
		return JobWeapons.of(job).stream().filter(w -> w.tier() == tier && w.level() <= level).toList();
	}

	/**
	 * The estimate of every weapon of {@code job}'s tier at {@code level}, wearing {@link #bestSet} and {@code loadout}, with talents spent
	 * for the most single-target damage in {@code scenario}.
	 */
	public static Build build(final JobClass job, final int level, final Loadout loadout, final Scenario scenario) {
		int tier = tierAt(level);
		JobData data = new JobData(job, tier, level, 0, 0, Map.of());
		List<WeaponDef> weapons = weaponsAt(job, level);
		ArmorSetDef set = bestSet(job, level);
		List<StatLine> base = new ArrayList<>();
		base.addAll(LevelRewards.of(data));
		base.addAll(ClassPassives.of(data));
		// Rampage (warrior tier 3) as the stacks it holds in a fight
		if (CombatHooks.has(data, JobClass.WARRIOR, 3)) {
			base.add(StatLine.of(DAMAGE, CombatHooks.RAMPAGE_STEP * CombatHooks.RAMPAGE_AVERAGE));
		}
		if (set != null) {
			for (ArmorPieceDef piece : ClassArmor.piecesOf(set)) {
				base.add(piece.baseOption());
				base.addAll(piece.defenseLines());
			}
			for (ArmorSetDef.SetBonus bonus : set.bonuses()) {
				base.addAll(bonus.lines());
			}
		}
		base.addAll(loadout.lines(set, weapons.isEmpty() ? null : weapons.getFirst()));
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
				double score = weapons.stream().mapToDouble(w -> offense(data, w, mods, scenario, Uplift.NONE).total1()).sum();
				if (score > bestScore + 1.0E-9) {
					bestScore = score;
					bestSplit = split;
				}
			}
		}
		EngraveTotals mods = totals(base, branches, bestSplit);
		Map<WeaponDef, Offense> offenses = new LinkedHashMap<>();
		for (WeaponDef w : weapons) {
			Offense plain = offense(data, w, mods, scenario, Uplift.NONE);
			if (scenario.support()) {
				// the support side, weighed by how often the skills are really cast (MP included), then the weapon again with it
				List<StatLine> boosts = new ArrayList<>(base);
				Uplift uplift = uplift(w, plain, scenario, boosts);
				plain = offense(data, w, totals(boosts, branches, bestSplit), scenario, uplift);
			}
			offenses.put(w, plain);
		}
		StringBuilder text = new StringBuilder();
		for (int i = 0; i < branches.size(); i++) {
			if (bestSplit[i] > 0) {
				text.append(text.isEmpty() ? "" : " · ").append(branches.get(i).ko()).append(' ').append(bestSplit[i]);
			}
		}
		return new Build(job, level, set, ranks(branches, bestSplit), text.toString(), mods, offenses);
	}

	static Result evaluate(final JobClass job, final int level) {
		int tier = tierAt(level);
		JobData data = new JobData(job, tier, level, 0, 0, Map.of());
		Build build = build(job, level, Loadout.BARE, Scenario.DOCS);
		ArmorSetDef set = build.set();
		EngraveTotals mods = build.mods();
		double basic = 0.0;
		double skill1 = 0.0;
		double total1 = 0.0;
		double total5 = 0.0;
		double mp = 0.0;
		double best = 0.0;
		String bestWeapon = "";
		for (Map.Entry<WeaponDef, Offense> entry : build.weapons().entrySet()) {
			Offense o = entry.getValue();
			basic += o.basic();
			skill1 += o.skill1();
			total1 += o.total1();
			total5 += o.total5();
			mp += o.mpLoad();
			if (o.total1() > best) {
				best = o.total1();
				bestWeapon = entry.getKey().ko();
			}
		}
		int n = Math.max(1, build.weapons().size());
		String talents = build.talentText();
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
		ranks(branches, split).forEach((id, rank) -> TalentTree.node(id).lines(rank).forEach(l -> builder.add(l.stat(), l.value())));
		return builder.build();
	}

	/** The ranks {@code split} (points per branch) buys: the capstone when the branch can open it, the rest from its first talent on. */
	private static Map<String, Integer> ranks(final List<TalentTree.Branch> branches, final int[] split) {
		Map<String, Integer> out = new LinkedHashMap<>();
		for (int i = 0; i < branches.size(); i++) {
			List<TalentTree.Node> nodes = branches.get(i).nodes();
			int left = split[i];
			// the capstone needs every earlier tier unlocked (4 points per tier), the rest fill from the first talent on
			if (left >= TalentTree.PER_TIER * (TalentTree.TIERS - 1) + 1) {
				out.put(nodes.get(TalentTree.TIERS - 1).id(), 1);
				left--;
			}
			for (int t = 0; t < TalentTree.TIERS - 1 && left > 0; t++) {
				int ranks = Math.min(nodes.get(t).maxRank(), left);
				out.put(nodes.get(t).id(), ranks);
				left -= ranks;
			}
		}
		return out;
	}

	/**
	 * The average of {@code w}'s support steps ({@link SkillAction#supports}) at the cast rates of {@code plain}: each boost is up
	 * {@code min(1, casts a second x seconds)} of the time; boosts of one kind (and marks) do not stack, the stronger counts while up
	 * (they are taken as independent); empower and stealth reach as many basic hits as land while they last; refreshes cut the other
	 * skills' cooldowns. Boost lines go into {@code lines}.
	 */
	private static Uplift uplift(final WeaponDef w, final Offense plain, final Scenario scenario, final List<StatLine> lines) {
		Map<SkillSupport.Kind, List<double[]>> up = new java.util.EnumMap<>(SkillSupport.Kind.class);
		double empowered = 0.0;
		double[] refresh = new double[w.skills().size()];
		double[] ownMark = new double[w.skills().size()];
		for (int i = 0; i < w.skills().size(); i++) {
			double rate = plain.casts()[i];
			for (SkillAction action : w.skills().get(i).actions()) {
				for (SkillSupport support : action.supports()) {
					if (support.kind() == SkillSupport.Kind.VULNERABLE && !scenario.debuffs()) {
						continue;
					}
					if (support.kind() == SkillSupport.Kind.REFRESH) {
						for (int other = 0; other < refresh.length; other++) {
							refresh[other] += other == i ? 0.0 : rate * support.pct() / 100.0;
						}
						continue;
					}
					if (support.kind() == SkillSupport.Kind.MARK) {
						ownMark[i] = Math.max(ownMark[i], support.pct() / 100.0);
					}
					if (support.kind() == SkillSupport.Kind.EMPOWER) {
						empowered += rate * Math.min(support.hits(), plain.basicHits() * support.seconds()) * support.pct() / 100.0;
						continue;
					}
					up.computeIfAbsent(support.kind(), k -> new ArrayList<>()).add(new double[] {support.pct(), Math.min(1.0, rate * support.seconds())});
				}
			}
		}
		Map<SkillSupport.Kind, Double> average = new java.util.EnumMap<>(SkillSupport.Kind.class);
		up.forEach((kind, list) -> {
			list.sort(Comparator.comparingDouble((double[] v) -> -v[0]));
			double value = 0.0;
			double free = 1.0;
			for (double[] v : list) {
				value += v[0] * v[1] * free;
				free *= 1.0 - v[1];
			}
			average.put(kind, value);
		});
		average.forEach((kind, value) -> {
			switch (kind) {
				case DAMAGE -> lines.add(StatLine.of(DAMAGE, (float)(double)value));
				case CRIT_CHANCE -> lines.add(StatLine.of(CRIT_CHANCE, (float)(double)value));
				case CRIT_DAMAGE -> lines.add(StatLine.of(CRIT_DAMAGE, (float)(double)value));
				default -> {
				}
			}
		});
		return new Uplift(average.getOrDefault(SkillSupport.Kind.MARK, 0.0) / 100.0, average.getOrDefault(SkillSupport.Kind.VULNERABLE, 0.0) / 100.0, empowered,
			refresh, ownMark);
	}

	static Offense offense(final JobData data, final WeaponDef w, final EngraveTotals mods, final Scenario scenario, final Uplift uplift) {
		Archetype type = w.archetype();
		double critChance = Math.min(1.0, mods.fraction(CRIT_CHANCE) + (CombatHooks.has(data, JobClass.ROGUE, 2) ? 0.15 : 0.0));
		double crit = 1.0 + critChance * (0.5 + mods.fraction(CRIT_DAMAGE) + selection(data, w, mods));
		double first = mods.fraction(FIRST_STRIKE) * scenario.firstStrikeShare();
		double execute = mods.fraction(EXECUTE) * scenario.executeShare();
		// every hit (CombatHooks.outgoing): damage dealt, boss damage and first strike join the hit's own bonuses, final damage multiplies last
		double every = mods.fraction(DAMAGE) + (scenario.boss() ? mods.fraction(BOSS_DAMAGE) : 0.0) + first + uplift.mark();
		// vulnerability multiplies whatever the target takes (CombatHooks.modifyIncoming)
		double last = (1.0 + mods.fraction(FINAL_DAMAGE)) * (1.0 + uplift.vulnerable());
		// melee: fully charged swings, which land through the hurt cooldown (CombatHooks)
		double swings = type.attackSpeed() * (1.0 + mods.fraction(ATTACK_SPEED));
		double meleeLanded = w.attackDamage() * (1.0 + mods.fraction(BASIC_DAMAGE) + (CombatHooks.has(data, JobClass.WARRIOR, 2) ? CombatHooks.WARRIOR_MELEE : 0.0) + execute + every)
			* crit * last;
		double meleeHit = meleeLanded * (1.0 + mods.fraction(DOUBLE_STRIKE));
		double melee = meleeHit * swings;
		double melee5 = melee * (1.0 + 4 * 0.5 * Math.min(1.0, mods.get(SPLASH)));
		// shots: one per draw or reload, the extra ones of a fan only reach the other monsters of a group (BasicAttacks.Fan; the stat
		// stops at its cap)
		double shot = 0.0;
		double shot5 = 0.0;
		double shotRate = 0.0;
		double shotLanded = 0.0;
		double shotBase = 0.0;
		if (type.isRanged()) {
			int ticks = Math.max(type.mode() == Archetype.BasicMode.DRAW ? 5 : 4, Math.round(type.shotCooldown() / (1.0F + mods.fraction(DRAW_SPEED))));
			// a bow is loosed when the use key is let go and drawn again when it is pressed, the tick after at the earliest
			shotRate = 20.0 / (ticks + (type.mode() == Archetype.BasicMode.DRAW ? 1 : 0));
			shotBase = w.shotDamage() * (1.0 + BasicAttacks.shotBonus(data, mods)) * (CombatHooks.has(data, JobClass.ARCHER, 1) ? 1.0 + BasicAttacks.EAGLE_EYE : 1.0);
			shotLanded = shotBase * (1.0 + execute + distanceBonus(data, mods, scenario) + every) * crit * last;
			shot = shotLanded * (CombatHooks.has(data, JobClass.ARCHER, 4) ? 1.0 + BasicAttacks.BABYLON_CHANCE * 2 : 1.0) * shotRate;
			double extra = mods.get(EXTRA_SHOT) * 0.5;
			shot5 = shot * (type.shot() == ProjectileStyle.CANNONBALL ? CANNON_TARGETS : 1.0 + extra);
		}
		boolean shoots = shot > melee;
		double basicHits = shoots ? shotRate : swings;
		// chain lightning (CombatHooks.chainLightning): a basic hit or shot that procs strikes its own target and two more for half of what it
		// dealt; the bolts are hits of their own (no crit), so damage dealt, boss damage, first strike and final damage count again
		double chainHit = Math.min(0.5, mods.fraction(CHAIN_LIGHTNING)) * 0.5 * (1.0 + every) * last;
		double chain = (shoots ? shot : meleeLanded * swings) * chainHit;
		// empowered and stealth hits: the bonus joins one basic hit's pool (a double strike repeats it)
		double empowered = uplift.empowered() * (shoots ? shotBase : w.attackDamage() * (1.0 + mods.fraction(DOUBLE_STRIKE))) * crit * last;
		double basic = (shoots ? shot : melee) + chain + empowered;
		double basic5 = (shoots ? shot5 : melee5) + chain * 3 + empowered;
		// skills: cast on cooldown; critical basic hits wind cooldowns down (crit refund); an echo skips the cooldown
		double skillPower = w.skillPower() * (1.0 + mods.fraction(SKILL_DAMAGE) + (CombatHooks.has(data, JobClass.MAGE, 3) ? SkillCaster.ARCANE_MASTERY : 0.0))
			* (1.0 + every) * crit * last;
		double speedUp = 1.0 + basicHits * critChance * mods.get(CRIT_REFUND);
		double echo = mods.fraction(ECHO);
		record Use(int slot, double casts, double dps1, double dps5, double mpPerSecond) {
		}
		List<Use> uses = new ArrayList<>();
		for (int i = 0; i < w.skills().size(); i++) {
			Skill skill = w.skills().get(i);
			double casts = scenario.casts(uplift.cooldownSeconds(i, SkillCaster.cooldown(data, skill, mods)) * (1.0 - echo) / speedUp);
			// a skill that marks its target before it hits gets its own mark on every hit, not the average
			double own = (1.0 + every - uplift.mark() + Math.max(uplift.mark(), uplift.ownMark(i))) / (1.0 + every);
			uses.add(new Use(i, casts, skill.estimate(1) * skillPower * own * casts, skill.estimate(5) * skillPower * own * casts,
				SkillCaster.manaCost(skill, mods) * casts));
		}
		double[] rates = new double[w.skills().size()];
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
			rates[u.slot()] = u.casts() * share;
			skill1 += u.dps1() * share;
			skill5 += u.dps5() * share;
			wanted += need;
		}
		return new Offense(basic, basic5, skill1, skill5, wanted / available, chain, shoots, basicHits, rates);
	}

	/** Sword of Selection (warrior tier 4): its critical damage after an ultimate, averaged over the weapon's ultimate cooldown. */
	private static double selection(final JobData data, final WeaponDef w, final EngraveTotals mods) {
		if (!CombatHooks.has(data, JobClass.WARRIOR, 4)) {
			return 0.0;
		}
		for (Skill skill : w.skills()) {
			if (skill.kind() == SkillKind.ULTIMATE) {
				double seconds = Math.max(1.0, SkillCaster.cooldown(data, skill, mods) / 20.0);
				return CombatHooks.SELECTION_CRIT_DAMAGE / 100.0 * Math.min(1.0, CombatHooks.SELECTION_TICKS / 20.0 / seconds);
			}
		}
		return 0.0;
	}

	/** Deadeye and the range bonus at the scenario's shot distance. */
	private static double distanceBonus(final JobData data, final EngraveTotals mods, final Scenario scenario) {
		return mods.fraction(RANGE_BONUS) * scenario.shotDistance() / 10.0
			+ (CombatHooks.has(data, JobClass.ARCHER, 3) ? Math.min(0.40, 0.02 * (int)(scenario.shotDistance() / 4.0)) : 0.0);
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
		md.append("- 뺀 것: 각인, 방어구 추가 옵션, 강화·각성, 소모품, 스킬의 지원 효과(버프·디버프·표식·강화 타격), 상태 이상 피해, 뒤치기 보너스. 광폭화(전사 3차)는 전투 중 평균 ").append(CombatHooks.RAMPAGE_AVERAGE).append("중첩, 선정의 검(전사 4차)은 궁극기 대기 시간에 대한 지속 비율로 셉니다.\n");
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
