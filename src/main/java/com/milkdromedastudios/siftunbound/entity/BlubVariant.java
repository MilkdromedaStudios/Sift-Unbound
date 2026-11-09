package com.milkdromedastudios.siftunbound.entity;

import java.util.function.IntFunction;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;

/** The blub's regional looks: soul-engorged (glowing blue), meadows (mossy) and carapace (shelled, desert). */
public enum BlubVariant implements StringRepresentable {
	SOUL(0, "soul"),
	MEADOWS(1, "meadows"),
	CARAPACE(2, "carapace");

	private static final IntFunction<BlubVariant> BY_ID = ByIdMap.continuous(v -> v.id, values(), ByIdMap.OutOfBoundsStrategy.ZERO);
	public static final StringRepresentable.EnumCodec<BlubVariant> CODEC = StringRepresentable.fromEnum(BlubVariant::values);
	public final int id;
	private final String name;

	BlubVariant(int id, String name) {
		this.id = id;
		this.name = name;
	}

	public static BlubVariant byId(int id) {
		return BY_ID.apply(id);
	}

	@Override
	public String getSerializedName() {
		return name;
	}
}
