package com.minecraftmode.character;

import com.minecraftmode.MinecraftMode;
import com.minecraftmode.bounty.BountyData;
import com.minecraftmode.bounty.Bounties;
import com.minecraftmode.city.CityServices;
import com.minecraftmode.city.CityZone;
import com.minecraftmode.companion.Companions;
import com.minecraftmode.companion.MountEntity;
import com.minecraftmode.dungeon.DungeonDimension;
import com.minecraftmode.economy.Wallet;
import com.minecraftmode.event.WorldEvents;
import com.minecraftmode.job.JobData;
import com.minecraftmode.job.JobProgression;
import com.minecraftmode.job.JobStats;
import com.minecraftmode.network.CharacterListPayload;
import com.minecraftmode.progress.Titles;
import com.minecraftmode.raid.RaidDimension;
import com.minecraftmode.raid.Raids;
import com.minecraftmode.raid.loot.LootSessions;
import com.minecraftmode.registry.ModAttachments;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.fabric.api.attachment.v1.AttachmentTarget;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetHeldSlotPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.PlayerEnderChestContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

/**
 * Character select: an account owns {@link #BASE_SLOTS} character slots and buys more with merit ({@link #SLOT_MERIT} each, up to
 * {@link #MAX_SLOTS}). Switching saves the whole player (inventory, position, attachments: class, quests, records, ...) into
 * {@link CharacterRoster} and loads the other slot onto the same player, or starts a fresh character at the plaza. The ender chest,
 * the codex and the wallet ({@link #SHARED}), the recipe book, land claims and the market (both keyed by the account's UUID) stay with the account.
 * Only inside the capital's walls (no fights to slip out of), except while choosing: a player who joins with more than one slot
 * (outside a raid or dungeon, which handle their own returns) waits in the {@link Lobby} on the select screen ({@link #choosing}: no
 * damage, no using or hitting anything, only character actions) until they {@link #enter} a slot.
 */
public final class Characters {
	public static final int BASE_SLOTS = 3;
	public static final int MAX_SLOTS = 7;
	public static final int SLOT_MERIT = 1000;
	/** What the select screen dresses each character's figure in, in this order. */
	public static final List<EquipmentSlot> GEAR_SLOTS = List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET,
		EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND);
	/** Where the saved character was (vanilla keeps the dimension outside the player's own data). */
	private static final String DIMENSION = "minecraft_mode:dimension";
	/** Player data the account keeps: never saved with a character or loaded from one. */
	private static final List<String> ACCOUNT_KEYS = List.of("EnderItems", "recipeBook", "RootVehicle", "ender_pearls");
	/** Attachments every character of an account shares: the monster and item codex, and the wallet. */
	private static final Set<AttachmentType<?>> SHARED = Set.of(ModAttachments.MONSTER_KILLS, ModAttachments.ITEMS_FOUND, ModAttachments.WALLET);
	/** Players on the select screen after joining (server memory: a reconnect starts choosing again). */
	private static final Set<UUID> CHOOSING = new HashSet<>();

	public static void init() {
		ServerPlayerEvents.JOIN.register(Characters::onJoin);
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> server.execute(() -> CHOOSING.remove(handler.player.getUUID())));
		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> !(entity instanceof ServerPlayer player && choosing(player)));
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			Lobby.tick(server);
			for (UUID id : CHOOSING) {
				ServerPlayer player = server.getPlayerList().getPlayer(id);
				if (player != null) {
					player.resetFallDistance();
				}
			}
		});
		UseBlockCallback.EVENT.register((player, level, hand, hit) -> blocked(player));
		UseItemCallback.EVENT.register((player, level, hand) -> blocked(player));
		UseEntityCallback.EVENT.register((player, level, hand, entity, hit) -> blocked(player));
		AttackEntityCallback.EVENT.register((player, level, hand, entity, hit) -> blocked(player));
		AttackBlockCallback.EVENT.register((player, level, hand, pos, direction) -> blocked(player));
		PlayerBlockBreakEvents.BEFORE.register((level, player, pos, state, blockEntity) -> blocked(player) == InteractionResult.PASS);
	}

	/**
	 * Joining: with another character to play (outside a raid or dungeon) the player goes to the lobby to choose; a new account's
	 * empty slots alone do not send anyone there. A player who logged out there while choosing is still there, chooses again and
	 * keeps their origin.
	 */
	public static void onJoin(final ServerPlayer player) {
		boolean instance = RaidDimension.is(player.level()) || DungeonDimension.is(player.level());
		boolean others = !CharacterRoster.get(server(player)).account(player.getUUID()).stored().isEmpty();
		if ((others || Lobby.is(player.level())) && !instance) {
			choose(player);
		} else {
			Lobby.leave(player);
			send(player);
		}
	}

	/** True while the player is on the select screen after joining. */
	public static boolean choosing(final ServerPlayer player) {
		return CHOOSING.contains(player.getUUID());
	}

	/** Puts the player in the lobby on the select screen (on join, or by a game test). */
	public static void choose(final ServerPlayer player) {
		CHOOSING.add(player.getUUID());
		Lobby.enter(player);
		send(player);
	}

	/** Leaves the select screen playing slot {@code slot}: the current one just goes on, another one is switched to first. */
	public static boolean enter(final ServerPlayer player, final int slot) {
		if (!choosing(player)) {
			return false;
		}
		int active = CharacterRoster.get(server(player)).account(player.getUUID()).active();
		if (slot != active && !switchTo(player, slot)) {
			return false;
		}
		CHOOSING.remove(player.getUUID());
		Lobby.leave(player);
		send(player);
		return true;
	}

	private static InteractionResult blocked(final Player player) {
		return player instanceof ServerPlayer server && choosing(server) ? InteractionResult.FAIL : InteractionResult.PASS;
	}

	/** Sends the slots to the player; while {@link #choosing} the client keeps the select screen open, else it closes it. */
	public static void send(final ServerPlayer player) {
		CharacterRoster.Account account = CharacterRoster.get(server(player)).account(player.getUUID());
		List<CharacterListPayload.Entry> entries = new ArrayList<>();
		for (int slot = 0; slot < account.slots(); slot++) {
			CharacterRoster.Stored stored = account.at(slot);
			if (slot == account.active()) {
				JobData job = JobProgression.get(player);
				entries.add(new CharacterListPayload.Entry(false, job.job().ordinal(), job.tier(), job.level(), gear(player)));
			} else if (stored == null) {
				entries.add(CharacterListPayload.Entry.EMPTY);
			} else {
				entries.add(new CharacterListPayload.Entry(false, stored.job().ordinal(), stored.tier(), stored.level(), stored.gear()));
			}
		}
		ServerPlayNetworking.send(player, new CharacterListPayload(entries, account.active(), MAX_SLOTS, SLOT_MERIT, choosing(player)));
	}

	/** Plays slot {@code slot}: the current character is saved into its slot first. */
	public static boolean switchTo(final ServerPlayer player, final int slot) {
		CharacterRoster roster = CharacterRoster.get(server(player));
		CharacterRoster.Account account = roster.account(player.getUUID());
		if (slot < 0 || slot >= account.slots() || slot == account.active()) {
			return false;
		}
		if (!choosing(player) && !CityZone.protectedAt(player.level(), player.blockPosition()) || player.isDeadOrDying()) {
			return fail(player, "not_here");
		}
		// rewards of a raid, its loot, an invasion or a titan go to whoever plays the account when they are paid: until 0.5.24 a
		// fallen raider could switch in town and hand the clear (and the lockout) to another character
		if (Raids.committed(player.getUUID()) || LootSessions.sessionOf(player.getUUID()) != null || WorldEvents.committed(player.getUUID())) {
			return fail(player, "busy");
		}
		CharacterRoster.Stored target = account.at(slot);
		player.closeContainer();
		if (player.getVehicle() instanceof MountEntity mount) {
			player.stopRiding();
			mount.discard();
		}
		player.stopRiding();
		Companions.dismissPet(player);
		JobData job = JobProgression.get(player);
		List<ItemStack> gear = gear(player);
		CompoundTag current = save(player);
		try {
			if (target == null) {
				start(player);
			} else {
				load(player, target.data());
			}
		} catch (RuntimeException e) {
			// the roster is untouched until the switch worked, so putting the current character back loses nothing
			MinecraftMode.LOGGER.error("Switching {} to character slot {} failed", player.getPlainTextName(), slot + 1, e);
			load(player, current);
			return fail(player, "failed");
		}
		roster.put(player.getUUID(), account.swap(new CharacterRoster.Stored(account.active(), current, job.job(), job.tier(), job.level(), gear), slot));
		// a switch inside one level fires none of the events that bring the active pet back (join, respawn, level change)
		Companions.restore(player);
		// write the player file and the roster now, together: the player file is written on logout but the roster only at the next
		// autosave, so a hard stop in between left one character twice and the other gone
		server(player).getPlayerList().saveAll();
		server(player).getDataStorage().scheduleSave();
		player.sendSystemMessage(Component.translatable(target == null ? "message.minecraft_mode.character.started" : "message.minecraft_mode.character.switched",
			slot + 1).withStyle(ChatFormatting.GREEN));
		send(player);
		return true;
	}

	/** Buys one more slot for {@link #SLOT_MERIT} merit. */
	public static boolean buySlot(final ServerPlayer player) {
		CharacterRoster roster = CharacterRoster.get(server(player));
		CharacterRoster.Account account = roster.account(player.getUUID());
		if (account.slots() >= MAX_SLOTS) {
			return fail(player, "max");
		}
		BountyData data = Bounties.get(player);
		if (data.merit() < SLOT_MERIT) {
			return fail(player, "no_merit");
		}
		Bounties.set(player, data.withMerit(data.merit() - SLOT_MERIT));
		roster.put(player.getUUID(), account.withSlots(account.slots() + 1));
		player.sendSystemMessage(Component.translatable("message.minecraft_mode.character.bought", account.slots() + 1).withStyle(ChatFormatting.GREEN));
		send(player);
		return true;
	}

	private static CompoundTag save(final ServerPlayer player) {
		TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, player.level().registryAccess());
		player.saveWithoutId(output);
		CompoundTag tag = output.buildResult();
		ACCOUNT_KEYS.forEach(tag::remove);
		takeStoredWallet(tag);
		tag.putString(DIMENSION, player.level().dimension().identifier().toString());
		if (Lobby.is(player.level())) {
			// saved from the lobby: the character really is where it was before
			CharacterRoster.Origin origin = Lobby.origin(player);
			tag.putString(DIMENSION, origin.dimension());
			tag.put("Pos", Vec3.CODEC.encodeStart(NbtOps.INSTANCE, new Vec3(origin.x(), origin.y(), origin.z())).getOrThrow());
			tag.put("Rotation", Vec2.CODEC.encodeStart(NbtOps.INSTANCE, new Vec2(origin.yRot(), origin.xRot())).getOrThrow());
		}
		return tag;
	}

	/**
	 * Removes the wallet from saved character data and returns its copper. Characters stored before the wallet became shared
	 * (0.5.21) still carry their own coins; the first load pours them into the account's wallet, and later saves leave it out.
	 */
	private static int takeStoredWallet(final CompoundTag tag) {
		return tag.getCompound(AttachmentTarget.NBT_ATTACHMENT_KEY).map(attachments -> {
			String key = ModAttachments.WALLET.identifier().toString();
			int copper = attachments.getIntOr(key, 0);
			attachments.remove(key);
			return Math.max(0, copper);
		}).orElse(0);
	}

	/** What the character wears and holds ({@link #GEAR_SLOTS}), for its figure on the select screen. */
	private static List<ItemStack> gear(final ServerPlayer player) {
		return GEAR_SLOTS.stream().map(slot -> player.getItemBySlot(slot).copy()).toList();
	}

	private static void load(final ServerPlayer player, final CompoundTag data) {
		MinecraftServer server = server(player);
		ServerLevel saved = CharacterRoster.level(server, data.getStringOr(DIMENSION, ""));
		CompoundTag tag = data.copy();
		tag.remove(DIMENSION);
		ACCOUNT_KEYS.forEach(tag::remove);
		int storedCoins = takeStoredWallet(tag);
		GameType mode = player.gameMode();
		Runnable keep = keepAccount(player);
		clear(player);
		player.load(TagValueInput.create(ProblemReporter.DISCARDING, player.level().registryAccess(), tag));
		keep.run();
		resync(player, mode);
		if (saved != null) {
			player.teleportTo(saved, player.getX(), player.getY(), player.getZ(), Set.of(), player.getYRot(), player.getXRot(), true);
		} else {
			// the dimension is gone (a removed datapack or mod): its coordinates mean nothing elsewhere
			toWorldSpawn(player);
		}
		player.setHealth(tag.getFloatOr("Health", player.getMaxHealth()));
		// last, so a load that throws (and is rolled back) never pays the stored coins out twice
		Wallet.add(player, storedCoins);
	}

	/** A fresh character: nothing carried, no class, at the world spawn (the plaza, with the handbook, in the capital's worlds). */
	private static void start(final ServerPlayer player) {
		GameType mode = player.gameMode();
		clear(player);
		player.setExperienceLevels(0);
		player.setExperiencePoints(0);
		player.getFoodData().setFoodLevel(20);
		player.getFoodData().setSaturation(5.0F);
		player.setRemainingFireTicks(0);
		// a new character starts with no bed or anchor and no grave of the one it replaced
		player.setRespawnPosition(null, false);
		player.setLastDeathLocation(java.util.Optional.empty());
		resync(player, mode);
		player.setHealth(player.getMaxHealth());
		toWorldSpawn(player);
		CityServices.welcome(player);
	}

	static void toWorldSpawn(final ServerPlayer player) {
		ServerLevel overworld = server(player).overworld();
		BlockPos spawn = overworld.getRespawnData().pos();
		player.teleportTo(overworld, spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5, Set.of(), player.getYRot(), 0.0F, true);
	}

	/** Takes away what belongs to the character: the mod's attachments (except {@link #SHARED}), effects and the inventory. */
	private static void clear(final ServerPlayer player) {
		for (AttachmentType<?> type : ModAttachments.all()) {
			if (!SHARED.contains(type)) {
				player.removeAttached(type);
			}
		}
		player.removeAllEffects();
		player.getInventory().clearContent();
	}

	/** What the account keeps across a load (the ender chest and the shared attachments), put back by the returned action. */
	private static Runnable keepAccount(final ServerPlayer player) {
		PlayerEnderChestContainer ender = player.getEnderChestInventory();
		List<ItemStack> items = new ArrayList<>();
		for (int i = 0; i < ender.getContainerSize(); i++) {
			items.add(ender.getItem(i).copy());
		}
		List<Runnable> shared = SHARED.stream().map(type -> keep(player, type)).toList();
		return () -> {
			for (int i = 0; i < items.size(); i++) {
				ender.setItem(i, items.get(i));
			}
			shared.forEach(Runnable::run);
		};
	}

	private static <T> Runnable keep(final ServerPlayer player, final AttachmentType<T> type) {
		T value = player.getAttached(type);
		return () -> {
			if (value == null) {
				player.removeAttached(type);
			} else {
				player.setAttached(type, value);
			}
		};
	}

	/**
	 * Loading player data in place tells the client nothing, so everything it shows is sent again: attachments (set anew so Fabric
	 * syncs them), game mode, abilities, health/food/experience, the inventory, the held slot and effects; then the class stats and
	 * the title in everyone's tab list.
	 */
	private static void resync(final ServerPlayer player, final GameType mode) {
		for (AttachmentType<?> type : ModAttachments.all()) {
			reattach(player, type);
		}
		player.setGameMode(mode);
		player.onUpdateAbilities();
		player.resetSentInfo();
		player.inventoryMenu.sendAllDataToRemote();
		player.connection.send(new ClientboundSetHeldSlotPacket(player.getInventory().getSelectedSlot()));
		for (MobEffectInstance effect : player.getActiveEffects()) {
			player.connection.send(new ClientboundUpdateMobEffectPacket(player.getId(), effect, false));
		}
		JobStats.refresh(player);
		Titles.apply(player);
	}

	private static <T> void reattach(final ServerPlayer player, final AttachmentType<T> type) {
		T value = player.getAttached(type);
		if (value != null) {
			player.removeAttached(type);
			player.setAttached(type, value);
		}
	}

	private static boolean fail(final ServerPlayer player, final String key) {
		player.sendSystemMessage(Component.translatable("message.minecraft_mode.character." + key).withStyle(ChatFormatting.RED));
		return false;
	}

	private static MinecraftServer server(final ServerPlayer player) {
		return player.level().getServer();
	}

	private Characters() {
	}
}
