package com.rcutanf.teamhunter.client;

import  com.rcutanf.teamhunter.NetWorking.CounterSyncPacket;
import  com.rcutanf.teamhunter.NetWorking.CounterTogglePacket;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

public class TeamhunterClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        PayloadTypeRegistry.playS2C().register(CounterTogglePacket.ID, CounterTogglePacket.CODEC);
        PayloadTypeRegistry.playS2C().register(CounterSyncPacket.ID, CounterSyncPacket.CODEC);

        ClientPlayNetworking.registerGlobalReceiver(CounterTogglePacket.ID, (payload, context) -> {
            // Handle CounterTogglePacket
        });

        ClientPlayNetworking.registerGlobalReceiver(CounterSyncPacket.ID, (payload, context) -> {
            // Handle CounterSyncPacket
        });
    }
}
