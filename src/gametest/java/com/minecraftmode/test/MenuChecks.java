package com.minecraftmode.test;

import com.minecraftmode.admin.Admin;
import com.minecraftmode.client.ClientSettings;
import com.minecraftmode.client.menu.AdminScreen;
import com.minecraftmode.client.menu.AdventurerScreen;
import com.minecraftmode.client.menu.MenuClient;
import com.minecraftmode.client.raid.RaidClient;
import com.minecraftmode.economy.Wallet;
import com.minecraftmode.job.JobClass;
import com.minecraftmode.job.JobProgression;
import com.minecraftmode.market.AuctionHouse;
import com.minecraftmode.network.AdminActionPayload;
import com.minecraftmode.network.MenuActionPayload;
import com.minecraftmode.raid.Parties;
import com.minecraftmode.raid.Raids;
import com.minecraftmode.registry.ModItems;
import com.minecraftmode.residence.LandClaims;
import com.minecraftmode.residence.ResidenceDimension;
import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerConnection;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.server.players.NameAndId;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;

/**
 * The adventurer's menu and the admin screen. A party is made and left through the menu's payload, paying moves coins, the settings
 * toggles flip and are saved; a player without operator rights is refused by the admin handler, while the operator gives and sets
 * money, sets a level and a class, summons tagged monsters, teleports onto the surface, takes a market listing back to its seller's
 * mailbox and frees an offline owner's land (deed to the mailbox). Every tab of both screens is screenshotted.
 */
final class MenuChecks {
	private static final GameProfile STRANGER = new GameProfile(UUID.nameUUIDFromBytes("MenuStranger".getBytes()), "Stranger");
	private static final GameProfile SELLER = new GameProfile(UUID.nameUUIDFromBytes("MenuSeller".getBytes()), "Seller");
	private static final int WAIT = 100;
	/** The chunk column (x and z) of the offline owner's plot, far from where the player stands. */
	private static final int LAND_CHUNK = 12;
	private static final int ADVENTURER_TABS = 3;
	private static final int ADMIN_TABS = 5;

	static void run(final ClientGameTestContext context, final TestServerContext server, final TestServerConnection connection) {
		checkParty(context, server, connection);
		checkPay(server, connection);
		checkSettings(context);
		// the test world has no cheats: its player is a plain player until made an operator
		server.runOnServer(s -> require(!Admin.allowed(connection.getServerPlayer()), "without cheats the singleplayer owner is no operator"));
		// (/op is a dedicated-server command)
		server.runOnServer(s -> s.getPlayerList().op(new NameAndId(connection.getServerPlayer().getGameProfile())));
		context.waitFor(mc -> mc.player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER), WAIT);
		checkAdmin(context, server, connection);
		checkScreens(context);
		server.runOnServer(s -> s.getPlayerList().deop(new NameAndId(connection.getServerPlayer().getGameProfile())));
	}

	private static void checkParty(final ClientGameTestContext context, final TestServerContext server, final TestServerConnection connection) {
		context.runOnClient(mc -> ClientPlayNetworking.send(new MenuActionPayload(MenuActionPayload.PARTY_CREATE)));
		context.waitFor(mc -> !RaidClient.party().isEmpty(), WAIT);
		server.runOnServer(s -> require(Parties.of(connection.getServerPlayer().getUUID()) != null, "the menu made a party"));
		context.runOnClient(mc -> ClientPlayNetworking.send(new MenuActionPayload(MenuActionPayload.PARTY_LEAVE)));
		context.waitFor(mc -> RaidClient.party().isEmpty(), WAIT);
		server.runOnServer(s -> require(Parties.of(connection.getServerPlayer().getUUID()) == null, "and left it"));
	}

	private static void checkPay(final TestServerContext server, final TestServerConnection connection) {
		server.runOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			FakePlayer friend = FakePlayer.get(player.level(), STRANGER);
			Wallet.add(player, 500);
			int before = Wallet.balance(player);
			require(Wallet.pay(player, friend, 120), "paying works");
			require(Wallet.balance(player) == before - 120 && Wallet.balance(friend) >= 120, "the coins moved");
			require(!Wallet.pay(player, friend, before * 10), "nobody pays more than they have");
			require(!Wallet.pay(player, player, 10), "nor themselves");
		});
	}

	private static void checkSettings(final ClientGameTestContext context) {
		context.runOnClient(mc -> {
			boolean was = ClientSettings.hpBar;
			ClientSettings.setHpBar(!was);
			require(ClientSettings.hpBar == !was, "the HP bar setting flips");
			ClientSettings.setHpBar(was);
		});
	}

	private static void checkAdmin(final ClientGameTestContext context, final TestServerContext server, final TestServerConnection connection) {
		server.runOnServer(s -> {
			ServerPlayer op = connection.getServerPlayer();
			require(Admin.allowed(op), "the player is an operator now");
			FakePlayer stranger = FakePlayer.get(op.level(), STRANGER);
			require(!Admin.allowed(stranger), "a plain player is not");
			int before = Wallet.balance(op);
			Admin.handle(stranger, new AdminActionPayload(AdminActionPayload.MONEY_GIVE, "", "", 9999, 0, 0));
			require(Wallet.balance(op) == before, "the admin handler refuses a non-operator");

			Admin.handle(op, new AdminActionPayload(AdminActionPayload.MONEY_GIVE, "", "", 300, 0, 0));
			require(Wallet.balance(op) == before + 300, "an operator gives money");
			Admin.handle(op, new AdminActionPayload(AdminActionPayload.MONEY_SET, "", "", 42, 0, 0));
			require(Wallet.balance(op) == 42, "and sets a balance");
			int level = JobProgression.get(op).level();
			Admin.handle(op, new AdminActionPayload(AdminActionPayload.LEVEL_SET, "", "", 33, 0, 0));
			require(JobProgression.get(op).level() == 33, "sets a level");
			JobClass job = JobProgression.get(op).job();
			int tier = JobProgression.get(op).tier();
			Admin.handle(op, new AdminActionPayload(AdminActionPayload.JOB_SET, "", JobClass.PLAYABLE.getFirst().id(), 2, 0, 0));
			require(JobProgression.get(op).job() == JobClass.PLAYABLE.getFirst() && JobProgression.get(op).tier() == 2, "sets a class");
			Admin.handle(op, new AdminActionPayload(AdminActionPayload.JOB_SET, "", job.id(), tier, 0, 0));
			Admin.handle(op, new AdminActionPayload(AdminActionPayload.LEVEL_SET, "", "", level, 0, 0));

			ServerLevel world = op.level();
			Admin.handle(op, new AdminActionPayload(AdminActionPayload.SUMMON, "", "minecraft:zombie", 3, 0, 0));
			var summoned = world.getEntitiesOfClass(Zombie.class, new AABB(op.blockPosition()).inflate(4), z -> z.entityTags().contains(Admin.KEEP_TAG));
			require(summoned.size() == 3, "summons three tagged zombies, got " + summoned.size());
			summoned.forEach(z -> z.discard());
			Admin.handle(op, new AdminActionPayload(AdminActionPayload.SUMMON, "", "minecraft:zombie", Admin.MAX_SUMMON + 1, 0, 0));
			require(world.getEntitiesOfClass(Zombie.class, new AABB(op.blockPosition()).inflate(4)).isEmpty(), "but never more than the cap");
			require(!Raids.stop(-1), "stopping a raid that does not exist does nothing");

			Admin.handle(op, new AdminActionPayload(AdminActionPayload.TP_COORDS, "", "minecraft:overworld", 40, AdminActionPayload.SURFACE, 40));
		});
		context.waitTicks(10);
		server.runOnServer(s -> {
			ServerPlayer op = connection.getServerPlayer();
			ServerLevel world = op.level();
			require(op.blockPosition().getX() == 40 && op.blockPosition().getZ() == 40 && world.getBlockState(op.blockPosition()).isAir()
				&& !world.getBlockState(op.blockPosition().below()).isAir(), "teleports onto the surface at 40, 40, got " + op.blockPosition());
			op.teleportTo(0.5, -60, 0.5);

			AuctionHouse house = AuctionHouse.get(s);
			AuctionHouse.Listing listing = house.add(SELLER.id(), "Seller", new ItemStack(Items.EMERALD, 7), 100, Long.MAX_VALUE);
			Admin.handle(op, new AdminActionPayload(AdminActionPayload.AUCTION_REMOVE, "", "", listing.id(), 0, 0));
			require(house.listing(listing.id()) == null, "a listing is taken off the market");
			require(house.mail(SELLER.id()).items().stream().anyMatch(i -> i.is(Items.EMERALD) && i.getCount() == 7), "and goes to its seller's mailbox");

			ServerLevel residence = ResidenceDimension.level(s);
			LandClaims claims = LandClaims.get(s);
			BlockPos feet = new BlockPos(LAND_CHUNK * 16 + 8, ResidenceDimension.SURFACE_Y + 1, LAND_CHUNK * 16 + 8);
			require(claims.claim(SELLER.id(), "Seller", feet, residence) == LandClaims.Result.CLAIMED, "an offline player owns land");
			LandClaims.Claim claim = claims.owned(SELLER.id()).getFirst();
			// a global action names a player who is not online: it must still run
			Admin.handle(op, new AdminActionPayload(AdminActionPayload.LAND_RELEASE, "Nobody", SELLER.id().toString(), claim.chunkX(), claim.chunkZ(), claim.minY()));
			require(claims.owned(SELLER.id()).size() == (residence.areEntitiesLoaded(claim.chunk()) ? 0 : 1), "land is only freed where its entities are loaded");
			residence.setChunkForced(claim.chunkX(), claim.chunkZ(), true);
		});
		// waitFor runs its predicate on the render thread, which cannot ask the server
		for (int i = 0; i < WAIT && !server.computeOnServer(s -> ResidenceDimension.level(s).areEntitiesLoaded(ChunkPos.pack(LAND_CHUNK, LAND_CHUNK))); i++) {
			context.waitTicks(1);
		}
		server.runOnServer(s -> {
			ServerPlayer op = connection.getServerPlayer();
			LandClaims claims = LandClaims.get(s);
			AuctionHouse house = AuctionHouse.get(s);
			if (!claims.owned(SELLER.id()).isEmpty()) {
				LandClaims.Claim claim = claims.owned(SELLER.id()).getFirst();
				Admin.handle(op, new AdminActionPayload(AdminActionPayload.LAND_RELEASE, "Nobody", SELLER.id().toString(), claim.chunkX(), claim.chunkZ(), claim.minY()));
				ResidenceDimension.level(s).setChunkForced(claim.chunkX(), claim.chunkZ(), false);
			}
			require(claims.owned(SELLER.id()).isEmpty(), "the operator frees it");
			require(house.mail(SELLER.id()).items().stream().anyMatch(i -> i.is(ModItems.LAND_DEED)), "and the deed waits in the owner's mailbox");
			house.takeMail(SELLER.id());
		});
	}

	private static void checkScreens(final ClientGameTestContext context) {
		context.setScreen(AdventurerScreen::new);
		for (int tab = 0; tab < ADVENTURER_TABS; tab++) {
			int index = tab;
			context.runOnClient(mc -> ((AdventurerScreen)mc.gui.screen()).showTab(index));
			context.waitTicks(2);
			context.takeScreenshot("menu_adventurer_" + tab);
		}
		context.runOnClient(mc -> MenuClient.openAdmin());
		context.waitFor(mc -> mc.gui.screen() instanceof AdminScreen admin && admin.loaded(), WAIT);
		for (int tab = 0; tab < ADMIN_TABS; tab++) {
			int index = tab;
			context.runOnClient(mc -> ((AdminScreen)mc.gui.screen()).showTab(index));
			context.waitTicks(2);
			context.takeScreenshot("menu_admin_" + tab);
		}
		context.setScreen(() -> null);
	}


	private static void require(final boolean condition, final String message) {
		if (!condition) {
			throw new AssertionError(message);
		}
	}

	private MenuChecks() {
	}
}
