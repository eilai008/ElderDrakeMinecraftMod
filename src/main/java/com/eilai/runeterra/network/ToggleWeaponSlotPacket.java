package com.eilai.runeterra.network;

import com.eilai.runeterra.champion.PlayerChampionData;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Sent when the player presses key 0 to toggle the weapon slot.
 * Server updates PlayerChampionData.weaponSlotSelected.
 */
public record ToggleWeaponSlotPacket() implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ToggleWeaponSlotPacket> TYPE =
            new CustomPacketPayload.Type<>(
                    Identifier.fromNamespaceAndPath("runeterra", "toggle_weapon"));

    public static final StreamCodec<ByteBuf, ToggleWeaponSlotPacket> STREAM_CODEC =
            StreamCodec.unit(new ToggleWeaponSlotPacket());

    @Override
    public CustomPacketPayload.Type<ToggleWeaponSlotPacket> type() { return TYPE; }

    public static void handle(ToggleWeaponSlotPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player)) return;
            PlayerChampionData data = PlayerChampionData.get(player);
            data.toggleWeaponSlot();
        });
    }
}
