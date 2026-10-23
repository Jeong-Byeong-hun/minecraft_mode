package com.minecraftmode.job.skill;

import com.minecraftmode.job.JobClass;
import com.minecraftmode.job.JobData;
import com.minecraftmode.job.JobProgression;
import com.minecraftmode.job.engrave.EngraveStat;
import com.minecraftmode.job.engrave.EngraveTotals;
import com.minecraftmode.job.weapon.JobWeapons;
import com.minecraftmode.job.weapon.WeaponDef;
import com.minecraftmode.jump.JumpForest;
import com.minecraftmode.registry.ModEffects;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.item.ItemStack;

/** Validates and casts a weapon skill for a player (from the skill keys or commands/tests). */
public final class SkillCaster {
	/** Arcane Mastery (mage tier 3): skill damage bonus, which a mage's orbs get too ({@code BasicAttacks.shotBonus}). */
	public static final float ARCANE_MASTERY = 0.15F;
	/** The most percent and flat cooldown reductions together take off a skill's cooldown. */
	public static final float MAX_COOLDOWN_CUT = 0.6F;
	/** Akashic Record (mage tier 4): skill cooldown reduction. */
	public static final float AKASHIC_RECORD = 0.20F;

	public enum Result {
		OK,
		NO_WEAPON,
		NO_SKILL,
		WRONG_CLASS,
		LOW_TIER,
		LOW_LEVEL,
		STUNNED,
		COOLDOWN,
		NO_MANA
	}

	public static Result tryCast(final ServerPlayer player, final int slot) {
		Result result = cast(player, slot);
		if (result != Result.OK && result != Result.NO_SKILL) {
			player.sendOverlayMessage(message(player, slot, result));
		}
		return result;
	}

	private static Result cast(final ServerPlayer player, final int slot) {
		if (!player.isAlive() || player.isSpectator()) {
			return Result.NO_WEAPON;
		}
		ItemStack stack = player.getMainHandItem();
		WeaponDef def = JobWeapons.def(stack);
		if (def == null) {
			return Result.NO_WEAPON;
		}
		if (slot < 0 || slot >= def.skills().size()) {
			return Result.NO_SKILL;
		}
		JobData data = JobProgression.get(player);
		if (data.job() != def.job()) {
			return Result.WRONG_CLASS;
		}
		if (data.tier() < def.tier()) {
			return Result.LOW_TIER;
		}
		if (data.level() < def.level()) {
			return Result.LOW_LEVEL;
		}
		if (player.hasEffect(ModEffects.STUN)) {
			return Result.STUNNED;
		}
		Skill skill = def.skills().get(slot);
		// jump courses are climbed by jumping: dashes, leaps, blinks and grapples are refused there (with their own message)
		if (skill.actions().stream().anyMatch(SkillAction::moves) && JumpForest.refusesMove(player)) {
			return Result.NO_SKILL;
		}
		long now = player.level().getGameTime();
		if (readyAt(data, skill, slot) > now) {
			return Result.COOLDOWN;
		}
		EngraveTotals mods = JobWeapons.activeTotals(player);
		int cost = manaCost(skill, mods);
		if (data.mana() < cost && !player.isCreative()) {
			return Result.NO_MANA;
		}
		int cooldown = cooldown(data, skill, mods);
		boolean echo = player.getRandom().nextFloat() < mods.fraction(EngraveStat.ECHO);
		JobData updated = data.withMana(player.isCreative() ? data.mana() : data.mana() - cost);
		if (!echo) {
			// the weapon's own key plus the class-wide slot key: swapping to another weapon of the class does not refresh the slot
			updated = updated.withCooldown(skill.id(), now + cooldown).withCooldown(slotKey(data.job(), slot), now + cooldown);
		}
		JobProgression.set(player, updated);
		double powerBonus = CombatHooks.has(data, JobClass.MAGE, 3) ? ARCANE_MASTERY : 0.0;
		SkillContext ctx = new SkillContext(player, def, skill, stack, mods, powerBonus);
		player.swing(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT, true);
		skill.cast(ctx);
		// Sword of Selection (warrior tier 4): an ultimate sharpens the critical hits that follow
		if (skill.kind() == SkillKind.ULTIMATE && CombatHooks.has(data, JobClass.WARRIOR, 4)) {
			player.addEffect(new MobEffectInstance(ModEffects.SELECTION, CombatHooks.SELECTION_TICKS, 0, false, false, true));
		}
		float heal = mods.get(EngraveStat.HEAL_ON_SKILL);
		if (heal > 0.0F) {
			player.heal(heal);
		}
		if (echo) {
			ctx.fx.burst(ctx.level, Fx.Kind.RUNE, player.position().add(0, 2.2, 0), 1, 0.0, 0.0);
		}
		return Result.OK;
	}

	public static int manaCost(final Skill skill, final EngraveTotals mods) {
		return Math.round(skill.manaCost() * (1.0F - mods.fraction(EngraveStat.MANA_COST)));
	}

	/** Cooldown key shared by every weapon of {@code job} for skill slot {@code slot} (0-based). */
	public static String slotKey(final JobClass job, final int slot) {
		return "slot." + job.name().toLowerCase(java.util.Locale.ROOT) + "." + slot;
	}

	/** When {@code skill} in {@code slot} can be cast again: the later of the weapon's own cooldown and the class slot's. */
	public static long readyAt(final JobData data, final Skill skill, final int slot) {
		return Math.max(data.readyAt(skill.id()), data.readyAt(slotKey(data.job(), slot)));
	}

	/** What a skill of {@code def} multiplies its percentages with ({@code SkillContext.power}): skill power, skill damage and Arcane Mastery. */
	public static double castPower(final JobData data, final WeaponDef def, final EngraveTotals mods) {
		return def.skillPower() * (1.0 + mods.fraction(EngraveStat.SKILL_DAMAGE) + (CombatHooks.has(data, JobClass.MAGE, 3) ? ARCANE_MASTERY : 0.0));
	}

	/**
	 * Percent reductions first, then flat seconds from armor; together they take at most {@link #MAX_COOLDOWN_CUT} off a skill (flat
	 * seconds on top of 50% took 6-9 s skills down to 1-2.5 s, so mana was the only limit left), and a skill never drops below 1 second
	 * (or its own cooldown if that is shorter).
	 */
	public static int cooldown(final JobData data, final Skill skill, final EngraveTotals mods) {
		int base = skill.cooldownTicks();
		float reduction = mods.fraction(EngraveStat.COOLDOWN) + (CombatHooks.has(data, JobClass.MAGE, 4) ? AKASHIC_RECORD : 0.0F);
		int ticks = Math.round(base * (1.0F - Math.min(MAX_COOLDOWN_CUT, reduction)));
		ticks -= Math.round(mods.get(EngraveStat.COOLDOWN_FLAT) * 20.0F);
		ticks = Math.max(ticks, Math.round(base * (1.0F - MAX_COOLDOWN_CUT)));
		return Math.max(Math.min(base, 20), ticks);
	}

	private static Component message(final ServerPlayer player, final int slot, final Result result) {
		WeaponDef def = JobWeapons.def(player.getMainHandItem());
		return switch (result) {
			case NO_WEAPON -> Component.translatable("message.minecraft_mode.skill.no_weapon").withStyle(ChatFormatting.GRAY);
			case WRONG_CLASS -> Component.translatable("message.minecraft_mode.skill.wrong_class", Component.translatable(def.job().nameKey()))
				.withStyle(ChatFormatting.RED);
			case LOW_TIER -> Component.translatable("message.minecraft_mode.skill.low_tier", def.tier()).withStyle(ChatFormatting.RED);
			case LOW_LEVEL -> Component.translatable("message.minecraft_mode.skill.low_level", def.level()).withStyle(ChatFormatting.RED);
			case STUNNED -> Component.translatable("message.minecraft_mode.skill.stunned").withStyle(ChatFormatting.YELLOW);
			case COOLDOWN -> {
				Skill skill = def.skills().get(slot);
				long left = JobProgression.get(player).readyAt(skill.id()) - player.level().getGameTime();
				yield Component.translatable("message.minecraft_mode.skill.cooldown", Component.translatable(skill.nameKey()), String.format(java.util.Locale.ROOT, "%.1f", left / 20.0))
					.withStyle(ChatFormatting.YELLOW);
			}
			case NO_MANA -> Component.translatable("message.minecraft_mode.skill.no_mana").withStyle(ChatFormatting.AQUA);
			default -> Component.empty();
		};
	}

	private SkillCaster() {
	}
}
