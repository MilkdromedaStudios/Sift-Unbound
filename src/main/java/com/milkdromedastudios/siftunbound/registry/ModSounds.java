package com.milkdromedastudios.siftunbound.registry;

import com.milkdromedastudios.siftunbound.SiftUnbound;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

public final class ModSounds {
	public static final SoundEvent BLUB_AMBIENT = register("entity.blub.ambient");
	public static final SoundEvent BLUB_HURT = register("entity.blub.hurt");
	public static final SoundEvent BLUB_DEATH = register("entity.blub.death");
	public static final SoundEvent BLUB_JUMP = register("entity.blub.jump");
	public static final SoundEvent SOUL_WISP_AMBIENT = register("entity.soul_wisp.ambient");
	public static final SoundEvent SOUL_WISP_HURT = register("entity.soul_wisp.hurt");
	public static final SoundEvent SOUL_WISP_DEATH = register("entity.soul_wisp.death");
	public static final SoundEvent SCULK_PROWLER_AMBIENT = register("entity.sculk_prowler.ambient");
	public static final SoundEvent SCULK_PROWLER_HURT = register("entity.sculk_prowler.hurt");
	public static final SoundEvent SCULK_PROWLER_DEATH = register("entity.sculk_prowler.death");
	public static final SoundEvent NUZZLE_AMBIENT = register("entity.nuzzle.ambient");
	public static final SoundEvent NUZZLE_HURT = register("entity.nuzzle.hurt");
	public static final SoundEvent NUZZLE_DEATH = register("entity.nuzzle.death");
	public static final SoundEvent SLABBER_AMBIENT = register("entity.slabber.ambient");
	public static final SoundEvent SLABBER_HURT = register("entity.slabber.hurt");
	public static final SoundEvent SLABBER_DEATH = register("entity.slabber.death");
	public static final SoundEvent ECHO_GOLEM_AMBIENT = register("entity.echo_golem.ambient");
	public static final SoundEvent ECHO_GOLEM_HURT = register("entity.echo_golem.hurt");
	public static final SoundEvent ECHO_GOLEM_DEATH = register("entity.echo_golem.death");
	public static final SoundEvent SINGER_AMBIENT = register("entity.singer.ambient");
	public static final SoundEvent SOUL_WATER_AMBIENT = register("block.soul_water.ambient");
	public static final SoundEvent SOUL_WATER_BURN = register("block.soul_water.burn");
	public static final SoundEvent SIFT_PORTAL_AMBIENT = register("block.sift_portal.ambient");
	public static final SoundEvent SIFT_PORTAL_TRAVEL = register("block.sift_portal.travel");
	public static final SoundEvent SIFT_PORTAL_ACTIVATE = register("block.sift_portal.activate");
	// Phase 3: the sifters and the scavenger
	public static final SoundEvent SEEDLING_AMBIENT = register("entity.seedling.ambient");
	public static final SoundEvent SEEDLING_HURT = register("entity.seedling.hurt");
	public static final SoundEvent SEEDLING_DEATH = register("entity.seedling.death");
	public static final SoundEvent NESTER_AMBIENT = register("entity.nester.ambient");
	public static final SoundEvent NESTER_HURT = register("entity.nester.hurt");
	public static final SoundEvent NESTER_DEATH = register("entity.nester.death");
	public static final SoundEvent POLLINATOR_AMBIENT = register("entity.pollinator.ambient");
	public static final SoundEvent POLLINATOR_HURT = register("entity.pollinator.hurt");
	public static final SoundEvent POLLINATOR_DEATH = register("entity.pollinator.death");
	public static final SoundEvent SENTINEL_AMBIENT = register("entity.sentinel.ambient");
	public static final SoundEvent SENTINEL_HURT = register("entity.sentinel.hurt");
	public static final SoundEvent SENTINEL_DEATH = register("entity.sentinel.death");
	public static final SoundEvent SPROUT_AMBIENT = register("entity.sprout.ambient");
	public static final SoundEvent SPROUT_HURT = register("entity.sprout.hurt");
	public static final SoundEvent SPROUT_DEATH = register("entity.sprout.death");
	public static final SoundEvent HARMONIZER_AMBIENT = register("entity.harmonizer.ambient");
	public static final SoundEvent HARMONIZER_HURT = register("entity.harmonizer.hurt");
	public static final SoundEvent HARMONIZER_DEATH = register("entity.harmonizer.death");
	public static final SoundEvent SCAVENGER_AMBIENT = register("entity.scavenger.ambient");
	public static final SoundEvent SCAVENGER_HURT = register("entity.scavenger.hurt");
	public static final SoundEvent SCAVENGER_DEATH = register("entity.scavenger.death");
	public static final SoundEvent SIFTER_ENRAGE = register("entity.sifter.enrage");
	public static final SoundEvent SIFTER_DODGE = register("entity.sifter.dodge");
	public static final SoundEvent POLLINATOR_PUFF = register("entity.pollinator.puff");
	public static final SoundEvent SENTINEL_SHAKE = register("entity.sentinel.shake");
	public static final SoundEvent SENTINEL_QUILLS = register("entity.sentinel.quills");
	public static final SoundEvent SPROUT_LINK = register("entity.sprout.link");
	public static final SoundEvent HARMONIZER_ROAR = register("entity.harmonizer.roar");
	public static final SoundEvent SCAVENGER_CAST = register("entity.scavenger.cast");
	public static final SoundEvent SCAVENGER_DASH = register("entity.scavenger.dash");
	public static final Holder<SoundEvent> AMBIENT_LOOP = registerHolder("ambient.sift.loop");
	public static final Holder<SoundEvent> AMBIENT_ADDITIONS = registerHolder("ambient.sift.additions");
	public static final Holder<SoundEvent> AMBIENT_MOOD = registerHolder("ambient.sift.mood");
	public static final Holder<SoundEvent> MUSIC_SIFT = registerHolder("music.sift");

	private static SoundEvent register(String name) {
		Identifier id = SiftUnbound.id(name);
		return Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
	}

	private static Holder<SoundEvent> registerHolder(String name) {
		Identifier id = SiftUnbound.id(name);
		return Registry.registerForHolder(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
	}

	public static void init() {
	}

	private ModSounds() {
	}
}
