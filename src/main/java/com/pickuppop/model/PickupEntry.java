// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at http://mozilla.org/MPL/2.0/.

package com.pickuppop.model;

/**
 * One visible pickup notification: what was collected, how many, and how long
 * it stays on screen.
 *
 * <p>Pure logic, no Minecraft types, so it is fully unit-testable. Rendering
 * state (cached text, measured width) lives in the renderer, keyed by entry,
 * and is rebuilt only when {@link #count} changes.
 */
public final class PickupEntry {
    private final PickupKey key;
    private final String displayName;
    private int count;
    private int remainingTicks;

    public PickupEntry(PickupKey key, String displayName, int amount, int displayTime) {
        if (amount <= 0) {
            throw new IllegalArgumentException("amount must be positive, was " + amount);
        }
        if (displayTime < 0) {
            throw new IllegalArgumentException("displayTime must not be negative");
        }
        this.key = key;
        this.displayName = displayName;
        this.count = amount;
        this.remainingTicks = displayTime;
    }

    public PickupKey key() {
        return this.key;
    }

    public String displayName() {
        return this.displayName;
    }

    public int count() {
        return this.count;
    }

    public int remainingTicks() {
        return this.remainingTicks;
    }

    public boolean mayDiscard() {
        return this.remainingTicks <= 0;
    }

    public void tick() {
        if (this.remainingTicks > 0) {
            this.remainingTicks--;
        }
    }

    /** Adds more pickups to this entry and restarts its lifetime. */
    public void mergeWith(int amount, int displayTime) {
        if (amount <= 0) {
            throw new IllegalArgumentException("amount must be positive, was " + amount);
        }
        this.count += amount;
        this.remainingTicks = displayTime;
    }

    /**
     * Fraction of the slide-out animation completed, mirroring upstream
     * {@code DisplayEntry#getRemainingTicksRelative}: 0 while fully shown,
     * approaching 1 as the entry leaves the screen.
     */
    public static float slideProgress(int remainingTicks, float partialTicks, int moveTime) {
        if (moveTime <= 0) {
            return 1.0F;
        }
        float ratio = (remainingTicks - partialTicks) / moveTime;
        float clamped = ratio < 0.0F ? 0.0F : Math.min(ratio, 1.0F);
        return 1.0F - clamped;
    }
}
