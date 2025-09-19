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
    protected boolean handleSurroundingBlockEvent(Map<String, Object> data) {
        // 检查普通钻石矿石
        return checkCondition4nearBlocks(data, "block.minecraft.diamond_ore", "diamond_ore") ||
               checkCondition4nearBlocks(data, "block.minecraft.deepslate_diamond_ore", "diamond_ore");

    }
}