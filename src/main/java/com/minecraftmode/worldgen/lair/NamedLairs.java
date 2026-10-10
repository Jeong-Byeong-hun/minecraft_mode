package com.minecraftmode.worldgen.lair;

import static com.minecraftmode.worldgen.lair.LairDef.Place.*;

import com.minecraftmode.MinecraftMode;
import com.minecraftmode.worldgen.lair.LairDef.Decor;
import com.minecraftmode.worldgen.lair.LairDef.Palette;
import com.minecraftmode.worldgen.lair.LairDef.Setting;
import com.minecraftmode.worldgen.lair.LairDef.Shape;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.structure.BuiltinStructureSets;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.StructureSpawnOverride;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.TerrainAdjustment;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.levelgen.structure.placement.AbstractSpreadingStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadType;
import org.jspecify.annotations.Nullable;

/**
 * The 22 named monster lairs. All of them share one structure set, so every placement cell holds at
 * most one lair, chosen among those whose biomes fit (about one per 28 chunks in each direction).
 * Inside a lair's grounds the named monster spawns several times more often ({@link LairMobs}), and
 * walking in shows the lair's name and level range.
 */
public final class NamedLairs {
	public static final StructureType<LairStructure> TYPE = Registry.register(BuiltInRegistries.STRUCTURE_TYPE, MinecraftMode.id("named_lair"),
		() -> LairStructure.CODEC);
	public static final StructurePieceType PIECE = Registry.register(BuiltInRegistries.STRUCTURE_PIECE, MinecraftMode.id("named_lair"),
		(StructurePieceType.ContextlessType)LairPiece::new);
	public static final ResourceKey<StructureSet> SET = ResourceKey.create(Registries.STRUCTURE_SET, MinecraftMode.id("named_lairs"));
	public static final int SPACING = 28;
	public static final int SEPARATION = 11;

	private static final Map<String, LairDef> DEFS = new LinkedHashMap<>();
	private static final Map<ServerPlayer, String> LAST_LAIR = new WeakHashMap<>();

	public static void init() {
		define();
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTickCount() % 40 == 0) {
				for (ServerPlayer player : server.getPlayerList().getPlayers()) {
					announce(player);
				}
			}
		});
		MinecraftMode.LOGGER.info("Registered {} named lairs", DEFS.size());
	}

	public static Collection<LairDef> all() {
		return Collections.unmodifiableCollection(DEFS.values());
	}

	public static @Nullable LairDef def(final String id) {
		return DEFS.get(id);
	}

	/** The lair whose grounds contain {@code pos}, if any. */
	public static @Nullable LairDef at(final ServerLevel level, final BlockPos pos) {
		if (!level.isLoaded(pos)) {
			return null;
		}
		for (Map.Entry<Structure, it.unimi.dsi.fastutil.longs.LongSet> entry : level.structureManager().getAllStructuresAt(pos).entrySet()) {
			if (entry.getKey() instanceof LairStructure lair && level.structureManager().getStructureAt(pos, lair).isValid()) {
				return DEFS.get(lair.lair());
			}
		}
		return null;
	}

	/** Shows the lair's name when a player walks into its grounds. */
	private static void announce(final ServerPlayer player) {
		LairDef lair = at(player.level(), player.blockPosition());
		String id = lair == null ? "" : lair.id();
		if (id.equals(LAST_LAIR.getOrDefault(player, ""))) {
			return;
		}
		LAST_LAIR.put(player, id);
		if (lair != null) {
			LairExp.entered(player, lair);
			player.connection.send(new ClientboundSetTitlesAnimationPacket(10, 50, 20));
			player.connection.send(new ClientboundSetTitleTextPacket(Component.translatable(lair.nameKey()).withStyle(ChatFormatting.GOLD)));
			player.connection.send(new ClientboundSetSubtitleTextPacket(Component.translatable("message.minecraft_mode.lair.enter",
				Component.translatable(lair.named().nameKey()), lair.named().lo(), lair.named().hi()).withStyle(ChatFormatting.GRAY)));
		}
	}

	// ------------------------------------------------------------------ datagen

	public static void bootstrapStructures(final BootstrapContext<Structure> context) {
		HolderGetter<Biome> biomes = context.lookup(Registries.BIOME);
		for (LairDef def : DEFS.values()) {
			HolderSet<Biome> set = HolderSet.direct(def.biomes().stream().map(biomes::getOrThrow).toList());
			GenerationStep.Decoration step = def.setting() == Setting.UNDERGROUND
				? GenerationStep.Decoration.UNDERGROUND_STRUCTURES
				: GenerationStep.Decoration.TOP_LAYER_MODIFICATION;
			Structure.StructureSettings settings = new Structure.StructureSettings(set,
				Map.of(MobCategory.MONSTER, new StructureSpawnOverride(StructureSpawnOverride.BoundingBoxType.STRUCTURE, def.mobs().spawns(def.named()))),
				step, TerrainAdjustment.NONE);
			context.register(def.key(), new LairStructure(settings, def.id()));
		}
	}

	@SuppressWarnings("deprecation")
	public static void bootstrapSets(final BootstrapContext<StructureSet> context) {
		HolderGetter<Structure> structures = context.lookup(Registries.STRUCTURE);
		HolderGetter<StructureSet> sets = context.lookup(Registries.STRUCTURE_SET);
		List<StructureSet.StructureSelectionEntry> entries = new ArrayList<>();
		for (LairDef def : DEFS.values()) {
			entries.add(StructureSet.entry(structures.getOrThrow(def.key())));
		}
		context.register(SET, new StructureSet(entries, new RandomSpreadStructurePlacement(Vec3i.ZERO,
			AbstractSpreadingStructurePlacement.FrequencyReductionMethod.DEFAULT, 1.0F, 74123091,
			Optional.of(new AbstractSpreadingStructurePlacement.ExclusionZone(sets.getOrThrow(BuiltinStructureSets.VILLAGES), 6)),
			SPACING, SEPARATION, RandomSpreadType.LINEAR)));
	}

	// ------------------------------------------------------------------ definitions

	private static void define() {
		List<ResourceKey<Biome>> grassland = List.of(Biomes.PLAINS, Biomes.SUNFLOWER_PLAINS, Biomes.MEADOW, Biomes.FLOWER_FOREST, Biomes.FOREST,
			Biomes.BIRCH_FOREST);
		add("golden_enderman", "Gilded Vault", "황금 보물고", Setting.SURFACE, Shape.DOME,
			palette(Blocks.QUARTZ_BRICKS, Blocks.PURPUR_BLOCK, Blocks.SMOOTH_QUARTZ, Blocks.HONEYCOMB_BLOCK, Blocks.OCHRE_FROGLIGHT, Blocks.QUARTZ_STAIRS,
				Blocks.STONE_BRICKS),
			List.of(Biomes.PLAINS, Biomes.SUNFLOWER_PLAINS, Biomes.MEADOW, Biomes.FOREST, Biomes.BIRCH_FOREST, Biomes.SAVANNA, Biomes.TAIGA,
				Biomes.SNOWY_PLAINS, Biomes.DESERT),
			LairMobs.PLAINS, List.of(new Decor(Blocks.IRON_CHAIN, CEILING, 20)));
		add("goblin_warchief", "Goblin Warcamp", "고블린 전쟁 야영지", Setting.SURFACE, Shape.FORTRESS,
			palette(Blocks.SPRUCE_LOG, Blocks.COARSE_DIRT, Blocks.SPRUCE_PLANKS, Blocks.MUD_BRICKS, Blocks.JACK_O_LANTERN, Blocks.SPRUCE_STAIRS,
				Blocks.COBBLESTONE),
			grassland, LairMobs.PLAINS, List.of(new Decor(Blocks.HAY_BLOCK, GROUND, 6), new Decor(Blocks.COBWEB, CEILING, 25),
				new Decor(Blocks.SKELETON_SKULL, GROUND, 3)));
		add("bandit_captain", "Bandit Hideout", "산적 소굴", Setting.SURFACE, Shape.ZIGGURAT,
			palette(Blocks.TERRACOTTA, Blocks.PACKED_MUD, Blocks.DYED_TERRACOTTA.pick(DyeColor.BROWN), Blocks.DYED_TERRACOTTA.pick(DyeColor.ORANGE),
				Blocks.SHROOMLIGHT, Blocks.MUD_BRICK_STAIRS, Blocks.PACKED_MUD),
			List.of(Biomes.SAVANNA, Biomes.SAVANNA_PLATEAU, Biomes.WINDSWEPT_SAVANNA, Biomes.TAIGA, Biomes.WINDSWEPT_HILLS),
			LairMobs.PLAINS, List.of(new Decor(Blocks.COBWEB, CEILING, 20)));
		add("bog_hag", "Hag's Crooked Spire", "마녀 할멈의 비뚤어진 첨탑", Setting.SURFACE, Shape.SPIRE,
			palette(Blocks.MUD_BRICKS, Blocks.PACKED_MUD, Blocks.MANGROVE_PLANKS, Blocks.MOSS_BLOCK, Blocks.VERDANT_FROGLIGHT, Blocks.MANGROVE_STAIRS,
				Blocks.MUD_BRICKS),
			List.of(Biomes.SWAMP, Biomes.MANGROVE_SWAMP), LairMobs.SWAMP,
			List.of(new Decor(Blocks.COBWEB, CEILING, 35), new Decor(Blocks.MOSS_BLOCK, FLOOR, 120), new Decor(Blocks.BROWN_MUSHROOM, GROUND, 15)));
		add("frost_alpha", "Frost Citadel", "서리 성채", Setting.SURFACE, Shape.FORTRESS,
			palette(Blocks.PACKED_ICE, Blocks.SNOW_BLOCK, Blocks.PACKED_ICE, Blocks.BLUE_ICE, Blocks.SEA_LANTERN, Blocks.POLISHED_DIORITE_STAIRS,
				Blocks.STONE_BRICKS),
			List.of(Biomes.SNOWY_PLAINS, Biomes.SNOWY_TAIGA, Biomes.GROVE, Biomes.SNOWY_SLOPES, Biomes.ICE_SPIKES), LairMobs.SNOW,
			List.of(new Decor(Blocks.POWDER_SNOW, FLOOR, 25), new Decor(Blocks.BONE_BLOCK, GROUND, 6)));
		add("dune_scorpion", "Pyramid of the Scorpion King", "전갈왕의 피라미드", Setting.SURFACE, Shape.PYRAMID,
			palette(Blocks.CUT_SANDSTONE, Blocks.SMOOTH_SANDSTONE, Blocks.SANDSTONE, Blocks.DYED_TERRACOTTA.pick(DyeColor.ORANGE), Blocks.OCHRE_FROGLIGHT,
				Blocks.SANDSTONE_STAIRS, Blocks.SANDSTONE),
			List.of(Biomes.DESERT), LairMobs.DESERT,
			List.of(new Decor(Blocks.COBWEB, CEILING, 30), new Decor(Blocks.SAND, FLOOR, 90), new Decor(Blocks.SKELETON_SKULL, GROUND, 3)));
		add("cave_troll", "Troll Warren", "트롤 굴", Setting.UNDERGROUND, Shape.NONE,
			palette(Blocks.MOSSY_COBBLESTONE, Blocks.COBBLESTONE, Blocks.COBBLESTONE, Blocks.MOSSY_STONE_BRICKS, Blocks.SHROOMLIGHT, Blocks.COBBLESTONE_STAIRS,
				Blocks.COBBLESTONE),
			List.of(Biomes.PLAINS, Biomes.SUNFLOWER_PLAINS, Biomes.FOREST, Biomes.BIRCH_FOREST, Biomes.TAIGA, Biomes.SAVANNA, Biomes.DESERT,
				Biomes.SNOWY_PLAINS, Biomes.WINDSWEPT_HILLS, Biomes.MEADOW, Biomes.DARK_FOREST, Biomes.JUNGLE, Biomes.BADLANDS, Biomes.STONY_PEAKS),
			LairMobs.CAVE, -12, List.of(new Decor(Blocks.COBWEB, CEILING, 30), new Decor(Blocks.BONE_BLOCK, GROUND, 8), new Decor(Blocks.GRAVEL, FLOOR, 80)));
		add("myconid_shaman", "Myconid Temple", "버섯 주술사의 사원", Setting.SURFACE, Shape.DOME,
			palette(Blocks.MUSHROOM_STEM, Blocks.MYCELIUM, Blocks.RED_MUSHROOM_BLOCK, Blocks.BROWN_MUSHROOM_BLOCK, Blocks.SHROOMLIGHT, Blocks.DARK_OAK_STAIRS,
				Blocks.MOSSY_COBBLESTONE),
			List.of(Biomes.DARK_FOREST, Biomes.MUSHROOM_FIELDS, Biomes.PALE_GARDEN), LairMobs.GLOOM,
			List.of(new Decor(Blocks.RED_MUSHROOM, GROUND, 20), new Decor(Blocks.BROWN_MUSHROOM, GROUND, 20)));
		add("jungle_stalker", "Stalker's Ziggurat", "추적자의 지구라트", Setting.SURFACE, Shape.ZIGGURAT,
			palette(Blocks.MOSSY_STONE_BRICKS, Blocks.MOSSY_COBBLESTONE, Blocks.STONE_BRICKS, Blocks.CHISELED_STONE_BRICKS, Blocks.VERDANT_FROGLIGHT,
				Blocks.MOSSY_STONE_BRICK_STAIRS, Blocks.COBBLESTONE),
			List.of(Biomes.JUNGLE, Biomes.SPARSE_JUNGLE, Biomes.BAMBOO_JUNGLE), LairMobs.JUNGLE,
			List.of(new Decor(Blocks.COBWEB, CEILING, 35), new Decor(Blocks.MOSS_BLOCK, FLOOR, 100)));
		add("mad_pig", "Mad Pig's Mud Mound", "미친 돼지의 진흙 둔덕", Setting.SURFACE, Shape.PYRAMID,
			palette(Blocks.MUD_BRICKS, Blocks.PACKED_MUD, Blocks.PACKED_MUD, Blocks.DYED_TERRACOTTA.pick(DyeColor.PINK), Blocks.PEARLESCENT_FROGLIGHT,
				Blocks.MUD_BRICK_STAIRS, Blocks.PACKED_MUD),
			grassland, LairMobs.PLAINS, List.of(new Decor(Blocks.HAY_BLOCK, GROUND, 10), new Decor(Blocks.MUD, FLOOR, 60)));
		add("drowned_corsair", "Drowned Lighthouse", "익사한 등대", Setting.SURFACE, Shape.SPIRE,
			palette(Blocks.PRISMARINE, Blocks.DARK_PRISMARINE, Blocks.PRISMARINE_BRICKS, Blocks.DARK_PRISMARINE, Blocks.SEA_LANTERN,
				Blocks.PRISMARINE_BRICK_STAIRS, Blocks.PRISMARINE),
			List.of(Biomes.BEACH, Biomes.STONY_SHORE, Biomes.SNOWY_BEACH, Biomes.MANGROVE_SWAMP), LairMobs.COAST,
			List.of(new Decor(Blocks.WET_SPONGE, FLOOR, 30), new Decor(Blocks.COBWEB, CEILING, 20)));
		add("badlands_gunslinger", "Outlaw Fort", "무법자의 요새", Setting.SURFACE, Shape.FORTRESS,
			palette(Blocks.SMOOTH_RED_SANDSTONE, Blocks.CUT_RED_SANDSTONE, Blocks.DARK_OAK_PLANKS, Blocks.DYED_TERRACOTTA.pick(DyeColor.RED),
				Blocks.OCHRE_FROGLIGHT, Blocks.RED_SANDSTONE_STAIRS, Blocks.RED_SANDSTONE),
			List.of(Biomes.BADLANDS, Biomes.ERODED_BADLANDS, Biomes.WOODED_BADLANDS), LairMobs.DESERT,
			List.of(new Decor(Blocks.RED_SAND, FLOOR, 80), new Decor(Blocks.COBWEB, CEILING, 20)));
		add("wendigo", "Wendigo's Dead Tree", "윈디고의 고목", Setting.SURFACE, Shape.TREE,
			palette(Blocks.STRIPPED_SPRUCE_LOG, Blocks.COARSE_DIRT, Blocks.SPRUCE_PLANKS, Blocks.BONE_BLOCK, Blocks.SEA_LANTERN, Blocks.SPRUCE_STAIRS,
				Blocks.COBBLESTONE),
			List.of(Biomes.TAIGA, Biomes.SNOWY_TAIGA, Biomes.OLD_GROWTH_PINE_TAIGA, Biomes.OLD_GROWTH_SPRUCE_TAIGA, Biomes.GROVE), LairMobs.SNOW,
			List.of(new Decor(Blocks.BONE_BLOCK, GROUND, 10), new Decor(Blocks.COBWEB, CEILING, 35), new Decor(Blocks.SKELETON_SKULL, GROUND, 3)));
		add("amethyst_sentinel", "Crystal Sanctum", "수정 성소", Setting.UNDERGROUND, Shape.NONE,
			palette(Blocks.CALCITE, Blocks.TUFF_BRICKS, Blocks.CALCITE, Blocks.AMETHYST_BLOCK, Blocks.PEARLESCENT_FROGLIGHT, Blocks.TUFF_BRICK_STAIRS,
				Blocks.SMOOTH_BASALT),
			List.of(Biomes.DRIPSTONE_CAVES, Biomes.LUSH_CAVES), LairMobs.CAVE, 0,
			List.of(new Decor(Blocks.AMETHYST_CLUSTER, GROUND, 15), new Decor(Blocks.AMETHYST_BLOCK, FLOOR, 50)));
		add("ancient_treant", "Heartwood Hall", "심재의 전당", Setting.SURFACE, Shape.TREE,
			new Palette(Blocks.DARK_OAK_LOG, Blocks.MOSS_BLOCK, Blocks.DARK_OAK_PLANKS, Blocks.MOSSY_COBBLESTONE, Blocks.SHROOMLIGHT, Blocks.DARK_OAK_STAIRS,
				Blocks.ROOTED_DIRT, Blocks.DARK_OAK_LEAVES),
			List.of(Biomes.OLD_GROWTH_BIRCH_FOREST, Biomes.OLD_GROWTH_PINE_TAIGA, Biomes.OLD_GROWTH_SPRUCE_TAIGA, Biomes.FOREST, Biomes.DARK_FOREST),
			LairMobs.PLAINS, List.of(new Decor(Blocks.MOSS_CARPET, GROUND, 60), new Decor(Blocks.COBWEB, CEILING, 20)));
		add("magma_behemoth", "Magma Forge", "마그마 용광로", Setting.NETHER, Shape.ZIGGURAT,
			palette(Blocks.POLISHED_BLACKSTONE_BRICKS, Blocks.BLACKSTONE, Blocks.POLISHED_BASALT, Blocks.MAGMA_BLOCK, Blocks.SHROOMLIGHT,
				Blocks.POLISHED_BLACKSTONE_BRICK_STAIRS, Blocks.BLACKSTONE),
			List.of(Biomes.NETHER_WASTES, Biomes.BASALT_DELTAS), LairMobs.NETHER,
			List.of(new Decor(Blocks.MAGMA_BLOCK, FLOOR, 50), new Decor(Blocks.IRON_CHAIN, CEILING, 25)));
		add("soul_reaper", "Soul Ossuary", "영혼 납골당", Setting.NETHER, Shape.SPIRE,
			palette(Blocks.BONE_BLOCK, Blocks.SOUL_SOIL, Blocks.NETHER_BRICKS, Blocks.POLISHED_BASALT, Blocks.SEA_LANTERN, Blocks.NETHER_BRICK_STAIRS,
				Blocks.NETHER_BRICKS),
			List.of(Biomes.SOUL_SAND_VALLEY), LairMobs.SOUL,
			List.of(new Decor(Blocks.SKELETON_SKULL, GROUND, 3), new Decor(Blocks.COBWEB, CEILING, 25)));
		add("wither_knight", "Wither Keep", "위더 아성", Setting.NETHER, Shape.FORTRESS,
			palette(Blocks.RED_NETHER_BRICKS, Blocks.NETHER_BRICKS, Blocks.NETHER_BRICKS, Blocks.GILDED_BLACKSTONE, Blocks.GLOWSTONE, Blocks.NETHER_BRICK_STAIRS,
				Blocks.NETHER_BRICKS),
			List.of(Biomes.CRIMSON_FOREST, Biomes.NETHER_WASTES), LairMobs.CRIMSON,
			List.of(new Decor(Blocks.SKELETON_SKULL, GROUND, 3), new Decor(Blocks.COBWEB, CEILING, 25)));
		add("void_watcher", "Void Observatory", "공허 관측소", Setting.NETHER, Shape.DOME,
			palette(Blocks.OBSIDIAN, Blocks.WARPED_PLANKS, Blocks.WARPED_WART_BLOCK, Blocks.CRYING_OBSIDIAN, Blocks.PEARLESCENT_FROGLIGHT, Blocks.WARPED_STAIRS,
				Blocks.BLACKSTONE),
			List.of(Biomes.WARPED_FOREST), LairMobs.WARPED, List.of(new Decor(Blocks.CRYING_OBSIDIAN, FLOOR, 30)));
		add("echo_stalker", "Echo Vault", "메아리 금고", Setting.UNDERGROUND, Shape.NONE,
			palette(Blocks.DEEPSLATE_BRICKS, Blocks.DEEPSLATE_TILES, Blocks.POLISHED_DEEPSLATE, Blocks.CHISELED_DEEPSLATE, Blocks.SEA_LANTERN,
				Blocks.DEEPSLATE_BRICK_STAIRS, Blocks.COBBLED_DEEPSLATE),
			List.of(Biomes.DEEP_DARK), LairMobs.DEEP_DARK, -46,
			List.of(new Decor(Blocks.SCULK, FLOOR, 120), new Decor(Blocks.COBWEB, CEILING, 20)));
		add("chorus_wraith", "Chorus Spire", "코러스 첨탑", Setting.END, Shape.SPIRE,
			palette(Blocks.PURPUR_BLOCK, Blocks.END_STONE_BRICKS, Blocks.PURPUR_BLOCK, Blocks.PURPUR_PILLAR, Blocks.PEARLESCENT_FROGLIGHT, Blocks.PURPUR_STAIRS,
				Blocks.END_STONE),
			List.of(Biomes.END_HIGHLANDS, Biomes.END_MIDLANDS), LairMobs.END, List.of(new Decor(Blocks.END_ROD, GROUND, 10)));
		add("astral_knight", "Astral Pyramid", "성운 피라미드", Setting.END, Shape.PYRAMID,
			palette(Blocks.END_STONE_BRICKS, Blocks.SMOOTH_QUARTZ, Blocks.END_STONE_BRICKS, Blocks.CRYING_OBSIDIAN, Blocks.SEA_LANTERN,
				Blocks.END_STONE_BRICK_STAIRS, Blocks.END_STONE),
			List.of(Biomes.END_HIGHLANDS, Biomes.END_MIDLANDS, Biomes.END_BARRENS), LairMobs.END, List.of(new Decor(Blocks.END_ROD, GROUND, 10)));
	}

	private static Palette palette(final Block wall, final Block floor, final Block roof, final Block accent, final Block light, final Block stairs,
		final Block foundation) {
		return new Palette(wall, floor, roof, accent, light, stairs, foundation, null);
	}

	private static void add(final String id, final String en, final String ko, final Setting setting, final Shape shape, final Palette palette,
		final List<ResourceKey<Biome>> biomes, final LairMobs mobs, final List<Decor> decor) {
		add(id, en, ko, setting, shape, palette, biomes, mobs, 0, decor);
	}

	private static void add(final String id, final String en, final String ko, final Setting setting, final Shape shape, final Palette palette,
		final List<ResourceKey<Biome>> biomes, final LairMobs mobs, final int depth, final List<Decor> decor) {
		DEFS.put(id, new LairDef(id, en, ko, setting, shape, palette, biomes, mobs, depth, decor));
	}

	private NamedLairs() {
	}
}
