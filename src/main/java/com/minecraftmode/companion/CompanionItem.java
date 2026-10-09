package com.minecraftmode.companion;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

/** A pet charm or mount whistle: using it adds the pet or mount to the collection (the item is used up) and summons it. */
public class CompanionItem extends Item {
	private final boolean pet;
	private final String id;

	public CompanionItem(final boolean pet, final String id, final Properties properties) {
		super(properties);
		this.pet = pet;
		this.id = id;
	}

	public boolean isPet() {
		return this.pet;
	}

	public String companionId() {
		return this.id;
	}

	@Override
	public InteractionResult use(final Level level, final Player player, final InteractionHand hand) {
		if (player instanceof ServerPlayer serverPlayer) {
			if (!Companions.learn(serverPlayer, this.pet, this.id)) {
				serverPlayer.sendOverlayMessage(Component.translatable("message.minecraft_mode.companion.known").withStyle(ChatFormatting.YELLOW));
				return InteractionResult.FAIL;
			}
			player.getItemInHand(hand).shrink(1);
			if (this.pet) {
				Companions.summonPet(serverPlayer, this.id);
			} else {
				Companions.summonMount(serverPlayer, this.id);
			}
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public Component getName(final ItemStack itemStack) {
		Component name = Component.translatable(this.pet ? Companions.pet(this.id).nameKey() : Companions.mount(this.id).nameKey());
		return Component.translatable(this.pet ? "item.minecraft_mode.pet_charm" : "item.minecraft_mode.mount_whistle", name);
	}

	@Override
	@SuppressWarnings("deprecation")
	public void appendHoverText(final ItemStack stack, final TooltipContext context, final TooltipDisplay display, final Consumer<Component> lines, final TooltipFlag flag) {
		if (this.pet) {
			Companions.PetDef def = Companions.pet(this.id);
			lines.accept(Component.translatable("entity.minecraft_mode.pet_" + this.id + ".desc").withStyle(ChatFormatting.GRAY));
			lines.accept(Component.translatable("tooltip.minecraft_mode.pet.bonus").withStyle(ChatFormatting.AQUA));
			def.lines(1).forEach(l -> lines.accept(Component.literal(" • ").withStyle(ChatFormatting.DARK_GRAY)
				.append(Component.translatable(l.stat().key(), fmt(l.value())).withStyle(ChatFormatting.AQUA))));
		} else {
			Companions.MountDef def = Companions.mount(this.id);
			lines.accept(Component.translatable("entity.minecraft_mode.mount_" + this.id + ".desc").withStyle(ChatFormatting.GRAY));
			lines.accept(Component.translatable(def.flying() ? "tooltip.minecraft_mode.mount.flying" : "tooltip.minecraft_mode.mount.speed", Math.round(def.speed() * 43.17))
				.withStyle(ChatFormatting.AQUA));
		}
		lines.accept(Component.translatable("tooltip.minecraft_mode.companion.use").withStyle(ChatFormatting.DARK_GRAY));
	}

	private static String fmt(final float v) {
		float r = Math.round(v * 100.0F) / 100.0F;
		return r == Math.rint(r) ? Integer.toString((int)r) : Float.toString(r);
	}
}
