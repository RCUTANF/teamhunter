package com.rcutanf.teamhunter.advancement;

import com.rcutanf.teamhunter.Phase;
import com.rcutanf.teamhunter.Teamhunter;
import com.rcutanf.teamhunter.CommandExecutor;
import net.minecraft.advancement.AdvancementEntry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class AdvancementListener {
    private final MinecraftServer server;
    private final AdvancementScoreLoader scoreLoader;

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

        // 添加队伍分数
        CommandExecutor.executeCommand(server,
                "scoreboard players add " + teamName + " TeamScore " + score
        );

        // 调试信息（直接在控制台打印）
        String playerName = player.getName().getString();
        String achievement = title != null ? title.getString() : id.toString();
        System.out.printf("%s 为 %s 队获得成就 %s (+%d分)%n",
                playerName, teamName, achievement, score);
    }
}