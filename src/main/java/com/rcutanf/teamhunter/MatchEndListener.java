package com.rcutanf.teamhunter;

import com.rcutanf.teamhunter.advancement.AdvancementListener;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityCombatEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.scoreboard.ReadableScoreboardScore;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreHolder;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import static com.rcutanf.teamhunter.TeamUtils.getTeamPlayerNames;

public class MatchEndListener {

    public MatchEndListener() {
        // 注册末影龙死亡事件监听器
        ServerEntityCombatEvents.AFTER_KILLED_OTHER_ENTITY.register((world, entity, killed, damageSource) -> {
            if (Teamhunter.phaseManager.Phase() == Phase.MATCH && CommandConfig.getCurrentGamemode().equals("ct")) {
                // 检查被杀死的实体是否为末影龙
                if (killed instanceof EnderDragonEntity) {
                    CommandExecutor.executeCommand(
                            world.getServer(),
                            "/say source:" + entity
                    );
                    Teamhunter.phaseManager.clear();
                    Teamhunter.phaseManager.then(Phase.END);
                }
            }
        });

        // 添加玩家死亡监听器
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            if (!alive) { // 玩家确实死亡了
                MinecraftServer server = newPlayer.getEntityWorld().getServer();

                // 只有在"ct"玩法下才检查死亡次数
                if (Teamhunter.phaseManager.Phase() == Phase.MATCH && CommandConfig.getCurrentGamemode().equals("ct")) {
                    checkDeathsAndEndMatch(server);

                    // 获取死亡玩家的击杀者
                    ServerPlayerEntity killer = null;
                    if (oldPlayer.getAttacker() instanceof ServerPlayerEntity) {
                        killer = (ServerPlayerEntity) oldPlayer.getAttacker();
                    } else if (oldPlayer.getPrimeAdversary() instanceof ServerPlayerEntity) {
                        killer = (ServerPlayerEntity) oldPlayer.getPrimeAdversary();
                    }

                    if (killer != null) {
                        // 记录击杀信息
                        Text killerName = killer.getName();
                        Text victimName = oldPlayer.getName();
                        String teamName = TeamUtils.getPlayerTeamName(killer);
                        AdvancementListener.addTeamScore(teamName, 50, server,
                                victimName.copy()
                                        .append("被")
                                        .append(killerName)
                                        .append( " 击杀")
                         ,killer);
                    }
                }
            }
        });
    }


    /**
     * 辅助方法，从计分板获取指定队伍所有成员的死亡总数
     * 该方法会遍历指定队伍的所有玩家，并从计分板的"Deaths"目标中
     * 获取每个玩家的死亡分数，然后累加得到队伍总死亡次数。
     *
     * @param server 游戏服务器实例
     * @param teamName 队伍名称（如"hunters"或"runners"）
     * @return 指定队伍的总死亡次数；如果计分板中没有"Deaths"目标则返回0
     */
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


    /**
     * 检查团队死亡次数并在达到上限时结束比赛
     * 该方法计算"hunters"和"runners"队伍的总死亡次数，并更新计分板显示。
     * 如果任一队伍的死亡次数达到或超过上限（队伍最大人数 * 5 + 1），则结束比赛。
     * 计分板显示格式为"死亡次数 §c[猎人死亡数]§f:§a[逃亡者死亡数]"，其中猎人队以红色显示，
     * 逃亡者队以绿色显示。
     *
     * @param server Minecraft服务器实例，用于获取计分板和执行命令
     */
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
            Teamhunter.phaseManager.clear();
            Teamhunter.phaseManager.then(Phase.END);
        }
    }
}