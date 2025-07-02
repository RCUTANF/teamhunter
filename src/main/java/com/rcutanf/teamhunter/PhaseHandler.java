package com.rcutanf.teamhunter;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;



public class PhaseHandler {
    private final MinecraftServer server;
    private final MatchEndListener matchEndListener = new MatchEndListener();
    //监听器用
    private int delayTicks = 0;
    private boolean shouldFreeze = false;


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
            //if (Teamhunter.phaseManager.Phase() == Phase.MATCH) {
            //    checkAndUpdateDeathStats();
            //}
        });
    }

    // WARMUP 阶段逻辑
    public void onWarmupStart() {
        for (String command : CommandConfig.getCommandsForPhase("warmup")) {
            CommandExecutor.executeCommand(server, command);
        }
    }

    // PREPARE 阶段逻辑
    public void onPrepareStart() {
        for (String command : CommandConfig.getCommandsForPhase("prepare")) {
            CommandExecutor.executeCommand(server, command);
        }


        // 设置世界边界
        CommandExecutor.executeCommand(server, "/worldborder center 0 0");
        CommandExecutor.executeCommand(server, "/worldborder set "+ TeamUtils.getMaxTeamPlayerCount(server)*70 ); // ±500*±500的边界


        // 初始化计分板显示
        String title = "死亡次数 §c0§f:§a0";
        CommandExecutor.executeCommand(server, "/scoreboard objectives modify Deaths displayname \"" + title + "\"");
        //触发监听器调用freezeall
        shouldFreeze = true;
        delayTicks = 0;



    }

    // MATCH 阶段逻辑
    public void onMatchStart() {
        for (String command : CommandConfig.getCommandsForPhase("match")) {
            CommandExecutor.executeCommand(server, command);
        }


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
