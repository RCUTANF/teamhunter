package com.rcutanf.teamhunter.advancement;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.advancement.AdvancementEntry;
import org.jetbrains.annotations.Nullable;

public interface PlayerAdvancementCallback {
    Event<PlayerAdvancementCallback> EVENT = EventFactory.createArrayBacked(
            PlayerAdvancementCallback.class,
            listeners -> (player, title, advancementEntry) -> {
                for (PlayerAdvancementCallback listener : listeners) {
                    listener.onAdvancementGet(player, title, advancementEntry);
                }
            }
    );

    void onAdvancementGet(ServerPlayerEntity player, @Nullable Text title, AdvancementEntry advancementEntry);
}