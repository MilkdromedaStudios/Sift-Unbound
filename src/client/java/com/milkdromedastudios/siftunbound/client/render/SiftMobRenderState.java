package com.milkdromedastudios.siftunbound.client.render;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

/** Render state shared by the Sift's newer mobs: flags, 0..1 progress values and animation channels. */
public class SiftMobRenderState extends LivingEntityRenderState {
	/** Nuzzle: sheared. Slabber: asleep. */
	public boolean flag;
	/** Nuzzle: how far the head is lowered to graze. Echo golem: soul block charge. */
	public float progress;
	/** 0..1 while singing (singer, echo golem). */
	public float sing;
	/** Sifters: enraged (purple details glow). */
	public boolean enraged;
	/** 0..1 attack animation (bite, whip, tongue, slap, cast...). */
	public float attack;
	/** 0..1 second animation channel (throat puff, head shake, roar, life link, dash...). */
	public float special;
	/** 0..1 third animation channel (harmonizer vortex). */
	public float extra;
	/** Tint for the glow layer (ARGB; -1 = untinted). */
	public int glowTint = -1;
}
