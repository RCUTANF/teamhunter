package com.rcutanf.teamhunter;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.mob.BlazeEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class EnvironmentController {
    private final MinecraftServer server;
    private static final Map<UUID, String> blazeKillerTeams = new HashMap<>();

    public EnvironmentController(MinecraftServer server) {
        this.server = server;

        // 注册tick事件监听器
        ServerTickEvents.START_SERVER_TICK.register(this::onServerTick);
    }

    private void onServerTick(MinecraftServer server) {
        // 只在比赛阶段执行
        if (Teamhunter.phaseManager.Phase() != Phase.MATCH) {
            return;
        }

        // 处理末地龙抗性
        handleEnderDragonResistance();
    }

    // 处理末地龙抗性
    private void handleEnderDragonResistance() {
        ServerWorld endWorld = server.getWorld(World.END);
        if (endWorld == null) return;

        // 检查末地中的队伍情况
        int huntersInEnd = countTeamPlayersInDimension("hunters", World.END);
        int runnersInEnd = countTeamPlayersInDimension("runners", World.END);

        // 如果两队都有人在末地，增强末影龙抗性
        if (huntersInEnd > 0 && runnersInEnd > 0) {
            for (Entity entity : endWorld.getEntitiesByType(net.minecraft.entity.EntityType.ENDER_DRAGON, entity -> true)) {
                if (entity instanceof EnderDragonEntity dragon) {
                    // 若龙没有抗性标签，添加抗性
                    if (!dragon.hasCustomName() || !dragon.getCustomName().getString().contains("[增强]")) {
                        dragon.setCustomName(Text.of("[增强] 末影龙"));
                        dragon.setCustomNameVisible(true);

                        // 增加80%的抗性（相当于只受20%的伤害）
                        CommandExecutor.executeCommand(server,
                            "/execute as @e[type=ender_dragon] run attribute @s minecraft:generic.armor base set 30");
                        CommandExecutor.executeCommand(server,
                            "/execute as @e[type=ender_dragon] run attribute @s minecraft:generic.armor_toughness base set 20");
                    }
                }
            }
        }
    }

    // 统计特定队伍在特定维度的玩家数
    private int countTeamPlayersInDimension(String teamName, RegistryKey<World> dimensionKey) {
        int count = 0;
        for (String playerName : TeamUtils.getTeamPlayerNames(server, teamName)) {
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(playerName);
            if (player != null && player.getWorld().getRegistryKey() == dimensionKey) {
                count++;
            }
        }
        return count;
    }

    // 记录杀死烈焰人的玩家所属队伍
    public static void registerBlazeKiller(BlazeEntity blaze, PlayerEntity player) {
        String teamName = null;
        if (player.getScoreboardTeam() != null) {
            teamName = player.getScoreboardTeam().getName();
        }

        if (teamName != null && (teamName.equals("hunters") || teamName.equals("runners"))) {
            blazeKillerTeams.put(blaze.getUuid(), teamName);
        }
    }

    // 检查烈焰人是否应该掉落物品
    public static boolean shouldBlazeDropItems(BlazeEntity blaze, MinecraftServer server) {
        String killerTeam = blazeKillerTeams.remove(blaze.getUuid());
        if (killerTeam == null) {
            return true; // 不是玩家杀死的，正常掉落
        }

        // 检查地狱中的队伍人数
        int huntersInNether = countTeamPlayersInDimension(server, "hunters", World.NETHER);
        int runnersInNether = countTeamPlayersInDimension(server, "runners", World.NETHER);

        // 如果队伍人数相同，正常掉落
        if (huntersInNether == runnersInNether) {
            return true;
        }

        // 如果是人数占优势的队伍杀死的，才掉落
        String dominantTeam = (huntersInNether > runnersInNether) ? "hunters" : "runners";
        return killerTeam.equals(dominantTeam);
    }

    // 静态方法，统计特定队伍在特定维度的玩家数
    private static int countTeamPlayersInDimension(MinecraftServer server, String teamName, RegistryKey<World> dimensionKey) {
        int count = 0;
        for (String playerName : TeamUtils.getTeamPlayerNames(server, teamName)) {
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(playerName);
            if (player != null && player.getWorld().getRegistryKey() == dimensionKey) {
                count++;
            }
        }
        return count;
    }
}