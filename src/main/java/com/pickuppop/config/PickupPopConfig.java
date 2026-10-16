// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at http://mozilla.org/MPL/2.0/.

package com.pickuppop.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * File-backed configuration, deliberately dependency-free (plain Gson JSON,
 * no config library) to keep the mod lightweight.
 *
 * <p>Options mirror the upstream {@code ClientConfig} groups (general /
 * behavior / display) with the same defaults. Loaded once at client startup
 * and on explicit reload; the file is written only then, never per tick or
 * per pickup. All reads during tick/render use the in-memory instance.
 */
public final class PickupPopConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "pickuppop.json";

    public boolean includeItems = true;
    public boolean includeExperience = true;
    public boolean includeArrows = true;
    /** Show total XP value instead of the orb count. */
    public boolean experienceAsValue = true;
    public boolean disableInCreative = false;

    public CombineMode combineEntries = CombineMode.EXCLUDE_NAMED;
    /** Ticks each entry stays visible. 0 means entries only leave when evicted. */
    public int displayTime = 80;
    public boolean moveOut = true;
    public int moveTime = 20;
    public boolean fadeAway = true;

    public boolean drawSprite = true;
    public boolean displayItemName = true;
    public DisplayAmount displayAmount = DisplayAmount.TEXT;
    public boolean displaySingleCount = true;
    /** Adds the total inventory count in brackets; off by default (costs a scan). */
    public boolean inventoryCount = false;
    public int textColor = 0xFFFFFF;
    public boolean ignoreRarity = false;
    /** Background behind entries. Upstream default is NONE; CHAT matches chat opacity. */
    public EntryBackground entryBackground = EntryBackground.NONE;
    public Position position = Position.BOTTOM_RIGHT;
    public int offsetX = 8;
    public int offsetY = 4;
    /** Fraction of the screen height the list may fill. */
    public double maxHeight = 0.5;
    /** Entry scale divisor base (upstream uses scale / 6.0F). */
    public int scale = 4;
    /** Hard cap on simultaneously visible entries; prevents unbounded growth. */
    public int maxEntries = 20;

    public enum CombineMode {
        ALWAYS,
        NEVER,
        EXCLUDE_NAMED
    }

    public enum EntryBackground {
        NONE,
        CHAT
    }

    public enum DisplayAmount {
        OFF,
        SPRITE,
        TEXT,
        BOTH;

        public boolean sprite() {
            return this == SPRITE || this == BOTH;
        }

        public boolean text() {
            return this == TEXT || this == BOTH;
        }
    }

    public enum Position {
        TOP_LEFT(false, false),
        TOP_RIGHT(true, false),
        BOTTOM_LEFT(false, true),
        BOTTOM_RIGHT(true, true);

        private final boolean mirrored;
        private final boolean bottom;

        Position(boolean mirrored, boolean bottom) {
            this.mirrored = mirrored;
            this.bottom = bottom;
        }

        public boolean mirrored() {
            return this.mirrored;
        }

        public boolean bottom() {
            return this.bottom;
        }

        public int getX(int entryWidth, int scaledWidth, int offset) {
            return Math.abs((scaledWidth - entryWidth) * (this.mirrored ? 1 : 0) - offset);
        }

        public int getY(int entryHeight, int scaledHeight, int offset) {
            return Math.abs((scaledHeight - entryHeight) * (this.bottom ? 1 : 0) - offset);
        }
    }

    public static PickupPopConfig defaults() {
        return new PickupPopConfig();
    }

    /** Loads {@code pickuppop.json} from the Fabric config dir. */
    public static PickupPopConfig load() {
        return load(FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME));
    }

    /** Loads from an explicit path (used by unit tests). */
    public static PickupPopConfig load(Path path) {
        if (!Files.isRegularFile(path)) {
            PickupPopConfig fresh = defaults();
            fresh.save(path);
            return fresh;
        }
        try (Reader reader = Files.newBufferedReader(path)) {
            PickupPopConfig parsed = GSON.fromJson(reader, PickupPopConfig.class);
            if (parsed == null) {
                return withBackup(path, "empty");
            }
            parsed.normalize();
            return parsed;
        } catch (IOException | JsonSyntaxException e) {
            return withBackup(path, "malformed");
        }
    }

    private static PickupPopConfig withBackup(Path path, String reason) {
        try {
            Path backup = path.resolveSibling(FILE_NAME + ".bak");
            Files.copy(path, backup, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ignored) {
            // Best effort: a broken config must never prevent startup.
        }
        PickupPopConfig fresh = defaults();
        fresh.save(path);
        return fresh;
    }

    /** Clamps out-of-range values so a hand-edited file cannot break layout. */
    void normalize() {
        if (this.combineEntries == null) {
            this.combineEntries = CombineMode.EXCLUDE_NAMED;
        }
        if (this.displayAmount == null) {
            this.displayAmount = DisplayAmount.TEXT;
        }
        if (this.position == null) {
            this.position = Position.BOTTOM_RIGHT;
        }
        if (this.entryBackground == null) {
            this.entryBackground = EntryBackground.NONE;
        }
        this.displayTime = Math.max(0, this.displayTime);
        this.moveTime = Math.max(0, this.moveTime);
        this.offsetX = Math.max(0, this.offsetX);
        this.offsetY = Math.max(0, this.offsetY);
        this.maxHeight = Math.min(1.0, Math.max(0.05, this.maxHeight));
        this.scale = Math.min(24, Math.max(1, this.scale));
        this.maxEntries = Math.min(100, Math.max(1, this.maxEntries));
        this.textColor &= 0xFFFFFF;
    }

    public void save() {
        save(FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME));
    }

    public void save(Path path) {
        try {
            Path parent = path.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            try (Writer writer = Files.newBufferedWriter(path)) {
                GSON.toJson(this, writer);
            }
        } catch (IOException ignored) {
            // Best effort: config persistence must never crash the client.
        }
    }
}
