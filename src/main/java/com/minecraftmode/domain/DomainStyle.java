package com.minecraftmode.domain;

/**
 * How a domain looks to those standing in it ({@code client/domain/DomainClient}): the fog and sky it turns to, the light tint,
 * how dark the world gets and the edge drawn on the ground. Synced by ordinal: append only. Domains of a future sorcerer class
 * get their own entry (a narrow, overwhelming one, say), and their rules stay with whoever opens them.
 *
 * @param fog       fog and sky color inside
 * @param light     tint of sky and ambient light inside
 * @param darkening how far the world darkens (0..1, like a wither's)
 * @param fogEnd    how far one sees inside, in blocks
 * @param edge      color of the edge on the ground
 */
public enum DomainStyle {
	/** Sukuna's: a blood-red dusk, the world dimmed, slashes everywhere. */
	MALEVOLENT_SHRINE(0x4A0A10, 0xFF5040, 0.35F, 56.0F, 0xB01020);

	public final int fog;
	public final int light;
	public final float darkening;
	public final float fogEnd;
	public final int edge;

	DomainStyle(final int fog, final int light, final float darkening, final float fogEnd, final int edge) {
		this.fog = fog;
		this.light = light;
		this.darkening = darkening;
		this.fogEnd = fogEnd;
		this.edge = edge;
	}

	public String nameKey() {
		return "domain.minecraft_mode." + this.name().toLowerCase(java.util.Locale.ROOT);
	}

	public static DomainStyle byId(final int id) {
		DomainStyle[] all = values();
		return id >= 0 && id < all.length ? all[id] : MALEVOLENT_SHRINE;
	}
}
