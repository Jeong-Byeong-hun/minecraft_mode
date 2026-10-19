package com.minecraftmode.jump;

import com.minecraftmode.MinecraftMode;
import com.minecraftmode.bag.Bags;
import com.minecraftmode.character.Characters;
import com.minecraftmode.character.Lobby;
import com.minecraftmode.city.CityZone;
import com.minecraftmode.companion.Companions;
import com.minecraftmode.dungeon.DungeonDimension;
import com.minecraftmode.jump.JumpCourse.Pad;
import com.minecraftmode.market.AuctionHouse;
import com.minecraftmode.raid.RaidDimension;
import com.minecraftmode.registry.ModAttachments;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BoatItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Jump courses (MapleStory-style jump quests): {@link #TENACITY} is the Forest of Tenacity (끈기의 숲). A player enters through
 * {@link #enter}; reaching the end calls every {@link #FINISHED} listener (town quests count it there).
 *
 * <p>Each player climbs alone, in a private copy of the course built fresh in the dungeon dimension (slots along x =
 * {@link #ORIGIN_X}, far from the dungeon runs at x = 0 and the trial hunts at x = -4096). Entering asks for the course's chunks
 * with a loading ticket and builds once they are all there (never a blocking chunk load), then takes the player to the start.
 * Inside: footholds up a giant tree ({@link JumpCourse}), a checkpoint at the end of each of five stages (standing on its glowing
 * moss saves it), and a fall below the current checkpoint brings the player back to it at once. Nothing hurts there. Standing on the
 * golden goal on the crown finishes the course: the listeners are called once, and a few seconds later the player goes back where
 * they came from. The pause menu's "return home" ({@link #leave}), logging out (back home on the next join, the return point is
 * saved in {@link ModAttachments#JUMP_RETURN}) and dying end the climb too.
 *
 * <p>Fair play: no mounts, pets, elytra gliding, flying, ender pearls, chorus fruit, wind charges, tridents (riptide), fireworks,
 * placing or breaking blocks, levitation, jump boost or slow falling, and no skill or innate ability that moves its user (refused
 * with a message, see {@link #refusesMove}). Monsters never spawn in the dungeon dimension.
 */
public final class JumpForest {
	/** The Forest of Tenacity. Saved quest key: never rename. */
	public static final String TENACITY = "tenacity";

	/** Called on the server thread when {@code player} reaches the end of {@code course}. */
	public interface Finish {
		void finished(ServerPlayer player, String course);
	}

	public static final List<Finish> FINISHED = new CopyOnWriteArrayList<>();

	/** Where a player in a course goes back to. Saved with the player while they climb. */
	public record Return(String dimension, double x, double y, double z, float yRot) {
		public static final Codec<Return> CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.STRING.fieldOf("dimension").forGetter(Return::dimension),
			Codec.DOUBLE.fieldOf("x").forGetter(Return::x),
			Codec.DOUBLE.fieldOf("y").forGetter(Return::y),
			Codec.DOUBLE.fieldOf("z").forGetter(Return::z),
			Codec.FLOAT.optionalFieldOf("y_rot", 0.0F).forGetter(Return::yRot)
		).apply(i, Return::new));

		static Return of(final ServerPlayer player) {
			return new Return(player.level().dimension().identifier().toString(), player.getX(), player.getY(), player.getZ(), player.getYRot());
		}
	}

	/** Courses sit far west of everything else in the dungeon dimension, one slot every {@link #SPACING} blocks north to south. */
	public static final int ORIGIN_X = -8192;
	public static final int SPACING = 64;
	private static final int MAX_SLOTS = 32;
	/** Ticks to wait for the course's chunks before giving up. */
	private static final int LOAD_TICKS = 20 * 30;
	/** Ticks between reaching the goal and going home. */
	public static final int RETURN_TICKS = 60;
	/** How far under the current checkpoint's footing a fall counts as a fall. */
	public static final int FALL_DEPTH = 3;
	/** Entities the course spawns (its signs) carry this tag. */
	public static final String TAG = "minecraft_mode.jump";
	private static final TicketType TICKET = Registry.register(BuiltInRegistries.TICKET_TYPE, MinecraftMode.id("jump_course"),
		new TicketType(TicketType.NO_TIMEOUT, TicketType.FLAG_LOADING));

	private static final Map<UUID, Run> RUNS = new HashMap<>();

	private enum State {
		LOADING, CLIMBING, FINISHED, LEAVING
	}

	/** One player's climb. */
	private static final class Run {
		final UUID player;
		final String course;
		final int slot;
		final BlockPos origin;
		final JumpCourse layout;
		final Return back;
		final List<ChunkPos> chunks = new ArrayList<>();
		/** This climb's own tag on its signs: signs of an earlier climb in the slot that load back from disk lack it and go. */
		final String tag = TAG + "." + UUID.randomUUID();
		State state = State.LOADING;
		int timer;
		int checkpoint;
		int falls;
		int warned;

		Run(final ServerPlayer player, final String course, final int slot, final JumpCourse layout) {
			this.player = player.getUUID();
			this.course = course;
			this.slot = slot;
			this.origin = new BlockPos(ORIGIN_X, DungeonDimension.FLOOR_Y, slot * SPACING);
			this.layout = layout;
			this.back = Return.of(player);
		}
	}

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(JumpForest::tick);
		ServerPlayerEvents.JOIN.register(JumpForest::onJoin);
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> server.execute(() -> {
			Run run = RUNS.remove(handler.player.getUUID());
			if (run != null) {
				end(server, null, run);
			}
		}));
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> RUNS.clear());
		// nothing hurts in the course (falls included); only what bypasses invulnerability (/kill) still lands
		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> !(entity instanceof ServerPlayer player && inCourse(player))
			|| source.is(DamageTypeTags.BYPASSES_INVULNERABILITY));
		UseItemCallback.EVENT.register((player, level, hand) -> {
			if (player instanceof ServerPlayer sp && bound(sp) && refusedItem(player.getItemInHand(hand))) {
				refuse(sp, "message.minecraft_mode.jump.no_item");
				return InteractionResult.FAIL;
			}
			return InteractionResult.PASS;
		});
		UseBlockCallback.EVENT.register((player, level, hand, hit) -> player instanceof ServerPlayer sp && bound(sp) ? InteractionResult.FAIL : InteractionResult.PASS);
		UseEntityCallback.EVENT.register((player, level, hand, entity, hit) -> player instanceof ServerPlayer sp && bound(sp) ? InteractionResult.FAIL
			: InteractionResult.PASS);
		AttackEntityCallback.EVENT.register((player, level, hand, entity, hit) -> player instanceof ServerPlayer sp && bound(sp) ? InteractionResult.FAIL
			: InteractionResult.PASS);
	}

	// ------------------------------------------------------------------ queries

	/** True while {@code player} is in a course (built and standing in it, or finishing). */
	public static boolean inCourse(final Player player) {
		Run run = RUNS.get(player.getUUID());
		return run != null && run.state != State.LOADING;
	}

	/** True while {@code player} is waiting for a course to be built or is in one. */
	public static boolean busy(final Player player) {
		return RUNS.containsKey(player.getUUID());
	}

	/** The course's corner (floor level) for {@code player}, or null outside one. */
	public static @Nullable BlockPos origin(final Player player) {
		Run run = RUNS.get(player.getUUID());
		return run == null || run.state == State.LOADING ? null : run.origin;
	}

	/** The checkpoint {@code player} would come back to (0 = the start), or -1 outside a course. */
	public static int checkpoint(final Player player) {
		Run run = RUNS.get(player.getUUID());
		return run == null ? -1 : run.checkpoint;
	}

	/** Falls (returns to a checkpoint) so far, or -1 outside a course. */
	public static int falls(final Player player) {
		Run run = RUNS.get(player.getUUID());
		return run == null ? -1 : run.falls;
	}

	/** Course rules apply to {@code player}: in a course and not in creative or spectator mode. */
	private static boolean bound(final Player player) {
		return inCourse(player) && !player.isCreative() && !player.isSpectator();
	}

	/**
	 * Whether a skill or ability that moves its user is refused now (in a course); tells the player why. {@code SkillCaster} and
	 * {@code ClassAbilities} ask before casting one.
	 */
	public static boolean refusesMove(final ServerPlayer player) {
		if (!bound(player)) {
			return false;
		}
		refuse(player, "message.minecraft_mode.jump.no_skill");
		return true;
	}

	/** Whether {@code player} may call a mount now; tells them why not. */
	public static boolean refusesMount(final ServerPlayer player) {
		if (!busy(player)) {
			return false;
		}
		refuse(player, "message.minecraft_mode.jump.no_mount");
		return true;
	}

	/** Items that would skip the climb or change the course. */
	public static boolean refusedItem(final ItemStack stack) {
		return stack.is(Items.ENDER_PEARL) || stack.is(Items.CHORUS_FRUIT) || stack.is(Items.WIND_CHARGE) || stack.is(Items.TRIDENT)
			|| stack.is(Items.FIREWORK_ROCKET) || stack.has(DataComponents.GLIDER) || stack.getItem() instanceof BucketItem
			|| stack.getItem() instanceof BoatItem || stack.getItem() instanceof SpawnEggItem
			// blocks never go down here, but berries, carrots and the like are still eaten
			|| stack.getItem() instanceof BlockItem && !stack.has(DataComponents.FOOD);
	}

	/** Whether {@code pos} in {@code level} is inside any course's box (the vine mixin keeps their vines from spreading). */
	public static boolean courseArea(final Level level, final BlockPos pos) {
		return DungeonDimension.is(level) && pos.getX() >= ORIGIN_X && pos.getX() < ORIGIN_X + JumpCourse.SIZE && pos.getZ() >= 0
			&& pos.getZ() < MAX_SLOTS * SPACING;
	}

	// ------------------------------------------------------------------ entering

	/** Sends {@code player} to the start of {@code course}; false (with a message to the player) when they cannot go now. */
	public static boolean enter(final ServerPlayer player, final String course) {
		if (!TENACITY.equals(course)) {
			return tell(player, "message.minecraft_mode.jump.unknown");
		}
		MinecraftServer server = player.level().getServer();
		ServerLevel level = DungeonDimension.level(server);
		if (busy(player)) {
			return tell(player, "message.minecraft_mode.jump.already");
		}
		if (level == null || Characters.choosing(player) || Lobby.is(player.level()) || RaidDimension.is(player.level())
			|| DungeonDimension.is(player.level()) || player.isSpectator() || !player.isAlive()) {
			return tell(player, "message.minecraft_mode.jump.busy");
		}
		int slot = freeSlot();
		if (slot < 0) {
			return tell(player, "message.minecraft_mode.jump.full");
		}
		Run run = new Run(player, course, slot, JumpCourse.tenacity());
		RUNS.put(player.getUUID(), run);
		// the course's chunks plus a ring around them (building touches the edges), loaded but never ticking
		ServerChunkCache cache = level.getChunkSource();
		int c0 = (run.origin.getX() >> 4) - 1;
		int c1 = (run.origin.getX() + JumpCourse.SIZE - 1 >> 4) + 1;
		int d0 = (run.origin.getZ() >> 4) - 1;
		int d1 = (run.origin.getZ() + JumpCourse.SIZE - 1 >> 4) + 1;
		for (int cx = c0; cx <= c1; cx++) {
			for (int cz = d0; cz <= d1; cz++) {
				ChunkPos pos = new ChunkPos(cx, cz);
				run.chunks.add(pos);
				cache.addTicketWithRadius(TICKET, pos, 0);
			}
		}
		player.sendSystemMessage(Component.translatable("message.minecraft_mode.jump.opening", name(course)).withStyle(ChatFormatting.DARK_GREEN));
		return true;
	}

	private static boolean loaded(final ServerLevel level, final Run run) {
		for (ChunkPos pos : run.chunks) {
			if (level.getChunkSource().getChunkNow(pos.x(), pos.z()) == null) {
				return false;
			}
		}
		return true;
	}

	/** Builds the course (its chunks are loaded) and takes the player to the start. */
	private static void open(final ServerLevel level, final ServerPlayer player, final Run run) {
		JumpForestBuilder.build(level, run.origin, run.layout);
		clearEntities(level, run);
		signs(level, run);
		release(level.getServer(), run);
		run.state = State.CLIMBING;
		player.setAttached(ModAttachments.JUMP_RETURN, run.back);
		if (player.getVehicle() != null) {
			player.stopRiding();
		}
		Companions.dismissPet(player);
		player.stopFallFlying();
		toCheckpoint(level, player, run);
		title(player, name(run.course).withStyle(ChatFormatting.GREEN), Component.translatable("message.minecraft_mode.jump.title_rules"));
		player.sendSystemMessage(Component.translatable("message.minecraft_mode.jump.entered", name(run.course)).withStyle(ChatFormatting.GREEN));
		player.sendSystemMessage(Component.translatable("message.minecraft_mode.jump.rules").withStyle(ChatFormatting.GRAY));
		level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0F, 0.8F);
	}

	/** The rules at the start and the stage names over the checkpoints, as floating text. */
	private static void signs(final ServerLevel level, final Run run) {
		JumpCourse layout = run.layout;
		Pad start = layout.pads.getFirst();
		int[] n = JumpCourse.normal(JumpCourse.side(1));
		int[] d = JumpCourse.direction(JumpCourse.side(1));
		long middle = JumpForestBuilder.middle(start);
		Vec3 rules = Vec3.atBottomCenterOf(run.origin.offset(JumpForestBuilder.x(middle) + n[0] * 4 + d[0] * 3, 3, JumpForestBuilder.z(middle) + n[1] * 4 + d[1] * 3));
		text(level, run, rules, Component.translatable("sign.minecraft_mode.jump.title", name(run.course)).withStyle(ChatFormatting.GREEN)
			.append("\n").append(stage(1, layout).withStyle(ChatFormatting.AQUA))
			.append("\n").append(Component.translatable("sign.minecraft_mode.jump.rules").withStyle(ChatFormatting.WHITE)), 0.7F);
		for (int i = 1; i < layout.checkpoints.size(); i++) {
			Pad pad = layout.pads.get(layout.checkpoints.get(i));
			long c = JumpForestBuilder.middle(pad);
			int[] out = JumpCourse.normal(JumpCourse.side(pad.arc()));
			Vec3 at = Vec3.atBottomCenterOf(run.origin.offset(JumpForestBuilder.x(c) + out[0] * 2, pad.y() + 3, JumpForestBuilder.z(c) + out[1] * 2));
			text(level, run, at, Component.translatable("sign.minecraft_mode.jump.checkpoint", i, layout.checkpoints.size() - 1).withStyle(ChatFormatting.AQUA)
				.append("\n").append(stage(i + 1, layout).withStyle(ChatFormatting.WHITE)), 1.0F);
		}
		Pad goal = layout.pads.get(layout.goalPad);
		text(level, run, Vec3.atBottomCenterOf(run.origin.offset(JumpCourse.CENTER, goal.y() + 3, JumpCourse.CENTER)),
			Component.translatable("sign.minecraft_mode.jump.goal").withStyle(ChatFormatting.GOLD), 1.6F);
	}

	private static void text(final ServerLevel level, final Run run, final Vec3 at, final Component text, final float scale) {
		Display.TextDisplay display = EntityTypes.TEXT_DISPLAY.create(level, EntitySpawnReason.COMMAND);
		if (display == null) {
			return;
		}
		display.setText(text);
		display.setBillboardConstraints(Display.BillboardConstraints.CENTER);
		display.setLineWidth(240);
		display.setTransformation(new com.mojang.math.Transformation(null, null, new org.joml.Vector3f(scale, scale, scale), null));
		display.setPos(at.x, at.y, at.z);
		display.addTag(TAG);
		display.addTag(run.tag);
		level.addFreshEntity(display);
	}

	// ------------------------------------------------------------------ leaving

	/**
	 * Takes {@code player} out of their course (or stops one still being prepared) and home; false when they are in none. The pause
	 * menu's "return home" calls this.
	 */
	public static boolean leave(final ServerPlayer player) {
		Run run = RUNS.get(player.getUUID());
		if (run == null) {
			return false;
		}
		player.sendSystemMessage(Component.translatable("message.minecraft_mode.jump.left").withStyle(ChatFormatting.YELLOW));
		end(player.level().getServer(), player, run);
		return true;
	}

	/** Dying in a course (only /kill gets through): back home on the next tick instead. Called by {@code Dungeons} first. */
	public static boolean fall(final ServerPlayer player) {
		Run run = RUNS.get(player.getUUID());
		if (run == null || run.state == State.LOADING || !DungeonDimension.is(player.level())) {
			return false;
		}
		player.setHealth(player.getMaxHealth());
		run.state = State.LEAVING;
		return true;
	}

	/** Ends {@code run}: the player (when given and still in the course) goes home; the course's signs and tickets go. */
	private static void end(final MinecraftServer server, final @Nullable ServerPlayer player, final Run run) {
		RUNS.remove(run.player);
		release(server, run);
		ServerLevel level = DungeonDimension.level(server);
		if (level != null && run.state != State.LOADING) {
			clearEntities(level, run);
		}
		if (player != null) {
			player.removeAttached(ModAttachments.JUMP_RETURN);
			if (run.state != State.LOADING) {
				sendHome(server, player, run.back);
			}
		}
	}

	private static void sendHome(final MinecraftServer server, final ServerPlayer player, final @Nullable Return back) {
		restoreAbilities(player);
		if (!DungeonDimension.is(player.level())) {
			return;
		}
		ServerLevel to = back == null ? null : level(server, back.dimension());
		Vec3 pos;
		float yaw;
		if (to != null && !DungeonDimension.is(to) && !RaidDimension.is(to) && !Lobby.is(to)) {
			pos = new Vec3(back.x(), back.y(), back.z());
			yaw = back.yRot();
		} else {
			to = server.overworld();
			BlockPos spawn = CityZone.isCityLevel(to) ? CityZone.spawn(CityZone.baseY(to)) : to.getRespawnData().pos();
			pos = Vec3.atBottomCenterOf(spawn);
			yaw = 180.0F;
		}
		player.teleportTo(to, pos.x, pos.y, pos.z, Set.of(), yaw, 0.0F, true);
		player.resetFallDistance();
		player.setDeltaMovement(Vec3.ZERO);
		player.clearFire();
	}

	private static @Nullable ServerLevel level(final MinecraftServer server, final String dimension) {
		Identifier id = Identifier.tryParse(dimension);
		return id == null ? null : server.getLevel(ResourceKey.create(Registries.DIMENSION, id));
	}

	/**
	 * A player who logged out in a course (or was there when the server stopped) comes back home. Game tests call it to stand in for
	 * a reconnect.
	 */
	public static void onJoin(final ServerPlayer player) {
		Return back = player.getAttached(ModAttachments.JUMP_RETURN);
		if (back == null) {
			return;
		}
		Run run = RUNS.remove(player.getUUID());
		if (run != null) {
			release(player.level().getServer(), run);
		}
		player.removeAttached(ModAttachments.JUMP_RETURN);
		sendHome(player.level().getServer(), player, back);
	}

	private static void release(final MinecraftServer server, final Run run) {
		ServerLevel level = DungeonDimension.level(server);
		if (level != null) {
			for (ChunkPos pos : run.chunks) {
				level.getChunkSource().removeTicketWithRadius(TICKET, pos, 0);
			}
		}
		run.chunks.clear();
	}

	private static void clearEntities(final ServerLevel level, final Run run) {
		AABB box = new AABB(run.origin.getX() - 2, run.origin.getY() + JumpForestBuilder.BOTTOM - 4, run.origin.getZ() - 2,
			run.origin.getX() + JumpCourse.SIZE + 2, run.origin.getY() + run.layout.top + JumpForestBuilder.ABOVE_TOP + 4, run.origin.getZ() + JumpCourse.SIZE + 2);
		ServerPlayer owner = level.getServer().getPlayerList().getPlayer(run.player);
		for (Entity e : level.getEntitiesOfClass(Entity.class, box, e -> !(e instanceof Player))) {
			// what the climber dropped goes back to them (the market mailbox when full or offline), never into the void
			if (e instanceof ItemEntity item && !item.getItem().isEmpty()) {
				if (owner != null) {
					Bags.giveOrMail(owner, item.getItem());
				} else {
					AuctionHouse.get(level.getServer()).sendItem(run.player, item.getItem().copy());
				}
			}
			e.discard();
		}
	}

	/** Signs of an earlier climb in this slot that loaded back from disk after the build (they lack this climb's tag). */
	private static void sweepSigns(final ServerLevel level, final Run run) {
		AABB box = new AABB(run.origin.getX(), run.origin.getY(), run.origin.getZ(), run.origin.getX() + JumpCourse.SIZE,
			run.origin.getY() + run.layout.top + JumpForestBuilder.ABOVE_TOP, run.origin.getZ() + JumpCourse.SIZE);
		for (Entity e : level.getEntitiesOfClass(Entity.class, box, e -> e.entityTags().contains(TAG) && !e.entityTags().contains(run.tag))) {
			e.discard();
		}
	}

	private static void restoreAbilities(final ServerPlayer player) {
		player.gameMode.getGameModeForPlayer().updatePlayerAbilities(player.getAbilities());
		player.onUpdateAbilities();
	}

	// ------------------------------------------------------------------ ticking

	private static void tick(final MinecraftServer server) {
		if (RUNS.isEmpty()) {
			return;
		}
		ServerLevel level = DungeonDimension.level(server);
		for (Run run : List.copyOf(RUNS.values())) {
			ServerPlayer player = server.getPlayerList().getPlayer(run.player);
			if (player == null || level == null) {
				end(server, null, run);
				continue;
			}
			run.timer++;
			switch (run.state) {
				case LOADING -> {
					if (Characters.choosing(player) || RaidDimension.is(player.level()) || DungeonDimension.is(player.level()) || Lobby.is(player.level())) {
						tell(player, "message.minecraft_mode.jump.cancelled");
						end(server, player, run);
					} else if (loaded(level, run)) {
						open(level, player, run);
					} else if (run.timer > LOAD_TICKS) {
						tell(player, "message.minecraft_mode.jump.failed");
						end(server, player, run);
					}
				}
				case CLIMBING -> climb(level, player, run);
				case FINISHED -> {
					keepSafe(player);
					if (run.timer >= RETURN_TICKS) {
						end(server, player, run);
					}
				}
				case LEAVING -> end(server, player, run);
			}
		}
	}

	private static void climb(final ServerLevel level, final ServerPlayer player, final Run run) {
		// walked out some other way (a teleport, an operator): the climb is over
		if (player.level() != level || !insideBox(run, player.position())) {
			RUNS.remove(run.player);
			release(level.getServer(), run);
			clearEntities(level, run);
			player.removeAttached(ModAttachments.JUMP_RETURN);
			restoreAbilities(player);
			return;
		}
		keepSafe(player);
		if (run.timer % 40 == 0) {
			sweepSigns(level, run);
		}
		JumpCourse layout = run.layout;
		double rx = player.getX() - run.origin.getX();
		double ry = player.getY() - run.origin.getY();
		double rz = player.getZ() - run.origin.getZ();
		int bx = (int)Math.floor(rx);
		int bz = (int)Math.floor(rz);
		int below = (int)Math.floor(ry - 0.2);
		if (player.onGround()) {
			Pad goal = layout.pads.get(layout.goalPad);
			if (below == goal.y() && Math.abs(bx - JumpCourse.CENTER) <= 1 && Math.abs(bz - JumpCourse.CENTER) <= 1) {
				finish(level, player, run);
				return;
			}
			for (int i = run.checkpoint + 1; i < layout.checkpoints.size(); i++) {
				Pad pad = layout.pads.get(layout.checkpoints.get(i));
				if (below == pad.y() && pad.contains(bx, bz)) {
					reach(level, player, run, i);
					break;
				}
			}
		}
		Pad saved = layout.pads.get(layout.checkpoints.get(run.checkpoint));
		boolean fell = !player.isSpectator() && (run.checkpoint > 0 ? ry < saved.y() + 1 - FALL_DEPTH : ry < -1);
		if (fell) {
			run.falls++;
			toCheckpoint(level, player, run);
			player.sendOverlayMessage(Component.translatable("message.minecraft_mode.jump.fell", run.checkpoint).withStyle(ChatFormatting.YELLOW));
			level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.CHICKEN_EGG, SoundSource.PLAYERS, 0.8F, 1.4F);
		}
	}

	/** The fair-play rules, every tick: no flying, gliding, riding, lifting effects or fall damage. */
	private static void keepSafe(final ServerPlayer player) {
		player.resetFallDistance();
		if (player.isCreative() || player.isSpectator()) {
			return;
		}
		if (player.getAbilities().mayfly || player.getAbilities().flying) {
			player.getAbilities().flying = false;
			player.getAbilities().mayfly = false;
			player.onUpdateAbilities();
		}
		if (player.isFallFlying()) {
			player.stopFallFlying();
			refuse(player, "message.minecraft_mode.jump.no_glide");
		}
		if (player.getVehicle() != null) {
			player.stopRiding();
			refuse(player, "message.minecraft_mode.jump.no_mount");
		}
		player.removeEffect(MobEffects.LEVITATION);
		player.removeEffect(MobEffects.JUMP_BOOST);
		player.removeEffect(MobEffects.SLOW_FALLING);
		if (player.tickCount % 20 == 0 && player.getFoodData().needsFood()) {
			player.getFoodData().setFoodLevel(20);
		}
	}

	private static void reach(final ServerLevel level, final ServerPlayer player, final Run run, final int checkpoint) {
		run.checkpoint = checkpoint;
		int stages = run.layout.checkpoints.size() - 1;
		title(player, Component.translatable("message.minecraft_mode.jump.title_checkpoint", checkpoint, stages).withStyle(ChatFormatting.AQUA),
			stage(checkpoint + 1, run.layout));
		level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 1.0F, 0.7F);
		level.sendParticles(ParticleTypes.HAPPY_VILLAGER, player.getX(), player.getY() + 0.5, player.getZ(), 20, 0.6, 0.4, 0.6, 0.0);
	}

	private static void finish(final ServerLevel level, final ServerPlayer player, final Run run) {
		run.state = State.FINISHED;
		run.timer = 0;
		title(player, Component.translatable("message.minecraft_mode.jump.title_cleared").withStyle(ChatFormatting.GOLD), name(run.course).withStyle(ChatFormatting.GREEN));
		player.sendSystemMessage(Component.translatable("message.minecraft_mode.jump.cleared", name(run.course), run.falls, RETURN_TICKS / 20).withStyle(ChatFormatting.GOLD));
		level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0F, 1.0F);
		level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, player.getX(), player.getY() + 1.0, player.getZ(), 60, 0.8, 1.0, 0.8, 0.3);
		for (Finish listener : FINISHED) {
			try {
				listener.finished(player, run.course);
			} catch (RuntimeException e) {
				MinecraftMode.LOGGER.error("A jump course listener failed for {}", player.getName().getString(), e);
			}
		}
	}

	/** Puts the player on their checkpoint (the start for 0), facing along the path. */
	private static void toCheckpoint(final ServerLevel level, final ServerPlayer player, final Run run) {
		Vec3 at = spot(run.origin, run.layout, run.checkpoint);
		Pad pad = run.layout.pads.get(run.layout.checkpoints.get(run.checkpoint));
		int[] d = JumpCourse.direction(JumpCourse.side(pad.arc() + 1));
		float yaw = (float)(Math.toDegrees(Math.atan2(-d[0], d[1])));
		player.teleportTo(level, at.x, at.y, at.z, Set.of(), yaw, 0.0F, true);
		player.resetFallDistance();
		player.setDeltaMovement(Vec3.ZERO);
	}

	/** Where a player stands on checkpoint {@code index} of a course built at {@code origin}. */
	public static Vec3 spot(final BlockPos origin, final JumpCourse layout, final int index) {
		Pad pad = layout.pads.get(layout.checkpoints.get(index));
		long c = index == 0 ? JumpCourse.xz(JumpCourse.ring(0, 0)[0], JumpCourse.ring(0, 0)[1]) : JumpForestBuilder.middle(pad);
		return new Vec3(origin.getX() + JumpForestBuilder.x(c) + 0.5, origin.getY() + pad.y() + 1, origin.getZ() + JumpForestBuilder.z(c) + 0.5);
	}

	/** Where the goal is: standing on the golden block in the crown's middle. */
	public static Vec3 goal(final BlockPos origin, final JumpCourse layout) {
		return new Vec3(origin.getX() + JumpCourse.CENTER + 0.5, origin.getY() + layout.pads.get(layout.goalPad).y() + 1, origin.getZ() + JumpCourse.CENTER + 0.5);
	}

	private static boolean insideBox(final Run run, final Vec3 pos) {
		return pos.x >= run.origin.getX() - 2 && pos.x <= run.origin.getX() + JumpCourse.SIZE + 2 && pos.z >= run.origin.getZ() - 2
			&& pos.z <= run.origin.getZ() + JumpCourse.SIZE + 2 && pos.y >= run.origin.getY() - 40 && pos.y <= run.origin.getY() + run.layout.top + 60;
	}

	// ------------------------------------------------------------------ helpers

	private static int freeSlot() {
		boolean[] used = new boolean[MAX_SLOTS];
		for (Run run : RUNS.values()) {
			used[run.slot] = true;
		}
		for (int i = 0; i < MAX_SLOTS; i++) {
			if (!used[i]) {
				return i;
			}
		}
		return -1;
	}

	/** Stage {@code stage}'s name (1-based); past the last stage, the way onto the crown. */
	private static net.minecraft.network.chat.MutableComponent stage(final int stage, final JumpCourse layout) {
		return Component.translatable(stage < layout.checkpoints.size() ? "sign.minecraft_mode.jump.stage." + stage : "sign.minecraft_mode.jump.stage.crown");
	}

	private static net.minecraft.network.chat.MutableComponent name(final String course) {
		return Component.translatable("jump.minecraft_mode." + course);
	}

	private static void title(final ServerPlayer player, final Component title, final Component subtitle) {
		player.connection.send(new ClientboundSetTitlesAnimationPacket(10, 50, 15));
		player.connection.send(new ClientboundSetSubtitleTextPacket(subtitle));
		player.connection.send(new ClientboundSetTitleTextPacket(title));
	}

	/** A refusal on the action bar, at most once a second per player. */
	private static void refuse(final ServerPlayer player, final String key) {
		Run run = RUNS.get(player.getUUID());
		if (run != null && player.tickCount - run.warned < 20 && run.warned != 0) {
			return;
		}
		if (run != null) {
			run.warned = player.tickCount;
		}
		player.sendOverlayMessage(Component.translatable(key).withStyle(ChatFormatting.RED));
	}

	private static boolean tell(final ServerPlayer player, final String key) {
		player.sendSystemMessage(Component.translatable(key).withStyle(ChatFormatting.YELLOW));
		return false;
	}

	private JumpForest() {
	}
}
