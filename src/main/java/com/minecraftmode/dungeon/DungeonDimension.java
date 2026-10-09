package com.minecraftmode.dungeon;

import com.minecraftmode.MinecraftMode;
import java.util.Optional;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.ARGB;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.world.attribute.AmbientSounds;
import net.minecraft.world.attribute.BedRule;
import net.minecraft.world.attribute.EnvironmentAttributeMap;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.level.CardinalLighting;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import org.jspecify.annotations.Nullable;

/**
 * The dungeon dimension {@code minecraft_mode:dungeon}: an empty, sunless void (flat generator without layers) with a ceiling,
 * cave sounds and no natural spawns. Dungeon runs are built into it by {@link DungeonLayout}. The dimension type is written by
 * datagen from {@link #bootstrapType}; the level stem ({@code data/minecraft_mode/dimension/dungeon.json}) by
 * {@code RaidDimensionProvider}.
 */
public final class DungeonDimension {
	public static final ResourceKey<Level> LEVEL = ResourceKey.create(Registries.DIMENSION, MinecraftMode.id("dungeon"));
	public static final ResourceKey<DimensionType> TYPE = ResourceKey.create(Registries.DIMENSION_TYPE, MinecraftMode.id("dungeon"));
	/** Height of every dungeon floor. */
	public static final int FLOOR_Y = 64;

	public static void bootstrapType(final BootstrapContext<DimensionType> context) {
		context.register(TYPE, new DimensionType(
			true,
			false,
			true,
			false,
			1.0,
			0,
			256,
			256,
			context.lookup(Registries.BLOCK).getOrThrow(BlockTags.INFINIBURN_OVERWORLD),
			0.08F,
			new DimensionType.MonsterSettings(ConstantInt.of(0), 0),
			DimensionType.Skybox.NONE,
			CardinalLighting.Type.DEFAULT,
			EnvironmentAttributeMap.builder()
				.set(EnvironmentAttributes.FOG_COLOR, ARGB.vector3fFromRGB24(0x0C0A10))
				.set(EnvironmentAttributes.SKY_COLOR, ARGB.vector3fFromRGB24(0x000000))
				.set(EnvironmentAttributes.AMBIENT_LIGHT_COLOR, ARGB.vector3fFromRGB24(0x302838))
				.set(EnvironmentAttributes.AMBIENT_SOUNDS, AmbientSounds.LEGACY_CAVE_SETTINGS)
				.set(EnvironmentAttributes.BED_RULE, BedRule.DESTROY_ON_USE)
				.set(EnvironmentAttributes.STRAW_BED_RULE, BedRule.DESTROY_ON_USE)
				.set(EnvironmentAttributes.RESPAWN_ANCHOR_WORKS, false)
				.set(EnvironmentAttributes.CAN_START_RAID, false)
				.build(),
			HolderSet.empty(),
			Optional.empty()
		));
	}

	public static boolean is(final Level level) {
		return level.dimension() == LEVEL;
	}

	public static @Nullable ServerLevel level(final MinecraftServer server) {
		return server.getLevel(LEVEL);
	}

	private DungeonDimension() {
	}
}
