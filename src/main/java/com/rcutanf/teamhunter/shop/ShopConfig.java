package com.rcutanf.teamhunter.shop;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
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
import java.util.Map;

public class ShopConfig {
    private static final Path CONFIG_DIR = FabricLoader.getInstance().getConfigDir();
    private static final String CONFIG_FOLDER = "teamhunter";
    private static final String CONFIG_FILENAME = "teamhunter_shop.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static File configFile;

    static {
        File teamhunterDir = new File(CONFIG_DIR.toFile(), CONFIG_FOLDER);
        if (!teamhunterDir.exists()) {
            teamhunterDir.mkdirs();
        }
        configFile = new File(teamhunterDir, CONFIG_FILENAME);
    }

    public static class ShopItemConfig {
        private String id;       // 物品基础ID
        private String name;     // 显示名称
        private int price;       // 价格
        private JsonObject components; // 组件数据 (JSON对象格式)

        public ShopItemConfig() {} // GSON反序列化需要无参构造

        public ShopItemConfig(String id,String name, int price, JsonObject components) {
            this.id = id;
            this.name = name;
            this.price = price;
            this.components = components;
        }

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public int getPrice() { return price; }
        public void setPrice(int price) { this.price = price; }
        public JsonObject getComponents() { return components; }
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
        defaultItems.add(new ShopItemConfig("minecraft:diamond_sword", "钻石剑", 50, null));
        defaultItems.add(new ShopItemConfig("minecraft:diamond_helmet", "钻石头盔", 40, null));
        defaultItems.add(new ShopItemConfig("minecraft:diamond_chestplate", "钻石胸甲", 50, null));
        defaultItems.add(new ShopItemConfig("minecraft:diamond_leggings", "钻石护腿", 45, null));
        defaultItems.add(new ShopItemConfig("minecraft:diamond_boots", "钻石靴子", 35, null));
        defaultItems.add(new ShopItemConfig("minecraft:enchanted_golden_apple", "附魔金苹果", 100, null));
        defaultItems.add(new ShopItemConfig("minecraft:ender_pearl", "末影珍珠", 40, null));
        defaultItems.add(new ShopItemConfig("minecraft:arrow", "箭", 25, null));
        defaultItems.add(new ShopItemConfig("minecraft:golden_carrot", "金胡萝卜", 20, null));
        defaultItems.add(new ShopItemConfig("minecraft:iron_axe", "铁斧", 45, null));

        // 添加带有NBT数据的物品示例
        defaultItems.add(new ShopItemConfig("minecraft:golden_sword", "抢夺III金剑", 29,
                "{Enchantments:[{id:\"minecraft:looting\",lvl:3}]}"));
        defaultItems.add(new ShopItemConfig("minecraft:diamond_pickaxe", "效率V钻镐", 60,
                "{Enchantments:[{id:\"minecraft:efficiency\",lvl:5}]}"));

        saveShopItems(defaultItems);
    }
}