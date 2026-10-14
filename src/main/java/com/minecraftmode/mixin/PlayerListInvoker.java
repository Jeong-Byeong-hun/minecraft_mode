package com.minecraftmode.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Writes one player's file (data, stats, advancements); vanilla only exposes saving everyone (character switch, {@code Characters}). */
@Mixin(PlayerList.class)
public interface PlayerListInvoker {
	@Invoker("save")
	void minecraftMode$save(ServerPlayer player);
}
