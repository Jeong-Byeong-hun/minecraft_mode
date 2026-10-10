package com.minecraftmode.test;

import com.minecraftmode.MinecraftMode;
import com.minecraftmode.client.endgame.AuctionScreen;
import com.minecraftmode.client.endgame.EndgameClient;
import com.minecraftmode.client.raid.LootScreen;
import com.minecraftmode.client.raid.RaidClient;
import com.minecraftmode.client.raid.RaidScreen;
import com.minecraftmode.economy.Wallet;
import com.minecraftmode.entity.CityNpc;
import com.minecraftmode.entity.named.NamedMob;
import com.minecraftmode.network.AuctionActionPayload;
import com.minecraftmode.network.AuctionStatePayload;
import com.minecraftmode.network.ProgressActionPayload;
import com.minecraftmode.network.RaidEnterPayload;
import com.minecraftmode.progress.Progress;
import com.minecraftmode.raid.RaidBosses;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Two real clients on a real dedicated server. Not part of the normal suite: it runs only when started as one of the two
 * multiplayer clients ({@code minecraft_mode.mp.role} = alice or bob), against a server started with {@code runServer}
 * (offline mode, RCON on). Alice drives the server through RCON and both clients act through their own screens and packets;
 * they hand steps to each other with flag files in {@code minecraft_mode.mp.dir}.
 *
 * <ol>
 * <li>Market: Alice lists diamonds, Bob finds and buys them, Alice collects the proceeds from her mailbox.</li>
 * <li>Titles: Alice wears a title and Bob sees it in front of her name.</li>
 * <li>Party kill sharing: Alice kills a named monster and Bob, standing by in her party, gets the codex kill too.</li>
 * <li>Lair lord: the treasure opens for Alice who was there when her lord fell; Bob, who came later, finds it sealed and a lord
 * rising for him, beats it, and then gets his own different loot.</li>
 * <li>Party raid: Alice leads both into Arachne, both see the fight and the loot distribution, both go home.</li>
 * </ol>
 */
public class MultiplayerClientGameTest implements FabricClientGameTest {
	private static final String ROLE = System.getProperty("minecraft_mode.mp.role", "");
	private static final Path DIR = Path.of(System.getProperty("minecraft_mode.mp.dir", "build/mp"));
	private static final String ADDRESS = System.getProperty("minecraft_mode.mp.address", "localhost:25565");
	private static final String RCON_PASSWORD = System.getProperty("minecraft_mode.mp.rcon", "");
	private static final int RCON_PORT = Integer.getInteger("minecraft_mode.mp.rconPort", 25575);
	private static final int STEP = 1200;
	private static final String NAMED = "dune_scorpion";

	@Override
	public void runTest(final ClientGameTestContext context) {
		if (TestSelection.skip(this)) {
			return;
		}
		if (ROLE.isEmpty()) {
			return;
		}
		log("connecting to {} as {}", ADDRESS, ROLE);
		context.runOnClient(mc -> ConnectScreen.startConnecting(new TitleScreen(), mc, ServerAddress.parseString(ADDRESS),
			new ServerData("multiplayer test", ADDRESS, ServerData.Type.OTHER), false, null));
		context.waitFor(mc -> mc.player != null && mc.level != null && mc.gui.screen() == null, 2400);
		context.waitTicks(40);
		try {
			if (ROLE.equals("alice")) {
				alice(context);
			} else {
				bob(context);
			}
		} catch (RuntimeException | Error e) {
			flag("fail_" + ROLE, String.valueOf(e.getMessage()));
			throw e;
		} finally {
			context.runOnClient(mc -> mc.disconnect(new TitleScreen(), false));
			context.waitFor(mc -> mc.level == null, 600);
			context.setScreen(TitleScreen::new);
		}
	}

	// ------------------------------------------------------------------ Alice (drives the server)

	private static void alice(final ClientGameTestContext context) {
		try (Rcon rcon = new Rcon(RCON_PORT, RCON_PASSWORD)) {
			for (int i = 0; i < 120 && !(rcon.run("list").contains("Alice") && rcon.run("list").contains("Bob")); i++) {
				context.waitTicks(20);
			}
			require(rcon.run("list").contains("Bob"), "Bob never joined: " + rcon.run("list"));
			// the market floor in front of the broker: the city floor height (players rejoin where they left)
			rcon.run("execute in minecraft:overworld positioned -6 0 71 positioned over motion_blocking_no_leaves run tp Alice ~ ~ ~ 90 0");
			context.waitTicks(20);
			int base = context.computeOnClient(mc -> (int)Math.floor(mc.player.getY()));
			log("both online, city floor at y {}", base);
			for (String cmd : new String[] {"time set noon", "weather clear", "gamerule spawn_mobs false", "job set Alice warrior 2", "job level Alice 30",
				"job set Bob rogue 2", "job level Bob 30", "wallet give Alice 30g", "wallet give Bob 30g", "effect give Alice resistance infinite 4 true",
				"effect give Bob resistance infinite 4 true", "clear Alice", "clear Bob", "give Alice minecraft:diamond 5"}) {
				rcon.run(cmd);
			}
			context.waitTicks(20);

			// 1. market: Alice sells, Bob buys, Alice collects
			rcon.run("tp Alice -6 " + base + " 71 90 0");
			rcon.run("tp Bob -6 " + base + " 73 90 0");
			flag("setup", "");
			CityNpc broker = npc(context, CityNpc.Role.BROKER);
			interact(context, broker);
			context.waitForScreen(AuctionScreen.class);
			int slot = context.computeOnClient(mc -> mc.player.getInventory().findSlotMatchingItem(new ItemStack(Items.DIAMOND)));
			require(slot >= 0, "Alice has no diamonds");
			int brokerId = broker.getId();
			context.runOnClient(mc -> ClientPlayNetworking.send(new AuctionActionPayload(brokerId, AuctionActionPayload.LIST, 0, slot, 90,
				AuctionActionPayload.Query.DEFAULT)));
			waitClient(context, mc -> EndgameClient.auction() != null && EndgameClient.auction().myListings() == 1, 200, "Alice's listing should be up");
			context.takeScreenshot("mp_alice_market");
			int walletBefore = context.computeOnClient(mc -> Wallet.balance(mc.player));
			flag("listed", "");
			await(context, "bought");
			context.runOnClient(mc -> ClientPlayNetworking.send(new AuctionActionPayload(brokerId, AuctionActionPayload.REFRESH, 0, 0, 0,
				AuctionActionPayload.Query.DEFAULT)));
			waitClient(context, mc -> EndgameClient.auction() != null && EndgameClient.auction().mail().coins() == 86, 200,
				"Alice's mailbox should hold 90 - 5% = 86 copper");
			context.setScreen(() -> null);
			AuctionScreen.showTab("mail");
			interact(context, broker);
			context.waitForScreen(AuctionScreen.class);
			context.waitTicks(10);
			context.takeScreenshot("mp_alice_mailbox");
			context.runOnClient(mc -> ClientPlayNetworking.send(new AuctionActionPayload(brokerId, AuctionActionPayload.CLAIM, 0, 0, 0,
				AuctionActionPayload.Query.DEFAULT)));
			waitClient(context, mc -> Wallet.balance(mc.player) == walletBefore + 86, 200, "the proceeds should reach Alice's wallet");
			AuctionScreen.showTab("browse");
			context.setScreen(() -> null);
			log("market: listed, sold to Bob, 86C collected");

			// 2. a title seen by the other player
			rcon.run("wallet give Alice 100g");
			waitClient(context, mc -> Progress.get(mc.player).has("wallet_100g"), 400, "100 gold should unlock the Tycoon title");
			context.runOnClient(mc -> ClientPlayNetworking.send(new ProgressActionPayload(ProgressActionPayload.TITLE, "wallet_100g")));
			context.waitTicks(20);
			rcon.run("tp Bob -6 " + base + " 75 180 0");
			flag("titled", "");
			await(context, "title_seen");
			log("title: Bob sees Alice's title");

			// 3. a party kill counts for both
			// parties live in server memory and survive reconnects: start from none
			rcon.run("execute as Alice run party leave");
			rcon.run("execute as Bob run party leave");
			rcon.run("execute as Alice run party create");
			rcon.run("execute as Alice run party invite Bob");
			rcon.run("execute as Bob run party accept");
			waitClient(context, mc -> RaidClient.party().size() == 2, 200, "Alice should see a party of two");
			// outside the walls: city guards remove hostile mobs (and lords) at once
			// a fresh spot every run, so nothing from an earlier run is around
			int ox = 1500 + (int)(System.currentTimeMillis() / 1000 % 200) * 48;
			surface(context, rcon, "Alice", ox, 1500);
			surface(context, rcon, "Bob", ox + 1, 1502);
			context.waitTicks(100);
			int aliceKills = context.computeOnClient(mc -> Progress.get(mc.player).kills(NAMED));
			rcon.run("execute at Alice run summon minecraft_mode:" + NAMED + " ~3 ~ ~ {NoAI:1b,PersistenceRequired:1b,Tags:[\"mp_target\"]}");
			context.waitTicks(10);
			rcon.run("damage @e[tag=mp_target,limit=1] 100000 minecraft:player_attack by Alice");
			waitClient(context, mc -> Progress.get(mc.player).kills(NAMED) == aliceKills + 1, 200, "Alice should have the kill");
			flag("party_kill", "");
			await(context, "bob_kill");
			log("party: Bob was credited for Alice's kill");

			// 4. the lair lord: only those near it when it falls may open the treasure
			surface(context, rcon, "Bob", ox + 140, 1500);
			BlockPos me = context.computeOnClient(mc -> mc.player.blockPosition());
			BlockPos chest = me.east(3);
			Files.writeString(DIR.resolve("chest.txt"), chest.getX() + " " + chest.getY() + " " + chest.getZ());
			String at = chest.getX() + " " + chest.getY() + " " + chest.getZ();
			rcon.run("setblock " + at + " minecraft_mode:lair_chest");
			rcon.run("data merge block " + at + " {Lair:\"" + NAMED + "\",Seed:7L,Cache:0b}");
			for (int i = 0; i < 20 && !rcon.run("execute positioned " + at + " if entity @e[type=minecraft_mode:" + NAMED + ",distance=..8]").contains("passed"); i++) {
				context.waitTicks(10);
			}
			require(rcon.run("execute positioned " + at + " if entity @e[type=minecraft_mode:" + NAMED + ",distance=..8]").contains("passed"), "Alice should wake a lord");
			context.getInput().lookAt(chest);
			context.waitTicks(20);
			context.takeScreenshot("mp_alice_lord");
			useChest(context, chest);
			context.waitTicks(20);
			require(!context.computeOnClient(mc -> mc.gui.screen() instanceof ContainerScreen), "the chest must stay sealed while Alice's lord lives");
			rcon.run("execute positioned " + at + " run damage @e[type=minecraft_mode:" + NAMED + ",limit=1,sort=nearest,distance=..16] 1000000 minecraft:player_attack by Alice");
			context.waitTicks(20);
			String victors = rcon.run("data get block " + at + " Victors");
			String aliceId = context.computeOnClient(mc -> mc.player.getStringUUID());
			require(victors.contains(aliceId) && !victors.contains(rcon.uuid("Bob")), "only Alice should be a victor: " + victors);
			useChest(context, chest);
			context.waitForScreen(ContainerScreen.class);
			context.waitTicks(5);
			String loot = context.computeOnClient(MultiplayerClientGameTest::contents);
			Files.writeString(DIR.resolve("alice_loot.txt"), loot);
			context.takeScreenshot("mp_alice_lair_loot");
			context.setScreen(() -> null);
			rcon.run("tp Bob " + chest.getX() + " " + chest.getY() + " " + (chest.getZ() + 3) + " 180 20");
			flag("bob_lair", "");
			await(context, "bob_sealed");
			rcon.run("execute positioned " + at + " run damage @e[type=minecraft_mode:" + NAMED + ",limit=1,sort=nearest,distance=..16] 1000000 minecraft:player_attack by Bob");
			context.waitTicks(20);
			flag("bob_lord_dead", "");
			await(context, "bob_looted");
			log("lair: Alice looted {}, Bob had to beat his own lord and got different loot", loot);

			// 5. a party raid seen from both clients
			rcon.run("tp Alice 10 " + base + " -51 180 0");
			rcon.run("tp Bob 11 " + base + " -50 180 0");
			CityNpc marshal = npc(context, CityNpc.Role.RAID_MARSHAL);
			RaidScreen.preset(0, com.minecraftmode.raid.RaidDifficulty.NORMAL, false);
			interact(context, marshal);
			context.waitForScreen(RaidScreen.class);
			waitClient(context, mc -> RaidClient.raidInfo() != null && RaidClient.raidInfo().affixes().size() == 2, 100, "the marshal should send the cycle's modifiers");
			context.waitTicks(10);
			context.takeScreenshot("mp_alice_raid_board");
			int marshalId = marshal.getId();
			context.runOnClient(mc -> ClientPlayNetworking.send(new RaidEnterPayload(marshalId, RaidBosses.ARACHNE.id(), "normal")));
			context.setScreen(() -> null);
			waitClient(context, mc -> mc.level != null && mc.level.dimension().identifier().getPath().equals("raid"), 400, "Alice should enter the raid");
			flag("raid", "");
			for (int i = 0; i < 60 && !rcon.run("execute in minecraft_mode:raid if entity @e[type=minecraft_mode:arachne]").contains("passed"); i++) {
				context.waitTicks(20);
			}
			context.waitTicks(40);
			context.takeScreenshot("mp_alice_raid_fight");
			flag("raid_fight", "");
			await(context, "bob_fight_shot");
			rcon.run("execute in minecraft_mode:raid run damage @e[type=minecraft_mode:arachne,limit=1] 1000000000 minecraft:player_attack by Alice");
			waitClient(context, mc -> mc.gui.screen() instanceof LootScreen && RaidClient.loot() != null && !RaidClient.loot().lots().isEmpty(), 400,
				"the loot distribution should open for Alice");
			context.waitTicks(10);
			context.takeScreenshot("mp_alice_raid_loot");
			flag("raid_loot", "");
			await(context, "bob_loot");
			context.setScreen(() -> null);
			rcon.run("execute as Alice run raid leave");
			rcon.run("execute as Bob run raid leave");
			waitClient(context, mc -> mc.level != null && mc.level.dimension().identifier().getPath().equals("overworld"), 400, "Alice should be home");
			flag("alice_done", "");
			await(context, "bob_done");
			log("raid: both entered, fought and saw the loot; all multiplayer checks passed");
		} catch (IOException e) {
			throw new AssertionError("RCON: " + e.getMessage(), e);
		}
	}

	// ------------------------------------------------------------------ Bob (acts on his own client)

	private static void bob(final ClientGameTestContext context) {
		try {
			await(context, "setup");
			await(context, "listed");
			CityNpc broker = npc(context, CityNpc.Role.BROKER);
			int brokerId = broker.getId();
			interact(context, broker);
			context.waitForScreen(AuctionScreen.class);
			AuctionStatePayload.Entry[] found = new AuctionStatePayload.Entry[1];
			for (int i = 0; i < 20 && found[0] == null; i++) {
				context.runOnClient(mc -> ClientPlayNetworking.send(new AuctionActionPayload(brokerId, AuctionActionPayload.REFRESH, 0, 0, 0,
					AuctionActionPayload.Query.DEFAULT)));
				context.waitTicks(10);
				found[0] = context.computeOnClient(mc -> EndgameClient.auction() == null ? null
					: EndgameClient.auction().entries().stream().filter(e -> e.seller().equals("Alice") && e.item().is(Items.DIAMOND)).findFirst().orElse(null));
			}
			require(found[0] != null, "Bob should see Alice's diamonds on the market");
			context.takeScreenshot("mp_bob_market");
			int id = found[0].id();
			context.runOnClient(mc -> ClientPlayNetworking.send(new AuctionActionPayload(brokerId, AuctionActionPayload.BUY, id, 0, 0, AuctionActionPayload.Query.DEFAULT)));
			waitClient(context, mc -> mc.player.getInventory().countItem(Items.DIAMOND) == 5, 200, "Bob should receive the diamonds");
			context.setScreen(() -> null);
			flag("bought", "");

			await(context, "titled");
			waitClient(context, mc -> mc.level.players().stream().anyMatch(p -> p.getPlainTextName().equals("Alice") && p.getDisplayName().getString().startsWith("[")), 200,
				"Bob should see Alice's title in front of her name");
			context.waitTicks(20);
			context.takeScreenshot("mp_bob_sees_title");
			int bobKills = context.computeOnClient(mc -> Progress.get(mc.player).kills(NAMED));
			flag("title_seen", "");

			await(context, "party_kill");
			waitClient(context, mc -> RaidClient.party().size() == 2, 100, "Bob should see a party of two");
			waitClient(context, mc -> Progress.get(mc.player).kills(NAMED) == bobKills + 1, 200, "Bob should share the party kill");
			flag("bob_kill", "");

			await(context, "bob_lair");
			String[] parts = Files.readString(DIR.resolve("chest.txt")).trim().split(" ");
			BlockPos chest = new BlockPos(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
			waitClient(context, mc -> lordNear(mc, chest) != null, 200, "a lord should rise for Bob");
			useChest(context, chest);
			context.waitTicks(20);
			require(!context.computeOnClient(mc -> mc.gui.screen() instanceof ContainerScreen), "the treasure must stay sealed for Bob");
			context.getInput().lookAt(chest);
			context.waitTicks(10);
			context.takeScreenshot("mp_bob_sealed");
			flag("bob_sealed", "");
			await(context, "bob_lord_dead");
			useChest(context, chest);
			context.waitForScreen(ContainerScreen.class);
			context.waitTicks(5);
			String loot = context.computeOnClient(MultiplayerClientGameTest::contents);
			String alice = Files.readString(DIR.resolve("alice_loot.txt"));
			require(!loot.isEmpty() && !loot.equals(alice), "Bob's treasure should be his own roll: " + loot + " vs " + alice);
			context.takeScreenshot("mp_bob_lair_loot");
			context.setScreen(() -> null);
			flag("bob_looted", "");

			await(context, "raid");
			waitClient(context, mc -> mc.level != null && mc.level.dimension().identifier().getPath().equals("raid"), 400, "Bob should enter the raid with his party");
			await(context, "raid_fight");
			context.takeScreenshot("mp_bob_raid_fight");
			flag("bob_fight_shot", "");
			await(context, "raid_loot");
			waitClient(context, mc -> mc.gui.screen() instanceof LootScreen && RaidClient.loot() != null && !RaidClient.loot().lots().isEmpty(), 400,
				"the loot distribution should open for Bob");
			context.waitTicks(10);
			context.takeScreenshot("mp_bob_raid_loot");
			flag("bob_loot", "");
			context.setScreen(() -> null);
			waitClient(context, mc -> mc.level != null && mc.level.dimension().identifier().getPath().equals("overworld"), 600, "Bob should be home");
			flag("bob_done", "");
			await(context, "alice_done");
		} catch (IOException e) {
			throw new AssertionError(e.getMessage(), e);
		}
	}

	// ------------------------------------------------------------------ helpers

	/** Sends {@code player} to the ground at x, z: up high first (slow falling) so the chunk loads, then down onto the surface. */
	private static void surface(final ClientGameTestContext context, final Rcon rcon, final String player, final int x, final int z) {
		rcon.run("effect give " + player + " slow_falling 20 0 true");
		rcon.run("tp " + player + " " + x + " 200 " + z);
		for (int i = 0; i < 20; i++) {
			context.waitTicks(10);
			if (rcon.run("execute as " + player + " at @s positioned over motion_blocking_no_leaves run tp @s ~ ~ ~").startsWith("Teleported")) {
				rcon.run("effect clear " + player + " slow_falling");
				return;
			}
		}
		throw new AssertionError(player + " could not be put on the ground at " + x + ", " + z);
	}

	private static @Nullable NamedMob lordNear(final Minecraft mc, final BlockPos pos) {
		for (Entity e : mc.level.entitiesForRendering()) {
			if (e instanceof NamedMob mob && mob.isAlive() && mob.blockPosition().closerThan(pos, 10)) {
				return mob;
			}
		}
		return null;
	}

	private static CityNpc npc(final ClientGameTestContext context, final CityNpc.Role role) {
		waitClient(context, mc -> find(mc, role) != null, 1200, role.id() + " should be at its post");
		return context.computeOnClient(mc -> find(mc, role));
	}

	private static @Nullable CityNpc find(final Minecraft mc, final CityNpc.Role role) {
		CityNpc best = null;
		for (Entity e : mc.level.entitiesForRendering()) {
			if (e instanceof CityNpc npc && npc.role() == role && (best == null || npc.distanceTo(mc.player) < best.distanceTo(mc.player))) {
				best = npc;
			}
		}
		return best;
	}

	private static void interact(final ClientGameTestContext context, final Entity target) {
		context.runOnClient(mc -> {
			Entity e = mc.level.getEntity(target.getId());
			mc.gameMode.interact(mc.player, e, new EntityHitResult(e), InteractionHand.MAIN_HAND);
		});
	}

	private static void useChest(final ClientGameTestContext context, final BlockPos pos) {
		context.runOnClient(mc -> mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false)));
	}

	private static String contents(final Minecraft mc) {
		if (!(mc.gui.screen() instanceof ContainerScreen screen)) {
			return "";
		}
		List<String> out = new ArrayList<>();
		for (int i = 0; i < 27; i++) {
			ItemStack stack = screen.getMenu().getSlot(i).getItem();
			if (!stack.isEmpty()) {
				out.add(i + ":" + stack.getItem() + "x" + stack.getCount());
			}
		}
		return String.join(",", out);
	}

	private static void waitClient(final ClientGameTestContext context, final Predicate<Minecraft> condition, final int maxTicks, final String message) {
		for (int t = 0; t <= maxTicks; t += 5) {
			if (context.computeOnClient(condition::test)) {
				return;
			}
			context.waitTicks(5);
		}
		throw new AssertionError(message);
	}

	private static void flag(final String name, final String text) {
		try {
			Files.createDirectories(DIR);
			Files.writeString(DIR.resolve(name), text);
		} catch (IOException e) {
			throw new AssertionError("cannot write flag " + name, e);
		}
	}

	/** Waits for the other client's flag; stops early with its message when the other client failed. */
	private static void await(final ClientGameTestContext context, final String name) {
		for (int t = 0; t < STEP * 3; t += 10) {
			if (Files.exists(DIR.resolve(name))) {
				return;
			}
			try (Stream<Path> files = Files.list(DIR)) {
				Path failed = files.filter(p -> p.getFileName().toString().startsWith("fail_")).findFirst().orElse(null);
				if (failed != null) {
					throw new AssertionError("the other client failed: " + Files.readString(failed));
				}
			} catch (IOException ignored) {
				// the directory appears with the first flag
			}
			context.waitTicks(10);
		}
		throw new AssertionError(ROLE + " waited too long for " + name);
	}

	private static void log(final String message, final Object... args) {
		MinecraftMode.LOGGER.info("[mp:" + ROLE + "] " + message, args);
	}

	private static void require(final boolean condition, final String message) {
		if (!condition) {
			throw new AssertionError(message);
		}
	}

	/** A minimal RCON client (Source RCON protocol: little-endian length, id, type, body, two zero bytes). */
	private static final class Rcon implements AutoCloseable {
		private final Socket socket;
		private final DataInputStream in;
		private final OutputStream out;
		private int id;

		Rcon(final int port, final String password) throws IOException {
			this.socket = new Socket("127.0.0.1", port);
			this.socket.setSoTimeout(20000);
			this.in = new DataInputStream(this.socket.getInputStream());
			this.out = this.socket.getOutputStream();
			int answer = this.send(3, password);
			if (answer == -1) {
				throw new IOException("RCON login refused");
			}
		}

		String run(final String command) {
			try {
				this.send(2, command);
				String reply = this.lastBody;
				MinecraftMode.LOGGER.info("[mp:rcon] {} -> {}", command, reply.replace('\n', ' '));
				return reply;
			} catch (IOException e) {
				throw new AssertionError("RCON failed on " + command, e);
			}
		}

		/** A player's UUID as the server knows it (from /data get entity). */
		String uuid(final String player) {
			String data = this.run("data get entity " + player + " UUID");
			// "Bob has the following entity data: [I; a, b, c, d]"
			int open = data.indexOf("[I;");
			if (open < 0) {
				return "?";
			}
			String[] ints = data.substring(open + 3, data.indexOf(']', open)).split(",");
			long most = ((long)Integer.parseInt(ints[0].trim()) << 32) | (Integer.parseInt(ints[1].trim()) & 0xFFFFFFFFL);
			long least = ((long)Integer.parseInt(ints[2].trim()) << 32) | (Integer.parseInt(ints[3].trim()) & 0xFFFFFFFFL);
			return new java.util.UUID(most, least).toString();
		}

		private String lastBody = "";

		private int send(final int type, final String body) throws IOException {
			byte[] payload = body.getBytes(StandardCharsets.UTF_8);
			ByteBuffer packet = ByteBuffer.allocate(14 + payload.length).order(ByteOrder.LITTLE_ENDIAN);
			packet.putInt(10 + payload.length).putInt(++this.id).putInt(type).put(payload).put((byte)0).put((byte)0);
			this.out.write(packet.array());
			this.out.flush();
			int length = Integer.reverseBytes(this.in.readInt());
			int answerId = Integer.reverseBytes(this.in.readInt());
			this.in.readInt();
			byte[] rest = new byte[length - 8];
			this.in.readFully(rest);
			this.lastBody = new String(rest, 0, Math.max(0, rest.length - 2), StandardCharsets.UTF_8);
			return answerId;
		}

		@Override
		public void close() throws IOException {
			this.socket.close();
		}
	}

}
