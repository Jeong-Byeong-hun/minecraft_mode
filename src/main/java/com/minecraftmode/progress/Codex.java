package com.minecraftmode.progress;

import com.minecraftmode.MinecraftMode;
import com.minecraftmode.bag.BagItem;
import com.minecraftmode.bag.Bags;
import com.minecraftmode.consumable.Consumables;
import com.minecraftmode.entity.named.NamedMobs;
import com.minecraftmode.job.gear.GearArmorItem;
import com.minecraftmode.job.weapon.JobWeaponItem;
import com.minecraftmode.raid.RaidBosses;
import com.minecraftmode.registry.ModAttachments;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import org.jspecify.annotations.Nullable;

/**
 * The monster and item codex records (named monsters, lairs and raids stay in {@link PlayerRecords}):
 * <ul>
 * <li>kills per common monster type ({@link ModAttachments#MONSTER_KILLS}, entity type ids): every vanilla monster that spawns on its
 * own (no creepers, giants or illusioners) and the mod's own non-named monsters, counted for everyone a kill is shared with;</li>
 * <li>items found ({@link ModAttachments#ITEMS_FOUND}, item paths of this mod): weapons, armor, consumables and the other materials, noted
 * when they first sit in the inventory, worn gear or a carried bag (checked every second).</li>
 * </ul>
 * Both are records only; they give no stats.
 */
public final class Codex {
	/** Kill milestones a monster's codex frame shows (bronze, silver, gold). */
	public static final int[] MILESTONES = {10, 100, 1000};
	private static final Set<String> NOT_LISTED = Set.of("minecraft:creeper", "minecraft:giant", "minecraft:illusioner");
	/** Mod items that are never found in play: blocks the world places and items that only stand for projectiles. */
	private static final Set<String> HIDDEN_ITEMS = Set.of("shop_block", "blacksmith_shop", "grocer_shop", "jeweler_shop", "alchemist_shop", "guild_shop",
		"engraving_table", "kitchen_station", "alchemy_station", "smithing_station", "lair_chest", "lair_cache");

	/** Where a common monster is met. */
	public enum Place {
		NIGHT, VARIANTS, CAVES, WATER, NETHER, END, RAIDERS, DEEP_DARK, BOSS;

		public String key() {
			return "screen.minecraft_mode.codex.place." + this.name().toLowerCase(Locale.ROOT);
		}
	}

	private static final Map<String, Place> PLACES = Map.ofEntries(
		Map.entry("zombie", Place.NIGHT), Map.entry("skeleton", Place.NIGHT), Map.entry("spider", Place.NIGHT), Map.entry("witch", Place.NIGHT),
		Map.entry("enderman", Place.NIGHT), Map.entry("slime", Place.NIGHT), Map.entry("phantom", Place.NIGHT), Map.entry("zombie_villager", Place.NIGHT),
		Map.entry("zombie_horse", Place.NIGHT),
		Map.entry("husk", Place.VARIANTS), Map.entry("stray", Place.VARIANTS), Map.entry("bogged", Place.VARIANTS), Map.entry("parched", Place.VARIANTS),
		Map.entry("camel_husk", Place.VARIANTS), Map.entry("creaking", Place.VARIANTS),
		Map.entry("cave_spider", Place.CAVES), Map.entry("silverfish", Place.CAVES), Map.entry("breeze", Place.CAVES), Map.entry("endermite", Place.CAVES),
		Map.entry("sulfur_cube", Place.CAVES), Map.entry("mine_raider", Place.CAVES), Map.entry("mythril_golem", Place.CAVES),
		Map.entry("drowned", Place.WATER), Map.entry("guardian", Place.WATER), Map.entry("elder_guardian", Place.WATER), Map.entry("zombie_nautilus", Place.WATER),
		Map.entry("blaze", Place.NETHER), Map.entry("ghast", Place.NETHER), Map.entry("magma_cube", Place.NETHER), Map.entry("piglin", Place.NETHER),
		Map.entry("piglin_brute", Place.NETHER), Map.entry("hoglin", Place.NETHER), Map.entry("zoglin", Place.NETHER), Map.entry("wither_skeleton", Place.NETHER),
		Map.entry("zombified_piglin", Place.NETHER),
		Map.entry("shulker", Place.END),
		Map.entry("pillager", Place.RAIDERS), Map.entry("vindicator", Place.RAIDERS), Map.entry("evoker", Place.RAIDERS), Map.entry("vex", Place.RAIDERS),
		Map.entry("ravager", Place.RAIDERS),
		Map.entry("warden", Place.DEEP_DARK),
		Map.entry("wither", Place.BOSS), Map.entry("ender_dragon", Place.BOSS));

	/** Item codex sections. */
	public enum Category {
		WEAPON, ARMOR, CONSUMABLE, MATERIAL;

		public String key() {
			return "screen.minecraft_mode.codex.items." + this.name().toLowerCase(Locale.ROOT);
		}
	}

	private static @Nullable List<EntityType<?>> monsters;
	private static @Nullable Set<EntityType<?>> monsterSet;
	private static @Nullable Map<Category, List<Item>> items;

	/** The common monsters of the codex, grouped by {@link Place} (then by name id). */
	public static List<EntityType<?>> monsters() {
		if (monsters == null) {
			Set<EntityType<?>> named = new HashSet<>();
			for (var def : NamedMobs.all()) {
				named.add(NamedMobs.type(def));
			}
			List<EntityType<?>> list = new ArrayList<>();
			for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
				Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
				boolean ours = id.getNamespace().equals(MinecraftMode.MOD_ID);
				if (type.getCategory() != MobCategory.MONSTER || NOT_LISTED.contains(id.toString()) || named.contains(type) || RaidBosses.def(type) != null
					|| !(id.getNamespace().equals("minecraft") || ours && PLACES.containsKey(id.getPath()))) {
					continue;
				}
				list.add(type);
			}
			list.sort(Comparator.comparing((EntityType<?> t) -> place(t).ordinal()).thenComparing(t -> BuiltInRegistries.ENTITY_TYPE.getKey(t).getPath()));
			monsters = List.copyOf(list);
			monsterSet = Set.copyOf(list);
		}
		return monsters;
	}

	public static Place place(final EntityType<?> type) {
		return PLACES.getOrDefault(BuiltInRegistries.ENTITY_TYPE.getKey(type).getPath(), Place.NIGHT);
	}

	/** The spawn egg that stands for {@code type} in the codex, if it has one. */
	public static @Nullable Item egg(final EntityType<?> type) {
		return SpawnEggItem.byId(type).map(holder -> holder.value()).orElse(null);
	}

	public static Map<String, Integer> kills(final Player player) {
		return player.getAttachedOrElse(ModAttachments.MONSTER_KILLS, Map.of());
	}

	public static int kills(final Player player, final EntityType<?> type) {
		return kills(player).getOrDefault(BuiltInRegistries.ENTITY_TYPE.getKey(type).toString(), 0);
	}

	/** 0 = none, 1..3 = the {@link #MILESTONES} reached. */
	public static int milestone(final int kills) {
		int reached = 0;
		for (int m : MILESTONES) {
			if (kills >= m) {
				reached++;
			}
		}
		return reached;
	}

	/** Called for everyone a monster's death is shared with ({@link Progress}). */
	static void killed(final ServerPlayer player, final LivingEntity dead) {
		monsters();
		if (monsterSet == null || !monsterSet.contains(dead.getType())) {
			return;
		}
		Map<String, Integer> map = new HashMap<>(kills(player));
		map.merge(BuiltInRegistries.ENTITY_TYPE.getKey(dead.getType()).toString(), 1, Integer::sum);
		player.setAttached(ModAttachments.MONSTER_KILLS, Map.copyOf(map));
	}

	// ------------------------------------------------------------------ items

	/** Every codex item by section, in registry order. */
	public static Map<Category, List<Item>> items() {
		if (items == null) {
			Map<Category, List<Item>> map = new EnumMap<>(Category.class);
			for (Category c : Category.values()) {
				map.put(c, new ArrayList<>());
			}
			for (Item item : BuiltInRegistries.ITEM) {
				Category category = category(item);
				if (category != null) {
					map.get(category).add(item);
				}
			}
			map.replaceAll((c, list) -> List.copyOf(list));
			items = map;
		}
		return items;
	}

	public static @Nullable Category category(final Item item) {
		Identifier id = BuiltInRegistries.ITEM.getKey(item);
		if (!id.getNamespace().equals(MinecraftMode.MOD_ID) || item instanceof SpawnEggItem || HIDDEN_ITEMS.contains(id.getPath())
			|| id.getPath().startsWith("projectile_")) {
			return null;
		}
		if (item instanceof JobWeaponItem) {
			return Category.WEAPON;
		}
		if (item instanceof GearArmorItem) {
			return Category.ARMOR;
		}
		if (Consumables.def(id.getPath()) != null && Consumables.item(id.getPath()) == item) {
			return Category.CONSUMABLE;
		}
		return Category.MATERIAL;
	}

	public static List<String> found(final Player player) {
		return player.getAttachedOrElse(ModAttachments.ITEMS_FOUND, List.of());
	}

	public static boolean found(final Player player, final Item item) {
		return found(player).contains(BuiltInRegistries.ITEM.getKey(item).getPath());
	}

	/** Notes every codex item {@code player} now holds (inventory, worn gear and carried bags). */
	static void scan(final ServerPlayer player) {
		Set<String> known = new LinkedHashSet<>(found(player));
		int before = known.size();
		for (ItemStack stack : player.getInventory()) {
			note(known, stack);
			if (stack.getItem() instanceof BagItem) {
				for (ItemStack inside : Bags.contents(stack)) {
					note(known, inside);
				}
			}
		}
		if (known.size() != before) {
			player.setAttached(ModAttachments.ITEMS_FOUND, List.copyOf(known));
		}
	}

	private static void note(final Set<String> known, final ItemStack stack) {
		if (!stack.isEmpty() && category(stack.getItem()) != null) {
			known.add(BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath());
		}
	}

	private Codex() {
	}
}
