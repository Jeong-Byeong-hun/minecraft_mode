package com.minecraftmode.network;

import com.minecraftmode.bounty.Bounties;
import com.minecraftmode.companion.Companions;
import com.minecraftmode.dungeon.DungeonDef;
import com.minecraftmode.dungeon.Dungeons;
import com.minecraftmode.entity.CityNpc;
import com.minecraftmode.entity.ClassTrainer;
import com.minecraftmode.job.Paragon;
import com.minecraftmode.job.gear.ClassAbilities;
import com.minecraftmode.job.quest.QuestService;
import com.minecraftmode.job.skill.SkillCaster;
import com.minecraftmode.market.AuctionService;
import com.minecraftmode.progress.Progress;
import com.minecraftmode.raid.BossDef;
import com.minecraftmode.raid.RaidBosses;
import com.minecraftmode.raid.RaidDifficulty;
import com.minecraftmode.raid.Raids;
import com.minecraftmode.raid.loot.LootSessions;
import com.minecraftmode.talent.Talents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import org.jspecify.annotations.Nullable;

public final class ModNetworking {
	/** Trainer actions must be done within this distance of the trainer. */
	private static final double TRAINER_RANGE = 8.0;

	public static void init() {
		PayloadTypeRegistry.serverboundPlay().register(CastSkillPayload.TYPE, CastSkillPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(QuestActionPayload.TYPE, QuestActionPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(InnateAbilityPayload.TYPE, InnateAbilityPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(RaidEnterPayload.TYPE, RaidEnterPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(DungeonEnterPayload.TYPE, DungeonEnterPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(OpenDungeonPayload.TYPE, OpenDungeonPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(LootActionPayload.TYPE, LootActionPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(BountyActionPayload.TYPE, BountyActionPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(AuctionActionPayload.TYPE, AuctionActionPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(ProgressActionPayload.TYPE, ProgressActionPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(OpenTrainerPayload.TYPE, OpenTrainerPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(OpenRaidPayload.TYPE, OpenRaidPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(PartySyncPayload.TYPE, PartySyncPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(LootStatePayload.TYPE, LootStatePayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(OpenBountyPayload.TYPE, OpenBountyPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(AuctionStatePayload.TYPE, AuctionStatePayload.CODEC);

		ServerPlayNetworking.registerGlobalReceiver(CastSkillPayload.TYPE, (payload, context) -> context.server().execute(
			() -> SkillCaster.tryCast(context.player(), payload.slot())
		));
		ServerPlayNetworking.registerGlobalReceiver(InnateAbilityPayload.TYPE, (payload, context) -> context.server().execute(
			() -> ClassAbilities.use(context.player())
		));
		ServerPlayNetworking.registerGlobalReceiver(DungeonEnterPayload.TYPE, (payload, context) -> context.server().execute(() -> {
			DungeonDef def = Dungeons.def(payload.dungeon());
			if (def != null) {
				Dungeons.tryEnter(context.player(), payload.wardenId(), def, payload.keystone());
			}
		}));
		ServerPlayNetworking.registerGlobalReceiver(RaidEnterPayload.TYPE, (payload, context) -> context.server().execute(() -> {
			BossDef def = RaidBosses.byId(payload.boss());
			if (def != null) {
				Raids.tryEnter(context.player(), payload.marshalId(), def, RaidDifficulty.byId(payload.difficulty()));
			}
		}));
		ServerPlayNetworking.registerGlobalReceiver(LootActionPayload.TYPE, (payload, context) -> context.server().execute(
			() -> LootSessions.handle(context.player(), payload)
		));
		ServerPlayNetworking.registerGlobalReceiver(QuestActionPayload.TYPE, (payload, context) -> context.server().execute(
			() -> handleQuest(context.player(), payload)
		));
		ServerPlayNetworking.registerGlobalReceiver(BountyActionPayload.TYPE, (payload, context) -> context.server().execute(
			() -> handleBounty(context.player(), payload)
		));
		ServerPlayNetworking.registerGlobalReceiver(AuctionActionPayload.TYPE, (payload, context) -> context.server().execute(
			() -> handleAuction(context.player(), payload)
		));
		ServerPlayNetworking.registerGlobalReceiver(ProgressActionPayload.TYPE, (payload, context) -> context.server().execute(
			() -> handleProgress(context.player(), payload)
		));
	}

	private static void handleQuest(final ServerPlayer player, final QuestActionPayload payload) {
		if (payload.action() == QuestActionPayload.Action.ABANDON) {
			QuestService.abandon(player);
			return;
		}
		if (!(player.level().getEntity(payload.entityId()) instanceof ClassTrainer trainer) || trainer.distanceTo(player) > TRAINER_RANGE) {
			return;
		}
		switch (payload.action()) {
			case ACCEPT -> QuestService.accept(player, trainer.job());
			case COMPLETE -> QuestService.complete(player, trainer.job());
			default -> {
			}
		}
	}

	/** The city NPC {@code entityId} when it has {@code role} and stands within reach of {@code player}. */
	private static @Nullable CityNpc npc(final ServerPlayer player, final int entityId, final CityNpc.Role role) {
		return player.level().getEntity(entityId) instanceof CityNpc npc && npc.role() == role && npc.distanceTo(player) <= TRAINER_RANGE ? npc : null;
	}

	private static void handleBounty(final ServerPlayer player, final BountyActionPayload payload) {
		if (npc(player, payload.entityId(), CityNpc.Role.BOUNTY_CLERK) == null) {
			return;
		}
		switch (payload.action()) {
			case BountyActionPayload.CLAIM -> Bounties.claim(player, payload.index());
			case BountyActionPayload.BUY -> Bounties.buy(player, payload.index());
			default -> {
			}
		}
		ServerPlayNetworking.send(player, CityNpc.bountyBoard(player, payload.entityId()));
	}

	private static void handleAuction(final ServerPlayer player, final AuctionActionPayload payload) {
		if (npc(player, payload.entityId(), CityNpc.Role.BROKER) == null) {
			return;
		}
		int suggestSlot = -1;
		int suggestEach = -1;
		switch (payload.action()) {
			case AuctionActionPayload.LIST -> AuctionService.list(player, payload.slot(), payload.price());
			case AuctionActionPayload.BUY -> AuctionService.buy(player, payload.id());
			case AuctionActionPayload.CANCEL -> AuctionService.cancel(player, payload.id());
			case AuctionActionPayload.CLAIM -> AuctionService.claim(player);
			case AuctionActionPayload.PRICE -> {
				suggestSlot = payload.slot();
				suggestEach = AuctionService.cheapestEach(player, payload.slot());
			}
			default -> {
			}
		}
		AuctionService.send(player, payload.entityId(), payload.query(), suggestSlot, suggestEach);
	}

	private static void handleProgress(final ServerPlayer player, final ProgressActionPayload payload) {
		switch (payload.action()) {
			case ProgressActionPayload.TITLE -> Progress.setTitle(player, payload.value());
			case ProgressActionPayload.TALENT -> Talents.spend(player, payload.value());
			case ProgressActionPayload.TALENT_RESET -> Talents.reset(player);
			case ProgressActionPayload.PARAGON -> Paragon.spend(player, payload.value());
			case ProgressActionPayload.PARAGON_RESET -> Paragon.reset(player);
			case ProgressActionPayload.PET -> Companions.summonPet(player, payload.value());
			case ProgressActionPayload.MOUNT -> Companions.toggleMount(player, payload.value());
			default -> {
			}
		}
	}

	private ModNetworking() {
	}
}
