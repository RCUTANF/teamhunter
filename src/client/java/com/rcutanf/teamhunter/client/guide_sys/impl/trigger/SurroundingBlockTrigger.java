package com.rcutanf.teamhunter.client.guide_sys.impl.trigger;

import com.rcutanf.teamhunter.client.guide_sys.AbstractGuideSysTrigger;
import com.rcutanf.teamhunter.client.guide_sys.TriggerType;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.*;

public class SurroundingBlockTrigger extends AbstractGuideSysTrigger {
    private static SurroundingBlockTrigger instance;
    private Set<String> detectedBlockTypes = new HashSet<>();
    private BlockPos lastPlayerPos = BlockPos.ORIGIN;
    private int scanInterval = 15; // 扫描间隔（ticks）
    private int tickCounter = 0;
    private int scanRadius = 2; // 扫描半径
    private boolean isEnabled = true; //默认启用

    public SurroundingBlockTrigger() {
        super(TriggerType.surroundingBlock);
        registerEvents();
    }

    public static SurroundingBlockTrigger getInstance() {
        if (instance == null) {
            instance = new SurroundingBlockTrigger();
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
                scanSurroundingBlocks();
            }
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            detectedBlockTypes.clear();
        });
    }

    private boolean shouldScan() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) {
            return false;
        }

        // 检查玩家是否移动了足够距离
        BlockPos currentPos = client.player.getBlockPos();
        double distanceSq = lastPlayerPos.getSquaredDistance(currentPos);

        // 如果移动超过3个方块，立即扫描
        if (distanceSq > 9) {
            lastPlayerPos = currentPos;
            tickCounter = 0; // 重置计数器，下次立即扫描
            return true;
        }

        return true;
    }

    public void enable() {
        this.isEnabled = true;
        this.tickCounter = 0; // 立即开始扫描
    }

    public void disable() {
        this.isEnabled = false;
        detectedBlockTypes.clear();
    }

    public void setScanRadius(int radius) {
        this.scanRadius = Math.max(1, Math.min(radius, 16));
    }

    public void setScanInterval(int interval) {
        this.scanInterval = Math.max(5, interval);
    }

    private void scanSurroundingBlocks() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) {
            return;
        }

        World world = client.world;
        BlockPos centerPos = client.player.getBlockPos();
        Set<String> currentBlockTypes = new HashSet<>();

        // 扫描周围的方块
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

        // 如果方块类型有变化，通知监听器
        if (!detectedBlockTypes.equals(currentBlockTypes)) {
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("blockTypes", new HashSet<>(currentBlockTypes));
            eventData.put("newBlocks", getNewBlocks(currentBlockTypes));
            eventData.put("removedBlocks", getRemovedBlocks(currentBlockTypes));
            eventData.put("playerPos", centerPos);

            fire(eventData);

            // 更新缓存
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

    // 提供立即扫描的方法
    public void forceScan() {
        if (isEnabled) {
            tickCounter = scanInterval; // 下一tick就会扫描
        }
    }
}