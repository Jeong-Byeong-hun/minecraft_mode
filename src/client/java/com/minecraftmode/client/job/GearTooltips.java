package com.minecraftmode.client.job;

import com.minecraftmode.enhance.Enhancement;
import com.minecraftmode.job.JobData;
import com.minecraftmode.job.JobProgression;
import com.minecraftmode.job.engrave.EngraveStat;
import com.minecraftmode.job.engrave.Engraving;
import com.minecraftmode.job.engrave.Engravings;
import com.minecraftmode.job.gear.ArmorPieceDef;
import com.minecraftmode.job.gear.ArmorSetDef;
import com.minecraftmode.job.gear.ClassGear;
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
		enhancement(lines, stack);

		ArmorSetDef set = def.set();
		int worn = player == null ? 0 : GearStats.setCounts(player).getOrDefault(set.id(), 0);
		lines.add(Component.translatable("tooltip.minecraft_mode.gear.set", Component.translatable(set.nameKey()), worn).withColor(set.accent() | 0xFF000000));

		lines.add(Component.translatable("tooltip.minecraft_mode.gear.base").withStyle(ChatFormatting.GOLD));
		// armor and toughness (the item hides vanilla's "When on feet:" block and its unbreakable line)
		lines.add(Component.literal(" • ").withStyle(ChatFormatting.DARK_GRAY).append(def.toughness() > 0.0F
			? Component.translatable("tooltip.minecraft_mode.gear.armor_toughness", def.armor(), JobTooltips.num(def.toughness()))
			: Component.translatable("tooltip.minecraft_mode.gear.armor", def.armor()))
			.withStyle(ChatFormatting.BLUE));
		lines.add(statLine(def.baseOption(), ChatFormatting.WHITE));
		for (StatLine line : def.defenseLines()) {
			lines.add(statLine(line, ChatFormatting.WHITE));
		}

		GearRolls rolls = stack.get(ModDataComponents.GEAR_ROLLS);
		lines.add(Component.translatable("tooltip.minecraft_mode.gear.options").withStyle(ChatFormatting.AQUA));
		if (rolls == null) {
			lines.add(Component.translatable("tooltip.minecraft_mode.gear.unrolled", def.optionCount()).withStyle(ChatFormatting.DARK_GRAY));
		} else {
			for (StatLine line : rolls.lines()) {
				lines.add(statLine(line, ChatFormatting.AQUA));
			}
		}
		GearRolls pending = stack.get(ModDataComponents.GEAR_ROLLS_PENDING);
		if (pending != null) {
			lines.add(Component.translatable("tooltip.minecraft_mode.gear.pending").withStyle(ChatFormatting.DARK_AQUA));
			for (StatLine line : pending.lines()) {
				lines.add(statLine(line, ChatFormatting.DARK_AQUA));
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
		// one row per stat (joined on one row they ran far past the screen edge)
		for (ArmorSetDef.SetBonus bonus : set.bonuses()) {
			ChatFormatting color = worn >= bonus.pieces() ? ChatFormatting.GREEN : ChatFormatting.DARK_GRAY;
			for (StatLine line : bonus.lines()) {
				lines.add(Component.literal(" (" + bonus.pieces() + ") ").withStyle(color)
					.append(Component.translatable(line.stat().key(), JobTooltips.num(line.value())).withStyle(color)));
			}
		}
		lines.add(Component.translatable("tooltip.minecraft_mode.gear.bracket", ItemLevels.bracket(def.level())).withStyle(ChatFormatting.DARK_GRAY));
		lines.add(Component.empty());
		return lines;
	}

	/** "Enhancement +7 (artisan's spirit 10%)" and what the level adds; nothing for unenhanced gear. */
	public static void enhancement(final List<Component> lines, final ItemStack stack) {
		ClassGear gear = ClassGear.of(stack);
		Enhancement e = Enhancement.of(stack);
		if (gear == null || e.level() <= 0 && e.pity() <= 0) {
			return;
		}
		MutableComponent head = Component.translatable("tooltip.minecraft_mode.enhance.level", e.level(), Enhancement.MAX).withColor(Enhancement.color(e.level()));
		if (e.pity() > 0) {
			head.append(Component.literal(" · ").withStyle(ChatFormatting.DARK_GRAY)).append(Component.translatable("tooltip.minecraft_mode.enhance.pity", e.pity())
				.withStyle(ChatFormatting.GRAY));
		}
		if (e.awaken() > 0) {
			head.append(Component.literal(" · ").withStyle(ChatFormatting.DARK_GRAY)).append(Component.translatable("tooltip.minecraft_mode.enhance.awaken", e.awaken(),
				Enhancement.MAX_AWAKEN).withColor(0xFF55FF));
		}
		lines.add(head);
		for (StatLine line : Enhancement.lines(gear, e.level(), e.awaken())) {
			lines.add(statLine(line, ChatFormatting.GREEN));
		}
	}

	static Component statLine(final StatLine line, final ChatFormatting color) {
		EngraveStat stat = line.stat();
		return Component.literal(" • ").withStyle(ChatFormatting.DARK_GRAY).append(Component.translatable(stat.key(), JobTooltips.num(line.value())).withStyle(color));
	}

	private GearTooltips() {
	}
}
