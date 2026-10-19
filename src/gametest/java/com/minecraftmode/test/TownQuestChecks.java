package com.minecraftmode.test;

import com.minecraftmode.MinecraftMode;
import com.minecraftmode.bag.Bags;
import com.minecraftmode.city.CityServices;
import com.minecraftmode.client.townquest.TownQuestScreen;
import com.minecraftmode.economy.Wallet;
import com.minecraftmode.entity.CityNpc;
import com.minecraftmode.job.JobClass;
import com.minecraftmode.job.JobProgression;
import com.minecraftmode.jump.JumpForest;
import com.minecraftmode.network.TownQuestActionPayload;
import com.minecraftmode.progress.Progress;
import com.minecraftmode.progress.RealReset;
import com.minecraftmode.registry.ModEntities;
import com.minecraftmode.registry.ModItems;
import com.minecraftmode.townquest.TownQuestService;
import com.minecraftmode.townquest.TownQuestService.Status;
import com.minecraftmode.townquest.TownQuests;
import com.minecraftmode.townquest.TownQuests.Goal;
import com.minecraftmode.townquest.TownQuests.Quest;
import com.minecraftmode.worldgen.lair.NamedLairs;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerConnection;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;

/**
 * Town quests from Stormhold's residents (flat world, so the residents are put at posts beside the player; their real posts are in
 * {@code CityChecks}): every resident is kept at its post (one each, wanderers brought back, twins removed), the child is small, the
 * quest list is sane (about twenty, every resident has some, every quest has lang), a kill quest counts real kills and ignores other
 * monsters and quests not taken, a delivery takes the items from the inventory and then the bags, a lair clear and the Forest of
 * Tenacity's finish (fired through {@link JumpForest#FINISHED}) complete theirs, rewards (EXP, coins, stones, the one-time bonus)
 * arrive, network actions need the right resident within reach, daily quests open again the next real day and one-time ones never
 * do; the quest screen is shot in English and Korean.
 */
final class TownQuestChecks {
	private static final CityNpc.Role[] RESIDENTS = {CityNpc.Role.FARMER_WIFE, CityNpc.Role.OLD_MINER, CityNpc.Role.FISHERMAN, CityNpc.Role.VETERAN,
		CityNpc.Role.CHILD, CityNpc.Role.LUMBERJACK};

	static void run(final ClientGameTestContext context, final TestServerContext server, final TestServerConnection connection) {
		server.runCommand("execute in minecraft:overworld run tp @p 40.5 -60 40.5 0 0");
		context.waitTicks(10);
		Map<CityNpc.Role, Integer> ids = server.computeOnServer(s -> posts(s.overworld(), connection.getServerPlayer()));
		definitions(server);
		quests(server, connection);
		network(context, server, connection, ids);
		screens(context, server, connection, ids);
		repeat(server, connection);
		server.runOnServer(s -> {
			for (CityNpc npc : s.overworld().getEntitiesOfClass(CityNpc.class, new AABB(new BlockPos(40, -60, 40)).inflate(16))) {
				npc.discard();
			}
		});
		// back where the town test keeps the player (the next checks set NPCs down beside the origin)
		server.runCommand("execute in minecraft:overworld run tp @p 0.5 -60 0.5 0 0");
		context.waitTicks(5);
	}

	private static BlockPos post(final ServerPlayer player, final int index) {
		// two rows of three beside the player, all within the 8 blocks network actions allow
		return BlockPos.containing(player.getX() + (index % 3 - 1) * 4, player.getY(), player.getZ() + (index < 3 ? 4 : -4));
	}

	/** One resident at each post, kept there like the service NPCs. */
	private static Map<CityNpc.Role, Integer> posts(final ServerLevel level, final ServerPlayer player) {
		Map<CityNpc.Role, Integer> ids = new EnumMap<>(CityNpc.Role.class);
		for (int i = 0; i < RESIDENTS.length; i++) {
			CityNpc.Role role = RESIDENTS[i];
			require(role.isResident(), role.id() + " should be a resident");
			BlockPos home = post(player, i);
			CityServices.keepNpc(level, role, home);
			List<CityNpc> found = level.getEntitiesOfClass(CityNpc.class, new AABB(home).inflate(2), n -> n.role() == role);
			require(found.size() == 1, role.id() + ": keepNpc should spawn exactly one, found " + found.size());
			CityNpc npc = found.getFirst();
			// a wanderer is brought back, a twin removed
			npc.teleportTo(home.getX() + 0.5, home.getY(), home.getZ() + 9.5);
			CityNpc twin = ModEntities.CITY_NPC.create(level, EntitySpawnReason.COMMAND);
			twin.setRole(role);
			twin.snapTo(home.getX() + 1.5, home.getY(), home.getZ() + 0.5, 0.0F, 0.0F);
			level.addFreshEntity(twin);
			CityServices.keepNpc(level, role, home);
			List<CityNpc> after = level.getEntitiesOfClass(CityNpc.class, new AABB(home).inflate(32), n -> n.role() == role && !n.isRemoved());
			require(after.size() == 1, role.id() + ": keepNpc should leave one, found " + after.size());
			require(after.getFirst().blockPosition().distManhattan(home) <= 2, role.id() + " should be back at its post");
			ids.put(role, after.getFirst().getId());
		}
		CityNpc pip = (CityNpc)level.getEntity(ids.get(CityNpc.Role.CHILD));
		require(Math.abs(pip.getScale() - 0.7F) < 0.01F && pip.getBbHeight() < 1.5F, "Little Pip is a child, got scale " + pip.getScale());
		CityNpc martha = (CityNpc)level.getEntity(ids.get(CityNpc.Role.FARMER_WIFE));
		require(Math.abs(martha.getScale() - 1.0F) < 0.01F, "the other residents are full size");
		return ids;
	}

	private static void definitions(final TestServerContext server) {
		String report = server.computeOnServer(s -> {
			List<Quest> all = TownQuests.all();
			require(all.size() >= 18 && all.size() <= 26, "about twenty town quests, got " + all.size());
			for (CityNpc.Role role : RESIDENTS) {
				int count = TownQuests.of(role).size();
				require(count >= 2 && count <= 5, role.id() + " should give 2-5 quests (one screen), got " + count);
			}
			for (CityNpc.Role role : CityNpc.Role.values()) {
				require(role.isResident() == !TownQuests.of(role).isEmpty(), role.id() + ": residents and only residents give quests");
			}
			require(all.stream().filter(q -> !q.daily()).count() >= 1 && all.stream().anyMatch(q -> q.goal() == Goal.JUMP && q.target().equals(JumpForest.TENACITY)),
				"a one-time quest and the Forest of Tenacity quest");
			for (Quest quest : all) {
				if (quest.goal() == Goal.DELIVER) {
					require(TownQuestService.item(quest.target()) != Items.AIR, quest.id() + " delivers an unknown item " + quest.target());
				}
				if (quest.goal() == Goal.KILL) {
					require(net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.containsKey(net.minecraft.resources.Identifier.parse(quest.target())), quest.id() + " hunts an unknown monster " + quest.target());
				}
			}
			return all.size() + " quests";
		});
		MinecraftMode.LOGGER.info("[town] quests: {}", report);
	}

	private static void quests(final TestServerContext server, final TestServerConnection connection) {
		String report = server.computeOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			ServerLevel level = s.overworld();
			long day = RealReset.day(s);
			player.getInventory().clearContent();
			JobProgression.set(player, JobProgression.get(player).withJob(JobClass.WARRIOR, 2).withProgress(30, 0));
			// a kill quest: real kills count, other monsters and quests not taken do not
			Quest night = quest("bjorn_night");
			Quest spiders = quest("pip_spiders");
			require(TownQuestService.status(player, night, day) == Status.AVAILABLE, "the zombie quest is open");
			require(TownQuestService.accept(player, night) && !TownQuestService.accept(player, night), "accepting once");
			kill(level, player, EntityTypes.ZOMBIE);
			kill(level, player, EntityTypes.ZOMBIE);
			kill(level, player, EntityTypes.SKELETON);
			kill(level, player, EntityTypes.SPIDER);
			require(TownQuestService.data(player).progress(night.id()) == 2, "two zombie kills count, got " + TownQuestService.data(player).progress(night.id()));
			require(!TownQuestService.data(player).isActive(spiders.id()), "a spider kill does not take the spider quest");
			TownQuestService.progress(player, Goal.KILL, "minecraft:zombie", 50);
			require(TownQuestService.data(player).progress(night.id()) == night.need(), "progress stops at the need");
			require(TownQuestService.status(player, night, day) == Status.READY, "the zombie quest is ready");
			int wallet = Wallet.balance(player);
			int exp = JobProgression.get(player).exp();
			int stones = Bags.count(player, ModItems.ENHANCEMENT_STONE);
			TownQuestService.Reward reward = TownQuestService.reward(player, night);
			require(TownQuestService.turnIn(player, night) && !TownQuestService.turnIn(player, night), "turning in once");
			require(Wallet.balance(player) - wallet == reward.coins() && reward.coins() > 0, "the coins arrive");
			require(JobProgression.get(player).exp() - exp == reward.exp() && reward.exp() > 0, "the EXP arrives");
			require(Bags.count(player, ModItems.ENHANCEMENT_STONE) - stones == reward.stones(), "the stones arrive");
			require(TownQuestService.status(player, night, day) == Status.DONE, "done for today");

			// a delivery: 20 wheat in the inventory and 12 in a bag make 32
			Quest wheat = quest("martha_wheat");
			require(TownQuestService.accept(player, wheat), "the wheat quest is taken");
			player.getInventory().add(new ItemStack(Items.WHEAT, 20));
			require(TownQuestService.status(player, wheat, day) == Status.ACTIVE && TownQuestService.progress(player, wheat) == 20, "20 of 32 carried");
			ItemStack bag = new ItemStack(ModItems.SUPPLY_BAG);
			Bags.insert(bag, new ItemStack(Items.WHEAT, 14));
			player.getInventory().add(bag);
			require(TownQuestService.status(player, wheat, day) == Status.READY, "inventory and bag together make the delivery");
			require(TownQuestService.turnIn(player, wheat), "the wheat is handed in");
			require(Bags.count(player, Items.WHEAT) == 2, "32 wheat taken (inventory first, then the bag), 2 left, got " + Bags.count(player, Items.WHEAT));

			// a lair clear, through the lair hook
			Quest lair = quest("rowan_lair");
			require(TownQuestService.accept(player, lair), "the lair quest is taken at level 30");
			Progress.lairCleared(player, NamedLairs.all().iterator().next());
			require(TownQuestService.status(player, lair, day) == Status.READY, "a lair clear finishes the lair quest");
			require(TownQuestService.turnIn(player, lair), "the lair quest is handed in");

			// the Forest of Tenacity: reaching the end (the course's FINISHED listeners) finishes the jump quest
			Quest jump = quest("pip_tenacity");
			require(TownQuestService.accept(player, jump), "the jump quest is taken");
			for (JumpForest.Finish listener : JumpForest.FINISHED) {
				listener.finished(player, "some_other_course");
			}
			require(TownQuestService.status(player, jump, day) == Status.ACTIVE, "another course does not count");
			for (JumpForest.Finish listener : JumpForest.FINISHED) {
				listener.finished(player, JumpForest.TENACITY);
			}
			require(TownQuestService.status(player, jump, day) == Status.READY, "the Forest of Tenacity's end finishes the jump quest");
			require(TownQuestService.turnIn(player, jump), "the jump quest is handed in");

			// a one-time quest pays its bonus
			Quest lord = quest("rowan_lord");
			require(!lord.daily() && lord.bonus() == ModItems.AWAKENING_CRYSTAL, "the first lord is a one-time quest with a crystal");
			require(TownQuestService.accept(player, lord), "the lord quest is taken");
			TownQuestService.progress(player, Goal.KILL_LORD, "", 1);
			int crystals = Bags.count(player, ModItems.AWAKENING_CRYSTAL);
			require(TownQuestService.turnIn(player, lord) && Bags.count(player, ModItems.AWAKENING_CRYSTAL) == crystals + 1, "the crystal arrives");

			// level gates and abandoning
			JobProgression.set(player, JobProgression.get(player).withProgress(20, 0));
			require(TownQuestService.status(player, quest("dugan_mythril"), day) == Status.LOCKED, "mythril needs level 25");
			require(!TownQuestService.accept(player, quest("dugan_mythril")), "a locked quest cannot be taken");
			JobProgression.set(player, JobProgression.get(player).withProgress(30, 0));
			Quest seam = quest("dugan_seam");
			require(TownQuestService.accept(player, seam), "the ore quest is taken");
			Progress.oreMined(player);
			require(TownQuestService.data(player).progress(seam.id()) == 1, "a mined ore counts");
			require(TownQuestService.abandon(player, seam) && TownQuestService.status(player, seam, day) == Status.AVAILABLE, "an abandoned quest is open again");
			return "turned in " + TownQuestService.data(player).done().size();
		});
		MinecraftMode.LOGGER.info("[town] town quests: {}", report);
	}

	/** Accept, turn in and abandon go through the network only at the quest's own giver. */
	private static void network(final ClientGameTestContext context, final TestServerContext server, final TestServerConnection connection,
		final Map<CityNpc.Role, Integer> ids) {
		context.runOnClient(minecraft -> {
			ClientPlayNetworking.send(new TownQuestActionPayload(ids.get(CityNpc.Role.CHILD), TownQuestActionPayload.ACCEPT, "pip_spiders"));
			// Pip does not hand out Martha's quests
			ClientPlayNetworking.send(new TownQuestActionPayload(ids.get(CityNpc.Role.CHILD), TownQuestActionPayload.ACCEPT, "martha_harvest"));
		});
		context.waitTicks(5);
		server.runOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			require(TownQuestService.data(player).isActive("pip_spiders"), "Pip hands out the spider quest");
			require(!TownQuestService.data(player).isActive("martha_harvest"), "Pip cannot hand out Martha's quest");
		});
		context.runOnClient(minecraft -> ClientPlayNetworking.send(new TownQuestActionPayload(ids.get(CityNpc.Role.CHILD), TownQuestActionPayload.ABANDON, "pip_spiders")));
		context.waitTicks(5);
		server.runOnServer(s -> require(!TownQuestService.data(connection.getServerPlayer()).isActive("pip_spiders"), "the spider quest is abandoned"));
		context.waitForScreen(TownQuestScreen.class);
		context.runOnClient(minecraft -> minecraft.gui.setScreen(null));
	}

	/** Martha's list with a quest done today, one in progress, one ready and one new, in English and Korean. */
	private static void screens(final ClientGameTestContext context, final TestServerContext server, final TestServerConnection connection,
		final Map<CityNpc.Role, Integer> ids) {
		server.runOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			TownQuestService.accept(player, quest("martha_harvest"));
			TownQuestService.progress(player, Goal.HARVEST, "rice", 7);
			TownQuestService.accept(player, quest("martha_cabbage"));
			player.getInventory().add(new ItemStack(TownQuestService.item("minecraft_mode:napa_cabbage"), 12));
		});
		context.runOnClient(minecraft -> TownQuestScreen.pick("martha_cabbage"));
		String language = context.computeOnClient(minecraft -> minecraft.options.languageCode);
		try {
			for (String code : new String[] {"en_us", "ko_kr"}) {
				setLanguage(context, code);
				server.runOnServer(s -> {
					ServerPlayer player = connection.getServerPlayer();
					ServerPlayNetworking.send(player, TownQuestService.screen(player, ids.get(CityNpc.Role.FARMER_WIFE), CityNpc.Role.FARMER_WIFE));
				});
				context.waitForScreen(TownQuestScreen.class);
				context.waitTicks(10);
				String fits = context.computeOnClient(minecraft -> {
					List<String> out = new ArrayList<>();
					var screen = minecraft.gui.screen();
					int lo = (screen.width - 340) / 2;
					for (var child : screen.children()) {
						if (child instanceof net.minecraft.client.gui.components.AbstractWidget w && (w.getX() < lo || w.getRight() > lo + 340)) {
							out.add(w.getMessage().getString());
						}
					}
					return String.join(", ", out);
				});
				require(fits.isEmpty(), "quest screen buttons outside the panel: " + fits);
				// the cabbage arrived with (or after) the screen: the ready quest offers its turn-in
				boolean turnIn = context.computeOnClient(minecraft -> minecraft.gui.screen().children().stream().anyMatch(child ->
					child instanceof net.minecraft.client.gui.components.AbstractWidget w
						&& w.getMessage().getString().equals(net.minecraft.network.chat.Component.translatable("screen.minecraft_mode.townquest.turn_in").getString())));
				require(turnIn, "the ready cabbage quest should show its turn-in button");
				context.runOnClient(minecraft -> minecraft.gui.toastManager().clear());
				context.takeScreenshot("town_quests_" + code);
				context.runOnClient(minecraft -> minecraft.gui.setScreen(null));
				context.waitTicks(2);
			}
		} finally {
			setLanguage(context, language);
		}
		// the jump quest's page has the button into the forest
		context.runOnClient(minecraft -> TownQuestScreen.pick("pip_tenacity"));
		server.runOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			ServerPlayNetworking.send(player, TownQuestService.screen(player, ids.get(CityNpc.Role.CHILD), CityNpc.Role.CHILD));
		});
		context.waitForScreen(TownQuestScreen.class);
		context.waitTicks(10);
		context.takeScreenshot("town_quests_pip");
		context.runOnClient(minecraft -> minecraft.gui.setScreen(null));
	}

	/** Daily quests open again the next real day; one-time quests never do; an accepted quest is kept over the reset. */
	private static void repeat(final TestServerContext server, final TestServerConnection connection) {
		server.runOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			RealReset.skipDays(s, 1);
			long day = RealReset.day(s);
			require(TownQuestService.status(player, quest("bjorn_night"), day) == Status.AVAILABLE, "the zombie quest is open again the next day");
			require(TownQuestService.status(player, quest("martha_wheat"), day) == Status.AVAILABLE, "the wheat quest is open again the next day");
			require(TownQuestService.status(player, quest("rowan_lord"), day) == Status.DONE, "a one-time quest stays done");
			require(TownQuestService.status(player, quest("martha_harvest"), day) == Status.ACTIVE, "an accepted quest is kept over the reset");
			require(TownQuestService.accept(player, quest("bjorn_night")), "and can be taken again");
			player.getInventory().clearContent();
		});
	}

	private static Quest quest(final String id) {
		Quest quest = TownQuests.get(id);
		require(quest != null, "no town quest " + id);
		return quest;
	}

	private static void kill(final ServerLevel level, final ServerPlayer player, final EntityType<? extends Mob> type) {
		Mob mob = type.create(level, EntitySpawnReason.COMMAND);
		mob.snapTo(player.getX() + 2, player.getY(), player.getZ(), 0, 0);
		level.addFreshEntity(mob);
		mob.hurtServer(level, player.damageSources().playerAttack(player), 10000.0F);
		require(mob.isDeadOrDying(), type + " should die");
	}

	private static void setLanguage(final ClientGameTestContext context, final String code) {
		AtomicReference<CompletableFuture<Void>> reload = new AtomicReference<>();
		context.runOnClient(minecraft -> {
			if (code.equals(minecraft.options.languageCode)) {
				reload.set(CompletableFuture.completedFuture(null));
				return;
			}
			minecraft.options.languageCode = code;
			minecraft.getLanguageManager().setSelected(code);
			reload.set(minecraft.reloadResourcePacks());
		});
		context.waitFor(minecraft -> reload.get().isDone(), 20 * 120);
		// the loading overlay fades out after the reload
		context.waitFor(minecraft -> minecraft.gui.overlay() == null, 20 * 60);
		context.waitTicks(5);
	}

	private static void require(final boolean condition, final String message) {
		if (!condition) {
			throw new AssertionError(message);
		}
	}

	private TownQuestChecks() {
	}
}
