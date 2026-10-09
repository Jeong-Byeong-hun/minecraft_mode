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

	// consumable buffs: markers read as stat lines by BuffEffects (see GearStats)
	/** Basic attack and shot damage (consumables). */
	public static final Holder<MobEffect> FURY = buff("fury", 0xD8402A);
	/** Less damage taken. */
	public static final Holder<MobEffect> IRONSKIN = buff("ironskin", 0x8A8F99);
	/** Critical chance. */
	public static final Holder<MobEffect> PRECISION = buff("precision", 0x3FC9B0);
	/** Skill damage. */
	public static final Holder<MobEffect> ARCANA = buff("arcana", 0x9B59D0);
	/** Shorter skill cooldowns. */
	public static final Holder<MobEffect> FOCUS = buff("focus", 0x6FA8DC);
	/** MP regeneration. */
	public static final Holder<MobEffect> CLARITY = buff("clarity", 0x4A8CFF);
	/** Heals every 4 seconds. */
	public static final Holder<MobEffect> REJUVENATION = buff("rejuvenation", 0x7CD86A);
	/** Equipment and coin drops. */
	public static final Holder<MobEffect> FORTUNE = buff("fortune", 0xE8B730);
	/** Class experience. */
	public static final Holder<MobEffect> WISDOM = buff("wisdom", 0xF2E6C0);
	/** Damage to bosses and named monsters. */
	public static final Holder<MobEffect> SLAYER = buff("slayer", 0x8E1F1F);

	private static Holder<MobEffect> buff(final String id, final int color) {
		return Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, MinecraftMode.id(id), new MarkerEffect(MobEffectCategory.BENEFICIAL, color));
	}

	public static void init() {
	}

	private ModEffects() {
	}
}
