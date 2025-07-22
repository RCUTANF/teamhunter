package com.rcutanf.teamhunter;

import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Formatting;

import java.util.*;

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
    /**
     * 获取所有队伍中人数最多的队伍的人数
     * @param server 服务器实例
     * @return 所有队伍中人数的最大值（如果没有队伍或所有队伍都没有玩家，返回0）
     */
    public static int getMaxTeamPlayerCount(MinecraftServer server) {
        Scoreboard scoreboard = server.getScoreboard();
        Collection<Team> teams = scoreboard.getTeams();

        if (teams.isEmpty()) {
            return 0;
        }

        return teams.stream()
                .mapToInt(team -> team.getPlayerList().size())
                .max()
                .orElse(0);
    }

    /**
     * 获取玩家所属的队伍
     * @param player 玩家实体
     * @return 玩家所属的队伍（如果玩家不在任何队伍中，返回null）
     */
    public static Team getPlayerTeam(ServerPlayerEntity player) {
        if (player == null) {
            return null;
        }
        return player.getScoreboardTeam();
    }

    public static String getPlayerTeamName(ServerPlayerEntity player) {
        Team team = getPlayerTeam(player);
        return (team != null) ? team.getName() : null;
    }

    /**
     * 检查玩家是否属于指定队伍
     * @param player 玩家实体
     * @param teamName 队伍名称
     * @return 如果玩家属于指定队伍，返回true；否则返回false
     */
    public static boolean isPlayerInTeam(ServerPlayerEntity player, String teamName) {
        Team team = getPlayerTeam(player);
        return team != null && team.getName().equals(teamName);
    }

    /**
     * 检查玩家是否属于猎人或逃亡者队伍
     * @param player 玩家实体
     * @return 如果玩家属于猎人或逃亡者队伍，返回true；否则返回false
     */
    public static boolean isPlayerInGameTeam(ServerPlayerEntity player) {
        Team team = getPlayerTeam(player);
        if (team == null) {
            return false;
        }
        String teamName = team.getName();
        return teamName.equals("hunters") || teamName.equals("runners");
    }

    /**
     * 获取队伍分配的颜色
     * @param team 队伍对象
     * @return 队伍的颜色（如果队伍为null，返回null）
     */
    public static Formatting getTeamColor(Team team) {
        if (team == null) {
            return null;
        }
        return team.getColor();
    }

    /**
     * 获取队伍分配的颜色
     * @param server 服务器实例
     * @param teamName 队伍名称
     * @return 队伍的颜色（如果队伍不存在，返回null）
     */
    public static Formatting getTeamColor(MinecraftServer server, String teamName) {
        Team team = server.getScoreboard().getTeam(teamName);
        return getTeamColor(team);
    }

    /**
     * 获取玩家所属队伍的颜色
     * @param player 玩家实体
     * @return 玩家所属队伍的颜色（如果玩家不在任何队伍中，返回null）
     */
    public static Formatting getPlayerTeamColor(ServerPlayerEntity player) {
        Team team = getPlayerTeam(player);
        return getTeamColor(team);
    }


}