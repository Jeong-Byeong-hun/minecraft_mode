package com.minecraftmode.dungeon;

import com.minecraftmode.registry.ModDataComponents;
import com.minecraftmode.registry.ModItems;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/** A dungeon keystone: named after its dungeon and level; the dungeon warden starts its run. */
public class KeystoneItem extends Item {
	public KeystoneItem(final Properties properties) {
		super(properties);
	}

	public static ItemStack of(final Keystone keystone) {
		ItemStack stack = new ItemStack(ModItems.DUNGEON_KEYSTONE);
		stack.set(ModDataComponents.KEYSTONE, keystone);
		return stack;
	}

	@Override
	public Component getName(final ItemStack stack) {
		Keystone keystone = stack.get(ModDataComponents.KEYSTONE);
		DungeonDef def = keystone == null ? null : Dungeons.def(keystone.dungeon());
		if (def == null) {
			return super.getName(stack);
		}
		return Component.translatable("item.minecraft_mode.dungeon_keystone.named", Component.translatable(def.nameKey()), keystone.level());
	}

	@Override
	@SuppressWarnings("deprecation")
	public void appendHoverText(final ItemStack stack, final TooltipContext context, final TooltipDisplay display, final Consumer<Component> lines, final TooltipFlag flag) {
		Keystone keystone = stack.get(ModDataComponents.KEYSTONE);
		DungeonDef def = keystone == null ? null : Dungeons.def(keystone.dungeon());
		if (def == null) {
			return;
		}
		lines.accept(Component.translatable("tooltip.minecraft_mode.keystone.level", def.minLevel()).withStyle(ChatFormatting.GRAY));
		lines.accept(Component.translatable("tooltip.minecraft_mode.keystone.scaling", Math.round(Dungeons.healthScale(keystone.level()) * 100 - 100),
			Math.round(Dungeons.damageScale(keystone.level()) * 100 - 100)).withStyle(ChatFormatting.RED));
		for (DungeonAffix affix : DungeonAffix.values()) {
			if (affix != DungeonAffix.TYRANNICAL && affix != DungeonAffix.RAGING && keystone.level() >= affix.level()) {
				lines.accept(Component.translatable("tooltip.minecraft_mode.keystone.affix_" + affix.level()).withStyle(ChatFormatting.LIGHT_PURPLE));
			}
		}
		lines.accept(Component.translatable("tooltip.minecraft_mode.keystone.time", def.timeLimit() / 60).withStyle(ChatFormatting.AQUA));
		lines.accept(Component.translatable("tooltip.minecraft_mode.keystone.use").withStyle(ChatFormatting.DARK_GRAY));
	}
}
