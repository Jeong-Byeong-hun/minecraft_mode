package com.minecraftmode.travel;

import com.minecraftmode.network.OpenTravelPayload;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

/** The waymaster: lists the {@link Destination places} offered where the player stands, and takes them there. */
public final class Travel {
	public static void open(final ServerPlayer player, final int npcId) {
		List<Integer> offered = new ArrayList<>();
		for (Destination destination : Destination.values()) {
			if (destination.offeredIn(player.level())) {
				offered.add(destination.ordinal());
			}
		}
		if (ServerPlayNetworking.canSend(player, OpenTravelPayload.TYPE)) {
			ServerPlayNetworking.send(player, new OpenTravelPayload(npcId, offered));
		}
	}

	/** Takes {@code player} to {@code destination} if it is offered where they are; false otherwise. */
	public static boolean go(final ServerPlayer player, final Destination destination) {
		ServerLevel from = player.level();
		Destination.Spot spot = destination.offeredIn(from) ? destination.spot(from.getServer()) : null;
		if (spot == null) {
			return false;
		}
		if (player.getVehicle() != null) {
			player.stopRiding();
		}
		from.sendParticles(ParticleTypes.PORTAL, player.getX(), player.getY(0.5), player.getZ(), 30, 0.4, 0.8, 0.4, 0.4);
		player.teleportTo(spot.level(), spot.pos().x, spot.pos().y, spot.pos().z, Set.of(), spot.yaw(), 0.0F, true);
		player.resetFallDistance();
		spot.level().playSound(null, spot.pos().x, spot.pos().y, spot.pos().z, SoundEvents.PLAYER_TELEPORT, SoundSource.PLAYERS, 0.8F, 1.0F);
		player.sendOverlayMessage(Component.translatable("message.minecraft_mode.travel.arrived", Component.translatable(destination.nameKey()))
			.withStyle(ChatFormatting.AQUA));
		return true;
	}

	private Travel() {
	}
}
