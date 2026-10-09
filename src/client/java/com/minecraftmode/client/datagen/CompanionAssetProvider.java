package com.minecraftmode.client.datagen;

import com.google.common.hash.Hashing;
import com.minecraftmode.MinecraftMode;
import com.minecraftmode.client.creature.BodyPlan;
import com.minecraftmode.client.creature.CreaturePlans;
import com.minecraftmode.client.creature.Skin;
import com.minecraftmode.client.datagen.art.CompanionArtist;
import com.minecraftmode.companion.Companions;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.world.item.Rarity;

/** Draws the pet charm and mount whistle icons ({@link CompanionArtist}) in the colors of each creature's body plan. */
public class CompanionAssetProvider implements DataProvider {
	private final PackOutput output;

	public CompanionAssetProvider(final FabricPackOutput output) {
		this.output = output;
	}

	@Override
	public CompletableFuture<?> run(final CachedOutput cache) {
		Path textures = this.output.getOutputFolder(PackOutput.Target.RESOURCE_PACK).resolve(MinecraftMode.MOD_ID).resolve("textures/item");
		List<CompletableFuture<?>> writes = new ArrayList<>();
		for (Companions.PetDef def : Companions.pets()) {
			int[] colors = colors("pet_" + def.id());
			writes.add(write(cache, textures.resolve("pet_" + def.id() + ".png"), CompanionArtist.charm(colors[0], colors[1], rim(def.rarity()))));
		}
		for (Companions.MountDef def : Companions.mounts()) {
			int[] colors = colors("mount_" + def.id());
			writes.add(write(cache, textures.resolve("mount_" + def.id() + ".png"), CompanionArtist.whistle(colors[0], colors[1], rim(def.rarity()))));
		}
		return CompletableFuture.allOf(writes.toArray(CompletableFuture[]::new));
	}

	/** The base colors of the plan's first two skins. */
	private static int[] colors(final String planId) {
		BodyPlan plan = CreaturePlans.get(planId);
		if (plan == null) {
			throw new IllegalStateException("No body plan " + planId);
		}
		Iterator<Skin> skins = plan.skins.values().iterator();
		Skin first = skins.next();
		Skin second = skins.hasNext() ? skins.next() : first;
		return new int[] {first.base(), second.base() == first.base() ? first.accent() : second.base()};
	}

	private static int rim(final Rarity rarity) {
		return switch (rarity) {
			case EPIC -> 0xC060FF;
			case RARE -> 0x5AB0E8;
			default -> 0xE8C24A;
		};
	}

	private static CompletableFuture<?> write(final CachedOutput cache, final Path path, final BufferedImage image) {
		byte[] bytes = ArmorAssetProvider.png(image);
		return CompletableFuture.runAsync(() -> {
			try {
				cache.writeIfNeeded(path, bytes, Hashing.sha256().hashBytes(bytes));
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		});
	}

	@Override
	public String getName() {
		return "Companion icons";
	}
}
