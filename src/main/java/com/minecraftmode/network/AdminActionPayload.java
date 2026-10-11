package com.minecraftmode.network;

import com.minecraftmode.MinecraftMode;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Client -> server, from the admin screen (operators only; {@code admin.Admin} checks every one). {@code target} names the online
 * player the action is about ("" = the operator), {@code text} and {@code a}..{@code c} are the action's arguments as noted on each
 * constant. The server answers every action with a fresh {@link AdminStatePayload}; {@link #REFRESH} only asks for one.
 */
public record AdminActionPayload(int action, String target, String text, int a, int b, int c) implements CustomPacketPayload {
	/** a = market page, b = land page, text = market search. */
	public static final int REFRESH = 0;
	/** text = class id, a = tier. */
	public static final int JOB_SET = 1;
	/** a = level. */
	public static final int LEVEL_SET = 2;
	/** a = class EXP. */
	public static final int EXP_ADD = 3;
	public static final int MANA_FILL = 4;
	public static final int COOLDOWNS = 5;
	public static final int QUEST_CLEAR = 6;
	public static final int QUEST_GOALS = 7;
	/** a = copper. */
	public static final int MONEY_GIVE = 8;
	/** a = copper. */
	public static final int MONEY_SET = 9;
	/** The operator goes to the target. */
	public static final int TP_TO = 10;
	/** The target comes to the operator. */
	public static final int BRING = 11;
	/** a = game type id. */
	public static final int GAMEMODE = 12;
	/** text = reason. */
	public static final int KICK = 13;
	/** Full health, food and no fire. */
	public static final int HEAL = 14;
	/** a = story chapter. */
	public static final int STORY_SET = 15;
	/** text = dungeon id, a = keystone level; the target's party. */
	public static final int DUNGEON_START = 20;
	/** text = boss id; the target's party. */
	public static final int RAID_START = 21;
	/** a = run id. */
	public static final int DUNGEON_STOP = 22;
	/** a = raid id. */
	public static final int RAID_STOP = 23;
	/** text = named monster id ("" = random), near the target. */
	public static final int TITAN = 24;
	public static final int INVASION = 25;
	public static final int EVENT_STOP = 26;
	/** text = entity type id, a = count, b = 1 for where the target looks (0 = at their feet). */
	public static final int SUMMON = 27;
	/** text = dimension id, a/b/c = x/y/z ({@link #SURFACE} as y = the top of the column). */
	public static final int TP_COORDS = 30;
	/** a = {@link com.minecraftmode.travel.Destination} ordinal. */
	public static final int TP_PLACE = 31;
	/** Every online player comes to the operator. */
	public static final int GATHER = 32;
	/** a = time of day in ticks. */
	public static final int TIME = 40;
	/** a = 0 clear, 1 rain, 2 thunder. */
	public static final int WEATHER = 41;
	/** a = real days to skip (raid and bounty resets). */
	public static final int SKIP_DAYS = 42;
	/** text = message for everyone. */
	public static final int BROADCAST = 43;
	/** a = listing id; the item goes to the seller's mailbox. */
	public static final int AUCTION_REMOVE = 50;
	/** text = owner UUID, a/b = chunk x/z, c = min y; items and the deed go back to the owner. */
	public static final int LAND_RELEASE = 51;

	public static final int SURFACE = Integer.MIN_VALUE;
	public static final int MAX_TEXT = 256;
	public static final Type<AdminActionPayload> TYPE = new Type<>(MinecraftMode.id("admin_action"));
	public static final StreamCodec<RegistryFriendlyByteBuf, AdminActionPayload> CODEC = StreamCodec.composite(
		ByteBufCodecs.VAR_INT, AdminActionPayload::action,
		ByteBufCodecs.stringUtf8(32), AdminActionPayload::target,
		ByteBufCodecs.stringUtf8(MAX_TEXT), AdminActionPayload::text,
		ByteBufCodecs.INT, AdminActionPayload::a,
		ByteBufCodecs.INT, AdminActionPayload::b,
		ByteBufCodecs.INT, AdminActionPayload::c,
		AdminActionPayload::new
	);

	public static AdminActionPayload of(final int action, final String target) {
		return new AdminActionPayload(action, target, "", 0, 0, 0);
	}

	@Override
	public Type<AdminActionPayload> type() {
		return TYPE;
	}
}
