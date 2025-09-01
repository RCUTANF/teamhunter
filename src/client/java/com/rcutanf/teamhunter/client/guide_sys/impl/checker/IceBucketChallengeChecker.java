package com.rcutanf.teamhunter.client.guide_sys.impl.checker;

import com.rcutanf.teamhunter.client.guide_sys.AbstractGuideSysChecker;
import com.rcutanf.teamhunter.client.guide_sys.TriggerType;
import net.minecraft.util.Identifier;

import java.util.Arrays;
import java.util.Map;

/**
 * Ice Bucket Challenge 成就检查器
 * 检查玩家是否获得钻石镐且附近有熔岩
 */
public class IceBucketChallengeChecker extends AbstractGuideSysChecker {

    public IceBucketChallengeChecker() {
        // 设置成就ID和使用物品栏+方块检测触发器
        super(Identifier.of("minecraft", "story/form_obsidian"), "ice_bucket_challenge",
              Arrays.asList(TriggerType.inventory, TriggerType.surroundingBlock));
        conditions.add(new Condition("diamond_pickaxe", "具备钻石镐", 50));
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
                checkCondition4itemAdd(data, "minecraft:diamond_pickaxe", "diamond_pickaxe", 1);
            } else if (data.containsKey("blockTypes")) {
                // 方块检测事件
                checkCondition4nearBlocks(data, "block.minecraft.lava", "lava");
            }
        }
    }
}