package com.eilai.runeterra.network;

import com.eilai.runeterra.champion.PlayerChampionData;
import com.eilai.runeterra.events.PlayerChampionEvents;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Sent from client → server when player confirms champion selection.
 * Server validates cooldowns, sets the champion, and equips the weapon.
 */
public record ChampionSelectPacket(String championId) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ChampionSelectPacket> TYPE =
            new CustomPacketPayload.Type<>(
                    Identifier.fromNamespaceAndPath("runeterra", "champion_select"));

    public static final StreamCodec<ByteBuf, ChampionSelectPacket> STREAM_CODEC =
            ByteBufCodecs.STRING_UTF8.map(ChampionSelectPacket::new, ChampionSelectPacket::championId);

    @Override
    public CustomPacketPayload.Type<ChampionSelectPacket> type() { return TYPE; }

    public static void handle(ChampionSelectPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player)) return;

            PlayerChampionData data = PlayerChampionData.get(player);

            // First-time selection always works — no cooldown check
            if (!data.hasSelectedOnce()) {
                data.forceSetChampion(packet.championId());
                PlayerChampionEvents.onChampionSelected(player, packet.championId());
                return;
            }

            // Subsequent changes go through cooldown check
            int result = data.trySetChampion(packet.championId(), player.level().getGameTime());
            switch (result) {
                case 0 -> PlayerChampionEvents.onChampionSelected(player, packet.championId());
                case 1 -> player.displayClientMessage(
                        net.minecraft.network.chat.Component.literal("§cYou were in combat recently!"), true);
                case 2 -> player.displayClientMessage(
                        net.minecraft.network.chat.Component.literal("§cChampion switch on cooldown!"), true);
            }
        });
    }
}