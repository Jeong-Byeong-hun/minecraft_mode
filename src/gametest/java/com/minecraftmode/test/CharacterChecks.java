package com.minecraftmode.test;

import com.minecraftmode.bounty.Bounties;
import com.minecraftmode.character.CharacterRoster;
import com.minecraftmode.character.Characters;
import com.minecraftmode.character.Lobby;
import com.minecraftmode.city.CityZone;
import com.minecraftmode.client.menu.CharacterSelectScreen;
import com.minecraftmode.city.StarterKit;
import com.minecraftmode.economy.Wallet;
import com.minecraftmode.job.JobClass;
import com.minecraftmode.job.JobProgression;
import com.minecraftmode.market.AuctionService;
import com.minecraftmode.network.MenuActionPayload;
import com.minecraftmode.raid.Parties;
import com.minecraftmode.registry.ModAttachments;
import com.minecraftmode.registry.ModDataComponents;
import java.util.Map;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerConnection;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;

/**
 * Bound rewards and character select. A starter kit piece is bound: no trade cost takes it and the market refuses it. Another slot
 * costs merit, and a world without the capital has no town to switch characters in ({@link #run}, flat world). Inside the capital's
 * walls ({@link #inCity}, from {@code CityChecks}) switching to the new slot starts an empty character (no coins, items or class)
 * that still sees the account's ender chest and codex, and switching back brings the first character's wallet, items, class and
 * place back, on the client too.
 */
final class CharacterChecks {
	private static final int WAIT = 100;
	private static final int COINS = 777;

	static void run(final ClientGameTestContext context, final TestServerContext server, final TestServerConnection connection) {
		checkBound();
		server.runOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			buySlot(player);
			Characters.onJoin(player);
			require(!Characters.choosing(player), "empty slots alone do not send a player to the lobby");
			require(!Characters.switchTo(player, 1), "no capital, no town to switch characters in");
		});
		checkChoosing(context, server, connection);
	}

	/**
	 * The select screen after joining: the player waits in the lobby, the screen opens and stays while choosing, nothing hurts the
	 * player, Done goes on with the current character where it was, and picking another slot switches even outside the capital.
	 */
	private static void checkChoosing(final ClientGameTestContext context, final TestServerContext server, final TestServerConnection connection) {
		double[] before = server.computeOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			// dressed, so the figure on the select screen shows the gear
			player.setItemSlot(EquipmentSlot.HEAD, StarterKit.stack(Items.IRON_HELMET));
			player.setItemSlot(EquipmentSlot.CHEST, StarterKit.stack(Items.IRON_CHESTPLATE));
			player.setItemSlot(EquipmentSlot.LEGS, StarterKit.stack(Items.IRON_LEGGINGS));
			player.setItemSlot(EquipmentSlot.FEET, StarterKit.stack(Items.IRON_BOOTS));
			player.setItemSlot(EquipmentSlot.MAINHAND, StarterKit.stack(Items.IRON_SWORD));
			return new double[] {player.getX(), player.getY(), player.getZ()};
		});
		server.runOnServer(s -> Characters.choose(connection.getServerPlayer()));
		context.waitFor(mc -> mc.gui.screen() instanceof CharacterSelectScreen, WAIT);
		context.waitTicks(40);
		context.takeScreenshot("character_select");
		server.runOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			require(Lobby.is(player.level()), "a choosing player waits in the lobby");
			CharacterRoster.Origin origin = CharacterRoster.get(s).account(player.getUUID()).origin();
			require(origin != null && Math.abs(origin.x() - before[0]) < 0.01 && Math.abs(origin.z() - before[2]) < 0.01, "the lobby remembers where they were");
			// logging out while choosing leaves the player in the lobby; joining again must not take the lobby for their origin
			Characters.onJoin(player);
			require(Characters.choosing(player) && Lobby.is(player.level()), "a player who joins in the lobby chooses again");
			require(CharacterRoster.get(s).account(player.getUUID()).origin().equals(origin), "and keeps the origin");
			float health = player.getHealth();
			player.hurtServer(player.level(), player.damageSources().generic(), 5.0F);
			require(player.getHealth() == health, "nothing hurts a player who is choosing");
		});
		context.runOnClient(mc -> mc.gui.screen().onClose());
		context.waitFor(mc -> mc.gui.screen() == null, WAIT);
		server.runOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			require(!Characters.choosing(player) && !Lobby.is(player.level()), "Done goes on with the current character");
			require(player.distanceToSqr(before[0], before[1], before[2]) < 0.01, "back where it was, got " + player.position());
			require(CharacterRoster.get(s).account(player.getUUID()).origin() == null, "the origin is forgotten once back");
		});
		checkVehicleAndCommands(context, server, connection);

		enter(context, server, connection, 1);
		server.runOnServer(s -> require(CharacterRoster.get(s).account(connection.getServerPlayer().getUUID()).active() == 1,
			"choosing another slot switches, even outside the capital"));
		enter(context, server, connection, 0);
		server.runOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			require(CharacterRoster.get(s).account(player.getUUID()).active() == 0, "and back");
			require(player.getItemBySlot(EquipmentSlot.CHEST).is(Items.IRON_CHESTPLATE), "still dressed");
			player.getInventory().clearContent();
		});
	}

	/** A boat waits where its rider left for the lobby and takes them back on; no command runs while choosing. */
	private static void checkVehicleAndCommands(final ClientGameTestContext context, final TestServerContext server, final TestServerConnection connection) {
		server.runCommand("execute as @p at @p run summon minecraft:oak_boat ~ ~ ~");
		server.runCommand("ride @p mount @e[type=minecraft:oak_boat,limit=1,sort=nearest]");
		context.waitFor(mc -> mc.player.isPassenger(), WAIT);
		server.runOnServer(s -> Characters.choose(connection.getServerPlayer()));
		context.waitFor(mc -> mc.gui.screen() instanceof CharacterSelectScreen, WAIT);
		server.runOnServer(s -> require(!connection.getServerPlayer().isPassenger(), "the boat stays behind"));
		context.runOnClient(mc -> mc.getConnection().sendCommand("party create"));
		context.waitTicks(10);
		server.runOnServer(s -> require(Parties.of(connection.getServerPlayer().getUUID()) == null, "no commands while choosing"));
		context.runOnClient(mc -> mc.gui.screen().onClose());
		context.waitFor(mc -> mc.gui.screen() == null, WAIT);
		context.waitFor(mc -> mc.player.isPassenger(), WAIT);
		server.runCommand("ride @p dismount");
		server.runCommand("kill @e[type=minecraft:oak_boat]");
		context.waitTicks(5);
	}

	private static void enter(final ClientGameTestContext context, final TestServerContext server, final TestServerConnection connection, final int slot) {
		server.runOnServer(s -> Characters.choose(connection.getServerPlayer()));
		context.waitFor(mc -> mc.gui.screen() instanceof CharacterSelectScreen, WAIT);
		context.runOnClient(mc -> ClientPlayNetworking.send(new MenuActionPayload(MenuActionPayload.CHARACTER_ENTER, "", slot)));
		context.waitFor(mc -> mc.gui.screen() == null, WAIT);
	}

	static void inCity(final ClientGameTestContext context, final TestServerContext server, final TestServerConnection connection, final int base) {
		BlockPos spawn = CityZone.spawn(base);
		double x = spawn.getX() + 2.5;
		double z = spawn.getZ() + 0.5;
		server.runOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			buySlot(player);
			player.teleportTo(CityZone.WALL + 20.5, spawn.getY() + 40, 0.5);
			require(!Characters.switchTo(player, 1), "not outside the walls");
			player.teleportTo(x, spawn.getY(), z);
		});
		checkSwitch(context, server, connection, x, z);
	}

	private static void buySlot(final ServerPlayer player) {
		require(!Characters.buySlot(player), "a slot costs merit");
		Bounties.addMerit(player, Characters.SLOT_MERIT);
		require(Characters.buySlot(player), "merit buys a slot");
		require(CharacterRoster.get(player.level().getServer()).account(player.getUUID()).slots() == Characters.BASE_SLOTS + 1
			&& Bounties.get(player).merit() == 0, "one more slot, merit spent");
	}

	private static void checkBound() {
		ItemStack kit = StarterKit.stack(Items.IRON_SWORD);
		require(kit.has(ModDataComponents.BOUND), "starter kit pieces are bound");
		require(new ItemCost(Items.IRON_SWORD).test(new ItemStack(Items.IRON_SWORD)), "a trade takes a plain sword");
		require(!new ItemCost(Items.IRON_SWORD).test(kit), "but never a bound one");
		require(!AuctionService.sellable(kit), "and the market refuses bound items");
	}

	private static void checkSwitch(final ClientGameTestContext context, final TestServerContext server, final TestServerConnection connection,
		final double x, final double z) {
		int[] coins = new int[1];
		server.runOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			player.getInventory().clearContent();
			Wallet.add(player, COINS);
			coins[0] = Wallet.balance(player);
			player.getInventory().add(new ItemStack(Items.DIAMOND, 3));
			player.getEnderChestInventory().setItem(0, new ItemStack(Items.EMERALD, 5));
			player.setAttached(ModAttachments.MONSTER_KILLS, Map.of("minecraft:zombie", 4));
			require(Characters.switchTo(player, 1), "switches to the empty slot");
			require(Wallet.balance(player) == 0 && !player.getInventory().contains(new ItemStack(Items.DIAMOND)), "a new character carries nothing");
			require(JobProgression.get(player).job() == JobClass.NONE && JobProgression.get(player).level() == 1, "and has no class");
			require(player.getEnderChestInventory().getItem(0).is(Items.EMERALD), "the ender chest stays with the account");
			require(player.getAttachedOrElse(ModAttachments.MONSTER_KILLS, Map.of()).getOrDefault("minecraft:zombie", 0) == 4, "so does the codex");
		});
		context.waitFor(mc -> Wallet.balance(mc.player) == 0, WAIT);
		context.runOnClient(mc -> require(!mc.player.getInventory().contains(new ItemStack(Items.DIAMOND)), "the client sees the empty inventory"));
		context.takeScreenshot("character_new");
		server.runOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			player.getEnderChestInventory().setItem(1, new ItemStack(Items.GOLD_INGOT, 2));
			require(Characters.switchTo(player, 0), "switches back");
			require(Wallet.balance(player) == coins[0], "the coins came back, got " + Wallet.balance(player));
			require(player.getInventory().countItem(Items.DIAMOND) == 3, "the diamonds came back");
			require(player.getEnderChestInventory().getItem(1).is(Items.GOLD_INGOT), "what the other character stored is in the ender chest");
			require(Math.abs(player.getX() - x) < 0.01 && Math.abs(player.getZ() - z) < 0.01, "the character is back where it left, got " + player.position());
			player.getEnderChestInventory().clearContent();
			player.getInventory().clearContent();
		});
		context.waitFor(mc -> Wallet.balance(mc.player) == coins[0], WAIT);
	}

	private static void require(final boolean condition, final String message) {
		if (!condition) {
			throw new AssertionError(message);
		}
	}

	private CharacterChecks() {
	}
}
