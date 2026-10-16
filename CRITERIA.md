# CRITERIA — PickupPop 1.0.0 (Fabric 1.21.1)

Evidence rule: a box is checked only with named evidence (CI run URL,
command output, log excerpt). Never from memory.

## Project location & repo (local + gh)

- [x] Folder `PickupPop/` exists in the Mods workspace, no nesting, no
      unrelated files touched.
- [ ] Public repo `PickupPop` exists under the authenticated account
      (https://github.com/Sekai0NI0itamio/PickupPop). Evidence: _pending push_.
- [ ] `gradle build` passes on GitHub Actions (Temurin Java 21, Gradle
      8.10.2); non-empty `pickuppop-*.jar` + test reports attached.
      Evidence: _pending first CI run_.
- [ ] All unit tests pass in CI (`:test` executed, not skipped; 4 test
      classes: PickupAggregator, PickupEntry, PickupPopConfig,
      PickupBenchmark). Evidence: _pending_.
- [ ] NEVER built locally — only via `.github/workflows/build.yml`.
      Evidence: workspace has no `build/` output (verify before done).

## Correctness

- [ ] Single item pickup creates one entry with accurate count (CI test:
      `singlePickupCreatesOneEntry`).
- [ ] Repeated pickups merge with lifetime refresh (CI test).
- [ ] Different items / renamed stacks stay separate (CI tests).
- [ ] Experience merges into one entry (CI test).
- [ ] Entries expire after `displayTime`; sticky mode (`0`) skips expiry
      (CI tests; sticky covered by `onEndTick` guard — no dedicated test).
- [ ] Full list evicts oldest; merged entry moves to end (CI tests).
- [ ] Config round-trip, missing/malformed/clamped handling (CI tests).
- [ ] Disconnect clears state (code present; in-game verification pending —
      no client launch possible in CI).
- [ ] In-game smoke (items/XP/arrows appear, merge, fade; config respected):
      pending — cannot run in headless CI. tracked as known limitation.

## Performance (honest)

- [ ] Burst benchmark (50k adds) + expiry sweep run in CI with timings in
      logs (CI tests print timings).
- [ ] No claim of FPS improvement without measurement (docs label
      in-game impact unverified).

## License & docs

- [ ] `LICENSE` (full MPL-2.0), `NOTICE`, `docs/UPSTREAM_AUDIT.md`,
      `docs/LICENSE_COMPLIANCE.md`, `docs/PERFORMANCE.md` present
      (asserted by CI license-check step).
- [ ] Exhibit A header on every source file.
- [ ] README documents upstream relation, versions, install, config,
      differences, no-FPS-claim performance note.
- [ ] No Modrinth/CurseForge release made.

## Failure cases (must hold)

- [ ] Malformed config does not crash; defaults + backup (CI test).
- [ ] Pickup with null world/player is ignored (null guards in
      `onEntityPickup`/`onHudRender`; no exception path).
- [ ] Unbounded growth prevented (`maxEntries` cap enforced in
      `addOrMerge`; `HANDLED_MAX_IDS` bound).
