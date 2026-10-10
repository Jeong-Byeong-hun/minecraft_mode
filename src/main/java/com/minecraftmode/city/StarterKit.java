package com.minecraftmode.city;

import com.minecraftmode.bag.Bags;
import com.minecraftmode.registry.ModAttachments;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Quartermaster Bram's starter kit: one set of plain (unenchanted) iron armor, an iron sword, pickaxe and axe and a shield, once
 * per player (attachment {@code ModAttachments.STARTER_KIT}, kept on death). The shield came later, so players who took the kit
 * before collect it on their next visit ({@code ModAttachments.STARTER_SHIELD}). Every piece is unbreakable and named
 * "Quartermaster's ..." ({@link #stack}). The three bags ({@link Bags#giveStarter}) come with the kit, or on the next visit for players
 * who took the kit before bags existed.
 */
public final class StarterKit {
	public static final List<Item> ITEMS = List.of(Items.IRON_HELMET, Items.IRON_CHESTPLATE, Items.IRON_LEGGINGS, Items.IRON_BOOTS, Items.IRON_SWORD,
		Items.IRON_PICKAXE, Items.IRON_AXE, Items.SHIELD);

	/** One kit piece: unbreakable (no durability bar) and named after the quartermaster. */
	public static ItemStack stack(final Item item) {
		ItemStack stack = new ItemStack(item);
		stack.set(DataComponents.UNBREAKABLE, Unit.INSTANCE);
		stack.set(DataComponents.ITEM_NAME, Component.translatable(nameKey(item)));
		return stack;
	}

	/** {@code item.minecraft_mode.starter.<vanilla item path>}, e.g. {@code item.minecraft_mode.starter.iron_helmet}. */
	public static String nameKey(final Item item) {
		return "item.minecraft_mode.starter." + BuiltInRegistries.ITEM.getKey(item).getPath();
	}

	public static boolean taken(final Player player) {
		return player.getAttachedOrElse(ModAttachments.STARTER_KIT, false);
	}

	public static boolean shieldTaken(final Player player) {
		return player.getAttachedOrElse(ModAttachments.STARTER_SHIELD, false);
	}

	/**
	 * Hands out the kit (what does not fit drops at the player's feet; the shield goes to an empty off hand). Returns false when the
	 * player already had everything.
	 */
	public static boolean give(final ServerPlayer player) {
		boolean bags = Bags.giveStarter(player);
		if (taken(player) && shieldTaken(player)) {
			if (!bags) {
				player.sendSystemMessage(Component.translatable("message.minecraft_mode.starter_kit.taken").withStyle(ChatFormatting.YELLOW));
			}
			return bags;
		}
		boolean shieldOnly = taken(player);
		player.setAttached(ModAttachments.STARTER_KIT, true);
		player.setAttached(ModAttachments.STARTER_SHIELD, true);
		for (Item item : shieldOnly ? List.of(Items.SHIELD) : ITEMS) {
			if (item == Items.SHIELD && player.getOffhandItem().isEmpty()) {
				player.setItemSlot(EquipmentSlot.OFFHAND, stack(item));
			} else {
				player.getInventory().placeItemBackInInventory(stack(item), net.minecraft.util.Prediction.SERVER_ONLY);
			}
		}
		player.sendSystemMessage(Component.translatable(shieldOnly ? "message.minecraft_mode.starter_kit.shield" : "message.minecraft_mode.starter_kit.given")
			.withStyle(ChatFormatting.GREEN));
		player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ARMOR_EQUIP_IRON.value(), SoundSource.PLAYERS, 1.0F, 1.0F);
		return true;
	}

	private StarterKit() {
	}
}
