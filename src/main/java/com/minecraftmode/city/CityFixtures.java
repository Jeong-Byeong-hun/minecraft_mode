package com.minecraftmode.city;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EndPortalFrameBlock;
import net.minecraft.world.level.block.EnderChestBlock;
import net.minecraft.world.level.block.NetherPortalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/**
 * Things the capital offers everyone: a lit Nether portal and an active End portal in the keep courtyard (beside the raid and
 * dungeon gates), ender chests across the city ({@link #enderChests}) and the travel circle on the plaza that takes players to the
 * homestead plains and back ({@link #useWaystone}). The generator builds them with the city ({@link #build}); worlds made before
 * they existed get them at runtime ({@link #ensure}, every 100 ticks), and missing pieces are put back the same way.
 */
public final class CityFixtures {
	/** Nether portal frame: obsidian x -10..-7 at z -60, dy 0..4; the portal fills x -9..-8, dy 1..3 (axis X). */
	public static final int NETHER_X0 = -10;
	public static final int NETHER_X1 = -7;
	public static final int NETHER_Z = -60;
	/** End portal: frames around x 6..8, z -61..-59 (the active portal), at the courtyard floor. */
	public static final int END_X0 = 6;
	public static final int END_X1 = 8;
	public static final int END_Z0 = -61;
	public static final int END_Z1 = -59;
	/** The plaza travel circle (a lodestone) right behind the spawn point. */
	public static final BlockPos WAYSTONE = new BlockPos(0, 0, 17);
	private static final int COOLDOWN_TICKS = 60;
	private static final Map<UUID, Long> LAST_TRAVEL = new ConcurrentHashMap<>();

	/** Ender chests (dy 0) and the way they face: plaza, keep courtyard, Adventurers' Guild, market and the south square. */
	public static final Map<BlockPos, Direction> ENDER_CHESTS = Map.ofEntries(
		Map.entry(new BlockPos(-3, 0, 17), Direction.NORTH),
		Map.entry(new BlockPos(3, 0, 17), Direction.NORTH),
		Map.entry(new BlockPos(-3, 0, -17), Direction.SOUTH),
		Map.entry(new BlockPos(3, 0, -17), Direction.SOUTH),
		Map.entry(new BlockPos(-11, 0, -52), Direction.SOUTH),
		Map.entry(new BlockPos(11, 0, -52), Direction.SOUTH),
		Map.entry(new BlockPos(24, 0, 53), Direction.WEST),
		Map.entry(new BlockPos(24, 0, 65), Direction.WEST),
		Map.entry(new BlockPos(-8, 0, 77), Direction.NORTH),
		Map.entry(new BlockPos(-20, 0, 77), Direction.NORTH),
		Map.entry(new BlockPos(-6, 0, 84), Direction.SOUTH),
		Map.entry(new BlockPos(6, 0, 84), Direction.SOUTH)
	);

	/** Absolute positions of the ender chests in a world whose city floor is {@code base}. */
	public static List<BlockPos> enderChests(final int base) {
		return ENDER_CHESTS.keySet().stream().map(p -> p.above(base)).toList();
	}

	public static BlockPos netherPortal(final int base) {
		return new BlockPos(NETHER_X0 + 1, base + 1, NETHER_Z);
	}

	public static BlockPos endPortal(final int base) {
		return new BlockPos((END_X0 + END_X1) / 2, base, (END_Z0 + END_Z1) / 2);
	}

	public static BlockPos waystone(final int base) {
		return WAYSTONE.above(base);
	}

	// ------------------------------------------------------------------ building

	/** Everything above, clipped to the chunk {@code b} builds (city generator and {@link #ensure}). */
	static void build(final Build b) {
		netherPortal(b);
		endPortal(b);
		for (Map.Entry<BlockPos, Direction> chest : ENDER_CHESTS.entrySet()) {
			BlockPos p = chest.getKey();
			b.set(p.getX(), p.getY(), p.getZ(), Blocks.ENDER_CHEST.defaultBlockState().setValue(EnderChestBlock.FACING, chest.getValue()));
		}
		waystone(b, WAYSTONE.getX(), WAYSTONE.getZ());
	}

	private static void netherPortal(final Build b) {
		if (!b.touches(NETHER_X0 - 1, NETHER_Z - 1, NETHER_X1 + 1, NETHER_Z + 1)) {
			return;
		}
		b.fill(NETHER_X0 - 1, -1, NETHER_Z - 1, NETHER_X1 + 1, -1, NETHER_Z + 1, Blocks.POLISHED_BLACKSTONE_BRICKS);
		b.fill(NETHER_X0, 0, NETHER_Z, NETHER_X1, 0, NETHER_Z, Blocks.OBSIDIAN);
		b.fill(NETHER_X0, 4, NETHER_Z, NETHER_X1, 4, NETHER_Z, Blocks.OBSIDIAN);
		b.fill(NETHER_X0, 1, NETHER_Z, NETHER_X0, 3, NETHER_Z, Blocks.OBSIDIAN);
		b.fill(NETHER_X1, 1, NETHER_Z, NETHER_X1, 3, NETHER_Z, Blocks.OBSIDIAN);
		b.fill(NETHER_X0 + 1, 1, NETHER_Z, NETHER_X1 - 1, 3, NETHER_Z,
			Blocks.NETHER_PORTAL.defaultBlockState().setValue(NetherPortalBlock.AXIS, Direction.Axis.X));
	}

	private static void endPortal(final Build b) {
		if (!b.touches(END_X0 - 1, END_Z0 - 1, END_X1 + 1, END_Z1 + 1)) {
			return;
		}
		b.fill(END_X0 - 1, -1, END_Z0 - 1, END_X1 + 1, -1, END_Z1 + 1, Blocks.END_STONE_BRICKS);
		BlockState frame = Blocks.END_PORTAL_FRAME.defaultBlockState().setValue(EndPortalFrameBlock.HAS_EYE, true);
		for (int x = END_X0; x <= END_X1; x++) {
			b.set(x, 0, END_Z0 - 1, frame.setValue(EndPortalFrameBlock.FACING, Direction.SOUTH));
			b.set(x, 0, END_Z1 + 1, frame.setValue(EndPortalFrameBlock.FACING, Direction.NORTH));
		}
		for (int z = END_Z0; z <= END_Z1; z++) {
			b.set(END_X0 - 1, 0, z, frame.setValue(EndPortalFrameBlock.FACING, Direction.EAST));
			b.set(END_X1 + 1, 0, z, frame.setValue(EndPortalFrameBlock.FACING, Direction.WEST));
		}
		b.fill(END_X0, 0, END_Z0, END_X1, 0, END_Z1, Blocks.END_PORTAL);
	}

	/** A lodestone on chiseled blackstone (one column, so it never straddles two chunks). */
	static void waystone(final Build b, final int x, final int z) {
		b.set(x, -1, z, Blocks.CHISELED_POLISHED_BLACKSTONE);
		b.set(x, 0, z, Blocks.LODESTONE);
		b.set(x, 1, z, Blocks.AIR);
	}

	// ------------------------------------------------------------------ runtime

	/** Puts back whatever is missing (or builds the lot in a world from before), chunk by chunk as they are loaded. */
	public static void ensure(final ServerLevel level) {
		if (!CityZone.isCityLevel(level)) {
			return;
		}
		int base = CityZone.baseY(level);
		// only loaded chunks are looked at (reading an unloaded one would load it)
		repair(level, base, netherPortal(base), Blocks.NETHER_PORTAL.defaultBlockState());
		repair(level, base, endPortal(base), Blocks.END_PORTAL.defaultBlockState());
		repair(level, base, waystone(base), Blocks.LODESTONE.defaultBlockState());
		for (BlockPos p : enderChests(base)) {
			repair(level, base, p, Blocks.ENDER_CHEST.defaultBlockState());
		}
		Homestead.ensure(level);
	}

	private static void repair(final ServerLevel level, final int base, final BlockPos p, final BlockState expected) {
		if (!level.isLoaded(p) || level.getBlockState(p).is(expected.getBlock())) {
			return;
		}
		int cx = p.getX() >> 4;
		int cz = p.getZ() >> 4;
		Build b = new Build(level, base, cx << 4, cz << 4);
		build(b);
	}

	/** Right-click on a travel circle: the plaza one goes to the homestead plains, the homestead one back to the plaza. */
	public static InteractionResult useWaystone(final Player player, final Level level, final BlockPos pos) {
		if (!(level instanceof ServerLevel server) || !(player instanceof ServerPlayer sp) || !CityZone.isCityLevel(server)
			|| !level.getBlockState(pos).is(Blocks.LODESTONE)) {
			return InteractionResult.PASS;
		}
		int base = CityZone.baseY(server);
		Vec3 to;
		Component where;
		if (pos.equals(waystone(base))) {
			to = Homestead.arrival(server);
			where = Component.translatable("message.minecraft_mode.waystone.homestead");
		} else if (pos.equals(Homestead.waystone(base))) {
			to = Vec3.atBottomCenterOf(CityZone.spawn(base));
			where = Component.translatable("message.minecraft_mode.waystone.plaza");
		} else {
			return InteractionResult.PASS;
		}
		long now = server.getGameTime();
		Long last = LAST_TRAVEL.get(sp.getUUID());
		if (last != null && now - last < COOLDOWN_TICKS) {
			return InteractionResult.SUCCESS;
		}
		LAST_TRAVEL.put(sp.getUUID(), now);
		if (sp.getVehicle() != null) {
			sp.stopRiding();
		}
		server.sendParticles(ParticleTypes.PORTAL, sp.getX(), sp.getY(0.5), sp.getZ(), 30, 0.4, 0.8, 0.4, 0.4);
		sp.teleportTo(server, to.x, to.y, to.z, java.util.Set.of(), sp.getYRot(), sp.getXRot(), true);
		sp.resetFallDistance();
		server.playSound(null, to.x, to.y, to.z, SoundEvents.PLAYER_TELEPORT, SoundSource.PLAYERS, 0.8F, 1.0F);
		sp.sendOverlayMessage(where.copy().withStyle(ChatFormatting.AQUA));
		return InteractionResult.SUCCESS;
	}

	/** Players may not break the travel circles (the homestead one stands outside the protected city). */
	public static boolean isWaystone(final Level level, final BlockPos pos) {
		if (!(level instanceof ServerLevel server) || !CityZone.isCityLevel(server)) {
			return false;
		}
		int base = CityZone.baseY(server);
		return pos.equals(waystone(base)) || pos.equals(Homestead.waystone(base));
	}

	/** Top of the column at x, z: where a teleport there can land. */
	static Vec3 surface(final ServerLevel level, final int x, final int z) {
		level.getChunk(x >> 4, z >> 4);
		return new Vec3(x + 0.5, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z + 0.5);
	}

	private CityFixtures() {
	}
}
