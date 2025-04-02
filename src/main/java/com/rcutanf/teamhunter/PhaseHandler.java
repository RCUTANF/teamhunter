package com.rcutanf.teamhunter;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

public class PhaseHandler {
    private final MinecraftServer server;

    public PhaseHandler(MinecraftServer server) {
        this.server = server;
    }

    // WARMUP 阶段逻辑
    public void onWarmupStart() {

    }

    // PREPARE 阶段逻辑
    public void onPrepareStart() {

    }

    // MATCH 阶段逻辑
    public void onMatchStart() {

    }


    // 共用方法
    public static void freezePlayer(ServerPlayerEntity player){

        player.getAbilities().setWalkSpeed(0f);
        player.getAbilities().setFlySpeed(0f);
        player.getAbilities().allowFlying = false;
        player.getAbilities().flying = false;
        player.getAbilities().allowModifyWorld = false;
    }
    public static void unFreezePlayer(ServerPlayerEntity player){
        player.getAbilities().setWalkSpeed(0.1f); // 默认行走速度
        player.getAbilities().setFlySpeed(0.05f);
        player.getAbilities().allowFlying = false;
        player.getAbilities().flying = false;
        player.getAbilities().allowModifyWorld = true;
    }
}
