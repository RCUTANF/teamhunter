package com.rcutanf.teamhunter.client.guide_sys.impl.trigger;

import com.rcutanf.teamhunter.client.guide_sys.TriggerType;
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
    private String currentStructureName = null;

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

        if (client.isInSingleplayer() && client.getServer() != null) {
            RegistryKey<World> worldKey = client.world.getRegistryKey();
            ServerWorld serverWorld = client.getServer().getWorld(worldKey);

            if (serverWorld != null) {
                MinecraftServer server = client.getServer();
                server.execute(() -> {
                    ChunkPos chunkPos = new ChunkPos(playerPos);
                    WorldChunk chunk = serverWorld.getChunk(chunkPos.x, chunkPos.z);

                    String structureName = null;
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

                    String finalStructureName = structureName;
                    client.execute(() -> updateStructureStateAndFireEvent(finalStructureName, playerPos));
                });
            }
        } else {
            fallbackStructureDetection(playerPos);
        }
    }

    private void updateStructureStateAndFireEvent(String structureName, BlockPos playerPos) {
        if (!Objects.equals(currentStructureName, structureName)) {
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("structureId", structureName);
            eventData.put("playerPos", playerPos);
            fire(eventData);
        }
        currentStructureName = structureName;
    }

    private void fallbackStructureDetection(BlockPos playerPos) {
        if (currentStructureName != null) {
            updateStructureStateAndFireEvent(null, playerPos);
        }
    }

    @Override
    protected void onDisconnect() {
        currentStructureName = null;
    }

    @Override
    protected void onDisable() {
        currentStructureName = null;
    }

    @Override
    protected double getMovementThreshold() {
        return 64.0;
    }
}