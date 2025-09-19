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
    protected boolean handleInventoryEvent(Map<String, Object> data) {
        // 物品栏事件 - 检查钻石镐
        return checkCondition4itemAdd(data, "minecraft:diamond_pickaxe", "diamond_pickaxe", 1);
    }

    @Override
    protected boolean handleSurroundingBlockEvent(Map<String, Object> data) {
        // 方块检测事件 - 检查附近熔岩
        return checkCondition4nearBlocks(data, "block.minecraft.lava", "lava");
    }
}