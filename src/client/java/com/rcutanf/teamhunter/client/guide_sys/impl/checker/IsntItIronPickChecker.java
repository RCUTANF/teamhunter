package com.rcutanf.teamhunter.client.guide_sys.impl.checker;

import com.rcutanf.teamhunter.client.guide_sys.AbstractGuideSysChecker;
import com.rcutanf.teamhunter.client.guide_sys.TriggerType;
import net.minecraft.util.Identifier;

import java.util.List;
import java.util.Map;

/**
 * 铁锭收集检查器
 * 检查玩家是否获得了3个铁锭
 */
public class IsntItIronPickChecker extends AbstractGuideSysChecker {

    private static final int REQUIRED_IRON_INGOT_COUNT = 3; // 需要的铁锭数量

    public IsntItIronPickChecker() {
        // 设置成就ID和使用物品栏触发器
        super(Identifier.of("minecraft", "story/iron_tools"), "isnt_it_iron_pick",
              List.of(TriggerType.inventory));
        // 添加铁锭条件，权重为100（唯一条件）
        conditions.add(new Condition("iron_ingot", "获取铁锭", 100));
    }

    @Override
    public void onEvent(Object eventData) {
        // 如果成就已完成，不再处理
        if (isCompleted()) {
            return;
        }

        // 只处理物品栏事件
        if (eventData instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) eventData;

            // 确认是物品栏事件
            if (data.containsKey("itemStack") && data.containsKey("isAdded")) {
                handleInventoryEvent(data);
            }
        }
    }

    private boolean handleInventoryEvent(Map<String, Object> data) {
        // 检查铁锭添加事件，需要3个铁锭
        return checkCondition4itemAdd(data, "minecraft:iron_ingot", "iron_ingot", REQUIRED_IRON_INGOT_COUNT);
    }
}