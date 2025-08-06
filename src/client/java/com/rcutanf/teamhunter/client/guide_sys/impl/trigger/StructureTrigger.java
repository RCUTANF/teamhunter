package com.rcutanf.teamhunter.client.guide_sys.impl.trigger;

import com.rcutanf.teamhunter.client.guide_sys.AbstractGuideSysTrigger;
import com.rcutanf.teamhunter.client.guide_sys.TriggerType;

public class StructureTrigger extends AbstractGuideSysTrigger {
    public StructureTrigger() {
        super(TriggerType.structure);
    }

    // 当结构生成时，直接调用此方法触发监听器
    public void fire() {

    }
}
