package com.minecraftmode.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/** Effect with no tick behavior; code checks for it (e.g. vulnerability, mana flow). */
public class MarkerEffect extends MobEffect {
	public MarkerEffect(final MobEffectCategory category, final int color) {
		super(category, color);
	}
}
