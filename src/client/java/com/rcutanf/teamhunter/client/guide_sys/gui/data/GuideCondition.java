package com.rcutanf.teamhunter.client.guide_sys.gui.data;

public class GuideCondition {
    private final String description;
    private final boolean completed;
    private final int progress;
    private final int maxProgress;

    public GuideCondition(String description, boolean completed, int progress, int maxProgress) {
        this.description = description;
        this.completed = completed;
        this.progress = progress;
        this.maxProgress = maxProgress;
    }

    public float getProgressPercentage() {
        return maxProgress > 0 ? (progress * 100.0f / maxProgress) : 0.0f;
    }

    // Getters
    public String getDescription() { return description; }
    public boolean isCompleted() { return completed; }
    public int getProgress() { return progress; }
    public int getMaxProgress() { return maxProgress; }
}
