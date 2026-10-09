package com.minecraftmode.raid;

import com.minecraftmode.MinecraftMode;
import com.minecraftmode.entity.boss.Aethryx;
import com.minecraftmode.entity.boss.Arachne;
import com.minecraftmode.entity.boss.Gorvath;
import com.minecraftmode.entity.boss.Ignis;
import com.minecraftmode.entity.boss.Kraken;
import com.minecraftmode.entity.boss.Malachar;
import com.minecraftmode.entity.boss.RaidBoss;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.BossEvent.BossBarColor;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import org.jspecify.annotations.Nullable;

/**
 * The six raid bosses, weakest first. Each is fought in its own themed arena of the raid dimension
 * by a party; its level range is both the gear it drops and the level needed to enter.
 */
public final class RaidBosses {
	private static final List<BossDef> DEFS = new ArrayList<>();
	private static final Map<String, EntityType<? extends RaidBoss>> TYPES = new LinkedHashMap<>();
	private static final Map<EntityType<?>, BossDef> BY_TYPE = new LinkedHashMap<>();
	/** Registers a definition's entity type (run in {@link #init}). */
	private static final Map<String, Runnable> REGISTRARS = new LinkedHashMap<>();

	public static final BossDef ARACHNE = add(new BossDef("arachne", "Arachne", "아라크네", "Queen of the Web Caves", "거미줄 동굴의 여왕",
		"The spider queen spins her caves full of webs and venom. She pounces, spits poison, calls her brood and finally reels in all who remain.",
		"독과 거미줄로 동굴을 채운 거미 여왕. 덮치고 독을 뱉고 새끼들을 부르다가, 마지막에는 남은 모두를 끌어당긴다.",
		20, 30, 2400, 9, 6, 0.30, 3.6F, 3.8F, ArenaTheme.WEB_CAVE, BossBarColor.GREEN, List.of(0.7F, 0.4F)), Arachne::new);
	public static final BossDef GORVATH = add(new BossDef("gorvath", "Gorvath", "고르바스", "Colossus of the Peaks", "산맥의 거신",
		"A mountain that learned to walk. Every step shakes the summit; its fists split the ground and its roar brings down the sky.",
		"걷는 법을 배운 산. 걸음마다 정상이 흔들리고, 주먹은 땅을 가르며, 포효하면 하늘에서 바위가 쏟아진다.",
		35, 45, 3600, 13, 14, 0.22, 3.4F, 8.0F, ArenaTheme.MOUNTAIN, BossBarColor.WHITE, List.of(0.7F, 0.35F)), Gorvath::new);
	public static final BossDef KRAKEN = add(new BossDef("kraken", "Abyssal Kraken", "심연의 크라켄", "Terror of the Drowned Fleet", "가라앉은 함대의 공포",
		"It dragged a whole fleet to the bottom and kept the flagship as its lair. Its tentacles reach every plank of the deck.",
		"함대 하나를 통째로 바닥에 끌고 내려가 기함을 둥지로 삼았다. 촉수는 갑판 구석구석까지 닿는다.",
		50, 60, 4800, 16, 10, 0.0, 4.0F, 5.0F, ArenaTheme.SUNKEN_SHIP, BossBarColor.BLUE, List.of(0.7F, 0.35F)), Kraken::new);
	public static final BossDef IGNIS = add(new BossDef("ignis", "Ignis", "이그니스", "The Undying Phoenix", "불사조",
		"A phoenix nesting in a living volcano. Strike it down and it is reborn in a blaze that levels the crater.",
		"살아 있는 화산에 둥지를 튼 불사조. 쓰러뜨리면 분화구를 휩쓰는 불꽃 속에서 다시 태어난다.",
		65, 75, 6000, 19, 10, 0.30, 2.6F, 2.4F, ArenaTheme.VOLCANO, BossBarColor.RED, List.of(0.7F, 0.3F)), Ignis::new);
	public static final BossDef MALACHAR = add(new BossDef("malachar", "Malachar", "말라카르", "The Lich King", "리치 왕",
		"The king who refused to die rules a sanctum of bones. He curses, raises the dead and fills the hall with soul fire.",
		"죽기를 거부한 왕이 뼈의 성소를 다스린다. 저주를 걸고 망자를 일으키며 홀을 영혼의 불로 채운다.",
		80, 90, 7200, 22, 12, 0.24, 1.6F, 4.6F, ArenaTheme.NECROPOLIS, BossBarColor.PURPLE, List.of(0.7F, 0.35F)), Malachar::new);
	public static final BossDef AETHRYX = add(new BossDef("aethryx", "Aethryx", "에테릭스", "Dragon of the Void", "공허룡",
		"The dragon at the end of the world, coiled around the last islands of the void. Its wings blot out the stars - until it calls them down.",
		"세계의 끝, 공허의 마지막 섬들을 휘감은 용. 날개로 별을 가리다가, 이윽고 별을 떨어뜨린다.",
		90, 100, 9000, 26, 16, 0.32, 4.0F, 3.0F, ArenaTheme.VOID_ISLES, BossBarColor.PURPLE, List.of(0.75F, 0.5F, 0.25F)), Aethryx::new);


	private static <T extends RaidBoss> BossDef add(final BossDef def, final EntityType.EntityFactory<T> factory) {
		DEFS.add(def);
		REGISTRARS.put(def.id(), () -> register(def, factory));
		return def;
	}

	public static void init() {
		for (BossDef def : DEFS) {
			REGISTRARS.get(def.id()).run();
		}
		MinecraftMode.LOGGER.info("Registered {} raid bosses", DEFS.size());
	}

	private static <T extends RaidBoss> void register(final BossDef def, final EntityType.EntityFactory<T> factory) {
		ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, MinecraftMode.id(def.id()));
		EntityType<T> type = Registry.register(BuiltInRegistries.ENTITY_TYPE, key, EntityType.Builder.of(factory, MobCategory.MONSTER)
			.sized(def.width(), def.height())
			.fireImmune()
			.clientTrackingRange(16)
			.updateInterval(2)
			.build(key));
		TYPES.put(def.id(), type);
		BY_TYPE.put(type, def);
		FabricDefaultAttributeRegistry.register(type, RaidBoss.createAttributes(def));
	}

	public static List<BossDef> all() {
		return Collections.unmodifiableList(DEFS);
	}

	public static @Nullable BossDef def(final EntityType<?> type) {
		return BY_TYPE.get(type);
	}

	public static @Nullable BossDef byId(final String id) {
		for (BossDef def : DEFS) {
			if (def.id().equals(id)) {
				return def;
			}
		}
		return null;
	}

	public static EntityType<? extends RaidBoss> type(final BossDef def) {
		return TYPES.get(def.id());
	}

	public static int index(final BossDef def) {
		return DEFS.indexOf(def);
	}

	private RaidBosses() {
	}
}
