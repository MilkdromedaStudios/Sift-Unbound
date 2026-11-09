package com.milkdromedastudios.siftunbound.test;

import com.milkdromedastudios.siftunbound.portal.SiftTravel;
import com.milkdromedastudios.siftunbound.portal.WardenPortal;
import com.milkdromedastudios.siftunbound.registry.ModEntities;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Renders the mod in a real client (headless via xvfb in CI) and saves screenshots of the Sift for the wiki:
 * the landscape at each tide, the mobs, the portal and soul water.
 */
public class SiftClientGameTest implements FabricClientGameTest {
	private static final java.util.UUID PIG = new java.util.UUID(0x0000000100000002L, 0x0000000300000004L);

	@Override
	public void runTest(ClientGameTestContext context) {
		if (Boolean.getBoolean("siftunbound.photoshoot")) {
			SiftPhotoshoot.run(context);
			return;
		}
		try (TestSingleplayerContext sp = context.worldBuilder().create()) {
			sp.getConnection().waitForChunksRender();
			sp.getServer().runCommand("gamerule spawn_mobs true"); // test worlds turn it off; world-gen spawns need it
			sp.getServer().runCommand("gamerule advance_time false");
			sp.getServer().runCommand("gamerule advance_weather false");
			sp.getServer().runCommand("difficulty peaceful");
			sp.getServer().runCommand("time set 1000");

			// --- travel: a pig and then the player walk through a Warden Portal in the Overworld and arrive in the Sift
			sp.getServer().runCommand("fill 10 100 10 13 104 10 minecraft:reinforced_deepslate");
			sp.getServer().runCommand("fill 11 101 10 12 103 10 air");
			sp.getServer().runCommand("fill 9 99 8 14 99 12 minecraft:stone");
			sp.getServer().runOnServer(server -> {
				ServerLevel overworld = server.overworld();
				if (WardenPortal.tryLight(overworld, new BlockPos(11, 101, 10)) != WardenPortal.Result.LIT) {
					throw new AssertionError("could not light a test portal");
				}
			});
			sp.getServer().runCommand("summon minecraft:pig 11.5 101 10.5 {UUID:[I;1,2,3,4],NoAI:1b}");
			sp.getServer().waitFor(server -> server.getLevel(SiftTravel.THE_SIFT).getEntity(PIG) != null, 600);
			sp.getServer().runOnServer(server -> {
				ServerLevel sift = server.getLevel(SiftTravel.THE_SIFT);
				var pig = sift.getEntity(PIG);
				var portal = SiftTravel.findPortal(sift, pig.blockPosition());
				if (portal.isEmpty()) {
					throw new AssertionError("no return portal was built in the Sift");
				}
				int surface = sift.getChunk(portal.get().getX() >> 4, portal.get().getZ() >> 4)
					.getHeight(Heightmap.Types.WORLD_SURFACE, portal.get().getX() & 15, portal.get().getZ() & 15);
				if (portal.get().getY() < surface - 4 || portal.get().getY() < 40) {
					throw new AssertionError("return portal at " + portal.get() + " is buried (surface " + surface + ")");
				}
				System.out.println("[SiftClientGameTest] return portal at " + portal.get().toShortString() + ", surface " + surface);
			});
			sp.getServer().runCommand("gamemode creative @p");
			sp.getServer().runCommand("tp @p 12.0 101 10.5");
			sp.getServer().waitFor(server -> server.getPlayerList().getPlayers().get(0).level().dimension() == SiftTravel.THE_SIFT, 600);
			sp.getConnection().waitForChunksRender();
			context.waitTicks(40);
			context.takeScreenshot(TestScreenshotOptions.of("sift_arrival").disableCounterPrefix());

			// From here on the camera flies (creative) so shots are never taken from the bottom of a soul water sea.
			sp.getServer().runOnServer(server -> {
				var player = server.getPlayerList().getPlayers().get(0);
				player.getAbilities().flying = true;
				player.onUpdateAbilities();
			});

			// --- one establishing shot per biome, from dry land
			for (String biome : new String[]{"sift_meadows", "pallid_grove", "soul_wetlands", "husklands", "lullaby_hills"}) {
				BlockPos spot = sp.getServer().computeOnServer(server -> landIn(server.getLevel(SiftTravel.THE_SIFT), biome));
				if (spot == null) {
					throw new AssertionError("biome sift_unbound:" + biome + " was not found within 4000 blocks");
				}
				// the husklands are cut by deep ravines, so look further down there
				float pitch = biome.equals("husklands") ? 55 : 32;
				shot(context, sp, spot.getX() + 0.5, spot.getY() + 12, spot.getZ() + 0.5, 35, pitch, "biome_" + biome);
			}

			// --- creatures spawned by world generation along the way (logged, so CI shows them)
			String census = sp.getServer().computeOnServer(server -> {
				var counts = new java.util.TreeMap<String, Integer>();
				for (var e : server.getLevel(SiftTravel.THE_SIFT).getAllEntities()) {
					counts.merge(e.getType().builtInRegistryHolder().key().identifier().toString(), 1, Integer::sum);
				}
				return counts.toString();
			});
			System.out.println("[SiftClientGameTest] entities loaded in the Sift: " + census);
			if (!census.contains("sift_unbound:blub")) {
				throw new AssertionError("no blubs were spawned by world generation in the Sift: " + census);
			}
			if (census.contains("sift_unbound:sculk_prowler")) {
				throw new AssertionError("Sculk Prowlers must not spawn on Peaceful: " + census);
			}

			// --- the same meadow view at each tide
			BlockPos meadow = sp.getServer().computeOnServer(server -> landIn(server.getLevel(SiftTravel.THE_SIFT), "pallid_grove"));
			sp.getServer().runCommand("time set 1000");
			shot(context, sp, meadow.getX() + 0.5, meadow.getY() + 14, meadow.getZ() + 0.5, 0, 10, "sift_flow_tide");
			sp.getServer().runCommand("time set 12000");
			shot(context, sp, meadow.getX() + 0.5, meadow.getY() + 14, meadow.getZ() + 0.5, 0, 10, "sift_endure_tide");
			sp.getServer().runCommand("time set 20000");
			shot(context, sp, meadow.getX() + 0.5, meadow.getY() + 14, meadow.getZ() + 0.5, 0, 10, "sift_thrive_tide");
			sp.getServer().runCommand("time set 1000");

			// --- creatures on a showcase pad high above the meadow
			int px = meadow.getX(), pz = meadow.getZ(), py = meadow.getY() + 40;
			String in = "execute in sift_unbound:the_sift run ";
			sp.getServer().runCommand(in + "fill " + (px - 6) + " " + (py - 1) + " " + (pz - 6) + " " + (px + 8) + " " + (py - 1) + " " + (pz + 8) + " sift_unbound:sift_grass_block");
			sp.getServer().runCommand(in + "fill " + (px + 5) + " " + (py - 1) + " " + (pz + 4) + " " + (px + 7) + " " + (py - 1) + " " + (pz + 6) + " sift_unbound:soul_water");
			sp.getServer().runCommand(in + "summon sift_unbound:blub " + px + " " + py + " " + (pz + 2) + " {NoAI:1b,Rotation:[200f,0f],Variant:\"soul\"}");
			sp.getServer().runCommand(in + "summon sift_unbound:blub " + (px + 1.5) + " " + py + " " + (pz + 2.5) + " {NoAI:1b,Rotation:[160f,0f],Variant:\"meadows\"}");
			sp.getServer().runCommand(in + "summon sift_unbound:blub " + (px - 1.5) + " " + py + " " + (pz + 2.5) + " {NoAI:1b,Rotation:[220f,0f],Variant:\"carapace\"}");
			sp.getServer().runCommand(in + "summon sift_unbound:sculk_prowler " + (px + 3) + " " + py + " " + (pz + 5) + " {NoAI:1b,Rotation:[150f,0f]}");
			sp.getServer().runCommand(in + "summon sift_unbound:soul_wisp " + (px - 2) + " " + (py + 1.5) + " " + (pz + 4) + " {NoAI:1b}");
			sp.getServer().runCommand(in + "setblock " + (px - 3) + " " + py + " " + (pz + 6) + " sift_unbound:lullaby_bloom");
			sp.getServer().runCommand(in + "setblock " + (px - 2) + " " + py + " " + (pz + 6) + " sift_unbound:tall_sift_grass[half=lower]");
			sp.getServer().runCommand(in + "setblock " + (px - 2) + " " + (py + 1) + " " + (pz + 6) + " sift_unbound:tall_sift_grass[half=upper]");
			sp.getServer().runCommand(in + "setblock " + (px - 4) + " " + py + " " + (pz + 5) + " sift_unbound:teal_sprouts");
			sp.getConnection().waitForClientboundEntityUpdates(ModEntities.BLUB);
			shot(context, sp, px + 0.5, py + 1, pz - 3.5, 0, 25, "sift_creatures");

			// --- an awakened portal with soul water in front, on a platform next to the creatures
			int fx = px + 20;
			sp.getServer().runCommand(in + "fill " + (fx - 4) + " " + (py - 1) + " " + (pz - 4) + " " + (fx + 5) + " " + (py - 1) + " " + (pz + 4) + " sift_unbound:polished_soulshale");
			sp.getServer().runCommand(in + "fill " + (fx - 1) + " " + (py - 1) + " " + pz + " " + (fx + 2) + " " + (py + 3) + " " + pz + " minecraft:reinforced_deepslate");
			sp.getServer().runCommand(in + "fill " + fx + " " + py + " " + pz + " " + (fx + 1) + " " + (py + 2) + " " + pz + " air");
			sp.getServer().runOnServer(server -> WardenPortal.tryLight(server.getLevel(SiftTravel.THE_SIFT), new BlockPos(fx, py, pz)));
			sp.getServer().runCommand(in + "fill " + (fx - 4) + " " + (py - 1) + " " + (pz - 3) + " " + (fx + 5) + " " + (py - 1) + " " + (pz - 2) + " sift_unbound:soul_water");
			shot(context, sp, fx + 1.0, py + 1.5, pz - 6.5, 0, 10, "sift_portal");
		}
	}

	/** Teleports the (flying) camera, waits for chunks, and saves a screenshot. */
	private static void shot(ClientGameTestContext context, TestSingleplayerContext sp, double x, double y, double z, float yaw, float pitch, String name) {
		sp.getServer().runCommand(String.format(java.util.Locale.ROOT, "execute in sift_unbound:the_sift run tp @p %.2f %.2f %.2f %.1f %.1f", x, y, z, yaw, pitch));
		sp.getConnection().waitForChunksRender();
		context.waitTicks(30);
		String where = sp.getServer().computeOnServer(server -> {
			var p = server.getPlayerList().getPlayers().get(0);
			var lvl = p.level();
			BlockPos feet = p.blockPosition();
			return String.format(java.util.Locale.ROOT, "%s pos=%s dim=%s feet=%s below=%s biome=%s flying=%s",
				name, feet.toShortString(), lvl.dimension().identifier(), lvl.getBlockState(feet), lvl.getBlockState(feet.below()),
				lvl.getBiome(feet).getRegisteredName(), p.getAbilities().flying);
		});
		System.out.println("[SiftClientGameTest] " + where);
		context.takeScreenshot(TestScreenshotOptions.of(name).disableCounterPrefix());
	}

	/** A dry surface position inside the given Sift biome (searching outward from the origin), or null. */
	static BlockPos landIn(ServerLevel sift, String biome) {
		var key = net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.BIOME,
			com.milkdromedastudios.siftunbound.SiftUnbound.id(biome));
		var found = sift.findClosestBiome3d(h -> h.is(key), new BlockPos(0, 64, 0), 4000, 32, 64);
		if (found == null) {
			return null;
		}
		BlockPos p = found.getFirst();
		for (int r = 0; r <= 96; r += 8) {
			for (int dx = -r; dx <= r; dx += 8) {
				for (int dz = -r; dz <= r; dz += 8) {
					if (Math.max(Math.abs(dx), Math.abs(dz)) != r) {
						continue;
					}
					int x = p.getX() + dx, z = p.getZ() + dz;
					int y = sift.getChunk(x >> 4, z >> 4).getHeight(Heightmap.Types.MOTION_BLOCKING, x & 15, z & 15) + 1;
					BlockPos ground = new BlockPos(x, y - 1, z);
					if (sift.getFluidState(ground).isEmpty() && sift.getFluidState(ground.above()).isEmpty()
						&& sift.getBiome(ground).is(key)) {
						return ground.above();
					}
				}
			}
		}
		return p.atY(sift.getChunk(p.getX() >> 4, p.getZ() >> 4).getHeight(Heightmap.Types.MOTION_BLOCKING, p.getX() & 15, p.getZ() & 15) + 1);
	}
}
