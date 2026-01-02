package com.rcutanf.teamhunter.client.radar.data;

    import com.rcutanf.teamhunter.client.PlayerPositionInfo;
    import net.minecraft.client.MinecraftClient;
    import net.minecraft.entity.player.PlayerEntity;
    import net.minecraft.util.Identifier;
    import net.minecraft.util.math.BlockPos;

    import java.util.HashMap;
    import java.util.Map;
    import java.util.UUID;

    /**
     * 玩家位置信息管理器
     * 负责管理和追踪所有玩家的位置、可见性和维度信息
     */
    public class PlayerPositionManager {

        private static PlayerPositionManager instance;

        /** 存储其他玩家位置信息的映射表 */
        private final Map<UUID, PlayerPositionInfo> playerPositions = new HashMap<>();

        private PlayerPositionManager() {}

        public static PlayerPositionManager getInstance() {
            if (instance == null) {
                instance = new PlayerPositionManager();
            }
            return instance;
        }

        /**
         * 更新玩家位置信息
         */
        public void updatePlayerPosition(UUID playerId, String playerName, BlockPos position, Identifier dimension) {
            if (playerPositions.containsKey(playerId)) {
                PlayerPositionInfo info = playerPositions.get(playerId);
                info.updatePosition(position);
                info.updateDimension(dimension);
            } else {
                playerPositions.put(playerId, new PlayerPositionInfo(playerName, position, dimension));
            }
        }

        /**
         * 更新玩家可见性
         */
        public void updatePlayerVisibility(UUID playerId, String playerName, boolean isVisible) {
            if (playerPositions.containsKey(playerId)) {
                PlayerPositionInfo info = playerPositions.get(playerId);
                info.updateVisible(isVisible);
            }
            // TODO: 如果玩家尚未在位置映射中，我们将等待位置更新数据包
        }

        /**
         * 获取指定玩家的位置信息
         */
        public PlayerPositionInfo getPlayerInfo(UUID playerId) {
            return playerPositions.get(playerId);
        }

        /**
         * 获取所有跟踪的玩家位置信息
         */
        public Map<UUID, PlayerPositionInfo> getAllPlayerPositions() {
            return new HashMap<>(playerPositions);
        }

        /**
         * 获取合并后的玩家位置信息（本地实时数据优先，远程缓存数据补充）
         * 用于为雷达提供最准确的位置数据
         */
        public Map<UUID, PlayerPositionInfo> getMergedPlayerPositions() {
            Map<UUID, PlayerPositionInfo> mergedPositions = new HashMap<>();

            // 首先添加缓存的远程玩家位置
            mergedPositions.putAll(playerPositions);

            // 获取本地客户端可见的玩家并更新位置信息
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.world != null && client.player != null) {
                for (PlayerEntity player : client.world.getPlayers()) {
                    if (!player.equals(client.player)) { // 排除自己
                        UUID playerId = player.getUuid();
                        BlockPos currentPos = player.getBlockPos();
                        Identifier currentDimension = client.world.getRegistryKey().getValue();

                        if (mergedPositions.containsKey(playerId)) {
                            // 更新现有玩家的实时位置
                            PlayerPositionInfo existingInfo = mergedPositions.get(playerId);
                            PlayerPositionInfo updatedInfo = new PlayerPositionInfo(
                                existingInfo.getPlayerName(),
                                currentPos,
                                currentDimension
                            );
                            updatedInfo.updateVisible(existingInfo.isVisible());
                            mergedPositions.put(playerId, updatedInfo);
                        } else {
                            // 添加新发现的本地玩家
                            PlayerPositionInfo localInfo = new PlayerPositionInfo(
                                player.getName().getString(),
                                currentPos,
                                currentDimension
                            );
                            localInfo.updateVisible(true); // 本地可见的玩家默认可见
                            mergedPositions.put(playerId, localInfo);
                        }
                    }
                }
            }

            return mergedPositions;
        }

        /**
         * 移除指定玩家信息
         */
        public void removePlayer(UUID playerId) {
            playerPositions.remove(playerId);
        }

        /**
         * 清空所有玩家位置信息
         */
        public void clearAllPlayers() {
            playerPositions.clear();
        }

        /**
         * 检查是否包含指定玩家
         */
        public boolean containsPlayer(UUID playerId) {
            return playerPositions.containsKey(playerId);
        }

        /**
         * 获取当前追踪的玩家数量
         */
        public int getPlayerCount() {
            return playerPositions.size();
        }
    }