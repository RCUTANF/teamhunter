package com.rcutanf.teamhunter;

    import com.mojang.authlib.GameProfile;
    import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
    import net.minecraft.server.MinecraftServer;
    import net.minecraft.server.network.ServerPlayerEntity;
    import net.minecraft.server.world.ServerWorld;
    import net.minecraft.util.Identifier;
    import net.minecraft.util.math.BlockPos;

    import java.util.HashMap;
    import java.util.Map;
    import java.util.UUID;

    public class PlayerPositionTracker {
        private final MinecraftServer server;
        private final Map<UUID, Map<Identifier, BlockPos>> lastPositionsByDimension = new HashMap<>();
        private final Map<UUID, Identifier> playerDimensions = new HashMap<>();
        private int tickCounter = 0;
        private static final int TICKS_PER_SECOND = 20; // Minecraft每秒20个tick

        public PlayerPositionTracker(MinecraftServer server) {
            this.server = server;
        }

        public void tick() {
            tickCounter++;

            // 每秒执行一次检查
            if (tickCounter >= TICKS_PER_SECOND) {
                tickCounter = 0;
                checkPlayerPositions();
            }
        }

        private void checkPlayerPositions() {
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                UUID playerId = player.getUuid();
                BlockPos currentPos = player.getBlockPos();

                // 获取玩家当前维度
                ServerWorld world = player.getEntityWorld();
                Identifier currentDimension = world.getRegistryKey().getValue();

                // 获取玩家上一次的维度
                Identifier lastDimension = playerDimensions.get(playerId);

                // 维度变化检测
                if (lastDimension != null && !lastDimension.equals(currentDimension)) {
                    // 玩家切换了维度，记录维度变化
                    playerDimensions.put(playerId, currentDimension);
                    // 向玩家发送新维度中所有其他玩家的位置信息
                    sendDimensionPlayerPositionsToPlayer(player, currentDimension);
                } else if (lastDimension == null) {
                    // 首次记录
                    playerDimensions.put(playerId, currentDimension);
                }

                // 获取玩家在当前维度的上一个位置
                Map<Identifier, BlockPos> dimensionPositions = lastPositionsByDimension
                    .computeIfAbsent(playerId, k -> new HashMap<>());
                BlockPos lastPos = dimensionPositions.get(currentDimension);

                // 检查位置是否变化
                if (lastPos == null || !lastPos.equals(currentPos)) {
                    // 更新位置并发送通知
                    dimensionPositions.put(currentDimension, currentPos);
                    recordPositionChange(player, currentPos, currentDimension);
                }
            }
        }

        private void recordPositionChange(ServerPlayerEntity player, BlockPos newPos, Identifier dimension) {
            UUID playerId = player.getUuid();
            String playerName = player.getName().getString();

            // 创建包含维度信息的数据包
            NetWorking.PlayerPositionUpdatePacket packet = new NetWorking.PlayerPositionUpdatePacket(
                playerId, playerName, newPos, dimension);

            // 只向同维度的玩家发送位置更新
            for (ServerPlayerEntity serverPlayer : server.getPlayerManager().getPlayerList()) {
                Identifier playerDimension = serverPlayer.getEntityWorld().getRegistryKey().getValue();
                if (playerDimension.equals(dimension)) {
                    ServerPlayNetworking.send(serverPlayer, packet);
                }
            }
        }

        /**
         * 当玩家登录时调用此方法，将所有玩家在各维度的最后位置发送给新登录的玩家
         *
         * @param newPlayer 新登录的玩家
         */
        public void sendAllPlayerPositionsToNewPlayer(ServerPlayerEntity newPlayer) {
            Identifier newPlayerDimension = newPlayer.getEntityWorld().getRegistryKey().getValue();

            // 发送每个玩家在新玩家当前维度的最后位置
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                UUID playerId = player.getUuid();

                // 如果是同一个玩家则跳过
                if (playerId.equals(newPlayer.getUuid())) continue;

                // 获取玩家在此维度的最后位置
                Map<Identifier, BlockPos> positions = lastPositionsByDimension.get(playerId);
                if (positions != null && positions.containsKey(newPlayerDimension)) {
                    BlockPos lastPos = positions.get(newPlayerDimension);
                    String playerName = player.getName().getString();

                    NetWorking.PlayerPositionUpdatePacket packet = new NetWorking.PlayerPositionUpdatePacket(
                        playerId, playerName, lastPos, newPlayerDimension);

                    ServerPlayNetworking.send(newPlayer, packet);
                }
            }
        }

        /**
         * 当玩家切换维度时，向该玩家发送指定维度中所有其他玩家的位置信息
         *
         * @param player 切换维度的玩家
         * @param dimension 玩家当前的新维度
         */
        private void sendDimensionPlayerPositionsToPlayer(ServerPlayerEntity player, Identifier dimension) {
            UUID playerId = player.getUuid();

            // 遍历所有玩家的位置记录
            for (Map.Entry<UUID, Map<Identifier, BlockPos>> entry : lastPositionsByDimension.entrySet()) {
                UUID otherPlayerId = entry.getKey();

                // 跳过玩家自己
                if (otherPlayerId.equals(playerId)) continue;

                // 获取该玩家在指定维度的位置
                Map<Identifier, BlockPos> positions = entry.getValue();
                if (positions != null && positions.containsKey(dimension)) {
                    BlockPos lastPos = positions.get(dimension);

                    // 获取玩家名称
                    ServerPlayerEntity otherPlayer = server.getPlayerManager().getPlayer(otherPlayerId);
                    String playerName = otherPlayer != null ?
                            otherPlayer.getGameProfile().name() :
                            "Unknown Player";

                    // 创建并发送位置更新包
                    NetWorking.PlayerPositionUpdatePacket packet = new NetWorking.PlayerPositionUpdatePacket(
                            otherPlayerId, playerName, lastPos, dimension);

                    ServerPlayNetworking.send(player, packet);
                }
            }
        }
    }