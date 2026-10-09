package com.minecraftmode.job.skill;

import com.minecraftmode.job.JobClass;
import com.minecraftmode.job.JobData;
import com.minecraftmode.job.JobProgression;
import com.minecraftmode.job.engrave.EngraveStat;
import com.minecraftmode.job.engrave.EngraveTotals;
import com.minecraftmode.job.weapon.JobWeapons;
import com.minecraftmode.job.weapon.WeaponDef;
import com.minecraftmode.registry.ModEffects;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.item.ItemStack;

/** Validates and casts a weapon skill for a player (from the skill keys or commands/tests). */
public final class SkillCaster {
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
		long now = player.level().getGameTime();
		if (data.readyAt(skill.id()) > now) {
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
			updated = updated.withCooldown(skill.id(), now + cooldown);
		}
		JobProgression.set(player, updated);
		double powerBonus = CombatHooks.has(data, JobClass.MAGE, 3) ? 0.15 : 0.0;
		SkillContext ctx = new SkillContext(player, def, skill, stack, mods, powerBonus);
		player.swing(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT, true);
		skill.cast(ctx);
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

	/**
	 * Percent reductions (capped at 60%) first, then flat seconds from armor; a skill never drops
	 * below 1 second (or its own cooldown if that is shorter).
	 */
	public static int cooldown(final JobData data, final Skill skill, final EngraveTotals mods) {
		float reduction = mods.fraction(EngraveStat.COOLDOWN) + (CombatHooks.has(data, JobClass.MAGE, 4) ? 0.20F : 0.0F);
		int ticks = Math.round(skill.cooldownTicks() * (1.0F - Math.min(0.6F, reduction)));
		ticks -= Math.round(mods.get(EngraveStat.COOLDOWN_FLAT) * 20.0F);
		return Math.max(Math.min(skill.cooldownTicks(), 20), ticks);
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
