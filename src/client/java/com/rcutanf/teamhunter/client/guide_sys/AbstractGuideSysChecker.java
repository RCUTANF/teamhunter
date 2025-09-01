package com.rcutanf.teamhunter.client.guide_sys;

import com.rcutanf.teamhunter.client.guide_sys.advancementListener.AdvancementCompletionListener;
import com.rcutanf.teamhunter.client.guide_sys.advancementListener.AdvancementEventManager;
import com.rcutanf.teamhunter.client.guide_sys.gui.GuideSysGuiManager;
import com.rcutanf.teamhunter.client.mixin.ClientAdvancementManagerAccessor;
import net.minecraft.advancement.AdvancementEntry;
import net.minecraft.advancement.AdvancementProgress;
import net.minecraft.advancement.PlacedAdvancement;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientAdvancementManager;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public abstract class AbstractGuideSysChecker implements TriggerListener, AdvancementCompletionListener {
    protected Identifier id;
    protected String checkerID;
    protected List<TriggerType> triggerTypes;
    protected boolean isActive; // 是否进入活动状态，也就是是否有机会完成，决定是否在屏幕上显示
    protected int progress; // 进度，0-100
    protected boolean completed;
    protected List<Condition> conditions;

    protected class Condition {
        public String name;
        public String description;
        public boolean isCompleted;
        public int maxProgress;
        public int insideProgress;

        public Condition(String name, String description, int maxProgress) {
            this.name = name;
            this.description = description;
            this.isCompleted = false; // 初始状态为未完成
            setMaxProgress(maxProgress);
            this.insideProgress = 0; // 初始进度为0
        }
        public void setInsideProgress(int insideProgress) {
            if (insideProgress < 0 || insideProgress > maxProgress) {
                throw new IllegalArgumentException("Inside progress must be between 0 and " + maxProgress + ".");
            }
            this.insideProgress = insideProgress;
        }
        public void setMaxProgress(int maxProgress) {
            if (maxProgress < 0) {
                throw new IllegalArgumentException("Max progress must be non-negative.");
            }
            if (maxProgress > 100){
                throw new IllegalArgumentException("Max progress cannot exceed 100.");
            }
            this.maxProgress = maxProgress;
        }

        public void finish() {
            this.isCompleted = true;
            this.insideProgress = maxProgress; // 完成时设置进度为最大值
        }

        public void setUnfinished() {
            this.isCompleted = false;
            this.insideProgress = 0; // 重置进度为0
        }
    }

    public AbstractGuideSysChecker(Identifier id, String checkerID, List<TriggerType> triggerTypes) {
        this.id = id;
        this.checkerID = checkerID;
        this.triggerTypes = triggerTypes;
        this.isActive = false;
        this.progress = 0; // 初始进度为0
        this.completed = false;
        this.conditions = new ArrayList<>();
        // 在构造函数中注册到相应的触发器
        register();

        // 注册到检查器管理器
        GuideSysCheckerManager.getInstance().registerChecker(this);

        // 注册成就监听器
        AdvancementEventManager.getInstance().registerListener(this);

        if(checkADinGameStatus()){
            markAsCompleted();
        }
    }

    public List<TriggerType> getTriggerTypes() {
        return triggerTypes;
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
            GuideSysGuiManager.addAdvancementGuide(id, progress);
        }
        else {
            System.out.println("AbstractGuideSysChecker " + checkerID + " is now inactive.");
            this.isActive = false;
            GuideSysGuiManager.removeAdvancementGuide(id);
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
        // 更新进度到GUI
        GuideSysGuiManager.updateAdvancementGuide(id, progress);
    }

    /**
     * 将检查器标记为已完成
     */
    protected void markAsCompleted() {
        setProgress(100);
        GuideSysGuiManager.markAdvancementComplete(id);
        Executors.newSingleThreadScheduledExecutor().schedule(() -> {
            MinecraftClient.getInstance().execute(() -> {
                setActive(false); // 在游戏主线程执行
            });
        }, 1000, TimeUnit.MILLISECONDS); // 延迟1秒
        this.completed = true;
        unregister();

    }

    /**
     * 注册到相应的触发器
     */
    public void register() {
        // 根据触发器类型获取相应的触发器实例并注册
        for (TriggerType type : triggerTypes) {
            AbstractGuideSysTrigger trigger = GuideSysTriggerManager.getInstance().getTrigger(type);
            if (trigger != null) {
                trigger.register(this);
            }
        }
    }

    /**
     * 取消注册
     */
    public void unregister() {
        for (TriggerType type : triggerTypes) {
            AbstractGuideSysTrigger trigger = GuideSysTriggerManager.getInstance().getTrigger(type);
            if (trigger != null) {
                trigger.unregister(this);
            }
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
    public boolean checkADinGameStatus(){


        if (MinecraftClient.getInstance().player == null) {
            return false;
        }
        PlacedAdvancement placedAdvancement = MinecraftClient.getInstance().player.networkHandler.getAdvancementHandler().getManager().get(id);
        ClientAdvancementManager advancementManager = MinecraftClient.getInstance().player.networkHandler.getAdvancementHandler();

        Map<AdvancementEntry, AdvancementProgress> advancementProgresses =
                ((ClientAdvancementManagerAccessor) advancementManager).getAdvancementProgresses();

        // 获取进度并检查是否完成
        AdvancementProgress progress = advancementProgresses.get(placedAdvancement.getAdvancementEntry());
        return progress != null && progress.isDone();
    }

    public void onAdvancementCompleted(AdvancementEntry advancement, AdvancementProgress progress) {
        if (advancement.id().equals(id)) {
            markAsCompleted();
        }
    }

    public void onAdvancementRemoved(AdvancementEntry advancement) {
        // 检查是否是对应成就成就
        if (advancement.id().equals(id)) {
            setActive(false);
            completed = false; // 成就被移除，重置状态
            //重新侦听触发器
            register();
        }
    }

    protected Condition getConditionByName(String name) {
        for (Condition condition : conditions) {
            if (name.equals(condition.name)) {
                return condition;
            }
        }
        return null;
    }

    public void updateTotalProgress() {
        int totalProgress = 0;
        for (Condition condition : conditions) {
            totalProgress += condition.insideProgress;
        }
        setProgress(totalProgress);
    }

    /**
     * 检查物品添加事件并更新相关条件进度
     * @param data 事件数据
     * @param itemName 目标物品名称
     * @param conditionName 条件名称
     * @param itemRequiringCount 所需物品数量
     * @return 是否成功处理该条件
     */
    protected boolean checkCondition4itemAdd(Map<String, Object> data, String itemName, String conditionName, int itemRequiringCount) {
        if (!(boolean)data.get("isAdded")) {
            return false;
        }

        ItemStack itemStack = (ItemStack) data.get("itemStack");
        String itemId = itemStack.getItem().toString();

        // 如果物品不匹配，直接返回
        if (!itemId.equals(itemName)) {
            return false;
        }

        // 激活检查器
        if (!isActive()) {
            setActive(true);
        }

        // 查找并更新条件
        Condition condition = getConditionByName(conditionName);
        if (condition == null) {
            System.out.println("找不到条件: " + conditionName);
            return false;
        }

        // 更新条件进度
        int itemCount = itemStack.getCount();
        if (itemCount >= itemRequiringCount) {
            condition.finish();
        } else {
            condition.setInsideProgress(itemCount * condition.maxProgress / itemRequiringCount);
        }

        // 更新总体进度
        updateTotalProgress();
        return true;
    }

    protected boolean checkCondition4nearBlocks(Map<String, Object> data, String blockName, String conditionName) {
        @SuppressWarnings("unchecked")
        Set<String> blockTypes = (Set<String>) data.get("blockTypes");

        // 检查附近是否有熔岩
        boolean hasBlocksNearby = blockTypes.contains(blockName);
        Condition condition = getConditionByName(conditionName);
        if (hasBlocksNearby) {
            if (condition != null) {
                if(!isActive()){setActive(true);}
                condition.finish();
                updateTotalProgress();
                return true;
            }
        }
        else  {
            if (condition != null) {
                condition.setUnfinished();
                updateTotalProgress();
                return true;
            }
        }
        return false;
    }

}