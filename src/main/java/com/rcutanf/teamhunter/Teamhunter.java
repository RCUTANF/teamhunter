package com.rcutanf.teamhunter;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.MinecraftServer;

import java.util.concurrent.atomic.AtomicBoolean;

public class Teamhunter implements ModInitializer {

    public static final String MOD_ID = "teamhunter";

    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register(Command::register);
    }

    private static final AtomicBoolean started = new AtomicBoolean(false);

    private static void broadcastPacket(MinecraftServer server, CustomPayload payload) {
        server.getPlayerManager()
                .getPlayerList()
                .forEach(player -> ServerPlayNetworking.send(player, payload));
    }

    public static void startInServer(MinecraftServer server, int countDownTicks) {
        if (countDownTicks <= 0) return;
        if (started.compareAndSet(false, true)) {
            broadcastPacket(server, new NetWorking.CounterTogglePacket(countDownTicks));
        }
    }

    public static void stopInServer(MinecraftServer server) {
        if (started.compareAndSet(true, false)) {
            broadcastPacket(server, new NetWorking.CounterTogglePacket(0));
        }
    }
}
