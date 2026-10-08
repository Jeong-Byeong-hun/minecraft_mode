package com.minecraftmode.job.skill;

import com.minecraftmode.city.CityServices;
import com.minecraftmode.job.JobClass;
import com.minecraftmode.job.JobData;
import com.minecraftmode.job.JobProgression;
import com.minecraftmode.job.JobStats;
import com.minecraftmode.job.engrave.EngraveStat;
import com.minecraftmode.job.engrave.EngraveTotals;
import com.minecraftmode.job.weapon.JobWeapons;
import com.minecraftmode.registry.ModEffects;
import java.util.Map;
import java.util.WeakHashMap;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Damage pipeline for classes: class passives, engravings, skill buffs and marks. Outgoing and
 * incoming damage is adjusted in {@link #modifyIncoming} (called from {@code LivingEntityMixin} at
 * the start of {@code hurtServer}); dodging and on-hit effects use Fabric events.
 *
 * <p>Damage dealt by skills and class projectiles goes through {@link #deal}, which tags it with a
 * {@link DamageKind} so basic-attack bonuses are not applied twice.
 */
public final class CombatHooks {
	public enum DamageKind {
		/** Basic shot of a class weapon (projectile). */
		SHOT,
		SKILL,
		/** Wide Swing splash; never splashes again. */
		SPLASH,
		/** Counter stance; never reflected again. */
		REFLECT
	}

	private static @Nullable DamageKind current;
	/** Vanilla arrows loosed by class bows -> their damage. */
	private static final Map<Entity, Float> ARROWS = new WeakHashMap<>();
	/** Vanilla arrows fired by skills -> the skill that fired them. */
	private static final Map<Entity, SkillArrow> SKILL_ARROWS = new WeakHashMap<>();

	private record SkillArrow(SkillContext ctx, double multiplier) {
	}

	public static void init() {
		ServerLivingEntityEvents.ALLOW_DAMAGE.register(CombatHooks::allowDamage);
		ServerLivingEntityEvents.AFTER_DAMAGE.register(CombatHooks::afterDamage);
	}

	public static void trackArrow(final Entity arrow, final float damage) {
		ARROWS.put(arrow, damage);
	}

	public static void trackSkillArrow(final Entity arrow, final SkillContext ctx, final double multiplier) {
		SKILL_ARROWS.put(arrow, new SkillArrow(ctx, multiplier));
	}

	/** Deals {@code amount} as {@code kind}; resets the target's damage cooldown so multi-hits land. */
	public static boolean deal(final ServerPlayer attacker, final LivingEntity target, final float amount, final DamageSource source, final DamageKind kind) {
		DamageKind previous = current;
		current = kind;
		try {
			target.damageCooldownTime = 0;
			return target.hurtServer(attacker.level(), source, amount);
		} finally {
			current = previous;
		}
	}

	// ------------------------------------------------------------------ targeting

	/** Hostile to the caster: monsters, mobs targeting them, and players when PvP and teams allow it. */
	public static boolean isEnemy(final Player caster, final Entity entity) {
		if (!canHarm(caster, entity)) {
			return false;
		}
		if (entity instanceof Player) {
			return true;
		}
		return entity instanceof Enemy || entity instanceof Mob mob && mob.getTarget() == caster;
	}

	/** Anything the caster may hurt with a basic shot: not themself, their pets, allies or protected players. */
	public static boolean canHarm(final Player caster, final Entity entity) {
		if (!(entity instanceof LivingEntity living) || !living.isAlive() || living == caster || living.isSpectator() || living instanceof ArmorStand) {
			return false;
		}
		if (living instanceof OwnableEntity ownable && ownable.getOwner() == caster) {
			return false;
		}
		if (living.isAlliedTo(caster) || CityServices.blocksPvp(living, caster)) {
			return false;
		}
		return !(living instanceof Player player) || caster.canHarmPlayer(player);
	}

	// ------------------------------------------------------------------ damage adjustment

	public static float modifyIncoming(final LivingEntity victim, final DamageSource source, final float amount) {
		if (!(victim.level() instanceof ServerLevel level)) {
			return amount;
		}
		long now = level.getGameTime();
		DamageKind kind = current;
		float result = amount;
		Entity direct = source.getDirectEntity();
		Float arrowDamage = direct == null ? null : ARROWS.get(direct);
		if (arrowDamage != null && kind == null) {
			result = arrowDamage;
			kind = DamageKind.SHOT;
		}
		SkillArrow skillArrow = direct == null ? null : SKILL_ARROWS.get(direct);
		if (skillArrow != null && kind == null) {
			result = skillArrow.ctx.damageFor(skillArrow.multiplier);
			kind = DamageKind.SKILL;
			victim.damageCooldownTime = 0;
		}
		if (source.getEntity() instanceof ServerPlayer attacker && attacker != victim) {
			result = outgoing(attacker, victim, source, result, kind, now);
		}
		MobEffectInstance vulnerable = victim.getEffect(ModEffects.VULNERABLE);
		if (vulnerable != null) {
			result *= 1.0F + 0.15F * (vulnerable.getAmplifier() + 1);
		}
		if (victim instanceof ServerPlayer player) {
			result = incoming(player, source, result, kind, now);
		}
		return result;
	}

	private static float outgoing(
		final ServerPlayer attacker, final LivingEntity victim, final DamageSource source, final float amount, final @Nullable DamageKind kind, final long now
	) {
		JobData data = JobProgression.get(attacker);
		EngraveTotals mods = JobWeapons.activeTotals(attacker);
		boolean melee = kind == null && source.getDirectEntity() == attacker;
		boolean basic = melee || kind == DamageKind.SHOT;
		CombatState state = CombatState.of(attacker);
		float bonus = 0.0F;
		float crit = 1.0F;
		if (basic) {
			if (melee) {
				bonus += mods.fraction(EngraveStat.BASIC_DAMAGE);
				if (has(data, JobClass.WARRIOR, 2)) {
					bonus += 0.10F;
				}
			}
			float critChance = mods.fraction(EngraveStat.CRIT_CHANCE) + (has(data, JobClass.ROGUE, 2) ? 0.15F : 0.0F);
			if (critChance > 0.0F && attacker.getRandom().nextFloat() < critChance) {
				crit = 1.5F + mods.fraction(EngraveStat.CRIT_DAMAGE);
				ServerLevel level = attacker.level();
				level.sendParticles(ParticleTypes.CRIT, victim.getX(), victim.getY(0.6), victim.getZ(), 12, 0.3, 0.3, 0.3, 0.3);
				level.playSound(null, victim.getX(), victim.getY(), victim.getZ(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 0.8F, 1.2F);
			}
			if (isBehind(attacker, victim)) {
				bonus += mods.fraction(EngraveStat.BACKSTAB) + (has(data, JobClass.ROGUE, 3) ? 0.30F : 0.0F);
			}
			if (victim.getHealth() < victim.getMaxHealth() * 0.3F) {
				bonus += mods.fraction(EngraveStat.EXECUTE);
			}
			if (state.empowerHits > 0 && now < state.empowerUntil) {
				bonus += state.empowerBonus;
				state.empowerHits--;
			}
			if (state.stealthBonus > 0.0F && now < state.stealthUntil) {
				bonus += state.stealthBonus;
				state.stealthBonus = 0.0F;
				attacker.removeEffect(MobEffects.INVISIBILITY);
			}
			if (kind == DamageKind.SHOT) {
				float distance = attacker.distanceTo(victim);
				bonus += mods.fraction(EngraveStat.RANGE_BONUS) * distance / 10.0F;
				if (has(data, JobClass.ARCHER, 3)) {
					bonus += Math.min(0.40F, 0.02F * (int)(distance / 4.0F));
				}
			}
		}
		if (has(data, JobClass.WARRIOR, 3) && attacker.getHealth() < attacker.getMaxHealth() * 0.4F) {
			bonus += 0.25F;
		}
		bonus += state.markBonus(victim.getUUID(), now);
		return amount * (1.0F + bonus) * crit;
	}

	private static float incoming(final ServerPlayer player, final DamageSource source, final float amount, final @Nullable DamageKind kind, final long now) {
		CombatState state = CombatState.of(player);
		EngraveTotals mods = JobWeapons.activeTotals(player);
		float reduce = state.stanceValue(CombatState.Stance.GUARD, now) / 100.0F + mods.fraction(EngraveStat.DAMAGE_REDUCTION);
		float result = amount * (1.0F - Math.min(0.8F, reduce));
		float counter = state.stanceValue(CombatState.Stance.COUNTER, now);
		if (counter > 0.0F && kind != DamageKind.REFLECT && source.getEntity() instanceof LivingEntity attacker && attacker != player && attacker.isAlive()) {
			float reflected = amount * counter / 100.0F;
			SkillScheduler.schedule(1, () -> {
				if (attacker.isAlive() && player.isAlive()) {
					deal(player, attacker, reflected, player.damageSources().thorns(player), DamageKind.REFLECT);
					player.level().sendParticles(ParticleTypes.ENCHANTED_HIT, attacker.getX(), attacker.getY(0.5), attacker.getZ(), 10, 0.3, 0.3, 0.3, 0.2);
				}
			});
		}
		return result;
	}

	// ------------------------------------------------------------------ events

	private static boolean allowDamage(final LivingEntity victim, final DamageSource source, final float amount) {
		if (source.getEntity() != null && CityServices.blocksPvp(victim, source.getEntity())) {
			return false;
		}
		if (!(victim instanceof ServerPlayer player) || source.getEntity() == null || source.getEntity() == player
			|| source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
			return true;
		}
		JobData data = JobProgression.get(player);
		long now = player.level().getGameTime();
		float chance = CombatState.of(player).stanceValue(CombatState.Stance.EVADE, now) / 100.0F
			+ (has(data, JobClass.ROGUE, 4) ? 0.15F : 0.0F)
			+ JobWeapons.activeTotals(player).fraction(EngraveStat.DODGE);
		if (chance > 0.0F && player.getRandom().nextFloat() < Math.min(0.75F, chance)) {
			ServerLevel level = player.level();
			level.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY(0.5), player.getZ(), 6, 0.3, 0.4, 0.3, 0.02);
			level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_ATTACK_NODAMAGE, SoundSource.PLAYERS, 0.8F, 1.6F);
			return false;
		}
		return true;
	}

	private static void afterDamage(final LivingEntity victim, final DamageSource source, final float baseDamage, final float damageTaken, final boolean blocked) {
		if (damageTaken <= 0.0F || !(source.getEntity() instanceof ServerPlayer attacker) || attacker == victim) {
			return;
		}
		DamageKind kind = current;
		Entity direct = source.getDirectEntity();
		SkillArrow skillArrow = kind == null && direct != null ? SKILL_ARROWS.get(direct) : null;
		if (skillArrow != null) {
			skillArrow.ctx.afterArrowHit(victim, damageTaken);
			return;
		}
		boolean melee = kind == null && direct == attacker;
		boolean shot = kind == DamageKind.SHOT || kind == null && direct != null && ARROWS.containsKey(direct);
		JobData data = JobProgression.get(attacker);
		EngraveTotals mods = JobWeapons.activeTotals(attacker);
		float lifesteal = 0.0F;
		if (melee || shot) {
			lifesteal += mods.fraction(EngraveStat.LIFESTEAL);
			int manaOnHit = (int)mods.get(EngraveStat.MANA_ON_HIT);
			if (manaOnHit > 0) {
				JobStats.addMana(attacker, manaOnHit);
			}
			applyOnHit(victim, mods);
			float splash = mods.get(EngraveStat.SPLASH);
			if (melee && splash > 0.0F) {
				splash(attacker, victim, damageTaken * 0.5F, splash);
			}
		}
		if (has(data, JobClass.WARRIOR, 3) && attacker.getHealth() < attacker.getMaxHealth() * 0.4F) {
			lifesteal += 0.05F;
		}
		if (lifesteal > 0.0F) {
			attacker.heal(damageTaken * lifesteal);
		}
		if (has(data, JobClass.PIRATE, 3) && kind != DamageKind.SPLASH && kind != DamageKind.REFLECT && victim.isAlive()
			&& attacker.getRandom().nextFloat() < 0.10F) {
			victim.addEffect(new MobEffectInstance(ModEffects.STUN, 30, 0), attacker);
			attacker.level().sendParticles(ParticleTypes.ELECTRIC_SPARK, victim.getX(), victim.getY(1.0), victim.getZ(), 10, 0.3, 0.2, 0.3, 0.1);
		}
	}

	private static void applyOnHit(final LivingEntity victim, final EngraveTotals mods) {
		if (!victim.isAlive()) {
			return;
		}
		int poison = (int)(mods.get(EngraveStat.POISON) * 20);
		if (poison > 0) {
			victim.addEffect(new MobEffectInstance(MobEffects.POISON, poison, 0));
		}
		int bleed = (int)(mods.get(EngraveStat.BLEED) * 20);
		if (bleed > 0) {
			victim.addEffect(new MobEffectInstance(ModEffects.BLEEDING, bleed, 0));
		}
		float burn = mods.get(EngraveStat.BURN);
		if (burn > 0.0F) {
			victim.igniteForSeconds(burn);
		}
		int slow = (int)(mods.get(EngraveStat.SLOW) * 20);
		if (slow > 0) {
			victim.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, slow, 1));
		}
	}

	private static void splash(final ServerPlayer attacker, final LivingEntity victim, final float damage, final float radius) {
		ServerLevel level = attacker.level();
		for (LivingEntity other : level.getEntitiesOfClass(LivingEntity.class, victim.getBoundingBox().inflate(radius), e -> e != victim && isEnemy(attacker, e))) {
			if (other.distanceTo(victim) <= radius + other.getBbWidth() / 2) {
				deal(attacker, other, damage, attacker.damageSources().playerAttack(attacker), DamageKind.SPLASH);
			}
		}
		level.sendParticles(ParticleTypes.SWEEP_ATTACK, victim.getX(), victim.getY(0.5), victim.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
	}

	// ------------------------------------------------------------------ helpers

	public static boolean has(final JobData data, final JobClass job, final int tier) {
		return data.job() == job && data.tier() >= tier;
	}

	/** True when the attacker stands behind the victim (relative to where the victim faces). */
	public static boolean isBehind(final Entity attacker, final LivingEntity victim) {
		Vec3 facing = Vec3.directionFromRotation(0.0F, victim.getYRot());
		Vec3 toAttacker = attacker.position().subtract(victim.position()).multiply(1.0, 0.0, 1.0);
		if (toAttacker.lengthSqr() < 1.0E-4) {
			return false;
		}
		return facing.dot(toAttacker.normalize()) < -0.5;
	}

	private CombatHooks() {
	}
}
