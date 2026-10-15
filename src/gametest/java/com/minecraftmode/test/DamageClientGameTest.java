package com.minecraftmode.test;

import com.minecraftmode.MinecraftMode;
import com.minecraftmode.accessory.Accessories;
import com.minecraftmode.accessory.AccessoryEnhance;
import com.minecraftmode.accessory.AccessorySet;
import com.minecraftmode.accessory.AccessorySlot;
import com.minecraftmode.client.datagen.ClassComparison;
import com.minecraftmode.enhance.Enhancement;
import com.minecraftmode.entity.TrainingDummy;
import com.minecraftmode.job.JobClass;
import com.minecraftmode.job.JobData;
import com.minecraftmode.job.JobProgression;
import com.minecraftmode.job.JobStats;
import com.minecraftmode.job.engrave.EngraveStat;
import com.minecraftmode.job.engrave.EngraveTotals;
import com.minecraftmode.job.gear.ArmorPieceDef;
import com.minecraftmode.job.gear.ClassArmor;
import com.minecraftmode.job.gear.GearRolls;
import com.minecraftmode.job.gear.GearStats;
import com.minecraftmode.job.skill.Actions;
import com.minecraftmode.job.skill.CombatHooks;
import com.minecraftmode.job.skill.CombatState;
import com.minecraftmode.job.skill.Skill;
import com.minecraftmode.job.skill.SkillAction;
import com.minecraftmode.job.skill.SkillCaster;
import com.minecraftmode.job.skill.SkillSupport;
import com.minecraftmode.job.weapon.Archetype;
import com.minecraftmode.job.weapon.BasicAttacks;
import com.minecraftmode.job.weapon.JobWeaponItem;
import com.minecraftmode.job.weapon.JobWeapons;
import com.minecraftmode.job.weapon.WeaponDef;
import com.minecraftmode.raid.RaidBosses;
import com.minecraftmode.registry.ModAttachments;
import com.minecraftmode.registry.ModDataComponents;
import com.minecraftmode.registry.ModEntities;
import com.minecraftmode.talent.Talents;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerConnection;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.client.CloudStatus;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ParticleStatus;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * What a full-gear character really deals (manual: {@code ./gradlew runClientGameTest -Ptests=damage}). Every class at Lv {@value #LEVEL}
 * holds each tier 4 weapon at +15 ✦5, wears its best armor set at +15 ✦5 and the five Aethryx accessories at +10 (no random options or
 * engravings, like the class docs), with the talents {@link ClassComparison} spends for one target, and fights a training dummy:
 * <ul>
 * <li>each damaging skill alone, {@value #PROBE_CASTS} times (damage of one cast);</li>
 * <li>a {@link ClassComparison#FIGHT_SECONDS}-second fight the way a player plays it: a fully charged swing whenever the swing timer is
 * full (never from behind), or the next full draw / reload as soon as the last shot is out, and the next ready skill it can pay for
 * every tick (buffs first, then the hardest hitting; a move that neither hurts nor helps is left out), stepping where the skill reaches
 * the dummy from ({@link #distanceFor}) and back once a movement skill is over.</li>
 * </ul>
 * The dummy never loses health, so the estimate it is compared with is {@link ClassComparison.Scenario#dummy}: first strike on every
 * hit, never execute. The report goes to {@code damage-report.md} (and the raw numbers to {@code damage-report.tsv}) in the run
 * directory; docs/DAMAGE.md is a copy of it.
 */
public class DamageClientGameTest implements FabricClientGameTest {
	static final int LEVEL = 100;
	static final AccessorySet ACCESSORIES = Accessories.setOf(RaidBosses.AETHRYX);
	static final ClassComparison.Loadout LOADOUT = new ClassComparison.Loadout(Enhancement.MAX, Enhancement.MAX_AWAKEN, Enhancement.MAX,
		Enhancement.MAX_AWAKEN, ACCESSORIES, AccessoryEnhance.MAX);
	/** Where melee weapons fight from (centre to centre): inside the shortest reach (knuckles, 2 blocks). */
	static final double MELEE_DISTANCE = 1.8;
	/** Where shooters fight from: the class docs' shot distance. */
	static final double RANGED_DISTANCE = 12.0;
	/** Where skills that do not reach from further are cast from. */
	static final double CLOSE_DISTANCE = 2.5;
	/** Where leaps start: they carry the caster about this far before they slam. */
	static final double LEAP_DISTANCE = 6.0;
	static final int FIGHT_TICKS = (int)Math.round(ClassComparison.FIGHT_SECONDS * 20);
	static final int PROBE_CASTS = 3;
	/** A probe cast is over once nothing has hit for this long (or after {@link #PROBE_MAX_TICKS}). */
	static final int PROBE_QUIET_TICKS = 25;
	static final int PROBE_MAX_TICKS = 200;
	/** Server (and with {@link TickRateCap}, client) tick rate while measuring: the fights are tick-exact, so they do not need real time. */
	static final int TICK_RATE = Integer.getInteger("minecraft_mode.damage.tickrate", 100);
	/** Only these classes (comma separated ids), for quick reruns; empty = all. */
	static final String ONLY = System.getProperty("minecraft_mode.damage.classes", "");
	/** Only these weapons (comma separated ids) of those classes; empty = all. */
	static final String WEAPONS = System.getProperty("minecraft_mode.damage.weapons", "");
	/**
	 * damage-report.tsv files of earlier runs (comma separated, e.g. one per class shard): no fighting, the report is written again from
	 * their measurements and today's estimates (for changes to the estimate only; a change to combat needs a new run).
	 */
	static final String REUSE = System.getProperty("minecraft_mode.damage.reuse", "");
	static final Vec3 DUMMY = new Vec3(0.5, -60.0, -20.5);
	private static final String DUMMY_TAG = "damage_dummy";
	/** Strikes around the caster, which miss after a dash or blink carried it past the target. */
	private static final List<String> AROUND = List.of("nova", "slash", "zone");
	/** Action keys whose first number is how far they reach (cones, novas, dashes, target areas). */
	private static final List<String> REACHING = List.of("slash", "nova", "dash_hit", "shadowstep", "beam", "strike", "lightning", "chain", "rain",
		"execute", "mark", "zone");

	private static boolean registered;
	private static @Nullable Fight fight;

	/** One weapon's numbers: the estimate, the probes and the fight. */
	record Row(
		JobClass job, WeaponDef weapon, ClassComparison.Offense estimate, ClassComparison.Offense plain, boolean shoots, double basic, double skill, double chain, double other, double neutral,
		int hits, int crits, int swings, int shots, int[] casts, double[] castEstimate, double[] castMeasured, int[] castCount, int fightTicks, int minMana
	) {
		double total() {
			return this.basic + this.skill + this.chain + this.other;
		}

		double dps(final double damage) {
			return damage / (this.fightTicks / 20.0);
		}

		double ratio() {
			return this.dps(this.total()) / this.estimate.total1();
		}

		/** The estimate with the skills cast as often as they really were in the fight (one cast each as the probes estimate it). */
		double atCasts() {
			double skills = 0.0;
			for (int i = 0; i < this.casts.length; i++) {
				skills += this.casts[i] * this.castEstimate[i];
			}
			return this.estimate.basic() + this.dps(skills);
		}
	}

	/** One class's build and the stats the game gave it that the estimate did not (or the other way round). */
	record ClassNote(JobClass job, ClassComparison.Build build, List<String> statDiffs) {
	}

	/** The running probe or fight (server thread). */
	private static final class Fight {
		final ServerPlayer player;
		final TrainingDummy dummy;
		final WeaponDef weapon;
		final boolean shoots;
		final Vec3 base;
		final int[] order;
		final boolean rotation;
		boolean counting;
		double basic;
		double skill;
		double chain;
		double other;
		/** The damage with every critical hit's luck taken out: each hit that could crit counted at its expected crit factor. */
		double neutral;
		int hits;
		int crits;
		int swings;
		int shots;
		final int[] casts;
		long lastDamage = Long.MIN_VALUE / 2;
		long castAt = Long.MIN_VALUE / 2;
		/** Where a skill took the player on purpose (null = the weapon's spot) and until when. */
		@Nullable Vec3 spot;
		long spotUntil;
		long drawStart = -1;
		int minMana = Integer.MAX_VALUE;

		Fight(final ServerPlayer player, final TrainingDummy dummy, final WeaponDef weapon, final boolean shoots, final boolean rotation) {
			this.player = player;
			this.dummy = dummy;
			this.weapon = weapon;
			this.shoots = shoots;
			this.base = spotAt(shoots ? RANGED_DISTANCE : MELEE_DISTANCE);
			this.rotation = rotation;
			this.casts = new int[weapon.skills().size()];
			List<Integer> slots = new ArrayList<>();
			for (int i = 0; i < weapon.skills().size(); i++) {
				// a move that neither hurts nor helps the damage only costs time on a dummy: a player would not use it
				Skill skill = weapon.skills().get(i);
				boolean moves = skill.actions().stream().anyMatch(SkillAction::moves);
				boolean helps = skill.estimate(1) > 0.0 || skill.actions().stream().anyMatch(a -> !a.supports().isEmpty());
				if (!moves || helps) {
					slots.add(i);
				}
			}
			// buffs first, then the hardest hitting
			slots.sort(Comparator.comparing((Integer i) -> weapon.skills().get(i).estimate(1) > 0.0)
				.thenComparingDouble(i -> -weapon.skills().get(i).estimate(1)));
			this.order = slots.stream().mapToInt(Integer::intValue).toArray();
		}

		double total() {
			return this.basic + this.skill + this.chain + this.other;
		}
	}

	@Override
	public void runTest(final ClientGameTestContext context) {
		if (TestSelection.skip(this)) {
			return;
		}
		if (!registered) {
			registered = true;
			// before CombatHooks' own listener, which clears the hit's crit flag
			Identifier early = MinecraftMode.id("damage_meter");
			ServerLivingEntityEvents.AFTER_DAMAGE.addPhaseOrdering(early, Event.DEFAULT_PHASE);
			ServerLivingEntityEvents.AFTER_DAMAGE.register(early, DamageClientGameTest::onDamage);
			ServerTickEvents.END_SERVER_TICK.register(DamageClientGameTest::tick);
		}
		List<Row> rows = new ArrayList<>();
		List<ClassNote> notes = new ArrayList<>();
		Map<String, Map<String, String>> earlier = new LinkedHashMap<>();
		for (String file : REUSE.isBlank() ? new String[0] : REUSE.split(",")) {
			earlier.putAll(readTsv(Path.of(file.trim())));
		}
		long started = System.nanoTime();
		try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
			TestServerConnection connection = singleplayer.getConnection();
			TestServerContext server = singleplayer.getServer();
			connection.waitForChunksRender();
			// nothing here is looked at: a small, plain picture keeps the software renderer (and with it every tick) quick
			context.runOnClient(minecraft -> {
				minecraft.options.renderDistance().set(2);
				minecraft.options.particles().set(ParticleStatus.MINIMAL);
				minecraft.options.cloudStatus().set(CloudStatus.OFF);
				minecraft.options.entityShadows().set(false);
				minecraft.getWindow().setWindowed(320, 240);
			});
			server.runCommand("difficulty normal");
			server.runCommand("gamerule send_command_feedback false");
			server.runCommand("gamerule spawn_mobs false");
			server.runCommand("time set noon");
			server.runCommand("weather clear");
			server.runCommand("gamemode survival @p");
			server.runCommand("tp @p 0.5 -60 -10.5 180 0");
			context.waitTicks(5);
			server.runOnServer(s -> {
				ServerPlayer player = connection.getServerPlayer();
				TrainingDummy dummy = ModEntities.TRAINING_DUMMY.create(player.level(), EntitySpawnReason.COMMAND);
				require(dummy != null, "could not create a training dummy");
				dummy.snapTo(DUMMY.x, DUMMY.y, DUMMY.z, 0.0F, 0.0F);
				dummy.setYBodyRot(0.0F);
				dummy.setYHeadRot(0.0F);
				dummy.addTag(DUMMY_TAG);
				player.level().addFreshEntity(dummy);
			});
			TickRateCap.lifted = true;
			server.runCommand("tick rate " + TICK_RATE);
			context.waitTicks(10);
			for (JobClass job : JobClass.PLAYABLE) {
				if (!ONLY.isBlank() && !List.of(ONLY.toLowerCase(Locale.ROOT).split(",")).contains(job.id())) {
					continue;
				}
				ClassComparison.Scenario scenario = ClassComparison.Scenario.dummy(RANGED_DISTANCE, false);
				ClassComparison.Build build = ClassComparison.build(job, LEVEL, LOADOUT, scenario);
				ClassComparison.Build plain = ClassComparison.build(job, LEVEL, LOADOUT, scenario.steady());
				List<String> diffs = null;
				for (WeaponDef weapon : build.weapons().keySet()) {
					if (!WEAPONS.isBlank() && !List.of(WEAPONS.split(",")).contains(weapon.id())) {
						continue;
					}
					Map<String, String> before = earlier.get(job.id() + "/" + weapon.id());
					if (!REUSE.isBlank() && before == null) {
						continue;
					}
					Row row = before == null ? measure(context, server, connection, build, plain.weapons().get(weapon), weapon)
						: reuse(server, connection, build, plain.weapons().get(weapon), weapon, before);
					rows.add(row);
					if (diffs == null) {
						diffs = server.computeOnServer(s -> statDiffs(connection.getServerPlayer(), build.mods()));
					}
					MinecraftMode.LOGGER.info("[damage] {} {}: estimate {} measured {} ({}%)", job.id(), weapon.id(), fmt(row.estimate().total1()),
						fmt(row.dps(row.total())), Math.round(row.ratio() * 100));
					// the raw numbers so far, so a crash halfway keeps them (REUSE writes the report from them)
					writeTsv(rows);
				}
				notes.add(new ClassNote(job, build, diffs == null ? List.of() : diffs));
			}
			server.runCommand("tick rate 20");
			TickRateCap.lifted = false;
			context.runOnClient(minecraft -> {
				minecraft.options.renderDistance().set(5);
				minecraft.options.particles().set(ParticleStatus.ALL);
				minecraft.getWindow().setWindowed(854, 480);
			});
			context.waitTicks(5);
			WorldClose.prepare(context, server);
		}
		double minutes = (System.nanoTime() - started) / 6.0E10;
		write(rows, notes, minutes);
	}

	// ------------------------------------------------------------------ one weapon

	private static Row measure(
		final ClientGameTestContext context, final TestServerContext server, final TestServerConnection connection, final ClassComparison.Build build,
		final ClassComparison.Offense plain, final WeaponDef weapon
	) {
		ClassComparison.Offense estimate = build.weapons().get(weapon);
		boolean shoots = estimate.shoots();
		server.runOnServer(s -> equip(connection.getServerPlayer(), build, weapon));
		context.waitTicks(5);
		// each damaging skill alone
		int skills = weapon.skills().size();
		double[] castEstimate = new double[skills];
		double[] castMeasured = new double[skills];
		int[] castCount = new int[skills];
		for (int slot = 0; slot < skills; slot++) {
			Skill skill = weapon.skills().get(slot);
			if (skill.estimate(1) <= 0.0) {
				continue;
			}
			castEstimate[slot] = server.computeOnServer(s -> castEstimate(connection.getServerPlayer(), weapon, skill));
			double sum = 0.0;
			int done = 0;
			for (int rep = 0; rep < PROBE_CASTS; rep++) {
				final int at = slot;
				boolean cast = server.computeOnServer(s -> {
					ServerPlayer player = connection.getServerPlayer();
					Fight probe = new Fight(player, dummy(player), weapon, shoots, false);
					reset(player);
					place(player, spotAt(distanceFor(skill, probe.base.distanceTo(DUMMY))), probe.dummy);
					probe.counting = true;
					fight = probe;
					SkillCaster.Result result = SkillCaster.tryCast(player, at);
					probe.castAt = player.level().getGameTime();
					return result == SkillCaster.Result.OK;
				});
				if (!cast) {
					MinecraftMode.LOGGER.warn("[damage] {} could not be cast", skill.id());
					break;
				}
				int waited = 0;
				while (waited < PROBE_MAX_TICKS) {
					context.waitTicks(5);
					waited += 5;
					boolean quiet = server.computeOnServer(s -> {
						ServerPlayer player = connection.getServerPlayer();
						long now = player.level().getGameTime();
						Fight probe = fight;
						// a leap only slams once the caster lands, which can take longer than the quiet time
						return probe != null && player.onGround() && now - probe.castAt >= PROBE_QUIET_TICKS && now - probe.lastDamage >= PROBE_QUIET_TICKS;
					});
					if (quiet) {
						break;
					}
				}
				sum += server.computeOnServer(s -> {
					Fight probe = fight;
					fight = null;
					return probe == null ? 0.0 : probe.neutral;
				});
				done++;
				clearField(server);
				context.waitTicks(2);
			}
			castMeasured[slot] = done == 0 ? 0.0 : sum / done;
			castCount[slot] = done;
		}
		// the fight
		server.runOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			reset(player);
			Fight run = new Fight(player, dummy(player), weapon, shoots, true);
			place(player, run.base, run.dummy);
			run.counting = true;
			fight = run;
		});
		context.waitTicks(FIGHT_TICKS);
		Fight done = server.computeOnServer(s -> {
			Fight run = fight;
			fight = null;
			return run;
		});
		require(done != null, "the fight with " + weapon.id() + " went missing");
		clearField(server);
		context.waitTicks(40);
		clearField(server);
		return new Row(build.job(), weapon, estimate, plain, shoots, done.basic, done.skill, done.chain, done.other, done.neutral, done.hits, done.crits, done.swings,
			done.shots, done.casts.clone(), castEstimate, castMeasured, castCount, FIGHT_TICKS, done.minMana);
	}

	/** {@code weapon}'s row from an earlier run's measurements ({@link #REUSE}), with today's estimates. */
	private static Row reuse(
		final TestServerContext server, final TestServerConnection connection, final ClassComparison.Build build, final ClassComparison.Offense plain,
		final WeaponDef weapon, final Map<String, String> m
	) {
		server.runOnServer(s -> equip(connection.getServerPlayer(), build, weapon));
		int skills = weapon.skills().size();
		double[] castEstimate = new double[skills];
		for (int slot = 0; slot < skills; slot++) {
			Skill skill = weapon.skills().get(slot);
			if (skill.estimate(1) > 0.0) {
				castEstimate[slot] = server.computeOnServer(s -> castEstimate(connection.getServerPlayer(), weapon, skill));
			}
		}
		int ticks = m.containsKey("fight_ticks") ? Integer.parseInt(m.get("fight_ticks")) : FIGHT_TICKS;
		double seconds = ticks / 20.0;
		java.util.function.Function<String, Double> total = key -> m.containsKey(key + "_total") ? Double.parseDouble(m.get(key + "_total"))
			: Double.parseDouble(m.get(key)) * seconds;
		double[] castMeasured = numbers(m.get("cast_measured_each"));
		int[] castCount = new int[skills];
		double[] counted = m.containsKey("cast_count") ? numbers(m.get("cast_count")) : null;
		for (int i = 0; i < skills; i++) {
			castCount[i] = counted != null ? (int)counted[i] : castMeasured[i] > 0.0 ? PROBE_CASTS : 0;
		}
		int[] casts = java.util.Arrays.stream(numbers(m.get("casts"))).mapToInt(d -> (int)d).toArray();
		return new Row(build.job(), weapon, build.weapons().get(weapon), plain, Boolean.parseBoolean(m.get("shoots")), total.apply("basic"), total.apply("skill"),
			total.apply("chain"), total.apply("other"), total.apply("neutral"), Integer.parseInt(m.get("hits")), Integer.parseInt(m.get("crits")),
			Integer.parseInt(m.get("swings")), Integer.parseInt(m.get("shots")), casts, castEstimate, castMeasured, castCount, ticks, Integer.parseInt(m.get("min_mp")));
	}

	/** "[1.5, 2.0]" -> {1.5, 2.0}. */
	private static double[] numbers(final String list) {
		String inner = list.trim().replace("[", "").replace("]", "").trim();
		return inner.isEmpty() ? new double[0] : java.util.Arrays.stream(inner.split(",")).mapToDouble(v -> Double.parseDouble(v.trim())).toArray();
	}

	/** An earlier damage-report.tsv: "class/weapon" -> column -> value. */
	private static Map<String, Map<String, String>> readTsv(final Path file) {
		try {
			List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
			String[] header = lines.getFirst().split("\t");
			Map<String, Map<String, String>> out = new LinkedHashMap<>();
			for (String line : lines.subList(1, lines.size())) {
				if (line.isBlank()) {
					continue;
				}
				String[] cells = line.split("\t");
				Map<String, String> row = new LinkedHashMap<>();
				for (int i = 0; i < header.length && i < cells.length; i++) {
					row.put(header[i], cells[i]);
				}
				out.put(row.get("class") + "/" + row.get("weapon"), row);
			}
			return out;
		} catch (IOException e) {
			throw new IllegalStateException("could not read " + file, e);
		}
	}

	/** Lv 100, the class's talents, best armor set at +15 ✦5, Aethryx accessories at +10 and {@code weapon} at +15 ✦5; nothing rolled or engraved. */
	private static void equip(final ServerPlayer player, final ClassComparison.Build build, final WeaponDef weapon) {
		JobClass job = build.job();
		JobProgression.set(player, JobProgression.get(player).withJob(job, 4).withProgress(LEVEL, 0).withCooldowns(Map.of()));
		player.setAttached(ModAttachments.TALENTS, new Talents.TalentData(job.id(), build.talents()));
		require(build.set() != null, job.id() + " has no armor set at Lv " + LEVEL);
		for (ArmorPieceDef piece : ClassArmor.piecesOf(build.set())) {
			ItemStack stack = new ItemStack(ClassArmor.item(piece));
			stack.set(ModDataComponents.ENHANCEMENT, new Enhancement(LOADOUT.armor(), 0, LOADOUT.armorAwaken()));
			stack.set(ModDataComponents.GEAR_ROLLS, GearRolls.EMPTY);
			player.setItemSlot(piece.slot().equipmentSlot(), stack);
		}
		List<ItemStack> worn = new ArrayList<>();
		for (AccessorySlot slot : AccessorySlot.values()) {
			ItemStack stack = new ItemStack(Accessories.item(Accessories.def(ACCESSORIES, slot)));
			stack.set(ModDataComponents.ENHANCEMENT, new Enhancement(LOADOUT.accessoryLevel(), 0, 0));
			stack.set(ModDataComponents.GEAR_ROLLS, GearRolls.EMPTY);
			worn.add(stack);
		}
		Accessories.setWorn(player, worn);
		ItemStack hand = new ItemStack(JobWeapons.item(weapon));
		hand.set(ModDataComponents.ENHANCEMENT, new Enhancement(LOADOUT.weapon(), 0, LOADOUT.weaponAwaken()));
		player.setItemInHand(InteractionHand.MAIN_HAND, hand);
		player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
		GearStats.invalidate(player);
		JobStats.refresh(player);
		reset(player);
	}

	/** Full health, MP and food, no cooldowns, effects or skill states: every probe and fight starts the same. */
	private static void reset(final ServerPlayer player) {
		GearStats.invalidate(player);
		JobStats.refresh(player);
		player.removeAllEffects();
		CombatState.forget(player);
		player.stopUsingItem();
		JobProgression.set(player, JobProgression.get(player).withCooldowns(Map.of()).withMana(JobStats.maxMana(player)));
		player.setHealth(player.getMaxHealth());
		player.getFoodData().setFoodLevel(20);
		player.getFoodData().setSaturation(20.0F);
		player.resetAttackStrengthTicker();
	}

	/** Skill summons and projectiles left over from the last probe or fight. */
	private static void clearField(final TestServerContext server) {
		server.runCommand("kill @e[type=!minecraft:player,tag=!" + DUMMY_TAG + "]");
	}

	private static TrainingDummy dummy(final ServerPlayer player) {
		List<TrainingDummy> found = player.level().getEntitiesOfClass(TrainingDummy.class, new net.minecraft.world.phys.AABB(DUMMY, DUMMY).inflate(2.0),
			d -> d.entityTags().contains(DUMMY_TAG));
		require(!found.isEmpty(), "the training dummy is gone");
		TrainingDummy dummy = found.getFirst();
		dummy.snapTo(DUMMY.x, DUMMY.y, DUMMY.z, 0.0F, 0.0F);
		return dummy;
	}

	/** {@code distance} blocks south of the dummy, on the same line. */
	static Vec3 spotAt(final double distance) {
		return DUMMY.add(0.0, 0.0, distance);
	}

	/** Puts the player at {@code pos} looking at the dummy's middle. */
	private static void place(final ServerPlayer player, final Vec3 pos, final TrainingDummy dummy) {
		Vec3 eye = pos.add(0.0, player.getEyeHeight(), 0.0);
		Vec3 to = dummy.getBoundingBox().getCenter().subtract(eye);
		float yaw = (float)(Mth.atan2(to.z, to.x) * Mth.RAD_TO_DEG) - 90.0F;
		float pitch = (float)(-(Mth.atan2(to.y, Math.sqrt(to.x * to.x + to.z * to.z)) * Mth.RAD_TO_DEG));
		player.connection.teleport(pos.x, pos.y, pos.z, yaw, pitch);
		player.setYHeadRot(yaw);
		player.setDeltaMovement(Vec3.ZERO);
		player.resetFallDistance();
	}

	/**
	 * How far from the dummy {@code skill} is cast. Moves before its first strike carry the caster forward, so it starts that far out:
	 * a strike around the caster lands on the dummy, a dash strike passes through it halfway. Otherwise leaps start
	 * {@link #LEAP_DISTANCE} out (they carry the caster about that far), skills that do not reach {@code from} (cones, novas, short
	 * dashes and target areas) start {@link #CLOSE_DISTANCE} out, and the rest from where the caster stands.
	 */
	static double distanceFor(final Skill skill, final double from) {
		double reach = Double.MAX_VALUE;
		boolean leap = false;
		double travel = 0.0;
		for (SkillAction action : skill.actions()) {
			List<double[]> found = new ArrayList<>();
			List<String> keys = new ArrayList<>();
			scan(action.describe(), keys, found);
			for (int i = 0; i < keys.size(); i++) {
				String key = keys.get(i);
				double first = found.get(i).length > 0 ? found.get(i)[0] : 0.0;
				if (key.equals("blink") || key.equals("dash")) {
					travel += first;
					continue;
				}
				if (key.equals("dash_hit")) {
					if (travel > 0.0) {
						return Math.max(CLOSE_DISTANCE, travel + first / 2.0);
					}
					travel += first;
				} else if (travel > 0.0 && action.damages() && AROUND.contains(key)) {
					return Math.max(CLOSE_DISTANCE, travel);
				}
				if (!action.damages()) {
					continue;
				}
				if (key.equals("leap")) {
					leap = true;
				} else if (REACHING.contains(key) && found.get(i).length > 0) {
					reach = Math.min(reach, first);
				}
			}
		}
		if (leap) {
			return LEAP_DISTANCE;
		}
		return reach < from + 0.3 ? Math.min(from, CLOSE_DISTANCE) : from;
	}

	/** Whether one of {@code skill}'s steps is an {@code key} action ({@code Actions} lang key). */
	static boolean describes(final Skill skill, final String key) {
		for (SkillAction action : skill.actions()) {
			List<String> keys = new ArrayList<>();
			scan(action.describe(), keys, new ArrayList<>());
			if (keys.contains(key)) {
				return true;
			}
		}
		return false;
	}

	/** Every skill action key in {@code text} (wrappers carry the wrapped step as an argument) with its leading number. */
	private static void scan(final Component text, final List<String> keys, final List<double[]> numbers) {
		if (text.getContents() instanceof TranslatableContents t && t.getKey().startsWith("skill_action.minecraft_mode.")) {
			keys.add(t.getKey().substring("skill_action.minecraft_mode.".length()));
			double[] first = new double[0];
			for (Object arg : t.getArgs()) {
				if (arg instanceof String s) {
					try {
						first = new double[] {Double.parseDouble(s)};
						break;
					} catch (NumberFormatException e) {
						// not a number
					}
				}
			}
			numbers.add(first);
			for (Object arg : t.getArgs()) {
				if (arg instanceof Component inner) {
					scan(inner, keys, numbers);
				}
			}
		}
		for (Component sibling : text.getSiblings()) {
			scan(sibling, keys, numbers);
		}
	}

	/** One cast of {@code skill} on one target as the estimate sees it: its multiplier sum x the cast power, bonuses and expected crits. */
	private static double castEstimate(final ServerPlayer player, final WeaponDef weapon, final Skill skill) {
		JobData data = JobProgression.get(player);
		EngraveTotals mods = GearStats.of(player);
		double critChance = Math.min(1.0, mods.fraction(EngraveStat.CRIT_CHANCE) + (CombatHooks.has(data, JobClass.ROGUE, 2) ? 0.15 : 0.0));
		double crit = 1.0 + critChance * (0.5 + mods.fraction(EngraveStat.CRIT_DAMAGE));
		// a skill that marks its target first hits it marked (ClassComparison counts the same)
		double mark = skill.actions().stream().flatMap(a -> a.supports().stream()).filter(x -> x.kind() == SkillSupport.Kind.MARK)
			.mapToDouble(x -> x.pct() / 100.0).max().orElse(0.0);
		double every = 1.0 + mods.fraction(EngraveStat.DAMAGE) + mods.fraction(EngraveStat.FIRST_STRIKE) + mark;
		return skill.estimate(1) * SkillCaster.castPower(data, weapon, mods) * every * crit * (1.0 + mods.fraction(EngraveStat.FINAL_DAMAGE));
	}

	/** Stats the game gives the equipped player that differ from the estimate's totals (beyond rounding). */
	private static List<String> statDiffs(final ServerPlayer player, final EngraveTotals estimate) {
		GearStats.invalidate(player);
		EngraveTotals real = GearStats.of(player);
		List<String> out = new ArrayList<>();
		for (EngraveStat stat : EngraveStat.values()) {
			float a = real.get(stat);
			float b = estimate.get(stat);
			if (Math.abs(a - b) > 0.051F) {
				out.add(stat.id() + " " + fmt(a) + " (추정 " + fmt(b) + ")");
			}
		}
		return out;
	}

	// ------------------------------------------------------------------ server side

	private static void onDamage(final LivingEntity victim, final DamageSource source, final float base, final float taken, final boolean blocked) {
		Fight f = fight;
		if (f == null || !f.counting || victim != f.dummy || taken <= 0.0F) {
			return;
		}
		f.lastDamage = victim.level().getGameTime();
		CombatHooks.DamageKind kind = CombatHooks.dealing();
		Entity direct = source.getDirectEntity();
		// hits that roll for a crit (CombatHooks.outgoing): swings, shots, skills and summons; double strike and chain lightning copy one
		boolean rolls = kind == null || kind == CombatHooks.DamageKind.SHOT || kind == CombatHooks.DamageKind.SKILL;
		if (rolls) {
			EngraveTotals mods = JobWeapons.activeTotals(f.player);
			double chance = Math.min(1.0, mods.fraction(EngraveStat.CRIT_CHANCE) + (CombatHooks.has(JobProgression.get(f.player), JobClass.ROGUE, 2) ? 0.15 : 0.0));
			double factor = 1.5 + mods.fraction(EngraveStat.CRIT_DAMAGE);
			boolean crit = CombatHooks.lastHitWasCrit();
			f.hits++;
			f.crits += crit ? 1 : 0;
			f.neutral += (crit ? taken / factor : taken) * (1.0 + chance * (factor - 1.0));
		} else {
			f.neutral += taken;
		}
		if (Actions.summoner(source.getEntity()) != null || kind == CombatHooks.DamageKind.SKILL) {
			f.skill += taken;
		} else if (kind == CombatHooks.DamageKind.EXTRA) {
			f.chain += taken;
		} else if (kind == CombatHooks.DamageKind.SHOT || kind == CombatHooks.DamageKind.REPEAT) {
			f.basic += taken;
		} else if (kind == null) {
			if (direct != null && direct != f.player && CombatHooks.skillArrow(direct)) {
				f.skill += taken;
			} else {
				f.basic += taken;
			}
		} else {
			f.other += taken;
		}
	}

	private static void tick(final MinecraftServer server) {
		Fight f = fight;
		if (f == null || !f.rotation || !f.counting) {
			return;
		}
		ServerPlayer player = f.player;
		ServerLevel level = player.level();
		long now = level.getGameTime();
		player.getFoodData().setFoodLevel(20);
		f.minMana = Math.min(f.minMana, JobProgression.get(player).mana());
		Vec3 want = f.spot != null ? f.spot : f.base;
		if (player.position().distanceTo(want) > 0.75) {
			// a movement skill took the player away: back to the spot once it is over (a player walking back)
			if (now - f.castAt >= 8 && player.onGround() || now - f.castAt >= 40) {
				place(player, want, f.dummy);
			}
			return;
		}
		if (f.spot != null && now >= f.spotUntil) {
			f.spot = null;
			place(player, f.base, f.dummy);
			return;
		}
		castNext(f, now);
		basicAttack(f, now);
	}

	/** The first skill in priority order that is ready and paid for, from where it reaches the dummy; one a tick. */
	private static void castNext(final Fight f, final long now) {
		ServerPlayer player = f.player;
		JobData data = JobProgression.get(player);
		EngraveTotals mods = JobWeapons.activeTotals(player);
		for (int slot : f.order) {
			Skill skill = f.weapon.skills().get(slot);
			if (SkillCaster.readyAt(data, skill, slot) > now || data.mana() < SkillCaster.manaCost(skill, mods)) {
				continue;
			}
			double from = player.position().distanceTo(DUMMY);
			double want = distanceFor(skill, f.base.distanceTo(DUMMY));
			if (Math.abs(want - from) > 0.5) {
				f.spot = spotAt(want);
				f.spotUntil = now + 30;
				place(player, f.spot, f.dummy);
			}
			if (SkillCaster.tryCast(player, slot) == SkillCaster.Result.OK) {
				f.casts[slot]++;
				f.castAt = now;
			}
			return;
		}
	}

	private static void basicAttack(final Fight f, final long now) {
		ServerPlayer player = f.player;
		ItemStack stack = player.getMainHandItem();
		if (f.shoots) {
			if (f.weapon.archetype().mode() == Archetype.BasicMode.DRAW) {
				// hold to a full draw, let go, draw again the next tick
				if (f.drawStart < 0) {
					f.drawStart = now;
				}
				if (now - f.drawStart >= JobWeaponItem.drawTicks(player, f.weapon)) {
					BasicAttacks.loose(player, stack, f.weapon, 1.0F);
					f.shots++;
					f.drawStart = now + 1;
				}
			} else if (!player.getCooldowns().isOnCooldown(stack)) {
				if (player.gameMode.useItem(player, player.level(), stack, InteractionHand.MAIN_HAND).consumesAction()) {
					f.shots++;
				}
			}
			return;
		}
		if (player.getAttackStrengthScale(0.5F) < 1.0F || CombatHooks.isBehind(player, f.dummy)) {
			return;
		}
		double reach = player.entityInteractionRange();
		if (f.dummy.getBoundingBox().distanceToSqr(player.getEyePosition()) > reach * reach) {
			return;
		}
		// what the attack and swing packets do (ServerGamePacketListenerImpl.handleAttack, handlePunch)
		player.attack(f.dummy);
		player.resetAttackStrengthTicker();
		f.swings++;
	}

	// ------------------------------------------------------------------ report

	private static void writeTsv(final List<Row> rows) {
		try {
			Files.writeString(Path.of("damage-report.tsv"), tsv(rows), StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new IllegalStateException("could not write damage-report.tsv", e);
		}
	}

	private static String tsv(final List<Row> rows) {
		StringBuilder tsv = new StringBuilder("class\tweapon\tshoots\test_basic\test_skill\test_chain\test_total\tplain_total\tbasic\tskill\tchain\tother\ttotal\tratio"
			+ "\tneutral\tcast_est\thits\tcrits\tswings\tshots\tcasts\tcast_est_each\tcast_measured_each\tmin_mp\tfight_ticks\tbasic_total\tskill_total\tchain_total"
			+ "\tother_total\tneutral_total\tcast_count\n");
		for (Row r : rows) {
			tsv.append(r.job().id()).append('\t').append(r.weapon().id()).append('\t').append(r.shoots()).append('\t').append(fmt(r.estimate().basic()))
				.append('\t').append(fmt(r.estimate().skill1())).append('\t').append(fmt(r.estimate().chain())).append('\t').append(fmt(r.estimate().total1()))
				.append('\t').append(fmt(r.plain().total1())).append('\t').append(fmt(r.dps(r.basic()))).append('\t').append(fmt(r.dps(r.skill())))
				.append('\t').append(fmt(r.dps(r.chain()))).append('\t').append(fmt(r.dps(r.other()))).append('\t').append(fmt(r.dps(r.total())))
				.append('\t').append(fmt(r.ratio() * 100)).append('\t').append(fmt(r.dps(r.neutral()))).append('\t').append(fmt(r.atCasts())).append('\t')
				.append(r.hits()).append('\t').append(r.crits()).append('\t').append(r.swings()).append('\t').append(r.shots()).append('\t')
				.append(java.util.Arrays.toString(r.casts())).append('\t').append(java.util.Arrays.toString(r.castEstimate())).append('\t')
				.append(java.util.Arrays.toString(r.castMeasured())).append('\t').append(r.minMana()).append('\t').append(r.fightTicks()).append('\t')
				.append(r.basic()).append('\t').append(r.skill()).append('\t').append(r.chain()).append('\t').append(r.other()).append('\t').append(r.neutral())
				.append('\t').append(java.util.Arrays.toString(r.castCount())).append('\n');
		}
		return tsv.toString();
	}

	private static void write(final List<Row> rows, final List<ClassNote> notes, final double minutes) {
		Map<JobClass, List<Row>> byClass = new LinkedHashMap<>();
		for (Row r : rows) {
			byClass.computeIfAbsent(r.job(), j -> new ArrayList<>()).add(r);
		}
		// per class: estimate, measured, mean ratio and its range, the ratio against the estimate without support, crit luck taken out
		record Sum(double estimate, double measured, double ratio, double lo, double hi, double plain, double neutral, double basic, double skill, double atCasts) {
		}
		Map<JobClass, Sum> sums = new LinkedHashMap<>();
		for (Map.Entry<JobClass, List<Row>> e : byClass.entrySet()) {
			List<Row> list = e.getValue();
			sums.put(e.getKey(), new Sum(mean(list, r -> r.estimate().total1()), mean(list, r -> r.dps(r.total())), mean(list, Row::ratio),
				list.stream().mapToDouble(Row::ratio).min().orElse(0.0), list.stream().mapToDouble(Row::ratio).max().orElse(0.0),
				mean(list, r -> r.dps(r.total()) / r.plain().total1()), mean(list, r -> r.dps(r.neutral()) / r.estimate().total1()),
				mean(list, r -> r.dps(r.basic() + r.chain()) / r.estimate().basic()), mean(list, r -> r.dps(r.skill() + r.other()) / Math.max(1.0E-9, r.estimate().skill1())),
				mean(list, r -> r.dps(r.neutral()) / r.atCasts())));
		}
		double meanEst = sums.values().stream().mapToDouble(Sum::estimate).average().orElse(1.0);
		double meanReal = sums.values().stream().mapToDouble(Sum::measured).average().orElse(1.0);
		long inside = sums.values().stream().filter(v -> v.ratio() >= 0.95 && v.ratio() <= 1.20).count();
		long insidePlain = sums.values().stream().filter(v -> v.plain() >= 0.95 && v.plain() <= 1.20).count();
		long weaponsInside = rows.stream().filter(r -> r.ratio() >= 0.95 && r.ratio() <= 1.20).count();

		StringBuilder md = new StringBuilder();
		md.append("# 풀세트 풀강 딜량: 추정치와 실측치\n\n");
		md.append("Lv ").append(LEVEL).append(" 모든 직업이 4차 무기를 하나씩 들고 훈련용 허수아비를 ").append(fmt(ClassComparison.FIGHT_SECONDS))
			.append("초 동안 때린 **실측 딜량**과, 같은 장비·특성으로 코드가 계산한 **추정 딜량**을 비교합니다.\n\n");
		md.append("- 실측: `./gradlew runClientGameTest -Ptests=damage`(`DamageClientGameTest`)가 실제 게임 클라이언트·서버로 때려 허수아비가 받은 피해를 모은 값입니다.")
			.append(REUSE.isBlank() ? " 이번 측정에 " + Math.round(minutes) + "분 걸렸습니다." : " 측정값은 이전 실행의 damage-report.tsv에서 가져오고 추정만 다시 계산했습니다.")
			.append(" 이 문서는 실행 디렉터리(`build/run/clientGameTest`)에 쓰인 `damage-report.md`를 옮긴 것이고, 숫자 원본은 `damage-report.tsv`입니다.\n");
		md.append("- 추정: `ClassComparison.build`(CLASSES.md \"직업 비교\"와 같은 계산)에 아래 장비의 강화·각성·장신구 수치, 스킬의 지원 효과, 모든 스킬이 준비된 상태로 시작하는 60초 전투를 넣어 같은 상황을 계산한 값입니다.\n");
		md.append("- 모든 수치는 초당 피해(DPS), 대상 1개 기준입니다.\n\n");

		md.append("## 요약\n\n");
		md.append("| 직업 | 추정 DPS | 실측 DPS | 실측 / 추정 (무기 평균) | 무기별 범위 | 95~120% | 직업 문서 방식 추정 대비 | 추정 평균 대비 | 실측 평균 대비 |\n");
		md.append("|---|---|---|---|---|---|---|---|---|\n");
		for (Map.Entry<JobClass, Sum> e : sums.entrySet()) {
			Sum v = e.getValue();
			boolean ok = v.ratio() >= 0.95 && v.ratio() <= 1.20;
			md.append("| ").append(e.getKey().ko()).append(" | ").append(fmt(v.estimate())).append(" | ").append(fmt(v.measured())).append(" | **")
				.append(pct(v.ratio())).append("** | ").append(pct(v.lo())).append(" ~ ").append(pct(v.hi())).append(" | ").append(ok ? "✅" : "❌").append(" | ")
				.append(pct(v.plain())).append(" | ").append(pct(v.estimate() / meanEst)).append(" | ").append(pct(v.measured() / meanReal)).append(" |\n");
		}
		md.append("\n- 실측 / 추정이 95~120%인 직업: **").append(inside).append(" / ").append(sums.size()).append("** (무기 ").append(weaponsInside).append(" / ")
			.append(rows.size()).append("개). 직업 평균은 그 직업 무기들의 실측 / 추정을 평균한 값입니다.\n");
		md.append("- \"직업 문서 방식\"은 CLASSES.md처럼 지원 효과(버프·표식·강화 타격·재사용 단축)를 빼고 스킬을 평균 빈도(60 ÷ 대기 시간)로 쓴 추정입니다. 이것과 비교하면 ")
			.append(insidePlain).append(" / ").append(sums.size()).append("개 직업이 95~120%입니다.\n");
		md.append("- \"추정 평균 대비\"·\"실측 평균 대비\"는 직업 사이 균형입니다(7개 직업 평균 = 100%).\n\n");

		md.append("## 측정 조건\n\n");
		md.append("- 장비: 무기 +15 ✦5, 그 레벨의 최고 방어구 세트 4부위 +15 ✦5, ").append(ACCESSORIES.adjKo()).append(" 장신구(에테릭스) 5부위 +10. 추가 옵션·각인·소모품은 없습니다(직업 문서의 추정과 같은 기준).\n");
		md.append("- 특성: 추정 계산이 1마리 피해가 가장 높게 찍는 분배(직업별 표의 \"특성\"). 게임에서 적용된 능력치를 추정의 능력치 합계와 하나씩 맞춰 봤고, 다른 것은 직업별 표에 ⚠로 적습니다.\n");
		md.append("- 대상: 일반 훈련용 허수아비 1개(보스 아님). 체력이 줄지 않아 선제 일격은 항상, 처형은 한 번도 붙지 않습니다. 갑옷 거치대라 상태 효과(취약 등)를 받지 않습니다. 추정도 같은 조건으로 계산했습니다.\n");
		md.append("- 위치: 근접 무기는 ").append(fmt(MELEE_DISTANCE)).append("블록, 사격 무기는 ").append(fmt(RANGED_DISTANCE))
			.append("블록 앞에서 허수아비를 정면으로 봅니다(뒤치기 없음).\n");
		md.append("- 평타: 근접은 공격 게이지가 가득 찰 때마다 휘두르고(점프 치명타 없음), 활은 끝까지 당겨 쏘고 다음 틱에 다시 당기며, 총·투척 무기는 재장전이 끝나는 대로 쏩니다. 사격 무기 중 근접이 더 센 무기는 근접으로 싸웁니다(추정과 같은 선택).\n");
		md.append("- 스킬: 준비되고 MP가 있으면 한 틱에 하나씩 바로 씁니다(버프 먼저, 그다음 피해가 큰 순서). 피해도 지원 효과도 없는 이동기(이동 속도만 주는 점멸 등)는 쓰지 않습니다. 서 있는 자리에서 닿지 않는 스킬(근접 범위, 짧은 돌진·범위)은 ")
			.append(fmt(CLOSE_DISTANCE)).append("블록, 도약은 ").append(fmt(LEAP_DISTANCE))
			.append("블록 앞으로 옮겨서 쓰고, 이동 스킬로 자리를 벗어나면 이동이 끝난 뒤 제자리로 돌아갑니다(그동안 평타 없음).\n");
		md.append("- 시간: 모든 스킬이 준비되고 MP가 가득 찬 상태에서 ").append(fmt(ClassComparison.FIGHT_SECONDS))
			.append("초. 받은 피해 합 ÷ 60초입니다. 스킬 1회 피해는 그 스킬만 ").append(PROBE_CASTS).append("번 따로 써서 평균을 냈습니다.\n\n");

		md.append("## 추정 방법\n\n");
		md.append("- 평타: 무기 위력 × 형태 배율 × (1 + 기본 공격 피해 + 주는 피해 + 선제 일격 [+ 사격 거리 보너스]) × 치명타 기대값 × (1 + 최종 피해) × 공격 속도(근접) 또는 장전 속도(사격; 활은 놓은 다음 틱에야 다시 당기므로 당기는 시간 + 1틱마다 1발). 이중 타격, 연쇄 번개(대상에게도 떨어지고 주는 피해·최종 피해가 한 번 더 붙음)를 더합니다.\n");
		md.append("- 스킬: 스킬 배율 합 × 무기 위력 × 1.5 × (1 + 스킬 피해) × (1 + 주는 피해 + 선제 일격) × 치명타 기대값 × (1 + 최종 피해). 60초 전투가 모든 스킬이 준비된 상태로 시작하므로 대기 시간이 C초인 스킬은 1 + ⌊60 ÷ C⌋번 씁니다(50초짜리 궁극기는 2번). MP는 최대 MP + 60초 회복으로 계산합니다.\n");
		md.append("- 지원 효과: 스킬의 버프(주는 피해·치명타 확률·치명타 피해)와 표식은 사용 빈도에서 유지되는 시간 비율만큼(같은 종류는 겹치지 않고 높은 쪽), 대상을 먼저 표식하고 때리는 스킬은 자기 타격에 표식을 그대로, 강화 타격·은신 일격은 그동안 들어가는 평타만큼, 재사용 단축(refresh)은 다른 스킬의 대기 시간을 줄여서 넣습니다. 허수아비는 취약 같은 상태 효과를 받지 않아 넣지 않습니다.\n");
		md.append("- 빠진 것: 자리 이동에 드는 시간, 치명타 운, 버프와 스킬이 겹치는 순서(버프 직후 궁극기 등), 상태 이상 피해.\n\n");

		md.append("## 차이가 나는 이유\n\n");
		md.append("| 직업 | 평타 실측/추정 | 스킬 실측/추정 | 치명타 운을 뺀 실측/추정 | 실제 사용 횟수로 다시 잰 추정 대비 |\n|---|---|---|---|---|\n");
		for (Map.Entry<JobClass, Sum> e : sums.entrySet()) {
			Sum v = e.getValue();
			md.append("| ").append(e.getKey().ko()).append(" | ").append(pct(v.basic())).append(" | ").append(pct(v.skill())).append(" | ").append(pct(v.neutral()))
				.append(" | ").append(pct(v.atCasts())).append(" |\n");
		}
		md.append("\n- 평타: 휘두를 수 있는 순간이 틱(0.05초) 단위라 공격 속도가 정확히 맞지 않고, 이동 스킬 뒤 제자리로 돌아오는 동안 평타를 쉬어서 보통 조금 낮게 나옵니다. 지원 효과(주는 피해·표식)는 평타에도 붙습니다.\n");
		md.append("- 스킬: 버프를 먼저 쓰니 전투 시작의 궁극기는 항상 버프를 받습니다(추정은 버프 유지 비율의 평균만 넣음). 치명타 운도 스킬 쪽이 타격 수가 적어 더 흔들립니다.\n");
		md.append("- \"실제 사용 횟수로 다시 잰 추정\"은 평타 추정 + 이번 전투에서 쓴 횟수 × 스킬 1회 추정(버프 없이)입니다. 이 값과 실측이 가까우면 스킬 하나하나는 추정대로 들어갔다는 뜻입니다.\n");
		md.append("- 무기 하나만 보면 더 흔들립니다. 도약·돌진처럼 시전자를 옮기는 스킬은 날아가 있는 동안과 돌아오는 동안 평타를 못 쳐서 평타가 낮게 나오고(추정은 이동 시간을 0으로 봄), ")
			.append("버프와 재사용 단축이 많은 무기는 전투 시작에 버프를 받은 궁극기와 당겨진 대기 시간 덕에 스킬이 높게 나옵니다.\n\n");
		List<Row> outside = rows.stream().filter(r -> r.ratio() < 0.95 || r.ratio() > 1.20).sorted(Comparator.comparingDouble(Row::ratio)).toList();
		if (!outside.isEmpty()) {
			md.append("### 95~120% 밖의 무기\n\n| 직업 | 무기 | 실측 / 추정 | 평타 실측/추정 | 스킬 실측/추정 | 치명타 운을 뺀 값 | 실제 사용 횟수로 다시 잰 추정 대비 |\n");
			md.append("|---|---|---|---|---|---|---|\n");
			for (Row r : outside) {
				md.append("| ").append(r.job().ko()).append(" | ").append(r.weapon().ko()).append(" | ").append(pct(r.ratio())).append(" | ")
					.append(pct(r.dps(r.basic() + r.chain()) / r.estimate().basic())).append(" | ")
					.append(pct(r.dps(r.skill() + r.other()) / Math.max(1.0E-9, r.estimate().skill1()))).append(" | ")
					.append(pct(r.dps(r.neutral()) / r.estimate().total1())).append(" | ").append(pct(r.dps(r.neutral()) / r.atCasts())).append(" |\n");
			}
			md.append("\n");
		}

		md.append("## 직업별\n");
		for (ClassNote note : notes) {
			List<Row> list = byClass.getOrDefault(note.job(), List.of());
			if (list.isEmpty()) {
				continue;
			}
			md.append("\n### ").append(note.job().ko()).append("\n\n");
			md.append("- 방어구: ").append(note.build().set() == null ? "-" : note.build().set().ko()).append(" · 특성: ")
				.append(note.build().talentText().isEmpty() ? "-" : note.build().talentText()).append('\n');
			EngraveTotals m = note.build().mods();
			md.append("- 능력치: 기본 공격 +").append(fmt(m.get(EngraveStat.BASIC_DAMAGE))).append("% · 사격 +").append(fmt(m.get(EngraveStat.SHOT_DAMAGE)))
				.append("% · 스킬 +").append(fmt(m.get(EngraveStat.SKILL_DAMAGE))).append("% · 주는 피해 +").append(fmt(m.get(EngraveStat.DAMAGE)))
				.append("% · 최종 피해 +").append(fmt(m.get(EngraveStat.FINAL_DAMAGE))).append("% · 치명타 ").append(fmt(m.get(EngraveStat.CRIT_CHANCE))).append("% / +")
				.append(fmt(m.get(EngraveStat.CRIT_DAMAGE))).append("% · 공격 속도 +").append(fmt(m.get(EngraveStat.ATTACK_SPEED))).append("% · 선제 일격 +")
				.append(fmt(m.get(EngraveStat.FIRST_STRIKE))).append("% · 연쇄 번개 ").append(fmt(m.get(EngraveStat.CHAIN_LIGHTNING))).append("% · 이중 타격 ")
				.append(fmt(m.get(EngraveStat.DOUBLE_STRIKE))).append("%\n");
			if (!note.statDiffs().isEmpty()) {
				md.append("- ⚠ 게임 능력치가 추정과 다름: ").append(String.join(", ", note.statDiffs())).append('\n');
			}
			md.append("\n| 무기 | 평타 | 추정 평타 / 스킬 / 합계 | 실측 평타 / 스킬 / 합계 | 실측 / 추정 | 직업 문서 방식 추정 | 평타 횟수 | 스킬 사용 횟수 |\n");
			md.append("|---|---|---|---|---|---|---|---|\n");
			for (Row r : list) {
				md.append("| ").append(r.weapon().ko()).append(" | ").append(r.shoots() ? "사격" : "근접").append(" | ")
					.append(fmt(r.estimate().basic())).append(" / ").append(fmt(r.estimate().skill1())).append(" / **").append(fmt(r.estimate().total1())).append("** | ")
					.append(fmt(r.dps(r.basic() + r.chain()))).append(" / ").append(fmt(r.dps(r.skill() + r.other()))).append(" / **").append(fmt(r.dps(r.total())))
					.append("** | ").append(pct(r.ratio())).append(" | ").append(fmt(r.plain().total1())).append(" (").append(pct(r.dps(r.total()) / r.plain().total1()))
					.append(") | ").append(r.shoots() ? r.shots() : r.swings()).append(" | ").append(casts(r)).append(" |\n");
			}
		}

		md.append("\n## 스킬 1회 피해 (허수아비 1개, ").append(PROBE_CASTS).append("회 평균)\n\n");
		md.append("추정은 스킬 배율 합 × 스킬 위력(무기 위력 × 1.5 × 스킬 피해) × 주는 피해·선제 일격(·먼저 거는 표식) × 치명타 기대값 × 최종 피해입니다(다른 스킬의 버프 없이). ")
			.append("실측은 치명타가 터진 타격을 치명타 기대값으로 바꿔 운을 뺀 값입니다. 소환수와 장판은 측정 시간(최대 ").append(fmt(PROBE_MAX_TICKS / 20.0))
			.append("초) 안에 들어간 피해만 셉니다. 소환수는 주인이 때린 대상만 공격해서 스킬만 따로 쓰면 아무도 때리지 않으므로 60초 전투에서만 셉니다.\n");
		for (Map.Entry<JobClass, List<Row>> e : byClass.entrySet()) {
			md.append("\n<details><summary>").append(e.getKey().ko()).append("</summary>\n\n| 무기 | 스킬 | 추정 | 실측 | 실측 / 추정 |\n|---|---|---|---|---|\n");
			for (Row r : e.getValue()) {
				for (int i = 0; i < r.weapon().skills().size(); i++) {
					if (r.castEstimate()[i] <= 0.0) {
						continue;
					}
					// summons only go for what their owner hits, and a skill cast alone hits nothing
					boolean summons = describes(r.weapon().skills().get(i), "summon");
					md.append("| ").append(r.weapon().ko()).append(" | ").append(r.weapon().skills().get(i).ko()).append(" | ").append(fmt(r.castEstimate()[i])).append(" | ")
						.append(summons ? "소환수 (전투 중에만 공격)" : r.castCount()[i] == 0 ? "못 씀" : fmt(r.castMeasured()[i])).append(" | ")
						.append(summons || r.castCount()[i] == 0 ? "-" : pct(r.castMeasured()[i] / r.castEstimate()[i])).append(" |\n");
				}
			}
			md.append("\n</details>\n");
		}
		try {
			Files.writeString(Path.of("damage-report.md"), md.toString(), StandardCharsets.UTF_8);
			Files.writeString(Path.of("damage-report.tsv"), tsv(rows), StandardCharsets.UTF_8);
			MinecraftMode.LOGGER.info("[damage] report written to {}", Path.of("damage-report.md").toAbsolutePath());
		} catch (IOException e) {
			throw new IllegalStateException("could not write the damage report", e);
		}
	}

	private static double mean(final List<Row> rows, final java.util.function.ToDoubleFunction<Row> value) {
		return rows.stream().mapToDouble(value).average().orElse(0.0);
	}

	private static String casts(final Row r) {
		StringBuilder out = new StringBuilder();
		for (int i = 0; i < r.casts().length; i++) {
			out.append(i == 0 ? "" : " · ").append(r.casts()[i]);
		}
		return out.toString();
	}

	static String fmt(final double v) {
		double r = Math.round(v * 10.0) / 10.0;
		return r == Math.rint(r) ? Long.toString((long)r) : String.format(Locale.ROOT, "%.1f", r);
	}

	static String pct(final double v) {
		return Math.round(v * 100) + "%";
	}

	private static void require(final boolean condition, final String message) {
		if (!condition) {
			throw new AssertionError(message);
		}
	}
}
