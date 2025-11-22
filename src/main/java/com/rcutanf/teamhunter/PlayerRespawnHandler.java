package com.rcutanf.teamhunter;

import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;

public class PlayerRespawnHandler {
    public PlayerRespawnHandler() {
        // 注册玩家复活事件监听器
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            // 玩家复活后应用复活保护
            ResurrectionProtection.applyEffects(newPlayer.getEntityWorld().getServer(), newPlayer);
        });
    }
}