package com.eilai.runeterra.network;

import com.eilai.runeterra.champion.PlayerChampionData;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Server → Client sync packet.
 * Sent whenever the player's champion, level, XP, or ability ranks change.
 * The client updates its local PlayerChampionData so the HUD stays current.
 */
public record SyncChampionDataPacket(
        String championId,
        int level,
        int xp,
        int qRank,
        int wRank,
        int eRank,
        int rRank,
        int availablePoints,
        int spellDCooldown,
        int spellFCooldown
) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SyncChampionDataPacket> TYPE =
            new CustomPacketPayload.Type<>(
                    Identifier.fromNamespaceAndPath("runeterra", "sync_champion"));

    public static final StreamCodec<ByteBuf, SyncChampionDataPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8,          SyncChampionDataPacket::championId,
                    ByteBufCodecs.INT,                  SyncChampionDataPacket::level,
                    ByteBufCodecs.INT,                  SyncChampionDataPacket::xp,
                    ByteBufCodecs.INT,                  SyncChampionDataPacket::qRank,
                    ByteBufCodecs.INT,                  SyncChampionDataPacket::wRank,
                    ByteBufCodecs.INT,                  SyncChampionDataPacket::eRank,
                    ByteBufCodecs.INT,                  SyncChampionDataPacket::rRank,
                    ByteBufCodecs.INT,                  SyncChampionDataPacket::availablePoints,
                    ByteBufCodecs.INT,                  SyncChampionDataPacket::spellDCooldown,
                    ByteBufCodecs.INT,                  SyncChampionDataPacket::spellFCooldown,
                    SyncChampionDataPacket::new);

    @Override
    public CustomPacketPayload.Type<SyncChampionDataPacket> type() { return TYPE; }

    // ── Client handler ────────────────────────────────────────────────────────

    public static void handle(SyncChampionDataPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.player == null) return;

            PlayerChampionData data = PlayerChampionData.get(mc.player);

            // Update all fields from server
            data.forceSetChampion(packet.championId());

            // Directly update the progress array via rankUpAbility won't work cleanly,
            // so we use a dedicated sync method
            data.syncFromServer(
                    packet.championId(),
                    packet.level(),
                    packet.xp(),
                    packet.qRank(),
                    packet.wRank(),
                    packet.eRank(),
                    packet.rRank()
            );

            // Sync spell cooldowns
            data.getSpellData().syncCooldowns(packet.spellDCooldown(), packet.spellFCooldown());
        });
    }

    // ── Static factory — build from server player data ────────────────────────

    public static SyncChampionDataPacket from(net.minecraft.server.level.ServerPlayer player) {
        PlayerChampionData data = PlayerChampionData.get(player);
        String id = data.getChampionId();
        return new SyncChampionDataPacket(
                id,
                data.getLevel(id),
                data.getXP(id),
                data.getQRank(id),
                data.getWRank(id),
                data.getERank(id),
                data.getRRank(id),
                data.availableSkillPoints(),
                data.getSpellData().getCooldownD(),
                data.getSpellData().getCooldownF()
        );
    }
}
