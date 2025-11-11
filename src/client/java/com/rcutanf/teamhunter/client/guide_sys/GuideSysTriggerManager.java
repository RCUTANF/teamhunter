package com.rcutanf.teamhunter.client.guide_sys;

import com.rcutanf.teamhunter.client.guide_sys.impl.trigger.*;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

public class GuideSysTriggerManager {
    private static GuideSysTriggerManager instance;
    private final Map<TriggerType, AbstractGuideSysTrigger> triggers;

    // 私有构造函数确保单例模式
    private GuideSysTriggerManager() {
        triggers = new EnumMap<>(TriggerType.class);
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
            triggers.put(type, type.createTrigger());
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