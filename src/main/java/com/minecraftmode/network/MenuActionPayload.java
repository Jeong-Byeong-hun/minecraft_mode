package com.minecraftmode.network;

import com.minecraftmode.MinecraftMode;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Client -> server, from the adventurer's menu: the party actions of {@code /party} ({@code name} is the player to invite, kick or
 * promote), {@link #PAY} {@code amount} copper to the online player {@code name} ({@code /wallet pay}) and {@link #LEAVE} the raid or
 * dungeon the player is in ({@code /raid leave}, {@code /dungeon leave}).
 */
public record MenuActionPayload(int action, String name, int amount) implements CustomPacketPayload {
	public static final int PARTY_CREATE = 0;
	public static final int PARTY_INVITE = 1;
	public static final int PARTY_ACCEPT = 2;
	public static final int PARTY_LEAVE = 3;
	public static final int PARTY_KICK = 4;
	public static final int PARTY_PROMOTE = 5;
	public static final int PARTY_DISBAND = 6;
	public static final int PAY = 7;
	public static final int LEAVE = 8;
	public static final Type<MenuActionPayload> TYPE = new Type<>(MinecraftMode.id("menu_action"));
	public static final StreamCodec<RegistryFriendlyByteBuf, MenuActionPayload> CODEC = StreamCodec.composite(
		ByteBufCodecs.VAR_INT, MenuActionPayload::action,
		ByteBufCodecs.stringUtf8(32), MenuActionPayload::name,
		ByteBufCodecs.VAR_INT, MenuActionPayload::amount,
		MenuActionPayload::new
	);

	public MenuActionPayload(final int action) {
		this(action, "", 0);
	}

	@Override
	public Type<MenuActionPayload> type() {
		return TYPE;
	}
}
