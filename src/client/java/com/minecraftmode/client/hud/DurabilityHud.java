package com.minecraftmode.client.hud;

import com.minecraftmode.MinecraftMode;
import com.minecraftmode.client.ClientSettings;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.ChatFormatting;
import net.minecraft.client.AttackIndicatorStatus;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;

/**
 * Durability as numbers. The bar under an item is 13 pixels for up to 2,031 uses and an Excavation II swing takes 25, so a pickaxe could
 * look nearly whole when it was nearly gone: right of the hotbar the held item shows "left / max" once it is worn, coloured like the
 * bar ({@code /durability false} hides it, {@link ClientSettings#durability}), and worn items get vanilla's "Durability: X / Y"
 * tooltip line without needing advanced tooltips (F3+H).
 */
public final class DurabilityHud {
	public static void init() {
		HudElementRegistry.attachElementAfter(VanillaHudElements.HOTBAR, MinecraftMode.id("durability"), DurabilityHud::extract);
		// tooltips are also built off the render thread with no player (recipe book search): only the stack is read here
		ItemTooltipCallback.EVENT.register((stack, context, flag, lines) -> {
			if (!flag.isAdvanced() && stack.isDamaged()) {
				lines.add(Component.translatable("item.durability", left(stack), stack.getMaxDamage()).withStyle(ChatFormatting.GRAY));
			}
		});
	}

	private static int left(final ItemStack stack) {
		return stack.getMaxDamage() - stack.getDamageValue();
	}

	private static void extract(final GuiGraphicsExtractor g, final DeltaTracker delta) {
		Minecraft minecraft = Minecraft.getInstance();
		LocalPlayer player = minecraft.player;
		if (!ClientSettings.durability || player == null || player.isSpectator()) {
			return;
		}
		ItemStack held = player.getMainHandItem();
		if (!held.isDamaged()) {
			return;
		}
		// right of the hotbar, past the off-hand slot or the attack indicator when they are drawn there
		int x = g.guiWidth() / 2 + 91 + 6;
		boolean rightHanded = player.getMainArm() == HumanoidArm.RIGHT;
		if (!rightHanded && !player.getOffhandItem().isEmpty()) {
			x += 29;
		}
		if (rightHanded && minecraft.options.attackIndicator().get() == AttackIndicatorStatus.HOTBAR) {
			x += 22;
		}
		g.text(minecraft.font, left(held) + " / " + held.getMaxDamage(), x, g.guiHeight() - 15, 0xFF000000 | held.getBarColor(), true);
	}

	private DurabilityHud() {
	}
}
