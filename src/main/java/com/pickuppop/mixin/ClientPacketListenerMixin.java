// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at http://mozilla.org/MPL/2.0/.

package com.pickuppop.mixin;

import com.pickuppop.PickupPopClient;
import com.pickuppop.client.PickupClientEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.ItemPickupAnimationS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Reports vanilla take-item packets (items, experience orbs and recovered
 * arrows all arrive through this packet) to the pickup tracker.
 *
 * <p>Injection is immediately after the same-thread check, mirroring the
 * upstream mixin. This is load-bearing: vanilla decrements the stack and
 * discards fully-collected entities later in the handler, so a TAIL
 * injection sees {@code getEntityById} return null and silently drops every
 * pickup (diagnosed via gate logs: {@code resolved: null}).
 */
@Mixin(ClientPlayNetworkHandler.class)
public abstract class ClientPacketListenerMixin {
    @Inject(method = "onItemPickupAnimation", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/NetworkThreadUtils;forceMainThread(Lnet/minecraft/network/packet/Packet;Lnet/minecraft/network/listener/PacketListener;Lnet/minecraft/util/thread/ThreadExecutor;)V", shift = At.Shift.AFTER))
    private void pickuppop$onItemPickup(ItemPickupAnimationS2CPacket packet, CallbackInfo ci) {
        PickupClientEvents events = PickupPopClient.events();
        if (events != null) {
            // Injected at the tail, so this runs on the client thread after
            // vanilla processing; no shadowed client field needed.
            events.onEntityPickup(MinecraftClient.getInstance(), packet.getEntityId(),
                    packet.getCollectorEntityId(), packet.getStackAmount());
        }
    }
}
