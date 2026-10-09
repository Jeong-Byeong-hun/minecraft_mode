package com.minecraftmode.client.job;

import com.minecraftmode.job.JobData;
import com.minecraftmode.job.JobProgression;
import com.minecraftmode.job.engrave.EngraveStat;
import com.minecraftmode.job.engrave.Engraving;
import com.minecraftmode.job.engrave.Engravings;
import com.minecraftmode.job.gear.ArmorPieceDef;
import com.minecraftmode.job.gear.ArmorSetDef;
import com.minecraftmode.job.gear.GearRolls;
import com.minecraftmode.job.gear.GearRules;
import com.minecraftmode.job.gear.GearStats;
import com.minecraftmode.job.gear.ItemLevels;
import com.minecraftmode.job.gear.StatLine;
import com.minecraftmode.registry.ModDataComponents;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Class armor tooltip: requirements (red when unmet), set and worn piece count, the fixed option,
 * the rolled options, engravings (4 lines) and the 2/3/4-piece bonuses (lit when active).
 */
public final class GearTooltips {
	public static List<Component> build(final ItemStack stack, final ArmorPieceDef def) {
		List<Component> lines = new ArrayList<>();
		Player player = Minecraft.getInstance().player;
		JobData data = player == null ? JobData.DEFAULT : JobProgression.get(player);
		boolean classOk = data.job() == def.job();
		boolean tierOk = classOk && data.tier() >= def.tier();
		boolean levelOk = data.level() >= def.level();
		lines.add(Component.empty()
			.append(Component.translatable(def.job().nameKey()).withColor(classOk ? def.job().color() : 0xFF5555))
			.append(Component.literal(" · ").withStyle(ChatFormatting.DARK_GRAY))
			.append(Component.translatable("tooltip.minecraft_mode.weapon.tier", def.tier(), Component.translatable(def.job().tierKey(def.tier())))
				.withStyle(tierOk ? ChatFormatting.GRAY : ChatFormatting.RED))
			.append(Component.literal(" · ").withStyle(ChatFormatting.DARK_GRAY))
			.append(Component.translatable("tooltip.minecraft_mode.weapon.level", def.level()).withStyle(levelOk ? ChatFormatting.GRAY : ChatFormatting.RED)));
		if (!GearRules.canUse(data, def.job(), def.level())) {
			lines.add(Component.translatable("tooltip.minecraft_mode.gear.locked").withStyle(ChatFormatting.RED));
		}

		ArmorSetDef set = def.set();
		int worn = player == null ? 0 : GearStats.setCounts(player).getOrDefault(set.id(), 0);
		lines.add(Component.translatable("tooltip.minecraft_mode.gear.set", Component.translatable(set.nameKey()), worn).withColor(set.accent() | 0xFF000000));

		lines.add(Component.translatable("tooltip.minecraft_mode.gear.base").withStyle(ChatFormatting.GOLD));
		lines.add(statLine(def.baseOption(), ChatFormatting.WHITE));

		GearRolls rolls = stack.get(ModDataComponents.GEAR_ROLLS);
		lines.add(Component.translatable("tooltip.minecraft_mode.gear.options").withStyle(ChatFormatting.AQUA));
		if (rolls == null) {
			lines.add(Component.translatable("tooltip.minecraft_mode.gear.unrolled", def.optionCount()).withStyle(ChatFormatting.DARK_GRAY));
		} else {
			for (StatLine line : rolls.lines()) {
				lines.add(statLine(line, ChatFormatting.AQUA));
			}
		}

		Engravings engravings = stack.getOrDefault(ModDataComponents.ENGRAVINGS, Engravings.EMPTY);
		lines.add(Component.translatable("tooltip.minecraft_mode.weapon.engravings", engravings.lines().size(), Engravings.ARMOR_LINES).withStyle(ChatFormatting.LIGHT_PURPLE));
		Map<Engraving, Integer> counts = new LinkedHashMap<>();
		for (Engraving e : engravings.resolved()) {
			counts.merge(e, 1, Integer::sum);
		}
		if (counts.isEmpty()) {
			lines.add(Component.translatable("tooltip.minecraft_mode.weapon.no_engravings").withStyle(ChatFormatting.DARK_GRAY));
		}
		counts.forEach((e, n) -> lines.add(JobTooltips.engravingLine(e, n)));

		lines.add(Component.translatable("tooltip.minecraft_mode.gear.set_bonus", Component.translatable(set.nameKey())).withStyle(ChatFormatting.YELLOW));
		for (ArmorSetDef.SetBonus bonus : set.bonuses()) {
			boolean active = worn >= bonus.pieces();
			ChatFormatting color = active ? ChatFormatting.GREEN : ChatFormatting.DARK_GRAY;
			MutableComponent row = Component.literal(" (" + bonus.pieces() + ") ").withStyle(color);
			for (int i = 0; i < bonus.lines().size(); i++) {
				if (i > 0) {
					row.append(Component.literal(", ").withStyle(color));
				}
				StatLine line = bonus.lines().get(i);
				row.append(Component.translatable(line.stat().key(), JobTooltips.num(line.value())).withStyle(color));
			}
			lines.add(row);
		}
		lines.add(Component.translatable("tooltip.minecraft_mode.gear.bracket", ItemLevels.bracket(def.level())).withStyle(ChatFormatting.DARK_GRAY));
		lines.add(Component.empty());
		return lines;
	}

	static Component statLine(final StatLine line, final ChatFormatting color) {
		EngraveStat stat = line.stat();
		return Component.literal(" • ").withStyle(ChatFormatting.DARK_GRAY).append(Component.translatable(stat.key(), JobTooltips.num(line.value())).withStyle(color));
	}

	private GearTooltips() {
	}
}
