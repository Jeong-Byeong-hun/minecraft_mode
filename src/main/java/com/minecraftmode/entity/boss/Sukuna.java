package com.minecraftmode.entity.boss;

import com.minecraftmode.domain.DomainStyle;
import com.minecraftmode.domain.Domains;
import com.minecraftmode.entity.CreatureAnim;
import com.minecraftmode.entity.combat.Attacks;
import com.minecraftmode.entity.combat.MobProjectile;
import com.minecraftmode.entity.combat.Telegraph;
import com.minecraftmode.job.skill.Fx;
import com.minecraftmode.raid.Arenas;
import com.minecraftmode.raid.RaidDamage;
import com.minecraftmode.registry.ModEntities;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Ryomen Sukuna, the King of Curses (Lv 100, the Malevolent Shrine): the raid past the level cap. Four arms, a second mouth in
 * his belly, the Ten Shadows' Mahoraga at his call.
 *
 * <p>Phase 1: Dismantle (three unseen slashes that weaken with distance), Cleave (a share of the target's own health, whatever
 * its defenses), Black Flash (a dash into a black-sparked blow) and Spiderweb (cracks spreading from his feet). Phase 2 (75%):
 * Dismantle storms crossing on everyone and Kamutoke's lightning. Phase 3 (45%): Fuga, the flame arrow that sets its crater
 * burning, and the World-Cutting Slash, which cuts space itself across the whole shrine. Phase 4 (20%): everything, faster.
 *
 * <p>Mechanics: Mahoraga (75%, once: Sukuna takes no damage until it falls, and it adapts), Malevolent Shrine (50%, then every
 * 150 s: the domain slashes everyone outside a Simple Domain). Reverse Cursed Technique: at 40% and 15% he channels a heal,
 * twice a fight at most, that enough damage breaks.
 */
public class Sukuna extends RaidBoss {
	private static final int SLASH = 0xF2F2FA;
	private static final int BLOOD = 0xC0182A;
	private static final int FLAME = 0xFF8A20;
	private static final int SIMPLE_DOMAIN = 0x40A0FF;
	/** Dismantle's reach, and what is left of it there. */
	static final double DISMANTLE_REACH = 24.0;
	static final float DISMANTLE_FLOOR = 0.4F;
	/** Cleave takes this share of the target's max health (times the difficulty's damage), past every defense. */
	static final float CLEAVE_SHARE = 0.16F;
	/** The World-Cutting Slash takes this share. */
	static final float WORLD_SLASH_SHARE = 0.6F;
	/** The domain's sure hit, every {@link #DOMAIN_STEP} ticks, on anyone outside a Simple Domain. */
	static final float DOMAIN_SHARE = 0.06F;
	static final int DOMAIN_STEP = 10;
	/** The domain: a circle this wide around the shrine (the arena reaches past it, so one can run out), open this long. */
	public static final double DOMAIN_RADIUS = 17.0;
	static final int DOMAIN_TICKS = 200;
	/** Where Sukuna stands in front of the shrine, and how far Simple Domains keep from its middle. */
	static final double SHRINE_FRONT = 8.0;
	static final double SHRINE_CLEAR = 8.0;
	public static final double SIMPLE_DOMAIN_RADIUS = 2.4;
	/** Reverse Cursed Technique: when it starts, how long the channel lasts, what it heals and the damage that breaks it. */
	static final float[] RCT_AT = {0.4F, 0.15F};
	static final int RCT_TICKS = 100;
	static final float RCT_HEAL = 0.15F;
	static final float RCT_BREAK = 0.06F;

	private @Nullable Mahoraga mahoraga;
	private @Nullable MalevolentShrine shrine;
	private Domains.@Nullable Domain openDomain;
	private int heals;
	private int rctTicks;
	private float rctFrom;
	private int refusedAt;

	public Sukuna(final EntityType<? extends Sukuna> type, final Level level) {
		super(type, level);
	}

	@Override
	protected List<Pattern> patterns() {
		return List.of(
			spell("dismantle", 1, 70, 0, 14, this::dismantle),
			spell("cleave", 1, 120, 7, 18, this::cleave),
			pattern("black_flash", 1, 160, 18, 28, this::blackFlash),
			spell("spiderweb", 1, 200, 0, 30, this::spiderweb),
			spell("dismantle_storm", 2, 220, 0, 24, this::dismantleStorm),
			spell("kamutoke", 2, 260, 0, 30, this::kamutoke),
			spell("fuga", 3, 340, 0, 60, this::fuga),
			spell("world_slash", 3, 420, 0, 70, this::worldSlash)
		);
	}

	@Override
	protected List<Mechanic> mechanics() {
		return List.of(
			mechanic("sukuna_mahoraga", 0.75F, 0, 40, this::summonMahoraga),
			mechanic("sukuna_domain", 0.5F, 150, 200, this::domain)
		);
	}

	@Override
	protected int globalCooldown() {
		// he leaves most of the fighting to Mahoraga while it stands
		return this.mahoragaUp() ? super.globalCooldown() * 8 / 5 : super.globalCooldown();
	}

	@Override
	protected void ambientFx(final ServerLevel level) {
		if (this.tickCount % 6 == 0) {
			new Fx(Fx.Kind.SMOKE, 0x3A0A14).burst(level, this.position().add(0, this.getBbHeight() * 0.5, 0), 2, 0.8, 0.01);
		}
	}

	@Override
	protected void customServerAiStep(final ServerLevel level) {
		super.customServerAiStep(level);
		if (this.mahoraga != null && !this.mahoraga.isAlive()) {
			this.mahoraga = null;
		}
		this.tickReverse(level);
	}

	public boolean mahoragaUp() {
		return this.mahoraga != null && this.mahoraga.isAlive();
	}

	public @Nullable Mahoraga mahoraga() {
		return this.mahoraga;
	}

	@Override
	public boolean hurtServer(final ServerLevel level, final DamageSource source, final float damage) {
		if (this.mahoragaUp() && !source.isCreativePlayer()) {
			level.sendParticles(ParticleTypes.ENCHANTED_HIT, this.getX(), this.getY(0.6), this.getZ(), 6, 0.5, 0.5, 0.5, 0.2);
			if (source.getEntity() instanceof ServerPlayer p && this.tickCount - this.refusedAt > 40) {
				this.refusedAt = this.tickCount;
				p.sendOverlayMessage(Component.translatable("raid.minecraft_mode.sukuna.guarded").withStyle(ChatFormatting.RED));
			}
			return false;
		}
		return super.hurtServer(level, source, damage);
	}

	// ------------------------------------------------------------------ patterns

	/** A telegraphed cut from {@code from} to {@code to}; with {@code falloff} > 0 it weakens with the distance from {@code from} like Dismantle. */
	private void slash(final ServerLevel level, final Vec3 from, final Vec3 to, final double width, final int delay, final float power, final double falloff) {
		final boolean magic = this.casting();
		Telegraph.line(level, from, to, width, delay, Telegraph.RED, () -> {
			if (!this.alive()) {
				return;
			}
			for (LivingEntity e : Attacks.inLine(level, from, to, width, 4.0)) {
				double d = e.position().subtract(from).horizontalDistance();
				float share = falloff <= 0 ? 1.0F : (float)Math.max(DISMANTLE_FLOOR, 1.0 - (1.0 - DISMANTLE_FLOOR) * d / falloff);
				Attacks.hit(this, e, this.dmg(power) * share, magic);
			}
			new Fx(Fx.Kind.SLASH, SLASH).line(level, from.add(0, 1.2, 0), to.add(0, 1.2, 0), 0.7);
			this.sound(level, from.lerp(to, 0.5), SoundEvents.PLAYER_ATTACK_SWEEP, 1.5F, 1.6F);
		});
	}

	/** Dismantle: three unseen slashes fanned at the target, weaker the further they fly. */
	private void dismantle(final ServerLevel level, final LivingEntity target) {
		this.playAnim(CreatureAnim.ATTACK);
		Vec3 from = this.ground(this.position());
		Vec3 dir = reach(from, target.position(), 1.0).subtract(from);
		for (int i = 0; i < 3; i++) {
			Vec3 d = dir.yRot((float)Math.toRadians((i - 1) * 12.0));
			this.slash(level, from, from.add(d.scale(DISMANTLE_REACH)), 1.4, Telegraph.MIN_WARNING + i * 5, 1.3F, DISMANTLE_REACH);
		}
	}

	/** Cleave: a close cut that adjusts itself to what it cuts, taking a share of the target's own health past every defense. */
	private void cleave(final ServerLevel level, final LivingEntity target) {
		this.playAnim(CreatureAnim.ATTACK);
		Vec3 origin = this.ground(this.position());
		Vec3 to = target.position().subtract(origin);
		float yaw = (float)Math.toDegrees(Math.atan2(-to.x, to.z));
		float share = CLEAVE_SHARE * this.difficulty().damage;
		Telegraph.cone(level, origin, yaw, 6.5, 60, 22, Telegraph.RED, () -> {
			if (!this.alive()) {
				return;
			}
			for (LivingEntity e : Attacks.inCone(level, origin, yaw, 6.5, 60)) {
				RaidDamage.portion(level, e, this, share);
			}
			Fx cut = new Fx(Fx.Kind.SLASH, BLOOD);
			cut.arc(level, this.position().add(0, 2.4, 0), yaw, 4.5, 120);
			cut.arc(level, this.position().add(0, 1.4, 0), yaw, 4.0, 120);
			this.sound(level, SoundEvents.PLAYER_ATTACK_STRONG, 2.0F, 0.6F);
		});
	}

	/** Black Flash: a dash into the target and a blow that sparks black. */
	private void blackFlash(final ServerLevel level, final LivingEntity target) {
		Vec3 from = this.ground(this.position());
		Vec3 at = this.ground(target.position());
		Vec3 back = from.subtract(at).multiply(1, 0, 1);
		Vec3 stop = back.lengthSqr() < 4.0 ? from : at.add(back.normalize().scale(1.5));
		Telegraph.line(level, from, at, 2.0, 22, Telegraph.RED, () -> {
		});
		this.circle(level, at, 2.8, 26, Telegraph.RED, 2.2F, e -> Attacks.knock(e, at, 1.8, 0.6), () -> {
			Vec3 c = at.add(0, 1.2, 0);
			new Fx(Fx.Kind.BOLT, 0x101014).burst(level, c, 24, 1.5, 0.4);
			new Fx(Fx.Kind.BOLT, BLOOD).burst(level, c, 16, 1.2, 0.3);
			level.sendParticles(ParticleTypes.SONIC_BOOM, c.x, c.y, c.z, 1, 0, 0, 0, 0);
			this.sound(level, at, SoundEvents.LIGHTNING_BOLT_IMPACT, 2.5F, 0.6F);
			this.sound(level, at, SoundEvents.GENERIC_EXPLODE.value(), 1.5F, 1.3F);
		});
		this.after(18, () -> {
			this.playAnim(CreatureAnim.CHARGE);
			this.dash(stop.add(0, 0.1, 0), 6, null);
		});
		this.after(24, () -> this.playAnim(CreatureAnim.ATTACK));
	}

	/** Spiderweb: a hand on the ground, and the floor cracks out from it in spokes and a ring. */
	private void spiderweb(final ServerLevel level, final LivingEntity target) {
		this.playAnim(CreatureAnim.SLAM);
		Vec3 at = this.ground(this.position());
		float turn = this.random.nextFloat() * 45.0F;
		for (int k = 0; k < 8; k++) {
			Vec3 to = at.add(Vec3.directionFromRotation(0.0F, turn + k * 45.0F).scale(14));
			this.slash(level, at, to, 1.2, 30, 1.2F, 0);
		}
		this.donut(level, at, 6.5, 8.5, 30, Telegraph.RED, 1.4F, null);
		this.sound(level, SoundEvents.DEEPSLATE_BREAK, 2.5F, 0.5F);
	}

	/** Dismantle storm: two slashes cross on every fighter, more cut across the shrine at random. */
	private void dismantleStorm(final ServerLevel level, final LivingEntity target) {
		this.playAnim(CreatureAnim.CAST);
		for (ServerPlayer p : this.fighters(level)) {
			Vec3 c = this.ground(p.position());
			float a = this.random.nextFloat() * 180.0F;
			for (int k = 0; k < 2; k++) {
				Vec3 d = Vec3.directionFromRotation(0.0F, a + k * 90.0F).scale(6);
				this.slash(level, c.subtract(d), c.add(d), 1.4, 24 + k * 10, 1.1F, 0);
			}
		}
		Vec3 center = this.arenaCenter();
		for (int i = 0; i < 4; i++) {
			double r = this.random.nextDouble() * (Arenas.RADIUS - 6);
			double a = this.random.nextDouble() * Math.PI * 2;
			Vec3 c = center.add(Math.cos(a) * r, 0, Math.sin(a) * r);
			Vec3 d = Vec3.directionFromRotation(0.0F, this.random.nextFloat() * 180.0F).scale(10);
			this.slash(level, c.subtract(d), c.add(d), 1.4, 30 + i * 4, 1.0F, 0);
		}
	}

	/** Kamutoke: the spear raised, lightning on everyone and around them. */
	private void kamutoke(final ServerLevel level, final LivingEntity target) {
		this.playAnim(CreatureAnim.CAST);
		this.sound(level, SoundEvents.LIGHTNING_BOLT_THUNDER, 2.0F, 0.7F);
		this.rain(level, this.fighters(level).size() * 2 + 2, 2.6, 30, Telegraph.ORANGE, 1.5F, e -> this.effect(e, MobEffects.SLOWNESS, 30, 1), spot -> {
			LightningBolt bolt = EntityTypes.LIGHTNING_BOLT.create(level, EntitySpawnReason.TRIGGERED);
			if (bolt != null) {
				bolt.snapTo(spot.x, spot.y, spot.z);
				bolt.setVisualOnly(true);
				level.addFreshEntity(bolt);
			}
		});
	}

	/** Fuga ("Open"): a flame arrow drawn between his hands; its crater keeps burning. */
	private void fuga(final ServerLevel level, final LivingEntity target) {
		this.playAnim(CreatureAnim.CAST);
		Vec3 at = this.ground(target.position());
		this.title(level, Component.translatable("raid.minecraft_mode.sukuna.fuga").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), Component.empty());
		this.sound(level, SoundEvents.FIRECHARGE_USE, 2.0F, 0.5F);
		this.during(40, 2, () -> {
			Vec3 hands = this.position().add(0, this.getBbHeight() * 0.55, 0).add(this.getLookAngle().scale(1.2));
			new Fx(Fx.Kind.SPARK, FLAME).burst(level, hands, 6, 0.6, 0.05);
			level.sendParticles(ParticleTypes.FLAME, hands.x, hands.y, hands.z, 4, 0.5, 0.2, 0.5, 0.01);
		});
		this.after(40, () -> {
			Vec3 eye = this.position().add(0, this.getBbHeight() * 0.55, 0);
			Vec3 dir = at.add(0, 0.5, 0).subtract(eye);
			MobProjectile arrow = MobProjectile.of(this, new ItemStack(Items.FIRE_CHARGE), Fx.Kind.SPARK, FLAME, 0.0F, 0.0F).visual()
				.launch(eye, dir, (float)(dir.length() / 10.0));
			arrow.maxAge(12);
			level.addFreshEntity(arrow);
			this.sound(level, SoundEvents.BLAZE_SHOOT, 2.5F, 0.6F);
		});
		this.circle(level, at, 8.0, 50, Telegraph.ORANGE, 2.4F, e -> e.igniteForSeconds(5.0F), () -> {
			this.boom(level, at, 8.0);
			level.sendParticles(ParticleTypes.FLAME, at.x, at.y + 0.5, at.z, 120, 4.0, 1.0, 4.0, 0.1);
			// the crater burns on
			final boolean magic = this.casting();
			this.during(100, 10, () -> {
				level.sendParticles(ParticleTypes.FLAME, at.x, at.y + 0.3, at.z, 30, 3.5, 0.2, 3.5, 0.02);
				Attacks.hitAll(this, Attacks.inCircle(level, at, 6.0, 3.0), this.dmg(0.3F), magic, e -> e.igniteForSeconds(2.0F));
			});
		});
	}

	/** The World-Cutting Slash: an incantation, then one cut through space across the whole shrine, past every defense. */
	private void worldSlash(final ServerLevel level, final LivingEntity target) {
		this.playAnim(CreatureAnim.CAST);
		this.title(level, Component.translatable("raid.minecraft_mode.sukuna.world_slash").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD),
			Component.translatable("raid.minecraft_mode.sukuna.world_slash.hint").withStyle(ChatFormatting.YELLOW));
		this.sound(level, SoundEvents.WARDEN_SONIC_CHARGE, 3.0F, 0.5F);
		Vec3 from = this.ground(this.position());
		Vec3 dir = reach(from, target.position(), 1.0).subtract(from);
		Vec3 a = from.subtract(dir.scale(Arenas.RADIUS * 2));
		Vec3 b = from.add(dir.scale(Arenas.RADIUS * 2));
		float share = WORLD_SLASH_SHARE * this.difficulty().damage;
		Telegraph.line(level, a, b, 3.0, 60, Telegraph.RED, () -> {
			if (!this.alive()) {
				return;
			}
			this.playAnim(CreatureAnim.ATTACK);
			for (LivingEntity e : Attacks.inLine(level, a, b, 3.0, 6.0)) {
				RaidDamage.portion(level, e, this, share);
			}
			new Fx(Fx.Kind.SLASH, SLASH).line(level, a.add(0, 1.0, 0), b.add(0, 1.0, 0), 0.4);
			new Fx(Fx.Kind.SPARK, 0xFFFFFF).line(level, a.add(0, 2.2, 0), b.add(0, 2.2, 0), 0.6);
			this.sound(level, SoundEvents.TRIDENT_THUNDER.value(), 3.0F, 0.6F);
			this.sound(level, SoundEvents.WITHER_BREAK_BLOCK, 2.0F, 0.6F);
		});
	}

	// ------------------------------------------------------------------ Mahoraga

	/** Ten Shadows: Mahoraga rises from his shadow. Sukuna takes no damage until it falls; at a full turn of its wheel the fight is lost. */
	private void summonMahoraga(final ServerLevel level, final LivingEntity target) {
		this.playAnim(CreatureAnim.SUMMON);
		this.sound(level, SoundEvents.WARDEN_EMERGE, 3.0F, 0.6F);
		Vec3 at = this.ground(this.position()).add(reach(this.ground(this.position()), this.arenaCenter(), 5.0).subtract(this.ground(this.position())));
		this.mechanicSpots.add(at);
		this.during(30, 3, () -> new Fx(Fx.Kind.SMOKE, 0x0A0A0C).burst(level, at.add(0, 0.2, 0), 20, 1.6, 0.02));
		this.after(30, () -> {
			Mahoraga m = ModEntities.MAHORAGA.create(level, EntitySpawnReason.MOB_SUMMONED);
			if (m == null) {
				return;
			}
			m.snapTo(at.x, at.y, at.z, this.getYRot(), 0.0F);
			m.addTag(Attacks.MINION_TAG);
			m.serve(this);
			level.addFreshEntity(m);
			m.setTarget(target);
			this.mahoraga = m;
			this.minions.add(m);
			this.sound(level, at, SoundEvents.BELL_BLOCK, 3.0F, 0.4F);
			level.sendParticles(ParticleTypes.LARGE_SMOKE, at.x, at.y + 1, at.z, 60, 1.5, 1.5, 1.5, 0.02);
			// Sukuna fights on (slower) while Mahoraga stands; the mechanic itself lasts until it falls or fully adapts
			this.endMechanic();
		});
	}

	/** Mahoraga fell: Sukuna can be hurt again. */
	void mahoragaDown(final ServerLevel level) {
		this.mahoraga = null;
		this.cleared(level, "sukuna_mahoraga");
	}

	/** Mahoraga has adapted to its enemies' existence: the fight is lost. */
	void mahoragaAdapted(final ServerLevel level) {
		this.mahoraga = null;
		this.title(level, Component.translatable("raid.minecraft_mode.mahoraga.complete").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD), Component.empty());
		this.wipe(level, "sukuna_mahoraga");
	}

	// ------------------------------------------------------------------ Malevolent Shrine

	/**
	 * Domain Expansion: the shrine rises in the middle of the arena, Sukuna steps out in front of it, and the domain (a circle of
	 * {@link #DOMAIN_RADIUS} around the shrine, drawn and felt by everyone in it: {@code Domains}) cuts everyone inside every half
	 * second. Its barrier is open, so one can run out past its edge; inside, only a Simple Domain (blue circles, two waves) keeps the
	 * sure hit off: the first wave holds until the second is up, then breaks.
	 */
	private void domain(final ServerLevel level, final LivingEntity target) {
		this.playAnim(CreatureAnim.CAST);
		this.sound(level, SoundEvents.WARDEN_ROAR, 3.0F, 0.5F);
		Vec3 center = this.arenaCenter();
		Vec3 front = reach(center, target.position(), 1.0).subtract(center);
		// he steps out in front of where the shrine will stand
		Vec3 stand = center.add(front.scale(SHRINE_FRONT));
		level.sendParticles(ParticleTypes.LARGE_SMOKE, this.getX(), this.getY(0.5), this.getZ(), 30, 0.6, 1.0, 0.6, 0.02);
		this.teleportTo(stand.x, stand.y, stand.z);
		MalevolentShrine s = ModEntities.MALEVOLENT_SHRINE.create(level, EntitySpawnReason.MOB_SUMMONED);
		if (s != null) {
			s.snapTo(center.x, center.y, center.z, (float)Math.toDegrees(Math.atan2(-front.x, front.z)), 0.0F);
			s.setYBodyRot(s.getYRot());
			s.setYHeadRot(s.getYRot());
			level.addFreshEntity(s);
			this.shrine = s;
			level.sendParticles(ParticleTypes.LARGE_SMOKE, center.x, center.y + 3, center.z, 160, 6, 4, 6, 0.03);
			// the shrine's mouth gapes now and then
			this.during(DOMAIN_TICKS, 40, () -> s.playAnim(CreatureAnim.ROAR));
		}
		Domains.Domain domain = Domains.open(level, center, (float)DOMAIN_RADIUS, DomainStyle.MALEVOLENT_SHRINE, DOMAIN_TICKS);
		this.openDomain = domain;
		int zones = Math.max(2, (this.fighters(level).size() + 1) / 2 + 1);
		List<Vec3> first = this.shelters(zones, center);
		List<Vec3> second = this.shelters(zones, center);
		this.mechanicSpots.addAll(first);
		// first wave: shown at once, holds 30..115; second wave: shown at 95, holds 110..190
		this.simpleDomains(level, first, 0, 115);
		this.after(95, () -> {
			this.mechanicSpots.clear();
			this.mechanicSpots.addAll(second);
			this.simpleDomains(level, second, 0, 95);
		});
		for (int t = 30; t < DOMAIN_TICKS - 10; t += DOMAIN_STEP) {
			final int at = t;
			this.after(t, () -> this.sureHit(level, domain, at < 112 ? first : second));
		}
		this.after(DOMAIN_TICKS, () -> {
			if (this.shrine != null) {
				this.shrine.discard();
				this.shrine = null;
			}
			this.cleared(level, "sukuna_domain");
		});
	}

	/** {@code count} Simple Domain spots inside the domain, spread around, clear of the shrine and of Sukuna. */
	private List<Vec3> shelters(final int count, final Vec3 center) {
		List<Vec3> out = new ArrayList<>();
		double base = this.random.nextDouble() * Math.PI * 2;
		for (int i = 0; i < count; i++) {
			Vec3 best = null;
			for (int tries = 0; tries < 16 && best == null; tries++) {
				double a = base + Math.PI * 2 * i / count + (this.random.nextDouble() - 0.5) * 0.7;
				double r = SHRINE_CLEAR + this.random.nextDouble() * (DOMAIN_RADIUS - 3 - SHRINE_CLEAR);
				Vec3 p = center.add(Math.cos(a) * r, 0, Math.sin(a) * r);
				if (p.subtract(this.position()).horizontalDistance() >= 5) {
					best = p;
				}
			}
			out.add(best != null ? best : center.add(Math.cos(base + i) * (DOMAIN_RADIUS - 4), 0, Math.sin(base + i) * (DOMAIN_RADIUS - 4)));
		}
		return out;
	}

	/** Draws Simple Domain circles at {@code spots} from {@code from} to {@code until} ticks from now. */
	private void simpleDomains(final ServerLevel level, final List<Vec3> spots, final int from, final int until) {
		this.during(until - from, 5, () -> {
			for (Vec3 s : spots) {
				Telegraph.ring(level, s, SIMPLE_DOMAIN_RADIUS, SIMPLE_DOMAIN, 1.4F);
				new Fx(Fx.Kind.RING, SIMPLE_DOMAIN).still(level, Fx.Kind.RING, s.add(0, 0.2, 0));
			}
		});
	}

	/** The domain's sure hit: everyone inside {@code domain} and outside {@code safe} is cut; slashes fly all over it. */
	private void sureHit(final ServerLevel level, final Domains.Domain domain, final List<Vec3> safe) {
		Fx cut = new Fx(Fx.Kind.SLASH, SLASH);
		Fx blood = new Fx(Fx.Kind.SLASH, BLOOD);
		for (int i = 0; i < 16; i++) {
			double r = this.random.nextDouble() * DOMAIN_RADIUS;
			double a = this.random.nextDouble() * Math.PI * 2;
			(i % 3 == 0 ? blood : cut).burst(level, domain.center().add(Math.cos(a) * r, 0.5 + this.random.nextDouble() * 3, Math.sin(a) * r), 2, 0.8, 0.15);
		}
		float share = DOMAIN_SHARE * this.difficulty().damage;
		for (ServerPlayer p : this.fighters(level)) {
			if (!domain.contains(p)) {
				continue;
			}
			boolean sheltered = false;
			for (Vec3 s : safe) {
				if (p.position().subtract(s).horizontalDistance() <= SIMPLE_DOMAIN_RADIUS) {
					sheltered = true;
				}
			}
			if (!sheltered) {
				RaidDamage.portion(level, p, this, share);
				// around them, not in front of their own eyes
				cut.burst(level, p.position().add(0, 0.4, 0), 3, 1.4, 0.15);
				blood.burst(level, p.position().add(0, 0.3, 0), 2, 1.2, 0.1);
			}
		}
		this.sound(level, domain.center(), SoundEvents.PLAYER_ATTACK_SWEEP, 2.0F, 1.4F + this.random.nextFloat() * 0.4F);
	}


	// ------------------------------------------------------------------ Reverse Cursed Technique

	/** Twice a fight, at {@link #RCT_AT}, he channels a heal; {@link #RCT_BREAK} of his health in damage during it breaks it. */
	private void tickReverse(final ServerLevel level) {
		float fraction = this.getHealth() / this.getMaxHealth();
		if (this.rctTicks > 0) {
			this.rctTicks--;
			Vec3 chest = this.position().add(0, this.getBbHeight() * 0.6, 0);
			if (this.rctTicks % 4 == 0) {
				new Fx(Fx.Kind.ORB, 0xF0FFF4).burst(level, chest, 4, 0.7, 0.03);
			}
			if ((this.rctFrom - this.getHealth()) / this.getMaxHealth() >= RCT_BREAK) {
				this.rctTicks = 0;
				this.endMechanic();
				this.title(level, Component.translatable("raid.minecraft_mode.sukuna.rct_broken").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD), Component.empty());
				this.sound(level, SoundEvents.GLASS_BREAK, 2.5F, 0.6F);
			} else if (this.rctTicks == 0) {
				this.heal(this.getMaxHealth() * RCT_HEAL);
				this.title(level, Component.translatable("raid.minecraft_mode.sukuna.rct_healed").withStyle(ChatFormatting.RED, ChatFormatting.BOLD), Component.empty());
				this.sound(level, SoundEvents.BEACON_POWER_SELECT, 2.5F, 0.6F);
			}
			return;
		}
		if (this.heals < RCT_AT.length && fraction <= RCT_AT[this.heals] && !this.inMechanic() && !this.mahoragaUp()) {
			this.startReverse(level);
		}
	}

	private void startReverse(final ServerLevel level) {
		this.heals++;
		this.rctTicks = RCT_TICKS;
		this.rctFrom = this.getHealth();
		this.setBusy(RCT_TICKS);
		this.playAnim(CreatureAnim.CAST);
		this.title(level, Component.translatable("raid.minecraft_mode.sukuna.rct").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD),
			Component.translatable("raid.minecraft_mode.sukuna.rct.hint", Math.round(RCT_BREAK * 100)).withStyle(ChatFormatting.YELLOW));
		this.sound(level, SoundEvents.BEACON_ACTIVATE, 2.5F, 0.5F);
	}

	/** Starts the Reverse Cursed Technique now if he has a heal left (tests). */
	public boolean runReverse() {
		if (!(this.level() instanceof ServerLevel level) || this.heals >= RCT_AT.length || this.rctTicks > 0) {
			return false;
		}
		this.startReverse(level);
		return true;
	}

	public boolean reversing() {
		return this.rctTicks > 0;
	}

	public int healsLeft() {
		return RCT_AT.length - this.heals;
	}

	@Override
	public void remove(final RemovalReason reason) {
		if (this.shrine != null) {
			this.shrine.discard();
		}
		// the domain falls with him
		if (this.openDomain != null && this.level() instanceof ServerLevel level) {
			Domains.close(level, this.openDomain);
		}
		super.remove(reason);
	}
}
