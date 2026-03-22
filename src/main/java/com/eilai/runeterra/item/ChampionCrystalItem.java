package com.eilai.runeterra.item;

import com.eilai.runeterra.champion.PlayerChampionData;
import com.eilai.runeterra.client.screen.ChampionSelectScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Champion Crystal — right-click to open Champion Select screen.
 *
 * Tooltip removed to avoid TooltipContext signature issues.
 * The item name itself acts as a sufficient indicator.
 */
public class ChampionCrystalItem extends Item {

    public ChampionCrystalItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) {
            openScreenIfAllowed(player);
        }
        return InteractionResult.SUCCESS;
    }

    private void openScreenIfAllowed(Player player) {
        PlayerChampionData data = PlayerChampionData.get(player);
        long gameTick = player.level().getGameTime();

        if (!data.hasSelectedOnce()) {
            net.minecraft.client.Minecraft.getInstance()
                    .setScreen(new ChampionSelectScreen());
            return;
        }

        if (data.getLastCombatTick() >= 0
                && (gameTick - data.getLastCombatTick()) < PlayerChampionData.OUT_OF_COMBAT_TICKS) {
            long secondsLeft = (PlayerChampionData.OUT_OF_COMBAT_TICKS
                    - (gameTick - data.getLastCombatTick())) / 20;
            player.displayClientMessage(
                    Component.literal("§cStill in combat! Wait " + secondsLeft + "s."), true);
            return;
        }

        long elapsed = System.currentTimeMillis() - data.getLastSwitchMs();
        if (elapsed < PlayerChampionData.SWITCH_COOLDOWN_MS) {
            long minutesLeft = (PlayerChampionData.SWITCH_COOLDOWN_MS - elapsed) / 60000;
            player.displayClientMessage(
                    Component.literal("§cCooldown! " + minutesLeft + " min remaining."), true);
            return;
        }

        net.minecraft.client.Minecraft.getInstance()
                .setScreen(new ChampionSelectScreen());
    }
}