package com.rcutanf.teamhunter.client.guide_sys.impl.trigger;

import com.rcutanf.teamhunter.client.guide_sys.AbstractGuideSysTrigger;
import com.rcutanf.teamhunter.client.guide_sys.TriggerType;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.structure.StructureStart;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
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

        World world = client.world;
        BlockPos playerPos = client.player.getBlockPos();

        Chunk chunk = world.getChunk(playerPos.getX() >> 4, playerPos.getZ() >> 4);

        String structureName = null;

        if (chunk.getStructureReferences() != null && !chunk.getStructureReferences().isEmpty()) {
            for (Map.Entry<Structure, LongSet> entry : chunk.getStructureReferences().entrySet()) {
                Structure structure = entry.getKey();
                LongSet references = entry.getValue();

                LongIterator iterator = references.iterator();
                while (iterator.hasNext()) {
                    long packedPos = iterator.nextLong();
                    ChunkPos chunkPos = new ChunkPos(packedPos);

                    StructureStart structureStart = world.getChunk(chunkPos.x, chunkPos.z).getStructureStart(structure);

                    if (structureStart != null && structureStart.hasChildren()) {
                        BlockBox boundingBox = structureStart.getBoundingBox();
                        if (boundingBox.contains(playerPos)) {
                            structureName = structure.toString();
                            break;
                        }
                    }
                }

                if (structureName != null) {
                    break;
                }
            }
        }

        if (!Objects.equals(currentStructureName, structureName)) {
            boolean isEntering = structureName != null && currentStructureName == null;
            boolean isLeaving = structureName == null && currentStructureName != null;

            if (isEntering || isLeaving) {
                Map<String, Object> eventData = new HashMap<>();
                eventData.put("structureName", structureName != null ? structureName : currentStructureName);
                eventData.put("isEntering", isEntering);
                eventData.put("playerPos", playerPos);

                fire(eventData);
            }

            currentStructureName = structureName;
        }
    }

    public void forceScan() {
        if (isEnabled) {
            tickCounter = scanInterval;
        }
    }
}
