package com.minecraftmode.job;

import com.minecraftmode.economy.Wallet;
import com.minecraftmode.job.engrave.EngraveStat;
import com.minecraftmode.job.gear.GearStats;
import com.minecraftmode.job.gear.StatLine;
import com.minecraftmode.loot.Coins;
import com.minecraftmode.loot.GearShop;
import com.minecraftmode.registry.ModAttachments;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
 * Paragon levels (초월): growth past level 100. Experience earned at the cap fills paragon levels (each a little longer than the
 * last); every paragon level is one point for the paragon board, eight small stats of {@link #MAX_RANK} ranks each. The board
 * is shared by every class and can be reset for coins.
 */
public final class Paragon {
	public static final int MAX_RANK = 50;

	/** One stat of the board: what one rank adds. Ids are saved keys. */
	public enum Stat {
		MIGHT("might", "Might", "위력", List.of(StatLine.of(EngraveStat.BASIC_DAMAGE, 0.3F), StatLine.of(EngraveStat.SKILL_DAMAGE, 0.3F))),
		VITALITY("vitality", "Vitality", "활력", List.of(StatLine.of(EngraveStat.MAX_HEALTH, 0.2F))),
		PRECISION("precision", "Precision", "정밀", List.of(StatLine.of(EngraveStat.CRIT_CHANCE, 0.1F))),
		FEROCITY("ferocity", "Ferocity", "흉포", List.of(StatLine.of(EngraveStat.CRIT_DAMAGE, 0.6F))),
		GUARD("guard", "Guard", "수호", List.of(StatLine.of(EngraveStat.DAMAGE_REDUCTION, 0.1F))),
		HASTE("haste", "Haste", "가속", List.of(StatLine.of(EngraveStat.COOLDOWN, 0.1F))),
		SPIRIT("spirit", "Spirit", "정신", List.of(StatLine.of(EngraveStat.MAX_MANA, 1.0F), StatLine.of(EngraveStat.MANA_REGEN, 0.02F))),
		FORTUNE("fortune", "Fortune", "행운", List.of(StatLine.of(EngraveStat.ITEM_FIND, 0.4F), StatLine.of(EngraveStat.GOLD_FIND, 0.4F)));

		public final String id;
		public final String en;
		public final String ko;
		public final List<StatLine> perRank;

		Stat(final String id, final String en, final String ko, final List<StatLine> perRank) {
			this.id = id;
			this.en = en;
			this.ko = ko;
			this.perRank = perRank;
		}

		public String nameKey() {
			return "paragon.minecraft_mode." + this.id;
		}

		public List<StatLine> lines(final int ranks) {
			List<StatLine> out = new ArrayList<>();
			for (StatLine line : this.perRank) {
				out.add(StatLine.of(line.stat(), Math.round(line.value() * ranks * 100.0F) / 100.0F));
			}
			return out;
		}

		public static @Nullable Stat byId(final String id) {
			for (Stat s : values()) {
				if (s.id.equals(id)) {
					return s;
				}
			}
			return null;
		}
	}

	public record ParagonData(int level, int exp, Map<String, Integer> ranks) {
		public static final ParagonData DEFAULT = new ParagonData(0, 0, Map.of());
		public static final Codec<ParagonData> CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.INT.optionalFieldOf("level", 0).forGetter(ParagonData::level),
			Codec.INT.optionalFieldOf("exp", 0).forGetter(ParagonData::exp),
			Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf("ranks", Map.of()).forGetter(ParagonData::ranks)
		).apply(i, ParagonData::new));
		public static final StreamCodec<ByteBuf, ParagonData> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);

		public ParagonData {
			ranks = Map.copyOf(ranks);
		}

		public int rank(final Stat stat) {
			return this.ranks.getOrDefault(stat.id, 0);
		}

		public int spent() {
			return this.ranks.values().stream().mapToInt(Integer::intValue).sum();
		}

		public int available() {
			return this.level - this.spent();
		}
	}

	public static ParagonData get(final Player player) {
		return player.getAttachedOrElse(ModAttachments.PARAGON, ParagonData.DEFAULT);
	}

	/** Experience from paragon level {@code level} to the next. */
	public static int expToNext(final int level) {
		return 4000 + 40 * level;
	}

	/** Experience earned at the class level cap. */
	public static void addExp(final ServerPlayer player, final int amount) {
		if (amount <= 0) {
			return;
		}
		ParagonData data = get(player);
		int level = data.level();
		int exp = data.exp() + amount;
		int gained = 0;
		while (exp >= expToNext(level)) {
			exp -= expToNext(level);
			level++;
			gained++;
		}
		player.setAttached(ModAttachments.PARAGON, new ParagonData(level, exp, data.ranks()));
		if (gained > 0) {
			player.sendSystemMessage(Component.translatable("message.minecraft_mode.paragon.level_up", level).withStyle(ChatFormatting.LIGHT_PURPLE));
			player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8F, 0.8F);
			com.minecraftmode.progress.Progress.changed(player);
		}
	}

	/** Puts one paragon point into {@code statId}. */
	public static boolean spend(final ServerPlayer player, final String statId) {
		Stat stat = Stat.byId(statId);
		ParagonData data = get(player);
		if (stat == null || data.available() <= 0 || data.rank(stat) >= MAX_RANK) {
			return false;
		}
		Map<String, Integer> ranks = new HashMap<>(data.ranks());
		ranks.merge(stat.id, 1, Integer::sum);
		player.setAttached(ModAttachments.PARAGON, new ParagonData(data.level(), data.exp(), ranks));
		GearStats.invalidate(player);
		return true;
	}

	public static int resetCost() {
		return GearShop.bracketPrice(100);
	}

	public static boolean reset(final ServerPlayer player) {
		ParagonData data = get(player);
		if (data.spent() == 0) {
			return false;
		}
		int cost = resetCost();
		if (!player.isCreative() && !Wallet.take(player, cost)) {
			player.sendOverlayMessage(Component.translatable("message.minecraft_mode.talent.no_coins", Coins.component(cost)).withStyle(ChatFormatting.RED));
			return false;
		}
		player.setAttached(ModAttachments.PARAGON, new ParagonData(data.level(), data.exp(), Map.of()));
		GearStats.invalidate(player);
		player.sendSystemMessage(Component.translatable("message.minecraft_mode.paragon.reset").withStyle(ChatFormatting.YELLOW));
		return true;
	}

	/** Every stat the paragon board adds. */
	public static List<StatLine> lines(final Player player) {
		ParagonData data = get(player);
		List<StatLine> out = new ArrayList<>();
		for (Stat stat : Stat.values()) {
			int rank = data.rank(stat);
			if (rank > 0) {
				out.addAll(stat.lines(rank));
			}
		}
		return out;
	}

	private Paragon() {
	}
}
