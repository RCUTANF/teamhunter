package com.rcutanf.teamhunter.client.guide_sys.advancementListener;

import net.minecraft.advancement.AdvancementEntry;
import net.minecraft.advancement.AdvancementProgress;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public class AdvancementEventManager {
    private static AdvancementEventManager INSTANCE;
    private final List<AdvancementCompletionListener> listeners;
    private final AdvancementRecordManager recordManager;

    private AdvancementEventManager() {
        listeners = new ArrayList<>();
        recordManager = new AdvancementRecordManager();
    }

    public static AdvancementEventManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new AdvancementEventManager();
        }
        return INSTANCE;
    }

    public void onWorldLoad() {
        recordManager.loadRecords();
    }

    public void onWorldUnload() {
        recordManager.saveRecords();
    }

    public void registerListener(AdvancementCompletionListener listener) {
        listeners.add(listener);
    }

    public void unregisterListener(AdvancementCompletionListener listener) {
        listeners.remove(listener);
    }

    public void fireAdvancementCompleted(AdvancementEntry advancement, AdvancementProgress progress) {
        // 记录成就完成
        if (MinecraftClient.getInstance().world != null) {
            long gameTime = MinecraftClient.getInstance().world.getTime();
            long realTime = System.currentTimeMillis();
            Identifier advancementId = advancement.id();

            AdvancementRecord record = new AdvancementRecord(advancementId, gameTime, realTime);
            recordManager.addRecord(record);
        }

        for (AdvancementCompletionListener listener : listeners) {
            listener.onAdvancementCompleted(advancement, progress);
        }
    }

    public void fireAdvancementRevoked(AdvancementEntry advancement) {
        // 移除成就记录
        recordManager.removeRecord(advancement.id());

        for (AdvancementCompletionListener listener : listeners) {
            listener.onAdvancementRemoved(advancement);
        }
    }

    public List<AdvancementRecord> getAdvancementRecords() {
        return recordManager.getRecords();
    }
}