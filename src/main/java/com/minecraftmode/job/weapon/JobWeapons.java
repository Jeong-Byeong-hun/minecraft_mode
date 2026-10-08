package com.minecraftmode.job.weapon;

import com.minecraftmode.MinecraftMode;
import com.minecraftmode.job.JobClass;
import com.minecraftmode.job.JobData;
import com.minecraftmode.job.JobProgression;
import com.minecraftmode.job.content.ArcherContent;
import com.minecraftmode.job.content.MageContent;
import com.minecraftmode.job.content.PirateContent;
import com.minecraftmode.job.content.RogueContent;
import com.minecraftmode.job.content.WarriorContent;
import com.minecraftmode.job.engrave.EngraveTotals;
import com.minecraftmode.job.engrave.Engravings;
import com.minecraftmode.job.skill.Skill;
import com.minecraftmode.registry.ModDataComponents;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/** All class weapons: definitions from the content classes and their registered items. */
public final class JobWeapons {
	private static final Map<String, WeaponDef> DEFS = new LinkedHashMap<>();
	private static final Map<String, Item> ITEMS = new LinkedHashMap<>();
	private static final Map<String, Skill> SKILLS = new HashMap<>();
	private static final List<Item> ITEM_LIST = new ArrayList<>();

	public static void add(final WeaponDef def) {
		if (DEFS.putIfAbsent(def.id(), def) != null) {
			throw new IllegalStateException("Duplicate class weapon " + def.id());
		}
		for (Skill skill : def.skills()) {
			if (SKILLS.putIfAbsent(skill.id(), skill) != null) {
				throw new IllegalStateException("Duplicate skill " + skill.id());
			}
		}
	}

	public static void init() {
		new WarriorContent().define();
		new RogueContent().define();
		new MageContent().define();
		new ArcherContent().define();
		new PirateContent().define();
		for (WeaponDef def : DEFS.values()) {
			ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, MinecraftMode.id(def.id()));
			Item item = new JobWeaponItem(JobWeaponItem.properties(def).setId(key), def);
			Registry.register(BuiltInRegistries.ITEM, key, item);
			ITEMS.put(def.id(), item);
			ITEM_LIST.add(item);
		}
		MinecraftMode.LOGGER.info("Registered {} class weapons with {} skills", DEFS.size(), SKILLS.size());
	}

	public static Collection<WeaponDef> all() {
		return Collections.unmodifiableCollection(DEFS.values());
	}

	public static List<WeaponDef> of(final JobClass job) {
		return DEFS.values().stream().filter(d -> d.job() == job).toList();
	}

	public static Item item(final WeaponDef def) {
		return ITEMS.get(def.id());
	}

	public static @Nullable WeaponDef def(final ItemStack stack) {
		return stack.getItem() instanceof JobWeaponItem weapon ? weapon.def() : null;
	}

	public static @Nullable WeaponDef def(final String id) {
		return DEFS.get(id);
	}

	public static @Nullable Skill skill(final String id) {
		return SKILLS.get(id);
	}

	public static int skillCount() {
		return SKILLS.size();
	}

	public static ItemStack randomWeaponStack(final RandomSource random) {
		return ITEM_LIST.isEmpty() ? ItemStack.EMPTY : new ItemStack(ITEM_LIST.get(random.nextInt(ITEM_LIST.size())));
	}

	/** Skills and engravings only work when class, tier and level all meet the weapon's requirements. */
	public static boolean isActive(final JobData data, final WeaponDef def) {
		return data.job() == def.job() && data.tier() >= def.tier() && data.level() >= def.level();
	}

	public static Engravings engravings(final ItemStack stack) {
		return stack.getOrDefault(ModDataComponents.ENGRAVINGS, Engravings.EMPTY);
	}

	/** Engraving totals of the main-hand class weapon, or empty when it is not usable by the player. */
	public static EngraveTotals activeTotals(final Player player) {
		ItemStack stack = player.getMainHandItem();
		WeaponDef def = def(stack);
		if (def == null || !isActive(JobProgression.get(player), def)) {
			return EngraveTotals.EMPTY;
		}
		return engravings(stack).totals();
	}

	private JobWeapons() {
	}
}
