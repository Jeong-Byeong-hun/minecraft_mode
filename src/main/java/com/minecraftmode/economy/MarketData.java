package com.minecraftmode.economy;

import com.minecraftmode.MinecraftMode;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * World-wide market pressure per trade. Every completed trade adds 1 pressure (max 20); pressure
 * fades by 1 per minute. Each point raises that trade's cost by 10% of its base cost: selling a lot
 * means more goods per coin, buying a lot means more coins per item.
 */
public class MarketData extends SavedData {
	public static final long DECAY_INTERVAL_TICKS = 1200;
	public static final float PRICE_STEP = 0.1F;
	public static final int MAX_PRESSURE = 20;

	private static final Codec<MarketData> CODEC = RecordCodecBuilder.create(
		instance -> instance.group(
				Codec.unboundedMap(Codec.STRING, Codec.INT).fieldOf("pressure").forGetter(data -> data.pressure),
				Codec.LONG.fieldOf("last_decay").forGetter(data -> data.lastDecay)
			)
			.apply(instance, MarketData::new)
	);

	// Fabric API handles the null DataFixTypes (mod data has no vanilla fixers).
	public static final SavedDataType<MarketData> TYPE = new SavedDataType<>(MinecraftMode.id("market"), MarketData::new, CODEC, null);

	private final Map<String, Integer> pressure;
	private long lastDecay;

	public MarketData() {
		this(Map.of(), -1);
	}

	private MarketData(final Map<String, Integer> pressure, final long lastDecay) {
		this.pressure = new HashMap<>(pressure);
		this.lastDecay = lastDecay;
	}

	public static MarketData get(final MinecraftServer server) {
		return server.getDataStorage().computeIfAbsent(TYPE);
	}

	public int pressure(final String key, final long gameTime) {
		this.decay(gameTime);
		return this.pressure.getOrDefault(key, 0);
	}

	public void recordTrade(final String key, final long gameTime) {
		this.decay(gameTime);
		this.pressure.merge(key, 1, (a, b) -> Math.min(MAX_PRESSURE, a + b));
		this.setDirty();
	}

	/** Extra cost on top of the base cost for the given pressure. */
	public static int priceIncrease(final int baseCost, final int pressure) {
		return Mth.floor(baseCost * PRICE_STEP * pressure);
	}

	private void decay(final long gameTime) {
		if (this.lastDecay < 0 || gameTime < this.lastDecay) {
			this.lastDecay = gameTime;
			return;
		}
		long steps = (gameTime - this.lastDecay) / DECAY_INTERVAL_TICKS;
		if (steps <= 0) {
			return;
		}
		this.pressure.replaceAll((key, value) -> (int) Math.max(0, value - steps));
		this.pressure.values().removeIf(value -> value == 0);
		this.lastDecay += steps * DECAY_INTERVAL_TICKS;
		this.setDirty();
	}
}
