package com.rcutanf.teamhunter.client.guide_sys.impl.trigger;

import com.rcutanf.teamhunter.client.guide_sys.TriggerType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.*;

public class SurroundingBlockTrigger extends ScanCore {
    private static SurroundingBlockTrigger instance;
    private Set<String> detectedBlockTypes = new HashSet<>();
    private int scanRadius = 2;

    public SurroundingBlockTrigger() {
        super(TriggerType.surroundingBlock, 15);
    }

    public static SurroundingBlockTrigger getInstance() {
        if (instance == null) {
            instance = new SurroundingBlockTrigger();
        }
        return instance;
    }

    public void setScanRadius(int radius) {
        this.scanRadius = Math.max(1, Math.min(radius, 16));
    }

    @Override
    protected void performScan() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) {
            return;
        }

        World world = client.world;
        BlockPos centerPos = client.player.getBlockPos();
        Set<String> currentBlockTypes = new HashSet<>();

        for (int x = -scanRadius; x <= scanRadius; x++) {
            for (int y = -scanRadius; y <= scanRadius; y++) {
                for (int z = -scanRadius; z <= scanRadius; z++) {
                    BlockPos pos = centerPos.add(x, y, z);
                    if (!world.isAir(pos)) {
                        String blockId = world.getBlockState(pos).getBlock().getTranslationKey();
                        currentBlockTypes.add(blockId);
                    }
                }
            }
        }

        if (!detectedBlockTypes.equals(currentBlockTypes)) {
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("blockTypes", new HashSet<>(currentBlockTypes));
            eventData.put("newBlocks", getNewBlocks(currentBlockTypes));
            eventData.put("removedBlocks", getRemovedBlocks(currentBlockTypes));
            eventData.put("playerPos", centerPos);

            fire(eventData);
            detectedBlockTypes = currentBlockTypes;
        }
    }

    private Set<String> getNewBlocks(Set<String> currentTypes) {
        Set<String> newBlocks = new HashSet<>(currentTypes);
        newBlocks.removeAll(detectedBlockTypes);
        return newBlocks;
    }

    private Set<String> getRemovedBlocks(Set<String> currentTypes) {
        Set<String> removedBlocks = new HashSet<>(detectedBlockTypes);
        removedBlocks.removeAll(currentTypes);
        return removedBlocks;
    }

    @Override
    protected void onDisconnect() {
        detectedBlockTypes.clear();
    }

    @Override
    protected void onDisable() {
        detectedBlockTypes.clear();
    }

    @Override
    protected double getMovementThreshold() {
        return 9.0;
    }
}