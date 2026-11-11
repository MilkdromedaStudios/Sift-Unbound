package com.milkdromedastudios.siftunbound.world;

import com.milkdromedastudios.siftunbound.portal.SiftTravel;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;

/**
 * The Sift's day is split into three tides (from Minecraft Dungeons II): Flow, Endure and Thrive, 8000 ticks each.
 * The sky/fog/light changes live in data/sift_unbound/timeline/sift_tides.json. Gameplay, as in Dungeons II:
 * <ul>
 * <li>Flow Tide: no change.</li>
 * <li>Endure Tide: stronger monsters spawn, and wounds don't mend on their own (no natural regeneration).</li>
 * <li>Thrive Tide: wounds mend 50% faster.</li>
 * </ul>
 * Players in the Sift are told when the tide turns.
 */
public final class SiftTides {
	public enum Tide {
		FLOW, ENDURE, THRIVE;

		public String translationKey() {
			return "tide.sift_unbound." + name().toLowerCase(Locale.ROOT);
		}
	}

	public static final float THRIVE_HEALING = 1.5F;
	private static final Map<ResourceKey<Level>, Tide> LAST = new HashMap<>();

	public static Tide current(Level level) {
		long t = Math.floorMod(level.getOverworldClockTime(), 24000L);
		return t < 8000 ? Tide.FLOW : t < 16000 ? Tide.ENDURE : Tide.THRIVE;
	}

	public static boolean inSift(Level level) {
		return level.dimension() == SiftTravel.THE_SIFT;
	}

	/** True when natural regeneration is suspended for this player (in the Sift during the Endure Tide). */
	public static boolean blocksNaturalHealing(ServerPlayer player) {
		return inSift(player.level()) && current(player.level()) == Tide.ENDURE;
	}

	/** Natural healing amount after the tide's effect (only called when healing is allowed). */
	public static float scaleNaturalHealing(ServerPlayer player, float amount) {
		return inSift(player.level()) && current(player.level()) == Tide.THRIVE ? amount * THRIVE_HEALING : amount;
	}

	/** Monsters that spawn in the Sift during the Endure Tide are stronger: Strength and Resistance for good. */
	public static void empowerIfEndure(Mob mob) {
		if (inSift(mob.level()) && current(mob.level()) == Tide.ENDURE) {
			for (Holder<MobEffect> effect : java.util.List.of(MobEffects.STRENGTH, MobEffects.RESISTANCE)) {
				mob.addEffect(new MobEffectInstance(effect, MobEffectInstance.INFINITE_DURATION, 0, false, false, true));
			}
		}
	}

	public static void init() {
		ServerTickEvents.END_LEVEL_TICK.register(level -> {
			if (!inSift(level) || level.getGameTime() % 20 != 0) {
				return;
			}
			Tide now = current(level);
			Tide before = LAST.put(level.dimension(), now);
			if (before != null && before != now) {
				announce(level, now);
			}
		});
	}

	private static void announce(ServerLevel level, Tide tide) {
		Component message = Component.translatable("message.sift_unbound.tide." + tide.name().toLowerCase(Locale.ROOT));
		for (ServerPlayer player : level.players()) {
			player.sendOverlayMessage(message);
			level.playSound(null, player.getX(), player.getY(), player.getZ(),
				tide == Tide.ENDURE ? SoundEvents.SCULK_SHRIEKER_SHRIEK : SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.AMBIENT,
				0.6F, tide == Tide.ENDURE ? 0.6F : 1.2F);
		}
	}

	private SiftTides() {
	}
}
