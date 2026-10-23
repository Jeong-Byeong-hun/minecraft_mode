package com.minecraftmode.enhance;

import com.minecraftmode.accessory.AccessorySlot;
import com.minecraftmode.job.engrave.EngraveStat;
import com.minecraftmode.job.gear.GearSlot;
import org.jspecify.annotations.Nullable;

/**
 * The nine pieces an imbue scroll can go on (0.5.49), each with the one stat its scroll adds: four class armor slots and the five accessory
 * slots. Values are the normal and greater scroll's; options are class-free, so class balance is untouched ({@code ClassComparison} leaves
 * options out). Ids are saved keys (the {@code IMBUE} component and the scroll items): never rename them.
 */
public enum ImbueSlot {
	HELMET("helmet", GearSlot.HEAD, null, EngraveStat.CRIT_DAMAGE, 10.0F, 15.0F),
	CHEST("chest", GearSlot.CHEST, null, EngraveStat.BOSS_DAMAGE, 4.0F, 6.0F),
	LEGS("legs", GearSlot.LEGS, null, EngraveStat.COOLDOWN, 3.0F, 5.0F),
	BOOTS("boots", GearSlot.FEET, null, EngraveStat.MOVE_SPEED, 10.0F, 15.0F),
	NECKLACE("necklace", null, AccessorySlot.NECKLACE, EngraveStat.SKILL_DAMAGE, 4.0F, 6.0F),
	EARRING("earring", null, AccessorySlot.EARRING, EngraveStat.CRIT_CHANCE, 2.0F, 3.0F),
	RING("ring", null, AccessorySlot.RING, EngraveStat.ATTACK_SPEED, 3.0F, 5.0F),
	BRACELET("bracelet", null, AccessorySlot.BRACELET, EngraveStat.FINAL_DAMAGE, 1.0F, 1.5F),
	TALISMAN("talisman", null, AccessorySlot.TALISMAN, EngraveStat.DAMAGE_REDUCTION, 2.0F, 3.0F);

	private final String id;
	private final @Nullable GearSlot armor;
	private final @Nullable AccessorySlot accessory;
	private final EngraveStat stat;
	private final float normal;
	private final float greater;

	ImbueSlot(final String id, final @Nullable GearSlot armor, final @Nullable AccessorySlot accessory, final EngraveStat stat, final float normal,
		final float greater) {
		this.id = id;
		this.armor = armor;
		this.accessory = accessory;
		this.stat = stat;
		this.normal = normal;
		this.greater = greater;
	}

	public String id() {
		return this.id;
	}

	public EngraveStat stat() {
		return this.stat;
	}

	/** The value a scroll of {@code grade} ({@link Imbue#NORMAL} or {@link Imbue#GREATER}) adds. */
	public float value(final int grade) {
		return grade >= Imbue.GREATER ? this.greater : this.normal;
	}

	public String nameKey() {
		return "imbue.minecraft_mode.slot." + this.id;
	}

	public static @Nullable ImbueSlot byId(final String id) {
		for (ImbueSlot slot : values()) {
			if (slot.id.equals(id)) {
				return slot;
			}
		}
		return null;
	}

	public static @Nullable ImbueSlot of(final GearSlot slot) {
		for (ImbueSlot s : values()) {
			if (s.armor == slot) {
				return s;
			}
		}
		return null;
	}

	public static @Nullable ImbueSlot of(final AccessorySlot slot) {
		for (ImbueSlot s : values()) {
			if (s.accessory == slot) {
				return s;
			}
		}
		return null;
	}
}
