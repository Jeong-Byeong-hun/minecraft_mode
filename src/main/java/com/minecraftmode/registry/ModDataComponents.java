package com.minecraftmode.registry;

import com.minecraftmode.MinecraftMode;
import com.minecraftmode.dungeon.Keystone;
import com.minecraftmode.enhance.Enhancement;
import com.minecraftmode.job.engrave.Engravings;
import com.minecraftmode.job.gear.GearRolls;
import com.mojang.serialization.Codec;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;

public final class ModDataComponents {
	/** Engraving lines on a class weapon. */
	public static final DataComponentType<Engravings> ENGRAVINGS = Registry.register(
		BuiltInRegistries.DATA_COMPONENT_TYPE,
		MinecraftMode.id("engravings"),
		DataComponentType.<Engravings>builder().persistent(Engravings.CODEC).networkSynchronized(Engravings.STREAM_CODEC).build()
	);

	/** Random extra options of a class armor piece. */
	public static final DataComponentType<GearRolls> GEAR_ROLLS = Registry.register(
		BuiltInRegistries.DATA_COMPONENT_TYPE,
		MinecraftMode.id("gear_rolls"),
		DataComponentType.<GearRolls>builder().persistent(GearRolls.CODEC).networkSynchronized(GearRolls.STREAM_CODEC).build()
	);

	/** Bracket (10..100) of an Evolution Ether stack. */
	public static final DataComponentType<Integer> ETHER_GRADE = Registry.register(
		BuiltInRegistries.DATA_COMPONENT_TYPE,
		MinecraftMode.id("ether_grade"),
		DataComponentType.<Integer>builder().persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT).build()
	);

	/** Enhancement level (+0..+15) and the artisan's spirit of a class weapon or armor piece. */
	public static final DataComponentType<Enhancement> ENHANCEMENT = Registry.register(
		BuiltInRegistries.DATA_COMPONENT_TYPE,
		MinecraftMode.id("enhancement"),
		DataComponentType.<Enhancement>builder().persistent(Enhancement.CODEC).networkSynchronized(Enhancement.STREAM_CODEC).build()
	);

	/** A dungeon keystone's dungeon and level. */
	public static final DataComponentType<Keystone> KEYSTONE = Registry.register(
		BuiltInRegistries.DATA_COMPONENT_TYPE,
		MinecraftMode.id("keystone"),
		DataComponentType.<Keystone>builder().persistent(Keystone.CODEC).networkSynchronized(Keystone.STREAM_CODEC).build()
	);

	public static void init() {
	}

	private ModDataComponents() {
	}
}
