package com.rcutanf.teamhunter;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.scoreboard.*;
import net.minecraft.server.MinecraftServer;

import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.server.network.ServerPlayerEntity;


import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static com.rcutanf.teamhunter.TeamUtils.getTeamPlayerNames;


public class PhaseHandler {
    private final MinecraftServer server;
    private final MatchEndListener matchEndListener = new MatchEndListener();
    //监听器用
    private int delayTicks = 0;
    private boolean shouldFreeze = false;

    // 死亡统计用
    private int lastHunterDeaths = 0;
    private int lastRunnerDeaths = 0;
    private int DeathsMax = 0;

    public PhaseHandler(MinecraftServer server) {
        this.server = server;
        ServerTickEvents.START_SERVER_TICK.register(tickServer -> {

            if (shouldFreeze) {
                delayTicks++;
                if (delayTicks >= 20) { // 20 ticks = 1秒
                    //System.out.println("[DEBUG] Tick监听器被触发");
                    FreezeAllPlayers(server);
                    shouldFreeze = false;
                }
            }

            // 在比赛阶段监听计分板变化
            if (Teamhunter.phaseManager.Phase() == Phase.MATCH) {
                checkAndUpdateDeathStats();
            }
        });
    }

    // WARMUP 阶段逻辑
    public void onWarmupStart() {
        CommandExecutor.executeCommand(server, "/luckperms group default permission set minecraft.command.trigger.* false");
        //设置牢房
        CommandExecutor.executeCommand(server, "/fill 10 256 10 -10 260 -10 minecraft:barrier");
        CommandExecutor.executeCommand(server, "/fill 9 257 9 -9 260 -9 minecraft:air");

        CommandExecutor.executeCommand(server, "/tp @a 0 257 0");
        CommandExecutor.executeCommand(server, "/gamemode survival @a[team=runners]");
        CommandExecutor.executeCommand(server, "/gamemode survival @a[team=hunters]");
        CommandExecutor.executeCommand(server, "/luckperms group default permission set minecraft.command.gamemode false");

        CommandExecutor.executeCommand(server, "/clear @a[team=runners]");
        CommandExecutor.executeCommand(server, "/clear @a[team=hunters]");

        //设置玩家无敌
        CommandExecutor.executeCommand(server, "/effect give @a[team=runners] minecraft:resistance 1000000 255 true");
        CommandExecutor.executeCommand(server, "/effect give @a[team=hunters] minecraft:resistance 1000000 255 true");
        CommandExecutor.executeCommand(server, "/say 赛前热身阶段，请各位玩家充分交流，制定计划，做好准备");

        //计分板
        CommandExecutor.executeCommand(server, "/scoreboard objectives add Deaths deathCount \"死亡次数\"");
        CommandExecutor.executeCommand(server, "/scoreboard objectives setdisplay sidebar Deaths");
    }

    // PREPARE 阶段逻辑
    public void onPrepareStart() {
        CommandExecutor.executeCommand(server, "/gamerule doImmediateRespawn true");
        CommandExecutor.executeCommand(server, "/kill @a[team=runners]");
        CommandExecutor.executeCommand(server, "/kill @a[team=hunters]");

        CommandExecutor.executeCommand(server, "/scoreboard players reset @a[team=runners] Deaths");
        CommandExecutor.executeCommand(server, "/scoreboard players reset @a[team=hunters] Deaths");
        CommandExecutor.executeCommand(server, "/team modify hunters friendlyFire false");
        CommandExecutor.executeCommand(server, "/team modify runners friendlyFire false");
        CommandExecutor.executeCommand(server, "/time set day");
        CommandExecutor.executeCommand(server, "/say 准备阶段，请及时确认你的伙伴位置");

        // 设置世界边界
        CommandExecutor.executeCommand(server, "/worldborder center 0 0");
        CommandExecutor.executeCommand(server, "/worldborder set 1000"); // ±500*±500的边界


        // 重置死亡统计
        lastHunterDeaths = 0;
        lastRunnerDeaths = 0;

        // 初始化计分板显示
        String title = "死亡次数 §c0§f:§a0";
        CommandExecutor.executeCommand(server, "/scoreboard objectives modify Deaths displayname \"" + title + "\"");
        //触发监听器调用freezeall
        shouldFreeze = true;
        delayTicks = 0;



    }

    // MATCH 阶段逻辑
    public void onMatchStart() {
        CommandExecutor.executeCommand(server, "/say §aGO!");
        CommandExecutor.executeCommand(server, "/gamerule doImmediateRespawn false");
        CommandExecutor.executeCommand(server, "/fill 10 256 10 -10 260 -10 minecraft:air");

        // 设置世界边界以每秒1格的速度扩展
        CommandExecutor.executeCommand(server, "/worldborder add 100000 100000");


        unFreezeAllPlayers(server);
    }


    // 共用方法
    public static void freezePlayer(MinecraftServer server, ServerPlayerEntity player){
        CommandExecutor.executeCommand(server, "/attribute @s minecraft:block_interaction_range modifier add freeze -1 add_multiplied_total");
        CommandExecutor.executeCommand(server, "/attribute @s minecraft:jump_strength modifier add freeze -1 add_multiplied_total");
        CommandExecutor.executeCommand(server, "/attribute @s minecraft:movement_speed modifier add freeze -1 add_multiplied_total");

        /*无法生效的新方案
        player.getAbilities().setWalkSpeed(0f);
        player.getAbilities().setFlySpeed(0f);
        player.getAbilities().allowFlying = false;
        player.getAbilities().flying = false;
        player.getAbilities().allowModifyWorld = false;
        player.setInvulnerable(true);
        player.getAbilities().allowModifyWorld = false;

         */
    }
    public static void unFreezePlayer(MinecraftServer server, ServerPlayerEntity player){
        CommandExecutor.executeCommand(server, "/attribute @s minecraft:block_interaction_range modifier remove freeze");
        CommandExecutor.executeCommand(server, "/attribute @s minecraft:jump_strength modifier remove freeze");
        CommandExecutor.executeCommand(server, "/attribute @s minecraft:movement_speed modifier remove freeze");
        /*
        player.getAbilities().setWalkSpeed(0.1f); // 默认行走速度
        player.getAbilities().setFlySpeed(0.05f);
        player.getAbilities().allowFlying = false;
        player.getAbilities().flying = false;
        player.getAbilities().allowModifyWorld = true;
        player.setInvulnerable(false);
        player.getAbilities().allowModifyWorld = true;

         */
    }
    public static void unFreezeAllPlayers(MinecraftServer server) {
        CommandExecutor.executeCommand(server, "/tick unfreeze");
        String[] teams = {"runners", "hunters"};
        for (String team : teams) {
            CommandExecutor.executeCommand(server, "/execute as @a[team=" + team + "] run attribute @s minecraft:block_interaction_range modifier remove freeze");
            CommandExecutor.executeCommand(server, "/execute as @a[team=" + team + "] run attribute @s minecraft:jump_strength modifier remove freeze");
            CommandExecutor.executeCommand(server, "/execute as @a[team=" + team + "] run attribute @s minecraft:movement_speed modifier remove freeze");
            CommandExecutor.executeCommand(server, "/execute as @a[team=" + team + "] run attribute @s minecraft:attack_damage modifier remove freeze");
            CommandExecutor.executeCommand(server, "/execute as @a[team=" + team + "] run attribute @s minecraft:attack_knockback modifier remove freeze");
        }
        /*
        for (String teamName : new String[]{"hunters", "runners"}) {
            for (String playerName : getTeamPlayerNames(server, teamName)) {
                ServerPlayerEntity player = server.getPlayerManager().getPlayer(playerName);
                if (player != null) {
                    unFreezePlayer(server, player);
                }
            }
        }

         */
    }
    public static void FreezeAllPlayers(MinecraftServer server) {
        CommandExecutor.executeCommand(server, "/tick freeze");
        String[] teams = {"runners", "hunters"};
        for (String team : teams) {
            CommandExecutor.executeCommand(server, "/execute as @a[team=" + team + "] run attribute @s minecraft:block_interaction_range modifier add freeze -1 add_multiplied_total");
            CommandExecutor.executeCommand(server, "/execute as @a[team=" + team + "] run attribute @s minecraft:jump_strength modifier add freeze -1 add_multiplied_total");
            CommandExecutor.executeCommand(server, "/execute as @a[team=" + team + "] run attribute @s minecraft:movement_speed modifier add freeze -1 add_multiplied_total");
            CommandExecutor.executeCommand(server, "/execute as @a[team=" + team + "] run attribute @s minecraft:attack_damage modifier add freeze -1 add_multiplied_total");
            CommandExecutor.executeCommand(server, "/execute as @a[team=" + team + "] run attribute @s minecraft:attack_knockback modifier add freeze -1 add_multiplied_total");
        }
        /*
        for (String teamName : new String[]{"hunters", "runners"}) {
            for (String playerName : getTeamPlayerNames(server, teamName)) {
                ServerPlayerEntity player = server.getPlayerManager().getPlayer(playerName);
                if (player != null) {
                    freezePlayer(server, player);
                }
            }
        }

         */
    }

    // 检查计分板变化并更新标题
    private void checkAndUpdateDeathStats() {
        // 计算当前队伍死亡总数
        int hunterDeaths = calculateTeamDeaths("hunters");
        int runnerDeaths = calculateTeamDeaths("runners");

        // 只在数值变化时更新标题
        if (hunterDeaths != lastHunterDeaths || runnerDeaths != lastRunnerDeaths) {
            lastHunterDeaths = hunterDeaths;
            lastRunnerDeaths = runnerDeaths;

            // 更新计分板标题（红色表示猎人队，绿色表示逃亡者队）
            String title = "死亡次数 §c" + hunterDeaths + "§f:§a" + runnerDeaths;
            CommandExecutor.executeCommand(server, "/scoreboard objectives modify Deaths displayname \"" + title + "\"");


            //如果死亡次数达到上限
            DeathsMax = TeamUtils.getMaxTeamPlayerCount(server)*5+1;
            CommandExecutor.executeCommand(server, "/say "+DeathsMax);
            if (hunterDeaths >= DeathsMax || runnerDeaths >= DeathsMax) {
                matchEnd(server);
            }
        }
    }

    // 计算队伍的总死亡数
    private int calculateTeamDeaths(String teamName) {
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

    public static void matchEnd(MinecraftServer server) {
        if (Teamhunter.phaseManager.Phase() == Phase.MATCH) {
            CommandExecutor.executeCommand(server, "/say §4比赛已结束");
            CommandExecutor.executeCommand(server, "/luckperms group default permission set minecraft.command.trigger.* true");
            CommandExecutor.executeCommand(server, "/luckperms group default permission set minecraft.command.gamemode true");
            //取消边界
            CommandExecutor.executeCommand(server, "/worldborder set 29999984"); // 将世界边界重置为Minecraft的默认值

            Teamhunter.phaseManager.clear();
            Teamhunter.phaseManager.then(Phase.END);
        }
    }

}
