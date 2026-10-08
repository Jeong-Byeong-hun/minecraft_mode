# minecraft_mode

Fabric mod for Minecraft Java **26.3**. Mod id `minecraft_mode`, package `com.minecraftmode`. See README.md for content.

## Toolchain

- JDK 25 required (`C:\Program Files\Eclipse Adoptium\jdk-25.0.4.101-hotspot`, set as machine JAVA_HOME).
- Loom plugin `net.fabricmc.fabric-loom` (non-remapping). 26.x is unobfuscated: **Mojang names, no mappings, `implementation` not `modImplementation`, `jar` not `remapJar`.**
- Fabric API uses Mojang-style names since 26.1 (`CreativeModeTabEvents`, `FabricBlockLootSubProvider`, `FabricTagsProvider.BlockTagsProvider`, `ModelLayerRegistry`, ...).
- Verify APIs against decompiled sources (`./gradlew genSources`) instead of guessing; many 1.21 names changed (e.g. `Identifier`, `EntityTypes`, `ContextIntProviders`, `Feature` replaces `ConfiguredFeature`, blocks have no codec).

## Layout

- `src/main` — common code. `registry/` (blocks, items, effects, entities, tabs, tags), `entity/`, `economy/` (shop merchant, offers, coin drops), `enchantment/` + `worldgen/` (bootstraps used by datagen), `mixin/`.
- `src/client` — renderer, model layer, **datagen providers** (`client/datagen`).
- `src/main/generated` — datagen output, committed. Never hand-edit; change the provider and run `./gradlew runDatagen`.
- `src/gametest` — client game test (`./gradlew runClientGameTest`); keep it passing after changes.
- `tools/TextureGen.java` — regenerates placeholder textures.

## Rules

- Content that is data-driven in 26.x (enchantments, ore features/placements, recipes, loot, tags, lang) goes through datagen, not hand-written JSON. Exception: `assets/minecraft_mode/equipment/mythril.json`.
- `ShopMerchant` is not an entity; `MerchantMenuMixin` must stay or shift-click trades crash the server.
- After a change: `runDatagen` (if data changed) → `build` → `runClientGameTest`, and look at the screenshots.
