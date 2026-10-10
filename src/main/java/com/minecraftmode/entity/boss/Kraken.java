package com.minecraftmode.entity.boss;

import java.util.ArrayList;
import com.minecraftmode.raid.RaidDamage;
import com.minecraftmode.entity.CreatureAnim;
import com.minecraftmode.entity.combat.Attacks;
import com.minecraftmode.entity.combat.Telegraph;
import com.minecraftmode.job.skill.Fx;
import com.minecraftmode.job.skill.SkillScheduler;
import com.minecraftmode.raid.Arenas;
import java.util.List;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * The Abyssal Kraken (Lv 50-60, sunken ship). Never leaves its pool in the middle of the deck; its
 * tentacles reach everywhere. Phase 1: tentacle slams, ink and water spouts. Phase 2 (70%): drowned
 * crew and a whirlpool that drags everyone to the edge of the pool. Phase 3 (35%): Crushing Tide -
 * crossing tentacle sweeps in two waves.
 */
public class Kraken extends RaidBoss {
	private static final int INK = 0x302848;
	private static final int SEA = 0x40A0D0;

	public Kraken(final EntityType<? extends Kraken> type, final Level level) {
		super(type, level);
		// it floats in its pool instead of sinking to the bottom
		this.setNoGravity(true);
	}

	@Override
	protected boolean anchored() {
		return true;
	}

	@Override
	protected List<Pattern> patterns() {
		return List.of(
			pattern("tentacle_slam", 1, 46, 0, 18, this::tentacleSlam),
			pattern("ink_spray", 1, 170, 0, 30, this::inkSpray),
			pattern("water_spout", 1, 140, 0, 10, this::waterSpout),
			pattern("drowned_call", 2, 400, 0, 20, (level, target) -> {
				this.playAnim(CreatureAnim.SUMMON);
				this.summonMinions(level, EntityTypes.DROWNED, 3, 6, target);
			}),
			pattern("whirlpool", 2, 260, 0, 50, this::whirlpool),
			pattern("crushing_tide", 3, 230, 0, 44, this::crushingTide)
		);
	}

	@Override
	protected List<Mechanic> mechanics() {
		return List.of(
			mechanic("kraken_ink_marks", 0.8F, 135, 160, this::inkMarks),
			mechanic("kraken_anchors", 0.45F, 150, 200, this::anchors)
		);
	}

	@Override
	protected void ambientFx(final ServerLevel level) {
		if (this.tickCount % 6 == 0) {
			level.sendParticles(ParticleTypes.BUBBLE_COLUMN_UP, this.getX(), this.getY() + 1.0, this.getZ(), 6, Arenas.POOL_RADIUS * 0.5, 0.2, Arenas.POOL_RADIUS * 0.5,
				0.1);
		}
	}

	/** The tentacles reach from the pool's edge. */
	private Vec3 poolEdgeToward(final Vec3 target) {
		Vec3 c = this.arenaCenter();
		return reach(c, target, Arenas.POOL_RADIUS - 1).add(0, 0, 0);
	}

	private void tentacleSlam(final ServerLevel level, final LivingEntity target) {
		this.playAnim(CreatureAnim.SLAM);
		Vec3 from = this.ground(this.poolEdgeToward(target.position()));
		Vec3 to = reach(from, target.position(), from.distanceTo(this.ground(target.position())) + 4);
		this.sound(level, SoundEvents.ELDER_GUARDIAN_AMBIENT, 2.5F, 0.6F);
		this.line(level, from, to, 2.6, 18, Telegraph.RED, 1.3F, e -> Attacks.knock(e, from, 1.0, 0.5), () -> {
			new Fx(Fx.Kind.BUBBLE, SEA).line(level, from.add(0, 0.5, 0), to.add(0, 0.5, 0), 0.6);
			this.sound(level, to, SoundEvents.GENERIC_SPLASH, 2.0F, 0.6F);
		});
	}

	private void inkSpray(final ServerLevel level, final LivingEntity target) {
		this.playAnim(CreatureAnim.BREATH);
		this.sound(level, SoundEvents.SQUID_SQUIRT, 3.0F, 0.5F);
		Vec3 origin = this.ground(this.poolEdgeToward(target.position()));
		this.cone(level, origin, target.position(), 15, 32, 16, Telegraph.PURPLE, 0.5F, 3, new Fx(Fx.Kind.SMOKE, INK), e -> {
			this.effect(e, MobEffects.BLINDNESS, 60, 0);
			this.effect(e, MobEffects.SLOWNESS, 60, 1);
		});
	}

	private void waterSpout(final ServerLevel level, final LivingEntity target) {
		this.playAnim(CreatureAnim.CAST);
		List<ServerPlayer> fighters = this.fighters(level);
		int n = Math.min(fighters.size(), 1 + this.phase());
		for (int i = 0; i < n; i++) {
			Vec3 at = this.ground(fighters.get((i + this.random.nextInt(Math.max(1, fighters.size()))) % fighters.size()).position());
			this.circle(level, at, 2.2, 24, Telegraph.ORANGE, 0.8F, e -> Attacks.push(e, new Vec3(0, 1.5, 0)), () -> {
				new Fx(Fx.Kind.BUBBLE, SEA).column(level, at, 6.0);
				level.sendParticles(ParticleTypes.SPLASH, at.x, at.y + 1, at.z, 60, 1.0, 2.0, 1.0, 0.3);
				this.sound(level, at, SoundEvents.GENERIC_SPLASH, 2.0F, 0.8F);
			});
		}
	}

	private void whirlpool(final ServerLevel level, final LivingEntity target) {
		this.playAnim(CreatureAnim.SPIN);
		this.sound(level, SoundEvents.CONDUIT_ACTIVATE, 3.0F, 0.5F);
		Vec3 c = this.arenaCenter();
		for (int i = 0; i < 40; i += 5) {
			SkillScheduler.schedule(i, () -> {
				if (this.alive()) {
					this.pull(level, c, Arenas.RADIUS + 2, 0.45);
					new Fx(Fx.Kind.BUBBLE, SEA).circle(level, Fx.Kind.BUBBLE, c.add(0, 0.3, 0), 6 + this.random.nextInt(8));
				}
			});
		}
		this.circle(level, c, Arenas.POOL_RADIUS + 3.5, 44, Telegraph.RED, 1.6F, e -> Attacks.knock(e, c, 1.4, 0.6), () -> {
			this.playAnim(CreatureAnim.SLAM);
			level.sendParticles(ParticleTypes.SPLASH, c.x, c.y + 1, c.z, 200, 6, 1.5, 6, 0.4);
			this.sound(level, c, SoundEvents.GENERIC_SPLASH, 3.0F, 0.5F);
		});
	}

	private void crushingTide(final ServerLevel level, final LivingEntity target) {
		this.playAnim(CreatureAnim.ROAR);
		this.sound(level, SoundEvents.ELDER_GUARDIAN_CURSE, 3.0F, 0.6F);
		Vec3 c = this.arenaCenter();
		float start = this.random.nextFloat() * 90.0F;
		for (int wave = 0; wave < 2; wave++) {
			final float base = start + wave * 45.0F;
			SkillScheduler.schedule(wave * 22 + 1, () -> {
				if (!this.alive()) {
					return;
				}
				for (int k = 0; k < 4; k++) {
					Vec3 dir = Vec3.directionFromRotation(0.0F, base + k * 90.0F);
					Vec3 from = c.add(dir.scale(Arenas.POOL_RADIUS - 1));
					Vec3 to = c.add(dir.scale(Arenas.RADIUS));
					this.line(level, from, to, 4.0, 24, Telegraph.RED, 1.6F, e -> Attacks.knock(e, c, 1.0, 0.5),
						() -> new Fx(Fx.Kind.BUBBLE, SEA).line(level, from.add(0, 0.5, 0), to.add(0, 0.5, 0), 0.8));
				}
			});
		}
	}

	/** Ink Marks: every player is marked; each mark kills everyone else within 6 blocks. Spread out (solo is safe). */
	private void inkMarks(final ServerLevel level, final LivingEntity target) {
		this.playAnim(CreatureAnim.ROAR);
		this.sound(level, SoundEvents.SQUID_SQUIRT, 4.0F, 0.4F);
		List<ServerPlayer> marked = new ArrayList<>(this.fighters(level));
		this.during(150, 5, () -> {
			for (ServerPlayer m : marked) {
				if (m.isAlive() && m.level() == level) {
					Telegraph.ring(level, m.position(), 6.0, Telegraph.PURPLE, 1.0F);
					level.sendParticles(ParticleTypes.SQUID_INK, m.getX(), m.getY() + 2.4, m.getZ(), 3, 0.2, 0.1, 0.2, 0.0);
				}
			}
		});
		this.after(150, () -> {
			for (ServerPlayer m : marked) {
				if (!m.isAlive() || m.level() != level) {
					continue;
				}
				for (ServerPlayer other : this.fighters(level)) {
					if (other != m && other.position().subtract(m.position()).horizontalDistance() <= 6.0) {
						this.lethal(level, other);
					}
				}
				RaidDamage.portion(level, m, this, 0.2F);
				level.sendParticles(ParticleTypes.SQUID_INK, m.getX(), m.getY() + 1, m.getZ(), 80, 2.5, 1.0, 2.5, 0.1);
			}
			this.sound(level, SoundEvents.GENERIC_SPLASH, 4.0F, 0.5F);
		});
	}

	/**
	 * Anchor Chains: one player must hold each anchor (as many anchors as players, up to 3) or the ship goes down. Fighters are
	 * counted again at the end, so a party that lost someone meanwhile only needs as many anchors as it has players left.
	 */
	private void anchors(final ServerLevel level, final LivingEntity target) {
		this.playAnim(CreatureAnim.SUMMON);
		this.sound(level, SoundEvents.CHAIN_PLACE, 4.0F, 0.5F);
		int n = Math.min(3, Math.max(1, this.fighters(level).size()));
		List<Vec3> anchors = this.spots(n, Arenas.POOL_RADIUS + 5, Arenas.RADIUS - 3, 4);
		this.mechanicSpots.addAll(anchors);
		Fx chain = new Fx(Fx.Kind.RING, 0xB0B8C0);
		this.during(190, 5, () -> {
			int manned = 0;
			for (Vec3 a : anchors) {
				Telegraph.ring(level, a, 2.5, 0x60FF60, 1.4F);
				chain.column(level, a, 4.0);
				if (!this.near(level, a, 2.5).isEmpty()) {
					manned++;
				}
			}
			this.tally(level, "kraken_anchors", manned, Math.min(anchors.size(), Math.max(1, this.fighters(level).size())));
		});
		this.after(190, () -> {
			long manned = anchors.stream().filter(a -> !this.near(level, a, 2.5).isEmpty()).count();
			if (manned < Math.min(anchors.size(), Math.max(1, this.fighters(level).size()))) {
				this.wipe(level, "kraken_anchors");
			} else {
				this.cleared(level, "kraken_anchors");
			}
			this.sound(level, SoundEvents.ELDER_GUARDIAN_CURSE, 4.0F, 0.5F);
		});
	}
}
