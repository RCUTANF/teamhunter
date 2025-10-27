package com.rcutanf.teamhunter;

// 导入正确的包
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.registry.RegistryKey;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.Random;

public class EnvironmentController {
    private final MinecraftServer server;
    private static final Map<UUID, String> blazeKillerTeams = new HashMap<>();
    private static final Random random = new Random();
    public enum TeamAdvantage {
        NONE,       // 无优势或相等
        HUNTERS,    // 猎人队优势
        RUNNERS     // 逃亡者队优势
    }

    //当前地狱中的优势队伍
    private static TeamAdvantage netherAdvantageTeam = TeamAdvantage.NONE;

    public static TeamAdvantage getNetherAdvantageTeam() {
        return netherAdvantageTeam;
    }

    public EnvironmentController(MinecraftServer server) {
        this.server = server;

        // 注册tick事件监听器
        ServerTickEvents.START_SERVER_TICK.register(this::onServerTick);

        // 注册烈焰人相关事件
        registerBlazeEvents();
    }

    // 静态初始化方法，在主类中调用
    public static void init(MinecraftServer server) {
        new EnvironmentController(server);
    }

    // 注册烈焰人相关事件处理
    private void registerBlazeEvents() {
        // 监听实体死亡事件
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {

        });
    }



    private void onServerTick(MinecraftServer server) {
        // 只在比赛阶段执行
        if (Teamhunter.phaseManager.Phase() != Phase.MATCH) {
            return;
        }

        // 处理末地龙抗性
        handleEnderDragonResistance();

        if (CommandConfig.getCurrentGamemode()==null) {
            return; // 如果不是CT模式，直接返回
        }
        // 更新玩家的烈焰棒掉落权限标签
        updateBlazeDroppingPermissions(server);
    }

    // 处理末地龙抗性
    private void handleEnderDragonResistance() {
        ServerWorld endWorld = server.getWorld(World.END);
        if (endWorld == null) return;

        // 检查末地中的队伍情况
        int huntersInEnd = countTeamPlayersInDimension("hunters", World.END);
        int runnersInEnd = countTeamPlayersInDimension("runners", World.END);

        // 检查末影龙状态
        boolean shouldEnhanceDragon = (huntersInEnd > 0 && runnersInEnd > 0);
        //打印人数
        //System.out.println("[TeamHunter] 末地队伍人数: 猎人 " + huntersInEnd + ", 逃亡者 " + runnersInEnd);

        for (Entity entity : endWorld.getEntitiesByType(net.minecraft.entity.EntityType.ENDER_DRAGON, entity -> true)) {
            if (entity instanceof EnderDragonEntity dragon) {
                boolean isCurrentlyEnhanced = dragon.hasCustomName() &&
                        dragon.getCustomName().getString().contains("[增强]");

                // 状态需要改变
                if (shouldEnhanceDragon != isCurrentlyEnhanced) {
                    if (shouldEnhanceDragon) {
                        // 增强龙的抗性
                        dragon.setCustomName(Text.of("[增强] 末影龙"));
                        dragon.setCustomNameVisible(true);

                        // 增加抗性（相当于只受20%的伤害）
                        CommandExecutor.executeCommand(server,
                                "/execute as @e[type=ender_dragon] run attribute @s minecraft:armor base set 30");
                        CommandExecutor.executeCommand(server,
                                "/execute as @e[type=ender_dragon] run attribute @s minecraft:armor_toughness base set 20");
                    } else {
                        // 恢复正常状态
                        dragon.setCustomName(Text.of("末影龙"));
                        dragon.setCustomNameVisible(true);

                        // 重置抗性到默认值
                        CommandExecutor.executeCommand(server,
                                "/execute as @e[type=ender_dragon] run attribute @s minecraft:armor base set 0");
                        CommandExecutor.executeCommand(server,
                                "/execute as @e[type=ender_dragon] run attribute @s minecraft:armor_toughness base set 0");
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

    private void updateBlazeDroppingPermissions(MinecraftServer server) {
        // 检查地狱中的队伍人数
        int huntersInNether = countTeamPlayersInDimension(server, "hunters", World.NETHER);
        int runnersInNether = countTeamPlayersInDimension(server, "runners", World.NETHER);

        boolean huntersCanDrop = false;
        boolean runnersCanDrop = false;

        // 更新优势队伍状态
        TeamAdvantage previousAdvantage = netherAdvantageTeam;

        if (huntersInNether == 0 && runnersInNether == 0) {
            netherAdvantageTeam = TeamAdvantage.NONE;
        } else if (huntersInNether == runnersInNether) {
            netherAdvantageTeam = TeamAdvantage.NONE;
            huntersCanDrop = true;
            runnersCanDrop = true;
        } else if (huntersInNether > runnersInNether) {
            netherAdvantageTeam = TeamAdvantage.HUNTERS;
            huntersCanDrop = true;
            runnersCanDrop = false;
        } else {
            netherAdvantageTeam = TeamAdvantage.RUNNERS;
            huntersCanDrop = false;
            runnersCanDrop = true;
        }

        // 更新所有在地狱的玩家标签
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            if (player.getWorld().getRegistryKey() == World.NETHER) {
                Team playerTeam = TeamUtils.getPlayerTeam(player);

                boolean canDrop;
                if ("hunters".equals(playerTeam.getName())) {
                    canDrop = huntersCanDrop;
                } else if ("runners".equals(playerTeam.getName())) {
                    canDrop = runnersCanDrop;
                } else {
                    // 观察者、管理员等其他玩家默认可以掉落烈焰棒
                    canDrop = true;
                }

                updatePlayerTag(player, canDrop);
            }
        }

        // 如果优势状态发生变化，向所有玩家发送更新
        if (previousAdvantage != netherAdvantageTeam) {
            System.out.println("[TeamHunter] 地狱优势状态更新: " + netherAdvantageTeam);
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                ServerPlayNetworking.send(player, new NetWorking.TeamAdvantagePacket(netherAdvantageTeam.ordinal()));
            }
        }
    }

    // 更新玩家的标签
    private void updatePlayerTag(ServerPlayerEntity player, boolean canDropBlazeRod) {
        boolean hasTag = player.getCommandTags().contains("can_drop_blaze_rod");

        if (canDropBlazeRod && !hasTag) {
            // 添加掉落权限标签
            player.addCommandTag("can_drop_blaze_rod");
        } else if (!canDropBlazeRod && hasTag) {
            // 移除掉落权限标签
            player.removeCommandTag("can_drop_blaze_rod");
        }
    }
}