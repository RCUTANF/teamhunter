package com.rcutanf.teamhunter.advancement;

import com.rcutanf.teamhunter.NetWorking;
import com.rcutanf.teamhunter.Phase;
import com.rcutanf.teamhunter.Teamhunter;
import com.rcutanf.teamhunter.CommandExecutor;
import net.minecraft.advancement.AdvancementEntry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.network.packet.s2c.common.CustomPayloadS2CPacket;

public class AdvancementListener {
    private final MinecraftServer server;
    private final AdvancementScoreLoader scoreLoader;

    // 直接使用变量存储队伍分数
    private static int huntersScore = 0;
    private static int runnersScore = 0;

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

        // 添加队伍分数（不再使用计分板，而是直接更新变量）
        int huntersAddedScore = 0;
        int runnersAddedScore = 0;

        if (teamName.equals("hunters")) {
            huntersScore += score;
            huntersAddedScore = score;
        } else {
            runnersScore += score;
            runnersAddedScore = score;
        }

        // 调试信息（直接在控制台打印）
        String playerName = player.getName().getString();
        String achievement = title != null ? title.getString() : id.toString();
        CommandExecutor.executeCommand(server,
                String.format("say %s 为 %s 队获得成就 %s (+%d分)",
                        playerName, teamName, achievement, score));

        // 可选：仍然保留计分板更新，用于其他地方显示
        CommandExecutor.executeCommand(server,
                "scoreboard players set hunters TeamScore " + huntersScore
        );
        CommandExecutor.executeCommand(server,
                "scoreboard players set runners TeamScore " + runnersScore
        );

        // 发送分数更新包
        sendTeamScoreUpdate(player.getServerWorld(), huntersScore, runnersScore, huntersAddedScore, runnersAddedScore);
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
    }

    public static void sendTeamScoreUpdate(ServerWorld world, int huntersScore, int runnersScore,
                                           int huntersAddedScore, int runnersAddedScore) {
        NetWorking.TeamScorePacket packet = new NetWorking.TeamScorePacket(huntersScore, runnersScore, huntersAddedScore, runnersAddedScore);

        for (ServerPlayerEntity player : world.getPlayers()) {
            player.networkHandler.sendPacket(new CustomPayloadS2CPacket(packet));
        }
    }
}