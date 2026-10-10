package com.minecraftmode.worldgen.lair;

import com.minecraftmode.entity.named.NamedMob;
import com.minecraftmode.entity.named.NamedMobs;
import com.minecraftmode.job.JobData;
import com.minecraftmode.job.JobProgression;
import com.minecraftmode.progress.ResetCycle;
import com.minecraftmode.registry.ModBlockEntities;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * A lair's treasure (or supply cache) with personal contents: every player gets their own roll once
 * per {@link ResetCycle cycle} and can come back for what they left until the cycle ends. The goal
 * chest opens only for this cycle's victors: the players who were near (within
 * {@link #CREDIT_RANGE}) when its lord fell. Anyone else who comes near wakes a lord of their own.
 */
public class LairChestBlockEntity extends BlockEntity {
	public static final int SIZE = 27;
	/** A player this close to the goal chest wakes the lord. */
	public static final double WAKE_RANGE = 20.0;
	/** Players this close to the lord when it falls may open the treasure this cycle. */
	public static final double CREDIT_RANGE = 32.0;

	private record Personal(long cycle, List<ItemStack> items) {
		static final Codec<Personal> CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.LONG.fieldOf("cycle").forGetter(Personal::cycle),
			ItemStack.OPTIONAL_CODEC.listOf().fieldOf("items").forGetter(Personal::items)
		).apply(i, Personal::new));
	}

	private static final Codec<Map<String, Personal>> PERSONAL_CODEC = Codec.unboundedMap(Codec.STRING, Personal.CODEC);

	private String lair = "";
	private long seed;
	private boolean cache;
	private long lordCycle = -1L;
	private @Nullable UUID lord;
	private long victorCycle = -1L;
	private final Set<String> victors = new HashSet<>();
	private final Map<String, Personal> personal = new HashMap<>();

	public LairChestBlockEntity(final BlockPos pos, final BlockState state) {
		super(ModBlockEntities.LAIR_CHEST, pos, state);
	}

	public void setup(final String lair, final long seed, final boolean cache) {
		this.lair = lair;
		this.seed = seed;
		this.cache = cache;
		this.setChanged();
	}

	public String lair() {
		return this.lair;
	}

	public boolean isCache() {
		return this.cache;
	}

	public @Nullable LairDef def() {
		return NamedLairs.def(this.lair);
	}

	// ------------------------------------------------------------------ the lord

	/** The lord of this cycle, while it lives (null when dead, despawned or not woken yet). */
	public @Nullable NamedMob lord() {
		if (this.lord == null || !(this.level instanceof ServerLevel level)) {
			return null;
		}
		Entity e = level.getEntity(this.lord);
		return e instanceof NamedMob mob && mob.isAlive() ? mob : null;
	}

	/** True while a lord of this chest is alive (and loaded). */
	public boolean sealed() {
		return !this.cache && this.lord() != null;
	}

	/** True when {@code entity} is this chest's current lord (an older lord that finds it is not removes itself). */
	public boolean isLord(final Entity entity) {
		return this.lord != null && this.lord.equals(entity.getUUID());
	}

	/** True when {@code player} helped defeat a lord of this chest in {@code cycle}, so the treasure opens for them. */
	public boolean victor(final Player player, final long cycle) {
		return this.cache || this.victorCycle == cycle && this.victors.contains(player.getStringUUID());
	}

	private void newCycle(final long cycle) {
		if (this.victorCycle != cycle) {
			this.victorCycle = cycle;
			this.victors.clear();
			this.setChanged();
		}
	}

	/** The lord fell: every player near it (and whoever dealt the blow) may open the treasure this cycle. */
	public void lordDefeated(final ServerLevel level, final NamedMob mob, final DamageSource source) {
		this.newCycle(ResetCycle.cycle(level));
		List<ServerPlayer> credited = new ArrayList<>(level.getPlayers(p -> !p.isSpectator() && p.distanceToSqr(mob) <= CREDIT_RANGE * CREDIT_RANGE));
		if (source.getEntity() instanceof ServerPlayer killer && !credited.contains(killer)) {
			credited.add(killer);
		}
		for (ServerPlayer p : credited) {
			this.victors.add(p.getStringUUID());
			p.sendSystemMessage(Component.translatable("message.minecraft_mode.lair.lord_defeated", Component.translatable(mob.def().nameKey()))
				.withStyle(ChatFormatting.GOLD));
		}
		if (this.isLord(mob)) {
			this.lord = null;
		}
		this.setChanged();
	}

	public long lordCycle() {
		return this.lordCycle;
	}

	public static void serverTick(final Level level, final BlockPos pos, final BlockState state, final LairChestBlockEntity chest) {
		if (chest.cache || level.getGameTime() % 40L != 0L || !(level instanceof ServerLevel server)) {
			return;
		}
		long cycle = ResetCycle.cycle(server);
		chest.newCycle(cycle);
		if (chest.lord() != null) {
			return;
		}
		// someone who has not beaten this cycle's lord comes near: a lord rises for them
		Player near = server.getNearestPlayer(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, WAKE_RANGE, p -> !p.isSpectator() && p instanceof Player pl && !chest.victor(pl, cycle));
		if (near != null) {
			chest.wakeLord(server, cycle);
		}
	}

	/** Spawns a lord next to the chest for {@code cycle} (also used by tests). */
	public @Nullable NamedMob wakeLord(final ServerLevel level, final long cycle) {
		LairDef def = this.def();
		if (def == null || this.cache) {
			return null;
		}
		NamedMob mob = NamedMobs.type(def.named()).create(level, EntitySpawnReason.STRUCTURE);
		if (mob == null) {
			return null;
		}
		// last cycle's lord, if nobody beat it, makes way for the new one
		NamedMob old = this.lord();
		if (old != null) {
			old.discard();
		}
		BlockPos at = this.worldPosition.south();
		mob.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 180.0F, 0.0F);
		mob.finalizeSpawn(level, level.getCurrentDifficultyAt(at), EntitySpawnReason.STRUCTURE, null);
		mob.makeLord(this.worldPosition);
		mob.setPersistenceRequired();
		level.addFreshEntity(mob);
		this.lord = mob.getUUID();
		this.lordCycle = cycle;
		this.setChanged();
		level.playSound(null, at, SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 0.6F, 1.4F);
		for (ServerPlayer p : level.getPlayers(p -> p.distanceToSqr(at.getX(), at.getY(), at.getZ()) < 48 * 48)) {
			p.sendSystemMessage(Component.translatable("message.minecraft_mode.lair.lord_awakens", Component.translatable(def.named().nameKey()),
				Component.translatable(def.nameKey())).withStyle(ChatFormatting.RED));
		}
		return mob;
	}

	// ------------------------------------------------------------------ personal loot

	/** True when {@code player} has not opened this chest in {@code cycle} yet. */
	public boolean fresh(final Player player, final long cycle) {
		Personal p = this.personal.get(player.getStringUUID());
		return p == null || p.cycle() != cycle;
	}

	/**
	 * {@code player}'s own contents for {@code cycle}: rolled on the first open of the cycle, then kept
	 * (minus what was taken) until the cycle ends. Changes write back into the chest.
	 */
	public SimpleContainer container(final Player player, final long cycle) {
		String key = player.getStringUUID();
		Personal p = this.personal.get(key);
		if (p == null || p.cycle() != cycle) {
			// what was left in earlier cycles is gone anyway; keep only this cycle's rolls
			this.personal.values().removeIf(old -> old.cycle() < cycle);
			p = new Personal(cycle, this.roll(player.getUUID(), JobProgression.get(player), cycle));
			this.personal.put(key, p);
			this.setChanged();
		}
		long rolled = p.cycle();
		SimpleContainer container = new SimpleContainer(SIZE) {
			@Override
			public void setChanged() {
				super.setChanged();
				List<ItemStack> now = new ArrayList<>(SIZE);
				for (int i = 0; i < SIZE; i++) {
					now.add(this.getItem(i).copy());
				}
				LairChestBlockEntity.this.personal.put(key, new Personal(rolled, now));
				LairChestBlockEntity.this.setChanged();
			}
		};
		List<ItemStack> items = p.items();
		for (int i = 0; i < Math.min(SIZE, items.size()); i++) {
			container.setItem(i, items.get(i).copy());
		}
		return container;
	}

	private List<ItemStack> roll(final UUID player, final JobData job, final long cycle) {
		LairDef def = this.def();
		NonNullList<ItemStack> slots = NonNullList.withSize(SIZE, ItemStack.EMPTY);
		if (def == null) {
			return slots;
		}
		RandomSource random = RandomSource.create(this.seed ^ player.getMostSignificantBits() ^ player.getLeastSignificantBits() * 31L ^ cycle * 0x9E3779B97F4A7C15L);
		List<ItemStack> loot = this.cache ? LairLoot.cache(def.named(), random) : LairLoot.goal(def.named(), job, random);
		for (ItemStack stack : loot) {
			for (int tries = 0; tries < 40; tries++) {
				int slot = random.nextInt(SIZE);
				if (slots.get(slot).isEmpty()) {
					slots.set(slot, stack);
					break;
				}
			}
		}
		return slots;
	}

	/** Drops everyone's stored contents (a new cycle started for all; also used by tests). */
	public void forgetPersonal() {
		this.personal.clear();
		this.setChanged();
	}

	// ------------------------------------------------------------------ saving

	@Override
	protected void saveAdditional(final ValueOutput output) {
		super.saveAdditional(output);
		output.putString("Lair", this.lair);
		output.putLong("Seed", this.seed);
		output.putBoolean("Cache", this.cache);
		output.putLong("LordCycle", this.lordCycle);
		if (this.lord != null) {
			output.putString("Lord", this.lord.toString());
		}
		output.store("Personal", PERSONAL_CODEC, this.personal);
		output.putLong("VictorCycle", this.victorCycle);
		output.store("Victors", Codec.STRING.listOf(), new ArrayList<>(this.victors));
	}

	@Override
	protected void loadAdditional(final ValueInput input) {
		super.loadAdditional(input);
		this.lair = input.getStringOr("Lair", "");
		this.seed = input.getLongOr("Seed", 0L);
		this.cache = input.getBooleanOr("Cache", false);
		this.lordCycle = input.getLongOr("LordCycle", -1L);
		this.lord = input.getString("Lord").map(UUID::fromString).orElse(null);
		this.personal.clear();
		this.personal.putAll(input.read("Personal", PERSONAL_CODEC).orElse(Map.of()));
		this.victorCycle = input.getLongOr("VictorCycle", -1L);
		this.victors.clear();
		this.victors.addAll(input.read("Victors", Codec.STRING.listOf()).orElse(List.of()));
	}
}
