package com.rcutanf.teamhunter.buff;

/**
 * Buff触发器接口，定义Buff的触发条件和逻辑
 */
public interface BuffTrigger {
    /**
     * 检查Buff是否应该被触发
     * @return 如果条件满足返回true
     */
    boolean shouldTrigger();

    /**
     * 执行触发逻辑
     */
    void execute();
}