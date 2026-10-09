package com.minecraftmode.bounty;

import com.minecraftmode.bounty.BountyData.Bounty;
import com.minecraftmode.companion.Companions;
import com.minecraftmode.consumable.ConsumableDef;
import com.minecraftmode.consumable.Consumables;
import com.minecraftmode.economy.Wallet;
import com.minecraftmode.job.JobProgression;
import com.minecraftmode.loot.EvolutionEtherItem;
import com.minecraftmode.loot.GearShop;
import com.minecraftmode.progress.Progress;
import com.minecraftmode.progress.ResetCycle;
import com.minecraftmode.registry.ModAttachments;
import com.minecraftmode.registry.ModItems;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;

/**
 * Guild bounties. Every Minecraft day a player gets three daily bounties and every three-day cycle one
 * bigger bounty, rolled for their level. They count on their own (deliveries are checked when handed
 * in); finished ones are claimed at the guild's bounty clerk for coins, merit, Evolution Ether and
 * enhancement stones. Merit buys from the {@link #SHOP merit shop}.
 */
public final class Bounties {
	public static final int DAILY = 3;
	public static final int SPECIAL = 3;

	private record Target(String id, int need, int minLevel) {
	}

	private static final List<Target> KILL_TYPES = List.of(
		new Target("minecraft:zombie", 15, 0), new Target("minecraft:skeleton", 15, 0), new Target("minecraft:spider", 12, 0),
		new Target("minecraft:creeper", 10, 0), new Target("minecraft:drowned", 8, 10), new Target("minecraft:husk", 8, 10),
		new Target("minecraft:enderman", 5, 15), new Target("minecraft:stray", 6, 15), new Target("minecraft:witch", 2, 20),
		new Target("minecraft:blaze", 6, 40), new Target("minecraft:magma_cube", 6, 40), new Target("minecraft:wither_skeleton", 5, 55));
	private static final List<Target> DELIVERIES = List.of(
		new Target("minecraft:iron_ingot", 16, 0), new Target("minecraft:wheat", 32, 0), new Target("minecraft:cooked_beef", 16, 0),
		new Target("minecraft:leather", 12, 0), new Target("minecraft:string", 16, 0), new Target("minecraft:gold_ingot", 8, 10),
		new Target("minecraft:gunpowder", 12, 10), new Target("minecraft:redstone", 32, 10), new Target("minecraft_mode:essence", 8, 10),
		new Target("minecraft:lapis_lazuli", 16, 15), new Target("minecraft_mode:mythril_ingot", 4, 25), new Target("minecraft:diamond", 2, 35),
		new Target("minecraft:quartz", 16, 40), new Target("minecraft:blaze_rod", 6, 45));

	/** What a finished bounty pays. */
	public record Reward(int coins, int merit, int ether, int stones) {
	}

	/** What a merit shop offer gives {@code player} (of class level {@code level}); empty when they can make no use of it. */
	@FunctionalInterface
	public interface Gift {
		ItemStack make(Player player, int level, RandomSource random);
	}

	/** One merit shop offer: the merit price and what it gives. */
	public record Offer(String id, int cost, Gift item) {
		public String nameKey() {
			return "merit.minecraft_mode." + this.id;
		}
	}

	public static final List<Offer> SHOP = List.of(
		new Offer("enhancement_stone", 12, (p, level, r) -> new ItemStack(ModItems.ENHANCEMENT_STONE)),
		new Offer("enhancement_stones", 55, (p, level, r) -> new ItemStack(ModItems.ENHANCEMENT_STONE, 5)),
		new Offer("protection_scroll", 60, (p, level, r) -> new ItemStack(ModItems.PROTECTION_SCROLL)),
		new Offer("lair_map", 15, (p, level, r) -> new ItemStack(ModItems.LAIR_MAP)),
		new Offer("condensed_essence", 10, (p, level, r) -> new ItemStack(ModItems.CONDENSED_ESSENCE, 2)),
		new Offer("ether", 35, (p, level, r) -> EvolutionEtherItem.of(Math.max(10, level), 10)),
		new Offer("tier3", 25, (p, level, r) -> random(3, 2, r)),
		new Offer("tier4", 90, (p, level, r) -> random(4, 1, r)),
		new Offer("return_scrolls", 5, (p, level, r) -> new ItemStack(ModItems.RETURN_SCROLL, 3)),
		// a pet the buyer does not own yet; nothing (and no charge) once they have them all
		new Offer("pet_charm", 50, (p, level, r) -> Companions.randomPet(Rarity.UNCOMMON, r, Companions.data(p))),
		new Offer("mount_whistle", 40, (p, level, r) -> Companions.data(p).hasMount("swift_stallion") ? ItemStack.EMPTY
			: new ItemStack(Companions.mountItem("swift_stallion"))));

	private static ItemStack random(final int tier, final int count, final RandomSource random) {
		List<ConsumableDef> pool = Consumables.tier(tier);
		return pool.isEmpty() ? ItemStack.EMPTY : new ItemStack(Consumables.item(pool.get(random.nextInt(pool.size()))), count);
	}

	public static BountyData get(final Player player) {
		return player.getAttachedOrElse(ModAttachments.BOUNTY, BountyData.DEFAULT);
	}

	public static void set(final ServerPlayer player, final BountyData data) {
		player.setAttached(ModAttachments.BOUNTY, data);
	}

	private static int level(final Player player) {
		return Math.max(1, JobProgression.get(player).level());
	}

	// ------------------------------------------------------------------ rolling

	/**
	 * Rolls new daily bounties on a new day and a new cycle bounty on a new cycle. A bounty that is finished but not handed in
	 * yet is carried over instead (the player never loses a reward to the clock); the new roll takes its slot once it is claimed.
	 */
	public static void ensure(final ServerPlayer player) {
		BountyData data = get(player);
		long day = ResetCycle.day(player.level());
		long cycle = ResetCycle.cycle(player.level());
		boolean changed = false;
		if (data.day() != day) {
			List<Bounty> daily = rollDaily(player, day);
			for (int i = 0; i < daily.size() && i < data.daily().size(); i++) {
				if (data.daily().get(i).keeps()) {
					daily.set(i, data.daily().get(i).asCarried());
				}
			}
			data = new BountyData(day, daily, data.cycle(), data.special(), data.merit());
			changed = true;
		}
		if (data.cycle() != cycle) {
			Bounty special = data.special().keeps() ? data.special().asCarried() : rollSpecial(player, cycle);
			data = new BountyData(data.day(), data.daily(), cycle, special, data.merit());
			changed = true;
		}
		if (changed) {
			set(player, data);
		}
	}

	/** The bounty slot {@code index} would hold now if nothing had been carried over. */
	private static Bounty fresh(final ServerPlayer player, final BountyData data, final int index) {
		if (index == SPECIAL) {
			return rollSpecial(player, data.cycle());
		}
		List<Bounty> daily = rollDaily(player, data.day());
		return index < daily.size() ? daily.get(index) : Bounty.EMPTY;
	}

	private static List<Bounty> rollDaily(final Player player, final long day) {
		RandomSource random = RandomSource.create(player.getUUID().getLeastSignificantBits() ^ day * 0x5DEECE66DL);
		int level = level(player);
		List<BountyKind> pool = new ArrayList<>(List.of(BountyKind.KILL_ANY, BountyKind.KILL_TYPE, BountyKind.KILL_NAMED, BountyKind.MINE_ORE, BountyKind.DELIVER));
		if (level >= 10) {
			pool.add(BountyKind.CLEAR_LAIR);
		}
		List<Bounty> out = new ArrayList<>();
		while (out.size() < DAILY && !pool.isEmpty()) {
			out.add(make(pool.remove(random.nextInt(pool.size())), level, random, false));
		}
		return out;
	}

	private static Bounty rollSpecial(final Player player, final long cycle) {
		RandomSource random = RandomSource.create(player.getUUID().getMostSignificantBits() ^ cycle * 0x9E3779B97F4A7C15L);
		int level = level(player);
		List<BountyKind> pool = new ArrayList<>(List.of(BountyKind.KILL_NAMED, BountyKind.KILL_ANY));
		if (level >= 10) {
			pool.add(BountyKind.CLEAR_LAIR);
		}
		if (level >= 20) {
			pool.add(BountyKind.RAID);
		}
		return make(pool.get(random.nextInt(pool.size())), level, random, true);
	}

	private static Bounty make(final BountyKind kind, final int level, final RandomSource random, final boolean special) {
		return switch (kind) {
			case KILL_ANY -> new Bounty(kind.id(), "", special ? 150 : 25 + level / 4, 0, false);
			case KILL_TYPE -> {
				Target t = pick(KILL_TYPES, level, random);
				yield new Bounty(kind.id(), t.id(), t.need(), 0, false);
			}
			case KILL_NAMED -> new Bounty(kind.id(), "", special ? (level < 30 ? 3 : 5) : (level < 30 ? 1 : 2), 0, false);
			case CLEAR_LAIR -> new Bounty(kind.id(), "", special ? 3 : 1, 0, false);
			case MINE_ORE -> new Bounty(kind.id(), "", 16 + level / 5, 0, false);
			case RAID -> new Bounty(kind.id(), "", 2, 0, false);
			case DELIVER -> {
				Target t = pick(DELIVERIES, level, random);
				yield new Bounty(kind.id(), t.id(), t.need(), 0, false);
			}
		};
	}

	private static Target pick(final List<Target> list, final int level, final RandomSource random) {
		List<Target> ok = list.stream().filter(t -> t.minLevel() <= level).toList();
		return ok.get(random.nextInt(ok.size()));
	}

	// ------------------------------------------------------------------ progress and claiming

	/** Counts {@code amount} toward every open bounty of {@code kind} (and {@code target} for targeted kinds). */
	public static void progress(final ServerPlayer player, final BountyKind kind, final String target, final int amount) {
		if (kind == BountyKind.DELIVER) {
			return;
		}
		BountyData data = get(player);
		if (data.day() < 0) {
			ensure(player);
			data = get(player);
		}
		boolean changed = false;
		for (int i = 0; i <= SPECIAL; i++) {
			Bounty b = data.get(i);
			if (b.isEmpty() || b.claimed() || b.complete() || b.type() != kind || kind.targeted() && !b.target().equals(target)) {
				continue;
			}
			Bounty next = b.withProgress(b.progress() + amount);
			data = data.with(i, next);
			changed = true;
			if (next.complete()) {
				player.sendSystemMessage(Component.translatable("message.minecraft_mode.bounty.complete", describe(next)).withStyle(ChatFormatting.GOLD));
			}
		}
		if (changed) {
			set(player, data);
		}
	}

	/** How many of a delivery's items the player carries (capped at the need). */
	public static int delivered(final Player player, final Bounty bounty) {
		Item item = item(bounty.target());
		if (item == Items.AIR) {
			return 0;
		}
		Inventory inventory = player.getInventory();
		int count = 0;
		for (int i = 0; i < inventory.getContainerSize(); i++) {
			ItemStack stack = inventory.getItem(i);
			if (stack.is(item)) {
				count += stack.getCount();
			}
		}
		return Math.min(bounty.need(), count);
	}

	public static Item item(final String id) {
		Identifier key = Identifier.tryParse(id);
		return key == null ? Items.AIR : BuiltInRegistries.ITEM.getValue(key);
	}

	/** True when bounty {@code index} can be handed in now. */
	public static boolean ready(final Player player, final Bounty bounty) {
		if (bounty.isEmpty() || bounty.claimed()) {
			return false;
		}
		return bounty.type() == BountyKind.DELIVER ? delivered(player, bounty) >= bounty.need() : bounty.complete();
	}

	public static Reward reward(final boolean special, final int level) {
		int coins = Math.max(2, Math.round(GearShop.bracketPrice(Math.max(10, level)) * (special ? 1.5F : 0.4F)));
		return special ? new Reward(coins, 40, 8, 3) : new Reward(coins, 10, 2, 1);
	}

	/** Hands in bounty {@code index} (0..2 daily, 3 the cycle bounty) and pays it. */
	public static boolean claim(final ServerPlayer player, final int index) {
		ensure(player);
		BountyData data = get(player);
		Bounty bounty = data.get(index);
		if (!ready(player, bounty)) {
			return false;
		}
		if (bounty.type() == BountyKind.DELIVER) {
			Item item = item(bounty.target());
			int left = bounty.need();
			Inventory inventory = player.getInventory();
			for (int i = 0; i < inventory.getContainerSize() && left > 0; i++) {
				ItemStack stack = inventory.getItem(i);
				if (stack.is(item)) {
					int take = Math.min(left, stack.getCount());
					stack.shrink(take);
					left -= take;
				}
			}
			bounty = bounty.withProgress(bounty.need());
		}
		int level = level(player);
		Reward reward = reward(index == SPECIAL, level);
		Wallet.add(player, reward.coins());
		give(player, EvolutionEtherItem.of(Math.max(10, level), reward.ether()));
		give(player, new ItemStack(ModItems.ENHANCEMENT_STONE, reward.stones()));
		Bounty after = bounty.carried() ? fresh(player, data, index) : bounty.asClaimed();
		set(player, data.with(index, after).withMerit(data.merit() + reward.merit()));
		player.sendSystemMessage(Component.translatable("message.minecraft_mode.bounty.claimed", describe(bounty), reward.merit()).withStyle(ChatFormatting.GREEN));
		player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.6F, 1.4F);
		Progress.bountyDone(player);
		return true;
	}

	public static void addMerit(final ServerPlayer player, final int amount) {
		if (amount > 0) {
			BountyData data = get(player);
			set(player, data.withMerit(data.merit() + amount));
		}
	}

	/** Buys merit shop offer {@code index}. */
	public static boolean buy(final ServerPlayer player, final int index) {
		if (index < 0 || index >= SHOP.size()) {
			return false;
		}
		Offer offer = SHOP.get(index);
		BountyData data = get(player);
		if (data.merit() < offer.cost()) {
			return false;
		}
		ItemStack stack = offer.item().make(player, level(player), player.getRandom());
		if (stack.isEmpty()) {
			player.sendSystemMessage(Component.translatable("message.minecraft_mode.bounty.shop_owned").withStyle(ChatFormatting.RED));
			return false;
		}
		set(player, data.withMerit(data.merit() - offer.cost()));
		give(player, stack);
		player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.VILLAGER_TRADE, SoundSource.PLAYERS, 0.6F, 1.1F);
		return true;
	}

	private static void give(final ServerPlayer player, final ItemStack stack) {
		if (!stack.isEmpty()) {
			player.getInventory().placeItemBackInInventory(stack, net.minecraft.util.Prediction.SERVER_ONLY);
		}
	}

	/** "Defeat 15 Zombie" style text for chat and screens. */
	public static Component describe(final Bounty bounty) {
		BountyKind kind = bounty.type();
		Component target = switch (kind) {
			case KILL_TYPE -> {
				Identifier id = Identifier.tryParse(bounty.target());
				yield id == null ? Component.literal(bounty.target())
					: BuiltInRegistries.ENTITY_TYPE.getOptional(id).map(t -> (Component)t.getDescription()).orElse(Component.literal(bounty.target()));
			}
			case DELIVER -> Component.translatable(item(bounty.target()).getDescriptionId());
			default -> Component.empty();
		};
		return Component.translatable(kind.key(), bounty.need(), target);
	}

	private Bounties() {
	}
}
