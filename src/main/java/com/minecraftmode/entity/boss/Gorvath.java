package com.minecraftmode.entity.boss;

import net.minecraft.core.BlockPos;
import com.minecraftmode.raid.RaidDamage;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.level.ClipContext;
import net.minecraft.server.level.ServerPlayer;
import com.minecraftmode.entity.CreatureAnim;
import com.minecraftmode.entity.combat.Attacks;
import com.minecraftmode.entity.combat.Telegraph;
import com.minecraftmode.job.skill.Fx;
import com.minecraftmode.job.skill.SkillScheduler;
import com.minecraftmode.raid.Arenas;
import java.util.List;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * Gorvath, the mountain colossus (Lv 35-45, summit). Slow and enormous. Phase 1: ground slams,
 * fissures and boulders. Phase 2 (70%): rockfall all over the arena and a shockwave that only spares
 * those who stay close. Phase 3 (35%): the avalanche charge across the summit and double fissures.
 */
public class Gorvath extends RaidBoss {
	private static final int STONE = 0xA09080;

	public Gorvath(final EntityType<? extends Gorvath> type, final Level level) {
		super(type, level);
	}

	@Override
	protected List<Pattern> patterns() {
		return List.of(
			pattern("ground_slam", 1, 110, 8, 26, this::groundSlam),
			pattern("fissure", 1, 130, 24, 22, this::fissure),
			pattern("boulder", 1, 90, 32, 12, this::boulder),
			pattern("rockfall", 2, 220, 0, 20, this::rockfall),
			pattern("shockwave", 2, 280, 0, 40, this::shockwave),
			pattern("avalanche", 3, 260, 0, 40, this::avalanche)
		);
	}

	@Override
	protected List<Mechanic> mechanics() {
		return List.of(
			mechanic("gorvath_rockslide", 0.8F, 150, 170, this::rockslide),
			mechanic("gorvath_shoulder", 0.5F, 150, 180, this::shoulder)
		);
	}

	@Override
	protected void ambientFx(final ServerLevel level) {
		if (this.tickCount % 8 == 0 && this.getDeltaMovement().horizontalDistanceSqr() > 1.0E-3) {
			level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.STONE.defaultBlockState()), this.getX(), this.getY() + 0.1, this.getZ(), 12, 1.2, 0.1,
				1.2, 0.1);
		}
	}

	private void groundSlam(final ServerLevel level, final LivingEntity target) {
		this.playAnim(CreatureAnim.SLAM);
		Vec3 at = this.ground(this.position());
		this.sound(level, SoundEvents.IRON_GOLEM_ATTACK, 3.0F, 0.4F);
		this.circle(level, at, 6.5, 24, Telegraph.RED, 1.5F, e -> Attacks.push(e, new Vec3(0, 1.0, 0)), () -> {
			this.boom(level, at, 6.5);
			new Fx(Fx.Kind.RING, STONE).circle(level, Fx.Kind.RING, at.add(0, 0.2, 0), 6.5);
		});
	}

	private void fissure(final ServerLevel level, final LivingEntity target) {
		this.playAnim(CreatureAnim.SLAM);
		this.sound(level, SoundEvents.RAVAGER_ROAR, 3.0F, 0.5F);
		Vec3 from = this.ground(this.position());
		this.fissureLine(level, from, this.ground(target.position()), 0);
		if (this.phase() >= 3) {
			Vec3 side = this.ground(target.position()).subtract(from).yRot((float)Math.toRadians(35));
			this.fissureLine(level, from, from.add(side), 6);
			this.fissureLine(level, from, from.add(side.yRot((float)Math.toRadians(-70))), 6);
		}
	}

	private void fissureLine(final ServerLevel level, final Vec3 from, final Vec3 toward, final int extraDelay) {
		Vec3 to = reach(from, toward, 20);
		SkillScheduler.schedule(Math.max(1, extraDelay), () -> {
			if (!this.alive()) {
				return;
			}
			this.line(level, from, to, 3.0, 22, Telegraph.RED, 1.3F, e -> Attacks.push(e, new Vec3(0, 0.9, 0)), () -> {
				Vec3 step = to.subtract(from).scale(1.0 / 10);
				for (int i = 1; i <= 10; i++) {
					Vec3 p = from.add(step.scale(i));
					level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.STONE.defaultBlockState()), p.x, p.y + 0.3, p.z, 12, 0.6, 0.4, 0.6, 0.2);
				}
				this.sound(level, to, SoundEvents.GENERIC_EXPLODE.value(), 1.4F, 0.6F);
			});
		});
	}

	private void boulder(final ServerLevel level, final LivingEntity target) {
		this.playAnim(CreatureAnim.ATTACK);
		Vec3 at = this.ground(target.position());
		this.sound(level, SoundEvents.STONE_BREAK, 3.0F, 0.5F);
		this.fallFromSky(level, at, new ItemStack(Items.COBBLESTONE), Fx.Kind.SMOKE, STONE, 26);
		this.circle(level, at, 3.0, 26, Telegraph.ORANGE, 1.2F, e -> this.effect(e, MobEffects.SLOWNESS, 40, 1), () -> this.boom(level, at, 3.0));
	}

	private void rockfall(final ServerLevel level, final LivingEntity target) {
		this.playAnim(CreatureAnim.ROAR);
		this.sound(level, SoundEvents.RAVAGER_ROAR, 3.0F, 0.4F);
		this.rain(level, 8, 2.6, 30, Telegraph.ORANGE, 1.2F, null, at -> {
			this.boom(level, at, 2.6);
			level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.COBBLESTONE.defaultBlockState()), at.x, at.y + 0.5, at.z, 20, 0.8, 0.5, 0.8, 0.2);
		});
		for (int i = 0; i < 8; i++) {
			Vec3 drop = this.arenaCenter().add((this.random.nextDouble() - 0.5) * Arenas.RADIUS * 1.6, 0, (this.random.nextDouble() - 0.5) * Arenas.RADIUS * 1.6);
			this.fallFromSky(level, drop, new ItemStack(Items.COBBLESTONE), Fx.Kind.SMOKE, STONE, 30);
		}
	}

	private void shockwave(final ServerLevel level, final LivingEntity target) {
		this.playAnim(CreatureAnim.SLAM);
		Vec3 at = this.ground(this.position());
		this.sound(level, SoundEvents.WARDEN_SONIC_CHARGE, 3.0F, 0.5F);
		this.donut(level, at, 4.5, 18, 38, Telegraph.RED, 1.7F, e -> Attacks.knock(e, at, 1.5, 0.6));
	}

	private void avalanche(final ServerLevel level, final LivingEntity target) {
		Vec3 from = this.ground(this.position());
		Vec3 to = reach(from, target.position(), 26);
		if (this.home() != null) {
			Vec3 c = this.arenaCenter();
			if (to.subtract(c).horizontalDistance() > Arenas.RADIUS - 2) {
				to = c.add(to.subtract(c).multiply(1, 0, 1).normalize().scale(Arenas.RADIUS - 3));
			}
		}
		final Vec3 end = to;
		this.playAnim(CreatureAnim.CHARGE);
		this.getLookControl().setLookAt(target);
		this.setBusy(40);
		this.sound(level, SoundEvents.RAVAGER_ROAR, 3.0F, 0.6F);
		Telegraph.line(level, from, end, 5.0, 22, Telegraph.RED, () -> {
			if (!this.alive()) {
				return;
			}
			java.util.Set<java.util.UUID> hit = new java.util.HashSet<>();
			this.dash(end, 12, () -> {
				level.sendParticles(ParticleTypes.CLOUD, this.getX(), this.getY() + 0.3, this.getZ(), 6, 1.0, 0.2, 1.0, 0.05);
				for (LivingEntity e : Attacks.inCircle(level, this.position(), 2.8, this.getBbHeight())) {
					if (hit.add(e.getUUID())) {
						Attacks.hit(this, e, this.dmg(2.0F));
						Attacks.knock(e, this.position(), 1.8, 0.7);
					}
				}
			});
		});
	}

	/** Rockslide: the peak comes down on everyone Gorvath can see. Hide behind a pillar. */
	private void rockslide(final ServerLevel level, final LivingEntity target) {
		this.playAnim(CreatureAnim.CAST);
		this.sound(level, SoundEvents.RAVAGER_ROAR, 4.0F, 0.4F);
		Vec3 c = this.arenaCenter();
		this.during(150, 5, () -> {
			if (this.home() != null) {
				for (BlockPos pillar : Arenas.pillars(this.home())) {
					Vec3 away = Vec3.atBottomCenterOf(pillar).subtract(this.position()).multiply(1, 0, 1).normalize().scale(2.5);
					Vec3 behind = Vec3.atBottomCenterOf(pillar).add(away);
					level.sendParticles(ParticleTypes.HAPPY_VILLAGER, behind.x, behind.y + 0.5, behind.z, 2, 0.4, 0.3, 0.4, 0.0);
				}
			}
			for (int k = 0; k < 6; k++) {
				double a = this.random.nextDouble() * Math.PI * 2;
				double r = this.random.nextDouble() * Arenas.RADIUS;
				level.sendParticles(new BlockParticleOption(ParticleTypes.FALLING_DUST, Blocks.STONE.defaultBlockState()), c.x + Math.cos(a) * r, c.y + 14, c.z + Math.sin(a) * r,
					2, 0.5, 0.2, 0.5, 0.0);
			}
		});
		this.after(150, () -> {
			Vec3 eye = this.getEyePosition();
			for (ServerPlayer p : this.fighters(level)) {
				HitResult hit = level.clip(new ClipContext(eye, p.getEyePosition(), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
				if (hit.getType() == HitResult.Type.MISS) {
					this.lethal(level, p);
				}
			}
			for (int i = 0; i < 12; i++) {
				Vec3 drop = c.add((this.random.nextDouble() - 0.5) * Arenas.RADIUS * 1.6, 0, (this.random.nextDouble() - 0.5) * Arenas.RADIUS * 1.6);
				this.fallFromSky(level, drop, new ItemStack(Items.COBBLESTONE), Fx.Kind.SMOKE, STONE, 12);
			}
			this.boom(level, c, 8.0);
		});
	}

	/** Shoulder the Mountain: a falling peak that 60% of the party (at least one) must hold up together. */
	private void shoulder(final ServerLevel level, final LivingEntity target) {
		this.playAnim(CreatureAnim.SLAM);
		this.sound(level, SoundEvents.IRON_GOLEM_ATTACK, 4.0F, 0.3F);
		Vec3 spot = this.spots(1, 6, Arenas.RADIUS - 6, 8).getFirst();
		this.mechanicSpots.add(spot);
		this.during(170, 5, () -> {
			Telegraph.ring(level, spot, 4.0, 0x60FF60, 1.4F);
			level.sendParticles(new BlockParticleOption(ParticleTypes.FALLING_DUST, Blocks.STONE.defaultBlockState()), spot.x, spot.y + 12, spot.z, 6, 2.0, 0.3, 2.0, 0.0);
			this.tally(level, "gorvath_shoulder", this.near(level, spot, 4.0).size(), need(this.fighters(level).size()));
		});
		this.after(170, () -> {
			List<ServerPlayer> inside = this.near(level, spot, 4.0);
			if (inside.size() < need(this.fighters(level).size())) {
				this.wipe(level, "gorvath_shoulder");
			} else {
				for (ServerPlayer p : inside) {
					RaidDamage.portion(level, p, this, 0.25F);
				}
				this.cleared(level, "gorvath_shoulder");
			}
			this.boom(level, spot, 4.0);
		});
	}

	/** Players a team mechanic needs: 60% of the party, at least one (solo is easy on purpose). */
	static int need(final int fighters) {
		return Math.max(1, (int)Math.ceil(fighters * 0.6));
	}
}
