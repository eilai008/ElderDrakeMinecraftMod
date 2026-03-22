package com.eilai.runeterra.init;

import com.eilai.runeterra.network.AbilityPacket;
import com.eilai.runeterra.network.ChampionSelectPacket;
import com.eilai.runeterra.network.ShootPacket;
import com.eilai.runeterra.network.ToggleWeaponSlotPacket;
import com.eilai.runeterra.network.SyncChampionDataPacket;
import com.eilai.runeterra.network.UpgradeAbilityPacket;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class ModNetwork {

    public static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");

        // Player pressed an ability key
        registrar.playToServer(
                AbilityPacket.TYPE,
                AbilityPacket.STREAM_CODEC,
                AbilityPacket::handle);

        // Player confirmed champion selection
        registrar.playToServer(
                ChampionSelectPacket.TYPE,
                ChampionSelectPacket.STREAM_CODEC,
                ChampionSelectPacket::handle);

        // Player upgraded an ability (Ctrl + key)
        registrar.playToServer(
                UpgradeAbilityPacket.TYPE,
                UpgradeAbilityPacket.STREAM_CODEC,
                UpgradeAbilityPacket::handle);

        registrar.playToServer(
                ToggleWeaponSlotPacket.TYPE,
                ToggleWeaponSlotPacket.STREAM_CODEC,
                ToggleWeaponSlotPacket::handle);

        // Client → Server: player right-clicked to shoot weapon
        registrar.playToServer(
                ShootPacket.TYPE,
                ShootPacket.STREAM_CODEC,
                ShootPacket::handle);

        // Server → Client: sync champion data after any change
        registrar.playToClient(
                SyncChampionDataPacket.TYPE,
                SyncChampionDataPacket.STREAM_CODEC,
                SyncChampionDataPacket::handle);
    }
}