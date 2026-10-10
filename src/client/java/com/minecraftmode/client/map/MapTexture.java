package com.minecraftmode.client.map;

import com.minecraftmode.MinecraftMode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.DynamicTexture;
import org.jspecify.annotations.Nullable;
import net.minecraft.resources.Identifier;

/**
 * A square window of the explored map as a GPU texture: {@code size} blocks across, one pixel per block, slid to keep a point
 * of interest near its middle. Chunks are painted as they are scanned; the upload is throttled.
 */
final class MapTexture {
	final int size;
	/** Ticks between uploads: a 2048 px window is 16 MB, so the big map refreshes less often than the minimap. */
	private final int interval;
	private final Identifier id;
	/** Created on the first upload: a GPU texture cannot exist before the render system is up (datagen has none). */
	private @Nullable DynamicTexture texture;
	private @Nullable NativeImage image;
	/** Block coordinates of the texture's top-left pixel. */
	private int originX;
	private int originZ;
	private boolean dirty;
	private long lastUpload = -100L;
	private boolean registered;

	MapTexture(final String name, final int size) {
		this.size = size;
		this.interval = size > 1024 ? 40 : 10;
		this.id = MinecraftMode.id("map/" + name);
		this.originX = Integer.MIN_VALUE;
	}

	int originX() {
		return this.originX;
	}

	int originZ() {
		return this.originZ;
	}

	/** Slides the window so {@code x, z} is near its centre, repainting everything from {@code data} when it moved. */
	void center(final MapData data, final int x, final int z) {
		int half = this.size / 2;
		if (this.originX != Integer.MIN_VALUE && Math.abs(x - (this.originX + half)) < half / 2 && Math.abs(z - (this.originZ + half)) < half / 2) {
			return;
		}
		this.originX = (x - half) & ~15;
		this.originZ = (z - half) & ~15;
		this.pixels().fillRect(0, 0, this.size, this.size, 0);
		for (int cz = this.originZ >> 4; cz < this.originZ + this.size >> 4; cz++) {
			for (int cx = this.originX >> 4; cx < this.originX + this.size >> 4; cx++) {
				int[] colors = data.chunk(cx, cz);
				if (colors != null) {
					this.paint(cx, cz, colors);
				}
			}
		}
		this.dirty = true;
	}

	void paint(final int cx, final int cz, final int[] colors) {
		int px = (cx << 4) - this.originX;
		int pz = (cz << 4) - this.originZ;
		if (this.originX == Integer.MIN_VALUE || px < 0 || pz < 0 || px + 16 > this.size || pz + 16 > this.size) {
			return;
		}
		for (int z = 0; z < 16; z++) {
			for (int x = 0; x < 16; x++) {
				this.pixels().setPixel(px + x, pz + z, colors[z << 4 | x]);
			}
		}
		this.dirty = true;
	}

	private NativeImage pixels() {
		if (this.image == null) {
			this.image = new NativeImage(this.size, this.size, true);
		}
		return this.image;
	}

	/** Uploads pending pixels (throttled). */
	void upload(final long now) {
		if (this.texture == null) {
			this.texture = new DynamicTexture(() -> "minecraft_mode map " + this.id.getPath(), this.pixels());
			Minecraft.getInstance().getTextureManager().register(this.id, this.texture);
			this.registered = true;
			this.dirty = false;
			this.lastUpload = now;
		}
		if (this.dirty && now - this.lastUpload >= this.interval) {
			this.texture.upload();
			this.dirty = false;
			this.lastUpload = now;
		}
	}

	/**
	 * Draws the blocks around {@code centerX, centerZ} into the screen rectangle {@code x, y, w, h} at {@code zoom} pixels per
	 * block. Callers clip with a scissor; parts outside the window stay unexplored-dark.
	 */
	void draw(final GuiGraphicsExtractor g, final int x, final int y, final int w, final int h, final double centerX, final double centerZ, final float zoom) {
		if (!this.registered) {
			return;
		}
		// the part of the view that lies inside the window (the sampler repeats, so never read past the edges)
		double bx0 = centerX - w / (2.0 * zoom);
		double bz0 = centerZ - h / (2.0 * zoom);
		double cx0 = Math.max(bx0, this.originX);
		double cz0 = Math.max(bz0, this.originZ);
		double cx1 = Math.min(bx0 + w / zoom, this.originX + this.size);
		double cz1 = Math.min(bz0 + h / zoom, this.originZ + this.size);
		if (cx0 >= cx1 || cz0 >= cz1) {
			return;
		}
		int sx0 = x + (int)Math.round((cx0 - bx0) * zoom);
		int sy0 = y + (int)Math.round((cz0 - bz0) * zoom);
		int sx1 = x + (int)Math.round((cx1 - bx0) * zoom);
		int sy1 = y + (int)Math.round((cz1 - bz0) * zoom);
		g.blit(this.id, sx0, sy0, sx1, sy1, (float)((cx0 - this.originX) / this.size), (float)((cx1 - this.originX) / this.size),
			(float)((cz0 - this.originZ) / this.size), (float)((cz1 - this.originZ) / this.size));
	}

	void clear() {
		this.originX = Integer.MIN_VALUE;
		this.pixels().fillRect(0, 0, this.size, this.size, 0);
		this.dirty = true;
	}
}
