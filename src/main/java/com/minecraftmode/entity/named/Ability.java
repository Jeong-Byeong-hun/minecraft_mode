package com.minecraftmode.entity.named;

import com.minecraftmode.job.skill.Fx;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import org.jspecify.annotations.Nullable;

/**
 * One pattern of a named monster. {@code power} multiplies the monster's attack damage; the other
 * fields are used by the types that need them (see {@link NamedMob} for what each type does).
 *
 * @param cooldown ticks between uses
 * @param range    how close the target must be (blocks)
 * @param radius   area size, cone length or projectile spread (degrees)
 * @param count    projectiles or minions
 * @param gravity  projectile gravity (0 = straight)
 */
public record Ability(
	Type type, int cooldown, float power, float range, float radius, int count, Fx.Kind fx, int color, @Nullable Holder<MobEffect> effect, int effectTicks, int amplifier,
	@Nullable EntityType<?> summon, @Nullable Item display, boolean homing, float gravity
) {
	public enum Type {
		/** Jumps onto the target; telegraphed landing circle. */
		LEAP,
		/** Telegraphed line, then a rush that hits and throws back. */
		CHARGE,
		/** Projectiles (fan of {@code count}, optional effect, homing, blast when {@code radius} > 0 on gravity shots). */
		BOLT,
		/** Calls {@code count} short-lived minions. */
		SUMMON,
		/** Appears behind the target. */
		TELEPORT,
		/** Pulses an effect on players within {@code radius}. */
		AURA,
		/** Knocks back everyone within {@code radius}, with an optional effect. */
		ROAR,
		/** Telegraphed eruption under the target. */
		SPIKES,
		/** Regenerates {@code power} of max health over a few seconds when below half health. */
		HEAL,
		/** Telegraphed cone, then a stream that hurts for a second. */
		BREATH,
		/** Turns invisible; the next hit is much stronger. */
		STEALTH,
		/** Telegraphed circle around itself, then a ground slam. */
		SLAM
	}

	public static Ability of(final Type type, final int cooldown, final float power, final float range, final float radius, final int count, final Fx.Kind fx, final int color) {
		return new Ability(type, cooldown, power, range, radius, count, fx, color, null, 0, 0, null, null, false, 0.0F);
	}

	public Ability effect(final Holder<MobEffect> effect, final int ticks, final int amplifier) {
		return new Ability(this.type, this.cooldown, this.power, this.range, this.radius, this.count, this.fx, this.color, effect, ticks, amplifier, this.summon,
			this.display, this.homing, this.gravity);
	}

	public Ability summon(final EntityType<?> summon) {
		return new Ability(this.type, this.cooldown, this.power, this.range, this.radius, this.count, this.fx, this.color, this.effect, this.effectTicks,
			this.amplifier, summon, this.display, this.homing, this.gravity);
	}

	public Ability display(final Item display) {
		return new Ability(this.type, this.cooldown, this.power, this.range, this.radius, this.count, this.fx, this.color, this.effect, this.effectTicks,
			this.amplifier, this.summon, display, this.homing, this.gravity);
	}

	public Ability seeking() {
		return new Ability(this.type, this.cooldown, this.power, this.range, this.radius, this.count, this.fx, this.color, this.effect, this.effectTicks,
			this.amplifier, this.summon, this.display, true, this.gravity);
	}

	public Ability gravity(final float gravity) {
		return new Ability(this.type, this.cooldown, this.power, this.range, this.radius, this.count, this.fx, this.color, this.effect, this.effectTicks,
			this.amplifier, this.summon, this.display, this.homing, gravity);
	}

	/**
	 * Whether its hits are spells ({@code MagicDamage}, reduced by magic defense): breaths and eruptions always, bolts unless they throw
	 * or shoot something solid (knives, bullets, rocks, gold).
	 */
	public boolean magic() {
		return switch (this.type) {
			case BREATH, SPIKES -> true;
			case BOLT -> this.display == null || !PHYSICAL_SHOTS.contains(this.display);
			default -> false;
		};
	}

	private static final java.util.Set<Item> PHYSICAL_SHOTS = java.util.Set.of(com.minecraftmode.registry.ModItems.PROJECTILE_KNIFE,
		com.minecraftmode.registry.ModItems.PROJECTILE_BULLET, net.minecraft.world.item.Items.COBBLESTONE, net.minecraft.world.item.Items.GOLD_NUGGET);
}
