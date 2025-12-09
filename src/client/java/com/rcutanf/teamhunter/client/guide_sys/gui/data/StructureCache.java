package com.rcutanf.teamhunter.client.guide_sys.gui.data;

import com.rcutanf.teamhunter.NetWorking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class StructureCache {
    private static StructureCache instance;

    // 缓存：区块位置 -> 结构信息列表（统一使用NetWorking.StructureInfo）
    private final Map<ChunkPos, List<NetWorking.StructureInfo>> chunkStructureCache = new ConcurrentHashMap<>();

    // 正在请求的区块，避免重复请求
    private final Set<ChunkPos> pendingRequests = ConcurrentHashMap.newKeySet();

    // 请求的Future映射（修正类型）
    private final Map<ChunkPos, CompletableFuture<List<NetWorking.StructureInfo>>> requestFutures = new ConcurrentHashMap<>();

    // 缓存过期时间（毫秒）
    private static final long CACHE_EXPIRE_TIME = 5 * 60 * 1000; // 5分钟

    // 缓存时间戳
    private final Map<ChunkPos, Long> cacheTimestamps = new ConcurrentHashMap<>();

    private StructureCache() {}

    public static StructureCache getInstance() {
        if (instance == null) {
            instance = new StructureCache();
        }
        return instance;
    }

    /**
     * 获取指定位置的结构信息
     * @param playerPos 玩家位置
     * @return 包含该位置的结构名称集合
     */
    public CompletableFuture<Set<String>> getStructuresAtPosition(BlockPos playerPos) {
        ChunkPos chunkPos = new ChunkPos(playerPos);

        // 检查缓存
        if (isCacheValid(chunkPos)) {
            List<NetWorking.StructureInfo> structures = chunkStructureCache.get(chunkPos);
            Set<String> result = new HashSet<>();

            for (NetWorking.StructureInfo info : structures) {
                if (info.boundingBox().contains(playerPos)) {
                    result.add(info.name());
                }
            }

            return CompletableFuture.completedFuture(result);
        }

        // 从服务器获取数据
        return requestStructureData(chunkPos).thenApply(structures -> {
            Set<String> result = new HashSet<>();
            for (NetWorking.StructureInfo info : structures) {
                if (info.boundingBox().contains(playerPos)) {
                    result.add(info.name());
                }
            }
            return result;
        });
    }

    /**
     * 从服务器请求结构数据
     */
    private CompletableFuture<List<NetWorking.StructureInfo>> requestStructureData(ChunkPos chunkPos) {
        // 检查是否已经在请求中
        if (pendingRequests.contains(chunkPos)) {
            return requestFutures.getOrDefault(chunkPos, CompletableFuture.completedFuture(new ArrayList<>()));
        }

        pendingRequests.add(chunkPos);
        CompletableFuture<List<NetWorking.StructureInfo>> future = new CompletableFuture<>();
        requestFutures.put(chunkPos, future);

        // 发送请求到服务器
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null && client.world != null) {
            Identifier dimension = client.world.getRegistryKey().getValue();
            NetWorking.StructureDataRequestPacket requestPacket =
                new NetWorking.StructureDataRequestPacket(chunkPos, dimension);

            ClientPlayNetworking.send(requestPacket);
        }

        return future;
    }

    /**
     * 处理服务器响应
     */
    public void handleServerResponse(ChunkPos chunkPos, List<NetWorking.StructureInfo> structures) {
        // 直接使用，无需转换
        chunkStructureCache.put(chunkPos, structures);
        cacheTimestamps.put(chunkPos, System.currentTimeMillis());

        CompletableFuture<List<NetWorking.StructureInfo>> future = requestFutures.remove(chunkPos);
        if (future != null) {
            future.complete(structures);
        }

        pendingRequests.remove(chunkPos);
    }

    /**
     * 检查缓存是否有效
     */
    private boolean isCacheValid(ChunkPos chunkPos) {
        if (!chunkStructureCache.containsKey(chunkPos)) {
            return false;
        }

        Long timestamp = cacheTimestamps.get(chunkPos);
        if (timestamp == null) {
            return false;
        }

        return System.currentTimeMillis() - timestamp < CACHE_EXPIRE_TIME;
    }

    /**
     * 清理过期缓存
     */
    public void cleanupExpiredCache() {
        long currentTime = System.currentTimeMillis();
        List<ChunkPos> expiredChunks = new ArrayList<>();

        for (Map.Entry<ChunkPos, Long> entry : cacheTimestamps.entrySet()) {
            if (currentTime - entry.getValue() > CACHE_EXPIRE_TIME) {
                expiredChunks.add(entry.getKey());
            }
        }

        for (ChunkPos chunk : expiredChunks) {
            chunkStructureCache.remove(chunk);
            cacheTimestamps.remove(chunk);
        }
    }

    /**
     * 清空所有缓存
     */
    public void clearCache() {
        chunkStructureCache.clear();
        cacheTimestamps.clear();
        pendingRequests.clear();
        requestFutures.clear();
    }
}