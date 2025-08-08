package com.rcutanf.teamhunter.client.guide_sys.advancementListener;

import net.minecraft.advancement.AdvancementEntry;
import net.minecraft.advancement.AdvancementProgress;

import java.util.ArrayList;
import java.util.List;

public class AdvancementEventManager {
    private static AdvancementEventManager INSTANCE;
    private final List<AdvancementCompletionListener> listeners;

    private AdvancementEventManager() {
        listeners = new ArrayList<>();
    }

    public static AdvancementEventManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new AdvancementEventManager();
        }
        return INSTANCE;
    }

    public void registerListener(AdvancementCompletionListener listener) {
        listeners.add(listener);
    }

    public void unregisterListener(AdvancementCompletionListener listener) {
        listeners.remove(listener);
    }

    public void fireAdvancementCompleted(AdvancementEntry advancement, AdvancementProgress progress) {
        for (AdvancementCompletionListener listener : listeners) {
            listener.onAdvancementCompleted(advancement, progress);
        }
    }

    public void fireAdvancementRevoked(AdvancementEntry advancement) {
        for (AdvancementCompletionListener listener : listeners) {
            listener.onAdvancementRemoved(advancement);
        }
    }
}