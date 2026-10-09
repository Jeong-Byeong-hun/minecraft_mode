package com.minecraftmode.entity.named;

import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import org.jspecify.annotations.Nullable;

/**
 * A named monster: name, the gear levels it drops ({@code lo}..{@code hi}), how often it replaces
 * ordinary monsters in its habitat ({@code rarity} = share of spawns, 2-15%), stats at its lowest
 * level, body size and its patterns.
 *
 * @param onHit effect its melee hits apply
 */
public record NamedDef(
	String id, String en, String ko, String descEn, String descKo, int lo, int hi, float rarity, double health, double damage, double armor, double speed,
	float width, float height, Habitat habitat, boolean daylight, boolean flying, boolean fireImmune, List<Ability> abilities, @Nullable Holder<MobEffect> onHit,
	int onHitTicks
) {
	public NamedDef {
		abilities = List.copyOf(abilities);
	}

	public String nameKey() {
		return "entity.minecraft_mode." + this.id;
	}

	public String descKey() {
		return this.nameKey() + ".desc";
	}
}
