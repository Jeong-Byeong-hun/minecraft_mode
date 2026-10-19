package com.minecraftmode.test;

import com.minecraftmode.MinecraftMode;
import com.minecraftmode.companion.Companions;
import com.minecraftmode.dungeon.DungeonDimension;
import com.minecraftmode.dungeon.Dungeons;
import com.minecraftmode.enchantment.ArmorEnchantments;
import com.minecraftmode.job.JobClass;
import com.minecraftmode.job.JobProgression;
import com.minecraftmode.job.JobStats;
import com.minecraftmode.job.skill.SkillCaster;
import com.minecraftmode.job.weapon.JobWeapons;
import com.minecraftmode.jump.JumpCourse;
import com.minecraftmode.jump.JumpForest;
import com.minecraftmode.registry.ModAttachments;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerConnection;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * The Forest of Tenacity ({@code jump/JumpForest}): entering builds a private course in the dungeon dimension once its chunks are
 * loaded, the start is safe, every jump of the layout can be made and the built blocks leave its air free, mounts, elytra, pearls,
 * flying and movement skills are refused, nothing hurts, a fall goes back to the checkpoint, stepping on a checkpoint saves it, the
 * goal calls the finish listeners exactly once and sends the player back where they came from; leaving, a reconnect and /kill end
 * a climb safely too. Screenshots look up the tree from the start, from a middle stage and from the side.
 */
final class JumpChecks {
	private static final AtomicInteger FINISHES = new AtomicInteger();
	private static final JumpForest.Finish LISTENER = (player, course) -> FINISHES.incrementAndGet();

	static void run(final ClientGameTestContext context, final TestServerContext server, final TestServerConnection connection) {
		Vec3 home = server.computeOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			player.setGameMode(GameType.SURVIVAL);
			player.snapTo(0.5, -60, 0.5, 0.0F, 0.0F);
			player.setHealth(player.getMaxHealth());
			require(JumpCourse.tenacity().problems().isEmpty(), "every jump of the layout can be made: " + JumpCourse.tenacity().problems());
			require(!JumpForest.enter(player, "nowhere"), "an unknown course is refused");
			FINISHES.set(0);
			JumpForest.FINISHED.add(LISTENER);
			require(JumpForest.enter(player, JumpForest.TENACITY), "the forest opens");
			require(!JumpForest.enter(player, JumpForest.TENACITY), "one course at a time");
			return player.position();
		});
		waitIn(context, server, connection);
		String built = server.computeOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			ServerLevel level = (ServerLevel)player.level();
			require(DungeonDimension.is(level), "the course is in the dungeon dimension");
			BlockPos origin = JumpForest.origin(player);
			JumpCourse layout = JumpCourse.tenacity();
			Vec3 start = JumpForest.spot(origin, layout, 0);
			require(player.position().distanceTo(start) < 0.6, "the player starts on the start circle, at " + player.position() + " not " + start);
			BlockPos feet = BlockPos.containing(start);
			require(level.getBlockState(feet.below()).is(Blocks.MOSSY_COBBLESTONE), "the start stands on mossy cobblestone");
			require(level.getBlockState(feet).getCollisionShape(level, feet).isEmpty() && level.getBlockState(feet.above()).getCollisionShape(level, feet.above()).isEmpty(),
				"the start has room to stand");
			require(level.getBlockState(origin.offset(JumpCourse.CENTER, 20, JumpCourse.CENTER)).is(Blocks.OAK_LOG), "the trunk stands in the middle");
			BlockPos goal = BlockPos.containing(JumpForest.goal(origin, layout)).below();
			require(level.getBlockState(goal).is(Blocks.GOLD_BLOCK), "the golden goal is on the crown");
			// the built course leaves free every block of air the layout needs to stand and jump
			int blocked = 0;
			String first = "";
			for (long key : layout.reserved) {
				BlockPos p = origin.offset((int)(key >> 32), (int)(short)(key >> 16 & 0xFFFF), (int)(key & 0xFFFF));
				// the climbs themselves (a ladder has a thin collision box) are what the player is meant to hold
				if (!level.getBlockState(p).getCollisionShape(level, p).isEmpty() && !level.getBlockState(p).is(BlockTags.CLIMBABLE)) {
					blocked++;
					first = first.isEmpty() ? p.subtract(origin) + " " + level.getBlockState(p) : first;
				}
			}
			require(blocked == 0, blocked + " blocks stand where a player needs air, first " + first);
			for (int i = 1; i < layout.checkpoints.size(); i++) {
				BlockPos spot = BlockPos.containing(JumpForest.spot(origin, layout, i));
				require(level.getBlockState(spot.below()).is(Blocks.VERDANT_FROGLIGHT), "checkpoint " + i + " glows");
			}
			return layout.pads.size() + " footholds, " + layout.climbs.size() + " climbs, goal at +" + layout.top + ", " + layout.reserved.size() + " air blocks kept";
		});
		MinecraftMode.LOGGER.info("[town] jump course: {}", built);
		context.waitTicks(40);
		look(context, server, connection, 0);
		context.takeScreenshot("town_jump_start");

		// fair play: no mount, no gliding, no pearls, no flying, no movement skills, no damage
		server.runOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			ServerLevel level = (ServerLevel)player.level();
			Companions.learn(player, false, "storm_griffin");
			require(!Companions.summonMount(player, "storm_griffin") && player.getVehicle() == null, "the flying mount stays home");
			player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.ELYTRA));
			player.setOnGround(false);
			require(!player.tryToStartFallFlying() && !player.isFallFlying(), "no gliding in the forest");
			player.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
			// Leap V boots (+50% jump strength) are worn but do nothing here
			ItemStack boots = new ItemStack(Items.IRON_BOOTS);
			boots.enchant(s.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(ArmorEnchantments.LEAP.key()), 5);
			player.setItemSlot(EquipmentSlot.FEET, boots);
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.ENDER_PEARL, 4));
			require(UseItemCallback.EVENT.invoker().interact(player, level, InteractionHand.MAIN_HAND) == InteractionResult.FAIL, "ender pearls are refused");
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.CHORUS_FRUIT));
			require(UseItemCallback.EVENT.invoker().interact(player, level, InteractionHand.MAIN_HAND) == InteractionResult.FAIL, "chorus fruit is refused");
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.OAK_PLANKS, 8));
			require(UseItemCallback.EVENT.invoker().interact(player, level, InteractionHand.MAIN_HAND) == InteractionResult.FAIL, "blocks cannot be placed");
			JobProgression.set(player, JobProgression.get(player).withJob(JobClass.WARRIOR, 1).withProgress(12, 0));
			JobProgression.set(player, JobProgression.get(player).withMana(JobStats.maxMana(player)));
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(JobWeapons.item(JobWeapons.def("iron_greatsword"))));
			Vec3 before = player.position();
			require(SkillCaster.tryCast(player, 2) == SkillCaster.Result.NO_SKILL && player.position().equals(before), "a dash cannot carry the player up");
			require(SkillCaster.tryCast(player, 1) == SkillCaster.Result.OK, "a skill that does not move still works");
			player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
			player.getAbilities().mayfly = true;
			player.getAbilities().flying = true;
			float health = player.getHealth();
			player.hurtServer(level, level.damageSources().fall(), 10.0F);
			player.hurtServer(level, level.damageSources().generic(), 10.0F);
			require(player.getHealth() == health, "nothing hurts in the forest");
		});
		context.waitTicks(2);
		server.runOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			require(!player.getAbilities().mayfly && !player.getAbilities().flying, "survival players cannot fly in the forest");
			AttributeInstance jump = player.getAttribute(Attributes.JUMP_STRENGTH);
			require(jump.getModifier(JumpForest.LEVELER) != null && Math.abs(jump.getValue() - jump.getBaseValue()) < 1.0E-4,
				"Leap boots do not raise jumps in the forest: " + jump.getValue() + " vs base " + jump.getBaseValue());
			// stepping on the first checkpoint saves it
			ServerLevel level = (ServerLevel)player.level();
			Vec3 spot = JumpForest.spot(JumpForest.origin(player), JumpCourse.tenacity(), 1);
			player.teleportTo(level, spot.x, spot.y, spot.z, Set.of(), 0.0F, 0.0F, true);
		});
		context.waitTicks(10);
		server.runOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			require(JumpForest.checkpoint(player) == 1, "standing on the glowing moss saves checkpoint 1, got " + JumpForest.checkpoint(player));
			// a fall far below it: back on the checkpoint, unhurt
			ServerLevel level = (ServerLevel)player.level();
			BlockPos origin = JumpForest.origin(player);
			Vec3 start = JumpForest.spot(origin, JumpCourse.tenacity(), 0);
			player.setHealth(player.getMaxHealth());
			player.teleportTo(level, start.x, start.y + 2, start.z, Set.of(), 0.0F, 0.0F, true);
		});
		context.waitTicks(5);
		server.runOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			Vec3 spot = JumpForest.spot(JumpForest.origin(player), JumpCourse.tenacity(), 1);
			require(JumpForest.falls(player) == 1, "the fall counts, got " + JumpForest.falls(player));
			require(player.position().distanceTo(spot) < 1.0, "a fall goes back to the checkpoint, at " + player.position() + " not " + spot);
			require(player.getHealth() == player.getMaxHealth(), "the fall did not hurt");
		});

		// pictures: from a middle stage, and the whole tree from the side
		look(context, server, connection, 3);
		context.takeScreenshot("town_jump_mid");
		server.runOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			BlockPos origin = JumpForest.origin(player);
			player.setGameMode(GameType.SPECTATOR);
			player.teleportTo((ServerLevel)player.level(), origin.getX() + 22.5, origin.getY() + 52, origin.getZ() + 2.5, Set.of(), 0.0F, 0.0F, true);
		});
		context.waitTicks(30);
		server.runOnServer(s -> {
			BlockPos origin = JumpForest.origin(connection.getServerPlayer());
			connection.getServerPlayer().lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES,
				new Vec3(origin.getX() + JumpCourse.CENTER, origin.getY() + 40, origin.getZ() + JumpCourse.CENTER));
		});
		context.waitTicks(10);
		context.runOnClient(minecraft -> {
			minecraft.gui.hud.getChat().clearMessages(false);
			minecraft.gui.hud.clearTitles();
		});
		context.takeScreenshot("town_jump_side");

		// the goal: the listeners hear it once, and the player goes home a few seconds later
		server.runOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			player.setGameMode(GameType.SURVIVAL);
			Vec3 goal = JumpForest.goal(JumpForest.origin(player), JumpCourse.tenacity());
			player.teleportTo((ServerLevel)player.level(), goal.x, goal.y, goal.z, Set.of(), 0.0F, 0.0F, true);
		});
		context.waitTicks(10);
		server.runOnServer(s -> require(FINISHES.get() == 1, "reaching the goal calls the listeners once, got " + FINISHES.get()));
		context.waitTicks(JumpForest.RETURN_TICKS + 10);
		server.runOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			require(FINISHES.get() == 1, "the goal counts once, got " + FINISHES.get());
			require(!JumpForest.busy(player) && !DungeonDimension.is(player.level()), "the player is sent back after the goal");
			require(player.position().distanceTo(home) < 1.5, "back where they came from: " + player.position() + " not " + home);
			require(player.getAttached(ModAttachments.JUMP_RETURN) == null, "the return point is forgotten");
			AttributeInstance jump = player.getAttribute(Attributes.JUMP_STRENGTH);
			require(jump.getModifier(JumpForest.LEVELER) == null && jump.getValue() > jump.getBaseValue() * 1.4, "the boots jump high again at home: " + jump.getValue());
			player.setItemSlot(EquipmentSlot.FEET, ItemStack.EMPTY);
			// a course still being prepared stops on a character switch (Characters.switchTo cancels it)
			require(JumpForest.enter(player, JumpForest.TENACITY), "the forest opens again");
			JumpForest.cancel(player);
			require(!JumpForest.busy(player) && !DungeonDimension.is(player.level()), "cancelled while preparing");
			// entering in mid-air (as from a flying mount) saves the ground under the player as the way back
			player.teleportTo(home.x, home.y + 6, home.z);
			player.setOnGround(false);
			require(JumpForest.enter(player, JumpForest.TENACITY), "the forest opens from mid-air");
		});

		// /dungeon leave (the pause menu and the return scroll take the same way out)
		waitIn(context, server, connection);
		server.runOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			require(Math.abs(player.getAttached(ModAttachments.JUMP_RETURN).y() - home.y) < 0.01, "the way back is on the ground, not in the air: " + player.getAttached(ModAttachments.JUMP_RETURN));
			// /dungeon leave (and the return scroll) end a climb like the pause menu does
			require(Dungeons.leave(player) && !JumpForest.busy(player), "/dungeon leave ends the climb");
			require(!DungeonDimension.is(player.level()) && player.position().distanceTo(home) < 1.5, "/dungeon leave goes back to the saved point, not the plaza: " + player.position());
			require(!player.hasEffect(MobEffects.SLOW_FALLING), "standing on ground needs no slow falling");
			require(JumpForest.enter(player, JumpForest.TENACITY), "and in again");
		});
		// a reconnect (the server forgot the climb) and /kill end it too
		waitIn(context, server, connection);
		server.runOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			require(player.getAttached(ModAttachments.JUMP_RETURN) != null, "the return point is saved with the player");
			JumpForest.onJoin(player);
			require(!JumpForest.busy(player) && !DungeonDimension.is(player.level()) && player.position().distanceTo(home) < 1.5, "a reconnect comes back home");
			require(JumpForest.enter(player, JumpForest.TENACITY), "and in once more");
		});
		waitIn(context, server, connection);
		server.runOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			player.kill((ServerLevel)player.level());
			require(player.isAlive(), "dying in the forest is caught");
		});
		context.waitTicks(3);
		server.runOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			require(!JumpForest.busy(player) && !DungeonDimension.is(player.level()) && player.position().distanceTo(home) < 1.5, "/kill sends the climber home");
			require(FINISHES.get() == 1, "only the goal finishes a course");
			JumpForest.FINISHED.remove(LISTENER);
			player.setHealth(player.getMaxHealth());
		});
	}

	/** Waits until the player stands in a freshly built course. */
	private static void waitIn(final ClientGameTestContext context, final TestServerContext server, final TestServerConnection connection) {
		for (int i = 0; i < 80; i++) {
			context.waitTicks(5);
			if (server.computeOnServer(s -> JumpForest.inCourse(connection.getServerPlayer()))) {
				return;
			}
		}
		throw new AssertionError("the course was not built within 20 s");
	}

	/** Puts the player on checkpoint {@code index} looking up the tree along the path, and waits for the view to settle. */
	private static void look(final ClientGameTestContext context, final TestServerContext server, final TestServerConnection connection, final int index) {
		server.runOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			BlockPos origin = JumpForest.origin(player);
			JumpCourse layout = JumpCourse.tenacity();
			Vec3 spot = JumpForest.spot(origin, layout, index);
			// toward the trunk and up it: the footholds wind round it on both sides
			double dx = origin.getX() + JumpCourse.CENTER + 0.5 - spot.x;
			double dz = origin.getZ() + JumpCourse.CENTER + 0.5 - spot.z;
			float yaw = (float)Math.toDegrees(Math.atan2(-dx, dz));
			// higher up the trunk is right beside a checkpoint: look along the path instead, half towards the trunk
			if (index > 0) {
				int[] d = JumpCourse.direction(JumpCourse.side(layout.pads.get(layout.checkpoints.get(index)).arc()));
				float along = (float)Math.toDegrees(Math.atan2(-d[0], d[1]));
				yaw = along + net.minecraft.util.Mth.wrapDegrees(yaw - along) * 0.35F;
			}
			player.teleportTo((ServerLevel)player.level(), spot.x, spot.y, spot.z, Set.of(), yaw, index > 0 ? -12.0F : -42.0F, true);
		});
		context.waitTicks(70);
		context.runOnClient(minecraft -> {
			minecraft.gui.hud.getChat().clearMessages(false);
			minecraft.gui.hud.clearTitles();
		});
	}

	private static void require(final boolean condition, final String message) {
		if (!condition) {
			throw new AssertionError(message);
		}
	}

	private JumpChecks() {
	}
}
