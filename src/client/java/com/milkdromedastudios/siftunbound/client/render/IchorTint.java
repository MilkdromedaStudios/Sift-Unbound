package com.milkdromedastudios.siftunbound.client.render;

import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Ichor is an oil film, not a tiled texture: every block of it is tinted by where it is in the world, drifting through
 * teal, blue, violet, pink, gold and green in broad, irregular slicks, so no two patches of a pond look the same.
 */
public final class IchorTint implements BlockTintSource {
	private static final int[] FILM = {0x35D6E6, 0x4E97F2, 0xB56BEA, 0xFF8FD0, 0xFFE46B, 0x58E39A, 0x35D6E6};

	@Override
	public int color(BlockState state) {
		return 0xFF000000 | FILM[0];
	}

	@Override
	public int colorInWorld(BlockState state, BlockAndTintGetter level, BlockPos pos) {
		double x = pos.getX(), z = pos.getZ();
		// wide slicks, warped by a second layer so their edges wander, plus fine variation from block to block
		double warp = noise(x / 23.0 + 40.0, z / 23.0 - 17.0);
		double film = noise(x / 11.0 + warp * 2.2, z / 11.0 - warp * 2.2) * 1.6 + noise(x / 3.5, z / 3.5) * 0.35 + pos.getY() * 0.03;
		double p = Mth.frac(film) * (FILM.length - 1);
		int i = (int) p;
		float t = (float) (p - i);
		int a = FILM[i], b = FILM[i + 1];
		int r = Mth.lerpInt(t, a >> 16 & 255, b >> 16 & 255);
		int g = Mth.lerpInt(t, a >> 8 & 255, b >> 8 & 255);
		int bl = Mth.lerpInt(t, a & 255, b & 255);
		return 0xFF000000 | r << 16 | g << 8 | bl;
	}

	/** Smooth value noise in 0..1. */
	private static double noise(double x, double z) {
		int x0 = Mth.floor(x), z0 = Mth.floor(z);
		double fx = x - x0, fz = z - z0;
		fx = fx * fx * (3 - 2 * fx);
		fz = fz * fz * (3 - 2 * fz);
		double top = Mth.lerp(fx, hash(x0, z0), hash(x0 + 1, z0));
		double bottom = Mth.lerp(fx, hash(x0, z0 + 1), hash(x0 + 1, z0 + 1));
		return Mth.lerp(fz, top, bottom);
	}

	private static double hash(int x, int z) {
		int h = x * 374761393 + z * 668265263;
		h = (h ^ h >>> 13) * 1274126177;
		return ((h ^ h >>> 16) & 0xFFFF) / 65535.0;
	}
}
