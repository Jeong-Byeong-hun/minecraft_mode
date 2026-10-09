package com.minecraftmode.raid;

import com.minecraftmode.MinecraftMode;
import java.util.Optional;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.Musics;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.ARGB;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.world.attribute.AmbientSounds;
import net.minecraft.world.attribute.BackgroundMusic;
import net.minecraft.world.attribute.BedRule;
import net.minecraft.world.attribute.EnvironmentAttributeMap;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.level.CardinalLighting;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import org.jspecify.annotations.Nullable;

/**
 * The raid dimension {@code minecraft_mode:raid}: an empty void (flat generator without layers) with
 * an End-like sky, fixed time, the boss music and no natural spawns. Arenas are built into it by
 * {@link Arenas}. The dimension type is written by datagen from {@link #bootstrapType}; the level stem
 * ({@code data/minecraft_mode/dimension/raid.json}) by {@code RaidDimensionProvider}.
 */
public final class RaidDimension {
	public static final ResourceKey<Level> LEVEL = ResourceKey.create(Registries.DIMENSION, MinecraftMode.id("raid"));
	public static final ResourceKey<DimensionType> TYPE = ResourceKey.create(Registries.DIMENSION_TYPE, MinecraftMode.id("raid"));
	/** Height of every arena floor. */
	public static final int FLOOR_Y = 64;

	public static void bootstrapType(final BootstrapContext<DimensionType> context) {
		context.register(TYPE, new DimensionType(
			true,
			true,
			false,
			false,
			1.0,
			0,
			256,
			256,
			context.lookup(Registries.BLOCK).getOrThrow(BlockTags.INFINIBURN_END),
			0.35F,
			new DimensionType.MonsterSettings(ConstantInt.of(15), 0),
			DimensionType.Skybox.END,
			CardinalLighting.Type.DEFAULT,
			EnvironmentAttributeMap.builder()
				.set(EnvironmentAttributes.FOG_COLOR, ARGB.vector3fFromRGB24(0x140C1E))
				.set(EnvironmentAttributes.SKY_LIGHT_COLOR, ARGB.vector3fFromRGB24(0xC0A8E0))
				.set(EnvironmentAttributes.SKY_COLOR, ARGB.vector3fFromRGB24(0x000000))
				.set(EnvironmentAttributes.AMBIENT_LIGHT_COLOR, ARGB.vector3fFromRGB24(0x504060))
				.set(EnvironmentAttributes.BACKGROUND_MUSIC, new BackgroundMusic(Musics.END_BOSS))
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

	private RaidDimension() {
	}
}
