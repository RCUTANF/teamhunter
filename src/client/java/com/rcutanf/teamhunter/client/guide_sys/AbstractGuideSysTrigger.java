package com.rcutanf.teamhunter.client.guide_sys;

import java.util.ArrayList;
import java.util.List;

/**
 * 指导系统触发器抽象类
 * 实现了触发器的通用功能
 */
public abstract class AbstractGuideSysTrigger {

    protected final TriggerType triggerType;
    protected final List<TriggerListener> listeners = new ArrayList<>();

    public AbstractGuideSysTrigger(TriggerType triggerType) {

        this.triggerType = triggerType;
    }

    /**
     * 注册一个触发监听器
     *
     * @param listener 要注册的监听器
     */
    public void register(TriggerListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    /**
     * 取消注册一个触发监听器
     *
     * @param listener 要取消注册的监听器
     */
    public void unregister(TriggerListener listener) {
        listeners.remove(listener);
    }

    /**
     * 触发事件，通知所有已注册的监听器
     *
     * @param eventData 事件相关的数据
     */
    public void fire(Object eventData) {
        for (TriggerListener listener : new ArrayList<>(listeners)) {
            listener.onEvent(eventData);
        }
    }

    /**
     * 获取此触发器的类型
     *
     * @return 触发器类型
     */
    public TriggerType getType(){
        return triggerType;
    };

    /**
     * 清除所有注册的监听器
     */
    public void clearListeners() {
        listeners.clear();
    }
}