package com.rcutanf.teamhunter.client.guide_sys.def;

import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public class AchievementChecker {
    public String id; // 如 "hot_stuff"
    public Identifier advancementId; // Minecraft 成就 ID，如 "minecraft:story/lava_bucket"
    public List<Condition> conditions;

    public AchievementChecker(AchievementDefinition definition) {
        this.id = definition.id;
        this.advancementId = Identifier.of(definition.advancementId);
        this.conditions = new ArrayList<>();

        if (definition.conditions != null) {
            for (AchievementDefinition.ConditionDefinition condDef : definition.conditions) {
                Condition condition = new Condition(condDef);
                this.conditions.add(condition);
            }
        }
    }
}