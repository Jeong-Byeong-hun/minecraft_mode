package com.minecraftmode.job.gear;

import com.minecraftmode.MinecraftMode;
import com.minecraftmode.job.JobClass;
import com.minecraftmode.job.gear.content.ArcherArmor;
import com.minecraftmode.job.gear.content.HunterArmor;
import com.minecraftmode.job.gear.content.MageArmor;
import com.minecraftmode.job.gear.content.PirateArmor;
import com.minecraftmode.job.gear.content.RogueArmor;
import com.minecraftmode.job.gear.content.ShinigamiArmor;
import com.minecraftmode.job.gear.content.WarriorArmor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
import org.jspecify.annotations.Nullable;

/** All class armor sets (from the content classes) and their registered pieces. */
public final class ClassArmor {
	private static final Map<String, ArmorSetDef> SETS = new LinkedHashMap<>();
	private static final Map<String, ArmorPieceDef> PIECES = new LinkedHashMap<>();
	private static final Map<String, Item> ITEMS = new LinkedHashMap<>();

	public static void add(final ArmorSetDef set) {
		if (SETS.putIfAbsent(set.id(), set) != null) {
			throw new IllegalStateException("Duplicate armor set " + set.id());
		}
	}

	public static void init() {
		new WarriorArmor().define();
		new RogueArmor().define();
		new MageArmor().define();
		new ArcherArmor().define();
		new PirateArmor().define();
		new ShinigamiArmor().define();
		new HunterArmor().define();
		for (ArmorSetDef set : SETS.values()) {
			ResourceKey<EquipmentAsset> asset = asset(set);
			for (GearSlot slot : GearSlot.ARMOR) {
				ArmorPieceDef piece = new ArmorPieceDef(set, slot);
				ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, MinecraftMode.id(piece.id()));
				Item item = new GearArmorItem(GearArmorItem.properties(piece, asset).setId(key), piece);
				Registry.register(BuiltInRegistries.ITEM, key, item);
				PIECES.put(piece.id(), piece);
				ITEMS.put(piece.id(), item);
			}
		}
		MinecraftMode.LOGGER.info("Registered {} class armor sets ({} pieces)", SETS.size(), PIECES.size());
	}

	public static ResourceKey<EquipmentAsset> asset(final ArmorSetDef set) {
		return ResourceKey.create(EquipmentAssets.ROOT_ID, MinecraftMode.id(set.id()));
	}

	public static Collection<ArmorSetDef> sets() {
		return Collections.unmodifiableCollection(SETS.values());
	}

	public static List<ArmorSetDef> sets(final JobClass job) {
		return SETS.values().stream().filter(s -> s.job() == job).toList();
	}

	public static @Nullable ArmorSetDef set(final String id) {
		return SETS.get(id);
	}

	public static Collection<ArmorPieceDef> pieces() {
		return Collections.unmodifiableCollection(PIECES.values());
	}

	public static List<ArmorPieceDef> pieces(final JobClass job) {
		return PIECES.values().stream().filter(p -> p.job() == job).toList();
	}

	public static @Nullable ArmorPieceDef piece(final String id) {
		return PIECES.get(id);
	}

	public static Item item(final ArmorPieceDef piece) {
		return ITEMS.get(piece.id());
	}

	public static @Nullable ArmorPieceDef def(final ItemStack stack) {
		return stack.getItem() instanceof GearArmorItem armor ? armor.def() : null;
	}

	public static List<ArmorPieceDef> piecesOf(final ArmorSetDef set) {
		List<ArmorPieceDef> list = new ArrayList<>();
		for (GearSlot slot : GearSlot.ARMOR) {
			list.add(PIECES.get(set.id() + "_" + slot.suffix()));
		}
		return list;
	}

	private ClassArmor() {
	}
}
