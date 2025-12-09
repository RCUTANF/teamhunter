package com.rcutanf.teamhunter;

import com.rcutanf.teamhunter.advancement.AdvancementListener;
import com.rcutanf.teamhunter.config.GameConfigLoader;
import com.rcutanf.teamhunter.loot.TeamhunterLootConditions;
import com.rcutanf.teamhunter.shop.ShopCommand;
import com.rcutanf.teamhunter.shop.ShopComponentTypes;
import com.rcutanf.teamhunter.shop.ShopManager;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.*;
import net.minecraft.advancement.AdvancementEntry;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerAdvancementLoader;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructureStart;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.gen.structure.Structure;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class Teamhunter implements ModInitializer {

    public static final String MOD_ID = "teamhunter";
    public static PhaseCombiner phaseManager;
    private PlayerRespawnHandler playerRespawnHandler;
    private EnvironmentController environmentController;
    public static AdvancementListener advancementListener;
    private PlayerPositionTracker positionTracker;
    private PlayerVisibilityTracker playerVisibilityTracker;

    /**
     * 向所有在线玩家广播数据包
     *
     * @param server 服务器实例
     * @param payload 要发送的数据包
     */
    public static void broadcastPacket(MinecraftServer server, CustomPayload payload) {
        server.getPlayerManager()
                .getPlayerList()
                .forEach(player -> ServerPlayNetworking.send(player, payload));
    }

    @Override
    public void onInitialize() {
        // 注册网络数据包
        registerNetworkPackets();

        // 注册命令
        registerCommands();

        // 注册生命周期事件
        registerLifecycleEvents();

        // 初始化战利品条件
        TeamhunterLootConditions.register();

        // 初始化商店系统
        initShopSystem();

        // 注册网络处理器
        registerNetworkHandlers();

        // 加载配置
        GameConfigLoader.loadConfig();
    }

    /**
     * 注册所有网络数据包类型
     */
    private void registerNetworkPackets() {
        // 游戏阶段数据包
        PayloadTypeRegistry.playS2C().register(Phase.ID, Phase.CODEC);

        // 计数器和团队相关数据包
        PayloadTypeRegistry.playS2C().register(NetWorking.CounterSyncPacket.ID, NetWorking.CounterSyncPacket.CODEC);
        PayloadTypeRegistry.playS2C().register(NetWorking.TeamAdvantagePacket.ID, NetWorking.TeamAdvantagePacket.CODEC);
        PayloadTypeRegistry.playS2C().register(NetWorking.TeamScorePacket.ID, NetWorking.TeamScorePacket.CODEC);
        PayloadTypeRegistry.playS2C().register(NetWorking.AdvantageBuffPacket.ID, NetWorking.AdvantageBuffPacket.CODEC);

        // 商店相关数据包
        PayloadTypeRegistry.playC2S().register(NetWorking.ShopItemsRequestPacket.ID, NetWorking.ShopItemsRequestPacket.CODEC);
        PayloadTypeRegistry.playS2C().register(NetWorking.ShopItemsResponsePacket.ID, NetWorking.ShopItemsResponsePacket.CODEC);
        PayloadTypeRegistry.playC2S().register(NetWorking.ShopPurchasePacket.ID, NetWorking.ShopPurchasePacket.CODEC);

        // 玩家位置更新数据包
        PayloadTypeRegistry.playS2C().register(NetWorking.PlayerPositionUpdatePacket.ID, NetWorking.PlayerPositionUpdatePacket.CODEC);
        PayloadTypeRegistry.playS2C().register(NetWorking.PlayerVisibilityUpdatePacket.ID, NetWorking.PlayerVisibilityUpdatePacket.CODEC);

        // 成就数据相关数据包
        PayloadTypeRegistry.playC2S().register(NetWorking.AdvancementDataRequestPacket.ID, NetWorking.AdvancementDataRequestPacket.CODEC);
        PayloadTypeRegistry.playS2C().register(NetWorking.AdvancementDataResponsePacket.ID, NetWorking.AdvancementDataResponsePacket.CODEC);

        // 结构数据相关数据包
        PayloadTypeRegistry.playC2S().register(NetWorking.StructureDataRequestPacket.ID, NetWorking.StructureDataRequestPacket.CODEC);
        PayloadTypeRegistry.playS2C().register(NetWorking.StructureDataResponsePacket.ID, NetWorking.StructureDataResponsePacket.CODEC);

    }

    /**
     * 注册命令
     */
    private void registerCommands() {
        CommandRegistrationCallback.EVENT.register(Command::register);
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            ShopCommand.register(dispatcher);
        });
    }

    /**
     * 初始化商店系统
     */
    private void initShopSystem() {
        new ShopComponentTypes();
    }

    /**
     * 注册服务器生命周期事件
     */
    private void registerLifecycleEvents() {
        // 服务器启动事件
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            phaseManager = new PhaseCombiner(server, new PhaseHandler(server));
            playerRespawnHandler = new PlayerRespawnHandler();
            environmentController = new EnvironmentController(server);
            advancementListener = new AdvancementListener(server);
            positionTracker = new PlayerPositionTracker(server);
            playerVisibilityTracker = new PlayerVisibilityTracker(server);

            TeamUtils.checkAndCreateTeams(server);
        });

        // 服务器关闭事件
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            try {
                if (phaseManager != null) {
                    phaseManager.close();
                    phaseManager = null;
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        // 服务器tick事件
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (positionTracker != null) {
                positionTracker.tick();
            }
            if (playerVisibilityTracker != null) {
                playerVisibilityTracker.tick();
            }
        });

        // 玩家加入事件
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            // 发送当前游戏阶段给新玩家
            var phase = phaseManager.Phase();
            ServerPlayerEntity player = handler.player;
            ServerPlayNetworking.send(player, phase);


            // 为新加入的玩家默认添加烈焰棒掉落标签
            player.addCommandTag("can_drop_blaze_rod");

            // 发送所有玩家位置给新玩家

            positionTracker.sendAllPlayerPositionsToNewPlayer(player);
            playerVisibilityTracker.sendAllVisibilityToPlayer(player);
        });
    }

    /**
     * 注册网络处理器
     */
    private void registerNetworkHandlers() {
        // 登录时检查客户端是否安装模组
        ServerLoginConnectionEvents.QUERY_START.register(
                (handler, server, sender, synchronizer) -> {
                    var future = CompletableFuture.runAsync(() -> {});
                    synchronizer.waitFor(future);
                    var buf = new PacketByteBuf(Unpooled.buffer());
                    sender.sendPacket(NetWorking.CHECK_CLIENT_MOD, buf);
                }
        );

        ServerLoginNetworking.registerGlobalReceiver(NetWorking.CHECK_CLIENT_MOD, (server, handler, understood, buf, synchronizer, packetSender) -> {
            if (!understood)
                handler.disconnect(Text.literal("请安装 " + MOD_ID + " 模组以加入服务器"));
        });

        // 商店物品请求处理器
        ServerPlayNetworking.registerGlobalReceiver(NetWorking.ShopItemsRequestPacket.ID, (packet, context) -> {
            List<ItemStack> shopItems = ShopManager.getAllItems(context.player());
            NetWorking.ShopItemsResponsePacket responsePacket = new NetWorking.ShopItemsResponsePacket(shopItems);
            ServerPlayNetworking.send(context.player(), responsePacket);
        });

        // 商店购买处理器
        ServerPlayNetworking.registerGlobalReceiver(NetWorking.ShopPurchasePacket.ID, (packet, context) -> {
            ShopManager.purchaseItemByIndex(context.player(), packet.id());
        });

        // 成就请求数据处理
        ServerPlayNetworking.registerGlobalReceiver(NetWorking.AdvancementDataRequestPacket.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            ServerAdvancementLoader loader = player.getEntityWorld().getServer().getAdvancementLoader();

            List<AdvancementEntry> entries = payload.advancementIds().stream()
                    .map(id -> {
                        var placed = loader.getManager().get(id);
                        if (placed != null) {
                            return new AdvancementEntry(placed.getAdvancementEntry().id(), placed.getAdvancement());
                        } else {
                            // 发送错误消息给客户端
                            player.sendMessage(Text.literal("§c[TeamHunter] 成就数据不存在: " + id), false);
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)  // 过滤掉null值
                    .collect(Collectors.toList());

            NetWorking.AdvancementDataResponsePacket response = new NetWorking.AdvancementDataResponsePacket(entries);
            ServerPlayNetworking.send(player, response);
        });

        ServerPlayNetworking.registerGlobalReceiver(NetWorking.StructureDataRequestPacket.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            MinecraftServer server = player.getEntityWorld().getServer();

            RegistryKey<World> worldKey = RegistryKey.of(RegistryKeys.WORLD, payload.dimension());
            ServerWorld serverWorld = server.getWorld(worldKey);

            if (serverWorld != null) {
                List<NetWorking.StructureInfo> structures =
                        getStructureDataForChunk(serverWorld, payload.chunkPos());

                NetWorking.StructureDataResponsePacket response =
                        new NetWorking.StructureDataResponsePacket(payload.chunkPos(), structures);

                ServerPlayNetworking.send(player, response);
            }
        });
    }

    /**
     * 获取指定区块的结构数据
     */
    private static List<NetWorking.StructureInfo> getStructureDataForChunk(
            ServerWorld world, ChunkPos chunkPos) {
        List<NetWorking.StructureInfo> structures = new ArrayList<>();

        try {
            WorldChunk chunk = world.getChunk(chunkPos.x, chunkPos.z);
            Map<Structure, LongSet> references = chunk.getStructureReferences();

            if (references != null && !references.isEmpty()) {
                for (Map.Entry<Structure, LongSet> entry : references.entrySet()) {
                    Structure structure = entry.getKey();
                    LongSet referenceSet = entry.getValue();

                    LongIterator iterator = referenceSet.iterator();
                    while (iterator.hasNext()) {
                        long packedPos = iterator.nextLong();
                        ChunkPos refChunkPos = new ChunkPos(packedPos);

                        StructureStart structureStart = world.getChunk(refChunkPos.x, refChunkPos.z)
                                .getStructureStart(structure);

                        if (structureStart != null && structureStart.hasChildren()) {
                            BlockBox boundingBox = structureStart.getBoundingBox();
                            String structureName = world.getRegistryManager()
                                    .getOptional(RegistryKeys.STRUCTURE)
                                    .map(registry -> registry.getId(structure))
                                    .map(Object::toString)
                                    .orElse("unknown");

                            structures.add(new NetWorking.StructureInfo(
                                structureName, boundingBox));
                        }
                    }
                }
            }
        } catch (Exception e) {
            // 记录错误但不中断处理
            System.err.println("获取区块结构数据时发生错误: " + e.getMessage());
        }

        return structures;
    }
}