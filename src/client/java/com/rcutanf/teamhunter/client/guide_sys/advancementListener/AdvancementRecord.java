package com.rcutanf.teamhunter.client.guide_sys.advancementListener;

import net.minecraft.util.Identifier;

import java.util.Objects;

public class AdvancementRecord {
    private final Identifier advancementId;
    private final long gameTime;
    private final long completionTime;

    public AdvancementRecord(Identifier advancementId, long gameTime, long completionTime) {
        this.advancementId = advancementId;
        this.gameTime = gameTime;
        this.completionTime = completionTime;
    }

    public Identifier getAdvancementId() {
        return advancementId;
    }

    public long getGameTime() {
        return gameTime;
    }

    public long getCompletionTime() {
        return completionTime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AdvancementRecord that = (AdvancementRecord) o;
        return Objects.equals(advancementId, that.advancementId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(advancementId);
    }
}