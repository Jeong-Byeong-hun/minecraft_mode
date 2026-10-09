package com.minecraftmode.consumable;

import static com.minecraftmode.consumable.ConsumableDef.Kind.CHARM;
import static com.minecraftmode.consumable.ConsumableDef.Kind.DRINK;
import static com.minecraftmode.consumable.ConsumableDef.Kind.FOOD;
import static com.minecraftmode.consumable.ConsumableDef.Shape.*;

import com.minecraftmode.MinecraftMode;
import com.minecraftmode.consumable.ConsumableDef.Buff;
import com.minecraftmode.consumable.ConsumableDef.Kind;
import com.minecraftmode.consumable.ConsumableDef.Shape;
import com.minecraftmode.consumable.ConsumableDef.Shop;
import com.minecraftmode.consumable.ConsumableDef.Special;
import com.minecraftmode.job.JobData;
import com.minecraftmode.job.JobProgression;
import com.minecraftmode.job.JobStats;
import com.minecraftmode.registry.ModEffects;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.UseCooldown;
import org.jspecify.annotations.Nullable;

/**
 * The 40 consumables: foods, potions, pills and charms in five tiers. Tier 1-2 are sold in the market
 * (and some are crafted), tier 3 at the alchemist for gold and dropped by named monsters, tier 4 are
 * rare drops and raid rewards (a few sold at steep prices), tier 5 only come from raid bosses. Lasting
 * effects run 10 minutes; instant heals and MP potions share cooldown groups so they cannot be
 * chained in a boss fight. Many borrow from games and anime (Dragon Ball's Senzu Bean, Final
 * Fantasy's Elixir and Phoenix Down, Dark Souls' Estus, The Witcher's Swallow, Naruto's Soldier
 * Pill, Mario's Super Mushroom and Star, Pokemon's Full Restore and Rare Candy, One Piece's meat).
 */
public final class Consumables {
	/** Ten minutes. */
	public static final int LONG = 20 * 60 * 10;
	/** Phoenix Feather: one revival per five minutes. */
	public static final int REVIVE_COOLDOWN = 20 * 60 * 5;
	private static final Map<String, ConsumableDef> DEFS = new LinkedHashMap<>();
	private static final Map<String, Item> ITEMS = new LinkedHashMap<>();

	public static void init() {
		define();
		for (ConsumableDef def : DEFS.values()) {
			ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, MinecraftMode.id(def.id()));
			ITEMS.put(def.id(), Registry.register(BuiltInRegistries.ITEM, key, new ConsumableItem(properties(def).setId(key), def)));
		}
		MinecraftMode.LOGGER.info("Registered {} consumables", DEFS.size());
	}

	/**
	 * The Phoenix Feather revival. Registered after Avalon (free) and before the raid handler, so a
	 * feather also saves the player from a raid mechanic.
	 */
	public static void initEvents() {
		ServerLivingEntityEvents.ALLOW_DEATH.register((entity, source, amount) -> !(entity instanceof ServerPlayer player) || !revive(player));
	}

	private static Item.Properties properties(final ConsumableDef def) {
		Item.Properties p = new Item.Properties().stacksTo(def.special() == Special.REVIVE ? 4 : 16).rarity(rarity(def.tier()));
		if (def.kind() == FOOD) {
			FoodProperties food = new FoodProperties.Builder().nutrition(def.nutrition()).saturationModifier(def.saturation()).alwaysEdible().build();
			p.food(food, net.minecraft.world.item.component.Consumables.defaultFood().consumeSeconds(def.tier() >= 4 ? 0.8F : 1.2F).build());
		} else if (def.kind() == DRINK) {
			p.component(DataComponents.CONSUMABLE, net.minecraft.world.item.component.Consumables.defaultDrink().consumeSeconds(def.tier() >= 3 ? 0.8F : 1.0F).build());
		}
		if (def.cooldown() > 0 && def.kind() != CHARM) {
			p.component(DataComponents.USE_COOLDOWN, new UseCooldown(def.cooldown(), Optional.of(MinecraftMode.id("consumable/" + def.group()))));
		}
		return p;
	}

	private static Rarity rarity(final int tier) {
		return switch (tier) {
			case 1, 2 -> Rarity.COMMON;
			case 3 -> Rarity.UNCOMMON;
			case 4 -> Rarity.RARE;
			default -> Rarity.EPIC;
		};
	}

	public static Collection<ConsumableDef> all() {
		return Collections.unmodifiableCollection(DEFS.values());
	}

	public static @Nullable ConsumableDef def(final String id) {
		return DEFS.get(id);
	}

	public static Item item(final ConsumableDef def) {
		return ITEMS.get(def.id());
	}

	public static Item item(final String id) {
		return ITEMS.get(id);
	}

	public static List<ConsumableDef> tier(final int tier) {
		return DEFS.values().stream().filter(d -> d.tier() == tier).toList();
	}

	// ------------------------------------------------------------------ using

	/** What eating or drinking {@code def} does. */
	public static void apply(final ServerPlayer player, final ConsumableDef def) {
		ServerLevel level = player.level();
		if (def.heal() > 0.0F) {
			player.heal(player.getMaxHealth() * def.heal());
			level.sendParticles(ParticleTypes.HEART, player.getX(), player.getY(1.0), player.getZ(), 4 + Math.round(def.heal() * 8), 0.4, 0.3, 0.4, 0.0);
		}
		if (def.mana() > 0.0F) {
			JobData data = JobProgression.get(player);
			int max = JobStats.maxMana(player);
			JobProgression.set(player, data.withMana(Math.min(max, data.mana() + Math.round(max * def.mana()))));
			level.sendParticles(ParticleTypes.GLOW, player.getX(), player.getY(0.8), player.getZ(), 10, 0.4, 0.4, 0.4, 0.0);
		}
		if (def.cleanse()) {
			List<Holder<MobEffect>> bad = new ArrayList<>();
			for (MobEffectInstance instance : player.getActiveEffects()) {
				if (instance.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) {
					bad.add(instance.getEffect());
				}
			}
			bad.forEach(player::removeEffect);
		}
		for (Buff buff : def.effects()) {
			player.addEffect(new MobEffectInstance(buff.effect(), buff.ticks(), buff.amplifier(), false, true, true));
		}
		switch (def.special()) {
			case EXP -> {
				JobData data = JobProgression.get(player);
				JobProgression.addExp(player, Math.max(20, JobProgression.expToNext(data.level()) / 2));
			}
			case STAR -> {
				player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 160, 4, false, true, true));
				player.addEffect(new MobEffectInstance(MobEffects.SPEED, 160, 1, false, true, true));
				level.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY(1.0), player.getZ(), 40, 0.5, 0.8, 0.5, 0.1);
			}
			case FEAST -> {
				player.getFoodData().setFoodLevel(20);
				player.getFoodData().setSaturation(20.0F);
			}
			default -> {
			}
		}
		if (!def.effects().isEmpty()) {
			level.sendParticles(ParticleTypes.HAPPY_VILLAGER, player.getX(), player.getY(1.0), player.getZ(), 8, 0.4, 0.5, 0.4, 0.0);
		}
		if (def.tier() >= 4) {
			level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.6F, 1.6F);
		}
	}

	/** A Phoenix Feather in the inventory (not on cooldown) turns a death into a revival at half health. */
	private static boolean revive(final ServerPlayer player) {
		Item feather = ITEMS.get("phoenix_feather");
		var inventory = player.getInventory();
		for (int i = 0; i < inventory.getContainerSize(); i++) {
			ItemStack stack = inventory.getItem(i);
			if (stack.is(feather) && !player.getCooldowns().isOnCooldown(stack)) {
				player.getCooldowns().addCooldown(stack, REVIVE_COOLDOWN);
				stack.shrink(1);
				player.setHealth(player.getMaxHealth() * 0.5F);
				List<Holder<MobEffect>> bad = new ArrayList<>();
				for (MobEffectInstance instance : player.getActiveEffects()) {
					if (instance.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) {
						bad.add(instance.getEffect());
					}
				}
				bad.forEach(player::removeEffect);
				player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 60, 3));
				player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 200, 0));
				player.clearFire();
				ServerLevel level = player.level();
				level.sendParticles(ParticleTypes.FLAME, player.getX(), player.getY(1.0), player.getZ(), 60, 0.6, 1.0, 0.6, 0.15);
				level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, player.getX(), player.getY(1.0), player.getZ(), 40, 0.5, 0.8, 0.5, 0.3);
				level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 0.8F, 1.4F);
				player.sendSystemMessage(Component.translatable("message.minecraft_mode.consumable.revived").withStyle(ChatFormatting.GOLD));
				return true;
			}
		}
		return false;
	}

	// ------------------------------------------------------------------ sources

	/** A named monster's consumable drop (25% chance): better tiers at higher levels, rarely tier 4. */
	public static ItemStack namedDrop(final int level, final RandomSource random) {
		if (random.nextFloat() >= 0.25F) {
			return ItemStack.EMPTY;
		}
		return forLevel(level, random);
	}

	/** One consumable for a source of {@code level}: tiers 1-2 early, 2-3 mid, mostly 3 late, rarely tier 4 from Lv 50. */
	public static ItemStack forLevel(final int level, final RandomSource random) {
		int tier;
		if (level >= 50 && random.nextFloat() < 0.04F) {
			tier = 4;
		} else if (level < 30) {
			tier = random.nextFloat() < 0.7F ? 1 : 2;
		} else if (level < 60) {
			tier = random.nextFloat() < 0.6F ? 2 : 3;
		} else {
			tier = random.nextFloat() < 0.85F ? 3 : 2;
		}
		List<ConsumableDef> pool = tier(tier);
		return pool.isEmpty() ? ItemStack.EMPTY : new ItemStack(item(pool.get(random.nextInt(pool.size()))));
	}

	/**
	 * A raid participant's consumables: one tier 3 always, a tier 4 more often against harder bosses,
	 * and for the last two bosses a chance at tier 5 (the Dragon Heart Steak only from Aethryx).
	 */
	public static List<ItemStack> raidRewards(final int bossIndex, final boolean finalBoss, final RandomSource random) {
		List<ItemStack> out = new ArrayList<>();
		List<ConsumableDef> t3 = tier(3);
		out.add(new ItemStack(item(t3.get(random.nextInt(t3.size()))), 1 + random.nextInt(2)));
		if (random.nextFloat() < 0.10F + 0.08F * bossIndex) {
			List<ConsumableDef> t4 = tier(4);
			out.add(new ItemStack(item(t4.get(random.nextInt(t4.size())))));
		}
		if (bossIndex >= 4 && random.nextFloat() < (finalBoss ? 0.12F : 0.06F)) {
			List<ConsumableDef> t5 = new ArrayList<>(tier(5));
			if (!finalBoss) {
				t5.removeIf(d -> d.id().equals("dragon_heart_steak"));
			}
			out.add(new ItemStack(item(t5.get(random.nextInt(t5.size())))));
		}
		return out;
	}

	/** Shop items of {@code shop}: tier then price order. */
	public static List<ConsumableDef> soldAt(final Shop shop) {
		return DEFS.values().stream().filter(d -> d.shop() == shop && d.price() > 0).toList();
	}

	// ------------------------------------------------------------------ definitions

	private static void define() {
		// ---------------------------------------------------------------- tier 1: market, copper
		c("healing_draught", "Healing Draught", "체력 물약", 1, DRINK, BOTTLE, 0xD8303A, 0xF2D6A0)
			.flavor("The red potion every adventurer carries.", "모든 모험가가 챙기는 빨간 물약.")
			.heal(0.25F).cooldown(15, "heal").sold(Shop.GENERAL, 6);
		c("mana_draught", "Mana Draught", "마나 물약", 1, DRINK, BOTTLE, 0x3A6AD8, 0xF2D6A0)
			.flavor("The blue potion every caster carries.", "모든 마법사가 챙기는 파란 물약.")
			.mana(0.25F).cooldown(15, "mana").sold(Shop.GENERAL, 6);
		c("antidote", "Antidote", "해독제", 1, DRINK, VIAL, 0x5ED86A, 0xE8E8E8)
			.flavor("Bitter, but it burns the poison right out.", "쓰지만 독을 단번에 몰아낸다.")
			.cleanse().cooldown(10, "cure").sold(Shop.GENERAL, 8);
		c("energy_drink", "Energy Drink", "에너지 드링크", 1, DRINK, CAN, 0x2A2A2A, 0x6AE84A)
			.flavor("Wings not included.", "날개는 별매.")
			.buff(MobEffects.HASTE, 0).buff(MobEffects.SPEED, 0).sold(Shop.GENERAL, 9);
		c("gimbap", "Gimbap", "김밥", 1, FOOD, ROLL, 0x1F3A1F, 0xF0A030).food(6, 0.6F)
			.flavor("Rice, seaweed and a bit of everything. The picnic classic.", "밥, 김, 그리고 이것저것. 소풍의 정석.")
			.buff(ModEffects.FURY, 0).sold(Shop.GROCER, 4);
		c("tteokbokki", "Tteokbokki", "떡볶이", 1, FOOD, BOWL, 0xE04020, 0xF4E6D0).food(6, 0.5F)
			.flavor("Spicy rice cakes. You will run faster to find water.", "매콤한 떡. 물을 찾아 더 빨리 뛰게 된다.")
			.buff(MobEffects.SPEED, 0).sold(Shop.GROCER, 4);
		c("roasted_sweet_potato", "Roasted Sweet Potato", "군고구마", 1, FOOD, SWEET_POTATO, 0x8A3A5A, 0xF0B040).food(7, 0.8F)
			.flavor("Warm in the hands, warm in the belly.", "손도 따뜻, 속도 따뜻.")
			.buff(ModEffects.REJUVENATION, 0).sold(Shop.GROCER, 5);
		c("bungeoppang", "Bungeoppang", "붕어빵", 1, FOOD, FISH_BREAD, 0xC88A3A, 0x6A2A2A).food(5, 0.5F)
			.flavor("A fish-shaped pastry full of sweet beans.", "단팥이 가득 든 붕어 모양 빵.")
			.buff(MobEffects.ABSORPTION, 0).sold(Shop.GROCER, 5);

		// ---------------------------------------------------------------- tier 2: market, silver
		c("greater_healing_draught", "Greater Healing Draught", "고급 체력 물약", 2, DRINK, BOTTLE, 0x9A1020, 0xFFE070)
			.flavor("Twice the herbs, twice the price.", "약초도 두 배, 값도 두 배.")
			.heal(0.5F).cooldown(25, "heal").sold(Shop.ALCHEMIST, 27);
		c("greater_mana_draught", "Greater Mana Draught", "고급 마나 물약", 2, DRINK, BOTTLE, 0x1A3A9A, 0xFFE070)
			.flavor("Distilled from moonlit spring water.", "달빛 샘물을 증류했다.")
			.mana(0.5F).cooldown(25, "mana").sold(Shop.ALCHEMIST, 27);
		c("samgyetang", "Samgyetang", "삼계탕", 2, FOOD, BOWL, 0xF4EAD0, 0xC89A50).food(10, 0.9F)
			.flavor("Ginseng chicken soup. Sweat it out, gain your strength.", "인삼 닭백숙. 땀 흘리고 기운을 차린다.")
			.buff(MobEffects.HEALTH_BOOST, 0).sold(Shop.GROCER, 18);
		c("meat_on_the_bone", "Meat on the Bone", "뼈 달린 고기", 2, FOOD, MEAT, 0xA0402A, 0xF4ECD8).food(12, 1.0F)
			.flavor("The meat pirates dream of. Bite and you can fight all day.", "해적들이 꿈꾸는 고기. 한 입이면 하루 종일 싸운다.")
			.buff(ModEffects.FURY, 0).buff(MobEffects.HEALTH_BOOST, 0).sold(Shop.GROCER, 27);
		c("onigiri", "Onigiri", "주먹밥", 2, FOOD, RICE_BALL, 0xF4F4F0, 0x1F3A1F).food(6, 0.8F)
			.flavor("Packed for long missions. Quietly restores your spirit.", "긴 임무를 위해 싸 둔 주먹밥. 조용히 기운이 돌아온다.")
			.mana(0.2F).cooldown(10, "snack").sold(Shop.GROCER, 9);
		c("red_ginseng_extract", "Red Ginseng Extract", "홍삼정", 2, DRINK, VIAL, 0x7A2A1A, 0xD8A040)
			.flavor("Six years in the ground, one spoon at a time.", "6년근을 한 숟가락씩.")
			.buff(ModEffects.CLARITY, 0).sold(Shop.ALCHEMIST, 18);
		c("ironskin_tonic", "Ironskin Tonic", "철갑 비약", 2, DRINK, BOTTLE, 0x8A8F99, 0x4A4A4A)
			.flavor("Tastes like a nail. Works like armor.", "못 맛이 나지만 갑옷처럼 효과가 있다.")
			.buff(ModEffects.IRONSKIN, 0).sold(Shop.ALCHEMIST, 27);
		c("green_tea", "Green Tea", "녹차", 2, DRINK, CUP, 0x6AA84F, 0xF0F0E0)
			.flavor("A calm mind wastes no motion.", "고요한 마음은 헛손질이 없다.")
			.buff(ModEffects.FOCUS, 0).sold(Shop.ALCHEMIST, 18);
		c("fortune_cookie", "Fortune Cookie", "포춘 쿠키", 2, FOOD, COOKIE, 0xE8C060, 0xFFFFFF).food(2, 0.3F)
			.flavor("\"Great fortune awaits the one who keeps hunting.\"", "\"계속 사냥하는 자에게 큰 행운이.\"")
			.buff(ModEffects.FORTUNE, 0).sold(Shop.GROCER, 18);
		c("scholars_coffee", "Scholar's Coffee", "학자의 커피", 2, DRINK, CUP, 0x5A3A20, 0xF0F0E0)
			.flavor("Keeps you up, keeps you learning.", "잠을 쫓고 배움을 늘린다.")
			.buff(ModEffects.WISDOM, 0).sold(Shop.ALCHEMIST, 27);

		// ---------------------------------------------------------------- tier 3: alchemist gold, named drops
		c("soldier_pill", "Soldier Pill", "병량환", 3, FOOD, PILL, 0x8A6A3A, 0x3A2A1A).food(2, 1.0F)
			.flavor("A ninja's ration: chakra and fighting spirit in one bite.", "닌자의 비상식량. 한 알에 차크라와 투지가.")
			.mana(0.5F).buff(ModEffects.FURY, 1).cooldown(30, "mana").sold(Shop.ALCHEMIST, 120);
		c("estus_flask", "Estus Flask", "에스투스 병", 3, DRINK, FLASK, 0xF08A20, 0x6A8A9A)
			.flavor("Warm as the bonfire it was filled at.", "채운 화톳불만큼 따뜻하다.")
			.heal(0.6F).cooldown(40, "heal").sold(Shop.ALCHEMIST, 81);
		c("swallow", "Swallow", "제비 물약", 3, DRINK, VIAL, 0xC83A2A, 0xE8C060)
			.flavor("A witcher's potion. Wounds close as you fight.", "위쳐의 물약. 싸우는 동안 상처가 아문다.")
			.buff(ModEffects.REJUVENATION, 1).sold(Shop.ALCHEMIST, 81);
		c("thunderbolt", "Thunderbolt", "천둥 물약", 3, DRINK, VIAL, 0xE8D040, 0x4A4AA0)
			.flavor("Another witcher's brew. Every blow lands like thunder.", "또 하나의 위쳐 물약. 모든 일격이 천둥처럼 꽂힌다.")
			.buff(ModEffects.FURY, 1).buff(ModEffects.PRECISION, 0).sold(Shop.ALCHEMIST, 120);
		c("giants_draught", "Giant's Draught", "거인의 비약", 3, DRINK, FLASK, 0x8A6A4A, 0xC8C8C8)
			.flavor("For a while you are as sturdy as a hill giant.", "잠시 언덕 거인만큼 단단해진다.")
			.buff(MobEffects.HEALTH_BOOST, 1).buff(ModEffects.IRONSKIN, 0).sold(Shop.ALCHEMIST, 120);
		c("hawkeye_draught", "Hawkeye Draught", "매의 눈 비약", 3, DRINK, VIAL, 0x3FC9B0, 0xF0F0F0)
			.flavor("Every weak spot glows.", "모든 급소가 빛나 보인다.")
			.buff(ModEffects.PRECISION, 1).sold(Shop.ALCHEMIST, 100);
		c("arcane_draught", "Arcane Draught", "비전 비약", 3, DRINK, BOTTLE, 0x9B59D0, 0x3AE8E8)
			.flavor("Spells come out bigger than you meant.", "주문이 의도보다 크게 나간다.")
			.buff(ModEffects.ARCANA, 1).sold(Shop.ALCHEMIST, 100);
		c("heros_bento", "Hero's Bento", "용사의 도시락", 3, FOOD, BOX, 0xC83A2A, 0xF0C040).food(14, 1.2F)
			.flavor("Packed with love for the one who saves the world.", "세상을 구할 사람을 위해 정성껏 싼 도시락.")
			.buff(ModEffects.FURY, 0).buff(ModEffects.IRONSKIN, 0).buff(ModEffects.CLARITY, 0).sold(Shop.ALCHEMIST, 100);
		c("super_mushroom", "Super Mushroom", "슈퍼 버섯", 3, FOOD, MUSHROOM, 0xE03030, 0xF4DDB0).food(4, 0.5F)
			.flavor("You feel bigger already.", "벌써 몸이 커진 기분이다.")
			.heal(0.3F).buff(MobEffects.HEALTH_BOOST, 1).cooldown(30, "heal");
		c("hi_ether", "Hi-Ether", "하이 에테르", 3, DRINK, FLASK, 0x4A8CFF, 0xE8F0FF)
			.flavor("Fully restores a caster's power.", "마법사의 힘을 가득 채운다.")
			.mana(1.0F).cooldown(40, "mana").sold(Shop.ALCHEMIST, 162);
		c("hunters_jerky", "Hunter's Jerky", "사냥꾼의 육포", 3, FOOD, JERKY, 0x7A3A20, 0xC88A50).food(8, 0.8F)
			.flavor("Chewed on the long stalk of big game.", "큰 사냥감을 오래 쫓으며 씹는 육포.")
			.buff(ModEffects.SLAYER, 0).buff(MobEffects.SPEED, 0).sold(Shop.ALCHEMIST, 100);

		// ---------------------------------------------------------------- tier 4: rare drops and raids (a few sold at steep prices)
		c("elixir", "Elixir", "엘릭서", 4, DRINK, FLASK, 0xFFD24A, 0xFF8A20)
			.flavor("Too precious to use. Use it anyway.", "아까워서 못 쓰는 그 물약. 그래도 써라.")
			.heal(1.0F).mana(1.0F).cooldown(60, "heal").sold(Shop.ALCHEMIST, 1620);
		c("senzu_bean", "Senzu Bean", "선두", 4, FOOD, BEAN, 0x6AC83A, 0x3A7A2A).food(1, 2.0F)
			.flavor("One bean and you are back on your feet, ready for ten days.", "한 알이면 다시 일어나고, 열흘을 버틴다.")
			.heal(0.8F).mana(0.5F).special(Special.FEAST).cooldown(60, "heal");
		c("super_star", "Super Star", "슈퍼 스타", 4, FOOD, STAR, 0xFFE040, 0xFFFFFF).food(1, 0.5F)
			.flavor("Nothing can touch you. For eight seconds.", "아무것도 너를 건드릴 수 없다. 8초 동안은.")
			.special(Special.STAR).cooldown(120, "star");
		c("full_restore", "Full Restore", "풀회복약", 4, DRINK, BOTTLE, 0xC83AC8, 0xFFD24A)
			.flavor("Every wound, every ailment, gone.", "모든 상처와 모든 상태 이상이 사라진다.")
			.heal(1.0F).mana(0.5F).cleanse().cooldown(60, "heal");
		c("phoenix_feather", "Phoenix Feather", "피닉스의 깃털", 4, CHARM, FEATHER, 0xF05A20, 0xFFD24A)
			.flavor("Carry it. When you fall, it burns and you rise again.", "지니고 다녀라. 쓰러지면 깃털이 타오르고 다시 일어난다.")
			.special(Special.REVIVE).cooldown(REVIVE_COOLDOWN / 20, "revive");
		c("berserkers_draught", "Berserker's Draught", "광전사의 비약", 4, DRINK, FLASK, 0x8A1010, 0x2A0A0A)
			.flavor("Red sight, and the boss looks small.", "눈앞이 붉어지고 보스가 작아 보인다.")
			.buff(ModEffects.FURY, 2).buff(ModEffects.SLAYER, 1).sold(Shop.ALCHEMIST, 1215);
		c("philosophers_shard", "Philosopher's Stone Shard", "현자의 돌 조각", 4, FOOD, STONE, 0xC81030, 0xFF6A8A).food(1, 0.5F)
			.flavor("A sliver of the impossible. Magic flows without end.", "불가능의 한 조각. 마력이 끝없이 흐른다.")
			.buff(ModEffects.ARCANA, 2).buff(ModEffects.FOCUS, 1).buff(ModEffects.CLARITY, 1);
		c("water_of_life", "Water of Life", "생명의 물", 4, DRINK, BOTTLE, 0x8AE8F0, 0xFFFFFF)
			.flavor("From the spring at the world's root.", "세계의 뿌리에서 솟는 샘물.")
			.heal(1.0F).buff(MobEffects.HEALTH_BOOST, 2).cooldown(60, "heal").sold(Shop.ALCHEMIST, 1215);

		// ---------------------------------------------------------------- tier 5: raid bosses only
		c("rare_candy", "Rare Candy", "이상한사탕", 5, FOOD, CANDY, 0x3A8AE8, 0xFFFFFF).food(1, 0.2F)
			.flavor("Nobody knows what is in it. You grow stronger anyway.", "무엇으로 만들었는지 아무도 모른다. 그래도 강해진다.")
			.special(Special.EXP);
		c("ambrosia", "Ambrosia", "암브로시아", 5, DRINK, CUP, 0xFFD24A, 0xFFFFFF)
			.flavor("The food of the gods. Every strength at once.", "신들의 음식. 모든 힘이 한꺼번에.")
			.buff(ModEffects.FURY, 1).buff(ModEffects.IRONSKIN, 1).buff(ModEffects.PRECISION, 1).buff(ModEffects.ARCANA, 1)
			.buff(ModEffects.FOCUS, 1).buff(ModEffects.CLARITY, 1).buff(ModEffects.REJUVENATION, 1);
		c("dragon_heart_steak", "Dragon Heart Steak", "용의 심장 스테이크", 5, FOOD, HEART, 0xB01020, 0x5A2A8A).food(20, 1.5F)
			.flavor("Cut from the heart of the void dragon. It still beats.", "공허룡의 심장에서 잘라낸 고기. 아직도 뛰고 있다.")
			.buff(ModEffects.FURY, 2).buff(ModEffects.IRONSKIN, 2).buff(MobEffects.HEALTH_BOOST, 2).buff(ModEffects.REJUVENATION, 1);
	}

	private static Builder c(final String id, final String en, final String ko, final int tier, final Kind kind, final Shape shape, final int color, final int accent) {
		return new Builder(id, en, ko, tier, kind, shape, color, accent).commit();
	}

	private static final class Builder {
		private final String id;
		private final String en;
		private final String ko;
		private final int tier;
		private final Kind kind;
		private final Shape shape;
		private final int color;
		private final int accent;
		private String flavorEn = "";
		private String flavorKo = "";
		private int nutrition;
		private float saturation;
		private float heal;
		private float mana;
		private boolean cleanse;
		private Special special = Special.NONE;
		private final List<Buff> effects = new ArrayList<>();
		private int cooldown;
		private String group = "";
		private Shop shop = Shop.NONE;
		private int price;

		Builder(final String id, final String en, final String ko, final int tier, final Kind kind, final Shape shape, final int color, final int accent) {
			this.id = id;
			this.en = en;
			this.ko = ko;
			this.tier = tier;
			this.kind = kind;
			this.shape = shape;
			this.color = color;
			this.accent = accent;
		}

		Builder flavor(final String en, final String ko) {
			this.flavorEn = en;
			this.flavorKo = ko;
			return this.commit();
		}

		Builder food(final int nutrition, final float saturation) {
			this.nutrition = nutrition;
			this.saturation = saturation;
			return this.commit();
		}

		Builder heal(final float fraction) {
			this.heal = fraction;
			return this.commit();
		}

		Builder mana(final float fraction) {
			this.mana = fraction;
			return this.commit();
		}

		Builder cleanse() {
			this.cleanse = true;
			return this.commit();
		}

		Builder special(final Special special) {
			this.special = special;
			return this.commit();
		}

		Builder buff(final Holder<MobEffect> effect, final int amplifier) {
			this.effects.add(new Buff(effect, amplifier, LONG));
			return this.commit();
		}

		Builder cooldown(final int seconds, final String group) {
			this.cooldown = seconds;
			this.group = group;
			return this.commit();
		}

		Builder sold(final Shop shop, final int price) {
			this.shop = shop;
			this.price = price;
			return this.commit();
		}

		/** Every call rewrites the definition, so the last state wins. */
		private Builder commit() {
			DEFS.put(this.id, new ConsumableDef(this.id, this.en, this.ko, this.flavorEn, this.flavorKo, this.tier, this.kind, this.shape, this.color, this.accent,
				this.nutrition, this.saturation, this.heal, this.mana, this.cleanse, this.special, this.effects, this.cooldown, this.group, this.shop, this.price));
			return this;
		}
	}

	private Consumables() {
	}
}
