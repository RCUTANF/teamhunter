package com.rcutanf.teamhunter;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.MinecraftServer;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

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

    public static void setPhase(MinecraftServer server, Phase phase, Duration countDown, Runnable then) {
        setPhase(server, phase);
        startCountDown(server, countDown, then);
    }

    public static void setPhase(MinecraftServer server, Phase phase, Duration countDown, Phase nextPhase) {
        setPhase(server, phase);
        startCountDown(server, countDown, () -> {
            setPhase(server, nextPhase);
        });
    }


    public static void startCountDown(MinecraftServer server, Duration duration, Runnable then) {
        counterTask.cancel(false);
        onCompleteTask.cancel(false);
        counterTask = executor.scheduleAtFixedRate(() -> {
            broadcastPacket(server, new NetWorking.CounterSyncPacket(duration.toMillis()));
        }, 0, 500, TimeUnit.MILLISECONDS);
        onCompleteTask = executor.schedule(() -> {
            counterTask.cancel(false);
            if (then != null)
                then.run();
        }, duration.toMillis(), TimeUnit.MILLISECONDS);
    }

    private static ScheduledFuture<?> counterTask;
    private static ScheduledFuture<?> onCompleteTask;

    private static final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

}
