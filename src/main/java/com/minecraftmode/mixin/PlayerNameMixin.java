package com.minecraftmode.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.minecraftmode.progress.Titles;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Puts the worn title in front of a player's display name (name tag, chat, join messages). */
@Mixin(Player.class)
public abstract class PlayerNameMixin {
	@ModifyReturnValue(method = "getDisplayName", at = @At("RETURN"))
	private Component minecraftMode$title(final Component name) {
		return Titles.decorate((Player)(Object)this, name);
	}
}
