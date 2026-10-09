package com.minecraftmode.job.gear;

import com.minecraftmode.job.JobClass;
import com.minecraftmode.job.JobData;
import com.minecraftmode.registry.ModEffects;
import com.minecraftmode.job.JobProgression;
import com.minecraftmode.job.skill.CombatHooks;
import com.minecraftmode.job.skill.Fx;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Innate class abilities (key B), unlocked at {@link LevelRewards#INNATE_LEVEL}: War Cry, Smoke
 * Step, Blink, Backstep, Grappling Hook, Flash Step and En. They do not need a class weapon.
 */
public final class ClassAbilities {
	public enum Ability {
		WAR_CRY(JobClass.WARRIOR, 30, 0, "war_cry", "War Cry", "전투의 함성",
			"6s Strength and Resistance; enemies within 5 blocks are knocked back and weakened.", "6초간 힘·저항, 주변 5블록의 적을 밀쳐 내고 약화시킵니다."),
		SMOKE_STEP(JobClass.ROGUE, 18, 0, "smoke_step", "Smoke Step", "연막 이동",
			"Dash 6 blocks and turn invisible for 2s.", "6블록 돌진한 뒤 2초간 투명해집니다."),
		BLINK(JobClass.MAGE, 8, 10, "blink", "Blink", "블링크",
			"Teleport up to 8 blocks where you look (10 MP).", "바라보는 방향으로 최대 8블록 순간이동 (MP 10)."),
		BACKSTEP(JobClass.ARCHER, 12, 0, "backstep", "Backstep", "백스텝",
			"Leap backwards and gain Swiftness for 2s.", "뒤로 크게 도약하고 2초간 신속."),
		GRAPPLING_HOOK(JobClass.PIRATE, 14, 0, "grappling_hook", "Grappling Hook", "갈고리",
			"Hook a block up to 24 blocks away and get pulled to it.", "24블록 안의 블록에 갈고리를 걸어 끌려갑니다."),
		FLASH_STEP(JobClass.SHINIGAMI, 10, 10, "flash_step", "Flash Step", "순보",
			"Vanish and reappear up to 11 blocks where you look, then 3s of Swiftness II (10 MP).", "바라보는 방향으로 최대 11블록 순식간에 이동하고 3초간 신속 II (MP 10)."),
		EN(JobClass.HUNTER, 20, 10, "nen_en", "En", "원",
			"Spread your aura 32 blocks: every enemy in it glows for 10s and you gain Night Vision (10 MP).", "오라를 32블록까지 펼쳐 범위 안의 모든 적을 10초간 드러내고 야간 투시를 얻습니다 (MP 10).");

		private final JobClass job;
		private final int cooldownSeconds;
		private final int mana;
		private final String id;
		public final String en;
		public final String ko;
		public final String descEn;
		public final String descKo;

		Ability(final JobClass job, final int cooldownSeconds, final int mana, final String id, final String en, final String ko, final String descEn, final String descKo) {
			this.job = job;
			this.cooldownSeconds = cooldownSeconds;
			this.mana = mana;
			this.id = id;
			this.en = en;
			this.ko = ko;
			this.descEn = descEn;
			this.descKo = descKo;
		}

		public JobClass job() {
			return this.job;
		}

		public int cooldownSeconds() {
			return this.cooldownSeconds;
		}

		public String cooldownKey() {
			return "innate." + this.id;
		}

		public String nameKey() {
			return "ability.minecraft_mode." + this.id;
		}

		public String descKey() {
			return this.nameKey() + ".desc";
		}

		public static Ability of(final JobClass job) {
			for (Ability ability : values()) {
				if (ability.job == job) {
					return ability;
				}
			}
			return null;
		}
	}

	public static void use(final ServerPlayer player) {
		if (!player.isAlive() || player.isSpectator()) {
			return;
		}
		JobData data = JobProgression.get(player);
		Ability ability = Ability.of(data.job());
		if (ability == null || !data.hasClass()) {
			player.sendOverlayMessage(Component.translatable("message.minecraft_mode.ability.no_class").withStyle(ChatFormatting.GRAY));
			return;
		}
		if (player.hasEffect(ModEffects.STUN)) {
			player.sendOverlayMessage(Component.translatable("message.minecraft_mode.skill.stunned").withStyle(ChatFormatting.YELLOW));
			return;
		}
		if (data.level() < LevelRewards.INNATE_LEVEL) {
			player.sendOverlayMessage(Component.translatable("message.minecraft_mode.ability.locked", LevelRewards.INNATE_LEVEL).withStyle(ChatFormatting.RED));
			return;
		}
		long now = player.level().getGameTime();
		long ready = data.readyAt(ability.cooldownKey());
		if (ready > now) {
			player.sendOverlayMessage(Component.translatable("message.minecraft_mode.skill.cooldown", Component.translatable(ability.nameKey()),
				String.format(Locale.ROOT, "%.1f", (ready - now) / 20.0)).withStyle(ChatFormatting.YELLOW));
			return;
		}
		if (data.mana() < ability.mana && !player.isCreative()) {
			player.sendOverlayMessage(Component.translatable("message.minecraft_mode.skill.no_mana").withStyle(ChatFormatting.AQUA));
			return;
		}
		boolean used = switch (ability) {
			case WAR_CRY -> warCry(player);
			case SMOKE_STEP -> smokeStep(player);
			case BLINK -> blink(player);
			case BACKSTEP -> backstep(player);
			case GRAPPLING_HOOK -> grapple(player);
			case FLASH_STEP -> flashStep(player);
			case EN -> en(player);
		};
		if (used) {
			JobData after = JobProgression.get(player);
			JobProgression.set(player, after.withMana(player.isCreative() ? after.mana() : after.mana() - ability.mana)
				.withCooldown(ability.cooldownKey(), now + ability.cooldownSeconds * 20L));
		}
	}

	private static boolean warCry(final ServerPlayer player) {
		ServerLevel level = player.level();
		player.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 120, 0));
		player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 120, 0));
		for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(5.0), e -> CombatHooks.isEnemy(player, e))) {
			Vec3 away = e.position().subtract(player.position()).multiply(1, 0, 1).normalize().scale(1.1);
			push(e, new Vec3(away.x, 0.4, away.z));
			e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 0), player);
		}
		Fx fx = new Fx(Fx.Kind.RING, 0xE0533D);
		fx.circle(level, Fx.Kind.RING, player.position().add(0, 0.2, 0), 2.5);
		fx.circle(level, Fx.Kind.SPARK, player.position().add(0, 0.2, 0), 5.0);
		level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.RAVAGER_ROAR, SoundSource.PLAYERS, 0.9F, 1.3F);
		return true;
	}

	private static boolean smokeStep(final ServerPlayer player) {
		ServerLevel level = player.level();
		Vec3 start = player.position();
		Vec3 dir = Vec3.directionFromRotation(0.0F, player.getYRot());
		Vec3 end = freePath(player, dir, 6.0);
		level.sendParticles(ParticleTypes.LARGE_SMOKE, start.x, start.y + 1, start.z, 25, 0.5, 0.6, 0.5, 0.02);
		teleport(player, end);
		player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 40, 0, false, false, true));
		player.addEffect(new MobEffectInstance(MobEffects.SPEED, 40, 1, false, false, true));
		level.playSound(null, start.x, start.y, start.z, SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.5F, 1.6F);
		return true;
	}

	private static boolean blink(final ServerPlayer player) {
		ServerLevel level = player.level();
		Vec3 look = player.getLookAngle();
		Vec3 dir = new Vec3(look.x, Math.max(-0.3, Math.min(0.6, look.y)), look.z).normalize();
		Vec3 start = player.position();
		Vec3 end = freePath(player, dir, 8.0);
		if (end.distanceToSqr(start) < 1.0) {
			return false;
		}
		Fx fx = new Fx(Fx.Kind.RUNE, 0x4A8CFF);
		fx.burst(level, start.add(0, 1, 0), 18, 0.4, 0.05);
		teleport(player, end);
		fx.burst(level, Fx.Kind.SPARK, end.add(0, 1, 0), 18, 0.4, 0.08);
		level.playSound(null, end.x, end.y, end.z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.7F, 1.4F);
		return true;
	}

	private static boolean backstep(final ServerPlayer player) {
		Vec3 back = Vec3.directionFromRotation(0.0F, player.getYRot()).scale(-1.3);
		push(player, new Vec3(back.x, 0.5, back.z));
		player.addEffect(new MobEffectInstance(MobEffects.SPEED, 40, 1, false, false, true));
		new Fx(Fx.Kind.FEATHER, 0x9BE07A).burst(player.level(), player.position().add(0, 0.5, 0), 12, 0.4, 0.05);
		player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PHANTOM_FLAP, SoundSource.PLAYERS, 0.6F, 1.5F);
		return true;
	}

	private static boolean grapple(final ServerPlayer player) {
		ServerLevel level = player.level();
		Vec3 eye = player.getEyePosition();
		BlockHitResult hit = level.clip(new ClipContext(eye, eye.add(player.getLookAngle().scale(24.0)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
		if (hit.getType() != HitResult.Type.BLOCK) {
			player.sendOverlayMessage(Component.translatable("message.minecraft_mode.ability.no_anchor").withStyle(ChatFormatting.GRAY));
			return false;
		}
		Vec3 target = hit.getLocation();
		new Fx(Fx.Kind.SPARK, 0xC8C8C8).line(level, eye.add(0, -0.3, 0), target, 0.5);
		Vec3 pull = target.subtract(player.position());
		double distance = pull.length();
		Vec3 velocity = pull.normalize().scale(Math.min(2.4, 0.6 + distance * 0.11)).add(0, 0.35, 0);
		push(player, velocity);
		player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 30, 0, false, false, true));
		player.resetFallDistance();
		level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.CHAIN_PLACE, SoundSource.PLAYERS, 0.9F, 0.8F);
		return true;
	}

	private static boolean flashStep(final ServerPlayer player) {
		ServerLevel level = player.level();
		Vec3 look = player.getLookAngle();
		Vec3 dir = new Vec3(look.x, Math.max(-0.3, Math.min(0.5, look.y)), look.z).normalize();
		Vec3 start = player.position();
		Vec3 end = freePath(player, dir, 11.0);
		if (end.distanceToSqr(start) < 1.0) {
			return false;
		}
		Fx fx = new Fx(Fx.Kind.SLASH, 0x9FD8E8);
		fx.line(level, start.add(0, 1, 0), end.add(0, 1, 0), 0.8);
		teleport(player, end);
		player.addEffect(new MobEffectInstance(MobEffects.SPEED, 60, 1, false, false, true));
		level.playSound(null, end.x, end.y, end.z, SoundEvents.BREEZE_JUMP, SoundSource.PLAYERS, 0.6F, 1.8F);
		return true;
	}

	private static boolean en(final ServerPlayer player) {
		ServerLevel level = player.level();
		int found = 0;
		for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(32.0), e -> CombatHooks.isEnemy(player, e))) {
			e.addEffect(new MobEffectInstance(MobEffects.GLOWING, 200, 0), player);
			found++;
		}
		player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 400, 0, false, false, true));
		Fx fx = new Fx(Fx.Kind.RING, 0xF5862B);
		fx.circle(level, Fx.Kind.RING, player.position().add(0, 0.2, 0), 3.0);
		fx.circle(level, Fx.Kind.SPARK, player.position().add(0, 0.4, 0), 6.0);
		player.sendOverlayMessage(Component.translatable("message.minecraft_mode.ability.en", found).withStyle(ChatFormatting.GOLD));
		level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.6F, 1.6F);
		return true;
	}

	// ------------------------------------------------------------------ helpers

	private static void push(final LivingEntity entity, final Vec3 velocity) {
		entity.setDeltaMovement(velocity);
		if (entity instanceof ServerPlayer) {
			entity.syncVelocity = true;
		} else {
			entity.needsSync = true;
		}
	}

	private static void teleport(final ServerPlayer player, final Vec3 pos) {
		player.teleportTo(pos.x, pos.y, pos.z);
		player.resetFallDistance();
	}

	/** Moves along {@code dir} until a wall (stepping up single blocks); returns the last free position. */
	private static Vec3 freePath(final ServerPlayer player, final Vec3 dir, final double distance) {
		Vec3 start = player.position();
		Vec3 last = start;
		for (double d = 0.5; d <= distance; d += 0.5) {
			Vec3 next = start.add(dir.scale(d));
			AABB box = player.getBoundingBox().move(next.subtract(player.position()));
			if (!player.level().noCollision(player, box)) {
				AABB up = box.move(0, 1.0, 0);
				if (!player.level().noCollision(player, up)) {
					break;
				}
				next = next.add(0, 1.0, 0);
				start = start.add(0, 1.0, 0);
			}
			last = next;
		}
		return last;
	}

	private ClassAbilities() {
	}
}
