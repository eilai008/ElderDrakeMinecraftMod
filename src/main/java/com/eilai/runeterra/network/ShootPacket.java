package com.eilai.runeterra.network;

import com.eilai.runeterra.champion.PlayerChampionData;
import com.eilai.runeterra.item.weapon.VayneWeaponItem;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Sent from client → server when the player right-clicks to shoot.
 * Server validates champion and cooldown, then fires the arrow.
 */
public record ShootPacket() implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ShootPacket> TYPE =
            new CustomPacketPayload.Type<>(
                    Identifier.fromNamespaceAndPath("runeterra", "shoot"));

    public static final StreamCodec<ByteBuf, ShootPacket> STREAM_CODEC =
            StreamCodec.unit(new ShootPacket());

    @Override
    public CustomPacketPayload.Type<ShootPacket> type() { return TYPE; }

    public static void handle(ShootPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player)) return;
            PlayerChampionData data = PlayerChampionData.get(player);

            // Only fire if weapon slot is selected (player pressed 0)
            if (!data.isWeaponSlotSelected()) return;
            // Route to correct champion weapon
            switch (data.getChampionId()) {
                case "vayne" -> VayneWeaponItem.tryFire(player);
            }
        });
    }
}