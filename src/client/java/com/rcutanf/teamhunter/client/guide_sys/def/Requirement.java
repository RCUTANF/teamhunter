package com.rcutanf.teamhunter.client.guide_sys.def;

import com.rcutanf.teamhunter.client.guide_sys.TriggerType;

import java.util.Map;

public class Requirement {
    public TriggerType triggerType; // inventory, surroundingBlock, structure, advancement
    public String matchKey; // "minecraft:bucket", "block.minecraft.lava"
    public Map<String, Object> components; // Minecraft 组件定义，可为null，用于更精确的物品/实体等匹配
    public int matchCount; // 需要的数量，通常为1
    public int insideProgress;
    public int weight; // 该需求的权重，通常为50
    public boolean typeAnd; // true=与逻辑，false=或逻辑
    public int OrGroupId; // 用于或逻辑分组，从1开始编号，0表示不分组
    public int priority; // 数值越大优先级越高，用于决定组合策略

    public Requirement(AchievementDefinition.ConditionDefinition.RequirementDefinition reqDef) {
        this.triggerType = reqDef.triggerType;
        this.matchKey = reqDef.matchKey;
        this.components = reqDef.components;
        this.matchCount = reqDef.matchCount;
        this.insideProgress = 0;
        setWeight(reqDef.weight);
        this.typeAnd = reqDef.typeAnd;
        this.OrGroupId = reqDef.OrGroupId;
        this.priority = reqDef.priority;
    }

    public void setInsideProgress(int insideProgress) {
        this.insideProgress = insideProgress;
    }

    public void setWeight(int weight) {
        if (weight < 0) {
            throw new IllegalArgumentException("Max progress must be non-negative.");
        }
        if (weight > 100){
            throw new IllegalArgumentException("Max progress cannot exceed 100.");
        }
        this.weight = weight;
    }
}
