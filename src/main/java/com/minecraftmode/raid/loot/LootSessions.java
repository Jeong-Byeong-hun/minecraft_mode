package com.minecraftmode.raid.loot;

import com.minecraftmode.loot.Coins;
import com.minecraftmode.network.LootActionPayload;
import com.minecraftmode.network.LootStatePayload;
import com.minecraftmode.raid.BossDef;
import com.minecraftmode.raid.Raids;
import com.minecraftmode.raid.loot.LootSession.Lot;
import com.minecraftmode.raid.loot.LootSession.Mode;
import com.minecraftmode.raid.loot.LootSession.State;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Prediction;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * Raid loot sharing. After a boss falls its gear drops become lots that are resolved one at a time:
 * <ul>
 * <li><b>Auction</b> (default): starts at 1.5x the bracket's shop price; bids go up in coin-friendly
 * steps (about 5% of the start). A bid takes the coins right away (escrow) and the previous highest
 * bidder is refunded. 30 seconds; a bid in the last 10 seconds extends it to 10. The winning price is
 * split evenly among the other participants. No bids: the lot goes to dice.</li>
 * <li><b>Dice</b>: 20 seconds to roll (1-100) or pass; the highest roll wins, ties re-roll among the
 * tied players, and if everyone passes the leader gets it.</li>
 * </ul>
 * The leader can switch the current lot between auction and dice until the first bid or roll. With
 * one participant everything goes to them directly. Items and coins for players who are offline are
 * handed over when they log back in (while the server keeps running).
 */
public final class LootSessions {
	public static final int AUCTION_TICKS = 20 * 30;
	public static final int SNIPE_TICKS = 20 * 10;
	public static final int DICE_TICKS = 20 * 20;
	private static final int PAUSE_TICKS = 40;
	private static final int KEEP_TICKS = 20 * 30;

	private static final Map<Integer, LootSession> SESSIONS = new LinkedHashMap<>();
	/** Items and coins owed to players who were offline. */
	private static final Map<UUID, Pending> PENDING = new HashMap<>();
	private static int nextId = 1;

	private static final class Pending {
		final List<ItemStack> items = new ArrayList<>();
		int copper;
	}

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(LootSessions::tick);
		ServerPlayerEvents.JOIN.register(LootSessions::deliverPending);
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			SESSIONS.clear();
			PENDING.clear();
		});
	}

	/** Starts sharing {@code items} among {@code participants}; a single participant just gets everything. */
	public static @Nullable LootSession start(final MinecraftServer server, final BossDef boss, final Map<UUID, String> participants, final UUID leader,
		final List<ItemStack> items) {
		if (participants.size() <= 1) {
			UUID only = participants.isEmpty() ? leader : participants.keySet().iterator().next();
			for (ItemStack stack : items) {
				giveItem(server, only, stack);
				ServerPlayer p = server.getPlayerList().getPlayer(only);
				if (p != null) {
					p.sendSystemMessage(msg("won_solo", stack.getHoverName()).withStyle(ChatFormatting.GOLD));
				}
			}
			return null;
		}
		LootSession session = new LootSession(nextId++, boss, participants, leader, items);
		SESSIONS.put(session.id, session);
		session.nextStart = PAUSE_TICKS;
		tell(server, session, msg("started", items.size()).withStyle(ChatFormatting.GOLD));
		broadcast(server, session, true);
		return session;
	}

	public static @Nullable LootSession get(final int id) {
		return SESSIONS.get(id);
	}

	/** The newest unfinished session {@code player} takes part in. */
	public static @Nullable LootSession sessionOf(final UUID player) {
		LootSession found = null;
		for (LootSession session : SESSIONS.values()) {
			if (session.participants.containsKey(player)) {
				found = session;
			}
		}
		return found;
	}

	/** {@code /raid loot} and the loot key: (re)open the screen. */
	public static boolean open(final ServerPlayer player) {
		LootSession session = sessionOf(player.getUUID());
		if (session == null) {
			player.sendSystemMessage(msg("none").withStyle(ChatFormatting.GRAY));
			return false;
		}
		send(player, session, true);
		return true;
	}

	// ------------------------------------------------------------------ actions

	public static void handle(final ServerPlayer player, final LootActionPayload action) {
		LootSession session = SESSIONS.get(action.session());
		if (session == null || !session.participants.containsKey(player.getUUID())) {
			return;
		}
		MinecraftServer server = player.level().getServer();
		Lot lot = session.currentLot();
		if (action.action() == LootActionPayload.Action.REFRESH || lot == null || lot.state != State.RUNNING) {
			send(player, session, false);
			return;
		}
		switch (action.action()) {
			case BID -> bid(server, session, lot, player, Math.max(1, Math.min(10, action.value())));
			case ROLL -> roll(server, session, lot, player);
			case PASS -> pass(server, session, lot, player);
			case SWITCH_MODE -> switchMode(server, session, lot, player);
			default -> {
			}
		}
		broadcast(server, session, false);
	}

	private static void bid(final MinecraftServer server, final LootSession session, final Lot lot, final ServerPlayer player, final int steps) {
		if (lot.mode != Mode.AUCTION) {
			return;
		}
		if (player.getUUID().equals(lot.bidder)) {
			player.sendSystemMessage(msg("already_highest").withStyle(ChatFormatting.GRAY));
			return;
		}
		int amount = lot.nextBid(steps);
		if (!Coins.take(player, amount)) {
			player.sendSystemMessage(msg("cannot_afford", Coins.component(amount)).withStyle(ChatFormatting.RED));
			return;
		}
		if (lot.bidder != null) {
			giveCoins(server, lot.bidder, lot.bid);
			ServerPlayer outbid = server.getPlayerList().getPlayer(lot.bidder);
			if (outbid != null) {
				outbid.sendSystemMessage(msg("outbid", player.getDisplayName(), Coins.component(amount)).withStyle(ChatFormatting.YELLOW));
			}
		}
		lot.bid = amount;
		lot.bidder = player.getUUID();
		if (lot.endsAt - session.ticks < SNIPE_TICKS) {
			lot.endsAt = session.ticks + SNIPE_TICKS;
		}
		tell(server, session, msg("bid", player.getDisplayName(), Coins.component(amount), lot.stack.getHoverName()).withStyle(ChatFormatting.AQUA));
	}

	private static void roll(final MinecraftServer server, final LootSession session, final Lot lot, final ServerPlayer player) {
		UUID id = player.getUUID();
		if (lot.mode != Mode.DICE || !lot.eligible.contains(id) || lot.rolls.containsKey(id) || lot.passed.contains(id)) {
			return;
		}
		int roll = 1 + player.getRandom().nextInt(100);
		lot.rolls.put(id, roll);
		tell(server, session, msg("rolled", player.getDisplayName(), roll).withStyle(ChatFormatting.AQUA));
		if (allDecided(lot)) {
			lot.endsAt = session.ticks;
		}
	}

	private static void pass(final MinecraftServer server, final LootSession session, final Lot lot, final ServerPlayer player) {
		UUID id = player.getUUID();
		if (lot.mode != Mode.DICE || !lot.eligible.contains(id) || lot.rolls.containsKey(id)) {
			return;
		}
		lot.passed.add(id);
		if (allDecided(lot)) {
			lot.endsAt = session.ticks;
		}
	}

	private static void switchMode(final MinecraftServer server, final LootSession session, final Lot lot, final ServerPlayer player) {
		if (!session.leader.equals(player.getUUID()) || lot.bid > 0 || !lot.rolls.isEmpty() || lot.round > 1) {
			return;
		}
		if (lot.mode == Mode.AUCTION) {
			startDice(session, lot, session.participants.keySet());
		} else {
			lot.mode = Mode.AUCTION;
			lot.passed.clear();
			lot.endsAt = session.ticks + AUCTION_TICKS;
		}
		tell(server, session, msg(lot.mode == Mode.AUCTION ? "mode_auction" : "mode_dice", lot.stack.getHoverName()).withStyle(ChatFormatting.GOLD));
	}

	private static boolean allDecided(final Lot lot) {
		for (UUID id : lot.eligible) {
			if (!lot.rolls.containsKey(id) && !lot.passed.contains(id)) {
				return false;
			}
		}
		return true;
	}

	private static void startDice(final LootSession session, final Lot lot, final java.util.Collection<UUID> eligible) {
		lot.mode = Mode.DICE;
		lot.rolls.clear();
		lot.passed.clear();
		lot.eligible.clear();
		lot.eligible.addAll(eligible);
		lot.round++;
		lot.endsAt = session.ticks + DICE_TICKS;
	}

	// ------------------------------------------------------------------ ticking

	private static void tick(final MinecraftServer server) {
		if (SESSIONS.isEmpty()) {
			return;
		}
		Iterator<LootSession> it = SESSIONS.values().iterator();
		while (it.hasNext()) {
			LootSession session = it.next();
			session.ticks++;
			if (session.finished()) {
				if (session.ticks - session.finishedAt > KEEP_TICKS) {
					it.remove();
				}
				continue;
			}
			Lot lot = session.currentLot();
			if (lot == null) {
				session.finishedAt = session.ticks;
				tell(server, session, msg("finished").withStyle(ChatFormatting.GOLD));
				broadcast(server, session, false);
				continue;
			}
			boolean changed = false;
			if (lot.state == State.WAITING && session.ticks >= session.nextStart) {
				lot.state = State.RUNNING;
				lot.endsAt = session.ticks + AUCTION_TICKS;
				tell(server, session, msg("lot", session.current + 1, session.lots.size(), lot.stack.getHoverName(), Coins.component(lot.start))
					.withStyle(ChatFormatting.GOLD));
				changed = true;
			} else if (lot.state == State.RUNNING && session.ticks >= lot.endsAt) {
				resolve(server, session, lot);
				changed = true;
			}
			if (changed || session.ticks % 20 == 0) {
				broadcast(server, session, false);
			}
		}
	}

	private static void resolve(final MinecraftServer server, final LootSession session, final Lot lot) {
		if (lot.mode == Mode.AUCTION) {
			if (lot.bidder == null) {
				startDice(session, lot, session.participants.keySet());
				tell(server, session, msg("no_bids", lot.stack.getHoverName()).withStyle(ChatFormatting.YELLOW));
				return;
			}
			award(server, session, lot, lot.bidder, lot.bid);
			List<UUID> others = new ArrayList<>(session.participants.keySet());
			others.remove(lot.bidder);
			if (!others.isEmpty()) {
				int share = lot.bid / others.size();
				int rest = lot.bid % others.size();
				for (int i = 0; i < others.size(); i++) {
					int amount = share + (i < rest ? 1 : 0);
					giveCoins(server, others.get(i), amount);
					ServerPlayer p = server.getPlayerList().getPlayer(others.get(i));
					if (p != null && amount > 0) {
						p.sendSystemMessage(msg("share", Coins.component(amount)).withStyle(ChatFormatting.GREEN));
					}
				}
			}
			return;
		}
		int best = 0;
		List<UUID> top = new ArrayList<>();
		for (Map.Entry<UUID, Integer> e : lot.rolls.entrySet()) {
			if (e.getValue() > best) {
				best = e.getValue();
				top.clear();
				top.add(e.getKey());
			} else if (e.getValue() == best) {
				top.add(e.getKey());
			}
		}
		if (top.isEmpty()) {
			award(server, session, lot, session.leader, 0);
		} else if (top.size() == 1) {
			award(server, session, lot, top.getFirst(), 0);
		} else {
			startDice(session, lot, top);
			tell(server, session, msg("tie", best).withStyle(ChatFormatting.YELLOW));
		}
	}

	private static void award(final MinecraftServer server, final LootSession session, final Lot lot, final UUID winner, final int price) {
		lot.state = State.DONE;
		lot.winner = winner;
		lot.price = price;
		giveItem(server, winner, lot.stack.copy());
		tell(server, session, (price > 0 ? msg("won_auction", session.name(winner), lot.stack.getHoverName(), Coins.component(price))
			: msg("won_dice", session.name(winner), lot.stack.getHoverName())).withStyle(ChatFormatting.GOLD));
		ServerPlayer p = server.getPlayerList().getPlayer(winner);
		if (p != null) {
			Raids.ping(p, BuiltInRegistries.SOUND_EVENT.wrapAsHolder(SoundEvents.PLAYER_LEVELUP), 1.4F);
		}
		session.current++;
		session.nextStart = session.ticks + PAUSE_TICKS;
	}

	// ------------------------------------------------------------------ delivery

	private static void giveItem(final MinecraftServer server, final UUID id, final ItemStack stack) {
		ServerPlayer p = server.getPlayerList().getPlayer(id);
		if (p != null) {
			p.getInventory().placeItemBackInInventory(stack, Prediction.SERVER_ONLY);
		} else {
			PENDING.computeIfAbsent(id, k -> new Pending()).items.add(stack);
		}
	}

	private static void giveCoins(final MinecraftServer server, final UUID id, final int copper) {
		if (copper <= 0) {
			return;
		}
		ServerPlayer p = server.getPlayerList().getPlayer(id);
		if (p != null) {
			Coins.give(p, copper);
		} else {
			PENDING.computeIfAbsent(id, k -> new Pending()).copper += copper;
		}
	}

	private static void deliverPending(final ServerPlayer player) {
		Pending pending = PENDING.remove(player.getUUID());
		if (pending == null) {
			return;
		}
		for (ItemStack stack : pending.items) {
			player.getInventory().placeItemBackInInventory(stack, Prediction.SERVER_ONLY);
		}
		Coins.give(player, pending.copper);
		player.sendSystemMessage(msg("delivered", pending.items.size(), Coins.component(pending.copper)).withStyle(ChatFormatting.GOLD));
	}

	// ------------------------------------------------------------------ sync

	private static void broadcast(final MinecraftServer server, final LootSession session, final boolean open) {
		for (UUID id : session.participants.keySet()) {
			ServerPlayer p = server.getPlayerList().getPlayer(id);
			if (p != null) {
				send(p, session, open);
			}
		}
	}

	private static void send(final ServerPlayer player, final LootSession session, final boolean open) {
		if (ServerPlayNetworking.canSend(player, LootStatePayload.TYPE)) {
			ServerPlayNetworking.send(player, session.payload(player.getUUID(), open));
		}
	}

	private static void tell(final MinecraftServer server, final LootSession session, final Component message) {
		for (UUID id : session.participants.keySet()) {
			ServerPlayer p = server.getPlayerList().getPlayer(id);
			if (p != null) {
				p.sendSystemMessage(message);
			}
		}
	}

	static MutableComponent msg(final String key, final Object... args) {
		return Component.translatable("message.minecraft_mode.loot." + key, args);
	}

	private LootSessions() {
	}
}
