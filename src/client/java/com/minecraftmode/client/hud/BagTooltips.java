package com.minecraftmode.client.hud;

import com.minecraftmode.bag.BagItem;
import com.minecraftmode.bag.Bags;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

/** Bag tooltips: what the bag picks up by itself, how full it is and how to open it. */
public final class BagTooltips {
	public static void init() {
		ItemTooltipCallback.EVENT.register((stack, context, flag, lines) -> {
			if (!(stack.getItem() instanceof BagItem bag)) {
				return;
			}
			long used = Bags.contents(stack).stream().filter(s -> !s.isEmpty()).count();
			lines.add(1, Component.translatable("item.minecraft_mode." + bag.kind().id() + ".tooltip").withStyle(ChatFormatting.GRAY));
			lines.add(2, Component.translatable("item.minecraft_mode.bag.slots", used, Bags.SIZE).withStyle(ChatFormatting.DARK_GRAY));
			lines.add(3, Component.translatable("item.minecraft_mode.bag.usage").withStyle(ChatFormatting.DARK_AQUA));
		});
	}

	private BagTooltips() {
	}
}
