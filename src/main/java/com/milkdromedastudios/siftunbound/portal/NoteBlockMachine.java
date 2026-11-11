package com.milkdromedastudios.siftunbound.portal;

import com.milkdromedastudios.siftunbound.registry.ModSounds;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.NoteBlock;
import net.minecraft.world.phys.Vec3;

/**
 * The note block machine (Minecraft Dungeons II): eight note blocks in front of the Warden Portal play the Sift's
 * melody and the portal wakes. Here any eight note blocks near a complete reinforced-deepslate frame will do, played one
 * after another (by hand or by redstone) within 20 seconds, each block playing one note of
 * <b>F♯ A C♯ B A E C♯ F♯</b> (note-block clicks 12, 15, 19, 17, 15, 10, 7, 12). Every correct note so far sends up a
 * soul; the eighth wakes the portal. The Echo Tuner hums the melody when used while sneaking, and lights a frame directly.
 */
public final class NoteBlockMachine {
	/** The Sift's melody, as note-block notes (0 = F♯3, 24 = F♯5). */
	public static final int[] MELODY = {12, 15, 19, 17, 15, 10, 7, 12};
	private static final String[] NAMES = {"F♯", "G", "G♯", "A", "A♯", "B", "C", "C♯", "D", "D♯", "E", "F"};
	private static final long WINDOW = 20 * 20;
	private static final int SEARCH_RADIUS = 24;

	private record Played(BlockPos pos, int note, long time) {
	}

	private record Hum(ResourceKey<Level> level, Vec3 at, int note, long tick) {
	}

	/** Notes played close together form one machine; machines elsewhere in the dimension don't interfere. */
	private static final class Machine {
		final Deque<Played> notes = new ArrayDeque<>();
		double x, y, z;
		long last;

		double distSqr(BlockPos p) {
			double dx = p.getX() - x, dy = p.getY() - y, dz = p.getZ() - z;
			return dx * dx + dy * dy + dz * dz;
		}

		void recentre() {
			x = notes.stream().mapToInt(n -> n.pos().getX()).average().orElse(x);
			y = notes.stream().mapToInt(n -> n.pos().getY()).average().orElse(y);
			z = notes.stream().mapToInt(n -> n.pos().getZ()).average().orElse(z);
		}
	}

	private static final double MACHINE_RADIUS_SQR = 12 * 12;
	private static final Map<ResourceKey<Level>, List<Machine>> MACHINES = new HashMap<>();
	private static final List<Hum> HUMS = new ArrayList<>();

	public static String noteName(int note) {
		return NAMES[Math.floorMod(note, 12)];
	}

	public static String melodyText() {
		StringBuilder sb = new StringBuilder();
		for (int note : MELODY) {
			sb.append(sb.isEmpty() ? "" : " ").append(noteName(note));
		}
		return sb.toString();
	}

	/** Called (from a mixin) whenever a note block actually sounds. */
	public static void onNotePlayed(ServerLevel level, BlockPos pos, int note) {
		long now = level.getGameTime();
		List<Machine> machines = MACHINES.computeIfAbsent(level.dimension(), k -> new ArrayList<>());
		machines.removeIf(m -> now - m.last > WINDOW);
		Machine machine = machines.stream().filter(m -> m.distSqr(pos) <= MACHINE_RADIUS_SQR)
			.min(Comparator.comparingDouble(m -> m.distSqr(pos))).orElse(null);
		if (machine == null) {
			machine = new Machine();
			machines.add(machine);
		}
		Deque<Played> recent = machine.notes;
		recent.removeIf(p -> now - p.time() > WINDOW || p.pos().equals(pos));
		recent.addLast(new Played(pos.immutable(), note, now));
		while (recent.size() > MELODY.length) {
			recent.removeFirst();
		}
		machine.recentre();
		machine.last = now;
		int progress = progress(recent);
		if (progress == 0) {
			return;
		}
		level.sendParticles(ParticleTypes.SCULK_SOUL, pos.getX() + 0.5, pos.getY() + 1.3, pos.getZ() + 0.5, 1, 0.1, 0.05, 0.1, 0.01);
		if (progress == MELODY.length) {
			List<BlockPos> blocks = recent.stream().map(Played::pos).toList();
			if (wakeNearestPortal(level, blocks)) {
				recent.clear();
			}
		}
	}

	/** How many of the most recent notes form the start of the melody (8 = the whole melody). */
	private static int progress(Deque<Played> recent) {
		List<Played> list = new ArrayList<>(recent);
		for (int k = Math.min(list.size(), MELODY.length); k > 0; k--) {
			boolean ok = true;
			for (int i = 0; i < k && ok; i++) {
				ok = list.get(list.size() - k + i).note() == MELODY[i];
			}
			if (ok) {
				return k;
			}
		}
		return 0;
	}

	/** Finds a complete Warden Portal frame near the note blocks and lights it. */
	static boolean wakeNearestPortal(ServerLevel level, List<BlockPos> noteBlocks) {
		Optional<Found> found = findDormant(level, centre(noteBlocks));
		if (found.isPresent() && WardenPortal.tryLight(level, found.get().start()) == WardenPortal.Result.LIT) {
			celebrate(level, noteBlocks, found.get().shape());
			return true;
		}
		return false;
	}

	/** Wakes the nearest dormant Warden Portal around {@code pos} (used by echo golems). */
	public static boolean wakePortalNear(ServerLevel level, BlockPos pos) {
		return wakeNearestPortal(level, List.of(pos));
	}

	/** Whether a complete, still dormant Warden Portal frame is within reach of {@code pos}. */
	public static boolean dormantPortalNear(ServerLevel level, BlockPos pos) {
		return findDormant(level, pos).isPresent();
	}

	private record Found(BlockPos start, WardenPortal.Shape shape) {
	}

	private static Optional<Found> findDormant(ServerLevel level, BlockPos centre) {
		List<BlockPos> frames = new ArrayList<>();
		for (BlockPos p : BlockPos.betweenClosed(centre.offset(-SEARCH_RADIUS, -8, -SEARCH_RADIUS), centre.offset(SEARCH_RADIUS, 12, SEARCH_RADIUS))) {
			if (WardenPortal.isFrame(level.getBlockState(p))) {
				frames.add(p.immutable());
			}
		}
		frames.sort(Comparator.comparingDouble(p -> p.distSqr(centre)));
		int tries = 0;
		for (BlockPos frame : frames) {
			for (Direction d : Direction.values()) {
				BlockPos start = frame.relative(d);
				if (WardenPortal.isFrame(level.getBlockState(start))) {
					continue;
				}
				Optional<WardenPortal.Shape> shape = WardenPortal.find(level, start);
				if (shape.isPresent() && !level.getBlockState(shape.get().bottomLeft()).is(com.milkdromedastudios.siftunbound.registry.ModBlocks.SIFT_PORTAL)) {
					return Optional.of(new Found(start, shape.get()));
				}
			}
			if (++tries > 48) {
				break;
			}
		}
		return Optional.empty();
	}

	private static void celebrate(ServerLevel level, List<BlockPos> noteBlocks, WardenPortal.Shape shape) {
		BlockPos mid = shape.bottomLeft().relative(shape.right(), shape.width() / 2).above(shape.height() / 2);
		level.playSound(null, mid, ModSounds.SIFT_PORTAL_ACTIVATE, SoundSource.BLOCKS, 1.0F, 1.0F);
		for (BlockPos p : noteBlocks) {
			level.sendParticles(ParticleTypes.SCULK_SOUL, p.getX() + 0.5, p.getY() + 1.2, p.getZ() + 0.5, 6, 0.2, 0.3, 0.2, 0.02);
		}
		level.sendParticles(ParticleTypes.SCULK_SOUL, mid.getX() + 0.5, mid.getY() + 0.5, mid.getZ() + 0.5, 60,
			shape.width() / 2.0, shape.height() / 2.0, 1.0, 0.05);
		Component message = Component.translatable("message.sift_unbound.portal.machine");
		for (ServerPlayer player : level.players()) {
			if (player.blockPosition().distSqr(mid) < 64 * 64) {
				player.sendOverlayMessage(message);
			}
		}
	}

	private static BlockPos centre(List<BlockPos> blocks) {
		long x = 0, y = 0, z = 0;
		for (BlockPos p : blocks) {
			x += p.getX();
			y += p.getY();
			z += p.getZ();
		}
		int n = blocks.size();
		return new BlockPos((int) (x / n), (int) (y / n), (int) (z / n));
	}

	/** The Echo Tuner hums the melody: eight harp notes, one every 6 ticks, with note particles. */
	public static void hum(ServerLevel level, Vec3 at) {
		long now = level.getServer().getTickCount();
		for (int i = 0; i < MELODY.length; i++) {
			HUMS.add(new Hum(level.dimension(), at, MELODY[i], now + 1 + i * 6L));
		}
	}

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (HUMS.isEmpty()) {
				return;
			}
			long now = server.getTickCount();
			for (Iterator<Hum> it = HUMS.iterator(); it.hasNext(); ) {
				Hum hum = it.next();
				if (hum.tick() > now) {
					continue;
				}
				it.remove();
				ServerLevel level = server.getLevel(hum.level());
				if (level == null) {
					continue;
				}
				Vec3 at = hum.at();
				level.playSound(null, at.x, at.y, at.z, SoundEvents.NOTE_BLOCK_HARP, SoundSource.PLAYERS, 1.0F, NoteBlock.getPitchFromNote(hum.note()));
				level.sendParticles(ParticleTypes.NOTE, at.x, at.y + 1.6, at.z, 0, hum.note() / 24.0, 0.0, 0.0, 1.0);
			}
		});
	}

	private NoteBlockMachine() {
	}
}
