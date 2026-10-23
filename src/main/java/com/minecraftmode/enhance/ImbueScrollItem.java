package com.minecraftmode.enhance;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/**
 * An imbue scroll (0.5.49): puts {@link ImbueSlot#stat} on one piece of its slot for good. Tier lairs drop them; Artisan Brokk's bench applies
 * them ({@code EnhanceMenu#BUTTON_IMBUE}). A greater scroll may replace a normal line; nothing else replaces a line.
 */
public class ImbueScrollItem extends Item {
	private final ImbueSlot slot;
	private final int grade;

	public ImbueScrollItem(final Properties properties, final ImbueSlot slot, final int grade) {
		super(properties);
		this.slot = slot;
		this.grade = grade;
	}

	public ImbueSlot slot() {
		return this.slot;
	}

	public int grade() {
		return this.grade;
	}

	@Override
	public void appendHoverText(final ItemStack stack, final TooltipContext context, final TooltipDisplay display, final Consumer<Component> lines, final TooltipFlag flag) {
		Imbue imbue = new Imbue(this.slot, this.grade);
		lines.accept(Component.translatable("item.minecraft_mode.imbue_scroll.line", Component.translatable(this.slot.nameKey()),
			Component.translatable(this.slot.stat().key(), number(imbue.line().value()))).withStyle(ChatFormatting.AQUA));
		lines.accept(Component.translatable(this.grade >= Imbue.GREATER ? "item.minecraft_mode.imbue_scroll.greater" : "item.minecraft_mode.imbue_scroll.normal")
			.withStyle(ChatFormatting.GRAY));
		lines.accept(Component.translatable("item.minecraft_mode.imbue_scroll.use").withStyle(ChatFormatting.DARK_GRAY));
	}

	/** A stat value as the stat lines show it: whole numbers without a decimal point. */
	public static String number(final float value) {
		return value == Math.rint(value) ? String.valueOf((int)value) : String.valueOf(Math.round(value * 10.0F) / 10.0F);
	}
}
