// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at http://mozilla.org/MPL/2.0/.

package com.pickuppop.collect;

import com.pickuppop.model.PickupEntry;
import com.pickuppop.model.PickupKey;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Ordered set of live pickup notifications with O(1) merge lookup.
 *
 * <p>Upstream scans the whole entry list with a stream filter on every pickup
 * ({@code PickUpCollector#findDuplicate}). The workload is tiny, but the scan
 * also allocates a stream pipeline and lambdas per pickup. A
 * {@link LinkedHashMap} keyed by {@link PickupKey} gives constant-time merges
 * while preserving insertion order for rendering, and merged entries are
 * moved to the end in the same step (upstream does a separate
 * remove-plus-add {@code refresh}).
 */
public final class PickupAggregator {
    private final Map<PickupKey, PickupEntry> entries = new LinkedHashMap<>();

    /**
     * Adds a pickup, merging into the existing entry for the key when present.
     *
     * @param maxEntries cap on simultaneously visible entries; when full the
     *                   oldest entry is evicted. Must be at least 1.
     */
    public void addOrMerge(PickupKey key, String displayName, int amount, int displayTime,
            int maxEntries) {
        if (maxEntries < 1) {
            throw new IllegalArgumentException("maxEntries must be at least 1");
        }
        PickupEntry existing = this.entries.remove(key);
        if (existing != null) {
            existing.mergeWith(amount, displayTime);
            this.entries.put(key, existing);
            return;
        }
        if (this.entries.size() >= maxEntries) {
            this.entries.remove(this.entries.keySet().iterator().next());
        }
        this.entries.put(key, new PickupEntry(key, displayName, amount, displayTime));
    }

    /** Ages every entry by one tick and drops expired ones in a single pass. */
    public void tick() {
        this.entries.values().removeIf(entry -> {
            entry.tick();
            return entry.mayDiscard();
        });
    }

    /** Oldest-first snapshot for the renderer; the returned list is a copy. */
    public List<PickupEntry> snapshot() {
        return Collections.unmodifiableList(new ArrayList<>(this.entries.values()));
    }

    /**
     * Summed slide progress across entries, driving the collective slide-out
     * offset (mirrors upstream {@code PickUpCollector#getTotalFade}, which is
     * average&nbsp;&times;&nbsp;size). Returns 0 when empty. Computed with a
     * plain loop: no streams, no boxing, no allocation on the render path.
     */
    public double totalSlideProgress(float partialTicks, int moveTime, int displayTime) {
        if (this.entries.isEmpty()) {
            return 0.0;
        }
        int effectiveMove = Math.min(moveTime, displayTime);
        double sum = 0.0;
        for (PickupEntry entry : this.entries.values()) {
            sum += PickupEntry.slideProgress(entry.remainingTicks(), partialTicks, effectiveMove);
        }
        return sum;
    }

    public void clear() {
        this.entries.clear();
    }

    public int size() {
        return this.entries.size();
    }

    public boolean isEmpty() {
        return this.entries.isEmpty();
    }
}
