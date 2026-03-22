package com.eilai.runeterra.network;

import com.eilai.runeterra.champion.PlayerChampionData;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import com.eilai.runeterra.network.SyncChampionDataPacket;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Sent from client → server when player presses Ctrl + ability key.
 * Server validates and ranks up the ability if allowed.
 *
 * Slot: 0=Q, 1=W, 2=E, 3=R
 */
public record UpgradeAbilityPacket(int slot) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<UpgradeAbilityPacket> TYPE =
            new CustomPacketPayload.Type<>(
                    Identifier.fromNamespaceAndPath("runeterra", "upgrade_ability"));

    public static final StreamCodec<ByteBuf, UpgradeAbilityPacket> STREAM_CODEC =
            ByteBufCodecs.INT.map(UpgradeAbilityPacket::new, UpgradeAbilityPacket::slot);

    @Override
    public CustomPacketPayload.Type<UpgradeAbilityPacket> type() { return TYPE; }

    public static void handle(UpgradeAbilityPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player)) return;

            PlayerChampionData data = PlayerChampionData.get(player);
            int available = data.availableSkillPoints();

            if (available <= 0) {
                player.displayClientMessage(
                        Component.literal("§cNo skill points available!"), true);
                return;
            }

            boolean success = data.rankUpAbility(packet.slot());
            if (success) {
                String[] names = {"Q", "W", "E", "R"};
                String slot = packet.slot() >= 0 && packet.slot() < 4
                        ? names[packet.slot()] : "?";
                int newRank = switch (packet.slot()) {
                    case 0 -> data.getQRank(data.getChampionId());
                    case 1 -> data.getWRank(data.getChampionId());
                    case 2 -> data.getERank(data.getChampionId());
                    case 3 -> data.getRRank(data.getChampionId());
                    default -> 0;
                };
                player.displayClientMessage(
                        Component.literal("§a" + slot + " upgraded to rank §f" + newRank + "§a!"), true);
                // Sync to client so HUD updates immediately
                PacketDistributor.sendToPlayer(player, SyncChampionDataPacket.from(player));
            } else {
                player.displayClientMessage(
                        Component.literal("§cCan't upgrade that ability yet!"), true);
            }
        });
    }
}