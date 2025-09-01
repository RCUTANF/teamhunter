package com.rcutanf.teamhunter.client.guide_sys.impl.checker;

import com.rcutanf.teamhunter.client.guide_sys.AbstractGuideSysChecker;
import com.rcutanf.teamhunter.client.guide_sys.TriggerType;
import net.minecraft.util.Identifier;

import java.util.*;

/**
 * Hot Stuff 成就检查器
 * 检查玩家是否获得铁锭/铁桶或附近有熔岩
 */
public class HotStuffChecker extends AbstractGuideSysChecker {

    public HotStuffChecker() {
        // 设置成就ID和使用物品栏+方块检测触发器
        super(Identifier.of("minecraft", "story/lava_bucket"), "hot_stuff",
              Arrays.asList(TriggerType.inventory, TriggerType.surroundingBlock));
        conditions.add(new Condition("bucket", "具备铁桶", 50));
        conditions.add(new Condition("lava", "附近可获得岩浆", 50));
    }

    @Override
    public void onEvent(Object eventData) {
        // 如果成就已完成，不再处理
        if (isCompleted()) {
            return;
        }

        // 根据事件数据类型处理不同触发器
        if (eventData instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) eventData;

            // 通过数据内容判断是哪种触发器
            if (data.containsKey("itemStack") && data.containsKey("isAdded")) {
                // 物品栏事件
                handleInventoryEvent(data);
            } else if (data.containsKey("blockTypes")) {
                // 方块检测事件
                checkCondition4nearBlocks(data, "block.minecraft.lava", "lava");
            }
        }
    }

    private void handleInventoryEvent(Map<String, Object> data) {
        // 检查铁锭和铁桶
        if (checkCondition4itemAdd(data, "minecraft:bucket", "bucket", 1)) {
            return; // 如果是铁桶，直接完成该条件
        }

        // 检查铁锭（需要3个铁锭合成一个铁桶）
        if (checkCondition4itemAdd(data, "minecraft:iron_ingot", "bucket", 3)) {
            // 更新铁锭进度后，保证进度最多为49（表示需要合成铁桶）
            Condition bucketCondition = getConditionByName("bucket");
            if (bucketCondition != null && bucketCondition.insideProgress >= 50) {
                bucketCondition.setInsideProgress(49);
                updateTotalProgress();
            }
        }
    }
}