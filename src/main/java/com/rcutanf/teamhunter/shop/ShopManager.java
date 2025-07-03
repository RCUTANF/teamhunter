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
        private final String nbt;        // 物品的NBT数据（如果有的话）

        public ShopItem(String name, String command, int price, String itemId, String nbt) {
            this.name = name;
            this.command = command;
            this.price = price;
            this.itemId = itemId;
            this.nbt = nbt;
        }

        public String getName() { return name; }
        public String getCommand() { return command; }
        public int getPrice() { return price; }
        public String getItemId() { return itemId; }
        public String getNbt() { return nbt; }
    }

    /**
     * 加载商店物品
     */
    public static void loadItems() {
        shopItems.clear();
        List<ShopConfig.ShopItemConfig> configItems = ShopConfig.loadShopItems();

        for (ShopConfig.ShopItemConfig item : configItems) {
            String itemId = item.getId(); // 这是完整的物品ID，如 minecraft:diamond_sword
            String itemName = item.getName();
            int price = item.getPrice();
            String command = "give @s " + itemId;
            String nbt = item.getNbt();
            // 使用格式化的名称作为显示名，但保留原始itemId
            addItem(itemName, command, price, itemId, nbt);
        }

        System.out.println("已加载 " + shopItems.size() + " 个商店物品");
    }

    /**
     * 添加商店物品
     */
    public static void addItem(String name, String command, int price, String itemId, String nbt) {
        shopItems.put(name, new ShopItem(name, command, price, itemId, nbt));
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
        return shopItems.get(name);
    }

    /**
     * 通过物品ID获取商店物品
     * @param itemId 物品ID，如 "iron_axe" 或 "minecraft:iron_axe"
     * @return 找到的商店物品，如果不存在则返回null
     */
    private static ShopItem getItemById(String itemId) {
        for (ShopItem item : shopItems.values()) {
            // 处理以下情况：
            // 1. 完全匹配
            // 2. 输入不带minecraft:前缀，但存储的ID带前缀
            // 3. 输入带minecraft:前缀，但存储的ID不带前缀
            if (item.getItemId().equalsIgnoreCase(itemId) ||
                item.getItemId().equalsIgnoreCase("minecraft:" + itemId) ||
                (itemId.startsWith("minecraft:") &&
                 item.getItemId().equalsIgnoreCase(itemId.substring("minecraft:".length())))) {
                return item;
            }
        }
        return null;
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
     * 通过物品ID购买物品
     * @return 是否购买成功
     */
    public static boolean purchaseItemById(ServerPlayerEntity player, String itemId) {
        // 检查比赛是否在进行中
        if (Teamhunter.phaseManager.Phase() != Phase.MATCH) {
            player.sendMessage(Text.of("§c商店只在比赛阶段可用！"), false);
            return false;
        }

        // 获取物品
        ShopItem item = getItemById(itemId);
        if (item == null) {
            player.sendMessage(Text.of("§c找不到ID为 " + itemId + " 的物品！"), false);
            return false;
        }

        return processPurchase(player, item);
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

        return processPurchase(player, item);
    }

    /**
     * 处理购买流程
     */
    private static boolean processPurchase(ServerPlayerEntity player, ShopItem item) {
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

        // 给予物品（支持NBT数据）
        //TODO：可以看到不应该多出一个数据结构command，应该是当初混乱了，先这样用着后面再修
        String playerName = player.getName().getString();
        if (item.getNbt() != null && !item.getNbt().isEmpty()) {
            // 如果有NBT数据，创建包含NBT的give命令
            String giveCommand = "/give " + playerName + " " + item.getItemId() + item.getNbt();
            CommandExecutor.executeCommand(server, giveCommand);
        } else {
            // 如果没有NBT数据，使用原有命令逻辑
            String[] commands = item.getCommand().split("\n");
            for (String cmd : commands) {
                CommandExecutor.executeCommand(server, "/execute as " + playerName + " run " + cmd);
            }
        }

        // 发送成功消息
        player.sendMessage(Text.of("§a成功购买 " + item.getName() + "，扣除 " + item.getPrice() + " 分！"), false);

        // 通知团队
        String teamMessage = "§e" + playerName + " 购买了 " + item.getName() + "，消耗团队 " + item.getPrice() + " 分！";
        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
            if (p.getScoreboardTeam() != null && p.getScoreboardTeam().getName().equals(teamName) && p != player) {
                p.sendMessage(Text.of(teamMessage), false);
            }
        }

        return true;
    }

    /**
     * 获取所有商店物品数据用于网络传输
     * @return 包含所有物品完整信息的列表
     */
    public static List<NetWorking.ShopItemData> getAllItemsForNetwork() {
        List<NetWorking.ShopItemData> result = new ArrayList<>();
        for (ShopItem item : getAllItems()) {
            result.add(new NetWorking.ShopItemData(
                item.getItemId(),   // 物品ID
                item.getName(),     // 物品名称
                item.getPrice(),    // 物品价格
                item.getNbt()       // NBT数据
            ));
        }
        return result;
    }
}