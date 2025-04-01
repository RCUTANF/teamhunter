package com.rcutanf.teamhunter;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.MinecraftServer;

import java.time.Duration;
import java.util.Objects;
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

    private static void setPhase(MinecraftServer server, Phase phase) {
        _phase = phase;
        broadcastPacket(server, phase);
    }

    private static CompletableFuture<Void> setPhase(MinecraftServer server, Phase phase, Duration countDown) {
        setPhase(server, phase);
        return ticker.start(server, countDown, Duration.ofMillis(500));
    }

    public static final Ticker<MinecraftServer> ticker = new Ticker<>((c, d) -> {
        broadcastPacket(c, new NetWorking.CounterSyncPacket(d.toMillis()));
    });

    public static PhaseCombiner startPhase(MinecraftServer server, Phase phase, Duration countDown) {
        return new PhaseCombiner(server, setPhase(server, phase, countDown));
    }

    public static final class PhaseCombiner {
        private final MinecraftServer server;
        private CompletableFuture<Void> future;

        public PhaseCombiner(MinecraftServer server, CompletableFuture<Void> future) {
            this.server = server;
            this.future = future;
        }

        public PhaseCombiner then(Phase phase, Duration countDown) {
            future = future.thenCompose(v -> setPhase(server, phase, countDown));
            return this;
        }

        public void then(Phase phase) {
            future.thenAccept(v -> setPhase(server, phase));
        }

    }


}
