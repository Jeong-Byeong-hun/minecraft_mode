package com.minecraftmode.registry;

import com.minecraftmode.MinecraftMode;
import com.minecraftmode.bounty.BountyData;
import com.minecraftmode.companion.Companions;
import com.minecraftmode.craft.Profession;
import com.minecraftmode.dungeon.DungeonData;
import com.minecraftmode.job.JobData;
import com.minecraftmode.job.Paragon;
import com.minecraftmode.job.quest.QuestData;
import com.minecraftmode.progress.PlayerRecords;
import com.minecraftmode.story.StoryData;
import com.minecraftmode.talent.Talents;
import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.network.codec.ByteBufCodecs;

public final class ModAttachments {
	/** Class, level, MP and cooldowns. Kept through death; synced to the owning player for the HUD. */
	public static final AttachmentType<JobData> JOB = AttachmentRegistry.create(
		MinecraftMode.id("job"),
		builder -> builder.persistent(JobData.CODEC)
			.copyOnDeath()
			.initializer(() -> JobData.DEFAULT)
			.syncWith(JobData.STREAM_CODEC, AttachmentSyncPredicate.targetOnly())
	);

	/** Active advancement trial and its kill counts, plus the first-visit flag for the city. */
	public static final AttachmentType<QuestData> QUEST = AttachmentRegistry.create(
		MinecraftMode.id("quest"),
		builder -> builder.persistent(QuestData.CODEC)
			.copyOnDeath()
			.initializer(() -> QuestData.DEFAULT)
			.syncWith(QuestData.STREAM_CODEC, AttachmentSyncPredicate.targetOnly())
	);

	/** Coins in the wallet, in copper (1 silver = 9, 1 gold = 81). Kept through death; synced for the HUD. */
	public static final AttachmentType<Integer> WALLET = AttachmentRegistry.create(
		MinecraftMode.id("wallet"),
		builder -> builder.persistent(Codec.INT)
			.copyOnDeath()
			.initializer(() -> 0)
			.syncWith(ByteBufCodecs.VAR_INT.cast(), AttachmentSyncPredicate.targetOnly())
	);

	/** Codex counts, raid lockouts, achievements and the chosen title. Kept through death; synced to the owner. */
	public static final AttachmentType<PlayerRecords> RECORDS = AttachmentRegistry.create(
		MinecraftMode.id("records"),
		builder -> builder.persistent(PlayerRecords.CODEC)
			.copyOnDeath()
			.initializer(() -> PlayerRecords.DEFAULT)
			.syncWith(PlayerRecords.STREAM_CODEC, AttachmentSyncPredicate.targetOnly())
	);

	/** Daily and three-day guild bounties and merit. Kept through death; synced to the owner. */
	public static final AttachmentType<BountyData> BOUNTY = AttachmentRegistry.create(
		MinecraftMode.id("bounty"),
		builder -> builder.persistent(BountyData.CODEC)
			.copyOnDeath()
			.initializer(() -> BountyData.DEFAULT)
			.syncWith(BountyData.STREAM_CODEC, AttachmentSyncPredicate.targetOnly())
	);

	/** Talent ranks of the current class. Kept through death; synced to the owner. */
	public static final AttachmentType<Talents.TalentData> TALENTS = AttachmentRegistry.create(
		MinecraftMode.id("talents"),
		builder -> builder.persistent(Talents.TalentData.CODEC)
			.copyOnDeath()
			.initializer(() -> Talents.TalentData.DEFAULT)
			.syncWith(Talents.TalentData.STREAM_CODEC, AttachmentSyncPredicate.targetOnly())
	);

	/** Paragon level, experience and board ranks (growth past level 100). Kept through death; synced to the owner. */
	public static final AttachmentType<Paragon.ParagonData> PARAGON = AttachmentRegistry.create(
		MinecraftMode.id("paragon"),
		builder -> builder.persistent(Paragon.ParagonData.CODEC)
			.copyOnDeath()
			.initializer(() -> Paragon.ParagonData.DEFAULT)
			.syncWith(Paragon.ParagonData.STREAM_CODEC, AttachmentSyncPredicate.targetOnly())
	);

	/** Experience of the cooking, alchemy and smithing professions. Kept through death; synced to the owner. */
	public static final AttachmentType<Profession.Data> PROFESSIONS = AttachmentRegistry.create(
		MinecraftMode.id("professions"),
		builder -> builder.persistent(Profession.Data.CODEC)
			.copyOnDeath()
			.initializer(() -> Profession.Data.DEFAULT)
			.syncWith(Profession.Data.STREAM_CODEC, AttachmentSyncPredicate.targetOnly())
	);

	/** Owned pets (with their experience) and mounts, the summoned pet and the last mount. Kept through death; synced to the owner. */
	public static final AttachmentType<Companions.Data> COMPANIONS = AttachmentRegistry.create(
		MinecraftMode.id("companions"),
		builder -> builder.persistent(Companions.Data.CODEC)
			.copyOnDeath()
			.initializer(() -> Companions.Data.DEFAULT)
			.syncWith(Companions.Data.STREAM_CODEC, AttachmentSyncPredicate.targetOnly())
	);

	/** Whether Quartermaster Bram's starter kit was taken. Kept through death; synced to the owner. */
	public static final AttachmentType<Boolean> STARTER_KIT = AttachmentRegistry.create(
		MinecraftMode.id("starter_kit"),
		builder -> builder.persistent(Codec.BOOL)
			.copyOnDeath()
			.syncWith(ByteBufCodecs.BOOL, AttachmentSyncPredicate.targetOnly())
	);

	/** Dungeon clears and best timed keystones. Kept through death; synced to the owner. */
	public static final AttachmentType<DungeonData> DUNGEON = AttachmentRegistry.create(
		MinecraftMode.id("dungeon"),
		builder -> builder.persistent(DungeonData.CODEC)
			.copyOnDeath()
			.initializer(() -> DungeonData.DEFAULT)
			.syncWith(DungeonData.STREAM_CODEC, AttachmentSyncPredicate.targetOnly())
	);

	/** Main story chapter, whether its completion was announced, and world bosses defeated. Kept through death; synced to the owner. */
	public static final AttachmentType<StoryData> STORY = AttachmentRegistry.create(
		MinecraftMode.id("story"),
		builder -> builder.persistent(StoryData.CODEC)
			.copyOnDeath()
			.initializer(() -> StoryData.DEFAULT)
			.syncWith(StoryData.STREAM_CODEC, AttachmentSyncPredicate.targetOnly())
	);

	/** The worn title (achievement id). Kept through death; synced to every player so names show it. */
	public static final AttachmentType<String> TITLE = AttachmentRegistry.create(
		MinecraftMode.id("title"),
		builder -> builder.persistent(Codec.STRING)
			.copyOnDeath()
			.syncWith(ByteBufCodecs.STRING_UTF8, AttachmentSyncPredicate.all())
	);

	public static void init() {
	}

	private ModAttachments() {
	}
}
