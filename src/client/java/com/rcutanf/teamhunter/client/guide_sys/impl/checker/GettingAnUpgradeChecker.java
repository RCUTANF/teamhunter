package com.rcutanf.teamhunter.client.guide_sys.impl.checker;

import com.rcutanf.teamhunter.client.guide_sys.AbstractGuideSysChecker;
import com.rcutanf.teamhunter.client.guide_sys.TriggerType;
import net.minecraft.util.Identifier;

import java.util.List;
import java.util.Map;

/**
 * 石器时代成就检查器
 * 检查玩家是否获得了石头类物品
 */
public class GettingAnUpgradeChecker extends AbstractGuideSysChecker {

    private static final int REQUIRED_COBBLESTONE_COUNT = 3; // 需要的圆石数量

    public GettingAnUpgradeChecker() {
        // 设置成就ID和使用物品栏触发器
        super(Identifier.of("minecraft", "story/upgrade_tools"), "getting_an_upgrade",
                List.of(TriggerType.inventory));
        // 添加圆石条件，权重为100（唯一条件）
        conditions.add(new Condition("cobblestone", "获取圆石", 100));
    }

    @Override
    protected boolean handleInventoryEvent(Map<String, Object> data) {
        // 检查圆石添加事件
        return checkCondition4itemAdd(data, "minecraft:cobblestone", "cobblestone", REQUIRED_COBBLESTONE_COUNT);
    }
}