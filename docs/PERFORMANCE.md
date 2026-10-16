# Performance — PickupPop

## Baseline

Upstream Pick Up Notifier, branch `1.21.1`, commit
`420357d93013a21feee0b2567b7611fe9f0a7b0d` (2026-09-06).
Method: static inspection of the upstream sources (see
`docs/UPSTREAM_AUDIT.md` §4 for per-location findings). No dynamic
profiling of upstream was possible in this environment (no local
execution; headless CI cannot launch a Minecraft client).

## Bottlenecks identified (upstream locations)

1. O(n) stream duplicate scan per pickup (`PickUpCollector.findDuplicate`).
2. Stream average×size per frame for slide offset (`getTotalFade`).
3. Dozens of config-holder lookups per entry per frame.
4. Repeated `font.width` measurement (up to 3× per entry per frame).
5. Fullscreen extra framebuffer + blit per sprite per frame
   (`TransparencyBuffer`).
6. Per-frame `currentTimeMillis` + `sin` + shader-color churn for XP icons.
7. Full-inventory scan on first text build (opt-in flag, default off).
8. Stack copy + component write per pickup.
9. Tick-map churn for duplicate suppression.

## Optimizations implemented

| # | Change | Expected benefit |
|---|---|---|
| 1 | `LinkedHashMap<PickupKey, PickupEntry>` aggregation | O(1) merges; zero stream/lambda allocation per pickup |
| 2 | Plain-loop expiry + slide-total | No per-frame stream pipelines or boxing |
| 3 | Config snapshotted once per event / per frame | Removes ~10+ holder lookups per pickup and per entry per frame |
| 4 | Cached text + width, rebuilt only on count change | Removes up to 3 font measurements per entry per frame |
| 5 | Direct `drawItem`; no extra framebuffer | Removes FBO bind/clear/full-screen blit per icon per frame (largest render saving) |
| 6 | Static XP orb frame | Removes per-frame time/sin/shader calls |
| 7 | Inventory scan only when flag enabled (default off) | Zero added cost by default |
| 8 | Single `ItemStack.copy()` at event time, no component write | One fewer map write per pickup |
| 9 | Bounded dedup map (80-tick TTL + 1024-id cap), `maxEntries` cap (default 20) | No unbounded growth on long sessions |

## Tests and benchmarks (CI)

- `PickupAggregatorTest` (10 tests): merge/separate/expire/evict/order/cap.
- `PickupEntryTest` (4 tests): lifetime, discard, validation, slide math.
- `PickupPopConfigTest` (5 tests): defaults, round-trip, missing/malformed
  files, clamping.
- `PickupBenchmarkTest` (2 tests): fixed workloads printing timings to CI
  logs — 50 000 burst adds and an 80-tick expiry sweep. Sanity bound only
  (< 5 s); no FPS claims.

Environment: GitHub Actions `ubuntu-latest`, Temurin Java 21, Gradle 8.10.2.
Workloads are deterministic (fixed keys/counts, no randomness). Results are
read from CI logs after the first green run and recorded here.

## Measurements

- CI microbenchmarks: _pending first green CI run — timings will be pasted
  from the Actions log into this section._
- In-game FPS / frame-time impact: **unverified**. CI runners are headless
  and cannot launch a Minecraft client, so no render-loop measurement is
  possible in this pipeline. The render savings above are argued from
  eliminated work (fewer GL calls, fewer allocations), not from profiling.
- Algorithmic claim status: merge O(1) vs O(n) and zero-per-frame
  allocation for aggregation state are structural facts of the
  implementation, covered by the tests above — not FPS measurements.

## Unmeasured claims and uncertainties

- Icon fade removal changes the look slightly (documented, not a metric).
- Real-world pickup bursts (e.g. TNT-mining drops) were not replayed
  in-game; the 50k synthetic burst is a ceiling test, not a gameplay trace.
- `maxEntries` default 20 vs upstream height-derived-only cap: stricter on
  short screens, looser bookkeeping; behavior logged, not benchmarked.
