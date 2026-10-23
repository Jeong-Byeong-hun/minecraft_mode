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
 * Land in the residence dimension (server SavedData). A claim is a whole chunk column (0.5.46): one chunk wide, from the bottom of the
 * world to the build limit, so a deed is enough for a tower or a cellar; {@code minY} (under the claimer's feet) only marks the floor the
 * borders are drawn on. Until 0.5.46 a claim was a 16x16x16 box from {@code minY} and taller houses needed a deed per 16 blocks:
 * several claims of one owner in one column, which loading folds into one, handing the extra deeds back ({@link #refunds}).
 * Each owner keeps one list of trusted players for all of their land. Names are remembered so the HUD and the land screen can show
 * owners and friends who are offline.
 */
public final class LandClaims extends SavedData {
	/** A claim's width (and length) in blocks: one chunk. */
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

		/** The whole column, any height. */
		public boolean contains(final BlockPos pos) {
			return ChunkPos.pack(pos) == this.chunk;
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
		Codec.unboundedMap(UUIDUtil.STRING_CODEC, Codec.STRING).optionalFieldOf("names", Map.of()).forGetter(d -> d.names),
		Codec.unboundedMap(UUIDUtil.STRING_CODEC, Codec.INT).optionalFieldOf("refunds", Map.of()).forGetter(d -> d.refunds)
	).apply(i, LandClaims::new));

	public static final SavedDataType<LandClaims> TYPE = new SavedDataType<>(MinecraftMode.id("land_claims"), LandClaims::new, CODEC, null);

	private final List<Claim> claims = new ArrayList<>();
	private final Map<UUID, List<UUID>> trusted = new HashMap<>();
	private final Map<UUID, String> names = new HashMap<>();
	private final Map<Long, List<Claim>> byChunk = new HashMap<>();
	/** Deeds owed to owners whose stacked claims of before 0.5.46 were folded into one column ({@link #takeRefunds}). */
	private final Map<UUID, Integer> refunds = new HashMap<>();
	/** Bumped on every change to claims or trust lists, so players in the residence get the new borders ({@code ResidenceRules}). */
	private int version;

	public LandClaims() {
	}

	private LandClaims(final List<Claim> claims, final Map<UUID, List<UUID>> trusted, final Map<UUID, String> names, final Map<UUID, Integer> refunds) {
		this.refunds.putAll(refunds);
		for (Claim claim : claims) {
			// a column holds one claim now: the first of an owner's stacked boxes stays, the others become deeds to hand back
			if (this.byChunk.containsKey(claim.chunk())) {
				this.refunds.merge(claim.owner(), 1, Integer::sum);
				this.setDirty();
			} else {
				this.add(claim);
			}
		}
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

	/** Claims the column of the chunk at {@code feet} for {@code owner}; its borders are drawn from the block under the feet. */
	public Result claim(final UUID owner, final String name, final BlockPos feet, final LevelHeightAccessor heights) {
		int minY = feet.getY() - 1;
		if (ResidenceDimension.isPublic(feet)) {
			return Result.SPAWN;
		}
		if (minY < heights.getMinY() || minY > heights.getMaxY()) {
			return Result.HEIGHT;
		}
		long chunk = ChunkPos.pack(feet);
		for (Claim other : this.byChunk.getOrDefault(chunk, List.of())) {
			return other.owner().equals(owner) ? Result.OVERLAP : Result.TAKEN;
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

	/** The deeds owed to {@code owner} from folded claims (see the class notes), now taken off the books. */
	public int takeRefunds(final UUID owner) {
		Integer owed = this.refunds.remove(owner);
		if (owed != null) {
			this.setDirty();
		}
		return owed == null ? 0 : owed;
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
