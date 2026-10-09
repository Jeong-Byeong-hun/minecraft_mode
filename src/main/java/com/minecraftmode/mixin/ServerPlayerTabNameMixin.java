package com.minecraftmode.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.minecraftmode.progress.Titles;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.jspecify.annotations.Nullable;

/** The tab list shows the worn title too (the client otherwise builds the entry from the bare profile name). */
@Mixin(ServerPlayer.class)
public abstract class ServerPlayerTabNameMixin {
	@ModifyReturnValue(method = "getTabListDisplayName", at = @At("RETURN"))
	private @Nullable Component minecraftMode$title(final @Nullable Component name) {
		ServerPlayer self = (ServerPlayer)(Object)this;
		if (name != null || Titles.of(self).isEmpty()) {
			return name;
		}
		return Titles.decorate(self, Component.literal(self.getPlainTextName()));
	}
}
