// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at http://mozilla.org/MPL/2.0/.

package com.pickuppop;

import com.pickuppop.model.PickupEntry;
import com.pickuppop.model.PickupKey;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PickupEntryTest {
    @Test
    void mergeAccumulatesAndResetsLifetime() {
        PickupEntry entry = new PickupEntry(PickupKey.item("minecraft:dirt", "x"), "Dirt", 2, 80);
        for (int i = 0; i < 30; i++) {
            entry.tick();
        }
        assertEquals(50, entry.remainingTicks());
        entry.mergeWith(3, 80);
        assertEquals(5, entry.count());
        assertEquals(80, entry.remainingTicks());
        assertFalse(entry.mayDiscard());
    }

    @Test
    void expiryReachesDiscard() {
        PickupEntry entry = new PickupEntry(PickupKey.experience(), "Experience", 4, 2);
        assertFalse(entry.mayDiscard());
        entry.tick();
        entry.tick();
        assertTrue(entry.mayDiscard());
        entry.tick();
        assertTrue(entry.mayDiscard());
        assertEquals(0, entry.remainingTicks());
    }

    @Test
    void nonPositiveAmountsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new PickupEntry(PickupKey.experience(), "XP", 0, 80));
        PickupEntry entry = new PickupEntry(PickupKey.experience(), "XP", 1, 80);
        assertThrows(IllegalArgumentException.class, () -> entry.mergeWith(0, 80));
    }

    @Test
    void slideProgressBounds() {
        assertEquals(0.0F, PickupEntry.slideProgress(20, 0.0F, 20), 0.0001F);
        assertEquals(1.0F, PickupEntry.slideProgress(0, 0.0F, 20), 0.0001F);
        assertEquals(0.5F, PickupEntry.slideProgress(10, 0.0F, 20), 0.0001F);
        assertEquals(1.0F, PickupEntry.slideProgress(5, 0.0F, 0), 0.0001F);
    }

    @Test
    void experienceKeyIsShared() {
        assertEquals(PickupKey.experience(), PickupKey.experience());
        assertEquals(PickupKey.Kind.EXPERIENCE, PickupKey.experience().kind());
    }
}
