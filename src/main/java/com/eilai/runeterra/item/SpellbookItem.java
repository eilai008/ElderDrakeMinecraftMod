
package com.eilai.runeterra.item;

import com.eilai.runeterra.client.screen.SpellSelectScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * The Spellbook — lets players change their D/F summoner spells.
 *
 * Right-clicking opens the SpellSelectScreen.
 * The item is consumed on use (like a one-time tome).
 */
public class SpellbookItem extends Item {

    public SpellbookItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) {
            Minecraft.getInstance().setScreen(new SpellSelectScreen());
        }
        return InteractionResult.SUCCESS;
    }

    /**
     * Creates the special named ItemStack for this spellbook.
     * This is what gets placed in loot tables and villager trades.
     */
    public static ItemStack createSpellbookStack(Item spellbookItem) {
        ItemStack stack = new ItemStack(spellbookItem);
        stack.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,
                Component.literal("§6§lSpellbook of the Rift"));
        return stack;
    }
}