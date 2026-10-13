package com.minecraftmode.craft;

import com.minecraftmode.MinecraftMode;
import com.minecraftmode.bag.Bags;
import com.minecraftmode.economy.ShopOffers;
import com.minecraftmode.enchantment.EnchantLevels;
import com.minecraftmode.enchantment.LifeEnchantments;
import com.minecraftmode.registry.ModItems;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The mod's twelve crops (농작물). Basic ones come from the grocer (seeds, or the crop itself for those planted like potatoes) and
 * sometimes from cut grass; advanced ones (chili, sesame, strawberry) only from grass and from harvests with Seed Finder, and only
 * for farmers of {@link #ADVANCED_LEVEL}. Harvesting a ripe one teaches farming ({@link Profession#FARMING}), whose level and the
 * hoe enchantments of {@link LifeEnchantments} add to the harvest. Bone meal does nothing on them ({@link FarmCropBlock}). The grocer
 * buys every crop ({@link #trades()}); cooking turns them into dishes. Ids are saved keys.
 */
public final class Crops {
	/** Farming level from which advanced seeds turn up. */
	public static final int ADVANCED_LEVEL = 10;
	/** Random ticks a stage waits on average (beetroots: 3, so basic crops ripen in about wheat's time). */
	public static final int BASIC_SLOWNESS = 2;
	public static final int ADVANCED_SLOWNESS = 3;
	public static final int BASIC_EXP = 3;
	public static final int ADVANCED_EXP = 6;
	/** Chance in percent that cutting grass gives a seed, plus {@link #SEED_FINDER_GRASS} per Seed Finder level. */
	public static final int GRASS_SEED_CHANCE = 5;
	public static final int SEED_FINDER_GRASS = 2;
	/** Share of grass seeds that are advanced (for farmers of {@link #ADVANCED_LEVEL}), plus {@link #SEED_FINDER_ADVANCED} per level. */
	public static final float GRASS_ADVANCED_SHARE = 0.10F;
	public static final float SEED_FINDER_ADVANCED = 0.10F;
	/** Chance per Seed Finder level that a ripe basic crop also gives an advanced seed. */
	public static final float SEED_FINDER_HARVEST = 0.03F;
	/** Chance per Bountiful Harvest level of one more crop. */
	public static final float BOUNTIFUL_PER_LEVEL = 0.15F;
	/** Farming experience per Farmer's Wisdom level. */
	public static final float WISDOM_PER_LEVEL = 0.20F;
	/** Copper the grocer pays for one crop. */
	public static final float BASIC_PRICE = 1.0F / 16;
	public static final float ADVANCED_PRICE = 1.0F / 6;
	/** A seed sells for this share of its crop (basic seeds stay well under the grocer's 1/8 copper asking price). */
	public static final float SEED_SHARE = 0.5F;

	/** How the crop's icon and plant are drawn (client art). */
	public enum Shape {
		GRAIN, LEAFY, STALK, POD, COB, ROUND, ROOT, BULB, PEPPER, SEEDS, BERRY
	}

	/**
	 * One crop. {@code plantsItself}: planted like potatoes (no seed item). {@code nutrition} &gt; 0 makes the crop edible raw.
	 */
	public record CropDef(String id, String en, String ko, String seedEn, String seedKo, boolean advanced, boolean plantsItself, Shape shape, int color,
		int accent, int nutrition) {
		public String nameKey() {
			return "item.minecraft_mode." + this.id;
		}

		public String seedKey() {
			return "item.minecraft_mode." + this.id + "_seeds";
		}
	}

	private static final List<CropDef> DEFS = List.of(
		seeded("rice", "Rice", "쌀", "Rice Seeds", "볍씨", false, Shape.GRAIN, 0xF2EEDC, 0xC8A850, 0),
		seeded("napa_cabbage", "Napa Cabbage", "배추", "Napa Cabbage Seeds", "배추 씨앗", false, Shape.LEAFY, 0x9ACD5A, 0xF0F0D0, 2),
		seeded("green_onion", "Green Onion", "대파", "Green Onion Seeds", "대파 씨앗", false, Shape.STALK, 0x5AB040, 0xF4F4EC, 1),
		seeded("soybean", "Soybean", "콩", "Soybean Seeds", "콩 씨앗", false, Shape.POD, 0xE8D9A0, 0x6A9A3A, 0),
		seeded("corn", "Corn", "옥수수", "Corn Seeds", "옥수수 씨앗", false, Shape.COB, 0xF2C430, 0x7AAA3A, 2),
		seeded("tomato", "Tomato", "토마토", "Tomato Seeds", "토마토 씨앗", false, Shape.ROUND, 0xE03A2A, 0x4A9A30, 2),
		planted("radish", "Radish", "무", Shape.ROOT, 0xF4F4EC, 0x7AC050, 2),
		planted("garlic", "Garlic", "마늘", Shape.BULB, 0xF0E8DC, 0xB89AA0, 0),
		planted("sweet_potato", "Sweet Potato", "고구마", Shape.ROOT, 0x9A3A5A, 0xF0B040, 2),
		seeded("chili_pepper", "Chili Pepper", "고추", "Chili Pepper Seeds", "고추 씨앗", true, Shape.PEPPER, 0xD82A1A, 0x3A8A2A, 0),
		seeded("sesame", "Sesame", "참깨", "Sesame Seeds", "참깨 씨앗", true, Shape.SEEDS, 0xE8DCB8, 0x8A7A50, 0),
		seeded("strawberry", "Strawberry", "딸기", "Strawberry Seeds", "딸기 씨앗", true, Shape.BERRY, 0xE8304A, 0x4AAA3A, 2)
	);

	private static final Map<String, Block> BLOCKS = new LinkedHashMap<>();
	private static final Map<String, Item> PRODUCE = new LinkedHashMap<>();
	private static final Map<String, Item> SEEDS = new LinkedHashMap<>();
	/** Set while Reaping breaks the crops around the first one, so those do not reap again. */
	private static boolean reaping;

	private static CropDef seeded(final String id, final String en, final String ko, final String seedEn, final String seedKo, final boolean advanced,
		final Shape shape, final int color, final int accent, final int nutrition) {
		return new CropDef(id, en, ko, seedEn, seedKo, advanced, false, shape, color, accent, nutrition);
	}

	private static CropDef planted(final String id, final String en, final String ko, final Shape shape, final int color, final int accent, final int nutrition) {
		return new CropDef(id, en, ko, "", "", false, true, shape, color, accent, nutrition);
	}

	/** Registers the crop blocks and their items. */
	public static void init() {
		for (CropDef def : DEFS) {
			ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK, MinecraftMode.id(def.id() + "_crop"));
			Block block = Blocks.register(blockKey, p -> new FarmCropBlock(def, p), BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).noCollision()
				.randomTicks().instabreak().sound(SoundType.CROP).pushReaction(PushReaction.POPPED));
			BLOCKS.put(def.id(), block);
			Item.Properties produce = new Item.Properties().rarity(def.advanced() ? Rarity.UNCOMMON : Rarity.COMMON);
			if (def.nutrition() > 0) {
				produce.food(new FoodProperties.Builder().nutrition(def.nutrition()).saturationModifier(0.3F).build());
			}
			if (def.plantsItself()) {
				PRODUCE.put(def.id(), register(def.id(), p -> new BlockItem(block, p.useItemDescriptionPrefix()), produce));
			} else {
				PRODUCE.put(def.id(), register(def.id(), Item::new, produce));
				SEEDS.put(def.id(), register(def.id() + "_seeds", p -> new BlockItem(block, p.useItemDescriptionPrefix()), new Item.Properties()));
			}
		}
		PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, blockEntity) -> {
			if (player instanceof ServerPlayer serverPlayer && level instanceof ServerLevel server && !player.isCreative()) {
				harvested(serverPlayer, server, pos, state);
				gather(serverPlayer, server, pos, state);
			}
		});
		// Replant works on the drops: the block is already gone when the loot is rolled, and the seeds exist only from here on.
		LootTableEvents.MODIFY_DROPS.register((table, context, drops) -> {
			BlockState state = context.getOptional(LootContextParams.BLOCK_STATE);
			ItemInstance tool = context.getOptional(LootContextParams.TOOL);
			Vec3 origin = context.getOptional(LootContextParams.ORIGIN);
			if (state != null && tool != null && origin != null && state.getBlock() instanceof CropBlock crop && crop.isMaxAge(state)
				&& context.getOptional(LootContextParams.THIS_ENTITY) instanceof ServerPlayer player
				&& EnchantLevels.get(player.level(), LifeEnchantments.REPLANT, tool) > 0) {
				replant(player, player.level(), BlockPos.containing(origin), state, drops);
			}
		});
		MinecraftMode.LOGGER.info("Registered {} crops", DEFS.size());
	}

	private static Item register(final String name, final java.util.function.Function<Item.Properties, Item> factory, final Item.Properties properties) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, MinecraftMode.id(name));
		Item item = factory.apply(properties.setId(key));
		if (item instanceof BlockItem blockItem) {
			blockItem.registerBlocks(Item.BY_BLOCK, item);
		}
		return Registry.register(BuiltInRegistries.ITEM, key, item);
	}

	public static List<CropDef> all() {
		return DEFS;
	}

	public static @Nullable CropDef def(final String id) {
		for (CropDef def : DEFS) {
			if (def.id().equals(id)) {
				return def;
			}
		}
		return null;
	}

	public static Block block(final CropDef def) {
		return BLOCKS.get(def.id());
	}

	public static Item produce(final CropDef def) {
		return PRODUCE.get(def.id());
	}

	public static Item produce(final String id) {
		return PRODUCE.get(id);
	}

	/** The seed item, or null for crops planted like potatoes. */
	public static @Nullable Item seed(final CropDef def) {
		return SEEDS.get(def.id());
	}

	/** What is planted: the seed, or the crop itself. */
	public static Item planting(final CropDef def) {
		Item seed = SEEDS.get(def.id());
		return seed != null ? seed : PRODUCE.get(def.id());
	}

	public static List<Item> seeds() {
		return List.copyOf(SEEDS.values());
	}

	public static List<CropDef> basic() {
		return DEFS.stream().filter(d -> !d.advanced()).toList();
	}

	public static List<CropDef> advanced() {
		return DEFS.stream().filter(CropDef::advanced).toList();
	}

	/** Copper the grocer pays for one {@code def}. */
	public static float price(final CropDef def) {
		return def.advanced() ? ADVANCED_PRICE : BASIC_PRICE;
	}

	/** The grocer's lines: buys every crop and seed, sells the basic seeds (and the self-planting crops) at twice what it pays. */
	public static List<ShopOffers.Trade> trades() {
		List<ShopOffers.Trade> out = new ArrayList<>();
		for (CropDef def : DEFS) {
			ShopOffers.Trade line = CraftPrices.line(produce(def), price(def));
			if (line != null) {
				out.add(line);
			}
			Item seed = seed(def);
			ShopOffers.Trade seedLine = seed == null ? null : CraftPrices.line(seed, price(def) * SEED_SHARE);
			if (seedLine != null) {
				out.add(seedLine);
			}
		}
		for (CropDef def : basic()) {
			out.add(new ShopOffers.Trade(ModItems.COPPER_COIN, 2, planting(def), 16));
		}
		return out;
	}

	// ------------------------------------------------------------------ harvest

	/** A ripe crop was broken: rewards for the mod's crops, then Replant and Reaping (any ripe crop, vanilla ones too). */
	static void harvested(final ServerPlayer player, final ServerLevel level, final BlockPos pos, final BlockState state) {
		if (!(state.getBlock() instanceof CropBlock crop) || !crop.isMaxAge(state)) {
			return;
		}
		ItemStack tool = player.getMainHandItem();
		if (crop instanceof FarmCropBlock farm) {
			reward(player, level, pos, farm.def(), tool);
		}
		int reap = EnchantLevels.get(level, LifeEnchantments.REAPING, tool);
		if (reap > 0 && !reaping) {
			reaping = true;
			try {
				for (BlockPos target : BlockPos.betweenClosed(pos.offset(-reap, 0, -reap), pos.offset(reap, 0, reap))) {
					if (player.getMainHandItem().isEmpty()) {
						break;
					}
					BlockState other = level.getBlockState(target);
					// destroyBlock refuses protected land itself (Player.blockActionRestricted -> CityServices.blocksBuilding)
					if (!target.equals(pos) && other.getBlock() instanceof CropBlock ripe && ripe.isMaxAge(other)) {
						player.gameMode.destroyBlock(target.immutable());
					}
				}
			} finally {
				reaping = false;
			}
		}
	}

	/** Farming experience and the extra crops of a ripe mod crop. */
	public static void reward(final ServerPlayer player, final ServerLevel level, final BlockPos pos, final CropDef def, final ItemStack tool) {
		RandomSource random = player.getRandom();
		int wisdom = EnchantLevels.get(level, LifeEnchantments.FARMERS_WISDOM, tool);
		Profession.FARMING.addExp(player, Math.round((def.advanced() ? ADVANCED_EXP : BASIC_EXP) * (1.0F + WISDOM_PER_LEVEL * wisdom)));
		if (random.nextInt(100) < Profession.FARMING.doubleChance(player)) {
			Block.popResource(level, pos, new ItemStack(produce(def)));
		}
		int bountiful = EnchantLevels.get(level, LifeEnchantments.BOUNTIFUL_HARVEST, tool);
		if (bountiful > 0 && random.nextFloat() < BOUNTIFUL_PER_LEVEL * bountiful) {
			Block.popResource(level, pos, new ItemStack(produce(def)));
		}
		int finder = EnchantLevels.get(level, LifeEnchantments.SEED_FINDER, tool);
		if (!def.advanced() && finder > 0 && Profession.FARMING.level(player) >= ADVANCED_LEVEL && random.nextFloat() < SEED_FINDER_HARVEST * finder) {
			Block.popResource(level, pos, new ItemStack(randomAdvancedSeed(random)));
		}
	}

	/** Plants the harvested crop again with one of its seeds: from its own drops, else from the inventory and bags. */
	private static void replant(final ServerPlayer player, final ServerLevel level, final BlockPos pos, final BlockState harvested, final List<ItemStack> drops) {
		BlockState fresh = harvested.getBlock().defaultBlockState();
		if (!level.getBlockState(pos).isAir() || !fresh.canSurvive(level, pos)) {
			return;
		}
		Item seed = harvested.getCloneItemStack(level, pos, false).getItem();
		boolean paid = false;
		for (ItemStack drop : drops) {
			if (drop.is(seed)) {
				drop.shrink(1);
				paid = true;
				break;
			}
		}
		drops.removeIf(ItemStack::isEmpty);
		if (!paid && Bags.count(player, seed) > 0) {
			Bags.take(player, seed, 1);
			paid = true;
		}
		if (paid) {
			level.setBlockAndUpdate(pos, fresh);
		}
	}

	// ------------------------------------------------------------------ grass

	/** Cutting grass or ferns sometimes gives a seed; advanced ones only for farmers of {@link #ADVANCED_LEVEL}. */
	private static void gather(final ServerPlayer player, final ServerLevel level, final BlockPos pos, final BlockState state) {
		if (!state.is(Blocks.SHORT_GRASS) && !state.is(Blocks.TALL_GRASS) && !state.is(Blocks.FERN) && !state.is(Blocks.LARGE_FERN)) {
			return;
		}
		Item seed = grassSeed(player, EnchantLevels.get(level, LifeEnchantments.SEED_FINDER, player.getMainHandItem()), player.getRandom());
		if (seed != null) {
			Block.popResource(level, pos, new ItemStack(seed));
		}
	}

	/** The seed a cut of grass gives {@code player} (null for none). */
	public static @Nullable Item grassSeed(final ServerPlayer player, final int seedFinder, final RandomSource random) {
		if (random.nextInt(100) >= GRASS_SEED_CHANCE + SEED_FINDER_GRASS * seedFinder) {
			return null;
		}
		if (Profession.FARMING.level(player) >= ADVANCED_LEVEL && random.nextFloat() < GRASS_ADVANCED_SHARE + SEED_FINDER_ADVANCED * seedFinder) {
			return randomAdvancedSeed(random);
		}
		List<CropDef> basic = basic();
		return planting(basic.get(random.nextInt(basic.size())));
	}

	private static Item randomAdvancedSeed(final RandomSource random) {
		List<CropDef> advanced = advanced();
		return planting(advanced.get(random.nextInt(advanced.size())));
	}

	/** Every crop item (produce, then seeds), for creative tabs and docs. */
	public static List<Item> items() {
		List<Item> out = new ArrayList<>(PRODUCE.values());
		out.addAll(SEEDS.values());
		return Collections.unmodifiableList(out);
	}

	private Crops() {
	}
}
