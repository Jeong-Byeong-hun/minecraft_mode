package com.minecraftmode.job.gear;

import com.minecraftmode.job.JobClass;
import com.minecraftmode.job.engrave.Engraving;
import com.minecraftmode.job.engrave.Engravings;
import com.minecraftmode.job.weapon.Archetype;
import com.minecraftmode.job.weapon.JobWeapons;
import com.minecraftmode.job.weapon.WeaponDef;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * The common view of a class weapon or class armor piece: class, level, tier, slot, engraving pool
 * and line limit. Used by the engraving table, the upgrade bench and loot tables.
 */
public record ClassGear(String id, JobClass job, int level, int tier, GearSlot slot, @Nullable Archetype archetype) {
	public static @Nullable ClassGear of(final ItemStack stack) {
		WeaponDef weapon = JobWeapons.def(stack);
		if (weapon != null) {
			return of(weapon);
		}
		ArmorPieceDef piece = ClassArmor.def(stack);
		return piece == null ? null : of(piece);
	}

	public static ClassGear of(final WeaponDef def) {
		return new ClassGear(def.id(), def.job(), def.level(), def.tier(), GearSlot.WEAPON, def.archetype());
	}

	public static ClassGear of(final ArmorPieceDef def) {
		return new ClassGear(def.id(), def.job(), def.level(), def.tier(), def.slot(), null);
	}

	public boolean isWeapon() {
		return this.slot == GearSlot.WEAPON;
	}

	public int maxLines() {
		return this.isWeapon() ? Engravings.WEAPON_LINES : Engravings.ARMOR_LINES;
	}

	public int bracket() {
		return ItemLevels.bracket(this.level);
	}

	public List<Engraving> engravingPool() {
		return this.isWeapon() ? Engraving.pool(this.job, this.archetype) : Engraving.pool(this.job, this.slot);
	}
}
