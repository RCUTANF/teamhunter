package com.rcutanf.teamhunter.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.scoreboard.Team;
import net.minecraft.util.math.BlockPos;

/**
 * 记录玩家位置信息的内部类
 */
public class PlayerPositionInfo {
    private final String playerName;
    private BlockPos position;
    private final String teamName;

    public PlayerPositionInfo(String playerName, BlockPos position, String teamName) {
        this.playerName = playerName;
        this.position = position;
        this.teamName = teamName;
    }

    public PlayerPositionInfo(String playerName, BlockPos position) {
        this(playerName, position, getTeamNameForPlayer(playerName));
    }

    public void updatePosition(BlockPos newPosition) {
        this.position = newPosition;
    }

    public String getPlayerName() {
        return playerName;
    }

    public BlockPos getPosition() {
        return position;
    }

    public String getTeamName() {
        return teamName;
    }

    private static String getTeamNameForPlayer(String playerName) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world != null) {
            Team playerTeam = client.world.getScoreboard().getTeam(playerName);
            return playerTeam != null ? playerTeam.getName() : null;
        }
        return null;
    }
}
