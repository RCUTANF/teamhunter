package com.rcutanf.teamhunter.shop;

import com.rcutanf.teamhunter.Phase;
import com.rcutanf.teamhunter.Teamhunter;
import com.rcutanf.teamhunter.advancement.AdvancementListener;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.loot.context.LootWorldContext;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.context.ContextType;

import java.util.List;

public class ShopManager {
    public static final ContextType SHOP_CONTEXT = new ContextType.Builder().allow(LootContextParameters.THIS_ENTITY).build();

    public static final RegistryKey<LootTable> SHOP_LOOT_TABLE = RegistryKey.of(RegistryKeys.LOOT_TABLE, Identifier.of(Teamhunter.MOD_ID, "shop"));

    /**
     * 通过物品ID购买物品
     *
     * @return 是否购买成功
     */
    public static boolean purchaseItemById(ServerPlayerEntity player, String itemId) {
        // 检查比赛是否在进行中
        if (Teamhunter.phaseManager.Phase() != Phase.MATCH) {
            player.sendMessage(Text.of("§c商店只在比赛阶段可用！"), false);
            return false;
        }

        // 获取物品
        var item = ItemStack.EMPTY;
        if (item == null) {
            player.sendMessage(Text.of("§c找不到ID为 " + itemId + " 的物品！"), false);
            return false;
        }

        return processPurchase(player, item);
    }

    /**
     * 购买物品
     *
     * @return 是否购买成功
     */
    public static boolean purchaseItem(ServerPlayerEntity player, String itemName) {

        // 检查比赛是否在进行中
        if (Teamhunter.phaseManager.Phase() != Phase.MATCH) {
            player.sendMessage(Text.of("§c商店只在比赛阶段可用！"), false);
            return false;
        }

        // 获取物品
        var item = ItemStack.EMPTY;
        if (item == null) {
            player.sendMessage(Text.of("§c该物品不存在！"), false);
            return false;
        }

        return processPurchase(player, item);
    }

    /**
     * 处理购买流程
     */
    private static boolean processPurchase(ServerPlayerEntity player, ItemStack item) {
        // 获取玩家队伍
        String teamName = player.getScoreboardTeam() != null ? player.getScoreboardTeam().getName() : null;

        if (teamName == null || (!teamName.equals("hunters") && !teamName.equals("runners"))) {
            player.sendMessage(Text.of("§c你不属于任何可用团队！"), false);
            return false;
        }

        // 检查团队分数是否足够
        int teamScore = teamName.equals("hunters") ? AdvancementListener.getHuntersScore() : AdvancementListener.getRunnersScore();

        var price = item.get(ShopComponentTypes.PRICE);
        if (teamScore < price) {
            player.sendMessage(Text.of("§c团队分数不足！需要 " + price + " 分，当前只有 " + teamScore + " 分"), false);
            return false;
        }

        // 扣除团队分数
        MinecraftServer server = player.getServer();
        ServerWorld world = player.getServerWorld();
        int newScore = AdvancementListener.reduceTeamScore(teamName, price, world, player.getName().getString() + " 购买了 " + item.getName());

        // 给予物品（支持NBT数据）
        //TODO：可以看到不应该多出一个数据结构command，应该是当初混乱了，先这样用着后面再修
        String playerName = player.getName().getString();


        // 发送成功消息
        player.sendMessage(Text.of("§a成功购买 " + item.getName() + "，扣除 " + price + " 分！"), false);

        // 通知团队
        String teamMessage = "§e" + playerName + " 购买了 " + item.getName() + "，消耗团队 " + price + " 分！";
        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
            if (p.getScoreboardTeam() != null && p.getScoreboardTeam().getName().equals(teamName) && p != player) {
                p.sendMessage(Text.of(teamMessage), false);
            }
        }

        return true;
    }

    public static List<ItemStack> getAllItems(ServerPlayerEntity player) {
        var server = player.getServer();
        final var world = player.getWorld();
        if (server == null || !(world instanceof ServerWorld serverWorld)) {
            return List.of();
        }
        var lootTable = server.getReloadableRegistries().getLootTable(SHOP_LOOT_TABLE);
        final var lootWorldContext = new LootWorldContext.Builder(serverWorld).add(LootContextParameters.THIS_ENTITY, player).build(SHOP_CONTEXT);
        return lootTable.generateLoot(lootWorldContext);
    }

}