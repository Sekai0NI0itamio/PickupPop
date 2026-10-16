// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at http://mozilla.org/MPL/2.0/.

package com.pickuppop.client;

import com.pickuppop.config.PickupPopConfig;
import com.pickuppop.model.PickupEntry;
import com.pickuppop.model.PickupKey;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * HUD renderer for pickup notifications.
 *
 * <p>Hot-path discipline (the whole method runs every frame):
 * <ul>
 *   <li>Config is snapshotted into locals once per frame; upstream reads the
 *       config holder dozens of times per entry per frame.</li>
 *   <li>Display text and its measured width are cached per entry and rebuilt
 *       only when the merged count changes; upstream re-measures
 *       ({@code font.width}) several times per entry per frame.</li>
 *   <li>Layout math uses the cached widths; no streams, no per-frame
 *       collections beyond the aggregator snapshot.</li>
 *   <li>Item sprites render directly with full opacity. Upstream routes every
 *       sprite through an extra full-screen framebuffer
 *       ({@code TransparencyBuffer}) per entry per frame to fade icons; that
 *       costs a framebuffer bind, a full-screen blit and several matrix
 *       pushes per icon. Text still fades; icons popping out with the entry
 *       is the documented visual difference.</li>
 * </ul>
 */
public final class PickupHudRenderer {
    static final int ENTRY_HEIGHT = 18;
    private static final int TEXT_ITEM_MARGIN = 4;
    private static final int SPRITE_SIZE = 16;
    private static final Identifier EXPERIENCE_ORB_TEXTURE =
            Identifier.of("minecraft", "textures/entity/experience_orb.png");

    private final PickupPopConfig config;
    private final PickupClientEvents events;
    private final Map<PickupKey, CachedRow> rows = new HashMap<>();

    public PickupHudRenderer(PickupPopConfig config, PickupClientEvents events) {
        this.config = config;
        this.events = events;
    }

    public void onHudRender(DrawContext drawContext, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) {
            return;
        }
        List<PickupEntry> visible = this.events.aggregator().snapshot();
        if (visible.isEmpty()) {
            this.rows.clear();
            this.events.pruneStacks(visible);
            return;
        }

        // Snapshot config once per frame.
        final float scale = this.config.scale / 6.0F;
        final PickupPopConfig.Position position = this.config.position;
        final int offsetX = (int) (this.config.offsetX / scale);
        final int offsetY = (int) (this.config.offsetY / scale);
        final boolean moveOut = this.config.moveOut;
        final boolean fadeAway = this.config.fadeAway;
        final boolean drawSprite = this.config.drawSprite;
        final boolean mirrored = position.mirrored();
        final boolean bottom = position.bottom();

        final int screenWidth = (int) (drawContext.getScaledWindowWidth() / scale);
        final int screenHeight = (int) (drawContext.getScaledWindowHeight() / scale);
        final float partialTicks = tickCounter.getTickDelta(false);
        final int offset = position.getY(ENTRY_HEIGHT, screenHeight, offsetY);
        final double totalFade = moveOut
                ? this.events.aggregator().totalSlideProgress(partialTicks, this.config.moveTime,
                        this.config.displayTime) * ENTRY_HEIGHT
                : 0.0;

        TextRenderer textRenderer = client.textRenderer;
        this.refreshRows(client, visible);

        int entryY = offset + (bottom ? (int) totalFade : -(int) totalFade);
        int step = bottom ? ENTRY_HEIGHT : -ENTRY_HEIGHT;
        for (PickupEntry entry : visible) {
            boolean mayRender = bottom ? entryY < offset + step : entryY > offset + step;
            if (mayRender) {
                CachedRow row = this.rows.get(entry.key());
                int entryX = position.getX(row.width, screenWidth, offsetX);
                float alpha = moveOut
                        ? clamp((float) (entryY - offset) / step, 0.0F, 1.0F)
                        : PickupEntry.slideProgress(entry.remainingTicks(), partialTicks,
                                Math.min(this.config.moveTime, this.config.displayTime));
                this.renderEntry(client, drawContext, textRenderer, entry, row, entryX, entryY,
                        alpha, scale, mirrored, drawSprite, fadeAway);
            }
            entryY -= step;
        }

        this.events.pruneStacks(visible);
        Set<PickupKey> retained = new HashSet<>();
        for (PickupEntry entry : visible) {
            retained.add(entry.key());
        }
        this.rows.keySet().retainAll(retained);
    }

    private void refreshRows(MinecraftClient client, List<PickupEntry> visible) {
        for (PickupEntry entry : visible) {
            CachedRow row = this.rows.get(entry.key());
            if (row == null || row.count != entry.count()) {
                String text = this.buildText(client, entry);
                int width = client.textRenderer.getWidth(text);
                if (this.config.drawSprite) {
                    width += (text.isEmpty() ? 0 : TEXT_ITEM_MARGIN) + SPRITE_SIZE;
                }
                this.rows.put(entry.key(), new CachedRow(entry.count(), text, width));
            }
        }
    }

    /**
     * Builds the display string; mirrors upstream entry text composition
     * (inventory count, name, amount), reversed for mirrored corners.
     */
    String buildText(MinecraftClient client, PickupEntry entry) {
        java.util.List<String> parts = new java.util.ArrayList<>(3);
        if (this.config.inventoryCount) {
            int inInventory = this.events.inventoryCount(client, entry);
            if (inInventory > 0) {
                parts.add("(" + inInventory + ")");
            }
        }
        if (this.config.displayItemName) {
            parts.add(entry.displayName());
        }
        if (this.config.displayAmount.text()
                && (entry.count() > 1 || entry.count() == 1 && this.config.displaySingleCount)) {
            parts.add(this.config.position.mirrored() ? entry.count() + "x" : "x" + entry.count());
        }
        if (this.config.position.mirrored()) {
            java.util.Collections.reverse(parts);
        }
        return String.join(" ", parts);
    }

    private void renderEntry(MinecraftClient client, DrawContext drawContext,
            TextRenderer textRenderer, PickupEntry entry, CachedRow row, int posX, int posY,
            float alpha, float scale, boolean mirrored, boolean drawSprite, boolean fadeAway) {
        ItemStack stack = entry.key().kind() == PickupKey.Kind.ITEM
                ? this.events.latestStack(entry.key())
                : null;

        drawContext.getMatrices().push();
        drawContext.getMatrices().scale(scale, scale, 1.0F);

        this.renderBackground(client, drawContext, posX, posY, row.width, alpha);

        int fadeTime = fadeAway ? 255 - (int) (255.0F * alpha) : 255;
        // Skip fully faded text (avoids a one-frame full-alpha flash at expiry).
        if (fadeTime >= 5) {
            int color = this.textColor(stack) | fadeTime << 24;
            int textX = mirrored || !drawSprite ? posX : posX + SPRITE_SIZE + TEXT_ITEM_MARGIN;
            drawContext.drawText(textRenderer, row.text, textX, posY + 4, color, true);
            if (drawSprite) {
                int spriteX = mirrored ? posX + row.width - SPRITE_SIZE : posX;
                if (stack != null) {
                    drawContext.drawItem(stack, spriteX, posY);
                    if (this.config.displayAmount.sprite()) {
                        this.renderSpriteCount(drawContext, textRenderer, entry, spriteX, posY);
                    }
                } else {
                    this.renderExperienceSprite(drawContext, entry, spriteX, posY);
                }
            }
        }

        drawContext.getMatrices().pop();
    }

    private void renderBackground(MinecraftClient client, DrawContext drawContext, int posX,
            int posY, int width, float alpha) {
        if (this.config.entryBackground != PickupPopConfig.EntryBackground.CHAT) {
            return;
        }
        double opacity = client.options.getTextBackgroundOpacity().getValue();
        int bgAlpha = (int) (opacity * (1.0F - alpha) * 255.0F) << 24;
        int endY = posY + SPRITE_SIZE + (this.config.displayAmount.sprite() ? 1 : 0);
        drawContext.fill(posX - 3, posY, posX + width + 5, endY, bgAlpha);
    }

    private void renderSpriteCount(DrawContext drawContext, TextRenderer textRenderer,
            PickupEntry entry, int spriteX, int spriteY) {
        if (entry.count() <= 1 && !this.config.displaySingleCount) {
            return;
        }
        String count = shorten(entry.count());
        int countWidth = textRenderer.getWidth(count);
        drawContext.drawText(textRenderer, count, spriteX + SPRITE_SIZE + 1 - countWidth,
                spriteY + SPRITE_SIZE - 7, 0xFFFFFF, true);
    }

    private void renderExperienceSprite(DrawContext drawContext, PickupEntry entry, int x, int y) {
        int frame = xpFrame(entry.count());
        int u = frame % 4 * SPRITE_SIZE;
        int v = frame / 4 * SPRITE_SIZE;
        drawContext.drawTexture(EXPERIENCE_ORB_TEXTURE, x, y, u, v, SPRITE_SIZE, SPRITE_SIZE, 64, 64);
    }

    /** XP orb sprite frame by value; same thresholds as vanilla ExperienceOrb. */
    static int xpFrame(int value) {
        if (value >= 2477) {
            return 10;
        } else if (value >= 1237) {
            return 9;
        } else if (value >= 617) {
            return 8;
        } else if (value >= 307) {
            return 7;
        } else if (value >= 149) {
            return 6;
        } else if (value >= 73) {
            return 5;
        } else if (value >= 37) {
            return 4;
        } else if (value >= 17) {
            return 3;
        } else if (value >= 7) {
            return 2;
        }
        return value >= 3 ? 1 : 0;
    }

    static String shorten(int value) {
        if (value >= 1_000_000_000) {
            return value / 1_000_000_000 + "B";
        } else if (value >= 1_000_000) {
            return value / 1_000_000 + "M";
        } else if (value >= 1_000) {
            return value / 1_000 + "K";
        }
        return String.valueOf(value);
    }

    private int textColor(ItemStack stack) {
        if (!this.config.ignoreRarity && stack != null && stack.getRarity() != Rarity.COMMON) {
            Integer rarityColor = stack.getRarity().getFormatting().getColorValue();
            if (rarityColor != null) {
                return rarityColor;
            }
        }
        return this.config.textColor;
    }

    private static float clamp(float value, float min, float max) {
        return value < min ? min : Math.min(value, max);
    }

    private record CachedRow(int count, String text, int width) {
    }
}
