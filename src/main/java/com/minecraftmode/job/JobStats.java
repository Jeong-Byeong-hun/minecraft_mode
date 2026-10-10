package com.minecraftmode.job;

import com.minecraftmode.MinecraftMode;
import com.minecraftmode.job.engrave.EngraveStat;
import com.minecraftmode.job.engrave.EngraveTotals;
import com.minecraftmode.job.gear.GearRules;
import com.minecraftmode.job.skill.CombatHooks;
import com.minecraftmode.job.weapon.JobWeapons;
import com.minecraftmode.registry.ModEffects;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

/**
 * Derived numbers (max MP, regeneration) and the attribute modifiers that come from levels, class
 * passives and the engravings of the weapon in hand. Modifiers use fixed ids so they never stack
 * with themselves; they are only re-sent when the value changes.
 */
public final class JobStats {
	private static final Identifier LEVEL_HEALTH = MinecraftMode.id("job/level_health");
	private static final Identifier PASSIVE_HEALTH = MinecraftMode.id("job/passive_health");
	private static final Identifier PASSIVE_SPEED = MinecraftMode.id("job/passive_speed");
	private static final Identifier PASSIVE_LUCK = MinecraftMode.id("job/passive_luck");
	private static final Identifier ENGRAVE_HEALTH = MinecraftMode.id("job/engrave_health");
	private static final Identifier ENGRAVE_SPEED = MinecraftMode.id("job/engrave_speed");
	private static final Identifier ENGRAVE_ATTACK_SPEED = MinecraftMode.id("job/engrave_attack_speed");
	private static final Identifier ENGRAVE_REACH = MinecraftMode.id("job/engrave_reach");
	private static final Identifier GEAR_ARMOR = MinecraftMode.id("job/gear_armor");
	private static final Identifier GEAR_KNOCKBACK = MinecraftMode.id("job/gear_knockback");
	private static final Identifier CLASS_HEALTH = MinecraftMode.id("job/class_health");
	private static final Identifier CLASS_ARMOR = MinecraftMode.id("job/class_armor");

	public static int maxMana(final Player player) {
		JobData data = JobProgression.get(player);
		int bonus = CombatHooks.has(data, JobClass.MAGE, 1) ? 30 : 0;
		return JobProgression.BASE_MANA + 2 * data.level() + bonus + (int)JobWeapons.activeTotals(player).get(EngraveStat.MAX_MANA);
	}

	/** Base MP per second: 1 plus this much per level (level 10: 2, level 50: 6, level 100: 11). */
	public static final float MANA_REGEN_PER_LEVEL = 0.1F;

	/** MP regenerated per second, to one decimal for display (the tick pays fractions out over time). */
	public static String manaRegen(final Player player) {
		return String.format(java.util.Locale.ROOT, "%.1f", manaRegenExact(player));
	}

	public static float manaRegenExact(final Player player) {
		JobData data = JobProgression.get(player);
		float regen = 1.0F + data.level() * MANA_REGEN_PER_LEVEL;
		if (CombatHooks.has(data, JobClass.MAGE, 2)) {
			regen *= 2;
		}
		if (player.hasEffect(ModEffects.MANA_FLOW)) {
			regen *= 2;
		}
		return regen + JobWeapons.activeTotals(player).get(EngraveStat.MANA_REGEN);
	}

	/** {@code value} rounded down, plus one with the probability of its fraction, so "+0.4 per rank" lines pay out on average. */
	public static int randomRound(final float value, final RandomSource random) {
		int whole = (int)value;
		return value - whole > random.nextFloat() ? whole + 1 : whole;
	}

	public static void addMana(final ServerPlayer player, final int amount) {
		JobData data = JobProgression.get(player);
		int mana = Math.min(maxMana(player), data.mana() + amount);
		if (mana != data.mana()) {
			JobProgression.set(player, data.withMana(mana));
		}
	}

	/** Called every 10 ticks per player. */
	static void tick(final ServerPlayer player, final int tick) {
		GearRules.enforce(player);
		refresh(player);
		if (tick % 80 == 0 && player.isAlive() && player.getHealth() < player.getMaxHealth()) {
			float regen = JobWeapons.activeTotals(player).get(EngraveStat.HEALTH_REGEN);
			if (regen > 0.0F) {
				player.heal(regen);
			}
		}
		JobData data = JobProgression.get(player);
		int max = maxMana(player);
		int mana = data.mana();
		if (tick % 20 == 0) {
			mana = Math.min(max, mana + randomRound(manaRegenExact(player), player.getRandom()));
		}
		mana = Math.min(max, mana);
		JobData updated = data.withMana(mana);
		if (tick % 200 == 0) {
			updated = updated.pruneCooldowns(player.level().getGameTime());
		}
		if (!updated.equals(data)) {
			JobProgression.set(player, updated);
		}
		if (CombatHooks.has(data, JobClass.PIRATE, 2) && player.isInWater()) {
			player.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, 40, 0, true, false, true));
			player.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 40, 0, true, false, true));
		}
	}

	/** Re-applies level, passive and engraving attribute modifiers. */
	public static void refresh(final ServerPlayer player) {
		JobData data = JobProgression.get(player);
		EngraveTotals mods = JobWeapons.activeTotals(player);
		set(player, Attributes.MAX_HEALTH, LEVEL_HEALTH, data.level() / 10, AttributeModifier.Operation.ADD_VALUE);
		set(player, Attributes.MAX_HEALTH, PASSIVE_HEALTH, CombatHooks.has(data, JobClass.WARRIOR, 1) ? 4 : 0, AttributeModifier.Operation.ADD_VALUE);
		// melee classes stand in front: health and armor per tier (JobClass.vitalityHealth/vitalityArmor)
		set(player, Attributes.MAX_HEALTH, CLASS_HEALTH, data.job().vitalityHealth() * data.tier(), AttributeModifier.Operation.ADD_VALUE);
		set(player, Attributes.ARMOR, CLASS_ARMOR, data.job().vitalityArmor() * data.tier(), AttributeModifier.Operation.ADD_VALUE);
		double speed = (CombatHooks.has(data, JobClass.ROGUE, 1) ? 0.10 : 0.0) + (CombatHooks.has(data, JobClass.ARCHER, 2) ? 0.08 : 0.0);
		set(player, Attributes.MOVEMENT_SPEED, PASSIVE_SPEED, speed, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
		set(player, Attributes.LUCK, PASSIVE_LUCK, CombatHooks.has(data, JobClass.PIRATE, 4) ? 2 : 0, AttributeModifier.Operation.ADD_VALUE);
		set(player, Attributes.MAX_HEALTH, ENGRAVE_HEALTH, mods.get(EngraveStat.MAX_HEALTH), AttributeModifier.Operation.ADD_VALUE);
		set(player, Attributes.MOVEMENT_SPEED, ENGRAVE_SPEED, mods.fraction(EngraveStat.MOVE_SPEED), AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
		set(player, Attributes.ATTACK_SPEED, ENGRAVE_ATTACK_SPEED, mods.fraction(EngraveStat.ATTACK_SPEED), AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
		set(player, Attributes.ENTITY_INTERACTION_RANGE, ENGRAVE_REACH, mods.get(EngraveStat.REACH), AttributeModifier.Operation.ADD_VALUE);
		set(player, Attributes.ARMOR, GEAR_ARMOR, mods.get(EngraveStat.ARMOR), AttributeModifier.Operation.ADD_VALUE);
		set(player, Attributes.KNOCKBACK_RESISTANCE, GEAR_KNOCKBACK, mods.fraction(EngraveStat.KNOCKBACK_RES), AttributeModifier.Operation.ADD_VALUE);
		if (player.getHealth() > player.getMaxHealth()) {
			player.setHealth(player.getMaxHealth());
		}
	}

	private static void set(final ServerPlayer player, final Holder<Attribute> attribute, final Identifier id, final double value, final AttributeModifier.Operation op) {
		AttributeInstance instance = player.getAttribute(attribute);
		if (instance == null) {
			return;
		}
		AttributeModifier existing = instance.getModifier(id);
		if (value == 0.0) {
			if (existing != null) {
				instance.removeModifier(id);
			}
		} else if (existing == null || existing.amount() != value || existing.operation() != op) {
			instance.addOrUpdateTransientModifier(new AttributeModifier(id, value, op));
		}
	}

	private JobStats() {
	}
}
