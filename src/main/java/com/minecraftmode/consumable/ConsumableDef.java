package com.minecraftmode.consumable;

import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;

/**
 * One consumable: names, flavor, how rare it is ({@code tier} 1-5), how it is used and drawn, what it
 * does and where it comes from.
 *
 * @param nutrition 0 = a drink or pill (no hunger needed)
 * @param heal fraction of max health restored at once
 * @param mana fraction of max MP restored at once
 * @param effects lasting effects (10 minutes unless a short special)
 * @param cooldown seconds before anything of the same {@code group} can be used again (0 = none)
 * @param price guild-style price in copper (0 = not sold)
 */
public record ConsumableDef(
	String id, String en, String ko, String flavorEn, String flavorKo, int tier, Kind kind, Shape shape, int color, int accent, int nutrition, float saturation,
	float heal, float mana, boolean cleanse, Special special, List<Buff> effects, int cooldown, String group, Shop shop, int price
) {
	public ConsumableDef {
		effects = List.copyOf(effects);
	}

	public enum Kind {
		FOOD,
		DRINK,
		/** Never eaten: works from the inventory (the Phoenix Feather). */
		CHARM
	}

	/** How {@code ConsumableArtist} draws the icon. */
	public enum Shape {
		BOTTLE, VIAL, FLASK, CUP, CAN, BEAN, PILL, ROLL, BOWL, MEAT, FISH_BREAD, SWEET_POTATO, RICE_BALL, COOKIE, MUSHROOM, STAR, CANDY, FEATHER, HEART, STONE,
		BOX, JERKY, SCROLL
	}

	public enum Special {
		NONE,
		/** Revives from a lethal blow at half health (consumed). */
		REVIVE,
		/** Class experience: half of what the current level needs. */
		EXP,
		/** A short invulnerability (Resistance V) with speed. */
		STAR,
		/** Fills hunger completely. */
		FEAST
	}

	public enum Shop {
		NONE,
		GENERAL,
		GROCER,
		ALCHEMIST
	}

	/** A lasting effect: {@code amplifier} 0 = level I. */
	public record Buff(Holder<MobEffect> effect, int amplifier, int ticks) {
	}

	public String nameKey() {
		return "item.minecraft_mode." + this.id;
	}

	public String flavorKey() {
		return this.nameKey() + ".flavor";
	}
}
