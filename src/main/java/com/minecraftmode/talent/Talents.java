package com.minecraftmode.talent;

import com.minecraftmode.economy.Wallet;
import com.minecraftmode.job.JobData;
import com.minecraftmode.job.JobProgression;
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

/**
 * Talent points: one per 1.5 levels from level 10 ({@link #points}, 60 at level 100), spent in the
 * class's {@link TalentTree}. Talents add stat lines through {@link GearStats}. Changing class starts
 * over; a reset costs the level bracket's guild price in coins.
 */
public final class Talents {
	/** The ranks a player put into nodes, for {@code job} (another class means none). Kept on death, synced. */
	public record TalentData(String job, Map<String, Integer> ranks) {
		public static final TalentData DEFAULT = new TalentData("", Map.of());
		public static final Codec<TalentData> CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.STRING.optionalFieldOf("job", "").forGetter(TalentData::job),
			Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf("ranks", Map.of()).forGetter(TalentData::ranks)
		).apply(i, TalentData::new));
		public static final StreamCodec<ByteBuf, TalentData> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);

		public TalentData {
			ranks = Map.copyOf(ranks);
		}
	}

	public static int points(final int level) {
		return level < 10 ? 0 : (level - 10) * 2 / 3;
	}

	/** The player's ranks for their current class. */
	public static Map<String, Integer> ranks(final Player player) {
		JobData job = JobProgression.get(player);
		TalentData data = player.getAttachedOrElse(ModAttachments.TALENTS, TalentData.DEFAULT);
		return job.hasClass() && data.job().equals(job.job().id()) ? data.ranks() : Map.of();
	}

	public static int rank(final Player player, final String node) {
		return ranks(player).getOrDefault(node, 0);
	}

	public static int spent(final Player player) {
		return ranks(player).values().stream().mapToInt(Integer::intValue).sum();
	}

	public static int available(final Player player) {
		JobData job = JobProgression.get(player);
		return job.hasClass() ? points(job.level()) - spent(player) : 0;
	}

	/** Points the player put into {@code branch}. */
	public static int inBranch(final Player player, final TalentTree.Branch branch) {
		Map<String, Integer> ranks = ranks(player);
		return branch.nodes().stream().mapToInt(n -> ranks.getOrDefault(n.id(), 0)).sum();
	}

	/** True when one more rank of {@code node} can be bought now. */
	public static boolean canSpend(final Player player, final TalentTree.Node node) {
		JobData job = JobProgression.get(player);
		if (!job.hasClass() || !node.id().startsWith(job.job().id() + ".") || available(player) <= 0 || rank(player, node.id()) >= node.maxRank()) {
			return false;
		}
		TalentTree.Branch branch = TalentTree.of(job.job()).stream().filter(b -> b.id().equals(node.branch())).findFirst().orElse(null);
		return branch != null && inBranch(player, branch) >= node.tier() * TalentTree.PER_TIER;
	}

	public static boolean spend(final ServerPlayer player, final String nodeId) {
		TalentTree.Node node = TalentTree.node(nodeId);
		if (node == null || !canSpend(player, node)) {
			return false;
		}
		Map<String, Integer> ranks = new HashMap<>(ranks(player));
		ranks.merge(nodeId, 1, Integer::sum);
		player.setAttached(ModAttachments.TALENTS, new TalentData(JobProgression.get(player).job().id(), ranks));
		GearStats.invalidate(player);
		player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.8F, 1.4F);
		return true;
	}

	public static int resetCost(final Player player) {
		return GearShop.bracketPrice(Math.max(10, JobProgression.get(player).level()));
	}

	public static boolean reset(final ServerPlayer player) {
		if (spent(player) == 0) {
			return false;
		}
		int cost = resetCost(player);
		if (!player.isCreative() && (Coins.total(player) < cost || !Wallet.take(player, cost))) {
			player.sendOverlayMessage(Component.translatable("message.minecraft_mode.talent.no_coins", Coins.component(cost)).withStyle(ChatFormatting.RED));
			return false;
		}
		player.setAttached(ModAttachments.TALENTS, new TalentData(JobProgression.get(player).job().id(), Map.of()));
		GearStats.invalidate(player);
		player.sendSystemMessage(Component.translatable("message.minecraft_mode.talent.reset").withStyle(ChatFormatting.YELLOW));
		return true;
	}

	/** Every stat the player's talents add. */
	public static List<StatLine> lines(final Player player) {
		List<StatLine> out = new ArrayList<>();
		for (Map.Entry<String, Integer> e : ranks(player).entrySet()) {
			TalentTree.Node node = TalentTree.node(e.getKey());
			if (node != null && e.getValue() > 0) {
				out.addAll(node.lines(e.getValue()));
			}
		}
		return out;
	}

	private Talents() {
	}
}
