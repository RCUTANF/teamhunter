package com.rcutanf.teamhunter.client.guide_sys.def;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Condition {
    public String name; // "bucket"
    public String description; // "具备铁桶"
    public List<Requirement> requirements;
    public boolean isCompleted;
    public int maxProgress;
    public int insideProgress;

    public Condition(AchievementDefinition.ConditionDefinition conditionDef) {
        this.name = conditionDef.name;
        this.description = conditionDef.description;
        this.isCompleted = false; // 初始状态为未完成
        setMaxProgress(conditionDef.weight);
        this.insideProgress = 0; // 初始进度为0

        this.requirements = new ArrayList<>();
        if (conditionDef.requirements != null) {
            for (AchievementDefinition.ConditionDefinition.RequirementDefinition reqDef : conditionDef.requirements) {
                Requirement requirement = new Requirement(reqDef);
                this.requirements.add(requirement);
            }
        }

        // 按组ID分组并排序
        this.requirements = this.requirements.stream()
                .collect(Collectors.groupingBy(req -> req.OrGroupId)) // 按组ID分组
                .entrySet().stream()
                .sorted(Map.Entry.comparingByKey()) // 按组ID升序排序
                .flatMap(entry -> entry.getValue().stream()
                        .sorted(Comparator.comparingInt(req -> req.priority))) // 组内按优先级升序排序
                .collect(Collectors.toList());
    }
    public void setInsideProgress(int insideProgress) {
        if (insideProgress < 0 || insideProgress > maxProgress) {
            throw new IllegalArgumentException("Inside progress must be between 0 and " + maxProgress + ".");
        }
        this.insideProgress = insideProgress;
    }
    public void setMaxProgress(int maxProgress) {
        if (maxProgress < 0) {
            throw new IllegalArgumentException("Max progress must be non-negative.");
        }
        if (maxProgress > 100){
            throw new IllegalArgumentException("Max progress cannot exceed 100.");
        }
        this.maxProgress = maxProgress;
    }

    public void finish() {
        this.isCompleted = true;
        this.insideProgress = maxProgress; // 完成时设置进度为最大值
    }

    public void setUnfinished() {
        this.isCompleted = false;
        this.insideProgress = 0; // 重置进度为0
    }

    public boolean isCompleted() {
        return isCompleted;
    }

    public int getInsideProgress() {
        return insideProgress;
    }

    public int getMaxProgress() {return maxProgress;}

}
