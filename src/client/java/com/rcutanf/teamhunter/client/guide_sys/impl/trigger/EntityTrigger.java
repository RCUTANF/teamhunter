package com.rcutanf.teamhunter.client.guide_sys.impl.trigger;

import com.rcutanf.teamhunter.client.guide_sys.TriggerType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

import java.util.*;

public class EntityTrigger extends ScanCore {
    private static EntityTrigger instance;
    // 去重用的唯一键集合：普通实体 -> typeId；猫/狼 -> typeId|variantId
    private Set<String> detectedEntityKeys = new HashSet<>();
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

        List<Entity> allEntities = client.world.getOtherEntities(client.player, scanBox);

        // 当前帧的唯一键集合与去重后的实体列表
        Set<String> currentKeys = new HashSet<>();
        List<Entity> uniqueEntities = new ArrayList<>();

        for (Entity e : allEntities) {
            String key = buildEntityKey(e);
            if (currentKeys.add(key)) {
                uniqueEntities.add(e);
            }
        }

        if (!detectedEntityKeys.equals(currentKeys)) {
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("entities", Collections.unmodifiableList(uniqueEntities));
            eventData.put("newEntities", getNewKeys(currentKeys));
            eventData.put("removedEntities", getRemovedKeys(currentKeys));
            eventData.put("playerPos", playerPos);

            fire(eventData);
            detectedEntityKeys = currentKeys;
        }
    }

    // 生成去重键：普通实体为 typeId；猫/狼追加变种ID
    private String buildEntityKey(Entity entity) {
        String typeId = entity.getType().getRegistryEntry().getIdAsString();

        // 猫变种
        var catVariant = entity.get(DataComponentTypes.CAT_VARIANT);
        if (catVariant != null) {
            String variantId = safeVariantId(catVariant.getIdAsString());
            return typeId + "|" + variantId;
        }

        // 狼变种
        var wolfVariant = entity.get(DataComponentTypes.WOLF_VARIANT);
        if (wolfVariant != null) {
            String variantId = safeVariantId(wolfVariant.getIdAsString());
            return typeId + "|" + variantId;
        }

        // 其他实体只看类型
        return typeId;
    }

    private String safeVariantId(String id) {
        return id == null ? "unknown" : id;
    }

    private Set<String> getNewKeys(Set<String> current) {
        Set<String> s = new HashSet<>(current);
        s.removeAll(detectedEntityKeys);
        return s;
    }

    private Set<String> getRemovedKeys(Set<String> current) {
        Set<String> s = new HashSet<>(detectedEntityKeys);
        s.removeAll(current);
        return s;
    }

    @Override
    protected void onDisconnect() {
        detectedEntityKeys.clear();
    }

    @Override
    protected void onDisable() {
        detectedEntityKeys.clear();
    }

    @Override
    protected double getMovementThreshold() {
        return 16.0;
    }
}