package com.minecraftmode.job.skill;

import com.minecraftmode.job.JobProgression;
import com.minecraftmode.job.JobStats;
import com.minecraftmode.job.weapon.JobWeapons;
import com.minecraftmode.job.weapon.ProjectileStyle;
import com.minecraftmode.progress.Contribution;
import com.minecraftmode.registry.ModEffects;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.animal.golem.SnowGolem;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The skill building blocks. Content classes compose these; every factory documents its units:
 * distances in blocks, durations in seconds, {@code mult} = multiple of the weapon's skill power
 * (1.5 = 150%). Each action also produces its own tooltip line.
 */
public final class Actions {
	/** Tag on summoned helpers; they are removed when they expire or get loaded again. */
	public static final String SUMMON_TAG = "minecraft_mode_summon";
	/** Tag prefix naming the player who summoned a helper (golems have no owner of their own). */
	private static final String SUMMONER_TAG = "minecraft_mode_summoner:";
	/** Falling projectiles of {@link #rain}: speed and how high above the ground they start. */
	private static final float RAIN_SPEED = 1.8F;
	private static final double RAIN_HEIGHT = 14.0;
	/** Speed of the projectiles {@link #barrage} launches from its portals. */
	private static final float BARRAGE_SPEED = 2.6F;

	/** The player a skill summon fights for, or null for anything else. */
	public static @Nullable UUID summoner(final @Nullable Entity entity) {
		if (entity == null || !entity.entityTags().contains(SUMMON_TAG)) {
			return null;
		}
		for (String tag : entity.entityTags()) {
			if (tag.startsWith(SUMMONER_TAG)) {
				try {
					return UUID.fromString(tag.substring(SUMMONER_TAG.length()));
				} catch (IllegalArgumentException e) {
					return null;
				}
			}
		}
		return null;
	}

	private static final Map<String, String[]> TEXTS = new LinkedHashMap<>();

	private static String key(final String id, final String en, final String ko) {
		TEXTS.put(id, new String[] {en, ko});
		return "skill_action.minecraft_mode." + id;
	}

	private static final String SLASH = key("slash", "Slash in a %2$s° arc within %1$s blocks: %3$s%% damage", "%1$s블록 %2$s° 베기: 피해 %3$s%%");
	private static final String NOVA = key("nova", "Strike all enemies within %1$s blocks: %2$s%% damage", "주변 %1$s블록 적 전체 타격: 피해 %2$s%%");
	private static final String DASH = key("dash", "Dash %1$s blocks forward", "앞으로 %1$s블록 돌진");
	private static final String DASH_HIT = key("dash_hit", "Dash %1$s blocks, hitting enemies in the way: %2$s%% damage", "%1$s블록 돌진하며 경로의 적 타격: 피해 %2$s%%");
	private static final String LEAP = key("leap", "Leap up and crash down, hitting enemies within %1$s blocks: %2$s%% damage", "도약 후 내려찍어 %1$s블록 내 적 타격: 피해 %2$s%%");
	private static final String SHOOT = key("shoot", "Fire %1$s x%2$s: %3$s%% damage each", "%1$s x%2$s 발사: 각 피해 %3$s%%");
	private static final String PIERCE = key("pierce", " (pierces %s)", " (%s회 관통)");
	private static final String EXPLODE = key("explode", " (explodes, %s-block radius)", " (폭발 반경 %s블록)");
	private static final String HOMING = key("homing", " (homing)", " (유도)");
	private static final String BEAM = key("beam", "Fire a %1$s-block beam through all enemies: %2$s%% damage", "%1$s블록 광선으로 관통 타격: 피해 %2$s%%");
	private static final String BLINK = key("blink", "Teleport up to %s blocks forward", "앞으로 최대 %s블록 순간이동");
	private static final String SHADOWSTEP = key("shadowstep", "Appear behind a target within %1$s blocks and strike: %2$s%% damage", "%1$s블록 내 대상의 뒤로 이동해 공격: 피해 %2$s%%");
	private static final String BUFF = key("buff", "Gain %1$s %2$s for %3$ss", "%3$s초간 %1$s %2$s 획득");
	private static final String ALLY_BUFF = key("ally_buff", "You and allies within %1$s blocks gain %2$s %3$s for %4$ss", "자신과 %1$s블록 내 아군에게 %4$s초간 %2$s %3$s");
	private static final String INFLICT = key("inflict", "Enemies hit: %1$s %2$s for %3$ss", "적중한 적: %3$s초간 %1$s %2$s");
	private static final String BURN = key("burn", "Enemies hit burn for %ss", "적중한 적 %s초간 화상");
	private static final String HEAL = key("heal", "Restore %s%% health", "체력 %s%% 회복");
	private static final String ALLY_HEAL = key("ally_heal", "Restore %2$s%% health to you and allies within %1$s blocks", "자신과 %1$s블록 내 아군 체력 %2$s%% 회복");
	private static final String SHIELD = key("shield", "Gain a %1$s-point shield for %2$ss", "%2$s초간 보호막 %1$s");
	private static final String PULL = key("pull", "Pull enemies within %s blocks toward you", "%s블록 내 적을 끌어당김");
	private static final String PUSH = key("push", "Knock back enemies within %s blocks", "%s블록 내 적을 밀쳐냄");
	private static final String LAUNCH = key("launch", "Launch enemies within %s blocks into the air", "%s블록 내 적을 공중으로 띄움");
	private static final String STRIKE = key("strike", "Call %3$s strikes on the target area (%2$s-block radius, up to %1$s blocks away): %4$s%% damage each",
		"최대 %1$s블록 앞 지점에 %3$s회 낙하 공격 (반경 %2$s블록): 각 피해 %4$s%%");
	private static final String LIGHTNING = key("lightning", "Call lightning on the target area (%2$s-block radius, up to %1$s blocks away): %3$s%% damage",
		"최대 %1$s블록 앞 지점에 낙뢰 (반경 %2$s블록): 피해 %3$s%%");
	private static final String CHAIN = key("chain", "Chain between up to %2$s enemies (from %1$s blocks away): %3$s%% damage each", "%1$s블록 내 적부터 최대 %2$s명에게 연쇄: 각 피해 %3$s%%");
	private static final String ZONE = key("zone", "Create a %1$s-block field for %2$ss: %3$s%% damage per second", "%2$s초간 반경 %1$s블록 장판: 초당 피해 %3$s%%");
	private static final String ZONE_HEAL = key("zone_heal", "Create a %1$s-block sanctuary for %2$ss: allies heal %3$s%% per second", "%2$s초간 반경 %1$s블록 성역: 아군 초당 체력 %3$s%% 회복");
	private static final String ZONE_TARGET = key("zone_target", " at the target point", " (지정 지점)");
	private static final String ZONE_PULL = key("zone_pull", " that pulls enemies in", " (적을 끌어당김)");
	private static final String DEBUFF = key("debuff", "Enemies within %1$s blocks: %2$s %3$s for %4$ss", "%1$s블록 내 적: %4$s초간 %2$s %3$s");
	private static final String TAUNT = key("taunt", "Taunt enemies within %s blocks", "%s블록 내 적 도발");
	private static final String GUARD = key("guard", "Guard stance for %1$ss: -%2$s%% damage taken", "%1$s초간 방어 태세: 받는 피해 -%2$s%%");
	private static final String COUNTER = key("counter", "Counter stance for %1$ss: reflect %2$s%% of damage taken", "%1$s초간 반격 태세: 받은 피해의 %2$s%% 반사");
	private static final String EVADE = key("evade", "Evasion for %1$ss: %2$s%% chance to dodge", "%1$s초간 회피 태세: %2$s%% 확률로 회피");
	private static final String EMPOWER = key("empower", "Your next %1$s basic attacks within %3$ss deal +%2$s%% damage", "%3$s초 안의 다음 기본 공격 %1$s회 피해 +%2$s%%");
	private static final String STEALTH = key("stealth", "Turn invisible for %1$ss; your next attack deals +%2$s%% damage", "%1$s초간 은신, 다음 공격 피해 +%2$s%%");
	private static final String MARK = key("mark", "Mark a target within %1$s blocks: it takes +%3$s%% damage from you for %2$ss", "%1$s블록 내 대상 표식: %2$s초간 내게 받는 피해 +%3$s%%");
	private static final String EXECUTE = key("execute", "Strike a target within %1$s blocks: %2$s%% damage (x%4$s below %3$s%% health)", "%1$s블록 내 대상 일격: 피해 %2$s%% (체력 %3$s%% 미만이면 x%4$s)");
	private static final String SUMMON = key("summon", "Summon %2$s x%1$s for %3$ss", "%3$s초간 %2$s x%1$s 소환");
	private static final String CLEANSE = key("cleanse", "Remove harmful effects and fire", "해로운 효과와 불 제거");
	private static final String RAIN = key("rain", "Rain %3$s x%4$s on the enemies in the target area (%2$s-block radius, up to %1$s blocks away): %5$s%% damage each",
		"최대 %1$s블록 앞 지점(반경 %2$s블록)의 적들에게 %3$s x%4$s 낙하: 각 피해 %5$s%%");
	private static final String BARRAGE = key("barrage", "Launch %1$s x%2$s from portals behind you: %3$s%% damage each", "등 뒤의 문에서 %1$s x%2$s 발사: 각 피해 %3$s%%");
	private static final String GRAPPLE = key("grapple", "Grapple to a block up to %s blocks away", "최대 %s블록 떨어진 블록으로 갈고리 이동");
	private static final String MANA = key("mana", "Restore %s MP", "MP %s 회복");
	private static final String REPEAT = key("repeat", "%1$s (x%2$s)", "%1$s (x%2$s)");
	private static final String DELAY = key("delay", "After %2$ss: %1$s", "%2$s초 후: %1$s");
	private static final String LIFESTEAL = key("lifesteal", "Heal for %s%% of damage dealt", "입힌 피해의 %s%% 회복");
	private static final String LOOT = key("loot", "Enemies hit have a %s%% chance to drop a coin", "적중한 적이 %s%% 확률로 동전 드롭");
	private static final String REFRESH = key("refresh", "Reduce this weapon's other cooldowns by %s%%", "이 무기의 다른 스킬 대기시간 %s%% 감소");

	/** Description templates (id -> {en, ko}) for datagen. */
	public static Map<String, String[]> texts() {
		return Collections.unmodifiableMap(TEXTS);
	}

	// ================================================================== formatting

	static String num(final double v) {
		double r = Math.round(v * 10.0) / 10.0;
		return r == Math.rint(r) ? Long.toString((long)r) : String.format(Locale.ROOT, "%.1f", r);
	}

	static String pct(final double mult) {
		return num(mult * 100.0);
	}

	static Component effectName(final Holder<MobEffect> effect) {
		return Component.translatable(effect.value().getDescriptionId());
	}

	static Component level(final int amplifier) {
		return Component.translatable("enchantment.level." + (amplifier + 1));
	}

	static Component style(final ProjectileStyle style) {
		return Component.translatable(style.nameKey());
	}

	private static SkillAction action(final Consumer<SkillContext> run, final Component description) {
		return new SkillAction() {
			@Override
			public void run(final SkillContext ctx) {
				run.accept(ctx);
			}

			@Override
			public Component describe() {
				return description;
			}
		};
	}

	/** A step that can hurt enemies. */
	private static SkillAction attack(final Consumer<SkillContext> run, final Component description) {
		return tagged(run, description, false, true);
	}

	/** A step that takes the caster somewhere, hurting what it passes or lands on when {@code damages}. */
	private static SkillAction move(final Consumer<SkillContext> run, final Component description, final boolean damages) {
		return tagged(run, description, true, damages);
	}

	private static SkillAction tagged(final Consumer<SkillContext> run, final Component description, final boolean moves, final boolean damages) {
		return new SkillAction() {
			@Override
			public void run(final SkillContext ctx) {
				run.accept(ctx);
			}

			@Override
			public Component describe() {
				return description;
			}

			@Override
			public boolean moves() {
				return moves;
			}

			@Override
			public boolean damages() {
				return damages;
			}
		};
	}

	/** Same flags as {@code inner} (for repeat and delay). */
	private static SkillAction wrap(final SkillAction inner, final Consumer<SkillContext> run, final Component description) {
		return tagged(run, description, inner.moves(), inner.damages());
	}

	private static SkillAction modifier(final Consumer<SkillContext> run, final Component description) {
		return new SkillAction() {
			@Override
			public void run(final SkillContext ctx) {
				run.accept(ctx);
			}

			@Override
			public Component describe() {
				return description;
			}

			@Override
			public boolean isModifier() {
				return true;
			}
		};
	}

	// ================================================================== helpers

	static void sound(final SkillContext ctx, final SoundEvent sound, final float volume, final float pitch) {
		ctx.level.playSound(null, ctx.caster.getX(), ctx.caster.getY(), ctx.caster.getZ(), sound, SoundSource.PLAYERS, volume, pitch);
	}

	static void soundAt(final SkillContext ctx, final Vec3 pos, final SoundEvent sound, final float volume, final float pitch) {
		ctx.level.playSound(null, pos.x, pos.y, pos.z, sound, SoundSource.PLAYERS, volume, pitch);
	}

	/** Sets velocity and makes sure clients (including a pushed player) hear about it. */
	static void impulse(final Entity entity, final Vec3 velocity) {
		entity.setDeltaMovement(velocity);
		if (entity instanceof ServerPlayer) {
			entity.syncVelocity = true;
		} else {
			entity.needsSync = true;
		}
	}

	// ================================================================== melee / area

	/** Cone in front of the caster. */
	public static SkillAction slash(final double range, final double arc, final double mult) {
		return attack(ctx -> {
			double r = ctx.area(range);
			for (LivingEntity e : ctx.enemiesInCone(r, arc)) {
				ctx.hit(e, mult);
			}
			ctx.fx.arc(ctx.level, ctx.eye(), ctx.caster.getYRot(), Math.max(1.5, r * 0.7), arc);
			sound(ctx, SoundEvents.PLAYER_ATTACK_SWEEP, 1.0F, 0.9F + ctx.caster.getRandom().nextFloat() * 0.2F);
		}, Component.translatable(SLASH, num(range), num(arc), pct(mult)));
	}

	/** Everything around the caster. */
	public static SkillAction nova(final double radius, final double mult) {
		return attack(ctx -> {
			double r = ctx.area(radius);
			Vec3 c = ctx.caster.position().add(0, 0.2, 0);
			for (LivingEntity e : ctx.enemiesNear(c, r)) {
				ctx.hit(e, mult);
			}
			ctx.fx.still(ctx.level, Fx.Kind.RING, c);
			ctx.fx.circle(ctx.level, ctx.fx.kind(), c, r);
			ctx.fx.burst(ctx.level, c.add(0, 1, 0), (int)(r * 6), r / 2, 0.1);
			sound(ctx, SoundEvents.PLAYER_ATTACK_SWEEP, 1.0F, 0.7F);
		}, Component.translatable(NOVA, num(radius), pct(mult)));
	}

	/** Dashes over a few ticks, hitting each enemy in the path once ({@code mult} 0 = no damage). */
	public static SkillAction dash(final double distance, final double mult) {
		Component text = mult > 0 ? Component.translatable(DASH_HIT, num(distance), pct(mult)) : Component.translatable(DASH, num(distance));
		return move(ctx -> {
			// follow the path itself (2 blocks a tick) so steps never cut through slopes
			List<Vec3> path = new ArrayList<>();
			path.add(ctx.caster.position());
			path.addAll(Movement.path(ctx.caster, ctx.flatLook(), distance, 0.0));
			List<Vec3> stops = new ArrayList<>();
			for (int i = 4; i < path.size(); i += 4) {
				stops.add(path.get(i));
			}
			if (stops.isEmpty() || !stops.getLast().equals(path.getLast())) {
				stops.add(path.getLast());
			}
			Set<UUID> hit = new HashSet<>();
			ctx.guard(stops.size() + Engage.GUARD_TICKS);
			sound(ctx, SoundEvents.PLAYER_ATTACK_SWEEP, 0.8F, 1.5F);
			for (int i = 0; i < stops.size(); i++) {
				final Vec3 from = i == 0 ? path.getFirst() : stops.get(i - 1);
				final Vec3 to = stops.get(i);
				SkillScheduler.schedule(i + 1, () -> {
					if (!ctx.valid()) {
						return;
					}
					Movement.teleport(ctx.caster, to);
					ctx.fx.line(ctx.level, from.add(0, 1, 0), to.add(0, 1, 0), 0.4);
					if (mult > 0) {
						for (LivingEntity e : ctx.enemiesAlong(from.add(0, 1, 0), to.add(0, 1, 0), 1.2)) {
							if (hit.add(e.getUUID())) {
								ctx.hit(e, mult);
							}
						}
					}
				});
			}
		}, text, mult > 0);
	}

	/** Jump, then slam on landing. */
	public static SkillAction leap(final double height, final double radius, final double mult) {
		return move(ctx -> {
			Vec3 dir = ctx.flatLook();
			impulse(ctx.caster, new Vec3(dir.x * 0.9, 0.5 + height * 0.12, dir.z * 0.9));
			sound(ctx, SoundEvents.PLAYER_ATTACK_STRONG, 1.0F, 0.6F);
			ctx.guard(Engage.AIR_TICKS);
			Movement.guardFall(ctx.caster, () -> {
				ctx.guard(Engage.GUARD_TICKS);
				double r = ctx.area(radius);
				Vec3 c = ctx.caster.position();
				for (LivingEntity e : ctx.enemiesNear(c, r)) {
					ctx.hit(e, mult);
				}
				ctx.fx.still(ctx.level, Fx.Kind.RING, c.add(0, 0.1, 0));
				ctx.fx.circle(ctx.level, Fx.Kind.SHARD, c.add(0, 0.2, 0), r);
				ctx.level.sendParticles(ParticleTypes.EXPLOSION, c.x, c.y + 0.5, c.z, 1, 0, 0, 0, 0);
				soundAt(ctx, c, SoundEvents.GENERIC_EXPLODE.value(), 0.8F, 0.8F);
			});
		}, Component.translatable(LEAP, num(radius), pct(mult)), mult > 0);
	}

	/** Instant line through every enemy. */
	public static SkillAction beam(final double length, final double mult) {
		return attack(ctx -> {
			Vec3 from = ctx.eye().add(0, -0.2, 0);
			Vec3 to = ctx.lookPoint(length);
			for (LivingEntity e : ctx.enemiesAlong(from, to, 0.9)) {
				ctx.hit(e, mult);
			}
			ctx.fx.line(ctx.level, from.add(ctx.look()), to, 0.3);
			ctx.fx.still(ctx.level, Fx.Kind.RING, to);
			sound(ctx, SoundEvents.BEACON_ACTIVATE, 0.8F, 1.8F);
		}, Component.translatable(BEAM, num(length), pct(mult)));
	}

	public static SkillAction blink(final double distance) {
		return move(ctx -> {
			Vec3 start = ctx.caster.position();
			Vec3 end = Movement.end(ctx.caster, ctx.look(), distance, Movement.rise(ctx.caster, distance));
			ctx.fx.burst(ctx.level, Fx.Kind.SMOKE, start.add(0, 1, 0), 15, 0.4, 0.02);
			Movement.teleport(ctx.caster, end);
			ctx.guard(Engage.GUARD_TICKS);
			ctx.fx.burst(ctx.level, end.add(0, 1, 0), 15, 0.4, 0.05);
			sound(ctx, SoundEvents.ENDERMAN_TELEPORT, 0.8F, 1.3F);
		}, Component.translatable(BLINK, num(distance)), false);
	}

	public static SkillAction shadowstep(final double range, final double mult) {
		return move(ctx -> {
			LivingEntity target = ctx.lookTarget(range);
			if (target == null) {
				return;
			}
			Vec3 behind = target.position().subtract(Vec3.directionFromRotation(0.0F, target.getYRot()).scale(1.2));
			ctx.fx.burst(ctx.level, Fx.Kind.SMOKE, ctx.caster.position().add(0, 1, 0), 15, 0.4, 0.02);
			if (Movement.canStand(ctx.caster, behind) && Movement.clearLine(ctx.caster, target.position(), behind)) {
				Movement.teleport(ctx.caster, behind);
				ctx.caster.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, target.getEyePosition());
			}
			ctx.guard(Engage.GUARD_TICKS);
			ctx.hit(target, mult);
			ctx.fx.burst(ctx.level, Fx.Kind.SLASH, target.position().add(0, target.getBbHeight() / 2, 0), 4, 0.3, 0.0);
			sound(ctx, SoundEvents.PLAYER_ATTACK_CRIT, 1.0F, 1.2F);
		}, Component.translatable(SHADOWSTEP, num(range), pct(mult)), true);
	}

	public static SkillAction execute(final double range, final double mult, final double thresholdPct, final double factor) {
		return attack(ctx -> {
			LivingEntity target = ctx.lookTarget(range);
			if (target == null) {
				return;
			}
			boolean low = target.getHealth() < target.getMaxHealth() * thresholdPct / 100.0;
			ctx.hit(target, low ? mult * factor : mult);
			Vec3 c = target.position().add(0, target.getBbHeight() / 2, 0);
			ctx.fx.burst(ctx.level, Fx.Kind.SLASH, c, low ? 8 : 3, 0.4, 0.0);
			if (low) {
				ctx.level.sendParticles(ParticleTypes.DAMAGE_INDICATOR, c.x, c.y, c.z, 10, 0.3, 0.3, 0.3, 0.2);
			}
			sound(ctx, low ? SoundEvents.PLAYER_ATTACK_CRIT : SoundEvents.PLAYER_ATTACK_STRONG, 1.0F, 0.8F);
		}, Component.translatable(EXECUTE, num(range), pct(mult), num(thresholdPct), num(factor)));
	}

	public static SkillAction chain(final double range, final int jumps, final double mult) {
		return attack(ctx -> {
			LivingEntity current = ctx.lookTarget(range);
			if (current == null) {
				current = ctx.enemiesNear(ctx.caster.position(), range).stream().min((a, b) -> Double.compare(a.distanceToSqr(ctx.caster), b.distanceToSqr(ctx.caster))).orElse(null);
			}
			Vec3 from = ctx.eye();
			Set<UUID> done = new HashSet<>();
			for (int i = 0; i < jumps && current != null; i++) {
				Vec3 to = current.getBoundingBox().getCenter();
				ctx.fx.withKind(Fx.Kind.BOLT).line(ctx.level, from, to, 0.5);
				ctx.hit(current, mult);
				done.add(current.getUUID());
				from = to;
				final Vec3 at = to;
				current = ctx.enemiesNear(at, 6.0).stream().filter(e -> !done.contains(e.getUUID()))
					.min((a, b) -> Double.compare(a.distanceToSqr(at), b.distanceToSqr(at))).orElse(null);
			}
			sound(ctx, SoundEvents.TRIDENT_THUNDER.value(), 0.5F, 1.6F);
		}, Component.translatable(CHAIN, num(range), jumps, pct(mult)));
	}

	// ================================================================== projectiles

	public static ProjectileAction shoot(final ProjectileStyle style, final double mult) {
		return new ProjectileAction(style, mult);
	}

	/** Builder for projectile skills: {@code shoot(SHURIKEN, 0.8).count(3).spread(30)}. */
	public static final class ProjectileAction implements SkillAction {
		private final ProjectileStyle style;
		private final double mult;
		private int count = 1;
		private double spread = 0.0;
		private float speed;
		private int pierce;
		private float explode;
		private boolean homing;
		private int interval;
		private @Nullable BiConsumer<SkillContext, Vec3> impact;

		ProjectileAction(final ProjectileStyle style, final double mult) {
			this.style = style;
			this.mult = mult;
			this.speed = style.speed();
		}

		@Override
		public boolean damages() {
			return this.mult > 0;
		}

		public ProjectileAction count(final int count) {
			this.count = count;
			return this;
		}

		/** Total fan angle in degrees. */
		public ProjectileAction spread(final double degrees) {
			this.spread = degrees;
			return this;
		}

		public ProjectileAction speed(final float speed) {
			this.speed = speed;
			return this;
		}

		public ProjectileAction pierce(final int pierce) {
			this.pierce = pierce;
			return this;
		}

		public ProjectileAction explode(final float radius) {
			this.explode = radius;
			return this;
		}

		public ProjectileAction homing() {
			this.homing = true;
			return this;
		}

		/** Fire one by one, {@code ticks} apart, instead of all at once. */
		public ProjectileAction interval(final int ticks) {
			this.interval = ticks;
			return this;
		}

		/** Extra effect where it lands (e.g. a fire field). */
		public ProjectileAction onImpact(final BiConsumer<SkillContext, Vec3> impact) {
			this.impact = impact;
			return this;
		}

		@Override
		public void run(final SkillContext ctx) {
			for (int i = 0; i < this.count; i++) {
				double yaw = this.count == 1 ? 0.0 : -this.spread / 2 + this.spread * i / (this.count - 1);
				if (this.interval > 0 && i > 0) {
					SkillScheduler.schedule(i * this.interval, () -> {
						if (ctx.valid()) {
							this.fire(ctx, yaw);
						}
					});
				} else {
					this.fire(ctx, yaw);
				}
			}
		}

		private void fire(final SkillContext ctx, final double yawOffset) {
			ServerPlayer p = ctx.caster;
			Vec3 dir = Vec3.directionFromRotation(p.getXRot(), (float)(p.getYRot() + yawOffset));
			Vec3 from = p.getEyePosition().add(0, -0.15, 0);
			spawn(ctx, this.style, from, dir, this.speed, this.mult, this.pierce, this.explode, this.homing, this.impact, false);
			sound(ctx, this.style.sound(), 0.6F, 1.0F + p.getRandom().nextFloat() * 0.3F);
		}

		@Override
		public Component describe() {
			MutableComponent text = Component.translatable(SHOOT, style(this.style), this.count, pct(this.mult));
			if (this.pierce > 0) {
				text.append(Component.translatable(PIERCE, this.pierce));
			}
			if (this.explode > 0) {
				text.append(Component.translatable(EXPLODE, num(this.explode)));
			}
			if (this.homing) {
				text.append(Component.translatable(HOMING));
			}
			return text;
		}
	}

	/** Spawns one skill projectile (vanilla arrow for plain ARROW shots). */
	static void spawn(
		final SkillContext ctx,
		final ProjectileStyle style,
		final Vec3 from,
		final Vec3 dir,
		final float speed,
		final double mult,
		final int pierce,
		final float explode,
		final boolean homing,
		final @Nullable BiConsumer<SkillContext, Vec3> impact,
		final boolean randomWeapon
	) {
		if (style == ProjectileStyle.ARROW && pierce == 0 && explode == 0 && !homing && impact == null) {
			Arrow arrow = new Arrow(ctx.level, ctx.caster, new ItemStack(Items.ARROW), ctx.stack);
			arrow.setPos(from.x, from.y, from.z);
			arrow.shoot(dir.x, dir.y, dir.z, speed, 0.0F);
			arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
			arrow.setCritArrow(true);
			CombatHooks.trackSkillArrow(arrow, ctx, mult);
			ctx.level.addFreshEntity(arrow);
			SkillScheduler.schedule(100, arrow::discard);
			return;
		}
		ItemStack display = switch (style) {
			case BLADE -> randomWeapon ? JobWeapons.randomWeaponStack(ctx.caster.getRandom()) : ctx.stack.copyWithCount(1);
			case ARROW -> new ItemStack(Items.ARROW);
			default -> style.display();
		};
		SkillProjectile projectile = SkillProjectile.forSkill(ctx, display, style.trail(), style.gravity(), mult)
			.sweep(style.hitRadius())
			.pierce(pierce)
			.explode(explode)
			.homing(homing)
			.onImpact(impact)
			.launch(from, dir, speed);
		ctx.level.addFreshEntity(projectile);
	}

	/** Projectiles fall from the sky onto the target area. */
	public static SkillAction rain(final ProjectileStyle style, final double range, final double radius, final int count, final double mult) {
		return rain(style, range, radius, count, mult, 0.0F);
	}

	public static SkillAction rain(final ProjectileStyle style, final double range, final double radius, final int count, final double mult, final float explode) {
		MutableComponent text = Component.translatable(RAIN, num(range), num(radius), style(style), count, pct(mult));
		if (explode > 0) {
			text.append(Component.translatable(EXPLODE, num(explode)));
		}
		return attack(ctx -> {
			Vec3 center = ctx.groundPoint(range);
			double r = ctx.area(radius);
			ctx.fx.circle(ctx.level, Fx.Kind.RUNE, center.add(0, 0.1, 0), r);
			for (int i = 0; i < count; i++) {
				SkillScheduler.schedule(4 + i * 2, () -> {
					if (!ctx.valid()) {
						return;
					}
					Vec3 ground = rainTarget(ctx, center, r);
					Vec3 from = sky(ctx, ground);
					Vec3 dir = ground.subtract(from).normalize();
					spawn(ctx, style, from, dir, RAIN_SPEED, mult, 0, explode, false, null, style == ProjectileStyle.BLADE);
				});
			}
			sound(ctx, SoundEvents.EVOKER_PREPARE_ATTACK, 0.8F, 1.2F);
		}, text);
	}

	/**
	 * Where one falling projectile lands: on a random enemy inside the circle (ahead of where it walks), so a lone target takes the
	 * whole rain and a crowd shares it; on a random spot when the circle is empty. Scattering them at random used to leave a
	 * single monster with about one hit in sixty.
	 */
	private static Vec3 rainTarget(final SkillContext ctx, final Vec3 center, final double r) {
		RandomSource random = ctx.caster.getRandom();
		List<LivingEntity> inside = ctx.enemiesNear(center, r);
		if (inside.isEmpty()) {
			double a = random.nextDouble() * Math.PI * 2;
			double d = Math.sqrt(random.nextDouble()) * r;
			return center.add(Math.cos(a) * d, 0, Math.sin(a) * d);
		}
		LivingEntity target = inside.get(random.nextInt(inside.size()));
		Vec3 feet = target.position();
		Vec3 step = new Vec3(target.getX() - target.xo, 0.0, target.getZ() - target.zo);
		if (step.lengthSqr() > 0.36) {
			step = step.normalize().scale(0.6);
		}
		double fall = sky(ctx, feet).y - feet.y;
		return feet.add(step.scale(fall / RAIN_SPEED));
	}

	/** Where a falling projectile starts: {@link #RAIN_HEIGHT} above {@code ground}, or just under the roof (caves, dungeon halls). */
	private static Vec3 sky(final SkillContext ctx, final Vec3 ground) {
		BlockHitResult roof = ctx.level.clip(new ClipContext(ground.add(0, 0.1, 0), ground.add(0, RAIN_HEIGHT, 0), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, ctx.caster));
		double h = roof.getType() == HitResult.Type.MISS ? RAIN_HEIGHT : Math.max(0.5, roof.getLocation().y - 0.4 - ground.y);
		double tilt = 0.6 * h / RAIN_HEIGHT;
		return ground.add(tilt, h, tilt);
	}

	/** Projectiles launched from glowing portals behind the caster toward the crosshair. */
	public static SkillAction barrage(final ProjectileStyle style, final int count, final double mult) {
		return attack(ctx -> {
			for (int i = 0; i < count; i++) {
				SkillScheduler.schedule(1 + i * 2, () -> {
					if (!ctx.valid()) {
						return;
					}
					ServerPlayer p = ctx.caster;
					Vec3 back = ctx.flatLook().scale(-1.2);
					Vec3 side = new Vec3(-back.z, 0, back.x).normalize();
					double lateral = (p.getRandom().nextDouble() - 0.5) * 5.0;
					Vec3 from = SkillContext.openPoint(p, p.getEyePosition().add(back).add(side.scale(lateral)).add(0, 0.5 + p.getRandom().nextDouble() * 1.8, 0));
					Vec3 dir = lob(from, ctx.aimPoint(32.0), BARRAGE_SPEED, style.gravity());
					ctx.fx.still(ctx.level, Fx.Kind.RUNE, from);
					ctx.fx.burst(ctx.level, Fx.Kind.SPARK, from, 4, 0.2, 0.02);
					spawn(ctx, style, from, dir, BARRAGE_SPEED, mult, 0, 0.0F, false, null, style == ProjectileStyle.BLADE);
				});
			}
			sound(ctx, SoundEvents.ILLUSIONER_CAST_SPELL, 0.8F, 1.4F);
		}, Component.translatable(BARRAGE, style(style), count, pct(mult)));
	}

	/**
	 * The direction that brings a projectile with {@code gravity} from {@code from} onto {@code to}: aimed above it by what it drops on
	 * the way (air drag 0.99 per tick). Cannonballs fired straight at a target 20 blocks off used to land 1.6 blocks short.
	 */
	private static Vec3 lob(final Vec3 from, final Vec3 to, final float speed, final float gravity) {
		Vec3 line = to.subtract(from);
		if (gravity <= 0.0F) {
			return line.normalize();
		}
		double slowed = 1.0 - line.length() * 0.01 / (speed * 0.99);
		double ticks = slowed > 0.05 ? Math.log(slowed) / Math.log(0.99) : line.length() / speed * 2.0;
		return line.add(0.0, gravity * ticks * (ticks + 1.0) / 2.0, 0.0).normalize();
	}

	// ================================================================== target area

	/** Delayed strikes from the sky (meteors, cannon fire, falling swords...). */
	public static SkillAction strike(final double range, final double radius, final int count, final double mult) {
		return attack(ctx -> {
			Vec3 center = ctx.groundPoint(range);
			double r = ctx.area(radius);
			ctx.fx.circle(ctx.level, Fx.Kind.RUNE, center.add(0, 0.1, 0), r);
			for (int i = 0; i < count; i++) {
				int delay = 8 + i * 6;
				SkillScheduler.schedule(delay, () -> {
					if (!ctx.valid()) {
						return;
					}
					Vec3 at = count == 1 ? center : center.add((ctx.caster.getRandom().nextDouble() - 0.5) * r, 0, (ctx.caster.getRandom().nextDouble() - 0.5) * r);
					ctx.fx.line(ctx.level, at.add(0, 10, 0), at, 0.6);
					for (LivingEntity e : ctx.enemiesNear(at, r)) {
						ctx.hit(e, mult);
					}
					ctx.fx.burst(ctx.level, at.add(0, 0.5, 0), 20, r / 2, 0.15);
					ctx.level.sendParticles(ParticleTypes.EXPLOSION, at.x, at.y + 0.5, at.z, 1, 0, 0, 0, 0);
					soundAt(ctx, at, SoundEvents.GENERIC_EXPLODE.value(), 0.8F, 0.9F + ctx.caster.getRandom().nextFloat() * 0.3F);
				});
			}
		}, Component.translatable(STRIKE, num(range), num(radius), count, pct(mult)));
	}

	public static SkillAction lightning(final double range, final double radius, final double mult) {
		return attack(ctx -> {
			Vec3 at = ctx.groundPoint(range);
			LightningBolt bolt = EntityTypes.LIGHTNING_BOLT.create(ctx.level, EntitySpawnReason.TRIGGERED);
			if (bolt != null) {
				bolt.snapTo(at);
				bolt.setVisualOnly(true);
				ctx.level.addFreshEntity(bolt);
			}
			double r = ctx.area(radius);
			for (LivingEntity e : ctx.enemiesNear(at, r)) {
				ctx.hit(e, mult);
			}
			ctx.fx.withKind(Fx.Kind.BOLT).column(ctx.level, at, 8.0);
			ctx.fx.burst(ctx.level, Fx.Kind.SPARK, at.add(0, 0.5, 0), 25, r / 2, 0.2);
		}, Component.translatable(LIGHTNING, num(range), num(radius), pct(mult)));
	}

	/** Lingering field. */
	public static ZoneAction zone(final double radius, final double seconds, final double multPerSecond) {
		return new ZoneAction(radius, seconds, multPerSecond, 0.0);
	}

	/** Lingering healing field for the caster and nearby players. */
	public static ZoneAction sanctuary(final double radius, final double seconds, final double healPctPerSecond) {
		return new ZoneAction(radius, seconds, 0.0, healPctPerSecond);
	}

	public static final class ZoneAction implements SkillAction {
		private final double radius;
		private final double seconds;
		private final double mult;
		private final double heal;
		private double targetRange;
		private double pull;
		private @Nullable Holder<MobEffect> effect;
		private int amplifier;

		ZoneAction(final double radius, final double seconds, final double mult, final double heal) {
			this.radius = radius;
			this.seconds = seconds;
			this.mult = mult;
			this.heal = heal;
		}

		@Override
		public boolean damages() {
			return this.mult > 0;
		}

		/** Place it where the caster looks instead of at their feet. */
		public ZoneAction at(final double range) {
			this.targetRange = range;
			return this;
		}

		public ZoneAction pull(final double strength) {
			this.pull = strength;
			return this;
		}

		/** Effect (1s, refreshed every second) on enemies inside. */
		public ZoneAction effect(final Holder<MobEffect> effect, final int amplifier) {
			this.effect = effect;
			this.amplifier = amplifier;
			return this;
		}

		@Override
		public void run(final SkillContext ctx) {
			Vec3 center = this.targetRange > 0 ? ctx.groundPoint(this.targetRange) : ctx.caster.position();
			double r = ctx.area(this.radius);
			int pulses = Math.max(1, (int)Math.round(this.seconds * 2));
			for (int i = 0; i < pulses; i++) {
				SkillScheduler.schedule(1 + i * 10, () -> {
					if (!ctx.valid()) {
						return;
					}
					ctx.fx.circle(ctx.level, ctx.fx.kind(), center.add(0, 0.15, 0), r);
					ctx.fx.burst(ctx.level, center.add(0, 0.6, 0), (int)(r * 3), r / 2, 0.02);
					for (LivingEntity e : ctx.enemiesNear(center, r)) {
						if (this.mult > 0) {
							ctx.hit(e, this.mult / 2);
						}
						if (this.effect != null) {
							e.addEffect(new MobEffectInstance(this.effect, 25, this.amplifier), ctx.caster);
						}
						if (this.pull > 0) {
							Vec3 to = center.subtract(e.position());
							if (to.lengthSqr() > 1.0) {
								impulse(e, e.getDeltaMovement().add(to.normalize().scale(this.pull * 0.3)));
							}
						}
					}
					if (this.heal > 0) {
						for (Player ally : allies(ctx, center, r)) {
							healAlly(ctx, ally, (float)(ally.getMaxHealth() * this.heal / 200.0));
						}
					}
				});
			}
			ctx.fx.still(ctx.level, Fx.Kind.RUNE, center.add(0, 0.1, 0));
			soundAt(ctx, center, SoundEvents.EVOKER_CAST_SPELL, 0.7F, 0.8F);
		}

		@Override
		public Component describe() {
			MutableComponent text = this.heal > 0
				? Component.translatable(ZONE_HEAL, num(this.radius), num(this.seconds), num(this.heal))
				: Component.translatable(ZONE, num(this.radius), num(this.seconds), pct(this.mult));
			if (this.targetRange > 0) {
				text.append(Component.translatable(ZONE_TARGET));
			}
			if (this.pull > 0) {
				text.append(Component.translatable(ZONE_PULL));
			}
			if (this.effect != null) {
				text.append(" + ").append(effectName(this.effect)).append(" ").append(level(this.amplifier));
			}
			return text;
		}
	}

	/** Heals an ally; healing someone else counts toward the kills they are fighting for (see {@link Contribution}). */
	static void healAlly(final SkillContext ctx, final Player ally, final float amount) {
		float before = ally.getHealth();
		ally.heal(amount);
		Contribution.healed(ctx.caster, ally, ally.getHealth() - before);
	}

	static List<Player> allies(final SkillContext ctx, final Vec3 center, final double radius) {
		List<Player> list = new ArrayList<>();
		for (Player p : ctx.level.players()) {
			if (p.isAlive() && !p.isSpectator() && p.position().distanceTo(center) <= radius && (p == ctx.caster || !CombatHooks.isEnemy(ctx.caster, p))) {
				list.add(p);
			}
		}
		return list;
	}

	// ================================================================== crowd control

	public static SkillAction pull(final double radius, final double strength) {
		return action(ctx -> {
			Vec3 c = ctx.caster.position();
			for (LivingEntity e : ctx.enemiesNear(c, ctx.area(radius))) {
				Vec3 to = c.subtract(e.position());
				impulse(e, to.normalize().scale(Math.min(to.length(), 1.0) * strength * 0.5).add(0, 0.25, 0));
			}
			ctx.fx.circle(ctx.level, Fx.Kind.SMOKE, c.add(0, 0.5, 0), ctx.area(radius));
			sound(ctx, SoundEvents.EVOKER_CAST_SPELL, 0.6F, 0.6F);
		}, Component.translatable(PULL, num(radius)));
	}

	public static SkillAction push(final double radius, final double strength) {
		return action(ctx -> {
			Vec3 c = ctx.caster.position();
			for (LivingEntity e : ctx.enemiesNear(c, ctx.area(radius))) {
				Vec3 away = e.position().subtract(c).multiply(1, 0, 1);
				Vec3 dir = away.lengthSqr() < 1.0E-4 ? ctx.flatLook() : away.normalize();
				impulse(e, dir.scale(strength * 0.6).add(0, 0.35, 0));
			}
			ctx.fx.still(ctx.level, Fx.Kind.RING, c.add(0, 0.3, 0));
			sound(ctx, SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1.0F, 0.8F);
		}, Component.translatable(PUSH, num(radius)));
	}

	public static SkillAction launch(final double radius, final double height) {
		return action(ctx -> {
			for (LivingEntity e : ctx.enemiesNear(ctx.caster.position(), ctx.area(radius))) {
				impulse(e, e.getDeltaMovement().add(0, 0.3 + height * 0.12, 0));
				ctx.fx.burst(ctx.level, Fx.Kind.SPARK, e.position(), 5, 0.3, 0.1);
			}
		}, Component.translatable(LAUNCH, num(radius)));
	}

	/** Effect on every enemy around the caster (no damage). */
	public static SkillAction debuff(final double radius, final Holder<MobEffect> effect, final double seconds, final int amplifier) {
		return action(ctx -> {
			double r = ctx.area(radius);
			for (LivingEntity e : ctx.enemiesNear(ctx.caster.position(), r)) {
				e.addEffect(new MobEffectInstance(effect, (int)(seconds * 20), amplifier), ctx.caster);
				Contribution.debuffed(ctx.caster, e, (int)(seconds * 20));
				ctx.fx.burst(ctx.level, Fx.Kind.SMOKE, e.position().add(0, e.getBbHeight() / 2, 0), 6, 0.3, 0.02);
			}
			ctx.fx.circle(ctx.level, Fx.Kind.RUNE, ctx.caster.position().add(0, 0.1, 0), r);
			sound(ctx, SoundEvents.EVOKER_CAST_SPELL, 0.7F, 0.7F);
		}, Component.translatable(DEBUFF, num(radius), effectName(effect), level(amplifier), num(seconds)));
	}

	public static SkillAction taunt(final double radius) {
		return action(ctx -> {
			for (LivingEntity e : ctx.enemiesNear(ctx.caster.position(), ctx.area(radius))) {
				if (e instanceof Mob mob) {
					mob.setTarget(ctx.caster);
				}
				e.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100, 0), ctx.caster);
			}
			ctx.fx.circle(ctx.level, Fx.Kind.RING, ctx.caster.position().add(0, 0.2, 0), ctx.area(radius));
			sound(ctx, SoundEvents.RAVAGER_ROAR, 0.7F, 1.3F);
		}, Component.translatable(TAUNT, num(radius)));
	}

	/** Modifier: enemies hit by this skill get the effect. */
	public static SkillAction inflict(final Holder<MobEffect> effect, final double seconds, final int amplifier) {
		return modifier(ctx -> ctx.addOnHit((c, e) -> e.addEffect(new MobEffectInstance(effect, (int)(seconds * 20), amplifier), c.caster)),
			Component.translatable(INFLICT, effectName(effect), level(amplifier), num(seconds)));
	}

	public static SkillAction stun(final double seconds) {
		return inflict(ModEffects.STUN, seconds, 0);
	}

	public static SkillAction burn(final double seconds) {
		return modifier(ctx -> ctx.addOnHit((c, e) -> e.igniteForSeconds((float)seconds)), Component.translatable(BURN, num(seconds)));
	}

	public static SkillAction lifesteal(final double pct) {
		return modifier(ctx -> ctx.addLifesteal(pct / 100.0), Component.translatable(LIFESTEAL, num(pct)));
	}

	public static SkillAction loot(final double pct) {
		return modifier(ctx -> ctx.addOnHit((c, e) -> {
			if (c.caster.getRandom().nextDouble() * 100 < pct) {
				e.spawnAtLocation(c.level, new ItemStack(c.caster.getRandom().nextInt(6) == 0 ? com.minecraftmode.registry.ModItems.SILVER_COIN : com.minecraftmode.registry.ModItems.COPPER_COIN));
			}
		}), Component.translatable(LOOT, num(pct)));
	}

	// ================================================================== self / allies

	public static SkillAction buff(final Holder<MobEffect> effect, final double seconds, final int amplifier) {
		return action(ctx -> {
			ctx.caster.addEffect(new MobEffectInstance(effect, (int)(seconds * 20), amplifier), ctx.caster);
			ctx.fx.burst(ctx.level, ctx.caster.position().add(0, 1, 0), 15, 0.5, 0.05);
			sound(ctx, SoundEvents.BEACON_POWER_SELECT, 0.6F, 1.5F);
		}, Component.translatable(BUFF, effectName(effect), level(amplifier), num(seconds)));
	}

	public static SkillAction allyBuff(final double radius, final Holder<MobEffect> effect, final double seconds, final int amplifier) {
		return action(ctx -> {
			for (Player ally : allies(ctx, ctx.caster.position(), radius)) {
				ally.addEffect(new MobEffectInstance(effect, (int)(seconds * 20), amplifier), ctx.caster);
				Contribution.buffed(ctx.caster, ally, (int)(seconds * 20));
				ctx.fx.burst(ctx.level, ally.position().add(0, 1, 0), 8, 0.4, 0.05);
			}
			ctx.fx.circle(ctx.level, Fx.Kind.RUNE, ctx.caster.position().add(0, 0.1, 0), radius);
			sound(ctx, SoundEvents.BEACON_POWER_SELECT, 0.7F, 1.2F);
		}, Component.translatable(ALLY_BUFF, num(radius), effectName(effect), level(amplifier), num(seconds)));
	}

	public static SkillAction heal(final double pct) {
		return action(ctx -> {
			ctx.caster.heal((float)(ctx.caster.getMaxHealth() * pct / 100.0));
			ctx.level.sendParticles(ParticleTypes.HEART, ctx.caster.getX(), ctx.caster.getY(1.0), ctx.caster.getZ(), 5, 0.4, 0.3, 0.4, 0.0);
			ctx.fx.burst(ctx.level, ctx.caster.position().add(0, 1, 0), 10, 0.4, 0.05);
		}, Component.translatable(HEAL, num(pct)));
	}

	public static SkillAction allyHeal(final double radius, final double pct) {
		return action(ctx -> {
			for (Player ally : allies(ctx, ctx.caster.position(), radius)) {
				healAlly(ctx, ally, (float)(ally.getMaxHealth() * pct / 100.0));
				ctx.level.sendParticles(ParticleTypes.HEART, ally.getX(), ally.getY(1.0), ally.getZ(), 4, 0.4, 0.3, 0.4, 0.0);
			}
			ctx.fx.circle(ctx.level, Fx.Kind.RUNE, ctx.caster.position().add(0, 0.1, 0), radius);
			sound(ctx, SoundEvents.AMETHYST_BLOCK_CHIME, 1.0F, 1.2F);
		}, Component.translatable(ALLY_HEAL, num(radius), num(pct)));
	}

	/** Absorption hearts ({@code amount} health points, rounded up to the effect's steps of 4). */
	public static SkillAction shield(final double amount, final double seconds) {
		int amplifier = Math.max(0, (int)Math.ceil(amount / 4.0) - 1);
		return action(ctx -> {
			ctx.caster.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, (int)(seconds * 20), amplifier), ctx.caster);
			ctx.fx.still(ctx.level, Fx.Kind.RING, ctx.caster.position().add(0, 1, 0));
			sound(ctx, SoundEvents.SHIELD_BLOCK.value(), 0.8F, 1.2F);
		}, Component.translatable(SHIELD, (amplifier + 1) * 4, num(seconds)));
	}

	public static SkillAction stance(final CombatState.Stance type, final double seconds, final double value) {
		String text = switch (type) {
			case GUARD -> GUARD;
			case COUNTER -> COUNTER;
			case EVADE -> EVADE;
		};
		return action(ctx -> {
			CombatState state = CombatState.of(ctx.caster);
			state.stance = type;
			state.stanceUntil = ctx.level.getGameTime() + (long)(seconds * 20);
			state.stanceValue = (float)value;
			ctx.fx.still(ctx.level, Fx.Kind.RING, ctx.caster.position().add(0, 1, 0));
			ctx.fx.burst(ctx.level, Fx.Kind.SHARD, ctx.caster.position().add(0, 1, 0), 12, 0.5, 0.05);
			sound(ctx, SoundEvents.ARMOR_EQUIP_IRON.value(), 1.0F, 0.8F);
		}, Component.translatable(text, num(seconds), num(value)));
	}

	public static SkillAction empower(final int hits, final double bonusPct, final double seconds) {
		return action(ctx -> {
			CombatState state = CombatState.of(ctx.caster);
			state.empowerHits = hits;
			state.empowerBonus = (float)(bonusPct / 100.0);
			state.empowerUntil = ctx.level.getGameTime() + (long)(seconds * 20);
			ctx.fx.burst(ctx.level, Fx.Kind.SPARK, ctx.caster.position().add(0, 1.2, 0), 20, 0.5, 0.1);
			sound(ctx, SoundEvents.BLAZE_SHOOT, 0.4F, 1.6F);
		}, Component.translatable(EMPOWER, hits, num(bonusPct), num(seconds)));
	}

	public static SkillAction stealth(final double seconds, final double bonusPct) {
		return action(ctx -> {
			ctx.caster.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, (int)(seconds * 20), 0, false, false, true), ctx.caster);
			CombatState state = CombatState.of(ctx.caster);
			state.stealthBonus = (float)(bonusPct / 100.0);
			state.stealthUntil = ctx.level.getGameTime() + (long)(seconds * 20);
			for (Mob mob : ctx.level.getEntitiesOfClass(Mob.class, ctx.caster.getBoundingBox().inflate(24.0), m -> m.getTarget() == ctx.caster)) {
				mob.setTarget(null);
			}
			ctx.fx.burst(ctx.level, Fx.Kind.SMOKE, ctx.caster.position().add(0, 1, 0), 30, 0.6, 0.02);
			sound(ctx, SoundEvents.ILLUSIONER_MIRROR_MOVE, 0.8F, 1.0F);
		}, Component.translatable(STEALTH, num(seconds), num(bonusPct)));
	}

	public static SkillAction mark(final double range, final double seconds, final double bonusPct) {
		return action(ctx -> {
			LivingEntity target = ctx.lookTarget(range);
			if (target == null) {
				return;
			}
			CombatState.of(ctx.caster).marks.put(target.getUUID(),
				new CombatState.Mark(ctx.level.getGameTime() + (long)(seconds * 20), (float)(bonusPct / 100.0)));
			target.addEffect(new MobEffectInstance(MobEffects.GLOWING, (int)(seconds * 20), 0), ctx.caster);
			ctx.fx.still(ctx.level, Fx.Kind.RUNE, target.position().add(0, target.getBbHeight() + 0.5, 0));
			sound(ctx, SoundEvents.EXPERIENCE_ORB_PICKUP, 0.6F, 0.6F);
		}, Component.translatable(MARK, num(range), num(seconds), num(bonusPct)));
	}

	public static SkillAction cleanse() {
		return action(ctx -> {
			List<Holder<MobEffect>> bad = new ArrayList<>();
			for (MobEffectInstance instance : ctx.caster.getActiveEffects()) {
				if (instance.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) {
					bad.add(instance.getEffect());
				}
			}
			bad.forEach(ctx.caster::removeEffect);
			ctx.caster.clearFire();
			ctx.fx.burst(ctx.level, Fx.Kind.BUBBLE, ctx.caster.position().add(0, 1, 0), 15, 0.4, 0.05);
			sound(ctx, SoundEvents.AMETHYST_BLOCK_CHIME, 1.0F, 1.6F);
		}, Component.translatable(CLEANSE));
	}

	public static SkillAction grapple(final double range) {
		return move(ctx -> {
			Vec3 eye = ctx.eye();
			BlockHitResult hit = ctx.level.clip(new ClipContext(eye, eye.add(ctx.look().scale(range)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, ctx.caster));
			if (hit.getType() == HitResult.Type.MISS) {
				return;
			}
			Vec3 to = hit.getLocation().subtract(ctx.caster.position());
			impulse(ctx.caster, to.normalize().scale(Math.min(3.0, 0.6 + to.length() * 0.18)).add(0, 0.35, 0));
			ctx.fx.line(ctx.level, eye, hit.getLocation(), 0.5);
			sound(ctx, SoundEvents.FISHING_BOBBER_THROW, 1.0F, 0.6F);
			ctx.guard(Engage.AIR_TICKS);
			Movement.guardFall(ctx.caster, () -> ctx.guard(Engage.GUARD_TICKS));
		}, Component.translatable(GRAPPLE, num(range)), false);
	}

	public static SkillAction restoreMana(final int amount) {
		return action(ctx -> {
			JobStats.addMana(ctx.caster, amount);
			ctx.fx.burst(ctx.level, Fx.Kind.ORB, ctx.caster.position().add(0, 1, 0), 12, 0.4, 0.05);
		}, Component.translatable(MANA, amount));
	}

	public static SkillAction refresh(final double pct) {
		return action(ctx -> {
			long now = ctx.level.getGameTime();
			var data = JobProgression.get(ctx.caster);
			Map<String, Long> cooldowns = new HashMap<>(data.cooldowns());
			for (int i = 0; i < ctx.weapon.skills().size(); i++) {
				Skill other = ctx.weapon.skills().get(i);
				if (other == ctx.skill) {
					continue;
				}
				for (String key : List.of(other.id(), SkillCaster.slotKey(data.job(), i))) {
					long left = cooldowns.getOrDefault(key, 0L) - now;
					if (left > 0) {
						cooldowns.put(key, now + (long)(left * (1.0 - pct / 100.0)));
					}
				}
			}
			JobProgression.set(ctx.caster, data.withCooldowns(cooldowns));
			ctx.fx.burst(ctx.level, Fx.Kind.SPARK, ctx.caster.position().add(0, 1.5, 0), 15, 0.3, 0.1);
		}, Component.translatable(REFRESH, num(pct)));
	}

	public enum Summon {
		WOLF("wolf", "Spirit wolves", "영혼 늑대"),
		IRON_GOLEM("iron_golem", "Iron golem", "철 골렘"),
		SNOW_GOLEM("snow_golem", "Snow golem", "눈 골렘");

		private final String id;
		private final String en;
		private final String ko;

		Summon(final String id, final String en, final String ko) {
			this.id = id;
			this.en = en;
			this.ko = ko;
		}

		public String nameKey() {
			return "summon.minecraft_mode." + this.id;
		}

		public String en() {
			return this.en;
		}

		public String ko() {
			return this.ko;
		}
	}

	public static SkillAction summon(final Summon kind, final int count, final double seconds) {
		return action(ctx -> {
			for (int i = 0; i < count; i++) {
				Mob mob = switch (kind) {
					case WOLF -> {
						Wolf wolf = EntityTypes.WOLF.create(ctx.level, EntitySpawnReason.MOB_SUMMONED);
						if (wolf != null) {
							wolf.tame(ctx.caster);
						}
						yield wolf;
					}
					case IRON_GOLEM -> {
						IronGolem golem = EntityTypes.IRON_GOLEM.create(ctx.level, EntitySpawnReason.MOB_SUMMONED);
						if (golem != null) {
							golem.setPlayerCreated(true);
						}
						yield golem;
					}
					case SNOW_GOLEM -> {
						SnowGolem golem = EntityTypes.SNOW_GOLEM.create(ctx.level, EntitySpawnReason.MOB_SUMMONED);
						if (golem != null) {
							golem.setPumpkin(false);
						}
						yield golem;
					}
				};
				if (mob == null) {
					continue;
				}
				double a = Math.PI * 2 * i / count;
				Vec3 pos = ctx.caster.position().add(Math.cos(a) * 1.5, 0.1, Math.sin(a) * 1.5);
				mob.snapTo(pos.x, pos.y, pos.z, ctx.caster.getYRot(), 0.0F);
				mob.addTag(SUMMON_TAG);
				mob.addTag(SUMMONER_TAG + ctx.caster.getUUID());
				mob.setPersistenceRequired();
				ctx.level.addFreshEntity(mob);
				ctx.fx.burst(ctx.level, Fx.Kind.SMOKE, pos.add(0, 0.8, 0), 15, 0.3, 0.03);
				SkillScheduler.schedule((int)(seconds * 20), () -> {
					if (mob.isAlive()) {
						ServerLevel level = (ServerLevel)mob.level();
						level.sendParticles(ParticleTypes.POOF, mob.getX(), mob.getY(0.5), mob.getZ(), 10, 0.3, 0.3, 0.3, 0.02);
						mob.discard();
					}
				});
			}
			sound(ctx, SoundEvents.EVOKER_PREPARE_SUMMON, 0.8F, 1.0F);
		}, Component.translatable(SUMMON, count, Component.translatable(kind.nameKey()), num(seconds)));
	}

	// ================================================================== composition

	/** Runs {@code action} {@code times} times, {@code intervalTicks} apart (the first right away). */
	public static SkillAction repeat(final int times, final int intervalTicks, final SkillAction action) {
		return wrap(action, ctx -> {
			action.run(ctx);
			for (int i = 1; i < times; i++) {
				SkillScheduler.schedule(i * intervalTicks, () -> {
					if (ctx.valid()) {
						action.run(ctx);
					}
				});
			}
		}, Component.translatable(REPEAT, action.describe(), times));
	}

	public static SkillAction delay(final double seconds, final SkillAction action) {
		return wrap(action, ctx -> SkillScheduler.schedule((int)(seconds * 20), () -> {
			if (ctx.valid()) {
				action.run(ctx);
			}
		}), Component.translatable(DELAY, action.describe(), num(seconds)));
	}

	private Actions() {
	}
}
