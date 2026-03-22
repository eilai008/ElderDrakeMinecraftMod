package com.eilai.runeterra.init;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.settings.KeyConflictContext;

/**
 * All champion ability keybinds.
 *
 * League → Minecraft mapping:
 *  Q → Q key  (LWJGL key 81)
 *  W → E key  (LWJGL key 69)
 *  E → R key  (LWJGL key 82)
 *  R → T key  (LWJGL key 84)
 *  D → F key  (LWJGL key 70)
 *  F → C key  (LWJGL key 67)
 *
 * Drop item is moved to G key (LWJGL key 71).
 * Register these in RuneterraClient via RegisterKeyMappingsEvent.
 */
public class ModKeybinds {

    public static final String CATEGORY = "key.categories.runeterra";

    public static final KeyMapping ABILITY_Q = new KeyMapping(
            "key.runeterra.ability_q",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_Q,
            CATEGORY);

    public static final KeyMapping ABILITY_W = new KeyMapping(
            "key.runeterra.ability_w",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_E,
            CATEGORY);

    public static final KeyMapping ABILITY_E = new KeyMapping(
            "key.runeterra.ability_e",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_R,
            CATEGORY);

    public static final KeyMapping ABILITY_R = new KeyMapping(
            "key.runeterra.ability_r",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_T,
            CATEGORY);

    public static final KeyMapping SPELL_D = new KeyMapping(
            "key.runeterra.spell_d",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_F,
            CATEGORY);

    public static final KeyMapping SPELL_F = new KeyMapping(
            "key.runeterra.spell_f",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_C,
            CATEGORY);
}
