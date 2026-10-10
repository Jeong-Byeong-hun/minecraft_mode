package com.minecraftmode.mixin;

import com.minecraftmode.job.skill.CombatHooks;
import com.minecraftmode.job.skill.SkillProjectile;
import com.minecraftmode.job.weapon.ProjectileStyle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Arrows of class bows and skills reach like {@link SkillProjectile}s: they hit a monster pressed against the archer (vanilla
 * misses a hitbox the arrow starts inside) and have the full 0.3 margin from the first tick on.
 */
@Mixin(AbstractArrow.class)
public abstract class AbstractArrowMixin {
	@Shadow
	protected abstract boolean canHitEntity(Entity entity);

	@Inject(method = "findHitEntities", at = @At("RETURN"), cancellable = true)
	private void minecraftMode$classArrowReach(final Vec3 from, final Vec3 to, final CallbackInfoReturnable<Collection<EntityHitResult>> cir) {
		AbstractArrow self = (AbstractArrow)(Object)this;
		if (self.level().isClientSide() || !(self.getOwner() instanceof ServerPlayer owner) || !CombatHooks.classArrow(self)) {
			return;
		}
		List<EntityHitResult> hits = new ArrayList<>(cir.getReturnValue());
		for (EntityHitResult extra : SkillProjectile.reach(self, from, to, ProjectileStyle.ARROW.hitRadius(),
			e -> this.canHitEntity(e) && CombatHooks.canHarm(owner, e))) {
			if (hits.stream().noneMatch(hit -> hit.getEntity() == extra.getEntity())) {
				hits.add(extra);
			}
		}
		cir.setReturnValue(hits);
	}
}
