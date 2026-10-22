package com.minecraftmode.test;

import com.minecraftmode.MinecraftMode;
import com.minecraftmode.entity.TrainingDummy;
import com.minecraftmode.job.JobProgression;
import com.minecraftmode.job.JobStats;
import com.minecraftmode.job.skill.Actions;
import com.minecraftmode.job.skill.CombatHooks;
import com.minecraftmode.job.skill.Skill;
import com.minecraftmode.job.skill.SkillAction;
import com.minecraftmode.job.skill.SkillCaster;
import com.minecraftmode.job.weapon.JobWeapons;
import com.minecraftmode.job.weapon.WeaponDef;
import com.minecraftmode.raid.Parties;
import com.minecraftmode.registry.ModEntities;
import com.mojang.authlib.GameProfile;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerConnection;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

/**
 * Every skill that summons helpers, played out for real (vanilla AI, real hits): do the helpers fight a training dummy and a
 * monster, do their hits land with the caster's power, and do neither the caster nor a party member hurt them, nor they the party.
 * Each problem is collected and the check fails at the end with all of them, after logging a table per skill.
 */
final class SummonChecks {
	private static final double X = 300.5;
	private static final double Y = -60.0;
	private static final double Z = 300.5;
	private static final UUID BOB = UUID.nameUUIDFromBytes("SummonBob".getBytes());

	/** One hit that involved a summon, as {@code AFTER_DAMAGE} saw it. */
	private record Hit(Entity attacker, LivingEntity victim, float amount) {
	}

	private static final List<Hit> HITS = new ArrayList<>();
	/** Summon attacks that reached {@code hurtServer} (landed or not), by victim. */
	private static final List<LivingEntity> TRIES = new ArrayList<>();
	private static boolean registered;
	private static boolean recording;

	private SummonChecks() {
	}

	static void run(final ClientGameTestContext context, final TestServerContext server, final TestServerConnection connection) {
		if (!registered) {
			registered = true;
			ServerLivingEntityEvents.AFTER_DAMAGE.register((victim, source, base, taken, blocked) -> {
				Entity attacker = source.getEntity();
				if (recording && (Actions.summoner(attacker) != null || Actions.summoner(victim) != null)) {
					HITS.add(new Hit(attacker, victim, taken));
				}
			});
			ServerLivingEntityEvents.ALLOW_DAMAGE.register((victim, source, amount) -> {
				if (recording && Actions.summoner(source.getEntity()) != null) {
					TRIES.add(victim);
				}
				return true;
			});
		}
		server.runCommand("forceload add 280 280 320 320");
		server.runCommand("fill 280 -61 280 320 -61 320 minecraft:stone");
		server.runCommand("fill 280 -60 280 320 -50 320 minecraft:air");
		List<String> problems = new ArrayList<>();
		List<String> rows = new ArrayList<>();
		for (WeaponDef def : JobWeapons.all()) {
			for (int slot = 0; slot < def.skills().size(); slot++) {
				Skill skill = def.skills().get(slot);
				if (summonCount(skill) > 0) {
					rows.add(check(context, server, connection, def, slot, problems));
				}
			}
		}
		server.runOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			Parties.leave(player);
			clear(player);
			player.connection.teleport(0.5, -60, 0.5, 180.0F, 0.0F);
		});
		server.runCommand("forceload remove 280 280 320 320");
		MinecraftMode.LOGGER.info("[job] summons:\n{}", String.join("\n", rows));
		require(problems.isEmpty(), "summon problems:\n" + String.join("\n", problems));
	}

	/** Plays one summon skill through its three phases and returns its report line. */
	private static String check(final ClientGameTestContext context, final TestServerContext server, final TestServerConnection connection,
		final WeaponDef def, final int slot, final List<String> problems) {
		Skill skill = def.skills().get(slot);
		String name = skill.id();

		// 1) the training dummy the caster is hitting with skills
		Phase dummy = phase(context, server, connection, def, slot, (level, player) -> {
			TrainingDummy target = ModEntities.TRAINING_DUMMY.create(level, EntitySpawnReason.COMMAND);
			target.snapTo(X, Y, Z - 5, 0, 0);
			level.addFreshEntity(target);
			return target;
		}, (player, target, tick) -> {
			if (tick % 20 == 0) {
				CombatHooks.deal(player, target, 5.0F, player.damageSources().playerAttack(player), CombatHooks.DamageKind.SKILL);
			}
		});
		// 2) a monster nobody has touched yet (does the helper start on its own?), then hit by the caster's skills, then by melee
		Phase alone = phase(context, server, connection, def, slot, SummonChecks::zombie, (player, target, tick) -> {
		});
		Phase bySkill = phase(context, server, connection, def, slot, SummonChecks::zombie, (player, target, tick) -> {
			if (tick % 20 == 0) {
				CombatHooks.deal(player, target, 1.0F, player.damageSources().playerAttack(player), CombatHooks.DamageKind.SKILL);
			}
		});
		Phase byMelee = phase(context, server, connection, def, slot, SummonChecks::zombie, (player, target, tick) -> {
			if (tick % 20 == 0) {
				player.attack(target);
			}
		});
		// 3) friendly fire: the caster and a party member swing at and shoot every helper, then everyone waits to see who bites back
		String friendly = friendlyFire(context, server, connection, def, slot, problems);

		String row = String.format(Locale.ROOT, "%-40s dummy %s | alone %s | skill-hit %s | melee-hit %s | %s",
			name, dummy, alone, bySkill, byMelee, friendly);
		if (dummy.hits == 0) {
			problems.add(name + ": its helpers never hit the training dummy the caster was fighting");
		}
		if (bySkill.hits == 0 && byMelee.hits == 0 && alone.hits == 0) {
			problems.add(name + ": its helpers never hit a monster");
		} else if (bySkill.hits == 0) {
			problems.add(name + ": its helpers ignore a monster the caster hits with skills");
		}
		for (Phase phase : List.of(dummy, alone, bySkill, byMelee)) {
			if (phase.hits > 0 && phase.lowest < phase.expected * 0.99F) {
				problems.add(name + ": a helper hit for " + phase.lowest + ", below its caster's power share " + phase.expected);
			}
		}
		return row;
	}

	private interface Spawner {
		LivingEntity spawn(ServerLevel level, ServerPlayer player);
	}

	private interface Act {
		void tick(ServerPlayer player, LivingEntity target, int tick);
	}

	/** Hits the helpers landed on the phase's target: how many, their total and lowest hit, and the share they should hit for. */
	private static final class Phase {
		int hits;
		float total;
		float lowest = Float.MAX_VALUE;
		float expected;
		int tries;
		String helpers = "";

		@Override
		public String toString() {
			return (this.hits == 0 ? "0 hits" : String.format(Locale.ROOT, "%d hits avg %.1f (base %.1f)", this.hits, this.total / this.hits, this.expected)) + " of " + this.tries + " tries [" + this.helpers + "]";
		}
	}

	private static Phase phase(final ClientGameTestContext context, final TestServerContext server, final TestServerConnection connection,
		final WeaponDef def, final int slot, final Spawner spawner, final Act act) {
		LivingEntity target = server.computeOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			ready(player, def);
			LivingEntity spawned = spawner.spawn(player.level(), player);
			SkillCaster.Result cast = SkillCaster.tryCast(player, slot);
			require(cast == SkillCaster.Result.OK, def.skills().get(slot).id() + " could not be cast: " + cast);
			HITS.clear();
			TRIES.clear();
			recording = true;
			return spawned;
		});
		for (int tick = 0; tick < 120; tick++) {
			final int now = tick;
			server.runOnServer(s -> act.tick(connection.getServerPlayer(), target, now));
			context.waitTick();
		}
		return server.computeOnServer(s -> {
			recording = false;
			Phase phase = new Phase();
			ServerPlayer player = connection.getServerPlayer();
			for (Entity e : helpers(player)) {
				float hit = Actions.summonDamage(e);
				if (!Float.isNaN(hit)) {
					phase.expected = hit;
				}
			}
			phase.tries = (int)TRIES.stream().filter(v -> v == target).count();
			for (Entity e : helpers(player)) {
				LivingEntity aim = e instanceof net.minecraft.world.entity.Mob mob ? mob.getTarget() : null;
				phase.helpers += String.format(Locale.ROOT, "%s->%s %.1f ", e.getType().toShortString(), aim == null ? "-" : aim == target ? "T" : aim.getType().toShortString(), e.distanceTo(target));
			}
			for (Hit hit : HITS) {
				if (hit.victim == target && Actions.summoner(hit.attacker) != null) {
					phase.hits++;
					phase.total += hit.amount;
					phase.lowest = Math.min(phase.lowest, hit.amount);
				}
			}
			clear(player);
			return phase;
		});
	}

	private static String friendlyFire(final ClientGameTestContext context, final TestServerContext server, final TestServerConnection connection,
		final WeaponDef def, final int slot, final List<String> problems) {
		String name = def.skills().get(slot).id();
		server.runOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			ready(player, def);
			FakePlayer bob = bob(player);
			bob.snapTo(X + 3, Y, Z + 3, 0, 0);
			require(SkillCaster.tryCast(player, slot) == SkillCaster.Result.OK, name + " could not be cast");
			HITS.clear();
			recording = true;
		});
		context.waitTicks(10);
		server.runOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			FakePlayer bob = bob(player);
			for (Entity e : helpers(player)) {
				player.resetAttackStrengthTicker();
				player.attack(e);
				bob.resetAttackStrengthTicker();
				bob.attack(e);
				shoot(player, e);
				shoot(bob, e);
			}
		});
		context.waitTicks(80);
		return server.computeOnServer(s -> {
			recording = false;
			ServerPlayer player = connection.getServerPlayer();
			FakePlayer bob = bob(player);
			int byOwner = 0;
			int byParty = 0;
			int onOwner = 0;
			int onParty = 0;
			int dead = 0;
			for (Hit hit : HITS) {
				boolean helperHurt = Actions.summoner(hit.victim) != null;
				if (helperHurt && hit.attacker == player) {
					byOwner++;
				} else if (helperHurt && hit.attacker == bob) {
					byParty++;
				} else if (hit.victim == player) {
					onOwner++;
				} else if (hit.victim == bob) {
					onParty++;
				}
			}
			for (Hit hit : HITS) {
				if (Actions.summoner(hit.victim) != null && !hit.victim.isAlive()) {
					dead++;
				}
			}
			if (byOwner > 0) {
				problems.add(name + ": the caster's own swings or arrows hurt their helpers " + byOwner + " times");
			}
			if (byParty > 0) {
				problems.add(name + ": a party member's swings or arrows hurt the caster's helpers " + byParty + " times");
			}
			if (onOwner > 0) {
				problems.add(name + ": the helpers turned on their caster " + onOwner + " times");
			}
			if (onParty > 0) {
				problems.add(name + ": the helpers turned on a party member " + onParty + " times");
			}
			clear(player);
			return String.format(Locale.ROOT, "hurt by owner %d, by party %d; bit owner %d, party %d; died %d", byOwner, byParty, onOwner, onParty, dead);
		});
	}

	/** The caster at the weapon's own class, tier and level, at the arena centre facing north, full MP and no cooldowns. */
	private static void ready(final ServerPlayer player, final WeaponDef def) {
		clear(player);
		JobProgression.set(player, JobProgression.get(player).withJob(def.job(), def.tier()).withProgress(def.level(), 0));
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(JobWeapons.item(def)));
		JobStats.refresh(player);
		JobProgression.set(player, JobProgression.get(player).withMana(JobStats.maxMana(player)).withCooldowns(Map.of()));
		player.connection.teleport(X, Y, Z, 180.0F, 0.0F);
		player.setHealth(player.getMaxHealth());
		player.removeAllEffects();
	}

	/** A sturdy zombie that stands still (no AI), five blocks ahead of the caster. */
	private static LivingEntity zombie(final ServerLevel level, final ServerPlayer player) {
		Zombie zombie = EntityTypes.ZOMBIE.create(level, EntitySpawnReason.COMMAND);
		zombie.snapTo(X, Y, Z - 5, 0, 0);
		zombie.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000.0);
		zombie.setHealth(1000.0F);
		zombie.setNoAi(true);
		zombie.setPersistenceRequired();
		level.addFreshEntity(zombie);
		return zombie;
	}

	/** A party member standing beside the caster (a fake player, in the caster's party). */
	private static FakePlayer bob(final ServerPlayer player) {
		FakePlayer bob = FakePlayer.get(player.level(), new GameProfile(BOB, "SummonBob"));
		if (!Parties.together(player.getUUID(), BOB)) {
			Parties.leave(bob);
			if (Parties.of(player.getUUID()) == null) {
				Parties.create(player);
			}
			Parties.invite(player, bob);
			Parties.accept(bob);
		}
		require(Parties.together(player.getUUID(), BOB), "the fake party member could not join");
		return bob;
	}

	/** A class arrow (as a basic bow shot makes it) from two blocks off, aimed at {@code target}. */
	private static void shoot(final Player shooter, final Entity target) {
		ServerLevel level = (ServerLevel)target.level();
		Arrow arrow = new Arrow(level, shooter, new ItemStack(Items.ARROW), new ItemStack(Items.BOW));
		Vec3 aim = target.getBoundingBox().getCenter();
		Vec3 from = aim.add(2.0, 0.6, 0.0);
		arrow.setPos(from.x, from.y, from.z);
		Vec3 d = aim.subtract(from);
		arrow.shoot(d.x, d.y, d.z, 2.0F, 0.0F);
		CombatHooks.trackArrow(arrow, 6.0F);
		level.addFreshEntity(arrow);
	}

	/** Living skill summons fighting for {@code player}. */
	private static List<Entity> helpers(final ServerPlayer player) {
		return player.level().getEntities((Entity)null, player.getBoundingBox().inflate(40),
			e -> e.isAlive() && player.getUUID().equals(Actions.summoner(e)));
	}

	/** Removes everything but players around the arena. */
	private static void clear(final ServerPlayer player) {
		ServerLevel level = player.level();
		for (Entity e : level.getEntities((Entity)null, new net.minecraft.world.phys.AABB(X - 40, Y - 10, Z - 40, X + 40, Y + 30, Z + 40),
			e -> !(e instanceof Player))) {
			e.discard();
		}
	}

	private static void require(final boolean condition, final String message) {
		if (!condition) {
			throw new AssertionError(message);
		}
	}

	private static int summonCount(final Skill skill) {
		int count = 0;
		for (SkillAction action : skill.actions()) {
			if (action.describe().getContents() instanceof TranslatableContents text && text.getKey().equals("skill_action.minecraft_mode.summon")) {
				count += (Integer)text.getArgs()[0];
			}
		}
		return count;
	}
}
