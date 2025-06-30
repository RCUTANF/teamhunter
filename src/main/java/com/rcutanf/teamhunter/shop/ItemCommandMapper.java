package com.rcutanf.teamhunter.shop;

import java.util.HashMap;
import java.util.Map;

public class ItemCommandMapper {
    private static final Map<String, String> ITEM_COMMANDS = new HashMap<>();

    static {
        // 基础物品
        ITEM_COMMANDS.put("钻石剑", "give @s diamond_sword");
        ITEM_COMMANDS.put("钻石头盔", "give @s diamond_helmet");
        ITEM_COMMANDS.put("钻石胸甲", "give @s diamond_chestplate");
        ITEM_COMMANDS.put("钻石护腿", "give @s diamond_leggings");
        ITEM_COMMANDS.put("钻石靴子", "give @s diamond_boots");

        // 特殊物品（带数量）
        ITEM_COMMANDS.put("附魔金苹果", "give @s enchanted_golden_apple 3");
        ITEM_COMMANDS.put("末影珍珠", "give @s ender_pearl 8");
        ITEM_COMMANDS.put("箭", "give @s arrow 64");
        ITEM_COMMANDS.put("金胡萝卜", "give @s golden_carrot 32");

        // 套装
        ITEM_COMMANDS.put("钻石套装", "give @s diamond_helmet\ngive @s diamond_chestplate\ngive @s diamond_leggings\ngive @s diamond_boots");
        ITEM_COMMANDS.put("铁套装", "give @s iron_helmet\ngive @s iron_chestplate\ngive @s iron_leggings\ngive @s iron_boots");
    }

    /**
     * 获取物品对应的命令
     * @param itemName 物品名称
     * @return 对应的命令字符串
     */
    public static String getCommand(String itemName) {
        // 如果找到预定义命令，则返回；否则尝试构造基本的give命令
        return ITEM_COMMANDS.getOrDefault(itemName, "give @s " + getMinecraftId(itemName));
    }

    /**
     * 将中文物品名转换为可能的Minecraft ID
     * 这只是一个简单示例，实际应用中可能需要更完整的映射
     */
    private static String getMinecraftId(String chineseName) {
        // 这里可以添加更多的中文名到Minecraft ID的映射
        Map<String, String> nameToId = new HashMap<>();
        nameToId.put("钻石剑", "diamond_sword");
        nameToId.put("钻石头盔", "diamond_helmet");

        return nameToId.getOrDefault(chineseName, chineseName.toLowerCase().replace(" ", "_"));
    }
}