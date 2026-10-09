package com.minecraftmode.job.gear;

import java.util.List;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.equipment.ArmorType;
import org.jspecify.annotations.Nullable;

/** Where a piece of class gear goes. Weapons are main hand; armor uses the four armor slots. */
public enum GearSlot {
	WEAPON("weapon", null, null),
	HEAD("helmet", EquipmentSlot.HEAD, ArmorType.HELMET),
	CHEST("chestplate", EquipmentSlot.CHEST, ArmorType.CHESTPLATE),
	LEGS("leggings", EquipmentSlot.LEGS, ArmorType.LEGGINGS),
	FEET("boots", EquipmentSlot.FEET, ArmorType.BOOTS);

	public static final List<GearSlot> ARMOR = List.of(HEAD, CHEST, LEGS, FEET);

	private final String suffix;
	private final @Nullable EquipmentSlot equipmentSlot;
	private final @Nullable ArmorType armorType;

	GearSlot(final String suffix, final @Nullable EquipmentSlot equipmentSlot, final @Nullable ArmorType armorType) {
		this.suffix = suffix;
		this.equipmentSlot = equipmentSlot;
		this.armorType = armorType;
	}

	/** Item id suffix of armor pieces ({@code <set>_helmet}). */
	public String suffix() {
		return this.suffix;
	}

	public EquipmentSlot equipmentSlot() {
		return this.equipmentSlot;
	}

	public ArmorType armorType() {
		return this.armorType;
	}

	/** Index 0..3 for armor slots. */
	public int armorIndex() {
		return this.ordinal() - 1;
	}

	public String nameKey() {
		return "gear_slot.minecraft_mode." + this.name().toLowerCase(java.util.Locale.ROOT);
	}
}
