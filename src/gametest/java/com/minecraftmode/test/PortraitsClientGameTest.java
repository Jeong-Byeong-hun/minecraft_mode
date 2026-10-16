package com.minecraftmode.test;

import com.minecraftmode.MinecraftMode;
import com.minecraftmode.companion.Companions;
import com.minecraftmode.companion.MountEntity;
import com.minecraftmode.companion.PetEntity;
import com.minecraftmode.entity.CreatureAnim;
import com.minecraftmode.entity.CreatureMob;
import com.minecraftmode.entity.named.NamedMobs;
import com.minecraftmode.raid.BossDef;
import com.minecraftmode.raid.RaidBosses;
import com.minecraftmode.client.creature.BodyPlan;
import com.minecraftmode.client.creature.CreaturePlans;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerConnection;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

/**
 * Manual (run with {@code -Ptests=portraits}): every creature model alone on a flat world, seen from its left side without the
 * HUD, for reviewing body plans and poses. Pets also resting, ground mounts rearing, named monsters mid-attack and raid bosses
 * mid-roar. Shots are named {@code portrait_<entity id>[_rest|_rear|_attack|_roar]} under build/run/clientGameTest/screenshots.
 */
public class PortraitsClientGameTest implements FabricClientGameTest {
	/** Every subject faces west: its left flank toward a camera standing south of it, head to the right of the picture. */
	private static final float FACING = -90.0F;
	private static final double X = 40.5;
	private static final double Z = 0.5;

	private enum Pose {
		STAND(""),
		REST("_rest"),
		REAR("_rear"),
		ATTACK("_attack"),
		ROAR("_roar");

		final String suffix;

		Pose(final String suffix) {
			this.suffix = suffix;
		}
	}

	@Override
	public void runTest(final ClientGameTestContext context) {
		if (TestSelection.skip(this)) {
			return;
		}
		try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
			TestServerConnection connection = singleplayer.getConnection();
			TestServerContext server = singleplayer.getServer();
			connection.waitForChunksRender();
			server.runCommand("gamerule send_command_feedback false");
			server.runCommand("gamerule spawn_mobs false");
			server.runCommand("time set noon");
			server.runCommand("weather clear");
			server.runCommand("gamemode creative @p");
			server.runCommand("effect give @p resistance 999999 4 true");
			context.runOnClient(minecraft -> {
				if (!minecraft.gui.hud.isHidden()) {
					minecraft.gui.hud.toggle();
				}
			});
			for (Companions.PetDef def : Companions.pets()) {
				portrait(context, server, connection, "pet_" + def.id(), def.flying() ? 1.0 : 0.0, Pose.STAND);
				portrait(context, server, connection, "pet_" + def.id(), def.flying() ? 1.0 : 0.0, Pose.REST);
			}
			for (Companions.MountDef def : Companions.mounts()) {
				portrait(context, server, connection, "mount_" + def.id(), 0.0, Pose.STAND);
				if (!def.flying() && !def.id().equals("dune_raptor")) {
					portrait(context, server, connection, "mount_" + def.id(), 0.0, Pose.REAR);
				}
			}
			NamedMobs.all().forEach(def -> {
				portrait(context, server, connection, def.id(), 0.0, Pose.STAND);
				portrait(context, server, connection, def.id(), 0.0, Pose.ATTACK);
			});
			for (BossDef def : RaidBosses.all()) {
				portrait(context, server, connection, def.id(), 0.0, Pose.STAND);
				portrait(context, server, connection, def.id(), 0.0, Pose.ROAR);
			}
			context.runOnClient(minecraft -> {
				if (minecraft.gui.hud.isHidden()) {
					minecraft.gui.hud.toggle();
				}
			});
			WorldClose.prepare(context, server);
		}
	}

	/** The largest side of the model's rest pose in blocks (pivots added up, rotations left out, like the datagen preview). */
	private static double span(final String id) {
		BodyPlan plan = CreaturePlans.get(id);
		if (plan == null) {
			return 0.0;
		}
		Map<String, float[]> origin = new HashMap<>();
		float[] min = {Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE};
		float[] max = {-Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE};
		for (BodyPlan.Part part : plan.parts) {
			float[] parent = part.parent == null ? new float[3] : origin.get(part.parent);
			float[] o = {parent[0] + part.px, parent[1] + part.py, parent[2] + part.pz};
			origin.put(part.name, o);
			for (BodyPlan.Box box : part.boxes) {
				float[] lo = {o[0] + box.x(), o[1] + box.y(), o[2] + box.z()};
				float[] size = {box.w(), box.h(), box.d()};
				for (int k = 0; k < 3; k++) {
					min[k] = Math.min(min[k], lo[k]);
					max[k] = Math.max(max[k], lo[k] + size[k]);
				}
			}
		}
		return Math.max(max[0] - min[0], Math.max(max[1] - min[1], max[2] - min[2])) * plan.scale / 16.0;
	}

	/** Spawns {@code id} alone (no AI) in {@code pose}, steps back as far as its size needs and takes the picture. */
	private static void portrait(final ClientGameTestContext context, final TestServerContext server, final TestServerConnection connection, final String id,
		final double lift, final Pose pose) {
		double[] size = server.computeOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			player.level().getEntitiesOfClass(Entity.class, player.getBoundingBox().inflate(80), e -> e instanceof Mob).forEach(Entity::discard);
			Entity e = BuiltInRegistries.ENTITY_TYPE.getValue(MinecraftMode.id(id)).create(player.level(), EntitySpawnReason.COMMAND);
			if (e instanceof PetEntity pet) {
				pet.setOwner(player.getUUID());
				pet.setResting(pose == Pose.REST);
			} else if (e instanceof MountEntity mount) {
				mount.setup(player);
				if (pose == Pose.REAR) {
					mount.setStanding(200);
				}
			}
			((Mob)e).setNoAi(true);
			((Mob)e).setPersistenceRequired();
			e.snapTo(X, -60 + lift, Z, FACING, 0.0F);
			((LivingEntity)e).setYBodyRot(FACING);
			((LivingEntity)e).setYHeadRot(FACING);
			player.level().addFreshEntity(e);
			return new double[] {e.getBbWidth(), e.getBbHeight()};
		});
		// frame the whole model, which reaches well past its hitbox (tails, wings, weapons)
		double distance = Math.max(1.3, Math.max(span(id), Math.max(size[0], size[1])) * 0.95 + 0.8);
		double centre = -60 + lift + size[1] * 0.5;
		double eyeHeight = centre + distance * 0.15;
		float pitch = (float)Math.toDegrees(Math.atan2(eyeHeight - centre, distance));
		// the player stands below the eye: creative flight keeps them where they are put
		server.runOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			player.getAbilities().flying = true;
			player.onUpdateAbilities();
		});
		server.runCommand(String.format(Locale.ROOT, "tp @p %.2f %.2f %.2f 180 %.1f", X, eyeHeight - 1.62, Z + distance, pitch));
		context.waitTicks(20);
		if (pose == Pose.ATTACK || pose == Pose.ROAR) {
			CreatureAnim anim = pose == Pose.ATTACK ? CreatureAnim.ATTACK : CreatureAnim.ROAR;
			server.runOnServer(s -> connection.getServerPlayer().level().getEntitiesOfClass(CreatureMob.class,
				connection.getServerPlayer().getBoundingBox().inflate(80)).forEach(m -> m.playAnim(anim)));
			// the middle of the move, where the pose is strongest
			context.waitTicks(anim.ticks() / 2);
		}
		context.takeScreenshot("portrait_" + id + pose.suffix);
	}
}
