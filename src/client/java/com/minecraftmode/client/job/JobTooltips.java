package com.minecraftmode.client.job;

import com.minecraftmode.job.JobData;
import com.minecraftmode.job.JobProgression;
import com.minecraftmode.job.engrave.EngraveStat;
import com.minecraftmode.job.engrave.Engraving;
import com.minecraftmode.job.engrave.Engravings;
import com.minecraftmode.job.gear.ArmorPieceDef;
import com.minecraftmode.job.gear.ClassArmor;
import com.minecraftmode.job.skill.Skill;
import com.minecraftmode.job.skill.SkillAction;
import com.minecraftmode.job.skill.SkillCaster;
import com.minecraftmode.job.weapon.Archetype;
import com.minecraftmode.job.weapon.JobWeaponItem;
import com.minecraftmode.job.weapon.JobWeapons;
import com.minecraftmode.job.weapon.WeaponDef;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Class weapon tooltip: requirements (red when unmet), basic attack, skill list with keys, MP and
 * cooldown (hold Shift for the generated descriptions), and engraving lines with their totals.
 */
public final class JobTooltips {
	public static void init() {
		ItemTooltipCallback.EVENT.register((stack, context, flag, lines) -> {
			WeaponDef def = JobWeapons.def(stack);
			if (def != null) {
				lines.addAll(1, build(stack, def));
				return;
			}
			ArmorPieceDef armor = ClassArmor.def(stack);
			if (armor != null) {
				lines.addAll(1, GearTooltips.build(stack, armor));
			}
		});
	}

	static String num(final double v) {
		double r = Math.round(v * 10.0) / 10.0;
		return r == Math.rint(r) ? Long.toString((long)r) : String.format(Locale.ROOT, "%.1f", r);
	}

	public static List<Component> build(final ItemStack stack, final WeaponDef def) {
		List<Component> lines = new ArrayList<>();
		Player player = Minecraft.getInstance().player;
		JobData data = player == null ? JobData.DEFAULT : JobProgression.get(player);
		boolean classOk = data.job() == def.job();
		boolean tierOk = classOk && data.tier() >= def.tier();
		boolean levelOk = data.level() >= def.level();

		MutableComponent requirement = Component.empty()
			.append(Component.translatable(def.job().nameKey()).withColor(classOk ? def.job().color() : 0xFF5555))
			.append(Component.literal(" · ").withStyle(ChatFormatting.DARK_GRAY))
			.append(Component.translatable("tooltip.minecraft_mode.weapon.tier", def.tier(), Component.translatable(def.job().tierKey(def.tier())))
				.withStyle(tierOk ? ChatFormatting.GRAY : ChatFormatting.RED))
			.append(Component.literal(" · ").withStyle(ChatFormatting.DARK_GRAY))
			.append(Component.translatable("tooltip.minecraft_mode.weapon.level", def.level()).withStyle(levelOk ? ChatFormatting.GRAY : ChatFormatting.RED));
		lines.add(requirement);
		if (!(classOk && tierOk && levelOk)) {
			lines.add(Component.translatable("tooltip.minecraft_mode.weapon.locked").withStyle(ChatFormatting.RED));
		}

		Archetype type = def.archetype();
		MutableComponent basic = Component.translatable(type.nameKey()).withStyle(ChatFormatting.GOLD)
			.append(Component.literal(" · ").withStyle(ChatFormatting.DARK_GRAY))
			.append(Component.translatable("tooltip.minecraft_mode.weapon.power", num(def.power())).withStyle(ChatFormatting.GRAY));
		if (type.isRanged()) {
			String key = type.mode() == Archetype.BasicMode.DRAW ? "tooltip.minecraft_mode.weapon.draw" : "tooltip.minecraft_mode.weapon.shot";
			basic.append(Component.literal(" · ").withStyle(ChatFormatting.DARK_GRAY))
				.append(Component.translatable(key, Component.translatable(type.shot().nameKey()), num(def.power() * type.shotMultiplier()), num(type.shotCooldown() / 20.0))
					.withStyle(ChatFormatting.GRAY));
		}
		lines.add(basic);

		Engravings engravings = JobWeapons.engravings(stack);
		boolean detail = Minecraft.getInstance().hasShiftDown();
		lines.add(Component.translatable(detail ? "tooltip.minecraft_mode.weapon.skills" : "tooltip.minecraft_mode.weapon.skills_hint").withStyle(ChatFormatting.YELLOW));
		var mods = player == null ? engravings.totals() : JobWeapons.activeTotals(player);
		for (int i = 0; i < def.skills().size(); i++) {
			Skill skill = def.skills().get(i);
			String key = JobKeys.SKILLS[i].getTranslatedKeyMessage().getString();
			int cooldown = SkillCaster.cooldown(data, skill, mods);
			lines.add(Component.literal(" [" + key + "] ").withStyle(ChatFormatting.WHITE)
				.append(Component.translatable(skill.nameKey()).withColor(skill.kind().color()))
				.append(Component.literal(" · ").withStyle(ChatFormatting.DARK_GRAY))
				.append(Component.translatable("tooltip.minecraft_mode.skill.cost", SkillCaster.manaCost(skill, mods), num(cooldown / 20.0)).withStyle(ChatFormatting.AQUA)));
			if (detail) {
				for (SkillAction action : skill.actions()) {
					lines.add(Component.literal("    - ").withStyle(ChatFormatting.DARK_GRAY).append(action.describe().copy().withStyle(ChatFormatting.GRAY)));
				}
			}
		}

		lines.add(Component.translatable("tooltip.minecraft_mode.weapon.engravings", engravings.lines().size(), Engravings.WEAPON_LINES).withStyle(ChatFormatting.LIGHT_PURPLE));
		Map<Engraving, Integer> counts = new LinkedHashMap<>();
		for (Engraving e : engravings.resolved()) {
			counts.merge(e, 1, Integer::sum);
		}
		if (counts.isEmpty()) {
			lines.add(Component.translatable("tooltip.minecraft_mode.weapon.no_engravings").withStyle(ChatFormatting.DARK_GRAY));
		}
		for (Map.Entry<Engraving, Integer> entry : counts.entrySet()) {
			Engraving e = entry.getKey();
			int n = entry.getValue();
			lines.add(engravingLine(e, n));
		}
		lines.add(Component.empty());
		return lines;
	}

	/** "• Wide Swing x2: Basic attacks also hit enemies within 3 blocks of the target". */
	public static Component engravingLine(final Engraving e, final int count) {
		EngraveStat stat = e.stat();
		float total = e.value() * count;
		if (stat.cap() > 0) {
			total = Math.min(total, stat.cap());
		}
		MutableComponent line = Component.literal(" • ").withStyle(ChatFormatting.DARK_PURPLE).append(Component.translatable(e.nameKey()).withStyle(ChatFormatting.LIGHT_PURPLE));
		if (count > 1) {
			line.append(Component.literal(" x" + count).withStyle(ChatFormatting.GOLD));
		}
		return line.append(Component.literal(": ").withStyle(ChatFormatting.DARK_GRAY))
			.append(Component.translatable(stat.key(), num(total)).withStyle(ChatFormatting.GRAY));
	}

	public static int tierColor(final int tier) {
		return JobWeaponItem.tierColor(tier);
	}

	private JobTooltips() {
	}
}
