package com.minecraftmode.test;

/** Whether the client follows a server tick rate above 20 ({@code MinecraftTickRateMixin}); only the damage measurement turns it on. */
public final class TickRateCap {
	public static volatile boolean lifted;

	private TickRateCap() {
	}
}
