package com.minecraftmode.entity.boss;

import java.util.ArrayList;
import com.minecraftmode.raid.RaidDamage;
import com.minecraftmode.entity.CreatureAnim;
import com.minecraftmode.entity.combat.Attacks;
import com.minecraftmode.entity.combat.MobProjectile;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Aethryx, the void dragon (Lv 90-100, floating isles) - the last raid. Four phases: void breath,
 * wing buffets and tail lashes; then (75%) seeking void orbs and dive bombs; (50%) rifts that drag
 * players in before bursting, and vex swarms; (25%) Starfall over the whole island and Collapse,
 * which only the island's heart survives.
 */
public class Aethryx extends RaidBoss {
	private static final int VOID = 0xB080FF;
	private static final int STAR = 0xFFF2B0;

	public Aethryx(final EntityType<? extends Aethryx> type, final Level level) {
		super(type, level);
	}

	@Override
	protected boolean flies() {
		return true;
	}

	@Override
	protected List<Pattern> patterns() {
		return List.of(
			pattern("void_breath", 1, 150, 0, 34, this::voidBreath),
			pattern("wing_buffet", 1, 170, 14, 22, this::wingBuffet),
			pattern("tail_lash", 1, 110, 0, 18, this::tailLash),
			pattern("void_orbs", 2, 160, 0, 10, this::voidOrbs),
			pattern("dive_bomb", 2, 220, 0, 40, this::diveBomb),
			pattern("rifts", 3, 260, 0, 20, this::rifts),
			pattern("vex_swarm", 3, 420, 0, 20, (level, target) -> {
				this.playAnim(CreatureAnim.SUMMON);
				this.summonMinions(level, EntityTypes.VEX, 3, 6, target);
			}),
			pattern("starfall", 4, 210, 0, 20, this::starfall),
			pattern("collapse", 4, 380, 0, 50, this::collapse)
		);
	}

	@Override
	protected List<Mechanic> mechanics() {
		return List.of(
			mechanic("aethryx_event_horizon", 0.9F, 120, 180, this::eventHorizon),
			mechanic("aethryx_seals", 0.6F, 150, 210, this::seals),
			mechanic("aethryx_judgment", 0.3F, 130, 180, this::judgment)
		);
	}

	@Override
	protected void ambientFx(final ServerLevel level) {
		if (this.tickCount % 4 == 0) {
			level.sendParticles(ParticleTypes.REVERSE_PORTAL, this.getX(), this.getY(0.5), this.getZ(), 6, this.getBbWidth() * 0.5, 0.6, this.getBbWidth() * 0.5, 0.02);
		}
	}

	private void voidBreath(final ServerLevel level, final LivingEntity target) {
		this.playAnim(CreatureAnim.BREATH);
		this.sound(level, SoundEvents.ENDER_DRAGON_GROWL, 3.0F, 0.9F);
		this.cone(level, this.ground(this.position()), target.position(), 17, 28, 16, Telegraph.PURPLE, 0.5F, 5, new Fx(Fx.Kind.ORB, VOID),
			e -> this.effect(e, MobEffects.DARKNESS, 60, 0));
	}

	private void wingBuffet(final ServerLevel level, final LivingEntity target) {
		this.playAnim(CreatureAnim.SLAM);
		this.sound(level, SoundEvents.ENDER_DRAGON_FLAP, 4.0F, 0.6F);
		Vec3 at = this.ground(this.position());
		this.circle(level, at, 8.0, 22, Telegraph.RED, 1.2F, e -> Attacks.knock(e, at, 2.2, 0.8), () -> {
			level.sendParticles(ParticleTypes.CLOUD, at.x, at.y + 0.5, at.z, 80, 6, 0.3, 6, 0.2);
			this.sound(level, at, SoundEvents.ENDER_DRAGON_FLAP, 4.0F, 0.4F);
		});
	}

	private void tailLash(final ServerLevel level, final LivingEntity target) {
		this.playAnim(CreatureAnim.SPIN);
		Vec3 from = this.ground(this.position());
		Vec3 to = reach(from, target.position(), 16);
		this.line(level, from, to, 3.4, 18, Telegraph.RED, 1.5F, e -> Attacks.knock(e, from, 1.4, 0.5),
			() -> new Fx(Fx.Kind.SLASH, VOID).line(level, from.add(0, 1, 0), to.add(0, 1, 0), 0.8));
		this.sound(level, SoundEvents.ENDER_DRAGON_HURT, 2.0F, 0.6F);
	}

	private void voidOrbs(final ServerLevel level, final LivingEntity target) {
		this.playAnim(CreatureAnim.CAST);
		for (MobProjectile p : this.volley(level, target, 5, 90.0F, 0.9F, new ItemStack(Items.ENDER_PEARL), Fx.Kind.ORB, VOID, 0.0F, 0.7F, true)) {
			p.effect(MobEffects.LEVITATION, 30, 0).maxAge(140);
		}
		this.sound(level, SoundEvents.ENDER_DRAGON_SHOOT, 2.5F, 0.8F);
	}

	private void diveBomb(final ServerLevel level, final LivingEntity target) {
		Vec3 at = this.ground(target.position());
		this.playAnim(CreatureAnim.DIVE);
		this.sound(level, SoundEvents.PHANTOM_SWOOP, 4.0F, 0.4F);
		this.setBusy(44);
		this.circle(level, at, 5.5, 26, Telegraph.RED, 2.0F, e -> Attacks.knock(e, at, 1.6, 0.8), () -> {
			this.boom(level, at, 5.5);
			new Fx(Fx.Kind.RING, VOID).circle(level, Fx.Kind.RING, at.add(0, 0.3, 0), 5.5);
		});
		SkillScheduler.schedule(12, () -> {
			if (this.alive()) {
				this.dash(at.add(0, 0.5, 0), 14, () -> level.sendParticles(ParticleTypes.PORTAL, this.getX(), this.getY(0.5), this.getZ(), 10, 1, 1, 1, 0.3));
			}
		});
	}

	private void rifts(final ServerLevel level, final LivingEntity target) {
		this.playAnim(CreatureAnim.CAST);
		this.sound(level, SoundEvents.END_PORTAL_SPAWN, 1.5F, 1.4F);
		for (ServerPlayer p : this.fighters(level)) {
			Vec3 at = this.ground(p.position()).add((this.random.nextDouble() - 0.5) * 3, 0, (this.random.nextDouble() - 0.5) * 3);
			for (int t = 0; t < 36; t += 4) {
				SkillScheduler.schedule(t + 1, () -> {
					if (this.alive()) {
						this.pull(level, at, 6.0, 0.25);
						level.sendParticles(ParticleTypes.REVERSE_PORTAL, at.x, at.y + 0.5, at.z, 20, 1.0, 0.3, 1.0, 0.05);
					}
				});
			}
			this.circle(level, at, 3.0, 40, Telegraph.PURPLE, 1.6F, e -> this.effect(e, MobEffects.LEVITATION, 20, 1), () -> this.boom(level, at, 3.0));
		}
	}

	private void starfall(final ServerLevel level, final LivingEntity target) {
		this.playAnim(CreatureAnim.ROAR);
		this.sound(level, SoundEvents.ENDER_DRAGON_GROWL, 4.0F, 0.6F);
		this.rain(level, 12, 3.0, 34, Telegraph.ORANGE, 1.8F, null, at -> {
			this.boom(level, at, 3.0);
			new Fx(Fx.Kind.SPARK, STAR).burst(level, at.add(0, 0.5, 0), 25, 1.5, 0.2);
		});
		for (int i = 0; i < 10; i++) {
			Vec3 drop = this.arenaCenter().add((this.random.nextDouble() - 0.5) * Arenas.RADIUS * 1.6, 0, (this.random.nextDouble() - 0.5) * Arenas.RADIUS * 1.6);
			this.fallFromSky(level, drop, new ItemStack(Items.NETHER_STAR), Fx.Kind.SPARK, STAR, 34);
		}
	}

	private void collapse(final ServerLevel level, final LivingEntity target) {
		this.playAnim(CreatureAnim.ROAR);
		this.sound(level, SoundEvents.WITHER_SPAWN, 2.0F, 0.5F);
		Vec3 c = this.arenaCenter();
		this.donut(level, c, 6.0, Arenas.RADIUS + 2, 50, Telegraph.PURPLE, 3.0F, e -> Attacks.push(e, new Vec3(0, 0.8, 0)));
		this.title(level, net.minecraft.network.chat.Component.empty(),
			net.minecraft.network.chat.Component.translatable("entity.minecraft_mode.aethryx.collapse").withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE));
	}

	/** Event Horizon: a black hole drags everyone in and implodes; be more than 11 blocks from the middle. */
	private void eventHorizon(final ServerLevel level, final LivingEntity target) {
		this.playAnim(CreatureAnim.ROAR);
		this.sound(level, SoundEvents.END_PORTAL_SPAWN, 3.0F, 0.5F);
		Vec3 c = this.arenaCenter();
		this.during(160, 4, () -> {
			for (ServerPlayer p : this.fighters(level)) {
				Vec3 in = c.subtract(p.position()).multiply(1, 0, 1);
				if (in.lengthSqr() > 0.25) {
					Attacks.push(p, p.getDeltaMovement().add(in.normalize().scale(0.12)));
				}
			}
			Telegraph.ring(level, c, 11.0, Telegraph.PURPLE, 1.3F);
			Telegraph.ring(level, c, 13.0, 0x60FF60, 1.0F);
			level.sendParticles(ParticleTypes.REVERSE_PORTAL, c.x, c.y + 1.5, c.z, 60, 2.5, 1.0, 2.5, 0.2);
		});
		this.after(160, () -> {
			for (ServerPlayer p : this.fighters(level)) {
				if (p.position().subtract(c).horizontalDistance() <= 11.0) {
					this.lethal(level, p);
				}
			}
			level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, c.x, c.y + 1, c.z, 3, 2, 0.5, 2, 0.0);
			this.sound(level, c, SoundEvents.GENERIC_EXPLODE.value(), 4.0F, 0.5F);
		});
	}

	/** Starfall Seals: every seal (one per player, up to 4) needs someone standing on it when the stars fall. */
	private void seals(final ServerLevel level, final LivingEntity target) {
		this.playAnim(CreatureAnim.CAST);
		this.sound(level, SoundEvents.BEACON_ACTIVATE, 4.0F, 0.6F);
		int n = Math.min(4, Math.max(1, this.fighters(level).size()));
		List<Vec3> seals = this.spots(n, 6, Arenas.RADIUS - 3, 5);
		this.mechanicSpots.addAll(seals);
		Fx star = new Fx(Fx.Kind.SPARK, STAR);
		this.during(200, 5, () -> {
			int manned = 0;
			for (Vec3 s : seals) {
				Telegraph.ring(level, s, 2.5, 0xFFD24A, 1.4F);
				star.column(level, s, 5.0);
				if (!this.near(level, s, 2.5).isEmpty()) {
					manned++;
				}
			}
			this.tally(level, "aethryx_seals", manned, seals.size());
		});
		this.after(200, () -> {
			long manned = seals.stream().filter(s -> !this.near(level, s, 2.5).isEmpty()).count();
			if (manned < seals.size()) {
				this.wipe(level, "aethryx_seals");
			} else {
				this.cleared(level, "aethryx_seals");
			}
			for (Vec3 s : seals) {
				this.fallFromSky(level, s, new ItemStack(Items.NETHER_STAR), Fx.Kind.SPARK, STAR, 8);
			}
		});
	}

	/** Void Judgment: the marked player must not stand alone; 60% of the party (at least one) gathers on them to split the blow. */
	private void judgment(final ServerLevel level, final LivingEntity target) {
		List<ServerPlayer> fighters = this.fighters(level);
		if (fighters.isEmpty()) {
			return;
		}
		ServerPlayer marked = fighters.get(this.random.nextInt(fighters.size()));
		this.playAnim(CreatureAnim.CAST);
		this.sound(level, SoundEvents.WITHER_AMBIENT, 4.0F, 0.5F);
		this.during(170, 5, () -> {
			if (marked.isAlive() && marked.level() == level) {
				Telegraph.ring(level, marked.position(), 4.0, Telegraph.PURPLE, 1.4F);
				level.sendParticles(ParticleTypes.REVERSE_PORTAL, marked.getX(), marked.getY() + 2.4, marked.getZ(), 6, 0.3, 0.2, 0.3, 0.02);
				this.tally(level, "aethryx_judgment", this.near(level, marked.position(), 4.0).size(), Gorvath.need(this.fighters(level).size()));
			}
		});
		this.after(170, () -> {
			if (!marked.isAlive() || marked.level() != level) {
				return;
			}
			List<ServerPlayer> together = this.near(level, marked.position(), 4.0);
			if (together.size() < Gorvath.need(this.fighters(level).size())) {
				this.wipe(level, "aethryx_judgment");
			} else {
				for (ServerPlayer p : together) {
					RaidDamage.portion(level, p, this, 0.3F);
				}
				this.cleared(level, "aethryx_judgment");
			}
			this.boom(level, marked.position(), 4.0);
		});
	}
}
