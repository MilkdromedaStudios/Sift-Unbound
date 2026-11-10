package com.milkdromedastudios.siftunbound.test;

import com.milkdromedastudios.siftunbound.SiftUnbound;
import com.milkdromedastudios.siftunbound.portal.SiftTravel;
import com.milkdromedastudios.siftunbound.portal.WardenPortal;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.StructureTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * "Beauty shots" for the README and the release page, run by tools/photoshoot.sh with Iris + a shader pack installed
 * (only when -Dsiftunbound.photoshoot=true; without Iris it shoots with the vanilla renderer). Not part of CI: it needs
 * third-party mods and is slow on a software GPU.
 */
final class SiftPhotoshoot {
	private static final int W = 1920;
	private static final int H = 1080;
	/** 1024 px at 90 degrees after cropping a 100-degree shot: 1024 * tan(50) / tan(45). */
	private static final int PANORAMA_CAPTURE = 1220;

	private record Frame(BlockPos inside, Direction.Axis axis, int minX, int maxX, int minY, int maxY, int minZ, int maxZ) {
	}

	static void run(ClientGameTestContext context) {
		context.runOnClient(mc -> {
			mc.options.renderDistance().set(8);
			mc.options.fov().set(70);
			if (!mc.gui.hud.isHidden()) {
				mc.gui.hud.toggle();
			}
		});
		// A normal (not the test framework's flat "consistent") world; seed 2027 has an ancient city about 2 400 blocks
		// from spawn (see tools/server_smoke_test.sh).
		try (TestSingleplayerContext sp = context.worldBuilder().setUseConsistentSettings(false)
			.adjustSettings(s -> s.setSeed("2027")).create()) {
			settle(context, sp, 100);
			for (String cmd : List.of("gamerule spawn_mobs true", "gamerule advance_time false", "gamerule advance_weather false",
				"difficulty peaceful", "time set 1000", "gamemode creative @p")) {
				sp.getServer().runCommand(cmd);
			}
			fly(sp);

			// ------------------------------------------------------------ 1. the Warden Portal at the heart of an ancient city
			Frame frame = sp.getServer().computeOnServer(server -> findAncientCityFrame(server.overworld()));
			if (frame == null) {
				throw new AssertionError("could not find an ancient city portal frame");
			}
			log("ancient city frame: " + frame);
			sp.getServer().runCommand("effect give @p minecraft:night_vision infinite 0 true");
			boolean alongZ = frame.axis() == Direction.Axis.Z;
			double cx = (frame.minX() + frame.maxX() + 1) / 2.0;
			double cz = (frame.minZ() + frame.maxZ() + 1) / 2.0;
			double eye = frame.minY() + 2.5;
			double back = 15;
			int side = sp.getServer().computeOnServer(server -> openSide(server.overworld(), frame));
			// camera in front of the frame, looking straight at it
			double camX = alongZ ? cx + side * back : cx;
			double camZ = alongZ ? cz : cz + side * back;
			float yaw = alongZ ? (side > 0 ? 90 : -90) : (side > 0 ? 180 : 0);
			shot(context, sp, "minecraft:overworld", camX, eye, camZ, yaw, -4, "01_warden_portal_dormant");
			sp.getServer().runOnServer(server -> WardenPortal.tryLight(server.overworld(), frame.inside()));
			shot(context, sp, "minecraft:overworld", camX, eye, camZ, yaw, -4, "02_warden_portal_awakened");
			double nearX = alongZ ? cx + side * 6 : cx - 4;
			double nearZ = alongZ ? cz - 4 : cz + side * 6;
			shot(context, sp, "minecraft:overworld", nearX, frame.minY() + 1.2, nearZ, yaw + (alongZ ? -side * 25 : side * 25), -12,
				"03_warden_portal_close");

			// ------------------------------------------------------------ 2. step through
			sp.getServer().runCommand("effect clear @p minecraft:night_vision");
			sp.getServer().runCommand(String.format(Locale.ROOT, "tp @p %.2f %.2f %.2f", frame.inside().getX() + 0.5, frame.minY() + 1.0,
				frame.inside().getZ() + 0.5));
			sp.getServer().waitFor(server -> server.getPlayerList().getPlayers().get(0).level().dimension() == SiftTravel.THE_SIFT, 2400);
			fly(sp);
			BlockPos arrival = sp.getServer().computeOnServer(server -> {
				var p = server.getPlayerList().getPlayers().get(0);
				return SiftTravel.findPortal((ServerLevel) p.level(), p.blockPosition()).orElse(p.blockPosition());
			});
			log("arrived next to the return portal at " + arrival.toShortString());
			shot(context, sp, "sift_unbound:the_sift", arrival.getX() + 9.5, arrival.getY() + 4, arrival.getZ() - 7.5, 50, 12, "04_arrival_in_the_sift");

			// ------------------------------------------------------------ 3. vistas
			record Vista(String biome, String time, double up, float yaw, float pitch, String name) {
			}
			List<Vista> vistas = new ArrayList<>(List.of(
				new Vista("pallid_grove", "1000", 18, 20, 18, "05_pallid_grove_flow_tide"),
				new Vista("pallid_grove", "20000", 18, 20, 18, "06_pallid_grove_thrive_tide"),
				new Vista("pallid_grove", "12000", 18, 20, 18, "07_pallid_grove_endure_tide"),
				new Vista("sift_meadows", "1000", 22, 120, 16, "08_sift_meadows"),
				new Vista("soul_wetlands", "20000", 16, 300, 22, "09_soul_wetlands_thrive_tide"),
				new Vista("husklands", "1000", 26, 35, 30, "10_husklands"),
				new Vista("lullaby_hills", "20000", 24, 200, 18, "11_lullaby_hills_thrive_tide")));
			for (Vista v : vistas) {
				BlockPos spot = sp.getServer().computeOnServer(server -> SiftClientGameTest.landIn(server.getLevel(SiftTravel.THE_SIFT), v.biome()));
				sp.getServer().runCommand("time set " + v.time());
				shot(context, sp, "sift_unbound:the_sift", spot.getX() + 0.5, spot.getY() + v.up(), spot.getZ() + 0.5, v.yaw(), v.pitch(), v.name());
			}

			// ------------------------------------------------------------ 4. creatures at home, by a soul water pool
			BlockPos pool = sp.getServer().computeOnServer(server -> SiftClientGameTest.landIn(server.getLevel(SiftTravel.THE_SIFT), "soul_wetlands"));
			sp.getServer().runCommand("time set 1000");
			String in = "execute in sift_unbound:the_sift run ";
			int px = pool.getX(), py = pool.getY(), pz = pool.getZ();
			sp.getServer().runCommand(in + "fill " + (px + 2) + " " + (py - 1) + " " + (pz + 3) + " " + (px + 5) + " " + (py - 1) + " " + (pz + 6) + " sift_unbound:soul_water");
			sp.getServer().runCommand(in + "summon sift_unbound:blub " + px + " " + py + " " + (pz + 2) + " {NoAI:1b,Rotation:[200f,0f],Variant:\"soul\"}");
			sp.getServer().runCommand(in + "summon sift_unbound:blub " + (px + 1.3) + " " + py + " " + (pz + 2.6) + " {NoAI:1b,Rotation:[150f,0f],Variant:\"meadows\"}");
			sp.getServer().runCommand(in + "summon sift_unbound:blub " + (px - 1.4) + " " + py + " " + (pz + 2.8) + " {NoAI:1b,Rotation:[230f,0f],Variant:\"carapace\"}");
			sp.getServer().runCommand(in + "summon sift_unbound:blub " + (px + 3.5) + " " + (py - 0.6) + " " + (pz + 4.5) + " {NoAI:1b,Rotation:[180f,0f],Variant:\"soul\",Age:-24000}");
			sp.getServer().runCommand(in + "summon sift_unbound:soul_wisp " + (px - 1) + " " + (py + 2.2) + " " + (pz + 4) + " {NoAI:1b}");
			sp.getServer().runCommand(in + "summon sift_unbound:soul_wisp " + (px + 2) + " " + (py + 3.0) + " " + (pz + 6) + " {NoAI:1b}");
			sp.getServer().runCommand(in + "setblock " + (px - 2) + " " + py + " " + (pz + 4) + " sift_unbound:lullaby_bloom");
			sp.getServer().runCommand(in + "setblock " + (px - 3) + " " + py + " " + (pz + 3) + " sift_unbound:teal_sprouts");
			sp.getConnection().waitForClientboundEntityUpdates(net.minecraft.world.entity.EntityTypes.PLAYER);
			shot(context, sp, "sift_unbound:the_sift", px + 0.5, py + 1.1, pz - 2.0, 0, 14, "12_blubs_by_the_soul_water");
			sp.getServer().runCommand(in + "summon sift_unbound:sculk_prowler " + (px - 6) + " " + py + " " + (pz + 1) + " {NoAI:1b,Rotation:[260f,0f],PersistenceRequired:1b}");
			sp.getServer().runCommand("difficulty easy");
			sp.getServer().runCommand("time set 12000");
			shot(context, sp, "sift_unbound:the_sift", px - 1.5, py + 1.3, pz - 3.0, -40, 10, "13_sculk_prowler_endure_tide");
			sp.getServer().runCommand("difficulty peaceful");

			// ------------------------------------------------------------ 5. title-screen panoramas (tools/make_panorama.py)
			for (String biome : List.of("pallid_grove", "lullaby_hills")) {
				BlockPos spot = sp.getServer().computeOnServer(server -> SiftClientGameTest.landIn(server.getLevel(SiftTravel.THE_SIFT), biome));
				sp.getServer().runCommand("time set 20000");
				panorama(context, sp, spot.getX() + 0.5, spot.getY() + 9, spot.getZ() + 0.5, 30, biome);
			}
		}
	}

	/**
	 * The six faces of a title-screen cube map, in the order and orientation Minecraft's own panorama capture uses
	 * (front, right, back, left, up, down). Each face is shot at 100 degrees and cropped to 90 by tools/make_panorama.py, so
	 * screen-edge effects of the shader pack never reach a seam.
	 */
	private static void panorama(ClientGameTestContext context, TestSingleplayerContext sp, double x, double y, double z, float yaw, String name) {
		context.runOnClient(mc -> {
			mc.options.fov().set(100);
			mc.options.fovEffectScale().set(0.0);
		});
		float[][] faces = {{yaw, 0}, {yaw + 90, 0}, {yaw + 180, 0}, {yaw - 90, 0}, {yaw, -90}, {yaw, 90}};
		for (int i = 0; i < faces.length; i++) {
			if (alreadyTaken("panorama_" + name + "_" + i)) {
				continue;
			}
			sp.getServer().runCommand(String.format(Locale.ROOT, "execute in sift_unbound:the_sift run tp @p %.2f %.2f %.2f %.1f %.1f",
				x, y, z, faces[i][0], faces[i][1]));
			settle(context, sp, i == 0 ? 240 : 160);
			capture(context, "panorama_" + name + "_" + i, PANORAMA_CAPTURE, PANORAMA_CAPTURE);
			log("panorama " + name + " face " + i);
		}
		context.runOnClient(mc -> mc.options.fov().set(70));
	}

	private static void fly(TestSingleplayerContext sp) {
		sp.getServer().runOnServer(server -> {
			var player = server.getPlayerList().getPlayers().get(0);
			player.getAbilities().flying = true;
			player.onUpdateAbilities();
		});
	}

	/**
	 * With Iris + Sodium, Sodium never reports "all sections rendered" (the shadow pass keeps scheduling graph updates), so
	 * the test API's chunk wait would always run into its timeout. With shaders, only try it briefly, then wait until the
	 * client's loaded-chunk count has stopped changing.
	 */
	private static final boolean SHADERS = FabricLoader.getInstance().isModLoaded("iris");

	/** Lets chunks (and, with shaders, the pipeline) settle. */
	private static void settle(ClientGameTestContext context, TestSingleplayerContext sp, int ticks) {
		try {
			sp.getConnection().waitForChunksRender(true, SHADERS ? 100 : 2400);
		} catch (AssertionError | RuntimeException e) { // the test API reports a timeout as an AssertionError
			if (SHADERS) {
				waitForLoadedChunks(context);
			} else {
				log("chunk wait gave up (" + e.getMessage() + "), waiting a fixed time instead");
			}
		}
		context.waitTicks(ticks);
	}

	private static void waitForLoadedChunks(ClientGameTestContext context) {
		int[] last = {-1};
		int[] steady = {0};
		try {
			int waited = context.waitFor(mc -> {
				int loaded = mc.level == null ? 0 : mc.level.getChunkSource().getLoadedChunksCount();
				steady[0] = loaded == last[0] ? steady[0] + 1 : 0;
				last[0] = loaded;
				return loaded > 100 && steady[0] >= 60;
			}, 2400);
			log("chunks loaded (" + last[0] + ") after " + waited + " ticks");
		} catch (AssertionError | RuntimeException e) {
			log("loaded-chunk wait gave up at " + last[0] + " chunks");
		}
	}

	/**
	 * -Pphotoshoot=resume: keep the shots an interrupted shoot already took and only take the missing ones. The test
	 * framework empties screenshots/ when the game starts, so every finished shot is also copied to photoshoot_kept/.
	 */
	private static final boolean RESUME = Boolean.getBoolean("siftunbound.photoshoot.resume");
	private static final Path SCREENSHOTS = FabricLoader.getInstance().getGameDir().resolve("screenshots");
	private static final Path KEPT = FabricLoader.getInstance().getGameDir().resolve("photoshoot_kept");

	private static boolean alreadyTaken(String name) {
		Path kept = KEPT.resolve(name + ".png");
		if (!RESUME || !Files.exists(kept)) {
			return false;
		}
		try {
			Files.createDirectories(SCREENSHOTS);
			Files.copy(kept, SCREENSHOTS.resolve(name + ".png"), StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		log("keeping " + name + " from the earlier run");
		return true;
	}

	private static void capture(ClientGameTestContext context, String name, int width, int height) {
		context.takeScreenshot(TestScreenshotOptions.of(name).withSize(width, height).disableCounterPrefix());
		try {
			Files.createDirectories(KEPT);
			Files.copy(SCREENSHOTS.resolve(name + ".png"), KEPT.resolve(name + ".png"), StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private static void shot(ClientGameTestContext context, TestSingleplayerContext sp, String dim, double x, double y, double z,
		float yaw, float pitch, String name) {
		if (alreadyTaken(name)) {
			return;
		}
		sp.getServer().runCommand(String.format(Locale.ROOT, "execute in %s run tp @p %.2f %.2f %.2f %.1f %.1f", dim, x, y, z, yaw, pitch));
		settle(context, sp, SHADERS ? 240 : 120); // shader packs also need a few seconds of temporal accumulation
		capture(context, name, W, H);
		log("shot " + name);
	}

	private static void log(String s) {
		System.out.println("[SiftPhotoshoot] " + s);
		SiftUnbound.LOGGER.info("[photoshoot] {}", s);
	}

	/** Finds the reinforced-deepslate frame of the nearest ancient city (it is the only reinforced deepslate there). */
	private static Frame findAncientCityFrame(ServerLevel level) {
		BlockPos city = level.findNearestMapStructure(StructureTags.ON_ANCIENT_CITY_MAPS, BlockPos.ZERO, 250, false);
		if (city == null) {
			return null;
		}
		log("nearest ancient city: " + city.toShortString());
		int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
		int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
		int count = 0;
		BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
		for (int cx = (city.getX() >> 4) - 4; cx <= (city.getX() >> 4) + 4; cx++) {
			for (int cz = (city.getZ() >> 4) - 4; cz <= (city.getZ() >> 4) + 4; cz++) {
				var chunk = level.getChunk(cx, cz);
				for (int x = 0; x < 16; x++) {
					for (int z = 0; z < 16; z++) {
						for (int y = level.getMinY(); y < 0; y++) {
							p.set((cx << 4) + x, y, (cz << 4) + z);
							if (chunk.getBlockState(p).is(Blocks.REINFORCED_DEEPSLATE)) {
								count++;
								minX = Math.min(minX, p.getX()); maxX = Math.max(maxX, p.getX());
								minY = Math.min(minY, p.getY()); maxY = Math.max(maxY, p.getY());
								minZ = Math.min(minZ, p.getZ()); maxZ = Math.max(maxZ, p.getZ());
							}
						}
					}
				}
			}
		}
		if (count == 0) {
			return null;
		}
		Direction.Axis axis = (maxX - minX) < (maxZ - minZ) ? Direction.Axis.Z : Direction.Axis.X;
		BlockPos inside = new BlockPos((minX + maxX) / 2, (minY + maxY) / 2, (minZ + maxZ) / 2);
		return new Frame(inside, axis, minX, maxX, minY, maxY, minZ, maxZ);
	}

	/** +1 or -1: which side of the frame has more open space for the camera. */
	private static int openSide(ServerLevel level, Frame f) {
		int best = 1;
		int bestAir = -1;
		for (int s : new int[]{1, -1}) {
			int air = 0;
			for (int d = 3; d <= 15; d++) {
				for (int up = 1; up <= 4; up++) {
					BlockPos q = f.axis() == Direction.Axis.Z
						? new BlockPos(f.minX() + s * d, f.minY() + up, (f.minZ() + f.maxZ()) / 2)
						: new BlockPos((f.minX() + f.maxX()) / 2, f.minY() + up, f.minZ() + s * d);
					if (level.getBlockState(q).isAir()) {
						air++;
					}
				}
			}
			if (air > bestAir) {
				bestAir = air;
				best = s;
			}
		}
		return best;
	}

	private SiftPhotoshoot() {
	}
}
