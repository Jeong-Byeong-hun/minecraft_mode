package com.minecraftmode.mixin;

import com.minecraftmode.job.skill.CombatHooks;
import com.minecraftmode.job.skill.SkillProjectile;
import com.minecraftmode.job.weapon.ProjectileStyle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
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
 * misses a hitbox the arrow starts inside) and have the full 0.3 margin from the first tick on. The arrows of one basic shot hit
 * each enemy once.
 */
@Mixin(AbstractArrow.class)
public abstract class AbstractArrowMixin {
	@Shadow
	protected abstract boolean canHitEntity(Entity entity);

	/** The arrows of one basic shot or skill fan hit each enemy once; the others fly on past it to the monsters around ({@code BasicAttacks.Fan}). */
	@Inject(method = "canHitEntity", at = @At("HEAD"), cancellable = true)
	private void minecraftMode$oneArrowPerEnemy(final Entity entity, final CallbackInfoReturnable<Boolean> cir) {
		AbstractArrow self = (AbstractArrow)(Object)this;
		if (CombatHooks.fanTaken(self, entity)) {
			cir.setReturnValue(false);
		}
		// a player's arrows fly through their own and their party's summons and pets (the damage would be refused anyway)
		if (!self.level().isClientSide() && self.getOwner() instanceof Player owner && CombatHooks.spares(owner, entity)) {
			cir.setReturnValue(false);
		}
	}

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
