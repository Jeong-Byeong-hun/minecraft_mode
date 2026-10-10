package com.minecraftmode.client.job;

import com.minecraftmode.MinecraftMode;
import com.minecraftmode.network.CastSkillPayload;
import com.minecraftmode.network.InnateAbilityPayload;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

/** Skill keys (R, X, V, Z by default; G is vanilla quick actions), the innate class ability (B) and the class screen key (K). */
public final class JobKeys {
	public static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(MinecraftMode.id("classes"));
	public static final KeyMapping[] SKILLS = {
		register("skill_1", InputConstants.KEY_R),
		register("skill_2", InputConstants.KEY_X),
		register("skill_3", InputConstants.KEY_V),
		register("skill_4", InputConstants.KEY_Z)
	};
	public static final KeyMapping OPEN_SCREEN = register("job_screen", InputConstants.KEY_K);
	public static final KeyMapping INNATE = register("innate_ability", InputConstants.KEY_B);
	public static final KeyMapping CHARACTER = register("character_screen", InputConstants.KEY_I);

	private static KeyMapping register(final String name, final int key) {
		return KeyMappingHelper.registerKeyMapping(new KeyMapping("key.minecraft_mode." + name, InputConstants.Type.KEYBOARD, key, CATEGORY));
	}

	public static void init() {
		ClientTickEvents.END_CLIENT_TICK.register(JobKeys::tick);
	}

	private static void tick(final Minecraft minecraft) {
		if (minecraft.player == null) {
			return;
		}
		for (int i = 0; i < SKILLS.length; i++) {
			while (SKILLS[i].consumeClick()) {
				if (ClientPlayNetworking.canSend(CastSkillPayload.TYPE)) {
					ClientPlayNetworking.send(new CastSkillPayload(i));
				}
			}
		}
		while (INNATE.consumeClick()) {
			if (ClientPlayNetworking.canSend(InnateAbilityPayload.TYPE)) {
				ClientPlayNetworking.send(InnateAbilityPayload.INSTANCE);
			}
		}
		while (OPEN_SCREEN.consumeClick()) {
			minecraft.gui.setScreen(new JobScreen());
		}
		while (CHARACTER.consumeClick()) {
			if (minecraft.gui.screen() == null) {
				minecraft.gui.setScreen(new CharacterScreen());
			}
		}
	}

	private JobKeys() {
	}
}
