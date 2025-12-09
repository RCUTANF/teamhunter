package com.rcutanf.teamhunter.client.guide_sys.impl.trigger;

import com.rcutanf.teamhunter.client.guide_sys.TriggerType;
import com.rcutanf.teamhunter.client.guide_sys.gui.data.StructureCache;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongSet;
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
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.gen.structure.Structure;

import java.util.*;

public class StructureTrigger extends ScanCore {
    private static StructureTrigger instance;
    private Set<String> currentStructures = new HashSet<>();

    public StructureTrigger() {
        super(TriggerType.structure, 40);
    }

    public static StructureTrigger getInstance() {
        if (instance == null) {
            instance = new StructureTrigger();
        }
        return instance;
    }

    @Override
    protected void performScan() {
        checkStructureChange();
    }

    private void checkStructureChange() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) {
            return;
        }

        BlockPos playerPos = client.player.getBlockPos();
        Set<String> detectedStructures = new HashSet<>();

        if (client.getServer() != null) {
            // 单人游戏 - 可以直接访问服务器数据
            client.getServer().execute(() -> {
                MinecraftServer server = client.getServer();
                if (server != null) {
                    RegistryKey<World> worldKey = client.world.getRegistryKey();
                    ServerWorld serverWorld = server.getWorld(worldKey);

                    if (serverWorld != null) {
                        detectStructuresFromServer(serverWorld, playerPos, detectedStructures);

                        // 回到客户端线程更新状态
                        client.execute(() -> {
                            updateStructureStateAndFireEvent(detectedStructures, playerPos);
                        });
                    }
                }
            });
        } else {
            // 多人游戏 - 使用客户端可用的方法或发送数据包请求
            detectStructuresFromClient(client.world, playerPos, detectedStructures);
            //updateStructureStateAndFireEvent(detectedStructures, playerPos);
        }
    }

    private void detectStructuresFromServer(ServerWorld serverWorld, BlockPos playerPos, Set<String> detectedStructures) {
        ChunkPos chunkPos = new ChunkPos(playerPos);
        WorldChunk chunk = serverWorld.getChunk(chunkPos.x, chunkPos.z);

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
                            String structureName = serverWorld.getRegistryManager()
                                    .getOptional(RegistryKeys.STRUCTURE)
                                    .map(registry -> registry.getId(structure))
                                    .map(Object::toString)
                                    .orElse(null);

                            if (structureName != null) {
                                detectedStructures.add(structureName);
                            }
                        }
                    }
                }
            }
        }
    }

    private void detectStructuresFromClient(net.minecraft.world.World clientWorld, BlockPos playerPos, Set<String> detectedStructures) {
        StructureCache cache = StructureCache.getInstance();

        cache.getStructuresAtPosition(playerPos).thenAccept(structures -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client != null) {
                client.execute(() -> {
                    updateStructureStateAndFireEvent(structures, playerPos);
                });
            }
        }).exceptionally(throwable -> {
            System.err.println("获取结构数据失败: " + throwable.getMessage());
            return null;
        });
    }

    private void updateStructureStateAndFireEvent(Set<String> detectedStructures, BlockPos playerPos) {
        if (!currentStructures.equals(detectedStructures)) {
            // 计算进入和离开的结构
            Set<String> enteredStructures = new HashSet<>(detectedStructures);
            enteredStructures.removeAll(currentStructures);

            Set<String> exitedStructures = new HashSet<>(currentStructures);
            exitedStructures.removeAll(detectedStructures);

            Map<String, Object> eventData = new HashMap<>();
            eventData.put("triggerType", TriggerType.structure);
            eventData.put("currentStructures", new ArrayList<>(detectedStructures));
            eventData.put("enteredStructures", new ArrayList<>(enteredStructures));
            eventData.put("exitedStructures", new ArrayList<>(exitedStructures));
            eventData.put("playerPos", playerPos);

            fire(eventData);
        }
        currentStructures = new HashSet<>(detectedStructures);
    }

    @Override
    protected void onDisconnect() {
        currentStructures.clear();
    }

    @Override
    protected void onDisable() {
        currentStructures.clear();
    }

    @Override
    protected double getMovementThreshold() {
        return 64.0;
    }
}