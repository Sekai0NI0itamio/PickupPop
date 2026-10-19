# CurseForge listing — PickupPop

Draft metadata and step-by-step publishing notes for the CurseForge upload.
(This file exists so the exact copy/paste values are reviewable and
version-controlled rather than living only in a browser session.)

## Project

| Field | Value |
|---|---|
| Name | `PickupPop` |
| Class | Mods (auto-set, leave it) |
| Summary | Lightweight pickup notifications: every item, XP orb and recovered arrow you collect, with merged counts and clean fade. Client-side only. |
| License | Mozilla Public License 2.0 |
| Distribution | Allow distribution to 3rd party |
| Main category | Utility & QoL |
| Additional categories | (optional) Decoration |
| Logo | `pickuppop-logo.png` (repo root, 512×512) |

Name rule check: CurseForge rejects names containing the game/class name
("Minecraft", "Mod"). `PickupPop` contains neither.

## First file

| Field | Value |
|---|---|
| File | `pickuppop-1.0.0.jar` (CI artifact `pickuppop-jar`) |
| Display name | `PickupPop 1.0.0` |
| Release type | Release |
| Environment | **Client** (fabric.mod.json declares `"environment": "client"`; server install is not required) |
| Modloader | Fabric |
| Java | Java 21 |
| Minecraft | 1.21.1 |

## Description (markdown, into `textarea.mde-text`)

```markdown
## PickupPop

Lightweight, client-side pickup notifications for Minecraft 1.21.1 (Fabric).

When you collect something, a compact list pops up showing its icon, name and
amount. Repeat pickups merge into a running count, then the entry slides and
fades away. No inventory HUD, no clutter — just a quiet readout when you loot.

### Features

- Notifications for collected **items**, **experience orbs** and **recovered arrows**
- Item icons with readable names (custom/enchanted names included) and accurate quantities
- Repeated pickups merge into one entry with a live count
- Configurable duration, screen corner, offsets, scale and colors
- Fade and slide-out animations
- Rarity-colored names and enchantment glint on icons
- Optional inventory-total display and chat-style background
- Hard cap on simultaneous entries, plus automatic cleanup on disconnect or world change

### Performance

PickupPop is a clean-room rebuild with the hot paths optimized:

- **O(1) merge lookup** (keyed map) instead of a list scan per pickup
- **No per-frame stream, boxing or temporary collection** in the render path
- **Text and measured width cached** until a count actually changes
- **Direct item rendering** — no extra fullscreen framebuffer pass per icon per frame
- **Config read once** per event/frame, never per entry
- 21 unit tests plus deterministic CI benchmarks; all builds run on CI

All source is available and the notification path is covered by automated tests.

### Configuration

A JSON file at `config/pickuppop.json` controls everything; defaults work out
of the box:

| Option | Default |
|---|---|
| `includeItems` / `includeExperience` / `includeArrows` | `true` |
| `combineEntries` | `EXCLUDE_NAMED` (also `ALWAYS`, `NEVER`) |
| `displayTime` | `80` ticks |
| `position` | `TOP_LEFT` |
| `maxHeight` | `0.5` (never fills more than half the screen) |
| `scale` | `4` |
| `maxEntries` | `20` |
| `displayAmount` | `TEXT` (also `OFF`, `SPRITE`, `BOTH`) |

The file is created once and never overwritten — delete it to regenerate with
new defaults.

### Requirements

- Minecraft **1.21.1** (Java Edition)
- Fabric Loader **0.16.14+**
- Fabric API **0.102.0+1.21.1**
- Java **21**

Client-side only: install in your own client, no server install needed.

### Relationship to other mods

PickupPop is inspired by **Pick Up Notifier** by Fuzs and is released under the
same **Mozilla Public License 2.0**. It is an independent clean-room adaptation,
not affiliated with or endorsed by the original authors. Its source code, full
attribution and modification notes are in the GitHub repository.

### Links

- Source code and full documentation: https://github.com/Sekai0NI0itamio/PickupPop
- License (MPL-2.0): https://github.com/Sekai0NI0itamio/PickupPop/blob/main/LICENSE
- Attribution: https://github.com/Sekai0NI0itamio/PickupPop/blob/main/NOTICE
```

## After upload

- Project and files stay invisible until a CurseForge moderator approves them
  (typically 24–72 h). That is expected.
- Optional follow-ups: gallery images under **Media**, and adding **Fabric API**
  as a required dependency under **Related Projects** so CurseForge auto-installs
  it.
- Public page once approved: `https://www.curseforge.com/minecraft/mc-mods/pickuppop`.
