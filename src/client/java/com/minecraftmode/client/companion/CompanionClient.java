package com.minecraftmode.client.companion;

import com.minecraftmode.client.creature.CreaturePlans;
import com.minecraftmode.client.creature.CreatureRenderer;
import com.minecraftmode.client.job.JobKeys;
import com.minecraftmode.companion.Companions;
import com.minecraftmode.network.ProgressActionPayload;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.renderer.entity.EntityRenderers;

/** Pet and mount renderers, the collection key (U; P is vanilla social interactions) and the mount key (H: call the last mount, or get off). */
public final class CompanionClient {
	public static final KeyMapping COLLECTION = KeyMappingHelper.registerKeyMapping(
		new KeyMapping("key.minecraft_mode.companion_screen", InputConstants.Type.KEYBOARD, InputConstants.KEY_U, JobKeys.CATEGORY));
	public static final KeyMapping MOUNT = KeyMappingHelper.registerKeyMapping(
		new KeyMapping("key.minecraft_mode.mount", InputConstants.Type.KEYBOARD, InputConstants.KEY_H, JobKeys.CATEGORY));

	/** Call after {@link CreaturePlans#registerLayers()}. */
	public static void init() {
		for (Companions.PetDef def : Companions.pets()) {
			String id = "pet_" + def.id();
			EntityRenderers.register(Companions.petType(def), context -> new CreatureRenderer<>(context, CreaturePlans.get(id), CreaturePlans.layer(id)));
		}
		for (Companions.MountDef def : Companions.mounts()) {
			String id = "mount_" + def.id();
			EntityRenderers.register(Companions.mountType(def), context -> new CreatureRenderer<>(context, CreaturePlans.get(id), CreaturePlans.layer(id)));
		}
		ClientTickEvents.END_CLIENT_TICK.register(minecraft -> {
			while (COLLECTION.consumeClick()) {
				if (minecraft.player != null && minecraft.gui.screen() == null) {
					minecraft.gui.setScreen(new CompanionScreen());
				}
			}
			while (MOUNT.consumeClick()) {
				if (minecraft.player != null) {
					send(ProgressActionPayload.MOUNT, "");
				}
			}
		});
	}

	static void send(final int action, final String value) {
		if (ClientPlayNetworking.canSend(ProgressActionPayload.TYPE)) {
			ClientPlayNetworking.send(new ProgressActionPayload(action, value));
		}
	}

	private CompanionClient() {
	}
}
