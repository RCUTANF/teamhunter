package com.rcutanf.teamhunter;

import com.rcutanf.teamhunter.advancement.AdvancementListener;
import com.rcutanf.teamhunter.loot.TeamhunterLootConditions;
import com.rcutanf.teamhunter.shop.ShopCommand;
import com.rcutanf.teamhunter.shop.ShopComponentTypes;
import com.rcutanf.teamhunter.shop.ShopManager;
import io.netty.buffer.Unpooled;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.*;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Text;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class Teamhunter implements ModInitializer {

    public static final String MOD_ID = "teamhunter";
    public static PhaseCombiner phaseManager;
    private PlayerRespawnHandler playerRespawnHandler;
    private EnvironmentController environmentController;
    private AdvancementListener advancementListener;

    public static void broadcastPacket(MinecraftServer server, CustomPayload payload) {
        server.getPlayerManager()
                .getPlayerList()
                .forEach(player -> ServerPlayNetworking.send(player, payload));
    }

    @Override
    public void onInitialize() {
        PayloadTypeRegistry.playS2C().register(Phase.ID, Phase.CODEC);
        PayloadTypeRegistry.playS2C().register(NetWorking.CounterSyncPacket.ID, NetWorking.CounterSyncPacket.CODEC);
        PayloadTypeRegistry.playS2C().register(NetWorking.TeamAdvantagePacket.ID, NetWorking.TeamAdvantagePacket.CODEC);
        PayloadTypeRegistry.playS2C().register(NetWorking.TeamScorePacket.ID, NetWorking.TeamScorePacket.CODEC);
        PayloadTypeRegistry.playS2C().register(NetWorking.AdvantageBuffPacket.ID, NetWorking.AdvantageBuffPacket.CODEC);
        // 在 Teamhunter.java 的 onInitialize 方法中添加
        PayloadTypeRegistry.playC2S().register(NetWorking.ShopItemsRequestPacket.ID, NetWorking.ShopItemsRequestPacket.CODEC);
        PayloadTypeRegistry.playS2C().register(NetWorking.ShopItemsResponsePacket.ID, NetWorking.ShopItemsResponsePacket.CODEC);


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

        // 在其他命令注册之后添加
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            ShopCommand.register(dispatcher);
        });
        CommandConfig.loadConfig();

        // 注册商店物品请求处理器
        ServerPlayNetworking.registerGlobalReceiver(NetWorking.ShopItemsRequestPacket.ID, (packet, context) -> {
            // 获取商店物品列表
            List<ItemStack> shopItems = ShopManager.getAllItems(context.player());
            // 创建响应数据包
            NetWorking.ShopItemsResponsePacket responsePacket = new NetWorking.ShopItemsResponsePacket(shopItems);
            // 发送响应到客户端
            ServerPlayNetworking.send(context.player(), responsePacket);
        });

        new ShopComponentTypes();


    }


}
