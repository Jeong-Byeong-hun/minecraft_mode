package com.minecraftmode.registry;

import com.minecraftmode.MinecraftMode;
import com.minecraftmode.effect.BleedingEffect;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

public final class ModEffects {
	public static final Holder<MobEffect> BLEEDING = Registry.registerForHolder(
		BuiltInRegistries.MOB_EFFECT,
		MinecraftMode.id("bleeding"),
		new BleedingEffect(MobEffectCategory.HARMFUL, 0x8A0303)
			.addAttributeModifier(Attributes.MOVEMENT_SPEED, MinecraftMode.id("effect.bleeding"), -0.1F, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
	);

	public static void init() {
	}

	private ModEffects() {
	}
}
