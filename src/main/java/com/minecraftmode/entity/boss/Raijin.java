package com.minecraftmode.entity.boss;

import com.minecraftmode.entity.CreatureAnim;
import com.minecraftmode.entity.combat.Attacks;
import com.minecraftmode.entity.combat.MobProjectile;
import com.minecraftmode.entity.combat.Telegraph;
import com.minecraftmode.job.skill.Fx;
import com.minecraftmode.job.skill.SkillScheduler;
import com.minecraftmode.raid.Arenas;
import com.minecraftmode.raid.RaidDamage;
import java.util.List;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.breeze.Breeze;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Raijin, Lord of the Thunderclouds (Lv 70-80, sky shrine). Phase 1: lightning strikes, the thunder drum shockwave and
 * wind blades. Phase 2 (65%): storm spirits and chain lightning. Phase 3 (30%): he crashes down on a player with a
 * thunderclap that splits four ways. Mechanics: the Eye of the Storm (stay inside the drifting calm) and Thunder Drums
 * (silence every drum in time).
 */
public class Raijin extends RaidBoss {
	private static final int SPARK = 0xFFE860;
	private static final int STORM = 0x8AB8FF;

	public Raijin(final EntityType<? extends Raijin> type, final Level level) {
		super(type, level);
	}

	@Override
	protected List<Pattern> patterns() {
		return List.of(
			spell("lightning_strike", 1, 80, 0, 12, this::lightningStrike),
			spell("thunder_drum", 1, 150, 10, 22, this::thunderDrum),
			pattern("wind_blade", 1, 100, 22, 20, this::windBlade),
			pattern("storm_spirits", 2, 420, 0, 20, (level, target) -> {
				this.playAnim(CreatureAnim.SUMMON);
				this.summonMinions(level, EntityTypes.BREEZE, 2, 4, target);
			}),
			spell("chain_lightning", 2, 180, 26, 12, this::chainLightning),
			spell("thunder_descent", 3, 260, 0, 40, this::thunderDescent)
		);
	}

	@Override
	protected List<Mechanic> mechanics() {
		return List.of(
			mechanic("raijin_storm_eye", 0.8F, 150, 240, this::stormEye),
			mechanic("raijin_thunder_drums", 0.45F, 160, 400, this::thunderDrums)
		);
	}

	@Override
	protected void ambientFx(final ServerLevel level) {
		if (this.tickCount % 5 == 0) {
			level.sendParticles(ParticleTypes.ELECTRIC_SPARK, this.getX(), this.getY(0.7), this.getZ(), 4, 1.2, 1.0, 1.2, 0.05);
		}
		if (this.tickCount % 12 == 0) {
			level.sendParticles(ParticleTypes.CLOUD, this.getX(), this.getY(), this.getZ(), 4, 1.0, 0.1, 1.0, 0.0);
		}
	}

	/** A bolt that only shows: the damage comes from the circle under it. */
	private void bolt(final ServerLevel level, final Vec3 at) {
		LightningBolt bolt = EntityTypes.LIGHTNING_BOLT.create(level, EntitySpawnReason.TRIGGERED);
		if (bolt != null) {
			bolt.snapTo(at);
			bolt.setVisualOnly(true);
			level.addFreshEntity(bolt);
		}
	}

	private void lightningStrike(final ServerLevel level, final LivingEntity target) {
		this.playAnim(CreatureAnim.CAST);
		this.sound(level, SoundEvents.LIGHTNING_BOLT_THUNDER, 2.0F, 1.2F);
		this.rain(level, this.fighters(level).size() + 2, 2.8, 20, Telegraph.PURPLE, 1.1F, e -> this.effect(e, MobEffects.SLOWNESS, 30, 1),
			at -> this.bolt(level, at));
	}

	/** He beats the great drum on his back: a shockwave rolls out, the middle is safe. */
	private void thunderDrum(final ServerLevel level, final LivingEntity target) {
		this.playAnim(CreatureAnim.SLAM);
		this.sound(level, SoundEvents.NOTE_BLOCK_BASEDRUM.value(), 4.0F, 0.5F);
		Vec3 me = this.ground(this.position());
		this.donut(level, me, 3.0, 10.0, 22, Telegraph.PURPLE, 1.3F, e -> Attacks.knock(e, me, 1.2, 0.5));
	}

	/** A blade of wind along a strip toward the target. */
	private void windBlade(final ServerLevel level, final LivingEntity target) {
		this.playAnim(CreatureAnim.ATTACK);
		Vec3 from = this.ground(this.position());
		Vec3 to = reach(from, target.position(), 18);
		this.sound(level, SoundEvents.BREEZE_SHOOT, 2.0F, 0.6F);
		this.line(level, from, to, 2.5, Telegraph.MIN_WARNING, Telegraph.ORANGE, 1.2F, e -> Attacks.knock(e, from, 1.0, 0.3),
			() -> new Fx(Fx.Kind.SLASH, STORM).line(level, from.add(0, 1.0, 0), to.add(0, 1.0, 0), 0.8));
	}

	private void chainLightning(final ServerLevel level, final LivingEntity target) {
		this.playAnim(CreatureAnim.CAST);
		for (MobProjectile p : this.volley(level, target, 3, 50.0F, 1.0F, new ItemStack(Items.GLOWSTONE_DUST), Fx.Kind.BOLT, SPARK, 0.0F, 0.9F, true)) {
			p.effect(MobEffects.SLOWNESS, 40, 1);
		}
		this.sound(level, SoundEvents.TRIDENT_THUNDER.value(), 1.5F, 1.4F);
	}

	/** He leaps into the clouds and crashes down on a player; the thunderclap splits four ways. */
	private void thunderDescent(final ServerLevel level, final LivingEntity target) {
		this.playAnim(CreatureAnim.LEAP);
		Vec3 at = this.ground(target.position());
		Vec3 d = at.subtract(this.position());
		Attacks.push(this, new Vec3(d.x * 0.16, 1.1 + Math.min(0.6, d.horizontalDistance() * 0.03), d.z * 0.16));
		this.sound(level, SoundEvents.LIGHTNING_BOLT_THUNDER, 3.0F, 0.6F);
		this.circle(level, at, 6.0, 26, Telegraph.RED, 1.8F, e -> Attacks.knock(e, at, 1.4, 0.6), () -> {
			this.playAnim(CreatureAnim.SLAM);
			this.bolt(level, at);
			this.boom(level, at, 6.0);
		});
		SkillScheduler.schedule(22, this.asSpell(() -> {
			if (!this.alive()) {
				return;
			}
			float base = this.random.nextFloat() * 90.0F;
			for (int k = 0; k < 4; k++) {
				Vec3 to = at.add(Vec3.directionFromRotation(0.0F, base + k * 90.0F).scale(Arenas.RADIUS));
				this.line(level, at, to, 2.4, 22, Telegraph.PURPLE, 1.3F, e -> this.effect(e, MobEffects.SLOWNESS, 40, 1),
					() -> new Fx(Fx.Kind.BOLT, SPARK).line(level, at.add(0, 0.8, 0), to.add(0, 0.8, 0), 0.8));
			}
		}));
	}

	/**
	 * The Eye of the Storm: a calm circle drifts across the shrine. Lightning strikes everyone outside it every two seconds,
	 * and the last strike, when the storm closes, is lethal.
	 */
	private void stormEye(final ServerLevel level, final LivingEntity target) {
		this.playAnim(CreatureAnim.ROAR);
		this.sound(level, SoundEvents.LIGHTNING_BOLT_THUNDER, 4.0F, 0.5F);
		Vec3 c = this.arenaCenter();
		double a = this.random.nextDouble() * Math.PI * 2;
		double r = Arenas.RADIUS - 7;
		Vec3 from = c.add(Math.cos(a) * r, 0, Math.sin(a) * r);
		Vec3 to = c.add(-Math.cos(a) * r, 0, -Math.sin(a) * r);
		int ticks = 230;
		double radius = 4.5;
		Vec3[] eye = {from};
		int[] elapsed = {0};
		this.mechanicSpots.add(from);
		this.during(ticks, 5, () -> {
			elapsed[0] += 5;
			int t = Math.min(ticks, elapsed[0]);
			eye[0] = from.add(to.subtract(from).scale(Math.min(1.0, t / (double)(ticks - 30))));
			this.mechanicSpots.clear();
			this.mechanicSpots.add(eye[0]);
			Telegraph.ring(level, eye[0], radius, 0x60FF60, 1.4F);
			level.sendParticles(ParticleTypes.CLOUD, eye[0].x, eye[0].y + 3.0, eye[0].z, 6, radius * 0.5, 0.3, radius * 0.5, 0.0);
		});
		for (int strike = 40; strike < ticks; strike += 40) {
			this.after(strike, () -> {
				for (ServerPlayer p : this.fighters(level)) {
					if (p.position().subtract(eye[0]).horizontalDistance() > radius) {
						this.bolt(level, p.position());
						RaidDamage.portion(level, p, this, 0.25F);
					}
				}
			});
		}
		this.after(ticks, () -> {
			for (ServerPlayer p : this.fighters(level)) {
				if (p.position().subtract(eye[0]).horizontalDistance() > radius + 0.3) {
					this.bolt(level, p.position());
					this.lethal(level, p);
				}
			}
		});
	}

	/** Thunder Drums: one more drum spirit than the party; silence them all within 18 seconds or the thunderclap ends the fight. */
	private void thunderDrums(final ServerLevel level, final LivingEntity target) {
		this.playAnim(CreatureAnim.SUMMON);
		this.sound(level, SoundEvents.NOTE_BLOCK_BASEDRUM.value(), 4.0F, 0.4F);
		int n = this.fighters(level).size() + 1;
		List<Vec3> at = this.spots(n, 6, Arenas.RADIUS - 4, 4);
		List<Breeze> drums = this.targets(level, EntityTypes.BREEZE, at, 60.0F, "raid.minecraft_mode.thunder_drum");
		boolean[] done = {false};
		this.during(360, 10, () -> {
			long alive = drums.stream().filter(Breeze::isAlive).count();
			this.tally(level, "raijin_thunder_drums", (int)(drums.size() - alive), drums.size());
			if (this.tickCount % 20 == 0) {
				this.sound(level, SoundEvents.NOTE_BLOCK_BASEDRUM.value(), 3.0F, 0.5F + 0.1F * alive);
			}
			if (alive == 0 && !done[0]) {
				done[0] = true;
				this.cleared(level, "raijin_thunder_drums");
				this.endMechanic();
			}
		});
		this.after(360, () -> {
			if (drums.stream().anyMatch(Breeze::isAlive)) {
				drums.forEach(d -> d.discard());
				for (ServerPlayer p : this.fighters(level)) {
					this.bolt(level, p.position());
				}
				this.wipe(level, "raijin_thunder_drums");
			}
		});
	}
}
