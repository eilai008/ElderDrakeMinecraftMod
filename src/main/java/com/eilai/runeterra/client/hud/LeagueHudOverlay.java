package com.eilai.runeterra.client.hud;

import com.eilai.runeterra.champion.LeagueXPHelper;
import com.eilai.runeterra.champion.PlayerChampionData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/**
 * League HUD — drawn in the BOTTOM CENTER of the screen.
 *
 * Vanilla layout at bottom:
 *   sh-0  to sh-22 : hotbar
 *   sh-22 to sh-39 : XP bar (vanilla)
 *   sh-39 to sh-54 : health / food bars
 *
 * We hook AFTER the CAMERA_OVERLAYS layer (renders last before chat)
 * and draw our HUD anchored to the TOP of the screen to guarantee
 * zero overlap with ANY vanilla bottom UI.
 *
 * Position: top-left corner starting at y=4, giving a clean compact panel.
 */
@EventBusSubscriber(modid = "runeterra", value = Dist.CLIENT)
public class LeagueHudOverlay {

    // Panel position — top left, safe from all vanilla UI
    private static final int PANEL_X = 4;
    private static final int PANEL_Y = 4;
    private static final int PANEL_W = 120;
    private static final int SLOT_W  = 22;
    private static final int SLOT_H  = 22;
    private static final int SLOT_PAD = 2;
    private static final int BAR_H   = 4;

    @SubscribeEvent
    public static void onRenderHud(RenderGuiLayerEvent.Post event) {
        if (!event.getName().equals(VanillaGuiLayers.CAMERA_OVERLAYS)) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;

        Player player = mc.player;
        PlayerChampionData data = PlayerChampionData.get(player);
        if (data.getChampionId().equals("no_champion")) return;

        GuiGraphics gfx = event.getGuiGraphics();
        String champId = data.getChampionId();
        int level    = data.getLevel(champId);
        int xp       = data.getXP(champId);
        int xpNeeded = level < 18 ? LeagueXPHelper.xpForLevel(level) : 1;
        float xpFrac = level < 18 ? Math.min(1f, (float) xp / xpNeeded) : 1f;

        int x = PANEL_X;
        int y = PANEL_Y;

        // ── Panel background ──────────────────────────────────────────────────
        int panelH = 14 + BAR_H + 4 + SLOT_H + 4;
        gfx.fill(x - 2, y - 2, x + PANEL_W + 2, y + panelH, 0xAA000000);
        gfx.fill(x - 2, y - 2, x + PANEL_W + 2, y - 1, 0xFFFFD700); // gold top border

        // ── Champion name + level ─────────────────────────────────────────────
        gfx.drawString(mc.font,
                Component.literal("§6" + capitalise(champId) + " §e" + level),
                x, y, 0xFFFFFFFF);
        y += 10;

        // ── XP bar ────────────────────────────────────────────────────────────
        gfx.fill(x, y, x + PANEL_W, y + BAR_H, 0xFF111111);
        int fillW = (int) ((PANEL_W - 2) * xpFrac);
        if (fillW > 0)
            gfx.fill(x + 1, y + 1, x + 1 + fillW, y + BAR_H - 1, 0xFF00BFFF);
        y += BAR_H + 3;

        // XP numbers
        String xpStr = level >= 18 ? "§6MAX"
                : "§7" + xp + "/" + xpNeeded;
        gfx.drawString(mc.font, Component.literal(xpStr), x, y, 0xFFFFFFFF);
        y += 10;

        // ── 4 Ability slots ───────────────────────────────────────────────────
        String[] keys   = {"Z","X","C","V"};
        String[] labels = {"Q","W","E","R"};
        int[] ranks = {
                data.getQRank(champId), data.getWRank(champId),
                data.getERank(champId), data.getRRank(champId)
        };
        int[] maxRanks = {5,5,5,3};
        int avail      = data.availableSkillPoints();

        for (int i = 0; i < 4; i++) {
            int sx      = x + i * (SLOT_W + SLOT_PAD);
            boolean ult = i == 3;
            boolean noR = ranks[i] == 0;
            boolean up  = avail > 0 && (ult
                    ? LeagueXPHelper.canRankUltimate(level, ranks[3]) && ranks[3] < 3
                    : ranks[i] < 5);

            int bg = noR ? 0xFF080810 : up ? (ult ? 0xFF3A0A5A : 0xFF0A2A4A)
                    : ult ? 0xFF1A0A2A : 0xFF0A1020;
            gfx.fill(sx, y, sx + SLOT_W, y + SLOT_H, bg);

            int border = up ? 0xFFFFD700 : noR ? 0xFF333344 : ult ? 0xFF9944CC : 0xFF2244AA;
            gfx.fill(sx,           y,            sx + SLOT_W, y + 1,       border);
            gfx.fill(sx,           y + SLOT_H-1, sx + SLOT_W, y + SLOT_H,  border);
            gfx.fill(sx,           y,             sx + 1,     y + SLOT_H,  border);
            gfx.fill(sx+SLOT_W-1,  y,             sx+SLOT_W,  y + SLOT_H,  border);

            String col = noR ? "§8" : ult ? "§d" : "§b";
            gfx.drawCenteredString(mc.font,
                    Component.literal(col + labels[i]),
                    sx + SLOT_W/2, y + 2, 0xFFFFFFFF);
            gfx.drawCenteredString(mc.font,
                    Component.literal("§8" + keys[i]),
                    sx + SLOT_W/2, y + SLOT_H - 9, 0xFFFFFFFF);

            // rank pips
            int pipW = (SLOT_W - 4) / maxRanks[i] - 1;
            int pipY = y + SLOT_H - 4;
            for (int p = 0; p < maxRanks[i]; p++) {
                int px = sx + 2 + p * (pipW + 1);
                gfx.fill(px, pipY, px + pipW, pipY + 3,
                        p < ranks[i] ? 0xFFFFD700 : 0xFF333333);
            }

            if (up) {
                gfx.fill(sx + SLOT_W - 6, y, sx + SLOT_W, y + 7, 0xCCFFD700);
                gfx.drawString(mc.font, Component.literal("§0+"), sx + SLOT_W - 6, y + 1, 0xFFFFFFFF);
            }
        }

        // ── Spell slots — side by side below ability row ───────────────────────
        int spellY = y + SLOT_H + 3;
        drawSpell(gfx, mc, data, x,        spellY, 20, true);   // D (R key)
        drawSpell(gfx, mc, data, x + 22,   spellY, 20, false);  // F (G key)
    }

    private static String capitalise(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private static void drawSpell(GuiGraphics gfx, Minecraft mc,
                                  PlayerChampionData data,
                                  int x, int y, int size, boolean isD) {
        var sd    = data.getSpellData();
        var spell = isD ? sd.getSpellD() : sd.getSpellF();
        int cd    = isD ? sd.getCooldownD() : sd.getCooldownF();
        boolean rdy = cd <= 0;

        gfx.fill(x, y, x+size, y+size, rdy ? 0xFF0A1020 : 0xFF150505);
        int b = rdy ? 0xFF4466AA : 0xFF663333;
        gfx.fill(x, y, x+size, y+1, b);
        gfx.fill(x, y+size-1, x+size, y+size, b);
        gfx.fill(x, y, x+1, y+size, b);
        gfx.fill(x+size-1, y, x+size, y+size, b);

        String name = spell.getDisplayName().substring(0, Math.min(2, spell.getDisplayName().length()));
        gfx.drawCenteredString(mc.font,
                Component.literal((rdy ? "§b" : "§8") + name),
                x + size/2, y + 2, 0xFFFFFFFF);
        if (!rdy)
            gfx.drawCenteredString(mc.font,
                    Component.literal("§c" + (cd/20)),
                    x + size/2, y + size/2 - 3, 0xFFFFFFFF);

        gfx.drawCenteredString(mc.font,
                Component.literal("§8" + (isD ? "R" : "G")),
                x + size/2, y + size - 8, 0xFFFFFFFF);
    }
}