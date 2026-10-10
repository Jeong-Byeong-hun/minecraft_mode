package com.minecraftmode.job;

import com.minecraftmode.job.gear.ClassGear;
import com.minecraftmode.job.gear.GearSlot;
import com.minecraftmode.job.gear.ItemLevels;
import com.minecraftmode.loot.GearDrops;
import com.minecraftmode.loot.GearIndex;
import com.minecraftmode.registry.ModAttachments;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Prediction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * Every advancement (the first choice of class included) hands out a class weapon and a full set of class armor for the player's
 * level: in each slot the lowest-level piece of the player's 10-level bracket they can use, or the best usable piece below it when
 * the bracket has none yet. Armor options are rolled as for a drop.
 */
public final class AdvanceKit {
	public static final List<GearSlot> SLOTS = List.of(GearSlot.WEAPON, GearSlot.HEAD, GearSlot.CHEST, GearSlot.LEGS, GearSlot.FEET);

	/** The pieces a {@code job} player of {@code level} who just reached {@code tier} gets (one per slot that has any). */
	public static List<ClassGear> pieces(final JobClass job, final int tier, final int level) {
		List<ClassGear> list = new ArrayList<>();
		for (GearSlot slot : SLOTS) {
			ClassGear piece = piece(job, tier, level, slot);
			if (piece != null) {
				list.add(piece);
			}
		}
		return list;
	}

	static @Nullable ClassGear piece(final JobClass job, final int tier, final int level, final GearSlot slot) {
		int bracket = ItemLevels.bracket(level);
		List<ClassGear> usable = GearIndex.list().stream()
			.filter(g -> g.job() == job && g.slot() == slot && g.level() <= level && ItemLevels.tier(g.level()) <= tier)
			.toList();
		Optional<ClassGear> inBracket = usable.stream().filter(g -> g.bracket() == bracket).min(Comparator.comparingInt(ClassGear::level));
		return inBracket.or(() -> usable.stream().max(Comparator.comparingInt(ClassGear::level))).orElse(null);
	}

	/** Hands out the kit for {@code job} tier {@code tier} once per player (a class reset does not give it again). */
	public static void give(final ServerPlayer player, final JobClass job, final int tier) {
		String key = job.id() + "_" + tier;
		List<String> given = player.getAttachedOrElse(ModAttachments.ADVANCE_KITS, List.of());
		if (given.contains(key)) {
			return;
		}
		List<String> updated = new ArrayList<>(given);
		updated.add(key);
		player.setAttached(ModAttachments.ADVANCE_KITS, List.copyOf(updated));
		RandomSource random = player.getRandom();
		List<ClassGear> pieces = pieces(job, tier, JobProgression.get(player).level());
		for (ClassGear gear : pieces) {
			ItemStack stack = GearDrops.create(gear, random);
			player.getInventory().placeItemBackInInventory(stack, Prediction.SERVER_ONLY);
		}
		if (!pieces.isEmpty()) {
			player.sendSystemMessage(Component.translatable("message.minecraft_mode.advance_kit", Component.translatable(job.tierKey(tier)).withColor(job.color()))
				.withStyle(ChatFormatting.GOLD));
		}
	}

	private AdvanceKit() {
	}
}
