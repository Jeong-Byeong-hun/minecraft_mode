package com.minecraftmode.mixin;

import com.minecraftmode.character.Characters;
import net.minecraft.network.protocol.game.ServerboundChatCommandSignedPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * A player on the character select screen ({@link Characters#choosing}) runs no commands: only the character actions of the
 * select screen reach the server then. Commands from the console, RCON or command blocks are not player packets and still run.
 */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerImplMixin {
	@Shadow
	public ServerPlayer player;

	@Inject(method = "performUnsignedChatCommand", at = @At("HEAD"), cancellable = true)
	private void minecraftMode$noCommandsWhileChoosing(final String command, final CallbackInfo ci) {
		if (Characters.choosing(this.player)) {
			ci.cancel();
		}
	}

	@Inject(method = "performSignedChatCommand", at = @At("HEAD"), cancellable = true)
	private void minecraftMode$noSignedCommandsWhileChoosing(final ServerboundChatCommandSignedPacket packet,
		final net.minecraft.network.chat.LastSeenMessages lastSeen, final CallbackInfo ci) {
		if (Characters.choosing(this.player)) {
			ci.cancel();
		}
	}
}
