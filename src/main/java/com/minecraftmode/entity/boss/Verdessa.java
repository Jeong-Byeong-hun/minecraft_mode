package com.minecraftmode.entity.boss;

import com.minecraftmode.entity.CreatureAnim;
import com.minecraftmode.entity.combat.Attacks;
import com.minecraftmode.entity.combat.MobProjectile;
import com.minecraftmode.entity.combat.Telegraph;
import com.minecraftmode.job.skill.Fx;
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
import net.minecraft.world.entity.monster.creaking.Creaking;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Verdessa, the Thornmother (Lv 10-20, overgrown grove): the first raid, so every attack is slow and clearly
 * telegraphed. Phase 1: thorn lashes, seed volleys and roots under every player. Phase 2 (65%): spore clouds and
 * bogged sentries. Phase 3 (30%): brambles burst outward from her in two rings. Mechanic: Seed Trees - destroy them
 * all before they bloom.
 */
public class Verdessa extends RaidBoss {
	private static final int LEAF = 0x6ABF4A;
	private static final int SPORE = 0xC8D86A;

	public Verdessa(final EntityType<? extends Verdessa> type, final Level level) {
		super(type, level);
	}

	@Override
	protected List<Pattern> patterns() {
		return List.of(
			pattern("thorn_lash", 1, 90, 20, 16, this::thornLash),
			pattern("seed_volley", 1, 70, 26, 8, this::seedVolley),
			spell("root_snare", 1, 180, 0, 16, this::rootSnare),
			spell("spore_cloud", 2, 200, 16, 24, this::sporeCloud),
			pattern("bogged_sentries", 2, 420, 0, 20, (level, target) -> {
				this.playAnim(CreatureAnim.SUMMON);
				this.summonMinions(level, EntityTypes.BOGGED, 2, 4, target);
			}),
			pattern("bramble_burst", 3, 260, 0, 44, this::brambleBurst)
		);
	}

	@Override
	protected List<Mechanic> mechanics() {
		return List.of(mechanic("verdessa_seed_trees", 0.6F, 150, 380, this::seedTrees));
	}

	@Override
	protected void ambientFx(final ServerLevel level) {
		if (this.tickCount % 8 == 0) {
			level.sendParticles(ParticleTypes.CHERRY_LEAVES, this.getX(), this.getY(0.8), this.getZ(), 3, 1.0, 0.8, 1.0, 0.0);
		}
	}

	/** A thorny vine whips along a strip toward the target. */
	private void thornLash(final ServerLevel level, final LivingEntity target) {
		this.playAnim(CreatureAnim.ATTACK);
		Vec3 from = this.ground(this.position());
		Vec3 to = reach(from, target.position(), 16);
		this.sound(level, SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES, 2.0F, 0.5F);
		this.line(level, from, to, 2.2, 22, Telegraph.ORANGE, 1.1F, e -> this.effect(e, MobEffects.SLOWNESS, 40, 1),
			() -> new Fx(Fx.Kind.PETAL, LEAF).line(level, from.add(0, 0.6, 0), to.add(0, 0.6, 0), 0.7));
	}

	private void seedVolley(final ServerLevel level, final LivingEntity target) {
		this.playAnim(CreatureAnim.CAST);
		for (MobProjectile p : this.volley(level, target, 3, 30.0F, 0.8F, new ItemStack(Items.WHEAT_SEEDS), Fx.Kind.PETAL, LEAF, 0.02F, 1.0F, false)) {
			p.effect(MobEffects.POISON, 40, 0);
		}
		this.sound(level, SoundEvents.CROP_PLANTED, 2.0F, 0.6F);
	}

	/** Roots burst out under every player. */
	private void rootSnare(final ServerLevel level, final LivingEntity target) {
		this.playAnim(CreatureAnim.CAST);
		this.sound(level, SoundEvents.ROOTED_DIRT_BREAK, 2.0F, 0.6F);
		for (ServerPlayer p : this.fighters(level)) {
			Vec3 at = this.ground(p.position());
			this.circle(level, at, 2.4, 28, Telegraph.ORANGE, 0.7F, e -> this.effect(e, MobEffects.SLOWNESS, 60, 2),
				() -> level.sendParticles(ParticleTypes.COMPOSTER, at.x, at.y + 0.3, at.z, 30, 1.0, 0.4, 1.0, 0.05));
		}
	}

	/** A cone of choking spores in front of her. */
	private void sporeCloud(final ServerLevel level, final LivingEntity target) {
		this.playAnim(CreatureAnim.BREATH);
		this.sound(level, SoundEvents.AZALEA_LEAVES_BREAK, 3.0F, 0.5F);
		this.cone(level, this.ground(this.position()), target.position(), 11, 40, 24, Telegraph.ORANGE, 0.45F, 3, new Fx(Fx.Kind.SMOKE, SPORE),
			e -> this.effect(e, MobEffects.POISON, 60, 1));
	}

	/** Brambles tear up the ground around her, then in a wider ring. */
	private void brambleBurst(final ServerLevel level, final LivingEntity target) {
		this.playAnim(CreatureAnim.SLAM);
		this.sound(level, SoundEvents.SWEET_BERRY_BUSH_BREAK, 3.0F, 0.4F);
		Vec3 me = this.ground(this.position());
		this.circle(level, me, 4.5, 24, Telegraph.RED, 1.3F, e -> Attacks.knock(e, me, 1.0, 0.4), () -> this.boom(level, me, 4.5));
		this.donut(level, me, 4.5, 11.0, 44, Telegraph.RED, 1.1F, e -> this.effect(e, MobEffects.SLOWNESS, 60, 1));
	}

	/** Seed Trees: one more tree than the party grows around the grove; every tree must fall within 18 seconds or they bloom in a burst of thorns. */
	private void seedTrees(final ServerLevel level, final LivingEntity target) {
		this.playAnim(CreatureAnim.SUMMON);
		this.sound(level, SoundEvents.CREAKING_ACTIVATE, 4.0F, 0.6F);
		int n = this.fighters(level).size() + 1;
		List<Vec3> at = this.spots(n, 6, Arenas.RADIUS - 4, 4);
		List<Creaking> trees = this.targets(level, EntityTypes.CREAKING, at, 24.0F, "raid.minecraft_mode.seed_tree");
		boolean[] done = {false};
		this.during(360, 10, () -> {
			long alive = trees.stream().filter(Creaking::isAlive).count();
			this.tally(level, "verdessa_seed_trees", (int)(trees.size() - alive), trees.size());
			for (Creaking t : trees) {
				if (t.isAlive()) {
					level.sendParticles(ParticleTypes.HAPPY_VILLAGER, t.getX(), t.getY(1.0), t.getZ(), 3, 0.4, 0.4, 0.4, 0.0);
				}
			}
			if (alive == 0 && !done[0]) {
				done[0] = true;
				this.cleared(level, "verdessa_seed_trees");
				this.endMechanic();
			}
		});
		this.after(360, () -> {
			if (trees.stream().anyMatch(Creaking::isAlive)) {
				trees.forEach(t -> t.discard());
				Vec3 c = this.arenaCenter();
				level.sendParticles(ParticleTypes.CHERRY_LEAVES, c.x, c.y + 1.0, c.z, 400, Arenas.RADIUS * 0.6, 1.0, Arenas.RADIUS * 0.6, 0.05);
				this.sound(level, c, SoundEvents.SWEET_BERRY_BUSH_BREAK, 4.0F, 0.3F);
				this.wipe(level, "verdessa_seed_trees");
			}
		});
	}
}
