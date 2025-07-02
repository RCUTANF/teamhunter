package com.rcutanf.teamhunter;

import net.fabricmc.fabric.api.entity.event.v1.ServerEntityCombatEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.scoreboard.ReadableScoreboardScore;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreHolder;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import static com.rcutanf.teamhunter.TeamUtils.getTeamPlayerNames;

public class MatchEndListener {

    public MatchEndListener() {
        // 注册末影龙死亡事件监听器
        ServerEntityCombatEvents.AFTER_KILLED_OTHER_ENTITY.register((world, entity, killed) -> {
            if (CommandConfig.getCurrentGamemode().equals("ct")) {
                // 检查被杀死的实体是否为末影龙
                if (killed instanceof EnderDragonEntity) {
                    CommandExecutor.executeCommand(
                            world.getServer(),
                            "/say source:" + entity
                    );
                    PhaseHandler.matchEnd(world.getServer());
                }
            }
        });

        // 添加玩家死亡监听器
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            if (!alive) { // 玩家确实死亡了
                MinecraftServer server = newPlayer.getServer();
                if (server == null) return;

                // 只有在"ct"玩法下才检查死亡次数
                if (CommandConfig.getCurrentGamemode().equals("ct")) {
                    checkDeathsAndEndMatch(server);
                }
            }
        });
    }

    // 从计分板获取队伍死亡总数
    private int calculateTeamDeaths(MinecraftServer server, String teamName) {
        int deaths = 0;
        Scoreboard scoreboard = server.getScoreboard();
        ScoreboardObjective objective = scoreboard.getNullableObjective("Deaths");
        if (objective == null) return 0;

        for (String playerName : getTeamPlayerNames(server, teamName)) {
            ReadableScoreboardScore score = scoreboard.getScore(ScoreHolder.fromName(playerName), objective);
            if (score != null) {
                deaths += score.getScore();
            }
        }
        return deaths;
    }

    // 检查死亡次数并在达到上限时结束比赛
    private void checkDeathsAndEndMatch(MinecraftServer server) {
        // 计算当前队伍死亡总数
        int hunterDeaths = calculateTeamDeaths(server, "hunters");
        int runnerDeaths = calculateTeamDeaths(server, "runners");

        // 更新计分板标题（红色表示猎人队，绿色表示逃亡者队）
        String title = "死亡次数 §c" + hunterDeaths + "§f:§a" + runnerDeaths;
        CommandExecutor.executeCommand(server, "/scoreboard objectives modify Deaths displayname \"" + title + "\"");



        // 计算死亡上限
        int deathsMax = TeamUtils.getMaxTeamPlayerCount(server) * 5 + 1;

        // 检查是否达到上限
        if (hunterDeaths >= deathsMax || runnerDeaths >= deathsMax) {
            CommandExecutor.executeCommand(server, "/say 死亡上限: " + deathsMax +
                    " (猎人: " + hunterDeaths + ", 逃亡者: " + runnerDeaths + ")");
            PhaseHandler.matchEnd(server);
        }
    }
}