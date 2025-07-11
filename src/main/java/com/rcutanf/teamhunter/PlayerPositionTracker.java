package com.rcutanf.teamhunter;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerPositionTracker {
    private final MinecraftServer server;
    private final Map<UUID, BlockPos> lastPositions = new HashMap<>();
    private int tickCounter = 0;
    private static final int TICKS_PER_SECOND = 20; // Minecraft每秒20个tick

    public PlayerPositionTracker(MinecraftServer server) {
        this.server = server;
    }

    public void tick() {
        tickCounter++;

        // 每秒执行一次检查
        if (tickCounter >= TICKS_PER_SECOND) {
            tickCounter = 0;
            checkPlayerPositions();
        }
    }

    private void checkPlayerPositions() {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            UUID playerId = player.getUuid();
            BlockPos currentPos = player.getBlockPos();

            // 获取上次记录的位置
            BlockPos lastPos = lastPositions.get(playerId);

            // 如果位置发生变化或者是第一次记录
            if (lastPos == null || !lastPos.equals(currentPos)) {
                recordPositionChange(player, currentPos);
                lastPositions.put(playerId, currentPos);
            }
        }
    }

    private void recordPositionChange(ServerPlayerEntity player, BlockPos newPos) {
        UUID playerId = player.getUuid();
        String playerName = player.getName().getString();

        // 创建数据包并发送给所有玩家
        NetWorking.PlayerPositionUpdatePacket packet = new NetWorking.PlayerPositionUpdatePacket(playerId, playerName, newPos);
        for (ServerPlayerEntity serverPlayer : server.getPlayerManager().getPlayerList()) {
            ServerPlayNetworking.send(serverPlayer, packet);
        }
    }
}