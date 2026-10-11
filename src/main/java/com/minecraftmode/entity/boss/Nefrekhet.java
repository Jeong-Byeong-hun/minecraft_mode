package com.minecraftmode.entity.boss;

import com.minecraftmode.entity.CreatureAnim;
import com.minecraftmode.entity.combat.Attacks;
import com.minecraftmode.entity.combat.MobProjectile;
import com.minecraftmode.entity.combat.Telegraph;
import com.minecraftmode.job.skill.Fx;
import com.minecraftmode.job.skill.SkillScheduler;
import com.minecraftmode.raid.Arenas;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.particles.BlockParticleOption;
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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * Nefrekhet, the Sand Pharaoh (Lv 40-50, desert tomb). Phase 1: sand spears, the Curse of Ages and a slam. Phase 2
 * (65%): sandstorms, his mummified guard and scarab swarms. Phase 3 (30%): the Judgment of Ra sweeps the hall with
 * sunbeams. Mechanic: Quicksand - the floor sinks; only two of the four pedestals hold.
 */
public class Nefrekhet extends RaidBoss {
	private static final int SAND = 0xE0C080;
	private static final int CURSE = 0x7A3CC8;
	private static final int SUN = 0xFFD040;

	public Nefrekhet(final EntityType<? extends Nefrekhet> type, final Level level) {
		super(type, level);
	}

	@Override
	protected List<Pattern> patterns() {
		return List.of(
			pattern("sand_spears", 1, 70, 26, 10, this::sandSpears),
			spell("curse_of_ages", 1, 160, 0, 14, this::curseOfAges),
			pattern("sand_slam", 1, 110, 8, 20, this::sandSlam),
			spell("sandstorm", 2, 220, 16, 26, this::sandstorm),
			pattern("mummy_guard", 2, 420, 0, 20, (level, target) -> {
				this.playAnim(CreatureAnim.SUMMON);
				this.summonMinions(level, EntityTypes.HUSK, 3, 5, target);
			}),
			pattern("scarab_swarm", 2, 240, 0, 18, this::scarabSwarm),
			spell("judgment_of_ra", 3, 300, 0, 50, this::judgmentOfRa)
		);
	}

	@Override
	protected List<Mechanic> mechanics() {
		return List.of(mechanic("nefrekhet_quicksand", 0.75F, 140, 200, this::quicksand));
	}

	@Override
	protected void ambientFx(final ServerLevel level) {
		if (this.tickCount % 6 == 0) {
			level.sendParticles(new BlockParticleOption(ParticleTypes.FALLING_DUST, Blocks.SAND.defaultBlockState()), this.getX(), this.getY(0.9), this.getZ(), 3,
				0.6, 0.6, 0.6, 0.0);
		}
	}

	private void sandSpears(final ServerLevel level, final LivingEntity target) {
		this.playAnim(CreatureAnim.ATTACK);
		int count = this.phase() >= 2 ? 5 : 4;
		this.volley(level, target, count, 36.0F, 0.9F, new ItemStack(Items.SANDSTONE), Fx.Kind.SHARD, SAND, 0.0F, 1.2F, false);
		this.sound(level, SoundEvents.SAND_BREAK, 2.0F, 0.5F);
	}

	/** The Curse of Ages: the target's ground withers whoever stands in it. */
	private void curseOfAges(final ServerLevel level, final LivingEntity target) {
		this.playAnim(CreatureAnim.CAST);
		this.sound(level, SoundEvents.EVOKER_CAST_SPELL, 2.0F, 0.6F);
		Vec3 at = this.ground(target.position());
		this.circle(level, at, 3.5, 26, Telegraph.PURPLE, 1.0F, e -> {
			this.effect(e, MobEffects.WITHER, 60, 1);
			this.effect(e, MobEffects.WEAKNESS, 100, 0);
		}, () -> new Fx(Fx.Kind.RUNE, CURSE).circle(level, Fx.Kind.RUNE, at.add(0, 0.3, 0), 3.5));
	}

	private void sandSlam(final ServerLevel level, final LivingEntity target) {
		this.playAnim(CreatureAnim.SLAM);
		Vec3 me = this.ground(this.position());
		this.circle(level, me, 5.0, 18, Telegraph.RED, 1.4F, e -> Attacks.knock(e, me, 1.2, 0.5), () -> this.boom(level, me, 5.0));
	}

	/** A conjured sandstorm blows from him: blinding gusts. */
	private void sandstorm(final ServerLevel level, final LivingEntity target) {
		this.playAnim(CreatureAnim.BREATH);
		this.sound(level, SoundEvents.SAND_BREAK, 3.0F, 0.3F);
		this.cone(level, this.ground(this.position()), target.position(), 13, 50, 22, Telegraph.ORANGE, 0.4F, 4, new Fx(Fx.Kind.SMOKE, SAND),
			e -> {
				this.effect(e, MobEffects.BLINDNESS, 40, 0);
				Attacks.knock(e, this.position(), 0.4, 0.1);
			});
	}

	/** Scarabs boil up under the fighters and around them. */
	private void scarabSwarm(final ServerLevel level, final LivingEntity target) {
		this.playAnim(CreatureAnim.CAST);
		this.sound(level, SoundEvents.SILVERFISH_AMBIENT, 3.0F, 0.5F);
		this.rain(level, this.fighters(level).size() * 2 + 2, 2.5, 24, Telegraph.ORANGE, 0.9F, e -> this.effect(e, MobEffects.POISON, 60, 1),
			at -> level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SAND.defaultBlockState()), at.x, at.y + 0.3, at.z, 30, 1.0, 0.2, 1.0, 0.1));
	}

	/** The Judgment of Ra: four sunbeams cross the hall from him, three times, each turned a little further. */
	private void judgmentOfRa(final ServerLevel level, final LivingEntity target) {
		this.playAnim(CreatureAnim.ROAR);
		this.sound(level, SoundEvents.BEACON_ACTIVATE, 3.0F, 0.6F);
		float start = this.random.nextFloat() * 90.0F;
		for (int wave = 0; wave < 3; wave++) {
			final float base = start + wave * 30.0F;
			SkillScheduler.schedule(wave * 16 + 1, this.asSpell(() -> {
				if (!this.alive()) {
					return;
				}
				Vec3 from = this.ground(this.position());
				for (int k = 0; k < 4; k++) {
					Vec3 to = from.add(Vec3.directionFromRotation(0.0F, base + k * 90.0F).scale(Arenas.RADIUS));
					this.line(level, from, to, 2.6, 24, Telegraph.ORANGE, 1.5F, e -> e.igniteForSeconds(3.0F),
						() -> new Fx(Fx.Kind.BOLT, SUN).line(level, from.add(0, 1.0, 0), to.add(0, 1.0, 0), 0.8));
				}
			}));
		}
	}

	/**
	 * Quicksand: the floor starts to swallow the hall. Four pedestals rise; after four seconds two of them crumble, and when
	 * the sand closes anyone not standing on a pedestal that held is lost. Any number of players fits on a pedestal.
	 */
	private void quicksand(final ServerLevel level, final LivingEntity target) {
		this.playAnim(CreatureAnim.ROAR);
		this.sound(level, SoundEvents.SAND_BREAK, 4.0F, 0.3F);
		Vec3 c = this.arenaCenter();
		List<Vec3> pedestals = this.spots(4, 7, Arenas.RADIUS - 5, 5);
		List<Vec3> holding = new ArrayList<>(pedestals);
		this.mechanicSpots.addAll(pedestals);
		this.after(80, () -> {
			// two of the four crumble, picked at random
			holding.remove(this.random.nextInt(holding.size()));
			holding.remove(this.random.nextInt(holding.size()));
			this.mechanicSpots.clear();
			this.mechanicSpots.addAll(holding);
			this.sound(level, c, SoundEvents.SAND_BREAK, 4.0F, 0.6F);
		});
		this.during(190, 5, () -> {
			for (Vec3 p : pedestals) {
				boolean holds = holding.contains(p);
				Telegraph.ring(level, p, 2.6, holds ? 0x60FF60 : Telegraph.RED, 1.4F);
				level.sendParticles(new BlockParticleOption(ParticleTypes.FALLING_DUST, Blocks.SANDSTONE.defaultBlockState()), p.x, p.y + 1.0, p.z, 6, 0.8, 0.5, 0.8, 0.0);
			}
			for (int k = 0; k < 10; k++) {
				double a = this.random.nextDouble() * Math.PI * 2;
				double r = this.random.nextDouble() * (Arenas.RADIUS - 1);
				level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SAND.defaultBlockState()), c.x + Math.cos(a) * r, c.y + 0.2, c.z + Math.sin(a) * r, 3,
					0.3, 0.1, 0.3, 0.0);
			}
		});
		this.during(190, 20, () -> Telegraph.ring(level, c, Arenas.RADIUS - 1, Telegraph.RED, 1.2F));
		this.after(190, () -> {
			for (ServerPlayer p : this.fighters(level)) {
				if (holding.stream().noneMatch(s -> p.position().subtract(s).horizontalDistance() <= 2.8)) {
					this.lethal(level, p);
				}
			}
			level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SAND.defaultBlockState()), c.x, c.y + 0.3, c.z, 400, Arenas.RADIUS * 0.6, 0.3,
				Arenas.RADIUS * 0.6, 0.1);
			this.sound(level, c, SoundEvents.SAND_BREAK, 4.0F, 0.2F);
		});
	}
}
