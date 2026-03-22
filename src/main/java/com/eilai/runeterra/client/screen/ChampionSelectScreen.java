package com.eilai.runeterra.client.screen;

import com.eilai.runeterra.champion.ChampionDefinition;
import com.eilai.runeterra.champion.ChampionRegistry;
import com.eilai.runeterra.champion.ChampionStatus;
import com.eilai.runeterra.champion.PlayerChampionData;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * Champion Select Screen.
 *
 * Layout:
 *  - Left panel: scrollable 3-column grid of champion cards
 *  - Right panel: selected champion details (splash, name, title, abilities)
 *  - Bottom: Confirm button
 *
 * Cards that are UNDER_CONSTRUCTION show a hammer overlay and are unclickable.
 */
public class ChampionSelectScreen extends Screen {

    // ── Layout constants ──────────────────────────────────────────────────────
    private static final int CARD_W      = 54;
    private static final int CARD_H      = 72;
    private static final int CARD_PAD    = 6;
    private static final int COLS        = 3;
    private static final int PANEL_LEFT_W = (CARD_W + CARD_PAD) * COLS + CARD_PAD + 10;

    // ── Textures ──────────────────────────────────────────────────────────────
    private static final ResourceLocation BACKGROUND =
            ResourceLocation.fromNamespaceAndPath("runeterra", "textures/gui/champion_select/background.png");
    private static final ResourceLocation CARD_AVAILABLE =
            ResourceLocation.fromNamespaceAndPath("runeterra", "textures/gui/champion_select/card_available.png");
    private static final ResourceLocation CARD_WIP =
            ResourceLocation.fromNamespaceAndPath("runeterra", "textures/gui/champion_select/card_wip.png");
    private static final ResourceLocation CARD_SELECTED =
            ResourceLocation.fromNamespaceAndPath("runeterra", "textures/gui/champion_select/card_selected.png");
    private static final ResourceLocation WIP_OVERLAY =
            ResourceLocation.fromNamespaceAndPath("runeterra", "textures/gui/champion_select/wip_overlay.png");
    private static final ResourceLocation SPLASH_BASE =
            ResourceLocation.fromNamespaceAndPath("runeterra", "textures/champion/splash/");

    // ── State ─────────────────────────────────────────────────────────────────
    private final List<ChampionDefinition> champions;
    private ChampionDefinition selected;
    private int scrollOffset = 0; // in card rows
    private Button confirmButton;

    // Detail panel
    private static final int DETAIL_X_OFFSET = PANEL_LEFT_W + 12;
    private static final int DETAIL_W         = 160;

    public ChampionSelectScreen() {
        super(Component.literal("Champion Select"));
        this.champions = ChampionRegistry.all();
        // Default selection: current champion or no_champion
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
        int confirmX = this.width / 2 + DETAIL_W / 2 - 60;
        int confirmY = this.height - 30;

        confirmButton = Button.builder(Component.literal("✔ Confirm"), btn -> confirmSelection())
                .pos(confirmX, confirmY)
                .size(120, 20)
                .build();
        this.addRenderableWidget(confirmButton);
    }

    // ── Rendering ─────────────────────────────────────────────────────────────

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        // Dark background
        this.renderBackground(gfx, mouseX, mouseY, partialTick);

        int startX = CARD_PAD;
        int startY = CARD_PAD + 20; // leave room for title

        // Title
        gfx.drawCenteredString(this.font,
                Component.literal("§6§lCHOOSE YOUR CHAMPION"),
                this.width / 2, 8, 0xFFFFFF);

        // ── Draw champion grid ─────────────────────────────────────────────
        int totalRows = (int) Math.ceil(champions.size() / (double) COLS);
        int visibleRows = (this.height - startY - 40) / (CARD_H + CARD_PAD);

        for (int row = 0; row < visibleRows; row++) {
            int dataRow = row + scrollOffset;
            if (dataRow >= totalRows) break;

            for (int col = 0; col < COLS; col++) {
                int idx = dataRow * COLS + col;
                if (idx >= champions.size()) break;

                ChampionDefinition champ = champions.get(idx);
                int cx = startX + col * (CARD_W + CARD_PAD);
                int cy = startY + row * (CARD_H + CARD_PAD);

                drawChampionCard(gfx, champ, cx, cy, mouseX, mouseY);
            }
        }

        // Scroll hint
        if (scrollOffset > 0)
            gfx.drawString(this.font, "▲", startX + PANEL_LEFT_W / 2 - 4, startY - 12, 0xAAAAAA);
        if (scrollOffset < totalRows - visibleRows)
            gfx.drawString(this.font, "▼", startX + PANEL_LEFT_W / 2 - 4, startY + visibleRows * (CARD_H + CARD_PAD) + 2, 0xAAAAAA);

        // ── Draw detail panel ──────────────────────────────────────────────
        if (selected != null) {
            drawDetailPanel(gfx, selected, DETAIL_X_OFFSET, 20);
        }

        super.render(gfx, mouseX, mouseY, partialTick);
    }

    private void drawChampionCard(GuiGraphics gfx, ChampionDefinition champ,
                                  int x, int y, int mouseX, int mouseY) {
        boolean isSelected = champ.id().equals(selected != null ? selected.id() : "");
        boolean isWIP      = champ.isUnderConstruction();
        boolean hovered    = !isWIP && mouseX >= x && mouseX < x + CARD_W
                && mouseY >= y && mouseY < y + CARD_H;

        // Card background
        int bgColor = isSelected ? 0xFF8B6914 : (hovered ? 0xFF3A3A3A : 0xFF1E1E2E);
        gfx.fill(x, y, x + CARD_W, y + CARD_H, bgColor);
        gfx.fill(x, y, x + CARD_W, y + 1, 0xFF888888); // top border
        gfx.fill(x, y + CARD_H - 1, x + CARD_W, y + CARD_H, 0xFF888888); // bottom border
        gfx.fill(x, y, x + 1, y + CARD_H, 0xFF888888); // left border
        gfx.fill(x + CARD_W - 1, y, x + CARD_W, y + CARD_H, 0xFF888888); // right border

        // Splash texture (top 2/3 of card)
        int splashH = CARD_H - 18;
        try {
            ResourceLocation splash = ResourceLocation.fromNamespaceAndPath(
                    "runeterra", "textures/champion/splash/" + champ.splashTexture());
            RenderSystem.setShaderColor(isWIP ? 0.4f : 1.0f, isWIP ? 0.4f : 1.0f,
                    isWIP ? 0.4f : 1.0f, 1.0f);
            gfx.blit(splash, x + 1, y + 1, 0, 0, CARD_W - 2, splashH, CARD_W - 2, splashH);
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        } catch (Exception ignored) {
            // Texture not found — draw colored placeholder
            gfx.fill(x + 1, y + 1, x + CARD_W - 1, y + splashH, isWIP ? 0xFF222222 : 0xFF2A4A7A);
        }

        // WIP overlay — hammer icon area
        if (isWIP) {
            // Dark tint already applied above; draw "🚧" text
            gfx.fill(x + 1, y + 1, x + CARD_W - 1, y + splashH, 0x88000000);
            gfx.drawCenteredString(this.font,
                    Component.literal("§e⚒"), x + CARD_W / 2, y + splashH / 2 - 4, 0xFFFFFF);
        }

        // Champion name at bottom of card
        String shortName = champ.displayName().length() > 9
                ? champ.displayName().substring(0, 8) + "." : champ.displayName();
        gfx.drawCenteredString(this.font,
                Component.literal(isWIP ? "§7" + shortName : "§f" + shortName),
                x + CARD_W / 2, y + CARD_H - 14, 0xFFFFFF);
    }

    private void drawDetailPanel(GuiGraphics gfx, ChampionDefinition champ, int x, int y) {
        int panelW = DETAIL_W;

        // Panel background
        gfx.fill(x - 4, y - 4, x + panelW + 4, this.height - 36, 0xCC0A0A14);

        // Splash (large)
        int splashH = 90;
        try {
            ResourceLocation splash = ResourceLocation.fromNamespaceAndPath(
                    "runeterra", "textures/champion/splash/" + champ.splashTexture());
            gfx.blit(splash, x, y, 0, 0, panelW, splashH, panelW, splashH);
        } catch (Exception ignored) {
            gfx.fill(x, y, x + panelW, y + splashH, 0xFF1A2A4A);
        }

        int textY = y + splashH + 6;

        // Name + title
        gfx.drawCenteredString(this.font,
                Component.literal("§6§l" + champ.displayName()),
                x + panelW / 2, textY, 0xFFFFFF);
        textY += 10;
        gfx.drawCenteredString(this.font,
                Component.literal("§7" + champ.title()),
                x + panelW / 2, textY, 0xFFFFFF);
        textY += 14;

        if (champ.isUnderConstruction()) {
            gfx.drawCenteredString(this.font,
                    Component.literal("§c§l⚒ Under Construction"),
                    x + panelW / 2, textY, 0xFFFFFF);
            return;
        }

        // Abilities
        drawAbilityRow(gfx, x, textY, panelW, "§ePassive", champ.passive());
        textY += 22;
        drawAbilityRow(gfx, x, textY, panelW, "§aQ (E key)", champ.abilityQ());
        textY += 22;
        drawAbilityRow(gfx, x, textY, panelW, "§aW (R key)", champ.abilityW());
        textY += 22;
        drawAbilityRow(gfx, x, textY, panelW, "§aE (T key)", champ.abilityE());
        textY += 22;
        drawAbilityRow(gfx, x, textY, panelW, "§dR (F key) §7[Ultimate]", champ.abilityR());
        textY += 22;
        drawAbilityRow(gfx, x, textY, panelW, "§bD (C key)", champ.abilityD());
    }

    private void drawAbilityRow(GuiGraphics gfx, int x, int y, int w,
                                 String keyLabel, String description) {
        gfx.fill(x, y, x + w, y + 20, 0x881A1A2E);
        gfx.drawString(this.font, Component.literal(keyLabel), x + 3, y + 2, 0xFFFFFF);
        // Truncate description to fit
        String desc = description.length() > 34 ? description.substring(0, 31) + "..." : description;
        gfx.drawString(this.font, Component.literal("§7" + desc), x + 3, y + 11, 0xFFFFFF);
    }

    // ── Input ─────────────────────────────────────────────────────────────────

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int startX = CARD_PAD;
        int startY = CARD_PAD + 20;
        int visibleRows = (this.height - startY - 40) / (CARD_H + CARD_PAD);

        for (int row = 0; row < visibleRows; row++) {
            int dataRow = row + scrollOffset;
            for (int col = 0; col < COLS; col++) {
                int idx = dataRow * COLS + col;
                if (idx >= champions.size()) break;

                ChampionDefinition champ = champions.get(idx);
                if (champ.isUnderConstruction()) continue;

                int cx = startX + col * (CARD_W + CARD_PAD);
                int cy = startY + row * (CARD_H + CARD_PAD);

                if (mouseX >= cx && mouseX < cx + CARD_W && mouseY >= cy && mouseY < cy + CARD_H) {
                    selected = champ;
                    return true;
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int totalRows = (int) Math.ceil(champions.size() / (double) COLS);
        int visibleRows = (this.height - (CARD_PAD + 20) - 40) / (CARD_H + CARD_PAD);
        int maxScroll = Math.max(0, totalRows - visibleRows);

        scrollOffset = (int) Math.max(0, Math.min(maxScroll, scrollOffset - scrollY));
        return true;
    }

    // ── Confirm ───────────────────────────────────────────────────────────────

    private void confirmSelection() {
        if (selected == null) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        PlayerChampionData data = PlayerChampionData.get(mc.player);
        int result = data.trySetChampion(selected.id(), mc.player.level().getGameTime());

        switch (result) {
            case 0 -> {
                // TODO: send ChampionSelectedPacket to server
                // ModPackets.sendToServer(new ChampionSelectedPacket(selected.id()));
                mc.setScreen(null);
            }
            case 1 -> mc.player.displayClientMessage(
                    Component.literal("§cYou are still in combat!"), true);
            case 2 -> mc.player.displayClientMessage(
                    Component.literal("§cChampion switch is on cooldown!"), true);
        }
    }

    // ── Prevent closing without selecting (first time) ────────────────────────

    @Override
    public boolean shouldCloseOnEsc() {
        if (Minecraft.getInstance().player == null) return true;
        return PlayerChampionData.get(Minecraft.getInstance().player).hasSelectedOnce();
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
