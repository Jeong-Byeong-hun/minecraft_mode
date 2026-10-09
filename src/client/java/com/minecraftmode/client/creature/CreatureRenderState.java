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
}
