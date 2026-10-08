package com.minecraftmode.job.quest;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Trial token: dropped only for the player doing the matching advancement trial. */
public class QuestTokenItem extends Item {
	public QuestTokenItem(final Properties properties) {
		super(properties);
	}

	@Override
	public boolean isFoil(final ItemStack itemStack) {
		return true;
	}
}
