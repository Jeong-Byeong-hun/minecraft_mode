package com.minecraftmode.accessory;

import com.minecraftmode.enhance.Enhancement;
import com.minecraftmode.job.engrave.EngraveStat;
import com.minecraftmode.job.gear.ItemLevels;
import com.minecraftmode.job.gear.StatLine;
import com.minecraftmode.loot.GearShop;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;

/**
 * Enhancement of a raid accessory (0.5.24) at Artisan Brokk's bench: +1 .. +{@value #MAX}, kept in the same
 * {@code ModDataComponents.ENHANCEMENT} component as class gear (level and artisan's spirit; never awakened). Every attempt costs
 * accessory orbs ({@link #orbs}) and coins on the bracket price curve; a failure keeps the level and adds artisan's spirit, so no
 * protection is needed. Each level raises the piece's own lines (fixed and rolled) by {@link #PER_LEVEL}, and +{@value #MAX} adds
 * {@link #MAX_DAMAGE}% damage dealt.
 */
public final class AccessoryEnhance {
	public static final int MAX = 10;
	/** Success chance in percent for reaching +1 .. +10. */
	private static final int[] RATE = {100, 100, 95, 90, 80, 70, 60, 50, 40, 30};
	/** Share of the piece's own lines each level adds (+10 = +50%). */
	public static final float PER_LEVEL = 0.05F;
	/** Damage dealt a +10 piece adds on top. */
	public static final float MAX_DAMAGE = 2.0F;

	public static int baseRate(final int target) {
		return RATE[Math.max(1, Math.min(MAX, target)) - 1];
	}

	/** Chance in percent of reaching {@code target}, with the artisan's spirit. */
	public static int chance(final int target, final int pity) {
		return Math.min(100, baseRate(target) + pity);
	}

	/** Accessory orbs for one attempt at {@code target}: 1 up to +4, 2 up to +7, 3 for +8 to +10. */
	public static int orbs(final int target) {
		return target <= 4 ? 1 : target <= 7 ? 2 : 3;
	}

	/** Coins for one attempt at {@code target} (as class gear of the piece's bracket). */
	public static int coins(final AccessoryDef def, final int target) {
		return Math.max(1, Math.round(GearShop.bracketPrice(ItemLevels.bracket(def.level())) * (0.1F + 0.05F * target)));
	}

	public static Enhancement succeeded(final Enhancement current) {
		return new Enhancement(Math.min(MAX, current.level() + 1), 0, 0);
	}

	public static Enhancement failed(final Enhancement current) {
		return new Enhancement(current.level(), Math.min(100, current.pity() + Enhancement.PITY_STEP), 0);
	}

	/** How much stronger the piece's own lines are at {@code level}. */
	public static float scale(final int level) {
		return 1.0F + PER_LEVEL * Math.max(0, Math.min(MAX, level));
	}

	/** {@code line} as a piece of {@code level} gives it (one decimal). */
	public static StatLine scaled(final StatLine line, final int level) {
		if (level <= 0) {
			return line;
		}
		return StatLine.of(line.stat(), Math.round(line.value() * scale(level) * 10.0F) / 10.0F);
	}

	/** What a piece of {@code level} adds besides its own lines. */
	public static List<StatLine> bonus(final int level) {
		return level >= MAX ? List.of(StatLine.of(EngraveStat.DAMAGE, MAX_DAMAGE)) : List.of();
	}

	/** Name colour: white, green from +4, blue from +7, gold at +10. */
	public static int color(final int level) {
		return level >= MAX ? 0xFFAA00 : level >= 7 ? 0x55AAFF : level >= 4 ? 0x55FF55 : 0xFFFFFF;
	}

	/** "+7 " in front of {@code name} when {@code stack} is enhanced. */
	public static Component decorate(final MutableComponent name, final ItemStack stack) {
		int level = Enhancement.level(stack);
		return level <= 0 ? name : Component.literal("+" + level + " ").withColor(color(level)).append(name);
	}

	private AccessoryEnhance() {
	}
}
