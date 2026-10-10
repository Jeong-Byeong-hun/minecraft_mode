package com.minecraftmode.mixin;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Vanilla monsters keep the weapons and armor they spawned with (skeleton bows, pillager crossbows, vindicator axes, zombie armor,
 * drowned tridents): gear comes from the mod's own drops. What a mob picked up or was handed (a player's item, a raid captain's banner,
 * a drowned's nautilus shell) is "preserved" by vanilla and still drops. The mod's own monsters decide their drops themselves.
 */
@Mixin(Mob.class)
public abstract class MobMixin {
	@Inject(method = "dropCustomDeathLoot", at = @At("HEAD"))
	private void minecraftMode$keepSpawnGear(final ServerLevel level, final DamageSource source, final boolean killedByPlayer, final CallbackInfo ci) {
		Mob self = (Mob)(Object)this;
		if (!BuiltInRegistries.ENTITY_TYPE.getKey(self.getType()).getNamespace().equals("minecraft")) {
			return;
		}
		for (EquipmentSlot slot : EquipmentSlot.VALUES) {
			if (!self.getDropChances().isPreserved(slot)) {
				self.setDropChance(slot, 0.0F);
			}
		}
	}
}
