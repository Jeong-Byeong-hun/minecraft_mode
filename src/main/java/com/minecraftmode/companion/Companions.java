package com.minecraftmode.companion;

import com.minecraftmode.MinecraftMode;
import com.minecraftmode.bag.Bags;
import com.minecraftmode.entity.boss.RaidBoss;
import com.minecraftmode.entity.named.NamedMob;
import com.minecraftmode.job.engrave.EngraveStat;
import com.minecraftmode.job.gear.GearStats;
import com.minecraftmode.job.gear.StatLine;
import com.minecraftmode.progress.Contribution;
import com.minecraftmode.raid.RaidDimension;
import com.minecraftmode.registry.ModAttachments;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityLevelChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import org.jspecify.annotations.Nullable;

/**
 * Pets and mounts. Pets follow their owner, never fight, and give a small bonus that grows with the pet's level (1..10, from
 * the owner's kills). Mounts are ridden; one of them flies. Both are learned from items (pet charms, mount whistles) into the
 * player's collection, then summoned from it (screen or keys); summoned ones are never saved with the world and come back with
 * their owner. Ids are saved keys.
 */
public final class Companions {
	public static final int MAX_PET_LEVEL = 10;

	public record PetDef(String id, String en, String ko, String descEn, String descKo, Rarity rarity, boolean flying, float width, float height,
		List<StatLine> perLevel) {
		public String nameKey() {
			return "entity.minecraft_mode.pet_" + this.id;
		}

		public List<StatLine> lines(final int level) {
			List<StatLine> out = new ArrayList<>();
			for (StatLine l : this.perLevel) {
				out.add(StatLine.of(l.stat(), Math.round(l.value() * level * 100.0F) / 100.0F));
			}
			return out;
		}
	}

	public record MountDef(String id, String en, String ko, String descEn, String descKo, Rarity rarity, boolean flying, float width, float height,
		double speed, double jump) {
		public String nameKey() {
			return "entity.minecraft_mode.mount_" + this.id;
		}
	}

	/** What a player owns: pets with their experience, mounts, the summoned pet and the last ridden mount. */
	public record Data(Map<String, Integer> pets, List<String> mounts, String activePet, String lastMount) {
		public static final Data DEFAULT = new Data(Map.of(), List.of(), "", "");
		public static final Codec<Data> CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf("pets", Map.of()).forGetter(Data::pets),
			Codec.STRING.listOf().optionalFieldOf("mounts", List.of()).forGetter(Data::mounts),
			Codec.STRING.optionalFieldOf("active_pet", "").forGetter(Data::activePet),
			Codec.STRING.optionalFieldOf("last_mount", "").forGetter(Data::lastMount)
		).apply(i, Data::new));
		public static final StreamCodec<ByteBuf, Data> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);

		public Data {
			pets = Map.copyOf(pets);
			mounts = List.copyOf(mounts);
		}

		public boolean hasPet(final String id) {
			return this.pets.containsKey(id);
		}

		public boolean hasMount(final String id) {
			return this.mounts.contains(id);
		}

		Data withPet(final String id, final int exp) {
			Map<String, Integer> map = new HashMap<>(this.pets);
			map.put(id, exp);
			return new Data(map, this.mounts, this.activePet, this.lastMount);
		}

		Data withMount(final String id) {
			if (this.mounts.contains(id)) {
				return this;
			}
			List<String> list = new ArrayList<>(this.mounts);
			list.add(id);
			return new Data(this.pets, list, this.activePet, this.lastMount);
		}

		Data withActive(final String pet) {
			return new Data(this.pets, this.mounts, pet, this.lastMount);
		}

		Data withLastMount(final String mount) {
			return new Data(this.pets, this.mounts, this.activePet, mount);
		}
	}

	private static final Map<String, PetDef> PETS = new LinkedHashMap<>();
	private static final Map<String, MountDef> MOUNTS = new LinkedHashMap<>();
	private static final Map<String, EntityType<PetEntity>> PET_TYPES = new LinkedHashMap<>();
	private static final Map<String, EntityType<MountEntity>> MOUNT_TYPES = new LinkedHashMap<>();
	private static final Map<String, Item> PET_ITEMS = new LinkedHashMap<>();
	private static final Map<String, Item> MOUNT_ITEMS = new LinkedHashMap<>();
	/** Summoned pets by owner (server memory). */
	private static final Map<UUID, UUID> SUMMONED = new HashMap<>();

	static {
		pet("ember_fox", "Ember Fox", "불여우", "Sniffs out treasure.", "보물을 잘 찾아냅니다.", Rarity.UNCOMMON, false, 0.5F, 0.6F,
			StatLine.of(EngraveStat.ITEM_FIND, 0.5F));
		pet("frost_owl", "Frost Owl", "서리 부엉이", "A wise companion that speeds up learning.", "배움을 돕는 현명한 동료입니다.", Rarity.UNCOMMON, true, 0.4F, 0.6F,
			StatLine.of(EngraveStat.EXP_BONUS, 0.5F));
		pet("golden_scarab", "Golden Scarab", "황금 풍뎅이", "Rolls coins your way.", "동전을 굴려 옵니다.", Rarity.UNCOMMON, false, 0.4F, 0.3F,
			StatLine.of(EngraveStat.GOLD_FIND, 1.0F));
		pet("moss_turtle", "Moss Turtle", "이끼 거북", "Slow, sturdy and calming.", "느리지만 든든합니다.", Rarity.UNCOMMON, false, 0.6F, 0.4F,
			StatLine.of(EngraveStat.DAMAGE_REDUCTION, 0.3F));
		pet("spark_sprite", "Spark Sprite", "불꽃 정령", "Hums with mana.", "마나로 웅웅거립니다.", Rarity.RARE, true, 0.4F, 0.5F,
			StatLine.of(EngraveStat.MANA_REGEN, 0.05F), StatLine.of(EngraveStat.MAX_MANA, 1.0F));
		pet("shadow_cat", "Shadow Cat", "그림자 고양이", "Points at weak spots.", "약점을 짚어 줍니다.", Rarity.RARE, false, 0.5F, 0.6F,
			StatLine.of(EngraveStat.CRIT_CHANCE, 0.2F));
		pet("crystal_slime", "Crystal Slime", "수정 슬라임", "Shares its bounce.", "탄력을 나눠 줍니다.", Rarity.RARE, false, 0.6F, 0.6F,
			StatLine.of(EngraveStat.MAX_HEALTH, 0.3F));
		pet("baby_dragon", "Baby Dragon", "아기 용", "Hates big monsters.", "큰 괴물을 싫어합니다.", Rarity.EPIC, true, 0.6F, 0.7F,
			StatLine.of(EngraveStat.BOSS_DAMAGE, 0.5F));

		mount("swift_stallion", "Swift Stallion", "질풍마", "A fast and faithful horse.", "빠르고 충직한 말입니다.", Rarity.UNCOMMON, false, 1.4F, 1.6F, 0.30, 0.75);
		mount("dune_raptor", "Dune Raptor", "사막 랩터", "Sprints across the sand.", "모래 위를 질주합니다.", Rarity.RARE, false, 1.0F, 1.8F, 0.34, 0.85);
		mount("frost_wolf", "Frost Wolf", "서리 늑대", "Bounds through snow.", "눈밭을 뛰어넘습니다.", Rarity.RARE, false, 1.2F, 1.6F, 0.33, 0.95);
		mount("ember_lion", "Ember Lion", "화염 사자", "Its mane never cools.", "갈기가 식지 않습니다.", Rarity.EPIC, false, 1.4F, 1.7F, 0.36, 0.95);
		mount("crystal_stag", "Crystal Stag", "수정 사슴", "Leaps like light.", "빛처럼 도약합니다.", Rarity.EPIC, false, 1.2F, 1.8F, 0.35, 1.15);
		mount("storm_griffin", "Storm Griffin", "폭풍 그리핀", "Flies where you look.", "바라보는 곳으로 날아갑니다.", Rarity.EPIC, true, 1.6F, 1.7F, 0.33, 1.0);
	}

	private static void pet(final String id, final String en, final String ko, final String descEn, final String descKo, final Rarity rarity, final boolean flying,
		final float width, final float height, final StatLine... perLevel) {
		PETS.put(id, new PetDef(id, en, ko, descEn, descKo, rarity, flying, width, height, List.of(perLevel)));
	}

	private static void mount(final String id, final String en, final String ko, final String descEn, final String descKo, final Rarity rarity, final boolean flying,
		final float width, final float height, final double speed, final double jump) {
		MOUNTS.put(id, new MountDef(id, en, ko, descEn, descKo, rarity, flying, width, height, speed, jump));
	}

	public static void init() {
		for (PetDef def : PETS.values()) {
			ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, MinecraftMode.id("pet_" + def.id()));
			EntityType<PetEntity> type = Registry.register(BuiltInRegistries.ENTITY_TYPE, key, EntityType.Builder.<PetEntity>of(PetEntity::new, MobCategory.MISC)
				.sized(def.width(), def.height()).clientTrackingRange(10).noSave().build(key));
			PET_TYPES.put(def.id(), type);
			FabricDefaultAttributeRegistry.register(type, PetEntity.createAttributes());
			PET_ITEMS.put(def.id(), registerItem("pet_" + def.id(), p -> new CompanionItem(true, def.id(), p), def.rarity()));
		}
		for (MountDef def : MOUNTS.values()) {
			ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, MinecraftMode.id("mount_" + def.id()));
			EntityType<MountEntity> type = Registry.register(BuiltInRegistries.ENTITY_TYPE, key, EntityType.Builder.<MountEntity>of(MountEntity::new, MobCategory.MISC)
				.sized(def.width(), def.height()).passengerAttachments(def.height() * 0.9F).clientTrackingRange(10).build(key));
			MOUNT_TYPES.put(def.id(), type);
			FabricDefaultAttributeRegistry.register(type, MountEntity.createAttributes());
			MOUNT_ITEMS.put(def.id(), registerItem("mount_" + def.id(), p -> new CompanionItem(false, def.id(), p), def.rarity()));
		}
		ServerLivingEntityEvents.AFTER_DEATH.register(Companions::afterDeath);
		ServerPlayerEvents.JOIN.register(Companions::restore);
		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> restore(newPlayer));
		ServerEntityLevelChangeEvents.AFTER_PLAYER_CHANGE_LEVEL.register((player, origin, destination) -> restore(player));
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			softLanding(handler.player);
			server.execute(() -> dismissPet(handler.player));
		});
	}

	/**
	 * Leaving the game in mid-flight: the mount is never saved, so the rider comes back in the air. Slow falling is saved with them
	 * (this runs before the player is written) and brings them down gently.
	 */
	private static void softLanding(final ServerPlayer player) {
		if (player.getVehicle() instanceof MountEntity mount && mount.flies() && !mount.onGround()) {
			player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 200, 0, false, false, true));
		}
	}

	private static Item registerItem(final String name, final java.util.function.Function<Item.Properties, Item> factory, final Rarity rarity) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, MinecraftMode.id(name));
		return Registry.register(BuiltInRegistries.ITEM, key, factory.apply(new Item.Properties().stacksTo(1).rarity(rarity).setId(key)));
	}

	public static Collection<PetDef> pets() {
		return PETS.values();
	}

	public static Collection<MountDef> mounts() {
		return MOUNTS.values();
	}

	public static @Nullable PetDef pet(final String id) {
		return PETS.get(id);
	}

	public static @Nullable MountDef mount(final String id) {
		return MOUNTS.get(id);
	}

	public static EntityType<PetEntity> petType(final PetDef def) {
		return PET_TYPES.get(def.id());
	}

	public static EntityType<MountEntity> mountType(final MountDef def) {
		return MOUNT_TYPES.get(def.id());
	}

	public static Item petItem(final String id) {
		return PET_ITEMS.get(id);
	}

	public static Item mountItem(final String id) {
		return MOUNT_ITEMS.get(id);
	}

	public static Data data(final Player player) {
		return player.getAttachedOrElse(ModAttachments.COMPANIONS, Data.DEFAULT);
	}

	private static void set(final ServerPlayer player, final Data data) {
		player.setAttached(ModAttachments.COMPANIONS, data);
	}

	// ------------------------------------------------------------------ pet levels and bonuses

	/** Total pet experience needed to reach {@code level}. */
	public static int petExpFor(final int level) {
		return 25 * level * (level - 1);
	}

	public static int petLevel(final int exp) {
		int level = 1;
		while (level < MAX_PET_LEVEL && exp >= petExpFor(level + 1)) {
			level++;
		}
		return level;
	}

	/** The summoned pet's bonus (GearStats adds it). */
	public static List<StatLine> lines(final Player player) {
		Data data = data(player);
		PetDef def = data.activePet().isEmpty() ? null : PETS.get(data.activePet());
		return def == null ? List.of() : def.lines(petLevel(data.pets().getOrDefault(def.id(), 0)));
	}

	/**
	 * Kills raise the summoned pet of everyone who shared them (see {@link Contribution}): 1 per monster, 10 per named monster, 50
	 * per boss. A named monster's pet or mount drop is rolled for the top contributor.
	 */
	private static void afterDeath(final LivingEntity entity, final net.minecraft.world.damagesource.DamageSource source) {
		if (!(entity instanceof Enemy)) {
			return;
		}
		List<Contribution.Share> shares = Contribution.shares(entity, source);
		if (shares.isEmpty()) {
			return;
		}
		if (entity instanceof NamedMob) {
			rollDrop(shares.getFirst().player(), NAMED_DROP, Rarity.RARE);
		}
		for (Contribution.Share share : shares) {
			growPet(share.player(), entity);
		}
	}

	private static void growPet(final ServerPlayer killer, final LivingEntity entity) {
		Data data = data(killer);
		if (data.activePet().isEmpty() || !data.hasPet(data.activePet())) {
			return;
		}
		int gain = entity instanceof RaidBoss ? 50 : entity instanceof NamedMob ? 10 : 1;
		int before = data.pets().get(data.activePet());
		int after = Math.min(petExpFor(MAX_PET_LEVEL), before + gain);
		if (after == before) {
			return;
		}
		set(killer, data.withPet(data.activePet(), after));
		if (petLevel(after) > petLevel(before)) {
			killer.sendSystemMessage(Component.translatable("message.minecraft_mode.pet.level_up", Component.translatable(PETS.get(data.activePet()).nameKey()),
				petLevel(after)).withStyle(ChatFormatting.AQUA));
			GearStats.invalidate(killer);
		}
	}

	// ------------------------------------------------------------------ drops

	/** Chance of a rare pet charm or mount whistle per named monster kill. */
	public static final float NAMED_DROP = 0.005F;
	/** Per personal lair treasure opened (rare). */
	public static final float LAIR_DROP = 0.04F;

	/** Per raid clear (epic), by difficulty. */
	public static float raidDrop(final com.minecraftmode.raid.RaidDifficulty difficulty) {
		return switch (difficulty) {
			case NORMAL -> 0.03F;
			case HEROIC -> 0.06F;
			case NIGHTMARE -> 0.10F;
		};
	}

	/** Every pet charm and mount whistle of {@code rarity}. */
	public static List<Item> items(final Rarity rarity) {
		List<Item> out = new ArrayList<>();
		PETS.values().stream().filter(d -> d.rarity() == rarity).forEach(d -> out.add(PET_ITEMS.get(d.id())));
		MOUNTS.values().stream().filter(d -> d.rarity() == rarity).forEach(d -> out.add(MOUNT_ITEMS.get(d.id())));
		return out;
	}

	/** A random pet charm of {@code rarity} that {@code owned} does not have yet (the merit shop's offer); empty when it has them all. */
	public static ItemStack randomPet(final Rarity rarity, final RandomSource random, final Data owned) {
		List<PetDef> pool = PETS.values().stream().filter(d -> d.rarity() == rarity && !owned.hasPet(d.id())).toList();
		return pool.isEmpty() ? ItemStack.EMPTY : new ItemStack(PET_ITEMS.get(pool.get(random.nextInt(pool.size())).id()));
	}

	/**
	 * With {@code chance}, gives {@code player} a pet charm or mount whistle of {@code rarity} they do not own yet (none when they own
	 * them all). Returns whether one dropped.
	 */
	public static boolean rollDrop(final ServerPlayer player, final float chance, final Rarity rarity) {
		if (player.getRandom().nextFloat() >= chance) {
			return false;
		}
		Data data = data(player);
		List<Item> pool = new ArrayList<>();
		PETS.values().stream().filter(d -> d.rarity() == rarity && !data.hasPet(d.id())).forEach(d -> pool.add(PET_ITEMS.get(d.id())));
		MOUNTS.values().stream().filter(d -> d.rarity() == rarity && !data.hasMount(d.id())).forEach(d -> pool.add(MOUNT_ITEMS.get(d.id())));
		if (pool.isEmpty()) {
			return false;
		}
		ItemStack stack = new ItemStack(pool.get(player.getRandom().nextInt(pool.size())));
		player.sendSystemMessage(Component.translatable("message.minecraft_mode.companion.found", stack.getHoverName()).withStyle(ChatFormatting.LIGHT_PURPLE));
		Bags.giveOrMail(player, stack);
		player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8F, 1.6F);
		return true;
	}

	// ------------------------------------------------------------------ learning and summoning

	/** Adds a pet or mount to the collection (from its item). Returns false when it was already there. */
	public static boolean learn(final ServerPlayer player, final boolean pet, final String id) {
		Data data = data(player);
		if (pet ? data.hasPet(id) : data.hasMount(id)) {
			return false;
		}
		set(player, pet ? data.withPet(id, 0) : data.withMount(id));
		Component name = Component.translatable(pet ? PETS.get(id).nameKey() : MOUNTS.get(id).nameKey());
		player.sendSystemMessage(Component.translatable(pet ? "message.minecraft_mode.pet.learned" : "message.minecraft_mode.mount.learned", name).withStyle(ChatFormatting.GOLD));
		player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 0.6F, 1.4F);
		com.minecraftmode.progress.Progress.companionLearned(player);
		return true;
	}

	public static @Nullable PetEntity summonedPet(final ServerPlayer owner) {
		UUID id = SUMMONED.get(owner.getUUID());
		Entity e = id == null ? null : owner.level().getEntity(id);
		return e instanceof PetEntity pet && pet.isAlive() ? pet : null;
	}

	/** Summons pet {@code id} (dismissing the current one); {@code id} "" just dismisses. */
	public static boolean summonPet(final ServerPlayer owner, final String id) {
		Data data = data(owner);
		if (!id.isEmpty() && !data.hasPet(id)) {
			return false;
		}
		dismissPet(owner);
		set(owner, data.withActive(id));
		GearStats.invalidate(owner);
		if (!id.isEmpty()) {
			spawnPet(owner, PETS.get(id));
		}
		return true;
	}

	private static void spawnPet(final ServerPlayer owner, final PetDef def) {
		ServerLevel level = owner.level();
		PetEntity pet = petType(def).create(level, EntitySpawnReason.MOB_SUMMONED);
		if (pet == null) {
			return;
		}
		pet.setOwner(owner.getUUID());
		pet.snapTo(owner.getX() + 1, owner.getY(), owner.getZ() + 1, owner.getYRot(), 0.0F);
		level.addFreshEntity(pet);
		SUMMONED.put(owner.getUUID(), pet.getUUID());
	}

	public static void dismissPet(final ServerPlayer owner) {
		PetEntity pet = summonedPet(owner);
		if (pet != null) {
			pet.discard();
		}
		SUMMONED.remove(owner.getUUID());
	}

	/** After join, respawn or a world change: the active pet comes along. */
	private static void restore(final ServerPlayer player) {
		dismissPet(player);
		Data data = data(player);
		PetDef def = data.activePet().isEmpty() ? null : PETS.get(data.activePet());
		if (def != null && data.hasPet(def.id())) {
			spawnPet(player, def);
		}
	}

	/** The mount key: gets off a summoned mount, or calls {@code id} ("" = the last ridden one, else the first owned). */
	public static void toggleMount(final ServerPlayer owner, final String id) {
		if (owner.getVehicle() instanceof MountEntity mount) {
			owner.stopRiding();
			mount.discard();
			return;
		}
		Data data = data(owner);
		String wanted = !id.isEmpty() ? id : data.hasMount(data.lastMount()) ? data.lastMount() : data.mounts().isEmpty() ? "" : data.mounts().getFirst();
		if (wanted.isEmpty()) {
			owner.sendOverlayMessage(Component.translatable("message.minecraft_mode.mount.none").withStyle(ChatFormatting.RED));
			return;
		}
		summonMount(owner, wanted);
	}

	/** Summons mount {@code id} and puts the owner on it. Not in raids. */
	public static boolean summonMount(final ServerPlayer owner, final String id) {
		Data data = data(owner);
		MountDef def = MOUNTS.get(id);
		if (def == null || !data.hasMount(id)) {
			return false;
		}
		if (RaidDimension.is(owner.level()) || com.minecraftmode.dungeon.DungeonDimension.is(owner.level()) || owner.isPassenger() || owner.isInWater()
			|| !owner.onGround()) {
			owner.sendOverlayMessage(Component.translatable("message.minecraft_mode.mount.not_here").withStyle(ChatFormatting.RED));
			return false;
		}
		ServerLevel level = owner.level();
		MountEntity mount = mountType(def).create(level, EntitySpawnReason.MOB_SUMMONED);
		if (mount == null) {
			return false;
		}
		mount.setup(owner);
		mount.snapTo(owner.getX(), owner.getY(), owner.getZ(), owner.getYRot(), 0.0F);
		level.addFreshEntity(mount);
		owner.startRiding(mount);
		set(owner, data.withLastMount(id));
		level.playSound(null, owner.getX(), owner.getY(), owner.getZ(), SoundEvents.HORSE_SADDLE.value(), SoundSource.PLAYERS, 0.8F, 1.0F);
		return true;
	}

	private Companions() {
	}
}
