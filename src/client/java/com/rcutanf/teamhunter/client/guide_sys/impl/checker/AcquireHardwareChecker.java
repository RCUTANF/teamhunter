package com.rcutanf.teamhunter.client.guide_sys.impl.checker;

import com.rcutanf.teamhunter.client.guide_sys.AbstractGuideSysChecker;
import com.rcutanf.teamhunter.client.guide_sys.TriggerType;
import net.minecraft.util.Identifier;

import java.util.List;
import java.util.Map;

/**
 * 铁器时代成就检查器
 * 检查玩家是否获得了铁矿石
 */
public class AcquireHardwareChecker extends AbstractGuideSysChecker {

    public AcquireHardwareChecker() {
        // 设置成就ID和使用物品栏触发器
        super(Identifier.of("minecraft", "story/smelt_iron"), "acquire_hardware", List.of(TriggerType.inventory));
        // 添加铁矿石条件，权重为100（唯一条件）
        conditions.add(new Condition("iron_ore", "获取铁矿石", 100));
    }

    @Override
    protected boolean handleInventoryEvent(Map<String, Object> data) {
        // 检查铁矿石和深层铁矿石，任一条件满足即可
        return checkCondition4itemAdd(data, "minecraft:iron_ore", "iron_ore", 1) ||
               checkCondition4itemAdd(data, "minecraft:deepslate_iron_ore", "iron_ore", 1);
    }
}