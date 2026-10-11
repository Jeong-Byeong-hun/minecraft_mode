package com.minecraftmode.residence;

import com.minecraftmode.city.CityServices;
import com.minecraftmode.entity.CityNpc;
import com.minecraftmode.network.LandBordersPayload;
import com.minecraftmode.registry.ModItems;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Who may do what in the residence dimension. Inside a claim only its owner and the players they trust may break, place or use
 * blocks (chests, doors, buttons included) or touch entities other than monsters and players; land nobody owns (and the arrival
 * square) is not built on at all. Operators and creative players are exempt like everywhere else. A refused player is told whose
 * land it is, and crossing into other land shows its owner on the action bar ({@link #label}).
 */
public final class ResidenceRules {
	private static final int REFUSAL_GAP = 20;
	private static final int HUD_INTERVAL = 10;
	private static final int WAYMASTER_INTERVAL = 100;
	private static final Object WILD = new Object();
	private static final Object SQUARE = new Object();
	private static final Object AVENUE = new Object();
	private static final Map<UUID, Long> LAST_REFUSAL = new ConcurrentHashMap<>();
	private static final Map<UUID, Object> LAST_PLACE = new ConcurrentHashMap<>();
	/** The claims version each player in the residence last got borders for. */
	private static final Map<UUID, Integer> SYNCED = new ConcurrentHashMap<>();

	public static void init() {
		// break/place and block-changing items go through CityServices.blocksBuilding; these cover the rest
		UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
			if (player.getItemInHand(hand).is(ModItems.LAND_DEED)) {
				return InteractionResult.PASS;
			}
			return refuse(player, level, hit.getBlockPos(), blocks(player, hit.getBlockPos()));
		});
		AttackBlockCallback.EVENT.register((player, level, hand, pos, direction) -> refuse(player, level, pos, blocks(player, pos)));
		UseEntityCallback.EVENT.register((player, level, hand, entity, hit) -> refuse(player, level, entity.blockPosition(), blocksEntity(player, entity)));
		AttackEntityCallback.EVENT.register((player, level, hand, entity, hit) -> refuse(player, level, entity.blockPosition(), blocksEntity(player, entity)));
		// arrows and the like; item frames, paintings, boats and minecarts are guarded by BlockAttachedEntityMixin
		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> !blocksDamage(entity, source));
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTickCount() % HUD_INTERVAL == 0) {
				showPlaces(server);
			}
			ServerLevel residence = ResidenceDimension.level(server);
			if (residence != null && server.getTickCount() % WAYMASTER_INTERVAL == 0) {
				CityServices.keepNpc(residence, CityNpc.Role.WAYMASTER, ResidenceDimension.waymaster());
			}
		});
		ServerPlayerEvents.JOIN.register(player -> LandClaims.get(player.level().getServer()).rename(player.getUUID(), player.getPlainTextName()));
		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			LAST_PLACE.clear();
			LAST_REFUSAL.clear();
			SYNCED.clear();
		});
	}

	/** Whether the claims keep {@code player} from changing the block at {@code pos} (server side; ignores exemptions). */
	public static boolean denies(final Player player, final BlockPos pos) {
		if (!(player.level() instanceof ServerLevel level) || !ResidenceDimension.is(level)) {
			return false;
		}
		LandClaims claims = LandClaims.get(level.getServer());
		LandClaims.Claim claim = claims.at(pos);
		return claim == null || !claims.allowed(player.getUUID(), claim);
	}

	public static boolean blocks(final Player player, final BlockPos pos) {
		return !CityServices.exempt(player) && denies(player, pos);
	}

	/** Animals, armor stands, item frames, vehicles... inside someone else's claim; monsters and players are never protected. */
	public static boolean blocksEntity(final Player player, final Entity entity) {
		if (entity instanceof Player || entity instanceof Enemy || CityServices.exempt(player)
			|| !(player.level() instanceof ServerLevel level) || !ResidenceDimension.is(level)) {
			return false;
		}
		LandClaims claims = LandClaims.get(level.getServer());
		LandClaims.Claim claim = claims.at(entity.blockPosition());
		return claim != null && !claims.allowed(player.getUUID(), claim);
	}

	/**
	 * Whether something spreading or moving from {@code from} to {@code to} (water, lava, fire, a piston, a dispenser, a hopper)
	 * would reach land with another owner; land nobody owns counts as one more owner. Only in the residence, only on the server.
	 */
	public static boolean crosses(final Level level, final BlockPos from, final BlockPos to) {
		if (!(level instanceof ServerLevel server) || !ResidenceDimension.is(server)) {
			return false;
		}
		LandClaims claims = LandClaims.get(server.getServer());
		LandClaims.Claim a = claims.at(from);
		LandClaims.Claim b = claims.at(to);
		return !Objects.equals(a == null ? null : a.owner(), b == null ? null : b.owner());
	}

	/** Damage from a player (arrows, tridents, TNT they lit...) to what {@link #blocksEntity} protects from them. */
	public static boolean blocksDamage(final Entity target, final DamageSource source) {
		return source.getEntity() instanceof Player player && blocksEntity(player, target);
	}

	private static InteractionResult refuse(final Player player, final Level level, final BlockPos pos, final boolean blocked) {
		if (!blocked) {
			return InteractionResult.PASS;
		}
		if (player instanceof ServerPlayer sp && level instanceof ServerLevel server) {
			long now = server.getGameTime();
			Long last = LAST_REFUSAL.get(sp.getUUID());
			if (last == null || now < last || now - last >= REFUSAL_GAP) {
				LAST_REFUSAL.put(sp.getUUID(), now);
				LandClaims claims = LandClaims.get(server.getServer());
				LandClaims.Claim claim = claims.at(pos);
				Component why = claim == null ? Component.translatable("message.minecraft_mode.land.refused_wild")
					: Component.translatable("message.minecraft_mode.land.refused", claims.name(claim.owner()));
				sp.sendOverlayMessage(why.copy().withStyle(ChatFormatting.RED));
			}
		}
		return InteractionResult.FAIL;
	}

	/**
	 * Tells each player in the residence whose land they stepped into whenever that changes, and sends the border lines to anyone
	 * who just came in or has not seen the latest change to the claims.
	 */
	private static void showPlaces(final MinecraftServer server) {
		ServerLevel level = ResidenceDimension.level(server);
		Set<UUID> here = new HashSet<>();
		if (level != null) {
			LandClaims claims = LandClaims.get(server);
			for (ServerPlayer player : level.players()) {
				here.add(player.getUUID());
				if (!Objects.equals(SYNCED.put(player.getUUID(), claims.version()), claims.version())) {
					ServerPlayNetworking.send(player, borders(claims, player.getUUID()));
				}
				BlockPos pos = player.blockPosition();
				LandClaims.Claim claim = claims.at(pos);
				Object place = claim != null ? claim : ResidenceDimension.isSpawn(pos) ? SQUARE : ResidenceDimension.isAvenue(pos) ? AVENUE : WILD;
				if (!Objects.equals(LAST_PLACE.put(player.getUUID(), place), place)) {
					player.sendOverlayMessage(label(claims, player.getUUID(), pos));
				}
			}
		}
		LAST_PLACE.keySet().retainAll(here);
		SYNCED.keySet().retainAll(here);
	}

	// ponytail: sends every claim (about 20 bytes each); filter by distance if residences ever reach tens of thousands of plots
	static LandBordersPayload borders(final LandClaims claims, final UUID viewer) {
		List<LandBordersPayload.Border> list = new ArrayList<>();
		for (LandClaims.Claim claim : claims.all()) {
			int relation = claim.owner().equals(viewer) ? LandBordersPayload.MINE
				: claims.allowed(viewer, claim) ? LandBordersPayload.TRUSTED : LandBordersPayload.OTHER;
			list.add(new LandBordersPayload.Border(claim.chunkX(), claim.chunkZ(), claim.minY(), relation));
		}
		return new LandBordersPayload(list);
	}

	/** The action bar text for {@code viewer} standing at {@code pos} in the residence. */
	public static Component label(final LandClaims claims, final UUID viewer, final BlockPos pos) {
		LandClaims.Claim claim = claims.at(pos);
		if (claim == null) {
			String key = ResidenceDimension.isSpawn(pos) ? "square" : ResidenceDimension.isAvenue(pos) ? "avenue" : "wild";
			return Component.translatable("message.minecraft_mode.land.hud." + key).withStyle(key.equals("wild") ? ChatFormatting.GRAY : ChatFormatting.AQUA);
		}
		if (claim.owner().equals(viewer)) {
			return Component.translatable("message.minecraft_mode.land.hud.mine").withStyle(ChatFormatting.GREEN);
		}
		String owner = claims.name(claim.owner());
		return claims.allowed(viewer, claim)
			? Component.translatable("message.minecraft_mode.land.hud.trusted", owner).withStyle(ChatFormatting.YELLOW)
			: Component.translatable("message.minecraft_mode.land.hud.other", owner).withStyle(ChatFormatting.RED);
	}

	private ResidenceRules() {
	}
}
