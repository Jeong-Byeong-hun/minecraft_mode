package com.minecraftmode.entity.combat;

import com.minecraftmode.MinecraftMode;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageScaling;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import org.jspecify.annotations.Nullable;

/**
 * Magic attacks, which class armor's magic defense reduces on top of its protection. The mod's own monsters hit with
 * {@code minecraft_mode:monster_magic} (breaths, eruptions, spell bolts; armor still counts, unlike vanilla magic); vanilla magic
 * counts too: potions and fangs, guardian beams, the Wither and its skulls, dragon breath, the Warden's sonic boom and fireballs.
 * Written by datagen (the damage type).
 */
public final class MagicDamage {
	public static final ResourceKey<DamageType> MONSTER_MAGIC = ResourceKey.create(Registries.DAMAGE_TYPE, MinecraftMode.id("monster_magic"));

	public static void bootstrap(final BootstrapContext<DamageType> context) {
		context.register(MONSTER_MAGIC, new DamageType("minecraft_mode.monster_magic", DamageScaling.WHEN_CAUSED_BY_LIVING_NON_PLAYER, 0.1F));
	}

	/** A spell hit by {@code attacker}, through {@code direct} (a projectile, or the attacker itself). */
	public static DamageSource source(final ServerLevel level, final @Nullable Entity direct, final @Nullable Entity attacker) {
		return level.damageSources().source(MONSTER_MAGIC, direct, attacker);
	}

	public static boolean is(final DamageSource source) {
		return source.is(MONSTER_MAGIC) || source.is(DamageTypes.MAGIC) || source.is(DamageTypes.INDIRECT_MAGIC) || source.is(DamageTypes.WITHER)
			|| source.is(DamageTypes.WITHER_SKULL) || source.is(DamageTypes.DRAGON_BREATH) || source.is(DamageTypes.SONIC_BOOM)
			|| source.is(DamageTypes.FIREBALL) || source.is(DamageTypes.UNATTRIBUTED_FIREBALL);
	}

	private MagicDamage() {
	}
}
