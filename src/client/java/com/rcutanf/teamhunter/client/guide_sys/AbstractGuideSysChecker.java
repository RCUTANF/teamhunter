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
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
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
            GuideSysGuiManager.addAdvancementGuide(achievementChecker, progress);
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
        GuideSysGuiManager.updateAdvancementGuide(achievementChecker, progress);
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
        }, 1000, TimeUnit.MILLISECONDS);// 延迟1秒，这样就看上去有动画了
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
            } else if (data.containsKey("entities")) {
                handleEntityEvent(data);
            } else if (data.containsKey("from")) {
                handleDimensionEvent(data);
            } else if (data.containsKey("biomeId")) {
                // 生物群系事件
                handleBiomeEvent(data);
            } else if (data.containsKey("newWeather")) {
                // 天气变化事件
                handleWeatherEvent(data);
            } else if (data.containsKey("currentHeight")) {
                // 高度变化事件
                handleHeightEvent(data);
            } else if (data.containsKey("advancementId")) {
                // 成就进度事件
                handleAdvancementEvent(data);
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

    protected  void handleDimensionEvent(Map<String, Object> data) {
        List<Requirement> reqs = requirementsByTrigger.get(TriggerType.dimension);
        if (reqs == null || reqs.isEmpty()) return;

        for (Requirement req : reqs) {
            checkCondition4Dimension(data, req, req.matchKey);
        }
    }

    protected void handleBiomeEvent(Map<String, Object> data) {
        List<Requirement> reqs = requirementsByTrigger.get(TriggerType.biome);
        if (reqs == null || reqs.isEmpty()) return;

        for (Requirement req : reqs) {
            checkCondition4Biome(data, req, req.matchKey);
        }
    }

    protected void handleWeatherEvent(Map<String, Object> data) {
        List<Requirement> reqs = requirementsByTrigger.get(TriggerType.weather);
        if (reqs == null || reqs.isEmpty()) return;

        for (Requirement req : reqs) {
            checkCondition4Weather(data, req, req.matchKey);
        }
    }

    protected void  handleHeightEvent(Map<String, Object> data) {
        List<Requirement> reqs = requirementsByTrigger.get(TriggerType.height);
        if (reqs == null || reqs.isEmpty()) return;

        for (Requirement req : reqs) {
            checkCondition4Height(data, req, Integer.parseInt(req.matchKey));
        }
    }

    protected void handleAdvancementEvent(Map<String, Object> data) {
        List<Requirement> reqs = requirementsByTrigger.get(TriggerType.advancementProcess);
        if (reqs == null || reqs.isEmpty()) return;

        for (Requirement req : reqs) {
            checkCondition4Advancement(data, req, req.matchKey);
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
            // 跳过仅提示的条件，不计入总进度
            if (condition.isHintOnly) {
                continue;
            }

            condition.insideProgress = 0;

            // 保留出现顺序的分组：key = OrGroupId, value = list of requirements in that group
            Map<Integer, List<Requirement>> orGroups = new LinkedHashMap<>();

            // 先把 typeAnd == true 的直接计入；typeAnd == false 的收集到分组
            for (Requirement req : condition.requirements) {
                if (req == null) continue;
                if (req.typeAnd) {
                    // 与逻辑，始终计入
                    condition.insideProgress += req.insideProgress;
                } else {
                    int gid = req.OrGroupId;
                    orGroups.computeIfAbsent(gid, k -> new ArrayList<>()).add(req);
                }
            }

            // 处理每个 OR 组：按 priority 升序排序，取第一个 insideProgress > 0 的值
            for (List<Requirement> groupReqs : orGroups.values()) {
                if (groupReqs == null || groupReqs.isEmpty()) continue;
                groupReqs.sort(Comparator.comparingInt(r -> r.priority));
                int groupProgress = 0;
                for (Requirement r : groupReqs) {
                    if (r != null && r.insideProgress > 0) {
                        groupProgress = r.insideProgress;
                        break; // 取优先级最高的首个非0项
                    }
                }
                condition.insideProgress += groupProgress;
            }

            totalProgress += condition.insideProgress;
            if (condition.insideProgress >= condition.maxProgress) {
                condition.finish();
            }
        }
        try{
            setProgress(totalProgress);
        }
        catch (IllegalArgumentException e) {
            throw new RuntimeException("Error updating progress for checker " + achievementChecker.id + ": " + e.getMessage());
        }
    }
    
    private boolean matchesEntityWithComponents(Entity entity, String entityId, Map<String, Object> components) {
        // 首先检查实体类型
        String actualEntityId = Identifier.tryParse(
            Registries.ENTITY_TYPE.getId(entity.getType()).toString()
        ).toString();
        
        if (!entityId.equals(actualEntityId)) {
            return false;
        }
    
        // 如果没有组件要求，只需类型匹配
        if (components == null || components.isEmpty()) {
            return true;
        }
    
        // 检查实体组件
        return checkEntityComponents(entity, components);
    }
    
    private boolean checkEntityComponents(Entity entity, Map<String, Object> components) {
        for (Map.Entry<String, Object> entry : components.entrySet()) {
            String componentKey = entry.getKey();
            Object expectedValue = entry.getValue();
    
            if (!matchEntityComponent(entity, componentKey, expectedValue)) {
                return false;
            }
        }
        return true;
    }
    
    private boolean matchEntityComponent(Entity entity, String componentKey, Object expectedValue) {
        switch (componentKey) {
            case "minecraft:cat_variant": {
                String expectedVariant = (String) expectedValue; // 例如 "minecraft:tabby"
                var catVariant = entity.get(DataComponentTypes.CAT_VARIANT);
                if (catVariant == null) {
                    return false; // 非猫或未设置变种则不匹配
                }
                String actualVariant = catVariant.getIdAsString();
                return expectedVariant.equals(actualVariant);
            }

            case "minecraft:wolf_variant": {
                String expectedVariant = (String) expectedValue;
                var wolfVariant = entity.get(DataComponentTypes.WOLF_VARIANT);
                if (wolfVariant == null) {
                    return false; // 非猫或未设置变种则不匹配
                }
                String actualVariant = wolfVariant.getIdAsString();
                return expectedVariant.equals(actualVariant);
            }

            case "minecraft:health":
                if (entity instanceof LivingEntity livingEntity) {
                    float expectedHealth = ((Number) expectedValue).floatValue();
                    return Math.abs(livingEntity.getHealth() - expectedHealth) < 0.1f;
                }
                return false;
    
            case "minecraft:type":
                String expectedType = (String) expectedValue;
                String actualType = Registries.ENTITY_TYPE
                    .getId(entity.getType()).toString();
                return expectedType.equals(actualType);
    
            case "minecraft:custom_name":
                String expectedName = (String) expectedValue;
                Text customName = entity.getCustomName();
                return customName != null && expectedName.equals(customName.getString());
    
            case "minecraft:passengers":
                @SuppressWarnings("unchecked")
                List<String> expectedPassengers = (List<String>) expectedValue;
                List<Entity> passengers = entity.getPassengerList();
                return passengers.size() >= expectedPassengers.size();
    
            // 可以继续添加更多组件类型
            default:
                return false; // 未知组件类型默认不匹配
        }
    }

    // 2) 新增：校验物品组件集合
    private boolean checkItemComponents(ItemStack stack, Map<String, Object> components) {
        for (Map.Entry<String, Object> entry : components.entrySet()) {
            if (!matchItemComponent(stack, entry.getKey(), entry.getValue())) {
                return false;
            }
        }
        return true;
    }

    // 3) 新增：匹配单个物品组件
    @SuppressWarnings("unchecked")
    private boolean matchItemComponent(ItemStack stack, String componentKey, Object expectedValue) {
        switch (componentKey) {
            case "minecraft:custom_name": {
                // 仅匹配自定义命名（铁砧命名），非显示名
                var custom = stack.get(DataComponentTypes.CUSTOM_NAME);
                String expected = (String) expectedValue;
                return custom != null && expected.equals(custom.getString());
            }

            case "minecraft:enchantments": {
                ItemEnchantmentsComponent ench = stack.get(DataComponentTypes.ENCHANTMENTS);
                if (ench == null) return false;

                // 允许两种写法：
                // 1) List<String>：仅要求包含这些附魔（忽略等级）
                // 2) Map<String, Integer>：要求对应附魔等级 >= 指定等级
                if (expectedValue instanceof List<?> list) {
                    for (Object o : list) {
                        if (!(o instanceof String id) || !hasEnchantment(ench, id)) {
                            return false;
                        }
                    }
                    return true;
                } else if (expectedValue instanceof Map<?, ?> map) {
                    for (Map.Entry<?, ?> e : ((Map<?, ?>) expectedValue).entrySet()) {
                        if (!(e.getKey() instanceof String id) || !(e.getValue() instanceof Number lvl)) {
                            return false;
                        }
                        if (!hasEnchantmentWithMinLevel(ench, id, ((Number) e.getValue()).intValue())) {
                            return false;
                        }
                    }
                    return true;
                }
                return false;
            }

            case "minecraft:potion_contents": {
                var contents = stack.get(DataComponentTypes.POTION_CONTENTS);
                if (contents == null) return false;

                // expectedValue 直接就是药水内容的Map，如 {"potion": "minecraft:swiftness"}
                if (!(expectedValue instanceof Map)) return false;
                Map<String, Object> potionContents = (Map<String, Object>) expectedValue;

                // 直接从expectedValue中获取药水ID
                String expectedPotionId = (String) potionContents.get("potion");
                if (expectedPotionId == null) return false;

                var opt = contents.potion();
                if (opt == null || opt.isEmpty()) return false;

                String actualPotionId = opt.get().getIdAsString();
                return actualPotionId.contains(expectedPotionId);//why use long_weakness as a component name f**k mj?
            }


            case "minecraft:trim_pattern": {
                var trim = stack.get(DataComponentTypes.TRIM);
                if (trim == null) return false;
                String expected = (String) expectedValue;
                String actual = trim.pattern().getIdAsString();
                return expected.equals(actual);
            }

            case "minecraft:trim_material": {
                var trim = stack.get(DataComponentTypes.TRIM);
                if (trim == null) return false;
                String expected = (String) expectedValue;
                String actual = trim.material().getIdAsString();
                return expected.equals(actual);
            }

            // 可继续扩展更多组件匹配
            default:
                return false;
        }
    }

    // 4) 新增：附魔辅助方法


    private boolean hasEnchantment(ItemEnchantmentsComponent ench, String enchantmentId) {
        for (RegistryEntry<Enchantment> entry : ench.getEnchantments()) {
            String actualId = entry.getIdAsString(); // 例如 "minecraft:sharpness"
            if (actualId != null && actualId.equals(enchantmentId)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasEnchantmentWithMinLevel(ItemEnchantmentsComponent ench, String enchantmentId, int minLevel) {
        for (RegistryEntry<Enchantment> entry : ench.getEnchantments()) {
            String actualId = entry.getIdAsString();
            if (actualId != null && actualId.equals(enchantmentId)) {
                int level = ench.getLevel(entry); // 从组件中读取实际等级
                return level >= minLevel;
            }
        }
        return false;
    }

    /**
     * 检查物品添加事件并更新相关条件进度
     * @param data 事件数据
     * @param itemName 目标物品名称
     * @param itemRequiringCount 所需物品数量
     * @return 是否成功处理该条件
     */
    protected boolean checkCondition4itemAdd(Map<String, Object> data, Requirement requirement, String itemName, int itemRequiringCount) {
        ItemStack itemStack = (ItemStack) data.get("itemStack");
        String itemId = itemStack.getItem().toString();
        boolean isAdded = (boolean) data.get("isAdded");

        // 检查物品是否匹配（支持部分匹配）
        boolean matches = requirement.partialMatch ?
                itemId.contains(itemName) :
                itemId.equals(itemName);

        if (!matches) {
            return false;
        }

        // 组件匹配（若有要求）
        if (requirement.components != null && !requirement.components.isEmpty()) {
            if (!checkItemComponents(itemStack, requirement.components)) {
                return false;
            }
        }

        int itemCount = itemStack.getCount();
        int currentProgress = requirement.getInsideProgress();

        if (isAdded) {
            // 物品添加逻辑
            if (!isActive()) {
                setActive(true);
            }

            if (itemCount >= itemRequiringCount) {
                requirement.setInsideProgress(requirement.weight);
            } else {
                requirement.setInsideProgress(itemCount * requirement.weight / itemRequiringCount);
            }
        } else {
            // 物品移除逻辑
            int progressToRemove = itemCount * requirement.weight / itemRequiringCount;
            int newProgress = Math.max(0, currentProgress - progressToRemove);
            requirement.setInsideProgress(newProgress);

            // 如果进度归零且检查器处于活动状态，考虑关闭
            if (newProgress == 0 && isActive() && getProgress() == 0) {
                setActive(false);
            }
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
        List<Entity> detectedEntities = (List<Entity>) data.get("entities");

        if (detectedEntities == null || detectedEntities.isEmpty()) {
            requirement.setInsideProgress(0);
            updateTotalProgress();
            if (isActive() && getProgress() == 0) {
                setActive(false);
            }
            return false;
        }

        // 检查是否有匹配的实体
        boolean hasMatch = detectedEntities.stream().anyMatch(entity ->
                matchesEntityWithComponents(entity, entityId, requirement.components));

        if (hasMatch) {
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


    protected boolean checkCondition4Dimension(Map<String, Object> data, Requirement requirement, String dimensionId) {
        Identifier from = (Identifier) data.get("from");
        Identifier to = (Identifier) data.get("to");

        if (dimensionId != null && dimensionId.equals(to.toString())) {
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

    protected boolean checkCondition4Biome(Map<String, Object> data, Requirement requirement, String biomeId) {
        String detectedBiome = (String) data.get("biomeId");

        if (biomeId != null && biomeId.equals(detectedBiome)) {
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


    protected boolean checkCondition4Weather(Map<String, Object> data, Requirement requirement, String targetWeather) {
        String currentWeather = (String) data.get("newWeather");

        if (targetWeather != null && targetWeather.equals(currentWeather)) {
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

    protected boolean checkCondition4Height(Map<String, Object> data, Requirement requirement, int matchline) {
        int currentHeight = (int) data.get("currentHeight");

        // 定义高度范围，这里假设世界高度范围是 -64 到 320
        int minHeight = -64;
        int maxHeight = 320;

        // 计算进度：(当前高度 - 目标高度) / (最大高度 - 目标高度) * 权重
        if (currentHeight >= matchline) {
            if (!isActive()) {
                setActive(true);
            }

            int heightDifference = currentHeight - matchline;
            int totalRange = maxHeight - matchline;

            // 避免除零错误
            if (totalRange <= 0) {
                requirement.setInsideProgress(requirement.weight);
            } else {
                //使用min确保进度不会超过权重，即使玩家超过限高
                int progress = Math.min(requirement.weight, (heightDifference * requirement.weight) / totalRange);
                requirement.setInsideProgress(progress);
            }

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

    protected boolean checkCondition4Advancement(Map<String, Object> data, Requirement requirement, String criteriaKey) {
        Identifier advancementId = (Identifier) data.get("advancementId");
        AdvancementProgress progress = (AdvancementProgress) data.get("advancementProgress");

        // 检查是否是对应的成就
        if (!advancementId.equals(achievementChecker.advancementId)) {
            return false;
        }

        // 检查指定criteria的完成状态
        boolean criteriaObtained = progress.getCriterionProgress(criteriaKey) != null &&
                                   progress.getCriterionProgress(criteriaKey).isObtained();

        if (criteriaObtained) {
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