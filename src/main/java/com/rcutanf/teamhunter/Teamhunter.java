package com.rcutanf.teamhunter;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.MinecraftServer;

public class Teamhunter implements ModInitializer {

    public static final String MOD_ID = "teamhunter";

    @Override
    public void onInitialize() {
        PayloadTypeRegistry.playS2C().register(Phase.ID, Phase.CODEC);
        PayloadTypeRegistry.playS2C().register(NetWorking.CounterSyncPacket.ID, NetWorking.CounterSyncPacket.CODEC);
        CommandRegistrationCallback.EVENT.register(Command::register);
        ServerLifecycleEvents.SERVER_STARTED.register(
                s -> phaseManager = new PhaseCombiner(s, new PhaseHandler(s))
        );
        ServerLifecycleEvents.SERVER_STOPPING.register(s -> {
            phaseManager.clear();
            phaseManager = null;
        });
    }


    public static void broadcastPacket(MinecraftServer server, CustomPayload payload) {
        server.getPlayerManager()
                .getPlayerList()
                .forEach(player -> ServerPlayNetworking.send(player, payload));
    }

    public static PhaseCombiner phaseManager;

}
