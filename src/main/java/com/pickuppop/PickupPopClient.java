// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at http://mozilla.org/MPL/2.0/.

package com.pickuppop;

import com.pickuppop.client.PickupClientEvents;
import com.pickuppop.client.PickupHudRenderer;
import com.pickuppop.config.PickupPopConfig;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Client-only entrypoint. No server component, no networking beyond reading
 * vanilla take-item packets, no background threads.
 */
public final class PickupPopClient implements ClientModInitializer {
    public static final String MOD_ID = "pickuppop";
    public static final Logger LOGGER = LoggerFactory.getLogger("PickupPop");

    private static PickupPopConfig config;
    private static PickupClientEvents events;

    @Override
    public void onInitializeClient() {
        config = PickupPopConfig.load();
        // TEMP diagnostics (issue: no notifications in-game): one init line so
        // the log proves the mod loaded; per-pickup lines live at the gates
        // in PickupClientEvents/PickupHudRenderer. Downgrade after diagnosis.
        LOGGER.info("[pickuppop] initialized: items={} xp={} arrows={} combine={}",
                config.includeItems, config.includeExperience, config.includeArrows,
                config.combineEntries);
        PickupClientEvents pickupEvents = new PickupClientEvents(config);
        events = pickupEvents;
        PickupHudRenderer renderer = new PickupHudRenderer(config, pickupEvents);
        ClientTickEvents.END_CLIENT_TICK.register(pickupEvents::onEndTick);
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> pickupEvents.onDisconnect());
        HudRenderCallback.EVENT.register(renderer::onHudRender);
    }

    public static PickupPopConfig config() {
        return config;
    }

    public static PickupClientEvents events() {
        return events;
    }
}
