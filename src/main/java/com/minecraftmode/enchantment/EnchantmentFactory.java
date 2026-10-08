package com.minecraftmode.enchantment;

import com.minecraftmode.MinecraftMode;
import java.util.Optional;
import net.minecraft.advancements.predicates.DamageSourcePredicate;
import net.minecraft.advancements.predicates.LocationPredicate;
import net.minecraft.advancements.predicates.entity.EntityFlagsPredicate;
import net.minecraft.advancements.predicates.entity.EntityPredicate;
import net.minecraft.advancements.predicates.entity.EntityTypePredicate;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.LevelBasedValue;
import net.minecraft.world.item.enchantment.effects.ApplyMobEffect;
import net.minecraft.world.item.enchantment.effects.EnchantmentAttributeEffect;
import net.minecraft.world.item.enchantment.effects.ExplodeEffect;
import net.minecraft.world.item.enchantment.effects.SummonEntityEffect;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.DamageSourceCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemEntityPropertyCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.floats.ContextFloatProviders;
import net.minecraft.world.phys.Vec3;

/**
 * Shared building blocks for the enchantment bootstraps: definitions with consistent costs,
 * common loot conditions and effects.
 */
final class EnchantmentFactory {
	final BootstrapContext<Enchantment> context;
	final HolderGetter<Item> items;
	final HolderGetter<Enchantment> enchantments;
	final HolderGetter<EntityType<?>> entityTypes;

	EnchantmentFactory(final BootstrapContext<Enchantment> context) {
		this.context = context;
		this.items = context.lookup(Registries.ITEM);
		this.enchantments = context.lookup(Registries.ENCHANTMENT);
		this.entityTypes = context.lookup(Registries.ENTITY_TYPE);
	}

	/**
	 * Level I is available from about table level 5; like Sharpness V, the top levels of a
	 * five-level enchantment need an anvil.
	 */
	Enchantment.Builder enchantment(final TagKey<Item> supported, final int weight, final int maxLevel, final EquipmentSlotGroup... slots) {
		Enchantment.Cost min = maxLevel == 1 ? Enchantment.constantCost(15) : Enchantment.dynamicCost(5, 8);
		Enchantment.Cost max = maxLevel == 1 ? Enchantment.constantCost(65) : Enchantment.dynamicCost(25, 8);
		return Enchantment.enchantment(Enchantment.definition(this.items.getOrThrow(supported), weight, maxLevel, min, max, Math.max(1, 8 / weight), slots));
	}

	void register(final EnchantInfo info, final Enchantment.Builder builder) {
		this.context.register(info.key(), builder.build(info.key().identifier()));
	}

	Holder<Enchantment> vanilla(final ResourceKey<Enchantment> key) {
		return this.enchantments.getOrThrow(key);
	}

	// ------------------------------------------------------------ conditions

	/** The hit came straight from the attacker (melee), not from a projectile. */
	static LootItemCondition.Builder melee() {
		return DamageSourceCondition.hasDamageSource(DamageSourcePredicate.Builder.damageType().isDirect(true));
	}

	/** The hit came from an arrow or bolt fired by the enchanted weapon. */
	LootItemCondition.Builder arrowHit() {
		return LootItemEntityPropertyCondition.hasProperties(
			LootContext.EntityTarget.DIRECT_ATTACKER, EntityPredicate.Builder.entity().of(this.entityTypes, EntityTypeTags.ARROWS)
		);
	}

	static LootItemCondition.Builder chancePerLevel(final float perLevel) {
		return LootItemRandomChanceCondition.randomChance(ContextFloatProviders.forEnchantmentLevel(LevelBasedValue.perLevel(perLevel)));
	}

	static LootItemCondition.Builder attacker(final EntityFlagsPredicate.Builder flags) {
		return LootItemEntityPropertyCondition.hasProperties(LootContext.EntityTarget.ATTACKER, EntityPredicate.Builder.entity().flags(flags));
	}

	static LootItemCondition.Builder victim(final EntityFlagsPredicate.Builder flags) {
		return LootItemEntityPropertyCondition.hasProperties(LootContext.EntityTarget.THIS, EntityPredicate.Builder.entity().flags(flags));
	}

	LootItemCondition.Builder victimType(final TagKey<EntityType<?>> tag) {
		return LootItemEntityPropertyCondition.hasProperties(
			LootContext.EntityTarget.THIS, EntityPredicate.Builder.entity().entityType(EntityTypePredicate.of(this.entityTypes, tag))
		);
	}

	static LootItemCondition.Builder victimUnderSky() {
		return LootItemEntityPropertyCondition.hasProperties(
			LootContext.EntityTarget.THIS, EntityPredicate.Builder.entity().located(LocationPredicate.Builder.location().setCanSeeSky(true))
		);
	}

	// ------------------------------------------------------------ effects

	static ApplyMobEffect mobEffect(final Holder<MobEffect> effect, final LevelBasedValue seconds, final LevelBasedValue amplifier) {
		return new ApplyMobEffect(HolderSet.direct(effect), seconds, seconds, amplifier, amplifier);
	}

	/** Amplifier 0 at levels I-II, 1 at III-IV, 2 at V. */
	static LevelBasedValue rampingAmplifier() {
		return LevelBasedValue.perLevel(0.0F, 0.5F);
	}

	SummonEntityEffect lightning() {
		Holder<EntityType<?>> bolt = this.entityTypes.getOrThrow(ResourceKey.create(Registries.ENTITY_TYPE, Identifier.withDefaultNamespace("lightning_bolt")));
		return new SummonEntityEffect(HolderSet.direct(bolt), false);
	}

	/** Damages entities only; never breaks blocks. */
	static ExplodeEffect explosion(final LevelBasedValue radius) {
		return new ExplodeEffect(
			true,
			Optional.empty(),
			Optional.empty(),
			Optional.empty(),
			Vec3.ZERO,
			radius,
			false,
			Level.ExplosionInteraction.NONE,
			ParticleTypes.EXPLOSION,
			ParticleTypes.EXPLOSION_EMITTER,
			WeightedList.of(),
			SoundEvents.GENERIC_EXPLODE
		);
	}

	static EnchantmentAttributeEffect attribute(
		final String id, final Holder<Attribute> attribute, final LevelBasedValue amount, final AttributeModifier.Operation operation
	) {
		return new EnchantmentAttributeEffect(MinecraftMode.id("enchantment." + id), attribute, amount, operation);
	}
}
