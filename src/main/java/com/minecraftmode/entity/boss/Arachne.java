package com.minecraftmode.entity.boss;

import com.minecraftmode.entity.CreatureAnim;
import com.minecraftmode.entity.combat.Attacks;
import com.minecraftmode.entity.combat.MobProjectile;
import com.minecraftmode.entity.combat.Telegraph;
import com.minecraftmode.job.skill.Fx;
import com.minecraftmode.job.skill.SkillScheduler;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Arachne, the spider queen (Lv 20-30, web cave). Phase 1: venom volleys, pounces and web shots that
 * leave sticky webs. Phase 2 (70%): spiderling broods and acid pools under every player. Phase 3
 * (40%): Silk Storm - she reels everyone in, then bursts - and a frenzy that speeds her up.
 */
public class Arachne extends RaidBoss {
	private static final int VENOM = 0x7FD14A;

	public Arachne(final EntityType<? extends Arachne> type, final Level level) {
		super(type, level);
	}

	@Override
	protected List<Pattern> patterns() {
		return List.of(
			pattern("venom_volley", 1, 70, 26, 8, this::venomVolley),
			pattern("pounce", 1, 120, 20, 20, this::pounce),
			pattern("web_shot", 1, 160, 26, 6, this::webShot),
			pattern("brood", 2, 420, 0, 20, (level, target) -> {
				this.playAnim(CreatureAnim.SUMMON);
				this.summonMinions(level, EntityTypes.CAVE_SPIDER, 3, 6, target);
			}),
			pattern("acid_pools", 2, 220, 0, 16, this::acidPools),
			pattern("silk_storm", 3, 320, 0, 50, this::silkStorm)
		);
	}

	@Override
	protected void onPhase(final ServerLevel level, final int phase) {
		if (phase == 3) {
			this.addEffect(new MobEffectInstance(MobEffects.SPEED, 20 * 600, 1, false, false));
		}
	}

	@Override
	public void makeStuckInBlock(final BlockState state, final Vec3 speedMultiplier) {
		if (!state.is(Blocks.COBWEB)) {
			super.makeStuckInBlock(state, speedMultiplier);
		}
	}

	@Override
	protected void ambientFx(final ServerLevel level) {
		if (this.tickCount % 10 == 0) {
			level.sendParticles(ParticleTypes.ITEM_SLIME, this.getX(), this.getY(0.3), this.getZ(), 2, 1.2, 0.2, 1.2, 0.0);
		}
	}

	private void venomVolley(final ServerLevel level, final LivingEntity target) {
		this.playAnim(CreatureAnim.ATTACK);
		int count = this.phase() >= 2 ? 5 : 3;
		for (MobProjectile p : this.volley(level, target, count, 40.0F, 0.9F, new ItemStack(Items.SPIDER_EYE), Fx.Kind.SMOKE, VENOM, 0.0F, 1.2F, false)) {
			p.effect(MobEffects.POISON, 80, 0);
		}
		this.sound(level, SoundEvents.SPIDER_AMBIENT, 2.0F, 0.6F);
	}

	private void pounce(final ServerLevel level, final LivingEntity target) {
		Vec3 to = this.ground(target.position());
		Vec3 d = to.subtract(this.position());
		this.playAnim(CreatureAnim.LEAP);
		Attacks.push(this, new Vec3(d.x * 0.16, 0.9 + Math.min(0.5, d.horizontalDistance() * 0.03), d.z * 0.16));
		this.sound(level, SoundEvents.SPIDER_HURT, 2.0F, 0.5F);
		this.circle(level, to, 3.5, 18, Telegraph.ORANGE, 1.4F, e -> Attacks.knock(e, to, 0.9, 0.5), () -> this.boom(level, to, 3.5));
	}

	private void webShot(final ServerLevel level, final LivingEntity target) {
		this.playAnim(CreatureAnim.CAST);
		for (MobProjectile p : this.volley(level, target, 1, 0.0F, 0.6F, new ItemStack(Blocks.COBWEB), Fx.Kind.SPARK, 0xF0F0F0, 0.02F, 1.0F, false)) {
			p.blast(2.5F).effect(MobEffects.SLOWNESS, 60, 2).onImpact(at -> this.webs(level, BlockPos.containing(at), 1));
		}
		this.sound(level, SoundEvents.SPIDER_STEP, 2.0F, 0.6F);
	}

	/** Sticky webs around {@code at} that melt after five seconds. */
	private void webs(final ServerLevel level, final BlockPos at, final int radius) {
		BlockPos floor = this.home() != null ? at.atY(this.home().getY()) : at;
		for (int dx = -radius; dx <= radius; dx++) {
			for (int dz = -radius; dz <= radius; dz++) {
				BlockPos p = floor.offset(dx, 0, dz);
				if (Math.abs(dx) + Math.abs(dz) <= radius + 1 && level.getBlockState(p).isAir()) {
					level.setBlockAndUpdate(p, Blocks.COBWEB.defaultBlockState());
					SkillScheduler.schedule(100 + this.random.nextInt(20), () -> {
						if (level.getBlockState(p).is(Blocks.COBWEB)) {
							level.removeBlock(p, false);
						}
					});
				}
			}
		}
	}

	private void acidPools(final ServerLevel level, final LivingEntity target) {
		this.playAnim(CreatureAnim.CAST);
		this.sound(level, SoundEvents.BREWING_STAND_BREW, 2.0F, 0.6F);
		for (ServerPlayer p : this.fighters(level)) {
			Vec3 at = this.ground(p.position());
			this.circle(level, at, 2.8, 26, Telegraph.ORANGE, 1.0F, e -> this.effect(e, MobEffects.POISON, 100, 1),
				() -> level.sendParticles(ParticleTypes.ITEM_SLIME, at.x, at.y + 0.2, at.z, 30, 1.4, 0.1, 1.4, 0.05));
		}
	}

	private void silkStorm(final ServerLevel level, final LivingEntity target) {
		this.playAnim(CreatureAnim.SPIN);
		this.sound(level, SoundEvents.SPIDER_AMBIENT, 3.0F, 0.4F);
		Vec3 me = this.ground(this.position());
		new Fx(Fx.Kind.SPARK, 0xF0F0F0).circle(level, Fx.Kind.SPARK, me.add(0, 1, 0), 10);
		this.pull(level, me, 22, 1.4);
		SkillScheduler.schedule(8, () -> {
			if (this.alive()) {
				this.pull(level, this.ground(this.position()), 22, 0.9);
			}
		});
		this.circle(level, me, 7.5, 34, Telegraph.RED, 2.0F, e -> Attacks.knock(e, me, 1.6, 0.6), () -> {
			this.playAnim(CreatureAnim.SLAM);
			this.boom(level, me, 7.5);
			this.webs(level, BlockPos.containing(me), 2);
		});
	}
}
