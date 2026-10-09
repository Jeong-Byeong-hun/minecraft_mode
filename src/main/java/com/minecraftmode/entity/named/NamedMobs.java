package com.minecraftmode.entity.named;

import static com.minecraftmode.entity.named.Ability.Type.*;

import com.minecraftmode.MinecraftMode;
import com.minecraftmode.job.skill.Fx;
import com.minecraftmode.registry.ModEffects;
import com.minecraftmode.registry.ModItems;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.levelgen.Heightmap;
import org.jspecify.annotations.Nullable;

/**
 * The 22 named monsters: rare, named versions of a habitat's monsters that drop class gear of their
 * level range and always Evolution Ether. Weaker gear droppers are more common (15%) than the
 * strongest (2%). Each gets an entity type, attributes, natural spawns and a spawn egg.
 */
public final class NamedMobs {
	private static final Map<String, NamedDef> DEFS = new LinkedHashMap<>();
	private static final Map<String, EntityType<NamedMob>> TYPES = new LinkedHashMap<>();
	private static final Map<EntityType<?>, NamedDef> BY_TYPE = new LinkedHashMap<>();
	private static final Map<String, Item> EGGS = new LinkedHashMap<>();

	public static void init() {
		define();
		for (NamedDef def : DEFS.values()) {
			ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, MinecraftMode.id(def.id()));
			EntityType.Builder<NamedMob> builder = EntityType.Builder.<NamedMob>of(NamedMob::new, MobCategory.MONSTER)
				.sized(def.width(), def.height())
				.clientTrackingRange(10);
			if (def.fireImmune()) {
				builder.fireImmune();
			}
			EntityType<NamedMob> type = Registry.register(BuiltInRegistries.ENTITY_TYPE, key, builder.build(key));
			TYPES.put(def.id(), type);
			BY_TYPE.put(type, def);
			FabricDefaultAttributeRegistry.register(type, NamedMob.createAttributes(def));
			SpawnPlacements.register(type, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, NamedMob::checkSpawnRules);
			BiomeModifications.addSpawn(def.habitat().biomes(), MobCategory.MONSTER, type, def.habitat().weight(def.rarity()), 1, 1);
			ResourceKey<Item> eggKey = ResourceKey.create(Registries.ITEM, MinecraftMode.id(def.id() + "_spawn_egg"));
			EGGS.put(def.id(), Registry.register(BuiltInRegistries.ITEM, eggKey, new SpawnEggItem(new Item.Properties().spawnEgg(type).setId(eggKey))));
		}
		MinecraftMode.LOGGER.info("Registered {} named monsters", DEFS.size());
	}

	public static Collection<NamedDef> all() {
		return Collections.unmodifiableCollection(DEFS.values());
	}

	public static @Nullable NamedDef def(final EntityType<?> type) {
		return BY_TYPE.get(type);
	}

	public static EntityType<NamedMob> type(final NamedDef def) {
		return TYPES.get(def.id());
	}

	public static Item egg(final NamedDef def) {
		return EGGS.get(def.id());
	}

	// ------------------------------------------------------------------ definitions

	private static void define() {
		named("golden_enderman", "Golden Enderman", "황금 엔더맨", 10, 20, 0.12F, 40, 5, 2, 0.30, 0.6F, 2.9F, Habitat.ANY_OVERWORLD,
			"An enderman gilded by some rich fool's wish. It blinks behind you and hurls gold shards.",
			"어느 부자의 소원으로 금박을 뒤집어쓴 엔더맨. 등 뒤로 순간이동하고 금 파편을 던진다.")
			.ability(teleport(140, 20))
			.ability(bolt(80, 0.6F, 18, 3, 12, Fx.Kind.SHARD, 0xFFD24A, Items.GOLD_NUGGET))
			.done();
		named("goblin_warchief", "Goblin Warchief", "고블린 족장", 10, 20, 0.15F, 45, 5, 4, 0.28, 0.7F, 1.6F, Habitat.GRASSLAND,
			"Leads goblin raiding parties with a cleaver and a war cry.", "식칼과 함성으로 고블린 약탈대를 이끄는 족장.")
			.ability(summon(260, EntityTypes.ZOMBIE, 2))
			.ability(roar(200, 0.5F, 5).effect(MobEffects.WEAKNESS, 100, 0))
			.ability(leap(160, 1.0F, 10, 2.5F))
			.done();
		named("bandit_captain", "Bandit Captain", "산적 두목", 10, 20, 0.14F, 42, 5, 3, 0.29, 0.6F, 1.95F, Habitat.DRYLAND,
			"A highwayman with a price on his head. He rushes in and throws knives.", "목에 현상금이 걸린 노상강도. 돌진하고 단검을 던진다.")
			.daylight()
			.ability(charge(140, 1.2F, 14))
			.ability(bolt(70, 0.7F, 16, 2, 10, Fx.Kind.SPARK, 0xC8C8C8, ModItems.PROJECTILE_KNIFE))
			.done();
		named("bog_hag", "Bog Hag", "늪지 마녀 할멈", 20, 30, 0.10F, 55, 5, 2, 0.25, 0.6F, 1.7F, Habitat.SWAMP,
			"A swamp witch who brews poison in the cauldron she calls a heart.", "가마솥 같은 심장에 독을 끓이는 늪지 마녀.")
			.ability(bolt(60, 0.6F, 18, 1, 0, Fx.Kind.SMOKE, 0x6AA84F, Items.SLIME_BALL).effect(MobEffects.POISON, 100, 1).gravity(0.03F))
			.ability(aura(160, 5, MobEffects.HUNGER, 120, 1, 0x6AA84F))
			.ability(summon(300, EntityTypes.SLIME, 2))
			.done();
		named("frost_alpha", "Frost Alpha", "서리 늑대 우두머리", 20, 30, 0.10F, 60, 6, 3, 0.34, 1.0F, 1.2F, Habitat.SNOW,
			"The white wolf that leads the blizzard packs. Its howl freezes blood.", "눈보라 무리를 이끄는 흰 늑대. 울음소리에 피가 언다.")
			.daylight()
			.onHit(MobEffects.SLOWNESS, 40)
			.ability(leap(120, 1.1F, 12, 2.5F))
			.ability(roar(220, 0.4F, 7).effect(MobEffects.SLOWNESS, 100, 1))
			.ability(breath(180, 0.25F, 8, 35, Fx.Kind.SHARD, 0xBFEFFF).effect(MobEffects.SLOWNESS, 60, 1))
			.done();
		named("dune_scorpion", "Dune Scorpion King", "사막 전갈왕", 20, 30, 0.09F, 65, 6, 6, 0.27, 1.6F, 1.0F, Habitat.DESERT,
			"A scorpion the size of a cart. Its stinger can drop a camel.", "수레만 한 전갈. 독침 한 방에 낙타도 쓰러진다.")
			.daylight()
			.onHit(MobEffects.POISON, 60)
			.ability(bolt(70, 0.6F, 16, 1, 0, Fx.Kind.SMOKE, 0x9ACD32, Items.SPIDER_EYE).effect(MobEffects.POISON, 100, 1))
			.ability(spikes(160, 1.2F, 16, 2.5F, 0xD8B070))
			.ability(charge(200, 1.0F, 12))
			.done();
		named("cave_troll", "Cave Troll", "동굴 트롤", 30, 40, 0.08F, 90, 9, 6, 0.25, 1.3F, 2.8F, Habitat.CAVES,
			"Slow, stupid and unbelievably strong. It throws rocks and its wounds close by themselves.",
			"느리고 우둔하지만 믿을 수 없을 만큼 강하다. 바위를 던지고 상처가 저절로 아문다.")
			.ability(slam(140, 1.3F, 4.5F, 0x8A7A6A))
			.ability(bolt(120, 1.0F, 20, 1, 2, Fx.Kind.SMOKE, 0x777777, Items.COBBLESTONE).gravity(0.04F))
			.ability(heal(400, 0.25F))
			.done();
		named("myconid_shaman", "Myconid Shaman", "버섯 주술사", 30, 40, 0.08F, 70, 6, 3, 0.24, 0.8F, 2.2F, Habitat.GLOOM,
			"It speaks through spores. Whoever breathes them becomes its puppet.", "포자로 말한다. 포자를 들이마신 자는 꼭두각시가 된다.")
			.ability(aura(140, 5, MobEffects.POISON, 80, 0, 0xD05050))
			.ability(bolt(70, 0.6F, 18, 3, 15, Fx.Kind.PETAL, 0xD05050, Items.RED_MUSHROOM).effect(MobEffects.BLINDNESS, 40, 0))
			.ability(summon(320, EntityTypes.ZOMBIE, 3))
			.done();
		named("jungle_stalker", "Jungle Stalker", "정글 추적자", 30, 40, 0.08F, 70, 8, 3, 0.36, 1.0F, 1.0F, Habitat.JUNGLE,
			"You never see it until it pounces.", "덮치기 전까지는 결코 보이지 않는다.")
			.daylight()
			.onHit(ModEffects.BLEEDING, 60)
			.ability(stealth(200, 60))
			.ability(leap(100, 1.3F, 14, 2.0F))
			.done();
		named("mad_pig", "Mad Pig", "미친 돼지", 40, 50, 0.07F, 110, 10, 4, 0.33, 1.4F, 1.4F, Habitat.GRASSLAND,
			"Something got into this pig. Nobody knows what, but it is furious.", "이 돼지에게 뭔가가 씌었다. 무엇인지는 아무도 모르지만, 단단히 화가 났다.")
			.daylight()
			.ability(charge(100, 1.5F, 16))
			.ability(roar(200, 0.5F, 6))
			.ability(leap(160, 1.2F, 12, 3.0F))
			.done();
		named("drowned_corsair", "Drowned Corsair", "익사한 해적 선장", 40, 50, 0.07F, 100, 9, 5, 0.28, 0.6F, 1.95F, Habitat.COAST,
			"He drowned with his treasure and still guards it.", "보물과 함께 익사해 여전히 보물을 지키는 선장.")
			.ability(bolt(60, 0.9F, 20, 1, 0, Fx.Kind.SMOKE, 0x404040, ModItems.PROJECTILE_BULLET))
			.ability(summon(260, EntityTypes.DROWNED, 2))
			.ability(charge(180, 1.1F, 12))
			.done();
		named("badlands_gunslinger", "Badlands Gunslinger", "황야의 총잡이", 40, 50, 0.07F, 90, 8, 3, 0.30, 0.6F, 1.99F, Habitat.BADLANDS,
			"A skeleton who never learned when to stop drawing.", "총 뽑는 걸 멈출 줄 모르는 해골 총잡이.")
			.daylight()
			.ability(bolt(90, 0.5F, 22, 6, 25, Fx.Kind.SMOKE, 0x505050, ModItems.PROJECTILE_BULLET))
			.ability(teleport(160, 12))
			.ability(bolt(140, 1.2F, 26, 1, 0, Fx.Kind.SPARK, 0xFFD24A, ModItems.PROJECTILE_BULLET))
			.done();
		named("wendigo", "Wendigo", "윈디고", 50, 60, 0.06F, 130, 12, 4, 0.34, 0.9F, 3.2F, Habitat.TAIGA,
			"Hunger given a body. It howls in the voices of those it ate.", "몸을 얻은 굶주림. 잡아먹은 자들의 목소리로 울부짖는다.")
			.onHit(MobEffects.HUNGER, 100)
			.ability(roar(220, 0.4F, 8).effect(MobEffects.DARKNESS, 120, 0))
			.ability(leap(120, 1.3F, 14, 3.0F))
			.ability(stealth(260, 50))
			.done();
		named("amethyst_sentinel", "Amethyst Sentinel", "자수정 파수꾼", 50, 60, 0.06F, 150, 11, 12, 0.22, 1.4F, 2.9F, Habitat.DEEP_CAVES,
			"A guardian grown inside a geode. It hums as it fights.", "정동 속에서 자라난 수호자. 싸울 때 낮게 웅웅거린다.")
			.ability(bolt(70, 0.6F, 20, 5, 30, Fx.Kind.SHARD, 0xC77DFF, Items.AMETHYST_SHARD))
			.ability(spikes(120, 1.4F, 18, 3.0F, 0xC77DFF))
			.ability(slam(180, 1.2F, 4.5F, 0xC77DFF))
			.done();
		named("ancient_treant", "Ancient Treant", "고대 트렌트", 50, 60, 0.05F, 170, 12, 8, 0.20, 1.6F, 3.6F, Habitat.OLD_FOREST,
			"Older than the first village. It remembers every axe.", "최초의 마을보다 오래된 나무. 모든 도끼를 기억한다.")
			.daylight()
			.ability(spikes(120, 1.3F, 20, 3.0F, 0x6B8E23).effect(MobEffects.SLOWNESS, 60, 3))
			.ability(heal(400, 0.3F))
			.ability(slam(160, 1.3F, 5.0F, 0x6B8E23))
			.done();
		named("magma_behemoth", "Magma Behemoth", "마그마 거수", 60, 70, 0.05F, 190, 14, 10, 0.26, 1.8F, 2.0F, Habitat.NETHER_WASTES,
			"A walking lava flow with a temper to match.", "성질까지 용암 같은, 걸어 다니는 용암류.")
			.fireImmune()
			.ability(charge(120, 1.4F, 16))
			.ability(slam(160, 1.3F, 5.0F, 0xFF6A00))
			.ability(bolt(90, 0.8F, 20, 3, 20, Fx.Kind.SPARK, 0xFF8A20, Items.MAGMA_CREAM).gravity(0.03F))
			.done();
		named("soul_reaper", "Soul Reaper", "영혼 수확자", 60, 70, 0.05F, 160, 14, 6, 0.30, 0.8F, 2.4F, Habitat.SOUL_VALLEY,
			"It reaps the souls the valley is made of.", "골짜기를 이루는 영혼들을 거둬 가는 자.")
			.flying()
			.fireImmune()
			.ability(teleport(140, 20))
			.ability(breath(160, 0.3F, 7, 50, Fx.Kind.SMOKE, 0x6FE0FF).effect(MobEffects.WITHER, 60, 0))
			.ability(summon(300, EntityTypes.SKELETON, 2))
			.ability(aura(180, 5, MobEffects.WITHER, 60, 0, 0x6FE0FF))
			.done();
		named("wither_knight", "Wither Knight", "위더 기사", 70, 80, 0.04F, 230, 16, 15, 0.27, 0.8F, 2.6F, Habitat.CRIMSON,
			"A knight who swore fealty to the Wither and never died.", "위더에게 충성을 맹세하고 결코 죽지 않은 기사.")
			.fireImmune()
			.onHit(MobEffects.WITHER, 80)
			.ability(charge(120, 1.4F, 16))
			.ability(slam(160, 1.4F, 5.0F, 0x404040))
			.ability(bolt(100, 1.0F, 20, 2, 20, Fx.Kind.SMOKE, 0x303030, Items.WITHER_SKELETON_SKULL).effect(MobEffects.WITHER, 80, 1))
			.done();
		named("void_watcher", "Void Watcher", "공허의 감시자", 70, 80, 0.04F, 200, 14, 8, 0.25, 1.4F, 1.6F, Habitat.WARPED,
			"An eye that watched the void too long. The void watched back.", "공허를 너무 오래 들여다본 눈. 공허도 그것을 들여다보았다.")
			.flying()
			.fireImmune()
			.ability(breath(140, 0.35F, 14, 12, Fx.Kind.ORB, 0xB040FF))
			.ability(bolt(80, 0.7F, 22, 3, 25, Fx.Kind.ORB, 0x9A4AFF, Items.ENDER_PEARL).seeking())
			.ability(teleport(180, 16))
			.ability(aura(200, 6, MobEffects.DARKNESS, 100, 0, 0x6A2ACF))
			.done();
		named("echo_stalker", "Echo Stalker", "메아리 추적자", 80, 90, 0.03F, 280, 18, 10, 0.32, 1.4F, 1.6F, Habitat.DEEP_DARK,
			"Born from the sculk's memory of every footstep.", "스컬크가 기억하는 모든 발소리에서 태어났다.")
			.daylight()
			.ability(breath(160, 0.5F, 16, 10, Fx.Kind.ORB, 0x3AE0E0))
			.ability(leap(110, 1.4F, 16, 3.0F))
			.ability(stealth(220, 60))
			.ability(roar(240, 0.4F, 8).effect(MobEffects.DARKNESS, 140, 0))
			.done();
		named("chorus_wraith", "Chorus Wraith", "코러스 망령", 80, 90, 0.03F, 260, 17, 8, 0.30, 0.8F, 2.6F, Habitat.END_HIGHLANDS,
			"The ghost of a traveler who ate far too much chorus fruit.", "코러스 열매를 너무 많이 먹은 여행자의 망령.")
			.flying()
			.ability(bolt(80, 0.8F, 22, 3, 20, Fx.Kind.PETAL, 0xD080FF, Items.CHORUS_FRUIT).effect(MobEffects.LEVITATION, 50, 1).seeking())
			.ability(teleport(140, 20))
			.ability(summon(300, EntityTypes.ENDERMITE, 3))
			.ability(breath(200, 0.3F, 8, 40, Fx.Kind.RUNE, 0xD080FF))
			.done();
		named("astral_knight", "Astral Knight", "성운 기사", 90, 100, 0.02F, 360, 22, 18, 0.29, 0.9F, 2.8F, Habitat.END_ISLANDS,
			"A knight forged from a fallen star, guarding the edge of the world.", "떨어진 별로 벼려진 기사. 세계의 끝을 지킨다.")
			.ability(charge(110, 1.5F, 18))
			.ability(spikes(130, 1.6F, 22, 3.5F, 0x9FD0FF))
			.ability(bolt(90, 1.0F, 24, 5, 30, Fx.Kind.SPARK, 0xFFF4C0, Items.END_ROD).seeking())
			.ability(heal(500, 0.25F))
			.done();
	}

	// ------------------------------------------------------------------ builder

	private static Builder named(final String id, final String en, final String ko, final int lo, final int hi, final float rarity, final double health, final double damage,
		final double armor, final double speed, final float width, final float height, final Habitat habitat, final String descEn, final String descKo) {
		return new Builder(id, en, ko, descEn, descKo, lo, hi, rarity, health, damage, armor, speed, width, height, habitat);
	}

	private static final class Builder {
		private final String id;
		private final String en;
		private final String ko;
		private final String descEn;
		private final String descKo;
		private final int lo;
		private final int hi;
		private final float rarity;
		private final double health;
		private final double damage;
		private final double armor;
		private final double speed;
		private final float width;
		private final float height;
		private final Habitat habitat;
		private boolean daylight;
		private boolean flying;
		private boolean fireImmune;
		private final List<Ability> abilities = new ArrayList<>();
		private @Nullable Holder<MobEffect> onHit;
		private int onHitTicks;

		Builder(final String id, final String en, final String ko, final String descEn, final String descKo, final int lo, final int hi, final float rarity,
			final double health, final double damage, final double armor, final double speed, final float width, final float height, final Habitat habitat) {
			this.id = id;
			this.en = en;
			this.ko = ko;
			this.descEn = descEn;
			this.descKo = descKo;
			this.lo = lo;
			this.hi = hi;
			this.rarity = rarity;
			this.health = health;
			this.damage = damage;
			this.armor = armor;
			this.speed = speed;
			this.width = width;
			this.height = height;
			this.habitat = habitat;
		}

		Builder daylight() {
			this.daylight = true;
			return this;
		}

		Builder flying() {
			this.flying = true;
			return this;
		}

		Builder fireImmune() {
			this.fireImmune = true;
			return this;
		}

		Builder onHit(final Holder<MobEffect> effect, final int ticks) {
			this.onHit = effect;
			this.onHitTicks = ticks;
			return this;
		}

		Builder ability(final Ability ability) {
			this.abilities.add(ability);
			return this;
		}

		void done() {
			if (this.rarity < 0.02F || this.rarity > 0.15F) {
				throw new IllegalArgumentException(this.id + ": rarity must be 2-15%");
			}
			DEFS.put(this.id, new NamedDef(this.id, this.en, this.ko, this.descEn, this.descKo, this.lo, this.hi, this.rarity, this.health, this.damage, this.armor,
				this.speed, this.width, this.height, this.habitat, this.daylight, this.flying, this.fireImmune, this.abilities, this.onHit, this.onHitTicks));
		}
	}

	private static Ability leap(final int cd, final float power, final float range, final float radius) {
		return Ability.of(LEAP, cd, power, range, radius, 0, Fx.Kind.SMOKE, 0xB0A080);
	}

	private static Ability charge(final int cd, final float power, final float range) {
		return Ability.of(CHARGE, cd, power, range, 1.6F, 0, Fx.Kind.SMOKE, 0xFFFFFF);
	}

	private static Ability bolt(final int cd, final float power, final float range, final int count, final float spread, final Fx.Kind fx, final int color, final Item display) {
		return Ability.of(BOLT, cd, power, range, spread, count, fx, color).display(display);
	}

	private static Ability summon(final int cd, final EntityType<?> type, final int count) {
		return Ability.of(SUMMON, cd, 0.0F, 18, 0.0F, count, Fx.Kind.SMOKE, 0x606060).summon(type);
	}

	private static Ability teleport(final int cd, final float range) {
		return Ability.of(TELEPORT, cd, 0.0F, range, 0.0F, 0, Fx.Kind.RUNE, 0xB040FF);
	}

	private static Ability aura(final int cd, final float radius, final Holder<MobEffect> effect, final int ticks, final int amplifier, final int color) {
		return Ability.of(AURA, cd, 0.0F, radius, radius, 0, Fx.Kind.RING, color).effect(effect, ticks, amplifier);
	}

	private static Ability roar(final int cd, final float power, final float radius) {
		return Ability.of(ROAR, cd, power, radius, radius, 0, Fx.Kind.RING, 0xFFFFFF);
	}

	private static Ability spikes(final int cd, final float power, final float range, final float radius, final int color) {
		return Ability.of(SPIKES, cd, power, range, radius, 0, Fx.Kind.SHARD, color);
	}

	private static Ability heal(final int cd, final float fraction) {
		return Ability.of(HEAL, cd, fraction, 64, 0.0F, 0, Fx.Kind.PETAL, 0x7CFC7C);
	}

	private static Ability breath(final int cd, final float power, final float range, final float angle, final Fx.Kind fx, final int color) {
		return Ability.of(BREATH, cd, power, range, angle, 0, fx, color);
	}

	private static Ability stealth(final int cd, final int ticks) {
		return Ability.of(STEALTH, cd, 1.8F, 20, 0.0F, ticks, Fx.Kind.SMOKE, 0x303030);
	}

	private static Ability slam(final int cd, final float power, final float radius, final int color) {
		return Ability.of(SLAM, cd, power, radius + 1.0F, radius, 0, Fx.Kind.SMOKE, color);
	}

	private static Ability slam(final int cd, final float power, final float radius) {
		return slam(cd, power, radius, 0x9A8A7A);
	}

	private NamedMobs() {
	}
}
