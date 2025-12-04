package com.rcutanf.teamhunter.client.guide_sys.gui.data;

import net.minecraft.advancement.AdvancementEntry;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class AdvancementDataCache {
    private static AdvancementDataCache instance;
    private final Map<Identifier, AdvancementEntry> cache = new HashMap<>();
    private CompletableFuture<Void> currentRequest;

    private AdvancementDataCache() {}

    public static synchronized AdvancementDataCache getInstance() {
        if (instance == null) {
            instance = new AdvancementDataCache();
        }
        return instance;
    }

    // 新增方法：直接缓存 Advancement 对象
    public void cacheAdvancement(Identifier id, AdvancementEntry advancementEntry) {
        cache.put(id, advancementEntry);
    }

    // 获取缓存的 Advancement
    public AdvancementEntry getCachedAdvancementEntry(Identifier id) {
        return cache.get(id);
    }

    // 检查是否有缓存数据
    public boolean hasCachedAdvancement(Identifier id) {
        return cache.containsKey(id);
    }

    public void clearCache() {
        cache.clear();
        if (currentRequest != null) {
            currentRequest.cancel(true);
        }
    }

    public void setCurrentRequest(CompletableFuture<Void> request) {
        this.currentRequest = request;
    }

    public  CompletableFuture<Void> getCurrentRequest() {return currentRequest;}

    // 为了向后兼容，保留旧的方法但标记为废弃
    @Deprecated
    public void cacheAdvancementData(Identifier id, net.minecraft.item.ItemStack icon, net.minecraft.text.Text title, net.minecraft.text.Text description, boolean exists) {
        // 这个方法现在不使用了，因为我们直接缓存 Advancement 对象
    }
}