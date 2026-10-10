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
 * Enhancement of a class weapon or armor piece: level +0..+15, the "artisan's spirit" bonus ({@code pity}, percent) that grows
 * with every failure until the next success, and past +15 the awakening ({@code awaken}, 0..{@value #MAX_AWAKEN}) paid with
 * awakening crystals. Evolution keeps all of it.
 */
public record Enhancement(int level, int pity, int awaken) {
	public static final Enhancement NONE = new Enhancement(0, 0, 0);
	public static final int MAX_AWAKEN = 5;
	public static final int MAX = 15;
	/** Success chance in percent for reaching +1 .. +15. */
	private static final int[] RATE = {100, 100, 100, 100, 100, 90, 80, 70, 60, 50, 40, 35, 30, 25, 20};
	/** Extra chance per failure since the last success. */
	public static final int PITY_STEP = 5;
	/** From this target level on a failure drops one level unless a protection scroll is used. */
	public static final int RISKY_FROM = 11;

	public static final Codec<Enhancement> CODEC = RecordCodecBuilder.create(i -> i.group(
		Codec.INT.optionalFieldOf("level", 0).forGetter(Enhancement::level),
		Codec.INT.optionalFieldOf("pity", 0).forGetter(Enhancement::pity),
		Codec.INT.optionalFieldOf("awaken", 0).forGetter(Enhancement::awaken)
	).apply(i, Enhancement::new));

	public static final StreamCodec<ByteBuf, Enhancement> STREAM_CODEC = StreamCodec.composite(
		ByteBufCodecs.VAR_INT, Enhancement::level,
		ByteBufCodecs.VAR_INT, Enhancement::pity,
		ByteBufCodecs.VAR_INT, Enhancement::awaken,
		Enhancement::new
	);

	public Enhancement(final int level, final int pity) {
		this(level, pity, 0);
	}

	/** Awakening crystals for awakening {@code target} (1..5). */
	public static int crystals(final int target) {
		return target;
	}

	/** Coins for awakening {@code target} on {@code gear}. */
	public static int awakenCoins(final ClassGear gear, final int target) {
		return Math.max(1, GearShop.bracketPrice(gear.bracket()) * (1 + target));
	}

	public Enhancement awakened() {
		return new Enhancement(this.level, 0, Math.min(MAX_AWAKEN, this.awaken + 1));
	}

	public static Enhancement of(final ItemStack stack) {
		return stack.getOrDefault(ModDataComponents.ENHANCEMENT, NONE);
	}

	public static int level(final ItemStack stack) {
		return of(stack).level();
	}

	/** From this level enhanced gear shines: the enchantment glint, a coloured slot frame and particles while held or worn. */
	public static final int GLOW_FROM = 10;
	public static final int GLOW_BRIGHT = 13;

	/** How brightly {@code stack} shows its enhancement: 0 below +10, 1 from +10, 2 from +13, 3 at +15, 4 once awakened. */
	public static int glow(final ItemStack stack) {
		Enhancement e = of(stack);
		if (e.level < GLOW_FROM) {
			return 0;
		}
		return e.awaken > 0 ? 4 : e.level >= MAX ? 3 : e.level >= GLOW_BRIGHT ? 2 : 1;
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

	/** What {@code level} and {@code awaken} add to a piece of {@code gear}. */
	public static List<StatLine> lines(final ClassGear gear, final int level, final int awaken) {
		List<StatLine> out = new ArrayList<>(lines(gear, level));
		if (awaken > 0) {
			if (gear.isWeapon()) {
				out.add(StatLine.of(EngraveStat.BASIC_DAMAGE, 2.0F * awaken));
				out.add(StatLine.of(EngraveStat.SKILL_DAMAGE, 2.0F * awaken));
				out.add(StatLine.of(EngraveStat.BOSS_DAMAGE, 2.0F * awaken));
			} else {
				out.add(StatLine.of(EngraveStat.DAMAGE_REDUCTION, 0.6F * awaken));
				out.add(StatLine.of(EngraveStat.MAX_HEALTH, 0.6F * awaken));
			}
		}
		return out;
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
		Enhancement e = of(stack);
		if (e.level() <= 0) {
			return name;
		}
		String tag = "+" + e.level() + (e.awaken() > 0 ? "✦" + e.awaken() : "") + " ";
		return Component.literal(tag).withColor(e.awaken() > 0 ? 0xFF55FF : color(e.level())).append(name);
	}

	public Enhancement succeeded() {
		return new Enhancement(Math.min(MAX, this.level + 1), 0, this.awaken);
	}

	public Enhancement failed(final boolean drop) {
		return new Enhancement(drop ? Math.max(0, this.level - 1) : this.level, Math.min(100, this.pity + PITY_STEP), this.awaken);
	}
}
