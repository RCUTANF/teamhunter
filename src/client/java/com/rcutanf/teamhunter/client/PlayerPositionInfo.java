package com.rcutanf.teamhunter.client;

 import net.minecraft.client.MinecraftClient;
 import net.minecraft.scoreboard.Team;
 import net.minecraft.util.Identifier;
 import net.minecraft.util.math.BlockPos;

 /**
  * 记录玩家位置信息的内部类
  */
 public class PlayerPositionInfo {
     private final String playerName;
     private BlockPos position;
     private final String teamName;
     private Identifier dimension; // 添加维度字段

     public PlayerPositionInfo(String playerName, BlockPos position, String teamName, Identifier dimension) {
         this.playerName = playerName;
         this.position = position;
         this.teamName = teamName;
         this.dimension = dimension;
     }

     public PlayerPositionInfo(String playerName, BlockPos position, Identifier dimension) {
         this(playerName, position, getTeamNameForPlayer(playerName), dimension);
     }

     // 为了向后兼容，保留原有构造函数
     public PlayerPositionInfo(String playerName, BlockPos position) {
         this(playerName, position, getTeamNameForPlayer(playerName), null);
     }

     public void updatePosition(BlockPos newPosition) {
         this.position = newPosition;
     }

     public void updateDimension(Identifier newDimension) {
         this.dimension = newDimension;
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

     public Identifier getDimension() {
         return dimension;
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