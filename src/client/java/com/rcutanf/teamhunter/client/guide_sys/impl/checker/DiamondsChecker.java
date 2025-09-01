package com.rcutanf.teamhunter.client.guide_sys.impl.checker;

import com.rcutanf.teamhunter.client.guide_sys.AbstractGuideSysChecker;
import com.rcutanf.teamhunter.client.guide_sys.TriggerType;
import net.minecraft.util.Identifier;

import java.util.List;
import java.util.Map;

/**
 * 钻石矿石检查器
 * 检查玩家周围是否存在钻石矿石或深层钻石矿石
 */
public class DiamondsChecker extends AbstractGuideSysChecker {

    public DiamondsChecker() {
        // 设置成就ID和使用方块检测触发器
        super(Identifier.of("minecraft", "story/mine_diamond"), "diamonds",
              List.of(TriggerType.surroundingBlock));
        // 添加钻石矿石条件，权重为100（唯一条件）
        conditions.add(new Condition("diamond_ore", "发现钻石矿石", 100));
    }

    @Override
    public void onEvent(Object eventData) {
        // 如果成就已完成，不再处理
        if (isCompleted()) {
            return;
        }

        // 只处理方块检测事件
        if (eventData instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) eventData;

            // 确认是方块检测事件
            if (data.containsKey("blockTypes")) {
                // 检查周围是否有钻石矿石或深层钻石矿石
                checkDiamondOres(data);
            }
        }
    }

    private void checkDiamondOres(Map<String, Object> data) {
        // 检查普通钻石矿石
        boolean foundDiamondOre = checkCondition4nearBlocks(data, "block.minecraft.diamond_ore", "diamond_ore");

        // 如果没找到普通钻石矿石，尝试检查深层钻石矿石
        if (!foundDiamondOre) {
            checkCondition4nearBlocks(data, "block.minecraft.deepslate_diamond_ore", "diamond_ore");
        }
    }
}