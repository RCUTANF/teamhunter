package com.rcutanf.teamhunter.buff;

public enum TriggerType {
    /**
     * 触发器类型，定义了触发器的不同类型
     */
    ON_TICK,       //每tick触发
    ON_DEATH,      // 死亡时触发
    ON_BUFF_ADD,   // 添加Buff时触发
    ON_BUFF_REMOVE // 移除Buff时触发
}
