package com.minecraftmode.client.creature;

import com.minecraftmode.client.creature.BodyPlan.Part;
import com.minecraftmode.client.creature.BodyPlan.PlacedBox;
import com.minecraftmode.entity.CreatureAnim;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.util.Mth;

/**
 * A model built from a {@link BodyPlan}. Every part is animated by its role: biped arms and legs swing,
 * quadruped and spider legs walk in their gaits, wings flap, tails, necks and tentacles sway, and the
 * one-shot {@link CreatureAnim animations} (slam, cast, roar, breath, charge...) pose arms, head, jaw
 * and body on top.
 */
public class CreatureModel extends EntityModel<CreatureRenderState> {
	private final BodyPlan plan;
	private final Map<Part, ModelPart> parts = new LinkedHashMap<>();

	public CreatureModel(final ModelPart root, final BodyPlan plan) {
		super(root, RenderTypes::entityCutout);
		this.plan = plan;
		Map<String, ModelPart> byName = new HashMap<>();
		for (Part part : plan.parts) {
			ModelPart parent = part.parent == null ? root : byName.get(part.parent);
			ModelPart model = parent.getChild(part.name);
			byName.put(part.name, model);
			this.parts.put(part, model);
		}
	}

	public static LayerDefinition createLayer(final BodyPlan plan) {
		MeshDefinition mesh = new MeshDefinition();
		Map<String, PartDefinition> defs = new HashMap<>();
		List<PlacedBox> placed = plan.pack();
		for (Part part : plan.parts) {
			CubeListBuilder cubes = CubeListBuilder.create();
			for (PlacedBox p : placed) {
				if (p.part() == part) {
					BodyPlan.Box b = p.box();
					cubes.texOffs(p.u(), p.v()).addBox(b.x(), b.y(), b.z(), b.w(), b.h(), b.d(), new CubeDeformation(b.inflate()));
				}
			}
			PartDefinition parent = part.parent == null ? mesh.getRoot() : defs.get(part.parent);
			defs.put(part.name, parent.addOrReplaceChild(part.name, cubes, PartPose.offsetAndRotation(part.px, part.py, part.pz, part.xRot, part.yRot, part.zRot)));
		}
		return LayerDefinition.create(mesh, plan.textureWidth(), plan.textureHeight());
	}

	/** 0 -> 1 -> 0 over an animation. */
	private static float env(final float p) {
		return Mth.sin(p * Mth.PI);
	}

	@Override
	public void setupAnim(final CreatureRenderState s) {
		super.setupAnim(s);
		float walk = s.walkAnimationPos;
		float speed = Math.min(1.0F, s.walkAnimationSpeed);
		float age = s.ageInTicks;
		float yaw = s.yRot * Mth.DEG_TO_RAD;
		float pitch = s.xRot * Mth.DEG_TO_RAD;
		CreatureAnim anim = s.anim;
		float p = anim == CreatureAnim.NONE ? 0.0F : Mth.clamp(s.animTime / anim.ticks(), 0.0F, 1.0F);
		float e = env(p);
		boolean flying = s.flying || this.plan.rig == BodyPlan.Rig.FLOATER;
		int necks = (int)this.parts.keySet().stream().filter(part -> part.role == BodyPlan.Role.NECK).count();
		float headShare = necks > 0 ? 0.5F : 1.0F;

		for (Map.Entry<Part, ModelPart> entry : this.parts.entrySet()) {
			Part part = entry.getKey();
			ModelPart m = entry.getValue();
			boolean right = part.role == BodyPlan.Role.ARM_RIGHT || part.role == BodyPlan.Role.LEG_RIGHT || part.role == BodyPlan.Role.WING_RIGHT;
			switch (part.role) {
				case BODY -> {
					if (flying) {
						m.y += Mth.sin(age * 0.1F) * 1.2F;
					}
					switch (anim) {
						case CHARGE -> m.xRot += 0.35F * e;
						case DIVE -> m.xRot += 0.6F * e;
						case ROAR -> m.xRot -= 0.15F * e;
						case SPIN -> m.yRot += p * Mth.TWO_PI;
						case LEAP -> m.y -= 3.0F * e;
						default -> {
						}
					}
				}
				case HEAD -> {
					m.yRot += yaw * headShare;
					m.xRot += pitch * headShare;
					if (anim == CreatureAnim.ROAR) {
						m.xRot -= 0.5F * e;
					} else if (anim == CreatureAnim.BREATH) {
						m.xRot += 0.25F * e;
					}
				}
				case JAW -> {
					float open = switch (anim) {
						case ATTACK -> 0.5F * e;
						case ROAR -> 0.7F * e;
						case BREATH -> 0.6F * Math.min(1.0F, p * 6.0F) * (p > 0.9F ? (1.0F - p) * 10.0F : 1.0F);
						case CAST, SUMMON -> 0.25F * e;
						default -> 0.0F;
					};
					m.xRot += open;
				}
				case ARM_RIGHT, ARM_LEFT -> {
					m.xRot += Mth.cos(walk * 0.6662F + (right ? Mth.PI : 0.0F)) * 1.1F * speed;
					m.zRot += (right ? 1 : -1) * (Mth.cos(age * 0.09F) * 0.05F + 0.05F);
					switch (anim) {
						case ATTACK -> {
							if (right) {
								m.xRot -= 1.7F * e;
							}
						}
						case SLAM -> m.xRot += p < 0.5F ? -2.8F * (p / 0.5F) : -2.8F + 3.6F * ((p - 0.5F) / 0.5F);
						case CAST -> {
							m.xRot -= 1.6F * e;
							m.zRot += (right ? 1 : -1) * 0.4F * e;
						}
						case SUMMON -> {
							m.zRot += (right ? 1 : -1) * 1.5F * e;
							m.xRot -= 0.4F * e;
						}
						case CHARGE -> m.xRot += 0.8F * e;
						default -> {
						}
					}
				}
				case LEG_RIGHT, LEG_LEFT -> {
					if (!flying) {
						m.xRot += Mth.cos(walk * 0.6662F + (right ? 0.0F : Mth.PI)) * 1.4F * speed;
					}
					if (anim == CreatureAnim.LEAP) {
						m.xRot += (right ? 1 : -1) * 0.6F * e;
					}
				}
				case LEG_QUAD -> {
					float phase = part.index == 0 || part.index == 3 ? 0.0F : Mth.PI;
					m.xRot += Mth.cos(walk * 0.6662F + phase) * 1.4F * speed;
					if (anim == CreatureAnim.LEAP || anim == CreatureAnim.CHARGE) {
						m.xRot += (part.index < 2 ? -0.7F : 0.7F) * e;
					}
				}
				case LEG_SPIDER -> {
					int k = part.index % 4;
					boolean left = part.index < 4;
					float swing = -Mth.cos(walk * 0.6662F * 2.0F + k * Mth.HALF_PI) * 0.4F * speed;
					float lift = Math.abs(Mth.sin(walk * 0.6662F + k * Mth.HALF_PI)) * 0.4F * speed;
					m.yRot += (left ? swing : -swing);
					m.zRot += left ? lift : -lift;
					if (anim == CreatureAnim.SLAM || anim == CreatureAnim.LEAP) {
						m.zRot += (left ? 0.5F : -0.5F) * e;
					}
				}
				case WING_RIGHT, WING_LEFT -> {
					float flap = flying ? Mth.sin(age * 0.35F) * 0.65F : Mth.sin(age * 0.08F) * 0.08F;
					if (anim == CreatureAnim.DIVE) {
						flap = -0.9F * e;
					} else if (anim == CreatureAnim.ROAR || anim == CreatureAnim.CAST) {
						flap += 0.6F * e;
					}
					m.zRot += right ? flap : -flap;
				}
				case TAIL -> {
					m.yRot += Mth.sin(age * 0.12F - part.index * 0.7F) * 0.15F * (1.0F + part.index * 0.15F) + Mth.cos(walk * 0.3F) * 0.1F * speed;
					if (anim == CreatureAnim.SPIN || anim == CreatureAnim.SLAM) {
						m.yRot += 0.4F * e;
					}
				}
				case NECK -> {
					m.yRot += yaw * 0.5F / Math.max(1, necks) + Mth.sin(age * 0.05F - part.index * 0.5F) * 0.05F;
					m.xRot += pitch * 0.5F / Math.max(1, necks);
					if (anim == CreatureAnim.BREATH) {
						m.xRot += 0.12F * e;
					} else if (anim == CreatureAnim.ROAR) {
						m.xRot -= 0.25F * e;
					}
				}
				case TENTACLE -> {
					int t = part.index / 10;
					int seg = part.index % 10;
					m.xRot += Mth.sin(age * 0.12F + t * 0.9F + seg * 0.7F) * 0.25F;
					m.zRot += Mth.cos(age * 0.1F + t * 1.3F + seg * 0.5F) * 0.15F;
					if (anim == CreatureAnim.SLAM) {
						m.xRot -= (seg == 0 ? 1.0F : 0.3F) * e;
					}
				}
				case ORBIT -> m.yRot += age * 0.05F;
				case CAPE -> m.xRot += 0.08F + speed * 0.7F + Mth.sin(age * 0.1F) * 0.03F;
				default -> {
				}
			}
		}
	}
}
