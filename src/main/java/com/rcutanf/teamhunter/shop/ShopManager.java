package com.rcutanf.teamhunter.shop;

import com.rcutanf.teamhunter.CommandExecutor;
import com.rcutanf.teamhunter.NetWorking;
import com.rcutanf.teamhunter.Phase;
import com.rcutanf.teamhunter.Teamhunter;
import com.rcutanf.teamhunter.advancement.AdvancementListener;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ShopManager {
    private static final Map<String, ShopItem> shopItems = new HashMap<>();

    /**
     * 商店物品类
     */
    public static class ShopItem {
        private final String name;       // 物品名称
        private final String command;    // 给予物品的命令
        private final int price;         // 价格(分数)
        private final String itemId;     // 物品的完整标识符

        public ShopItem(String name, String command, int price, String itemId) {
            this.name = name;
            this.command = command;
            this.price = price;
            this.itemId = itemId;
        }

        public String getName() { return name; }
        public String getCommand() { return command; }
        public int getPrice() { return price; }
        public String getItemId() { return itemId; }
    }

    /**
     * 加载商店物品
     */
    public static void loadItems() {
        shopItems.clear();
        List<ShopConfig.ShopItemConfig> configItems = ShopConfig.loadShopItems();

        for (ShopConfig.ShopItemConfig item : configItems) {
            String itemId = item.getName(); // 这是完整的物品ID，如 minecraft:diamond_sword
            int price = item.getPrice();
            String command = "give @s " + itemId;
            // 使用格式化的名称作为显示名，但保留原始itemId
            addItem(formatItemName(itemId), command, price, itemId);
        }

        System.out.println("已加载 " + shopItems.size() + " 个商店物品");
    }

    /**
     * 格式化物品名称显示
     */
    private static String formatItemName(String itemId) {
        // 将物品ID转换为更友好的显示形式
        // 例如：diamond_sword -> 钻石剑
        // 这只是简单示例，可以根据需要扩展
        String displayName = itemId.replace("_", " ");
        // 首字母大写
        String[] words = displayName.split(" ");
        StringBuilder formatted = new StringBuilder();
        for (String word : words) {
            if (word.length() > 0) {
                formatted.append(Character.toUpperCase(word.charAt(0)))
                        .append(word.substring(1))
                        .append(" ");
            }
        }
        return formatted.toString().trim();
    }

    /**
     * 添加商店物品
     */
    public static void addItem(String name, String command, int price, String itemId) {
        shopItems.put(name.toLowerCase(), new ShopItem(name, command, price, itemId));
    }

    /**
     * 获取所有商店物品
     */
    public static List<ShopItem> getAllItems() {
        return new ArrayList<>(shopItems.values());
    }

    /**
     * 获取指定物品
     */
    public static ShopItem getItem(String name) {
        return shopItems.get(name.toLowerCase());
    }

    /**
     * 重载商店配置
     */
    public static boolean reloadConfig() {
        try {
            loadItems();
            return true;
        } catch (Exception e) {
            System.err.println("重载商店配置失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 购买物品
     * @return 是否购买成功
     */
    public static boolean purchaseItem(ServerPlayerEntity player, String itemName) {
        // 检查比赛是否在进行中
        if (Teamhunter.phaseManager.Phase() != Phase.MATCH) {
            player.sendMessage(Text.of("§c商店只在比赛阶段可用！"), false);
            return false;
        }

        // 获取物品
        ShopItem item = getItem(itemName.toLowerCase());
        if (item == null) {
            player.sendMessage(Text.of("§c该物品不存在！"), false);
            return false;
        }

        // 获取玩家队伍
        String teamName = player.getScoreboardTeam() != null
                ? player.getScoreboardTeam().getName() : null;

        if (teamName == null || (!teamName.equals("hunters") && !teamName.equals("runners"))) {
            player.sendMessage(Text.of("§c你不属于任何可用团队！"), false);
            return false;
        }

        // 检查团队分数是否足够
        int teamScore = teamName.equals("hunters")
                ? AdvancementListener.getHuntersScore()
                : AdvancementListener.getRunnersScore();

        if (teamScore < item.getPrice()) {
            player.sendMessage(Text.of("§c团队分数不足！需要 " + item.getPrice() + " 分，当前只有 " + teamScore + " 分"), false);
            return false;
        }

        // 扣除团队分数
        MinecraftServer server = player.getServer();
        ServerWorld world = player.getServerWorld();
        int newScore = AdvancementListener.reduceTeamScore(
                teamName,
                item.getPrice(),
                world,
                player.getName().getString() + " 购买了 " + item.getName()
        );

        // 给予物品
        String[] commands = item.getCommand().split("\n");
        for (String cmd : commands) {
            CommandExecutor.executeCommand(server, "/execute as " + player.getName().getString() + " run " + cmd);
        }

        // 发送成功消息
        player.sendMessage(Text.of("§a成功购买 " + item.getName() + "，扣除 " + item.getPrice() + " 分！"), false);

        // 通知团队
        String teamMessage = "§e" + player.getName().getString() + " 购买了 " + item.getName() + "，消耗团队 " + item.getPrice() + " 分！";
        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
            if (p.getScoreboardTeam() != null && p.getScoreboardTeam().getName().equals(teamName) && p != player) {
                p.sendMessage(Text.of(teamMessage), false);
            }
        }

        return true;
    }

    /**
     * 获取所有商店物品数据用于网络传输
     * @return 包含所有物品ID和价格的列表
     */
    public static List<NetWorking.ShopItemData> getAllItemsForNetwork() {
        List<NetWorking.ShopItemData> result = new ArrayList<>();
        for (ShopItem item : getAllItems()) {
            result.add(new NetWorking.ShopItemData(item.getItemId(), item.getPrice()));
        }
        return result;
    }
}