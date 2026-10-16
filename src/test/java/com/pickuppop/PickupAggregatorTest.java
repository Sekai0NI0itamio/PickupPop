// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at http://mozilla.org/MPL/2.0/.

package com.pickuppop;

import com.pickuppop.collect.PickupAggregator;
import com.pickuppop.model.PickupEntry;
import com.pickuppop.model.PickupKey;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PickupAggregatorTest {
    private static final int DISPLAY_TIME = 80;

    @Test
    void singlePickupCreatesOneEntry() {
        PickupAggregator aggregator = new PickupAggregator();
        aggregator.addOrMerge(PickupKey.item("minecraft:dirt", "minecraft:dirt"), "Dirt", 3,
                DISPLAY_TIME, 20);
        assertEquals(1, aggregator.size());
        PickupEntry entry = aggregator.snapshot().get(0);
        assertEquals(3, entry.count());
        assertEquals("Dirt", entry.displayName());
    }

    @Test
    void repeatedPickupsOfSameItemMergeAndRefreshLifetime() {
        PickupAggregator aggregator = new PickupAggregator();
        PickupKey key = PickupKey.item("minecraft:cobblestone", "minecraft:cobblestone");
        aggregator.addOrMerge(key, "Cobblestone", 5, DISPLAY_TIME, 20);
        for (int i = 0; i < 10; i++) {
            aggregator.tick();
        }
        aggregator.addOrMerge(key, "Cobblestone", 2, DISPLAY_TIME, 20);
        assertEquals(1, aggregator.size());
        PickupEntry entry = aggregator.snapshot().get(0);
        assertEquals(7, entry.count());
        assertEquals(DISPLAY_TIME, entry.remainingTicks());
    }

    @Test
    void differentItemsStaySeparate() {
        PickupAggregator aggregator = new PickupAggregator();
        aggregator.addOrMerge(PickupKey.item("minecraft:dirt", "minecraft:dirt"), "Dirt", 1,
                DISPLAY_TIME, 20);
        aggregator.addOrMerge(PickupKey.item("minecraft:stone", "minecraft:stone"), "Stone", 1,
                DISPLAY_TIME, 20);
        aggregator.addOrMerge(PickupKey.experience(), "Experience", 7, DISPLAY_TIME, 20);
        assertEquals(3, aggregator.size());
    }

    @Test
    void renamedStacksStaySeparate() {
        PickupAggregator aggregator = new PickupAggregator();
        aggregator.addOrMerge(PickupKey.item("minecraft:stick", "My Stick"), "My Stick", 1,
                DISPLAY_TIME, 20);
        aggregator.addOrMerge(PickupKey.item("minecraft:stick", "minecraft:stick"), "Stick", 1,
                DISPLAY_TIME, 20);
        assertEquals(2, aggregator.size());
    }

    @Test
    void experienceMergesIntoOneEntry() {
        PickupAggregator aggregator = new PickupAggregator();
        aggregator.addOrMerge(PickupKey.experience(), "Experience", 3, DISPLAY_TIME, 20);
        aggregator.addOrMerge(PickupKey.experience(), "Experience", 5, DISPLAY_TIME, 20);
        assertEquals(1, aggregator.size());
        assertEquals(8, aggregator.snapshot().get(0).count());
    }

    @Test
    void entriesExpireAfterDisplayTime() {
        PickupAggregator aggregator = new PickupAggregator();
        aggregator.addOrMerge(PickupKey.item("minecraft:dirt", "minecraft:dirt"), "Dirt", 1, 5, 20);
        for (int i = 0; i < 4; i++) {
            aggregator.tick();
            assertEquals(1, aggregator.size());
        }
        aggregator.tick();
        assertTrue(aggregator.isEmpty());
    }

    @Test
    void fullListEvictsOldest() {
        PickupAggregator aggregator = new PickupAggregator();
        for (int i = 0; i < 5; i++) {
            aggregator.addOrMerge(PickupKey.item("item:" + i, "item:" + i), "Item " + i, 1,
                    DISPLAY_TIME, 3);
        }
        assertEquals(3, aggregator.size());
        List<PickupEntry> snapshot = aggregator.snapshot();
        assertEquals("Item 2", snapshot.get(0).displayName());
        assertEquals("Item 4", snapshot.get(2).displayName());
    }

    @Test
    void mergedEntryMovesToEnd() {
        PickupAggregator aggregator = new PickupAggregator();
        PickupKey first = PickupKey.item("item:first", "item:first");
        aggregator.addOrMerge(first, "First", 1, DISPLAY_TIME, 20);
        aggregator.addOrMerge(PickupKey.item("item:second", "item:second"), "Second", 1,
                DISPLAY_TIME, 20);
        aggregator.addOrMerge(first, "First", 1, DISPLAY_TIME, 20);
        List<PickupEntry> snapshot = aggregator.snapshot();
        assertEquals("Second", snapshot.get(0).displayName());
        assertEquals("First", snapshot.get(1).displayName());
        assertEquals(2, snapshot.get(1).count());
    }

    @Test
    void invalidMaxEntriesRejected() {
        PickupAggregator aggregator = new PickupAggregator();
        assertThrows(IllegalArgumentException.class, () -> aggregator.addOrMerge(
                PickupKey.item("minecraft:dirt", "minecraft:dirt"), "Dirt", 1, DISPLAY_TIME, 0));
    }

    @Test
    void largeBatchKeepsAccurateTotals() {
        PickupAggregator aggregator = new PickupAggregator();
        for (int i = 0; i < 10_000; i++) {
            PickupKey key = PickupKey.item("item:" + (i % 12), "item:" + (i % 12));
            aggregator.addOrMerge(key, "Item", 1, DISPLAY_TIME, 20);
        }
        assertEquals(12, aggregator.size());
        int total = aggregator.snapshot().stream().mapToInt(PickupEntry::count).sum();
        assertEquals(10_000, total);
    }
}
