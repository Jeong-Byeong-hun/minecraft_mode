package com.minecraftmode.event;

import com.minecraftmode.bounty.Bounties;
import com.minecraftmode.city.CityZone;
import com.minecraftmode.companion.Companions;
import com.minecraftmode.dungeon.Dungeons;
import com.minecraftmode.entity.MobPower;
import com.minecraftmode.entity.named.NamedDef;
import com.minecraftmode.entity.named.NamedMob;
import com.minecraftmode.entity.named.NamedMobs;
import com.minecraftmode.job.JobProgression;
import com.minecraftmode.loot.Coins;
import com.minecraftmode.loot.EvolutionEtherItem;
import com.minecraftmode.loot.GearShop;
import com.minecraftmode.progress.Progress;
import com.minecraftmode.progress.ResetCycle;
import com.minecraftmode.registry.ModItems;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * World events in the overworld, once a day at dusk: on two days of every three a titan (a giant champion of a named monster)
 * rises near a player out in the wilds, and on the third the capital is invaded in three waves. Everyone who did enough damage to
 * a titan (or fought the invaders) is rewarded when it falls. Operators start and stop events with {@code /worldevent}.
 */
public final class WorldEvents {
	public static final String INVADER_TAG = "minecraft_mode.invader";
	/** Day phase (ticks after sunrise) when the day's event starts. */
	public static final long DUSK = 12500L;
	public static final int TITAN_LIFETIME = 20 * 60 * 20;
	public static final float TITAN_HEALTH = 12.0F;
	public static final float TITAN_SCALE = 2.2F;
	public static final double TITAN_LEASH = 40.0;
	/** Share of the titan's health a player must deal to be rewarded (the killing blow always counts). */
	public static final float CONTRIBUTION = 0.02F;
	public static final int WAVES = 3;
	public static final int INVASION_LIFETIME = 20 * 60 * 10;
	private static final double INVASION_RANGE = 160.0;

	private static @Nullable UUID titan;
	private static @Nullable NamedDef titanDef;
	private static int titanAge;
	/** Where the titan rose; it is put back there if it follows someone into the capital. */
	private static @Nullable BlockPos titanHome;
	private static final Map<UUID, Float> DEALT = new HashMap<>();
	private static long lastDay = -1L;
	private static @Nullable Invasion invasion;

	/** A running invasion: the wave, its living invaders, everyone who fought, and the bar. */
	private static final class Invasion {
		int wave;
		int age;
		int waveAge;
		final Set<UUID> alive = new HashSet<>();
		int waveSize;
		final Set<UUID> fighters = new HashSet<>();
		final ServerBossEvent bar;

		Invasion(final ServerBossEvent bar) {
			this.bar = bar;
		}
	}

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(WorldEvents::tick);
		ServerLivingEntityEvents.AFTER_DAMAGE.register(WorldEvents::afterDamage);
		ServerLivingEntityEvents.AFTER_DEATH.register(WorldEvents::afterDeath);
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			titan = null;
			titanDef = null;
			DEALT.clear();
			invasion = null;
			lastDay = -1L;
		});
		// Invaders are persistent and spawn inside the walls: ones saved with the world (server stopped mid-invasion) would roam
		// the capital forever, ignored by the guards, so they vanish when no invasion is running.
		ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
			if (invasion == null && entity.entityTags().contains(INVADER_TAG)) {
				entity.discard();
			}
		});
	}

	// ------------------------------------------------------------------ queries

	public static @Nullable UUID titan() {
		return titan;
	}

	public static boolean invasionRunning() {
		return invasion != null;
	}

	public static int invasionWave() {
		return invasion == null ? 0 : invasion.wave;
	}

	public static Set<UUID> invaders() {
		return invasion == null ? Set.of() : Set.copyOf(invasion.alive);
	}

	// ------------------------------------------------------------------ scheduling

	private static void tick(final MinecraftServer server) {
		ServerLevel overworld = server.overworld();
		long ticks = ResetCycle.ticks(overworld);
		long day = ticks / ResetCycle.DAY_TICKS;
		long phase = ticks % ResetCycle.DAY_TICKS;
		if (lastDay < 0) {
			lastDay = phase >= DUSK ? day : day - 1;
		}
		if (day != lastDay && phase >= DUSK && phase < DUSK + 3000) {
			lastDay = day;
			// no event monsters while mob spawning is off (peaceful building worlds, tests)
			if (!overworld.players().isEmpty() && overworld.getGameRules().get(GameRules.SPAWN_MOBS)) {
				if (day % 3 == 2 && CityZone.isCityLevel(overworld)) {
					startInvasion(overworld);
				} else {
					spawnTitan(overworld, null, null);
				}
			}
		}
		if (titan != null) {
			tickTitan(overworld);
		}
		if (invasion != null) {
			tickInvasion(overworld);
		}
	}

	// ------------------------------------------------------------------ titans

	/**
	 * Raises a titan near {@code near} (a random player out of the city when null) from {@code def} (the named monster whose range
	 * best fits that player's level when null). Returns the titan or null when there was nowhere to put it.
	 */
	public static @Nullable NamedMob spawnTitan(final ServerLevel level, final @Nullable ServerPlayer near, final @Nullable NamedDef def) {
		if (titan != null) {
			return null;
		}
		RandomSource random = level.getRandom();
		ServerPlayer player = near;
		if (player == null) {
			List<ServerPlayer> outside = level.players().stream().filter(p -> !p.isSpectator() && !CityZone.inside(p.blockPosition())).toList();
			if (outside.isEmpty()) {
				return null;
			}
			player = outside.get(random.nextInt(outside.size()));
		}
		NamedDef chosen = def != null ? def : fitting(JobProgression.get(player).level());
		BlockPos at = null;
		for (int attempt = 0; attempt < 12 && at == null; attempt++) {
			double angle = random.nextDouble() * Math.PI * 2;
			double distance = 22 + random.nextInt(14);
			int x = Mth.floor(player.getX() + Math.cos(angle) * distance);
			int z = Mth.floor(player.getZ() + Math.sin(angle) * distance);
			level.getChunk(x >> 4, z >> 4);
			BlockPos top = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, 0, z));
			if (!CityZone.inside(top) && level.getFluidState(top.below()).isEmpty() && top.getY() > level.getMinY()) {
				at = top;
			}
		}
		if (at == null) {
			return null;
		}
		NamedMob mob = NamedMobs.type(chosen).create(level, EntitySpawnReason.EVENT);
		if (mob == null) {
			return null;
		}
		BlockPos home = at;
		int nearby = (int)level.players().stream().filter(p -> p.distanceToSqr(Vec3.atCenterOf(home)) < 128 * 128).count();
		mob.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, random.nextFloat() * 360.0F, 0.0F);
		mob.makeChampion(TITAN_HEALTH * (1.0F + 0.5F * Math.max(0, nearby - 1)), TITAN_SCALE, "entity.minecraft_mode.world_boss", at, TITAN_LEASH);
		titan = mob.getUUID();
		titanDef = chosen;
		titanAge = 0;
		titanHome = at;
		DEALT.clear();
		MobPower.set(mob.getUUID(), 1.5F);
		level.addFreshEntity(mob);
		level.playSound(null, at, SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 4.0F, 0.6F);
		Component name = Component.translatable(chosen.nameKey());
		for (ServerPlayer p : level.getServer().getPlayerList().getPlayers()) {
			p.sendSystemMessage(msg("titan_rises", name, at.getX(), at.getZ()).withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD));
			if (p.level() == level) {
				title(p, msg("titan_title").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD), name.copy().withStyle(ChatFormatting.GOLD));
			}
		}
		return mob;
	}

	/** The named monster whose top level is the first at or above {@code level} (the strongest for very high levels). */
	private static NamedDef fitting(final int level) {
		NamedDef best = null;
		for (NamedDef def : NamedMobs.all()) {
			if (def.hi() >= level && (best == null || def.hi() < best.hi())) {
				best = def;
			}
		}
		if (best == null) {
			best = NamedMobs.all().stream().max(Comparator.comparingInt(NamedDef::hi)).orElseThrow();
		}
		return best;
	}

	private static void tickTitan(final ServerLevel level) {
		titanAge++;
		Entity entity = level.getEntity(titan);
		if (entity != null && titanHome != null && titanAge % 20 == 0 && CityZone.inside(entity.blockPosition())) {
			// the guards would remove it (and the event would hang): a titan that reaches the walls is sent back where it rose
			entity.teleportTo(titanHome.getX() + 0.5, titanHome.getY(), titanHome.getZ() + 0.5);
			if (entity instanceof Mob mob) {
				mob.setTarget(null);
			}
		}
		if (titanAge > TITAN_LIFETIME) {
			if (entity != null) {
				entity.discard();
			}
			MobPower.clear(titan);
			for (ServerPlayer p : level.getServer().getPlayerList().getPlayers()) {
				p.sendSystemMessage(msg("titan_left", titanDef == null ? Component.empty() : Component.translatable(titanDef.nameKey())).withStyle(ChatFormatting.GRAY));
			}
			titan = null;
			titanDef = null;
			DEALT.clear();
		}
	}

	/** Ends the titan without rewards ({@code /worldevent stop}). */
	public static void stopTitan(final ServerLevel level) {
		if (titan != null) {
			Entity entity = level.getEntity(titan);
			if (entity != null) {
				entity.discard();
			}
			MobPower.clear(titan);
		}
		titan = null;
		titanDef = null;
		DEALT.clear();
	}

	private static void afterDamage(final LivingEntity entity, final DamageSource source, final float base, final float taken, final boolean blocked) {
		if (!(source.getEntity() instanceof ServerPlayer player) || taken <= 0.0F) {
			return;
		}
		if (entity.getUUID().equals(titan)) {
			DEALT.merge(player.getUUID(), taken, Float::sum);
		} else if (invasion != null && invasion.alive.contains(entity.getUUID())) {
			invasion.fighters.add(player.getUUID());
		}
	}

	private static void afterDeath(final LivingEntity entity, final DamageSource source) {
		if (!(entity.level() instanceof ServerLevel level)) {
			return;
		}
		if (entity.getUUID().equals(titan)) {
			titanDefeated(level, entity, source);
			return;
		}
		if (invasion != null && invasion.alive.remove(entity.getUUID())) {
			MobPower.clear(entity.getUUID());
			if (source.getEntity() instanceof ServerPlayer player) {
				invasion.fighters.add(player.getUUID());
			}
		}
	}

	/**
	 * Rewards everyone who dealt {@link #CONTRIBUTION} of the titan's health (and the killer): titan shards (one more for the top
	 * three), coins and Evolution Ether of its level, enhancement stones, and a 10% chance of an epic pet or mount.
	 */
	private static void titanDefeated(final ServerLevel level, final LivingEntity entity, final DamageSource source) {
		NamedDef def = titanDef;
		MobPower.clear(entity.getUUID());
		titan = null;
		titanDef = null;
		float need = entity.getMaxHealth() * CONTRIBUTION;
		Map<UUID, Float> dealt = new LinkedHashMap<>(DEALT);
		DEALT.clear();
		if (source.getEntity() instanceof ServerPlayer killer) {
			dealt.merge(killer.getUUID(), need, Math::max);
		}
		List<Map.Entry<UUID, Float>> ranked = new ArrayList<>(dealt.entrySet());
		ranked.sort(Map.Entry.<UUID, Float>comparingByValue().reversed());
		MinecraftServer server = level.getServer();
		RandomSource random = level.getRandom();
		int hi = def == null ? 50 : def.hi();
		List<String> top = new ArrayList<>();
		int rewarded = 0;
		for (int i = 0; i < ranked.size(); i++) {
			Map.Entry<UUID, Float> e = ranked.get(i);
			ServerPlayer p = server.getPlayerList().getPlayer(e.getKey());
			if (p == null || e.getValue() < need) {
				continue;
			}
			rewarded++;
			if (top.size() < 3) {
				top.add(p.getPlainTextName());
			}
			give(p, new ItemStack(ModItems.TITAN_SHARD, 1 + (i < 3 ? 1 : 0) + random.nextInt(2)));
			give(p, EvolutionEtherItem.of(hi, 4));
			give(p, new ItemStack(ModItems.ENHANCEMENT_STONE, 2));
			Coins.give(p, GearShop.bracketPrice(Math.max(10, hi)) * 2);
			Companions.rollDrop(p, 0.10F, Rarity.EPIC);
			Progress.worldBossDefeated(p);
			p.sendSystemMessage(msg("titan_reward", Math.round(e.getValue())).withStyle(ChatFormatting.GOLD));
		}
		Component name = def == null ? Component.empty() : Component.translatable(def.nameKey());
		for (ServerPlayer p : server.getPlayerList().getPlayers()) {
			p.sendSystemMessage(msg("titan_fallen", name, rewarded, String.join(", ", top)).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
		}
	}

	// ------------------------------------------------------------------ invasions

	public static boolean startInvasion(final ServerLevel level) {
		if (invasion != null || !CityZone.isCityLevel(level)) {
			return false;
		}
		ServerBossEvent bar = new ServerBossEvent(Mth.createInsecureUUID(level.getRandom()), msg("invasion_bar", 1, WAVES), BossEvent.BossBarColor.RED,
			BossEvent.BossBarOverlay.NOTCHED_6);
		invasion = new Invasion(bar);
		for (ServerPlayer p : level.getServer().getPlayerList().getPlayers()) {
			p.sendSystemMessage(msg("invasion_start").withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
			if (p.level() == level) {
				title(p, msg("invasion_title").withStyle(ChatFormatting.RED, ChatFormatting.BOLD), msg("invasion_subtitle").withStyle(ChatFormatting.GOLD));
			}
		}
		nextWave(level);
		return true;
	}

	/** Ends the invasion without rewards ({@code /worldevent stop}). */
	public static void stopInvasion(final ServerLevel level) {
		if (invasion == null) {
			return;
		}
		for (UUID id : invasion.alive) {
			Entity e = level.getEntity(id);
			if (e != null) {
				e.discard();
			}
			MobPower.clear(id);
		}
		invasion.bar.removeAllPlayers();
		invasion = null;
	}

	private static void nextWave(final ServerLevel level) {
		Invasion inv = invasion;
		if (inv == null) {
			return;
		}
		inv.wave++;
		inv.waveAge = 0;
		int base = CityZone.baseY(level);
		List<ServerPlayer> defenders = defenders(level);
		int players = Math.max(1, defenders.size());
		int avgLevel = defenders.isEmpty() ? 20 : (int)defenders.stream().mapToInt(p -> JobProgression.get(p).level()).average().orElse(20);
		float health = Dungeons.powerHealth(avgLevel) * (0.6F + 0.4F * players);
		float damage = Dungeons.powerDamage(avgLevel);
		RandomSource random = level.getRandom();
		List<EntityType<? extends Mob>> kinds = List.of(EntityTypes.ZOMBIE, EntityTypes.SKELETON, EntityTypes.PILLAGER, EntityTypes.VINDICATOR, EntityTypes.HUSK);
		BlockPos[] gates = {new BlockPos(CityZone.WALL - 6, base, 0), new BlockPos(-CityZone.WALL + 6, base, 0), new BlockPos(0, base, CityZone.WALL - 6)};
		int count = 6 + 3 * inv.wave + 3 * players;
		for (int i = 0; i < count; i++) {
			BlockPos gate = gates[i % gates.length];
			Mob mob = kinds.get(random.nextInt(kinds.size())).create(level, EntitySpawnReason.EVENT);
			if (mob == null) {
				continue;
			}
			spawnInvader(level, mob, gate.offset(random.nextInt(5) - 2, 0, random.nextInt(5) - 2), health, damage);
		}
		if (inv.wave == WAVES) {
			// the warlord: a champion of the named monster fitting the defenders
			NamedDef def = fitting(avgLevel);
			NamedMob boss = NamedMobs.type(def).create(level, EntitySpawnReason.EVENT);
			if (boss != null) {
				BlockPos gate = gates[random.nextInt(gates.length)];
				boss.snapTo(gate.getX() + 0.5, gate.getY(), gate.getZ() + 0.5, 0.0F, 0.0F);
				boss.makeChampion(3.0F * (0.6F + 0.4F * players), 1.4F, "entity.minecraft_mode.warlord", CityZone.spawn(base), 140.0);
				boss.addTag(INVADER_TAG);
				inv.alive.add(boss.getUUID());
				MobPower.set(boss.getUUID(), 1.2F);
				level.addFreshEntity(boss);
			}
		}
		inv.waveSize = inv.alive.size();
		inv.bar.setName(msg("invasion_bar", inv.wave, WAVES));
		inv.bar.setProgress(1.0F);
		for (ServerPlayer p : defenders) {
			title(p, msg("invasion_wave", inv.wave, WAVES).withStyle(ChatFormatting.RED, ChatFormatting.BOLD), Component.empty());
		}
		level.playSound(null, CityZone.spawn(base), SoundEvents.RAID_HORN.value(), SoundSource.HOSTILE, 8.0F, 1.0F);
	}

	private static void spawnInvader(final ServerLevel level, final Mob mob, final BlockPos at, final float health, final float damage) {
		BlockPos ground = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, at);
		mob.snapTo(ground.getX() + 0.5, ground.getY(), ground.getZ() + 0.5, 0.0F, 0.0F);
		mob.finalizeSpawn(level, level.getCurrentDifficultyAt(ground), EntitySpawnReason.EVENT, null);
		var attr = mob.getAttribute(Attributes.MAX_HEALTH);
		if (attr != null) {
			attr.addOrReplacePermanentModifier(new AttributeModifier(com.minecraftmode.MinecraftMode.id("invader_scale"), health - 1.0,
				AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
			mob.setHealth(mob.getMaxHealth());
		}
		mob.addTag(INVADER_TAG);
		mob.setPersistenceRequired();
		invasion.alive.add(mob.getUUID());
		MobPower.set(mob.getUUID(), damage);
		level.addFreshEntity(mob);
	}

	private static List<ServerPlayer> defenders(final ServerLevel level) {
		return level.players().stream().filter(p -> !p.isSpectator() && Math.abs(p.getX()) < INVASION_RANGE && Math.abs(p.getZ()) < INVASION_RANGE).toList();
	}

	private static void tickInvasion(final ServerLevel level) {
		Invasion inv = invasion;
		inv.age++;
		inv.waveAge++;
		if (inv.age % 20 == 0) {
			for (ServerPlayer p : level.players()) {
				boolean near = Math.abs(p.getX()) < INVASION_RANGE && Math.abs(p.getZ()) < INVASION_RANGE;
				if (near) {
					inv.bar.addPlayer(p);
				} else {
					inv.bar.removePlayer(p);
				}
			}
			inv.alive.removeIf(id -> level.getEntity(id) instanceof LivingEntity e && !e.isAlive());
			inv.bar.setProgress(inv.waveSize == 0 ? 0.0F : Mth.clamp(inv.alive.size() / (float)inv.waveSize, 0.0F, 1.0F));
		}
		if (inv.age % 40 == 0) {
			BlockPos plaza = CityZone.spawn(CityZone.baseY(level));
			for (UUID id : inv.alive) {
				if (level.getEntity(id) instanceof Mob mob && mob.getTarget() == null) {
					ServerPlayer near = level.getNearestPlayer(mob, 32.0) instanceof ServerPlayer p && !p.isCreative() && !p.isSpectator() ? p : null;
					if (near != null) {
						mob.setTarget(near);
					} else {
						mob.getNavigation().moveTo(plaza.getX() + 0.5, plaza.getY(), plaza.getZ() + 0.5, 1.0);
					}
				}
			}
		}
		if (inv.age > INVASION_LIFETIME) {
			for (ServerPlayer p : level.getServer().getPlayerList().getPlayers()) {
				p.sendSystemMessage(msg("invasion_retreat").withStyle(ChatFormatting.GRAY));
			}
			stopInvasion(level);
			return;
		}
		// the last wave also ends once only stragglers are left for a while (an invader stuck in an unloaded chunk or the void
		// would otherwise hold the whole invasion until it retreats without rewards)
		boolean waveDone = inv.alive.isEmpty() || inv.alive.size() <= inv.waveSize / 5 && inv.waveAge > 20 * (inv.wave < WAVES ? 30 : 120)
			|| inv.wave < WAVES && inv.waveAge > 20 * 150;
		if (!waveDone) {
			return;
		}
		if (inv.wave < WAVES) {
			nextWave(level);
			return;
		}
		// victory
		MinecraftServer server = level.getServer();
		for (UUID id : inv.alive) {
			Entity e = level.getEntity(id);
			if (e != null) {
				e.discard();
			}
			MobPower.clear(id);
		}
		int rewarded = 0;
		for (UUID id : inv.fighters) {
			ServerPlayer p = server.getPlayerList().getPlayer(id);
			if (p == null) {
				continue;
			}
			rewarded++;
			int lvl = Math.max(10, JobProgression.get(p).level());
			Coins.give(p, GearShop.bracketPrice(lvl) * 2);
			give(p, EvolutionEtherItem.of(lvl, 3));
			give(p, new ItemStack(ModItems.ENHANCEMENT_STONE, 2));
			Bounties.addMerit(p, 10);
			Progress.invasionRepelled(p);
			p.sendSystemMessage(msg("invasion_reward").withStyle(ChatFormatting.GOLD));
		}
		for (ServerPlayer p : server.getPlayerList().getPlayers()) {
			p.sendSystemMessage(msg("invasion_won", rewarded).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
		}
		inv.bar.removeAllPlayers();
		invasion = null;
	}

	// ------------------------------------------------------------------ helpers

	private static void give(final ServerPlayer player, final ItemStack stack) {
		player.getInventory().placeItemBackInInventory(stack, net.minecraft.util.Prediction.SERVER_ONLY);
	}

	private static void title(final ServerPlayer p, final Component title, final Component subtitle) {
		p.connection.send(new ClientboundSetTitlesAnimationPacket(10, 60, 20));
		p.connection.send(new ClientboundSetSubtitleTextPacket(subtitle));
		p.connection.send(new ClientboundSetTitleTextPacket(title));
	}

	static MutableComponent msg(final String key, final Object... args) {
		return Component.translatable("message.minecraft_mode.event." + key, args);
	}

	private WorldEvents() {
	}
}
