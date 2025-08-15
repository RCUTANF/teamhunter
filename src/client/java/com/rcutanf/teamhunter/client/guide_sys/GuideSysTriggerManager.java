package com.rcutanf.teamhunter.client.guide_sys;

import com.rcutanf.teamhunter.client.guide_sys.impl.trigger.BiomeTrigger;
import com.rcutanf.teamhunter.client.guide_sys.impl.trigger.InventoryTrigger;
import com.rcutanf.teamhunter.client.guide_sys.impl.trigger.StructureTrigger;
import com.rcutanf.teamhunter.client.guide_sys.impl.trigger.SurroundingBlockTrigger;

import java.util.HashMap;
import java.util.Map;

public class GuideSysTriggerManager {
    private static GuideSysTriggerManager instance;
    private final Map<TriggerType, AbstractGuideSysTrigger> triggers;

    // 私有构造函数确保单例模式
    private GuideSysTriggerManager() {
        triggers = new HashMap<>();
        initializeTriggers();
    }

    // 单例获取方法
    public static synchronized GuideSysTriggerManager getInstance() {
        if (instance == null) {
            instance = new GuideSysTriggerManager();
        }
        return instance;
    }

    // 初始化所有触发器
    private void initializeTriggers() {
        // 为每种触发器类型创建对应的触发器实例
        for (TriggerType type : TriggerType.values()) {
            triggers.put(type, createTrigger(type));
        }
    }

    // 根据类型创建触发器
    private AbstractGuideSysTrigger createTrigger(TriggerType type) {
        // 根据类型创建不同的触发器实现
        switch (type) {
            case inventory:
                return new InventoryTrigger();
            case biome:
                return new BiomeTrigger();
            case structure:
                return new StructureTrigger();
            case surroundingBlock:
                return new SurroundingBlockTrigger();
            // 可以添加其他类型的触发器
            default:
                throw new IllegalArgumentException("Invalid trigger type");
        }
    }

    // 获取指定类型的触发器
    public AbstractGuideSysTrigger getTrigger(TriggerType type) {
        return triggers.get(type);
    }

    // 注册新的触发器
    public void registerTrigger(TriggerType type, AbstractGuideSysTrigger trigger) {
        triggers.put(type, trigger);
    }

    // 移除触发器
    public void removeTrigger(TriggerType type) {
        triggers.remove(type);
    }
}