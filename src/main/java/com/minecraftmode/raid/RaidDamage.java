package com.minecraftmode.raid;

import com.minecraftmode.MinecraftMode;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageScaling;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.jspecify.annotations.Nullable;

/**
 * The damage of a failed raid mechanic ({@code minecraft_mode:raid_mechanic}): ignores armor,
 * resistance, enchantments and the mod's own dodge/reduction, but not invulnerability, so a Totem of
 * Undying, a Phoenix Feather or Avalon can still save the player. Written by datagen (type + tags).
 */
public final class RaidDamage {
	public static final ResourceKey<DamageType> MECHANIC = ResourceKey.create(Registries.DAMAGE_TYPE, MinecraftMode.id("raid_mechanic"));
	/** Enough to kill anyone. */
	public static final float LETHAL = 9999.0F;

	public static void bootstrap(final BootstrapContext<DamageType> context) {
		context.register(MECHANIC, new DamageType("minecraft_mode.raid_mechanic", DamageScaling.NEVER, 0.0F));
	}

	public static DamageSource source(final ServerLevel level, final @Nullable Entity boss) {
		return level.damageSources().source(MECHANIC, boss);
	}

	public static boolean is(final DamageSource source) {
		return source.is(MECHANIC);
	}

	/** Kills {@code target} unless something saves them (totem, feather, Avalon). */
	public static void lethal(final ServerLevel level, final LivingEntity target, final @Nullable Entity boss) {
		target.hurtServer(level, source(level, boss), LETHAL);
	}

	/** A heavy but survivable hit: {@code fraction} of the target's max health. */
	public static void portion(final ServerLevel level, final LivingEntity target, final @Nullable Entity boss, final float fraction) {
		target.hurtServer(level, source(level, boss), target.getMaxHealth() * fraction);
	}

	private RaidDamage() {
	}
}
