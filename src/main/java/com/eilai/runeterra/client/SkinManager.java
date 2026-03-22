package com.eilai.runeterra.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;

import java.util.HashMap;
import java.util.Map;

/**
 * Manages champion skin application on the client.
 *
 * When a champion is selected, their skin texture is applied as an
 * override to the local player's skin. When "no_champion" is selected,
 * the original skin is restored.
 *
 * Skin textures go at:
 *   assets/runeterra/textures/champion/skin/<champId>.png
 *
 * The PNG must be a standard 64x64 Minecraft skin format.
 *
 * NOTE: This is client-side only. Other players on a multiplayer server
 * won't see the skin change unless you implement a custom player renderer.
 * For singleplayer this works perfectly.
 */
public class SkinManager {

    /** Maps champId → skin resource location */
    private static final Map<String, Identifier> SKINS = new HashMap<>();

    static {
        // Register champion skins here
        // Pattern: SKINS.put("champId", Identifier.fromNamespaceAndPath("runeterra", "textures/champion/skin/champId.png"));
        SKINS.put("vayne", Identifier.fromNamespaceAndPath("runeterra", "textures/champion/skin/vayne.png"));
        // Add more as you implement them:
        // SKINS.put("garen", Identifier.fromNamespaceAndPath("runeterra", "textures/champion/skin/garen.png"));
    }

    /** Stored original skin before any champion was applied */
    private static PlayerSkin originalSkin = null;

    /**
     * Apply the skin for the given champion.
     * Call this from ClientChampionEvents when champion select is confirmed.
     * Must be called on the client thread.
     */
    public static void applySkin(String championId) {
        if (FMLEnvironment.dist != Dist.CLIENT) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        if (championId.equals("no_champion")) {
            restoreOriginalSkin();
            return;
        }

        Identifier skinTex = SKINS.get(championId);
        if (skinTex == null) return; // no skin registered for this champion

        // Store original skin the first time we apply a champion skin
        if (originalSkin == null) {
            originalSkin = mc.player.getSkin();
        }

        // Build a new PlayerSkin with our champion texture
        // keeping the same model (slim/classic) as the original
        PlayerSkin current = mc.player.getSkin();
        PlayerSkin champSkin = new PlayerSkin(
                skinTex,
                null,                    // no cape
                null,                    // no elytra
                null,                    // no ear texture
                current.model(),         // keep slim/classic model
                current.secure()
        );

        // Apply via the skin override — uses NeoForge's player skin hook
        SkinOverrideHandler.setOverride(mc.player.getUUID(), champSkin);
    }

    /**
     * Restore the player's original skin.
     */
    public static void restoreOriginalSkin() {
        if (FMLEnvironment.dist != Dist.CLIENT) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        SkinOverrideHandler.clearOverride(mc.player.getUUID());
        originalSkin = null;
    }

    /**
     * Returns the skin texture for a champion, or null if none is registered.
     */
    public static Identifier getSkinTexture(String championId) {
        return SKINS.get(championId);
    }

    /**
     * Returns true if a skin is registered for this champion.
     */
    public static boolean hasSkin(String championId) {
        return SKINS.containsKey(championId);
    }
}
