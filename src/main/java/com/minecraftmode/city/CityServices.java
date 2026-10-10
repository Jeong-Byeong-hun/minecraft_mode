package com.minecraftmode.city;

import com.minecraftmode.dungeon.DungeonDimension;
import com.minecraftmode.entity.CityNpc;
import com.minecraftmode.entity.ClassTrainer;
import com.minecraftmode.entity.TrainingDummy;
import com.minecraftmode.event.WorldEvents;
import com.minecraftmode.job.JobClass;
import com.minecraftmode.job.quest.QuestService;
import com.minecraftmode.job.skill.Actions;
import com.minecraftmode.raid.RaidDimension;
import com.minecraftmode.registry.ModEntities;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.Filterable;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Prediction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.BrushItem;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.EndCrystalItem;
import net.minecraft.world.item.FireChargeItem;
import net.minecraft.world.item.FlintAndSteelItem;
import net.minecraft.world.item.HoneycombItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.ShearsItem;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Items;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.phys.AABB;

/**
 * Runtime side of the capital: world spawn on the plaza, first-join welcome (teleport and guide
 * book), keeping one trainer of each class at its post, the city guards that drive off hostile mobs,
 * and safe-zone rules (no hostile spawns, no building, no PvP, no explosion damage) used by the mixins.
 */
public final class CityServices {
	public static void init() {
		ServerLifecycleEvents.SERVER_STARTED.register(CityServices::setSpawn);
		// Items that change blocks without placing one (fire, stripping, paths, tilling, bone meal, wax, dye...) bypass
		// Player.mayUseItemAt, so the protected zones refuse them here; doors, chests, tables and NPCs still work.
		UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
			ItemStack stack = player.getItemInHand(hand);
			if (!stack.isEmpty() && changesBlocks(stack) && blocksBuilding(player, hit.getBlockPos())) {
				return InteractionResult.FAIL;
			}
			return InteractionResult.PASS;
		});
		// the travel circles (plaza <-> homestead plains) answer a right-click, and nobody breaks them
		UseBlockCallback.EVENT.register((player, level, hand, hit) -> CityFixtures.isWaystone(level, hit.getBlockPos())
			? CityFixtures.useWaystone(player, level, hit.getBlockPos()) : InteractionResult.PASS);
		PlayerBlockBreakEvents.BEFORE.register((level, player, pos, state, blockEntity) -> !CityFixtures.isWaystone(level, pos));
		ServerPlayerEvents.JOIN.register(CityServices::welcome);
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			feedResidents(server.overworld());
			HomesteadLand.tick(server.overworld());
			if (server.getTickCount() % 100 == 0) {
				keepTrainers(server.overworld());
				keepNpcs(server.overworld());
				keepDummies(server.overworld());
				keepAnvils(server.overworld());
				CityFixtures.ensure(server.overworld());
				TrainingGrounds.ensureBuilt(server.overworld());
			}
			if (server.getTickCount() % 20 == 0) {
				TrainingGrounds.tick(server.overworld());
				driveOffHostiles(server.overworld());
			}
		});
		// phantoms and patrols do not use the natural spawner; keep them out of the city too
		ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
			if (entity instanceof Phantom && CityZone.protectedAt(level, entity.blockPosition())) {
				entity.discard();
			}
		});
	}

	private static final int MAX_FOOD = 20;

	/** Nobody goes hungry in the capital: inside the walls food and saturation are kept full every tick. */
	private static void feedResidents(final ServerLevel level) {
		if (!CityZone.isCityLevel(level)) {
			return;
		}
		for (ServerPlayer player : level.players()) {
			FoodData food = player.getFoodData();
			if (CityZone.inside(player.blockPosition()) && (food.getFoodLevel() < MAX_FOOD || food.getSaturationLevel() < MAX_FOOD)) {
				food.setFoodLevel(MAX_FOOD);
				food.setSaturation(MAX_FOOD);
			}
		}
	}

	// ------------------------------------------------------------------ spawn and welcome

	private static void setSpawn(final MinecraftServer server) {
		ServerLevel level = server.overworld();
		if (!CityZone.isCityLevel(level)) {
			return;
		}
		BlockPos spawn = CityZone.spawn(CityZone.baseY(level));
		if (!level.getRespawnData().pos().equals(spawn)) {
			level.setRespawnData(LevelData.RespawnData.of(level.dimension(), spawn, 180.0F, 0.0F));
		}
		level.getGameRules().set(GameRules.RESPAWN_RADIUS, 0, server);
	}

	private static void welcome(final ServerPlayer player) {
		var quest = QuestService.get(player);
		if (quest.visitedCity()) {
			return;
		}
		QuestService.set(player, quest.visited());
		ServerLevel overworld = player.level().getServer().overworld();
		if (CityZone.isCityLevel(overworld)) {
			BlockPos spawn = CityZone.spawn(CityZone.baseY(overworld));
			player.teleportTo(overworld, spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5, java.util.Set.of(), 180.0F, 0.0F, true);
		}
		player.getInventory().add(guideBook());
		player.sendSystemMessage(Component.translatable("message.minecraft_mode.city.welcome").withStyle(ChatFormatting.GOLD));
	}

	/** Number of handbook pages ({@code book.minecraft_mode.guide.page1..N}). */
	public static final int GUIDE_PAGES = 16;

	/** Whether {@code stack} is a copy of the handbook (any edition). */
	public static boolean isGuideBook(final ItemStack stack) {
		WrittenBookContent content = stack.get(DataComponents.WRITTEN_BOOK_CONTENT);
		return stack.is(Items.WRITTEN_BOOK) && content != null && "Stormhold".equals(content.author())
			&& "Stormhold".equals(content.title().raw());
	}

	/**
	 * Guide Nella hands over the current handbook: older copies in the inventory are replaced (their page count is fixed when
	 * printed, so new pages only reach a fresh copy).
	 */
	public static void reissueGuideBook(final ServerPlayer player) {
		Inventory inventory = player.getInventory();
		for (int i = 0; i < inventory.getContainerSize(); i++) {
			if (isGuideBook(inventory.getItem(i))) {
				inventory.setItem(i, ItemStack.EMPTY);
			}
		}
		inventory.placeItemBackInInventory(guideBook(), Prediction.SERVER_ONLY);
		player.sendSystemMessage(Component.translatable("message.minecraft_mode.guide_book.given").withStyle(ChatFormatting.GOLD));
	}

	/** Adventurer's handbook: city map, classes and trainers, trials, essence, the wallet, the story, dungeons, companions, town comforts, maps and keys. Pages are translated on the client. */
	public static ItemStack guideBook() {
		ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
		List<Filterable<Component>> pages = new ArrayList<>();
		for (int i = 1; i <= GUIDE_PAGES; i++) {
			pages.add(Filterable.passThrough(Component.translatable("book.minecraft_mode.guide.page" + i)));
		}
		book.set(DataComponents.WRITTEN_BOOK_CONTENT, new WrittenBookContent(Filterable.passThrough("Stormhold"), "Stormhold", 0, pages, true));
		book.set(DataComponents.CUSTOM_NAME, Component.translatable("book.minecraft_mode.guide.title").withStyle(ChatFormatting.GOLD));
		return book;
	}

	// ------------------------------------------------------------------ trainers

	/** One trainer per class at its post: spawn missing ones, pull wanderers back, remove extras. */
	public static void keepTrainers(final ServerLevel level) {
		if (!CityZone.isCityLevel(level)) {
			return;
		}
		int base = CityZone.baseY(level);
		for (JobClass job : JobClass.PLAYABLE) {
			BlockPos home = CityZone.trainerHome(job, base);
			if (!level.isLoaded(home) || !level.shouldTickBlocksAt(home)) {
				continue;
			}
			List<ClassTrainer> found = level.getEntitiesOfClass(ClassTrainer.class, new AABB(home).inflate(32), t -> t.job() == job);
			if (found.isEmpty()) {
				ClassTrainer trainer = ModEntities.CLASS_TRAINER.create(level, EntitySpawnReason.STRUCTURE);
				if (trainer != null) {
					trainer.setJob(job);
					trainer.snapTo(home.getX() + 0.5, home.getY(), home.getZ() + 0.5, 0.0F, 0.0F);
					level.addFreshEntity(trainer);
				}
				continue;
			}
			ClassTrainer keep = found.getFirst();
			for (int i = 1; i < found.size(); i++) {
				found.get(i).discard();
			}
			if (keep.blockPosition().distManhattan(home) > 2) {
				keep.teleportTo(home.getX() + 0.5, home.getY(), home.getZ() + 0.5);
			}
		}
	}

	/** The warrior arena's training dummies at their posts (put back when missing, extras removed), like the trainers. */
	public static void keepDummies(final ServerLevel level) {
		if (!CityZone.isCityLevel(level)) {
			return;
		}
		for (CityZone.DummyPost post : CityZone.dummies(CityZone.baseY(level))) {
			BlockPos pos = post.pos();
			if (!level.isLoaded(pos) || !level.shouldTickBlocksAt(pos)) {
				continue;
			}
			List<TrainingDummy> found = level.getEntitiesOfClass(TrainingDummy.class, new AABB(pos).inflate(1.5), d -> d.isBossKind() == post.boss());
			if (found.isEmpty()) {
				TrainingDummy dummy = (post.boss() ? ModEntities.TRAINING_DUMMY_BOSS : ModEntities.TRAINING_DUMMY).create(level, EntitySpawnReason.STRUCTURE);
				if (dummy != null) {
					dummy.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0.0F, 0.0F);
					level.addFreshEntity(dummy);
				}
				continue;
			}
			for (int i = 1; i < found.size(); i++) {
				found.get(i).discard();
			}
		}
	}

	/** One NPC of each service role at its post, like the trainers. */
	public static void keepNpcs(final ServerLevel level) {
		if (!CityZone.isCityLevel(level)) {
			return;
		}
		int base = CityZone.baseY(level);
		for (CityNpc.Role role : CityNpc.Role.values()) {
			BlockPos home = CityZone.npcHome(role, base);
			if (!level.isLoaded(home) || !level.shouldTickBlocksAt(home)) {
				continue;
			}
			List<CityNpc> found = level.getEntitiesOfClass(CityNpc.class, new AABB(home).inflate(32), n -> n.role() == role);
			if (found.isEmpty()) {
				CityNpc npc = ModEntities.CITY_NPC.create(level, EntitySpawnReason.STRUCTURE);
				if (npc != null) {
					npc.setRole(role);
					npc.snapTo(home.getX() + 0.5, home.getY(), home.getZ() + 0.5, 0.0F, 0.0F);
					level.addFreshEntity(npc);
				}
				continue;
			}
			CityNpc keep = found.getFirst();
			for (int i = 1; i < found.size(); i++) {
				found.get(i).discard();
			}
			if (keep.blockPosition().distManhattan(home) > 2) {
				keep.teleportTo(home.getX() + 0.5, home.getY(), home.getZ() + 0.5);
			}
		}
	}

	/**
	 * Anvils wear out and break with use, and nobody may place blocks in the city: worn or missing city
	 * anvils are put back as new ones (keeping their facing).
	 */
	public static void keepAnvils(final ServerLevel level) {
		if (!CityZone.isCityLevel(level)) {
			return;
		}
		for (BlockPos pos : CityZone.anvils(CityZone.baseY(level))) {
			if (!level.isLoaded(pos)) {
				continue;
			}
			BlockState state = level.getBlockState(pos);
			if (state.is(Blocks.ANVIL)) {
				continue;
			}
			BlockState fresh = Blocks.ANVIL.defaultBlockState();
			if (state.hasProperty(AnvilBlock.FACING)) {
				fresh = fresh.setValue(AnvilBlock.FACING, state.getValue(AnvilBlock.FACING));
			}
			if (state.isAir() || state.is(BlockTags.ANVIL)) {
				level.setBlockAndUpdate(pos, fresh);
			}
		}
	}

	// ------------------------------------------------------------------ safe zone rules

	/**
	 * City guards: hostile mobs that get inside the walls anyway (spiders climbing them, mobs walking
	 * in through caves or the gates, eggs and summons) are driven off. Skill summons, NoAI
	 * decorations, invaders and the monsters of the Training Grounds (inside the hall) are left alone.
	 */
	public static void driveOffHostiles(final ServerLevel level) {
		if (!CityZone.isCityLevel(level)) {
			return;
		}
		int w = CityZone.WALL;
		AABB city = new AABB(-w, level.getMinY(), -w, w + 1, level.getMaxY(), w + 1);
		for (Mob mob : level.getEntitiesOfClass(Mob.class, city,
			m -> m.getType().getCategory() == MobCategory.MONSTER && !m.isNoAi() && !m.entityTags().contains(Actions.SUMMON_TAG)
				&& !m.entityTags().contains(WorldEvents.INVADER_TAG) && !m.getUUID().equals(WorldEvents.titan()) && CityZone.inside(m.blockPosition())
				&& !TrainingGrounds.holds(m))) {
			level.sendParticles(ParticleTypes.POOF, mob.getX(), mob.getY() + mob.getBbHeight() / 2.0, mob.getZ(), 12, 0.3, 0.4, 0.3, 0.02);
			mob.discard();
		}
	}

	/** Players may not build or break inside the city, in raid arenas or in dungeons unless they are operators or in creative. */
	/** Items whose use on a block alters it (no block placed, so {@code BlockItem.place} never sees them). */
	private static boolean changesBlocks(final ItemStack stack) {
		Item item = stack.getItem();
		return item instanceof FlintAndSteelItem || item instanceof FireChargeItem || stack.is(ItemTags.AXES) || stack.is(ItemTags.SHOVELS)
			|| stack.is(ItemTags.HOES) || item instanceof BoneMealItem || item instanceof HoneycombItem || item instanceof ShearsItem
			|| item instanceof DyeItem || item instanceof PotionItem || item instanceof EndCrystalItem || item instanceof BrushItem
			|| item == Items.INK_SAC || item == Items.GLOW_INK_SAC;
	}

	public static boolean blocksBuilding(final Player player, final BlockPos pos) {
		return !player.isCreative()
			&& !player.permissions().hasPermission(net.minecraft.server.permissions.Permissions.COMMANDS_GAMEMASTER)
			&& (CityZone.protectedAt(player.level(), pos) || RaidDimension.is(player.level()) || DungeonDimension.is(player.level()));
	}

	/**
	 * Hostile natural spawns are refused inside the city, and on the surface of the outskirts around it (as if the countryside
	 * were kept lit); caves under the outskirts still spawn.
	 */
	public static boolean blocksSpawn(final ServerLevel level, final BlockPos pos, final boolean monster) {
		if (!monster) {
			return false;
		}
		if (CityZone.protectedAt(level, pos)) {
			return true;
		}
		return CityZone.outskirts(pos.getX(), pos.getZ()) && CityZone.isCityLevel(level)
			&& pos.getY() >= level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, pos.getX(), pos.getZ()) - CityZone.OUTSKIRTS_SURFACE_DEPTH;
	}

	/** No player-versus-player damage inside the city, in raid arenas or in dungeons. */
	public static boolean blocksPvp(final Entity victim, final Entity attacker) {
		return victim instanceof Player && attacker instanceof Player && attacker != victim
			&& (CityZone.protectedAt(victim.level(), victim.blockPosition()) || RaidDimension.is(victim.level()) || DungeonDimension.is(victim.level()));
	}

	private CityServices() {
	}
}
