package com.rcutanf.teamhunter;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import static com.rcutanf.teamhunter.TeamUtils.getTeamPlayerNames;


public class PhaseHandler {
    private final MinecraftServer server;
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

        CommandExecutor.executeCommand(server, "/scoreboard players set @a[team=runners] Deaths 0");
        CommandExecutor.executeCommand(server, "/scoreboard players set @a[team=hunters] Deaths 0");
        CommandExecutor.executeCommand(server, "/team modify hunters friendlyFire false");
        CommandExecutor.executeCommand(server, "/team modify runners friendlyFire false");
        CommandExecutor.executeCommand(server, "/time set day");
        CommandExecutor.executeCommand(server, "/say 准备阶段，请及时确认你的伙伴位置");

        //触发监听器调用freezeall
        shouldFreeze = true;
        delayTicks = 0;



    }

    // MATCH 阶段逻辑
    public void onMatchStart() {
        CommandExecutor.executeCommand(server, "/say &aGO!");
        CommandExecutor.executeCommand(server, "/gamerule doImmediateRespawn false");
        CommandExecutor.executeCommand(server, "/fill 10 256 10 -10 260 -10 minecraft:air");
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
        CommandExecutor.executeCommand(server, "/execute as @a[team=runners] run attribute @s minecraft:block_interaction_range modifier remove freeze");
        CommandExecutor.executeCommand(server, "/execute as @a[team=runners] run attribute @s minecraft:jump_strength modifier remove freeze");
        CommandExecutor.executeCommand(server, "/execute as @a[team=runners] run attribute @s minecraft:movement_speed modifier remove freeze");
        CommandExecutor.executeCommand(server, "/execute as @a[team=hunters] run attribute @s minecraft:block_interaction_range modifier remove freeze");
        CommandExecutor.executeCommand(server, "/execute as @a[team=hunters] run attribute @s minecraft:jump_strength modifier remove freeze");
        CommandExecutor.executeCommand(server, "/execute as @a[team=hunters] run attribute @s minecraft:movement_speed modifier remove freeze");
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
        CommandExecutor.executeCommand(server, "/execute as @a[team=runners] run attribute @s minecraft:block_interaction_range modifier add freeze -1 add_multiplied_total");
        CommandExecutor.executeCommand(server, "/execute as @a[team=runners] run attribute @s minecraft:jump_strength modifier add freeze -1 add_multiplied_total");
        CommandExecutor.executeCommand(server, "/execute as @a[team=runners] run attribute @s minecraft:movement_speed modifier add freeze -1 add_multiplied_total");
        CommandExecutor.executeCommand(server, "/execute as @a[team=hunters] run attribute @s minecraft:block_interaction_range modifier add freeze -1 add_multiplied_total");
        CommandExecutor.executeCommand(server, "/execute as @a[team=hunters] run attribute @s minecraft:jump_strength modifier add freeze -1 add_multiplied_total");
        CommandExecutor.executeCommand(server, "/execute as @a[team=hunters] run attribute @s minecraft:movement_speed modifier add freeze -1 add_multiplied_total");
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


}
