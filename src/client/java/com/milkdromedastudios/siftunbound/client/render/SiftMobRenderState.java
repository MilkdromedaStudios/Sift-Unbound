package com.milkdromedastudios.siftunbound.client.render;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

/** Render state shared by the Sift's newer mobs: one flag, one 0..1 progress value and a singing amount. */
public class SiftMobRenderState extends LivingEntityRenderState {
	/** Nuzzle: sheared. Slabber: asleep. */
	public boolean flag;
	/** Nuzzle: how far the head is lowered to graze. Echo golem: soul block charge. */
	public float progress;
	/** 0..1 while singing (singer, echo golem). */
	public float sing;
}
