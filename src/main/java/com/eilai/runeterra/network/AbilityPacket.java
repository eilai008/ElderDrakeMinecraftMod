package com.eilai.runeterra.network;

import com.eilai.runeterra.champion.PlayerChampionData;
import com.eilai.runeterra.champion.ability.VayneAbilities;
import com.eilai.runeterra.champion.spell.PlayerSpellData;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Sent from client → server when a player presses an ability key.
 *
 * Slot values:
 *  0 = Q ability
 *  1 = W ability
 *  2 = E ability
 *  3 = R ability (ultimate)
 *  4 = D spell (F key)
 *  5 = F spell (C key)
 */
public record AbilityPacket(int slot) implements CustomPacketPayload {

    public static final Type<AbilityPacket> TYPE =
            new Type<>(
                    Identifier.fromNamespaceAndPath("runeterra", "ability"));

    public static final StreamCodec<ByteBuf, AbilityPacket> STREAM_CODEC =
            ByteBufCodecs.INT.map(AbilityPacket::new, AbilityPacket::slot);

    @Override
    public Type<AbilityPacket> type() { return TYPE; }

    // ── Server handler ────────────────────────────────────────────────────────

    public static void handle(AbilityPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player)) return;

            PlayerChampionData data = PlayerChampionData.get(player);
            String champId = data.getChampionId();

            switch (packet.slot()) {
                case 0 -> executeAbility(champId, "Q", player, data);
                case 1 -> executeAbility(champId, "W", player, data);
                case 2 -> executeAbility(champId, "E", player, data);
                case 3 -> executeAbility(champId, "R", player, data);
                case 4 -> executeSpellD(player, data);
                case 5 -> executeSpellF(player, data);
            }
        });
    }

    private static void executeAbility(String champId, String slot,
                                        ServerPlayer player, PlayerChampionData data) {
        switch (champId) {
            case "vayne" -> VayneAbilities.execute(slot, player, data);
            // Add more champions here as you implement them:
            // case "garen" -> GarenAbilities.execute(slot, player, data);
        }
    }

    private static void executeSpellD(ServerPlayer player, PlayerChampionData data) {
        PlayerSpellData spells = data.getSpellData();
        if (!spells.isSpellDReady()) {
            long secsLeft = spells.getCooldownD() / 20;
            player.displayClientMessage(
                    net.minecraft.network.chat.Component.literal(
                            "§c" + spells.getSpellD().getDisplayName() + " on cooldown! " + secsLeft + "s"), true);
            return;
        }
        if (spells.useSpellD()) {
            spells.getSpellD().execute(player);
        }
    }

    private static void executeSpellF(ServerPlayer player, PlayerChampionData data) {
        PlayerSpellData spells = data.getSpellData();
        if (!spells.isSpellFReady()) {
            long secsLeft = spells.getCooldownF() / 20;
            player.displayClientMessage(
                    net.minecraft.network.chat.Component.literal(
                            "§c" + spells.getSpellF().getDisplayName() + " on cooldown! " + secsLeft + "s"), true);
            return;
        }
        if (spells.useSpellF()) {
            spells.getSpellF().execute(player);
        }
    }
}
