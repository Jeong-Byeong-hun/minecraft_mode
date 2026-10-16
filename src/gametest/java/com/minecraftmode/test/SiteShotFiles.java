package com.minecraftmode.test;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.minecraftmode.MinecraftMode;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

/**
 * The files {@link SiteShotsClientGameTest} leaves for the website: {@code site-shots/<section>/<name>.jpg} and
 * {@code site-shots/site-shots.json}, the list of sections in the folder (name to when it was taken, epoch ms) and of shots (file,
 * wiki page, the heading or table row to put it under, caption). A run replaces only its own sections and keeps the others.
 */
final class SiteShotFiles {
	private static final float JPEG_QUALITY = 0.88F;
	private static final String MANIFEST = "site-shots.json";

	/** One picture and where on the site it goes ({@code page} null: copied, not placed). */
	record Shot(String file, String page, String after, String caption) {
	}

	private SiteShotFiles() {
	}

	/** Turns the screenshot {@code png} into {@code <out>/<section>/<name>.jpg} and returns that file relative to {@code out}. */
	static String saveJpeg(final Path png, final Path out, final String section, final String name) {
		String file = section + "/" + name + ".jpg";
		try {
			BufferedImage image = ImageIO.read(png.toFile());
			// JPEG has no alpha: draw onto an opaque RGB image first
			BufferedImage rgb = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
			rgb.createGraphics().drawImage(image, 0, 0, null);
			ImageWriter writer = ImageIO.getImageWritersByFormatName("jpg").next();
			ImageWriteParam param = writer.getDefaultWriteParam();
			param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
			param.setCompressionQuality(JPEG_QUALITY);
			try (ImageOutputStream stream = ImageIO.createImageOutputStream(out.resolve(file).toFile())) {
				writer.setOutput(stream);
				writer.write(null, new IIOImage(rgb, null, null), param);
			} finally {
				writer.dispose();
			}
			Files.delete(png);
			MinecraftMode.LOGGER.info("[site] {} {}x{}", file, image.getWidth(), image.getHeight());
		} catch (IOException e) {
			throw new UncheckedIOException("could not convert " + png, e);
		}
		return file;
	}

	/** Empties (or makes) {@code dir}. */
	static void resetDir(final Path dir) {
		try {
			if (Files.exists(dir)) {
				try (Stream<Path> files = Files.walk(dir)) {
					for (Path p : files.sorted(Comparator.reverseOrder()).toList()) {
						Files.delete(p);
					}
				}
			}
			Files.createDirectories(dir);
		} catch (IOException e) {
			throw new UncheckedIOException("could not reset " + dir, e);
		}
	}

	/**
	 * The list a run folder already has. Empty when there is none, or it cannot be read, or it is from before sections were timed;
	 * so a run folder that once held other sections keeps them.
	 */
	static JsonObject previous(final Path out) {
		Path file = out.resolve(MANIFEST);
		try {
			JsonObject manifest = Files.exists(file) ? JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject() : null;
			if (manifest != null && manifest.get("sections") instanceof JsonObject && manifest.get("shots") instanceof JsonArray) {
				return manifest;
			}
		} catch (IOException | RuntimeException e) {
			MinecraftMode.LOGGER.warn("[site] ignoring the old screenshot list {}: {}", file, e.toString());
		}
		JsonObject empty = new JsonObject();
		empty.add("sections", new JsonObject());
		empty.add("shots", new JsonArray());
		return empty;
	}

	/** Writes the previous list without the sections this run took, plus the ones it finished (stamped now). */
	static void write(final Path out, final JsonObject previous, final List<String> picked, final List<String> done, final List<Shot> shots) {
		JsonObject sections = new JsonObject();
		previous.getAsJsonObject("sections").entrySet().stream().filter(e -> !picked.contains(e.getKey()))
			.forEach(e -> sections.add(e.getKey(), e.getValue()));
		long now = System.currentTimeMillis();
		done.forEach(section -> sections.addProperty(section, now));
		JsonArray list = new JsonArray();
		for (JsonElement old : previous.getAsJsonArray("shots")) {
			String section = sectionOf(old);
			// an entry this code cannot place in a section is dropped, never allowed to stop the run from writing its list
			if (section != null && !picked.contains(section) && sections.has(section)) {
				list.add(old);
			}
		}
		for (Shot shot : shots) {
			JsonObject o = new JsonObject();
			o.addProperty("file", shot.file());
			o.addProperty("page", shot.page());
			o.addProperty("after", shot.after());
			o.addProperty("caption", shot.caption());
			list.add(o);
		}
		JsonObject manifest = new JsonObject();
		manifest.add("sections", sections);
		manifest.add("shots", list);
		try {
			Files.writeString(out.resolve(MANIFEST), new GsonBuilder().setPrettyPrinting().serializeNulls().create().toJson(manifest), StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new UncheckedIOException("could not write the screenshot list", e);
		}
	}

	/** The section of a listed shot ({@code "<section>/<name>.jpg"}), or null when the entry is not one. */
	private static String sectionOf(final JsonElement entry) {
		if (!(entry instanceof JsonObject o) || !(o.get("file") instanceof JsonElement f) || !f.isJsonPrimitive()) {
			return null;
		}
		String file = f.getAsString();
		int slash = file.indexOf('/');
		return slash > 0 ? file.substring(0, slash) : null;
	}
}
