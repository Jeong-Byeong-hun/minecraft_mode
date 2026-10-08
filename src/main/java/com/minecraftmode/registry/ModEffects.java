package com.minecraftmode.registry;

import com.minecraftmode.MinecraftMode;
import com.minecraftmode.effect.BleedingEffect;
import com.minecraftmode.effect.MarkerEffect;
import com.minecraftmode.effect.StunEffect;
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

	/** Skill crowd control: cannot move or attack. */
	public static final Holder<MobEffect> STUN = Registry.registerForHolder(
		BuiltInRegistries.MOB_EFFECT,
		MinecraftMode.id("stun"),
		new StunEffect(MobEffectCategory.HARMFUL, 0xF2D649)
			.addAttributeModifier(Attributes.MOVEMENT_SPEED, MinecraftMode.id("effect.stun"), -1.0F, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
			.addAttributeModifier(Attributes.JUMP_STRENGTH, MinecraftMode.id("effect.stun"), -1.0F, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
	);
	/** Takes 15% more damage per level (see CombatHooks). */
	public static final Holder<MobEffect> VULNERABLE = Registry.registerForHolder(
		BuiltInRegistries.MOB_EFFECT, MinecraftMode.id("vulnerable"), new MarkerEffect(MobEffectCategory.HARMFUL, 0xC2185B)
	);
	/** Doubles MP regeneration (see JobStats). */
	public static final Holder<MobEffect> MANA_FLOW = Registry.registerForHolder(
		BuiltInRegistries.MOB_EFFECT, MinecraftMode.id("mana_flow"), new MarkerEffect(MobEffectCategory.BENEFICIAL, 0x4A8CFF)
	);

	public static void init() {
	}

	private ModEffects() {
	}
}
