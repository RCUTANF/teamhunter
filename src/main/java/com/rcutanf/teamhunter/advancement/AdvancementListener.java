package com.rcutanf.teamhunter.advancement;

import com.rcutanf.teamhunter.*;
import net.minecraft.advancement.AdvancementEntry;
import net.minecraft.network.packet.s2c.common.CustomPayloadS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.List;

public class AdvancementListener {
    private final MinecraftServer server;
    private final AdvancementScoreLoader scoreLoader;

    // 直接使用变量存储队伍分数
    private static int huntersScore = 0;
    private static int runnersScore = 0;

    private static boolean huntersHadAdvantage = false;
    private static boolean runnersHadAdvantage = false;

    // 定义领先状态枚举
    public enum AdvantageState {
        HUNTERS_ADVANTAGE,  // 猎人队领先
        RUNNERS_ADVANTAGE,  // 逃亡者队领先
        NO_ADVANTAGE        // 无领先优势
    }


    // 记录上一次的领先状态
    private static AdvantageState lastAdvantageState = AdvantageState.NO_ADVANTAGE;


    public AdvancementListener(MinecraftServer server) {
        this.server = server;
        this.scoreLoader = new AdvancementScoreLoader();
        PlayerAdvancementCallback.EVENT.register(this::onAdvancement);
    }

    private void onAdvancement(ServerPlayerEntity player, Text title, AdvancementEntry advancementEntry) {
        // 跳过非比赛阶段
        if (Teamhunter.phaseManager.Phase() != Phase.MATCH) return;

        Identifier id = advancementEntry.id();
        int score = scoreLoader.getScore(id);
        if (score <= 0) return;

        // 只处理指定队伍的玩家
        String teamName = player.getScoreboardTeam() != null
                ? player.getScoreboardTeam().getName()
                : "";

        if (!teamName.equals("hunters") && !teamName.equals("runners")) return;

        // 更新分数
        addTeamScore(teamName, score, server, null);
    }

    // 获取当前猎人队伍分数
    public static int getHuntersScore() {
        return huntersScore;
    }

    // 获取当前逃亡者队伍分数
    public static int getRunnersScore() {
        return runnersScore;
    }

    // 重置分数（比赛开始时调用）
    public static void resetScores() {
        huntersScore = 0;
        runnersScore = 0;
        lastAdvantageState = AdvantageState.NO_ADVANTAGE;
    }

    public static void sendTeamScoreUpdate(MinecraftServer server, int huntersScore, int runnersScore,
                                           int huntersAddedScore, int runnersAddedScore) {
        NetWorking.TeamScorePacket packet = new NetWorking.TeamScorePacket(huntersScore, runnersScore, huntersAddedScore, runnersAddedScore);

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            player.networkHandler.sendPacket(new CustomPayloadS2CPacket(packet));
        }
    }

    /**
     * 增加指定队伍的分数
     * @param teamName 队伍名称 ("hunters" 或 "runners")
     * @param amount 要增加的分数值
     * @param server 服务器实例，用于发送分数更新包
     * @param message 可选的消息，解释为什么增加分数
     * @return 增加后的新分数
     */
    public static int addTeamScore(String teamName, int amount, MinecraftServer server, Text message) {

        // 验证参数
        if (amount <= 0) {
            return teamName.equals("hunters") ? huntersScore : runnersScore;
        }

        // 增加对应队伍的分数
        int huntersAddedScore = 0;
        int runnersAddedScore = 0;

        if (teamName.equals("hunters")) {
            huntersScore += amount;
            huntersAddedScore = amount;
        } else if (teamName.equals("runners")) {
            runnersScore += amount;
            runnersAddedScore = amount;
        } else {
            // 如果队伍名称无效，返回0
            return 0;
        }

        // 如果提供了消息，显示加分通知
        if (message != null) {
            server.getPlayerManager().broadcast(
                    Text.of(String.format("%s队伍", teamName))
                            .copy().append(message)
                            .append(Text.literal(" (+" + amount + "分)").formatted(Formatting.GOLD, Formatting.BOLD)),
                    false
            );
        }

        // 应用队伍优势效果
        applyTeamAdvantageEffects(server, huntersScore, runnersScore);

        // 发送分数更新包
        sendTeamScoreUpdate(server, huntersScore, runnersScore, huntersAddedScore, runnersAddedScore);

        // 返回当前队伍的新分数
        return teamName.equals("hunters") ? huntersScore : runnersScore;
    }

    /**
     * 减少指定队伍的分数
     * @param teamName 队伍名称 ("hunters" 或 "runners")
     * @param amount 要减少的分数值
     * @param server 服务器实例，用于发送分数更新包
     * @param message 可选的消息，解释为什么减少分数
     * @return 减少后的新分数
     */
    public static int reduceTeamScore(String teamName, int amount, MinecraftServer server, Text message) {
        // 验证参数
        if (amount <= 0) {
            return teamName.equals("hunters") ? huntersScore : runnersScore;
        }

        // 减少对应队伍的分数
        int huntersReducedScore = 0;
        int runnersReducedScore = 0;

        if (teamName.equals("hunters")) {
            huntersScore = Math.max(0, huntersScore - amount); // 确保分数不会变为负数
            huntersReducedScore = amount;
        } else if (teamName.equals("runners")) {
            runnersScore = Math.max(0, runnersScore - amount); // 确保分数不会变为负数
            runnersReducedScore = amount;
        } else {
            // 如果队伍名称无效，返回0
            return 0;
        }


        // 如果提供了消息，显示减分通知
        if (message != null) {
            server.getPlayerManager().broadcast(
                    Text.of(String.format("%s队伍", teamName))
                            .copy().append(message)
                            .append(Text.literal(" (-"+amount+"分)").formatted(Formatting.GOLD, Formatting.BOLD)),
                    false
            );
        }

        // 应用队伍优势效果
        applyTeamAdvantageEffects(server, huntersScore, runnersScore);

        // 发送分数更新包，使用负值表示减少的分数
        sendTeamScoreUpdate(server, huntersScore, runnersScore, -huntersReducedScore, -runnersReducedScore);

        // 返回当前队伍的新分数
        return teamName.equals("hunters") ? huntersScore : runnersScore;
    }

    private static void applyTeamAdvantageEffects(MinecraftServer server, int huntersScore, int runnersScore) {
        // 计算当前领先状态
        AdvantageState currentState;
        if (huntersScore - runnersScore > 100) {
            currentState = AdvantageState.HUNTERS_ADVANTAGE;
        } else if (runnersScore - huntersScore > 100) {
            currentState = AdvantageState.RUNNERS_ADVANTAGE;
        } else {
            currentState = AdvantageState.NO_ADVANTAGE;
        }

        // 如果状态没有变化，直接返回
        if (currentState == lastAdvantageState) {
            return;
        }

        // 状态已变化，更新记录并应用新效果
        lastAdvantageState = currentState;

        switch (currentState) {
            case HUNTERS_ADVANTAGE:
                setAllTeamplayerVisible(server, "runners", true);

                // 发送网络包通知客户端更新UI
                sendAdvantageBuffUpdate(server.getOverworld(), true, true);
                sendAdvantageBuffUpdate(server.getOverworld(), false, false);
                break;

            case RUNNERS_ADVANTAGE:
                setAllTeamplayerVisible(server, "hunters", true);
                // 发送网络包通知客户端更新UI
                sendAdvantageBuffUpdate(server.getOverworld(), false, true);
                sendAdvantageBuffUpdate(server.getOverworld(), true, false);
                break;

            case NO_ADVANTAGE:
                setAllTeamplayerVisible(server, "runners", false);
                setAllTeamplayerVisible(server, "hunters", false);
                // 发送网络包通知客户端更新UI
                sendAdvantageBuffUpdate(server.getOverworld(), true, false);
                sendAdvantageBuffUpdate(server.getOverworld(), false, false);
                break;
        }
    }

    // 添加一个新方法发送优势Buff更新包
    private static void sendAdvantageBuffUpdate(ServerWorld world, boolean isHunterTeam, boolean hasAdvantage) {
        // 创建一个新的优势Buff数据包
        NetWorking.AdvantageBuffPacket packet =
                new NetWorking.AdvantageBuffPacket(isHunterTeam, hasAdvantage);

        MinecraftServer server = world.getServer();
        // 使用与其他数据包一致的发送方式
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            player.networkHandler.sendPacket(new CustomPayloadS2CPacket(packet));
        }
    }

    public static AdvantageState getLastAdvantageState(){
        // 返回当前的领先状态
        return lastAdvantageState;
    }

    /**
     * 设置指定队伍的所有玩家为可见状态
     * @param server Minecraft服务器实例
     */
    public static void setAllTeamplayerVisible(MinecraftServer server, String teamName, boolean isVisible) {
        // 获取所有逃亡者队伍的玩家名称
        List<String> teamPlayerNames = TeamUtils.getTeamPlayerNames(server, teamName);

        // 遍历所有逃亡者玩家并设置为可见
        for (String playerName : teamPlayerNames) {
            // 通过名称获取玩家实体
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(playerName);
            if (player != null) {
                // 调用之前实现的方法设置玩家为可见
                PlayerVisibilityTracker.setPlayerVisibility(player, isVisible);
            }
        }
    }
}