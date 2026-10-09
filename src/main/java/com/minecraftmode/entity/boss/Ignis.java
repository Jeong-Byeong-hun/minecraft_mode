package com.minecraftmode.entity.boss;

import com.minecraftmode.entity.CreatureAnim;
import com.minecraftmode.entity.combat.Attacks;
import com.minecraftmode.entity.combat.MobProjectile;
import com.minecraftmode.entity.combat.Telegraph;
import com.minecraftmode.job.skill.Fx;
import com.minecraftmode.job.skill.SkillScheduler;
import java.util.List;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * Ignis, the undying phoenix (Lv 65-75, volcanic crater). Flies over the crater. Phase 1: fireball
 * volleys, flame dives and fire breath. Phase 2 (70%): meteor rain and burning ground. Phase 3 (30%):
 * Rebirth - once, it wraps itself in flame, cannot be hurt and heals, then erupts in a Supernova that
 * only those far away survive; afterwards Supernova returns now and then.
 */
public class Ignis extends RaidBoss {
	private static final int FLAME = 0xFF7A1A;
	private static final int REBIRTH_TICKS = 100;
	private boolean reborn;

	public Ignis(final EntityType<? extends Ignis> type, final Level level) {
		super(type, level);
	}

	@Override
	protected boolean flies() {
		return true;
	}

	@Override
	protected List<Pattern> patterns() {
		return List.of(
			pattern("fireball_volley", 1, 70, 0, 8, this::fireballs),
			pattern("flame_dive", 1, 190, 0, 40, this::flameDive),
			pattern("fire_breath", 1, 160, 18, 34, this::breath),
			pattern("meteor_rain", 2, 230, 0, 20, this::meteors),
			pattern("burning_ground", 2, 210, 0, 10, this::burningGround),
			pattern("supernova", 3, 520, 0, 50, (level, target) -> this.supernova(level))
		);
	}

	@Override
	protected void ambientFx(final ServerLevel level) {
		if (this.tickCount % 3 == 0) {
			level.sendParticles(ParticleTypes.FLAME, this.getX(), this.getY(0.5), this.getZ(), 4, this.getBbWidth() * 0.4, 0.4, this.getBbWidth() * 0.4, 0.01);
		}
		if (this.isInvulnerablePhase() && this.tickCount % 2 == 0) {
			new Fx(Fx.Kind.SPARK, FLAME).burst(level, this.position().add(0, this.getBbHeight() * 0.5, 0), 10, this.getBbWidth() * 0.8, 0.05);
		}
	}

	@Override
	protected void onPhase(final ServerLevel level, final int phase) {
		if (phase == 3 && !this.reborn) {
			this.reborn = true;
			this.rebirth(level);
		}
	}

	private void rebirth(final ServerLevel level) {
		this.setInvulnerableFor(REBIRTH_TICKS);
		this.setBusy(REBIRTH_TICKS + 10);
		this.playAnim(CreatureAnim.CAST);
		this.sound(level, SoundEvents.BLAZE_AMBIENT, 3.0F, 0.4F);
		float healTo = this.getMaxHealth() * 0.45F;
		float perTick = Math.max(0.0F, healTo - this.getHealth()) / REBIRTH_TICKS;
		for (int i = 0; i < REBIRTH_TICKS; i += 2) {
			SkillScheduler.schedule(i + 1, () -> {
				if (this.alive()) {
					this.heal(perTick * 2);
				}
			});
		}
		SkillScheduler.schedule(REBIRTH_TICKS - 40, () -> {
			if (this.alive()) {
				this.supernova(level);
			}
		});
	}

	private void supernova(final ServerLevel level) {
		this.playAnim(CreatureAnim.ROAR);
		this.sound(level, SoundEvents.WITHER_SPAWN, 2.0F, 1.4F);
		Vec3 at = this.ground(this.position());
		this.circle(level, at, 13.0, 40, Telegraph.RED, 3.0F, e -> e.igniteForSeconds(6.0F), () -> {
			this.boom(level, at, 13.0);
			new Fx(Fx.Kind.RING, FLAME).circle(level, Fx.Kind.RING, at.add(0, 0.5, 0), 12);
			level.sendParticles(ParticleTypes.FLAME, at.x, at.y + 1, at.z, 300, 8, 2, 8, 0.2);
		});
	}

	private void fireballs(final ServerLevel level, final LivingEntity target) {
		this.playAnim(CreatureAnim.ATTACK);
		int count = 3 + (this.phase() >= 2 ? 2 : 0);
		for (MobProjectile p : this.volley(level, target, count, 50.0F, 1.0F, new ItemStack(Items.FIRE_CHARGE), Fx.Kind.SPARK, FLAME, 0.0F, 1.1F, true)) {
			p.blast(1.8F).onImpact(at -> level.sendParticles(ParticleTypes.FLAME, at.x, at.y, at.z, 15, 0.5, 0.3, 0.5, 0.05));
		}
		this.sound(level, SoundEvents.BLAZE_SHOOT, 2.5F, 0.7F);
	}

	private void flameDive(final ServerLevel level, final LivingEntity target) {
		Vec3 start = this.ground(this.position());
		Vec3 end = reach(start, target.position(), start.distanceTo(this.ground(target.position())) + 6);
		this.playAnim(CreatureAnim.DIVE);
		this.sound(level, SoundEvents.PHANTOM_SWOOP, 3.0F, 0.6F);
		this.setBusy(40);
		Telegraph.line(level, start, end, 3.5, 20, Telegraph.RED, () -> {
			if (!this.alive()) {
				return;
			}
			java.util.Set<java.util.UUID> hit = new java.util.HashSet<>();
			this.teleportTo(start.x, start.y + 1.0, start.z);
			this.dash(end.add(0, 1.0, 0), 14, () -> {
				level.sendParticles(ParticleTypes.FLAME, this.getX(), this.getY() + 0.5, this.getZ(), 12, 1.0, 0.4, 1.0, 0.02);
				for (LivingEntity e : Attacks.inCircle(level, this.position(), 2.4, 4.0)) {
					if (hit.add(e.getUUID())) {
						Attacks.hit(this, e, this.dmg(1.6F));
						e.igniteForSeconds(4.0F);
						Attacks.knock(e, this.position(), 1.0, 0.6);
					}
				}
			});
		});
	}

	private void breath(final ServerLevel level, final LivingEntity target) {
		this.playAnim(CreatureAnim.BREATH);
		this.sound(level, SoundEvents.ENDER_DRAGON_SHOOT, 2.5F, 1.2F);
		this.cone(level, this.ground(this.position()), target.position(), 14, 28, 14, Telegraph.RED, 0.45F, 5, new Fx(Fx.Kind.SPARK, FLAME),
			e -> e.igniteForSeconds(3.0F));
	}

	private void meteors(final ServerLevel level, final LivingEntity target) {
		this.playAnim(CreatureAnim.CAST);
		this.sound(level, SoundEvents.BLAZE_AMBIENT, 3.0F, 0.5F);
		int count = 7 + this.phase();
		this.rain(level, count, 3.0, 32, Telegraph.ORANGE, 1.4F, e -> e.igniteForSeconds(4.0F), at -> {
			this.boom(level, at, 3.0);
			level.sendParticles(ParticleTypes.LAVA, at.x, at.y + 0.3, at.z, 12, 1.0, 0.2, 1.0, 0.0);
		});
		for (ServerPlayer p : this.fighters(level)) {
			this.fallFromSky(level, this.ground(p.position()), new ItemStack(Items.MAGMA_BLOCK), Fx.Kind.SPARK, FLAME, 32);
		}
	}

	private void burningGround(final ServerLevel level, final LivingEntity target) {
		this.playAnim(CreatureAnim.CAST);
		for (ServerPlayer p : this.fighters(level)) {
			Vec3 at = this.ground(p.position());
			Telegraph.circle(level, at, 3.0, 16, Telegraph.ORANGE, () -> {
				for (int t = 0; t < 100; t += 10) {
					SkillScheduler.schedule(t + 1, () -> {
						if (!this.alive()) {
							return;
						}
						level.sendParticles(ParticleTypes.FLAME, at.x, at.y + 0.2, at.z, 20, 1.5, 0.1, 1.5, 0.01);
						Telegraph.ring(level, at, 3.0, FLAME, 0.8F);
						Attacks.hitAll(this, Attacks.inCircle(level, at, 3.0, 2.0), this.dmg(0.35F), e -> e.igniteForSeconds(2.0F));
					});
				}
			});
		}
		this.sound(level, SoundEvents.FIRECHARGE_USE, 2.5F, 0.6F);
	}

	@Override
	protected void addAdditionalSaveData(final ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putBoolean("Reborn", this.reborn);
	}

	@Override
	protected void readAdditionalSaveData(final ValueInput input) {
		super.readAdditionalSaveData(input);
		this.reborn = input.getBooleanOr("Reborn", false);
	}
}
