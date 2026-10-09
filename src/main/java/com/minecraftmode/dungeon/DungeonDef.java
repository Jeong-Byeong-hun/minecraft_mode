package com.minecraftmode.dungeon;

import java.util.List;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.Block;

/**
 * One dungeon: three halls of monsters and a boss room, built in its theme.
 *
 * @param minLevel  every member must have reached this level
 * @param boss      named monster id of the boss (fought at the top of its level range as a champion)
 * @param power     level the monsters are tuned for (trash health and damage grow with it)
 * @param trash     monsters of the halls
 * @param timeLimit seconds to beat for a keystone upgrade
 */
public record DungeonDef(String id, String en, String ko, String descEn, String descKo, int minLevel, String boss, int power, Theme theme,
	List<EntityType<? extends Mob>> trash, int timeLimit) {
	/** Blocks of a dungeon: floor mix, walls, trim (floor edges and ceiling beams), pillars, lights and the hall doors. */
	public record Theme(List<Block> floor, Block wall, Block trim, Block pillar, Block light, Block door) {
	}

	public String nameKey() {
		return "dungeon.minecraft_mode." + this.id;
	}

	public String descKey() {
		return this.nameKey() + ".desc";
	}
}
