package com.minecraftmode.job.skill;

import com.minecraftmode.mixin.MobAccessor;
import java.util.Comparator;
import java.util.EnumSet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import org.jspecify.annotations.Nullable;

/**
 * What a skill summon fights, in place of vanilla's target goals: what its caster hit last (skills included), else whoever hurt its
 * caster, else the nearest enemy of its caster, training dummies included. Vanilla's goals left wolves idle unless the caster swung
 * at something (no dummies at all: wolves refuse armor stands, golems only look for mobs) and let golems and wolves turn on the
 * caster or a party member who hit them.
 */
public final class SummonTargetGoal extends Goal {
	/** How long the caster's last hit or last attacker stays the summons' target, in ticks. */
	private static final int MEMORY = 100;
	/** How far a summon looks for an enemy on its own, and how far it follows one. */
	private static final double SEEK = 16.0;
	private static final double FOLLOW = 32.0;

	private final Mob mob;

	private SummonTargetGoal(final Mob mob) {
		this.mob = mob;
		this.setFlags(EnumSet.of(Goal.Flag.TARGET));
	}

	/** Replaces every target goal of {@code mob} (a fresh skill summon) with this one. */
	public static void install(final Mob mob) {
		var selector = ((MobAccessor)mob).minecraftMode$targetSelector();
		selector.removeAllGoals(goal -> true);
		selector.addGoal(1, new SummonTargetGoal(mob));
		mob.setTarget(null);
	}

	@Override
	public boolean canUse() {
		return true;
	}

	@Override
	public boolean requiresUpdateEveryTick() {
		return false;
	}

	@Override
	public void tick() {
		if (!(this.mob.level() instanceof ServerLevel level)) {
			return;
		}
		ServerPlayer caster = CombatHooks.summonOwner(level, this.mob);
		LivingEntity pick = caster == null ? null : this.choose(caster);
		if (pick != this.mob.getTarget()) {
			this.mob.setTarget(pick);
		}
	}

	private @Nullable LivingEntity choose(final ServerPlayer caster) {
		LivingEntity hit = caster.getLastHurtMob();
		if (hit != null && caster.tickCount - caster.getLastHurtMobTimestamp() < MEMORY && this.fights(caster, hit)) {
			return hit;
		}
		LivingEntity by = caster.getLastHurtByMob();
		if (by != null && caster.tickCount - caster.getLastHurtByMobTimestamp() < MEMORY && this.fights(caster, by)) {
			return by;
		}
		LivingEntity current = this.mob.getTarget();
		if (current != null && !(current instanceof Player) && this.fights(caster, current)) {
			return current;
		}
		// players only when the caster fights them (above): a summon never starts a fight with someone on its own
		return this.mob.level().getEntitiesOfClass(LivingEntity.class, this.mob.getBoundingBox().inflate(SEEK),
				e -> !(e instanceof Player) && CombatHooks.isEnemy(caster, e))
			.stream().min(Comparator.comparingDouble(this.mob::distanceToSqr)).orElse(null);
	}

	private boolean fights(final ServerPlayer caster, final LivingEntity target) {
		return target.level() == this.mob.level() && target.distanceToSqr(this.mob) < FOLLOW * FOLLOW && CombatHooks.isEnemy(caster, target);
	}
}
