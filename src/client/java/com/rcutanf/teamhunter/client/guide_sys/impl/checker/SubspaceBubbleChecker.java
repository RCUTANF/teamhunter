package com.rcutanf.teamhunter.client.guide_sys.impl.checker;

import com.rcutanf.teamhunter.client.guide_sys.AbstractGuideSysChecker;
import com.rcutanf.teamhunter.client.guide_sys.TriggerType;
import com.rcutanf.teamhunter.client.guide_sys.def.AchievementDefinition;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class SubspaceBubbleChecker extends AbstractGuideSysChecker {

    private boolean isInNether = false;
    private BlockPos netherStartPos = null;
    private static final int REQUIRED_DISTANCE = 875; // 地狱中需要移动的距离

    private ScheduledExecutorService scheduler;
    private ScheduledFuture<?> distanceTask;

    public SubspaceBubbleChecker() {
        super(createDefinition());
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
    }

    private static AchievementDefinition createDefinition() {
        AchievementDefinition definition = new AchievementDefinition();
        definition.id = "subspace_bubble";
        definition.advancementId = "minecraft:nether/fast_travel";

        // 移动条件（主要条件）
        AchievementDefinition.ConditionDefinition movementCondition =
                new AchievementDefinition.ConditionDefinition();
        movementCondition.name = "move_distance";
        movementCondition.description = "在地狱中移动875格";
        movementCondition.weight = 100;
        movementCondition.isHintOnly = false;

        AchievementDefinition.ConditionDefinition.RequirementDefinition movementReq =
                new AchievementDefinition.ConditionDefinition.RequirementDefinition();
        movementReq.triggerType = TriggerType.dimension; // 改为dimension触发器
        movementReq.matchKey = "nether_movement";
        movementReq.weight = 100;
        movementReq.typeAnd = true;
        movementReq.OrGroupId = 0;
        movementReq.priority = 1;

        movementCondition.requirements = Arrays.asList(movementReq);
        definition.conditions = Arrays.asList(movementCondition);

        return definition;
    }

    @Override
    protected void handleDimensionEvent(Map<String, Object> data) {
        Identifier from = (Identifier) data.get("from");
        Identifier to = (Identifier) data.get("to");

        if ("minecraft:the_nether".equals(to.toString())) {
            // 进入地狱
            enterNether(data);
        } else if ("minecraft:the_nether".equals(from.toString())) {
            // 离开地狱
            exitNether();
        }
    }

    private void enterNether(Map<String, Object> data) {
        isInNether = true;

        // 记录起始位置
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            netherStartPos = client.player.getBlockPos();
        }

        if (!isActive()) {
            setActive(true);
        }

        // 启动距离计算轮询
        startDistancePolling();
    }

    private void exitNether() {
        isInNether = false;
        netherStartPos = null;

        // 停止轮询
        stopDistancePolling();

        // 重置进度
        resetProgress();
        setActive(false);
    }

    private void startDistancePolling() {
        if (distanceTask != null && !distanceTask.isDone()) {
            distanceTask.cancel(false);
        }

        // 每1000ms检查一次距离
        distanceTask = scheduler.scheduleAtFixedRate(this::calculateDistance, 0, 1000, TimeUnit.MILLISECONDS);
    }

    private void stopDistancePolling() {
        if (distanceTask != null && !distanceTask.isDone()) {
            distanceTask.cancel(false);
        }
    }

    private void calculateDistance() {
        if (!isInNether || netherStartPos == null) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) {
            return;
        }

        // 确认仍在地狱
        String currentDimension = client.world.getRegistryKey().getValue().toString();
        if (!"minecraft:the_nether".equals(currentDimension)) {
            return;
        }

        BlockPos currentPos = client.player.getBlockPos();

        // 计算水平距离（忽略Y轴）
        double distance = Math.sqrt(
                Math.pow(currentPos.getX() - netherStartPos.getX(), 2) +
                        Math.pow(currentPos.getZ() - netherStartPos.getZ(), 2)
        );

        // 更新进度
        updateMovementProgress(distance);
    }

    private void updateMovementProgress(double distance) {
        // 找到移动条件的requirement
        for (var condition : achievementChecker.conditions) {
            if ("move_distance".equals(condition.name)) {
                for (var requirement : condition.requirements) {
                    // 计算进度百分比
                    int progressPercent = (int) Math.min(100, (distance / REQUIRED_DISTANCE) * 100);
                    int weightedProgress = (progressPercent * requirement.weight) / 100;

                    requirement.setInsideProgress(weightedProgress);
                }
                break;
            }
        }

        // 更新总进度
        updateTotalProgress();
    }

    private void resetProgress() {
        for (var condition : achievementChecker.conditions) {
            for (var requirement : condition.requirements) {
                requirement.setInsideProgress(0);
            }
            condition.insideProgress = 0;
        }
        setProgress(0);
    }

    @Override
    protected void markAsCompleted() {
        stopDistancePolling(); // 完成后停止轮询
        super.markAsCompleted();
    }

    // 清理资源
    @Override
    public void unregister() {
        // 停止轮询线程
        stopDistancePolling();

        // 关闭线程池
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
            try {
                // 等待最多5秒让任务完成
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        // 清理状态
        isInNether = false;
        netherStartPos = null;

        // 调用父类的unregister
        super.unregister();
    }
}