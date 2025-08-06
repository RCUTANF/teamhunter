package com.rcutanf.teamhunter.client.guide_sys.advancementListener;

import net.minecraft.advancement.AdvancementEntry;
import net.minecraft.advancement.AdvancementProgress;

public interface AdvancementCompletionListener {
    /**
     * 当玩家完成一个成就时调用
     */
    void onAdvancementCompleted(AdvancementEntry advancement, AdvancementProgress progress);
    void onAdvancementRemoved(AdvancementEntry advancement);
}