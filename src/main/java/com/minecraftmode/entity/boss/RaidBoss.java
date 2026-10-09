package com.minecraftmode.entity.boss;

import com.minecraftmode.entity.CreatureAnim;
import com.minecraftmode.entity.CreatureMob;
import com.minecraftmode.entity.combat.Attacks;
import com.minecraftmode.entity.combat.MobProjectile;
import com.minecraftmode.entity.combat.Telegraph;
import com.minecraftmode.job.skill.Fx;
import com.minecraftmode.job.skill.SkillScheduler;
import com.minecraftmode.raid.Arenas;
import com.minecraftmode.raid.BossDef;
import com.minecraftmode.raid.RaidAffix;
import com.minecraftmode.raid.RaidBosses;
import com.minecraftmode.raid.RaidDamage;
import com.minecraftmode.raid.RaidDifficulty;
import com.minecraftmode.raid.RaidDimension;
import com.minecraftmode.raid.Raids;
import com.minecraftmode.registry.ModEffects;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Base of the six raid bosses. Health: the health attribute is always {@link #HEALTH_BAR}; the real
 * toughness ({@link BossDef#health()} x (1 + 0.6 x (party size - 1))) comes from dividing all
 * damage taken, so bosses can be far tougher than the 1024 attribute cap. Phases start at the
 * definition's health fractions; each phase is announced with a title and unlocks more
 * {@link Pattern}s. Every area attack is telegraphed on the ground first ({@link Telegraph}).
 */
public abstract class RaidBoss extends CreatureMob {
	public static final float HEALTH_BAR = 1000.0F;
	/** Extra health per party member beyond the first. */
	public static final float PARTY_SCALE = 0.6F;
	private static final int RETARGET_TICKS = 160;

	private final ServerBossEvent bossEvent;
	private float divisor = 1.0F;
	private int partySize = 1;
	private boolean configured;
	private @Nullable BlockPos home;
	private int globalCooldown = 70;
	private int busyTicks;
	private int invulnerableTicks;
	private final Map<String, Integer> cooldowns = new HashMap<>();
	protected final List<Mob> minions = new ArrayList<>();
	private @Nullable Vec3 hoverTarget;
	private int hoverTicks;
	private RaidDifficulty difficulty = RaidDifficulty.NORMAL;
	private List<RaidAffix> affixes = List.of();
	private boolean enraged;
	private int eruptions;

	protected RaidBoss(final EntityType<? extends RaidBoss> type, final Level level) {
		super(type, level);
		BossDef def = this.def();
		this.bossEvent = new ServerBossEvent(Mth.createInsecureUUID(this.random), Component.translatable(def.nameKey()), def.color(), BossEvent.BossBarOverlay.NOTCHED_10);
		this.bossEvent.setDarkenScreen(false);
		this.xpReward = 100 + def.lo() * 3;
		this.setPersistenceRequired();
		if (this.flies()) {
			this.moveControl = new FlyingMoveControl<>(this, 20, true);
			this.setNoGravity(true);
		}
	}

	public BossDef def() {
		BossDef def = RaidBosses.def(this.getType());
		if (def == null) {
			throw new IllegalStateException("No boss definition for " + this.getType());
		}
		return def;
	}

	public static AttributeSupplier.Builder createAttributes(final BossDef def) {
		return Monster.createMonsterAttributes()
			.add(Attributes.MAX_HEALTH, HEALTH_BAR)
			.add(Attributes.ATTACK_DAMAGE, def.damage())
			.add(Attributes.ARMOR, def.armor())
			.add(Attributes.ARMOR_TOUGHNESS, def.armor() / 2)
			.add(Attributes.MOVEMENT_SPEED, def.speed())
			.add(Attributes.FLYING_SPEED, def.speed() * 1.6)
			.add(Attributes.FOLLOW_RANGE, 64.0)
			.add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
			.add(Attributes.STEP_HEIGHT, 1.5)
			.add(Attributes.ATTACK_KNOCKBACK, 1.0);
	}

	/** True for bosses that fly around the arena instead of walking. */
	protected boolean flies() {
		return false;
	}

	/** True for bosses that stay where they spawned (the kraken in its pool). */
	protected boolean anchored() {
		return false;
	}

	@Override
	public boolean isFlyingCreature() {
		return this.flies();
	}

	@Override
	protected PathNavigation createNavigation(final Level level) {
		if (this.flies()) {
			FlyingPathNavigation navigation = new FlyingPathNavigation(this, level);
			navigation.setCanOpenDoors(false);
			return navigation;
		}
		return super.createNavigation(level);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(1, new PatternGoal());
		this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, true) {
			@Override
			public boolean canUse() {
				return !RaidBoss.this.flies() && !RaidBoss.this.anchored() && RaidBoss.this.busyTicks <= 0 && super.canUse();
			}

			@Override
			public boolean canContinueToUse() {
				return RaidBoss.this.busyTicks <= 0 && super.canContinueToUse();
			}
		});
		this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 24.0F));
		this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
		this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, false));
	}

	// ------------------------------------------------------------------ setup

	/** Scales health to the party and anchors the boss to its arena (null home = free boss, e.g. summoned by command). */
	public void configure(final int partySize, final @Nullable BlockPos home) {
		this.configure(partySize, home, RaidDifficulty.NORMAL, List.of());
	}

	/** As {@link #configure(int, BlockPos)} on {@code difficulty} (toughness and damage) with the cycle's {@code affixes}. */
	public void configure(final int partySize, final @Nullable BlockPos home, final RaidDifficulty difficulty, final List<RaidAffix> affixes) {
		BossDef def = this.def();
		this.partySize = Math.max(1, partySize);
		this.home = home;
		this.difficulty = difficulty;
		this.affixes = List.copyOf(affixes);
		float effective = (float)def.health() * (1.0F + PARTY_SCALE * (this.partySize - 1)) * difficulty.health;
		if (this.affixes.contains(RaidAffix.FORTIFIED)) {
			effective /= 0.85F;
		}
		this.divisor = effective / HEALTH_BAR;
		this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(def.damage() * difficulty.damage);
		this.setHealth(this.getMaxHealth());
		this.configured = true;
		this.setPhase(1);
		this.refreshName();
	}

	private void refreshName() {
		BossDef def = this.def();
		MutableComponent name = Component.literal("[Lv." + def.hi() + "] ").withStyle(ChatFormatting.GRAY)
			.append(Component.translatable(def.nameKey()).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
		if (this.difficulty != RaidDifficulty.NORMAL) {
			name.append(Component.literal(" (").append(Component.translatable(this.difficulty.nameKey())).append(")")
				.withStyle(this.difficulty == RaidDifficulty.NIGHTMARE ? ChatFormatting.DARK_RED : ChatFormatting.LIGHT_PURPLE));
		}
		this.setCustomName(name);
		this.setCustomNameVisible(false);
		this.bossEvent.setName(name);
	}

	public @Nullable BlockPos home() {
		return this.home;
	}

	public int partySize() {
		return this.partySize;
	}

	public RaidDifficulty difficulty() {
		return this.difficulty;
	}

	public List<RaidAffix> affixes() {
		return this.affixes;
	}

	/** Damage needed per health point of the bar. */
	public float divisor() {
		return this.divisor;
	}

	@Override
	protected void addAdditionalSaveData(final ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putBoolean("Configured", this.configured);
		output.putInt("PartySize", this.partySize);
		output.putFloat("Divisor", this.divisor);
		output.putInt("Phase", this.phase());
		output.putString("Difficulty", this.difficulty.id());
		output.putString("Affixes", String.join(",", this.affixes.stream().map(RaidAffix::id).toList()));
		output.putBoolean("Enraged", this.enraged);
		output.putInt("Eruptions", this.eruptions);
		if (this.home != null) {
			output.putLong("Home", this.home.asLong());
		}
	}

	@Override
	protected void readAdditionalSaveData(final ValueInput input) {
		super.readAdditionalSaveData(input);
		this.configured = input.getBooleanOr("Configured", false);
		this.partySize = input.getIntOr("PartySize", 1);
		this.divisor = input.getFloatOr("Divisor", 1.0F);
		this.setPhase(input.getIntOr("Phase", 1));
		this.difficulty = RaidDifficulty.byId(input.getStringOr("Difficulty", "normal"));
		String affixes = input.getStringOr("Affixes", "");
		this.affixes = affixes.isEmpty() ? List.of() : java.util.Arrays.stream(affixes.split(",")).map(RaidAffix::byId).toList();
		this.enraged = input.getBooleanOr("Enraged", false);
		this.eruptions = input.getIntOr("Eruptions", 0);
		this.home = input.getLong("Home").map(BlockPos::of).orElse(null);
		if (this.configured) {
			this.refreshName();
		}
	}

	// ------------------------------------------------------------------ ticking

	@Override
	protected void customServerAiStep(final ServerLevel level) {
		super.customServerAiStep(level);
		if (!this.configured) {
			this.configure(1, null);
		}
		if (this.tickCount > 40 && RaidDimension.is(level) && !Raids.ownsBoss(this)) {
			// the fight this boss belonged to is over (server restart, everyone left)
			this.discard();
			return;
		}
		if (this.busyTicks > 0) {
			this.busyTicks--;
			this.getNavigation().stop();
		}
		if (this.invulnerableTicks > 0) {
			this.invulnerableTicks--;
		}
		this.minions.removeIf(m -> !m.isAlive());
		this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());
		this.checkPhase(level);
		this.fightTicks++;
		this.tickMechanics(level);
		this.tickAffixes(level);
		this.leash();
		if (this.flies() && this.busyTicks <= 0) {
			this.hover();
		}
		if (this.tickCount % RETARGET_TICKS == 0) {
			List<ServerPlayer> fighters = this.fighters(level);
			if (!fighters.isEmpty()) {
				this.setTarget(fighters.get(this.random.nextInt(fighters.size())));
			}
		}
		this.ambientFx(level);
	}

	/** The cycle's raid modifiers (Heroic and Nightmare). */
	private void tickAffixes(final ServerLevel level) {
		if (this.affixes.isEmpty()) {
			return;
		}
		float fraction = this.getHealth() / this.getMaxHealth();
		if (this.affixes.contains(RaidAffix.ENRAGE) && !this.enraged && fraction < 0.3F) {
			this.enraged = true;
			this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(this.getAttribute(Attributes.ATTACK_DAMAGE).getBaseValue() * 1.3);
			this.title(level, Component.translatable(RaidAffix.ENRAGE.nameKey()).withStyle(ChatFormatting.RED, ChatFormatting.BOLD),
				Component.translatable(RaidAffix.ENRAGE.descKey()).withStyle(ChatFormatting.GOLD));
		}
		if (this.affixes.contains(RaidAffix.BLEEDING) && this.fightTicks % 400 == 200) {
			for (ServerPlayer p : this.fighters(level)) {
				p.addEffect(new MobEffectInstance(ModEffects.BLEEDING, 80, 0), this);
			}
		}
		if (this.affixes.contains(RaidAffix.GLOOM) && this.fightTicks % 600 == 300) {
			for (ServerPlayer p : this.fighters(level)) {
				p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 100, 0), this);
			}
		}
		if (this.affixes.contains(RaidAffix.VOLATILE) && this.eruptions < 3 && fraction <= 0.75F - 0.25F * this.eruptions) {
			this.eruptions++;
			Vec3 at = this.position();
			Telegraph.circle(level, at, 7.0, 40, Telegraph.ORANGE, () -> {
				if (this.isAlive()) {
					Attacks.hitAll(this, Attacks.inCircle(level, at, 7.0, 5.0), Attacks.damage(this, 1.4F), null);
					level.sendParticles(ParticleTypes.EXPLOSION, at.x, at.y + 1, at.z, 6, 3.0, 0.5, 3.0, 0.0);
				}
			});
		}
	}

	/** Theme particles around the boss (every few ticks). */
	protected void ambientFx(final ServerLevel level) {
	}

	private void checkPhase(final ServerLevel level) {
		List<Float> thresholds = this.def().phases();
		float fraction = this.getHealth() / this.getMaxHealth();
		int phase = 1;
		for (float t : thresholds) {
			if (fraction <= t) {
				phase++;
			}
		}
		if (phase > this.phase()) {
			this.setPhase(phase);
			this.invulnerableTicks = Math.max(this.invulnerableTicks, 30);
			this.globalCooldown = 50;
			this.playAnim(CreatureAnim.ROAR);
			level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 3.0F, 0.7F);
			for (LivingEntity e : Attacks.inCircle(level, this.position(), 6.0, 6.0)) {
				Attacks.knock(e, this.position(), 1.2, 0.5);
			}
			this.title(level, Component.translatable("raid.minecraft_mode.phase", phase).withStyle(ChatFormatting.RED, ChatFormatting.BOLD),
				Component.translatable(this.def().nameKey() + ".phase" + phase).withStyle(ChatFormatting.GOLD));
			this.onPhase(level, phase);
		}
	}

	/** Called once when a new phase starts (2, 3, ...). */
	protected void onPhase(final ServerLevel level, final int phase) {
	}

	/** Keeps the boss inside its arena. */
	private void leash() {
		if (this.home == null) {
			return;
		}
		Vec3 spawn = Arenas.bossSpawn(this.home, this.def().arena());
		if (this.anchored()) {
			if (this.position().distanceToSqr(spawn) > 2.0) {
				this.teleportTo(spawn.x, spawn.y, spawn.z);
			}
			this.getNavigation().stop();
			return;
		}
		if (!Arenas.inside(this.home, this.position(), 1.0) || this.getY() < this.home.getY() - 4) {
			this.teleportTo(spawn.x, spawn.y + (this.flies() ? 6 : 0), spawn.z);
		}
	}

	/** Flying bosses circle the arena between waypoints a few blocks above the floor. */
	private void hover() {
		Vec3 center = this.home != null ? Vec3.atBottomCenterOf(this.home) : this.position();
		if (this.hoverTarget == null || --this.hoverTicks <= 0 || this.position().distanceToSqr(this.hoverTarget) < 4.0) {
			LivingEntity target = this.getTarget();
			Vec3 around = target != null && this.random.nextFloat() < 0.6F ? target.position() : center;
			double a = this.random.nextDouble() * Math.PI * 2;
			double r = 5 + this.random.nextDouble() * 7;
			Vec3 p = around.add(Math.cos(a) * r, 0, Math.sin(a) * r);
			if (this.home != null && p.subtract(center).horizontalDistance() > Arenas.RADIUS - 3) {
				p = center.add(p.subtract(center).multiply(1, 0, 1).normalize().scale(Arenas.RADIUS - 4));
			}
			double baseY = this.home != null ? this.home.getY() : Math.max(this.getY(), around.y);
			this.hoverTarget = new Vec3(p.x, baseY + 6 + this.random.nextDouble() * 4, p.z);
			this.hoverTicks = 60 + this.random.nextInt(40);
		}
		this.moveControl.setWantedPosition(this.hoverTarget.x, this.hoverTarget.y, this.hoverTarget.z, 1.0);
		LivingEntity target = this.getTarget();
		if (target != null) {
			this.getLookControl().setLookAt(target, 30.0F, 30.0F);
		}
	}

	// ------------------------------------------------------------------ damage

	@Override
	public boolean hurtServer(final ServerLevel level, final DamageSource source, final float damage) {
		if (this.invulnerableTicks > 0 && !source.isCreativePlayer() && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
			level.sendParticles(ParticleTypes.ENCHANTED_HIT, this.getX(), this.getY(0.6), this.getZ(), 6, 0.5, 0.5, 0.5, 0.2);
			return false;
		}
		Entity attacker = source.getEntity();
		if (attacker != null && !(attacker instanceof Player) && attacker != this) {
			return false;
		}
		return super.hurtServer(level, source, damage / this.divisor);
	}

	public boolean isInvulnerablePhase() {
		return this.invulnerableTicks > 0;
	}

	protected void setInvulnerableFor(final int ticks) {
		this.invulnerableTicks = Math.max(this.invulnerableTicks, ticks);
	}

	protected void clearInvulnerable() {
		this.invulnerableTicks = 0;
	}

	@Override
	public void die(final DamageSource source) {
		super.die(source);
		if (this.level() instanceof ServerLevel level) {
			this.bossEvent.setProgress(0.0F);
			this.title(level, Component.translatable("raid.minecraft_mode.victory").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
				Component.translatable(this.def().nameKey()).withStyle(ChatFormatting.YELLOW));
			Raids.onBossDefeated(this);
		}
	}

	@Override
	public void remove(final RemovalReason reason) {
		for (Mob minion : this.minions) {
			if (minion.isAlive()) {
				minion.discard();
			}
		}
		this.bossEvent.removeAllPlayers();
		super.remove(reason);
	}

	@Override
	public void startSeenByPlayer(final ServerPlayer player) {
		super.startSeenByPlayer(player);
		this.bossEvent.addPlayer(player);
	}

	@Override
	public void stopSeenByPlayer(final ServerPlayer player) {
		super.stopSeenByPlayer(player);
		this.bossEvent.removePlayer(player);
	}

	@Override
	public boolean removeWhenFarAway(final double distSqr) {
		return false;
	}

	@Override
	public boolean canBreatheUnderwater() {
		return true;
	}

	@Override
	public boolean causeFallDamage(final double fallDistance, final float damageModifier, final DamageSource damageSource) {
		return false;
	}

	@Override
	public boolean isPushable() {
		return false;
	}

	@Override
	public boolean isPushedByFluid() {
		return false;
	}

	@Override
	public boolean canUsePortal(final boolean ignorePassenger) {
		return false;
	}

	@Override
	protected boolean canRide(final Entity vehicle) {
		return false;
	}

	@Override
	protected SoundEvent getHurtSound(final DamageSource source) {
		return SoundEvents.RAVAGER_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.WITHER_DEATH;
	}

	@Override
	protected float getSoundVolume() {
		return 2.5F;
	}

	@Override
	public boolean doHurtTarget(final ServerLevel level, final Entity target) {
		this.playAnim(CreatureAnim.ATTACK);
		return super.doHurtTarget(level, target);
	}

	// ------------------------------------------------------------------ patterns

	/** What a pattern does to its target. */
	@FunctionalInterface
	protected interface Action {
		void run(ServerLevel level, LivingEntity target);
	}

	/**
	 * One attack. Available from {@code phase} on, every {@code cooldown} ticks, while the target is
	 * within {@code range} blocks (0 = any distance); the boss stands still for {@code busy} ticks.
	 */
	protected record Pattern(String id, int phase, int cooldown, double range, int busy, Action action) {
	}

	protected static Pattern pattern(final String id, final int phase, final int cooldown, final double range, final int busy, final Action action) {
		return new Pattern(id, phase, cooldown, range, busy, action);
	}

	/** The boss's patterns, built once per instance. */
	protected abstract List<Pattern> patterns();

	private @Nullable List<Pattern> patternCache;

	private List<Pattern> patternList() {
		if (this.patternCache == null) {
			this.patternCache = List.copyOf(this.patterns());
		}
		return this.patternCache;
	}

	/** Pattern ids in definition order (tests and debugging). */
	public List<String> patternIds() {
		return this.patternList().stream().map(Pattern::id).toList();
	}

	/** Runs pattern {@code id} at {@code target} right now, ignoring phase and cooldowns (tests and debugging). */
	public boolean runPattern(final String id, final LivingEntity target) {
		if (!(this.level() instanceof ServerLevel level)) {
			return false;
		}
		for (Pattern p : this.patternList()) {
			if (p.id().equals(id)) {
				p.action().run(level, target);
				return true;
			}
		}
		return false;
	}

	// ------------------------------------------------------------------ mechanics

	/** Least time between two mechanics (and from the start of the fight to the first one). */
	public static final int MECHANIC_GAP = 45 * 20;
	private static final int MECHANIC_GRACE = 30 * 20;

	/**
	 * A lethal raid mechanic: it first runs when health falls to {@code firstAt} (1.0 = from the
	 * start), then every {@code interval} ticks (0 = once). Regular patterns pause for {@code duration}
	 * ticks while it plays out; no two mechanics run within {@link #MECHANIC_GAP} ticks.
	 */
	protected record Mechanic(String id, float firstAt, int interval, int duration, Action action) {
	}

	protected static Mechanic mechanic(final String id, final float firstAt, final int intervalSeconds, final int duration, final Action action) {
		return new Mechanic(id, firstAt, intervalSeconds * 20, duration, action);
	}

	/** The mechanics of this boss (easy bosses one, hard ones up to three). */
	protected List<Mechanic> mechanics() {
		return List.of();
	}

	private @Nullable List<Mechanic> mechanicCache;
	private final Map<String, Long> mechanicDue = new HashMap<>();
	private long fightTicks;
	private long lastMechanic = MECHANIC_GRACE - MECHANIC_GAP;
	private int mechanicTicks;
	/** Positions that matter in the running mechanic (safe zones, anchors, seals), for tests and hints. */
	protected final List<Vec3> mechanicSpots = new ArrayList<>();

	private List<Mechanic> mechanicList() {
		if (this.mechanicCache == null) {
			this.mechanicCache = List.copyOf(this.mechanics());
		}
		return this.mechanicCache;
	}

	private void tickMechanics(final ServerLevel level) {
		if (this.mechanicTicks > 0) {
			this.mechanicTicks--;
			if (this.mechanicTicks == 0) {
				this.mechanicSpots.clear();
			}
			return;
		}
		if (this.fightTicks - this.lastMechanic < MECHANIC_GAP) {
			return;
		}
		LivingEntity target = this.getTarget();
		if (target == null || this.fighters(level).isEmpty()) {
			return;
		}
		float fraction = this.getHealth() / this.getMaxHealth();
		for (Mechanic m : this.mechanicList()) {
			Long due = this.mechanicDue.get(m.id());
			if (due == null) {
				if (fraction > m.firstAt()) {
					continue;
				}
				due = this.fightTicks;
				this.mechanicDue.put(m.id(), due);
			}
			if (this.fightTicks >= due) {
				this.startMechanic(level, m, target);
				return;
			}
		}
	}

	private void startMechanic(final ServerLevel level, final Mechanic m, final LivingEntity target) {
		int interval = this.affixes.contains(RaidAffix.TURBULENT) ? Math.round(m.interval() * 0.8F) : m.interval();
		this.mechanicDue.put(m.id(), interval > 0 ? this.fightTicks + interval : Long.MAX_VALUE);
		this.lastMechanic = this.fightTicks;
		this.mechanicTicks = m.duration();
		this.mechanicSpots.clear();
		this.setBusy(m.duration());
		this.title(level, Component.translatable("raid.minecraft_mode.mechanic." + m.id()).withStyle(ChatFormatting.RED, ChatFormatting.BOLD),
			Component.translatable("raid.minecraft_mode.mechanic." + m.id() + ".hint").withStyle(ChatFormatting.YELLOW));
		for (ServerPlayer p : this.fighters(level)) {
			p.sendSystemMessage(Component.literal("⚠ ").append(Component.translatable("raid.minecraft_mode.mechanic." + m.id())).append(": ")
				.append(Component.translatable("raid.minecraft_mode.mechanic." + m.id() + ".hint")).withStyle(ChatFormatting.GOLD));
		}
		m.action().run(level, target);
	}

	/** True while a mechanic plays out (patterns wait). */
	public boolean inMechanic() {
		return this.mechanicTicks > 0;
	}

	public List<String> mechanicIds() {
		return this.mechanicList().stream().map(Mechanic::id).toList();
	}

	/** Starts mechanic {@code id} now, ignoring its triggers and the gap (tests and debugging). */
	public boolean runMechanic(final String id, final LivingEntity target) {
		if (!(this.level() instanceof ServerLevel level)) {
			return false;
		}
		for (Mechanic m : this.mechanicList()) {
			if (m.id().equals(id)) {
				this.startMechanic(level, m, target);
				return true;
			}
		}
		return false;
	}

	/** Ends the running mechanic early (a team cleared it): patterns resume shortly. */
	protected void endMechanic() {
		this.mechanicTicks = Math.min(this.mechanicTicks, 10);
		this.busyTicks = Math.min(this.busyTicks, 10);
	}

	/**
	 * Spawns stationary mechanic targets ({@code type} with {@code health}) at {@code at}, named
	 * {@code nameKey}; they are minions (cleaned up with the boss) and do not move or attack.
	 */
	protected <T extends Mob> List<T> targets(final ServerLevel level, final EntityType<T> type, final List<Vec3> at, final float health, final String nameKey) {
		List<T> out = new ArrayList<>();
		for (Vec3 p : at) {
			T mob = type.create(level, net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED);
			if (mob == null) {
				continue;
			}
			mob.snapTo(p.x, p.y, p.z, this.random.nextFloat() * 360.0F, 0.0F);
			mob.setNoAi(true);
			mob.setPersistenceRequired();
			mob.addTag(Attacks.MINION_TAG);
			var maxHealth = mob.getAttribute(Attributes.MAX_HEALTH);
			if (maxHealth != null) {
				maxHealth.setBaseValue(health);
			}
			mob.setHealth(health);
			mob.setCustomName(Component.translatable(nameKey).withStyle(ChatFormatting.RED));
			mob.setCustomNameVisible(true);
			level.addFreshEntity(mob);
			level.sendParticles(ParticleTypes.LARGE_SMOKE, p.x, p.y + 0.5, p.z, 20, 0.5, 0.5, 0.5, 0.02);
			this.minions.add(mob);
			out.add(mob);
		}
		return out;
	}

	public List<Vec3> mechanicSpots() {
		return List.copyOf(this.mechanicSpots);
	}

	/** Kills {@code p} unless a totem, a Phoenix Feather or Avalon saves them. */
	protected void lethal(final ServerLevel level, final LivingEntity p) {
		level.sendParticles(ParticleTypes.SOUL, p.getX(), p.getY(0.6), p.getZ(), 30, 0.4, 0.6, 0.4, 0.05);
		RaidDamage.lethal(level, p, this);
	}

	/** Every fighter dies (a failed team mechanic). */
	protected void wipe(final ServerLevel level, final String id) {
		for (ServerPlayer p : this.fighters(level)) {
			p.sendSystemMessage(Component.translatable("raid.minecraft_mode.mechanic.failed", Component.translatable("raid.minecraft_mode.mechanic." + id))
				.withStyle(ChatFormatting.DARK_RED));
			this.lethal(level, p);
		}
	}

	protected void cleared(final ServerLevel level, final String id) {
		for (ServerPlayer p : this.fighters(level)) {
			p.sendOverlayMessage(Component.translatable("raid.minecraft_mode.mechanic.cleared", Component.translatable("raid.minecraft_mode.mechanic." + id))
				.withStyle(ChatFormatting.GREEN));
		}
	}

	/** Runs {@code each} every {@code step} ticks for {@code ticks} ticks while the boss lives. */
	protected void during(final int ticks, final int step, final Runnable each) {
		for (int t = 0; t < ticks; t += step) {
			SkillScheduler.schedule(t + 1, () -> {
				if (this.alive()) {
					each.run();
				}
			});
		}
	}

	/** Runs {@code then} after {@code ticks} ticks if the boss still lives. */
	protected void after(final int ticks, final Runnable then) {
		SkillScheduler.schedule(ticks, () -> {
			if (this.alive()) {
				then.run();
			}
		});
	}

	/** {@code count} spots on the arena floor, spread around, at least {@code minFromBoss} from the boss. */
	protected List<Vec3> spots(final int count, final double minRadius, final double maxRadius, final double minFromBoss) {
		Vec3 c = this.arenaCenter();
		List<Vec3> out = new ArrayList<>();
		double base = this.random.nextDouble() * Math.PI * 2;
		for (int i = 0; i < count; i++) {
			Vec3 best = null;
			for (int tries = 0; tries < 12 && best == null; tries++) {
				double a = base + Math.PI * 2 * i / count + (this.random.nextDouble() - 0.5) * 0.6;
				double r = minRadius + this.random.nextDouble() * (maxRadius - minRadius);
				Vec3 p = c.add(Math.cos(a) * r, 0, Math.sin(a) * r);
				if (p.subtract(this.position()).horizontalDistance() >= minFromBoss) {
					best = p;
				}
			}
			out.add(best != null ? best : c.add(Math.cos(base + i) * maxRadius, 0, Math.sin(base + i) * maxRadius));
		}
		return out;
	}

	/** Players standing within {@code radius} (horizontally) of {@code at}. */
	protected List<ServerPlayer> near(final ServerLevel level, final Vec3 at, final double radius) {
		List<ServerPlayer> out = new ArrayList<>();
		for (ServerPlayer p : this.fighters(level)) {
			if (p.position().subtract(at).horizontalDistance() <= radius && Math.abs(p.getY() - at.y) < 4.0) {
				out.add(p);
			}
		}
		return out;
	}

	/** Shows "name: have/need" above the hotbar of every fighter. */
	protected void tally(final ServerLevel level, final String id, final int have, final int need) {
		for (ServerPlayer p : this.fighters(level)) {
			p.sendOverlayMessage(Component.translatable("raid.minecraft_mode.mechanic.count", Component.translatable("raid.minecraft_mode.mechanic." + id), have, need)
				.withStyle(have >= need ? ChatFormatting.GREEN : ChatFormatting.RED));
		}
	}

	/** Ticks between patterns: shorter in later phases. */
	protected int globalCooldown() {
		return Math.max(22, 54 - 10 * (this.phase() - 1));
	}

	private final class PatternGoal extends Goal {
		PatternGoal() {
			this.setFlags(EnumSet.noneOf(Flag.class));
		}

		@Override
		public boolean canUse() {
			LivingEntity target = RaidBoss.this.getTarget();
			return target != null && target.isAlive();
		}

		@Override
		public boolean requiresUpdateEveryTick() {
			return true;
		}

		@Override
		public void tick() {
			RaidBoss boss = RaidBoss.this;
			boss.cooldowns.replaceAll((k, v) -> v - 1);
			if (--boss.globalCooldown > 0 || boss.busyTicks > 0 || boss.invulnerableTicks > 0) {
				return;
			}
			LivingEntity target = boss.getTarget();
			if (target == null || !(boss.level() instanceof ServerLevel level)) {
				return;
			}
			List<Pattern> ready = new ArrayList<>();
			for (Pattern p : boss.patternList()) {
				if (p.phase() <= boss.phase() && boss.cooldowns.getOrDefault(p.id(), 0) <= 0 && (p.range() <= 0 || boss.distanceTo(target) <= p.range())) {
					ready.add(p);
				}
			}
			if (ready.isEmpty()) {
				return;
			}
			// patterns of the newest phase are favored so the fight visibly changes
			Pattern pick = ready.get(boss.random.nextInt(ready.size()));
			for (Pattern p : ready) {
				if (p.phase() == boss.phase() && p.phase() > 1 && boss.random.nextFloat() < 0.35F) {
					pick = p;
					break;
				}
			}
			boss.cooldowns.put(pick.id(), pick.cooldown());
			boss.globalCooldown = boss.globalCooldown();
			boss.busyTicks = pick.busy();
			if (boss.flies()) {
				boss.getLookControl().setLookAt(target, 40.0F, 40.0F);
			}
			pick.action().run(level, target);
		}
	}

	protected void setBusy(final int ticks) {
		this.busyTicks = Math.max(this.busyTicks, ticks);
	}

	protected boolean alive() {
		return this.isAlive() && !this.isRemoved();
	}

	// ------------------------------------------------------------------ helpers for patterns

	protected float dmg(final float power) {
		return Attacks.damage(this, power);
	}

	/** Players fighting this boss: inside its arena (or within 40 blocks when it has none). */
	public List<ServerPlayer> fighters(final ServerLevel level) {
		Vec3 center = this.home != null ? Vec3.atBottomCenterOf(this.home) : this.position();
		AABB box = new AABB(center, center).inflate(Arenas.RADIUS + 6, 30, Arenas.RADIUS + 6);
		List<ServerPlayer> out = new ArrayList<>();
		for (Player p : level.getEntitiesOfClass(Player.class, box, Attacks::isTarget)) {
			if (p instanceof ServerPlayer sp) {
				out.add(sp);
			}
		}
		return out;
	}

	/** The ground under {@code pos} (the arena floor for bosses with a home). */
	protected Vec3 ground(final Vec3 pos) {
		double y = this.home != null ? this.home.getY() : pos.y;
		return new Vec3(pos.x, y, pos.z);
	}

	protected Vec3 arenaCenter() {
		return this.home != null ? Vec3.atBottomCenterOf(this.home) : this.ground(this.position());
	}

	/** A telegraphed circle: after {@code delay} ticks everyone in it takes {@code power} x attack. */
	protected void circle(final ServerLevel level, final Vec3 at, final double radius, final int delay, final int color, final float power,
		final @Nullable Consumer<LivingEntity> extra, final @Nullable Runnable fx) {
		Telegraph.circle(level, at, radius, delay, color, () -> {
			if (!this.alive()) {
				return;
			}
			Attacks.hitAll(this, Attacks.inCircle(level, at, radius, 4.0), this.dmg(power), extra);
			if (fx != null) {
				fx.run();
			}
		});
	}

	/** A telegraphed ring with a safe middle. */
	protected void donut(final ServerLevel level, final Vec3 at, final double inner, final double outer, final int delay, final int color, final float power,
		final @Nullable Consumer<LivingEntity> extra) {
		Telegraph.donut(level, at, inner, outer, delay, color, () -> {
			if (!this.alive()) {
				return;
			}
			List<LivingEntity> hit = new ArrayList<>();
			for (LivingEntity e : Attacks.inCircle(level, at, outer, 5.0)) {
				if (e.position().subtract(at).horizontalDistance() > inner) {
					hit.add(e);
				}
			}
			Attacks.hitAll(this, hit, this.dmg(power), extra);
			new Fx(Fx.Kind.RING, color).circle(level, Fx.Kind.RING, at.add(0, 0.3, 0), outer * 0.7);
			level.sendParticles(ParticleTypes.EXPLOSION, at.x, at.y + 0.5, at.z, 12, outer * 0.5, 0.2, outer * 0.5, 0.0);
		});
	}

	/** A telegraphed strip. */
	protected void line(final ServerLevel level, final Vec3 from, final Vec3 to, final double width, final int delay, final int color, final float power,
		final @Nullable Consumer<LivingEntity> extra, final @Nullable Runnable fx) {
		Telegraph.line(level, from, to, width, delay, color, () -> {
			if (!this.alive()) {
				return;
			}
			Attacks.hitAll(this, Attacks.inLine(level, from, to, width, 4.0), this.dmg(power), extra);
			if (fx != null) {
				fx.run();
			}
		});
	}

	/** A strip from {@code from} toward {@code toward}, {@code length} long. */
	protected static Vec3 reach(final Vec3 from, final Vec3 toward, final double length) {
		Vec3 dir = toward.subtract(from).multiply(1, 0, 1);
		if (dir.lengthSqr() < 1.0E-4) {
			dir = new Vec3(0, 0, 1);
		}
		return from.add(dir.normalize().scale(length));
	}

	/** A telegraphed cone from {@code origin} toward {@code target}; hits {@code pulses} times. */
	protected void cone(final ServerLevel level, final Vec3 origin, final Vec3 target, final double range, final double halfAngle, final int delay, final int color,
		final float power, final int pulses, final Fx fx, final @Nullable Consumer<LivingEntity> extra) {
		Vec3 to = target.subtract(origin);
		float yaw = (float)Math.toDegrees(Math.atan2(-to.x, to.z));
		Telegraph.cone(level, origin, yaw, range, halfAngle, delay, color, () -> {
			for (int i = 0; i < pulses; i++) {
				SkillScheduler.schedule(i * 5, () -> {
					if (!this.alive()) {
						return;
					}
					Vec3 mouth = this.position().add(0, this.getBbHeight() * 0.6, 0);
					for (int k = 0; k < 8; k++) {
						float a = (float)(yaw - halfAngle + 2 * halfAngle * this.random.nextFloat());
						fx.moving(level, fx.kind(), mouth, Vec3.directionFromRotation(10.0F, a).scale(range / 12.0));
					}
					Attacks.hitAll(this, Attacks.inCone(level, origin, yaw, range, halfAngle), this.dmg(power), extra);
				});
			}
		});
	}

	/** Circles at {@code count} spots around the fighters (some right on them), going off together. */
	protected void rain(final ServerLevel level, final int count, final double radius, final int delay, final int color, final float power,
		final @Nullable Consumer<LivingEntity> extra, final @Nullable Consumer<Vec3> fx) {
		List<ServerPlayer> fighters = this.fighters(level);
		for (int i = 0; i < count; i++) {
			Vec3 at;
			if (!fighters.isEmpty() && i < fighters.size() * 2) {
				Vec3 p = fighters.get(i % fighters.size()).position();
				at = i < fighters.size() ? p : p.add((this.random.nextDouble() - 0.5) * 8, 0, (this.random.nextDouble() - 0.5) * 8);
			} else {
				double a = this.random.nextDouble() * Math.PI * 2;
				double r = this.random.nextDouble() * (Arenas.RADIUS - 3);
				at = this.arenaCenter().add(Math.cos(a) * r, 0, Math.sin(a) * r);
			}
			Vec3 spot = this.ground(at);
			int wait = delay + i * 3;
			this.circle(level, spot, radius, wait, color, power, extra, fx == null ? null : () -> fx.accept(spot));
		}
	}

	/** Fires {@code count} projectiles at {@code target} in a fan of {@code spread} degrees. */
	protected List<MobProjectile> volley(final ServerLevel level, final LivingEntity target, final int count, final float spread, final float power, final ItemStack display,
		final Fx.Kind trail, final int color, final float gravity, final float speed, final boolean homing) {
		Vec3 eye = this.position().add(0, this.getBbHeight() * 0.7, 0);
		Vec3 aim = target.position().add(0, target.getBbHeight() * 0.5, 0).subtract(eye);
		if (gravity > 0.0F) {
			aim = aim.add(0, aim.horizontalDistance() * 0.3, 0);
		}
		List<MobProjectile> out = new ArrayList<>();
		for (int k = 0; k < count; k++) {
			float offset = count == 1 ? 0.0F : -spread / 2 + spread * k / (count - 1);
			Vec3 dir = aim.yRot((float)Math.toRadians(offset));
			MobProjectile p = MobProjectile.of(this, display, trail, color, gravity, this.dmg(power)).launch(eye, dir, speed);
			if (homing) {
				p.homing(target);
			}
			level.addFreshEntity(p);
			out.add(p);
		}
		return out;
	}

	/** Drops a visual projectile from high above onto {@code at} (the damage comes from the matching circle). */
	protected void fallFromSky(final ServerLevel level, final Vec3 at, final ItemStack display, final Fx.Kind trail, final int color, final int ticks) {
		double height = 0.04 * ticks * ticks / 2.0;
		MobProjectile p = MobProjectile.of(this, display, trail, color, 0.04F, 0.0F).visual().launch(at.add(0, Math.min(30, height), 0), new Vec3(0, -0.01, 0), 0.01F);
		p.maxAge(ticks + 10);
		level.addFreshEntity(p);
	}

	/** Pulls everyone within {@code radius} of {@code center} inward. */
	protected void pull(final ServerLevel level, final Vec3 center, final double radius, final double strength) {
		for (LivingEntity e : Attacks.inCircle(level, center, radius, 8.0)) {
			Vec3 in = center.subtract(e.position()).multiply(1, 0, 1);
			if (in.lengthSqr() > 1.0) {
				Attacks.push(e, in.normalize().scale(strength).add(0, 0.15, 0));
			}
		}
	}

	/** Moves the boss to {@code to} over {@code ticks} ticks; {@code each} runs every tick on the way. */
	protected void dash(final Vec3 to, final int ticks, final @Nullable Runnable each) {
		this.setBusy(ticks + 4);
		for (int i = 0; i < ticks; i++) {
			final int left = ticks - i;
			SkillScheduler.schedule(i, () -> {
				if (!this.alive()) {
					return;
				}
				Vec3 step = to.subtract(this.position()).scale(1.0 / left);
				Attacks.push(this, step);
				this.move(net.minecraft.world.entity.MoverType.SELF, step);
				if (each != null) {
					each.run();
				}
			});
		}
	}

	@SuppressWarnings("unchecked")
	protected void summonMinions(final ServerLevel level, final EntityType<? extends Mob> type, final int count, final int max, final LivingEntity target) {
		if (this.minions.size() >= max) {
			return;
		}
		Vec3 at = this.anchored() || this.flies() ? this.ground(target.position()).add(0, 0.2, 0) : this.position();
		this.minions.addAll(Attacks.summon(level, (EntityType<Mob>)type, at, Math.min(count, max - this.minions.size()), target, 900));
		level.sendParticles(ParticleTypes.LARGE_SMOKE, at.x, at.y + 0.5, at.z, 40, 2.0, 0.5, 2.0, 0.02);
		level.playSound(null, at.x, at.y, at.z, SoundEvents.EVOKER_PREPARE_SUMMON, SoundSource.HOSTILE, 1.5F, 0.8F);
	}

	protected void effect(final LivingEntity target, final Holder<MobEffect> effect, final int ticks, final int amplifier) {
		Attacks.effect(target, effect, ticks, amplifier, this);
	}

	protected void sound(final ServerLevel level, final SoundEvent sound, final float volume, final float pitch) {
		level.playSound(null, this.getX(), this.getY(), this.getZ(), sound, SoundSource.HOSTILE, volume, pitch);
	}

	protected void sound(final ServerLevel level, final Vec3 at, final SoundEvent sound, final float volume, final float pitch) {
		level.playSound(null, at.x, at.y, at.z, sound, SoundSource.HOSTILE, volume, pitch);
	}

	protected void boom(final ServerLevel level, final Vec3 at, final double radius) {
		level.sendParticles(ParticleTypes.EXPLOSION, at.x, at.y + 0.3, at.z, Math.max(2, (int)radius), radius / 3, 0.1, radius / 3, 0.0);
		this.sound(level, at, SoundEvents.GENERIC_EXPLODE.value(), 1.2F, 0.9F);
	}

	/** Shows a title and subtitle to everyone fighting this boss. */
	public void title(final ServerLevel level, final Component title, final Component subtitle) {
		Vec3 center = this.arenaCenter();
		AABB box = new AABB(center, center).inflate(Arenas.RADIUS + 8, 40, Arenas.RADIUS + 8);
		for (ServerPlayer p : level.getEntitiesOfClass(ServerPlayer.class, box, e -> true)) {
			p.connection.send(new ClientboundSetTitlesAnimationPacket(10, 50, 15));
			p.connection.send(new ClientboundSetSubtitleTextPacket(subtitle));
			p.connection.send(new ClientboundSetTitleTextPacket(title));
		}
	}
}
