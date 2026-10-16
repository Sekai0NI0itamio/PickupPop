# PickupPop

A lightweight, client-side pickup-notification mod for Minecraft Java
**1.21.1** (Fabric, Java 21). When you collect items, experience or
recovered arrows, a compact list pops up on the HUD, merges repeat
pickups, then slides and fades away.

PickupPop is an independent adaptation inspired by
**Pick Up Notifier** by Fuzs
([GitHub](https://github.com/Fuzss/pick-up-notifier) ·
[Modrinth](https://modrinth.com/mod/pick-up-notifier), MPL-2.0),
rebuilt as a dependency-free Fabric mod with the per-pickup and per-frame
hot paths optimized. See `NOTICE`, `docs/UPSTREAM_AUDIT.md` and
`docs/LICENSE_COMPLIANCE.md` for provenance and licensing.

## Features

- Item pickup notifications with icons, readable names and accurate counts.
- Experience-orb notifications (orb count or total XP value).
- Recovered-arrow notifications.
- Merging of repeated pickups (`ALWAYS` / `NEVER` / `EXCLUDE_NAMED`).
- Configurable duration, screen corner, offsets, scale, colors, sprites,
  amount display, inventory count and entry background.
- Fade + slide-out animations.
- Correct expiry, display cap and cleanup on disconnect/world change.
- JSON config (`config/pickuppop.json`) that survives restarts.

## Supported versions

- Minecraft Java Edition **1.21.1** only.
- Fabric Loader `>= 0.16.14`, Fabric API, Java 21.
- Client-side only: install in the client `mods` folder; no server install
  needed, works on vanilla servers.

## Installation

1. Install Fabric Loader 0.16.14+ for Minecraft 1.21.1 and the matching
   Fabric API (`0.102.0+1.21.1`).
2. Drop the PickupPop JAR from the CI artifacts into `mods/`.
3. Launch the game. Tune `config/pickuppop.json` while the game is closed
   (edits apply on restart). Note: the file is created once with defaults
   and never overwritten afterwards — after updating the mod, delete it to
   pick up new defaults, or edit the individual options.

## Configuration (defaults)

| Option | Default | Meaning |
|---|---|---|
| `includeItems` / `includeExperience` / `includeArrows` | `true` | Which pickup kinds notify |
| `experienceAsValue` | `true` | Show XP value instead of orb count |
| `disableInCreative` | `false` | Silence notifications in creative |
| `combineEntries` | `EXCLUDE_NAMED` | Merge mode |
| `displayTime` | `80` | Ticks visible (`0` = sticky until evicted) |
| `moveOut` / `moveTime` / `fadeAway` | `true` / `20` / `true` | Slide/fade animation |
| `drawSprite` | `true` | Item/XP icons |
| `displayItemName` | `true` | Show names |
| `displayAmount` | `TEXT` | `OFF`/`SPRITE`/`TEXT`/`BOTH` |
| `displaySingleCount` | `true` | Show `x1` |
| `inventoryCount` | `false` | Show total in inventory (costs a scan) |
| `textColor` / `ignoreRarity` | white / `false` | Text coloring |
| `entryBackground` | `NONE` | `NONE` or `CHAT` |
| `position` / `offsetX` / `offsetY` | `TOP_LEFT` / `8` / `4` | Placement (left edge, grows down) |
| `maxHeight` / `scale` | `0.5` / `4` | List budget (max half the screen height) / size |
| `maxEntries` | `20` | Hard cap on visible entries |

## Differences from upstream (intentional)

- Fabric-only, zero extra dependencies (no Puzzles Lib / Architectury);
  no server component, so no partial-pickup server packets or backpack
  integration — pure client behavior from vanilla packets.
- No per-dimension item blacklist (deferred; see CHANGELOG).
- Background styles: `NONE`/`CHAT` only (no `TOOLTIP` gradient).
- Item icons render at full opacity while text fades (upstream fades icons
  through an extra fullscreen framebuffer — the single most expensive
  per-frame operation, removed deliberately).
- XP orbs use a static sprite frame (no per-frame color cycling).
- Aggregation is O(1) via a keyed map instead of a per-pickup list scan;
  display text/width cached until counts change.

## Performance findings (honest)

Algorithmic and allocation savings are covered by CI unit tests and
deterministic microbenchmarks (`PickupBenchmarkTest`; timings in Actions
logs). In-game FPS impact is **unverified** — headless CI cannot launch a
Minecraft client. Details, methodology and open items:
`docs/PERFORMANCE.md`.

## Build and test (CI only)

All compilation, tests and validation run exclusively through GitHub
Actions — never locally:

- Workflow: `.github/workflows/build.yml` (Temurin Java 21, Gradle 8.10.2).
- `gradle build` compiles, runs all tests, verifies a non-empty
  `build/libs/pickuppop-*.jar`, and uploads the JAR + test reports.
- The workflow also asserts the legal documents
  (`LICENSE`, `NOTICE`, `docs/*`) are present.

## Licensing

PickupPop is distributed under the **Mozilla Public License 2.0** (see
`LICENSE`; every source file carries the Exhibit A header). Upstream
attribution and the modification record live in `NOTICE`;
the file-by-file audit in `docs/LICENSE_COMPLIANCE.md`. PickupPop is not
affiliated with or endorsed by the upstream authors.

No releases are published to Modrinth/CurseForge without explicit owner
approval.
