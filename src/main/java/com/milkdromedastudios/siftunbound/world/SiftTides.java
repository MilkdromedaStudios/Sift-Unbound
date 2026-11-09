package com.milkdromedastudios.siftunbound.world;

import net.minecraft.world.level.Level;

/**
 * The Sift's day is split into three tides (from Minecraft Dungeons II): Flow, Endure and Thrive, 8000 ticks each.
 * The sky/fog/light changes live in data/sift_unbound/timeline/sift_tides.json; this class just names the current one.
 */
public final class SiftTides {
	public enum Tide {
		FLOW, ENDURE, THRIVE;

		public String translationKey() {
			return "tide.sift_unbound." + name().toLowerCase(java.util.Locale.ROOT);
		}
	}

	public static Tide current(Level level) {
		long t = Math.floorMod(level.getOverworldClockTime(), 24000L);
		return t < 8000 ? Tide.FLOW : t < 16000 ? Tide.ENDURE : Tide.THRIVE;
	}

	private SiftTides() {
	}
}
