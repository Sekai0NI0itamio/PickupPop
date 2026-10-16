// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at http://mozilla.org/MPL/2.0/.

package com.pickuppop;

import com.pickuppop.config.PickupPopConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PickupPopConfigTest {
    @TempDir
    Path tempDir;

    @Test
    void defaultsMatchUpstreamBehavior() {
        PickupPopConfig config = PickupPopConfig.defaults();
        assertTrue(config.includeItems);
        assertTrue(config.includeExperience);
        assertTrue(config.includeArrows);
        assertTrue(config.experienceAsValue);
        assertEquals(PickupPopConfig.CombineMode.EXCLUDE_NAMED, config.combineEntries);
        assertEquals(80, config.displayTime);
        assertTrue(config.moveOut);
        assertEquals(20, config.moveTime);
        assertTrue(config.fadeAway);
        assertTrue(config.drawSprite);
        assertEquals(PickupPopConfig.EntryBackground.NONE, config.entryBackground);
        // Intentional difference from upstream (BOTTOM_RIGHT): left-edge list.
        assertEquals(PickupPopConfig.Position.TOP_LEFT, config.position);
        assertEquals(0.5, config.maxHeight);
    }

    @Test
    void roundTripPreservesEditedValues() {
        Path file = tempDir.resolve("pickuppop.json");
        PickupPopConfig config = PickupPopConfig.defaults();
        config.displayTime = 120;
        config.position = PickupPopConfig.Position.TOP_LEFT;
        config.maxEntries = 7;
        config.save(file);

        PickupPopConfig reloaded = PickupPopConfig.load(file);
        assertEquals(120, reloaded.displayTime);
        assertEquals(PickupPopConfig.Position.TOP_LEFT, reloaded.position);
        assertEquals(7, reloaded.maxEntries);
    }

    @Test
    void missingFileCreatesDefaults() {
        Path file = tempDir.resolve("pickuppop.json");
        PickupPopConfig config = PickupPopConfig.load(file);
        assertEquals(80, config.displayTime);
        assertTrue(Files.isRegularFile(file));
    }

    @Test
    void malformedFileFallsBackToDefaultsAndBacksUp() throws Exception {
        Path file = tempDir.resolve("pickuppop.json");
        Files.writeString(file, "{not valid json");
        PickupPopConfig config = PickupPopConfig.load(file);
        assertEquals(80, config.displayTime);
        assertTrue(Files.isRegularFile(tempDir.resolve("pickuppop.json.bak")));
    }

    @Test
    void outOfRangeValuesAreClamped() {
        Path file = tempDir.resolve("pickuppop.json");
        PickupPopConfig config = PickupPopConfig.defaults();
        config.displayTime = -5;
        config.maxEntries = 10_000;
        config.scale = 99;
        config.maxHeight = 5.0;
        config.save(file);

        PickupPopConfig reloaded = PickupPopConfig.load(file);
        assertEquals(0, reloaded.displayTime);
        assertEquals(100, reloaded.maxEntries);
        assertEquals(24, reloaded.scale);
        assertEquals(1.0, reloaded.maxHeight);
    }
}
