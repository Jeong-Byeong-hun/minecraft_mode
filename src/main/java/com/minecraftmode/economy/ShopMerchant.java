package com.minecraftmode.economy;

import com.minecraftmode.block.ShopBlock;
import java.util.IdentityHashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Server-side merchant for a shop block. One instance per opened screen; offers never run out but
 * their prices follow the shared {@link MarketData}. Not an entity, so {@code MerchantMenuMixin}
 * routes the trade sound through {@link #playTradeSound()}.
 */
public class ShopMerchant implements Merchant {
	private static final double MAX_DISTANCE_SQR = 8.0 * 8.0;

	private final Level level;
	private final BlockPos pos;
	private final ShopType type;
	private final MarketData market;
	private final Map<MerchantOffer, ShopOffers.Trade> trades = new IdentityHashMap<>();
	private @Nullable Player tradingPlayer;
	private MerchantOffers offers = new MerchantOffers();

	public ShopMerchant(final Player player, final Level level, final BlockPos pos, final ShopType type) {
		this.level = level;
		this.pos = pos.immutable();
		this.type = type;
		this.tradingPlayer = player;
		this.market = MarketData.get(level.getServer());
		for (ShopOffers.Trade trade : ShopOffers.trades(type)) {
			MerchantOffer offer = trade.toOffer();
			offer.setSpecialPriceDiff(MarketData.priceIncrease(trade.costCount(), this.market.pressure(this.marketKey(trade), level.getGameTime())));
			this.offers.add(offer);
			this.trades.put(offer, trade);
		}
	}

	public ShopType type() {
		return this.type;
	}

	private String marketKey(final ShopOffers.Trade trade) {
		return this.type.id() + ":" + trade.key();
	}

	@Override
	public void setTradingPlayer(final @Nullable Player player) {
		this.tradingPlayer = player;
	}

	@Override
	public @Nullable Player getTradingPlayer() {
		return this.tradingPlayer;
	}

	@Override
	public MerchantOffers getOffers() {
		return this.offers;
	}

	@Override
	public void overrideOffers(final MerchantOffers offers) {
		this.offers = offers;
	}

	@Override
	public void notifyTrade(final MerchantOffer offer) {
		ShopOffers.Trade trade = this.trades.get(offer);
		if (trade != null) {
			String key = this.marketKey(trade);
			long time = this.level.getGameTime();
			this.market.recordTrade(key, time);
			offer.setSpecialPriceDiff(MarketData.priceIncrease(trade.costCount(), this.market.pressure(key, time)));
			// Show the new price right away
			if (this.tradingPlayer instanceof ServerPlayer player && player.containerMenu instanceof MerchantMenu menu) {
				player.sendMerchantOffers(menu.containerId, this.offers, 1, 0, false, false);
			}
		}
		this.playTradeSound();
	}

	@Override
	public void notifyTradeUpdated(final ItemStack itemStack) {
	}

	@Override
	public int getVillagerXp() {
		return 0;
	}

	@Override
	public void overrideXp(final int xp) {
	}

	@Override
	public boolean showProgressBar() {
		return false;
	}

	@Override
	public SoundEvent getNotifyTradeSound() {
		return SoundEvents.EXPERIENCE_ORB_PICKUP;
	}

	@Override
	public boolean isClientSide() {
		return false;
	}

	@Override
	public boolean stillValid(final Player player) {
		return this.tradingPlayer == player
			&& this.level.getBlockState(this.pos).getBlock() instanceof ShopBlock
			&& player.distanceToSqr(Vec3.atCenterOf(this.pos)) <= MAX_DISTANCE_SQR;
	}

	public void playTradeSound() {
		this.level.playSound(null, this.pos, this.getNotifyTradeSound(), SoundSource.BLOCKS, 0.6F, 1.2F);
	}
}
