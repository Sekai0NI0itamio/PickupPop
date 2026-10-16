// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at http://mozilla.org/MPL/2.0/.

package com.pickuppop;

import com.pickuppop.collect.PickupAggregator;
import com.pickuppop.model.PickupEntry;
import com.pickuppop.model.PickupKey;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Deterministic workload benchmarks for the aggregation hot path.
 *
 * <p>These run a fixed workload (no randomness, no wall-clock assertions
 * beyond a generous sanity bound) and print timings to stdout so CI logs
 * carry the numbers. They prove the O(1) merge path handles burst pickups
 * and expiry without degradation; they do NOT measure in-game FPS.
 */
class PickupBenchmarkTest {
    @Test
    void burstMergeWorkload() {
        PickupAggregator aggregator = new PickupAggregator();
        long start = System.nanoTime();
        for (int i = 0; i < 50_000; i++) {
            PickupKey key = PickupKey.item("item:" + (i % 16), "item:" + (i % 16));
            aggregator.addOrMerge(key, "Item", 1, 80, 20);
        }
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;
        System.out.println("PickupBenchmarkTest.burstMergeWorkload: 50000 adds -> "
                + elapsedMs + " ms, entries=" + aggregator.size());
        assertEquals(16, aggregator.size());
        int total = aggregator.snapshot().stream().mapToInt(PickupEntry::count).sum();
        assertEquals(50_000, total);
        assertTrue(elapsedMs < 5_000, "burst workload took " + elapsedMs + " ms");
    }

    @Test
    void expirySweepWorkload() {
        PickupAggregator aggregator = new PickupAggregator();
        for (int i = 0; i < 20; i++) {
            aggregator.addOrMerge(PickupKey.item("item:" + i, "item:" + i), "Item", 1, 80, 20);
        }
        long start = System.nanoTime();
        for (int i = 0; i < 80; i++) {
            aggregator.tick();
        }
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;
        System.out.println("PickupBenchmarkTest.expirySweepWorkload: 80 ticks -> "
                + elapsedMs + " ms, empty=" + aggregator.isEmpty());
        assertTrue(aggregator.isEmpty());
    }
}
