package com.rcutanf.teamhunter.buff;

/**
 * 触发器接口，检查条件并执行相应操作
 */
public interface Trigger {
    /**
     * 检查是否满足触发条件
     * @return 是否应该触发,true时添加buff，false时移除buff
     */
    boolean check();

    TriggerType getType();

    void execute();


}