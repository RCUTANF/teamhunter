package com.rcutanf.teamhunter;

import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.*;
import java.util.stream.Collectors;

public final class TeamUtils {
    /**
     * 获取队伍的所有玩家名称（包括离线玩家）
     * @param server 服务器实例
     * @param teamName 队伍名称
     * @return 玩家名称列表（如果队伍不存在，返回空列表）
     */
    public static List<String> getTeamPlayerNames(MinecraftServer server, String teamName) {
        Team team = server.getScoreboard().getTeam(teamName);
        return (team != null) ? List.copyOf(team.getPlayerList()) : List.of();
    }

    /**
     * 获取队伍的所有在线玩家实体
     * @param server 服务器实例
     * @param teamName 队伍名称
     * @return 在线玩家列表（如果队伍不存在或没有在线玩家，返回空列表）
     */
    public static List<ServerPlayerEntity> getOnlineTeamPlayers(MinecraftServer server, String teamName) {
        return getTeamPlayerNames(server, teamName).stream()
                .map(server.getPlayerManager()::getPlayer)
                .filter(Objects::nonNull)
                .toList(); // Java 16+ 的 `toList()`（不可变列表）
    }


}