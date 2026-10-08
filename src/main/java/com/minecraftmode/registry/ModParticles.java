package com.minecraftmode.registry;

import com.minecraftmode.MinecraftMode;
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;

/**
 * Skill effect particles. All take a color (sprites are drawn white/grey and tinted), so one sprite
 * serves every class color. Sprites are drawn by tools/TextureGen.java.
 */
public final class ModParticles {
	public static final ParticleType<ColorParticleOption> SPARK = register("spark");
	public static final ParticleType<ColorParticleOption> SLASH = register("slash");
	public static final ParticleType<ColorParticleOption> ORB = register("orb");
	public static final ParticleType<ColorParticleOption> RING = register("ring");
	public static final ParticleType<ColorParticleOption> RUNE = register("rune");
	public static final ParticleType<ColorParticleOption> SHARD = register("shard");
	public static final ParticleType<ColorParticleOption> SMOKE = register("smoke");
	public static final ParticleType<ColorParticleOption> PETAL = register("petal");
	public static final ParticleType<ColorParticleOption> COIN = register("coin");
	public static final ParticleType<ColorParticleOption> BOLT = register("bolt");
	public static final ParticleType<ColorParticleOption> FEATHER = register("feather");
	public static final ParticleType<ColorParticleOption> BUBBLE = register("bubble");

	private static ParticleType<ColorParticleOption> register(final String name) {
		return Registry.register(
			BuiltInRegistries.PARTICLE_TYPE, MinecraftMode.id(name), FabricParticleTypes.complex(ColorParticleOption::codec, ColorParticleOption::streamCodec)
		);
	}

	public static void init() {
	}

	private ModParticles() {
	}
}
