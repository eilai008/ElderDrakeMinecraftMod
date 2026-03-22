package com.eilai.runeterra.client;

import net.minecraft.resources.Identifier;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Stores champion skin texture overrides per player UUID.
 *
 * Skin PNGs: assets/runeterra/textures/champion/<champId>.png
 * Must be standard 64×64 Minecraft skin format.
 *
 * NOTE: path is textures/champion/ (NOT textures/champion/skin/)
 * to match the actual file location in your resources folder.
 */
public class SkinManager {

    private static final Map<UUID, Identifier> OVERRIDES = new HashMap<>();
    private static final Map<String, Identifier> CHAMPION_SKINS = new HashMap<>();

    static {
        // Path matches: src/main/resources/assets/runeterra/textures/champion/vayne.png
        CHAMPION_SKINS.put("vayne", Identifier.fromNamespaceAndPath("runeterra",
                "textures/champion/vayne.png"));
    }

    public static void applySkin(UUID playerId, String championId) {
        if (championId.equals("no_champion")) {
            OVERRIDES.remove(playerId);
            return;
        }
        Identifier skin = CHAMPION_SKINS.get(championId);
        if (skin != null) {
            OVERRIDES.put(playerId, skin);
        } else {
            OVERRIDES.remove(playerId);
        }
    }

    public static Identifier getOverride(UUID playerId) {
        return OVERRIDES.get(playerId);
    }

    public static boolean hasOverride(UUID playerId) {
        return OVERRIDES.containsKey(playerId);
    }

    public static boolean hasSkin(String championId) {
        return CHAMPION_SKINS.containsKey(championId);
    }

    public static void clearAll() {
        OVERRIDES.clear();
    }
}