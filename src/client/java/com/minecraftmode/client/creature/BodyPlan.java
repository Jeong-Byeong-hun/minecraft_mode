package com.minecraftmode.client.creature;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * A creature's body as data: parts (pivot, rotation, boxes) with an animation role and a skin. The
 * same plan builds the runtime model ({@link CreatureModel}) and is painted into its texture at
 * datagen time, so UVs always match: {@link #pack()} lays every box out on the texture deterministically.
 *
 * <p>Coordinates are vanilla model units (16 per block, y down, feet at y = 24 for scale 1).
 */
public final class BodyPlan {
	/** Overall animation style. */
	public enum Rig {
		BIPED,
		QUADRUPED,
		ARACHNID,
		FLYER,
		FLOATER,
		SERPENT,
		OCTOPUS
	}

	/** What a part does when the model animates. */
	public enum Role {
		BODY,
		HEAD,
		JAW,
		ARM_RIGHT,
		ARM_LEFT,
		LEG_RIGHT,
		LEG_LEFT,
		/** Quadruped leg; index 0 front-left, 1 front-right, 2 back-left, 3 back-right. */
		LEG_QUAD,
		/** Spider leg; index 0..3 left side front to back, 4..7 right side. */
		LEG_SPIDER,
		WING_RIGHT,
		WING_LEFT,
		/** Tail chain; index = segment from the body. */
		TAIL,
		/** Neck chain; index = segment from the body. */
		NECK,
		/** Tentacle; index = tentacle * 10 + segment. */
		TENTACLE,
		/** Spins slowly around its pivot (rings, orbiting skulls). */
		ORBIT,
		CAPE,
		EXTRA
	}

	public record Box(float x, float y, float z, int w, int h, int d, float inflate) {
		/** Width and height of this box's UV unwrap. */
		public int uvWidth() {
			return 2 * (this.w + this.d);
		}

		public int uvHeight() {
			return this.d + this.h;
		}
	}

	/** A packed box: the box and its texture origin. */
	public record PlacedBox(Part part, int boxIndex, Box box, int u, int v) {
	}

	public static final class Part {
		public final String name;
		public final @Nullable String parent;
		public final Role role;
		public final int index;
		public final float px;
		public final float py;
		public final float pz;
		public float xRot;
		public float yRot;
		public float zRot;
		public final List<Box> boxes = new ArrayList<>();
		public final String skin;
		/** Eyes painted on the front of the first box: 0 none. */
		public int eyes;
		public int eyeColor = 0xFFE040;

		Part(final String name, final @Nullable String parent, final Role role, final int index, final float px, final float py, final float pz, final String skin) {
			this.name = name;
			this.parent = parent;
			this.role = role;
			this.index = index;
			this.px = px;
			this.py = py;
			this.pz = pz;
			this.skin = skin;
		}
	}

	public final String id;
	public final Rig rig;
	public final float scale;
	public final List<Part> parts;
	public final Map<String, Skin> skins;
	private @Nullable List<PlacedBox> placed;
	private int texWidth;
	private int texHeight;

	private BodyPlan(final String id, final Rig rig, final float scale, final List<Part> parts, final Map<String, Skin> skins) {
		this.id = id;
		this.rig = rig;
		this.scale = scale;
		this.parts = List.copyOf(parts);
		this.skins = Map.copyOf(skins);
	}

	public Skin skin(final Part part) {
		Skin skin = this.skins.get(part.skin);
		if (skin == null) {
			throw new IllegalStateException(this.id + ": part " + part.name + " uses unknown skin " + part.skin);
		}
		return skin;
	}

	public int textureWidth() {
		this.pack();
		return this.texWidth;
	}

	public int textureHeight() {
		this.pack();
		return this.texHeight;
	}

	/**
	 * Shelf-packs every box's UV unwrap (tallest first) into the narrowest of 64/128/256 pixels that
	 * keeps the texture no taller than it is wide; the height is rounded up to a power of two.
	 */
	public synchronized List<PlacedBox> pack() {
		if (this.placed != null) {
			return this.placed;
		}
		List<PlacedBox> order = new ArrayList<>();
		for (Part part : this.parts) {
			for (int i = 0; i < part.boxes.size(); i++) {
				order.add(new PlacedBox(part, i, part.boxes.get(i), 0, 0));
			}
		}
		List<PlacedBox> sorted = new ArrayList<>(order);
		sorted.sort(Comparator.comparingInt((PlacedBox p) -> -p.box().uvHeight()).thenComparingInt(order::indexOf));
		for (int width : new int[] {64, 128, 256, 512}) {
			List<PlacedBox> result = new ArrayList<>();
			int x = 0;
			int y = 0;
			int rowHeight = 0;
			boolean fits = true;
			for (PlacedBox p : sorted) {
				int w = p.box().uvWidth();
				int h = p.box().uvHeight();
				if (w > width) {
					fits = false;
					break;
				}
				if (x + w > width) {
					x = 0;
					y += rowHeight;
					rowHeight = 0;
				}
				result.add(new PlacedBox(p.part(), p.boxIndex(), p.box(), x, y));
				x += w;
				rowHeight = Math.max(rowHeight, h);
			}
			int used = y + rowHeight;
			if (fits && (used <= width || width == 512)) {
				int height = 32;
				while (height < used) {
					height *= 2;
				}
				this.texWidth = width;
				this.texHeight = height;
				result.sort(Comparator.comparingInt(order::indexOf));
				this.placed = List.copyOf(result);
				return this.placed;
			}
		}
		throw new IllegalStateException(this.id + ": boxes do not fit a 512 texture");
	}

	// ------------------------------------------------------------------ builder

	public static Builder builder(final String id, final Rig rig) {
		return new Builder(id, rig);
	}

	public static Box box(final float x, final float y, final float z, final int w, final int h, final int d) {
		return new Box(x, y, z, w, h, d, 0.0F);
	}

	public static Box box(final float x, final float y, final float z, final int w, final int h, final int d, final float inflate) {
		return new Box(x, y, z, w, h, d, inflate);
	}

	public static final class Builder {
		private final String id;
		private final Rig rig;
		private float scale = 1.0F;
		private final List<Part> parts = new ArrayList<>();
		private final Map<String, Part> byName = new LinkedHashMap<>();
		private final Map<String, Skin> skins = new LinkedHashMap<>();

		Builder(final String id, final Rig rig) {
			this.id = id;
			this.rig = rig;
		}

		public Builder scale(final float scale) {
			this.scale = scale;
			return this;
		}

		public Builder skin(final String name, final Skin skin) {
			this.skins.put(name, skin);
			return this;
		}

		/** Adds a part; returns it so rotations and eyes can be set. */
		public Part part(final String name, final @Nullable String parent, final Role role, final int index, final float px, final float py, final float pz,
			final String skin, final Box... boxes) {
			if (this.byName.containsKey(name)) {
				throw new IllegalArgumentException(this.id + ": duplicate part " + name);
			}
			if (parent != null && !this.byName.containsKey(parent)) {
				throw new IllegalArgumentException(this.id + ": part " + name + " has unknown parent " + parent + " (declare parents first)");
			}
			Part part = new Part(name, parent, role, index, px, py, pz, skin);
			part.boxes.addAll(List.of(boxes));
			this.parts.add(part);
			this.byName.put(name, part);
			return part;
		}

		public Part part(final String name, final @Nullable String parent, final Role role, final float px, final float py, final float pz, final String skin,
			final Box... boxes) {
			return this.part(name, parent, role, 0, px, py, pz, skin, boxes);
		}

		public Part get(final String name) {
			return this.byName.get(name);
		}

		public BodyPlan build() {
			for (Part part : this.parts) {
				if (!this.skins.containsKey(part.skin)) {
					throw new IllegalArgumentException(this.id + ": part " + part.name + " uses unknown skin " + part.skin);
				}
			}
			return new BodyPlan(this.id, this.rig, this.scale, this.parts, this.skins);
		}
	}
}
