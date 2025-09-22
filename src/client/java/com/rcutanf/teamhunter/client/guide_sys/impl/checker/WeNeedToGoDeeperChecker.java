package com.rcutanf.teamhunter.client.guide_sys.impl.checker;

import com.rcutanf.teamhunter.client.guide_sys.AbstractGuideSysChecker;
import com.rcutanf.teamhunter.client.guide_sys.TriggerType;
import net.minecraft.util.Identifier;

import java.util.Arrays;
import java.util.Map;

/**
 * We Need to Go Deeper 成就检查器
 * 检查玩家是否获得10个黑曜石或进入废弃传送门结构
 */
public class WeNeedToGoDeeperChecker extends AbstractGuideSysChecker {

    private static final int REQUIRED_OBSIDIAN_COUNT = 10;

    public WeNeedToGoDeeperChecker() {
        // 设置成就ID和使用物品栏+结构检测触发器
        super(Identifier.of("minecraft", "story/enter_the_nether"), "we_need_to_go_deeper",
              Arrays.asList(TriggerType.inventory, TriggerType.structure));
        conditions.add(new Condition("obsidian", "收集黑曜石10个", 50));
        conditions.add(new Condition("ruined_portal", "发现废弃传送门", 50));
    }

    @Override
    protected boolean handleInventoryEvent(Map<String, Object> data) {
        // 物品栏事件 - 检查黑曜石
        return checkCondition4itemAdd(data, "minecraft:obsidian", "obsidian", REQUIRED_OBSIDIAN_COUNT);
    }

    @Override
    protected boolean handleStructureEvent(Map<String, Object> data) {
        // 结构检测事件 - 检查废弃传送门
         return checkCondition4structure(data, "minecraft:ruined_portal", "ruined_portal");
    }
}