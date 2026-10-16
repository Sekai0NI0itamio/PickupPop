// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at http://mozilla.org/MPL/2.0/.

package com.pickuppop.client;

import com.pickuppop.collect.PickupAggregator;
import com.pickuppop.config.PickupPopConfig;
import com.pickuppop.model.PickupEntry;
import com.pickuppop.model.PickupKey;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Client-side pickup detection and aggregation.
 *
 * <p>Detection is event-driven: a single mixin on the client play handler
 * reports vanilla take-item packets (items, experience orbs, recovered
 * arrows all arrive through that packet), so there is no inventory polling
 * and no per-tick scanning. Aggregation itself is pure
 * {@link PickupAggregator} logic; this class only translates Minecraft types
 * into keys at event time and expires entries on the client tick.
 */
public final class PickupClientEvents {
    /** Ticks a duplicate-suppression id is remembered (mirrors upstream). */
    static final int HANDLED_TTL_TICKS = 80;
    /** Safety bound so the suppression map cannot grow without limit. */
    static final int HANDLED_MAX_IDS = 1024;

    private final PickupPopConfig config;
    private final PickupAggregator aggregator = new PickupAggregator();
    private final Map<Integer, Integer> handledEntities = new HashMap<>();
    /** Latest seen stack per item key, for sprite/rarity/count rendering. */
    private final Map<PickupKey, ItemStack> latestStacks = new HashMap<>();
    private final AtomicLong uniqueCounter = new AtomicLong();

    public PickupClientEvents(PickupPopConfig config) {
        this.config = config;
    }

    public PickupAggregator aggregator() {
        return this.aggregator;
    }

    /** Called by the play-handler mixin for every vanilla take-item packet. */
    public void onEntityPickup(MinecraftClient client, int entityId, int playerId, int amount) {
        if (client.world == null || client.player == null) {
            return;
        }
        if (!(client.world.getEntityById(playerId) instanceof ClientPlayerEntity)) {
            return;
        }
        if (this.handledEntities.containsKey(entityId)) {
            return;
        }
        this.resolvePickup(client, entityId, amount);
    }

    private void resolvePickup(MinecraftClient client, int entityId, int amount) {
        Entity entity = client.world.getEntityById(entityId);
        if (entity instanceof ItemEntity item) {
            if (this.config.includeItems) {
                this.addItem(client, item.getStack(), amount);
            }
        } else if (entity instanceof PersistentProjectileEntity arrow) {
            if (this.config.includeArrows) {
                this.addItem(client, arrow.getItemStack(), amount);
            }
        } else if (entity instanceof ExperienceOrbEntity orb) {
            if (this.config.includeExperience && orb.getExperienceAmount() > 0) {
                int shown = this.config.experienceAsValue ? orb.getExperienceAmount() : amount;
                this.addEntry(client, PickupKey.experience(), orb.getName().getString(), shown, null);
            }
        }
    }

    private void addItem(MinecraftClient client, ItemStack stack, int amount) {
        if (stack == null || stack.isEmpty() || amount <= 0) {
            return;
        }
        String itemId = Registries.ITEM.getId(stack.getItem()).toString();
        String label = this.stackLabel(stack, itemId);
        PickupKey key = PickupKey.item(itemId, label);
        // Copy once at event time; never touch the live entity stack afterwards.
        // Note: upstream strips the enchantment glint in ALWAYS mode; PickupPop
        // keeps the copied stack as-is to avoid an extra component write per
        // pickup (documented in docs/PERFORMANCE.md).
        this.addEntry(client, key, label, amount, stack.copy());
    }

    /**
     * Label used for both the aggregation key and the shown name. Custom item
     * names intentionally participate: {@code ALWAYS} ignores them (base item
     * name), {@code EXCLUDE_NAMED} keeps renamed stacks apart, {@code NEVER}
     * gets a unique label per pickup from the caller.
     */
    String stackLabel(ItemStack stack, String itemId) {
        if (this.config.combineEntries == PickupPopConfig.CombineMode.ALWAYS) {
            return stack.getItem().getName().getString();
        }
        if (stack.hasCustomName()) {
            return stack.getName().getString();
        }
        return itemId;
    }

    private void addEntry(MinecraftClient client, PickupKey key, String displayName, int amount,
            ItemStack stack) {
        if (client.player.getAbilities().creativeMode && this.config.disableInCreative) {
            return;
        }
        PickupKey effectiveKey = key;
        if (this.config.combineEntries == PickupPopConfig.CombineMode.NEVER
                && key.kind() == PickupKey.Kind.ITEM) {
            effectiveKey = PickupKey.item(key.id(), key.label() + "#" + this.uniqueCounter.incrementAndGet());
        }
        int cap = this.visibleCap(client);
        this.aggregator.addOrMerge(effectiveKey, displayName, amount, this.config.displayTime, cap);
        if (stack != null) {
            this.latestStacks.put(effectiveKey, stack);
        }
    }

    /**
     * Visible-entry budget: the screen-height-derived bound (as upstream)
     * clamped by the hard {@code maxEntries} cap. Computed once per pickup,
     * never per frame.
     */
    int visibleCap(MinecraftClient client) {
        float scale = this.config.scale / 6.0F;
        int scaledHeight = (int) (client.getWindow().getScaledHeight() / scale);
        int fromHeight = (int) (scaledHeight * this.config.maxHeight / PickupEntryHeight.ENTRY_HEIGHT) - 1;
        return Math.min(Math.max(1, fromHeight), this.config.maxEntries);
    }

    public ItemStack latestStack(PickupKey key) {
        return this.latestStacks.get(key);
    }

    /** Counts matching stacks in the player inventory (only when enabled). */
    public int inventoryCount(MinecraftClient client, PickupEntry entry) {
        if (!this.config.inventoryCount || client.player == null) {
            return 0;
        }
        int total = 0;
        for (ItemStack stack : client.player.getInventory().main) {
            if (!stack.isEmpty() && this.matchesEntry(stack, entry)) {
                total += stack.getCount();
            }
        }
        for (ItemStack stack : client.player.getInventory().offHand) {
            if (!stack.isEmpty() && this.matchesEntry(stack, entry)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    private boolean matchesEntry(ItemStack stack, PickupEntry entry) {
        if (entry.key().kind() != PickupKey.Kind.ITEM) {
            return false;
        }
        String itemId = Registries.ITEM.getId(stack.getItem()).toString();
        if (!itemId.equals(entry.key().id())) {
            return false;
        }
        return this.config.combineEntries == PickupPopConfig.CombineMode.ALWAYS
                || this.stackLabel(stack, itemId).equals(entry.displayName());
    }

    /** Client tick: age duplicate-suppression ids and expire entries. */
    public void onEndTick(MinecraftClient client) {
        if (client.isPaused()) {
            return;
        }
        if (!this.handledEntities.isEmpty()) {
            this.handledEntities.replaceAll((id, age) -> age + 1);
            this.handledEntities.values().removeIf(age -> age > HANDLED_TTL_TICKS);
            if (this.handledEntities.size() > HANDLED_MAX_IDS) {
                this.handledEntities.clear();
            }
        }
        if (this.config.displayTime != 0 && !this.aggregator.isEmpty()) {
            this.aggregator.tick();
        }
    }

    /** World change / disconnect must not leave stale notifications behind. */
    public void onDisconnect() {
        this.aggregator.clear();
        this.handledEntities.clear();
        this.latestStacks.clear();
    }

    /** Drops cached stacks whose entries are no longer visible. */
    public void pruneStacks(List<PickupEntry> visible) {
        if (this.latestStacks.isEmpty()) {
            return;
        }
        Set<PickupKey> retained = new HashSet<>();
        for (PickupEntry entry : visible) {
            retained.add(entry.key());
        }
        this.latestStacks.keySet().retainAll(retained);
    }

    /** Entry height in HUD pixels; kept beside the renderer constant. */
    static final class PickupEntryHeight {
        static final int ENTRY_HEIGHT = 18;

        private PickupEntryHeight() {
        }
    }
}
