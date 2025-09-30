package com.rcutanf.teamhunter.client.guide_sys.def;

import com.rcutanf.teamhunter.client.guide_sys.TriggerType;

import java.util.List;
import java.util.Map;

public class AchievementDefinition {
    public String id; // 如 "hot_stuff"
    public String advancementId; // Minecraft 成就 ID，如 "minecraft:story/lava_bucket"
    public List<ConditionDefinition> conditions;

    public static class ConditionDefinition {
        public String name; // "bucket"
        public String description;
        public int weight; // 50
        public List<RequirementDefinition> requirements;

        public static class RequirementDefinition {
            public TriggerType triggerType; // inventory, surroundingBlock, structure, advancement
            public String matchKey; // "minecraft:bucket", "block.minecraft.lava"
            public int matchCount; // 需要的数量，通常为1
            public int weight; // 该需求的权重，通常为50
            public boolean typeAnd; // true=与逻辑，false=或逻辑
            public int OrGroupId; // 用于或逻辑分组，从1开始编号，0表示不分组
            public int priority; // 数值越小优先级越高，用于决定组合策略
        }
    }
}