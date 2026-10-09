package com.minecraftmode.client.creature;

import com.minecraftmode.MinecraftMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import net.minecraft.client.model.geom.ModelLayerLocation;
import org.jspecify.annotations.Nullable;

/**
 * Every creature body plan (named monsters and raid bosses), keyed by entity id, plus their model
 * layer locations. Used by the client renderers and by datagen to paint the textures.
 */
public final class CreaturePlans {
	private static @Nullable Map<String, BodyPlan> plans;
	private static final Map<String, ModelLayerLocation> LAYERS = new LinkedHashMap<>();

	private static Map<String, BodyPlan> plans() {
		if (plans == null) {
			Map<String, BodyPlan> map = new LinkedHashMap<>();
			List<BodyPlan> list = new ArrayList<>(NamedPlans.all());
			list.addAll(BossPlans.all());
			for (BodyPlan plan : list) {
				if (map.put(plan.id, plan) != null) {
					throw new IllegalStateException("Duplicate creature plan " + plan.id);
				}
				plan.pack();
			}
			plans = map;
		}
		return plans;
	}

	public static Collection<BodyPlan> all() {
		return Collections.unmodifiableCollection(plans().values());
	}

	public static @Nullable BodyPlan get(final String id) {
		return plans().get(id);
	}

	/** Registers a model layer for every plan (client init). */
	public static void registerLayers() {
		for (BodyPlan plan : all()) {
			ModelLayerLocation layer = new ModelLayerLocation(MinecraftMode.id("creature/" + plan.id), "main");
			LAYERS.put(plan.id, layer);
			ModelLayerRegistry.registerModelLayer(layer, () -> CreatureModel.createLayer(plan));
		}
	}

	public static ModelLayerLocation layer(final String id) {
		return LAYERS.get(id);
	}

	private CreaturePlans() {
	}
}
