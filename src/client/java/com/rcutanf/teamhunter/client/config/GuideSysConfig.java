package com.rcutanf.teamhunter.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class GuideSysConfig {
    private static GuideSysConfig instance;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_FILE = new File(FabricLoader.getInstance().getConfigDir().toFile(), "teamhunter_guidesys.json");

    // 配置项
    private boolean enabled = true;
    private int posX = MinecraftClient.getInstance().getWindow().getScaledWidth() - 148;
    private int posY = 8;
    private int width = 140;
    private int maxItems = 12;
    private float textScale = 1.0f;
    private boolean showBackground = true;
    private int backgroundColor = 0x90000000;
    private int textColor = 0xFFFFFFFF;

    // 提示文本配置
    private int hintSwitchInterval = 3000;
    private int hintScrollSpeed = 50;
    private boolean enableHintCarousel = true;

    public static GuideSysConfig getInstance() {
        if (instance == null) {
            loadConfig();
        }
        return instance;
    }

    public static void loadConfig() {
        if (CONFIG_FILE.exists()) {
            try (FileReader reader = new FileReader(CONFIG_FILE)) {
                instance = GSON.fromJson(reader, GuideSysConfig.class);
            } catch (IOException e) {
                e.printStackTrace();
                instance = new GuideSysConfig();
            }
        } else {
            instance = new GuideSysConfig();
        }
    }

    public static void saveConfig() {
        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            GSON.toJson(instance, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Getters and Setters
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public int getPosX() { return posX; }
    public void setPosX(int posX) { this.posX = posX; }

    public int getPosY() { return posY; }
    public void setPosY(int posY) { this.posY = posY; }

    public int getWidth() { return width; }
    public void setWidth(int width) { this.width = width; }

    public int getMaxItems() { return maxItems; }
    public void setMaxItems(int maxItems) { this.maxItems = maxItems; }

    public float getTextScale() { return textScale; }
    public void setTextScale(float textScale) { this.textScale = textScale; }

    public boolean isShowBackground() { return showBackground; }
    public void setShowBackground(boolean showBackground) { this.showBackground = showBackground; }

    public int getBackgroundColor() { return backgroundColor; }
    public void setBackgroundColor(int backgroundColor) { this.backgroundColor = backgroundColor; }

    public int getTextColor() { return textColor; }
    public void setTextColor(int textColor) { this.textColor = textColor; }

    public int getHintSwitchInterval() { return hintSwitchInterval; }
    public void setHintSwitchInterval(int hintSwitchInterval) { this.hintSwitchInterval = hintSwitchInterval; }

    public int getHintScrollSpeed() { return hintScrollSpeed; }
    public void setHintScrollSpeed(int hintScrollSpeed) { this.hintScrollSpeed = hintScrollSpeed; }

    public boolean isEnableHintCarousel() { return enableHintCarousel; }
    public void setEnableHintCarousel(boolean enableHintCarousel) { this.enableHintCarousel = enableHintCarousel; }
}