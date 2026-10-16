# License compliance — PickupPop (MPL-2.0)

Upstream: Pick Up Notifier by Fuzs, https://github.com/Fuzss/pick-up-notifier,
branch `1.21.1`, commit `420357d93013a21feee0b2567b7611fe9f0a7b0d`, MPL-2.0.
Full analysis: `docs/UPSTREAM_AUDIT.md`. Attribution: `NOTICE`.

## Strategy

Clean-room adaptation: **no upstream file was copied or modified**. All
sources are newly written in the `com.pickuppop` namespace. Compliance
therefore rests on: (1) PickupPop itself being distributed under MPL-2.0,
(2) full license text shipped, (3) Exhibit A headers on every source file,
(4) prominent upstream attribution with provenance and modification notes,
(5) corresponding source available (this public repository), and
(6) no proprietary/conflicting terms anywhere.

## File-by-file audit

| File | Status | Notes |
|---|---|---|
| `LICENSE` | New | Complete unmodified MPL-2.0 text (mirrors upstream `LICENSE.md`) |
| `NOTICE` | New | Upstream credit, commit provenance, reuse statement, no-endorsement, non-reused third-party code |
| `src/main/java/com/pickuppop/PickupPopClient.java` | New | Own entrypoint; no upstream code |
| `src/main/java/com/pickuppop/client/PickupClientEvents.java` | New | Own bridge; behavior inspired by `AddEntriesHandler` |
| `src/main/java/com/pickuppop/client/PickupHudRenderer.java` | New | Own renderer; behavior inspired by `DrawEntriesHandler`/`DisplayEntry` |
| `src/main/java/com/pickuppop/collect/PickupAggregator.java` | New | Own data structure; replaces `PickUpCollector` |
| `src/main/java/com/pickuppop/model/PickupKey.java` | New | Own key type |
| `src/main/java/com/pickuppop/model/PickupEntry.java` | New | Own entry type; slide math mirrors upstream formula |
| `src/main/java/com/pickuppop/config/PickupPopConfig.java` | New | Own Gson config; option set/defaults mirror upstream `ClientConfig` |
| `src/main/java/com/pickuppop/mixin/ClientPacketListenerMixin.java` | New | Own mixin; same injection concept as upstream `ClientPacketListenerMixin`, Yarn names |
| `src/main/resources/fabric.mod.json` | New | Own metadata, own mod id |
| `src/main/resources/pickuppop.mixins.json` | New | Own mixin config |
| `src/main/resources/assets/pickuppop/lang/en_us.json` | New | Trivial locale stub |
| `src/test/java/...` (4 files) | New | Own tests |
| `README.md`, `CHANGELOG.md`, `CRITERIA.md` | New | Own prose (feature descriptions paraphrased, not copied) |
| `docs/*.md` | New | Own analysis |
| `build.gradle`, `settings.gradle`, `gradle.properties` | New | Own build; versions pinned, no remote scripts |
| `.github/workflows/build.yml` | New | Own CI |

Deliberately NOT reused (license-risk or bloat):
- `TransparencyBuffer.java` — adapted from ClientStorage under LGPL-3.0.
- Tooltip-gradient code in `DisplayEntryRenderHelper` — reimplemented
  minimally (CHAT only) rather than copied.
- Puzzles Lib / Architectury code — not a dependency at all.

## Obligation checklist (MPL-2.0 §3)

- [x] §3.1 Source distribution under MPL-2.0: this public repo is the
      source; `LICENSE` + per-file Exhibit A headers included.
- [x] §3.2 Executable distribution: any JAR built from this repo is
      accompanied by the source repo; `LICENSE`/`NOTICE` are packed into
      the JAR (`jar.from` in `build.gradle`).
- [x] §3.3 Larger Work: mod metadata/license fields say MPL-2.0; no
      secondary-license mixing (no GPL/LGPL code included).
- [x] §3.4 Notices: no upstream notice removed (nothing copied); own
      headers added; upstream credited in `NOTICE` + README.
- [x] No trademark confusion: own name/id/icon language; explicit
      no-endorsement statement in `NOTICE` and README.
- [x] Modifications documented: `NOTICE` + audit §5 + README
      "Differences from upstream" + CHANGELOG.
- [x] Dependency licenses reviewed: Fabric Loader / Fabric API
      (Apache-2.0), Yarn (CC0-1.0), Gson (Apache-2.0), Minecraft
      (proprietary, user-supplied). No redistribution of Minecraft code.

## Residual uncertainty

None blocking. The conservative clean-room approach (copy nothing, license
everything MPL-2.0, attribute prominently) exceeds what the license demands
for behavioral inspiration. If any future change copies upstream code
verbatim, this file must gain a per-file "modified upstream file" row and
the file must retain/extend the original copyright notice.
