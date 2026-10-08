package com.minecraftmode.job.weapon;

import com.minecraftmode.job.JobClass;
import com.minecraftmode.job.JobData;
import com.minecraftmode.job.JobProgression;
import com.minecraftmode.job.engrave.EngraveStat;
import com.minecraftmode.job.engrave.EngraveTotals;
import com.minecraftmode.job.skill.CombatHooks;
import com.minecraftmode.job.skill.Fx;
import com.minecraftmode.job.skill.SkillProjectile;
import com.minecraftmode.job.skill.SkillScheduler;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

/**
 * Right-click shots and bow shots of class weapons. Anyone can use them; class bonuses (engravings,
 * passives, extra projectiles) only apply when the weapon is active for the shooter.
 */
public final class BasicAttacks {
	public static void shoot(final ServerPlayer player, final ItemStack stack, final WeaponDef def) {
		fire(player, stack, def, 1.0F);
	}

	public static void loose(final ServerPlayer player, final ItemStack stack, final WeaponDef def, final float power) {
		fire(player, stack, def, power);
	}

	private static void fire(final ServerPlayer player, final ItemStack stack, final WeaponDef def, final float power) {
		JobData data = JobProgression.get(player);
		boolean active = JobWeapons.isActive(data, def);
		EngraveTotals mods = active ? JobWeapons.engravings(stack).totals() : EngraveTotals.EMPTY;
		float damage = def.power() * def.archetype().shotMultiplier() * power * (1.0F + mods.fraction(EngraveStat.SHOT_DAMAGE));
		if (CombatHooks.has(data, JobClass.ARCHER, 1)) {
			damage *= 1.15F;
		}
		int count = 1 + (int)mods.get(EngraveStat.EXTRA_SHOT);
		ProjectileStyle style = def.archetype().shot();
		float spread = count == 1 ? 0.0F : Math.min(30.0F, 8.0F * (count - 1));
		for (int i = 0; i < count; i++) {
			float yaw = count == 1 ? 0.0F : -spread / 2 + spread * i / (count - 1);
			spawn(player, stack, def, style, damage, yaw, power);
		}
		ServerLevel level = player.level();
		level.playSound(null, player.getX(), player.getY(), player.getZ(), style.sound(), SoundSource.PLAYERS, 0.6F, 1.0F + player.getRandom().nextFloat() * 0.2F);
		// King of Heroes: treasure blades join the shot
		if (CombatHooks.has(data, JobClass.ARCHER, 4) && player.getRandom().nextFloat() < 0.2F) {
			for (int i = 0; i < 2; i++) {
				Vec3 back = Vec3.directionFromRotation(0.0F, player.getYRot()).scale(-1.0);
				Vec3 side = new Vec3(-back.z, 0, back.x).scale(i == 0 ? 1.2 : -1.2);
				Vec3 from = player.getEyePosition().add(back).add(side).add(0, 0.8, 0);
				Vec3 dir = player.getEyePosition().add(player.getLookAngle().scale(30)).subtract(from).normalize();
				SkillProjectile blade = SkillProjectile.forBasicShot(player, JobWeapons.randomWeaponStack(player.getRandom()), 0xFFD700, Fx.Kind.SPARK, 0.0F, damage)
					.launch(from, dir, 2.6F);
				level.addFreshEntity(blade);
			}
		}
	}

	private static void spawn(
		final ServerPlayer player, final ItemStack stack, final WeaponDef def, final ProjectileStyle style, final float damage, final float yawOffset, final float power
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
			CombatHooks.trackArrow(arrow, damage);
			level.addFreshEntity(arrow);
			SkillScheduler.schedule(100, arrow::discard);
			return;
		}
		SkillProjectile projectile = SkillProjectile.forBasicShot(player, style.display(), def.fx().color(), style.trail(), style.gravity(), damage)
			.explode(style == ProjectileStyle.CANNONBALL ? 2.0F : 0.0F)
			.launch(from, dir, speed);
		level.addFreshEntity(projectile);
	}

	private BasicAttacks() {
	}
}
