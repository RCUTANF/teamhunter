package com.rcutanf.teamhunter.client.guide_sys;

import com.rcutanf.teamhunter.client.mixin.ClientAdvancementManagerAccessor;
import net.minecraft.advancement.AdvancementEntry;
import net.minecraft.advancement.AdvancementProgress;
import net.minecraft.advancement.PlacedAdvancement;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientAdvancementManager;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.Map;

public abstract class AbstractGuideSysChecker implements TriggerListener {
    protected String checkerID;
    protected TriggerType triggerType;
    protected boolean isActive; // 是否进入活动状态，也就是是否有机会完成，决定是否在屏幕上显示
    protected int progress; // 进度，0-100
    protected boolean completed;

    public AbstractGuideSysChecker(String checkerID, TriggerType triggerType) {
        this.checkerID = checkerID;
        this.triggerType = triggerType;
        this.isActive = false;
        this.progress = 0; // 初始进度为0
        this.completed = false;
        // 在构造函数中注册到相应的触发器
        register();
    }

    public TriggerType getTriggerType() {
        return triggerType;
    }

    public boolean isCompleted() {
        return completed;
    }

    public String getCheckerID() {
        return checkerID;
    }

    public boolean isActive() {
        return isActive;
    }

    /**
     * 设置检查器为活动状态
     */
    public void setActive(boolean active) {
        if (active) {
            System.out.println("AbstractGuideSysChecker " + checkerID + " is now active.");
            this.isActive = true;
        }
        else {
            System.out.println("AbstractGuideSysChecker " + checkerID + " is now inactive.");
            this.isActive = false;
        }
    }

    /**
     * 获取当前进度
     * @return 进度值，范围从0到100
     */
    public int getProgress() {
        return progress;
    }

    /**
     * 设置当前进度
     * @param progress 进度值，范围从0到100
     */
    public void setProgress(int progress) {
        if (progress < 0 || progress > 100) {
            throw new IllegalArgumentException("Progress must be between 0 and 100.");
        }
        this.progress = progress;
        // 如果进度达到100%，则标记为已完成
        if (this.progress >= 100) {
            markAsCompleted();
        }
    }

    /**
     * 将检查器标记为已完成
     */
    protected void markAsCompleted() {
        setActive(false);
        this.completed = true;
        unregister();

    }

    /**
     * 注册到相应的触发器
     */
    public void register() {
        // 根据触发器类型获取相应的触发器实例并注册
        AbstractGuideSysTrigger trigger = GuideSysTriggerManager.getInstance().getTrigger(triggerType);
        if (trigger != null) {
            trigger.register(this);
        }
    }

    /**
     * 取消注册
     */
    public void unregister() {
        AbstractGuideSysTrigger trigger = GuideSysTriggerManager.getInstance().getTrigger(triggerType);
        if (trigger != null) {
            trigger.unregister(this);
        }
    }

    /**
     * 当触发器触发事件时调用此方法
     * 子类需要实现具体的事件处理逻辑
     */
    @Override
    public abstract void onEvent(Object eventData);

    /**
     * 检查成就是否已经完成，避免子类建立的时候没有和游戏内的数据一致
     */
    public boolean checkADinGameStatus(String namespace, String path){
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity player = mc.player;
        if (player == null) {
            return false; // 如果玩家不存在，直接返回false
        }

        ClientAdvancementManager advancementManager = mc.player.networkHandler.getAdvancementHandler();
        Identifier advancementId = Identifier.of(namespace, path);
        // 获取成就条目
        PlacedAdvancement placedAdvancement = advancementManager.getManager().get(advancementId);
        if (placedAdvancement == null) {
            return false; // 成就不存在
        }

        Map<AdvancementEntry, AdvancementProgress> advancementProgresses =
                ((ClientAdvancementManagerAccessor) advancementManager).getAdvancementProgresses();

        // 获取进度并检查是否完成
        AdvancementProgress progress = advancementProgresses.get(placedAdvancement.getAdvancementEntry());
        return progress != null && progress.isDone();
    }

}