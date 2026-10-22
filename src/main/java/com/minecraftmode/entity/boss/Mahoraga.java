package com.minecraftmode.entity.boss;

import com.minecraftmode.entity.CreatureAnim;
import com.minecraftmode.entity.CreatureMob;
import com.minecraftmode.entity.MobPower;
import com.minecraftmode.entity.combat.Attacks;
import com.minecraftmode.entity.combat.Telegraph;
import com.minecraftmode.job.JobClass;
import com.minecraftmode.job.JobProgression;
import com.minecraftmode.job.skill.Actions;
import com.minecraftmode.job.skill.CombatHooks;
import com.minecraftmode.job.skill.Fx;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Eight-Handled Sword Divergent Sila Divine General Mahoraga, the shikigami Sukuna calls: a white giant with the Sword of
 * Extermination grown from its right wrist and the wheel of adaptation over its head. While it stands, Sukuna takes no damage.
 *
 * <p>Adaptation: hits are sorted by the phenomenon behind them ({@link #phenomenon}: strikes, shots, fire, lightning,
 * explosions, and each class's technique on its own). Every {@link #ADAPT_TICKS} the phenomenon that hurt it most since the last
 * turn is adapted to: the wheel turns one notch, that phenomenon does less from then on ({@link #RESIST}, never immune) and the
 * wounds close ({@link #HEAL_PER_TURN}). After {@link #FULL_TURN} notches it has adapted to its enemies' very existence and the
 * fight is lost. So it must fall fast, to many kinds of damage at once. The turns are its synced {@link #phase()}.
 */
public class Mahoraga extends CreatureMob {
	/** Mahoraga's whole health as a share of Sukuna's. */
	public static final float HEALTH_SHARE = 0.22F;
	/** Its damage as a share of Sukuna's. */
	public static final float DAMAGE_SHARE = 0.7F;
	public static final int ADAPT_TICKS = 120;
	public static final int FULL_TURN = 8;
	/** What an adapted phenomenon still does is multiplied by 1 - this, by the number of times it was adapted to. */
	static final float[] RESIST = {0.0F, 0.35F, 0.55F, 0.7F, 0.8F};
	static final float HEAL_PER_TURN = 0.06F;
	private static final int SWEEP_COOLDOWN = 160;
	private static final int WHITE = 0xF4F2EE;

	private final ServerBossEvent bar = new ServerBossEvent(java.util.UUID.randomUUID(), Component.translatable("entity.minecraft_mode.mahoraga"),
		BossEvent.BossBarColor.WHITE, BossEvent.BossBarOverlay.NOTCHED_10);
	private final Map<String, Integer> adapted = new HashMap<>();
	private final Map<String, Float> taken = new HashMap<>();
	private @Nullable Sukuna master;
	private int adaptTimer;
	private boolean analysing;
	private int sweepCooldown = SWEEP_COOLDOWN / 2;
	/** Client: eased turn of the wheel. */
	private float wheel;
	private float wheelO;

	public Mahoraga(final EntityType<? extends Mahoraga> type, final Level level) {
		super(type, level);
		this.setPersistenceRequired();
		this.bar.setDarkenScreen(false);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
			.add(Attributes.MAX_HEALTH, RaidBoss.HEALTH_BAR)
			.add(Attributes.ATTACK_DAMAGE, 40.0)
			.add(Attributes.ARMOR, 20.0)
			.add(Attributes.MOVEMENT_SPEED, 0.3)
			.add(Attributes.FOLLOW_RANGE, 64.0)
			.add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
			.add(Attributes.STEP_HEIGHT, 1.5);
	}

	/** Takes its share of {@code master}'s health (all of it, divisor included) and damage. */
	public void serve(final Sukuna master) {
		this.master = master;
		float whole = master.divisor() * master.getMaxHealth() * HEALTH_SHARE;
		MobPower.setToughness(this.getUUID(), whole / this.getMaxHealth());
		this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(master.getAttributeValue(Attributes.ATTACK_DAMAGE) * DAMAGE_SHARE);
		this.setHealth(this.getMaxHealth());
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, true));
		this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
		this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, false));
	}

	@Override
	public boolean doHurtTarget(final ServerLevel level, final Entity target) {
		this.playAnim(CreatureAnim.ATTACK);
		return super.doHurtTarget(level, target);
	}

	/** The wheel's turns so far. */
	public int turns() {
		return this.phase();
	}

	/** Times {@code phenomenon} has been adapted to. */
	public int adaptations(final String phenomenon) {
		return this.adapted.getOrDefault(phenomenon, 0);
	}

	@Override
	protected void customServerAiStep(final ServerLevel level) {
		super.customServerAiStep(level);
		this.bar.setProgress(this.getHealth() / this.getMaxHealth());
		if (this.master == null || !this.master.isAlive()) {
			// a shikigami without its master returns to the shadows
			this.discard();
			return;
		}
		if (this.analysing && ++this.adaptTimer >= ADAPT_TICKS) {
			this.adaptTimer = 0;
			String most = null;
			for (Map.Entry<String, Float> e : this.taken.entrySet()) {
				if (most == null || e.getValue() > this.taken.get(most)) {
					most = e.getKey();
				}
			}
			this.taken.clear();
			if (most != null) {
				this.adapt(level, most);
			}
		}
		if (--this.sweepCooldown <= 0 && this.getTarget() != null && this.distanceTo(this.getTarget()) < 9.0) {
			this.sweepCooldown = SWEEP_COOLDOWN;
			this.sweep(level, this.getTarget());
		}
	}

	/** The Sword of Extermination swept in a wide arc in front of it. */
	private void sweep(final ServerLevel level, final LivingEntity target) {
		this.playAnim(CreatureAnim.ATTACK);
		Vec3 origin = this.position();
		Vec3 to = target.position().subtract(origin);
		float yaw = (float)Math.toDegrees(Math.atan2(-to.x, to.z));
		level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 2.5F, 0.5F);
		Telegraph.cone(level, origin, yaw, 7.5, 70, 24, Telegraph.RED, () -> {
			if (!this.isAlive()) {
				return;
			}
			Attacks.hitAll(this, Attacks.inCone(level, origin, yaw, 7.5, 70), Attacks.damage(this, 1.6F), null);
			new Fx(Fx.Kind.SLASH, WHITE).arc(level, this.position().add(0, 2.0, 0), yaw, 5.0, 140);
		});
	}

	/** One notch: the wheel turns, {@code phenomenon} does less from now on and the wounds close. */
	private void adapt(final ServerLevel level, final String phenomenon) {
		int times = Math.min(RESIST.length - 1, this.adaptations(phenomenon) + 1);
		this.adapted.put(phenomenon, times);
		this.setPhase(this.phase() + 1);
		this.heal(this.getMaxHealth() * HEAL_PER_TURN);
		this.playAnim(CreatureAnim.ROAR);
		level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.BELL_BLOCK, SoundSource.HOSTILE, 3.0F, 0.5F);
		level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ANVIL_PLACE, SoundSource.HOSTILE, 1.5F, 0.6F);
		Vec3 wheelAt = this.position().add(0, this.getBbHeight() + 0.6, 0);
		new Fx(Fx.Kind.RING, 0xF0D890).circle(level, Fx.Kind.RING, wheelAt, 1.6);
		level.sendParticles(ParticleTypes.END_ROD, wheelAt.x, wheelAt.y, wheelAt.z, 16, 0.8, 0.1, 0.8, 0.02);
		Component name = phenomenonName(phenomenon);
		int resist = Math.round(RESIST[times] * 100);
		for (ServerPlayer p : this.master.fighters(level)) {
			p.sendSystemMessage(Component.translatable("raid.minecraft_mode.mahoraga.adapted", name, resist, this.phase(), FULL_TURN).withStyle(ChatFormatting.GRAY));
		}
		if (this.phase() >= FULL_TURN) {
			this.master.mahoragaAdapted(level);
			this.discard();
		}
	}

	@Override
	public boolean hurtServer(final ServerLevel level, final DamageSource source, final float damage) {
		// players and their skill summons only, like a raid boss
		Entity attacker = source.getEntity();
		if (attacker != null && !(attacker instanceof Player) && Actions.summoner(attacker) == null) {
			return false;
		}
		String phenomenon = phenomenon(level, source);
		float resist = RESIST[this.adaptations(phenomenon)];
		this.analysing = true;
		this.taken.merge(phenomenon, damage, Float::sum);
		if (resist > 0.0F && this.random.nextInt(3) == 0) {
			level.sendParticles(ParticleTypes.ENCHANTED_HIT, this.getX(), this.getY(0.6), this.getZ(), 6, 0.5, 0.6, 0.5, 0.2);
		}
		return super.hurtServer(level, source, damage * (1.0F - resist));
	}

	/**
	 * What caused a hit, as Mahoraga tells phenomena apart: fire, lightning and explosions by their damage type, a class skill as
	 * that class's technique ({@code technique:<class>}), shots, and everything else as a strike.
	 */
	public static String phenomenon(final ServerLevel level, final DamageSource source) {
		if (source.is(DamageTypeTags.IS_FIRE)) {
			return "fire";
		}
		if (source.is(DamageTypeTags.IS_LIGHTNING)) {
			return "lightning";
		}
		if (source.is(DamageTypeTags.IS_EXPLOSION)) {
			return "explosion";
		}
		CombatHooks.DamageKind kind = CombatHooks.dealing();
		Entity attacker = source.getEntity();
		UUID summoner = Actions.summoner(attacker);
		Player owner = attacker instanceof Player p ? p : summoner != null ? level.getPlayerByUUID(summoner) : null;
		if (kind == CombatHooks.DamageKind.SKILL && owner != null) {
			return "technique:" + JobProgression.get(owner).job().id();
		}
		if (kind == CombatHooks.DamageKind.SHOT || source.is(DamageTypeTags.IS_PROJECTILE)) {
			return "shot";
		}
		return "strike";
	}

	public static Component phenomenonName(final String phenomenon) {
		if (phenomenon.startsWith("technique:")) {
			JobClass job = JobClass.byId(phenomenon.substring("technique:".length()));
			return Component.translatable("raid.minecraft_mode.mahoraga.phenomenon.technique", Component.translatable(job.nameKey()));
		}
		return Component.translatable("raid.minecraft_mode.mahoraga.phenomenon." + phenomenon);
	}

	@Override
	public void tick() {
		super.tick();
		if (this.level().isClientSide()) {
			this.wheelO = this.wheel;
			this.wheel += (this.phase() * Mth.PI / 4 - this.wheel) * 0.12F;
		}
	}

	@Override
	public float wheel(final float partialTicks) {
		return Mth.lerp(partialTicks, this.wheelO, this.wheel);
	}

	@Override
	public void die(final DamageSource source) {
		super.die(source);
		if (this.level() instanceof ServerLevel level && this.master != null) {
			this.master.mahoragaDown(level);
		}
	}

	@Override
	public void remove(final RemovalReason reason) {
		this.bar.removeAllPlayers();
		MobPower.clear(this.getUUID());
		super.remove(reason);
	}

	@Override
	public void startSeenByPlayer(final ServerPlayer player) {
		super.startSeenByPlayer(player);
		this.bar.addPlayer(player);
	}

	@Override
	public void stopSeenByPlayer(final ServerPlayer player) {
		super.stopSeenByPlayer(player);
		this.bar.removePlayer(player);
	}

	@Override
	public boolean removeWhenFarAway(final double distSqr) {
		return false;
	}

	@Override
	public boolean isPushable() {
		return false;
	}

	@Override
	public boolean causeFallDamage(final double fallDistance, final float damageModifier, final DamageSource damageSource) {
		return false;
	}
}
