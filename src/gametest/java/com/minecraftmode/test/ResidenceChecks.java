package com.minecraftmode.test;

import com.minecraftmode.bounty.Bounties;
import com.minecraftmode.city.CityServices;
import com.minecraftmode.client.residence.LandBorders;
import com.minecraftmode.client.residence.LandScreen;
import com.minecraftmode.client.travel.TravelScreen;
import com.minecraftmode.craft.Crops;
import com.minecraftmode.craft.Fishing;
import com.minecraftmode.economy.MarketData;
import com.minecraftmode.economy.ShopMerchant;
import com.minecraftmode.economy.ShopOffers;
import com.minecraftmode.economy.ShopType;
import com.minecraftmode.entity.CityNpc;
import com.minecraftmode.network.LandActionPayload;
import com.minecraftmode.network.TravelPayload;
import com.minecraftmode.registry.ModBlocks;
import com.minecraftmode.registry.ModEntities;
import com.minecraftmode.registry.ModItems;
import com.minecraftmode.residence.IdleGrowth;
import com.minecraftmode.residence.LandClaims;
import com.minecraftmode.residence.LandOffice;
import com.minecraftmode.residence.ResidenceDimension;
import com.minecraftmode.residence.ResidenceRules;
import com.minecraftmode.travel.Destination;
import com.minecraftmode.travel.Travel;
import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerConnection;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.FarmlandBlock;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The residence: the dimension is flat grassland with the arrival square; the waymaster's list (screenshot) takes the player there
 * and a second waymaster on the square takes them back; a deed claims 16x16x16 from under the feet (and is used up), more deeds stack upwards, nobody else can claim
 * in that column, overlaps, the square and the ninth plot are refused. Strangers cannot break, place, open chests or touch armor
 * stands inside a claim, nobody builds on unclaimed land, trusted players can, explosions break nothing, the action bar names the
 * owner, giving land up returns the deed, and the land screen opens (screenshot).
 */
final class ResidenceChecks {
	private static final GameProfile ALICE = new GameProfile(UUID.nameUUIDFromBytes("ResidenceAlice".getBytes()), "Alice");
	private static final GameProfile BOB = new GameProfile(UUID.nameUUIDFromBytes("ResidenceBob".getBytes()), "Bob");
	private static final GameProfile CAROL = new GameProfile(UUID.nameUUIDFromBytes("ResidenceCarol".getBytes()), "Carol");
	private static final int TRAVEL_TICKS = 200;

	static void run(final ClientGameTestContext context, final TestServerContext server, final TestServerConnection connection) {
		checkWorldAndPortals(context, server, connection);
		checkClaims(server);
		checkEdges(context, server, connection);
		checkScreen(context, server, connection);
		checkIdleGrowth(context, server, connection);
	}

	private static final BlockPos FIELD = new BlockPos(40, ResidenceDimension.SURFACE_Y + 1, 104);

	/** A chunk with crops that stops ticking (nobody near) waits to catch up; the missed time ripens them, no time changes nothing. */
	private static void checkIdleGrowth(final ClientGameTestContext context, final TestServerContext server, final TestServerConnection connection) {
		ChunkPos chunk = ChunkPos.containing(FIELD);
		server.runOnServer(s -> {
			ServerLevel level = ResidenceDimension.level(s);
			connection.getServerPlayer().teleportTo(level, FIELD.getX() + 0.5, FIELD.getY(), FIELD.getZ() - 4.5, java.util.Set.of(), 0.0F, 0.0F, true);
		});
		context.waitTicks(40);
		server.runOnServer(s -> {
			ServerLevel level = ResidenceDimension.level(s);
			level.setBlockAndUpdate(FIELD.below(), Blocks.FARMLAND.defaultBlockState().setValue(FarmlandBlock.MOISTURE, FarmlandBlock.MAX_MOISTURE));
			level.setBlockAndUpdate(FIELD, Crops.block(Crops.all().getFirst()).defaultBlockState());
			require(!IdleGrowth.waiting(level, chunk), "a ticking field does not wait");
		});
		server.runCommand("execute in minecraft:overworld run tp @p 0.5 -60 0.5 0 0");
		context.waitTicks(100);
		server.runOnServer(s -> {
			ServerLevel level = ResidenceDimension.level(s);
			require(IdleGrowth.waiting(level, chunk), "a field nobody is near waits to catch up");
			LevelChunk field = level.getChunk(chunk.x(), chunk.z());
			CropBlock crop = (CropBlock)Crops.block(Crops.all().getFirst());
			level.setBlockAndUpdate(FIELD, crop.getStateForAge(0));
			IdleGrowth.catchUp(level, field, 0);
			require(crop.getAge(level.getBlockState(FIELD)) == 0, "no missed time, no growth");
			IdleGrowth.catchUp(level, field, 24000 * 10);
			require(crop.isMaxAge(level.getBlockState(FIELD)), "ten missed days ripen a watered crop");
			level.setBlockAndUpdate(FIELD, Blocks.AIR.defaultBlockState());
			level.setBlockAndUpdate(FIELD.below(), Blocks.GRASS_BLOCK.defaultBlockState());
			// a crop buried in the dirt has no light: it never grows, so catching up skips it
			BlockPos dark = FIELD.below(5);
			level.setBlockAndUpdate(dark.below(), Blocks.FARMLAND.defaultBlockState().setValue(FarmlandBlock.MOISTURE, FarmlandBlock.MAX_MOISTURE));
			level.setBlockAndUpdate(dark, crop.getStateForAge(0));
			require(level.getRawBrightness(dark, 0) < 9, "the buried crop is dark");
			IdleGrowth.catchUp(level, field, 24000 * 10);
			require(crop.getAge(level.getBlockState(dark)) == 0, "a dark crop does not grow");
			level.setBlockAndUpdate(dark, ResidenceDimension.natural(dark.getY()));
			level.setBlockAndUpdate(dark.below(), ResidenceDimension.natural(dark.below().getY()));
		});
	}

	// Alice owns chunk 2,2 (x 32..47), Bob chunk 3,2 (x 48..63): every setup sits on Alice's east edge, x 47.
	private static final int EDGE = 47;
	private static final int WATER_Z = 34;
	private static final int PISTON_Z = 37;
	private static final int DISPENSER_Z = 40;
	private static final int HOPPER_Z = 43;
	private static final int INNER_PISTON_X = 40;

	/** Water, pistons, dispensers and hoppers stop at the edge of the land; player arrows do not hurt what is protected. */
	private static void checkEdges(final ClientGameTestContext context, final TestServerContext server, final TestServerConnection connection) {
		int g = ResidenceDimension.SURFACE_Y;
		int y = g + 1;
		server.runOnServer(s -> {
			ServerLevel level = ResidenceDimension.level(s);
			LandClaims claims = LandClaims.get(s);
			require(claims.claim(ALICE.id(), "Alice", new BlockPos(40, y, 40), level) == LandClaims.Result.CLAIMED, "Alice claims chunk 2,2");
			require(claims.claim(BOB.id(), "Bob", new BlockPos(56, y, 40), level) == LandClaims.Result.CLAIMED, "Bob claims chunk 3,2");
			require(claims.claim(BOB.id(), "Bob", new BlockPos(8, y, 80), level) == LandClaims.Result.SPAWN, "nobody claims the south avenue");
			require(ResidenceRules.crosses(level, new BlockPos(EDGE, y, 40), new BlockPos(EDGE + 1, y, 40)), "x 47 to 48 crosses from Alice to Bob");
			require(!ResidenceRules.crosses(level, new BlockPos(EDGE - 1, y, 40), new BlockPos(EDGE, y, 40)), "inside Alice's land nothing crosses");
			require(ResidenceRules.crosses(level, new BlockPos(40, y, 40), new BlockPos(40, y + 16, 40)), "Alice's land to the open air above crosses");
			// the chunks must tick for water, pistons, dispensers and hoppers
			ServerPlayer player = connection.getServerPlayer();
			player.teleportTo(level, 44.5, y, 20.5, java.util.Set.of(), 0.0F, 0.0F, true);
			// water at the edge
			level.setBlockAndUpdate(new BlockPos(EDGE, y, WATER_Z), Blocks.WATER.defaultBlockState());
			// a piston on the edge pushing east into Bob's land, and one inside pushing north
			level.setBlockAndUpdate(new BlockPos(EDGE, y, PISTON_Z), Blocks.PISTON.defaultBlockState().setValue(PistonBaseBlock.FACING, Direction.EAST));
			level.setBlockAndUpdate(new BlockPos(EDGE, y + 1, PISTON_Z), Blocks.REDSTONE_BLOCK.defaultBlockState());
			level.setBlockAndUpdate(new BlockPos(INNER_PISTON_X, y, PISTON_Z), Blocks.PISTON.defaultBlockState().setValue(PistonBaseBlock.FACING, Direction.NORTH));
			level.setBlockAndUpdate(new BlockPos(INNER_PISTON_X, y, PISTON_Z - 1), Blocks.STONE.defaultBlockState());
			level.setBlockAndUpdate(new BlockPos(INNER_PISTON_X, y + 1, PISTON_Z), Blocks.REDSTONE_BLOCK.defaultBlockState());
			// a dispenser on the edge facing Bob's land with a water bucket
			BlockPos dispenser = new BlockPos(EDGE, y, DISPENSER_Z);
			level.setBlockAndUpdate(dispenser, Blocks.DISPENSER.defaultBlockState().setValue(DispenserBlock.FACING, Direction.EAST));
			((DispenserBlockEntity)level.getBlockEntity(dispenser)).setItem(0, new ItemStack(Items.WATER_BUCKET));
			level.setBlockAndUpdate(dispenser.above(), Blocks.REDSTONE_BLOCK.defaultBlockState());
			// a hopper on the edge pushing into Bob's chest
			level.setBlockAndUpdate(new BlockPos(EDGE + 1, y, HOPPER_Z), Blocks.CHEST.defaultBlockState());
			BlockPos hopper = new BlockPos(EDGE, y, HOPPER_Z);
			level.setBlockAndUpdate(hopper, Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.EAST));
			((HopperBlockEntity)level.getBlockEntity(hopper)).setItem(0, new ItemStack(Items.COBBLESTONE, 4));
		});
		context.waitTicks(60);
		server.runOnServer(s -> {
			ServerLevel level = ResidenceDimension.level(s);
			require(level.getFluidState(new BlockPos(EDGE - 1, y, WATER_Z)).is(FluidTags.WATER), "water still flows on Alice's land");
			require(!level.getFluidState(new BlockPos(EDGE + 1, y, WATER_Z)).is(FluidTags.WATER), "water stops at the edge of Alice's land");
			require(!level.getBlockState(new BlockPos(EDGE, y, PISTON_Z)).getValue(PistonBaseBlock.EXTENDED), "a piston does not push into Bob's land");
			require(level.getBlockState(new BlockPos(INNER_PISTON_X, y, PISTON_Z - 2)).is(Blocks.STONE), "a piston inside Alice's land still pushes");
			require(!level.getFluidState(new BlockPos(EDGE + 1, y, DISPENSER_Z)).is(FluidTags.WATER), "a dispenser does not pour into Bob's land");
			require(((DispenserBlockEntity)level.getBlockEntity(new BlockPos(EDGE, y, DISPENSER_Z))).getItem(0).is(Items.WATER_BUCKET), "the bucket stays in the dispenser");
			require(((ChestBlockEntity)level.getBlockEntity(new BlockPos(EDGE + 1, y, HOPPER_Z))).isEmpty(), "a hopper does not fill Bob's chest");
			// player arrows: Bob cannot hurt Alice's item frame or cow from afar, Alice can hit her cow
			FakePlayer bob = FakePlayer.get(level, BOB);
			FakePlayer alice = FakePlayer.get(level, ALICE);
			ItemFrame frame = new ItemFrame(level, new BlockPos(40, y, 40), Direction.UP);
			require(!frame.hurtServer(level, level.damageSources().playerAttack(bob), 1.0F) && !frame.isRemoved(), "Bob's arrow does not break Alice's item frame");
			Cow cow = new Cow(EntityTypes.COW, level);
			cow.setPos(41.5, y, 41.5);
			level.addFreshEntity(cow);
			float health = cow.getHealth();
			require(!cow.hurtServer(level, level.damageSources().playerAttack(bob), 4.0F) && cow.getHealth() == health, "Bob's arrow does not hurt Alice's cow");
			require(cow.hurtServer(level, level.damageSources().playerAttack(alice), 1.0F), "Alice can hit her own cow");
			cow.discard();
			// clean up
			for (int x = EDGE - 8; x <= EDGE + 8; x++) {
				for (int z = WATER_Z - 8; z <= HOPPER_Z + 8; z++) {
					for (int dy = 0; dy <= 3; dy++) {
						level.setBlock(new BlockPos(x, y + dy, z), Blocks.AIR.defaultBlockState(), 2 | 16);
					}
				}
			}
			LandClaims claims = LandClaims.get(s);
			claims.abandon(ALICE.id(), ChunkPos.pack(2, 2), g);
			claims.abandon(BOB.id(), ChunkPos.pack(3, 2), g);
		});
		server.runCommand("execute in minecraft:overworld run tp @p 0.5 -60 0.5 0 0");
		context.waitTicks(10);
	}

	private static void checkWorldAndPortals(final ClientGameTestContext context, final TestServerContext server, final TestServerConnection connection) {
		server.runOnServer(s -> {
			ServerLevel residence = ResidenceDimension.level(s);
			require(residence != null, "the residence dimension exists");
			require(residence.getBlockState(new BlockPos(40, ResidenceDimension.SURFACE_Y, 40)).is(Blocks.GRASS_BLOCK), "the residence is grassland at the surface");
			require(residence.getBlockState(new BlockPos(40, ResidenceDimension.SURFACE_Y + 1, 40)).isAir(), "nothing grows on the residence");
			require(ShopOffers.trades(ShopType.GENERAL).stream().noneMatch(t -> t.toOffer().getResult().is(ModItems.LAND_DEED)), "deeds are not sold for coins");
			// the merit shop sells deeds, dearer each time
			ServerPlayer buyer = connection.getServerPlayer();
			int deedOffer = -1;
			for (int i = 0; i < Bounties.SHOP.size(); i++) {
				deedOffer = Bounties.SHOP.get(i).id().equals("land_deed") ? i : deedOffer;
			}
			require(deedOffer >= 0, "the merit shop offers land deeds");
			Bounties.Offer offer = Bounties.SHOP.get(deedOffer);
			require(offer.price(Bounties.get(buyer)) == Bounties.DEED_MERIT, "the first deed costs " + Bounties.DEED_MERIT + " merit");
			Bounties.set(buyer, Bounties.get(buyer).withMerit(Bounties.DEED_MERIT * 2 + Bounties.DEED_RAISE));
			require(Bounties.buy(buyer, deedOffer) && Bounties.buy(buyer, deedOffer), "two deeds are bought with merit");
			require(Bounties.get(buyer).merit() == 0 && Bounties.get(buyer).deeds() == 2, "500 then 750 merit were paid");
			require(offer.price(Bounties.get(buyer)) == Bounties.DEED_MERIT + 2 * Bounties.DEED_RAISE, "the third deed costs 1000");
			require(!Bounties.buy(buyer, deedOffer), "no merit, no deed");
			require(buyer.getInventory().countItem(ModItems.LAND_DEED) == 2, "the bought deeds are in the inventory");
			buyer.getInventory().clearContent();
			Bounties.set(buyer, Bounties.get(buyer).withDeeds(0));
			// the waymaster (the test world has no capital, so one is set down by hand)
			CityNpc npc = ModEntities.CITY_NPC.create(s.overworld(), EntitySpawnReason.COMMAND);
			npc.setRole(CityNpc.Role.WAYMASTER);
			npc.snapTo(3.5, -60, 0.5, 90.0F, 0.0F);
			s.overworld().addFreshEntity(npc);
			require(!Destination.PLAZA.offeredIn(s.overworld()) && Destination.RESIDENCE.offeredIn(s.overworld()), "outside the residence the waymaster offers the residence");
			Travel.open(buyer, npc.getId());
		});
		context.waitForScreen(TravelScreen.class);
		context.takeScreenshot("travel_screen");
		int waymaster = server.computeOnServer(s -> s.overworld().getEntitiesOfClass(CityNpc.class, new AABB(3.5, -60, 0.5, 3.5, -60, 0.5).inflate(2),
			n -> n.role() == CityNpc.Role.WAYMASTER).getFirst().getId());
		context.runOnClient(mc -> {
			mc.gui.setScreen(null);
			ClientPlayNetworking.send(new TravelPayload(waymaster, Destination.RESIDENCE.ordinal()));
		});
		context.waitFor(mc -> mc.level != null && mc.level.dimension() == ResidenceDimension.LEVEL, TRAVEL_TICKS);
		context.waitTicks(110);
		int home = server.computeOnServer(s -> {
			ServerLevel residence = ResidenceDimension.level(s);
			ServerPlayer player = connection.getServerPlayer();
			require(player.level() == residence, "the waymaster sends the player to the residence");
			require(player.position().distanceTo(ResidenceDimension.arrival()) < 2.0, "players arrive on the square, got " + player.position());
			require(!residence.getBlockState(BlockPos.containing(ResidenceDimension.arrival()).below()).is(Blocks.GRASS_BLOCK), "the arrival square is paved");
			require(!residence.getBlockState(new BlockPos(8, ResidenceDimension.SURFACE_Y, 80)).is(Blocks.GRASS_BLOCK), "the south avenue is paved");
			require(!Travel.go(player, Destination.RESIDENCE), "the residence is not offered inside the residence");
			java.util.List<CityNpc> keepers = residence.getEntitiesOfClass(CityNpc.class, new AABB(ResidenceDimension.waymaster()).inflate(4),
				n -> n.role() == CityNpc.Role.WAYMASTER);
			require(keepers.size() == 1, "one waymaster stands on the residence square, got " + keepers.size());
			ResidenceDimension.shops().forEach((block, pos) -> require(residence.getBlockState(pos).is(block), block + " stands on the square at " + pos));
			BlockPos farmer = ResidenceDimension.shops().get(ModBlocks.FARMER_SHOP);
			residence.removeBlock(farmer, false);
			ResidenceDimension.keepShops(residence);
			require(residence.getBlockState(farmer).is(ModBlocks.FARMER_SHOP), "a missing stall is put back");
			java.util.List<ShopOffers.Trade> crops = ShopOffers.trades(ShopType.FARMER);
			java.util.List<ShopOffers.Trade> fish = ShopOffers.trades(ShopType.FISHMONGER);
			require(crops.containsAll(Crops.trades()), "the farmer buys every crop and sells the seeds");
			require(fish.containsAll(Fishing.trades()), "the fishmonger buys every fish");
			// the stalls share the grocer's market: a fish sold at the fishmonger counts as sold at the grocer too
			ShopOffers.Trade carp = Fishing.trades().getFirst();
			require(ShopOffers.trades(ShopType.GROCER, player).contains(carp), "the grocer buys " + carp.key());
			String grocerKey = ShopType.GROCER.marketId() + ":" + carp.key();
			int before = MarketData.get(s).pressure(grocerKey, residence.getGameTime());
			ShopMerchant stall = new ShopMerchant(player, residence, ResidenceDimension.shops().get(ModBlocks.FISHMONGER_SHOP), ShopType.FISHMONGER);
			stall.notifyTrade(stall.getOffers().get(ShopOffers.trades(ShopType.FISHMONGER, player).indexOf(carp)));
			int after = MarketData.get(s).pressure(grocerKey, residence.getGameTime());
			require(after == before + 1, "a sale at the fishmonger adds to the grocer's market pressure, got " + before + " -> " + after);
			player.teleportTo(residence, ResidenceDimension.waymaster().getX() + 0.5, ResidenceDimension.SURFACE_Y + 1, ResidenceDimension.waymaster().getZ() + 3.5, java.util.Set.of(), 180.0F, 0.0F, true);
			return keepers.getFirst().getId();
		});
		context.waitTicks(40);
		context.takeScreenshot("residence_square");
		context.runOnClient(mc -> ClientPlayNetworking.send(new TravelPayload(home, Destination.PLAZA.ordinal())));
		context.waitFor(mc -> mc.level != null && mc.level.dimension() == Level.OVERWORLD, TRAVEL_TICKS);
		context.waitTicks(20);
		server.runOnServer(s -> {
			require(connection.getServerPlayer().level() == s.overworld(), "the residence waymaster takes the player back");
			s.overworld().getEntitiesOfClass(CityNpc.class, new AABB(3.5, -60, 0.5, 3.5, -60, 0.5).inflate(2)).forEach(CityNpc::discard);
		});
		server.runCommand("tp @p 0.5 -60 0.5 0 0");
		context.waitTicks(5);
	}

	private static void checkClaims(final TestServerContext server) {
		server.runOnServer(s -> {
			ServerLevel level = ResidenceDimension.level(s);
			LandClaims claims = LandClaims.get(s);
			FakePlayer alice = FakePlayer.get(level, ALICE);
			FakePlayer bob = FakePlayer.get(level, BOB);
			FakePlayer carol = FakePlayer.get(level, CAROL);
			int ground = ResidenceDimension.SURFACE_Y;
			// a deed claims the chunk from under the feet up 16 blocks, and is used up
			alice.setPos(40.5, ground + 1, 40.5);
			alice.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.LAND_DEED, 3));
			InteractionResult used = ModItems.LAND_DEED.use(level, alice, InteractionHand.MAIN_HAND);
			require(used instanceof InteractionResult.Success, "the deed claims the land, got " + used);
			require(alice.getMainHandItem().getCount() == 2, "the deed is used up");
			LandClaims.Claim claim = claims.at(new BlockPos(40, ground, 40));
			require(claim != null && claim.owner().equals(ALICE.id()) && claim.minY() == ground, "the claim starts under the feet");
			require(claims.at(new BlockPos(32, ground + 15, 47)) == claim, "the claim is the whole chunk, 16 high");
			require(claims.at(new BlockPos(40, ground + 16, 40)) == null && claims.at(new BlockPos(40, ground - 1, 40)) == null, "the claim is 16 blocks high");
			require(claims.at(new BlockPos(48, ground, 40)) == null, "the claim stops at the chunk edge");
			// stacking, the column rule, overlaps, the square and the plot limit
			alice.setPos(40.5, ground + 17, 40.5);
			require(ModItems.LAND_DEED.use(level, alice, InteractionHand.MAIN_HAND) instanceof InteractionResult.Success, "a second deed stacks on top");
			require(claims.at(new BlockPos(40, ground + 31, 40)) != null, "the land now reaches 32 blocks up");
			alice.setPos(40.5, ground + 5, 40.5);
			require(claims.claim(ALICE.id(), "Alice", alice.blockPosition(), level) == LandClaims.Result.OVERLAP, "overlapping land is refused");
			require(claims.claim(BOB.id(), "Bob", new BlockPos(44, ground + 60, 44), level) == LandClaims.Result.TAKEN, "nobody else claims in Alice's column");
			require(claims.claim(BOB.id(), "Bob", new BlockPos(3, ground + 1, 3), level) == LandClaims.Result.SPAWN, "the square cannot be claimed");
			for (int cx = 4; claims.owned(ALICE.id()).size() < LandClaims.MAX_CLAIMS; cx++) {
				require(claims.claim(ALICE.id(), "Alice", new BlockPos(cx * 16, ground + 1, 40), level) == LandClaims.Result.CLAIMED, "Alice claims plot at chunk " + cx);
			}
			require(claims.claim(ALICE.id(), "Alice", new BlockPos(200, ground + 1, 200), level) == LandClaims.Result.LIMIT, "the ninth plot is refused");
			// strangers, unclaimed land, operators
			BlockPos inside = new BlockPos(41, ground + 1, 41);
			require(ResidenceRules.blocks(bob, inside) && CityServices.blocksBuilding(bob, inside), "Bob cannot build on Alice's land");
			require(!ResidenceRules.blocks(alice, inside) && !CityServices.blocksBuilding(alice, inside), "Alice builds on her land");
			require(ResidenceRules.blocks(alice, new BlockPos(150, ground + 1, 150)), "nobody builds on unclaimed land");
			level.setBlockAndUpdate(inside, Blocks.CHEST.defaultBlockState());
			BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(inside), Direction.UP, inside, false);
			bob.setPos(41.5, ground + 1, 43.5);
			require(bob.gameMode.useItemOn(bob, level, ItemStack.EMPTY, InteractionHand.MAIN_HAND, hit) instanceof InteractionResult.Fail, "Bob cannot open Alice's chest");
			require(bob.containerMenu == bob.inventoryMenu, "no chest menu opens for Bob");
			BlockPos wall = new BlockPos(43, ground + 1, 43);
			level.setBlockAndUpdate(wall, Blocks.STONE.defaultBlockState());
			require(!bob.gameMode.destroyBlock(wall) && level.getBlockState(wall).is(Blocks.STONE), "Bob cannot break Alice's blocks");
			ArmorStand stand = new ArmorStand(EntityTypes.ARMOR_STAND, level);
			stand.setPos(44.5, ground + 1, 44.5);
			Zombie zombie = new Zombie(EntityTypes.ZOMBIE, level);
			zombie.setPos(44.5, ground + 1, 44.5);
			require(ResidenceRules.blocksEntity(bob, stand), "Bob cannot touch Alice's armor stand");
			require(!ResidenceRules.blocksEntity(bob, zombie), "monsters are fair game anywhere");
			// trust
			require(ResidenceRules.blocks(carol, inside), "Carol is a stranger at first");
			require(claims.trust(ALICE.id(), CAROL.id(), "Carol") && !claims.trust(ALICE.id(), CAROL.id(), "Carol"), "Alice trusts Carol once");
			require(!ResidenceRules.blocks(carol, inside) && !ResidenceRules.blocksEntity(carol, stand), "Carol may build on Alice's land");
			carol.setPos(41.5, ground + 1, 43.5);
			require(carol.gameMode.useItemOn(carol, level, ItemStack.EMPTY, InteractionHand.MAIN_HAND, hit).consumesAction(), "Carol opens Alice's chest");
			carol.closeContainer();
			// the action bar
			require(key(ResidenceRules.label(claims, ALICE.id(), inside)).endsWith("hud.mine"), "Alice sees her land");
			require(key(ResidenceRules.label(claims, CAROL.id(), inside)).endsWith("hud.trusted"), "Carol sees she may build");
			require(key(ResidenceRules.label(claims, BOB.id(), inside)).endsWith("hud.other"), "Bob sees whose land it is");
			require(key(ResidenceRules.label(claims, BOB.id(), new BlockPos(150, ground + 1, 150))).endsWith("hud.wild"), "unclaimed land says so");
			require(key(ResidenceRules.label(claims, BOB.id(), new BlockPos(0, ground + 1, 0))).endsWith("hud.square"), "the square has its own name");
			require(claims.untrust(ALICE.id(), CAROL.id()) && ResidenceRules.blocks(carol, inside), "untrusting takes Carol's rights away");
			// explosions break nothing
			level.explode(null, 45.5, ground + 1, 45.5, 4.0F, Level.ExplosionInteraction.TNT);
			require(level.getBlockState(new BlockPos(45, ground, 45)).is(Blocks.GRASS_BLOCK) && level.getBlockState(wall).is(Blocks.STONE), "explosions break nothing in the residence");
			// giving land up
			require(claims.abandon(ALICE.id(), ChunkPos.pack(2, 2), ground + 16), "Alice gives up the upper plot");
			require(claims.at(new BlockPos(40, ground + 20, 40)) == null && claims.at(inside) != null, "only that plot is gone");
			// clean up for the land screen check
			for (LandClaims.Claim own : claims.owned(ALICE.id())) {
				claims.abandon(ALICE.id(), own.chunk(), own.minY());
			}
			level.removeBlock(inside, false);
			level.removeBlock(wall, false);
		});
	}

	private static void checkScreen(final ClientGameTestContext context, final TestServerContext server, final TestServerConnection connection) {
		server.runOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			LandClaims claims = LandClaims.get(s);
			ServerLevel level = ResidenceDimension.level(s);
			require(claims.claim(player.getUUID(), player.getPlainTextName(), new BlockPos(-40, ResidenceDimension.SURFACE_Y + 1, 40), level) == LandClaims.Result.CLAIMED,
				"the player claims a plot");
			require(claims.claim(player.getUUID(), player.getPlainTextName(), new BlockPos(-40, ResidenceDimension.SURFACE_Y + 17, 40), level) == LandClaims.Result.CLAIMED,
				"and the plot above it");
			claims.trust(player.getUUID(), ALICE.id(), "Alice");
			claims.trust(player.getUUID(), BOB.id(), "Bob");
			LandOffice.open(player);
		});
		context.waitForScreen(LandScreen.class);
		context.takeScreenshot("residence_land_screen");
		context.runOnClient(mc -> mc.gui.setScreen(null));
		// border lines: the player's two stacked plots (green), Alice's land where the player is trusted (yellow), Bob's (red)
		server.runOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			LandClaims claims = LandClaims.get(s);
			ServerLevel level = ResidenceDimension.level(s);
			int y = ResidenceDimension.SURFACE_Y + 1;
			require(claims.claim(ALICE.id(), "Alice", new BlockPos(-24, y, 40), level) == LandClaims.Result.CLAIMED, "Alice claims next door");
			require(claims.claim(BOB.id(), "Bob", new BlockPos(-56, y, 40), level) == LandClaims.Result.CLAIMED, "Bob claims on the other side");
			claims.trust(ALICE.id(), player.getUUID(), player.getPlainTextName());
			player.teleportTo(level, -39.5, y + 10, 78.5, java.util.Set.of(), 180.0F, 12.0F, true);
		});
		context.waitFor(mc -> LandBorders.count() >= 4, TRAVEL_TICKS);
		context.waitTicks(60);
		context.takeScreenshot("residence_borders");
		// giving land up resets it to grassland and hands back what its containers, frames and stands held
		server.runOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			ServerLevel level = ResidenceDimension.level(s);
			int g = ResidenceDimension.SURFACE_Y;
			BlockPos chest = new BlockPos(-40, g + 1, 40);
			level.setBlockAndUpdate(chest, Blocks.CHEST.defaultBlockState());
			((ChestBlockEntity)level.getBlockEntity(chest)).setItem(0, new ItemStack(Items.DIAMOND, 5));
			level.setBlockAndUpdate(new BlockPos(-38, g + 3, 40), Blocks.STONE.defaultBlockState());
			level.setBlockAndUpdate(new BlockPos(-37, g, 41), Blocks.AIR.defaultBlockState());
			ItemFrame frame = new ItemFrame(level, new BlockPos(-36, g + 1, 42), Direction.UP);
			frame.setItem(new ItemStack(Items.EMERALD));
			level.addFreshEntity(frame);
			player.getInventory().clearContent();
			LandOffice.act(player, new LandActionPayload(LandActionPayload.ABANDON, "", -3, 2, g));
			LandClaims claims = LandClaims.get(s);
			require(claims.at(chest) == null && claims.at(new BlockPos(-40, g + 20, 40)) != null, "only the lower plot is given up");
			require(level.getBlockState(chest).isAir() && level.getBlockState(new BlockPos(-38, g + 3, 40)).isAir(), "what was built is gone");
			require(level.getBlockState(new BlockPos(-37, g, 41)).is(Blocks.GRASS_BLOCK) && level.getBlockState(new BlockPos(-40, g, 40)).is(Blocks.GRASS_BLOCK),
				"the ground is filled back in");
			require(frame.isRemoved(), "the item frame is taken down");
			require(player.getInventory().countItem(Items.DIAMOND) == 5 && player.getInventory().countItem(Items.EMERALD) == 1,
				"the chest and the frame's items come back");
			require(player.getInventory().countItem(ModItems.LAND_DEED) == 1, "and the deed");
			player.getInventory().clearContent();
		});
		context.waitForScreen(LandScreen.class);
		context.runOnClient(mc -> mc.gui.setScreen(null));
		server.runOnServer(s -> {
			LandClaims claims = LandClaims.get(s);
			ServerPlayer player = connection.getServerPlayer();
			for (LandClaims.Claim claim : claims.all()) {
				claims.abandon(claim.owner(), claim.chunk(), claim.minY());
			}
			claims.untrust(ALICE.id(), player.getUUID());
		});
		context.waitFor(mc -> LandBorders.count() == 0, TRAVEL_TICKS);
		server.runCommand("execute in minecraft:overworld run tp @p 0.5 -60 0.5 0 0");
		context.waitTicks(10);
	}

	private static String key(final Component text) {
		return text.getContents() instanceof TranslatableContents t ? t.getKey() : "";
	}

	private static void require(final boolean condition, final String message) {
		if (!condition) {
			throw new AssertionError(message);
		}
	}

	private ResidenceChecks() {
	}
}
