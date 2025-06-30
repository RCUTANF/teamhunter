package com.rcutanf.teamhunter.shop;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.rcutanf.teamhunter.Teamhunter;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ShopConfig {
    private static final Path CONFIG_DIR = FabricLoader.getInstance().getConfigDir();
    private static final String CONFIG_FILENAME = "teamhunter_shop.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static File configFile;

    static {
        configFile = new File(CONFIG_DIR.toFile(), CONFIG_FILENAME);
    }

    public static class ShopItemConfig {
        private String name;  // 物品ID，如 diamond_sword
        private int price;    // 价格

        public ShopItemConfig(String name, int price) {
            this.name = name;
            this.price = price;
        }

        public String getName() { return name; }
        public int getPrice() { return price; }
    }

    public static List<ShopItemConfig> loadShopItems() {
        if (!configFile.exists()) {
            createDefaultConfig();
        }

        try (FileReader reader = new FileReader(configFile)) {
            Type type = new TypeToken<List<ShopItemConfig>>(){}.getType();
            List<ShopItemConfig> items = GSON.fromJson(reader, type);
            return items != null ? items : new ArrayList<>();
        } catch (IOException e) {
            System.err.println("无法加载商店配置文件: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public static void saveShopItems(List<ShopItemConfig> items) {
        try (FileWriter writer = new FileWriter(configFile)) {
            GSON.toJson(items, writer);
        } catch (IOException e) {
            System.err.println("无法保存商店配置文件: " + e.getMessage());
        }
    }

    private static void createDefaultConfig() {
        List<ShopItemConfig> defaultItems = new ArrayList<>();
        defaultItems.add(new ShopItemConfig("diamond_sword", 50));
        defaultItems.add(new ShopItemConfig("diamond_helmet", 40));
        defaultItems.add(new ShopItemConfig("diamond_chestplate", 50));
        defaultItems.add(new ShopItemConfig("diamond_leggings", 45));
        defaultItems.add(new ShopItemConfig("diamond_boots", 35));
        defaultItems.add(new ShopItemConfig("enchanted_golden_apple", 100));
        defaultItems.add(new ShopItemConfig("ender_pearl", 40));
        defaultItems.add(new ShopItemConfig("arrow", 25));
        defaultItems.add(new ShopItemConfig("golden_carrot", 20));
        defaultItems.add(new ShopItemConfig("iron_axe", 45));

        saveShopItems(defaultItems);
    }
}