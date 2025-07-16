package com.rcutanf.teamhunter.buff;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import java.util.*;

/**
 * Buff处理器，集中管理Buff的添加和移除
 */
public class BuffHandler {
    // 单例
    private static final BuffHandler INSTANCE = new BuffHandler();

    // 管理激活的Buff
    private final Map<String, Buff> activeBuffs = new HashMap<>();


    // 按类型管理触发器
    private final Map<TriggerType, List<Trigger>> triggersByType = new HashMap<>();

    private BuffHandler() {
        // 初始化所有触发器类型的列表
        for (TriggerType type : TriggerType.values()) {
            triggersByType.put(type, new ArrayList<>());
        }

        // 注册各种事件监听器
        registerEventListeners();
    }

    public static BuffHandler getInstance() {
        return INSTANCE;
    }

    /**
     * 注册事件监听器
     */
    private void registerEventListeners() {
        // 注册tick事件
        ServerTickEvents.START_SERVER_TICK.register(server -> {
            processTriggers(TriggerType.ON_TICK);
        });

        // 其他事件可以类似方式注册
        // 例如: 玩家死亡事件、buff添加事件等
    }

    /**
     * 处理指定类型的所有触发器
     */
    public void processTriggers(TriggerType type) {
        List<Trigger> triggers = triggersByType.get(type);
        if (triggers != null) {
            for (Trigger trigger : triggers) {
                if (trigger.check()) {
                    trigger.execute();
                }
            }
        }
    }

    /**
     * 注册触发器
     */
    public void registerTrigger(Trigger trigger) {
        TriggerType type = trigger.getType();
        triggersByType.get(type).add(trigger);
    }

    /**
     * 移除触发器
     */
    public void unregisterTrigger(Trigger trigger) {
        TriggerType type = trigger.getType();
        triggersByType.get(type).remove(trigger);
    }


    /**
     * 添加Buff
     */
    public void addBuff(Buff buff) {
        activeBuffs.put(buff.getId(), buff);
        buff.apply();
        processTriggers(TriggerType.ON_BUFF_ADD);
    }

    /**
     * 移除Buff
     */
    public void removeBuff(String buffId) {
        Buff buff = activeBuffs.remove(buffId);
        if (buff != null) {
            buff.remove();
            processTriggers(TriggerType.ON_BUFF_REMOVE);
        }
    }


    /**
     * 获取所有激活的Buff
     */
    public Collection<Buff> getActiveBuffs() {
        return activeBuffs.values();
    }

    /**
     * 获取指定ID的Buff
     */
    public Buff getBuff(String buffId) {
        return activeBuffs.get(buffId);
    }
}