package com.rcutanf.teamhunter.client.guide_sys.impl.checker;

import com.rcutanf.teamhunter.client.guide_sys.AbstractGuideSysChecker;
import com.rcutanf.teamhunter.client.guide_sys.TriggerType;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Hot Stuff 成就检查器
 * 检查玩家是否获得铁锭/铁桶或附近有熔岩
 */
public class EnchanterChecker extends AbstractGuideSysChecker {

    public EnchanterChecker() {
        // 设置成就ID和使用物品栏+方块检测触发器
        super(Identifier.of("minecraft", "story/enchant_item"), "enchanter", List.of(TriggerType.inventory));
        conditions.add(new Condition("diamond", "具备钻石", 34));
        conditions.add(new Condition("obsidian", "具备黑曜石", 33));
        conditions.add(new Condition("book", "具备书本", 33));
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
        // 尝试处理各种物品条件，如果任何一个处理成功，就不需要继续检查了
        return checkCondition4itemAdd(data, "minecraft:diamond", "diamond", 2) ||
                checkCondition4itemAdd(data, "minecraft:obsidian", "obsidian", 4) ||
                checkCondition4itemAdd(data, "minecraft:book", "book", 1);
    }
}