package com.minecraftmode.command;

import com.minecraftmode.residence.LandOffice;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.Commands;

/** {@code /land}: opens the land screen (claims and trusted players) without a deed in hand. */
public final class LandCommand {
	public static void init() {
		CommandRegistrationCallback.EVENT.register((dispatcher, context, selection) -> dispatcher.register(Commands.literal("land").executes(c -> {
			LandOffice.open(c.getSource().getPlayerOrException());
			return 1;
		})));
	}

	private LandCommand() {
	}
}
