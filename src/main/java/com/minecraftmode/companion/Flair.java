package com.minecraftmode.companion;

import com.minecraftmode.entity.CreatureAnim;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * Each pet's and mount's particles and voice: an ambient trail drawn by the client (mounts only while moving or flying), a burst
 * when it is summoned and a puff when it leaves, and its idle sound. Keyed by companion id.
 */
public final class Flair {
	/** {@code every}: one trail particle per that many ticks on average; {@code react}: a pet's move when its owner scores a kill. */
	public record Style(ParticleOptions particle, int every, SoundEvent voice, float pitch, float volume, CreatureAnim react) {
	}

	private static final int FLAME_STEPS = 10;
	private static final Style DEFAULT = new Style(ParticleTypes.HAPPY_VILLAGER, 20, SoundEvents.ALLAY_AMBIENT_WITHOUT_ITEM, 1.0F, 0.5F, CreatureAnim.LEAP);
	private static final Map<String, Style> STYLES = Map.ofEntries(
		Map.entry("ember_fox", new Style(ParticleTypes.SMALL_FLAME, 10, SoundEvents.FOX_AMBIENT, 1.3F, 0.6F, CreatureAnim.LEAP)),
		Map.entry("frost_owl", new Style(ParticleTypes.SNOWFLAKE, 16, SoundEvents.PARROT_AMBIENT, 0.7F, 0.6F, CreatureAnim.ROAR)),
		Map.entry("golden_scarab", new Style(ParticleTypes.WAX_ON, 14, SoundEvents.SILVERFISH_AMBIENT, 1.6F, 0.4F, CreatureAnim.SPIN)),
		Map.entry("moss_turtle", new Style(ParticleTypes.SPORE_BLOSSOM_AIR, 20, SoundEvents.TURTLE_AMBIENT_LAND, 1.4F, 0.6F, CreatureAnim.ROAR)),
		Map.entry("spark_sprite", new Style(ParticleTypes.ELECTRIC_SPARK, 8, SoundEvents.ALLAY_AMBIENT_WITHOUT_ITEM, 1.3F, 0.5F, CreatureAnim.CAST)),
		Map.entry("shadow_cat", new Style(ParticleTypes.REVERSE_PORTAL, 10, SoundEvents.OCELOT_AMBIENT, 1.2F, 0.6F, CreatureAnim.ATTACK)),
		Map.entry("crystal_slime", new Style(ParticleTypes.GLOW, 16, SoundEvents.SLIME_SQUISH_SMALL, 1.2F, 0.6F, CreatureAnim.LEAP)),
		Map.entry("baby_dragon", new Style(ParticleTypes.SMALL_FLAME, 10, SoundEvents.ENDER_DRAGON_GROWL, 2.0F, 0.15F, CreatureAnim.BREATH)),
		Map.entry("swift_stallion", new Style(ParticleTypes.END_ROD, 8, SoundEvents.HORSE_AMBIENT, 1.0F, 0.6F, CreatureAnim.NONE)),
		Map.entry("dune_raptor", new Style(ParticleTypes.POOF, 14, SoundEvents.RAVAGER_AMBIENT, 1.7F, 0.4F, CreatureAnim.NONE)),
		Map.entry("frost_wolf", new Style(ParticleTypes.SNOWFLAKE, 8, SoundEvents.POLAR_BEAR_AMBIENT, 1.4F, 0.6F, CreatureAnim.NONE)),
		Map.entry("ember_lion", new Style(ParticleTypes.FLAME, 6, SoundEvents.RAVAGER_AMBIENT, 0.9F, 0.5F, CreatureAnim.NONE)),
		Map.entry("crystal_stag", new Style(ParticleTypes.END_ROD, 8, SoundEvents.AMETHYST_BLOCK_CHIME, 1.0F, 0.8F, CreatureAnim.NONE)),
		Map.entry("storm_griffin", new Style(ParticleTypes.ELECTRIC_SPARK, 6, SoundEvents.PARROT_AMBIENT, 0.5F, 0.7F, CreatureAnim.NONE))
	);

	/** Mounts whose steps clop like a horse's; the rest pad. */
	private static final Set<String> HOOVED = Set.of("swift_stallion", "crystal_stag");

	public static boolean hooved(final String id) {
		return HOOVED.contains(id);
	}

	public static Style of(final String id) {
		return STYLES.getOrDefault(id, DEFAULT);
	}

	/** Client side, every tick: now and then one trail particle somewhere on the body. */
	public static void trail(final Entity entity, final String id) {
		Style style = of(id);
		RandomSource random = entity.getRandom();
		if (random.nextInt(style.every()) != 0) {
			return;
		}
		double w = entity.getBbWidth();
		entity.level().addParticle(style.particle(), entity.getX() + (random.nextDouble() - 0.5) * w, entity.getY() + random.nextDouble() * entity.getBbHeight(),
			entity.getZ() + (random.nextDouble() - 0.5) * w, 0.0, 0.02, 0.0);
	}

	/** A companion arrives: its own particles in a burst around it and a chime. */
	public static void arrive(final ServerLevel level, final Entity entity, final String id) {
		Style style = of(id);
		double w = entity.getBbWidth();
		double h = entity.getBbHeight();
		level.sendParticles(style.particle(), entity.getX(), entity.getY() + h / 2, entity.getZ(), 24, w * 0.6, h * 0.5, w * 0.6, 0.05);
		level.sendParticles(ParticleTypes.POOF, entity.getX(), entity.getY() + h / 2, entity.getZ(), 8, w * 0.4, h * 0.3, w * 0.4, 0.02);
		level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.NEUTRAL, 1.0F, 1.3F);
		level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), style.voice(), SoundSource.NEUTRAL, style.volume(), style.pitch());
	}

	/**
	 * A pet cheers its owner's kill: its own particles burst on the fallen monster with its voice; a breathing pet (the dragon) also
	 * sends a stream of flame at it.
	 */
	public static void react(final ServerLevel level, final Entity pet, final Entity victim, final String id) {
		Style style = of(id);
		Vec3 at = victim.position().add(0, victim.getBbHeight() / 2, 0);
		level.sendParticles(style.particle(), at.x, at.y, at.z, 16, victim.getBbWidth() * 0.4, victim.getBbHeight() * 0.3, victim.getBbWidth() * 0.4, 0.04);
		if (style.react() == CreatureAnim.BREATH) {
			Vec3 from = pet.getEyePosition();
			for (int i = 1; i <= FLAME_STEPS; i++) {
				Vec3 p = from.lerp(at, i / (double)FLAME_STEPS);
				level.sendParticles(ParticleTypes.FLAME, p.x, p.y, p.z, 2, 0.08, 0.08, 0.08, 0.01);
			}
			level.playSound(null, pet.getX(), pet.getY(), pet.getZ(), SoundEvents.FIRECHARGE_USE, SoundSource.NEUTRAL, 0.4F, 1.6F);
		}
		level.playSound(null, pet.getX(), pet.getY(), pet.getZ(), style.voice(), SoundSource.NEUTRAL, style.volume(), style.pitch() + 0.1F);
	}

	/** A companion leaves: a puff of smoke where it stood. */
	public static void leave(final ServerLevel level, final Entity entity) {
		double h = entity.getBbHeight();
		level.sendParticles(ParticleTypes.POOF, entity.getX(), entity.getY() + h / 2, entity.getZ(), 12, entity.getBbWidth() * 0.5, h * 0.4, entity.getBbWidth() * 0.5,
			0.02);
		level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.NEUTRAL, 0.6F, 0.8F);
	}

	private Flair() {
	}
}
