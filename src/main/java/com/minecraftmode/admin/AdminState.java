package com.minecraftmode.admin;

import com.minecraftmode.job.JobData;
import com.minecraftmode.job.JobProgression;
import com.minecraftmode.economy.Wallet;
import com.minecraftmode.dungeon.DungeonInstance;
import com.minecraftmode.dungeon.Dungeons;
import com.minecraftmode.market.AuctionHouse;
import com.minecraftmode.market.AuctionService;
import com.minecraftmode.network.AdminActionPayload;
import com.minecraftmode.network.AdminStatePayload;
import com.minecraftmode.progress.RealReset;
import com.minecraftmode.raid.RaidInstance;
import com.minecraftmode.raid.Raids;
import com.minecraftmode.residence.LandClaims;
import com.minecraftmode.story.Story;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Builds the {@link AdminStatePayload} an operator sees. The market and land pages and the market search an operator last asked for
 * (with {@link AdminActionPayload#REFRESH}) are remembered per operator, so answering any other action keeps them on the same page.
 */
public final class AdminState {
	public static final int MARKET_PAGE = 8;
	public static final int LAND_PAGE = 10;
	private static final Map<UUID, View> VIEWS = new HashMap<>();

	private record View(int marketPage, int landPage, String search) {
		static final View FIRST = new View(0, 0, "");
	}

	public static void init() {
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> VIEWS.clear());
	}

	static void send(final ServerPlayer op, final AdminActionPayload action, final int result) {
		View view = action.action() == AdminActionPayload.REFRESH
			? new View(Math.max(0, action.a()), Math.max(0, action.b()), action.text().strip().toLowerCase(Locale.ROOT))
			: VIEWS.getOrDefault(op.getUUID(), View.FIRST);
		VIEWS.put(op.getUUID(), view);
		if (ServerPlayNetworking.canSend(op, AdminStatePayload.TYPE)) {
			ServerPlayNetworking.send(op, build(op.level().getServer(), view, result));
		}
	}

	private static AdminStatePayload build(final MinecraftServer server, final View view, final int result) {
		List<AdminStatePayload.PlayerInfo> players = new ArrayList<>();
		for (ServerPlayer p : server.getPlayerList().getPlayers()) {
			JobData job = JobProgression.get(p);
			players.add(new AdminStatePayload.PlayerInfo(p.getUUID(), p.getPlainTextName(), job.job().ordinal(), job.tier(), job.level(), Wallet.balance(p),
				p.level().dimension().identifier().toString(), p.gameMode().getId(), Story.data(p).chapter()));
		}
		List<AdminStatePayload.Instance> raids = new ArrayList<>();
		for (RaidInstance raid : Raids.instances()) {
			List<String> names = new ArrayList<>();
			raid.members().forEach(m -> names.add(m.name() + (m.active() ? "" : "*")));
			raids.add(new AdminStatePayload.Instance(raid.id(), raid.boss().id(), raid.state().name(), String.join(", ", names)));
		}
		List<AdminStatePayload.Instance> dungeons = new ArrayList<>();
		for (DungeonInstance run : Dungeons.instances()) {
			List<String> names = new ArrayList<>();
			run.members().forEach(m -> names.add(m.name() + (m.active() ? "" : "*")));
			dungeons.add(new AdminStatePayload.Instance(run.id(), run.def().id() + " +" + run.level(), run.state().name(), String.join(", ", names)));
		}
		List<AuctionHouse.Listing> found = AuctionHouse.get(server).listings().stream()
			.filter(l -> view.search().isEmpty() || matches(l, view.search()))
			.toList();
		int marketPages = pages(found.size(), MARKET_PAGE);
		int marketPage = Math.min(view.marketPage(), marketPages - 1);
		long now = AuctionService.now(server);
		List<AdminStatePayload.Listing> listings = page(found, marketPage, MARKET_PAGE).stream()
			.map(l -> new AdminStatePayload.Listing(l.id(), l.item(), l.price(), l.sellerName(), Math.max(0, l.expires() - now)))
			.toList();
		LandClaims claims = LandClaims.get(server);
		List<LandClaims.Claim> all = claims.all();
		int landPages = pages(all.size(), LAND_PAGE);
		int landPage = Math.min(view.landPage(), landPages - 1);
		List<AdminStatePayload.Plot> plots = page(all, landPage, LAND_PAGE).stream()
			.map(c -> new AdminStatePayload.Plot(c.owner(), claims.name(c.owner()), c.chunkX(), c.chunkZ(), c.minY(), claims.trusted(c.owner()).size()))
			.toList();
		return new AdminStatePayload(players, raids, dungeons, RealReset.ticksToNextDay(server), RealReset.ticksToNextCycle(server),
			listings, marketPage, marketPages, plots, landPage, landPages, result);
	}

	/** Item id, English name or seller (the server only knows English names). */
	private static boolean matches(final AuctionHouse.Listing l, final String search) {
		return l.sellerName().toLowerCase(Locale.ROOT).contains(search)
			|| l.item().getHoverName().getString().toLowerCase(Locale.ROOT).contains(search)
			|| BuiltInRegistries.ITEM.getKey(l.item().getItem()).toString().contains(search);
	}

	private static int pages(final int count, final int size) {
		return Math.max(1, (count + size - 1) / size);
	}

	private static <T> List<T> page(final List<T> all, final int page, final int size) {
		int from = Math.min(all.size(), page * size);
		return all.subList(from, Math.min(all.size(), from + size));
	}

	private AdminState() {
	}
}
