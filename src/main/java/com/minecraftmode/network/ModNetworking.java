package com.minecraftmode.network;

import com.minecraftmode.accessory.AccessoryMenu;
import com.minecraftmode.character.Characters;
import com.minecraftmode.admin.Admin;
import com.minecraftmode.economy.Buyback;
import com.minecraftmode.economy.LootBuyer;
import com.minecraftmode.economy.SellAll;
import com.minecraftmode.bag.Trash;
import com.minecraftmode.bounty.Bounties;
import com.minecraftmode.city.CityServices;
import com.minecraftmode.city.CityZone;
import com.minecraftmode.command.DiceCommand;
import com.minecraftmode.companion.Companions;
import com.minecraftmode.dungeon.DungeonDef;
import com.minecraftmode.dungeon.DungeonDimension;
import com.minecraftmode.dungeon.Dungeons;
import com.minecraftmode.economy.Wallet;
import com.minecraftmode.entity.CityNpc;
import com.minecraftmode.entity.ClassTrainer;
import com.minecraftmode.job.Paragon;
import com.minecraftmode.job.gear.ClassAbilities;
import com.minecraftmode.job.quest.QuestService;
import com.minecraftmode.jump.JumpForest;
import com.minecraftmode.job.skill.SkillCaster;
import com.minecraftmode.loot.EtherAutoFuse;
import com.minecraftmode.market.AuctionService;
import com.minecraftmode.progress.Progress;
import com.minecraftmode.raid.BossDef;
import com.minecraftmode.raid.Parties;
import com.minecraftmode.raid.RaidBosses;
import com.minecraftmode.raid.RaidDimension;
import com.minecraftmode.raid.RaidDifficulty;
import com.minecraftmode.raid.Raids;
import com.minecraftmode.raid.loot.LootSessions;
import com.minecraftmode.residence.LandOffice;
import com.minecraftmode.talent.Talents;
import com.minecraftmode.townquest.TownQuestService;
import com.minecraftmode.townquest.TownQuests;
import com.minecraftmode.travel.Destination;
import com.minecraftmode.travel.Travel;
import com.minecraftmode.worldgen.lair.LairTravel;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
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
		PayloadTypeRegistry.clientboundPlay().register(OpenGuidePayload.TYPE, OpenGuidePayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(OpenNoticePayload.TYPE, OpenNoticePayload.CODEC);
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
		PayloadTypeRegistry.clientboundPlay().register(CityInfoPayload.TYPE, CityInfoPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(TrashPayload.TYPE, TrashPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(OpenAccessoriesPayload.TYPE, OpenAccessoriesPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(BuybackPayload.TYPE, BuybackPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(SellAllPayload.TYPE, SellAllPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(GuideBookPayload.TYPE, GuideBookPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(TargetHealthPayload.TYPE, TargetHealthPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(DamageNumberPayload.TYPE, DamageNumberPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(OpenLandPayload.TYPE, OpenLandPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(LandBordersPayload.TYPE, LandBordersPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(OpenTravelPayload.TYPE, OpenTravelPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(OpenLootBuyerPayload.TYPE, OpenLootBuyerPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(LootSellPayload.TYPE, LootSellPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(OpenTownQuestPayload.TYPE, OpenTownQuestPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(TownQuestActionPayload.TYPE, TownQuestActionPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(TravelPayload.TYPE, TravelPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(LandActionPayload.TYPE, LandActionPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(MapMarkPayload.TYPE, MapMarkPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(FieldBossPayload.TYPE, FieldBossPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(MenuActionPayload.TYPE, MenuActionPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(AdminActionPayload.TYPE, AdminActionPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(AdminStatePayload.TYPE, AdminStatePayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(CharacterListPayload.TYPE, CharacterListPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(LairOfferPayload.TYPE, LairOfferPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(LairClearPayload.TYPE, LairClearPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(LairTravelPayload.TYPE, LairTravelPayload.CODEC);
		ServerPlayerEvents.JOIN.register(player -> ServerPlayNetworking.send(player, new CityInfoPayload(CityZone.isCityLevel(player.level().getServer().overworld()))));

		ServerPlayNetworking.registerGlobalReceiver(CastSkillPayload.TYPE, (payload, context) -> context.server().execute(
			() -> SkillCaster.tryCast(context.player(), payload.slot())
		));
		ServerPlayNetworking.registerGlobalReceiver(InnateAbilityPayload.TYPE, (payload, context) -> context.server().execute(
			() -> ClassAbilities.use(context.player())
		));
		ServerPlayNetworking.registerGlobalReceiver(BuybackPayload.TYPE, (payload, context) -> context.server().execute(
			() -> Buyback.buy(context.player(), payload.index())
		));
		ServerPlayNetworking.registerGlobalReceiver(SellAllPayload.TYPE, (payload, context) -> context.server().execute(
			() -> SellAll.sell(context.player(), payload.offer())
		));
		ServerPlayNetworking.registerGlobalReceiver(TrashPayload.TYPE, (payload, context) -> context.server().execute(
			() -> Trash.click(context.player())
		));
		ServerPlayNetworking.registerGlobalReceiver(OpenAccessoriesPayload.TYPE, (payload, context) -> context.server().execute(() -> {
			// a player choosing a character in the lobby only gets the character menu
			if (!Characters.choosing(context.player()) && context.player().isAlive()) {
				AccessoryMenu.open(context.player());
			}
		}));
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
		ServerPlayNetworking.registerGlobalReceiver(GuideBookPayload.TYPE, (payload, context) -> context.server().execute(() -> {
			if (npc(context.player(), payload.entityId(), CityNpc.Role.GUIDE) != null) {
				CityServices.reissueGuideBook(context.player());
			}
		}));
		ServerPlayNetworking.registerGlobalReceiver(LandActionPayload.TYPE, (payload, context) -> context.server().execute(
			() -> LandOffice.act(context.player(), payload)
		));
		ServerPlayNetworking.registerGlobalReceiver(AdminActionPayload.TYPE, (payload, context) -> context.server().execute(
			() -> Admin.handle(context.player(), payload)
		));
		ServerPlayNetworking.registerGlobalReceiver(MenuActionPayload.TYPE, (payload, context) -> context.server().execute(
			() -> handleMenu(context.player(), payload)
		));
		ServerPlayNetworking.registerGlobalReceiver(TravelPayload.TYPE, (payload, context) -> context.server().execute(() -> {
			Destination destination = Destination.byOrdinal(payload.destination());
			if (destination != null && npc(context.player(), payload.entityId(), CityNpc.Role.WAYMASTER) != null) {
				Travel.go(context.player(), destination);
			}
		}));
		ServerPlayNetworking.registerGlobalReceiver(LairTravelPayload.TYPE, (payload, context) -> context.server().execute(() -> {
			switch (payload.action()) {
				case LairTravelPayload.TRAVEL -> LairTravel.confirm(context.player());
				case LairTravelPayload.EXIT -> LairTravel.exit(context.player());
				default -> {
				}
			}
		}));
		ServerPlayNetworking.registerGlobalReceiver(LootSellPayload.TYPE, (payload, context) -> context.server().execute(() -> {
			if (npc(context.player(), payload.entityId(), CityNpc.Role.LOOT_BUYER) != null) {
				LootBuyer.sell(context.player(), payload.entityId(), payload.picks());
			}
		}));
		ServerPlayNetworking.registerGlobalReceiver(TownQuestActionPayload.TYPE, (payload, context) -> context.server().execute(() -> {
			TownQuests.Quest quest = TownQuests.get(payload.quest());
			CityNpc giver = quest == null ? null : npc(context.player(), payload.entityId(), quest.giver());
			if (giver != null) {
				TownQuestService.act(context.player(), quest, payload.action());
				if (payload.action() != TownQuestActionPayload.ENTER) {
					ServerPlayNetworking.send(context.player(), TownQuestService.screen(context.player(), payload.entityId(), giver.role()));
				}
			}
		}));
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

	/** The adventurer's menu: the same calls as {@code /party}, {@code /wallet pay}, {@code /raid leave}, {@code /dungeon leave} and {@code /dice}, and character select. */
	private static void handleMenu(final ServerPlayer player, final MenuActionPayload payload) {
		if (Characters.choosing(player) && (payload.action() < MenuActionPayload.CHARACTERS || payload.action() > MenuActionPayload.CHARACTER_ENTER)) {
			return;
		}
		switch (payload.action()) {
			case MenuActionPayload.PARTY_CREATE -> Parties.create(player);
			case MenuActionPayload.PARTY_INVITE -> Parties.invite(player, payload.name());
			case MenuActionPayload.PARTY_ACCEPT -> Parties.accept(player);
			case MenuActionPayload.PARTY_LEAVE -> Parties.leave(player);
			case MenuActionPayload.PARTY_KICK -> Parties.kick(player, payload.name());
			case MenuActionPayload.PARTY_PROMOTE -> Parties.promote(player, payload.name());
			case MenuActionPayload.PARTY_DISBAND -> Parties.disband(player);
			case MenuActionPayload.PAY -> {
				ServerPlayer to = player.level().getServer().getPlayerList().getPlayerByName(payload.name());
				if (to == null) {
					player.sendSystemMessage(Wallet.msg("not_online", payload.name()).withStyle(ChatFormatting.RED));
				} else {
					Wallet.pay(player, to, payload.amount());
				}
			}
			case MenuActionPayload.LEAVE -> {
				if (JumpForest.busy(player)) {
					JumpForest.leave(player);
				} else if (RaidDimension.is(player.level())) {
					Raids.leave(player);
				} else if (DungeonDimension.is(player.level())) {
					Dungeons.leave(player);
				}
			}
			case MenuActionPayload.CHARACTERS -> Characters.send(player);
			case MenuActionPayload.CHARACTER_SWITCH -> Characters.switchTo(player, payload.amount());
			case MenuActionPayload.CHARACTER_BUY -> Characters.buySlot(player);
			case MenuActionPayload.CHARACTER_ENTER -> Characters.enter(player, payload.amount());
			case MenuActionPayload.DICE -> DiceCommand.roll(player);
			case MenuActionPayload.ETHER_AUTO -> EtherAutoFuse.set(player, payload.amount() > 0, Math.abs(payload.amount()));
			default -> {
			}
		}
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
