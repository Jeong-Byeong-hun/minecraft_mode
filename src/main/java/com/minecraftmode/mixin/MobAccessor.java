package com.minecraftmode.mixin;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** The target goals of a mob, which skill summons replace with their own ({@code SummonTargetGoal}). */
@Mixin(Mob.class)
public interface MobAccessor {
	@Accessor("targetSelector")
	GoalSelector minecraftMode$targetSelector();
}
