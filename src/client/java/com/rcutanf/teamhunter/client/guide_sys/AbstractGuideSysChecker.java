package com.rcutanf.teamhunter.client.guide_sys;

import com.rcutanf.teamhunter.client.guide_sys.advancementListener.AdvancementCompletionListener;
import com.rcutanf.teamhunter.client.guide_sys.advancementListener.AdvancementEventManager;
import com.rcutanf.teamhunter.client.guide_sys.def.AchievementChecker;
import com.rcutanf.teamhunter.client.guide_sys.def.AchievementDefinition;
import com.rcutanf.teamhunter.client.guide_sys.def.Condition;
import com.rcutanf.teamhunter.client.guide_sys.def.Requirement;
import com.rcutanf.teamhunter.client.guide_sys.gui.GuideSysGuiManager;
import com.rcutanf.teamhunter.client.mixin.ClientAdvancementManagerAccessor;
import net.minecraft.advancement.AdvancementEntry;
import net.minecraft.advancement.AdvancementProgress;
import net.minecraft.advancement.PlacedAdvancement;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientAdvancementManager;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class AbstractGuideSysChecker implements TriggerListener, AdvancementCompletionListener {
    protected AchievementChecker achievementChecker;
    protected List<TriggerType> triggerTypes;
    protected boolean isActive; // 是否进入活动状态，也就是是否有机会完成，决定是否在屏幕上显示
    protected int progress; // 进度，0-100
    protected boolean completed;
    protected Map<TriggerType, List<Requirement>> requirementsByTrigger = new HashMap<>();

    public AbstractGuideSysChecker(AchievementDefinition definition) {

        this.achievementChecker = new AchievementChecker(definition);
        this.isActive = false;
        this.progress = 0;
        this.completed = false;

        // 按触发器分类存储需求
        for (Condition cd: achievementChecker.conditions) {
            for (Requirement req : cd.requirements) {
                requirementsByTrigger
                        .computeIfAbsent(req.triggerType, k -> new ArrayList<>())
                        .add(req);
            }
        }

        // 自动注册所有触发器（从 requirementsByTrigger 提取）
        Set<TriggerType> triggerTypes = requirementsByTrigger.keySet();
        this.triggerTypes = new ArrayList<>(triggerTypes);
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
        return achievementChecker.id;
    }

    public boolean isActive() {
        return isActive;
    }

    /**
     * 设置检查器为活动状态
     */
    public void setActive(boolean active) {
        if (active) {
            System.out.println("AbstractGuideSysChecker " + achievementChecker.id + " is now active.");
            this.isActive = true;
            GuideSysGuiManager.addAdvancementGuide(achievementChecker.advancementId, progress);
        }
        else {
            System.out.println("AbstractGuideSysChecker " + achievementChecker.id + " is now inactive.");
            this.isActive = false;
            GuideSysGuiManager.removeAdvancementGuide(achievementChecker.advancementId);
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
        GuideSysGuiManager.updateAdvancementGuide(achievementChecker.advancementId, progress);
    }

    /**
     * 将检查器标记为已完成
     */
    protected void markAsCompleted() {
        setProgress(100);
        GuideSysGuiManager.markAdvancementComplete(achievementChecker.advancementId);
        ScheduledFuture<?> schedule = Executors.newSingleThreadScheduledExecutor().schedule(() -> {
            MinecraftClient.getInstance().execute(() -> {
                setActive(false); // 在游戏主线程执行
            });
        }, 1000, TimeUnit.MILLISECONDS);// 延迟1秒
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
     */
    public void onEvent(Object eventData) {
        // 如果成就已完成，不再处理
        if (isCompleted()) {
            return;
        }

        // 根据事件数据类型处理不同触发器
        if (eventData instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) eventData;

            // 通过数据内容判断是哪种触发器
            if (data.containsKey("itemStack") && data.containsKey("isAdded")) {
                // 物品栏事件
                handleInventoryEvent(data);
            } else if (data.containsKey("blockTypes")) {
                // 方块检测事件
                handleSurroundingBlockEvent(data);
            } else if (data.containsKey("structureId")) {
                // 结构检测事件
                handleStructureEvent(data);
            } else if (data.containsKey("entityIds")) {
                handleEntityEvent(data);
            }
        }
    }

    protected void handleSurroundingBlockEvent(Map<String, Object> data){
        List<Requirement> reqs = requirementsByTrigger.get(TriggerType.surroundingBlock);
        if (reqs == null || reqs.isEmpty()) return;

        for (Requirement req : reqs) {
            checkCondition4nearBlocks(data, req, req.matchKey);
        }
    }

    protected void handleInventoryEvent(Map<String, Object> data){
        List<Requirement> reqs = requirementsByTrigger.get(TriggerType.inventory);
        if (reqs == null || reqs.isEmpty()) return;

        for (Requirement req : reqs) {
            checkCondition4itemAdd(data, req, req.matchKey, req.matchCount);
        }
    }

    protected void handleStructureEvent(Map<String, Object> data) {
        List<Requirement> reqs = requirementsByTrigger.get(TriggerType.structure);
        if (reqs == null || reqs.isEmpty()) return;

        for (Requirement req : reqs) {

            checkCondition4structure(data, req, req.matchKey);
        }
    }

    protected void handleEntityEvent(Map<String, Object> data) {
        List<Requirement> reqs = requirementsByTrigger.get(TriggerType.entity);
        if (reqs == null || reqs.isEmpty()) return;

        for (Requirement req : reqs) {
            checkCondition4Entity(data, req, req.matchKey);
        }
    }

    /**
     * 检查成就是否已经完成，避免子类建立的时候没有和游戏内的数据一致
     */
    public boolean checkADinGameStatus(){


        if (MinecraftClient.getInstance().player == null) {
            return false;
        }
        PlacedAdvancement placedAdvancement = MinecraftClient.getInstance().player.networkHandler.getAdvancementHandler().getManager().get(achievementChecker.advancementId);
        if (placedAdvancement == null) {
            return false;
        }
        ClientAdvancementManager advancementManager = MinecraftClient.getInstance().player.networkHandler.getAdvancementHandler();

        Map<AdvancementEntry, AdvancementProgress> advancementProgresses =
                ((ClientAdvancementManagerAccessor) advancementManager).getAdvancementProgresses();

        // 获取进度并检查是否完成
        AdvancementProgress progress = advancementProgresses.get(placedAdvancement.getAdvancementEntry());
        return progress != null && progress.isDone();
    }

    public void onAdvancementCompleted(AdvancementEntry advancement, AdvancementProgress progress) {
        if (advancement.id().equals(achievementChecker.advancementId)) {
            markAsCompleted();
        }
    }

    public void onAdvancementRemoved(AdvancementEntry advancement) {
        // 检查是否是对应成就成就
        if (advancement.id().equals(achievementChecker.advancementId)) {
            setActive(false);
            completed = false; // 成就被移除，重置状态
            //重新侦听触发器
            register();
        }
    }

    protected Condition getConditionByName(String name) {
        for (Condition condition : achievementChecker.conditions) {
            if (name.equals(condition.name)) {
                return condition;
            }
        }
        return null;
    }

    public void updateTotalProgress() {
        int totalProgress = 0;
        for (Condition condition : achievementChecker.conditions) {
            condition.insideProgress = 0;
            int currentGroupId = -1;
            int groupMaxProgress = 0;

            for (Requirement req : condition.requirements) {
                if (!(req.OrGroupId ==currentGroupId)) {
                    // 新的组，累加上一组的最大进度
                    condition.insideProgress += groupMaxProgress;
                    groupMaxProgress = 0;
                    currentGroupId = req.OrGroupId;
                }

                // 更新当前组的最大进度
                groupMaxProgress = Math.max(groupMaxProgress, req.insideProgress);
            }

            // 累加最后一组的最大进度
            condition.insideProgress += groupMaxProgress;
            totalProgress += condition.insideProgress;
            if (condition.insideProgress >= condition.maxProgress) {
                condition.finish();
            } else {
                condition.setUnfinished();
            }
        }
        setProgress(totalProgress);
    }

    /**
     * 检查物品添加事件并更新相关条件进度
     * @param data 事件数据
     * @param itemName 目标物品名称
     * @param itemRequiringCount 所需物品数量
     * @return 是否成功处理该条件
     */
    protected boolean checkCondition4itemAdd(Map<String, Object> data, Requirement requirement, String itemName, int itemRequiringCount) {
        if (!(boolean)data.get("isAdded")) {
            return false;
        }

        ItemStack itemStack = (ItemStack) data.get("itemStack");
        String itemId = itemStack.getItem().toString();

        //TODO:支持组件匹配

        // 如果物品不匹配，直接返回
        if (!itemId.equals(itemName)) {
            return false;
        }

        // 激活检查器
        if (!isActive()) {
            setActive(true);
        }

        // 更新条件进度
        int itemCount = itemStack.getCount();
        if (itemCount >= itemRequiringCount) {
            requirement.setInsideProgress(requirement.weight);
        } else {
            requirement.setInsideProgress(itemCount * requirement.weight / itemRequiringCount);
        }

        // 更新总体进度
        updateTotalProgress();
        return true;
    }

    protected boolean checkCondition4nearBlocks(Map<String, Object> data, Requirement requirement, String blockName) {
        @SuppressWarnings("unchecked")
        Set<String> blockTypes = (Set<String>) data.get("blockTypes");

        if (blockTypes.contains(blockName)) {
            if(!isActive()){setActive(true);}
            requirement.setInsideProgress(requirement.weight);
            updateTotalProgress();
            return true;
        }
        else  {
            requirement.setInsideProgress(0);
            updateTotalProgress();
            if(isActive()){
                if (getProgress() == 0){
                    setActive(false);
                }
            }
            return false;
        }
    }

    /**
     * 检查结构检测事件并更新相关条件进度
     * @param data 事件数据
     * @param structureId 目标结构标识符
     * @return 是否成功处理该条件
     */
    protected boolean checkCondition4structure(Map<String, Object> data, Requirement requirement, String structureId) {
        String detectedStructure = (String) data.get("structureId");

        if (structureId != null && structureId.equals(detectedStructure)) {
            if(!isActive()){setActive(true);}
            requirement.setInsideProgress(requirement.weight);
            updateTotalProgress();
            return true;
        }
        else  {
            requirement.setInsideProgress(0);
            updateTotalProgress();
            if(isActive()){
                if (getProgress() == 0){
                    setActive(false);
                }
            }
            return false;
        }
    }

    protected boolean checkCondition4Entity(Map<String, Object> data, Requirement requirement, String entityId) {
        @SuppressWarnings("unchecked")
        Set<String> detectedEntities = (Set<String>) data.get("entityIds");

        if (detectedEntities != null && detectedEntities.contains(entityId)) {
            if (!isActive()) {
                setActive(true);
            }
            requirement.setInsideProgress(requirement.weight);
            updateTotalProgress();
            return true;
        } else {
            requirement.setInsideProgress(0);
            updateTotalProgress();
            if (isActive() && getProgress() == 0) {
                setActive(false);
            }
            return false;
        }
    }
}