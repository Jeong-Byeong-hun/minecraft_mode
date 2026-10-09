package com.minecraftmode.client;

import com.minecraftmode.client.job.JobTooltips;
import com.minecraftmode.consumable.BuffEffects;
import com.minecraftmode.consumable.ConsumableDef;
import com.minecraftmode.consumable.ConsumableItem;
import com.minecraftmode.job.gear.StatLine;
import com.minecraftmode.registry.ModItems;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * Consumable tooltips: tier stars, every effect (instant heals, buffs with their stats and 10 minute
 * duration, specials), the shared cooldown and a line of flavor.
 */
public final class ConsumableTooltips {
	private static final String K = "consumable.minecraft_mode.";

	public static void init() {
		ItemTooltipCallback.EVENT.register((stack, context, flag, lines) -> {
			if (stack.getItem() instanceof ConsumableItem item) {
				lines.addAll(1, build(item.def()));
			} else if (stack.is(ModItems.RETURN_SCROLL)) {
				lines.add(1, Component.translatable("item.minecraft_mode.return_scroll.tooltip").withStyle(ChatFormatting.GRAY));
			}
		});
	}

	static List<Component> build(final ConsumableDef def) {
		List<Component> out = new ArrayList<>();
		out.add(Component.literal("★".repeat(def.tier()) + "☆".repeat(5 - def.tier())).withStyle(def.tier() >= 4 ? ChatFormatting.GOLD : ChatFormatting.YELLOW));
		if (def.heal() > 0.0F) {
			out.add(line(Component.translatable(K + "heal", pct(def.heal())), ChatFormatting.RED));
		}
		if (def.mana() > 0.0F) {
			out.add(line(Component.translatable(K + "mana", pct(def.mana())), ChatFormatting.BLUE));
		}
		if (def.cleanse()) {
			out.add(line(Component.translatable(K + "cleanse"), ChatFormatting.GREEN));
		}
		for (ConsumableDef.Buff buff : def.effects()) {
			MutableComponent name = Component.translatable(buff.effect().value().getDescriptionId()).append(" " + roman(buff.amplifier() + 1));
			MutableComponent text = Component.translatable(K + "effect", name, buff.ticks() / 1200);
			List<StatLine> stats = BuffEffects.lines(buff.effect(), buff.amplifier());
			for (int i = 0; i < stats.size(); i++) {
				text.append(i == 0 ? ": " : ", ").append(Component.translatable(stats.get(i).stat().key(), JobTooltips.num(stats.get(i).value())));
			}
			out.add(line(text, ChatFormatting.AQUA));
		}
		switch (def.special()) {
			case REVIVE -> out.add(line(Component.translatable(K + "revive"), ChatFormatting.GOLD));
			case EXP -> out.add(line(Component.translatable(K + "exp"), ChatFormatting.GOLD));
			case STAR -> out.add(line(Component.translatable(K + "star"), ChatFormatting.GOLD));
			case FEAST -> out.add(line(Component.translatable(K + "feast"), ChatFormatting.GOLD));
			default -> {
			}
		}
		if (def.cooldown() > 0) {
			out.add(Component.translatable(K + "cooldown", def.cooldown(), Component.translatable(K + "group." + def.group())).withStyle(ChatFormatting.DARK_GRAY));
		}
		out.add(Component.translatable(def.flavorKey()).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
		return out;
	}

	private static Component line(final Component text, final ChatFormatting color) {
		return Component.literal(" ").append(text).withStyle(color);
	}

	private static int pct(final float fraction) {
		return Math.round(fraction * 100);
	}

	private static String roman(final int n) {
		return switch (n) {
			case 1 -> "I";
			case 2 -> "II";
			case 3 -> "III";
			case 4 -> "IV";
			default -> "V";
		};
	}

	private ConsumableTooltips() {
	}
}
