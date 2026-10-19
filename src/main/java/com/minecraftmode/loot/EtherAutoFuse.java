package com.minecraftmode.loot;

import com.minecraftmode.bag.Bags;
import com.minecraftmode.character.Characters;
import com.minecraftmode.job.gear.ItemLevels;
import com.minecraftmode.registry.ModAttachments;
import com.minecraftmode.registry.ModItems;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Automatic ether fusing (0.5.36): with it on, every second the Evolution Ether below the chosen grade is fused
 * {@link EvolutionEtherItem#FUSE} to one, grade by grade, so ether of lower grades climbs up to the chosen one by itself (the inventory
 * and the carried bags both count). The setting is the attachment {@link ModAttachments#ETHER_AUTO}: the chosen grade, negative while
 * switched off (it keeps the grade), 0 when never set.
 */
public final class EtherAutoFuse {
	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTickCount() % 20 == 10) {
				for (ServerPlayer player : server.getPlayerList().getPlayers()) {
					if (target(player) > 0 && player.isAlive() && !player.isSpectator() && !Characters.choosing(player)) {
						fuse(player);
					}
				}
			}
		});
	}

	public static int setting(final Player player) {
		return player.getAttachedOrElse(ModAttachments.ETHER_AUTO, 0);
	}

	/** The grade ether is fused up to, or 0 when switched off. */
	public static int target(final Player player) {
		return Math.max(0, setting(player));
	}

	/** The grade shown for a setting: the chosen one, or the player's bracket when never set. */
	public static int shownGrade(final int setting, final int level) {
		return setting == 0 ? Math.max(ItemLevels.MIN_BRACKET + 10, ItemLevels.bracket(level)) : Math.abs(setting);
	}

	/** {@code on} with {@code grade} (clamped to 20..100 in steps of 10). Fuses at once when switched on. */
	public static void set(final ServerPlayer player, final boolean on, final int grade) {
		int g = Math.max(ItemLevels.MIN_BRACKET + 10, ItemLevels.bracket(grade));
		player.setAttached(ModAttachments.ETHER_AUTO, on ? g : -g);
		player.sendOverlayMessage(Component.translatable(on ? "message.minecraft_mode.ether.auto_on" : "message.minecraft_mode.ether.auto_off", g)
			.withStyle(on ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.GRAY));
		if (on) {
			fuse(player);
		}
	}

	/** Fuses everything below {@link #target}; returns how many ether were made. */
	public static int fuse(final ServerPlayer player) {
		int target = target(player);
		int made = 0;
		int top = 0;
		for (int grade = ItemLevels.MIN_BRACKET; grade < target && grade < ItemLevels.MAX_BRACKET; grade += 10) {
			int fused = count(player, grade) / EvolutionEtherItem.FUSE;
			if (fused > 0) {
				take(player, grade, fused * EvolutionEtherItem.FUSE);
				// never on the floor: a full inventory sends it to the market mailbox (instances and jump courses sweep the floor)
				Bags.giveOrMail(player, EvolutionEtherItem.of(grade + 10, fused));
				made += fused;
				top = grade + 10;
			}
		}
		if (made > 0) {
			player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 0.6F, 1.2F);
			player.sendOverlayMessage(Component.translatable("message.minecraft_mode.ether.auto_fused", made, top).withStyle(ChatFormatting.LIGHT_PURPLE));
		}
		return made;
	}

	/** Ether of {@code grade} in the inventory and the carried bags. */
	public static int count(final ServerPlayer player, final int grade) {
		Inventory inventory = player.getInventory();
		int total = 0;
		for (int i = 0; i < inventory.getContainerSize(); i++) {
			ItemStack stack = inventory.getItem(i);
			if (stack.is(ModItems.EVOLUTION_ETHER) && EvolutionEtherItem.grade(stack) == grade) {
				total += stack.getCount();
			}
		}
		return total + Bags.countInBags(player, EvolutionEtherItem.of(grade, 1));
	}

	private static void take(final ServerPlayer player, final int grade, final int amount) {
		Inventory inventory = player.getInventory();
		int left = amount;
		for (int i = 0; i < inventory.getContainerSize() && left > 0; i++) {
			ItemStack stack = inventory.getItem(i);
			if (stack.is(ModItems.EVOLUTION_ETHER) && EvolutionEtherItem.grade(stack) == grade) {
				int n = Math.min(left, stack.getCount());
				stack.shrink(n);
				left -= n;
			}
		}
		if (left > 0) {
			Bags.takeFromBags(player, EvolutionEtherItem.of(grade, 1), left);
		}
	}

	private EtherAutoFuse() {
	}
}
