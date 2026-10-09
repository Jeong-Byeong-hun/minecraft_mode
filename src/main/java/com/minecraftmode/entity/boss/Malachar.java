package com.minecraftmode.entity.boss;

import net.minecraft.world.entity.monster.skeleton.WitherSkeleton;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import java.util.UUID;
import java.util.Set;
import java.util.Map;
import java.util.HashSet;
import java.util.HashMap;
import java.util.ArrayList;
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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Malachar, the lich king (Lv 80-90, sanctum of the dead). Phase 1: homing shadow bolts, bone spikes
 * under every player and a life-draining beam. Phase 2 (70%): raises the dead, blinks across the
 * sanctum leaving a soul bomb behind, and curses the party. Phase 3 (35%): Death Nova (safe only at
 * his feet) and Requiem - rotating beams of death.
 */
public class Malachar extends RaidBoss {
	private static final int SOUL = 0x5AD8E8;
	private static final int SHADOW = 0x8A3CD0;

	public Malachar(final EntityType<? extends Malachar> type, final Level level) {
		super(type, level);
	}

	@Override
	protected List<Pattern> patterns() {
		return List.of(
			pattern("shadow_bolts", 1, 64, 0, 10, this::shadowBolts),
			pattern("bone_spikes", 1, 140, 0, 14, this::boneSpikes),
			pattern("soul_drain", 1, 190, 26, 24, this::soulDrain),
			pattern("raise_dead", 2, 400, 0, 24, this::raiseDead),
			pattern("blink", 2, 170, 0, 6, this::blink),
			pattern("curse", 2, 320, 0, 16, this::curse),
			pattern("death_nova", 3, 280, 0, 44, this::deathNova),
			pattern("requiem", 3, 320, 0, 60, this::requiem)
		);
	}

	@Override
	protected List<Mechanic> mechanics() {
		return List.of(
			mechanic("malachar_stillness", 0.85F, 120, 150, this::stillness),
			mechanic("malachar_phylactery", 0.6F, 160, 420, this::phylactery),
			mechanic("malachar_doom", 0.35F, 140, 220, this::doom)
		);
	}

	@Override
	protected void ambientFx(final ServerLevel level) {
		if (this.tickCount % 4 == 0) {
			level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, this.getX(), this.getY(0.2), this.getZ(), 3, 0.6, 0.2, 0.6, 0.01);
		}
	}

	private void shadowBolts(final ServerLevel level, final LivingEntity target) {
		this.playAnim(CreatureAnim.CAST);
		int count = this.phase() >= 3 ? 6 : 4;
		for (MobProjectile p : this.volley(level, target, count, 70.0F, 0.9F, new ItemStack(Items.WITHER_SKELETON_SKULL), Fx.Kind.SMOKE, SHADOW, 0.0F, 0.8F, true)) {
			p.effect(MobEffects.WITHER, 60, 0).maxAge(120);
		}
		this.sound(level, SoundEvents.WITHER_SHOOT, 2.0F, 0.8F);
	}

	private void boneSpikes(final ServerLevel level, final LivingEntity target) {
		this.playAnim(CreatureAnim.CAST);
		this.sound(level, SoundEvents.SKELETON_AMBIENT, 2.5F, 0.5F);
		Fx bone = new Fx(Fx.Kind.SHARD, 0xE8E0C8);
		for (ServerPlayer p : this.fighters(level)) {
			Vec3 at = this.ground(p.position());
			this.circle(level, at, 2.6, 22, Telegraph.RED, 1.4F, e -> Attacks.push(e, new Vec3(0, 0.9, 0)), () -> {
				for (int i = 0; i < 8; i++) {
					double a = Math.PI * 2 * i / 8;
					bone.column(level, at.add(Math.cos(a) * 1.6, 0, Math.sin(a) * 1.6), 2.5);
				}
				this.sound(level, at, SoundEvents.SKELETON_HURT, 1.5F, 0.5F);
			});
		}
	}

	private void soulDrain(final ServerLevel level, final LivingEntity target) {
		this.playAnim(CreatureAnim.BREATH);
		Vec3 from = this.ground(this.position());
		Vec3 to = reach(from, target.position(), 24);
		this.sound(level, SoundEvents.SOUL_ESCAPE.value(), 3.0F, 0.5F);
		this.line(level, from, to, 2.2, 20, Telegraph.PURPLE, 1.2F, e -> {
			this.effect(e, MobEffects.WITHER, 100, 1);
			this.heal(this.getMaxHealth() * 0.004F);
		}, () -> new Fx(Fx.Kind.ORB, SOUL).line(level, from.add(0, 1.2, 0), to.add(0, 1.2, 0), 0.5));
	}

	private void raiseDead(final ServerLevel level, final LivingEntity target) {
		this.playAnim(CreatureAnim.SUMMON);
		this.summonMinions(level, EntityTypes.WITHER_SKELETON, 2, 6, target);
		this.summonMinions(level, EntityTypes.ZOMBIE, 2, 6, target);
	}

	private void blink(final ServerLevel level, final LivingEntity target) {
		Vec3 old = this.ground(this.position());
		Vec3 c = this.arenaCenter();
		for (int tries = 0; tries < 8; tries++) {
			double a = this.random.nextDouble() * Math.PI * 2;
			double r = 4 + this.random.nextDouble() * (Arenas.RADIUS - 8);
			Vec3 to = c.add(Math.cos(a) * r, 0, Math.sin(a) * r);
			AABB box = this.getBoundingBox().move(to.subtract(this.position()));
			if (level.noCollision(this, box)) {
				level.sendParticles(ParticleTypes.SOUL, this.getX(), this.getY(0.5), this.getZ(), 40, 0.6, 1.0, 0.6, 0.05);
				this.teleportTo(to.x, to.y, to.z);
				this.sound(level, SoundEvents.ENDERMAN_TELEPORT, 2.0F, 0.5F);
				break;
			}
		}
		this.circle(level, old, 3.5, 24, Telegraph.PURPLE, 1.5F, e -> this.effect(e, MobEffects.WITHER, 80, 1), () -> this.boom(level, old, 3.5));
	}

	private void curse(final ServerLevel level, final LivingEntity target) {
		this.playAnim(CreatureAnim.ROAR);
		this.sound(level, SoundEvents.ELDER_GUARDIAN_CURSE, 2.5F, 0.6F);
		for (ServerPlayer p : this.fighters(level)) {
			this.effect(p, MobEffects.WEAKNESS, 120, 0);
			this.effect(p, MobEffects.SLOWNESS, 80, 0);
			level.sendParticles(ParticleTypes.SOUL, p.getX(), p.getY(1.0), p.getZ(), 12, 0.4, 0.4, 0.4, 0.02);
		}
	}

	private void deathNova(final ServerLevel level, final LivingEntity target) {
		this.playAnim(CreatureAnim.CAST);
		this.sound(level, SoundEvents.WITHER_SPAWN, 1.5F, 0.7F);
		Vec3 at = this.ground(this.position());
		this.donut(level, at, 4.0, Arenas.RADIUS + 2, 44, Telegraph.PURPLE, 2.2F, e -> this.effect(e, MobEffects.WITHER, 100, 1));
	}

	private void requiem(final ServerLevel level, final LivingEntity target) {
		this.playAnim(CreatureAnim.SPIN);
		this.sound(level, SoundEvents.WARDEN_SONIC_CHARGE, 3.0F, 0.6F);
		float start = this.random.nextFloat() * 90.0F;
		for (int wave = 0; wave < 3; wave++) {
			final float base = start + wave * 30.0F;
			SkillScheduler.schedule(wave * 14 + 1, () -> {
				if (!this.alive()) {
					return;
				}
				Vec3 from = this.ground(this.position());
				for (int k = 0; k < 4; k++) {
					Vec3 to = from.add(Vec3.directionFromRotation(0.0F, base + k * 90.0F).scale(Arenas.RADIUS));
					this.line(level, from, to, 2.4, 22, Telegraph.PURPLE, 1.4F, e -> this.effect(e, MobEffects.WITHER, 60, 1),
						() -> new Fx(Fx.Kind.ORB, SOUL).line(level, from.add(0, 1.0, 0), to.add(0, 1.0, 0), 0.8));
				}
			});
		}
	}

	/** Requiem of Stillness: after the warning, anyone who moves during the requiem dies. */
	private void stillness(final ServerLevel level, final LivingEntity target) {
		this.playAnim(CreatureAnim.CAST);
		this.sound(level, SoundEvents.WARDEN_HEARTBEAT, 4.0F, 0.6F);
		this.during(80, 20, () -> {
			for (ServerPlayer p : this.fighters(level)) {
				level.sendParticles(ParticleTypes.SOUL, p.getX(), p.getY() + 2.2, p.getZ(), 4, 0.2, 0.1, 0.2, 0.01);
			}
		});
		this.after(80, () -> {
			Map<UUID, Vec3> start = new HashMap<>();
			for (ServerPlayer p : this.fighters(level)) {
				start.put(p.getUUID(), p.position());
			}
			this.title(level, Component.translatable("raid.minecraft_mode.mechanic.malachar_stillness.now").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD),
				Component.empty());
			this.sound(level, SoundEvents.WARDEN_SONIC_CHARGE, 4.0F, 0.4F);
			Set<UUID> struck = new HashSet<>();
			this.during(60, 4, () -> {
				for (ServerPlayer p : this.fighters(level)) {
					Vec3 from = start.get(p.getUUID());
					if (from == null || struck.contains(p.getUUID())) {
						continue;
					}
					if (p.position().subtract(from).horizontalDistance() > 0.35 || p.getY() - from.y > 0.5) {
						struck.add(p.getUUID());
						this.lethal(level, p);
					}
				}
			});
		});
	}

	/** Phylactery: Malachar cannot be hurt while his guardians stand; destroy them all in 20 seconds or die in a Death Nova. */
	private void phylactery(final ServerLevel level, final LivingEntity target) {
		this.playAnim(CreatureAnim.SUMMON);
		this.sound(level, SoundEvents.WITHER_SPAWN, 2.0F, 0.6F);
		this.setInvulnerableFor(400);
		int n = 2 + this.fighters(level).size() / 2;
		List<WitherSkeleton> guards = this.targets(level, EntityTypes.WITHER_SKELETON, this.spots(n, 8, Arenas.RADIUS - 3, 6), 60.0F,
			"raid.minecraft_mode.phylactery_guard");
		boolean[] done = {false};
		this.during(400, 10, () -> {
			long alive = guards.stream().filter(WitherSkeleton::isAlive).count();
			this.tally(level, "malachar_phylactery", (int)(guards.size() - alive), guards.size());
			for (WitherSkeleton g : guards) {
				if (g.isAlive()) {
					new Fx(Fx.Kind.ORB, SOUL).line(level, g.position().add(0, 1.2, 0), this.position().add(0, this.getBbHeight() * 0.6, 0), 1.0);
				}
			}
			if (alive == 0 && !done[0]) {
				done[0] = true;
				this.clearInvulnerable();
				this.cleared(level, "malachar_phylactery");
				this.endMechanic();
			}
		});
		this.after(400, () -> {
			this.clearInvulnerable();
			if (guards.stream().anyMatch(WitherSkeleton::isAlive)) {
				guards.forEach(g -> g.discard());
				this.wipe(level, "malachar_phylactery");
			}
		});
	}

	/** Doom: a third of the party is marked; each must reach the soul font within 10 seconds to be cleansed. */
	private void doom(final ServerLevel level, final LivingEntity target) {
		this.playAnim(CreatureAnim.CAST);
		this.sound(level, SoundEvents.ELDER_GUARDIAN_CURSE, 4.0F, 0.4F);
		List<ServerPlayer> fighters = new ArrayList<>(this.fighters(level));
		java.util.Collections.shuffle(fighters, new java.util.Random(this.random.nextLong()));
		List<ServerPlayer> marked = new ArrayList<>(fighters.subList(0, Math.min(fighters.size(), Math.max(1, (int)Math.ceil(fighters.size() / 3.0)))));
		Vec3 font = this.spots(1, 5, Arenas.RADIUS - 5, 6).getFirst();
		this.mechanicSpots.add(font);
		for (ServerPlayer m : marked) {
			m.sendSystemMessage(Component.translatable("raid.minecraft_mode.mechanic.malachar_doom.marked").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
		}
		Set<UUID> cleansed = new HashSet<>();
		this.during(200, 5, () -> {
			Telegraph.ring(level, font, 2.2, 0x60FF60, 1.4F);
			new Fx(Fx.Kind.ORB, SOUL).column(level, font, 3.0);
			for (ServerPlayer m : marked) {
				if (cleansed.contains(m.getUUID()) || !m.isAlive()) {
					continue;
				}
				level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, m.getX(), m.getY() + 2.3, m.getZ(), 3, 0.2, 0.1, 0.2, 0.0);
				if (m.position().subtract(font).horizontalDistance() <= 2.2) {
					cleansed.add(m.getUUID());
					m.sendSystemMessage(Component.translatable("raid.minecraft_mode.mechanic.malachar_doom.cleansed").withStyle(ChatFormatting.GREEN));
				}
			}
		});
		this.after(200, () -> {
			for (ServerPlayer m : marked) {
				if (!cleansed.contains(m.getUUID()) && m.isAlive() && m.level() == level) {
					this.lethal(level, m);
				}
			}
		});
	}
}
