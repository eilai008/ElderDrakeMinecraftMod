package com.eilai.runeterra.init;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

/**
 * All Runeterra keybinds.
 *
 * Ability keys (no vanilla conflicts):
 *  Q → Z    W → X    E → C    R → V
 *  D spell → R key   F spell → G key
 *
 * Weapon:
 *  SHOOT       → Right Mouse Button
 *  WEAPON_SLOT → key 0 (GLFW_KEY_0, which is the "0" number key above letters)
 */
public class ModKeybinds {

    public static final KeyMapping.Category CATEGORY = new KeyMapping.Category(
            Identifier.fromNamespaceAndPath("runeterra", "category"));

    public static final KeyMapping ABILITY_Q = new KeyMapping(
            "key.runeterra.ability_q",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_Z,
            CATEGORY);

    public static final KeyMapping ABILITY_W = new KeyMapping(
            "key.runeterra.ability_w",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_X,
            CATEGORY);

    public static final KeyMapping ABILITY_E = new KeyMapping(
            "key.runeterra.ability_e",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_C,
            CATEGORY);

    public static final KeyMapping ABILITY_R = new KeyMapping(
            "key.runeterra.ability_r",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_V,
            CATEGORY);

    public static final KeyMapping SPELL_D = new KeyMapping(
            "key.runeterra.spell_d",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_R,
            CATEGORY);

    public static final KeyMapping SPELL_F = new KeyMapping(
            "key.runeterra.spell_f",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_G,
            CATEGORY);

    /** Key 0 (above letters) — toggle champion weapon slot selected */
    public static final KeyMapping WEAPON_SLOT = new KeyMapping(
            "key.runeterra.weapon_slot",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_0,
            CATEGORY);

    /** Right mouse button — shoot champion weapon */
    public static final KeyMapping SHOOT = new KeyMapping(
            "key.runeterra.shoot",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.MOUSE,
            GLFW.GLFW_MOUSE_BUTTON_RIGHT,
            CATEGORY);
}