# minecraft_mode

Fabric mod for Minecraft Java **26.3**. Mod id `minecraft_mode`, package `com.minecraftmode`. See README.md for content.

## Toolchain

- JDK 25 required (`C:\Program Files\Eclipse Adoptium\jdk-25.0.4.101-hotspot`, set as machine JAVA_HOME).
- Loom plugin `net.fabricmc.fabric-loom` (non-remapping). 26.x is unobfuscated: **Mojang names, no mappings, `implementation` not `modImplementation`, `jar` not `remapJar`.**
- Fabric API uses Mojang-style names since 26.1 (`CreativeModeTabEvents`, `FabricBlockLootSubProvider`, `FabricTagsProvider.BlockTagsProvider`, `ModelLayerRegistry`, ...).
- Verify APIs against decompiled sources (`./gradlew genSources`) instead of guessing; many 1.21 names changed (e.g. `Identifier`, `EntityTypes`, `ContextIntProviders`, `Feature` replaces `ConfiguredFeature`, blocks have no codec).
- 26.x worldgen: statuses are merged (`TERRAIN` = noise + surface + carvers) and from then on chunks only keep the final heightmaps; `*_WG` heightmaps are stale/unprimed during decoration, and `ChunkAccess.getHeight` already returns the top block's Y (no `- 1`). Worldgen code that needs column heights should scan the column (see `CityGenerator.shapeTerrain`).
- Client gametests run at render distance 5. For wide shots raise both `options.renderDistance()` and `PlayerList.setViewDistance`; chunk sending is still slow, so prefer fixed waits over `waitForChunksRender` there (`CityChecks` also writes a top-down `city_map.png`).
- 26.x client: GUI code uses `GuiGraphicsExtractor` (`extract*` methods instead of `render*`), screens open with `minecraft.gui.setScreen`, input is SDL (`InputConstants.KEY_*`, `Type.KEYBOARD`; no GLFW on the classpath).

## Layout

- `src/main` — common code. `registry/` (blocks, items, effects, entities, tabs, tags, particles, menus, components, attachments), `entity/`, `economy/` (shop merchant, offers, wallet), `consumable/` (the 40 consumables and their buffs), `enchantment/` + `worldgen/` (bootstraps used by datagen; `worldgen/lair/` named lairs), `job/` (class system, `job/quest/` advancement trials, `job/gear/` class armor and stat totals), `loot/` (shop split, drops, ether, evolution), `entity/named/` + `entity/boss/` (named monsters, raid bosses), `raid/` (raid dimension, arenas, parties, instances, loot sessions), `city/` (the capital at 0, 0), `progress/` (reset cycle, records, achievements, titles, collection bonuses), `bounty/`, `enhance/`, `market/`, `talent/`, `network/`, `command/`, `mixin/`.
- `src/client` — renderer, model layer, **datagen providers** (`client/datagen`).
- `src/main/generated` — datagen output, committed. Never hand-edit; change the provider and run `./gradlew runDatagen`.
- `src/gametest` — client game tests (`./gradlew runClientGameTest` runs every entrypoint of `src/gametest/resources/fabric.mod.json` in order, about 20 minutes); keep them passing after changes. Put a test you are iterating on first in that list, then restore the order.
- `tools/TextureGen.java` (+ `tools/ClassArt.java`, `tools/QuestArt.java` for trial tokens and trainer/NPC skins, `tools/EndgameArt.java` for endgame items and lair chests) — draws every texture except class weapons from scratch (no vanilla assets); run it instead of editing PNGs by hand. Class weapon textures are drawn at datagen time by `client/datagen/art` (`WeaponArtist`, `Shapes`, `Bows`) from each weapon's archetype, tier and `WeaponArt` colors; `runDatagen` also writes a review sheet to `build/weapon-preview.png`.

## Rules

- Content that is data-driven in 26.x (enchantments, ore features/placements, recipes, loot, tags, lang) goes through datagen, not hand-written JSON. Exception: `assets/minecraft_mode/equipment/mythril.json`.
- `ShopMerchant` is not an entity; `MerchantMenuMixin` must stay or shift-click trades crash the server.
- Shop prices: base lists in `ShopOffers.trades(ShopType)`; market pressure lives in `MarketData` (world SavedData) and is applied through `MerchantOffer.setSpecialPriceDiff`. Keep `ShopType` ids and trade item ids stable — they are the saved market keys.
- 26.3 reads natural spawns from the `NATURAL_MOB_SPAWNS` environment attribute; `NormalWorldClientGameTest` checks that Fabric `addSpawn` reaches it.
- Enchantments: each set is a class in `enchantment/` (`WeaponEnchantments`, `RangedEnchantments`, `ToolEnchantments`, `ArmorEnchantments`) with `EnchantInfo` (key + en/ko name + en/ko description) and a bootstrap built on `EnchantmentFactory`. Prefer data-driven effects; marker enchantments are read in code by `ToolEnchantmentHandlers`, `CombatEnchantmentHandlers`, `ArmorAuras`, `BowItemMixin` and the client `LightmapRenderStateExtractorMixin`. Lang/tags iterate `ModEnchantments.ALL`.
- Auras must never stack: one fixed transient modifier id per aura (`aura/<name>`), level = max over nearby sources. Buffs use the enchantment's own attribute ids, so buff + aura stack by design.
- `docs/ENCHANTMENTS.md` is generated from `src/main/generated` (en/ko lang + max levels); regenerate it after changing enchantments.
- After a change: `runDatagen` (if data changed) → `build` → `runClientGameTest`, and look at the screenshots.

## Class system (`job/`)

- `JobClass` (7 classes x 4 tiers, titles + passives in en/ko; append new classes at the end — the network codec uses the ordinal), `JobData` (player attachment `ModAttachments.JOB`: class, tier, level, exp, MP, cooldowns; synced to the owner, kept on death), `JobProgression` (exp curve, tier levels 10/25/45/70; `advance` only checks level — items are the trial's job), `JobStats` (max MP/regen, attribute modifiers from level, passives and the held weapon's engravings), `JobEvents` (exp/essence from kills and ores, MP tick, Avalon, death penalty).
- Content: one file per class in `job/content/` built with the `ClassContent` DSL (`weapon(...)` + `skill(...)` + `Actions.*`). Startup validates tier/level ranges and skill counts (3 per weapon, 4 at tier 4); ids are saved item/cooldown keys — never rename them. Tooltips, lang and docs are generated from these definitions.
- Skills: `job/skill/Actions` holds every building block with its own tooltip template (`Actions.texts()` -> lang). Add new behavior there, not in content files. Damage from skills/shots goes through `CombatHooks.deal` with a `DamageKind`; `LivingEntityMixin` feeds `CombatHooks.modifyIncoming` (passives, engravings, marks, stances, vulnerability). Delayed steps use `SkillScheduler`; `SkillContext.valid()` must be checked in delayed code.
- A class weapon is "active" only when class, tier and level all match (`JobWeapons.isActive`); otherwise it is a plain weapon (basic attack/shot only, no skills, no engravings).
- Engravings: `Engraving` enum (per class, optional archetype filter), stored in the `ModDataComponents.ENGRAVINGS` item component (max 3 lines on weapons, 4 on class armor with per-slot armor engravings; duplicates stack, caps in `EngraveStat`). The `EngravingMenu` (engraving table) charges essence from the inventory; condensed essence = 9.
- Guild shop offers depend on the visitor (`ShopOffers.trades(ShopType, Player)`); trade keys stay stable for market pressure.
- `docs/CLASSES.md` is written by `ClassDocProvider` during `runDatagen`; do not edit it by hand.
- `JobClientGameTest` casts every skill of every weapon once; keep it passing when adding or changing skills.

## Trials and trainers (`job/quest/`, `entity/ClassTrainer`)

- Choosing a class and every advancement is a trial from that class's `ClassTrainer` (no advance button). `Quests` defines the 28 trials (`<class>_<tier>`) and their 28 trial tokens; a trial = kill goals + token count + materials (always essence; tier 3 adds a golem core, some tier 4 a boss item). Quest and token ids are saved keys — never rename them.
- `QuestData` (attachment `ModAttachments.QUEST`, synced to the owner, kept on death): active trial id, kill progress, `visitedCity`. `QuestService` holds the rules (status per trainer, accept/complete/abandon, kill credit on `AFTER_DEATH`); screens read the same synced data. Tokens only drop for a player whose trial needs them and go straight into the inventory; bosses (`Quests.BOSSES`) credit everyone with the trial within 64 blocks.
- Trainer actions come in as `QuestActionPayload` and need the trainer within 8 blocks; right-clicking a trainer sends `OpenTrainerPayload` (client `TrainerScreen`).
- Trial lang (trainer names/greetings, trial texts, tokens, guide book) lives in `client/datagen/TrialLang`; `ClassDocProvider` writes the trial table into `docs/CLASSES.md`.

## The capital (`city/`)

- Stormhold is generated at 0, 0 in the overworld of noise worlds only (flat test worlds have none). `ChunkGeneratorMixin` calls `CityGenerator` during decoration: chunks inside `CityZone.CORE` are flattened to `CityZone.baseY` (median natural height, cached per seed) and built; vanilla features/structures there are suppressed except ores; a `BLEND` ring eases terrain back. Every write goes through `Build`, which clips to the chunk being decorated — buildings must be pure functions of coordinates (no randomness that differs per chunk, no reading neighbour chunks).
- Districts: `CityCore` (roads, plaza + fountain, walls, gates, lamps), `CityNorth` (keep + raid gate, mage tower, Enchanter's Hall, warrior arena, Hunter Association), `CityMiddle` (old town + rogue Shadow Hall, cathedral, homes), `CitySouth` (archer park + Urahara Shop, market + alchemist + Adventurers' Guild with shops/engraving tables, forge, harbor + pirate ship). Trainer posts are `CityZone.trainerHome`; `CityServices.keepTrainers` (every 100 ticks) spawns missing trainers, removes duplicates and brings wanderers back. The hall's four enchanting tables (15 bookshelves each, level 30) and the city anvils are listed in `CityZone.enchantingTables/anvils`; `CityServices.keepAnvils` replaces worn anvils because nobody can place blocks in the city.
- Multiplayer-first: world spawn is the plaza (respawn radius 0); first join teleports there and gives the guide book. Inside the walls (`CityZone.inside`) the city is a safe zone: no hostile natural spawns (`NaturalSpawnerMixin`), city guards (`CityServices.driveOffHostiles`, every second) remove hostile mobs that get in anyway (climbing spiders, cave wanderers, eggs; skill summons and NoAI mobs are exempt), no building/breaking for non-op survival players (`PlayerMixin`, `BlockItemMixin`), no PvP (`CombatHooks`), no explosion block damage (`ServerExplosionMixin`).
- `NormalWorldClientGameTest` runs `CityChecks` (layout, trainers, safe zone, district/trainer screenshots) before moving to natural terrain for the ore/spawn checks.

## Economy, consumables and raids

- Coins never sit in the inventory: `Wallet` (attachment `ModAttachments.WALLET`, copper) deposits them every 10 ticks; `Coins.total/take/give` see wallet + inventory. Shops pay from the wallet through `MerchantMenuMixin` (fills the payment slots). `KEEP_INVENTORY` is forced on at server start.
- Coin sinks scale with `GearShop.bracketPrice` (raid fee, engraving/reroll/removal, evolution, armor reroll), so they grow with level. Keep new sinks on that curve.
- `consumable/Consumables` defines the 40 consumables (`ConsumableDef`, tiers 1-5; lasting effects are 10 minutes = `Consumables.LONG`); buffs are marker effects whose stats `BuffEffects` feeds into `GearStats`. Icons are drawn by `client/datagen/art/ConsumableArtist`. Item ids are saved keys. `docs/CONSUMABLES.md` is generated.
- Raid bosses declare their mechanics in `RaidBoss.mechanics()` (first at an HP fraction, then on an interval, never closer than `MECHANIC_GAP` = 45 s). Mechanic damage uses `RaidDamage` (bypasses armor and resistance, not totems/feather/Avalon). Team mechanics must scale their requirement with party size so solo players can clear them.
- Soul Reaper and Hunter passives are stat lines in `job/gear/ClassPassives` (added up by `GearStats`); the first five classes keep theirs as rules in `CombatHooks`/`JobStats`.

## Named lairs (`worldgen/lair/`)

- `NamedLairs` defines one lair per named monster (`LairDef`: setting, landmark shape, palette, biomes, `LairMobs` spawn list, decor). All 22 share one structure set (`minecraft_mode:named_lairs`, one lair per cell, excluded near villages, the capital and the End's main island). Structures and the set are datagen (`bootstrapStructures/bootstrapSets`).
- `LairPiece` builds everything in `postProcess` clipped to the chunk; the maze (`Maze`) and goal are pure functions of the piece's seed. The piece box includes `GROUNDS` blocks of land around the lair; the structure's MONSTER spawn override applies there, and `NamedMob.checkSpawnRules` relaxes height band and spacing inside its own lair (`NamedLairs.at`).
- Loot is `LairLoot` (coins always + at least one reward, gear share `GEAR_SHARE`). `/place structure` does not register structure references, so `NamedLairs.at` only sees naturally generated lairs (`NormalWorldClientGameTest` locates one); `LairClientGameTest` builds every shape with `/place`.

## Content direction

- New classes, skills and weapons should come mainly from anime/light novels the user likes: **Hunter x Hunter, Bleach, Naruto, Type-Moon (Fate)** (One Piece, Fate, Bleach — Soul Reaper — and Hunter x Hunter — Hunter — are already used; Naruto is next). Keep Korean names faithful to the official Korean translations.

## Gear, named monsters, raids

Design notes: `docs/DESIGN-gear-raids.md`. `docs/GEAR.md` and `docs/MONSTERS.md` are written by `GearDocProvider` during `runDatagen`; do not edit them by hand.

- Stats: weapon engravings, armor options/engravings, set bonuses and level rewards all sum into `EngraveStat` through `GearStats.of(player)` (cached per tick, separate client/server caches). `JobWeapons.activeTotals` delegates to it. A new stat = an `EngraveStat` entry + the code that reads it.
- Armor (`job/gear/`): one content file per class (`ArmorContent` DSL, 10 sets x 4 pieces). Set/piece ids are saved item keys — never rename. Only the matching class/tier/level can wear a piece (`GearRules`); options roll lazily (`GearArmorItem.inventoryTick`) or on drop (`GearDrops.create`).
- Shop: one weapon + one armor piece per class and 10-level bracket (`GearIndex.shopItems`); everything else is drop-only. Evolution (blacksmith `CityNpc` -> `UpgradeMenu`) costs `GearUpgrades.ETHER_COST` ether of the target bracket.
- Named monsters: definitions in `NamedMobs.define()`, behavior in `NamedMob` (one method per `Ability.Type`). Models are body plans (`client/creature/NamedPlans`, `BossPlans`) rendered by `CreatureModel`; textures/glow/eggs are painted at datagen by `CreaturePainter` (`build/creature-preview.png` shows sizes in blocks — size hitboxes from it).
- Bosses (`entity/boss/`): `RaidBoss` holds phases, boss bar, damage divisor (health attribute is always 1000; toughness = `BossDef.health` x party scale), leash and the pattern scheduler. Each boss lists `pattern(id, phase, cooldown, range, busy, action)`. Every area attack is telegraphed first (`Telegraph` / `RaidBoss.circle|line|cone|donut|rain`), and delayed code must check `alive()`.
- Raids (`raid/`): dimension type from `RaidDimension.bootstrapType` (datagen dynamic registry), level stem from `RaidDimensionProvider`. `Raids` runs instances (arena slots 1024 blocks apart, rebuilt by `Arenas.build` each time); dying in the raid dimension never kills (`Raids.fall` -> sent home with items). Building and PvP are blocked there through `CityServices.blocksBuilding/blocksPvp`. Parties (`Parties`), raids and loot sessions (`raid/loot/`) live in server memory only.
- `GearRaidClientGameTest` checks content counts, armor/sets/cooldown floor, evolution, named spawns, runs every boss pattern once, the raid flow, dying in a raid and both loot modes; keep it passing.

## Endgame (`progress/`, `bounty/`, `enhance/`, `market/`, `talent/`)

Design notes: `docs/DESIGN-endgame.md`. `docs/ENDGAME.md` is written by `GearDocProvider` and the per-class talent tables in `docs/CLASSES.md` by `ClassDocProvider`; do not edit them by hand.

- Resets follow the Minecraft calendar, never real time: `ResetCycle` (day = 24000 overworld clock ticks, cycle = 3 days). Lair rewards and lords, raid lockouts and modifiers and the cycle bounty renew per cycle; daily bounties per day.
- Lairs: goal chests and caches are `LairChestBlock` + `LairChestBlockEntity` with per-player, per-cycle contents (seeded by chest, player UUID and cycle). The goal chest wakes the lair lord (`NamedMob.makeLord`) once per cycle when a non-spectator comes within 20 blocks or opens it, and stays sealed while the lord lives. The first open of a cycle counts as a lair clear (`Progress.lairCleared`).
- Raids: `RaidDifficulty` (unlock order, fee, health/damage, extra rewards), `RaidAffix` (2 per cycle, Heroic/Nightmare only), lockout keys `PlayerRecords.raidKey(boss, difficulty)` (locked members join as free practice), record table `RaidRecordsData`. The marshal screen gets cycle, modifiers and records in `OpenRaidPayload`.
- Player data attachments (synced to the owner, kept on death): `RECORDS` (`PlayerRecords`: kills, clears, lockouts, achievements, title), `BOUNTY` (`BountyData` + merit), `TALENTS`. Everything that counts toward the codex, achievements and bounties goes through `Progress`; achievement and merit offer ids, bounty kinds and talent node ids (`class.branch.tier`) are saved keys — never rename them.
- Enhancement is the `ModDataComponents.ENHANCEMENT` component (level + artisan's spirit); `Enhancement.lines` feeds `GearStats`, `GearUpgrades.evolve` keeps it, names get "+N". The bench is `EnhanceMenu` (Artisan Brokk).
- Market: `AuctionHouse` (SavedData: listings + mailboxes) and `AuctionService` (rules, fees, expiry on overworld game time). The whole market is sent to the client (`AuctionStatePayload`), which filters and pages.
- City NPC roles: `BOUNTY_CLERK` (guild), `BROKER` (market stall with the lectern), `ENHANCER` (forge); network actions check the NPC is within 8 blocks.
- `EndgameClientGameTest` covers all of it (fake players for other sellers and loot rolls) and screenshots every new screen; keep it passing.
