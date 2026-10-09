package com.minecraftmode.job.gear;

import com.minecraftmode.job.JobClass;
import com.minecraftmode.job.JobData;
import com.minecraftmode.job.JobProgression;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Prediction;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Who may use class gear: the gear's class, at the tier its level belongs to and at least its level.
 * Class armor that does not fit is taken off again by {@link #enforce} (called every half second).
 */
public final class GearRules {
	public static boolean canUse(final JobData data, final JobClass job, final int level) {
		return data.job() == job && data.tier() >= ItemLevels.tier(level) && data.level() >= level;
	}

	/** May {@code player} put it on? Creative players may wear anything; its stats still need {@link #canUse(JobData, JobClass, int)}. */
	public static boolean canUse(final Player player, final JobClass job, final int level) {
		return player.isCreative() || canUse(JobProgression.get(player), job, level);
	}

	public static Component refusal(final JobClass job, final int level) {
		return Component.translatable("message.minecraft_mode.gear.cannot_wear", Component.translatable(job.nameKey()).withColor(job.color()), level)
			.withStyle(ChatFormatting.RED);
	}

	/** Moves class armor the player may not wear back into the inventory (or drops it when full). */
	public static void enforce(final ServerPlayer player) {
		if (player.isCreative()) {
			return;
		}
		JobData data = JobProgression.get(player);
		for (GearSlot slot : GearSlot.ARMOR) {
			EquipmentSlot equipmentSlot = slot.equipmentSlot();
			ItemStack worn = player.getItemBySlot(equipmentSlot);
			ArmorPieceDef def = ClassArmor.def(worn);
			if (def == null || canUse(data, def.job(), def.level())) {
				continue;
			}
			player.setItemSlot(equipmentSlot, ItemStack.EMPTY);
			if (!player.getInventory().add(worn)) {
				player.drop(worn, false, Prediction.SERVER_ONLY);
			}
			player.sendOverlayMessage(refusal(def.job(), def.level()));
		}
	}

	private GearRules() {
	}
}
