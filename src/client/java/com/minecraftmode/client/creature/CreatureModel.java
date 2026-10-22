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
import org.jspecify.annotations.Nullable;

/**
 * A model built from a {@link BodyPlan}. Every part is animated by its role: biped arms and legs swing,
 * quadruped and spider legs walk in their gaits, wings flap, tails, necks and tentacles sway, and the
 * one-shot {@link CreatureAnim animations} (slam, cast, roar, breath, charge...) pose arms, head, jaw
 * and body on top. Strikes are eased (a wind-up that slows into the hold, a blow that accelerates into the hit, a
 * follow-through that settles), a hit makes the creature flinch, and an idle creature looks around.
 */
public class CreatureModel extends EntityModel<CreatureRenderState> {
	/** Ground speed (blocks per tick) where a trot starts turning into a gallop, and where it is a full gallop. */
	private static final float GALLOP_FROM = 0.3F;
	private static final float GALLOP_FULL = 0.5F;
	/** How far the body tips back when sitting (radians). */
	private static final float SIT_TILT = -0.55F;
	/** How far a mount rears up (radians). */
	private static final float REAR_TILT = -0.75F;
	/** A resting pet yawns once per this many ticks, for {@link #YAWN_TICKS}. */
	private static final float YAWN_EVERY = 240.0F;
	private static final float YAWN_TICKS = 26.0F;
	/** Shares of an attack: wind-up until here, the hit lands here, then the follow-through. */
	private static final float STRIKE_WINDUP = 0.35F;
	private static final float STRIKE_HIT = 0.6F;
	/** Share of a slam's second half spent bringing the arms down; the rest settles them back to rest. */
	private static final float SLAM_DOWN = 0.35F;
	/** How far a hit knocks the body and head back (radians) at the start of the flinch; halved during a one-shot animation. */
	private static final float FLINCH = 0.22F;

	private final BodyPlan plan;
	private final Map<Part, ModelPart> parts = new LinkedHashMap<>();
	private final @Nullable Part body;
	/** Length of a root-level leg (0 when the legs hang from the body): how far a resting body sinks. */
	private final float legLength;

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
		this.body = plan.parts.stream().filter(p -> p.role == BodyPlan.Role.BODY).findFirst().orElse(null);
		this.legLength = plan.parts.stream().filter(p -> p.parent == null && (p.role == BodyPlan.Role.LEG_QUAD || p.role == BodyPlan.Role.LEG_RIGHT))
			.findFirst().map(p -> 24.0F - p.py).orElse(0.0F);
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
		boolean resting = s.resting;
		boolean flying = !resting && (s.flying || this.plan.rig == BodyPlan.Rig.FLOATER);
		float gallop = Mth.clamp((s.gait - GALLOP_FROM) / (GALLOP_FULL - GALLOP_FROM), 0.0F, 1.0F);
		float stride = walk * 0.6662F;
		// now and then a resting pet yawns: head up, jaw open
		float cycle = age % YAWN_EVERY;
		float yawn = resting && cycle < YAWN_TICKS ? env(cycle / YAWN_TICKS) * s.rest : 0.0F;
		// a hit: knocked back at once, recovering with an ease; smaller while a one-shot animation drives the pose
		float flinch = s.hurt * s.hurt * FLINCH * (anim == CreatureAnim.NONE ? 1.0F : 0.5F);
		// standing still and doing nothing: now and then the head turns to look around
		float idle = anim == CreatureAnim.NONE && !resting ? 1.0F - speed : 0.0F;
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
					} else {
						// breathing, and the rocking bound of a gallop
						m.y += Mth.sin(age * 0.08F) * 0.25F - Math.abs(Mth.sin(stride)) * 1.5F * gallop;
						m.xRot += Mth.sin(stride) * 0.08F * gallop;
						if (this.plan.rig == BodyPlan.Rig.BIPED) {
							// each step lifts the body a little and turns the shoulders against the hips
							m.y -= Math.abs(Mth.cos(stride)) * 0.6F * speed;
							m.yRot += Mth.sin(stride) * 0.08F * speed;
						}
					}
					m.xRot -= flinch * 0.6F;
					if (anim == CreatureAnim.ATTACK) {
						m.yRot += strikeTwist(p);
						m.xRot += 0.18F * strike(p);
					} else if (anim == CreatureAnim.SLAM) {
						m.y += 2.0F * slamCrouch(p);
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
					m.xRot += pitch * headShare + Mth.sin(stride + 1.0F) * 0.12F * gallop;
					m.xRot -= flinch;
					m.yRot += lookAround(age) * idle;
					if (resting) {
						m.yRot += Mth.sin(age * 0.035F) * 0.4F;
						m.xRot += Mth.sin(age * 0.023F) * 0.1F - 0.35F * yawn;
					}
					if (anim == CreatureAnim.ROAR) {
						m.xRot -= 0.5F * e;
						m.yRot += Mth.sin(age * 1.6F) * 0.12F * e;
					} else if (anim == CreatureAnim.BREATH) {
						m.xRot += 0.25F * e;
					} else if (anim == CreatureAnim.ATTACK) {
						// a beast lunges with its head; a fighter watches its swing
						m.xRot += 0.3F * strike(p);
					}
				}
				case JAW -> {
					float open = switch (anim) {
						case ATTACK -> 0.6F * Math.max(e, strike(p));
						case ROAR -> 0.8F * e;
						case BREATH -> 0.6F * Math.min(1.0F, p * 6.0F) * (p > 0.9F ? (1.0F - p) * 10.0F : 1.0F);
						case CAST, SUMMON -> 0.25F * e;
						default -> 0.0F;
					};
					m.xRot += open + 0.6F * yawn;
				}
				case ARM_RIGHT, ARM_LEFT -> {
					m.xRot += Mth.cos(walk * 0.6662F + (right ? Mth.PI : 0.0F)) * 1.1F * speed;
					m.zRot += (right ? 1 : -1) * (Mth.cos(age * 0.09F) * 0.05F + 0.05F + flinch * 0.8F);
					switch (anim) {
						case ATTACK -> {
							if (right) {
								m.xRot += swing(p);
							} else {
								// the off arm braces back while the other swings
								m.xRot += 0.5F * strike(p);
							}
						}
						case ROAR -> {
							m.zRot += (right ? 1 : -1) * 1.1F * e;
							m.xRot -= 0.5F * e;
						}
						case SLAM -> m.xRot += slamArms(p);
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
					} else if (anim == CreatureAnim.SLAM || anim == CreatureAnim.ATTACK) {
						// step into the blow: hips bend, the knee below takes it
						m.xRot -= 0.5F * (anim == CreatureAnim.SLAM ? slamCrouch(p) : strike(p)) * (right ? 1.0F : 0.4F);
					}
				}
				case LEG_QUAD -> {
					m.xRot += Mth.cos(stride + legPhase(part.index, gallop)) * Mth.lerp(gallop, 1.4F, 1.1F) * speed;
					if (this.plan.folds) {
						// tucked in flight
						m.xRot += 0.9F * (1.0F - s.fold);
					}
					if (anim == CreatureAnim.LEAP || anim == CreatureAnim.CHARGE) {
						m.xRot += (part.index < 2 ? -0.7F : 0.7F) * e;
					} else if (anim == CreatureAnim.ATTACK && part.index < 2) {
						// forepaws rake forward
						m.xRot -= 0.8F * strike(p);
					} else if (anim == CreatureAnim.ROAR) {
						m.xRot += (part.index < 2 ? -0.35F : 0.2F) * e;
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
					float flap = flying || this.plan.folds ? Mth.sin(age * 0.35F) * 0.65F : Mth.sin(age * 0.08F) * 0.08F;
					if (anim == CreatureAnim.DIVE) {
						flap = -0.9F * e;
					} else if (anim == CreatureAnim.ROAR || anim == CreatureAnim.CAST) {
						flap += 0.6F * e;
					}
					if (this.plan.folds) {
						flap *= 1.0F - s.fold;
						fold(m, part, right, s.fold);
					}
					m.zRot += right ? flap : -flap;
				}
				case TAIL -> {
					if (part.index == 0) {
						m.xRot += 0.35F * gallop + (anim == CreatureAnim.ROAR ? 0.4F * e : 0.0F);
					}
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
				case WHEEL -> m.yRot += s.wheel;
				case CAPE -> m.xRot += 0.08F + speed * 0.7F + Mth.sin(age * 0.1F) * 0.03F;
				case SHIN -> {
					if (part.index >= 10) {
						// biped knee: bends while the leg swings forward, and under a slam or a blow
						float phase = part.index == 10 ? 0.0F : Mth.PI;
						m.xRot += Math.max(0.0F, -Mth.sin(stride + phase)) * 0.9F * speed;
						if (anim == CreatureAnim.SLAM) {
							m.xRot += 1.0F * slamCrouch(p);
						} else if (anim == CreatureAnim.ATTACK) {
							m.xRot += 0.6F * strike(p) * (part.index == 10 ? 1.0F : 0.4F);
						}
					} else {
						if (!flying) {
							// front knees fold back, hind hocks forward, while the foot is off the ground
							float flex = Math.max(0.0F, -Mth.sin(stride + legPhase(part.index, gallop))) * Mth.lerp(gallop, 0.8F, 1.2F) * speed;
							m.xRot += part.index < 2 ? flex : -flex;
						}
						if (this.plan.folds) {
							m.xRot -= 1.2F * (1.0F - s.fold);
						}
					}
				}
				case EAR -> {
					float t = (age + part.index * 37.0F) % 90.0F;
					if (t < 6.0F) {
						m.zRot += Mth.sin(t / 6.0F * Mth.PI) * 0.35F * (part.index == 0 ? -1 : 1);
					}
				}
				default -> {
				}
			}
		}
		if (s.rest > 0.001F) {
			this.rest(s.rest);
		}
		if (s.stand > 0.001F) {
			this.rear(s.stand);
		}
	}

	/** Fast start, slow arrival: wind-ups that come to a hold, recoveries that settle. */
	private static float easeOut(final float t) {
		float u = 1.0F - Mth.clamp(t, 0.0F, 1.0F);
		return 1.0F - u * u * u;
	}

	/** Slow start, fast arrival: blows that accelerate into the hit. */
	private static float easeIn(final float t) {
		float u = Mth.clamp(t, 0.0F, 1.0F);
		return u * u * u;
	}

	/**
	 * A weapon arm's swing over an attack: raised high behind the head (easing into the top), brought down hard (accelerating into
	 * the hit), then settling back to rest.
	 */
	private static float swing(final float p) {
		if (p < STRIKE_WINDUP) {
			return -2.6F * easeOut(p / STRIKE_WINDUP);
		}
		if (p < STRIKE_HIT) {
			return -2.6F + 3.2F * easeIn((p - STRIKE_WINDUP) / (STRIKE_HIT - STRIKE_WINDUP));
		}
		return 0.6F * (1.0F - easeOut((p - STRIKE_HIT) / (1.0F - STRIKE_HIT)));
	}

	/** 0 during the wind-up, rising (accelerating) to 1 at the hit and settling off after it. */
	private static float strike(final float p) {
		if (p < STRIKE_WINDUP) {
			return 0.0F;
		}
		if (p < STRIKE_HIT) {
			return easeIn((p - STRIKE_WINDUP) / (STRIKE_HIT - STRIKE_WINDUP));
		}
		return 1.0F - easeOut((p - STRIKE_HIT) / (1.0F - STRIKE_HIT));
	}

	/** Shoulders turn away during the wind-up and whip through the swing. */
	private static float strikeTwist(final float p) {
		if (p < STRIKE_WINDUP) {
			return 0.3F * easeOut(p / STRIKE_WINDUP);
		}
		if (p < STRIKE_HIT) {
			return 0.3F - 0.65F * easeIn((p - STRIKE_WINDUP) / (STRIKE_HIT - STRIKE_WINDUP));
		}
		return -0.35F * (1.0F - easeOut((p - STRIKE_HIT) / (1.0F - STRIKE_HIT)));
	}

	/**
	 * A slam's arms: raised overhead over the first half (easing into the top), brought down hard, then settled back to rest by the
	 * end, so nothing pops when the animation stops.
	 */
	private static float slamArms(final float p) {
		if (p < 0.5F) {
			return -2.8F * easeOut(p / 0.5F);
		}
		float t = (p - 0.5F) / 0.5F;
		if (t < SLAM_DOWN) {
			return -2.8F + 3.6F * easeIn(t / SLAM_DOWN);
		}
		return 0.8F * (1.0F - easeOut((t - SLAM_DOWN) / (1.0F - SLAM_DOWN)));
	}

	/** Head yaw of an idle look-around: mostly still, a slow turn to one side every few seconds, then back. */
	private static float lookAround(final float age) {
		float cycle = age % 140.0F;
		if (cycle > 50.0F) {
			return 0.0F;
		}
		float side = ((int)(age / 140.0F) & 1) == 0 ? 1.0F : -0.8F;
		return Mth.sin(cycle / 50.0F * Mth.PI) * 0.3F * side;
	}

	/** How low a slam brings the body: nothing while the arms go up, the full crouch at the impact (half way), then up again. */
	private static float slamCrouch(final float p) {
		return p < 0.45F ? 0.0F : Mth.sin((p - 0.45F) / 0.55F * Mth.PI);
	}

	/** Leg phase offsets: a diagonal trot (0 with 3, 1 with 2) that turns into a bounding gallop (fronts, then hinds). */
	private static float legPhase(final int index, final float gallop) {
		float trot = index == 0 || index == 3 ? 0.0F : Mth.PI;
		float bound = switch (index) {
			case 0 -> 0.0F;
			case 1 -> 0.4F;
			case 2 -> Mth.PI;
			default -> Mth.PI + 0.4F;
		};
		return Mth.lerp(gallop, trot, bound);
	}

	/**
	 * A folded wing: the main panel (index 0) turns on edge, trailing edge down, and sweeps back along the body; the tip (index 1)
	 * turns over around its chord and lies back along the panel.
	 */
	private static void fold(final ModelPart m, final Part part, final boolean right, final float amount) {
		if (part.index == 0) {
			m.xRot -= Mth.HALF_PI * amount;
			m.yRot += (right ? 1.5F : -1.5F) * amount;
		} else {
			m.zRot += (Mth.PI - 0.12F) * amount;
		}
	}

	/**
	 * Tips the body by {@code angle} around its pivot and lowers it by {@code sink}; every other root-level part moves with it (legs
	 * keep their own angle, the rest turn by {@code follow} x the angle).
	 */
	private void tiltRoot(final float angle, final float sink, final float follow) {
		ModelPart bodyModel = this.parts.get(this.body);
		float sin = Mth.sin(angle);
		float cos = Mth.cos(angle);
		bodyModel.xRot += angle;
		bodyModel.y += sink;
		for (Map.Entry<Part, ModelPart> entry : this.parts.entrySet()) {
			Part part = entry.getKey();
			if (part.parent != null || part == this.body) {
				continue;
			}
			ModelPart m = entry.getValue();
			float dy = part.py - this.body.py;
			float dz = part.pz - this.body.pz;
			m.y += dy * cos - dz * sin - dy + sink;
			m.z += dy * sin + dz * cos - dz;
			if (part.role != BodyPlan.Role.LEG_QUAD) {
				m.xRot += angle * follow;
			}
		}
	}

	/** The plan's rest pose blended in by {@code t}: the body sits back or lies down and the root-level parts move with it. */
	private void rest(final float t) {
		if (this.body == null || this.plan.rest == BodyPlan.Rest.NONE) {
			return;
		}
		if (this.plan.rest == BodyPlan.Rest.LIE) {
			float sink = (this.legLength > 0.0F ? this.legLength * 0.6F : 1.5F) * t;
			for (Map.Entry<Part, ModelPart> entry : this.parts.entrySet()) {
				Part part = entry.getKey();
				ModelPart m = entry.getValue();
				if (part.parent == null) {
					m.y += sink;
				}
				if (part.role == BodyPlan.Role.LEG_QUAD) {
					m.zRot += (part.px > 0 ? -0.6F : 0.6F) * t;
					m.xRot = Mth.lerp(t, m.xRot, part.index < 2 ? -0.3F : 0.3F);
				} else if (part.role == BodyPlan.Role.LEG_SPIDER) {
					m.zRot += (part.index < 4 ? -0.35F : 0.35F) * t;
				} else if (part.role == BodyPlan.Role.HEAD) {
					m.xRot += 0.15F * t;
					m.z += 1.0F * t;
				}
			}
			return;
		}
		// SIT: haunches down, front legs straight, hind legs folded flat under the body, tail along the ground
		this.tiltRoot(SIT_TILT * t, this.legLength * 0.45F * t, 0.0F);
		for (Map.Entry<Part, ModelPart> entry : this.parts.entrySet()) {
			Part part = entry.getKey();
			ModelPart m = entry.getValue();
			if (part.role == BodyPlan.Role.LEG_QUAD && part.parent == null) {
				m.xRot = Mth.lerp(t, m.xRot, part.index < 2 ? -0.15F : -1.45F);
			} else if (part.role == BodyPlan.Role.SHIN) {
				m.xRot = Mth.lerp(t, m.xRot, part.index < 2 ? 0.0F : 2.6F);
			} else if (part.role == BodyPlan.Role.TAIL && part.index == 0) {
				m.xRot -= SIT_TILT * t;
			}
		}
	}

	/** Rearing (a mount about to jump): up on the hind legs, forelegs pawing with folded knees, the head thrown back. */
	private void rear(final float t) {
		if (this.body == null || this.plan.rig != BodyPlan.Rig.QUADRUPED || this.legLength <= 0.0F) {
			return;
		}
		Part hind = this.parts.keySet().stream().filter(p -> p.role == BodyPlan.Role.LEG_QUAD && p.index == 2 && p.parent == null).findFirst().orElse(null);
		if (hind == null) {
			return;
		}
		float angle = REAR_TILT * t;
		// lift so the hips stay where they are: the body turns around its hind legs
		float dy = hind.py - this.body.py;
		float dz = hind.pz - this.body.pz;
		float hipDrop = dy * Mth.cos(angle) - dz * Mth.sin(angle) - dy;
		this.tiltRoot(angle, -hipDrop, 0.6F);
		for (Map.Entry<Part, ModelPart> entry : this.parts.entrySet()) {
			Part part = entry.getKey();
			ModelPart m = entry.getValue();
			if (part.role == BodyPlan.Role.LEG_QUAD && part.parent == null) {
				m.xRot = Mth.lerp(t, m.xRot, part.index < 2 ? -0.9F + Mth.sin(part.index * 1.5F) * 0.3F : 0.25F);
			} else if (part.role == BodyPlan.Role.SHIN && part.index < 2) {
				m.xRot = Mth.lerp(t, m.xRot, 1.3F);
			}
		}
	}
}
