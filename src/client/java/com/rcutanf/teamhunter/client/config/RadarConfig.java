package com.rcutanf.teamhunter.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class RadarConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_FILE = FabricLoader.getInstance().getConfigDir().resolve("teamhunter_radar.json").toFile();
    private static RadarConfig INSTANCE;

    // 雷达配置
    private int radarSize = 60;
    private int radarX = 5;
    private int radarY = 5;
    private float nameScale = 0.4f;
    private boolean enabled = true;

    // 获取配置实例
    public static RadarConfig getInstance() {
        if (INSTANCE == null) {
            loadConfig();
        }
        return INSTANCE;
    }

    // 加载配置
    public static void loadConfig() {
        try {
            if (CONFIG_FILE.exists()) {
                try (FileReader reader = new FileReader(CONFIG_FILE)) {
                    INSTANCE = GSON.fromJson(reader, RadarConfig.class);
                }
            } else {
                INSTANCE = new RadarConfig();
                saveConfig();
            }
        } catch (IOException e) {
            INSTANCE = new RadarConfig();
        }
    }

    // 保存配置
    public static void saveConfig() {
        try {
            if (!CONFIG_FILE.exists()) {
                CONFIG_FILE.getParentFile().mkdirs();
                CONFIG_FILE.createNewFile();
            }

            try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
                GSON.toJson(INSTANCE, writer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Getter 和 Setter 方法
    public int getRadarSize() {
        return radarSize;
    }

    public void setRadarSize(int radarSize) {
        this.radarSize = Math.max(30, Math.min(200, radarSize));
    }

    public int getRadarX() {
        return radarX;
    }

    public void setRadarX(int radarX) {
        this.radarX = Math.max(0, radarX);
    }

    public int getRadarY() {
        return radarY;
    }

    public void setRadarY(int radarY) {
        this.radarY = Math.max(0, radarY);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public float getNameScale() {return nameScale;}

    public void setNameScale(float nameScale) {this.nameScale = Math.max(0.2f, Math.min(1.0f, nameScale));}
}