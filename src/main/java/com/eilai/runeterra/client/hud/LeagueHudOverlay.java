package com.eilai.runeterra.client.hud;

import com.eilai.runeterra.champion.LeagueXPHelper;
import com.eilai.runeterra.champion.PlayerChampionData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/**
 * Renders the League-style HUD overlay:
 *
 *  Bottom-center:
 *   [ Level badge ] [ XP bar ]
 *
 *  Bottom-center below XP bar:
 *   [ Q: rank ] [ W: rank ] [ E: rank ] [ R: rank ] [ D ] [ F ]
 *   (keys shown as E / R / T / F / C in Minecraft)
 *
 *  Only shown when player has selected a champion (not no_champion).
 */
@EventBusSubscriber(modid = "runeterra", value = Dist.CLIENT)
public class LeagueHudOverlay {

    // ── Layout ─────────────────────────────────────────────────────────────────
    private static final int BAR_W      = 180;
    private static final int BAR_H      = 8;
    private static final int BADGE_SIZE = 20;

    // Ability slot sizes
    private static final int SLOT_W     = 26;
    private static final int SLOT_H     = 26;
    private static final int SLOT_PAD   = 4;

    @SubscribeEvent
    public static void onRenderHud(RenderGuiLayerEvent.Post event) {
        if (!event.getName().equals(VanillaGuiLayers.EXPERIENCE_BAR)) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;

        Player player = mc.player;
        PlayerChampionData data = PlayerChampionData.get(player);

        // Don't render if no champion selected or it's "no_champion"
        if (data.getChampionId().equals("no_champion")) return;

        GuiGraphics gfx = event.getGuiGraphics();
        int screenW = mc.getWindow().getGuiScaledWidth();
        int screenH = mc.getWindow().getGuiScaledHeight();

        String champId = data.getChampionId();
        int level      = data.getLevel(champId);
        int xp         = data.getXP(champId);
        int xpNeeded   = level < 18 ? LeagueXPHelper.xpForLevel(level) : 1;
        float xpFrac   = level < 18 ? Math.min(1f, (float) xp / xpNeeded) : 1f;

        int barX = (screenW - BAR_W) / 2 + BADGE_SIZE + 4;
        int barY = screenH - 32;

        // ── Level badge ────────────────────────────────────────────────────
        int badgeX = (screenW - BAR_W) / 2;
        gfx.fill(badgeX, barY - 6, badgeX + BADGE_SIZE, barY - 6 + BADGE_SIZE, 0xFF8B6914);
        gfx.fill(badgeX + 1, barY - 5, badgeX + BADGE_SIZE - 1, barY - 6 + BADGE_SIZE - 1, 0xFF1A1A2E);
        String lvlStr = String.valueOf(level);
        gfx.drawCenteredString(mc.font,
                net.minecraft.network.chat.Component.literal("§e" + lvlStr),
                badgeX + BADGE_SIZE / 2, barY - 6 + BADGE_SIZE / 2 - 4, 0xFFFFFF);

        // ── XP bar background ──────────────────────────────────────────────
        gfx.fill(barX, barY, barX + BAR_W, barY + BAR_H, 0xFF111111);
        gfx.fill(barX + 1, barY + 1, barX + BAR_W - 1, barY + BAR_H - 1, 0xFF333333);

        // XP fill
        int fillW = (int) ((BAR_W - 2) * xpFrac);
        if (fillW > 0) {
            // Gradient: dark blue → bright cyan
            gfx.fill(barX + 1, barY + 1, barX + 1 + fillW, barY + BAR_H - 1, 0xFF00BFFF);
        }

        // Max level text
        if (level >= 18) {
            gfx.drawCenteredString(mc.font,
                    net.minecraft.network.chat.Component.literal("§6MAX"),
                    barX + BAR_W / 2, barY + 1, 0xFFFFFF);
        } else {
            // XP numbers
            String xpText = xp + " / " + xpNeeded;
            gfx.drawCenteredString(mc.font,
                    net.minecraft.network.chat.Component.literal("§7" + xpText),
                    barX + BAR_W / 2, barY + 1, 0xFFFFFF);
        }

        // ── Ability slots ──────────────────────────────────────────────────
        // Q→E, W→R, E→T, R→F, D→C
        String[] keys    = { "E", "R", "T", "F", "C" };
        int[]    ranks   = {
                data.getQRank(champId),
                data.getWRank(champId),
                data.getERank(champId),
                data.getRRank(champId),
                0 // D slot — no rank system
        };
        int[]    maxRanks = { 5, 5, 5, 3, 0 };

        int totalSlotsW = (SLOT_W + SLOT_PAD) * keys.length - SLOT_PAD;
        int slotStartX  = (screenW - totalSlotsW) / 2;
        int slotY       = barY + BAR_H + 4;

        int availablePoints = data.availableSkillPoints();

        for (int i = 0; i < keys.length; i++) {
            int sx = slotStartX + i * (SLOT_W + SLOT_PAD);

            // Slot background
            boolean isUlt   = i == 3;
            boolean hasPoint = availablePoints > 0 && (isUlt
                    ? LeagueXPHelper.canRankUltimate(level, ranks[3]) && ranks[3] < 3
                    : ranks[i] < maxRanks[i]);

            int slotBg = isUlt ? 0xFF2A0A4A : 0xFF0A1A2E;
            if (hasPoint) slotBg = isUlt ? 0xFF6A1A9A : 0xFF1A4A8A; // highlight if upgradeable

            gfx.fill(sx, slotY, sx + SLOT_W, slotY + SLOT_H, slotBg);
            gfx.fill(sx, slotY, sx + SLOT_W, slotY + 1,       isUlt ? 0xFFAA44FF : 0xFF4488FF);
            gfx.fill(sx, slotY + SLOT_H - 1, sx + SLOT_W, slotY + SLOT_H, isUlt ? 0xFFAA44FF : 0xFF4488FF);
            gfx.fill(sx, slotY, sx + 1, slotY + SLOT_H,       isUlt ? 0xFFAA44FF : 0xFF4488FF);
            gfx.fill(sx + SLOT_W - 1, slotY, sx + SLOT_W, slotY + SLOT_H, isUlt ? 0xFFAA44FF : 0xFF4488FF);

            // Key label
            gfx.drawCenteredString(mc.font,
                    net.minecraft.network.chat.Component.literal(
                            isUlt ? "§d" + keys[i] : "§b" + keys[i]),
                    sx + SLOT_W / 2, slotY + 3, 0xFFFFFF);

            // Rank pips (small squares at bottom of slot)
            if (i < 4 && maxRanks[i] > 0) {
                int pipW    = (SLOT_W - 4) / maxRanks[i] - 1;
                int pipY    = slotY + SLOT_H - 5;
                for (int p = 0; p < maxRanks[i]; p++) {
                    int px    = sx + 2 + p * (pipW + 1);
                    int color = p < ranks[i] ? 0xFFFFD700 : 0xFF444444;
                    gfx.fill(px, pipY, px + pipW, pipY + 3, color);
                }
            }

            // Upgradeable indicator
            if (hasPoint) {
                gfx.drawCenteredString(mc.font,
                        net.minecraft.network.chat.Component.literal("§e+"),
                        sx + SLOT_W - 5, slotY + 3, 0xFFFFFF);
            }
        }
    }
}
