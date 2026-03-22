package com.eilai.runeterra.champion;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.fml.loading.FMLPaths;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Reads mob League XP values from:
 *   config/runeterra/mob_xp.json
 *
 * Format:
 * {
 *   "minecraft:zombie":      40,
 *   "minecraft:skeleton":    40,
 *   "minecraft:creeper":     55,
 *   "minecraft:ender_dragon": 3000,
 *   "default":               20
 * }
 *
 * "default" is used for any mob not listed.
 * Edit the file and run /reload or restart to apply changes.
 */
public final class MobXPConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FMLPaths.CONFIGDIR.get()
            .resolve("runeterra").resolve("mob_xp.json");

    private static Map<String, Integer> xpMap = new LinkedHashMap<>();

    private MobXPConfig() {}

    // ── Default values ────────────────────────────────────────────────────────

    private static Map<String, Integer> buildDefaults() {
        Map<String, Integer> m = new LinkedHashMap<>();
        m.put("default",                        20);
        // Passive
        m.put("minecraft:cow",                  15);
        m.put("minecraft:pig",                  15);
        m.put("minecraft:sheep",                15);
        m.put("minecraft:chicken",              15);
        // Common hostile
        m.put("minecraft:zombie",               40);
        m.put("minecraft:skeleton",             40);
        m.put("minecraft:creeper",              55);
        m.put("minecraft:spider",               35);
        m.put("minecraft:cave_spider",          35);
        m.put("minecraft:enderman",             80);
        m.put("minecraft:witch",                65);
        m.put("minecraft:drowned",              40);
        m.put("minecraft:husk",                 40);
        m.put("minecraft:stray",                40);
        m.put("minecraft:phantom",              50);
        m.put("minecraft:slime",                20);
        m.put("minecraft:magma_cube",           25);
        // Nether
        m.put("minecraft:blaze",                90);
        m.put("minecraft:ghast",                95);
        m.put("minecraft:piglin",               50);
        m.put("minecraft:piglin_brute",        100);
        m.put("minecraft:zombified_piglin",     55);
        m.put("minecraft:wither_skeleton",     100);
        // End
        m.put("minecraft:shulker",              80);
        // Mini-bosses
        m.put("minecraft:elder_guardian",      300);
        m.put("minecraft:ravager",             250);
        m.put("minecraft:evoker",              200);
        m.put("minecraft:vindicator",           80);
        // Bosses
        m.put("minecraft:wither",             1500);
        m.put("minecraft:ender_dragon",       3000);
        return m;
    }

    // ── Load / Save ───────────────────────────────────────────────────────────

    public static void load() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            if (!Files.exists(CONFIG_PATH)) {
                // First run — write defaults
                xpMap = buildDefaults();
                save();
                return;
            }
            try (Reader r = Files.newBufferedReader(CONFIG_PATH)) {
                Type type = new TypeToken<LinkedHashMap<String, Integer>>(){}.getType();
                Map<String, Integer> loaded = GSON.fromJson(r, type);
                if (loaded != null) {
                    xpMap = loaded;
                } else {
                    xpMap = buildDefaults();
                }
            }
        } catch (Exception e) {
            System.err.println("[Runeterra] Failed to load mob_xp.json: " + e.getMessage());
            xpMap = buildDefaults();
        }
    }

    public static void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (Writer w = Files.newBufferedWriter(CONFIG_PATH)) {
                GSON.toJson(xpMap, w);
            }
        } catch (Exception e) {
            System.err.println("[Runeterra] Failed to save mob_xp.json: " + e.getMessage());
        }
    }

    // ── Lookup ────────────────────────────────────────────────────────────────

    public static int getXP(LivingEntity entity) {
        if (xpMap.isEmpty()) load();
        String key = EntityType.getKey(entity.getType()).toString();
        return xpMap.getOrDefault(key, xpMap.getOrDefault("default", 20));
    }

    public static void setXP(String entityId, int xp) {
        xpMap.put(entityId, xp);
        save();
    }

    public static Map<String, Integer> getAll() {
        return new LinkedHashMap<>(xpMap);
    }
}
