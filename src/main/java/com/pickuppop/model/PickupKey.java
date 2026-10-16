// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at http://mozilla.org/MPL/2.0/.

package com.pickuppop.model;

import java.util.Objects;

/**
 * Aggregation key for pickup notifications.
 *
 * <p>Two pickups merge into one notification entry only when their keys are
 * equal. Item identity is the registry id plus a label whose content depends
 * on the combine mode (see the client bridge): {@code ALWAYS} ignores custom
 * names, {@code EXCLUDE_NAMED} keeps differently named stacks apart, and
 * {@code NEVER} is implemented by the caller with a unique label per pickup.
 * All experience pickups share a single key, matching upstream behavior where
 * every experience entry merges with every other one.
 */
public record PickupKey(Kind kind, String id, String label) {

    public enum Kind {
        ITEM,
        EXPERIENCE
    }

    public PickupKey {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(label, "label");
    }

    public static PickupKey item(String itemId, String label) {
        return new PickupKey(Kind.ITEM, itemId, label);
    }

    /** Single shared key: all experience pickups merge into one entry. */
    public static PickupKey experience() {
        return new PickupKey(Kind.EXPERIENCE, "experience", "");
    }
}
