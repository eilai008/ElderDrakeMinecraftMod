package com.eilai.runeterra.client.screen;

import com.eilai.runeterra.champion.ChampionDefinition;
import com.eilai.runeterra.champion.ChampionRegistry;
import com.eilai.runeterra.champion.PlayerChampionData;
import com.eilai.runeterra.client.SkinManager;
import com.eilai.runeterra.network.ChampionSelectPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import java.util.List;

public class ChampionSelectScreen extends Screen {

    // Grid: 2 columns on left side, narrower so detail panel has room
    private static final int CARD_W   = 70;
    private static final int CARD_H   = 18;
    private static final int CARD_PAD = 2;
    private static final int COLS     = 2;
    private static final int TITLE_H  = 14;
    private static final int GRID_X   = 4;
    private static final int GRID_Y   = TITLE_H + 4;
    private static final int GRID_W   = (CARD_W + CARD_PAD) * COLS - CARD_PAD; // 142

    // Detail panel
    private static final int DETAIL_GAP = 8;

    // Colors
    private static final int COL_BG          = 0xFF0A0A14;
    private static final int COL_CARD_NORMAL = 0xFF1A1A2E;
    private static final int COL_CARD_HOVER  = 0xFF252540;
    private static final int COL_CARD_SEL    = 0xFF3A2800;
    private static final int COL_BORDER_SEL  = 0xFFFFD700;
    private static final int COL_BORDER_NORM = 0xFF444466;
    private static final int COL_BORDER_WIP  = 0xFF333333;
    private static final int COL_DETAIL_BG   = 0xFF0F0F1E;
    private static final int COL_ABILITY_BG  = 0xFF16162A;
    private static final int COL_DIVIDER     = 0xFF2A2A4A;

    private final List<ChampionDefinition> champions;
    private ChampionDefinition selected;
    private int scrollOffset = 0;

    public ChampionSelectScreen() {
        super(Component.literal("Champion Select"));
        this.champions = ChampionRegistry.all();
        String currentId = "no_champion";
        if (Minecraft.getInstance().player != null) {
            currentId = PlayerChampionData.get(Minecraft.getInstance().player).getChampionId();
        }
        final String id = currentId;
        this.selected = champions.stream()
                .filter(c -> c.id().equals(id))
                .findFirst()
                .orElse(ChampionRegistry.noChampion());
    }

    @Override
    protected void init() {
        rebuildCardButtons();
    }

    private int detailX() { return GRID_X + GRID_W + DETAIL_GAP; }
    private int detailW() { return this.width - detailX() - 4; }

    private void rebuildCardButtons() {
        this.clearWidgets();

        int visibleRows = getVisibleRows();
        int totalRows   = getTotalRows();

        for (int row = 0; row < visibleRows; row++) {
            int dataRow = row + scrollOffset;
            if (dataRow >= totalRows) break;
            for (int col = 0; col < COLS; col++) {
                int idx = dataRow * COLS + col;
                if (idx >= champions.size()) break;
                ChampionDefinition champ = champions.get(idx);
                if (champ.isUnderConstruction()) continue;
                int cx = GRID_X + col * (CARD_W + CARD_PAD);
                int cy = GRID_Y + row * (CARD_H + CARD_PAD);
                final ChampionDefinition fc = champ;
                this.addRenderableWidget(
                        Button.builder(Component.empty(), btn -> selected = fc)
                                .pos(cx, cy).size(CARD_W, CARD_H).build());
            }
        }

        int dx = detailX(), dw = detailW();
        this.addRenderableWidget(
                Button.builder(Component.literal("✔ Confirm"), btn -> confirmSelection())
                        .pos(dx + dw / 2 - 45, this.height - 20)
                        .size(90, 14).build());

        if (scrollOffset > 0)
            this.addRenderableWidget(
                    Button.builder(Component.literal("▲"), btn -> scroll(-1))
                            .pos(GRID_X + GRID_W / 2 - 8, GRID_Y - 13)
                            .size(16, 11).build());
        if (scrollOffset < getTotalRows() - getVisibleRows())
            this.addRenderableWidget(
                    Button.builder(Component.literal("▼"), btn -> scroll(1))
                            .pos(GRID_X + GRID_W / 2 - 8,
                                    GRID_Y + getVisibleRows() * (CARD_H + CARD_PAD))
                            .size(16, 11).build());
    }

    private int getVisibleRows() {
        return Math.max(1, (this.height - GRID_Y - 24) / (CARD_H + CARD_PAD));
    }

    private int getTotalRows() {
        return (int) Math.ceil(champions.size() / (double) COLS);
    }

    private void scroll(int delta) {
        scrollOffset = Math.max(0, Math.min(
                Math.max(0, getTotalRows() - getVisibleRows()), scrollOffset + delta));
        rebuildCardButtons();
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        gfx.fill(0, 0, this.width, this.height, 0xEE000000);

        // Title bar
        gfx.fill(0, 0, this.width, TITLE_H, 0xFF0D0D1A);
        gfx.fill(0, TITLE_H - 1, this.width, TITLE_H, COL_BORDER_SEL);
        gfx.drawCenteredString(this.font,
                Component.literal("§6§lCHOOSE YOUR CHAMPION"),
                this.width / 2, 3, 0xFFFFFFFF);

        // Left grid background
        gfx.fill(GRID_X - 2, GRID_Y - 2,
                GRID_X + GRID_W + 4, this.height - 2, COL_BG);
        gfx.fill(GRID_X + GRID_W + 3, GRID_Y - 2,
                GRID_X + GRID_W + 4, this.height - 2, COL_DIVIDER);

        // Cards
        int visibleRows = getVisibleRows(), totalRows = getTotalRows();
        for (int row = 0; row < visibleRows; row++) {
            int dataRow = row + scrollOffset;
            if (dataRow >= totalRows) break;
            for (int col = 0; col < COLS; col++) {
                int idx = dataRow * COLS + col;
                if (idx >= champions.size()) break;
                drawCard(gfx, champions.get(idx),
                        GRID_X + col * (CARD_W + CARD_PAD),
                        GRID_Y + row * (CARD_H + CARD_PAD),
                        mouseX, mouseY);
            }
        }

        // Detail panel
        int dx = detailX(), dw = detailW();
        if (dw > 60 && selected != null) {
            drawDetailPanel(gfx, selected, dx, GRID_Y, dw);
        }

        super.render(gfx, mouseX, mouseY, partialTick);
    }

    private void drawCard(GuiGraphics gfx, ChampionDefinition champ,
                          int x, int y, int mx, int my) {
        boolean isSel   = champ.id().equals(selected != null ? selected.id() : "");
        boolean isWIP   = champ.isUnderConstruction();
        boolean hovered = !isWIP && mx >= x && mx < x + CARD_W && my >= y && my < y + CARD_H;

        gfx.fill(x, y, x + CARD_W, y + CARD_H,
                isSel ? COL_CARD_SEL : hovered ? COL_CARD_HOVER : COL_CARD_NORMAL);
        int border = isSel ? COL_BORDER_SEL : isWIP ? COL_BORDER_WIP : COL_BORDER_NORM;
        gfx.fill(x,            y,            x + CARD_W, y + 1,      border);
        gfx.fill(x,            y + CARD_H-1, x + CARD_W, y + CARD_H, border);
        gfx.fill(x,            y,            x + 1,      y + CARD_H, border);
        gfx.fill(x + CARD_W-1, y,            x + CARD_W, y + CARD_H, border);

        String prefix = isWIP ? "§7⚒ §8" : isSel ? "§6§l" : "§f";
        // Truncate name to fit card width (about 9 chars at default font)
        String name = champ.displayName();
        if (name.length() > 10) name = name.substring(0, 9) + ".";
        gfx.drawCenteredString(this.font,
                Component.literal(prefix + name),
                x + CARD_W / 2, y + CARD_H / 2 - 3, 0xFFFFFFFF);
    }

    private void drawDetailPanel(GuiGraphics gfx, ChampionDefinition champ,
                                 int x, int y, int w) {
        int panelH = this.height - y - 22;
        gfx.fill(x, y, x + w, y + panelH, COL_DETAIL_BG);
        gfx.fill(x, y, x + w, y + 1, COL_BORDER_SEL);

        int ty = y + 4;

        // Splash art (file is at textures/champion/splash/)
        if (!champ.isUnderConstruction() && !champ.splashTexture().equals("placeholder.png")) {
            Identifier splash = Identifier.fromNamespaceAndPath("runeterra",
                    "textures/champion/splash/" + champ.splashTexture());
            try {
                int splashH = Math.min(50, panelH / 4);
                gfx.blit(splash, x, y, 0, 0, w, splashH, 256, 128);
                gfx.fill(x, y + splashH - 10, x + w, y + splashH, 0xCC000000);
                ty = y + splashH + 2;
            } catch (Exception ignored) {}
        }

        gfx.drawCenteredString(this.font,
                Component.literal("§6§l" + champ.displayName()),
                x + w / 2, ty, 0xFFFFFFFF);
        ty += 9;
        gfx.drawCenteredString(this.font,
                Component.literal("§7" + champ.title()),
                x + w / 2, ty, 0xFFFFFFFF);
        ty += 10;
        gfx.fill(x + 4, ty, x + w - 4, ty + 1, COL_DIVIDER);
        ty += 3;

        if (champ.isUnderConstruction()) {
            gfx.drawCenteredString(this.font,
                    Component.literal("§c⚒ Under Construction"),
                    x + w / 2, ty + 8, 0xFFFFFFFF);
            return;
        }

        ty = drawAbility(gfx, x, ty, w, "§ePassive",       champ.passive());
        ty = drawAbility(gfx, x, ty, w, "§aQ §7(Z)",       champ.abilityQ());
        ty = drawAbility(gfx, x, ty, w, "§aW §7(X)",       champ.abilityW());
        ty = drawAbility(gfx, x, ty, w, "§aE §7(C)",       champ.abilityE());
        ty = drawAbility(gfx, x, ty, w, "§dR §7(V) §8Ult", champ.abilityR());
        drawAbility(gfx, x, ty, w, "§bD/F §7(R/G)",   champ.abilityD());
    }

    private int drawAbility(GuiGraphics gfx, int x, int y, int w,
                            String key, String desc) {
        gfx.fill(x + 2, y, x + w - 2, y + 17, COL_ABILITY_BG);
        gfx.drawString(this.font, Component.literal(key), x + 4, y + 1, 0xFFFFFFFF);

        // Truncate description based on actual pixel width
        String t = desc;
        int maxWidth = w - 10;
        while (t.length() > 4 && this.font.width("§8" + t + "…") > maxWidth) {
            t = t.substring(0, t.length() - 1);
        }
        if (t.length() < desc.length()) t += "…";
        gfx.drawString(this.font, Component.literal("§8" + t), x + 4, y + 9, 0xFFFFFFFF);
        return y + 19;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY,
                                 double scrollX, double scrollY) {
        if (mouseX < detailX()) scroll(scrollY > 0 ? -1 : 1);
        return true;
    }

    private void confirmSelection() {
        if (selected == null) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        PlayerChampionData data = PlayerChampionData.get(mc.player);
        if (!data.hasSelectedOnce()) {
            data.forceSetChampion(selected.id());
        }

        SkinManager.applySkin(mc.player.getUUID(), selected.id());
        ClientPacketDistributor.sendToServer(new ChampionSelectPacket(selected.id()));
        mc.setScreen(null);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        if (Minecraft.getInstance().player == null) return true;
        return PlayerChampionData.get(Minecraft.getInstance().player).hasSelectedOnce();
    }

    @Override
    public boolean isPauseScreen() { return false; }
}