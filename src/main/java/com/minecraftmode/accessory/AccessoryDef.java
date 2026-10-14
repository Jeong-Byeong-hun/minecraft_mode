package com.minecraftmode.accessory;

import com.minecraftmode.job.gear.StatLine;

/** One accessory: a raid boss's set and a slot. */
public record AccessoryDef(AccessorySet set, AccessorySlot slot) {
	/** Item id, a saved key: {@code <boss>_<slot>}. */
	public String id() {
		return this.set.id() + "_" + this.slot.id();
	}

	public int level() {
		return this.set.level();
	}

	public String nameKey() {
		return "item.minecraft_mode." + this.id();
	}

	public StatLine baseLine() {
		return this.slot.baseLine(this.level());
	}
}
