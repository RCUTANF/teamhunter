package com.rcutanf.teamhunter.shop;

import com.rcutanf.teamhunter.Phase;
import com.rcutanf.teamhunter.Teamhunter;
import com.rcutanf.teamhunter.advancement.AdvancementListener;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.item.PlayerInventoryStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
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

import static com.rcutanf.teamhunter.shop.ShopComponentTypes.ID;
import static com.rcutanf.teamhunter.shop.ShopComponentTypes.PRICE;

public class ShopManager {
    public static final ContextType SHOP_CONTEXT = new ContextType.Builder().allow(LootContextParameters.THIS_ENTITY).build();

    public static final RegistryKey<LootTable> SHOP_LOOT_TABLE = RegistryKey.of(RegistryKeys.LOOT_TABLE, Identifier.of(Teamhunter.MOD_ID, "shop"));

    /**
     * 通过物品ID购买物品
     *
     * @return 是否购买成功
     */
    public static boolean purchaseItemByIndex(ServerPlayerEntity player, int index) {
        // 检查比赛是否在进行中
        if (Teamhunter.phaseManager.Phase() != Phase.MATCH) {
            player.sendMessage(Text.of("§c商店只在比赛阶段可用！"), false);
            return false;
        }

        // 获取物品
        var item = getAllItems(player).get(index);
        if (item.isEmpty()) {
            player.sendMessage(Text.of("§c找不到ID为 " + index + " 的物品！"), false);
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
        var item = getAllItems(player).stream().filter(i->i.getItemName().getString().equalsIgnoreCase(itemName)||i.getCustomName().getString().equalsIgnoreCase(itemName))
                .findFirst().orElse(null);

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
        // 获取并校验玩家队伍
        String teamName = player.getScoreboardTeam() != null ? player.getScoreboardTeam().getName() : null;
        if (teamName == null || (!teamName.equals("hunters") && !teamName.equals("runners"))) {
            player.sendMessage(Text.of("§c你不属于任何可用团队！"), false);
            return false;
        }

        // 单价
        var unitPrice = item.get(PRICE);
        if (unitPrice == null || unitPrice <= 0) return false;

        // 构造可插入物品（移除自定义组件）
        ItemStack buyItem = item.copy();
        buyItem.remove(PRICE);
        buyItem.remove(ID);

        // 准备变量
        long inserted;
        long cost;

        // 向背包插入物品并保持事务开启
        try (var tx = Transaction.openOuter()) {
            inserted = PlayerInventoryStorage.of(player).offer(ItemVariant.of(buyItem), item.getCount(), tx);
            if (inserted == 0) {
                player.sendMessage(Text.of("§c背包空间不足或交易失败！"), false);
                return false;            // 自动回滚
            }

            cost = (long) unitPrice * inserted;

            // 检查团队分数
            int teamScore = teamName.equals("hunters") ? AdvancementListener.getHuntersScore()
                    : AdvancementListener.getRunnersScore();
            if (teamScore < cost) {
                player.sendMessage(Text.of("§c团队分数不足！需要 " + cost + " 分，当前只有 " + teamScore + " 分"), false);
                return false;            // 自动回滚
            }

            // 扣除团队分数（非物品事务，但必须先成功）
            ServerWorld world = player.getServerWorld();
            AdvancementListener.reduceTeamScore(teamName, (int) cost, player.getServer(),
                    player.getName().copy().append(" 购买了 ").append( item.getName()).append(  " ×" + inserted));

            // 分数扣除成功后提交物品事务
            tx.commit();
        }

        // 至此物品和分数均已生效
        // 成功提示
        player.sendMessage(Text.of("§a成功购买 ").copy().append(item.getName()).append(" ×" + inserted + "，扣除 " + cost + " 分！"), false);

        // 通知团队其他成员
        String playerName = player.getName().getString();
        var teamMessage = Text.of("§e" + playerName + " 购买了 ").copy().append(item.getName()).append(" ×" + inserted + "，消耗团队 " + cost + " 分！");
        MinecraftServer server = player.getServer();
        if (server != null) {
            for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
                if (p != player && p.getScoreboardTeam() != null
                    && p.getScoreboardTeam().getName().equals(teamName)) {
                    p.sendMessage(teamMessage, false);
                }
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