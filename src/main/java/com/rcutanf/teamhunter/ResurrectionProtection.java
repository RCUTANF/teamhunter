package com.rcutanf.teamhunter;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

public class ResurrectionProtection {
    private static boolean invincibilityEnabled = true; // 默认开启无敌
    private static boolean invisibilityEnabled = false; // 默认关闭隐身
    private static boolean speedEnabled = false;        // 默认关闭速度
    private static final int DURATION_SECONDS = 10;     // 保护时间10秒

    // Getter和Setter方法
    public static boolean isInvincibilityEnabled() { return invincibilityEnabled; }
    public static void setInvincibilityEnabled(boolean enabled) { invincibilityEnabled = enabled; }

    public static boolean isInvisibilityEnabled() { return invisibilityEnabled; }
    public static void setInvisibilityEnabled(boolean enabled) { invisibilityEnabled = enabled; }

    public static boolean isSpeedEnabled() { return speedEnabled; }
    public static void setSpeedEnabled(boolean enabled) { speedEnabled = enabled; }

    // 应用复活保护效果
    public static void applyEffects(MinecraftServer server, ServerPlayerEntity player) {
        // 只在Match阶段对参赛玩家生效
        if (Teamhunter.phaseManager.Phase() != Phase.MATCH) {
            return;
        }

        String playerName = player.getName().getString();

        // 检查玩家是否属于参赛队伍
        if (isParticipant(player)) {
            if (invincibilityEnabled) {
                CommandExecutor.executeCommand(server,
                        "/effect give " + playerName + " minecraft:resistance " + DURATION_SECONDS + " 255 true");
            }

            if (invisibilityEnabled) {
                CommandExecutor.executeCommand(server,
                        "/effect give " + playerName + " minecraft:invisibility " + DURATION_SECONDS + " 0 true");
            }

            if (speedEnabled) {
                CommandExecutor.executeCommand(server,
                        "/effect give " + playerName + " minecraft:speed " + DURATION_SECONDS + " 1 true");
            }

            if (invincibilityEnabled || invisibilityEnabled || speedEnabled) {
                //CommandExecutor.executeCommand(server, "/title " + playerName + " subtitle {\"text\":\"复活保护生效中\",\"color\":\"green\"}");
                //CommandExecutor.executeCommand(server, "/title " + playerName + " title {\"text\":\"10秒\",\"color\":\"gold\"}");
            }
        }
    }

    // 检查玩家是否为参赛者(runner或hunter队伍成员)
    private static boolean isParticipant(ServerPlayerEntity player) {
        String teamName = player.getScoreboardTeam() != null ? player.getScoreboardTeam().getName() : "";
        return "runners".equals(teamName) || "hunters".equals(teamName);
    }
}