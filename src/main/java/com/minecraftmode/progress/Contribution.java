package com.minecraftmode.progress;

import com.minecraftmode.job.skill.Actions;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.player.Player;
import org.jspecify.annotations.Nullable;

/**
 * Who earned a kill. Every player's damage to a creature is recorded as the health it really lost (after armor, absorption and
 * overkill; a tamed summon's damage counts for its owner). Support counts too: healing another player counts as damage to the
 * monsters that player is fighting ({@link #HEAL_WEIGHT}, overhealing excluded), a buff on another player earns
 * {@link #BUFF_SHARE} of the damage they deal while it lasts, and a debuff on a monster earns {@link #DEBUFF_SHARE} of the damage
 * others deal to it. When it dies, the players with at least {@link #MIN_SHARE} of the credit (each within {@link #FORGET_TICKS} of
 * their last part, online and in the same world) share it: class experience is split by share with a group bonus, and the top
 * contributor — the MVP — takes the rewards that cannot be split (gear, pet and mount drops, gold find). Server memory only: the
 * record goes away with the creature.
 */
public final class Contribution {
	/** Below this share of the credit a player gets nothing (no tagging a monster once for a cut). */
	public static final float MIN_SHARE = 0.05F;
	/** Health healed on another player counts as this much damage, split over the monsters they fought in the last {@link #COMBAT_TICKS}. */
	public static final float HEAL_WEIGHT = 1.0F;
	/** A buff on another player earns this share of the damage they deal while it lasts (each buffer once, however many buffs). */
	public static final float BUFF_SHARE = 0.25F;
	/** A debuff on a monster earns this share of the damage others deal to it while it lasts. */
	public static final float DEBUFF_SHARE = 0.15F;
	/** A player who hit a monster this recently is still fighting it (healing them supports that fight). */
	public static final int COMBAT_TICKS = 20 * 10;
	/** Each extra contributor adds this much to the experience pool, so hunting together pays more in total. */
	public static final float GROUP_BONUS = 0.2F;
	/** Contributors past this many add no more group bonus. */
	public static final int MAX_BONUS_MEMBERS = 5;
	/** Named monsters: contributors (other than the MVP) with at least this share get Evolution Ether of their own. */
	public static final float ASSIST_SHARE = 0.25F;
	/** A hitter who has not hit for this long is forgotten. */
	public static final int FORGET_TICKS = 20 * 60;

	private static final Map<LivingEntity, Map<UUID, Hit>> DEALT = new WeakHashMap<>();
	/** Monsters each player hit lately (game time of the last hit). */
	private static final Map<UUID, Map<LivingEntity, Long>> FIGHTING = new HashMap<>();
	/** Buffed player -> buffer -> game time the buff ends. */
	private static final Map<UUID, Map<UUID, Long>> BUFFED = new HashMap<>();
	/** Debuffed monster -> debuffer -> game time the debuff ends. */
	private static final Map<LivingEntity, Map<UUID, Long>> DEBUFFED = new WeakHashMap<>();

	private static final class Hit {
		private float credit;
		private long last;
	}

	/** One contributor and their share of the kill (shares of a kill add up to 1). */
	public record Share(ServerPlayer player, float fraction) {
	}

	/** Called after {@code victim} lost {@code lost} health to {@code source} (see {@code LivingEntityMixin}). */
	public static void record(final LivingEntity victim, final DamageSource source, final float lost) {
		if (lost <= 0.0F || victim instanceof Player || victim.level().isClientSide()) {
			return;
		}
		UUID id = creditFor(source.getEntity());
		if (id == null) {
			return;
		}
		long now = victim.level().getGameTime();
		credit(victim, id, lost, now);
		FIGHTING.computeIfAbsent(id, k -> new WeakHashMap<>()).put(victim, now);
		Map<UUID, Long> buffers = BUFFED.get(id);
		if (buffers != null) {
			buffers.values().removeIf(until -> until < now);
			buffers.forEach((buffer, until) -> {
				if (!buffer.equals(id)) {
					credit(victim, buffer, lost * BUFF_SHARE, now);
				}
			});
		}
		Map<UUID, Long> debuffers = DEBUFFED.get(victim);
		if (debuffers != null) {
			debuffers.values().removeIf(until -> until < now);
			debuffers.forEach((debuffer, until) -> {
				if (!debuffer.equals(id)) {
					credit(victim, debuffer, lost * DEBUFF_SHARE, now);
				}
			});
		}
	}

	/** {@code healer} restored {@code amount} health to {@code ally}: it supports the fights {@code ally} is in. */
	public static void healed(final Player healer, final Player ally, final float amount) {
		if (amount <= 0.0F || healer == ally || healer.level().isClientSide()) {
			return;
		}
		Map<LivingEntity, Long> fights = FIGHTING.get(ally.getUUID());
		if (fights == null) {
			return;
		}
		long now = ally.level().getGameTime();
		fights.entrySet().removeIf(e -> now - e.getValue() > COMBAT_TICKS || !e.getKey().isAlive());
		if (fights.isEmpty()) {
			return;
		}
		float each = amount * HEAL_WEIGHT / fights.size();
		for (LivingEntity monster : List.copyOf(fights.keySet())) {
			credit(monster, healer.getUUID(), each, now);
		}
	}

	/** {@code buffer} gave {@code ally} a buff lasting {@code ticks}. */
	public static void buffed(final Player buffer, final Player ally, final int ticks) {
		if (buffer == ally || buffer.level().isClientSide()) {
			return;
		}
		BUFFED.computeIfAbsent(ally.getUUID(), k -> new HashMap<>()).merge(buffer.getUUID(), buffer.level().getGameTime() + ticks, Math::max);
	}

	/** {@code debuffer} weakened {@code monster} for {@code ticks}. */
	public static void debuffed(final Player debuffer, final LivingEntity monster, final int ticks) {
		if (monster instanceof Player || debuffer.level().isClientSide()) {
			return;
		}
		DEBUFFED.computeIfAbsent(monster, k -> new HashMap<>()).merge(debuffer.getUUID(), debuffer.level().getGameTime() + ticks, Math::max);
	}

	/** A player who logged out no longer buffs or fights anything. */
	public static void forget(final UUID player) {
		FIGHTING.remove(player);
		BUFFED.remove(player);
	}

	/** Server stopped: nothing carries over to the next world. */
	public static void clear() {
		DEALT.clear();
		FIGHTING.clear();
		BUFFED.clear();
		DEBUFFED.clear();
	}

	private static void credit(final LivingEntity monster, final UUID player, final float amount, final long now) {
		Hit hit = DEALT.computeIfAbsent(monster, k -> new HashMap<>()).computeIfAbsent(player, k -> new Hit());
		hit.credit += amount;
		hit.last = now;
	}

	/** The player a hit counts for: the attacker, or the owner of a tamed creature or skill summon. */
	private static @Nullable UUID creditFor(final @Nullable Entity attacker) {
		if (attacker instanceof Player player) {
			return player.getUUID();
		}
		if (attacker instanceof OwnableEntity pet && pet.getOwner() instanceof Player owner) {
			return owner.getUUID();
		}
		return Actions.summoner(attacker);
	}

	/** Credit each player earned on {@code victim} so far: damage plus support (forgotten players left out). */
	public static Map<UUID, Float> dealt(final LivingEntity victim) {
		Map<UUID, Hit> hits = DEALT.get(victim);
		Map<UUID, Float> out = new LinkedHashMap<>();
		if (hits != null) {
			long now = victim.level().getGameTime();
			hits.forEach((id, hit) -> {
				if (now - hit.last <= FORGET_TICKS) {
					out.put(id, hit.credit);
				}
			});
		}
		return out;
	}

	/**
	 * Who shares the death of {@code victim}, best first. Empty when no player took part: killed by something else with no player
	 * hit in vanilla's memory window, or removed with {@code /kill}. A killer with no recorded damage (a kill outside the normal
	 * damage path) takes it all.
	 */
	public static List<Share> shares(final LivingEntity victim, final DamageSource source) {
		UUID killerId = creditFor(source.getEntity());
		ServerPlayer killer = resolve(victim, killerId, source);
		if (killer == null && (victim.getLastHurtByPlayer() == null || source.is(DamageTypes.GENERIC_KILL))) {
			return List.of();
		}
		Map<UUID, Float> dealt = dealt(victim);
		Map<ServerPlayer, Float> present = new LinkedHashMap<>();
		dealt.forEach((id, damage) -> {
			ServerPlayer p = resolve(victim, id, source);
			if (p != null) {
				present.put(p, damage);
			}
		});
		if (present.isEmpty()) {
			return killer == null ? List.of() : List.of(new Share(killer, 1.0F));
		}
		List<Share> out = new ArrayList<>();
		split(present).forEach((p, f) -> out.add(new Share(p, f)));
		return out;
	}

	/**
	 * Damage to shares: drops everyone under {@link #MIN_SHARE} of the total (the top one always stays), then rescales the rest
	 * to add up to 1, best first. Pure, so tests can check it.
	 */
	public static <K> Map<K, Float> split(final Map<K, Float> dealt) {
		float total = 0.0F;
		for (float d : dealt.values()) {
			total += Math.max(0.0F, d);
		}
		List<Map.Entry<K, Float>> sorted = new ArrayList<>(dealt.entrySet());
		sorted.sort(Map.Entry.<K, Float>comparingByValue(Comparator.reverseOrder()));
		Map<K, Float> kept = new LinkedHashMap<>();
		if (total <= 0.0F || sorted.isEmpty()) {
			return kept;
		}
		float keptTotal = 0.0F;
		for (int i = 0; i < sorted.size(); i++) {
			float d = Math.max(0.0F, sorted.get(i).getValue());
			if (i == 0 || d / total >= MIN_SHARE) {
				kept.put(sorted.get(i).getKey(), d);
				keptTotal += d;
			}
		}
		float sum = keptTotal;
		kept.replaceAll((k, d) -> d / sum);
		return kept;
	}

	/** Total experience pool for {@code contributors} players, as a multiple of a solo kill. */
	public static float groupMultiplier(final int contributors) {
		return 1.0F + GROUP_BONUS * (Math.min(Math.max(contributors, 1), MAX_BONUS_MEMBERS) - 1);
	}

	/** {@code share}'s part of a reward worth {@code solo} to one player, with the group bonus (at least 1). */
	public static int portion(final int solo, final Share share, final int contributors) {
		return Math.max(1, Math.round(solo * groupMultiplier(contributors) * share.fraction()));
	}

	private static @Nullable ServerPlayer resolve(final LivingEntity victim, final @Nullable UUID id, final DamageSource source) {
		if (id == null) {
			return null;
		}
		ServerPlayer p = victim.level() instanceof ServerLevel level ? level.getServer().getPlayerList().getPlayer(id) : null;
		if (p == null && source.getEntity() instanceof ServerPlayer attacker && attacker.getUUID().equals(id)) {
			p = attacker; // fake players are in no player list
		}
		return p != null && p.level() == victim.level() && !p.isSpectator() ? p : null;
	}

	private Contribution() {
	}
}
