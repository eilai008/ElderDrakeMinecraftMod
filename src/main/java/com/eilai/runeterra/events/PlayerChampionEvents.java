package com.eilai.runeterra.events;

import com.eilai.runeterra.champion.ChampionWeaponSlot;
import com.eilai.runeterra.champion.LeagueXPHelper;
import com.eilai.runeterra.champion.PlayerChampionData;
import com.eilai.runeterra.champion.ability.VayneAbilities;
import com.eilai.runeterra.init.ModItems;
import com.eilai.runeterra.item.weapon.VayneWeaponItem;
import com.eilai.runeterra.network.SyncChampionDataPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = "runeterra")
public class PlayerChampionEvents {

    // ── First join ────────────────────────────────────────────────────────────

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        PlayerChampionData data = PlayerChampionData.get(sp);
        ChampionWeaponSlot.validateSlot(sp);

        if (!data.hasSelectedOnce()) {
            sp.sendSystemMessage(Component.literal(
                    "§6§lWelcome to Runeterra! §eRight-click the §6Champion Crystal §eto choose your champion."));
            // Give the player a Champion Crystal so they can select their champion
            giveCrystalIfNotHeld(sp);
        }

        PacketDistributor.sendToPlayer(sp, SyncChampionDataPacket.from(sp));
    }

    // ── Respawn ───────────────────────────────────────────────────────────────

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        ChampionWeaponSlot.validateSlot(sp);
        PacketDistributor.sendToPlayer(sp, SyncChampionDataPacket.from(sp));
    }

    // ── Clone (death/respawn) ─────────────────────────────────────────────────

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (!(event.getEntity() instanceof ServerPlayer newPlayer)) return;
        if (event.isWasDeath()) {
            PlayerChampionData oldData = PlayerChampionData.get(event.getOriginal());
            PlayerChampionData newData = PlayerChampionData.get(newPlayer);
            newData.copyFrom(oldData);
        }
    }

    // ── Player leave ──────────────────────────────────────────────────────────

    @SubscribeEvent
    public static void onPlayerLeave(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        VayneAbilities.onPlayerLeave(sp.getUUID());
        VayneWeaponItem.clearPlayer(sp.getUUID());
    }

    // ── Per-player tick ───────────────────────────────────────────────────────

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        PlayerChampionData data = PlayerChampionData.get(sp);

        VayneWeaponItem.tickCooldowns();

        switch (data.getChampionId()) {
            case "vayne" -> VayneAbilities.tick(sp, data);
        }
    }

    // ── Combat tracking ───────────────────────────────────────────────────────

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer sp)
            PlayerChampionData.get(sp).setLastCombatTick(sp.level().getGameTime());
        if (event.getSource().getEntity() instanceof ServerPlayer sp)
            PlayerChampionData.get(sp).setLastCombatTick(sp.level().getGameTime());
    }

    // ── Silver Bolts ──────────────────────────────────────────────────────────

    @SubscribeEvent
    public static void onPlayerAttack(LivingIncomingDamageEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer attacker)) return;
        if (!(event.getEntity() instanceof LivingEntity target)) return;
        PlayerChampionData data = PlayerChampionData.get(attacker);
        if (!data.getChampionId().equals("vayne")) return;
        VayneAbilities.onVayneHit(attacker, target, data);
    }

    // ── Mob kill XP ───────────────────────────────────────────────────────────

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer killer)) return;
        LivingEntity victim = event.getEntity();
        if (victim instanceof Player) return;

        PlayerChampionData data = PlayerChampionData.get(killer);
        if (data.getChampionId().equals("no_champion")) return;

        int xp = LeagueXPHelper.getMobXP(victim);
        if (xp <= 0) return;

        int levelsGained = data.addXP(xp);
        if (levelsGained > 0) {
            int newLevel = data.getCurrentLevel();
            if (levelsGained == 1) {
                killer.sendSystemMessage(Component.literal(
                        "§6§l▲ Level Up! §eNow level §6" + newLevel + "§e!"));
            } else {
                killer.sendSystemMessage(Component.literal(
                        "§6§l▲ Level Up x" + levelsGained + "! §eNow level §6" + newLevel + "§e!"));
            }
            int points = data.availableSkillPoints();
            if (points > 0)
                killer.sendSystemMessage(Component.literal(
                        "§b✦ §f" + points + "§b skill point(s)! Press §fCtrl+Z/X/C/V §bto upgrade."));
            int rRank = data.getRRank(data.getChampionId());
            if (LeagueXPHelper.canRankUltimate(newLevel, rRank) && rRank < 3)
                killer.sendSystemMessage(Component.literal(
                        "§d✦ Ultimate (V) can now be ranked up! Press §fCtrl+V§d."));
        }

        killer.displayClientMessage(Component.literal("§a+" + xp + " XP"), true);
        PacketDistributor.sendToPlayer(killer, SyncChampionDataPacket.from(killer));
    }

    // ── Champion selected ─────────────────────────────────────────────────────

    public static void onChampionSelected(ServerPlayer player, String newChampionId) {
        ChampionWeaponSlot.onChampionChanged(player, newChampionId);
        player.sendSystemMessage(Component.literal("§6Champion set to: §e" + newChampionId));
        PacketDistributor.sendToPlayer(player, SyncChampionDataPacket.from(player));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static void giveCrystalIfNotHeld(ServerPlayer player) {
        // Check if they already have one somewhere in their inventory
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack s = player.getInventory().getItem(i);
            if (s.getItem() instanceof com.eilai.runeterra.item.ChampionCrystalItem) return;
        }
        // Give one crystal
        ItemStack crystal = new ItemStack(ModItems.CHAMPION_CRYSTAL.get());
        player.getInventory().add(crystal);
    }
}