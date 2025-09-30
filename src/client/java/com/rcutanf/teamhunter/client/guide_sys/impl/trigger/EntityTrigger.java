package com.rcutanf.teamhunter.client.guide_sys.impl.trigger;

import com.rcutanf.teamhunter.client.guide_sys.TriggerType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

import java.util.*;

public class EntityTrigger extends ScanCore {
    private static EntityTrigger instance;
    private Set<String> detectedEntityIds = new HashSet<>();
    private int scanRadius = 10;

    public EntityTrigger() {
        super(TriggerType.entity, 20);
    }

    public static EntityTrigger getInstance() {
        if (instance == null) {
            instance = new EntityTrigger();
        }
        return instance;
    }

    public void setScanRadius(int radius) {
        this.scanRadius = Math.max(1, Math.min(radius, 64));
    }

    @Override
    protected void performScan() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) {
            return;
        }

        BlockPos playerPos = client.player.getBlockPos();
        Box scanBox = new Box(
                playerPos.add(-scanRadius, -scanRadius, -scanRadius).toCenterPos(),
                playerPos.add(scanRadius, scanRadius, scanRadius).toCenterPos()
        );

        Set<String> currentEntityIds = new HashSet<>();
        List<Entity> entities = client.world.getOtherEntities(client.player, scanBox);

        for (Entity entity : entities) {
            currentEntityIds.add(entity.getType().getRegistryEntry().getIdAsString());
        }

        if (!detectedEntityIds.equals(currentEntityIds)) {
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("entityIds", new HashSet<>(currentEntityIds));
            eventData.put("newEntities", getNewEntities(currentEntityIds));
            eventData.put("removedEntities", getRemovedEntities(currentEntityIds));
            eventData.put("playerPos", playerPos);

            fire(eventData);
            detectedEntityIds = currentEntityIds;
        }
    }

    private Set<String> getNewEntities(Set<String> currentIds) {
        Set<String> newEntities = new HashSet<>(currentIds);
        newEntities.removeAll(detectedEntityIds);
        return newEntities;
    }

    private Set<String> getRemovedEntities(Set<String> currentIds) {
        Set<String> removedEntities = new HashSet<>(detectedEntityIds);
        removedEntities.removeAll(currentIds);
        return removedEntities;
    }

    @Override
    protected void onDisconnect() {
        detectedEntityIds.clear();
    }

    @Override
    protected void onDisable() {
        detectedEntityIds.clear();
    }

    @Override
    protected double getMovementThreshold() {
        return 16.0;
    }
}