package com.rcutanf.teamhunter.client.guide_sys.impl.trigger;

import com.rcutanf.teamhunter.client.guide_sys.AbstractGuideSysTrigger;
import com.rcutanf.teamhunter.client.guide_sys.TriggerType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.Identifier;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

import java.util.HashMap;
import java.util.Map;

public class dimensionTrigger extends AbstractGuideSysTrigger {
    private Identifier lastDimension;

    public dimensionTrigger() {
        super(TriggerType.dimension); // 假如你有这个类型枚举
        registerTickListener();
    }

    // 注册客户端tick事件监听
    private void registerTickListener() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            ClientPlayerEntity player = MinecraftClient.getInstance().player;
            if (player == null) return;

            Identifier currentDimension = player.getWorld().getRegistryKey().getValue();
            if (lastDimension == null) {
                lastDimension = currentDimension;
            } else if (!lastDimension.equals(currentDimension)) {
                fire(createDimensionChangeEventData(lastDimension, currentDimension));
                lastDimension = currentDimension;
            }
        });
    }

    /** 构造和SurroundingBlockTrigger风格一致的事件数据 */
    private Map<String, Object> createDimensionChangeEventData(Identifier from, Identifier to) {
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("from", from); // Identifier，可以直接 cast 为字符串用
        eventData.put("to", to);
        eventData.put("timestamp", System.currentTimeMillis());
        return eventData;
    }
}