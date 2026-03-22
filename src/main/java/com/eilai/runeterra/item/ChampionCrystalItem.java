package com.eilai.runeterra.item;

import com.eilai.runeterra.champion.PlayerChampionData;
import com.eilai.runeterra.client.screen.ChampionSelectScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.List;

public class ChampionCrystalItem extends Item {

    public ChampionCrystalItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) {
            openScreenIfAllowed(player);
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide());
    }

    @OnlyIn(Dist.CLIENT)
    private void openScreenIfAllowed(Player player) {
        PlayerChampionData data = PlayerChampionData.get(player);
        long gameTick = player.level().getGameTime();

        // First time — always open
        if (!data.hasSelectedOnce()) {
            net.minecraft.client.Minecraft.getInstance()
                    .setScreen(new ChampionSelectScreen());
            return;
        }

        // Combat check
        if (data.getLastCombatTick() >= 0
                && (gameTick - data.getLastCombatTick()) < PlayerChampionData.OUT_OF_COMBAT_TICKS) {
            long ticksLeft = PlayerChampionData.OUT_OF_COMBAT_TICKS
                    - (gameTick - data.getLastCombatTick());
            long secondsLeft = ticksLeft / 20;
            player.displayClientMessage(
                    Component.literal("§cYou were in combat recently! Wait " + secondsLeft + "s."),
                    true);
            return;
        }

        // Cooldown check
        long now = System.currentTimeMillis();
        long elapsed = now - data.getLastSwitchMs();
        if (elapsed < PlayerChampionData.SWITCH_COOLDOWN_MS) {
            long minutesLeft = (PlayerChampionData.SWITCH_COOLDOWN_MS - elapsed) / 60000;
            player.displayClientMessage(
                    Component.literal("§cChampion switch on cooldown! " + minutesLeft + " min remaining."),
                    true);
            return;
        }

        net.minecraft.client.Minecraft.getInstance()
                .setScreen(new ChampionSelectScreen());
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("§7Right-click to open Champion Select."));
        tooltip.add(Component.literal("§8Requires 5 min out of combat + 1h cooldown."));
    }
}
