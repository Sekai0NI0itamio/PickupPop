# Changelog — PickupPop

## [1.0.0] — 2026-10-16 (initial implementation)

Adapted from Pick Up Notifier (`1.21.1` branch, commit `420357d`).

Added:
- Client-only Fabric mod for Minecraft 1.21.1 / Java 21 (`pickuppop`).
- Event-driven pickup detection via the vanilla take-item packet mixin
  (items, experience orbs, recovered arrows); no inventory polling.
- O(1) `LinkedHashMap` aggregation with merge-to-end refresh, accurate
  counts, expiry ticks, height-derived + hard (`maxEntries`) display caps.
- HUD renderer with once-per-frame config snapshot, cached text/width rows,
  slide + fade animations, four corners, CHAT background option, sprite
  count overlays, static XP sprite frames.
- Plain-Gson JSON config with defaults mirroring upstream, clamping,
  backup-on-corrupt, load-once/save-explicitly lifecycle.
- Disconnect/world-change cleanup; bounded duplicate-suppression map.
- 21 unit tests + 2 deterministic benchmark workloads (CI).
- GitHub Actions CI: Temurin 21 build, tests, JAR + report artifacts,
  legal-document presence check.
- Legal set: `LICENSE` (MPL-2.0), `NOTICE`, `docs/UPSTREAM_AUDIT.md`,
  `docs/LICENSE_COMPLIANCE.md`, `docs/PERFORMANCE.md`.

Intentionally deferred (documented, not silent):
- Server-side partial-pickup packets / backpack integration.
- Per-dimension item blacklist.
- `TOOLTIP` entry background style.
- Icon fading (icons render full-opacity by design).
