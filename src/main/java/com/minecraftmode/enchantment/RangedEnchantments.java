package com.minecraftmode.enchantment;

import static com.minecraftmode.enchantment.EnchantmentFactory.chancePerLevel;
import static com.minecraftmode.enchantment.EnchantmentFactory.explosion;
import static com.minecraftmode.enchantment.EnchantmentFactory.mobEffect;
import static com.minecraftmode.enchantment.EnchantmentFactory.rampingAmplifier;
import static com.minecraftmode.enchantment.EnchantmentFactory.victimUnderSky;

import java.util.List;
import net.minecraft.core.HolderSet;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.valueproviders.ConstantFloat;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentTarget;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.LevelBasedValue;
import net.minecraft.world.item.enchantment.effects.AddValue;
import net.minecraft.world.item.enchantment.effects.AllOf;
import net.minecraft.world.item.enchantment.effects.Ignite;
import net.minecraft.world.item.enchantment.effects.PlaySoundEffect;
import net.minecraft.world.item.enchantment.effects.SetValue;
import net.minecraft.world.level.storage.loot.predicates.AllOfCondition;

/**
 * Ten bow and ten crossbow enchantments. Sharpshooter/Reinforced Bolt add flat damage per level on
 * top of Power. Quick Draw is implemented by {@code BowItemMixin}.
 */
public final class RangedEnchantments {
	// --- Bow
	public static final EnchantInfo QUICK_DRAW = EnchantInfo.of("quick_draw", "Quick Draw", "속사",
		"Draws the bow 25% faster per level.", "활을 당기는 속도가 레벨당 25% 빨라집니다.");
	public static final EnchantInfo SHARPSHOOTER = EnchantInfo.of("sharpshooter", "Sharpshooter", "명사수",
		"Arrows deal +1 flat damage per level (stacks with Power).", "화살 피해가 레벨당 +1 늘어납니다(힘과 별도로 중첩).");
	public static final EnchantInfo VOLLEY = EnchantInfo.of("volley", "Volley", "일제 사격",
		"Fires one extra arrow per level.", "레벨당 화살을 1발 더 쏩니다.");
	public static final EnchantInfo PIERCING_SHOT = EnchantInfo.of("piercing_shot", "Piercing Shot", "관통 사격",
		"Arrows pass through one extra enemy per level.", "화살이 레벨당 적 1명을 더 관통합니다.");
	public static final EnchantInfo CONSERVATION = EnchantInfo.of("conservation", "Conservation", "화살 절약",
		"15% chance per level not to use up an arrow.", "레벨당 15% 확률로 화살을 소모하지 않습니다.");
	public static final EnchantInfo VENOM_ARROW = EnchantInfo.of("venom_arrow", "Venom Arrow", "독화살",
		"Arrows poison the target for 3-7 seconds.", "화살에 맞은 대상을 3~7초 동안 중독시킵니다.");
	public static final EnchantInfo FROST_ARROW = EnchantInfo.of("frost_arrow", "Frost Arrow", "빙결 화살",
		"Arrows slow the target; longer and stronger with each level.", "화살에 맞은 대상에게 구속을 겁니다. 레벨이 오를수록 길고 강해집니다.");
	public static final EnchantInfo EXPLOSIVE_ARROW = EnchantInfo.of("explosive_arrow", "Explosive Arrow", "폭발 화살",
		"Arrows explode on hit (radius 1.5-2.5). Never breaks blocks.", "화살이 명중하면 폭발합니다(반경 1.5~2.5). 블록은 부수지 않습니다.");
	public static final EnchantInfo THUNDER_ARROW = EnchantInfo.of("thunder_arrow", "Thunder Arrow", "낙뢰 화살",
		"10% chance per level to call lightning on a target under the open sky.", "하늘이 보이는 대상에게 레벨당 10% 확률로 번개를 내립니다.");
	public static final EnchantInfo GALE_ARROW = EnchantInfo.of("gale_arrow", "Gale Arrow", "부양 화살",
		"Arrows lift the target into the air (Levitation for 1-3 seconds).", "화살에 맞은 대상을 공중으로 띄웁니다(공중 부양 1~3초).");

	// --- Crossbow
	public static final EnchantInfo RAPID_RELOAD = EnchantInfo.of("rapid_reload", "Rapid Reload", "신속 장전",
		"Reloads 0.2 seconds faster per level. Incompatible with Quick Charge.", "장전이 레벨당 0.2초 빨라집니다. 빠른 장전과 함께 쓸 수 없습니다.");
	public static final EnchantInfo REINFORCED_BOLT = EnchantInfo.of("reinforced_bolt", "Reinforced Bolt", "강화 볼트",
		"Bolts deal +1 flat damage per level.", "볼트 피해가 레벨당 +1 늘어납니다.");
	public static final EnchantInfo BARRAGE = EnchantInfo.of("barrage", "Barrage", "연발",
		"Fires two extra bolts per level. Incompatible with Multishot and Piercing.", "레벨당 볼트를 2발 더 쏩니다. 다중 발사·관통과 함께 쓸 수 없습니다.");
	public static final EnchantInfo IMPACT_BOLT = EnchantInfo.of("impact_bolt", "Impact Bolt", "충격 볼트",
		"+1 knockback per level for bolts.", "볼트의 밀치기가 레벨당 +1 늘어납니다.");
	public static final EnchantInfo THRIFT = EnchantInfo.of("thrift", "Thrift", "탄약 절약",
		"15% chance per level not to use up ammunition when loading.", "장전할 때 레벨당 15% 확률로 탄약을 소모하지 않습니다.");
	public static final EnchantInfo EXPLOSIVE_BOLT = EnchantInfo.of("explosive_bolt", "Explosive Bolt", "폭발 볼트",
		"Bolts explode on hit (radius 1.5-2.5). Never breaks blocks.", "볼트가 명중하면 폭발합니다(반경 1.5~2.5). 블록은 부수지 않습니다.");
	public static final EnchantInfo INCENDIARY_BOLT = EnchantInfo.of("incendiary_bolt", "Incendiary Bolt", "소이 볼트",
		"Bolts fly burning and set the target on fire for 3-7 seconds.", "볼트가 불붙은 채 날아가 대상을 3~7초 동안 불태웁니다.");
	public static final EnchantInfo FLASH_BOLT = EnchantInfo.of("flash_bolt", "Flash Bolt", "섬광 볼트",
		"Bolts blind the target for 2-6 seconds and make it glow.", "볼트에 맞은 대상을 2~6초 동안 실명시키고 발광시킵니다.");
	public static final EnchantInfo WITHERING_BOLT = EnchantInfo.of("withering_bolt", "Withering Bolt", "부패 볼트",
		"Bolts wither the target for 3-7 seconds.", "볼트에 맞은 대상을 3~7초 동안 시들게 합니다.");
	public static final EnchantInfo THUNDER_BOLT = EnchantInfo.of("thunder_bolt", "Thunder Bolt", "뇌전 볼트",
		"12% chance per level to call lightning on a target under the open sky.", "하늘이 보이는 대상에게 레벨당 12% 확률로 번개를 내립니다.");

	public static final List<EnchantInfo> ALL = List.of(
		QUICK_DRAW, SHARPSHOOTER, VOLLEY, PIERCING_SHOT, CONSERVATION, VENOM_ARROW, FROST_ARROW, EXPLOSIVE_ARROW, THUNDER_ARROW, GALE_ARROW,
		RAPID_RELOAD, REINFORCED_BOLT, BARRAGE, IMPACT_BOLT, THRIFT, EXPLOSIVE_BOLT, INCENDIARY_BOLT, FLASH_BOLT, WITHERING_BOLT, THUNDER_BOLT
	);

	static void bootstrap(final EnchantmentFactory f) {
		LevelBasedValue threeToSeven = LevelBasedValue.perLevel(3.0F, 1.0F);
		var lightningStrike = AllOf.entityEffects(
			f.lightning(), new PlaySoundEffect(List.of(SoundEvents.TRIDENT_THUNDER), ConstantFloat.of(5.0F), ConstantFloat.of(1.0F))
		);

		// --- Bow
		f.register(QUICK_DRAW, f.enchantment(ItemTags.BOW_ENCHANTABLE, 5, 5, EquipmentSlotGroup.MAINHAND));
		f.register(SHARPSHOOTER, f.enchantment(ItemTags.BOW_ENCHANTABLE, 5, 5, EquipmentSlotGroup.MAINHAND)
			.withEffect(EnchantmentEffectComponents.DAMAGE, new AddValue(LevelBasedValue.perLevel(1.0F)), f.arrowHit()));
		f.register(VOLLEY, f.enchantment(ItemTags.BOW_ENCHANTABLE, 2, 3, EquipmentSlotGroup.MAINHAND)
			.withEffect(EnchantmentEffectComponents.PROJECTILE_COUNT, new AddValue(LevelBasedValue.perLevel(1.0F)))
			.withEffect(EnchantmentEffectComponents.PROJECTILE_SPREAD, new AddValue(LevelBasedValue.perLevel(6.0F))));
		f.register(PIERCING_SHOT, f.enchantment(ItemTags.BOW_ENCHANTABLE, 5, 4, EquipmentSlotGroup.MAINHAND)
			.withEffect(EnchantmentEffectComponents.PROJECTILE_PIERCING, new AddValue(LevelBasedValue.perLevel(1.0F))));
		f.register(CONSERVATION, f.enchantment(ItemTags.BOW_ENCHANTABLE, 2, 5, EquipmentSlotGroup.MAINHAND)
			.exclusiveWith(HolderSet.direct(f.vanilla(Enchantments.INFINITY)))
			.withEffect(EnchantmentEffectComponents.AMMO_USE, new SetValue(LevelBasedValue.constant(0.0F)), chancePerLevel(0.15F)));
		f.register(VENOM_ARROW, f.enchantment(ItemTags.BOW_ENCHANTABLE, 5, 5, EquipmentSlotGroup.MAINHAND)
			.withEffect(EnchantmentEffectComponents.POST_ATTACK, EnchantmentTarget.ATTACKER, EnchantmentTarget.VICTIM,
				mobEffect(MobEffects.POISON, threeToSeven, LevelBasedValue.constant(0.0F)), f.arrowHit()));
		f.register(FROST_ARROW, f.enchantment(ItemTags.BOW_ENCHANTABLE, 5, 5, EquipmentSlotGroup.MAINHAND)
			.withEffect(EnchantmentEffectComponents.POST_ATTACK, EnchantmentTarget.ATTACKER, EnchantmentTarget.VICTIM,
				mobEffect(MobEffects.SLOWNESS, LevelBasedValue.perLevel(2.0F, 1.0F), rampingAmplifier()), f.arrowHit()));
		f.register(EXPLOSIVE_ARROW, f.enchantment(ItemTags.BOW_ENCHANTABLE, 1, 3, EquipmentSlotGroup.MAINHAND)
			.withEffect(EnchantmentEffectComponents.POST_ATTACK, EnchantmentTarget.ATTACKER, EnchantmentTarget.VICTIM,
				explosion(LevelBasedValue.perLevel(1.5F, 0.5F)), f.arrowHit()));
		f.register(THUNDER_ARROW, f.enchantment(ItemTags.BOW_ENCHANTABLE, 1, 3, EquipmentSlotGroup.MAINHAND)
			.withEffect(EnchantmentEffectComponents.POST_ATTACK, EnchantmentTarget.ATTACKER, EnchantmentTarget.VICTIM,
				lightningStrike, AllOfCondition.allOf(f.arrowHit(), victimUnderSky(), chancePerLevel(0.1F))));
		f.register(GALE_ARROW, f.enchantment(ItemTags.BOW_ENCHANTABLE, 2, 5, EquipmentSlotGroup.MAINHAND)
			.withEffect(EnchantmentEffectComponents.POST_ATTACK, EnchantmentTarget.ATTACKER, EnchantmentTarget.VICTIM,
				mobEffect(MobEffects.LEVITATION, LevelBasedValue.perLevel(1.0F, 0.5F), LevelBasedValue.constant(0.0F)), f.arrowHit()));

		// --- Crossbow
		f.register(RAPID_RELOAD, f.enchantment(ItemTags.CROSSBOW_ENCHANTABLE, 5, 5, EquipmentSlotGroup.MAINHAND, EquipmentSlotGroup.OFFHAND)
			.exclusiveWith(HolderSet.direct(f.vanilla(Enchantments.QUICK_CHARGE)))
			.withSpecialEffect(EnchantmentEffectComponents.CROSSBOW_CHARGE_TIME, new AddValue(LevelBasedValue.perLevel(-0.2F))));
		f.register(REINFORCED_BOLT, f.enchantment(ItemTags.CROSSBOW_ENCHANTABLE, 5, 5, EquipmentSlotGroup.MAINHAND)
			.withEffect(EnchantmentEffectComponents.DAMAGE, new AddValue(LevelBasedValue.perLevel(1.0F)), f.arrowHit()));
		// Exclusive with Multishot and Piercing through the #exclusive_set/crossbow tag (see ModTagProviders).
		f.register(BARRAGE, f.enchantment(ItemTags.CROSSBOW_ENCHANTABLE, 2, 2, EquipmentSlotGroup.MAINHAND)
			.exclusiveWith(f.enchantments.getOrThrow(net.minecraft.tags.EnchantmentTags.CROSSBOW_EXCLUSIVE))
			.withEffect(EnchantmentEffectComponents.PROJECTILE_COUNT, new AddValue(LevelBasedValue.perLevel(2.0F)))
			.withEffect(EnchantmentEffectComponents.PROJECTILE_SPREAD, new AddValue(LevelBasedValue.perLevel(10.0F))));
		f.register(IMPACT_BOLT, f.enchantment(ItemTags.CROSSBOW_ENCHANTABLE, 5, 5, EquipmentSlotGroup.MAINHAND)
			.withEffect(EnchantmentEffectComponents.KNOCKBACK, new AddValue(LevelBasedValue.perLevel(1.0F)), f.arrowHit()));
		f.register(THRIFT, f.enchantment(ItemTags.CROSSBOW_ENCHANTABLE, 2, 5, EquipmentSlotGroup.MAINHAND)
			.withEffect(EnchantmentEffectComponents.AMMO_USE, new SetValue(LevelBasedValue.constant(0.0F)), chancePerLevel(0.15F)));
		f.register(EXPLOSIVE_BOLT, f.enchantment(ItemTags.CROSSBOW_ENCHANTABLE, 1, 3, EquipmentSlotGroup.MAINHAND)
			.withEffect(EnchantmentEffectComponents.POST_ATTACK, EnchantmentTarget.ATTACKER, EnchantmentTarget.VICTIM,
				explosion(LevelBasedValue.perLevel(1.5F, 0.5F)), f.arrowHit()));
		f.register(INCENDIARY_BOLT, f.enchantment(ItemTags.CROSSBOW_ENCHANTABLE, 2, 3, EquipmentSlotGroup.MAINHAND)
			.withEffect(EnchantmentEffectComponents.PROJECTILE_SPAWNED, new Ignite(LevelBasedValue.constant(100.0F)))
			.withEffect(EnchantmentEffectComponents.POST_ATTACK, EnchantmentTarget.ATTACKER, EnchantmentTarget.VICTIM,
				new Ignite(LevelBasedValue.perLevel(3.0F, 2.0F)), f.arrowHit()));
		f.register(FLASH_BOLT, f.enchantment(ItemTags.CROSSBOW_ENCHANTABLE, 5, 5, EquipmentSlotGroup.MAINHAND)
			.withEffect(EnchantmentEffectComponents.POST_ATTACK, EnchantmentTarget.ATTACKER, EnchantmentTarget.VICTIM,
				AllOf.entityEffects(
					mobEffect(MobEffects.BLINDNESS, LevelBasedValue.perLevel(2.0F, 1.0F), LevelBasedValue.constant(0.0F)),
					mobEffect(MobEffects.GLOWING, LevelBasedValue.perLevel(4.0F, 2.0F), LevelBasedValue.constant(0.0F))
				),
				f.arrowHit()));
		f.register(WITHERING_BOLT, f.enchantment(ItemTags.CROSSBOW_ENCHANTABLE, 2, 5, EquipmentSlotGroup.MAINHAND)
			.withEffect(EnchantmentEffectComponents.POST_ATTACK, EnchantmentTarget.ATTACKER, EnchantmentTarget.VICTIM,
				mobEffect(MobEffects.WITHER, threeToSeven, LevelBasedValue.constant(0.0F)), f.arrowHit()));
		f.register(THUNDER_BOLT, f.enchantment(ItemTags.CROSSBOW_ENCHANTABLE, 1, 3, EquipmentSlotGroup.MAINHAND)
			.withEffect(EnchantmentEffectComponents.POST_ATTACK, EnchantmentTarget.ATTACKER, EnchantmentTarget.VICTIM,
				lightningStrike, AllOfCondition.allOf(f.arrowHit(), victimUnderSky(), chancePerLevel(0.12F))));
	}

	private RangedEnchantments() {
	}
}
