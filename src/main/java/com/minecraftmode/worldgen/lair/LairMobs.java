package com.minecraftmode.worldgen.lair;

import com.minecraftmode.entity.named.NamedDef;
import com.minecraftmode.entity.named.NamedMobs;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.util.random.WeightedList;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.biome.MobSpawnSettings;

/**
 * The monsters that spawn inside a lair's grounds (its structure box): ordinary monsters of the
 * region plus the lair's named monster at several times its usual rate.
 */
public enum LairMobs {
	PLAINS(Map.of(EntityTypes.ZOMBIE, 100, EntityTypes.SKELETON, 100, EntityTypes.SPIDER, 100)),
	DESERT(Map.of(EntityTypes.HUSK, 110, EntityTypes.SKELETON, 80, EntityTypes.SPIDER, 70)),
	SNOW(Map.of(EntityTypes.STRAY, 100, EntityTypes.ZOMBIE, 90, EntityTypes.SPIDER, 60)),
	SWAMP(Map.of(EntityTypes.ZOMBIE, 90, EntityTypes.BOGGED, 70, EntityTypes.SLIME, 60, EntityTypes.WITCH, 25, EntityTypes.SPIDER, 60)),
	JUNGLE(Map.of(EntityTypes.ZOMBIE, 90, EntityTypes.SKELETON, 80, EntityTypes.SPIDER, 110, EntityTypes.CAVE_SPIDER, 40)),
	COAST(Map.of(EntityTypes.DROWNED, 110, EntityTypes.ZOMBIE, 70, EntityTypes.SKELETON, 80)),
	GLOOM(Map.of(EntityTypes.ZOMBIE, 90, EntityTypes.SKELETON, 80, EntityTypes.SPIDER, 80, EntityTypes.WITCH, 20)),
	CAVE(Map.of(EntityTypes.ZOMBIE, 90, EntityTypes.SKELETON, 100, EntityTypes.CAVE_SPIDER, 70)),
	DEEP_DARK(Map.of(EntityTypes.ZOMBIE, 80, EntityTypes.SKELETON, 100, EntityTypes.CAVE_SPIDER, 40)),
	NETHER(Map.of(EntityTypes.ZOMBIFIED_PIGLIN, 80, EntityTypes.MAGMA_CUBE, 70, EntityTypes.BLAZE, 50, EntityTypes.WITHER_SKELETON, 40)),
	SOUL(Map.of(EntityTypes.SKELETON, 110, EntityTypes.WITHER_SKELETON, 50, EntityTypes.ENDERMAN, 20, EntityTypes.BLAZE, 30)),
	CRIMSON(Map.of(EntityTypes.HOGLIN, 70, EntityTypes.ZOMBIFIED_PIGLIN, 60, EntityTypes.WITHER_SKELETON, 80, EntityTypes.MAGMA_CUBE, 30)),
	WARPED(Map.of(EntityTypes.ENDERMAN, 110, EntityTypes.MAGMA_CUBE, 40, EntityTypes.WITHER_SKELETON, 40)),
	END(Map.of(EntityTypes.ENDERMAN, 120, EntityTypes.ENDERMITE, 30, EntityTypes.SKELETON, 40));

	private final Map<EntityType<?>, Integer> base;

	LairMobs(final Map<EntityType<?>, Integer> base) {
		this.base = new LinkedHashMap<>(base);
	}

	public int totalWeight() {
		return this.base.values().stream().mapToInt(Integer::intValue).sum();
	}

	/**
	 * The lair's spawn list. The named monster gets twice its share of ordinary spawns in its
	 * habitat (6% to 20% of the spawns here), still limited by its own spacing rule.
	 */
	public WeightedList<MobSpawnSettings.SpawnerData> spawns(final NamedDef named) {
		WeightedList.Builder<MobSpawnSettings.SpawnerData> list = WeightedList.builder();
		this.base.entrySet().stream()
			.sorted(Map.Entry.comparingByKey((a, b) -> EntityType.getKey(a).toString().compareTo(EntityType.getKey(b).toString())))
			.forEach(e -> list.add(new MobSpawnSettings.SpawnerData(e.getKey(), UniformInt.of(1, 2)), e.getValue()));
		list.add(new MobSpawnSettings.SpawnerData(NamedMobs.type(named), ConstantInt.of(1)), namedWeight(named));
		return list.build();
	}

	public int namedWeight(final NamedDef named) {
		float share = Math.clamp(named.rarity() * 2.0F, 0.06F, 0.20F);
		return Math.max(1, Math.round(share / (1.0F - share) * this.totalWeight()));
	}
}
