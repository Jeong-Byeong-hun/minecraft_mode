package com.minecraftmode.registry;

import com.minecraftmode.MinecraftMode;
import com.minecraftmode.job.engrave.Engravings;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;

public final class ModDataComponents {
	/** Engraving lines on a class weapon. */
	public static final DataComponentType<Engravings> ENGRAVINGS = Registry.register(
		BuiltInRegistries.DATA_COMPONENT_TYPE,
		MinecraftMode.id("engravings"),
		DataComponentType.<Engravings>builder().persistent(Engravings.CODEC).networkSynchronized(Engravings.STREAM_CODEC).build()
	);

	public static void init() {
	}

	private ModDataComponents() {
	}
}
