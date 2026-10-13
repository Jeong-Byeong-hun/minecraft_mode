package com.minecraftmode.client.accessory;

import com.minecraftmode.accessory.Accessories;
import com.minecraftmode.accessory.AccessoryDef;
import com.minecraftmode.accessory.AccessorySet;
import com.minecraftmode.client.job.GearTooltips;
import com.minecraftmode.client.job.JobTooltips;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Accessory tooltip: slot and level (red when too low), where it comes from (only that boss's raid), the fixed line, the rolled
 * options and the set's 2/3/5-piece bonuses (lit by the pieces worn). Built off the render thread too (search), so the viewer may
 * be missing and nothing here touches the font.
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
		lines.add(Component.translatable("tooltip.minecraft_mode.accessory.source", Component.translatable(set.boss().nameKey())).withStyle(ChatFormatting.DARK_PURPLE));
		lines.add(Component.translatable("tooltip.minecraft_mode.gear.base").withStyle(ChatFormatting.GOLD));
		lines.add(GearTooltips.statLine(def.baseLine(), ChatFormatting.WHITE, null));
		GearRolls rolls = stack.get(ModDataComponents.GEAR_ROLLS);
		lines.add(Component.translatable("tooltip.minecraft_mode.gear.options").withStyle(ChatFormatting.AQUA));
		if (rolls == null) {
			lines.add(Component.translatable("tooltip.minecraft_mode.gear.unrolled", 1).withStyle(ChatFormatting.DARK_GRAY));
		} else {
			for (StatLine line : rolls.lines()) {
				lines.add(GearTooltips.statLine(line, ChatFormatting.AQUA, null));
			}
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
		lines.add(Component.translatable("tooltip.minecraft_mode.accessory.wear").withStyle(ChatFormatting.DARK_GRAY));
		lines.add(Component.empty());
		return lines;
	}

	private AccessoryTooltips() {
	}
}
