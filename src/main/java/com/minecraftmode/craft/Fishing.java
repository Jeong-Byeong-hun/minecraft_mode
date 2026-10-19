package com.minecraftmode.craft;

import com.minecraftmode.MinecraftMode;
import com.minecraftmode.economy.ShopOffers;
import com.minecraftmode.enchantment.EnchantLevels;
import com.minecraftmode.enchantment.LifeEnchantments;
import com.minecraftmode.townquest.TownQuestService;
import com.minecraftmode.townquest.TownQuests;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The mod's thirteen fish (물고기), landed with a plain fishing rod: {@link #CUSTOM_SHARE} of the fish vanilla's fishing table gives
 * become one of them ({@link #fishFor}). First the fish of the place the hook is in may bite ({@link #PLACE_CHANCE}, the sea,
 * rivers, cold water, swamps, warm seas), else one of the fish found anywhere by rarity. Rare kinds need a fishing level
 * ({@link FishDef#minLevel}); below it a lesser fish bites instead. Landing one teaches fishing ({@link Profession#FISHING}), whose
 * level and the rod enchantments of {@link LifeEnchantments} add to the catch. The grocer buys them all. Ids are saved keys.
 */
public final class Fishing {
	/** Share of vanilla's fish that become one of the mod's. */
	public static final float CUSTOM_SHARE = 0.75F;
	/** Chance that a fish of the place bites (when the place has any). */
	public static final float PLACE_CHANCE = 0.40F;
	/** Weight of the rare and better fish per Angler's Instinct level, and per point of luck (Luck of the Sea, the Luck effect). */
	public static final float INSTINCT_PER_LEVEL = 0.5F;
	public static final float LUCK_PER_POINT = 0.25F;
	/** Chance per Net Cast level of one more fish. */
	public static final float NET_CAST_PER_LEVEL = 0.15F;
	/** Fishing experience per Angler's Wisdom level. */
	public static final float WISDOM_PER_LEVEL = 0.20F;

	/** Where a fish lives; {@link #ANY} bites everywhere. */
	public enum Habitat {
		ANY, OCEAN, WARM_OCEAN, RIVER, COLD, SWAMP
	}

	/** How rare a fish is: its item rarity and the fishing experience it teaches. */
	public enum Tier {
		COMMON(Rarity.COMMON, 2), UNCOMMON(Rarity.COMMON, 3), RARE(Rarity.UNCOMMON, 6), VERY_RARE(Rarity.RARE, 15), LEGENDARY(Rarity.EPIC, 30);

		public final Rarity rarity;
		public final int exp;

		Tier(final Rarity rarity, final int exp) {
			this.rarity = rarity;
			this.exp = exp;
		}
	}

	/** One fish: {@code weight} among the fish that can bite, {@code price} the copper the grocer pays. */
	public record FishDef(String id, String en, String ko, Habitat habitat, Tier tier, int minLevel, float weight, float price, int color, int accent) {
		public String nameKey() {
			return "item.minecraft_mode." + this.id;
		}
	}

	private static final List<FishDef> DEFS = List.of(
		new FishDef("mackerel", "Mackerel", "고등어", Habitat.OCEAN, Tier.UNCOMMON, 1, 30.0F, 1.0F / 8, 0x3A6A8A, 0xD8E4EC),
		new FishDef("trout", "Trout", "송어", Habitat.RIVER, Tier.UNCOMMON, 1, 30.0F, 1.0F / 8, 0x8A9A6A, 0xE87A6A),
		new FishDef("smelt", "Smelt", "빙어", Habitat.COLD, Tier.UNCOMMON, 1, 30.0F, 1.0F / 8, 0xC8DCE8, 0x8AA8C8),
		new FishDef("eel", "Eel", "장어", Habitat.SWAMP, Tier.UNCOMMON, 1, 30.0F, 1.0F / 4, 0x4A4A2A, 0xC8B870),
		new FishDef("tuna", "Tuna", "참치", Habitat.WARM_OCEAN, Tier.RARE, 10, 12.0F, 2.0F, 0x2A3A6A, 0xC8D0D8),
		new FishDef("crucian_carp", "Crucian Carp", "붕어", Habitat.ANY, Tier.COMMON, 1, 30.0F, 1.0F / 10, 0x8A7A4A, 0xD8C890),
		new FishDef("minnow", "Minnow", "피라미", Habitat.ANY, Tier.COMMON, 1, 30.0F, 1.0F / 12, 0xA8B8C0, 0xE86A8A),
		new FishDef("loach", "Loach", "미꾸라지", Habitat.ANY, Tier.COMMON, 1, 25.0F, 1.0F / 10, 0x6A5A3A, 0xA89A6A),
		new FishDef("catfish", "Catfish", "메기", Habitat.ANY, Tier.UNCOMMON, 1, 12.0F, 1.0F / 5, 0x4A4A3A, 0x9A9A80),
		new FishDef("carp", "Carp", "잉어", Habitat.ANY, Tier.UNCOMMON, 1, 12.0F, 1.0F / 5, 0xB87A3A, 0xE8C060),
		new FishDef("snakehead", "Snakehead", "가물치", Habitat.ANY, Tier.RARE, 10, 5.0F, 1.0F, 0x3A3A2A, 0x7A8A4A),
		new FishDef("golden_carp", "Golden Carp", "황금 잉어", Habitat.ANY, Tier.VERY_RARE, 25, 1.5F, 9.0F, 0xF0C030, 0xFFF0A0),
		new FishDef("coelacanth", "Coelacanth", "실러캔스", Habitat.ANY, Tier.LEGENDARY, 40, 0.4F, 27.0F, 0x2A4A7A, 0xE8E8F0)
	);

	private static final Map<String, Item> ITEMS = new LinkedHashMap<>();

	public static void init() {
		for (FishDef def : DEFS) {
			ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, MinecraftMode.id(def.id()));
			Item.Properties properties = new Item.Properties().rarity(def.tier().rarity).setId(key)
				.food(new FoodProperties.Builder().nutrition(2).saturationModifier(0.1F).build());
			ITEMS.put(def.id(), Registry.register(BuiltInRegistries.ITEM, key, new Item(properties)));
		}
		LootTableEvents.MODIFY_DROPS.register((table, context, drops) -> {
			if (!table.is(BuiltInLootTables.FISHING) || !(context.getOptional(LootContextParams.THIS_ENTITY) instanceof FishingHook hook)
				|| !(hook.getPlayerOwner() instanceof ServerPlayer player)) {
				return;
			}
			ItemInstance tool = context.getOptional(LootContextParams.TOOL);
			ItemInstance rod = tool != null ? tool : player.getMainHandItem();
			for (int i = 0; i < drops.size(); i++) {
				if (drops.get(i).is(ItemTags.FISHES) && context.getRandom().nextFloat() < CUSTOM_SHARE) {
					drops.set(i, land(player, hook.position(), rod, context.getLuck(), context.getRandom()));
				}
			}
		});
		MinecraftMode.LOGGER.info("Registered {} fish", DEFS.size());
	}

	public static List<FishDef> all() {
		return DEFS;
	}

	public static Item item(final FishDef def) {
		return ITEMS.get(def.id());
	}

	public static Item item(final String id) {
		return ITEMS.get(id);
	}

	public static @Nullable FishDef def(final String id) {
		for (FishDef def : DEFS) {
			if (def.id().equals(id)) {
				return def;
			}
		}
		return null;
	}

	/** The grocer's lines: buys every fish. */
	public static List<ShopOffers.Trade> trades() {
		List<ShopOffers.Trade> out = new ArrayList<>();
		for (FishDef def : DEFS) {
			ShopOffers.Trade line = CraftPrices.line(item(def), def.price());
			if (line != null) {
				out.add(line);
			}
		}
		return out;
	}

	/** One fish landed by {@code player}: the kind, Net Cast and the fishing mastery, and the experience it teaches. */
	public static ItemStack land(final ServerPlayer player, final Vec3 hook, final ItemInstance rod, final float luck, final RandomSource random) {
		ServerLevel level = player.level();
		FishDef def = fishFor(level, BlockPos.containing(hook), Profession.FISHING.level(player), EnchantLevels.get(level, LifeEnchantments.ANY_WATERS, rod) > 0,
			EnchantLevels.get(level, LifeEnchantments.ANGLERS_INSTINCT, rod), luck, random);
		int count = 1;
		if (random.nextInt(100) < Profession.FISHING.doubleChance(player)) {
			count++;
		}
		if (random.nextFloat() < NET_CAST_PER_LEVEL * EnchantLevels.get(level, LifeEnchantments.NET_CAST, rod)) {
			count++;
		}
		int wisdom = EnchantLevels.get(level, LifeEnchantments.ANGLERS_WISDOM, rod);
		Profession.FISHING.addExp(player, Math.round(def.tier().exp * (1.0F + WISDOM_PER_LEVEL * wisdom)));
		TownQuestService.progress(player, TownQuests.Goal.FISH, def.id(), 1);
		return new ItemStack(item(def), count);
	}

	/**
	 * The fish that bites at {@code pos} for a fisher of {@code fishingLevel}: a fish of the place ({@link #PLACE_CHANCE}; every
	 * place's with Any Waters), else one found anywhere. Fish above the fisher's level never bite.
	 */
	public static FishDef fishFor(final ServerLevel level, final BlockPos pos, final int fishingLevel, final boolean anyWaters, final int instinct,
		final float luck, final RandomSource random) {
		Holder<Biome> biome = level.getBiome(pos);
		List<FishDef> place = new ArrayList<>();
		List<FishDef> anywhere = new ArrayList<>();
		for (FishDef def : DEFS) {
			if (def.minLevel() > fishingLevel) {
				continue;
			}
			if (def.habitat() == Habitat.ANY) {
				anywhere.add(def);
			} else if (anyWaters || lives(def.habitat(), biome, level, pos)) {
				place.add(def);
			}
		}
		if (!place.isEmpty() && random.nextFloat() < PLACE_CHANCE) {
			return pick(place, instinct, luck, random);
		}
		return pick(anywhere, instinct, luck, random);
	}

	/** Whether the water at {@code pos} is {@code habitat}. */
	public static boolean lives(final Habitat habitat, final Holder<Biome> biome, final ServerLevel level, final BlockPos pos) {
		return switch (habitat) {
			case ANY -> true;
			case OCEAN -> biome.is(BiomeTags.IS_OCEAN);
			case WARM_OCEAN -> biome.is(Biomes.WARM_OCEAN) || biome.is(Biomes.LUKEWARM_OCEAN) || biome.is(Biomes.DEEP_LUKEWARM_OCEAN);
			case RIVER -> biome.is(BiomeTags.IS_RIVER);
			case COLD -> biome.value().coldEnoughToSnow(pos, level.getSeaLevel());
			case SWAMP -> biome.is(Biomes.SWAMP) || biome.is(Biomes.MANGROVE_SWAMP);
		};
	}

	/** Weighted pick; rare and better fish weigh more with Angler's Instinct and luck. */
	private static FishDef pick(final List<FishDef> pool, final int instinct, final float luck, final RandomSource random) {
		float boost = 1.0F + INSTINCT_PER_LEVEL * instinct + LUCK_PER_POINT * Math.max(0.0F, luck);
		float total = 0.0F;
		for (FishDef def : pool) {
			total += weight(def, boost);
		}
		float roll = random.nextFloat() * total;
		for (FishDef def : pool) {
			roll -= weight(def, boost);
			if (roll < 0.0F) {
				return def;
			}
		}
		return pool.getLast();
	}

	private static float weight(final FishDef def, final float boost) {
		return def.tier().ordinal() >= Tier.RARE.ordinal() ? def.weight() * boost : def.weight();
	}

	public static List<Item> items() {
		return List.copyOf(ITEMS.values());
	}

	private Fishing() {
	}
}
