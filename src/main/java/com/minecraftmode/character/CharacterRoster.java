package com.minecraftmode.character;

import com.minecraftmode.MinecraftMode;
import com.minecraftmode.job.JobClass;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import org.jspecify.annotations.Nullable;

/**
 * Character slots per account (server SavedData, {@link Characters}). The character being played lives in the vanilla player data
 * as before; the others wait here as saved player data. Field names are save data — keep them stable.
 */
public final class CharacterRoster extends SavedData {
	/**
	 * A character that is not being played: its saved player data and what the select screen shows of it ({@code gear}: what it
	 * wears and holds, in {@link Characters#GEAR_SLOTS} order).
	 */
	public record Stored(int slot, CompoundTag data, JobClass job, int tier, int level, List<ItemStack> gear) {
		static final Codec<Stored> CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.INT.fieldOf("slot").forGetter(Stored::slot),
			CompoundTag.CODEC.fieldOf("data").forGetter(Stored::data),
			JobClass.CODEC.optionalFieldOf("job", JobClass.NONE).forGetter(Stored::job),
			Codec.INT.optionalFieldOf("tier", 0).forGetter(Stored::tier),
			Codec.INT.optionalFieldOf("level", 1).forGetter(Stored::level),
			ItemStack.OPTIONAL_CODEC.listOf().optionalFieldOf("gear", List.of()).forGetter(Stored::gear)
		).apply(i, Stored::new));
	}

	/**
	 * Where a player was before the lobby ({@link Lobby}), and what they rode there ({@code vehicle}, absent when on foot): it stays
	 * behind and takes them back on when they come back.
	 */
	public record Origin(String dimension, double x, double y, double z, float yRot, float xRot, Optional<UUID> vehicle) {
		static final Codec<Origin> CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.STRING.fieldOf("dimension").forGetter(Origin::dimension),
			Codec.DOUBLE.fieldOf("x").forGetter(Origin::x),
			Codec.DOUBLE.fieldOf("y").forGetter(Origin::y),
			Codec.DOUBLE.fieldOf("z").forGetter(Origin::z),
			Codec.FLOAT.optionalFieldOf("y_rot", 0.0F).forGetter(Origin::yRot),
			Codec.FLOAT.optionalFieldOf("x_rot", 0.0F).forGetter(Origin::xRot),
			UUIDUtil.CODEC.optionalFieldOf("vehicle").forGetter(Origin::vehicle)
		).apply(i, Origin::new));

		static Origin of(final ServerPlayer player) {
			Entity vehicle = player.getRootVehicle();
			return new Origin(player.level().dimension().identifier().toString(), player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot(),
				vehicle == player ? Optional.empty() : Optional.of(vehicle.getUUID()));
		}

		/** The origin's level, or null when that dimension is gone. */
		@Nullable ServerLevel level(final MinecraftServer server) {
			return CharacterRoster.level(server, this.dimension);
		}
	}

	/**
	 * One account: the slot being played, how many slots it owns, the characters waiting in the others and, while the player is in
	 * the lobby, where they came from.
	 */
	public record Account(int active, int slots, List<Stored> stored, @Nullable Origin origin) {
		public static final Account NEW = new Account(0, Characters.BASE_SLOTS, List.of(), null);
		static final Codec<Account> CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.INT.optionalFieldOf("active", 0).forGetter(Account::active),
			Codec.INT.optionalFieldOf("slots", Characters.BASE_SLOTS).forGetter(Account::slots),
			Stored.CODEC.listOf().optionalFieldOf("stored", List.of()).forGetter(Account::stored),
			Origin.CODEC.optionalFieldOf("origin").forGetter(a -> Optional.ofNullable(a.origin()))
		).apply(i, (active, slots, stored, origin) -> new Account(active, Math.max(slots, Characters.BASE_SLOTS), stored, origin.orElse(null))));

		public @Nullable Stored at(final int slot) {
			return this.stored.stream().filter(s -> s.slot() == slot).findFirst().orElse(null);
		}

		/** {@code character} goes into its slot (replacing what was there) and {@code active} becomes the slot being played. */
		Account swap(final Stored character, final int active) {
			List<Stored> list = new ArrayList<>(this.stored.stream().filter(s -> s.slot() != character.slot() && s.slot() != active).toList());
			list.add(character);
			return new Account(active, this.slots, List.copyOf(list), this.origin);
		}

		Account withSlots(final int slots) {
			return new Account(this.active, slots, this.stored, this.origin);
		}

		Account withOrigin(final @Nullable Origin origin) {
			return new Account(this.active, this.slots, this.stored, origin);
		}
	}

	private static final Codec<CharacterRoster> CODEC = RecordCodecBuilder.create(i -> i.group(
		Codec.unboundedMap(UUIDUtil.STRING_CODEC, Account.CODEC).optionalFieldOf("accounts", Map.of()).forGetter(r -> r.accounts)
	).apply(i, CharacterRoster::new));

	public static final SavedDataType<CharacterRoster> TYPE = new SavedDataType<>(MinecraftMode.id("characters"), CharacterRoster::new, CODEC, null);

	private final Map<UUID, Account> accounts = new HashMap<>();

	public CharacterRoster() {
	}

	private CharacterRoster(final Map<UUID, Account> accounts) {
		this.accounts.putAll(accounts);
	}

	/** The level named {@code dimension} (as saved with a character or an origin), or null when there is none. */
	static @Nullable ServerLevel level(final MinecraftServer server, final String dimension) {
		Identifier id = Identifier.tryParse(dimension);
		return id == null ? null : server.getLevel(ResourceKey.create(Registries.DIMENSION, id));
	}

	public static CharacterRoster get(final MinecraftServer server) {
		return server.getDataStorage().computeIfAbsent(TYPE);
	}

	public Account account(final UUID player) {
		return this.accounts.getOrDefault(player, Account.NEW);
	}

	void put(final UUID player, final Account account) {
		this.accounts.put(player, account);
		this.setDirty();
	}
}
