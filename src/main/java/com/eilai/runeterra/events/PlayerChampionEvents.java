package com.eilai.runeterra.events;

import com.eilai.runeterra.champion.ChampionWeaponSlot;
import com.eilai.runeterra.champion.LeagueXPHelper;
import com.eilai.runeterra.champion.PlayerChampionData;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = "runeterra")
public class PlayerChampionEvents {

    // ── First join: force champion select screen ──────────────────────────────

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer serverPlayer)) return;

        PlayerChampionData data = PlayerChampionData.get(serverPlayer);

        // Validate weapon slot on every login (handles server restarts)
        ChampionWeaponSlot.validateSlot(serverPlayer);

        if (!data.hasSelectedOnce()) {
            serverPlayer.sendSystemMessage(
                    Component.literal("§6§lWelcome to Runeterra! Choose your champion."));
            // TODO: send OpenChampionSelectPacket to client
            // ModPackets.sendToPlayer(new OpenChampionSelectPacket(), serverPlayer);
        }
    }

    // ── Respawn: restore weapon slot after death ──────────────────────────────

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer serverPlayer)) return;
        // Give a tick for inventory to settle, then validate
        ChampionWeaponSlot.validateSlot(serverPlayer);
    }

    // ── Player clone (respawn/dimension): copy data ───────────────────────────

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (!(event.getEntity() instanceof ServerPlayer newPlayer)) return;
        if (!(event.getOriginal() instanceof ServerPlayer oldPlayer)) return;

        // NeoForge attachments are copied automatically, but weapon slot
        // needs to be re-validated after clone
        if (!event.isWasDeath()) {
            ChampionWeaponSlot.validateSlot(newPlayer);
        }
    }

    // ── Combat tracking ───────────────────────────────────────────────────────

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            PlayerChampionData data = PlayerChampionData.get(serverPlayer);
            data.setLastCombatTick(serverPlayer.level().getGameTime());
        }
        if (event.getSource().getEntity() instanceof ServerPlayer attacker) {
            PlayerChampionData data = PlayerChampionData.get(attacker);
            data.setLastCombatTick(attacker.level().getGameTime());
        }
    }

    // ── Mob kill: award League XP ─────────────────────────────────────────────

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer killer)) return;

        LivingEntity victim = event.getEntity();
        if (victim instanceof Player) return;

        PlayerChampionData data = PlayerChampionData.get(killer);
        if (data.getChampionId().equals("no_champion")) return;

        int xp = LeagueXPHelper.getMobXP(victim);
        if (xp <= 0) return;

        boolean leveledUp = data.addXP(xp);

        if (leveledUp) {
            int newLevel = data.getCurrentLevel();
            killer.sendSystemMessage(
                    Component.literal("§6§l▲ Level Up! §eYou are now level §6" + newLevel + "§e!"));

            int points = data.availableSkillPoints();
            if (points > 0) {
                killer.sendSystemMessage(
                        Component.literal("§b✦ You have §f" + points + "§b skill point(s) to spend!"));
            }

            int rRank = data.getRRank(data.getChampionId());
            if (LeagueXPHelper.canRankUltimate(newLevel, rRank) && rRank < 3) {
                killer.sendSystemMessage(
                        Component.literal("§d✦ Your Ultimate (F) can now be ranked up!"));
            }
        }

        killer.displayClientMessage(Component.literal("§a+" + xp + " XP"), true);
    }

    // ── Champion switch: apply weapon + skin + stats ──────────────────────────

    /**
     * Called whenever a champion is successfully selected/switched.
     * This is the central hook — call this from your network packet handler
     * on the server side when the client confirms a champion selection.
     */
    public static void onChampionSelected(ServerPlayer player, String newChampionId) {
        // Swap the weapon in slot 0
        ChampionWeaponSlot.onChampionChanged(player, newChampionId);

        player.sendSystemMessage(
                Component.literal("§6Champion set to: §e" + newChampionId));

        // TODO: equip champion skin texture
        // SkinManager.applySkin(player, newChampionId);

        // TODO: apply champion-specific attribute modifiers
        // ChampionAttributeHandler.apply(player, newChampionId);
    }
}
