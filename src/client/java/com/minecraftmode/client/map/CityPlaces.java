package com.minecraftmode.client.map;

import com.minecraftmode.city.CityFixtures;
import com.minecraftmode.city.CityZone;
import com.minecraftmode.city.Homestead;
import com.minecraftmode.city.TrainingGrounds;
import com.minecraftmode.entity.CityNpc;
import com.minecraftmode.entity.ClassTrainer;
import com.minecraftmode.job.JobClass;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;

/**
 * The capital's people and landmarks as map markers: every service NPC, every class trainer and the Training Grounds stairs, at
 * their posts from {@link CityZone} (the city keeps them there, see {@code CityServices.keepTrainers}). Only the x and z matter.
 */
final class CityPlaces {
	enum Kind {
		NPC,
		TRAINER,
		LANDMARK
	}

	record Place(String nameKey, int x, int z, int color, Kind kind) {
		Component name() {
			return Component.translatable(this.nameKey);
		}
	}

	private static List<Place> places;
	/** The server said this world's overworld has the capital ({@code CityInfoPayload}, sent on join). */
	static boolean cityWorld;

	/** Stormhold and its markers belong on the map of this level (the client cannot ask {@link CityZone#isCityLevel}). */
	static boolean shown(final Level level) {
		return cityWorld && level.dimension() == Level.OVERWORLD;
	}

	static List<Place> all() {
		if (places == null) {
			List<Place> out = new ArrayList<>();
			for (CityNpc.Role role : CityNpc.Role.values()) {
				out.add(place(role.nameKey(), CityZone.npcHome(role, 0), role.color(), Kind.NPC));
			}
			for (JobClass job : JobClass.PLAYABLE) {
				out.add(place(ClassTrainer.nameKey(job), CityZone.trainerHome(job, 0), job.color(), Kind.TRAINER));
			}
			out.add(place("screen.minecraft_mode.guide.training", TrainingGrounds.entrance(0), 0xC08040, Kind.LANDMARK));
			out.add(place("screen.minecraft_mode.guide.nether_portal", CityFixtures.netherPortal(0), 0x9040E0, Kind.LANDMARK));
			out.add(place("screen.minecraft_mode.guide.end_portal", CityFixtures.endPortal(0), 0x40C0A0, Kind.LANDMARK));
			out.add(place("screen.minecraft_mode.guide.waystone", CityFixtures.waystone(0), 0x60A0FF, Kind.LANDMARK));
			out.add(place("screen.minecraft_mode.guide.homestead", Homestead.waystone(0), 0x7CC050, Kind.LANDMARK));
			places = List.copyOf(out);
		}
		return places;
	}

	private static Place place(final String nameKey, final BlockPos pos, final int rgb, final Kind kind) {
		return new Place(nameKey, pos.getX(), pos.getZ(), 0xFF000000 | rgb, kind);
	}

	private CityPlaces() {
	}
}
