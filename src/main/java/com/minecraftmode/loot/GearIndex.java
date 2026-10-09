package com.minecraftmode.loot;

import com.minecraftmode.job.JobClass;
import com.minecraftmode.job.gear.ArmorPieceDef;
import com.minecraftmode.job.gear.ClassArmor;
import com.minecraftmode.job.gear.ClassGear;
import com.minecraftmode.job.gear.GearSlot;
import com.minecraftmode.job.gear.ItemLevels;
import com.minecraftmode.job.weapon.JobWeapons;
import com.minecraftmode.job.weapon.WeaponDef;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * Every piece of class gear (161 weapons, 280 armor pieces) with its level, and the split between
 * the shop and drops: for each class and 10-level bracket the guild sells the lowest-level weapon
 * of the bracket and one armor piece (head, chest, legs, feet in turn); everything else only drops.
 */
public final class GearIndex {
	private static @Nullable Map<String, ClassGear> all;
	private static @Nullable Set<String> shop;

	private static Map<String, ClassGear> all() {
		if (all == null) {
			Map<String, ClassGear> map = new LinkedHashMap<>();
			for (WeaponDef def : JobWeapons.all()) {
				map.put(def.id(), ClassGear.of(def));
			}
			for (ArmorPieceDef piece : ClassArmor.pieces()) {
				map.put(piece.id(), ClassGear.of(piece));
			}
			all = map;
		}
		return all;
	}

	public static List<ClassGear> list() {
		return List.copyOf(all().values());
	}

	public static @Nullable ClassGear byId(final String id) {
		return all().get(id);
	}

	public static Item item(final ClassGear gear) {
		if (gear.isWeapon()) {
			return JobWeapons.item(JobWeapons.def(gear.id()));
		}
		return ClassArmor.item(ClassArmor.piece(gear.id()));
	}

	public static ItemStack stack(final ClassGear gear) {
		return new ItemStack(item(gear));
	}

	/** Armor slot the shop sells in a bracket: head at 10, chest at 20, legs at 30, feet at 40, head again at 50... */
	public static GearSlot shopArmorSlot(final int bracket) {
		return GearSlot.ARMOR.get((bracket / 10 - 1) % 4);
	}

	/** The shop items of a class in a bracket: the bracket's lowest-level weapon and one armor piece. */
	public static List<ClassGear> shopItems(final JobClass job, final int bracket) {
		List<ClassGear> list = new ArrayList<>();
		all().values().stream()
			.filter(g -> g.job() == job && g.isWeapon() && ItemLevels.bracket(g.level()) == bracket)
			.min(Comparator.comparingInt(ClassGear::level))
			.ifPresent(list::add);
		GearSlot slot = shopArmorSlot(bracket);
		all().values().stream()
			.filter(g -> g.job() == job && g.slot() == slot && ItemLevels.bracket(g.level()) == bracket)
			.min(Comparator.comparingInt(ClassGear::level))
			.ifPresent(list::add);
		return list;
	}

	public static boolean isShopItem(final ClassGear gear) {
		if (shop == null) {
			Set<String> ids = new HashSet<>();
			for (JobClass job : JobClass.PLAYABLE) {
				for (int bracket = ItemLevels.MIN_BRACKET; bracket <= ItemLevels.MAX_BRACKET; bracket += 10) {
					shopItems(job, bracket).forEach(g -> ids.add(g.id()));
				}
			}
			shop = ids;
		}
		return shop.contains(gear.id());
	}

	/** Drop-only gear (any class) whose level is within [lo, hi]. */
	public static List<ClassGear> dropPool(final int lo, final int hi) {
		return all().values().stream().filter(g -> g.level() >= lo && g.level() <= hi && !isShopItem(g)).toList();
	}

	private GearIndex() {
	}
}
