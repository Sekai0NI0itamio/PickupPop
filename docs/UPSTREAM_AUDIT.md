# Upstream audit — Pick Up Notifier (Fuzs)

- Upstream repository: https://github.com/Fuzss/pick-up-notifier
- Mod page: https://modrinth.com/mod/pick-up-notifier
- Selected branch: `1.21.1` (correct branch for target Minecraft 1.21.1)
- Selected commit: `420357d93013a21feee0b2567b7611fe9f0a7b0d`
  ("Update build configuration")
- Date inspected: 2026-10-16
- Upstream license: Mozilla Public License 2.0 (`LICENSE.md`, standard text)

> Scope note: the audit covers the `Common` and `Fabric` modules only.
> `NeoForge` sources were listed but not used — PickupPop is Fabric-only.

## 1. Gradle / Fabric setup (upstream)

- Multi-loader project (`settings.gradle` includes `Common`, `Fabric`,
  `NeoForge`) driven by remote build scripts fetched at configuration time:
  `apply from: "https://raw.githubusercontent.com/Fuzss/modresources/main/gradle/.../fabric.gradle"`.
  Consequence: the build requires network access to an unpinned moving
  script; versions are resolved through a `1.21.1-SNAPSHOT` version catalog.
- `Fabric/build.gradle` depends on Fabric API **and** Puzzles Lib
  (`libs.puzzleslib.fabric`); `Common/build.gradle` depends on Puzzles Lib
  common. Puzzles Lib supplies the config system, networking and client
  events used throughout.
- `fabric.mod.json` uses placeholder expansion (`${modId}` …), declares
  `environment: "*"` with both `main` and `client` entrypoints, and depends
  on `fabric-api`, `puzzleslib`, `minecraft`, `java >= 17`, mixins
  `pickupnotifier.common.mixins.json` + `pickupnotifier.fabric.mixins.json`.
- Mappings are Mojang (Architectury-style `Common` sources), not Yarn.

PickupPop deliberately does **not** reuse this setup: single-module
Fabric-only build, pinned versions in `gradle.properties`
(mc 1.21.1, yarn `1.21.1+build.3`, loader `0.16.14`,
fabric-api `0.102.0+1.21.1`, loom `1.7.4`), no remote scripts, no Puzzles
Lib, Yarn mappings, client-only mod.

## 2. Relevant source files and their roles

| File | Role |
|---|---|
| `Common/.../PickUpNotifier.java` | Mod constants, Puzzles `NetworkHandler` (2 legacy clientbound messages), `ConfigHolder` |
| `Common/.../client/PickUpNotifierClient.java` | Registers tick / render-gui / logout / player-copy handlers, loads blacklist |
| `Common/.../client/handler/AddEntriesHandler.java` | Entry point for all pickups: client packet path (`onEntityPickup`), server packet paths (`addPickUpEntry`, `addItemEntry`); builds `ItemDisplayEntry` / `ExperienceDisplayEntry`; merge-or-add logic |
| `Common/.../client/handler/DrawEntriesHandler.java` | Singleton render+tick state: `PickUpCollector`, dedup map `handledEntities`, `onClientTick` (TTL prune, expiry tick), `onRenderGui` (per-frame layout + alpha), logout/copy cleanup |
| `Common/.../client/util/PickUpCollector.java` | `ArrayList<DisplayEntry>` with `tick`, `refresh` (remove+add), capped `add`, `findDuplicate` (stream scan), `getTotalFade` (stream average × size) |
| `Common/.../client/gui/entry/DisplayEntry.java` | Base entry: lifetime ticks, lazy `Component` text (cached until merge), width measure, `render` (pose scale, bg, text, sprite), `mergeWith` (accumulate + reset) |
| `Common/.../client/gui/entry/ItemDisplayEntry.java` | Item specifics: name source per combine mode, `sameItem` merge predicate, inventory count via full-container scan, sprite via `TransparencyBuffer` FBO |
| `Common/.../client/gui/entry/ExperienceDisplayEntry.java` | XP specifics: always merges, static orb sprite frame table, animated RGB cycling per frame |
| `Common/.../client/util/DisplayEntryRenderHelper.java` | Count shortening (K/M/B), sprite count overlay, tooltip-gradient background |
| `Common/.../client/util/TransparencyBuffer.java` | Extra fullscreen framebuffer to draw fading item sprites (adapted from ClientStorage, LGPL-3.0 — **not reused**) |
| `Common/.../client/handler/ItemBlacklistManager.java` | Per-dimension item blacklist loaded from JSON files |
| `Common/.../client/gui/PositionPreset.java` | Four screen corners, mirrored/bottom flags, anchor math |
| `Common/.../config/ClientConfig.java` | General/behavior/display groups + `CombineEntries`, `DisplayAmount`, `EntryBackground` enums (Puzzles/NeoForge config spec) |
| `Common/.../config/ServerConfig.java` | `partialPickUps`, `backpackCompat` (server side) |
| `Common/.../network/S2CTakeItemMessage.java`, `S2CTakeItemStackMessage.java` | Server→client packets for partial pickups / full stacks |
| `Common/.../mixin/client/ClientPacketListenerMixin.java` | Injects into `handleTakeItemEntity` after the same-thread check; source of client-side detection |
| `Common/.../mixin/client/MinecraftMixin.java` | Resize hook for the transparency framebuffer |
| `Fabric/.../handler/FabricItemPickupHandler.java` | Server-side partial-pickup computation (free slot / remaining space) |
| `Fabric/.../PickUpNotifierFabric.java`, `.../client/PickUpNotifierFabricClient.java` | Loader entrypoints; client registers a reload command |

## 3. Pickup-detection and notification pipeline

1. **Client detection (primary):** `ClientPacketListenerMixin` hooks vanilla
   `ClientboundTakeItemEntityPacket` handling. `AddEntriesHandler.onEntityPickup`
   resolves the entity id: `ItemEntity` → item entry (if `includeItems`),
   `AbstractArrow` → arrow entry from `getPickupItemStackOrigin` (if
   `includeArrows`), `ExperienceOrb` → XP entry (if `includeExperience`;
   amount is orb value when `experienceValue`, else packet amount).
2. **Server detection (supplement):** when the server runs the mod,
   `FabricItemPickupHandler` pre-computes partial pickups and sends
   `S2CTakeItemMessage`/`S2CTakeItemStackMessage`; ids are recorded in
   `handledEntities` so the later client event for the same entity is
   skipped (`isItemEntityHandled`). Without the mod on the server,
   `forceClient` behavior applies automatically.
3. **Aggregation:** `addEntry` computes a height-derived cap, then either
   merges into the first `findDuplicate` match (`mergeWith` + `refresh` to
   list end) or appends (evicting index 0 when over cap). Creative-mode
   players are skipped when configured.
4. **Lifetime:** `onClientTick` ages `handledEntities` (80-tick TTL) and
   ticks entries unless `displayTime == 0` (sticky mode).
5. **Rendering:** `onRenderGui` computes scale/position/alpha per frame,
   walks the list bottom-up/top-down, draws background (NONE/CHAT/TOOLTIP),
   text with fade alpha, and sprites through the transparency framebuffer.
6. **Config:** Puzzles Lib config with `general` / `behavior` / `display`
   groups; blacklist JSONs loaded at client setup.

## 4. Suspected performance costs (with locations)

All costs below are code-inspection findings; the upstream mod is small and
none of these is pathological. They are optimized in PickupPop because they
sit on per-pickup or per-frame paths.

1. **O(n) stream duplicate scan per pickup** — `PickUpCollector.findDuplicate`
   (`PickUpCollector.java:36-39`): `stream().filter(...).findAny()` on every
   pickup, plus a separate remove+add `refresh`. → PickupPop uses a
   `LinkedHashMap<PickupKey, PickupEntry>` (O(1), reorder in the same step).
2. **Stream + boxing per frame for slide offset** — `getTotalFade`
   (`PickUpCollector.java:41-47`): `mapToDouble(...).average()` allocates a
   stream pipeline every frame. → plain-loop sum in
   `PickupAggregator.totalSlideProgress`.
3. **Repeated config-holder lookups on hot paths** — `PickUpNotifier.CONFIG.get(ClientConfig.class)`
   appears ~10× in `AddEntriesHandler.addEntry` alone and many more times
   per entry per frame in `DisplayEntry.render`/`getTextComponent`/`renderBg`
   and `DrawEntriesHandler.onRenderGui`. → PickupPop snapshots config fields
   into locals once per event / once per frame.
4. **Repeated text measurement per frame** — `getEntryWidth` calls
   `minecraft.font.width(getTextComponent(...))` up to 3× per entry per frame
   (`DisplayEntry.java:134-138,149,161,179,183`). → cached row
   (text + width), rebuilt only when the merged count changes.
5. **Fullscreen extra framebuffer per sprite per frame** —
   `TransparencyBuffer.prepareExtraFramebuffer/drawExtraFramebuffer`
   (`ItemDisplayEntry.renderSprite`, `ExperienceDisplayEntry.renderSprite`):
   framebuffer bind + clear + full-screen blit + matrix juggling for every
   icon every frame, plus a static GL-size buffer never resized except via
   `MinecraftMixin`. → PickupPop draws items directly at full opacity
   (documented visual difference: icons no longer fade).
6. **Per-frame time/color computation for XP** —
   `ExperienceDisplayEntry.renderSprite:49-52`: `System.currentTimeMillis`,
   two `Mth.sin` calls and shader-color changes per XP entry per frame. →
   static orb frame, no color cycling.
7. **Full-inventory scan for counts** — `ItemDisplayEntry.getInventoryCount`
   (`:53-56`) scans the whole container via `ContainerHelper` the first time
   text is built. Upstream default has `inventoryCount = false`, so this is
   opt-in; PickupPop keeps the same default and additionally skips the scan
   call entirely when disabled.
8. **Copy + component write per item pickup** — `addItemEntry:86-91` copies
   the stack and writes `ENCHANTMENT_GLINT_OVERRIDE` in ALWAYS mode. Cheap
   but per-pickup; PickupPop keeps the copy (required for safe rendering)
   and drops the glint write.
9. **`handledEntities` churn** — `Int2ObjectArrayMap<MutableInt>` incremented
   and `removeIf`-scanned every tick while non-empty
   (`DrawEntriesHandler.java:46-50`). Small; PickupPop keeps an equivalent
   bounded `HashMap` with the same 80-tick TTL and a 1024-id safety bound.
10. **String/Component assembly per text build** — `createTextComponent`
    builds `ArrayList`, reversals and `Component` chains; cached until merge
    upstream (`component` field), which is already sound — PickupPop keeps
    the same cache-until-merge strategy with plain strings.

## 5. Planned optimizations (implemented)

- O(1) `LinkedHashMap` aggregation with same-step refresh-to-end.
- Plain-loop expiry and slide-total; single-pass tick+remove.
- Once-per-event / once-per-frame config snapshots.
- Cached display text + measured width, invalidated only on count change.
- Direct sprite rendering; no extra framebuffer; no per-frame XP color math.
- `ItemStack.copy()` once at event time; no per-frame metadata lookups
  except rarity color (cheap field read) and optional inventory count.
- Bounded dedup map (TTL + size cap); hard `maxEntries` cap (default 20)
  against unbounded growth; clear-on-disconnect/copy lifecycle.
- Zero new dependencies (no Puzzles Lib / Architectury); plain-Gson config
  loaded once, saved only on explicit save.

## 6. License and attribution approach

- Upstream is MPL-2.0. No file was copied; all PickupPop sources are new
  (`com.pickuppop` namespace) carrying Exhibit A headers, and the full
  MPL-2.0 text ships as `LICENSE`. Upstream is credited in `NOTICE`,
  `README.md` and `docs/LICENSE_COMPLIANCE.md` with repo, branch, commit
  and a reuse statement. Deliberately NOT copied: `TransparencyBuffer`
  (LGPL-3.0-tainted) and tooltip-gradient code.
- Dependency licenses: Fabric Loader (Apache-2.0), Fabric API (Apache-2.0),
  Yarn mappings (CC0-1.0), Minecraft (proprietary, provided by the user),
  Gson (Apache-2.0, test scope + runtime-bundled via Minecraft).

## 7. Limitations (no reliable FPS comparison)

- No in-game profiling was performed (all execution restricted to CI by
  project rules; no Minecraft client can launch in a headless runner).
  `docs/PERFORMANCE.md` therefore reports algorithmic deltas +
  deterministic CI microbenchmarks, explicitly labeled, and marks FPS
  impact unverified.
- Upstream multi-loader/Puzzles behavior (server packets, partial pickups
  with the mod on the server, backpack compat, dimension blacklist,
  TOOLTIP background, BOTH/SPRITE amount modes partially) is intentionally
  out of scope for this client-only adaptation; differences are listed in
  README and PERFORMANCE docs rather than silently dropped.
- Yarn API assumptions made during clean-room reimplementation
  (`ClientPlayNetworkHandler.onItemPickupAnimation`,
  `ItemPickupAnimationS2CPacket` getters,
  `PersistentProjectileEntity.getItemStack`,
  `DrawContext.drawTexture` classic overload,
  `textBackgroundOpacity.getValue()`) are verified by CI compilation, not
  by prior execution; any mismatch fails loudly in `gradle build`.
