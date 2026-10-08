# minecraft_mode

Fabric mod for Minecraft Java **26.3**. Mod id `minecraft_mode`, package `com.minecraftmode`. See README.md for content.

## Toolchain

- JDK 25 required (`C:\Program Files\Eclipse Adoptium\jdk-25.0.4.101-hotspot`, set as machine JAVA_HOME).
- Loom plugin `net.fabricmc.fabric-loom` (non-remapping). 26.x is unobfuscated: **Mojang names, no mappings, `implementation` not `modImplementation`, `jar` not `remapJar`.**
- Fabric API uses Mojang-style names since 26.1 (`CreativeModeTabEvents`, `FabricBlockLootSubProvider`, `FabricTagsProvider.BlockTagsProvider`, `ModelLayerRegistry`, ...).
- Verify APIs against decompiled sources (`./gradlew genSources`) instead of guessing; many 1.21 names changed (e.g. `Identifier`, `EntityTypes`, `ContextIntProviders`, `Feature` replaces `ConfiguredFeature`, blocks have no codec).
- 26.x client: GUI code uses `GuiGraphicsExtractor` (`extract*` methods instead of `render*`), screens open with `minecraft.gui.setScreen`, input is SDL (`InputConstants.KEY_*`, `Type.KEYBOARD`; no GLFW on the classpath).

## Layout

- `src/main` — common code. `registry/` (blocks, items, effects, entities, tabs, tags, particles, menus, components, attachments), `entity/`, `economy/` (shop merchant, offers, coin drops), `enchantment/` + `worldgen/` (bootstraps used by datagen), `job/` (class system), `network/`, `command/`, `mixin/`.
- `src/client` — renderer, model layer, **datagen providers** (`client/datagen`).
- `src/main/generated` — datagen output, committed. Never hand-edit; change the provider and run `./gradlew runDatagen`.
- `src/gametest` — client game test (`./gradlew runClientGameTest`); keep it passing after changes.
- `tools/TextureGen.java` (+ `tools/ClassArt.java`) — draws every texture except class weapons from scratch (no vanilla assets); run it instead of editing PNGs by hand. Class weapon textures are drawn at datagen time by `client/datagen/art` (`WeaponArtist`, `Shapes`, `Bows`) from each weapon's archetype, tier and `WeaponArt` colors; `runDatagen` also writes a review sheet to `build/weapon-preview.png`.

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

- `JobClass` (5 classes x 4 tiers, titles + passives in en/ko), `JobData` (player attachment `ModAttachments.JOB`: class, tier, level, exp, MP, cooldowns; synced to the owner, kept on death), `JobProgression` (exp curve, tier levels 10/25/45/70, advancement costs), `JobStats` (max MP/regen, attribute modifiers from level, passives and the held weapon's engravings), `JobEvents` (exp/essence from kills and ores, MP tick, Avalon, death penalty).
- Content: one file per class in `job/content/` built with the `ClassContent` DSL (`weapon(...)` + `skill(...)` + `Actions.*`). Startup validates tier/level ranges and skill counts (3 per weapon, 4 at tier 4); ids are saved item/cooldown keys — never rename them. Tooltips, lang and docs are generated from these definitions.
- Skills: `job/skill/Actions` holds every building block with its own tooltip template (`Actions.texts()` -> lang). Add new behavior there, not in content files. Damage from skills/shots goes through `CombatHooks.deal` with a `DamageKind`; `LivingEntityMixin` feeds `CombatHooks.modifyIncoming` (passives, engravings, marks, stances, vulnerability). Delayed steps use `SkillScheduler`; `SkillContext.valid()` must be checked in delayed code.
- A class weapon is "active" only when class, tier and level all match (`JobWeapons.isActive`); otherwise it is a plain weapon (basic attack/shot only, no skills, no engravings).
- Engravings: `Engraving` enum (per class, optional archetype filter), stored in the `ModDataComponents.ENGRAVINGS` item component (max 3 lines, duplicates stack, caps in `EngraveStat`). The `EngravingMenu` (engraving table) charges essence from the inventory; condensed essence = 9.
- Guild shop offers depend on the visitor (`ShopOffers.trades(ShopType, Player)`); trade keys stay stable for market pressure.
- `docs/CLASSES.md` is written by `ClassDocProvider` during `runDatagen`; do not edit it by hand.
- `JobClientGameTest` casts every skill of every weapon once; keep it passing when adding or changing skills.
