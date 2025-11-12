package com.rcutanf.teamhunter.client.guide_sys.impl.trigger;

import com.rcutanf.teamhunter.client.guide_sys.AbstractGuideSysTrigger;
import com.rcutanf.teamhunter.client.guide_sys.TriggerType;
import net.minecraft.advancement.AdvancementEntry;
import net.minecraft.advancement.AdvancementProgress;
import net.minecraft.advancement.PlacedAdvancement;

import java.util.HashMap;
import java.util.Map;

public class AdvancementProgressTrigger extends AbstractGuideSysTrigger {
    private static AdvancementProgressTrigger instance;

    public AdvancementProgressTrigger() {
        super(TriggerType.advancementProcess);
        instance = this;
    }

    public static AdvancementProgressTrigger getInstance() {
        return instance;
    }

    public void onAdvancementProgress(PlacedAdvancement placedAdvancement, AdvancementProgress progress) {
        Map<String, Object> eventData = createAdvancementProgressEventData(placedAdvancement, progress);
        fire(eventData);
    }

    private Map<String, Object> createAdvancementProgressEventData(PlacedAdvancement placedAdvancement, AdvancementProgress progress) {
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("advancementId", placedAdvancement.getAdvancementEntry().id());
        eventData.put("advancementProgress", progress);
        eventData.put("timestamp", System.currentTimeMillis());
        return eventData;
    }
}