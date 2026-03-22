package com.eilai.runeterra.item;

import com.eilai.runeterra.client.screen.SpellSelectScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * The Spellbook — lets players change their D/F summoner spells.
 *
 * Created as a signed written book by "TheOwl1234".
 * Found in village chests or bought from Librarian villagers.
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
            openSpellSelect();
        }
        return InteractionResult.SUCCESS;
    }

    @OnlyIn(Dist.CLIENT)
    private void openSpellSelect() {
        net.minecraft.client.Minecraft.getInstance()
                .setScreen(new SpellSelectScreen());
    }

    /**
     * Creates the special signed-book-looking ItemStack for this spellbook.
     * This is what gets placed in loot tables and villager trades.
     */
    public static ItemStack createSpellbookStack(Item spellbookItem) {
        ItemStack stack = new ItemStack(spellbookItem);
        stack.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,
                Component.literal("§6§lSpellbook of the Rift"));
        return stack;
    }
}
