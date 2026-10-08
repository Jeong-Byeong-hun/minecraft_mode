package com.minecraftmode.enchantment;

import com.minecraftmode.MinecraftMode;
import java.util.List;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

/**
 * Applies the armor auras once per second. For every player and aura, the effective level is the
 * highest level among all players within {@link #RADIUS} blocks (the player included) — never a sum,
 * so auras do not stack. Attribute auras use one fixed modifier id each, which also keeps them
 * separate from (and stackable with) the matching personal buff enchantment.
 */
public final class ArmorAuras {
	public static final double RADIUS = 10.0;
	private static final int UPDATE_INTERVAL = 20;
	private static final int REGENERATION_INTERVAL = 60;
	private static final int SATIATION_INTERVAL = 200;
	private static final EquipmentSlot[] ARMOR_SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

	/** One attribute modifier of an aura, scaled by the aura level. */
	record Bonus(Holder<Attribute> attribute, double perLevel, AttributeModifier.Operation operation) {
	}

	/** An aura enchantment and what it gives per level. Regeneration and satiation have no attributes. */
	public enum Aura {
		MINING(ArmorEnchantments.MINING_AURA, new Bonus(Attributes.BLOCK_BREAK_SPEED, 0.2, AttributeModifier.Operation.ADD_MULTIPLIED_BASE)),
		STRENGTH(ArmorEnchantments.STRENGTH_AURA, new Bonus(Attributes.ATTACK_DAMAGE, 1.0, AttributeModifier.Operation.ADD_VALUE)),
		GUARDIAN(ArmorEnchantments.GUARDIAN_AURA, new Bonus(Attributes.ARMOR, 1.0, AttributeModifier.Operation.ADD_VALUE)),
		SWIFTNESS(ArmorEnchantments.SWIFTNESS_AURA, new Bonus(Attributes.MOVEMENT_SPEED, 0.05, AttributeModifier.Operation.ADD_MULTIPLIED_BASE)),
		REGENERATION(ArmorEnchantments.REGENERATION_AURA),
		VITALITY(ArmorEnchantments.VITALITY_AURA, new Bonus(Attributes.MAX_HEALTH, 2.0, AttributeModifier.Operation.ADD_VALUE)),
		FORTUNE(ArmorEnchantments.FORTUNE_AURA, new Bonus(Attributes.LUCK, 1.0, AttributeModifier.Operation.ADD_VALUE)),
		TOUGHNESS(ArmorEnchantments.TOUGHNESS_AURA, new Bonus(Attributes.ARMOR_TOUGHNESS, 1.0, AttributeModifier.Operation.ADD_VALUE)),
		LEAP(
			ArmorEnchantments.LEAP_AURA,
			new Bonus(Attributes.JUMP_STRENGTH, 0.1, AttributeModifier.Operation.ADD_MULTIPLIED_BASE),
			new Bonus(Attributes.SAFE_FALL_DISTANCE, 1.0, AttributeModifier.Operation.ADD_VALUE)
		),
		SATIATION(ArmorEnchantments.SATIATION_AURA);

		final EnchantInfo info;
		final List<Bonus> bonuses;
		final Identifier modifierId;

		Aura(final EnchantInfo info, final Bonus... bonuses) {
			this.info = info;
			this.bonuses = List.of(bonuses);
			this.modifierId = MinecraftMode.id("aura/" + info.key().identifier().getPath());
		}

		public Identifier modifierId() {
			return this.modifierId;
		}
	}

	private static final Aura[] AURAS = Aura.values();

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(ArmorAuras::tick);
	}

	private static void tick(final MinecraftServer server) {
		int tick = server.getTickCount();
		if (tick % UPDATE_INTERVAL != 0) {
			return;
		}
		for (ServerLevel level : server.getAllLevels()) {
			update(level, tick);
		}
	}

	/** Recomputes and applies every aura for every player of the level. */
	public static void update(final ServerLevel level, final int tick) {
		List<ServerPlayer> players = level.players();
		if (players.isEmpty()) {
			return;
		}

		// Which auras each player emits (strongest piece wins; pieces do not add up either).
		int[][] emitted = new int[players.size()][];
		for (int p = 0; p < players.size(); p++) {
			emitted[p] = emittedLevels(level, players.get(p));
		}

		for (int r = 0; r < players.size(); r++) {
			ServerPlayer receiver = players.get(r);
			int[] received = new int[AURAS.length];
			if (receiver.isAlive() && !receiver.isSpectator()) {
				for (int e = 0; e < players.size(); e++) {
					ServerPlayer emitter = players.get(e);
					if (!emitter.isAlive() || emitter.isSpectator() || emitter.distanceToSqr(receiver) > RADIUS * RADIUS) {
						continue;
					}
					for (int a = 0; a < AURAS.length; a++) {
						received[a] = Math.max(received[a], emitted[e][a]);
					}
				}
			}
			apply(receiver, received, tick);
		}

		if (tick % (UPDATE_INTERVAL * 2) == 0) {
			for (int p = 0; p < players.size(); p++) {
				if (hasAny(emitted[p]) && !players.get(p).isSpectator()) {
					ServerPlayer emitter = players.get(p);
					level.sendParticles(ParticleTypes.ENCHANT, emitter.getX(), emitter.getY() + 1.0, emitter.getZ(), 8, 0.6, 0.5, 0.6, 0.4);
				}
			}
		}
	}

	/** The aura levels a player currently receives, as set on their attributes (for tests and debugging). */
	public static double currentBonus(final ServerPlayer player, final Aura aura) {
		if (aura.bonuses.isEmpty()) {
			return 0.0;
		}
		Bonus bonus = aura.bonuses.getFirst();
		AttributeInstance instance = player.getAttribute(bonus.attribute());
		AttributeModifier modifier = instance == null ? null : instance.getModifier(aura.modifierId);
		return modifier == null ? 0.0 : modifier.amount();
	}

	private static int[] emittedLevels(final ServerLevel level, final ServerPlayer player) {
		int[] levels = new int[AURAS.length];
		for (int a = 0; a < AURAS.length; a++) {
			var holder = EnchantLevels.holder(level, AURAS[a].info.key());
			if (holder.isEmpty()) {
				continue;
			}
			Holder<Enchantment> enchantment = holder.get();
			for (EquipmentSlot slot : ARMOR_SLOTS) {
				levels[a] = Math.max(levels[a], EnchantmentHelper.getItemEnchantmentLevel(enchantment, player.getItemBySlot(slot)));
			}
		}
		return levels;
	}

	private static void apply(final ServerPlayer player, final int[] levels, final int tick) {
		for (int a = 0; a < AURAS.length; a++) {
			Aura aura = AURAS[a];
			int level = levels[a];
			for (Bonus bonus : aura.bonuses) {
				AttributeInstance instance = player.getAttribute(bonus.attribute());
				if (instance == null) {
					continue;
				}
				if (level > 0) {
					instance.addOrUpdateTransientModifier(new AttributeModifier(aura.modifierId, bonus.perLevel() * level, bonus.operation()));
				} else {
					instance.removeModifier(aura.modifierId);
				}
			}
		}
		if (player.getHealth() > player.getMaxHealth()) {
			player.setHealth(player.getMaxHealth());
		}

		int regeneration = levels[Aura.REGENERATION.ordinal()];
		if (regeneration > 0 && tick % REGENERATION_INTERVAL == 0) {
			player.heal(0.5F * regeneration);
		}
		int satiation = levels[Aura.SATIATION.ordinal()];
		if (satiation > 0 && tick % SATIATION_INTERVAL == 0) {
			player.getFoodData().eat(satiation, 0.1F);
		}
	}

	private static boolean hasAny(final int[] levels) {
		for (int level : levels) {
			if (level > 0) {
				return true;
			}
		}
		return false;
	}

	private ArmorAuras() {
	}
}
