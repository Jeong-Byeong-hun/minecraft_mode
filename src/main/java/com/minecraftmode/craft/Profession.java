package com.minecraftmode.craft;

import com.minecraftmode.registry.ModAttachments;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import org.jspecify.annotations.Nullable;

/**
 * Crafting professions (생활 기술): cooking, alchemy and smithing, each levelled 1..{@value #MAX_LEVEL} by crafting at its station
 * (alchemy also by gathering herbs). Every player has all three. Ids are saved keys.
 */
public enum Profession {
	COOKING("cooking", "Cooking", "요리", 0xE0A040),
	ALCHEMY("alchemy", "Alchemy", "연금술", 0x60C060),
	SMITHING("smithing", "Smithing", "대장 기술", 0x9AA0B0);

	public static final int MAX_LEVEL = 50;

	public final String id;
	public final String en;
	public final String ko;
	public final int color;

	Profession(final String id, final String en, final String ko, final int color) {
		this.id = id;
		this.en = en;
		this.ko = ko;
		this.color = color;
	}

	public String nameKey() {
		return "profession.minecraft_mode." + this.id;
	}

	public static @Nullable Profession byId(final String id) {
		for (Profession p : values()) {
			if (p.id.equals(id)) {
				return p;
			}
		}
		return null;
	}

	/** Experience of every profession. Synced to the owner, kept on death. */
	public record Data(Map<String, Integer> exp) {
		public static final Data DEFAULT = new Data(Map.of());
		public static final Codec<Data> CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf("exp", Map.of()).forGetter(Data::exp)
		).apply(i, Data::new));
		public static final StreamCodec<ByteBuf, Data> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);

		public Data {
			exp = Map.copyOf(exp);
		}

		public int exp(final Profession p) {
			return this.exp.getOrDefault(p.id, 0);
		}
	}

	/** Experience from level {@code level} to the next. */
	public static int expToNext(final int level) {
		return 20 + 6 * level;
	}

	/** Level reached with {@code total} experience (1..50). */
	public static int levelFor(final int total) {
		int level = 1;
		int left = total;
		while (level < MAX_LEVEL && left >= expToNext(level)) {
			left -= expToNext(level);
			level++;
		}
		return level;
	}

	/** Experience gathered inside the current level. */
	public static int progress(final int total) {
		int level = 1;
		int left = total;
		while (level < MAX_LEVEL && left >= expToNext(level)) {
			left -= expToNext(level);
			level++;
		}
		return level >= MAX_LEVEL ? 0 : left;
	}

	/** Total experience needed to reach {@code level}. */
	public static int totalFor(final int level) {
		int total = 0;
		for (int l = 1; l < Math.min(level, MAX_LEVEL); l++) {
			total += expToNext(l);
		}
		return total;
	}

	public static Data data(final Player player) {
		return player.getAttachedOrElse(ModAttachments.PROFESSIONS, Data.DEFAULT);
	}

	public int level(final Player player) {
		return levelFor(data(player).exp(this));
	}

	/** Chance in percent of making twice the output (mastery). */
	public int doubleChance(final Player player) {
		return this.level(player) / 2;
	}

	public void addExp(final ServerPlayer player, final int amount) {
		if (amount <= 0) {
			return;
		}
		Data data = data(player);
		int before = levelFor(data.exp(this));
		Map<String, Integer> exp = new HashMap<>(data.exp());
		exp.merge(this.id, amount, Integer::sum);
		player.setAttached(ModAttachments.PROFESSIONS, new Data(exp));
		int after = levelFor(exp.get(this.id));
		if (after > before) {
			player.sendSystemMessage(Component.translatable("message.minecraft_mode.profession.level_up", Component.translatable(this.nameKey()), after)
				.withColor(this.color));
			player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.6F, 1.6F);
			com.minecraftmode.progress.Progress.professionLevel(player, this, after);
		}
	}
}
