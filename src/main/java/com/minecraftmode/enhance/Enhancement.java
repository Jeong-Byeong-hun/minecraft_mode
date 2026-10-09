package com.minecraftmode.enhance;

import com.minecraftmode.job.engrave.EngraveStat;
import com.minecraftmode.job.gear.ClassGear;
import com.minecraftmode.job.gear.StatLine;
import com.minecraftmode.loot.GearShop;
import com.minecraftmode.registry.ModDataComponents;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

/**
 * Enhancement of a class weapon or armor piece: level +0..+15 and the "artisan's spirit" bonus
 * ({@code pity}, percent) that grows with every failure until the next success. Evolution keeps it.
 */
public record Enhancement(int level, int pity) {
	public static final Enhancement NONE = new Enhancement(0, 0);
	public static final int MAX = 15;
	/** Success chance in percent for reaching +1 .. +15. */
	private static final int[] RATE = {100, 100, 100, 100, 100, 90, 80, 70, 60, 50, 40, 35, 30, 25, 20};
	/** Extra chance per failure since the last success. */
	public static final int PITY_STEP = 5;
	/** From this target level on a failure drops one level unless a protection scroll is used. */
	public static final int RISKY_FROM = 11;

	public static final Codec<Enhancement> CODEC = RecordCodecBuilder.create(i -> i.group(
		Codec.INT.optionalFieldOf("level", 0).forGetter(Enhancement::level),
		Codec.INT.optionalFieldOf("pity", 0).forGetter(Enhancement::pity)
	).apply(i, Enhancement::new));

	public static final StreamCodec<ByteBuf, Enhancement> STREAM_CODEC = StreamCodec.composite(
		ByteBufCodecs.VAR_INT, Enhancement::level,
		ByteBufCodecs.VAR_INT, Enhancement::pity,
		Enhancement::new
	);

	public static Enhancement of(final ItemStack stack) {
		return stack.getOrDefault(ModDataComponents.ENHANCEMENT, NONE);
	}

	public static int level(final ItemStack stack) {
		return of(stack).level();
	}

	public static int baseRate(final int target) {
		return RATE[Math.max(1, Math.min(MAX, target)) - 1];
	}

	/** Chance in percent of reaching {@code target}, including the artisan's spirit. */
	public static int chance(final int target, final int pity) {
		return Math.min(100, baseRate(target) + pity);
	}

	public static boolean risky(final int target) {
		return target >= RISKY_FROM;
	}

	/** Coins for one attempt at {@code target}. */
	public static int coins(final ClassGear gear, final int target) {
		return Math.max(1, Math.round(GearShop.bracketPrice(gear.bracket()) * (0.1F + 0.05F * target)));
	}

	/** Essence for one attempt: plain essence up to +5, condensed essence after. */
	public static int essence(final int target) {
		return target <= 5 ? 2 + target : 1 + (target - 6) / 3;
	}

	public static boolean condensed(final int target) {
		return target > 5;
	}

	/** Enhancement stones for one attempt (from +6). */
	public static int stones(final int target) {
		return target < 6 ? 0 : 1 + (target - 6) / 3;
	}

	/** What {@code level} adds to a piece of {@code gear}. */
	public static List<StatLine> lines(final ClassGear gear, final int level) {
		List<StatLine> out = new ArrayList<>();
		if (level <= 0) {
			return out;
		}
		if (gear.isWeapon()) {
			out.add(StatLine.of(EngraveStat.BASIC_DAMAGE, 3 * level));
			out.add(StatLine.of(EngraveStat.SKILL_DAMAGE, 3 * level));
			if (level >= 10) {
				out.add(StatLine.of(EngraveStat.CRIT_CHANCE, 3));
			}
			if (level >= 15) {
				out.add(StatLine.of(EngraveStat.BOSS_DAMAGE, 10));
			}
		} else {
			out.add(StatLine.of(EngraveStat.DAMAGE_REDUCTION, 0.5F * level));
			out.add(StatLine.of(EngraveStat.MAX_HEALTH, Math.round(4.0F * level) / 10.0F));
			if (level >= 10) {
				out.add(StatLine.of(EngraveStat.MOVE_SPEED, 2));
			}
			if (level >= 15) {
				out.add(StatLine.of(EngraveStat.DAMAGE_REDUCTION, 2));
			}
		}
		return out;
	}

	/** Name colour for {@code level}: white, green from +5, blue from +10, purple from +13, gold at +15. */
	public static int color(final int level) {
		return level >= MAX ? 0xFFAA00 : level >= 13 ? 0xC060FF : level >= 10 ? 0x55AAFF : level >= 5 ? 0x55FF55 : 0xFFFFFF;
	}

	/** "+7 " in front of {@code name} when {@code stack} is enhanced. */
	public static Component decorate(final MutableComponent name, final ItemStack stack) {
		int level = level(stack);
		return level <= 0 ? name : Component.literal("+" + level + " ").withColor(color(level)).append(name);
	}

	public Enhancement succeeded() {
		return new Enhancement(Math.min(MAX, this.level + 1), 0);
	}

	public Enhancement failed(final boolean drop) {
		return new Enhancement(drop ? Math.max(0, this.level - 1) : this.level, Math.min(100, this.pity + PITY_STEP));
	}
}
