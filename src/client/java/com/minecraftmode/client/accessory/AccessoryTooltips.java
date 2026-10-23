package com.minecraftmode.client.accessory;

import com.minecraftmode.accessory.Accessories;
import com.minecraftmode.accessory.AccessoryDef;
import com.minecraftmode.accessory.AccessoryEnhance;
import com.minecraftmode.accessory.AccessorySet;
import com.minecraftmode.client.job.GearTooltips;
import com.minecraftmode.client.job.JobTooltips;
import com.minecraftmode.enhance.Enhancement;
import com.minecraftmode.enhance.Imbue;
import com.minecraftmode.job.JobData;
import com.minecraftmode.job.JobProgression;
import com.minecraftmode.job.gear.ArmorSetDef;
import com.minecraftmode.job.gear.GearRolls;
import com.minecraftmode.job.gear.StatLine;
import com.minecraftmode.registry.ModDataComponents;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Accessory tooltip: slot and level (red when too low), where it comes from (that boss's raid, its tier), the enhancement, the fixed
 * line and the rolled options as the enhancement raises them, options waiting at the blacksmith and the set's 2/3/5-piece bonuses
 * (lit by the pieces worn). Built off the render thread too (search), so the viewer may be missing and nothing here touches the font.
 */
public final class AccessoryTooltips {
	public static void init() {
		ItemTooltipCallback.EVENT.register((stack, context, flag, lines) -> {
			AccessoryDef def = Accessories.def(stack);
			if (def != null) {
				lines.addAll(1, build(stack, def));
			}
		});
	}

	static List<Component> build(final ItemStack stack, final AccessoryDef def) {
		List<Component> lines = new ArrayList<>();
		Player player = JobTooltips.viewer();
		JobData data = player == null ? JobData.DEFAULT : JobProgression.get(player);
		boolean levelOk = Accessories.canWear(data, def);
		AccessorySet set = def.set();
		lines.add(Component.translatable(def.slot().nameKey()).withStyle(ChatFormatting.GOLD)
			.append(Component.literal(" · ").withStyle(ChatFormatting.DARK_GRAY))
			.append(Component.translatable("tooltip.minecraft_mode.weapon.level", def.level()).withStyle(levelOk ? ChatFormatting.GRAY : ChatFormatting.RED)));
		lines.add(Component.translatable("tooltip.minecraft_mode.accessory.source", Component.translatable(set.boss().nameKey())).withStyle(ChatFormatting.DARK_PURPLE)
			.append(Component.literal(" · ").withStyle(ChatFormatting.DARK_GRAY))
			.append(Component.translatable("tooltip.minecraft_mode.accessory.tier", set.tier()).withStyle(ChatFormatting.LIGHT_PURPLE)));
		Enhancement e = Enhancement.of(stack);
		if (e.level() > 0 || e.pity() > 0) {
			MutableComponent head = Component.translatable("tooltip.minecraft_mode.enhance.level", e.level(), AccessoryEnhance.MAX).withColor(AccessoryEnhance.color(e.level()));
			if (e.pity() > 0) {
				head.append(Component.literal(" · ").withStyle(ChatFormatting.DARK_GRAY)).append(Component.translatable("tooltip.minecraft_mode.enhance.pity", e.pity())
					.withStyle(ChatFormatting.GRAY));
			}
			lines.add(head);
			if (e.level() > 0) {
				lines.add(Component.literal(" • ").withStyle(ChatFormatting.DARK_GRAY).append(Component.translatable("tooltip.minecraft_mode.accessory.enhanced",
					JobTooltips.num(Math.round(AccessoryEnhance.PER_LEVEL * e.level() * 100.0F))).withStyle(ChatFormatting.GREEN)));
			}
			for (StatLine line : AccessoryEnhance.bonus(e.level())) {
				lines.add(GearTooltips.statLine(line, ChatFormatting.GREEN, null));
			}
		}
		lines.add(Component.translatable("tooltip.minecraft_mode.gear.base").withStyle(ChatFormatting.GOLD));
		lines.add(GearTooltips.statLine(AccessoryEnhance.scaled(def.baseLine(), e.level()), ChatFormatting.WHITE, null));
		GearRolls rolls = stack.get(ModDataComponents.GEAR_ROLLS);
		lines.add(Component.translatable("tooltip.minecraft_mode.gear.options").withStyle(ChatFormatting.AQUA));
		if (rolls == null) {
			lines.add(Component.translatable("tooltip.minecraft_mode.gear.unrolled", 1).withStyle(ChatFormatting.DARK_GRAY));
		} else {
			for (StatLine line : rolls.lines()) {
				lines.add(GearTooltips.statLine(AccessoryEnhance.scaled(line, e.level()), ChatFormatting.AQUA, null));
			}
		}
		GearRolls pending = stack.get(ModDataComponents.GEAR_ROLLS_PENDING);
		if (pending != null) {
			lines.add(Component.translatable("tooltip.minecraft_mode.gear.pending").withStyle(ChatFormatting.DARK_AQUA));
			for (StatLine line : pending.lines()) {
				lines.add(GearTooltips.statLine(AccessoryEnhance.scaled(line, e.level()), ChatFormatting.DARK_AQUA, null));
			}
		}
		Imbue imbue = Imbue.of(stack);
		if (imbue != null) {
			lines.add(Component.translatable(imbue.grade() >= Imbue.GREATER ? "tooltip.minecraft_mode.gear.imbued_greater" : "tooltip.minecraft_mode.gear.imbued")
				.withStyle(ChatFormatting.LIGHT_PURPLE));
			lines.add(GearTooltips.statLine(imbue.line(), ChatFormatting.LIGHT_PURPLE, null));
		}
		int worn = player == null ? 0 : Accessories.setCounts(player).getOrDefault(set.id(), 0);
		lines.add(Component.translatable("tooltip.minecraft_mode.gear.set_bonus", Component.translatable(set.nameKey())).withStyle(ChatFormatting.YELLOW));
		for (ArmorSetDef.SetBonus bonus : set.bonuses()) {
			ChatFormatting color = worn >= bonus.pieces() ? ChatFormatting.GREEN : ChatFormatting.DARK_GRAY;
			for (StatLine line : bonus.lines()) {
				lines.add(Component.literal(" (" + bonus.pieces() + ") ").withStyle(color)
					.append(Component.translatable(line.stat().key(), JobTooltips.num(line.value())).withStyle(color)));
			}
		}
		Item ticket = Accessories.inheritTicket(set.tier());
		if (ticket != null) {
			lines.add(Component.translatable("tooltip.minecraft_mode.accessory.inherit", Component.translatable(ticket.getDescriptionId()), set.tier() + 1)
				.withStyle(ChatFormatting.DARK_AQUA));
		}
		lines.add(Component.translatable("tooltip.minecraft_mode.accessory.wear").withStyle(ChatFormatting.DARK_GRAY));
		lines.add(Component.empty());
		return lines;
	}

	private AccessoryTooltips() {
	}
}
