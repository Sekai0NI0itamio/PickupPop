// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at http://mozilla.org/MPL/2.0/.

package com.pickuppop.mixin;

import com.pickuppop.PickupPopClient;
import com.pickuppop.client.PickupClientEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.ItemPickupAnimationS2CPacket;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Reports vanilla take-item packets (items, experience orbs and recovered
 * arrows all arrive through this packet) to the pickup tracker.
 *
 * <p>Injection is at the tail of the handler so it runs on the client thread
 * after vanilla processing, mirroring the upstream mixin which injects after
 * the same-thread check.
 */
@Mixin(ClientPlayNetworkHandler.class)
public abstract class ClientPacketListenerMixin {
    @Shadow
    @Final
    private MinecraftClient client;

    @Inject(method = "onItemPickupAnimation", at = @At("TAIL"))
    private void pickuppop$onItemPickup(ItemPickupAnimationS2CPacket packet, CallbackInfo ci) {
        PickupClientEvents events = PickupPopClient.events();
        if (events != null) {
            events.onEntityPickup(this.client, packet.getEntityId(), packet.getPlayerId(),
                    packet.getStackAmount());
        }
    }
}
