package com.rcutanf.teamhunter.client.guide_sys.impl.checker;

import com.rcutanf.teamhunter.client.guide_sys.AbstractGuideSysChecker;
import com.rcutanf.teamhunter.client.guide_sys.TriggerType;
import net.minecraft.util.Identifier;

import java.util.List;
import java.util.Map;

public class CoverMeWithDiamondsChecker extends AbstractGuideSysChecker {
    private static final int REQUIRED_DIAMOND_INGOT_COUNT = 6; // 需要的钻石数量

    public CoverMeWithDiamondsChecker() {
        // 设置成就ID和使用物品栏触发器
        super(Identifier.of("minecraft", "story/shiny_gear"), "cover_me_with_diamonds", List.of(TriggerType.inventory));
        // 添加钻石条件，权重为100（唯一条件）
        conditions.add(new Condition("diamond", "获取钻石", 100));
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
        // 检查钻石添加事件
        return checkCondition4itemAdd(data, "minecraft:diamond", "diamond", REQUIRED_DIAMOND_INGOT_COUNT);
    }
}