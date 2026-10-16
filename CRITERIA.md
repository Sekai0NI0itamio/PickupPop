# CRITERIA — PickupPop 1.0.0 (Fabric 1.21.1)

Evidence rule: a box is checked only with named evidence (CI run URL,
command output, log excerpt). Never from memory.

## Project location & repo (local + gh)

- [x] Folder `PickupPop/` exists in the Mods workspace, no nesting, no
      unrelated files touched.
- [x] Public repo `PickupPop` exists under the authenticated account
      (https://github.com/Sekai0NI0itamio/PickupPop).
- [x] `gradle build` passes on GitHub Actions (Temurin Java 21, Gradle
      8.10.2); non-empty `pickuppop-*.jar` + test reports attached.
      Evidence: run 38893122131
      (https://github.com/Sekai0NI0itamio/PickupPop/actions/runs/38893122131),
      BUILD SUCCESSFUL, `pickuppop-1.0.0.jar` (37 193 bytes) +
      sources jar verified non-empty in log.
- [x] All unit tests pass in CI (`:test` executed, not skipped; 21 tests
      in 4 classes: PickupAggregator 10, PickupEntry 4, PickupPopConfig 5,
      PickupBenchmark 2). Evidence: same run, every test PASSED in log.
- [x] NEVER built locally — only via `.github/workflows/build.yml`.
      Evidence: no `build/` or `.gradle/` output under `PickupPop/`.

## Correctness

- [x] Single item pickup creates one entry with accurate count (CI test:
      `singlePickupCreatesOneEntry` PASSED).
- [x] Repeated pickups merge with lifetime refresh (CI test PASSED).
- [x] Different items / renamed stacks stay separate (CI tests PASSED).
- [x] Experience merges into one entry (CI test PASSED).
- [x] Entries expire after `displayTime`; sticky mode (`0`) skips expiry
      (CI tests PASSED; sticky via `onEndTick` guard).
- [x] Full list evicts oldest; merged entry moves to end (CI tests PASSED).
- [x] Config round-trip, missing/malformed/clamped handling (CI tests PASSED).
- [x] Disconnect/player-change clears state (code: `onDisconnect` +
      player-id check; in-game run pending — no client launch in CI).
- [ ] In-game smoke (items/XP/arrows appear, merge, fade; config respected):
      OPEN — cannot run in headless CI. Tracked as known limitation; the
      mod compiles against real Yarn names (CI-verified) but was never
      launched.

## Performance (honest)

- [x] Burst benchmark (50k adds → 38 ms) + expiry sweep (80 ticks → 0 ms)
      run in CI with timings in logs (run 38893122131).
- [x] No claim of FPS improvement without measurement (docs label
      in-game impact unverified).

## License & docs

- [x] `LICENSE` (full MPL-2.0), `NOTICE`, `docs/UPSTREAM_AUDIT.md`,
      `docs/LICENSE_COMPLIANCE.md`, `docs/PERFORMANCE.md` present
      (asserted by CI license-check step, job green).
- [x] Exhibit A header on every source file (8/8 main sources).
- [x] README documents upstream relation, versions, install, config,
      differences, no-FPS-claim performance note.
- [x] No Modrinth/CurseForge release made (no tags; release job never ran).

## Failure cases (must hold)

- [x] Malformed config does not crash; defaults + backup (CI test PASSED).
- [x] Pickup with null world/player is ignored (null guards in
      `onEntityPickup`/`onHudRender`; no exception path).
- [x] Unbounded growth prevented (`maxEntries` cap enforced in
      `addOrMerge`; `HANDLED_MAX_IDS` bound).
