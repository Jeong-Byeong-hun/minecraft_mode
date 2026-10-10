package com.minecraftmode.loot;

import com.minecraftmode.bag.Bags;
import com.minecraftmode.job.gear.ItemLevels;
import com.minecraftmode.job.weapon.JobWeaponItem;
import com.minecraftmode.registry.ModDataComponents;
import com.minecraftmode.registry.ModItems;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

/**
 * Evolution Ether: dropped by every named monster and boss kill, graded by the level bracket it came
 * from (the grade is a component, so different grades never stack). The city blacksmith turns a piece
 * of gear into the next one for {@link GearUpgrades#etherCost} of the target bracket's grade.
 *
 * <p>Use to fuse {@link #FUSE} ether into one of the next grade; sneak-use to break one into
 * {@link #FUSE} of the grade below. Both directions are the same ratio, so nothing is lost either way.
 */
public class EvolutionEtherItem extends Item {
	public static final int FUSE = 3;

	public EvolutionEtherItem(final Properties properties) {
		super(properties);
	}

	public static int grade(final ItemStack stack) {
		return stack.getOrDefault(ModDataComponents.ETHER_GRADE, ItemLevels.MIN_BRACKET);
	}

	public static ItemStack of(final int grade, final int count) {
		ItemStack stack = new ItemStack(ModItems.EVOLUTION_ETHER, count);
		stack.set(ModDataComponents.ETHER_GRADE, ItemLevels.bracket(grade));
		return stack;
	}

	@Override
	public InteractionResult use(final Level level, final Player player, final InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		int grade = grade(stack);
		boolean split = player.isShiftKeyDown();
		String problem = split
			? grade <= ItemLevels.MIN_BRACKET ? "split_min" : null
			: grade >= ItemLevels.MAX_BRACKET ? "fuse_max" : stack.getCount() < FUSE ? "fuse_need" : null;
		if (problem != null) {
			if (!level.isClientSide()) {
				player.sendOverlayMessage(Component.translatable("message.minecraft_mode.ether." + problem, FUSE).withStyle(ChatFormatting.RED));
			}
			return InteractionResult.FAIL;
		}
		if (player instanceof ServerPlayer serverPlayer) {
			ItemStack result = split ? of(grade - 10, FUSE) : of(grade + 10, 1);
			stack.shrink(split ? 1 : FUSE);
			// Bags.give hands over a copy, so the result can still be read for the message (an emptied stack reads as grade 10)
			Bags.give(serverPlayer, result);
			level.playSound(null, player.getX(), player.getY(), player.getZ(), split ? SoundEvents.AMETHYST_BLOCK_BREAK : SoundEvents.AMETHYST_BLOCK_RESONATE,
				SoundSource.PLAYERS, 0.8F, split ? 1.4F : 1.2F);
			player.sendOverlayMessage(Component.translatable("message.minecraft_mode.ether." + (split ? "split" : "fused"), result.getCount(), grade(result))
				.withStyle(ChatFormatting.LIGHT_PURPLE));
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public void appendHoverText(final ItemStack stack, final TooltipContext context, final TooltipDisplay display, final Consumer<Component> lines, final TooltipFlag flag) {
		int grade = grade(stack);
		lines.accept(Component.translatable("item.minecraft_mode.evolution_ether.cost", grade, GearUpgrades.etherCost(grade)).withStyle(ChatFormatting.GRAY));
		if (grade < ItemLevels.MAX_BRACKET) {
			lines.accept(Component.translatable("item.minecraft_mode.evolution_ether.fuse", FUSE, grade + 10).withStyle(ChatFormatting.DARK_AQUA));
		}
		if (grade > ItemLevels.MIN_BRACKET) {
			lines.accept(Component.translatable("item.minecraft_mode.evolution_ether.split", FUSE, grade - 10).withStyle(ChatFormatting.DARK_AQUA));
		}
	}

	@Override
	public Component getName(final ItemStack itemStack) {
		int grade = grade(itemStack);
		return Component.translatable("item.minecraft_mode.evolution_ether.graded", grade).withColor(JobWeaponItem.tierColor(ItemLevels.tier(grade)));
	}

	@Override
	public boolean isFoil(final ItemStack itemStack) {
		return true;
	}
}
