package com.rcutanf.teamhunter.client.guide_sys.advancementListener;

import com.rcutanf.teamhunter.client.guide_sys.GuideSysCheckerManager;
import net.minecraft.advancement.AdvancementEntry;
import net.minecraft.advancement.AdvancementProgress;
import net.minecraft.advancement.PlacedAdvancement;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientAdvancementManager;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.Identifier;

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

    public void fireAdvancementRemoved(AdvancementEntry advancement) {
        for (AdvancementCompletionListener listener : listeners) {
            listener.onAdvancementRemoved(advancement);
        }
    }

    public PlacedAdvancement fromId(String namespace, String path) {
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity player = mc.player;
        if (player == null) {
            return null; // 玩家未加载或不存在
        }

        ClientAdvancementManager advancementManager = mc.player.networkHandler.getAdvancementHandler();
        Identifier advancementId = Identifier.of(namespace, path);

        return advancementManager.getManager().get(advancementId);
    }
}