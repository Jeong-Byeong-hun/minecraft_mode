package com.minecraftmode.job.skill;

import com.minecraftmode.entity.EliteMob;
import com.minecraftmode.job.quest.Quests;
import com.minecraftmode.registry.ModEffects;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

/**
 * Skills that move their caster into melee range and hit there ({@link Skill#engages}) would otherwise hand the first blow to the
 * monster they land next to. Their movement guards the caster (no damage, {@link #GUARD_TICKS} after arriving) and their hits
 * stagger what they hit for a moment ({@link #STAGGER_TICKS}, {@link #BOSS_STAGGER_TICKS} for bosses and named monsters).
 * Raid mechanics ({@code RaidDamage}) and damage that bypasses invulnerability still land (see {@code CombatHooks}).
 */
public final class Engage {
	/** 0.5 s of guard after the move. */
	public static final int GUARD_TICKS = 10;
	/** Guard while a leap or grapple is in the air (cut to {@link #GUARD_TICKS} on landing). */
	public static final int AIR_TICKS = 60;
	/** 0.3 s stagger on monsters. */
	public static final int STAGGER_TICKS = 6;
	/** 0.15 s stagger on bosses, named monsters and other elites. */
	public static final int BOSS_STAGGER_TICKS = 3;

	/** Ticks as seconds for tooltips and docs ("0.5", "0.3", "0.15"). */
	public static String seconds(final int ticks) {
		return java.math.BigDecimal.valueOf(ticks / 20.0).stripTrailingZeros().toPlainString();
	}

	public static void guard(final ServerPlayer player, final int ticks) {
		CombatState.of(player).guardUntil = player.level().getGameTime() + ticks;
	}

	public static boolean guarded(final ServerPlayer player) {
		return player.level().getGameTime() < CombatState.of(player).guardUntil;
	}

	/** Stuns {@code target} briefly; a longer stun it already has is kept. */
	public static void stagger(final LivingEntity target, final ServerPlayer caster) {
		target.addEffect(new MobEffectInstance(ModEffects.STUN, staggerTicks(target), 0, false, false, false), caster);
	}

	public static int staggerTicks(final LivingEntity target) {
		return isBoss(target) ? BOSS_STAGGER_TICKS : STAGGER_TICKS;
	}

	public static boolean isBoss(final LivingEntity target) {
		return target instanceof EliteMob || Quests.BOSSES.contains(target.getType());
	}

	/** Feedback for a hit the guard swallowed. */
	static void deflected(final ServerPlayer player) {
		ServerLevel level = player.level();
		level.sendParticles(ParticleTypes.ENCHANTED_HIT, player.getX(), player.getY(0.6), player.getZ(), 6, 0.3, 0.4, 0.3, 0.1);
		level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_ATTACK_NODAMAGE, SoundSource.PLAYERS, 0.6F, 1.8F);
	}

	private Engage() {
	}
}
