package com.minecraftmode.job.weapon;

import com.minecraftmode.job.JobClass;
import com.minecraftmode.job.JobData;
import com.minecraftmode.job.JobProgression;
import com.minecraftmode.job.engrave.EngraveStat;
import com.minecraftmode.job.engrave.EngraveTotals;
import com.minecraftmode.job.skill.CombatHooks;
import com.minecraftmode.job.skill.Fx;
import com.minecraftmode.job.skill.SkillCaster;
import com.minecraftmode.job.skill.SkillContext;
import com.minecraftmode.job.skill.SkillProjectile;
import com.minecraftmode.job.skill.SkillScheduler;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragonPart;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Right-click shots and bow shots of class weapons. Anyone can use them; class bonuses (engravings,
 * passives, extra projectiles) only apply when the weapon is active for the shooter.
 */
public final class BasicAttacks {
	/** King of Heroes (archer tier 4): chance that two treasure blades join a basic shot. */
	public static final float BABYLON_CHANCE = 0.15F;
	/** Eagle Eye (archer tier 1): basic shot damage bonus, a multiplier on top of the shot damage stat. */
	public static final float EAGLE_EYE = 0.20F;
	/**
	 * Basic shot damage (percent) each extra projectile ({@link EngraveStat#EXTRA_SHOT}) adds. The extras only reach other enemies
	 * ({@link Fan}); this is what they are worth on a lone target, steady whatever its size or distance (Heavy Draw gives 10 a line).
	 */
	public static final int EXTRA_SHOT_DAMAGE = 4;

	/**
	 * The projectiles of one basic shot. Each enemy takes one of them: the extra ones ({@link EngraveStat#EXTRA_SHOT}) fly on past it to the
	 * monsters around, as the class docs count them (ClassComparison). The whole fan used to land on anything wide enough, so three
	 * Multishot lines made a raid boss take five or more arrows a shot. The treasure blades of King of Heroes are not part of it.
	 */
	public static final class Fan {
		private final Set<UUID> struck = new HashSet<>();

		/** Whether a projectile of this shot already hit {@code target}. */
		public boolean taken(final Entity target) {
			return this.struck.contains(body(target).getUUID());
		}

		/** A projectile of this shot hits {@code target}. */
		public void take(final Entity target) {
			this.struck.add(body(target).getUUID());
		}

		/** The Ender Dragon is hit through its parts. */
		private static Entity body(final Entity entity) {
			return entity instanceof EnderDragonPart part ? part.parentMob : entity;
		}
	}

	/**
	 * Damage bonus (fraction) of an active weapon's basic shot: the shot damage stat, {@link #EXTRA_SHOT_DAMAGE} per extra projectile
	 * and, for mages, their skill damage too (Mana Affinity: a mage's orbs are spells, so whatever strengthens the spells strengthens them).
	 */
	public static float shotBonus(final JobData data, final EngraveTotals mods) {
		float bonus = mods.fraction(EngraveStat.SHOT_DAMAGE) + mods.get(EngraveStat.EXTRA_SHOT) * EXTRA_SHOT_DAMAGE / 100.0F;
		if (CombatHooks.has(data, JobClass.MAGE, 1)) {
			bonus += mods.fraction(EngraveStat.SKILL_DAMAGE) + (CombatHooks.has(data, JobClass.MAGE, 3) ? SkillCaster.ARCANE_MASTERY : 0.0F);
		}
		return bonus;
	}

	public static void shoot(final ServerPlayer player, final ItemStack stack, final WeaponDef def) {
		fire(player, stack, def, 1.0F);
	}

	public static void loose(final ServerPlayer player, final ItemStack stack, final WeaponDef def, final float power) {
		fire(player, stack, def, power);
	}

	private static void fire(final ServerPlayer player, final ItemStack stack, final WeaponDef def, final float power) {
		JobData data = JobProgression.get(player);
		boolean active = JobWeapons.isActive(data, def);
		EngraveTotals mods = active ? JobWeapons.activeTotals(player) : EngraveTotals.EMPTY;
		float damage = def.shotDamage() * power * (1.0F + (active ? shotBonus(data, mods) : 0.0F));
		if (CombatHooks.has(data, JobClass.ARCHER, 1)) {
			damage *= 1.0F + EAGLE_EYE;
		}
		int count = 1 + (int)mods.get(EngraveStat.EXTRA_SHOT);
		ProjectileStyle style = def.archetype().shot();
		// one shot always on the aim line, the extra ones fanned out to alternate sides (an even count spread evenly left the middle
		// empty: two arrows 8° apart passed either side of a monster from a few blocks on)
		float step = count == 1 ? 0.0F : Math.min(30.0F, 8.0F * (count - 1)) / (count - 1);
		Fan fan = count == 1 ? null : new Fan();
		for (int i = 0; i < count; i++) {
			float yaw = (i + 1) / 2 * step * (i % 2 == 1 ? 1 : -1);
			spawn(player, stack, def, style, damage, yaw, power, fan);
		}
		ServerLevel level = player.level();
		level.playSound(null, player.getX(), player.getY(), player.getZ(), style.sound(), SoundSource.PLAYERS, 0.6F, 1.0F + player.getRandom().nextFloat() * 0.2F);
		// King of Heroes: treasure blades join the shot
		if (CombatHooks.has(data, JobClass.ARCHER, 4) && player.getRandom().nextFloat() < BABYLON_CHANCE) {
			// they start beside the archer, so they meet on the target in the crosshair (or 30 blocks out)
			LivingEntity aimed = SkillContext.aimed(player, 30.0, e -> CombatHooks.canHarm(player, e));
			Vec3 target = aimed != null ? aimed.getBoundingBox().getCenter() : player.getEyePosition().add(player.getLookAngle().scale(30));
			for (int i = 0; i < 2; i++) {
				Vec3 back = Vec3.directionFromRotation(0.0F, player.getYRot()).scale(-1.0);
				Vec3 side = new Vec3(-back.z, 0, back.x).scale(i == 0 ? 1.2 : -1.2);
				Vec3 from = SkillContext.openPoint(player, player.getEyePosition().add(back).add(side).add(0, 0.8, 0));
				Vec3 dir = target.subtract(from).normalize();
				SkillProjectile blade = SkillProjectile.forBasicShot(player, JobWeapons.randomWeaponStack(player.getRandom()), 0xFFD700, Fx.Kind.SPARK, 0.0F, damage)
					.launch(from, dir, 2.6F);
				level.addFreshEntity(blade);
			}
		}
	}

	private static void spawn(
		final ServerPlayer player, final ItemStack stack, final WeaponDef def, final ProjectileStyle style, final float damage, final float yawOffset, final float power,
		final @Nullable Fan fan
	) {
		ServerLevel level = player.level();
		Vec3 dir = Vec3.directionFromRotation(player.getXRot(), player.getYRot() + yawOffset);
		Vec3 from = player.getEyePosition().add(0, -0.1, 0);
		float speed = style.speed() * (style == ProjectileStyle.ARROW ? power : 1.0F);
		if (style == ProjectileStyle.ARROW) {
			Arrow arrow = new Arrow(level, player, new ItemStack(Items.ARROW), stack);
			arrow.setPos(from.x, from.y, from.z);
			arrow.shoot(dir.x, dir.y, dir.z, speed, 1.0F);
			arrow.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
			arrow.setCritArrow(power >= 1.0F);
			CombatHooks.trackArrow(arrow, damage, fan);
			level.addFreshEntity(arrow);
			SkillScheduler.schedule(100, arrow::discard);
			return;
		}
		SkillProjectile projectile = SkillProjectile.forBasicShot(player, style.display(), def.fx().color(), style.trail(), style.gravity(), damage)
			.sweep(style.hitRadius())
			.explode(style == ProjectileStyle.CANNONBALL ? 2.0F : 0.0F)
			.fan(fan)
			.launch(from, dir, speed);
		level.addFreshEntity(projectile);
	}

	private BasicAttacks() {
	}
}
