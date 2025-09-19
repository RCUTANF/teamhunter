package com.rcutanf.teamhunter.client.guide_sys.impl.checker;

import com.rcutanf.teamhunter.client.guide_sys.AbstractGuideSysChecker;
import com.rcutanf.teamhunter.client.guide_sys.TriggerType;
import net.minecraft.util.Identifier;

import java.util.List;
import java.util.Map;

/**
 * Suit Up 成就检查器
 * 检查玩家是否获得足够的铁锭可以制作盔甲
 */
public class SuitUpChecker extends AbstractGuideSysChecker {
    private static final int REQUIRED_IRON_INGOT_COUNT = 6; // 需要的铁锭数量

    public SuitUpChecker() {
        // 设置成就ID和使用物品栏触发器
        super(Identifier.of("minecraft", "story/obtain_armor"), "suit_up",
              List.of(TriggerType.inventory));
        // 添加铁锭条件，权重为100（唯一条件）
        conditions.add(new Condition("iron_ingot", "获取铁锭", 100));
    }

    protected boolean handleInventoryEvent(Map<String, Object> data) {
        // 检查铁锭添加事件
        return checkCondition4itemAdd(data, "minecraft:iron_ingot", "iron_ingot", REQUIRED_IRON_INGOT_COUNT);
    }
}