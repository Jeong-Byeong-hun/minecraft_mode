package com.minecraftmode.city;

import com.minecraftmode.registry.ModAttachments;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Quartermaster Bram's starter kit: one set of plain (unenchanted) iron armor, an iron sword, pickaxe and axe, once per player
 * (attachment {@code ModAttachments.STARTER_KIT}, kept on death).
 */
public final class StarterKit {
	public static final List<Item> ITEMS = List.of(Items.IRON_HELMET, Items.IRON_CHESTPLATE, Items.IRON_LEGGINGS, Items.IRON_BOOTS, Items.IRON_SWORD,
		Items.IRON_PICKAXE, Items.IRON_AXE);

	public static boolean taken(final Player player) {
		return player.getAttachedOrElse(ModAttachments.STARTER_KIT, false);
	}

	/** Hands out the kit (what does not fit drops at the player's feet). Returns false when the player already had it. */
	public static boolean give(final ServerPlayer player) {
		if (taken(player)) {
			player.sendSystemMessage(Component.translatable("message.minecraft_mode.starter_kit.taken").withStyle(ChatFormatting.YELLOW));
			return false;
		}
		player.setAttached(ModAttachments.STARTER_KIT, true);
		for (Item item : ITEMS) {
			player.getInventory().placeItemBackInInventory(new ItemStack(item), net.minecraft.util.Prediction.SERVER_ONLY);
		}
		player.sendSystemMessage(Component.translatable("message.minecraft_mode.starter_kit.given").withStyle(ChatFormatting.GREEN));
		player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ARMOR_EQUIP_IRON.value(), SoundSource.PLAYERS, 1.0F, 1.0F);
		return true;
	}

	private StarterKit() {
	}
}
