package com.rcutanf.teamhunter.shop;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.List;

import static net.minecraft.server.command.CommandManager.literal;
import static net.minecraft.server.command.CommandManager.argument;

public class ShopCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
            literal("shop")
                .then(literal("list")
                    .executes(ShopCommand::listItems)
                )
                .then(literal("buy")
                    .then(argument("item", StringArgumentType.greedyString())
                        .executes(ShopCommand::buyItem)
                    )
                )
                .then(literal("buyid")
                        .then(argument("itemId", StringArgumentType.word())
                                .executes(ShopCommand::buyItemById)
                        )
                )
                .then(literal("reload")
                    .requires(source -> source.hasPermissionLevel(2)) // 需要OP权限
                    .executes(ShopCommand::reloadConfig)
                )
                .executes(ShopCommand::showHelp)
        );
    }

    private static int showHelp(CommandContext<ServerCommandSource> context) {
        ServerPlayerEntity player;
        try {
            player = context.getSource().getPlayerOrThrow();
        } catch (Exception e) {
            context.getSource().sendError(Text.of("此命令只能由玩家执行"));
            return 0;
        }

        player.sendMessage(Text.of("§6===== 团队商店命令 ====="), false);
        player.sendMessage(Text.of("§e/shop list §7- 查看所有可购买物品"), false);
        player.sendMessage(Text.of("§e/shop buy <物品名称> §7- 购买指定物品"), false);
        player.sendMessage(Text.of("§e/shop buyid <物品ID> §7- 通过物品ID购买物品"), false);
        if (player.hasPermissionLevel(2)) {
            player.sendMessage(Text.of("§e/shop reload §7- 重载商店配置文件"), false);
        }
        player.sendMessage(Text.of("§6==================="), false);

        return 1;
    }

    private static int listItems(CommandContext<ServerCommandSource> context) {
        ServerPlayerEntity player;
        try {
            player = context.getSource().getPlayerOrThrow();
        } catch (Exception e) {
            context.getSource().sendError(Text.of("此命令只能由玩家执行"));
            return 0;
        }

        List<ShopManager.ShopItem> items = ShopManager.getAllItems();

        player.sendMessage(Text.of("§6===== 团队商店物品 ====="), false);
        for (ShopManager.ShopItem item : items) {
            player.sendMessage(Text.of("§e" + item.getName() + " §7- §a" + item.getPrice() + " 分"), false);
        }
        player.sendMessage(Text.of("§6使用 §e/shop buy <物品名称> §6购买物品"), false);

        return 1;
    }

    private static int buyItem(CommandContext<ServerCommandSource> context) {
        ServerPlayerEntity player;
        try {
            player = context.getSource().getPlayerOrThrow();
        } catch (Exception e) {
            context.getSource().sendError(Text.of("此命令只能由玩家执行"));
            return 0;
        }

        String itemName = StringArgumentType.getString(context, "item");
        ShopManager.purchaseItem(player, itemName);

        return 1;
    }

    private static int reloadConfig(CommandContext<ServerCommandSource> context) {
        boolean success = ShopManager.reloadConfig();

        if (success) {
            context.getSource().sendFeedback(() -> Text.of("§a商店配置已成功重载！"), true);
        } else {
            context.getSource().sendError(Text.of("§c商店配置重载失败，请查看控制台获取详细信息"));
        }

        return success ? 1 : 0;
    }

    private static int buyItemById(CommandContext<ServerCommandSource> context) {
        ServerPlayerEntity player;
        try {
            player = context.getSource().getPlayerOrThrow();
        } catch (Exception e) {
            context.getSource().sendError(Text.of("此命令只能由玩家执行"));
            return 0;
        }

        String itemId = StringArgumentType.getString(context, "itemId");
        ShopManager.purchaseItemById(player, itemId);

        return 1;
    }
}