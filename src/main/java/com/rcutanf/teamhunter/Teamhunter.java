package com.rcutanf.teamhunter;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.MinecraftServer;

import java.time.Duration;
import java.util.concurrent.*;

public class Teamhunter implements ModInitializer {

    public static final String MOD_ID = "teamhunter";

    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register(Command::register);
    }

    private static Phase _phase = Phase.WAITING;

    private static void broadcastPacket(MinecraftServer server, CustomPayload payload) {
        server.getPlayerManager()
                .getPlayerList()
                .forEach(player -> ServerPlayNetworking.send(player, payload));
    }

    public static Phase getPhase() {
        return _phase;
    }

    public static void setPhase(MinecraftServer server, Phase phase) {
        _phase = phase;
        broadcastPacket(server, phase);
    }

    public static CompletableFuture<Void> setPhase(MinecraftServer server, Phase phase, Duration countDown) {
        setPhase(server, phase);
        return ticker.start(server, countDown, Duration.ofMillis(500));
    }

    public static final Ticker<MinecraftServer> ticker = new Ticker<>((c, d) -> {
        broadcastPacket(c, new NetWorking.CounterSyncPacket(d.toMillis()));
    });


}
