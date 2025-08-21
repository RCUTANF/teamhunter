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

    private void handleInventoryEvent(Map<String, Object> data) {
        ItemStack itemStack = (ItemStack) data.get("itemStack");
        boolean isAdded = (boolean) data.get("isAdded");
        String itemId = itemStack.getItem().toString();

        if (isAdded) {
            // 激活检查器
            if (!isActive()) {
                setActive(true);
            }

            // 处理钻石物品（需要2个）
            if (itemId.equals("minecraft:diamond")) {
                Condition diamondCondition = getConditionByName("diamond");
                if (diamondCondition != null) {
                    int diamondCount = itemStack.getCount();
                    if (diamondCount >= 2) {
                        diamondCondition.finish();
                    } else {
                        diamondCondition.setInsideProgress(diamondCount * 34 / 2);
                    }
                }
            }
            // 处理黑曜石物品（需要4个）
            else if (itemId.equals("minecraft:obsidian")) {
                Condition obsidianCondition = getConditionByName("obsidian");
                if (obsidianCondition != null) {
                    int obsidianCount = itemStack.getCount();
                    if (obsidianCount >= 4) {
                        obsidianCondition.finish();
                    } else {
                        obsidianCondition.setInsideProgress(obsidianCount * 33 / 4);
                    }
                }
            }
            // 处理书本物品
            else if (itemId.equals("minecraft:book")) {
                Condition bookCondition = getConditionByName("book");
                if (bookCondition != null) {
                    bookCondition.finish();
                }
            }

            // 更新总体进度
            updateTotalProgress();
        }
    }
}