package com.minecraftmode.test;

import com.minecraftmode.MinecraftMode;
import com.minecraftmode.bag.BagItem;
import com.minecraftmode.bag.BagKind;
import com.minecraftmode.bag.BagMenu;
import com.minecraftmode.bag.Bags;
import com.minecraftmode.bag.Trash;
import com.minecraftmode.client.hud.TargetHealthHud;
import com.minecraftmode.client.hud.TrashButton;
import com.minecraftmode.dungeon.DungeonDimension;
import com.minecraftmode.economy.ShopOffers;
import com.minecraftmode.economy.ShopType;
import com.minecraftmode.event.WorldEvents;
import com.minecraftmode.job.AdvanceKit;
import com.minecraftmode.job.JobClass;
import com.minecraftmode.job.JobData;
import com.minecraftmode.job.JobProgression;
import com.minecraftmode.job.JobStats;
import com.minecraftmode.job.gear.ClassGear;
import com.minecraftmode.job.gear.ItemLevels;
import com.minecraftmode.job.quest.QuestDef;
import com.minecraftmode.job.quest.QuestService;
import com.minecraftmode.job.quest.Quests;
import com.minecraftmode.job.quest.TrialHunts;
import com.minecraftmode.job.skill.CombatHooks;
import com.minecraftmode.job.skill.CombatState;
import com.minecraftmode.job.skill.Engage;
import com.minecraftmode.job.skill.Skill;
import com.minecraftmode.job.skill.SkillCaster;
import com.minecraftmode.job.weapon.JobWeapons;
import com.minecraftmode.job.weapon.WeaponDef;
import com.minecraftmode.registry.ModEffects;
import com.minecraftmode.registry.ModEntities;
import com.minecraftmode.registry.ModItems;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerConnection;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;

/**
 * The town comforts and the combat changes that came with them: gap-closing skills guard their caster and stagger what they hit,
 * melee classes get health and armor per tier, a vindicator hits for a flat 2.5 hearts, every advancement hands out a kit (once),
 * bags pick up by kind and keep what they hold, the trash button with its undo, the general store's odds-and-ends buying, the
 * target health bar, the invasion numbers, Huntmaster Garrick's trial hunt (only what the trial needs, and back out) and respawning
 * at a bed. The capital's portals, ender chests, travel circles and homestead plains need a noise world: see {@code CityChecks}.
 */
public class TownClientGameTest implements FabricClientGameTest {
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
			server.runCommand("tp @p 0.5 -60 0.5 0 0");
			context.waitTicks(5);
			server.runOnServer(s -> connection.getServerPlayer().getInventory().clearContent());

			checkEngage(context, server, connection);
			checkVitality(server, connection);
			checkVindicator(context, server, connection);
			checkAdvanceKit(server, connection);
			checkBags(context, server, connection);
			checkTrash(context, server, connection);
			checkShop();
			checkTargetHealth(context, server, connection);
			checkInvasionNumbers();
			checkTrialHunt(context, server, connection);
			checkBedRespawn(context, server, connection);
			WorldClose.prepare(context, server);
		}
	}

	// ------------------------------------------------------------------ gap-closing skills

	private static void checkEngage(final ClientGameTestContext context, final TestServerContext server, final TestServerConnection connection) {
		String report = server.computeOnServer(s -> {
			int engaging = 0;
			for (WeaponDef def : JobWeapons.all()) {
				for (Skill skill : def.skills()) {
					engaging += skill.engages() ? 1 : 0;
				}
			}
			require(skill("iron_greatsword", 2).engages() && skill("ronin_katana", 0).engages(), "dash strikes close in");
			require(skill("pickpocket_shiv", 0).engages(), "the backstab steps in");
			require(!skill("iron_greatsword", 1).engages(), "Iron Wall does not move");
			require(!skill("apprentice_staff", 2).engages(), "a plain teleport only moves");
			require(!skill("apprentice_staff", 0).engages(), "a plain shot does not move");
			require(Engage.staggerTicks(EntityTypes.ZOMBIE.create(s.overworld(), EntitySpawnReason.COMMAND)) == Engage.STAGGER_TICKS, "monsters stagger 0.3 s");
			require(Engage.staggerTicks(ModEntities.MYTHRIL_GOLEM.create(s.overworld(), EntitySpawnReason.COMMAND)) == Engage.BOSS_STAGGER_TICKS, "bosses stagger 0.15 s");
			require(engaging >= 60, "every move-and-strike skill engages, got " + engaging);

			ServerPlayer player = connection.getServerPlayer();
			ServerLevel level = s.overworld();
			JobProgression.set(player, JobProgression.get(player).withJob(JobClass.WARRIOR, 1).withProgress(12, 0));
			JobProgression.set(player, JobProgression.get(player).withMana(JobStats.maxMana(player)));
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(JobWeapons.item(JobWeapons.def("iron_greatsword"))));
			player.snapTo(0.5, -60, 0.5, 0.0F, 0.0F);
			Zombie zombie = EntityTypes.ZOMBIE.create(level, EntitySpawnReason.COMMAND);
			zombie.snapTo(0.5, -60, 3.5, 180.0F, 0.0F);
			zombie.setNoAi(true);
			zombie.addTag("town_test");
			level.addFreshEntity(zombie);
			SkillCaster.Result result = SkillCaster.tryCast(player, 2);
			require(result == SkillCaster.Result.OK, "Charging Step casts, got " + result);
			require(Engage.guarded(player), "the dash guards its caster");
			return engaging + " engaging skills";
		});
		context.waitTicks(4);
		server.runOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			ServerLevel level = s.overworld();
			Zombie zombie = (Zombie)level.getEntitiesOfClass(Zombie.class, new AABB(player.blockPosition()).inflate(12), z -> z.entityTags().contains("town_test")).getFirst();
			require(zombie.hasEffect(ModEffects.STUN), "the dash staggers what it hits");
			require(zombie.getEffect(ModEffects.STUN).getDuration() <= Engage.STAGGER_TICKS, "the stagger is short");
			require(Engage.guarded(player), "still guarded right after arriving");
			float before = player.getHealth();
			player.hurtServer(level, level.damageSources().mobAttack(zombie), 4.0F);
			require(player.getHealth() == before, "a hit during the guard does nothing");
		});
		context.waitTicks(Engage.GUARD_TICKS + 10);
		server.runOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			ServerLevel level = s.overworld();
			Zombie zombie = (Zombie)level.getEntitiesOfClass(Zombie.class, new AABB(player.blockPosition()).inflate(12), z -> z.entityTags().contains("town_test")).getFirst();
			require(!Engage.guarded(player), "the guard ends");
			float before = player.getHealth();
			player.hurtServer(level, level.damageSources().mobAttack(zombie), 4.0F);
			require(player.getHealth() < before, "hits land again after the guard");
			player.setHealth(player.getMaxHealth());
			JobProgression.set(player, JobProgression.get(player).withMana(JobStats.maxMana(player)));
			require(SkillCaster.tryCast(player, 1) == SkillCaster.Result.OK && !Engage.guarded(player), "Iron Wall does not guard like a dash");
			// Iron Wall's guard stance would soften the hits measured next
			CombatState.forget(player);
			zombie.discard();
		});
		MinecraftMode.LOGGER.info("[town] {}", report);
	}

	private static Skill skill(final String weapon, final int index) {
		WeaponDef def = JobWeapons.def(weapon);
		return def == null || def.skills().size() <= index ? null : def.skills().get(index);
	}

	// ------------------------------------------------------------------ melee vitality and the vindicator

	private static void checkVitality(final TestServerContext server, final TestServerConnection connection) {
		server.runOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			JobProgression.set(player, JobProgression.get(player).withJob(JobClass.WARRIOR, 4).withProgress(70, 0));
			JobStats.refresh(player);
			AttributeModifier health = player.getAttribute(Attributes.MAX_HEALTH).getModifier(MinecraftMode.id("job/class_health"));
			AttributeModifier armor = player.getAttribute(Attributes.ARMOR).getModifier(MinecraftMode.id("job/class_armor"));
			require(health != null && health.amount() == 16.0, "a tier 4 warrior gets +16 health, got " + (health == null ? "none" : health.amount()));
			require(armor != null && armor.amount() == 8.0, "a tier 4 warrior gets +8 armor, got " + (armor == null ? "none" : armor.amount()));
			JobProgression.set(player, JobProgression.get(player).withJob(JobClass.ROGUE, 2));
			JobStats.refresh(player);
			require(player.getAttribute(Attributes.MAX_HEALTH).getModifier(MinecraftMode.id("job/class_health")).amount() == 4.0, "a tier 2 rogue gets +4 health");
			JobProgression.set(player, JobProgression.get(player).withJob(JobClass.MAGE, 4));
			JobStats.refresh(player);
			require(player.getAttribute(Attributes.MAX_HEALTH).getModifier(MinecraftMode.id("job/class_health")) == null, "mages get no melee vitality");
			JobProgression.set(player, JobProgression.get(player).withJob(JobClass.NONE, 0).withProgress(1, 0));
			JobStats.refresh(player);
			player.setHealth(player.getMaxHealth());
		});
	}

	private static void checkVindicator(final ClientGameTestContext context, final TestServerContext server, final TestServerConnection connection) {
		for (String difficulty : new String[] {"hard", "easy", "normal"}) {
			server.runCommand("difficulty " + difficulty);
			context.waitTicks(25);
			float lost = server.computeOnServer(s -> {
				ServerPlayer player = connection.getServerPlayer();
				ServerLevel level = s.overworld();
				player.setHealth(player.getMaxHealth());
				Mob vindicator = EntityTypes.VINDICATOR.create(level, EntitySpawnReason.COMMAND);
				vindicator.snapTo(2.5, -60, 2.5, 0.0F, 0.0F);
				vindicator.setNoAi(true);
				level.addFreshEntity(vindicator);
				float before = player.getHealth();
				player.hurtServer(level, level.damageSources().mobAttack(vindicator), 13.0F);
				vindicator.discard();
				return before - player.getHealth();
			});
			require(Math.abs(lost - CombatHooks.VINDICATOR_DAMAGE) < 0.01F, "a vindicator hit takes 2.5 hearts on " + difficulty + ", took " + lost);
		}
		server.runOnServer(s -> connection.getServerPlayer().setHealth(connection.getServerPlayer().getMaxHealth()));
	}

	// ------------------------------------------------------------------ advancement kit

	private static void checkAdvanceKit(final TestServerContext server, final TestServerConnection connection) {
		String report = server.computeOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			player.getInventory().clearContent();
			JobProgression.set(player, JobProgression.get(player).withJob(JobClass.NONE, 0).withProgress(10, 0));
			require(JobProgression.advance(player, JobClass.WARRIOR) == JobProgression.AdvanceResult.OK, "a level 10 adventurer becomes a warrior");
			List<ClassGear> first = gear(player);
			require(first.size() == AdvanceKit.SLOTS.size(), "the first advancement gives a weapon and four armor pieces, got " + first.size());
			require(first.stream().allMatch(g -> g.job() == JobClass.WARRIOR && g.level() <= 10 && ItemLevels.tier(g.level()) <= 1), "all of it fits a level 10 warrior");
			require(first.stream().anyMatch(ClassGear::isWeapon), "one piece is the weapon");
			JobProgression.resetClass(player);
			JobProgression.advance(player, JobClass.WARRIOR);
			require(gear(player).size() == first.size(), "a class reset does not hand the kit out again");
			JobProgression.set(player, JobProgression.get(player).withProgress(27, 0));
			JobProgression.advance(player, JobClass.WARRIOR);
			List<ClassGear> second = gear(player);
			require(second.size() == first.size() * 2, "tier 2 gives another kit, got " + (second.size() - first.size()));
			require(second.stream().filter(g -> g.bracket() == 20).count() == AdvanceKit.SLOTS.size(), "the tier 2 kit is from the level 20 bracket");
			player.getInventory().clearContent();
			JobProgression.set(player, JobProgression.get(player).withJob(JobClass.NONE, 0).withProgress(1, 0));
			JobStats.refresh(player);
			return first.size() + " + " + (second.size() - first.size()) + " pieces";
		});
		MinecraftMode.LOGGER.info("[town] advancement kits: {}", report);
	}

	private static List<ClassGear> gear(final ServerPlayer player) {
		List<ClassGear> list = new java.util.ArrayList<>();
		for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
			ClassGear gear = ClassGear.of(player.getInventory().getItem(i));
			if (gear != null) {
				list.add(gear);
			}
		}
		return list;
	}

	// ------------------------------------------------------------------ bags and the trash

	private static void checkBags(final ClientGameTestContext context, final TestServerContext server, final TestServerConnection connection) {
		server.runOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			player.getInventory().clearContent();
			require(Bags.giveStarter(player) && !Bags.giveStarter(player), "the starter bags come once");
			require(Bags.items().stream().allMatch(item -> player.getInventory().contains(new ItemStack(item))), "one bag of each kind");
			ServerLevel level = s.overworld();
			for (ItemStack stack : List.of(new ItemStack(Items.RAW_IRON, 5), new ItemStack(Items.COBBLESTONE, 20), new ItemStack(Items.DIAMOND, 2),
				new ItemStack(Items.IRON_SWORD), new ItemStack(Items.BREAD, 4), new ItemStack(Items.STICK, 3), new ItemStack(ModItems.ESSENCE, 2))) {
				ItemEntity drop = new ItemEntity(level, player.getX(), player.getY() + 0.2, player.getZ(), stack);
				drop.setNoPickUpDelay();
				level.addFreshEntity(drop);
			}
		});
		context.waitTicks(10);
		server.runOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			require(inBag(player, BagKind.ORE, Items.RAW_IRON) == 5 && inBag(player, BagKind.ORE, Items.COBBLESTONE) == 20
				&& inBag(player, BagKind.ORE, Items.DIAMOND) == 2, "ores, stone and gems go into the ore bag");
			require(inBag(player, BagKind.GEAR, Items.IRON_SWORD) == 1, "the sword goes into the gear bag");
			require(inBag(player, BagKind.SUPPLY, Items.BREAD) == 4, "bread goes into the supply bag");
			require(count(player, Items.STICK) == 3 && count(player, ModItems.ESSENCE) == 2, "sticks and essence stay in the inventory");
			require(count(player, Items.RAW_IRON) == 0 && count(player, Items.BREAD) == 0, "nothing the bags took is left in the inventory");
			require(!BagKind.ORE.accepts(new ItemStack(ModItems.GEAR_BAG)) && !BagKind.GEAR.accepts(new ItemStack(Items.SHULKER_BOX)), "bags never take bags");
			BagMenu.open(player, slot(player, BagKind.ORE));
			require(player.containerMenu instanceof BagMenu, "the ore bag opens");
		});
		context.waitTicks(5);
		context.takeScreenshot("town_bag");
		server.runOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			BagMenu menu = (BagMenu)player.containerMenu;
			int bagIndex = -1;
			for (int i = 0; i < menu.slots.size(); i++) {
				if (menu.slots.get(i).container == player.getInventory() && menu.slots.get(i).getContainerSlot() == slot(player, BagKind.ORE)) {
					bagIndex = i;
				}
			}
			require(bagIndex >= 0, "the open bag is in the menu");
			menu.clicked(bagIndex, 0, ContainerInput.PICKUP, player);
			require(menu.getCarried().isEmpty() && player.getInventory().getItem(slot(player, BagKind.ORE)).getItem() == ModItems.ORE_BAG, "the open bag cannot be picked up");
			require(!menu.slots.get(0).mayPlace(new ItemStack(ModItems.SUPPLY_BAG)), "a bag cannot go into a bag");
			int rawSlot = -1;
			for (int i = 0; i < Bags.SIZE; i++) {
				if (menu.slots.get(i).getItem().is(Items.RAW_IRON)) {
					rawSlot = i;
				}
			}
			menu.quickMoveStack(player, rawSlot);
			player.closeContainer();
			require(count(player, Items.RAW_IRON) == 5 && inBag(player, BagKind.ORE, Items.RAW_IRON) == 0, "taking the raw iron out is saved in the bag");
			require(inBag(player, BagKind.ORE, Items.COBBLESTONE) == 20, "the rest stays in the bag");
		});
	}

	private static int slot(final ServerPlayer player, final BagKind kind) {
		for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
			if (player.getInventory().getItem(i).getItem() instanceof BagItem bag && bag.kind() == kind) {
				return i;
			}
		}
		return -1;
	}

	private static int inBag(final ServerPlayer player, final BagKind kind, final Item item) {
		int slot = slot(player, kind);
		return slot < 0 ? 0 : Bags.contents(player.getInventory().getItem(slot)).stream().filter(s -> s.is(item)).mapToInt(ItemStack::getCount).sum();
	}

	private static int count(final ServerPlayer player, final Item item) {
		return JobProgression.count(player.getInventory(), item);
	}

	private static void checkTrash(final ClientGameTestContext context, final TestServerContext server, final TestServerConnection connection) {
		server.runOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			player.inventoryMenu.setCarried(new ItemStack(Items.ROTTEN_FLESH, 7));
			Trash.click(player);
			require(player.inventoryMenu.getCarried().isEmpty(), "the trash destroys the cursor stack");
			Trash.click(player);
			require(player.inventoryMenu.getCarried().is(Items.ROTTEN_FLESH) && player.inventoryMenu.getCarried().getCount() == 7, "an empty click takes it back");
			player.inventoryMenu.setCarried(ItemStack.EMPTY);
		});
		// the bag closed on the server just before: let that reach the client first, or it would close the inventory again
		context.waitTicks(10);
		context.setScreen(() -> new InventoryScreen(Minecraft.getInstance().player));
		context.waitForScreen(InventoryScreen.class);
		context.waitTicks(5);
		boolean button = context.computeOnClient(minecraft -> {
			for (AbstractWidget widget : Screens.getWidgets(minecraft.gui.screen())) {
				if (widget.getMessage().getContents() instanceof TranslatableContents t && t.getKey().equals("screen.minecraft_mode.trash.button")
					&& widget.getWidth() == TrashButton.WIDTH) {
					return true;
				}
			}
			return false;
		});
		require(button, "the inventory has the trash button");
		context.takeScreenshot("town_trash_button");
		context.setScreen(() -> null);
	}

	private static void checkShop() {
		List<ShopOffers.Trade> trades = ShopOffers.trades(ShopType.GENERAL);
		Set<Item> bought = new HashSet<>();
		Set<Item> sold = new HashSet<>();
		for (ShopOffers.Trade trade : trades) {
			bought.add(trade.cost().asItem());
			sold.add(trade.result().asItem());
		}
		for (Item junk : List.of(Items.STICK, Items.WHEAT_SEEDS, Items.POISONOUS_POTATO, Items.KELP, Items.NETHERRACK, Items.ROTTEN_FLESH, Items.BONE)) {
			require(bought.contains(junk), "the general store buys " + junk);
		}
		require(sold.containsAll(Bags.items()), "the general store sells bags");
	}

	// ------------------------------------------------------------------ target health bar

	private static void checkTargetHealth(final ClientGameTestContext context, final TestServerContext server, final TestServerConnection connection) {
		server.runOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			ServerLevel level = s.overworld();
			player.snapTo(0.5, -60, 0.5, 0.0F, 0.0F);
			Zombie zombie = EntityTypes.ZOMBIE.create(level, EntitySpawnReason.COMMAND);
			zombie.snapTo(0.5, -60, 2.5, 180.0F, 0.0F);
			zombie.setNoAi(true);
			zombie.addTag("town_target");
			level.addFreshEntity(zombie);
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WOODEN_SWORD));
			player.attack(zombie);
		});
		context.waitTicks(5);
		require(context.computeOnClient(minecraft -> TargetHealthHud.target() != null && TargetHealthHud.target().getType() == EntityTypes.ZOMBIE),
			"the zombie's health bar shows after hitting it");
		context.getInput().lookAt(new BlockPos(0, -59, 2));
		context.waitTicks(3);
		context.takeScreenshot("town_target_health");
		server.runOnServer(s -> s.overworld().getEntitiesOfClass(Zombie.class, new AABB(0, -64, 0, 4, -56, 6).inflate(4), z -> z.entityTags().contains("town_target"))
			.forEach(Entity::discard));
		context.waitTicks(3);
		require(context.computeOnClient(minecraft -> TargetHealthHud.target() == null), "the bar goes with its target");
	}

	private static void checkInvasionNumbers() {
		require(WorldEvents.invasionPower(50) == 45 && WorldEvents.invasionPower(3) == 1, "invaders fight at the defenders' level - 5");
		require(WorldEvents.partyScale(1) < WorldEvents.partyScale(2) && WorldEvents.partyScale(4) == WorldEvents.partyScale(7), "invader health grows up to four defenders");
		require(WorldEvents.invaderHealth(45, 1) < WorldEvents.invaderHealth(45, 4), "a lone defender meets weaker invaders");
		require(WorldEvents.invaderDamage(45) < com.minecraftmode.dungeon.Dungeons.powerDamage(45), "invaders are weakened");
		require(WorldEvents.invaderCount(3, 9) == WorldEvents.invaderCount(3, 4), "no more invaders past four defenders");
	}

	// ------------------------------------------------------------------ trial hunt

	private static void checkTrialHunt(final ClientGameTestContext context, final TestServerContext server, final TestServerConnection connection) {
		server.runOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			require(!TrialHunts.enter(player), "no trial, no hunt");
			player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 20 * 60, 4, false, false));
			JobProgression.set(player, JobProgression.get(player).withJob(JobClass.ROGUE, 1).withProgress(30, 0));
			require(QuestService.accept(player, JobClass.ROGUE), "the rogue trial starts");
			require(!TrialHunts.needs(player).isEmpty(), "the trial needs skeletons and witches");
			require(TrialHunts.enter(player), "Garrick sends the player in");
			require(DungeonDimension.is(player.level()), "the hunt is in the dungeon dimension");
		});
		context.waitTicks(100);
		String report = server.computeOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			ServerLevel level = (ServerLevel)player.level();
			QuestDef quest = QuestService.active(player);
			Set<EntityType<?>> allowed = new HashSet<>();
			TrialHunts.needs(player).forEach(allowed::addAll);
			BlockPos origin = TrialHunts.arena(player);
			List<Mob> mobs = level.getEntitiesOfClass(Mob.class, new AABB(origin).expandTowards(TrialHunts.SIZE + 2, TrialHunts.HEIGHT, TrialHunts.SIZE + 2).inflate(2, 8, 2),
				m -> m.entityTags().contains(TrialHunts.TAG));
			require(!mobs.isEmpty() && mobs.size() <= TrialHunts.CAP, "monsters come into the arena, got " + mobs.size());
			require(mobs.stream().allMatch(m -> allowed.contains(m.getType())), "only what the trial needs comes");
			require(level.getBlockState(TrialHunts.exit(origin)).is(Blocks.LODESTONE), "the exit lodestone is there");
			return mobs.size() + " of " + allowed.size() + " kinds for " + quest.id();
		});
		context.takeScreenshot("town_trial_hunt");
		server.runOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			BlockPos origin = TrialHunts.arena(player);
			ServerLevel level = (ServerLevel)player.level();
			TrialHunts.leave(player, "message.minecraft_mode.hunt.left");
			require(!DungeonDimension.is(player.level()) && !TrialHunts.hunting(player), "the lodestone brings the player back");
			require(level.getEntitiesOfClass(Mob.class, new AABB(origin).inflate(40), m -> m.entityTags().contains(TrialHunts.TAG)).isEmpty(), "the arena empties");
			// a finished trial ends the hunt by itself
			QuestDef quest = QuestService.active(player);
			var data = QuestService.get(player);
			for (int i = 0; i < quest.kills().size(); i++) {
				data = data.withProgress(i, quest.kills().get(i).count() - (i == 0 ? 1 : 0));
			}
			QuestService.set(player, data);
			player.getInventory().add(new ItemStack(quest.token(), quest.tokenCount()));
			require(TrialHunts.enter(player), "one skeleton left to hunt");
		});
		context.waitTicks(60);
		server.runOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			QuestDef quest = QuestService.active(player);
			QuestService.set(player, QuestService.get(player).withProgress(0, quest.kills().getFirst().count()));
			require(TrialHunts.needs(player).isEmpty(), "nothing left to hunt");
		});
		context.waitTicks(260);
		server.runOnServer(s -> {
			ServerPlayer player = connection.getServerPlayer();
			require(!DungeonDimension.is(player.level()) && !TrialHunts.hunting(player), "a finished hunt sends the player back");
			QuestService.abandon(player);
			player.getInventory().clearContent();
			JobProgression.set(player, JobProgression.get(player).withJob(JobClass.NONE, 0).withProgress(1, 0));
			JobStats.refresh(player);
			player.snapTo(0.5, -60, 0.5, 0.0F, 0.0F);
		});
		MinecraftMode.LOGGER.info("[town] trial hunt: {}", report);
	}

	// ------------------------------------------------------------------ respawning at a bed

	private static void checkBedRespawn(final ClientGameTestContext context, final TestServerContext server, final TestServerConnection connection) {
		BlockPos bed = new BlockPos(8, -60, 8);
		server.runCommand("gamerule respawn_radius 0");
		server.runOnServer(s -> {
			ServerLevel level = s.overworld();
			BlockState foot = Blocks.BED.red().defaultBlockState().setValue(BedBlock.FACING, Direction.SOUTH).setValue(BedBlock.PART, BedPart.FOOT);
			level.setBlockAndUpdate(bed, foot);
			level.setBlockAndUpdate(bed.south(), foot.setValue(BedBlock.PART, BedPart.HEAD));
			ServerPlayer player = connection.getServerPlayer();
			player.snapTo(8.5, -60, 6.5, 0.0F, 0.0F);
			level.getBlockState(bed).useWithoutItem(level, player, new BlockHitResult(Vec3.atCenterOf(bed), Direction.UP, bed, false));
			require(player.getRespawnConfig() != null, "clicking a bed sets the respawn point");
		});
		BlockPos atBed = respawn(context, server, connection);
		require(atBed.closerThan(bed, 3.0), "the player respawns at the bed, got " + atBed);
		server.runOnServer(s -> {
			s.overworld().removeBlock(bed, false);
			s.overworld().removeBlock(bed.south(), false);
		});
		BlockPos atSpawn = respawn(context, server, connection);
		BlockPos worldSpawn = server.computeOnServer(s -> s.overworld().getRespawnData().pos());
		require(!atSpawn.closerThan(bed, 3.0) && atSpawn.closerThan(worldSpawn, 3.0), "without the bed the player respawns at the world spawn, got " + atSpawn);
	}

	private static BlockPos respawn(final ClientGameTestContext context, final TestServerContext server, final TestServerConnection connection) {
		server.runOnServer(s -> s.getPlayerList().respawn(connection.getServerPlayer(), false, Entity.RemovalReason.KILLED));
		context.waitTicks(20);
		return server.computeOnServer(s -> s.getPlayerList().getPlayers().getFirst().blockPosition());
	}

	private static void require(final boolean condition, final String message) {
		if (!condition) {
			throw new AssertionError(message);
		}
	}
}
