package com.rcutanf.teamhunter.client.guide_sys.impl.trigger;

import com.rcutanf.teamhunter.client.guide_sys.TriggerType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.Map;

public class HeightTrigger extends ScanCore {
    private static HeightTrigger instance;
    private int lastPlayerY = Integer.MIN_VALUE;
    private int heightThreshold = 1; // 最小高度变化阈值

    public HeightTrigger() {
        super(TriggerType.height, 5); // 较短的扫描间隔以快速响应高度变化
    }

    public static HeightTrigger getInstance() {
        if (instance == null) {
            instance = new HeightTrigger();
        }
        return instance;
    }

    public void setHeightThreshold(int threshold) {
        this.heightThreshold = Math.max(1, threshold);
    }

    @Override
    protected void performScan() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) {
            return;
        }

        BlockPos playerPos = client.player.getBlockPos();
        int currentY = playerPos.getY();

        // 首次初始化
        if (lastPlayerY == Integer.MIN_VALUE) {
            lastPlayerY = currentY;
            return;
        }

        // 检查高度变化
        int heightDifference = Math.abs(currentY - lastPlayerY);
        if (heightDifference >= heightThreshold) {
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("currentHeight", currentY);
            eventData.put("previousHeight", lastPlayerY);
            eventData.put("heightChange", currentY - lastPlayerY);
            eventData.put("isAscending", currentY > lastPlayerY);
            eventData.put("isDescending", currentY < lastPlayerY);
            eventData.put("playerPos", playerPos);

            fire(eventData);
            lastPlayerY = currentY;
        }
    }

    @Override
    protected void onDisconnect() {
        lastPlayerY = Integer.MIN_VALUE;
    }

    @Override
    protected void onDisable() {
        lastPlayerY = Integer.MIN_VALUE;
    }

    @Override
    protected double getMovementThreshold() {
        return 0.0; // 不依赖水平移动，专注于高度变化
    }
}