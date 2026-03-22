package com.eilai.runeterra.client.screen;

import com.eilai.runeterra.champion.PlayerChampionData;
import com.eilai.runeterra.champion.spell.PlayerSpellData;
import com.eilai.runeterra.champion.spell.SummonerSpell;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Spell Select Screen — pick your 2 summoner spells (D and F slots).
 *
 * Layout:
 *  - Title at top
 *  - 6 spell cards in a 3x2 grid
 *  - Selected D shows in bottom-left panel, selected F in bottom-right
 *  - Confirm button at bottom center
 */
public class SpellSelectScreen extends Screen {

    private static final int CARD_W   = 80;
    private static final int CARD_H   = 36;
    private static final int CARD_PAD = 6;
    private static final int COLS     = 3;

    private static final int COL_BG       = 0xE5000000;
    private static final int COL_CARD     = 0xFF1A1A2E;
    private static final int COL_CARD_HOV = 0xFF252540;
    private static final int COL_SEL_D    = 0xFF2A1400; // orange tint for D
    private static final int COL_SEL_F    = 0xFF001A2A; // blue tint for F
    private static final int COL_GOLD     = 0xFFFFD700;
    private static final int COL_BLUE     = 0xFF4488FF;
    private static final int COL_BORDER   = 0xFF444466;

    private final SummonerSpell[] spells = SummonerSpell.values();
    private SummonerSpell selectedD;
    private SummonerSpell selectedF;

    // Which slot we're assigning next: 0 = D, 1 = F
    private int assigningSlot = 0;

    public SpellSelectScreen() {
        super(Component.literal("Choose Summoner Spells"));
        // Load current selections
        if (Minecraft.getInstance().player != null) {
            PlayerSpellData sd = PlayerChampionData
                    .get(Minecraft.getInstance().player).getSpellData();
            selectedD = sd.getSpellD();
            selectedF = sd.getSpellF();
        } else {
            selectedD = SummonerSpell.FLASH;
            selectedF = SummonerSpell.IGNITE;
        }
    }

    @Override
    protected void init() {
        rebuildButtons();
    }

    private void rebuildButtons() {
        this.clearWidgets();

        int gridW     = (CARD_W + CARD_PAD) * COLS - CARD_PAD;
        int startX    = (this.width - gridW) / 2;
        int startY    = 30;

        int rows = (int) Math.ceil(spells.length / (double) COLS);
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < COLS; col++) {
                int idx = row * COLS + col;
                if (idx >= spells.length) break;
                SummonerSpell spell = spells[idx];
                int cx = startX + col * (CARD_W + CARD_PAD);
                int cy = startY + row * (CARD_H + CARD_PAD);
                final SummonerSpell fs = spell;
                this.addRenderableWidget(
                        Button.builder(Component.empty(), btn -> selectSpell(fs))
                                .pos(cx, cy).size(CARD_W, CARD_H).build());
            }
        }

        // Confirm button
        this.addRenderableWidget(
                Button.builder(Component.literal("✔ Confirm"), btn -> confirmSpells())
                        .pos(this.width / 2 - 40, this.height - 22)
                        .size(80, 16).build());
    }

    private void selectSpell(SummonerSpell spell) {
        if (assigningSlot == 0) {
            // Don't allow same spell in both slots
            if (spell == selectedF) selectedF = selectedD;
            selectedD = spell;
            assigningSlot = 1;
        } else {
            if (spell == selectedD) selectedD = selectedF;
            selectedF = spell;
            assigningSlot = 0;
        }
    }

    @Override
    public void renderBackground(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        gfx.fill(0, 0, this.width, this.height, COL_BG);
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(gfx, mouseX, mouseY, partialTick);

        // Title
        gfx.fill(0, 0, this.width, 18, 0xFF0D0D1A);
        gfx.fill(0, 17, this.width, 18, COL_GOLD);
        gfx.drawCenteredString(this.font,
                Component.literal("§6§lCHOOSE SUMMONER SPELLS"),
                this.width / 2, 4, 0xFFFFFFFF);

        // Assignment hint
        String hint = assigningSlot == 0
                ? "§eClick a spell to assign §6D §e(F key)"
                : "§eClick a spell to assign §bF §e(C key)";
        gfx.drawCenteredString(this.font, Component.literal(hint),
                this.width / 2, 20, 0xFFFFFFFF);

        // Spell cards
        int gridW  = (CARD_W + CARD_PAD) * COLS - CARD_PAD;
        int startX = (this.width - gridW) / 2;
        int startY = 30;
        int rows   = (int) Math.ceil(spells.length / (double) COLS);

        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < COLS; col++) {
                int idx = row * COLS + col;
                if (idx >= spells.length) break;
                SummonerSpell spell = spells[idx];
                int cx = startX + col * (CARD_W + CARD_PAD);
                int cy = startY + row * (CARD_H + CARD_PAD);
                drawSpellCard(gfx, spell, cx, cy, mouseX, mouseY);
            }
        }

        // Selected spell panels at bottom
        int panelY = this.height - 40;
        int panelW = (this.width / 2) - 8;

        // D panel
        gfx.fill(4, panelY, 4 + panelW, panelY + 18, 0xFF1A0A00);
        gfx.fill(4, panelY, 4 + panelW, panelY + 1, COL_GOLD);
        gfx.drawString(this.font,
                Component.literal("§6D: §f" + (selectedD != null ? selectedD.getDisplayName() : "—")),
                8, panelY + 4, 0xFFFFFFFF);

        // F panel
        gfx.fill(this.width / 2 + 4, panelY, this.width - 4, panelY + 18, 0xFF000A1A);
        gfx.fill(this.width / 2 + 4, panelY, this.width - 4, panelY + 1, COL_BLUE);
        gfx.drawString(this.font,
                Component.literal("§bF: §f" + (selectedF != null ? selectedF.getDisplayName() : "—")),
                this.width / 2 + 8, panelY + 4, 0xFFFFFFFF);

        super.render(gfx, mouseX, mouseY, partialTick);
    }

    private void drawSpellCard(GuiGraphics gfx, SummonerSpell spell,
                                int x, int y, int mouseX, int mouseY) {
        boolean isD    = spell == selectedD;
        boolean isF    = spell == selectedF;
        boolean hovered = mouseX >= x && mouseX < x + CARD_W
                       && mouseY >= y && mouseY < y + CARD_H;

        int bg = isD ? COL_SEL_D : isF ? COL_SEL_F : hovered ? COL_CARD_HOV : COL_CARD;
        int border = isD ? COL_GOLD : isF ? COL_BLUE : COL_BORDER;

        gfx.fill(x, y, x + CARD_W, y + CARD_H, bg);
        gfx.fill(x, y, x + CARD_W, y + 1, border);
        gfx.fill(x, y + CARD_H-1, x + CARD_W, y + CARD_H, border);
        gfx.fill(x, y, x + 1, y + CARD_H, border);
        gfx.fill(x + CARD_W-1, y, x + CARD_W, y + CARD_H, border);

        // Spell name
        String label = isD ? "§6" : isF ? "§b" : "§f";
        gfx.drawCenteredString(this.font,
                Component.literal(label + spell.getDisplayName()),
                x + CARD_W / 2, y + 4, 0xFFFFFFFF);

        // Cooldown
        gfx.drawCenteredString(this.font,
                Component.literal("§8" + spell.getCooldownTicks() / 20 + "s CD"),
                x + CARD_W / 2, y + 14, 0xFFFFFFFF);

        // Slot badge
        if (isD) gfx.drawString(this.font, Component.literal("§6[D]"), x + 2, y + 25, 0xFFFFFFFF);
        if (isF) gfx.drawString(this.font, Component.literal("§b[F]"), x + 2, y + 25, 0xFFFFFFFF);
    }

    private void confirmSpells() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || selectedD == null || selectedF == null) return;

        PlayerSpellData sd = PlayerChampionData.get(mc.player).getSpellData();
        sd.setSpellD(selectedD);
        sd.setSpellF(selectedF);

        // TODO: send SpellUpdatePacket to server so server-side data is updated

        mc.setScreen(null);
        mc.player.displayClientMessage(
                Component.literal("§6Spells set: §f" + selectedD.getDisplayName()
                        + " §7& §f" + selectedF.getDisplayName()), false);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
