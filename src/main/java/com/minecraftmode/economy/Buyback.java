package com.minecraftmode.economy;

import com.minecraftmode.bag.Bags;
import com.minecraftmode.loot.Coins;
import com.minecraftmode.mixin.MerchantMenuAccessor;
import com.minecraftmode.registry.ModAttachments;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;

/**
 * Buying back what was sold by mistake: the last {@link #LIMIT} things a player sold to any shop (newest first, the exact stacks
 * with their engravings and enhancement) stay in {@link ModAttachments#SOLD}, and can be bought back from any shop for
 * {@link #MARKUP} times what the shop paid. Selling the same thing again right away adds to the newest line. Buybacks never touch
 * market pressure.
 */
public final class Buyback {
	public static final int LIMIT = 10;
	public static final double MARKUP = 1.5;

	/** One sold stack and the copper the shop paid for it. */
	public record Entry(ItemStack stack, int paid) {
		public static final Codec<Entry> CODEC = RecordCodecBuilder.create(i -> i.group(
			ItemStack.CODEC.fieldOf("stack").forGetter(Entry::stack),
			Codec.INT.fieldOf("paid").forGetter(Entry::paid)
		).apply(i, Entry::new));
		public static final StreamCodec<RegistryFriendlyByteBuf, Entry> STREAM_CODEC = StreamCodec.composite(
			ItemStack.STREAM_CODEC, Entry::stack,
			ByteBufCodecs.VAR_INT, Entry::paid,
			Entry::new);

		/** What buying it back costs. */
		public int price() {
			return Math.max(1, (int) Math.ceil(this.paid * MARKUP));
		}
	}

	public static List<Entry> list(final Player player) {
		return player.getAttachedOrElse(ModAttachments.SOLD, List.of());
	}

	/**
	 * Called before a shop trade is taken: when the trade pays coins for goods, the goods in the payment slots ({@code buyA}/{@code buyB},
	 * whichever pair satisfies the offer) are remembered.
	 */
	public static void beforeTrade(final ServerPlayer player, final MerchantOffer offer, final ItemStack buyA, final ItemStack buyB) {
		ItemStack result = offer.getResult();
		int each = Wallet.value(result.getItem());
		if (each <= 0 || Wallet.value(offer.getCostA().getItem()) > 0) {
			return;
		}
		ItemStack goods = offer.satisfiedBy(buyA, buyB) ? buyA : offer.satisfiedBy(buyB, buyA) ? buyB : ItemStack.EMPTY;
		if (goods.isEmpty()) {
			return;
		}
		record(player, goods.copyWithCount(offer.getCostA().getCount()), each * result.getCount());
	}

	static void record(final ServerPlayer player, final ItemStack sold, final int paid) {
		List<Entry> entries = new ArrayList<>(list(player));
		Entry newest = entries.isEmpty() ? null : entries.getFirst();
		if (newest != null && ItemStack.isSameItemSameComponents(newest.stack(), sold) && newest.stack().getCount() + sold.getCount() <= sold.getMaxStackSize()) {
			entries.set(0, new Entry(newest.stack().copyWithCount(newest.stack().getCount() + sold.getCount()), newest.paid() + paid));
		} else {
			entries.addFirst(new Entry(sold, paid));
		}
		while (entries.size() > LIMIT) {
			entries.removeLast();
		}
		player.setAttached(ModAttachments.SOLD, List.copyOf(entries));
	}

	/** Buys back line {@code index}; only while a shop screen is open. */
	public static void buy(final ServerPlayer player, final int index) {
		if (!(player.containerMenu instanceof MerchantMenu menu) || !(((MerchantMenuAccessor) menu).minecraftMode$trader() instanceof ShopMerchant shop)
			|| !shop.stillValid(player)) {
			return;
		}
		List<Entry> entries = new ArrayList<>(list(player));
		if (index < 0 || index >= entries.size()) {
			return;
		}
		Entry entry = entries.get(index);
		if (!Bags.fits(player, entry.stack())) {
			player.sendSystemMessage(Component.translatable("message.minecraft_mode.buyback.full").withStyle(ChatFormatting.RED));
			return;
		}
		Wallet.deposit(player);
		if (!Coins.take(player, entry.price())) {
			player.sendSystemMessage(Component.translatable("message.minecraft_mode.buyback.short", Coins.format(entry.price())).withStyle(ChatFormatting.RED));
			return;
		}
		entries.remove(index);
		player.setAttached(ModAttachments.SOLD, List.copyOf(entries));
		Bags.give(player, entry.stack());
		player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.6F, 0.9F);
	}

	private Buyback() {
	}
}
