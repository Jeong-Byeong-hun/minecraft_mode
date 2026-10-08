package com.minecraftmode.mixin;

import com.minecraftmode.city.CityZone;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ServerExplosion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Explosions still hurt entities inside the capital but never break or burn its blocks. */
@Mixin(ServerExplosion.class)
public abstract class ServerExplosionMixin {
	@Inject(method = "calculateExplodedPositions", at = @At("RETURN"))
	private void minecraftMode$sparePublicWorks(final CallbackInfoReturnable<List<BlockPos>> cir) {
		ServerExplosion explosion = (ServerExplosion)(Object)this;
		if (CityZone.isCityLevel(explosion.level())) {
			cir.getReturnValue().removeIf(CityZone::inside);
		}
	}
}
