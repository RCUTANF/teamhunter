package com.rcutanf.teamhunter.client;

import com.rcutanf.teamhunter.NetWorking.CounterSyncPacket;
import com.rcutanf.teamhunter.Phase;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.text.Text;

public class TeamhunterClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        PayloadTypeRegistry.playS2C().register(Phase.ID, Phase.CODEC);
        PayloadTypeRegistry.playS2C().register(CounterSyncPacket.ID, CounterSyncPacket.CODEC);

        ClientPlayNetworking.registerGlobalReceiver(Phase.ID, (payload, context) -> {
            phase = payload;
            context.player().sendMessage(Text.literal("Phase: " + phase.name()), false);
        });

        ClientPlayNetworking.registerGlobalReceiver(CounterSyncPacket.ID, (payload, context) -> {
            counter = payload.countDownMilliseconds();
            context.player().sendMessage(Text.literal(String.valueOf(counter)), false);
        });
    }

    public static Phase phase = Phase.WAITING;
    public static long counter = 0;

    public static boolean shouldShowCountDown() {
        return phase.countDown;
    }
}
