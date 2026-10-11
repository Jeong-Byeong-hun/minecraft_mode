package com.minecraftmode.travel;

import com.minecraftmode.city.CityZone;
import com.minecraftmode.residence.ResidenceDimension;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Where the waymaster ({@code CityNpc.Role.WAYMASTER}) can send a player. The ordinal goes over the network: append new places at
 * the end. Each place says from where it is offered and where the player lands.
 */
public enum Destination {
	/** The residence square, from anywhere but the residence. */
	RESIDENCE("residence"),
	/** The capital's plaza (the world spawn without a capital), from the residence. */
	PLAZA("plaza");

	/** A landing spot: level, feet position and facing. */
	public record Spot(ServerLevel level, Vec3 pos, float yaw) {
	}

	private final String id;

	Destination(final String id) {
		this.id = id;
	}

	public String nameKey() {
		return "travel.minecraft_mode." + this.id;
	}

	public boolean offeredIn(final ServerLevel level) {
		return switch (this) {
			case RESIDENCE -> !ResidenceDimension.is(level);
			case PLAZA -> ResidenceDimension.is(level);
		};
	}

	public @Nullable Spot spot(final MinecraftServer server) {
		return switch (this) {
			case RESIDENCE -> {
				ServerLevel residence = ResidenceDimension.level(server);
				if (residence == null) {
					yield null;
				}
				yield new Spot(residence, ResidenceDimension.arrival(), 0.0F);
			}
			case PLAZA -> {
				ServerLevel overworld = server.overworld();
				BlockPos to = CityZone.isCityLevel(overworld) ? CityZone.spawn(CityZone.baseY(overworld)) : overworld.getRespawnData().pos();
				yield new Spot(overworld, Vec3.atBottomCenterOf(to), 180.0F);
			}
		};
	}

	public static @Nullable Destination byOrdinal(final int ordinal) {
		return ordinal >= 0 && ordinal < values().length ? values()[ordinal] : null;
	}
}
