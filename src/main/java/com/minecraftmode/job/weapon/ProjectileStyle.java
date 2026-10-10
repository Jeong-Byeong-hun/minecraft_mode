package com.minecraftmode.job.weapon;

import com.minecraftmode.job.skill.Fx;
import com.minecraftmode.registry.ModItems;
import java.util.function.Supplier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * What a skill or basic shot fires. {@code ARROW} spawns a real arrow; everything else is a
 * {@code SkillProjectile} showing {@link #display()} (empty = particles only) with a particle trail.
 * {@code BLADE} shows the weapon that fired it.
 */
public enum ProjectileStyle {
	ARROW("arrow", "Arrows", "화살", null, Fx.Kind.SPARK, 3.0F, 0.05F, SoundEvents.ARROW_SHOOT),
	SHURIKEN("shuriken", "Shuriken", "표창", () -> new ItemStack(ModItems.PROJECTILE_SHURIKEN), Fx.Kind.SPARK, 2.2F, 0.0F, SoundEvents.TRIDENT_THROW.value()),
	KUNAI("kunai", "Kunai", "쿠나이", () -> new ItemStack(ModItems.PROJECTILE_KUNAI), Fx.Kind.SPARK, 2.4F, 0.01F, SoundEvents.TRIDENT_THROW.value()),
	KNIFE("knife", "Throwing knives", "투척 단검", () -> new ItemStack(ModItems.PROJECTILE_KNIFE), Fx.Kind.SPARK, 2.4F, 0.01F, SoundEvents.TRIDENT_THROW.value()),
	BULLET("bullet", "Bullets", "총알", () -> new ItemStack(ModItems.PROJECTILE_BULLET), Fx.Kind.SMOKE, 4.0F, 0.0F, SoundEvents.GENERIC_EXPLODE.value()),
	CANNONBALL("cannonball", "Cannonballs", "포탄", () -> new ItemStack(ModItems.PROJECTILE_CANNONBALL), Fx.Kind.SMOKE, 1.6F, 0.04F, SoundEvents.GENERIC_EXPLODE.value()),
	ORB("orb", "Magic orbs", "마력탄", null, Fx.Kind.ORB, 1.7F, 0.0F, SoundEvents.EVOKER_CAST_SPELL),
	FIREBALL("fireball", "Fireballs", "화염구", null, Fx.Kind.ORB, 1.4F, 0.0F, SoundEvents.BLAZE_SHOOT),
	ICICLE("icicle", "Icicles", "고드름", () -> new ItemStack(ModItems.PROJECTILE_ICICLE), Fx.Kind.SHARD, 2.0F, 0.01F, SoundEvents.GLASS_BREAK),
	COIN("coin", "Gold coins", "금화", () -> new ItemStack(ModItems.GOLD_COIN), Fx.Kind.COIN, 1.8F, 0.02F, SoundEvents.EXPERIENCE_ORB_PICKUP),
	BLADE("blade", "Blades", "검", null, Fx.Kind.SPARK, 2.6F, 0.0F, SoundEvents.TRIDENT_THROW.value()),
	HARPOON("harpoon", "Harpoons", "작살", () -> new ItemStack(ModItems.PROJECTILE_HARPOON), Fx.Kind.BUBBLE, 2.2F, 0.03F, SoundEvents.TRIDENT_THROW.value()),
	WAVE("wave", "Sword waves", "검기", null, Fx.Kind.SLASH, 1.5F, 0.0F, SoundEvents.PLAYER_ATTACK_SWEEP),
	FEATHER("feather", "Feather darts", "깃털 화살", () -> new ItemStack(Items.FEATHER), Fx.Kind.FEATHER, 2.6F, 0.0F, SoundEvents.ARROW_SHOOT);

	private final String id;
	private final String en;
	private final String ko;
	private final Supplier<ItemStack> display;
	private final Fx.Kind trail;
	private final float speed;
	private final float gravity;
	private final SoundEvent sound;

	ProjectileStyle(
		final String id, final String en, final String ko, final Supplier<ItemStack> display, final Fx.Kind trail, final float speed, final float gravity,
		final SoundEvent sound
	) {
		this.id = id;
		this.en = en;
		this.ko = ko;
		this.display = display;
		this.trail = trail;
		this.speed = speed;
		this.gravity = gravity;
		this.sound = sound;
	}

	public String id() {
		return this.id;
	}

	public String en() {
		return this.en;
	}

	public String ko() {
		return this.ko;
	}

	public String nameKey() {
		return "projectile.minecraft_mode." + this.id;
	}

	public ItemStack display() {
		return this.display == null ? ItemStack.EMPTY : this.display.get();
	}

	public Fx.Kind trail() {
		return this.trail;
	}

	public float speed() {
		return this.speed;
	}

	public float gravity() {
		return this.gravity;
	}

	public SoundEvent sound() {
		return this.sound;
	}

	/**
	 * How far from its path a {@code SkillProjectile} hits: sword waves a little past their slash trail (the largest slash
	 * particle's half size), everything else vanilla's full margin from the first tick on.
	 */
	public double hitRadius() {
		return this == WAVE ? 1.0 : ProjectileUtil.DEFAULT_ENTITY_HIT_RESULT_MARGIN;
	}
}
