package com.rcutanf.teamhunter;

import com.rcutanf.teamhunter.advancement.AdvancementListener;
import com.rcutanf.teamhunter.loot.TeamhunterLootConditions;
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
    private AdvancementListener advancementListener;

    @Override
    public void onInitialize() {
        PayloadTypeRegistry.playS2C().register(Phase.ID, Phase.CODEC);
        PayloadTypeRegistry.playS2C().register(NetWorking.CounterSyncPacket.ID, NetWorking.CounterSyncPacket.CODEC);
        PayloadTypeRegistry.playS2C().register(NetWorking.TeamAdvantagePacket.ID, NetWorking.TeamAdvantagePacket.CODEC);
        PayloadTypeRegistry.playS2C().register(NetWorking.TeamScorePacket.ID, NetWorking.TeamScorePacket.CODEC);
        CommandRegistrationCallback.EVENT.register(Command::register);
        ServerLifecycleEvents.SERVER_STARTED.register(s -> {
            phaseManager = new PhaseCombiner(s, new PhaseHandler(s));
            playerRespawnHandler = new PlayerRespawnHandler();
            environmentController = new EnvironmentController(s);
            advancementListener = new AdvancementListener(s);
        });
        TeamhunterLootConditions.register();
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
                    sender.sendPacket(NetWorking.CHECK_CLIENT_MOD, buf);
                }
        );

        ServerLoginNetworking.registerGlobalReceiver(NetWorking.CHECK_CLIENT_MOD, (server, handler, understood, buf, synchronizer, packetSender) -> {
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
