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
