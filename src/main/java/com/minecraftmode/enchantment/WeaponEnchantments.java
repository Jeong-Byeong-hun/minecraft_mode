package com.minecraftmode.enchantment;

import static com.minecraftmode.enchantment.EnchantmentFactory.attacker;
import static com.minecraftmode.enchantment.EnchantmentFactory.chancePerLevel;
import static com.minecraftmode.enchantment.EnchantmentFactory.melee;
import static com.minecraftmode.enchantment.EnchantmentFactory.mobEffect;
import static com.minecraftmode.enchantment.EnchantmentFactory.rampingAmplifier;
import static com.minecraftmode.enchantment.EnchantmentFactory.victim;

import java.util.List;
import net.minecraft.advancements.predicates.entity.EntityFlagsPredicate;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentTarget;
import net.minecraft.world.item.enchantment.LevelBasedValue;
import net.minecraft.world.item.enchantment.effects.AddValue;
import net.minecraft.world.item.enchantment.effects.AllOf;
import net.minecraft.world.item.enchantment.effects.Ignite;
import net.minecraft.world.item.enchantment.effects.MultiplyValue;
import net.minecraft.world.level.storage.loot.predicates.AllOfCondition;

/** Ten sword and ten axe enchantments. Timber, Cleave, Plunder and Guillotine are implemented in code. */
public final class WeaponEnchantments {
	// --- Sword
	public static final EnchantInfo VENOM = EnchantInfo.of("venom", "Venom", "독날",
		"Melee hits poison the target for 3-7 seconds.", "근접 공격 시 대상을 3~7초 동안 중독시킵니다.");
	public static final EnchantInfo FROSTBITE = EnchantInfo.of("frostbite", "Frostbite", "서리 칼날",
		"Melee hits slow the target; longer and stronger with each level.", "근접 공격 시 대상에게 구속을 겁니다. 레벨이 오를수록 길고 강해집니다.");
	public static final EnchantInfo ENFEEBLE = EnchantInfo.of("enfeeble", "Enfeeble", "약화",
		"Melee hits weaken the target for 3-7 seconds.", "근접 공격 시 대상에게 3~7초 동안 나약함을 겁니다.");
	public static final EnchantInfo WITHERING_BLADE = EnchantInfo.of("withering_blade", "Withering Blade", "시듦의 칼날",
		"Melee hits wither the target for 3-7 seconds.", "근접 공격 시 대상을 3~7초 동안 시들게 합니다.");
	public static final EnchantInfo AMBUSH = EnchantInfo.of("ambush", "Ambush", "기습",
		"+1.5 damage per level while sneaking.", "웅크린 상태로 공격하면 레벨당 피해 +1.5.");
	public static final EnchantInfo CHARGE = EnchantInfo.of("charge", "Charge", "돌격",
		"+1 damage per level while sprinting.", "달리면서 공격하면 레벨당 피해 +1.");
	public static final EnchantInfo RAIDER_BANE = EnchantInfo.of("raider_bane", "Raider's Bane", "토벌",
		"+2.5 damage per level against illagers, witches and ravagers.", "약탈자 무리(일리저·마녀·파괴수)에게 레벨당 피해 +2.5.");
	public static final EnchantInfo MOMENTUM = EnchantInfo.of("momentum", "Momentum", "기세",
		"Melee hits grant Speed I for 2-6 seconds.", "근접 공격 시 2~6초 동안 신속 I을 얻습니다.");
	public static final EnchantInfo BULWARK_STRIKE = EnchantInfo.of("bulwark_strike", "Bulwark Strike", "방벽 일격",
		"8% chance per level on hit to gain Absorption I for 5 seconds.", "공격 시 레벨당 8% 확률로 5초 동안 흡수 I을 얻습니다.");
	public static final EnchantInfo SOUL_HARVEST = EnchantInfo.of("soul_harvest", "Soul Harvest", "영혼 수확",
		"Mobs drop 50% more experience per level.", "몹이 떨어뜨리는 경험치가 레벨당 50% 늘어납니다.");

	// --- Axe
	public static final EnchantInfo TIMBER = EnchantInfo.of("timber", "Timber", "벌목",
		"Chopping a log fells up to 16 connected logs per level.", "통나무를 베면 연결된 통나무를 레벨당 16개까지 함께 벱니다.");
	public static final EnchantInfo CLEAVE = EnchantInfo.of("cleave", "Cleave", "가르기",
		"Melee hits also deal 20% of the damage per level to enemies within 2.5 blocks.", "근접 공격 시 2.5블록 안의 다른 적에게도 피해의 레벨당 20%를 줍니다.");
	public static final EnchantInfo SUNDER = EnchantInfo.of("sunder", "Sunder", "갑옷 파쇄",
		"Ignores 10% of the target's armor per level.", "대상 방어력의 레벨당 10%를 무시합니다.");
	public static final EnchantInfo HEAVY_BLOW = EnchantInfo.of("heavy_blow", "Heavy Blow", "강타",
		"+0.5 knockback per level.", "레벨당 밀치기 +0.5.");
	public static final EnchantInfo STUN = EnchantInfo.of("stun", "Stun", "기절",
		"6% chance per level to stun the target (Slowness V and Mining Fatigue III) for 1.5 seconds.", "레벨당 6% 확률로 대상을 1.5초 동안 기절시킵니다(구속 V, 채굴 피로 III).");
	public static final EnchantInfo EMBER = EnchantInfo.of("ember", "Ember", "불씨",
		"Sets the target on fire for 3-7 seconds.", "대상을 3~7초 동안 불태웁니다.");
	public static final EnchantInfo SEARING = EnchantInfo.of("searing", "Searing", "작열",
		"+1.5 damage per level against burning targets.", "불타는 대상에게 레벨당 피해 +1.5.");
	public static final EnchantInfo BERSERK = EnchantInfo.of("berserk", "Berserk", "광폭",
		"10% chance per level on hit to gain Strength I for 3 seconds.", "공격 시 레벨당 10% 확률로 3초 동안 힘 I을 얻습니다.");
	public static final EnchantInfo PLUNDER = EnchantInfo.of("plunder", "Plunder", "약탈",
		"Hostile mobs you kill drop up to one extra copper coin per level.", "처치한 적대적 몹이 레벨당 최대 1개의 동화를 추가로 떨어뜨립니다.");
	public static final EnchantInfo GUILLOTINE = EnchantInfo.of("guillotine", "Guillotine", "참수",
		"5% chance per level for zombies, skeletons, creepers and piglins to drop their head (wither skeletons 2%).",
		"좀비·스켈레톤·크리퍼·피글린을 처치하면 레벨당 5% 확률로 머리를 떨어뜨립니다(위더 스켈레톤 2%).");

	public static final List<EnchantInfo> ALL = List.of(
		VENOM, FROSTBITE, ENFEEBLE, WITHERING_BLADE, AMBUSH, CHARGE, RAIDER_BANE, MOMENTUM, BULWARK_STRIKE, SOUL_HARVEST,
		TIMBER, CLEAVE, SUNDER, HEAVY_BLOW, STUN, EMBER, SEARING, BERSERK, PLUNDER, GUILLOTINE
	);

	static void bootstrap(final EnchantmentFactory f) {
		LevelBasedValue threeToSeven = LevelBasedValue.perLevel(3.0F, 1.0F);

		// --- Sword: on-hit effects
		f.register(VENOM, f.enchantment(ItemTags.SWORDS, 5, 5, EquipmentSlotGroup.MAINHAND)
			.withEffect(EnchantmentEffectComponents.POST_ATTACK, EnchantmentTarget.ATTACKER, EnchantmentTarget.VICTIM,
				mobEffect(MobEffects.POISON, threeToSeven, LevelBasedValue.constant(0.0F)), melee()));
		f.register(FROSTBITE, f.enchantment(ItemTags.SWORDS, 5, 5, EquipmentSlotGroup.MAINHAND)
			.withEffect(EnchantmentEffectComponents.POST_ATTACK, EnchantmentTarget.ATTACKER, EnchantmentTarget.VICTIM,
				mobEffect(MobEffects.SLOWNESS, LevelBasedValue.perLevel(2.0F, 1.0F), rampingAmplifier()), melee()));
		f.register(ENFEEBLE, f.enchantment(ItemTags.SWORDS, 5, 5, EquipmentSlotGroup.MAINHAND)
			.withEffect(EnchantmentEffectComponents.POST_ATTACK, EnchantmentTarget.ATTACKER, EnchantmentTarget.VICTIM,
				mobEffect(MobEffects.WEAKNESS, threeToSeven, LevelBasedValue.constant(0.0F)), melee()));
		f.register(WITHERING_BLADE, f.enchantment(ItemTags.SWORDS, 2, 5, EquipmentSlotGroup.MAINHAND)
			.withEffect(EnchantmentEffectComponents.POST_ATTACK, EnchantmentTarget.ATTACKER, EnchantmentTarget.VICTIM,
				mobEffect(MobEffects.WITHER, threeToSeven, LevelBasedValue.constant(0.0F)), melee()));

		// --- Sword: conditional damage
		f.register(AMBUSH, f.enchantment(ItemTags.SWORDS, 5, 5, EquipmentSlotGroup.MAINHAND)
			.withEffect(EnchantmentEffectComponents.DAMAGE, new AddValue(LevelBasedValue.perLevel(1.5F)),
				attacker(EntityFlagsPredicate.Builder.flags().setCrouching(true))));
		f.register(CHARGE, f.enchantment(ItemTags.SWORDS, 5, 5, EquipmentSlotGroup.MAINHAND)
			.withEffect(EnchantmentEffectComponents.DAMAGE, new AddValue(LevelBasedValue.perLevel(1.0F)),
				attacker(EntityFlagsPredicate.Builder.flags().setSprinting(true))));
		f.register(RAIDER_BANE, f.enchantment(ItemTags.SWORDS, 5, 5, EquipmentSlotGroup.MAINHAND)
			.withEffect(EnchantmentEffectComponents.DAMAGE, new AddValue(LevelBasedValue.perLevel(2.5F)), f.victimType(EntityTypeTags.RAIDERS)));

		// --- Sword: self buffs and rewards
		f.register(MOMENTUM, f.enchantment(ItemTags.SWORDS, 5, 5, EquipmentSlotGroup.MAINHAND)
			.withEffect(EnchantmentEffectComponents.POST_ATTACK, EnchantmentTarget.ATTACKER, EnchantmentTarget.ATTACKER,
				mobEffect(MobEffects.SPEED, LevelBasedValue.perLevel(2.0F, 1.0F), LevelBasedValue.constant(0.0F)), melee()));
		f.register(BULWARK_STRIKE, f.enchantment(ItemTags.SWORDS, 2, 5, EquipmentSlotGroup.MAINHAND)
			.withEffect(EnchantmentEffectComponents.POST_ATTACK, EnchantmentTarget.ATTACKER, EnchantmentTarget.ATTACKER,
				mobEffect(MobEffects.ABSORPTION, LevelBasedValue.constant(5.0F), LevelBasedValue.constant(0.0F)),
				AllOfCondition.allOf(melee(), chancePerLevel(0.08F))));
		f.register(SOUL_HARVEST, f.enchantment(ItemTags.SWORDS, 2, 5, EquipmentSlotGroup.MAINHAND)
			.withEffect(EnchantmentEffectComponents.MOB_EXPERIENCE, new MultiplyValue(LevelBasedValue.perLevel(1.5F, 0.5F))));

		// --- Axe (Timber, Cleave, Plunder, Guillotine: marker enchantments read by CombatEnchantmentHandlers/ToolEnchantmentHandlers)
		f.register(TIMBER, f.enchantment(ItemTags.AXES, 2, 5, EquipmentSlotGroup.MAINHAND));
		f.register(CLEAVE, f.enchantment(ItemTags.AXES, 2, 5, EquipmentSlotGroup.MAINHAND));
		f.register(SUNDER, f.enchantment(ItemTags.AXES, 5, 5, EquipmentSlotGroup.MAINHAND)
			.withEffect(EnchantmentEffectComponents.ARMOR_EFFECTIVENESS, new AddValue(LevelBasedValue.perLevel(-0.1F))));
		f.register(HEAVY_BLOW, f.enchantment(ItemTags.AXES, 5, 5, EquipmentSlotGroup.MAINHAND)
			.withEffect(EnchantmentEffectComponents.KNOCKBACK, new AddValue(LevelBasedValue.perLevel(0.5F))));
		f.register(STUN, f.enchantment(ItemTags.AXES, 2, 5, EquipmentSlotGroup.MAINHAND)
			.withEffect(EnchantmentEffectComponents.POST_ATTACK, EnchantmentTarget.ATTACKER, EnchantmentTarget.VICTIM,
				AllOf.entityEffects(
					mobEffect(MobEffects.SLOWNESS, LevelBasedValue.constant(1.5F), LevelBasedValue.constant(4.0F)),
					mobEffect(MobEffects.MINING_FATIGUE, LevelBasedValue.constant(1.5F), LevelBasedValue.constant(2.0F))
				),
				AllOfCondition.allOf(melee(), chancePerLevel(0.06F))));
		f.register(EMBER, f.enchantment(ItemTags.AXES, 5, 5, EquipmentSlotGroup.MAINHAND)
			.withEffect(EnchantmentEffectComponents.POST_ATTACK, EnchantmentTarget.ATTACKER, EnchantmentTarget.VICTIM,
				new Ignite(threeToSeven), melee()));
		f.register(SEARING, f.enchantment(ItemTags.AXES, 5, 5, EquipmentSlotGroup.MAINHAND)
			.withEffect(EnchantmentEffectComponents.DAMAGE, new AddValue(LevelBasedValue.perLevel(1.5F)),
				victim(EntityFlagsPredicate.Builder.flags().setOnFire(true))));
		f.register(BERSERK, f.enchantment(ItemTags.AXES, 2, 5, EquipmentSlotGroup.MAINHAND)
			.withEffect(EnchantmentEffectComponents.POST_ATTACK, EnchantmentTarget.ATTACKER, EnchantmentTarget.ATTACKER,
				mobEffect(MobEffects.STRENGTH, LevelBasedValue.constant(3.0F), LevelBasedValue.constant(0.0F)),
				AllOfCondition.allOf(melee(), chancePerLevel(0.1F))));
		f.register(PLUNDER, f.enchantment(ItemTags.AXES, 2, 5, EquipmentSlotGroup.MAINHAND));
		f.register(GUILLOTINE, f.enchantment(ItemTags.AXES, 1, 5, EquipmentSlotGroup.MAINHAND));
	}

	private WeaponEnchantments() {
	}
}
