package com.minecraftmode.residence;

import com.minecraftmode.bag.Bags;
import com.minecraftmode.market.AuctionHouse;
import com.minecraftmode.network.LandActionPayload;
import com.minecraftmode.network.OpenLandPayload;
import com.minecraftmode.registry.ModItems;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;

/** The land screen's server side: what it shows ({@link #open}) and what it may change ({@link #act}). */
public final class LandOffice {
	private static final int EDGE_STEP = 2;

	public static void open(final ServerPlayer player) {
		LandClaims claims = LandClaims.get(player.level().getServer());
		List<OpenLandPayload.Plot> plots = new ArrayList<>();
		for (LandClaims.Claim claim : claims.owned(player.getUUID())) {
			plots.add(new OpenLandPayload.Plot(claim.chunkX(), claim.chunkZ(), claim.minY()));
		}
		List<OpenLandPayload.Friend> friends = new ArrayList<>();
		for (UUID friend : claims.trusted(player.getUUID())) {
			friends.add(new OpenLandPayload.Friend(friend, claims.name(friend)));
		}
		ServerPlayNetworking.send(player, new OpenLandPayload(plots, friends, LandClaims.MAX_CLAIMS, LandClaims.MAX_TRUSTED));
	}

	/** Everything here only touches the asking player's own land and list, so no other check is needed. */
	public static void act(final ServerPlayer player, final LandActionPayload action) {
		LandClaims claims = LandClaims.get(player.level().getServer());
		UUID owner = player.getUUID();
		switch (action.action()) {
			case LandActionPayload.TRUST -> {
				ServerPlayer friend = player.level().getServer().getPlayerList().getPlayerByName(action.text().trim());
				if (friend == null) {
					tell(player, "not_online", ChatFormatting.RED, action.text().trim());
				} else if (friend == player) {
					tell(player, "trust_self", ChatFormatting.RED);
				} else if (claims.trust(owner, friend.getUUID(), friend.getPlainTextName())) {
					tell(player, "trusted", ChatFormatting.GREEN, friend.getPlainTextName());
					tell(friend, "trusted_you", ChatFormatting.GREEN, player.getPlainTextName());
				} else {
					tell(player, "trust_full", ChatFormatting.RED);
				}
			}
			case LandActionPayload.UNTRUST -> {
				try {
					UUID friend = UUID.fromString(action.text());
					if (claims.untrust(owner, friend)) {
						tell(player, "untrusted", ChatFormatting.YELLOW, claims.name(friend));
					}
				} catch (IllegalArgumentException e) {
					return;
				}
			}
			case LandActionPayload.ABANDON -> {
				LandClaims.Claim claim = new LandClaims.Claim(ChunkPos.pack(action.chunkX(), action.chunkZ()), action.minY(), owner);
				ServerLevel residence = ResidenceDimension.level(player.level().getServer());
				if (residence != null && claims.abandon(owner, claim.chunk(), claim.minY())) {
					int returned = LandRestore.restore(residence, claim, stack -> Bags.give(player, stack));
					Bags.give(player, new ItemStack(ModItems.LAND_DEED));
					tell(player, "abandoned", ChatFormatting.YELLOW, returned);
				}
			}
			default -> {
				return;
			}
		}
		open(player);
	}

	/**
	 * Operator release (admin screen): frees {@code claim} like its owner giving it up. Items and the deed go to the owner, or to
	 * their market mailbox when they are offline, so nothing is lost.
	 */
	public static boolean release(final MinecraftServer server, final LandClaims.Claim claim) {
		ServerLevel residence = ResidenceDimension.level(server);
		if (residence == null || !LandClaims.get(server).abandon(claim.owner(), claim.chunk(), claim.minY())) {
			return false;
		}
		ServerPlayer owner = server.getPlayerList().getPlayer(claim.owner());
		Consumer<ItemStack> back = owner != null ? stack -> Bags.give(owner, stack) : stack -> AuctionHouse.get(server).sendItem(claim.owner(), stack);
		int returned = LandRestore.restore(residence, claim, back);
		back.accept(new ItemStack(ModItems.LAND_DEED));
		if (owner != null) {
			tell(owner, "released", ChatFormatting.YELLOW, returned);
		}
		return true;
	}

	/** Shows the 12 edges of a freshly claimed box with sparks. */
	static void outline(final ServerLevel level, final LandClaims.Claim claim) {
		int x0 = claim.chunkX() << 4;
		int z0 = claim.chunkZ() << 4;
		int y0 = claim.minY();
		int size = LandClaims.SIZE;
		for (int i = 0; i <= size; i += EDGE_STEP) {
			for (int a = 0; a <= size; a += size) {
				for (int b = 0; b <= size; b += size) {
					level.sendParticles(ParticleTypes.HAPPY_VILLAGER, x0 + i, y0 + a, z0 + b, 1, 0, 0, 0, 0);
					level.sendParticles(ParticleTypes.HAPPY_VILLAGER, x0 + a, y0 + i, z0 + b, 1, 0, 0, 0, 0);
					level.sendParticles(ParticleTypes.HAPPY_VILLAGER, x0 + a, y0 + b, z0 + i, 1, 0, 0, 0, 0);
				}
			}
		}
		level.playSound(null, x0 + 8, y0 + 1, z0 + 8, SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.6F, 1.4F);
	}

	static void tell(final ServerPlayer player, final String key, final ChatFormatting color, final Object... args) {
		player.sendSystemMessage(Component.translatable("message.minecraft_mode.land." + key, args).withStyle(color));
	}

	private LandOffice() {
	}
}
