package com.rcutanf.teamhunter;

import io.netty.buffer.Unpooled;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.*;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Text;

import java.util.concurrent.CompletableFuture;

public class Teamhunter implements ModInitializer {

    public static final String MOD_ID = "teamhunter";
    private PlayerRespawnHandler playerRespawnHandler;
    private EnvironmentController environmentController;

    @Override
    public void onInitialize() {
        PayloadTypeRegistry.playS2C().register(Phase.ID, Phase.CODEC);
        PayloadTypeRegistry.playS2C().register(NetWorking.CounterSyncPacket.ID, NetWorking.CounterSyncPacket.CODEC);
        CommandRegistrationCallback.EVENT.register(Command::register);
        ServerLifecycleEvents.SERVER_STARTED.register(s -> {
            phaseManager = new PhaseCombiner(s, new PhaseHandler(s));
            playerRespawnHandler = new PlayerRespawnHandler();
            environmentController = new EnvironmentController(s);
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(s -> {
            try {
                phaseManager.close();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            phaseManager = null;
        });

        ServerLoginConnectionEvents.QUERY_START.register(
                (handler, server, sender, synchronizer) -> {
                    var future = CompletableFuture.runAsync(() -> {
                    });
                    synchronizer.waitFor(future);
                    var buf = new PacketByteBuf(Unpooled.buffer());
                    sender.sendPacket(NetWorking.CheckClientMod, buf);
                }
        );

        ServerLoginNetworking.registerGlobalReceiver(NetWorking.CheckClientMod, (server, handler, understood, buf, synchronizer, packetSender) -> {
            if (!understood)
                handler.disconnect(Text.literal("install " + MOD_ID + " to join the server"));
        });
        ServerPlayConnectionEvents.JOIN.register(((handler, sender, server) -> {
            var phase = phaseManager.Phase();
            ServerPlayNetworking.send(handler.player, phase);
        }));

    }


    public static void broadcastPacket(MinecraftServer server, CustomPayload payload) {
        server.getPlayerManager()
                .getPlayerList()
                .forEach(player -> ServerPlayNetworking.send(player, payload));
    }

    public static PhaseCombiner phaseManager;


}
