package com.rcutanf.teamhunter.client.guide_sys.impl.trigger;

import com.rcutanf.teamhunter.client.guide_sys.TriggerType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class BiomeTrigger extends ScanCore {
    private static BiomeTrigger instance;
    private String currentBiomeName = null;

    public BiomeTrigger() {
        super(TriggerType.biome, 20); // 每秒检查一次
    }

    public static BiomeTrigger getInstance() {
        if (instance == null) {
            instance = new BiomeTrigger();
        }
        return instance;
    }

    @Override
    protected void performScan() {
        checkBiomeChange();
    }

    private void checkBiomeChange() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) {
            return;
        }

        BlockPos playerPos = client.player.getBlockPos();
        RegistryEntry<Biome> currentBiome = client.world.getBiome(playerPos);

        String biomeName = currentBiome.getKey()
                .map(key -> key.getValue().toString())
                .orElse(null);

        updateBiomeStateAndFireEvent(biomeName, playerPos);
    }

    private void updateBiomeStateAndFireEvent(String biomeName, BlockPos playerPos) {
        if (!Objects.equals(currentBiomeName, biomeName)) {
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("biomeId", biomeName);
            eventData.put("playerPos", playerPos);
            fire(eventData);
        }
        currentBiomeName = biomeName;
    }

    @Override
    protected void onDisconnect() {
        currentBiomeName = null;
    }

    @Override
    protected void onDisable() {
        currentBiomeName = null;
    }

    @Override
    protected double getMovementThreshold() {
        return 16.0; // 玩家移动4格时触发扫描
    }
}