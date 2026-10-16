package com.minecraftmode.client.creature;

import com.minecraftmode.entity.CreatureAnim;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

/** Render state of a creature: the vanilla living state plus its current one-shot animation. */
public class CreatureRenderState extends LivingEntityRenderState {
	public CreatureAnim anim = CreatureAnim.NONE;
	/** Ticks into {@link #anim}. */
	public float animTime;
	public boolean flying;
	public int phase;
	/** Sitting, lying or perched (companions while their owner stands still). */
	public boolean resting;
	/** Eased amounts, 0..1: into the rest pose, wings folded, rearing. */
	public float rest;
	public float fold;
	public float stand;
	/** Ground speed in blocks per tick; fast walkers break into a gallop. */
	public float gait;
	/** Bank into turns and nose up or down in flight, in degrees. */
	public float roll;
	public float tilt;
}
