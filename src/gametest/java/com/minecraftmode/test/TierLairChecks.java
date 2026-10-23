package com.minecraftmode.test;

import com.minecraftmode.MinecraftMode;
import com.minecraftmode.accessory.Accessories;
import com.minecraftmode.accessory.AccessoryDef;
import com.minecraftmode.accessory.AccessorySlot;
import com.minecraftmode.bag.Bags;
import com.minecraftmode.client.companion.CompanionScreen;
import com.minecraftmode.client.endgame.EnhanceScreen;
import com.minecraftmode.client.lair.MapCombinerScreen;
import com.minecraftmode.client.lair.TierLairScreen;
import com.minecraftmode.companion.Companions;
import com.minecraftmode.dungeon.DungeonDimension;
import com.minecraftmode.dungeon.Dungeons;
import com.minecraftmode.enhance.EnhanceMenu;
import com.minecraftmode.enhance.Imbue;
import com.minecraftmode.enhance.ImbueSlot;
import com.minecraftmode.entity.MobPower;
import com.minecraftmode.entity.named.NamedDef;
import com.minecraftmode.entity.named.NamedMob;
import com.minecraftmode.entity.named.NamedMobs;
import com.minecraftmode.job.JobClass;
import com.minecraftmode.job.JobData;
import com.minecraftmode.job.JobProgression;
import com.minecraftmode.job.Paragon;
import com.minecraftmode.job.engrave.EngraveStat;
import com.minecraftmode.job.gear.ArmorPieceDef;
import com.minecraftmode.job.gear.ClassArmor;
import com.minecraftmode.job.gear.ClassGear;
import com.minecraftmode.job.gear.GearSlot;
import com.minecraftmode.job.gear.GearStats;
import com.minecraftmode.job.gear.StatLine;
import com.minecraftmode.loot.GearUpgrades;
import com.minecraftmode.registry.ModAttachments;
import com.minecraftmode.registry.ModBlocks;
import com.minecraftmode.registry.ModDataComponents;
import com.minecraftmode.registry.ModItems;
import com.minecraftmode.worldgen.lair.LairDef;
import com.minecraftmode.worldgen.lair.LairTier;
import com.minecraftmode.worldgen.lair.MapCombinerMenu;
import com.minecraftmode.worldgen.lair.NamedLairs;
import com.minecraftmode.worldgen.lair.TierLairs;
import java.util.List;
import java.util.Map;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerConnection;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Tier lairs (0.5.49): the tier numbers and the next-map rolls, a real run (offer screen, confirm, build, packs, named monsters and the
 * lord at Lv 100 times the tier, a fall that wakes at the gate, the lord's kill and its rewards, leaving home and the run closing), the
 * map combiner (merge, raise, material), pet evolution and imbue scrolls (apply, the stat, greater over normal, kept through evolution and
 * inheritance). Screenshots of the offer, the gate, the lord, the combiner, the pet window and the imbue button.
 */
final class TierLairChecks {
	private TierLairChecks() {
	}

	static void run(final ClientGameTestContext context, final TestServerContext server, final TestServerConnection connection) {
		checkNumbers();
		server.runOnServer(s -> {
			ServerPlayer player = s.getPlayerList().getPlayers().getFirst();
			JobProgression.set(player, new JobData(JobClass.WARRIOR, 4, JobProgression.MAX_LEVEL, 0, 0, Map.of()));
			player.getInventory().clearContent();
		});
		checkRun(context, server);
		checkCombiner(context, server);
		checkPets(context, server);
		checkImbue(context, server);
		server.runOnServer(s -> {
			ServerPlayer player = s.getPlayerList().getPlayers().getFirst();
			JobProgression.set(player, JobData.DEFAULT);
			player.setAttached(ModAttachments.COMPANIONS, Companions.Data.DEFAULT);
			player.getInventory().clearContent();
			for (EquipmentSlot slot : EquipmentSlot.values()) {
				player.setItemSlot(slot, ItemStack.EMPTY);
			}
			Accessories.setWorn(player, List.of());
		});
	}

	/** The tables behind tier lairs, and the next-map rolls over many clears. */
	private static void checkNumbers() {
		require(Math.abs(LairTier.health(1) - 1.6F) < 1.0E-4F && Math.abs(LairTier.damage(1) - 1.25F) < 1.0E-4F, "tier 1 should be x1.6 health, x1.25 damage");
		require(Math.abs(LairTier.health(20) - 19.33F) < 0.05F && Math.abs(LairTier.damage(20) - 3.16F) < 0.01F, "tier 20: x" + LairTier.health(20) + " / x" + LairTier.damage(20));
		require(LairTier.affixCount(5) == 0 && LairTier.affixCount(6) == 1 && LairTier.affixCount(11) == 2 && LairTier.affixCount(16) == 3, "affix counts");
		require(LairTier.combineChance(1) == 80 && LairTier.combineChance(9) == 30 && LairTier.combineChance(19) == 5 && LairTier.combineChance(20) == 0, "combine chances");
		require(LairTier.combineCost(9).is(ModItems.CONDENSED_ESSENCE) && LairTier.combineCost(10).is(ModItems.EVOLUTION_ETHER), "combine material by tier");
		require(Math.abs(LairTier.expectedMaps() - 2.12F) < 0.01F, "expected maps " + LairTier.expectedMaps());
		require(LairTier.shards(1) == 1 && LairTier.shards(20) == 6 && LairTier.coreChance(10) == 0.0F && LairTier.greaterChance(10) == 0.0F, "shard and core numbers");
		RandomSource random = RandomSource.create(5L);
		int maps = 0;
		int up = 0;
		int down = 0;
		int rolls = 20000;
		for (int i = 0; i < rolls; i++) {
			for (int tier : LairTier.nextMaps(5, random)) {
				maps++;
				up += tier == 6 ? 1 : 0;
				down += tier == 4 ? 1 : 0;
				require(tier >= 4 && tier <= 6, "a tier 5 clear gives maps of tier 4..6, got " + tier);
			}
		}
		float perClear = maps / (float)rolls;
		require(Math.abs(perClear - LairTier.expectedMaps()) < 0.05F, "about 2.1 maps a clear, got " + perClear);
		require(Math.abs(up / (float)maps - LairTier.upChance(5)) < 0.02F && Math.abs(down / (float)maps - LairTier.downChance(5)) < 0.02F,
			"tier 5 maps go up " + up / (float)maps + ", down " + down / (float)maps);
		for (int tier : LairTier.nextMaps(LairTier.MAX, RandomSource.create(1L))) {
			require(tier <= LairTier.MAX, "no map past the top tier");
		}
		ItemStack map = LairTier.map(3, 2);
		require(LairTier.of(map) == 3 && LairTier.of(new ItemStack(ModItems.LAIR_MAP)) == 0 && map.getCount() == 2, "the tier component");
		MinecraftMode.LOGGER.info("[lair] tier numbers: {} maps a clear, tier 5 up {}%, down {}%", perClear, Math.round(up * 100.0F / maps), Math.round(down * 100.0F / maps));
	}

	/** A real tier 1 run, solo. */
	private static void checkRun(final ClientGameTestContext context, final TestServerContext server) {
		server.runOnServer(s -> {
			ServerPlayer player = s.getPlayerList().getPlayers().getFirst();
			// survival: creative players keep their maps; resistance so the packs cannot add falls before the test's own (kill ignores it)
			player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
			player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.RESISTANCE, 20 * 120, 4, false, false));
			player.setItemInHand(InteractionHand.MAIN_HAND, LairTier.map(1, 1));
			require(TierLairs.offer(player, 1), "a Lv 100 player should be offered the tier lair");
		});
		context.waitForScreen(TierLairScreen.class);
		context.waitTicks(5);
		context.takeScreenshot("tier_lair_offer");
		context.runOnClient(minecraft -> minecraft.gui.setScreen(null));
		int id = server.computeOnServer(s -> {
			ServerPlayer player = s.getPlayerList().getPlayers().getFirst();
			TierLairs.Run run = TierLairs.confirm(player);
			require(run != null, "confirm should start a run");
			require(player.getMainHandItem().isEmpty(), "the map is used up when the run starts");
			return run.id();
		});
		waitForState(context, server, id, TierLairs.State.RUN, 400);
		String report = server.computeOnServer(s -> {
			ServerPlayer player = s.getPlayerList().getPlayers().getFirst();
			TierLairs.Run run = TierLairs.byId(id);
			require(DungeonDimension.is(player.level()) && TierLairs.runOf(player) == run, "the player should be in the tier lair");
			ServerLevel level = (ServerLevel)player.level();
			LairDef def = run.def();
			require(!level.getBlockState(run.piece().goalPos()).is(ModBlocks.LAIR_CHEST), "the goal chest is taken out");
			require(run.mobs().size() >= TierLairs.PACKS, "packs in the corridors, got " + run.mobs().size());
			require(run.lordId() != null && level.getEntity(run.lordId()) instanceof NamedMob, "a lord at the goal");
			NamedMob lord = (NamedMob)level.getEntity(run.lordId());
			NamedDef named = def.named();
			double[] typical = NamedMobs.typicalPower(JobProgression.MAX_LEVEL);
			double effective = lord.getMaxHealth() * MobPower.toughness(lord);
			double wanted = typical[0] * NamedMob.LORD_HEALTH * Dungeons.bossPartyScale(1) * LairTier.health(1) * LairTier.LORD_HEALTH;
			// a lair whose own lord is already stronger than a typical Lv 100 one keeps its own numbers (never weaker)
			require(effective / wanted > 0.95, "the lord should have at least Lv 100 lord health x tier 1: " + Math.round(effective) + " vs " + Math.round(wanted));
			require(lord.effectiveLevel() >= JobProgression.MAX_LEVEL, "the lord fights at Lv 100");
			int elites = 0;
			for (java.util.UUID mob : run.mobs()) {
				if (level.getEntity(mob) instanceof NamedMob elite && MobPower.toughness(elite) > 1.0F) {
					elites++;
				}
			}
			require(elites >= 1, "named monsters roam the corridors");
			player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.NIGHT_VISION, 20 * 120, 0, false, false));
			return def.id() + " (" + named.lo() + "-" + named.hi() + "), " + run.mobs().size() + " monsters, " + elites + " named, lord " + Math.round(effective)
				+ " effective health, damage x" + MobPower.factor(lord) + ", affixes " + run.affixes();
		});
		MinecraftMode.LOGGER.info("[lair] tier run: {}", report);
		server.runCommand("title @p clear");
		context.waitTicks(40);
		context.takeScreenshot("tier_lair_gate");
		// a fall wakes the player at the gate
		server.runOnServer(s -> {
			ServerPlayer player = s.getPlayerList().getPlayers().getFirst();
			player.teleportTo(player.getX(), player.getY(), player.getZ() - 6);
		});
		server.runCommand("kill @p");
		context.waitTicks(5);
		server.runOnServer(s -> {
			ServerPlayer player = s.getPlayerList().getPlayers().getFirst();
			TierLairs.Run run = TierLairs.byId(id);
			require(player.isAlive() && DungeonDimension.is(player.level()), "a fall in a tier lair should not kill");
			require(run.member(player.getUUID()).falls() == 1 && run.member(player.getUUID()).active(), "one fall counted, still in");
			require(player.position().distanceTo(TierLairs.gate(run, 0)) < 2.0, "woken at the gate, at " + player.position());
		});
		server.runCommand("gamemode creative @p");
		server.runCommand("title @p clear");
		// in front of the lord for its picture
		server.runOnServer(s -> {
			ServerPlayer player = s.getPlayerList().getPlayers().getFirst();
			TierLairs.Run run = TierLairs.byId(id);
			NamedMob lord = (NamedMob)((ServerLevel)player.level()).getEntity(run.lordId());
			lord.setNoAi(true);
			player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.NIGHT_VISION, 20 * 60, 0, false, false));
			player.teleportTo(lord.getX(), lord.getY() + 1, lord.getZ() + 4.5);
			player.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, lord.getEyePosition());
		});
		context.waitTicks(40);
		context.takeScreenshot("tier_lair_lord");
		// the lord falls: every member there is paid
		String paid = server.computeOnServer(s -> {
			ServerPlayer player = s.getPlayerList().getPlayers().getFirst();
			TierLairs.Run run = TierLairs.byId(id);
			int paragon = Paragon.get(player).exp() + Paragon.get(player).level() * 1_000_000;
			int shards = Bags.count(player, ModItems.EVOLUTION_SHARD);
			NamedMob lord = (NamedMob)((ServerLevel)player.level()).getEntity(run.lordId());
			lord.kill((ServerLevel)player.level());
			require(run.state() == TierLairs.State.VICTORY, "the lord's death wins the run, state " + run.state());
			int maps = 0;
			for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
				ItemStack stack = player.getInventory().getItem(i);
				if (LairTier.of(stack) > 0) {
					maps += stack.getCount();
					require(LairTier.of(stack) <= 2, "a tier 1 clear gives tier 1 or 2 maps, got " + LairTier.of(stack));
				}
			}
			require(maps >= 1 && maps <= 5, "1..5 next maps, got " + maps);
			require(Bags.count(player, ModItems.EVOLUTION_SHARD) - shards == LairTier.shards(1), "evolution shards");
			int after = Paragon.get(player).exp() + Paragon.get(player).level() * 1_000_000;
			require(after > paragon, "the clear pays experience (paragon at Lv 100)");
			return maps + " maps, " + (after - paragon) + " paragon exp";
		});
		MinecraftMode.LOGGER.info("[lair] tier clear: {}", paid);
		// leaving goes home, and the run closes once nobody is in it
		server.runOnServer(s -> require(Dungeons.leave(s.getPlayerList().getPlayers().getFirst()), "leave from a tier lair"));
		context.waitTicks(40);
		server.runOnServer(s -> {
			ServerPlayer player = s.getPlayerList().getPlayers().getFirst();
			require(player.level().dimension() == Level.OVERWORLD, "home after leaving, in " + player.level().dimension());
			require(TierLairs.byId(id) == null && TierLairs.runs().isEmpty(), "the run closed");
			player.removeAllEffects();
			player.getInventory().clearContent();
		});
	}

	private static void waitForState(final ClientGameTestContext context, final TestServerContext server, final int id, final TierLairs.State state, final int ticks) {
		for (int waited = 0; waited < ticks; waited += 10) {
			boolean there = server.computeOnServer(s -> TierLairs.byId(id) != null && TierLairs.byId(id).state() == state);
			if (there) {
				return;
			}
			context.waitTicks(10);
		}
		throw new AssertionError("tier lair " + id + " never reached " + state);
	}

	/** Cartographer Ysolde's table: three of a tier merge for sure, a raise takes one material and keeps the map on a failure. */
	private static void checkCombiner(final ClientGameTestContext context, final TestServerContext server) {
		server.runOnServer(s -> {
			ServerPlayer player = s.getPlayerList().getPlayers().getFirst();
			player.openMenu(new SimpleMenuProvider((id, inventory, p) -> new MapCombinerMenu(id, inventory, null), Component.translatable("container.minecraft_mode.map_combiner")));
		});
		context.waitForScreen(MapCombinerScreen.class);
		String report = server.computeOnServer(s -> {
			ServerPlayer player = s.getPlayerList().getPlayers().getFirst();
			require(player.containerMenu instanceof MapCombinerMenu, "the combiner should be open");
			MapCombinerMenu menu = (MapCombinerMenu)player.containerMenu;
			menu.slots.get(0).set(LairTier.map(2, 3));
			require(menu.clickMenuButton(player, MapCombinerMenu.BUTTON_MERGE), "three tier 2 maps merge");
			require(LairTier.of(menu.input()) == 3 && menu.input().getCount() == 1, "the merge leaves one tier 3 map, got " + menu.input());
			player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
			require(!menu.clickMenuButton(player, MapCombinerMenu.BUTTON_UPGRADE), "no raise without condensed essence");
			player.getInventory().add(new ItemStack(ModItems.CONDENSED_ESSENCE, 40));
			int attempts = 0;
			while (LairTier.of(menu.input()) == 3 && attempts < 40) {
				require(menu.clickMenuButton(player, MapCombinerMenu.BUTTON_UPGRADE), "a raise with essence");
				attempts++;
				require(!menu.input().isEmpty(), "a failed raise keeps the map");
			}
			require(LairTier.of(menu.input()) == 4, "a raise should succeed within 40 tries at 68%");
			require(com.minecraftmode.economy.Essence.held(player.getInventory(), ModItems.CONDENSED_ESSENCE) == 40 - attempts, "one condensed essence per attempt");
			menu.slots.get(0).set(LairTier.map(10, 1));
			require(!menu.clickMenuButton(player, MapCombinerMenu.BUTTON_UPGRADE), "tier 10 takes Lv 100 ether, not essence");
			player.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
			menu.broadcastChanges();
			return attempts + " attempt(s) from 3 to 4";
		});
		MinecraftMode.LOGGER.info("[lair] combiner: {}", report);
		context.waitTicks(10);
		context.takeScreenshot("tier_map_combiner");
		context.runOnClient(minecraft -> minecraft.gui.setScreen(null));
		context.waitTicks(5);
		server.runOnServer(s -> s.getPlayerList().getPlayers().getFirst().getInventory().clearContent());
	}

	/** Pet evolution: needs Lv 10 and the shards (cores for the third star), multiplies the bonus and is saved per pet. */
	private static void checkPets(final ClientGameTestContext context, final TestServerContext server) {
		server.runOnServer(s -> {
			ServerPlayer player = s.getPlayerList().getPlayers().getFirst();
			player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
			player.setAttached(ModAttachments.COMPANIONS, Companions.Data.DEFAULT);
			require(Companions.learn(player, true, "shadow_cat"), "learn the shadow cat");
			require(!Companions.evolve(player, "shadow_cat"), "a Lv 1 pet cannot evolve");
			Companions.Data data = Companions.data(player);
			player.setAttached(ModAttachments.COMPANIONS, new Companions.Data(Map.of("shadow_cat", Companions.petExpFor(Companions.MAX_PET_LEVEL)), data.mounts(),
				data.activePet(), data.lastMount(), data.evolutions()));
			require(!Companions.evolve(player, "shadow_cat"), "no evolution without shards");
			player.getInventory().add(new ItemStack(ModItems.EVOLUTION_SHARD, 40));
			require(Companions.evolve(player, "shadow_cat") && Companions.evolve(player, "shadow_cat"), "two evolutions with 40 shards");
			require(Companions.data(player).stars("shadow_cat") == 2 && Bags.count(player, ModItems.EVOLUTION_SHARD) == 0, "stars 2, shards used up");
			player.getInventory().add(new ItemStack(ModItems.EVOLUTION_SHARD, 60));
			require(!Companions.evolve(player, "shadow_cat"), "the third star needs cores");
			player.getInventory().add(new ItemStack(ModItems.EVOLUTION_CORE, 3));
			require(Companions.evolve(player, "shadow_cat") && Companions.data(player).stars("shadow_cat") == Companions.MAX_EVOLUTION, "the third star");
			require(Companions.summonPet(player, "shadow_cat"), "summon the cat");
			List<StatLine> lines = Companions.lines(player);
			float crit = lines.stream().filter(l -> l.stat() == EngraveStat.CRIT_CHANCE).map(StatLine::value).findFirst().orElse(0.0F);
			require(Math.abs(crit - 0.2F * Companions.MAX_PET_LEVEL * Companions.evolveScale(3)) < 0.01F, "a ★3 Lv 10 shadow cat gives 7.5% crit, got " + crit);
			require(Math.abs(Companions.evolveScale(1) - 2.1F) < 1.0E-4F && Math.abs(Companions.evolveScale(2) - 2.7F) < 1.0E-4F, "evolution scales");
			// the new field survives a save
			var encoded = Companions.Data.CODEC.encodeStart(net.minecraft.nbt.NbtOps.INSTANCE, Companions.data(player)).getOrThrow();
			require(Companions.Data.CODEC.parse(net.minecraft.nbt.NbtOps.INSTANCE, encoded).getOrThrow().stars("shadow_cat") == 3, "stars are saved");
			var old = new net.minecraft.nbt.CompoundTag();
			old.put("pets", new net.minecraft.nbt.CompoundTag());
			require(Companions.Data.CODEC.parse(net.minecraft.nbt.NbtOps.INSTANCE, old).getOrThrow().evolutions().isEmpty(), "data from before reads as unevolved");
			player.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
		});
		context.runOnClient(minecraft -> minecraft.gui.setScreen(new CompanionScreen()));
		context.waitForScreen(CompanionScreen.class);
		context.waitTicks(20);
		context.takeScreenshot("tier_pet_evolution");
		context.runOnClient(minecraft -> minecraft.gui.setScreen(null));
		server.runOnServer(s -> {
			ServerPlayer player = s.getPlayerList().getPlayers().getFirst();
			Companions.summonPet(player, "");
			player.getInventory().clearContent();
		});
	}

	/** Imbue scrolls on a Lv 100 helmet and necklace: the bench applies them, the stat counts, greater replaces normal, evolution and inheritance keep them. */
	private static void checkImbue(final ClientGameTestContext context, final TestServerContext server) {
		server.runOnServer(s -> {
			ServerPlayer player = s.getPlayerList().getPlayers().getFirst();
			ArmorPieceDef helmet = ClassArmor.pieces(JobClass.WARRIOR).stream().filter(p -> p.slot() == GearSlot.HEAD)
				.max(java.util.Comparator.comparingInt(ArmorPieceDef::level)).orElseThrow();
			player.getInventory().add(new ItemStack(ClassArmor.item(helmet)));
			player.getInventory().add(new ItemStack(ModItems.IMBUE_SCROLLS.get(ImbueSlot.HELMET)));
			player.getInventory().add(new ItemStack(ModItems.IMBUE_SCROLLS.get(ImbueSlot.BOOTS)));
			player.openMenu(new SimpleMenuProvider((id, inventory, p) -> new EnhanceMenu(id, inventory, null), Component.translatable("container.minecraft_mode.enhance")));
		});
		context.waitForScreen(EnhanceScreen.class);
		server.runOnServer(s -> {
			ServerPlayer player = s.getPlayerList().getPlayers().getFirst();
			EnhanceMenu menu = (EnhanceMenu)player.containerMenu;
			for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
				if (ClassArmor.def(player.getInventory().getItem(i)) != null) {
					menu.slots.get(0).set(player.getInventory().getItem(i).copy());
					player.getInventory().setItem(i, ItemStack.EMPTY);
				}
			}
			require(EnhanceMenu.scrollFor(player, menu.input()) == ModItems.IMBUE_SCROLLS.get(ImbueSlot.HELMET), "the helmet scroll fits the helmet (the boots one does not)");
			menu.broadcastChanges();
		});
		context.waitTicks(10);
		context.takeScreenshot("tier_imbue_button");
		String report = server.computeOnServer(s -> {
			ServerPlayer player = s.getPlayerList().getPlayers().getFirst();
			EnhanceMenu menu = (EnhanceMenu)player.containerMenu;
			require(menu.clickMenuButton(player, EnhanceMenu.BUTTON_IMBUE), "imbue the helmet");
			ItemStack piece = menu.input().copy();
			require(Imbue.of(piece) != null && Imbue.of(piece).grade() == Imbue.NORMAL, "the helmet is imbued");
			require(Bags.count(player, ModItems.IMBUE_SCROLLS.get(ImbueSlot.HELMET)) == 0, "the scroll is used up");
			require(EnhanceMenu.scrollFor(player, piece) == null && !Imbue.canApply(piece, ImbueSlot.HELMET, Imbue.NORMAL), "a normal scroll never replaces a line");
			require(Imbue.canApply(piece, ImbueSlot.HELMET, Imbue.GREATER), "a greater scroll may replace a normal line");
			menu.slots.get(0).set(ItemStack.EMPTY);
			player.closeContainer();
			// the stat counts while worn
			float before = GearStats.of(player).get(EngraveStat.CRIT_DAMAGE);
			player.setItemSlot(EquipmentSlot.HEAD, piece);
			GearStats.invalidate(player);
			Float imbued = GearStats.breakdown(player).getOrDefault(EngraveStat.CRIT_DAMAGE, Map.of()).get(GearStats.Source.IMBUE);
			require(imbued != null && Math.abs(imbued - ImbueSlot.HELMET.value(Imbue.NORMAL)) < 1.0E-4F, "the imbued line counts as IMBUE, got " + imbued);
			float worn = GearStats.of(player).get(EngraveStat.CRIT_DAMAGE);
			// greater over normal
			ItemStack greater = piece.copy();
			greater.set(ModDataComponents.IMBUE, new Imbue(ImbueSlot.HELMET, Imbue.GREATER));
			require(!Imbue.canApply(greater, ImbueSlot.HELMET, Imbue.GREATER), "nothing replaces a greater line");
			// evolution keeps it (an armor piece of a lower bracket evolving)
			ArmorPieceDef low = ClassArmor.pieces(JobClass.WARRIOR).stream().filter(p -> p.slot() == GearSlot.HEAD)
				.min(java.util.Comparator.comparingInt(ArmorPieceDef::level)).orElseThrow();
			ItemStack lowPiece = new ItemStack(ClassArmor.item(low));
			lowPiece.set(ModDataComponents.IMBUE, new Imbue(ImbueSlot.HELMET, Imbue.NORMAL));
			ClassGear target = GearUpgrades.targets(ClassGear.of(low)).getFirst();
			require(Imbue.of(GearUpgrades.evolve(lowPiece, target, RandomSource.create(3L))) != null, "evolution keeps the imbued line");
			// accessories: the necklace takes the necklace scroll, inheritance keeps it
			AccessoryDef necklace = Accessories.all().stream().filter(a -> a.slot() == AccessorySlot.NECKLACE)
				.min(java.util.Comparator.comparingInt(a -> a.set().tier())).orElseThrow();
			ItemStack neck = Accessories.create(necklace, 1, RandomSource.create(2L));
			require(Imbue.slotOf(neck) == ImbueSlot.NECKLACE && !Imbue.canApply(neck, ImbueSlot.HELMET, Imbue.NORMAL), "slots of accessories");
			neck.set(ModDataComponents.IMBUE, new Imbue(ImbueSlot.NECKLACE, Imbue.NORMAL));
			List<AccessoryDef> targets = Accessories.inheritTargets(necklace);
			require(!targets.isEmpty() && Imbue.of(Accessories.inherit(neck, targets.getFirst())) != null, "inheritance keeps the imbued line");
			player.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
			return "crit damage " + before + " -> " + worn;
		});
		MinecraftMode.LOGGER.info("[lair] imbue: {}", report);
		context.runOnClient(minecraft -> minecraft.gui.setScreen(null));
		context.waitTicks(5);
	}

	private static void require(final boolean condition, final String message) {
		if (!condition) {
			throw new AssertionError(message);
		}
	}
}
