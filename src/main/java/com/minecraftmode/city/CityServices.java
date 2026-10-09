package com.minecraftmode.city;

import com.minecraftmode.entity.CityNpc;
import com.minecraftmode.entity.ClassTrainer;
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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;
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
		ServerPlayerEvents.JOIN.register(CityServices::welcome);
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTickCount() % 100 == 0) {
				keepTrainers(server.overworld());
				keepNpcs(server.overworld());
				keepAnvils(server.overworld());
			}
			if (server.getTickCount() % 20 == 0) {
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
	public static final int GUIDE_PAGES = 9;

	/** Adventurer's handbook: city map, classes and trainers, trials, essence, the wallet. Pages are translated on the client. */
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

	/** One blacksmith and one raid marshal at their posts, like the trainers. */
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
	 * in through caves or the gates, eggs and summons) are driven off. Skill summons and NoAI
	 * decorations are left alone.
	 */
	public static void driveOffHostiles(final ServerLevel level) {
		if (!CityZone.isCityLevel(level)) {
			return;
		}
		int w = CityZone.WALL;
		AABB city = new AABB(-w, level.getMinY(), -w, w + 1, level.getMaxY(), w + 1);
		for (Mob mob : level.getEntitiesOfClass(Mob.class, city,
			m -> m.getType().getCategory() == MobCategory.MONSTER && !m.isNoAi() && !m.entityTags().contains(Actions.SUMMON_TAG) && CityZone.inside(m.blockPosition()))) {
			level.sendParticles(ParticleTypes.POOF, mob.getX(), mob.getY() + mob.getBbHeight() / 2.0, mob.getZ(), 12, 0.3, 0.4, 0.3, 0.02);
			mob.discard();
		}
	}

	/** Players may not build or break inside the city or in raid arenas unless they are operators or in creative. */
	public static boolean blocksBuilding(final Player player, final BlockPos pos) {
		return !player.isCreative()
			&& !player.permissions().hasPermission(net.minecraft.server.permissions.Permissions.COMMANDS_GAMEMASTER)
			&& (CityZone.protectedAt(player.level(), pos) || RaidDimension.is(player.level()));
	}

	/** Hostile natural spawns are refused inside the city. */
	public static boolean blocksSpawn(final ServerLevel level, final BlockPos pos, final boolean monster) {
		return monster && CityZone.protectedAt(level, pos);
	}

	/** No player-versus-player damage inside the city or in raid arenas. */
	public static boolean blocksPvp(final Entity victim, final Entity attacker) {
		return victim instanceof Player && attacker instanceof Player && attacker != victim
			&& (CityZone.protectedAt(victim.level(), victim.blockPosition()) || RaidDimension.is(victim.level()));
	}

	private CityServices() {
	}
}
