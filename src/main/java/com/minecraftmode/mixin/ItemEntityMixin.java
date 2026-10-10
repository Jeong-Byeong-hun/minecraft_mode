package com.minecraftmode.mixin;

import com.minecraftmode.bag.Bags;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Picked-up items go into a carried bag that takes them before the inventory (see {@link Bags#pickup}). */
@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin {
	@Shadow
	private UUID target;

	@Shadow
	public abstract boolean hasPickUpDelay();

	@Inject(method = "playerTouch", at = @At("HEAD"))
	private void minecraftMode$fillBags(final Player player, final CallbackInfo ci) {
		ItemEntity self = (ItemEntity)(Object)this;
		if (player instanceof ServerPlayer serverPlayer && !self.isRemoved() && !this.hasPickUpDelay()
			&& (this.target == null || this.target.equals(player.getUUID()))) {
			Bags.pickup(self, serverPlayer);
		}
	}
}
