package com.minecraftmode.test;

import com.minecraftmode.MinecraftMode;
import com.minecraftmode.bounty.Bounties;
import com.minecraftmode.bounty.BountyData;
import com.minecraftmode.bounty.BountyKind;
import com.minecraftmode.client.endgame.AuctionScreen;
import com.minecraftmode.client.endgame.BountyScreen;
import com.minecraftmode.client.endgame.CodexScreen;
import com.minecraftmode.client.endgame.EnhanceScreen;
import com.minecraftmode.client.endgame.TalentScreen;
import com.minecraftmode.client.raid.RaidScreen;
import com.minecraftmode.economy.Wallet;
import com.minecraftmode.enhance.EnhanceMenu;
import com.minecraftmode.enhance.Enhancement;
import com.minecraftmode.entity.CityNpc;
import com.minecraftmode.entity.boss.RaidBoss;
import com.minecraftmode.entity.named.NamedDef;
import com.minecraftmode.entity.named.NamedMob;
import com.minecraftmode.entity.named.NamedMobs;
import com.minecraftmode.job.JobClass;
import com.minecraftmode.job.JobData;
import com.minecraftmode.job.JobProgression;
import com.minecraftmode.job.engrave.EngraveStat;
import com.minecraftmode.job.gear.ClassGear;
import com.minecraftmode.job.gear.GearStats;
import com.minecraftmode.job.weapon.JobWeapons;
import com.minecraftmode.job.weapon.WeaponDef;
import com.minecraftmode.loot.Coins;
import com.minecraftmode.loot.GearIndex;
import com.minecraftmode.loot.GearUpgrades;
import com.minecraftmode.market.AuctionHouse;
import com.minecraftmode.market.AuctionService;
import com.minecraftmode.network.AuctionActionPayload;
import com.minecraftmode.network.AuctionStatePayload;
import com.minecraftmode.progress.Achievements;
import com.minecraftmode.progress.CollectionBonuses;
import com.minecraftmode.progress.Contribution;
import com.minecraftmode.progress.PlayerRecords;
import com.minecraftmode.progress.Progress;
import com.minecraftmode.progress.ResetCycle;
import com.minecraftmode.progress.Titles;
import com.minecraftmode.raid.BossDef;
import com.minecraftmode.raid.Parties;
import com.minecraftmode.raid.RaidAffix;
import com.minecraftmode.raid.RaidBosses;
import com.minecraftmode.raid.RaidDifficulty;
import com.minecraftmode.raid.RaidInstance;
import com.minecraftmode.raid.RaidRecordsData;
import com.minecraftmode.raid.Raids;
import com.minecraftmode.registry.ModBlocks;
import com.minecraftmode.registry.ModDataComponents;
import com.minecraftmode.registry.ModEntities;
import com.minecraftmode.registry.ModItems;
import com.minecraftmode.talent.TalentTree;
import com.minecraftmode.talent.Talents;
import com.minecraftmode.worldgen.lair.LairChestBlock;
import com.minecraftmode.worldgen.lair.LairChestBlockEntity;
import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.platform.InputConstants;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerConnection;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.scores.PlayerTeam;

/**
 * The endgame loop: the three-day reset cycle, personal lair chests (one roll per player and cycle, kept until taken) and the lair
 * lord that seals them, raid difficulties (unlock order, fee, health and damage, modifiers, reward lockout and practice runs, the
 * record table), guild bounties (progress, hand-in, deliveries, the merit shop, the daily renewal), enhancement (costs, levels,
 * stats, the "+N" name, evolution keeps it), the market (fees, buyout, cancel, mailbox, expiry; other sellers are fake players),
 * achievements, titles and collection bonuses, and talents. Screenshots of every new screen.
 */
public class EndgameClientGameTest implements FabricClientGameTest {
	private static final BossDef BOSS = RaidBosses.ARACHNE;

	@Override
	public void runTest(final ClientGameTestContext context) {
		try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
			TestServerConnection connection = singleplayer.getConnection();
			TestServerContext server = singleplayer.getServer();
			connection.waitForChunksRender();
			server.runCommand("difficulty normal");
			server.runCommand("gamerule send_command_feedback false");
			server.runCommand("gamerule spawn_mobs false");
			server.runCommand("time set noon");
			server.runCommand("gamemode survival @p");
			server.runCommand("tp @p 0.5 -60 0.5 180 0");
			server.runCommand("effect give @p resistance 999999 4 true");
			context.waitTicks(5);
			server.runOnServer(s -> {
				ServerPlayer player = connection.getServerPlayer();
				player.getInventory().clearContent();
				JobProgression.set(player, JobProgression.get(player).withJob(JobClass.WARRIOR, 2).withProgress(30, 0));
				Wallet.add(player, 200 * Coins.GOLD);
			});

			checkLair(context, server, connection);
			checkRaidDifficulty(context, server, connection);
			checkBounties(server, connection);
			checkEnhancement(server, connection);
			checkMarket(server, connection);
			checkKillSharing(server, connection);
			checkTalents(server, connection);
			checkProgress(server, connection);
			screens(context, server, connection);
			checkNewCycle(context, server, connection);
			WorldClose.prepare(context, server);
		}
	}

	// ------------------------------------------------------------------ lairs

	private static final GameProfile ALICE = new GameProfile(UUID.fromString("00000000-0000-0000-0000-00000000a11c"), "Alice");
	private static final BlockPos GOAL = new BlockPos(40, -60, 40);
	private static final BlockPos CACHE = new BlockPos(44, -60, 40);

	private static String contents(final Container container) {
		List<String> out = new ArrayList<>();
		for (int i = 0; i < container.getContainerSize(); i++) {
			ItemStack stack = container.getItem(i);
			if (!stack.isEmpty()) {
				out.add(i + ":" + stack.getItem() + "x" + stack.getCount());
			}
		}
		return String.join(",", out);
	}

	private static void checkLair(final ClientGameTestContext context, final TestServerContext server, final TestServerConnection connection) {
		String report = server.computeOnServer(s -> {
			ServerLevel level = s.overworld();
			ServerPlayer player = connection.getServerPlayer();
			level.setBlockAndUpdate(GOAL, ModBlocks.LAIR_CHEST.defaultBlockState());
			level.setBlockAndUpdate(CACHE, ModBlocks.LAIR_CACHE.defaultBlockState());
			LairChestBlockEntity goal = (LairChestBlockEntity)level.getBlockEntity(GOAL);
			LairChestBlockEntity cache = (LairChestBlockEntity)level.getBlockEntity(CACHE);
			goal.setup("dune_scorpion", 1234L, false);
			cache.setup("dune_scorpion", 99L, true);
			long cycle = ResetCycle.cycle(level);
			// every player rolls their own contents once per cycle
			FakePlayer alice = FakePlayer.get(level, ALICE);
			FakePlayer bob = FakePlayer.get(level, new GameProfile(UUID.fromString("00000000-0000-0000-0000-000000000b0b"), "Bob"));
			Container a = goal.container(alice, cycle);
			Container b = goal.container(bob, cycle);
			String aItems = contents(a);
			require(!aItems.isEmpty() && !contents(b).isEmpty(), "personal rolls should not be empty");
			require(!aItems.equals(contents(b)), "two players should roll different treasure, both got " + aItems);
			require(contents(goal.container(alice, cycle)).equals(aItems), "reopening in the same cycle must show the same contents");
			// taking an item sticks for the rest of the cycle
			int taken = -1;
			for (int i = 0; i < a.getContainerSize() && taken < 0; i++) {
				if (!a.getItem(i).isEmpty()) {
					taken = i;
				}
			}
			a.removeItemNoUpdate(taken);
			a.setChanged();
			require(goal.container(alice, cycle).getItem(taken).isEmpty(), "a taken item should stay taken this cycle");
			require(!goal.fresh(alice, cycle) && goal.fresh(alice, cycle + 1), "the chest should be fresh again next cycle");
			require(!contents(goal.container(alice, cycle + 1)).isEmpty(), "next cycle rolls again");
			require(goal.fresh(bob, cycle), "a roll in a later cycle forgets the earlier cycle's leftovers");
			require(!goal.sealed() && !cache.sealed(), "nothing is sealed before the lord wakes");
			require(goal.lordCycle() != cycle, "no lord before anyone came");
			// the lord wakes when a player comes close
			player.teleportTo(level, GOAL.getX() + 0.5, GOAL.getY(), GOAL.getZ() + 8.5, java.util.Set.of(), 180.0F, 0.0F, true);
			return aItems;
		});
		MinecraftMode.LOGGER.info("[endgame] lair: alice rolled {}", report);
		context.waitTicks(50);
		String lordReport = server.computeOnServer(s -> {
			ServerLevel level = s.overworld();
			ServerPlayer player = connection.getServerPlayer();
			LairChestBlockEntity goal = (LairChestBlockEntity)level.getBlockEntity(GOAL);
			LairChestBlockEntity cache = (LairChestBlockEntity)level.getBlockEntity(CACHE);
			long cycle = ResetCycle.cycle(level);
			require(goal.lordCycle() == cycle && goal.lord() != null, "a player near the goal chest should wake the lord");
			NamedMob lord = goal.lord();
			require(lord.isLord() && goal.sealed() && !goal.victor(player, cycle), "the lord should wake and seal the goal chest");
			require(NamedMobs.def(lord.getType()).id().equals("dune_scorpion"), "the lord should be the lair's named monster");
			require(!cache.sealed(), "caches are never sealed");
			require(lord.getMaxHealth() > NamedMobs.def(lord.getType()).health() * 2.0, "a lord should be much tougher than its kind, has " + lord.getMaxHealth());
			require(!LairChestBlock.open(player, goal), "a sealed chest must not open");
			// the lord falls next to the player: everyone near it may open the treasure this cycle, nobody else
			lord.setNoAi(true);
			lord.hurtServer(level, player.damageSources().playerAttack(player), 1.0E7F);
			require(!lord.isAlive(), "the lord should fall");
			require(goal.victor(player, cycle) && !goal.sealed(), "the player who fought should be a victor");
			FakePlayer alice = FakePlayer.get(level, ALICE);
			require(!goal.victor(alice, cycle), "a player far from the fight is not a victor");
			int before = Progress.get(player).lairClears("dune_scorpion");
			require(LairChestBlock.open(player, goal), "the chest should open after the lord falls");
			player.closeContainer();
			require(Progress.get(player).lairClears("dune_scorpion") == before + 1, "opening the treasure should count as a lair clear");
			require(LairChestBlock.open(player, goal), "it can be opened again this cycle");
			player.closeContainer();
			require(Progress.get(player).lairClears("dune_scorpion") == before + 1, "only the first open of a cycle counts");
			require(Progress.get(player).has("first_lair"), "the first lair clear unlocks an achievement");
			// someone who missed the fight finds it sealed, and a lord rises for them; victors still open it
			require(!LairChestBlock.open(alice, goal), "the treasure stays sealed for someone who did not fight");
			NamedMob second = goal.lord();
			require(second != null && second != lord && second.isLord(), "a new lord should rise for the latecomer");
			require(LairChestBlock.open(player, goal), "a victor still opens it while another lord is up");
			player.closeContainer();
			second.discard();
			player.teleportTo(level, 0.5, -60, 0.5, java.util.Set.of(), 180.0F, 0.0F, true);
			return "lord hp " + lord.getMaxHealth();
		});
		MinecraftMode.LOGGER.info("[endgame] lair: {}", lordReport);
	}

	// ------------------------------------------------------------------ raids

	private static void checkRaidDifficulty(final ClientGameTestContext context, final TestServerContext server, final TestServerConnection connection) {
		int marshal = server.computeOnServer(s -> {
			ServerLevel level = s.overworld();
			CityNpc npc = ModEntities.CITY_NPC.create(level, EntitySpawnReason.COMMAND);
			npc.setRole(CityNpc.Role.RAID_MARSHAL);
			npc.snapTo(2.5, -60, 0.5, 90.0F, 0.0F);
			level.addFreshEntity(npc);
			Parties.create(connection.getServerPlayer());
			return npc.getId();
		});
		String rules = server.computeOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			require(RaidDifficulty.HEROIC.fee(BOSS) > BOSS.fee() && RaidDifficulty.NIGHTMARE.fee(BOSS) > RaidDifficulty.HEROIC.fee(BOSS), "harder raids cost more");
			require(!Raids.tryEnter(player, marshal, BOSS, RaidDifficulty.HEROIC), "Heroic needs a Normal clear first");
			require(!Raids.tryEnter(player, marshal, BOSS, RaidDifficulty.NIGHTMARE), "Nightmare needs a Heroic clear first");
			long cycle = ResetCycle.cycle(player.level());
			Progress.set(player, Progress.get(player).withRaidClear(PlayerRecords.raidKey(BOSS.id(), RaidDifficulty.NORMAL.id()), cycle - 1));
			int wallet = Wallet.balance(player);
			require(Raids.tryEnter(player, marshal, BOSS, RaidDifficulty.HEROIC), "Heroic should open after a Normal clear");
			require(wallet - Wallet.balance(player) == RaidDifficulty.HEROIC.fee(BOSS), "the Heroic fee should be paid");
			RaidInstance instance = Raids.instanceOf(player);
			require(instance != null && instance.difficulty() == RaidDifficulty.HEROIC, "the instance should be Heroic");
			require(instance.affixes().equals(RaidAffix.forCycle(cycle)) && instance.affixes().size() == RaidAffix.PER_CYCLE, "Heroic uses the cycle's modifiers");
			require(!instance.practice(player.getUUID()), "a first Heroic run pays out");
			return instance.affixes().toString();
		});
		MinecraftMode.LOGGER.info("[endgame] heroic modifiers {}", rules);
		connection.waitForChunksRender();
		waitFor(context, server, s -> {
			RaidInstance instance = Raids.instanceOf(connection.getServerPlayer());
			return instance != null && instance.state() == RaidInstance.State.FIGHT;
		}, 400, "the Heroic boss should appear");
		context.waitTicks(20);
		context.takeScreenshot("endgame_heroic_fight");
		String fight = server.computeOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			RaidBoss boss = player.level().getEntitiesOfClass(RaidBoss.class, player.getBoundingBox().inflate(60)).stream().findFirst().orElse(null);
			require(boss != null, "no boss in the arena");
			require(boss.difficulty() == RaidDifficulty.HEROIC, "the boss should know its difficulty");
			float expected = (float)BOSS.health() * RaidDifficulty.HEROIC.health / (boss.affixes().contains(RaidAffix.FORTIFIED) ? 0.85F : 1.0F) / RaidBoss.HEALTH_BAR;
			require(Math.abs(boss.divisor() - expected) < 1.0E-3F, "Heroic toughness should be x" + RaidDifficulty.HEROIC.health + ", divisor " + boss.divisor());
			double damage = boss.getAttributeBaseValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
			require(Math.abs(damage - BOSS.damage() * RaidDifficulty.HEROIC.damage) < 0.01, "Heroic damage should be x" + RaidDifficulty.HEROIC.damage + ", got " + damage);
			boss.setInvulnerableTime(0);
			boss.hurtServer(player.level(), player.damageSources().playerAttack(player), 1.0E8F);
			return boss.isAlive() ? "boss should be dead" : "";
		});
		require(fight.isEmpty(), fight);
		context.waitTicks(10);
		String after = server.computeOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			RaidInstance instance = Raids.instanceOf(player);
			require(instance != null && instance.state() == RaidInstance.State.VICTORY, "the Heroic raid should be won");
			long cycle = ResetCycle.cycle(player.level());
			String key = PlayerRecords.raidKey(BOSS.id(), RaidDifficulty.HEROIC.id());
			PlayerRecords records = Progress.get(player);
			require(records.raidClears(key) == 1 && records.raidLocked(key, cycle), "the Heroic clear should be recorded and locked for the cycle");
			require(records.has("raid_heroic") && records.has("raid_first"), "raid achievements should unlock");
			List<RaidRecordsData.Entry> top = RaidRecordsData.get(s).top(key, 5);
			require(!top.isEmpty() && top.getFirst().names().contains(player.getPlainTextName()), "the clear should enter the record table");
			Raids.leave(player);
			return "time " + Raids.clock(top.getFirst().ticks());
		});
		MinecraftMode.LOGGER.info("[endgame] heroic {}", after);
		waitFor(context, server, s -> Raids.instances().isEmpty(), 100, "the Heroic instance should close");
		context.waitTicks(10);
		// the same raid again this cycle is a practice run: no fee, no rewards
		server.runCommand("execute in minecraft:overworld run tp @p 0.5 -60 0.5 180 0");
		// the marshal's chunk was unloaded while the player was away; it comes back with a new entity id
		waitFor(context, server, s -> !s.overworld().getEntitiesOfClass(CityNpc.class, new net.minecraft.world.phys.AABB(-8, -64, -8, 8, -50, 8)).isEmpty(), 100,
			"the marshal should load again");
		String practice = server.computeOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			int npc = s.overworld().getEntitiesOfClass(CityNpc.class, new net.minecraft.world.phys.AABB(-8, -64, -8, 8, -50, 8)).getFirst().getId();
			int wallet = Wallet.balance(player);
			require(Raids.tryEnter(player, npc, BOSS, RaidDifficulty.HEROIC), "a locked raid can still be run for practice");
			require(Wallet.balance(player) == wallet, "practice runs are free");
			RaidInstance instance = Raids.instanceOf(player);
			require(instance != null && instance.practice(player.getUUID()), "the run should be practice");
			Raids.leave(player);
			return "";
		});
		require(practice.isEmpty(), practice);
		waitFor(context, server, s -> Raids.instances().isEmpty(), 100, "the practice instance should close");
		server.runCommand("execute in minecraft:overworld run tp @p 0.5 -60 0.5 180 0");
		context.waitTicks(10);
		connection.waitForChunksRender();
	}

	// ------------------------------------------------------------------ bounties

	private static void checkBounties(final TestServerContext server, final TestServerConnection connection) {
		String report = server.computeOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			Bounties.ensure(player);
			BountyData data = Bounties.get(player);
			require(data.daily().size() == Bounties.DAILY && !data.special().isEmpty(), "three daily bounties and a cycle bounty, got " + data);
			// a kill bounty counts and pays
			Bounties.set(player, data.with(0, new BountyData.Bounty(BountyKind.KILL_ANY.id(), "", 2, 0, false))
				.with(1, new BountyData.Bounty(BountyKind.DELIVER.id(), "minecraft:wheat", 4, 0, false)));
			Bounties.progress(player, BountyKind.KILL_ANY, "", 1);
			require(!Bounties.claim(player, 0), "an unfinished bounty cannot be handed in");
			Bounties.progress(player, BountyKind.KILL_ANY, "", 1);
			int merit = Bounties.get(player).merit();
			int stones = JobProgression.count(player.getInventory(), ModItems.ENHANCEMENT_STONE);
			int wallet = Wallet.balance(player);
			require(Bounties.claim(player, 0), "a finished bounty should be handed in");
			Bounties.Reward reward = Bounties.reward(false, 30);
			require(Bounties.get(player).merit() >= merit + reward.merit(), "the bounty should pay merit");
			require(JobProgression.count(player.getInventory(), ModItems.ENHANCEMENT_STONE) == stones + reward.stones(), "the bounty should pay stones");
			require(Wallet.balance(player) - wallet == reward.coins(), "the bounty should pay coins");
			require(!Bounties.claim(player, 0), "a bounty pays only once");
			require(Progress.get(player).bountiesDone() == 1 && Progress.get(player).has("bounty_1"), "finished bounties count toward achievements");
			// deliveries take the items when handed in
			require(!Bounties.claim(player, 1), "a delivery without the items cannot be handed in");
			player.getInventory().add(new ItemStack(Items.WHEAT, 6));
			require(Bounties.claim(player, 1), "a delivery with the items should be handed in");
			require(JobProgression.count(player.getInventory(), Items.WHEAT) == 2, "the delivery should take exactly 4 wheat");
			// the merit shop
			Bounties.set(player, Bounties.get(player).withMerit(100));
			int before = JobProgression.count(player.getInventory(), ModItems.PROTECTION_SCROLL);
			int scroll = -1;
			for (int i = 0; i < Bounties.SHOP.size(); i++) {
				if (Bounties.SHOP.get(i).id().equals("protection_scroll")) {
					scroll = i;
				}
			}
			require(Bounties.buy(player, scroll), "merit should buy a protection scroll");
			require(Bounties.get(player).merit() == 100 - Bounties.SHOP.get(scroll).cost(), "the scroll should cost its merit");
			require(JobProgression.count(player.getInventory(), ModItems.PROTECTION_SCROLL) == before + 1, "the scroll should arrive");
			Bounties.set(player, Bounties.get(player).withMerit(0));
			require(!Bounties.buy(player, scroll), "no merit, no purchase");
			return "daily " + data.daily().stream().map(b -> b.kind() + ":" + b.target() + "x" + b.need()).toList() + ", special " + data.special().kind();
		});
		MinecraftMode.LOGGER.info("[endgame] bounties: {}", report);
	}

	// ------------------------------------------------------------------ enhancement

	private static WeaponDef activeWeapon(final JobData data) {
		WeaponDef best = null;
		for (WeaponDef def : JobWeapons.of(JobClass.WARRIOR)) {
			if (JobWeapons.isActive(data, def) && (best == null || def.level() > best.level())) {
				best = def;
			}
		}
		require(best != null, "no usable warrior weapon at level " + data.level());
		return best;
	}

	private static void checkEnhancement(final TestServerContext server, final TestServerConnection connection) {
		String report = server.computeOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			ClassGear gear = ClassGear.of(activeWeapon(JobProgression.get(player)));
			player.getInventory().add(new ItemStack(ModItems.ESSENCE, 64));
			player.getInventory().add(new ItemStack(ModItems.CONDENSED_ESSENCE, 16));
			player.getInventory().add(new ItemStack(ModItems.ENHANCEMENT_STONE, 16));
			EnhanceMenu menu = new EnhanceMenu(0, player.getInventory(), null);
			menu.slots.get(0).set(GearIndex.stack(gear));
			int wallet = Wallet.balance(player);
			int essence = JobProgression.count(player.getInventory(), ModItems.ESSENCE);
			int coins = 0;
			int essenceCost = 0;
			for (int target = 1; target <= 5; target++) {
				coins += Enhancement.coins(gear, target);
				essenceCost += Enhancement.essence(target);
				require(Enhancement.chance(target, 0) == 100, "+" + target + " should always succeed");
				require(menu.clickMenuButton(player, EnhanceMenu.BUTTON_ENHANCE), "attempt at +" + target + " should go through");
				require(menu.result() == EnhanceMenu.RESULT_SUCCESS, "+" + target + " should succeed");
			}
			ItemStack weapon = menu.input().copy();
			require(Enhancement.level(weapon) == 5, "the weapon should be +5, is +" + Enhancement.level(weapon));
			require(wallet - Wallet.balance(player) == coins, "coins paid " + (wallet - Wallet.balance(player)) + ", expected " + coins);
			require(essence - JobProgression.count(player.getInventory(), ModItems.ESSENCE) == essenceCost, "essence paid should match the costs");
			require(weapon.getHoverName().getString().startsWith("+5 "), "the name should show +5, got " + weapon.getHoverName().getString());
			require(Progress.get(player).maxEnhance() == 5 && Progress.get(player).has("enhance_5"), "+5 should count toward achievements");
			// from +6 it takes condensed essence and stones; the rules for failures
			require(Enhancement.condensed(6) && Enhancement.stones(6) == 1 && !Enhancement.risky(10) && Enhancement.risky(11), "cost and risk rules");
			Enhancement failed = new Enhancement(11, 0).failed(true);
			require(failed.level() == 10 && failed.pity() == Enhancement.PITY_STEP, "a risky failure drops a level and builds spirit");
			require(new Enhancement(11, 10).failed(false).level() == 11, "a protected failure keeps the level");
			require(Enhancement.chance(15, 10) == Enhancement.baseRate(15) + 10 && new Enhancement(14, 30).succeeded().pity() == 0, "spirit adds and resets");
			// the enhanced weapon hits harder while held, and evolution keeps the level
			player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, GearIndex.stack(gear));
			GearStats.invalidate(player);
			float plain = GearStats.of(player).get(EngraveStat.BASIC_DAMAGE);
			player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, weapon);
			GearStats.invalidate(player);
			float enhanced = GearStats.of(player).get(EngraveStat.BASIC_DAMAGE);
			require(Math.abs(enhanced - plain - 15.0F) < 0.01F, "+5 should add 15% basic damage, got " + (enhanced - plain));
			List<ClassGear> targets = GearUpgrades.targets(gear);
			require(!targets.isEmpty(), "the weapon should evolve");
			ItemStack evolved = GearUpgrades.evolve(weapon, targets.getFirst(), net.minecraft.util.RandomSource.create(3));
			require(Enhancement.level(evolved) == 5, "evolution should keep the enhancement");
			player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, ItemStack.EMPTY);
			String name = weapon.getHoverName().getString();
			player.getInventory().add(weapon);
			return name + ", +" + (enhanced - plain) + "% basic damage";
		});
		MinecraftMode.LOGGER.info("[endgame] enhancement: {}", report);
	}

	// ------------------------------------------------------------------ market

	private static final UUID SELLER = UUID.fromString("00000000-0000-0000-0000-0000000005e1");

	private static void checkMarket(final TestServerContext server, final TestServerConnection connection) {
		String report = server.computeOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			AuctionHouse house = AuctionHouse.get(s);
			long now = AuctionService.now(s);
			// listing takes the stack and the 1% fee
			player.getInventory().setItem(3, new ItemStack(Items.DIAMOND, 5));
			int wallet = Wallet.balance(player);
			require(AuctionService.list(player, 3, 300), "listing should work");
			require(player.getInventory().getItem(3).isEmpty(), "the listed stack should leave the inventory");
			require(wallet - Wallet.balance(player) == AuctionService.listingFee(300) && AuctionService.listingFee(300) == 3, "the listing fee should be 1%");
			require(house.countBy(player.getUUID()) == 1, "one listing up");
			AuctionHouse.Listing mine = house.listings().stream().filter(l -> l.seller().equals(player.getUUID())).findFirst().orElseThrow();
			require(!AuctionService.buy(player, mine.id()), "you cannot buy your own listing");
			player.getInventory().setItem(4, new ItemStack(ModItems.GOLD_COIN, 3));
			require(!AuctionService.list(player, 4, 10), "coins cannot be listed");
			player.getInventory().setItem(4, ItemStack.EMPTY);
			// buying from another seller pays them 95% into their mailbox
			AuctionHouse.Listing other = house.add(SELLER, "Merchant Kim", new ItemStack(Items.EMERALD, 3), 50, now + AuctionService.DURATION);
			wallet = Wallet.balance(player);
			require(AuctionService.buy(player, other.id()), "buying should work");
			require(wallet - Wallet.balance(player) == 50, "the buyer pays the price");
			require(JobProgression.count(player.getInventory(), Items.EMERALD) == 3, "the bought items should arrive");
			require(house.mail(SELLER).coins() == 48 && house.mail(SELLER).sales() == 1, "the seller should get 95% in the mailbox, got " + house.mail(SELLER));
			require(house.listing(other.id()) == null && !AuctionService.buy(player, other.id()), "a sold listing is gone");
			// cancel returns the item
			require(AuctionService.cancel(player, mine.id()), "cancelling should work");
			require(JobProgression.count(player.getInventory(), Items.DIAMOND) == 5, "the cancelled stack should come back");
			// expiry sends the item to the mailbox, collecting pays out and counts sales
			house.add(player.getUUID(), player.getPlainTextName(), new ItemStack(Items.GOLDEN_APPLE, 2), 90, now - 1);
			house.expire(now);
			require(house.mail(player.getUUID()).items().size() == 1, "an expired listing should go to the mailbox");
			house.sendCoins(player.getUUID(), 120, true);
			wallet = Wallet.balance(player);
			require(AuctionService.claim(player), "collecting the mailbox should work");
			require(Wallet.balance(player) - wallet == 120 && JobProgression.count(player.getInventory(), Items.GOLDEN_APPLE) == 2, "the mailbox should pay out");
			require(house.mail(player.getUUID()).isEmpty(), "the mailbox should be empty after collecting");
			require(Progress.get(player).marketSold() == 1 && Progress.get(player).has("market_1"), "sales count toward achievements");
			// some wares from other sellers for the screenshot
			String[] names = {"Merchant Kim", "Alice", "Bob"};
			ItemStack[] wares = {new ItemStack(ModItems.ENHANCEMENT_STONE, 5), new ItemStack(ModItems.PROTECTION_SCROLL), new ItemStack(Items.DIAMOND_SWORD),
				new ItemStack(ModItems.CONDENSED_ESSENCE, 8), new ItemStack(Items.ENCHANTED_GOLDEN_APPLE), new ItemStack(ModItems.LAIR_MAP, 2),
				GearIndex.stack(GearIndex.shopItems(JobClass.MAGE, 30).getFirst()), new ItemStack(Items.NETHERITE_INGOT, 2)};
			for (int i = 0; i < wares.length; i++) {
				house.add(UUID.nameUUIDFromBytes(names[i % 3].getBytes()), names[i % 3], wares[i], (i + 1) * 47, now + AuctionService.DURATION - i * 5000L);
			}
			// filled boxes and data-heavy items stay off the market
			ItemStack box = new ItemStack(Items.SHULKER_BOX);
			box.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(new ItemStack(Items.DIAMOND, 64))));
			player.getInventory().setItem(5, box);
			require(!AuctionService.list(player, 5, 100) && !player.getInventory().getItem(5).isEmpty(), "a filled shulker box cannot be listed");
			require(AuctionService.sellable(new ItemStack(Items.SHULKER_BOX)), "an empty shulker box can be sold");
			ItemStack heavy = new ItemStack(Items.PAPER);
			CompoundTag tag = new CompoundTag();
			tag.putString("text", "x".repeat(AuctionService.MAX_ITEM_BYTES * 2));
			heavy.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
			player.getInventory().setItem(6, heavy);
			require(AuctionService.tooLarge(heavy, s.registryAccess()) && !AuctionService.list(player, 6, 100), "an item with too much data cannot be listed");
			player.getInventory().setItem(5, ItemStack.EMPTY);
			player.getInventory().setItem(6, ItemStack.EMPTY);
			// more wares than one page: the server pages, sorts, filters and searches
			UUID mason = UUID.nameUUIDFromBytes("Mason".getBytes());
			for (int i = 0; i < 10; i++) {
				house.add(mason, "Mason", new ItemStack(Items.COBBLESTONE, 64), 64 * (2 + i), now + AuctionService.DURATION);
			}
			int active = (int)house.listings().stream().filter(l -> l.expires() > now).count();
			AuctionStatePayload first = AuctionService.state(player, -1, AuctionActionPayload.Query.DEFAULT, -1, -1);
			require(first.total() == active && first.entries().size() == AuctionService.PAGE_SIZE && first.pages() == (active + AuctionService.PAGE_SIZE - 1) / AuctionService.PAGE_SIZE,
				"one page of " + AuctionService.PAGE_SIZE + " out of " + active + ", got " + first.entries().size() + " of " + first.total() + " in " + first.pages() + " pages");
			for (int i = 1; i < first.entries().size(); i++) {
				AuctionStatePayload.Entry a = first.entries().get(i - 1);
				AuctionStatePayload.Entry b = first.entries().get(i);
				require(a.price() / a.item().getCount() <= b.price() / b.item().getCount(), "cheapest per piece first");
			}
			AuctionStatePayload last = AuctionService.state(player, -1, new AuctionActionPayload.Query(false, "", List.of(), false, 0, 0, 99), -1, -1);
			require(last.page() == last.pages() - 1 && !last.entries().isEmpty(), "a page past the end shows the last page");
			AuctionStatePayload cobble = AuctionService.state(player, -1, new AuctionActionPayload.Query(false, "cobble", List.of(), false, 0, 0, 0), -1, -1);
			require(cobble.total() == 10 && cobble.entries().stream().allMatch(e -> e.item().is(Items.COBBLESTONE)), "a search by name finds the cobblestone");
			// a search in the viewer's own language arrives as the matching items
			AuctionStatePayload sword = AuctionService.state(player, -1, new AuctionActionPayload.Query(false, "검", List.of(BuiltInRegistries.ITEM.getId(Items.DIAMOND_SWORD)),
				true, 0, 0, 0), -1, -1);
			require(sword.total() == 1 && sword.entries().getFirst().item().is(Items.DIAMOND_SWORD), "the client's item matches should find the sword, got " + sword.total());
			AuctionStatePayload gear = AuctionService.state(player, -1, new AuctionActionPayload.Query(false, "", List.of(), false,
				AuctionService.Category.GEAR.ordinal(), 0, 0), -1, -1);
			require(gear.total() >= 2 && gear.entries().stream().allMatch(e -> AuctionService.Category.GEAR.test(e.item())), "the gear filter shows only gear");
			AuctionStatePayload dear = AuctionService.state(player, -1, new AuctionActionPayload.Query(false, "cobble", List.of(), false, 0,
				AuctionService.Sort.DEAR.ordinal(), 0), -1, -1);
			require(dear.entries().getFirst().price() == 64 * 11, "priciest first, got " + dear.entries().getFirst().price());
			// the going price for what the player holds
			player.getInventory().setItem(7, new ItemStack(Items.COBBLESTONE, 10));
			require(AuctionService.cheapestEach(player, 7) == 2, "the going price of cobblestone is 2C a piece, got " + AuctionService.cheapestEach(player, 7));
			require(AuctionService.cheapestEach(player, 8) == -1, "nothing to price in an empty slot");
			player.getInventory().setItem(7, ItemStack.EMPTY);
			house.sendCoins(player.getUUID(), 333, true);
			return house.listings().size() + " listings, " + first.pages() + " pages";
		});
		MinecraftMode.LOGGER.info("[endgame] market: {}", report);
	}

	// ------------------------------------------------------------------ talents

	private static void checkKillSharing(final TestServerContext server, final TestServerConnection connection) {
		String report = server.computeOnServer(s -> {
			ServerLevel level = s.overworld();
			ServerPlayer player = connection.getServerPlayer();
			net.minecraft.world.entity.monster.zombie.Zombie dead = EntityTypes.ZOMBIE.create(level, EntitySpawnReason.COMMAND);
			dead.snapTo(player.getX() + 4, player.getY(), player.getZ(), 0.0F, 0.0F);
			FakePlayer near = FakePlayer.get(level, new GameProfile(UUID.nameUUIDFromBytes("Carol".getBytes()), "Carol"));
			near.snapTo(player.getX() + 20, player.getY(), player.getZ(), 0.0F, 0.0F);
			FakePlayer far = FakePlayer.get(level, new GameProfile(UUID.nameUUIDFromBytes("Dave".getBytes()), "Dave"));
			far.snapTo(player.getX() + 200, player.getY(), player.getZ(), 0.0F, 0.0F);
			List<ServerPlayer> shared = Progress.sharers(player, dead, List.of(player, near, far));
			require(shared.size() == 2 && shared.contains(player) && shared.contains(near), "the killer and a party member nearby share the kill, got " + shared);
			require(Progress.sharers(player, dead, List.of(player)).equals(List.of(player)), "alone, only the killer");
			return shared.size() + " credited";
		});
		MinecraftMode.LOGGER.info("[endgame] kill sharing: {}", report);

		String contribution = server.computeOnServer(s -> {
			// the split itself: shares by damage, hitters under 5% dropped, the group bonus
			java.util.Map<String, Float> split = Contribution.split(new java.util.LinkedHashMap<>(java.util.Map.of("a", 40.0F, "b", 60.0F)));
			require(split.keySet().iterator().next().equals("b") && Math.abs(split.get("b") - 0.6F) < 1.0E-4F && Math.abs(split.get("a") - 0.4F) < 1.0E-4F,
				"60:40 damage should split 0.6/0.4 best first, got " + split);
			java.util.Map<String, Float> tagged = Contribution.split(new java.util.LinkedHashMap<>(java.util.Map.of("main", 97.0F, "tap", 3.0F)));
			require(tagged.size() == 1 && tagged.get("main") == 1.0F, "a 3% tap should get nothing, got " + tagged);
			require(Contribution.groupMultiplier(1) == 1.0F && Math.abs(Contribution.groupMultiplier(2) - 1.2F) < 1.0E-4F
				&& Math.abs(Contribution.groupMultiplier(9) - 1.8F) < 1.0E-4F, "group bonus +20% per extra hunter up to five");

			// a real fight: the player and Carol hurt a zombie, Carol finishes it; both share by the health it really lost
			ServerLevel level = s.overworld();
			ServerPlayer player = connection.getServerPlayer();
			FakePlayer carol = FakePlayer.get(level, new GameProfile(UUID.nameUUIDFromBytes("Carol".getBytes()), "Carol"));
			carol.snapTo(player.getX() + 2, player.getY(), player.getZ(), 0.0F, 0.0F);
			net.minecraft.world.entity.monster.zombie.Zombie zombie = EntityTypes.ZOMBIE.create(level, EntitySpawnReason.COMMAND);
			zombie.snapTo(player.getX() + 3, player.getY(), player.getZ(), 0.0F, 0.0F);
			zombie.setNoAi(true);
			level.addFreshEntity(zombie);
			float max = zombie.getMaxHealth();
			zombie.hurtServer(level, player.damageSources().playerAttack(player), 3.0F);
			float byPlayer = max - zombie.getHealth();
			require(zombie.isAlive() && byPlayer > 0.0F && byPlayer < max / 2, "the player's hit should take less than half, took " + byPlayer + " of " + max);
			zombie.hurtServer(level, carol.damageSources().playerAttack(carol), 1000.0F);
			require(!zombie.isAlive(), "the zombie should be dead");
			var dealt = Contribution.dealt(zombie);
			require(Math.abs(dealt.get(player.getUUID()) - byPlayer) < 1.0E-3F && Math.abs(dealt.get(carol.getUUID()) - (max - byPlayer)) < 1.0E-3F,
				"recorded damage should be the health lost, no overkill: player " + byPlayer + ", Carol " + (max - byPlayer) + ", got " + dealt);
			List<Contribution.Share> shares = Contribution.shares(zombie, carol.damageSources().playerAttack(carol));
			require(shares.size() == 2 && shares.getFirst().player() == carol && Math.abs(shares.get(1).fraction() - byPlayer / max) < 1.0E-3F,
				"the player and the killer should share by damage, got " + shares);

			// killed by something else right after a player hit it: the hitter still gets it all; a fresh mob killed by nothing gets nobody
			net.minecraft.world.entity.monster.zombie.Zombie burnt = EntityTypes.ZOMBIE.create(level, EntitySpawnReason.COMMAND);
			burnt.snapTo(player.getX() + 3, player.getY(), player.getZ() + 2, 0.0F, 0.0F);
			burnt.setNoAi(true);
			level.addFreshEntity(burnt);
			burnt.hurtServer(level, player.damageSources().playerAttack(player), 4.0F);
			burnt.hurtServer(level, player.damageSources().magic(), 1000.0F);
			List<Contribution.Share> dot = Contribution.shares(burnt, player.damageSources().magic());
			require(dot.size() == 1 && dot.getFirst().player() == player && dot.getFirst().fraction() == 1.0F, "a damage-over-time kill should count for the hitter, got " + dot);
			net.minecraft.world.entity.monster.zombie.Zombie lone = EntityTypes.ZOMBIE.create(level, EntitySpawnReason.COMMAND);
			lone.snapTo(player.getX() + 3, player.getY(), player.getZ() - 2, 0.0F, 0.0F);
			level.addFreshEntity(lone);
			lone.hurtServer(level, player.damageSources().magic(), 1000.0F);
			require(Contribution.shares(lone, player.damageSources().magic()).isEmpty(), "a monster no player touched counts for nobody");
			net.minecraft.world.entity.monster.zombie.Zombie removed = EntityTypes.ZOMBIE.create(level, EntitySpawnReason.COMMAND);
			removed.snapTo(player.getX() + 5, player.getY(), player.getZ(), 0.0F, 0.0F);
			removed.setNoAi(true);
			level.addFreshEntity(removed);
			removed.hurtServer(level, player.damageSources().playerAttack(player), 2.0F);
			removed.kill(level);
			require(Contribution.shares(removed, removed.damageSources().genericKill()).isEmpty(), "a monster removed with /kill pays nobody, even right after a hit");

			// support: Dave buffs and heals the player and debuffs the monster; he earns credit without hitting it
			FakePlayer dave = FakePlayer.get(level, new GameProfile(UUID.nameUUIDFromBytes("Dave".getBytes()), "Dave"));
			net.minecraft.world.entity.monster.zombie.Zombie fought = EntityTypes.ZOMBIE.create(level, EntitySpawnReason.COMMAND);
			fought.snapTo(player.getX() + 3, player.getY(), player.getZ() + 4, 0.0F, 0.0F);
			fought.setNoAi(true);
			level.addFreshEntity(fought);
			Contribution.buffed(dave, player, 200);
			float start = fought.getHealth();
			fought.hurtServer(level, player.damageSources().playerAttack(player), 3.0F);
			float buffedHit = start - fought.getHealth();
			float fromBuff = Contribution.dealt(fought).getOrDefault(dave.getUUID(), 0.0F);
			require(buffedHit > 0.0F && Math.abs(fromBuff - buffedHit * Contribution.BUFF_SHARE) < 1.0E-3F,
				"a buff should earn " + Contribution.BUFF_SHARE + " of the buffed player's damage " + buffedHit + ", got " + fromBuff);
			Contribution.healed(dave, player, 4.0F);
			float afterHeal = Contribution.dealt(fought).get(dave.getUUID());
			require(afterHeal > fromBuff && afterHeal <= fromBuff + 4.0F * Contribution.HEAL_WEIGHT + 1.0E-3F,
				"healing a fighting player should count toward their fight, got " + fromBuff + " -> " + afterHeal);
			Contribution.healed(dave, player, 0.0F);
			require(Contribution.dealt(fought).get(dave.getUUID()) == afterHeal, "overhealing (nothing restored) counts for nothing");
			Contribution.debuffed(dave, fought, 200);
			fought.damageCooldownTime = 0; // a later blow, not one swallowed by the hurt cooldown of the last
			float before = fought.getHealth();
			fought.hurtServer(level, carol.damageSources().playerAttack(carol), 2.0F);
			float carolHit = before - fought.getHealth();
			float afterDebuff = Contribution.dealt(fought).get(dave.getUUID());
			require(carolHit > 0.0F && Math.abs(afterDebuff - afterHeal - carolHit * Contribution.DEBUFF_SHARE) < 1.0E-3F,
				"a debuff should earn " + Contribution.DEBUFF_SHARE + " of others' damage " + carolHit + ", got " + afterHeal + " -> " + afterDebuff);
			fought.discard();
			return String.format(java.util.Locale.ROOT, "player %.0f%% / Carol %.0f%%", shares.get(1).fraction() * 100, shares.getFirst().fraction() * 100);
		});
		MinecraftMode.LOGGER.info("[endgame] contribution: {}", contribution);
	}

	private static void checkTalents(final TestServerContext server, final TestServerConnection connection) {
		String report = server.computeOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			require(Talents.points(30) == 13 && Talents.points(100) == 60 && Talents.points(9) == 0, "talent points by level");
			int points = Talents.points(JobProgression.get(player).level());
			require(points >= 13 && Talents.available(player) == points, "a level " + JobProgression.get(player).level() + " player should have " + points + " points, has " + Talents.available(player));
			for (JobClass job : JobClass.PLAYABLE) {
				List<TalentTree.Branch> branches = TalentTree.of(job);
				require(branches.size() == 3, job + " should have three branches");
				for (TalentTree.Branch b : branches) {
					require(b.nodes().size() == TalentTree.TIERS, job + "." + b.id() + " should have five tiers");
				}
			}
			GearStats.invalidate(player);
			float before = GearStats.of(player).get(EngraveStat.BASIC_DAMAGE);
			String first = "warrior.fury.0";
			String second = "warrior.fury.1";
			String third = "warrior.fury.2";
			require(!Talents.spend(player, second), "tier 2 is closed with an empty branch");
			for (int i = 0; i < 4; i++) {
				require(Talents.spend(player, first), "rank " + (i + 1) + " of " + first);
			}
			require(Talents.spend(player, second), "four points open tier 2");
			require(!Talents.spend(player, third), "tier 3 needs eight points in the branch");
			require(!Talents.spend(player, "rogue.venom.0"), "another class's talents are off limits");
			GearStats.invalidate(player);
			float after = GearStats.of(player).get(EngraveStat.BASIC_DAMAGE);
			require(Math.abs(after - before - 8.0F) < 0.01F, "four ranks of Heavy Blows should add 8% basic damage, got " + (after - before));
			require(Talents.available(player) == points - 5, "five points spent");
			int wallet = Wallet.balance(player);
			require(Talents.reset(player) && Talents.spent(player) == 0, "a reset should clear the talents");
			require(wallet - Wallet.balance(player) == Talents.resetCost(player), "a reset costs coins");
			// put a few back for the screenshot
			for (int i = 0; i < 5; i++) {
				Talents.spend(player, first);
			}
			Talents.spend(player, "warrior.guard.0");
			Talents.spend(player, "warrior.guard.0");
			Talents.spend(player, second);
			return "spent " + Talents.spent(player);
		});
		MinecraftMode.LOGGER.info("[endgame] talents: {}", report);
	}

	// ------------------------------------------------------------------ achievements, titles, codex

	private static void checkProgress(final TestServerContext server, final TestServerConnection connection) {
		String report = server.computeOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			require(Achievements.all().size() == 48, "expected 48 achievements (31 endgame + 17 later content), got " + Achievements.all().size());
			Progress.check(player);
			PlayerRecords records = Progress.get(player);
			require(records.has("level_25"), "level 30 should unlock the level 25 achievement");
			// a title from an unlocked achievement shows in front of the name
			Achievements.Achievement titled = Achievements.all().stream().filter(Achievements.Achievement::hasTitle).findFirst().orElseThrow();
			require(!Progress.setTitle(player, titled.id()), "a locked title cannot be worn");
			Progress.set(player, records.withAchievement(titled.id()));
			require(Progress.setTitle(player, titled.id()), "an unlocked title can be worn");
			require(Titles.of(player).equals(titled.id()) && player.getDisplayName().getString().startsWith("["), "the title should be in front of the name, got " + player.getDisplayName().getString());
			require(s.getScoreboard().getPlayersTeam(player.getScoreboardName()) == null, "titles must not use scoreboard teams");
			// collection bonuses: two mastered named kinds give boss damage
			GearStats.invalidate(player);
			float boss = GearStats.of(player).get(EngraveStat.BOSS_DAMAGE);
			PlayerRecords more = Progress.get(player);
			List<NamedDef> named = new ArrayList<>(NamedMobs.all());
			for (int i = 0; i < CollectionBonuses.KILLS; i++) {
				more = more.withNamedKill(named.get(0).id()).withNamedKill(named.get(1).id());
			}
			more = more.withNamedKill(named.get(2).id());
			Progress.set(player, more);
			require(CollectionBonuses.masteredKinds(more) == 2, "two kinds should be mastered");
			GearStats.invalidate(player);
			float bossAfter = GearStats.of(player).get(EngraveStat.BOSS_DAMAGE);
			require(Math.abs(bossAfter - boss - 1.0F) < 0.01F, "two mastered kinds should add 1% boss damage, got " + (bossAfter - boss));
			Progress.check(player);
			return Progress.get(player).achievements().size() + " achievements, name " + player.getDisplayName().getString();
		});
		MinecraftMode.LOGGER.info("[endgame] progress: {}", report);
	}

	// ------------------------------------------------------------------ screens

	private static void screens(final ClientGameTestContext context, final TestServerContext server, final TestServerConnection connection) {
		context.runOnClient(minecraft -> minecraft.gui.toastManager().clear());
		context.waitTicks(5);
		// the raid board with this cycle's modifiers and the record table
		RaidScreen.preset(0, RaidDifficulty.HEROIC, false);
		server.runOnServer(s -> ServerPlayNetworking.send(connection.getServerPlayer(), CityNpc.raidScreen(connection.getServerPlayer(), -1)));
		context.waitForScreen(RaidScreen.class);
		context.waitTicks(10);
		shot(context, "endgame_raid_board");
		requireFits(context, "raid board");
		context.runOnClient(minecraft -> minecraft.gui.setScreen(null));
		RaidScreen.preset(0, RaidDifficulty.HEROIC, true);
		server.runOnServer(s -> ServerPlayNetworking.send(connection.getServerPlayer(), CityNpc.raidScreen(connection.getServerPlayer(), -1)));
		context.waitForScreen(RaidScreen.class);
		context.waitTicks(10);
		shot(context, "endgame_raid_records");
		context.runOnClient(minecraft -> minecraft.gui.setScreen(null));
		RaidScreen.preset(0, RaidDifficulty.NORMAL, false);

		// the bounty board
		server.runOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			BountyData data = Bounties.get(player);
			Bounties.set(player, data.with(0, new BountyData.Bounty(BountyKind.KILL_TYPE.id(), "minecraft:zombie", 15, 9, false)).withMerit(64));
			ServerPlayNetworking.send(player, CityNpc.bountyBoard(player, -1));
		});
		context.waitForScreen(BountyScreen.class);
		context.waitTicks(10);
		shot(context, "endgame_bounty_board");
		requireFits(context, "bounty board");
		context.runOnClient(minecraft -> minecraft.gui.setScreen(null));

		// the market: browse, sell and the mailbox
		for (String tab : new String[] {"browse", "sell", "mail"}) {
			AuctionScreen.showTab(tab);
			server.runOnServer(s -> AuctionService.send(connection.getServerPlayer(), -1));
			context.waitForScreen(AuctionScreen.class);
			context.waitTicks(10);
			shot(context, "endgame_market_" + tab);
			requireFits(context, "market " + tab);
			context.runOnClient(minecraft -> minecraft.gui.setScreen(null));
			context.waitTicks(2);
		}
		AuctionScreen.showTab("browse");

		// the enhancement bench with a +5 weapon
		server.runOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			player.openMenu(new SimpleMenuProvider((id, inventory, p) -> new EnhanceMenu(id, inventory, null), Component.translatable("container.minecraft_mode.enhance")));
		});
		context.waitForScreen(EnhanceScreen.class);
		server.runOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			if (player.containerMenu instanceof EnhanceMenu menu) {
				for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
					ItemStack stack = player.getInventory().getItem(i);
					if (stack.has(ModDataComponents.ENHANCEMENT)) {
						menu.slots.get(0).set(stack.copy());
						player.getInventory().setItem(i, ItemStack.EMPTY);
						break;
					}
				}
				menu.broadcastChanges();
			}
		});
		context.waitTicks(10);
		shot(context, "endgame_enhance");
		context.runOnClient(minecraft -> minecraft.gui.setScreen(null));
		context.waitTicks(5);

		// talents and the codex
		context.runOnClient(minecraft -> minecraft.gui.setScreen(new TalentScreen()));
		context.waitForScreen(TalentScreen.class);
		context.waitTicks(5);
		shot(context, "endgame_talents");
		requireFits(context, "talents");
		clickTalent(context, server, connection);
		for (String tab : new String[] {"codex", "achievements", "titles"}) {
			CodexScreen.showTab(tab);
			context.runOnClient(minecraft -> minecraft.gui.setScreen(new CodexScreen()));
			context.waitForScreen(CodexScreen.class);
			context.waitTicks(5);
			shot(context, "endgame_codex_" + tab);
			requireFits(context, "codex " + tab);
		}
		CodexScreen.showTab("codex");
		context.runOnClient(minecraft -> minecraft.gui.setScreen(null));
	}

	// ------------------------------------------------------------------ the next cycle

	private static void checkNewCycle(final ClientGameTestContext context, final TestServerContext server, final TestServerConnection connection) {
		long[] before = server.computeOnServer(s -> new long[] {ResetCycle.day(s.overworld()), ResetCycle.cycle(s.overworld()), Bounties.get(connection.getServerPlayer()).day()});
		server.runCommand("time add " + ResetCycle.CYCLE_TICKS);
		context.waitTicks(5);
		String report = server.computeOnServer(s -> {
			ServerLevel level = s.overworld();
			ServerPlayer player = connection.getServerPlayer();
			long cycle = ResetCycle.cycle(level);
			require(cycle == before[1] + 1 && ResetCycle.day(level) == before[0] + ResetCycle.DAYS, "three days later is the next cycle: day " + before[0] + " -> "
				+ ResetCycle.day(level) + ", cycle " + before[1] + " -> " + cycle);
			require(!Progress.get(player).raidLocked(PlayerRecords.raidKey(BOSS.id(), RaidDifficulty.HEROIC.id()), cycle), "raid rewards open again in a new cycle");
			Bounties.ensure(player);
			BountyData data = Bounties.get(player);
			require(data.day() > before[2] && data.daily().stream().noneMatch(BountyData.Bounty::claimed), "daily bounties renew");
			require(data.cycle() == cycle, "the cycle bounty renews");
			LairChestBlockEntity goal = (LairChestBlockEntity)level.getBlockEntity(GOAL);
			require(goal.fresh(player, cycle) && !goal.victor(player, cycle), "the lair chest refills and has to be won again next cycle");
			require(RaidAffix.forCycle(cycle).size() == RaidAffix.PER_CYCLE, "every cycle has its modifiers");
			return "cycle " + before[1] + " -> " + cycle + ", modifiers " + RaidAffix.forCycle(before[1]) + " -> " + RaidAffix.forCycle(cycle);
		});
		MinecraftMode.LOGGER.info("[endgame] next cycle: {}", report);
	}

	// ------------------------------------------------------------------ helpers

	/** A screenshot without recipe and advancement toasts over the screen. */
	/** A left click on a node through the real mouse input spends a point (SDL numbers the left button 1, not 0). */
	private static void clickTalent(final ClientGameTestContext context, final TestServerContext server, final TestServerConnection connection) {
		String node = "warrior.guard.0";
		int before = server.computeOnServer(s -> Talents.rank(connection.getServerPlayer(), node));
		// the middle of the second branch's first node (TalentScreen: 320 x 236 panel, nodes at x 8 + branch * 102, y 40, 100 x 30)
		double[] pos = context.computeOnClient(minecraft -> {
			double scale = minecraft.getWindow().getGuiScale();
			int left = (minecraft.getWindow().getGuiScaledWidth() - 320) / 2;
			int top = (minecraft.getWindow().getGuiScaledHeight() - 236) / 2;
			return new double[] {(left + 8 + 102 + 50) * scale, (top + 40 + 15) * scale};
		});
		context.getInput().setCursorPos(pos[0], pos[1]);
		context.waitTicks(2);
		context.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT);
		context.waitTicks(10);
		int after = server.computeOnServer(s -> Talents.rank(connection.getServerPlayer(), node));
		require(after == before + 1, "clicking " + node + " should spend a point, rank " + before + " -> " + after);
		require(context.computeOnClient(minecraft -> Talents.rank(minecraft.player, node)) == after, "the client should see the new rank");
	}

	private static void shot(final ClientGameTestContext context, final String name) {
		context.runOnClient(minecraft -> minecraft.gui.toastManager().clear());
		context.waitTicks(2);
		context.takeScreenshot(name);
	}

	private static void requireFits(final ClientGameTestContext context, final String what) {
		String report = context.computeOnClient(minecraft -> {
			net.minecraft.client.gui.screens.Screen screen = minecraft.gui.screen();
			if (screen == null) {
				return what + " is not open";
			}
			int lo = (screen.width - 320) / 2;
			int hi = lo + 320;
			for (var child : screen.children()) {
				if (child instanceof net.minecraft.client.gui.components.AbstractWidget w && (w.getX() < lo || w.getRight() > hi)) {
					return what + ": " + w.getMessage().getString() + " at " + w.getX() + ".." + w.getRight() + " is outside " + lo + ".." + hi;
				}
			}
			return "";
		});
		require(report.isEmpty(), report);
	}

	private static void waitFor(final ClientGameTestContext context, final TestServerContext server, final Function<MinecraftServer, Boolean> condition, final int maxTicks,
		final String message) {
		for (int t = 0; t < maxTicks; t += 5) {
			if (server.computeOnServer(condition::apply)) {
				return;
			}
			context.waitTicks(5);
		}
		throw new AssertionError(message);
	}

	private static void require(final boolean condition, final String message) {
		if (!condition) {
			throw new AssertionError(message);
		}
	}
}
