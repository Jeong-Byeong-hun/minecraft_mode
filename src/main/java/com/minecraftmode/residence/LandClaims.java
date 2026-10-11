package com.minecraftmode.residence;

import com.minecraftmode.MinecraftMode;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import org.jspecify.annotations.Nullable;

/**
 * Land in the residence dimension (server SavedData). A claim is a 16x16x16 box: one chunk wide, from {@code minY} up 16 blocks.
 * Each owner keeps one list of trusted players for all of their land. Only one owner can hold land in a chunk column (so nobody
 * buys the air over someone's house); an owner extends upwards or downwards with more deeds. Names are remembered so the HUD and
 * the land screen can show owners and friends who are offline.
 */
public final class LandClaims extends SavedData {
	public static final int SIZE = 16;
	public static final int MAX_CLAIMS = 8;
	public static final int MAX_TRUSTED = 12;

	public record Claim(long chunk, int minY, UUID owner) {
		static final Codec<Claim> CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.LONG.fieldOf("chunk").forGetter(Claim::chunk),
			Codec.INT.fieldOf("min_y").forGetter(Claim::minY),
			UUIDUtil.CODEC.fieldOf("owner").forGetter(Claim::owner)
		).apply(i, Claim::new));

		public int chunkX() {
			return ChunkPos.getX(this.chunk);
		}

		public int chunkZ() {
			return ChunkPos.getZ(this.chunk);
		}

		public int maxY() {
			return this.minY + SIZE - 1;
		}

		public boolean contains(final BlockPos pos) {
			return ChunkPos.pack(pos) == this.chunk && pos.getY() >= this.minY && pos.getY() <= this.maxY();
		}
	}

	/** Outcome of {@link #claim}; anything but {@link #CLAIMED} is a refusal with its own message. */
	public enum Result {
		CLAIMED, SPAWN, TAKEN, OVERLAP, LIMIT, HEIGHT;

		public String key() {
			return "message.minecraft_mode.land." + this.name().toLowerCase(Locale.ROOT);
		}
	}

	private static final Codec<LandClaims> CODEC = RecordCodecBuilder.create(i -> i.group(
		Claim.CODEC.listOf().optionalFieldOf("claims", List.of()).forGetter(d -> d.claims),
		Codec.unboundedMap(UUIDUtil.STRING_CODEC, UUIDUtil.CODEC.listOf()).optionalFieldOf("trusted", Map.of()).forGetter(d -> d.trusted),
		Codec.unboundedMap(UUIDUtil.STRING_CODEC, Codec.STRING).optionalFieldOf("names", Map.of()).forGetter(d -> d.names)
	).apply(i, LandClaims::new));

	public static final SavedDataType<LandClaims> TYPE = new SavedDataType<>(MinecraftMode.id("land_claims"), LandClaims::new, CODEC, null);

	private final List<Claim> claims = new ArrayList<>();
	private final Map<UUID, List<UUID>> trusted = new HashMap<>();
	private final Map<UUID, String> names = new HashMap<>();
	private final Map<Long, List<Claim>> byChunk = new HashMap<>();
	/** Bumped on every change to claims or trust lists, so players in the residence get the new borders ({@code ResidenceRules}). */
	private int version;

	public LandClaims() {
	}

	private LandClaims(final List<Claim> claims, final Map<UUID, List<UUID>> trusted, final Map<UUID, String> names) {
		claims.forEach(this::add);
		trusted.forEach((owner, list) -> this.trusted.put(owner, new ArrayList<>(list)));
		this.names.putAll(names);
	}

	public static LandClaims get(final MinecraftServer server) {
		return server.getDataStorage().computeIfAbsent(TYPE);
	}

	private void add(final Claim claim) {
		this.claims.add(claim);
		this.byChunk.computeIfAbsent(claim.chunk(), k -> new ArrayList<>()).add(claim);
	}

	public @Nullable Claim at(final BlockPos pos) {
		for (Claim claim : this.byChunk.getOrDefault(ChunkPos.pack(pos), List.of())) {
			if (claim.contains(pos)) {
				return claim;
			}
		}
		return null;
	}

	public List<Claim> all() {
		return List.copyOf(this.claims);
	}

	public int version() {
		return this.version;
	}

	private void changed() {
		this.version++;
		this.setDirty();
	}

	public List<Claim> owned(final UUID owner) {
		return this.claims.stream().filter(c -> c.owner().equals(owner)).toList();
	}

	public boolean allowed(final UUID player, final Claim claim) {
		return claim.owner().equals(player) || this.trusted(claim.owner()).contains(player);
	}

	/** Claims the box of the chunk at {@code feet}, from the block under the feet up 16 blocks, for {@code owner}. */
	public Result claim(final UUID owner, final String name, final BlockPos feet, final LevelHeightAccessor heights) {
		int minY = feet.getY() - 1;
		if (ResidenceDimension.isSpawn(feet)) {
			return Result.SPAWN;
		}
		if (minY < heights.getMinY() || minY + SIZE - 1 > heights.getMaxY()) {
			return Result.HEIGHT;
		}
		long chunk = ChunkPos.pack(feet);
		for (Claim other : this.byChunk.getOrDefault(chunk, List.of())) {
			if (!other.owner().equals(owner)) {
				return Result.TAKEN;
			}
			if (Math.abs(other.minY() - minY) < SIZE) {
				return Result.OVERLAP;
			}
		}
		if (this.owned(owner).size() >= MAX_CLAIMS) {
			return Result.LIMIT;
		}
		this.add(new Claim(chunk, minY, owner));
		this.names.put(owner, name);
		this.changed();
		return Result.CLAIMED;
	}

	public boolean abandon(final UUID owner, final long chunk, final int minY) {
		Claim claim = new Claim(chunk, minY, owner);
		if (!this.claims.remove(claim)) {
			return false;
		}
		List<Claim> column = this.byChunk.get(chunk);
		column.remove(claim);
		if (column.isEmpty()) {
			this.byChunk.remove(chunk);
		}
		this.changed();
		return true;
	}

	public List<UUID> trusted(final UUID owner) {
		return List.copyOf(this.trusted.getOrDefault(owner, List.of()));
	}

	/** Adds {@code friend} to the owner's list; false when already there or the list is full. */
	public boolean trust(final UUID owner, final UUID friend, final String friendName) {
		List<UUID> list = this.trusted.computeIfAbsent(owner, k -> new ArrayList<>());
		if (list.contains(friend) || list.size() >= MAX_TRUSTED) {
			return false;
		}
		list.add(friend);
		this.names.put(friend, friendName);
		this.changed();
		return true;
	}

	public boolean untrust(final UUID owner, final UUID friend) {
		List<UUID> list = this.trusted.get(owner);
		if (list == null || !list.remove(friend)) {
			return false;
		}
		if (list.isEmpty()) {
			this.trusted.remove(owner);
		}
		this.changed();
		return true;
	}

	public String name(final UUID player) {
		return this.names.getOrDefault(player, player.toString().substring(0, 8));
	}

	/** Keeps a known player's name current (called on join; strangers are not recorded). */
	public void rename(final UUID player, final String name) {
		String old = this.names.get(player);
		if (old != null && !old.equals(name)) {
			this.names.put(player, name);
			this.setDirty();
		}
	}
}
