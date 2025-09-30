package com.rcutanf.teamhunter.client.guide_sys.impl.trigger;

import com.rcutanf.teamhunter.client.guide_sys.AbstractGuideSysTrigger;
import com.rcutanf.teamhunter.client.guide_sys.TriggerType;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructureStart;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.gen.structure.Structure;

import java.util.*;

public class StructureTrigger extends AbstractGuideSysTrigger {
    private static StructureTrigger instance;
    private String currentStructureName = null;
    private BlockPos lastPlayerPos = BlockPos.ORIGIN;
    private int scanInterval = 40;
    private int tickCounter = 0;
    private boolean isEnabled = true;

    public StructureTrigger() {
        super(TriggerType.structure);
        registerEvents();
    }

    public static StructureTrigger getInstance() {
        if (instance == null) {
            instance = new StructureTrigger();
        }
        return instance;
    }

    private void registerEvents() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!isEnabled || !shouldScan()) {
                return;
            }

            tickCounter++;
            if (tickCounter >= scanInterval) {
                tickCounter = 0;
                checkStructureChange();
            }
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            currentStructureName = null;
        });
    }

    private boolean shouldScan() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) {
            return false;
        }

        BlockPos currentPos = client.player.getBlockPos();
        double distanceSq = lastPlayerPos.getSquaredDistance(currentPos);

        if (distanceSq > 64) {
            lastPlayerPos = currentPos;
            tickCounter = 0;
            return true;
        }

        return true;
    }

    public void enable() {
        this.isEnabled = true;
        this.tickCounter = 0;
    }

    public void disable() {
        this.isEnabled = false;
        currentStructureName = null;
    }

    public void setScanInterval(int interval) {
        this.scanInterval = Math.max(5, interval);
    }

    private void checkStructureChange() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) {
            return;
        }

        // 获取玩家位置
        BlockPos playerPos = client.player.getBlockPos();

        // 单人游戏时，获取集成服务器的世界
        if (client.isInSingleplayer() && client.getServer() != null) {
            RegistryKey<World> worldKey = client.world.getRegistryKey();
            ServerWorld serverWorld = client.getServer().getWorld(worldKey);

            if (serverWorld != null) {
                // 在服务器线程上执行
                MinecraftServer server = client.getServer();
                server.execute(() -> {
                    // 获取玩家所在区块
                    ChunkPos chunkPos = new ChunkPos(playerPos);
                    WorldChunk chunk = serverWorld.getChunk(chunkPos.x, chunkPos.z);

                    // 使用服务器端的结构数据
                    String structureName = null;

                    // 获取结构引用
                    Map<Structure, LongSet> references = chunk.getStructureReferences();
                    if (references != null && !references.isEmpty()) {
                        for (Map.Entry<Structure, LongSet> entry : references.entrySet()) {
                            Structure structure = entry.getKey();
                            LongSet referenceSet = entry.getValue();

                            LongIterator iterator = referenceSet.iterator();
                            while (iterator.hasNext()) {
                                long packedPos = iterator.nextLong();
                                ChunkPos refChunkPos = new ChunkPos(packedPos);

                                StructureStart structureStart = serverWorld.getChunk(refChunkPos.x, refChunkPos.z)
                                        .getStructureStart(structure);

                                if (structureStart != null && structureStart.hasChildren()) {
                                    BlockBox boundingBox = structureStart.getBoundingBox();
                                    if (boundingBox.contains(playerPos)) {
                                        structureName = serverWorld.getRegistryManager()
                                                .getOptional(RegistryKeys.STRUCTURE)
                                                .map(registry -> registry.getId(structure))
                                                .map(Object::toString)
                                                .orElse(null);
                                        break;
                                    }
                                }
                            }

                            if (structureName != null) {
                                break;
                            }
                        }
                    }

                    // 使用最终的结构名称更新状态并触发事件
                    String finalStructureName = structureName;
                    client.execute(() -> updateStructureStateAndFireEvent(finalStructureName, playerPos));
                });
            }
        } else {
            // 多人游戏或无法获取服务器时的备用方案
            fallbackStructureDetection(playerPos);
        }
    }

    // 更新结构状态并触发事件
    private void updateStructureStateAndFireEvent(String structureName, BlockPos playerPos) {
        //System.out.println(structureName);
        if (!Objects.equals(currentStructureName, structureName)) {
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("structureId", structureName);
            eventData.put("playerPos", playerPos);
            fire(eventData);
        }
        currentStructureName = structureName;
    }

    // 备用检测方法，使用客户端可用的API
    private void fallbackStructureDetection(BlockPos playerPos) {
        // 这里可以实现备用检测方法
        // 在多人游戏中，可能需要通过数据包或其他方式获取结构信息

        // 简单的清除当前结构状态
        if (currentStructureName != null) {
            updateStructureStateAndFireEvent(null, playerPos);
        }
    }

    public void forceScan() {
        if (isEnabled) {
            tickCounter = scanInterval;
        }
    }
}
