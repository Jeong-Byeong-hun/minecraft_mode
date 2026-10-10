package com.minecraftmode.entity.named;

import com.minecraftmode.bag.Bags;
import com.minecraftmode.consumable.Consumables;
import com.minecraftmode.entity.CreatureAnim;
import com.minecraftmode.entity.CreatureMob;
import com.minecraftmode.entity.combat.Attacks;
import com.minecraftmode.entity.combat.MobProjectile;
import com.minecraftmode.entity.combat.Telegraph;
import com.minecraftmode.job.gear.ItemLevels;
import com.minecraftmode.job.skill.Fx;
import com.minecraftmode.job.skill.SkillScheduler;
import com.minecraftmode.job.weapon.JobWeaponItem;
import com.minecraftmode.loot.Coins;
import com.minecraftmode.loot.GearDrops;
import com.minecraftmode.loot.GearShop;
import com.minecraftmode.progress.Contribution;
import com.minecraftmode.raid.RaidDamage;
import com.minecraftmode.registry.ModItems;
import com.minecraftmode.worldgen.lair.LairChestBlockEntity;
import com.minecraftmode.worldgen.lair.LairDef;
import com.minecraftmode.worldgen.lair.NamedLairs;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.Prediction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * A named monster ({@link NamedDef}): picks its level inside the definition's range when it spawns,
 * shows "[Lv.N] name", fights with melee plus its patterns, and on death drops Evolution Ether and,
 * by chance, a piece of class gear of its level range.
 */
public class NamedMob extends CreatureMob {
	private static final int GLOBAL_COOLDOWN = 30;
	/** Only one named monster within this many blocks. */
	public static final double SPAWN_SPACING = 96.0;
	/** Spacing between named monsters inside their own lair. */
	private static final double LAIR_SPACING = 20.0;

	private int namedLevel;
	private int @Nullable [] cooldowns;
	private int globalCooldown = 40;
	/** Ticks the monster stands still (winding up a telegraphed pattern). */
	private int busyTicks;
	private boolean stealthStrike;
	private float stealthPower;
	private int healTicks;
	private float healPerTick;
	private final List<Mob> minions = new ArrayList<>();
	/** A lair lord: the cycle's guardian of a lair treasure (tougher, boss bar, the lair's wrath). */
	private boolean lord;
	private @Nullable BlockPos lordHome;
	private @Nullable ServerBossEvent lordBar;
	/** A champion: a dungeon boss or a world boss (tougher, boss bar, the wrath on a timer, leashed to its home). Never saved. */
	private boolean champion;
	private double championLeash;
	public static final float LORD_HEALTH = 3.0F;
	public static final float LORD_DAMAGE = 1.3F;
	public static final float LORD_SCALE = 1.2F;
	/** A lord this far from its chest drops the chase and returns (it cannot be lured out of its lair). */
	public static final double LORD_LEASH = 24.0;
	/** Ticks between two casts of the lair's wrath. */
	public static final int WRATH_INTERVAL = 400;

	public NamedMob(final EntityType<? extends NamedMob> type, final Level level) {
		super(type, level);
		NamedDef def = this.def();
		this.xpReward = 10 + def.lo() / 2;
		if (def.flying()) {
			this.moveControl = new FlyingMoveControl<>(this, 10, true);
			this.setNoGravity(true);
		}
	}

	public NamedDef def() {
		NamedDef def = NamedMobs.def(this.getType());
		if (def == null) {
			throw new IllegalStateException("No named definition for " + this.getType());
		}
		return def;
	}

	public static AttributeSupplier.Builder createAttributes(final NamedDef def) {
		AttributeSupplier.Builder builder = Monster.createMonsterAttributes()
			.add(Attributes.MAX_HEALTH, def.health())
			.add(Attributes.ATTACK_DAMAGE, def.damage())
			.add(Attributes.ARMOR, def.armor())
			.add(Attributes.MOVEMENT_SPEED, def.speed())
			.add(Attributes.FOLLOW_RANGE, 32.0)
			.add(Attributes.KNOCKBACK_RESISTANCE, 0.4)
			.add(Attributes.STEP_HEIGHT, 1.0);
		if (def.flying()) {
			builder.add(Attributes.FLYING_SPEED, def.speed() * 1.4);
		}
		return builder;
	}

	public static boolean checkSpawnRules(final EntityType<NamedMob> type, final ServerLevelAccessor level, final EntitySpawnReason reason, final BlockPos pos,
		final RandomSource random) {
		NamedDef def = NamedMobs.def(type);
		if (def == null) {
			return false;
		}
		if ((reason == EntitySpawnReason.NATURAL || reason == EntitySpawnReason.CHUNK_GENERATION) && !fits(def, level, pos)) {
			return false;
		}
		return def.daylight()
			? Monster.checkAnyLightMonsterSpawnRules(type, level, reason, pos, random)
			: Monster.checkMonsterSpawnRules(type, level, reason, pos, random);
	}

	/** In its own lair a named monster ignores its usual height band and keeps much less distance. */
	private static boolean fits(final NamedDef def, final ServerLevelAccessor level, final BlockPos pos) {
		LairDef lair = NamedLairs.at(level.getLevel(), pos);
		boolean home = lair != null && lair.id().equals(def.id());
		if (!home && !def.habitat().allows(pos.getY())) {
			return false;
		}
		return level.getEntitiesOfClass(NamedMob.class, new AABB(pos).inflate(home ? LAIR_SPACING : SPAWN_SPACING)).isEmpty();
	}

	@Override
	protected PathNavigation createNavigation(final Level level) {
		NamedDef def = NamedMobs.def(this.getType());
		if (def != null && def.flying()) {
			FlyingPathNavigation navigation = new FlyingPathNavigation(this, level);
			navigation.setCanOpenDoors(false);
			return navigation;
		}
		return super.createNavigation(level);
	}

	@Override
	protected void registerGoals() {
		NamedDef def = NamedMobs.def(this.getType());
		boolean flying = def != null && def.flying();
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(1, new AbilityGoal());
		this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.1, false));
		this.goalSelector.addGoal(6, flying ? new WaterAvoidingRandomFlyingGoal(this, 0.8) : new WaterAvoidingRandomStrollGoal(this, 0.8));
		this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 12.0F));
		this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
		this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
		this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
	}

	@Override
	public boolean isFlyingCreature() {
		return this.def().flying();
	}

	// ------------------------------------------------------------------ level and name

	@Override
	public @Nullable SpawnGroupData finalizeSpawn(final ServerLevelAccessor level, final DifficultyInstance difficulty, final EntitySpawnReason spawnReason,
		final @Nullable SpawnGroupData groupData) {
		SpawnGroupData data = super.finalizeSpawn(level, difficulty, spawnReason, groupData);
		this.rollLevel(level.getRandom());
		return data;
	}

	private void rollLevel(final RandomSource random) {
		NamedDef def = this.def();
		this.applyLevel(def.lo() + random.nextInt(def.hi() - def.lo() + 1));
		this.setHealth(this.getMaxHealth());
	}

	/** Sets the level: health and damage grow up to +30% across the range; updates the name. */
	public void applyLevel(final int level) {
		NamedDef def = this.def();
		this.namedLevel = level;
		float scale = 1.0F + 0.3F * (level - def.lo()) / Math.max(1, def.hi() - def.lo());
		this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(def.health() * scale);
		this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(def.damage() * scale);
		this.setCustomName(Component.literal("[Lv." + level + "] ").withStyle(ChatFormatting.GRAY)
			.append(Component.translatable(def.nameKey()).withColor(JobWeaponItem.tierColor(ItemLevels.tier(level)))));
		this.setCustomNameVisible(true);
	}

	public int namedLevel() {
		return this.namedLevel;
	}

	public boolean isLord() {
		return this.lord;
	}

	/** Turns this monster into the lord guarding the lair treasure at {@code home}: top level, tougher, boss bar, stays near home. */
	public void makeLord(final BlockPos home) {
		this.lord = true;
		this.lordHome = home;
		this.applyLevel(this.def().hi());
		this.applyLord();
		this.setHealth(this.getMaxHealth());
	}

	/**
	 * Turns this monster into a champion at {@code home}: top level, health times {@code health}, size times {@code scale}, named by
	 * {@code titleKey} (with the monster's name), a boss bar, the wrath every {@link #WRATH_INTERVAL} ticks, and back home when
	 * lured farther than {@code leash}.
	 */
	public void makeChampion(final float health, final float scale, final String titleKey, final BlockPos home, final double leash) {
		this.champion = true;
		this.championLeash = leash;
		this.lordHome = home;
		this.applyLevel(this.def().hi());
		this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(this.getAttribute(Attributes.MAX_HEALTH).getBaseValue() * health);
		this.getAttribute(Attributes.SCALE).setBaseValue(scale);
		this.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1.0);
		Component name = Component.translatable(titleKey, Component.literal("[Lv." + this.namedLevel + "] ").withStyle(ChatFormatting.GRAY)
			.append(Component.translatable(this.def().nameKey()).withStyle(ChatFormatting.GOLD))).withStyle(ChatFormatting.RED);
		this.setCustomName(name);
		this.lordBar = new ServerBossEvent(Mth.createInsecureUUID(this.random), name, BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.NOTCHED_10);
		this.setHomeTo(home, (int)Math.max(8, leash * 0.6));
		this.setPersistenceRequired();
		this.setHealth(this.getMaxHealth());
	}

	public boolean isChampion() {
		return this.champion;
	}

	private void applyLord() {
		NamedDef def = this.def();
		this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(this.getAttribute(Attributes.MAX_HEALTH).getBaseValue() * LORD_HEALTH);
		this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(this.getAttribute(Attributes.ATTACK_DAMAGE).getBaseValue() * LORD_DAMAGE);
		this.getAttribute(Attributes.SCALE).setBaseValue(LORD_SCALE);
		Component name = Component.translatable("entity.minecraft_mode.lair_lord", Component.literal("[Lv." + this.namedLevel + "] ").withStyle(ChatFormatting.GRAY)
			.append(Component.translatable(def.nameKey()).withStyle(ChatFormatting.GOLD))).withStyle(ChatFormatting.RED);
		this.setCustomName(name);
		if (this.lordBar == null) {
			this.lordBar = new ServerBossEvent(Mth.createInsecureUUID(this.random), name, BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.NOTCHED_6);
		} else {
			this.lordBar.setName(name);
		}
		if (this.lordHome != null) {
			this.setHomeTo(this.lordHome, 14);
		}
	}

	@Override
	protected void addAdditionalSaveData(final ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putInt("NamedLevel", this.namedLevel);
		output.putBoolean("Lord", this.lord);
		if (this.lordHome != null) {
			output.putLong("LordHome", this.lordHome.asLong());
		}
	}

	@Override
	protected void readAdditionalSaveData(final ValueInput input) {
		super.readAdditionalSaveData(input);
		int level = input.getIntOr("NamedLevel", 0);
		if (level > 0) {
			this.applyLevel(level);
		}
		this.lord = input.getBooleanOr("Lord", false);
		this.lordHome = input.getLong("LordHome").map(BlockPos::of).orElse(null);
		if (this.lord && level > 0) {
			float health = this.getHealth();
			this.applyLord();
			this.setHealth(health);
		}
	}

	// ------------------------------------------------------------------ ticking

	@Override
	protected void customServerAiStep(final ServerLevel level) {
		super.customServerAiStep(level);
		if (this.namedLevel == 0) {
			this.rollLevel(this.getRandom());
		}
		if (this.busyTicks > 0) {
			this.busyTicks--;
			this.getNavigation().stop();
		}
		if (this.healTicks > 0) {
			this.healTicks--;
			this.heal(this.healPerTick);
			if (this.healTicks % 10 == 0) {
				level.sendParticles(ParticleTypes.HAPPY_VILLAGER, this.getX(), this.getY(0.6), this.getZ(), 6, 0.5, 0.6, 0.5, 0.0);
			}
		}
		if (this.tickCount % 12 == 0) {
			int color = JobWeaponItem.tierColor(ItemLevels.tier(Math.max(10, this.namedLevel)));
			new Fx(Fx.Kind.SPARK, color).burst(level, this.position().add(0, this.getBbHeight() * 0.6, 0), 2, this.getBbWidth() * 0.5, 0.01);
		}
		this.minions.removeIf(m -> !m.isAlive());
		if (this.lord) {
			this.tickLord(level);
		} else if (this.champion) {
			this.tickChampion(level);
		}
	}

	/** Boss bar, and every {@link #WRATH_INTERVAL} ticks the lair's wrath: a telegraphed ring that takes 40% of a victim's health. */
	private void tickLord(final ServerLevel level) {
		if (this.lordBar != null) {
			this.lordBar.setProgress(this.getHealth() / this.getMaxHealth());
		}
		// a lord its chest no longer knows (replaced while its chunk was unloaded, or the chest is gone) leaves
		if (this.tickCount % 100 == 0 && this.lordHome != null && level.isLoaded(this.lordHome)
			&& !(level.getBlockEntity(this.lordHome) instanceof LairChestBlockEntity chest && chest.isLord(this))) {
			this.discard();
			return;
		}
		if (this.tickCount % 20 == 0 && this.lordHome != null && this.distanceToSqr(Vec3.atBottomCenterOf(this.lordHome)) > LORD_LEASH * LORD_LEASH) {
			this.setTarget(null);
			this.getNavigation().stop();
			this.teleportTo(this.lordHome.getX() + 0.5, this.lordHome.getY(), this.lordHome.getZ() + 1.5);
		}
		if (this.tickCount % WRATH_INTERVAL != WRATH_INTERVAL / 2 || this.getTarget() == null) {
			return;
		}
		this.wrath(level);
	}

	private void tickChampion(final ServerLevel level) {
		if (this.lordBar != null) {
			this.lordBar.setProgress(this.getHealth() / this.getMaxHealth());
		}
		if (this.tickCount % 20 == 0 && this.lordHome != null && this.distanceToSqr(Vec3.atBottomCenterOf(this.lordHome)) > this.championLeash * this.championLeash) {
			this.setTarget(null);
			this.getNavigation().stop();
			this.teleportTo(this.lordHome.getX() + 0.5, this.lordHome.getY(), this.lordHome.getZ() + 0.5);
		}
		if (this.tickCount % WRATH_INTERVAL == WRATH_INTERVAL / 2 && this.getTarget() != null) {
			this.wrath(level);
		}
	}

	/** Casts the lair's wrath now (also used by tests). */
	public void wrath(final ServerLevel level) {
		Vec3 at = this.position();
		this.busyTicks = Math.max(this.busyTicks, 35);
		this.playAnim(CreatureAnim.ROAR);
		level.playSound(null, at.x, at.y, at.z, SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 1.4F, 0.7F);
		Telegraph.circle(level, at, 5.0, 30, Telegraph.RED, () -> {
			if (!this.isAlive()) {
				return;
			}
			for (LivingEntity e : Attacks.inCircle(level, at, 5.0, 4.0)) {
				if (e instanceof Player player && !player.isCreative() && !player.isSpectator()) {
					RaidDamage.portion(level, player, this, 0.4F);
				}
			}
			level.sendParticles(ParticleTypes.EXPLOSION, at.x, at.y + 0.5, at.z, 4, 2.0, 0.3, 2.0, 0.0);
		});
	}

	@Override
	public void startSeenByPlayer(final ServerPlayer player) {
		super.startSeenByPlayer(player);
		if (this.lordBar != null) {
			this.lordBar.addPlayer(player);
		}
	}

	@Override
	public void stopSeenByPlayer(final ServerPlayer player) {
		super.stopSeenByPlayer(player);
		if (this.lordBar != null) {
			this.lordBar.removePlayer(player);
		}
	}

	@Override
	public boolean doHurtTarget(final ServerLevel level, final Entity target) {
		this.playAnim(CreatureAnim.ATTACK);
		boolean hurt = super.doHurtTarget(level, target);
		if (hurt && target instanceof LivingEntity living) {
			NamedDef def = this.def();
			if (def.onHit() != null) {
				living.addEffect(new MobEffectInstance(def.onHit(), def.onHitTicks(), 0), this);
			}
			if (def.fireImmune()) {
				living.igniteForSeconds(4.0F);
			}
			if (this.stealthStrike) {
				this.stealthStrike = false;
				this.removeEffect(MobEffects.INVISIBILITY);
				Attacks.hit(this, living, Attacks.damage(this, this.stealthPower - 1.0F));
				level.sendParticles(ParticleTypes.CRIT, living.getX(), living.getY(0.6), living.getZ(), 20, 0.4, 0.4, 0.4, 0.3);
			}
		}
		return hurt;
	}

	@Override
	public boolean hurtServer(final ServerLevel level, final DamageSource source, final float damage) {
		if (source.getEntity() instanceof Player) {
			this.setPersistenceRequired();
		}
		return super.hurtServer(level, source, damage);
	}

	@Override
	public void die(final DamageSource source) {
		super.die(source);
		// the chest may sit in a chunk that just unloaded (the lord roams up to its leash): loading it once is worth the credit
		if (this.lord && this.lordHome != null && this.level() instanceof ServerLevel level
			&& level.getBlockEntity(this.lordHome) instanceof LairChestBlockEntity chest) {
			chest.lordDefeated(level, this, source);
		}
	}

	@Override
	protected void dropCustomDeathLoot(final ServerLevel level, final DamageSource source, final boolean killedByPlayer) {
		super.dropCustomDeathLoot(level, source, killedByPlayer);
		// shared by everyone who did their part (Contribution): coins by share, ether for big helpers, gear for the top contributor
		List<Contribution.Share> shares = Contribution.shares(this, source);
		if (shares.isEmpty()) {
			return;
		}
		ServerPlayer mvp = shares.getFirst().player();
		NamedDef def = this.def();
		this.dropGlowing(level, GearDrops.ether(def.lo(), def.hi(), this.getRandom()));
		if (this.lord) {
			this.dropGlowing(level, new ItemStack(ModItems.ENHANCEMENT_STONE, 1 + this.getRandom().nextInt(2)));
			if (this.getRandom().nextFloat() < 0.1F) {
				this.dropGlowing(level, new ItemStack(ModItems.PROTECTION_SCROLL));
			}
		} else if (this.getRandom().nextFloat() < 0.1F) {
			this.dropGlowing(level, new ItemStack(ModItems.ENHANCEMENT_STONE));
		}
		// coins grow with the level: about 4 copper at Lv 20, about a gold coin at Lv 90
		int copper = Math.max(1, Math.round(GearShop.bracketPrice(Math.max(10, this.namedLevel)) * 0.15F * (0.6F + this.getRandom().nextFloat() * 0.8F)));
		if (shares.size() == 1) {
			for (ItemStack coins : Coins.asItems(copper)) {
				this.spawnAtLocation(level, coins);
			}
		} else {
			for (Contribution.Share share : shares) {
				int part = Contribution.portion(copper, share, shares.size());
				Coins.give(share.player(), part);
				share.player().sendSystemMessage(Component.translatable("message.minecraft_mode.contribution.coins", Component.translatable(def.nameKey()),
					Math.round(share.fraction() * 100.0F), Coins.component(part)).withStyle(ChatFormatting.YELLOW));
				if (share.player() != mvp && share.fraction() >= Contribution.ASSIST_SHARE) {
					share.player().getInventory().placeItemBackInInventory(GearDrops.ether(def.lo(), def.hi(), this.getRandom()), Prediction.SERVER_ONLY);
					share.player().sendSystemMessage(Component.translatable("message.minecraft_mode.contribution.assist").withStyle(ChatFormatting.AQUA));
				}
			}
		}
		ItemStack supply = Consumables.namedDrop(Math.max(def.lo(), this.namedLevel), this.getRandom());
		if (!supply.isEmpty()) {
			this.dropGlowing(level, supply);
		}
		ItemStack gear = GearDrops.namedDrop(mvp, def.lo(), def.hi(), this.getRandom());
		if (!gear.isEmpty()) {
			Component name = gear.getHoverName();
			if (shares.size() == 1) {
				this.dropGlowing(level, gear);
			} else {
				// the top contributor's piece goes straight to them, so nobody else picks it up
				Bags.give(mvp, gear);
			}
			level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.HOSTILE, 0.6F, 1.4F);
			mvp.sendSystemMessage(Component.translatable("message.minecraft_mode.named.drop", Component.translatable(def.nameKey()), name).withStyle(ChatFormatting.GOLD));
		}
	}

	private void dropGlowing(final ServerLevel level, final ItemStack stack) {
		ItemEntity item = this.spawnAtLocation(level, stack);
		if (item != null) {
			item.setGlowingTag(true);
			item.setUnlimitedLifetime();
		}
	}

	@Override
	public void remove(final RemovalReason reason) {
		for (Mob minion : this.minions) {
			if (minion.isAlive()) {
				minion.discard();
			}
		}
		if (this.lordBar != null) {
			this.lordBar.removeAllPlayers();
		}
		super.remove(reason);
	}

	@Override
	protected @Nullable SoundEvent getAmbientSound() {
		return this.def().flying() ? SoundEvents.VEX_AMBIENT : SoundEvents.ZOMBIE_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(final DamageSource source) {
		return SoundEvents.RAVAGER_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.RAVAGER_DEATH;
	}

	@Override
	public float getVoicePitch() {
		return 0.75F + this.random.nextFloat() * 0.2F;
	}

	// ------------------------------------------------------------------ patterns

	/** Picks a ready pattern every so often while there is a target; never blocks walking or melee. */
	private final class AbilityGoal extends Goal {
		AbilityGoal() {
			this.setFlags(EnumSet.noneOf(Flag.class));
		}

		@Override
		public boolean canUse() {
			LivingEntity target = NamedMob.this.getTarget();
			return target != null && target.isAlive();
		}

		@Override
		public boolean requiresUpdateEveryTick() {
			return true;
		}

		@Override
		public void tick() {
			NamedMob mob = NamedMob.this;
			List<Ability> abilities = mob.def().abilities();
			if (mob.cooldowns == null) {
				mob.cooldowns = new int[abilities.size()];
				for (int i = 0; i < abilities.size(); i++) {
					mob.cooldowns[i] = 40 + mob.random.nextInt(Math.max(1, abilities.get(i).cooldown()));
				}
			}
			for (int i = 0; i < mob.cooldowns.length; i++) {
				mob.cooldowns[i]--;
			}
			if (--mob.globalCooldown > 0 || mob.busyTicks > 0) {
				return;
			}
			LivingEntity target = mob.getTarget();
			if (target == null || !(mob.level() instanceof ServerLevel level)) {
				return;
			}
			int start = mob.random.nextInt(abilities.size());
			for (int k = 0; k < abilities.size(); k++) {
				int i = (start + k) % abilities.size();
				Ability ability = abilities.get(i);
				if (mob.cooldowns[i] > 0 || mob.distanceTo(target) > ability.range() || !mob.ready(ability)) {
					continue;
				}
				mob.use(level, ability, target);
				mob.cooldowns[i] = Math.round(ability.cooldown() * (0.8F + 0.4F * mob.random.nextFloat()));
				mob.globalCooldown = GLOBAL_COOLDOWN;
				return;
			}
		}
	}

	private boolean ready(final Ability ability) {
		return switch (ability.type()) {
			case HEAL -> this.getHealth() < this.getMaxHealth() * 0.5F && this.healTicks == 0;
			case STEALTH -> !this.hasEffect(MobEffects.INVISIBILITY);
			case LEAP -> this.onGround() || this.def().flying();
			case SUMMON -> this.minions.size() < ability.count() * 2;
			default -> true;
		};
	}

	private float damage(final Ability ability) {
		return Attacks.damage(this, ability.power());
	}

	private void use(final ServerLevel level, final Ability ability, final LivingEntity target) {
		switch (ability.type()) {
			case LEAP -> this.leap(level, ability, target);
			case CHARGE -> this.charge(level, ability, target);
			case BOLT -> this.bolt(level, ability, target);
			case SUMMON -> this.summon(level, ability, target);
			case TELEPORT -> this.teleportBehind(level, target);
			case AURA -> this.aura(level, ability);
			case ROAR -> this.roar(level, ability);
			case SPIKES -> this.spikes(level, ability, target);
			case HEAL -> this.startHeal(level, ability);
			case BREATH -> this.breath(level, ability, target);
			case STEALTH -> this.stealth(level, ability);
			case SLAM -> this.slam(level, ability);
		}
	}

	private void leap(final ServerLevel level, final Ability ability, final LivingEntity target) {
		Vec3 to = target.position();
		Vec3 d = to.subtract(this.position());
		this.playAnim(CreatureAnim.LEAP);
		Attacks.push(this, new Vec3(d.x * 0.14, 0.75 + Math.min(0.45, d.horizontalDistance() * 0.025), d.z * 0.14));
		level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.RAVAGER_ATTACK, SoundSource.HOSTILE, 0.8F, 1.3F);
		Telegraph.circle(level, to, ability.radius(), 16, Telegraph.ORANGE, () -> {
			if (!this.isAlive()) {
				return;
			}
			Attacks.hitAll(this, Attacks.inCircle(level, to, ability.radius(), 3.0), this.damage(ability), e -> Attacks.knock(e, to, 0.8, 0.5));
			level.sendParticles(ParticleTypes.EXPLOSION, to.x, to.y + 0.3, to.z, 3, ability.radius() / 3, 0.1, ability.radius() / 3, 0.0);
			level.playSound(null, to.x, to.y, to.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 0.7F, 1.2F);
		});
	}

	private void charge(final ServerLevel level, final Ability ability, final LivingEntity target) {
		Vec3 start = this.position();
		Vec3 dir = target.position().subtract(start).multiply(1, 0, 1).normalize();
		Vec3 end = start.add(dir.scale(ability.range()));
		this.busyTicks = 18;
		this.playAnim(CreatureAnim.CHARGE);
		this.getLookControl().setLookAt(target);
		level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 0.8F, 1.4F);
		Telegraph.line(level, start, end, ability.radius() * 2, 18, Telegraph.RED, () -> {
			Set<UUID> hit = new HashSet<>();
			for (int i = 0; i < 10; i++) {
				SkillScheduler.schedule(i, () -> {
					if (!this.isAlive()) {
						return;
					}
					Attacks.push(this, new Vec3(dir.x * 1.1, this.getDeltaMovement().y, dir.z * 1.1));
					level.sendParticles(ParticleTypes.CLOUD, this.getX(), this.getY() + 0.3, this.getZ(), 3, 0.3, 0.1, 0.3, 0.02);
					for (LivingEntity e : Attacks.inCircle(level, this.position(), ability.radius(), this.getBbHeight())) {
						if (hit.add(e.getUUID())) {
							Attacks.hit(this, e, this.damage(ability));
							Attacks.knock(e, this.position(), 1.4, 0.5);
						}
					}
				});
			}
		});
	}

	private void bolt(final ServerLevel level, final Ability ability, final LivingEntity target) {
		this.playAnim(ability.count() > 1 ? CreatureAnim.CAST : CreatureAnim.ATTACK);
		Vec3 eye = this.getEyePosition();
		Vec3 aim = target.position().add(0, target.getBbHeight() * 0.55, 0).subtract(eye);
		boolean lob = ability.gravity() > 0.0F;
		float speed = lob ? 0.9F : 1.3F;
		if (lob) {
			aim = aim.add(0, aim.horizontalDistance() * 0.35, 0);
		}
		ItemStack display = ability.display() == null ? ItemStack.EMPTY : new ItemStack(ability.display());
		for (int k = 0; k < ability.count(); k++) {
			float offset = ability.count() == 1 ? 0.0F : -ability.radius() / 2 + ability.radius() * k / (ability.count() - 1);
			Vec3 dir = aim.yRot((float)Math.toRadians(offset));
			MobProjectile projectile = MobProjectile.of(this, display, ability.fx(), ability.color(), ability.gravity(), this.damage(ability)).magic(ability.magic())
				.launch(eye, dir, speed);
			if (ability.effect() != null) {
				projectile.effect(ability.effect(), ability.effectTicks(), ability.amplifier());
			}
			if (ability.homing()) {
				projectile.homing(target);
			}
			if (lob && ability.radius() > 0.0F && ability.count() == 1) {
				projectile.blast(ability.radius());
			}
			level.addFreshEntity(projectile);
		}
		level.playSound(null, this.getX(), this.getY(), this.getZ(), lob ? SoundEvents.WITCH_THROW : SoundEvents.FIRECHARGE_USE, SoundSource.HOSTILE, 0.8F, 1.2F);
	}

	@SuppressWarnings("unchecked")
	private void summon(final ServerLevel level, final Ability ability, final LivingEntity target) {
		if (ability.summon() == null) {
			return;
		}
		this.playAnim(CreatureAnim.SUMMON);
		this.minions.addAll(Attacks.summon(level, (EntityType<? extends Mob>)ability.summon(), this.position(), ability.count(), target, 600));
		level.sendParticles(ParticleTypes.LARGE_SMOKE, this.getX(), this.getY() + 0.5, this.getZ(), 30, 1.5, 0.5, 1.5, 0.02);
		level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.EVOKER_PREPARE_SUMMON, SoundSource.HOSTILE, 0.8F, 0.9F);
	}

	private void teleportBehind(final ServerLevel level, final LivingEntity target) {
		Vec3 back = Vec3.directionFromRotation(0.0F, target.getYRot()).scale(-2.5);
		Vec3[] tries = {target.position().add(back), target.position().add(back.yRot(1.2F)), target.position().add(back.yRot(-1.2F))};
		for (Vec3 to : tries) {
			AABB box = this.getBoundingBox().move(to.subtract(this.position()));
			if (level.noCollision(this, box)) {
				level.sendParticles(ParticleTypes.PORTAL, this.getX(), this.getY(0.5), this.getZ(), 40, 0.4, 0.8, 0.4, 0.3);
				this.teleportTo(to.x, to.y, to.z);
				this.getLookControl().setLookAt(target);
				level.sendParticles(ParticleTypes.REVERSE_PORTAL, to.x, to.y + 1, to.z, 40, 0.4, 0.8, 0.4, 0.1);
				level.playSound(null, to.x, to.y, to.z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.0F, 0.9F);
				return;
			}
		}
	}

	private void aura(final ServerLevel level, final Ability ability) {
		this.playAnim(CreatureAnim.CAST);
		Telegraph.ring(level, this.position(), ability.radius(), ability.color(), 1.0F);
		new Fx(Fx.Kind.RING, ability.color()).circle(level, Fx.Kind.RING, this.position().add(0, 0.2, 0), ability.radius() * 0.6);
		for (LivingEntity e : Attacks.inCircle(level, this.position(), ability.radius(), 4.0)) {
			if (ability.effect() != null) {
				Attacks.effect(e, ability.effect(), ability.effectTicks(), ability.amplifier(), this);
			}
		}
		level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.EVOKER_CAST_SPELL, SoundSource.HOSTILE, 0.7F, 0.8F);
	}

	private void roar(final ServerLevel level, final Ability ability) {
		this.playAnim(CreatureAnim.ROAR);
		this.busyTicks = 10;
		level.playSound(null, this.getX(), this.getY(), this.getZ(), this.def().lo() >= 70 ? SoundEvents.WARDEN_ROAR : SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE,
			1.2F, 0.8F);
		Telegraph.ring(level, this.position(), ability.radius(), Telegraph.ORANGE, 1.2F);
		for (LivingEntity e : Attacks.inCircle(level, this.position(), ability.radius(), 4.0)) {
			if (ability.power() > 0.0F) {
				Attacks.hit(this, e, this.damage(ability));
			}
			Attacks.knock(e, this.position(), 1.0, 0.4);
			if (ability.effect() != null) {
				Attacks.effect(e, ability.effect(), ability.effectTicks(), ability.amplifier(), this);
			}
		}
		level.sendParticles(ParticleTypes.SONIC_BOOM, this.getX(), this.getY(0.6), this.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
	}

	private void spikes(final ServerLevel level, final Ability ability, final LivingEntity target) {
		this.playAnim(CreatureAnim.CAST);
		Vec3 at = target.position();
		Fx fx = new Fx(Fx.Kind.SHARD, ability.color());
		Telegraph.circle(level, at, ability.radius(), 24, Telegraph.RED, () -> {
			if (!this.isAlive()) {
				return;
			}
			Attacks.hitAll(this, Attacks.inCircle(level, at, ability.radius(), 3.0), this.damage(ability), ability.magic(), e -> {
				Attacks.push(e, new Vec3(0, 0.7, 0));
				if (ability.effect() != null) {
					Attacks.effect(e, ability.effect(), ability.effectTicks(), ability.amplifier(), this);
				}
			});
			for (int i = 0; i < 12; i++) {
				double a = Math.PI * 2 * i / 12;
				Vec3 p = at.add(Math.cos(a) * ability.radius() * 0.6, 0, Math.sin(a) * ability.radius() * 0.6);
				fx.column(level, p, 2.0);
			}
			fx.burst(level, at.add(0, 0.5, 0), 25, ability.radius() * 0.4, 0.2);
			level.playSound(null, at.x, at.y, at.z, SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.HOSTILE, 1.2F, 0.6F);
		});
	}

	private void startHeal(final ServerLevel level, final Ability ability) {
		this.playAnim(CreatureAnim.CAST);
		this.healTicks = 80;
		this.healPerTick = this.getMaxHealth() * ability.power() / 80.0F;
		level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ZOMBIE_VILLAGER_CURE, SoundSource.HOSTILE, 0.6F, 1.4F);
	}

	private void breath(final ServerLevel level, final Ability ability, final LivingEntity target) {
		Vec3 origin = this.position();
		Vec3 to = target.position().subtract(origin);
		float yaw = (float)Math.toDegrees(Math.atan2(-to.x, to.z));
		float half = ability.radius() / 2.0F;
		this.busyTicks = 34;
		this.getLookControl().setLookAt(target);
		this.playAnim(CreatureAnim.BREATH);
		Fx fx = new Fx(ability.fx(), ability.color());
		Telegraph.cone(level, origin, yaw, ability.range(), half, 14, Telegraph.RED, () -> {
			for (int i = 0; i < 20; i += 4) {
				SkillScheduler.schedule(i, () -> {
					if (!this.isAlive()) {
						return;
					}
					Vec3 eye = this.position().add(0, this.getBbHeight() * 0.6, 0);
					for (int k = 0; k < 6; k++) {
						float a = yaw - half + (2 * half) * this.random.nextFloat();
						fx.moving(level, ability.fx(), eye, Vec3.directionFromRotation(0.0F, a).scale(ability.range() / 12.0).add(0, -0.02, 0));
					}
					Attacks.hitAll(this, Attacks.inCone(level, origin, yaw, ability.range(), half), this.damage(ability), ability.magic(), e -> {
						if (ability.effect() != null) {
							Attacks.effect(e, ability.effect(), ability.effectTicks(), ability.amplifier(), this);
						}
					});
				});
			}
			level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ENDER_DRAGON_SHOOT, SoundSource.HOSTILE, 0.9F, 1.3F);
		});
	}

	private void stealth(final ServerLevel level, final Ability ability) {
		this.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, ability.count(), 0, false, false));
		this.stealthStrike = true;
		this.stealthPower = ability.power();
		level.sendParticles(ParticleTypes.LARGE_SMOKE, this.getX(), this.getY(0.5), this.getZ(), 25, 0.5, 0.6, 0.5, 0.02);
		level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ILLUSIONER_MIRROR_MOVE, SoundSource.HOSTILE, 0.8F, 0.8F);
	}

	private void slam(final ServerLevel level, final Ability ability) {
		this.playAnim(CreatureAnim.SLAM);
		this.busyTicks = 20;
		Vec3 at = this.position();
		Telegraph.circle(level, at, ability.radius(), 20, Telegraph.RED, () -> {
			if (!this.isAlive()) {
				return;
			}
			Attacks.hitAll(this, Attacks.inCircle(level, at, ability.radius(), 3.0), this.damage(ability), e -> Attacks.knock(e, at, 0.9, 0.7));
			level.sendParticles(ParticleTypes.EXPLOSION, at.x, at.y + 0.2, at.z, 6, ability.radius() / 2, 0.1, ability.radius() / 2, 0.0);
			new Fx(Fx.Kind.RING, ability.color()).circle(level, Fx.Kind.RING, at.add(0, 0.2, 0), ability.radius());
			level.playSound(null, at.x, at.y, at.z, SoundEvents.ANVIL_LAND, SoundSource.HOSTILE, 1.0F, 0.6F);
		});
	}
}
