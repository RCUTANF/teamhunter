package com.rcutanf.teamhunter.client.guide_sys.impl.trigger;

import com.rcutanf.teamhunter.client.guide_sys.AbstractGuideSysTrigger;
import com.rcutanf.teamhunter.client.guide_sys.TriggerType;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.world.ClientWorld;

import java.util.HashMap;
import java.util.Map;

public class WeatherTrigger extends AbstractGuideSysTrigger {
    private static WeatherTrigger instance;
    private WeatherState lastWeatherState;

    public WeatherTrigger() {
        super(TriggerType.weather);
        instance = this;
        registerConnectionListener();
    }

    public static WeatherTrigger getInstance() {
        return instance;
    }

    private void registerConnectionListener() {
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            this.lastWeatherState = null;
        });
    }

    // 由Mixin调用的天气变化处理方法
    public void onWeatherChanged(ClientWorld world) {
        WeatherState currentWeatherState = getCurrentWeatherState(world);

        if (lastWeatherState == null) {
            lastWeatherState = currentWeatherState;
        } else if (!lastWeatherState.equals(currentWeatherState)) {
            fire(createWeatherChangeEventData(lastWeatherState, currentWeatherState));
            lastWeatherState = currentWeatherState;
        }
    }

    private WeatherState getCurrentWeatherState(ClientWorld world) {
        if (world.isThundering()) {
            return WeatherState.THUNDERING;
        } else if (world.isRaining()) {
            return WeatherState.RAINING;
        } else {
            return WeatherState.CLEAR;
        }
    }

    public Map<String, Object> createWeatherChangeEventData(WeatherState from, WeatherState to) {
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("oldWeather", from.name());
        eventData.put("newWeather", to.name());
        eventData.put("timestamp", System.currentTimeMillis());
        return eventData;
    }

    public enum WeatherState {
        CLEAR,
        RAINING,
        THUNDERING
    }
}