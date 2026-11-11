package com.milkdromedastudios.siftunbound.registry;

import com.milkdromedastudios.siftunbound.SiftUnbound;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.advancements.triggers.SimpleCriterionTrigger;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

/**
 * The mod's advancement trigger, {@code sift_unbound:sift_event}: things that happen in the Sift that no vanilla trigger
 * sees, such as playing the melody or a turn of the tide. Advancements match it by {@code "event"} name; the guide
 * advancements use it to teach each mechanic as the player first meets it.
 */
public final class ModCriteria {
	public static final String HUMMED_MELODY = "hummed_melody";
	public static final String PLAYED_MELODY = "played_melody";
	public static final String GOLEM_WOKE_PORTAL = "golem_woke_portal";
	public static final String SHEARED_NUZZLE = "sheared_nuzzle";
	public static final String TIDE_ENDURE = "tide_endure";
	public static final String TIDE_THRIVE = "tide_thrive";
	public static final String SIFTERS_ENRAGED = "sifters_enraged";
	public static final String MONARCH_ECHO = "monarch_echo";

	public static final SiftEventTrigger SIFT_EVENT = Registry.register(BuiltInRegistries.TRIGGER_TYPES, SiftUnbound.id("sift_event"), new SiftEventTrigger());

	/** Awards {@code event} to one player. */
	public static void award(ServerPlayer player, String event) {
		SIFT_EVENT.trigger(player, event);
	}

	/** Awards {@code event} to every player within {@code radius} blocks of {@code pos}. */
	public static void awardNearby(ServerLevel level, BlockPos pos, double radius, String event) {
		for (ServerPlayer player : level.players()) {
			if (player.blockPosition().distSqr(pos) <= radius * radius) {
				award(player, event);
			}
		}
	}

	public static void init() {
	}

	public static final class SiftEventTrigger extends SimpleCriterionTrigger<SiftEventTrigger.TriggerInstance> {
		@Override
		public Codec<TriggerInstance> codec() {
			return TriggerInstance.CODEC;
		}

		public void trigger(ServerPlayer player, String event) {
			trigger(player, t -> t.event().equals(event));
		}

		public record TriggerInstance(Optional<Holder<LootItemCondition>> player, String event) implements SimpleCriterionTrigger.SimpleInstance {
			public static final Codec<TriggerInstance> CODEC = RecordCodecBuilder.create(i -> i.group(
					LootItemCondition.CODEC.optionalFieldOf("player").forGetter(TriggerInstance::player),
					Codec.STRING.fieldOf("event").forGetter(TriggerInstance::event))
				.apply(i, TriggerInstance::new));
		}
	}

	private ModCriteria() {
	}
}
