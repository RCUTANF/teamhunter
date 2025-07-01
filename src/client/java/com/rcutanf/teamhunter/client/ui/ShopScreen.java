package com.rcutanf.teamhunter.client.ui;

import com.rcutanf.teamhunter.NetWorking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.math.ColorHelper;
import io.netty.buffer.Unpooled;

import java.util.ArrayList;
import java.util.List;

public class ShopScreen extends Screen {
    private static final int ITEMS_PER_PAGE = 7;
    private static final int BUTTON_WIDTH = 180;
    private static final int BUTTON_HEIGHT = 20;
    private static final int PADDING = 10;

    private int currentPage = 0;
    private List<ShopItem> shopItems = new ArrayList<>();
    private int teamScore = 0;
    private boolean isHunterTeam = false;

    private ButtonWidget nextPageButton;
    private ButtonWidget prevPageButton;

    private static ShopScreen INSTANCE;

    // 商店物品缓存
    private static List<ShopItem> cachedShopItems = null;

    public ShopScreen() {
        super(Text.translatable("teamhunter.shop.title"));
    }

    /**
     * 获取商店界面实例
     */
    public static ShopScreen getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new ShopScreen();
        }
        return INSTANCE;
    }

    /**
     * 打开商店界面
     */
    public static void open() {
        ShopScreen screen = getInstance();

        // 先加载缓存数据（如果有）
        if (cachedShopItems != null && !cachedShopItems.isEmpty()) {
            screen.shopItems = new ArrayList<>(cachedShopItems);
        } else {
            // 无缓存则加载默认数据
            screen.loadDefaultItems();
        }

        // 请求最新数据
        screen.requestShopItems();

        // 显示界面
        MinecraftClient.getInstance().setScreen(screen);
    }

    /**
     * 内部类表示商店物品
     */
    static class ShopItem {
        final String id;
        final int price;
        final ItemStack stack;

        ShopItem(String id, int price) {
            this.id = id;
            this.price = price;

            // 从物品ID创建ItemStack
            Item item = Registries.ITEM.get(Identifier.of(id));
            this.stack = new ItemStack(item);
        }
    }

    /**
     * 加载默认商店物品
     */
    private void loadDefaultItems() {
        shopItems.add(new ShopItem("minecraft:diamond_sword", 50));
        shopItems.add(new ShopItem("minecraft:diamond_helmet", 40));
        shopItems.add(new ShopItem("minecraft:diamond_chestplate", 50));
        shopItems.add(new ShopItem("minecraft:diamond_leggings", 45));
        shopItems.add(new ShopItem("minecraft:diamond_boots", 35));
        shopItems.add(new ShopItem("minecraft:enchanted_golden_apple", 100));
        shopItems.add(new ShopItem("minecraft:ender_pearl", 40));
        shopItems.add(new ShopItem("minecraft:arrow", 25));
        shopItems.add(new ShopItem("minecraft:golden_carrot", 20));
        shopItems.add(new ShopItem("minecraft:iron_axe", 45));
    }

    @Override
    protected void init() {
        super.init();

        // 确定当前玩家队伍和分数
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null && client.player.getScoreboardTeam() != null) {
            String teamName = client.player.getScoreboardTeam().getName();
            isHunterTeam = "hunters".equals(teamName);
            teamScore = isHunterTeam ? TeamScoreHud.getHuntersScore() : TeamScoreHud.getRunnersScore();
        }

        // 计算屏幕中心
        int centerX = width / 2;
        int startY = 50;

        // 添加商店物品按钮
        int maxPage = (int) Math.ceil(shopItems.size() / (double) ITEMS_PER_PAGE);
        int startIdx = currentPage * ITEMS_PER_PAGE;
        int endIdx = Math.min(startIdx + ITEMS_PER_PAGE, shopItems.size());

        for (int i = startIdx; i < endIdx; i++) {
            ShopItem item = shopItems.get(i);
            int buttonY = startY + (i - startIdx) * (BUTTON_HEIGHT + 5);

            ButtonWidget button = ButtonWidget.builder(
                    Text.literal(item.stack.getName().getString() + " - " + item.price + " 分"),
                    btn -> purchaseItem(item))
                .dimensions(centerX - BUTTON_WIDTH / 2, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build();

            // 如果积分不足，禁用按钮
            button.active = teamScore >= item.price;

            this.addDrawableChild(button);
        }

        // 添加翻页按钮
        if (maxPage > 1) {
            prevPageButton = ButtonWidget.builder(
                    Text.literal("上一页"),
                    btn -> {
                        currentPage = Math.max(0, currentPage - 1);
                        this.clearAndInit();
                    })
                .dimensions(centerX - BUTTON_WIDTH / 2, height - 60, 80, BUTTON_HEIGHT)
                .build();

            nextPageButton = ButtonWidget.builder(
                    Text.literal("下一页"),
                    btn -> {
                        currentPage = Math.min(maxPage - 1, currentPage + 1);
                        this.clearAndInit();
                    })
                .dimensions(centerX + BUTTON_WIDTH / 2 - 80, height - 60, 80, BUTTON_HEIGHT)
                .build();

            prevPageButton.active = currentPage > 0;
            nextPageButton.active = currentPage < maxPage - 1;

            this.addDrawableChild(prevPageButton);
            this.addDrawableChild(nextPageButton);
        }

        // 添加关闭按钮
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("关闭"),
                btn -> this.close())
            .dimensions(centerX - 40, height - 30, 80, BUTTON_HEIGHT)
            .build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);

        // 绘制标题
        context.drawCenteredTextWithShadow(
                this.textRenderer,
                this.title,
                width / 2,
                20,
                0xFFFFFFFF);

        // 绘制团队分数
        String teamText = (isHunterTeam ? "猎人队" : "逃亡者队") + " 分数: " + teamScore;
        context.drawCenteredTextWithShadow(
                this.textRenderer,
                Text.literal(teamText),
                width / 2,
                35,
                isHunterTeam ? 0xFFFF5555 : 0xFF55FFFF);

        // 绘制物品图标
        int centerX = width / 2;
        int startY = 50;
        int startIdx = currentPage * ITEMS_PER_PAGE;
        int endIdx = Math.min(startIdx + ITEMS_PER_PAGE, shopItems.size());

        for (int i = startIdx; i < endIdx; i++) {
            ShopItem item = shopItems.get(i);
            int buttonY = startY + (i - startIdx) * (BUTTON_HEIGHT + 5);

            // 在按钮左侧绘制物品图标
            context.drawItem(item.stack, centerX - BUTTON_WIDTH / 2 - 20, buttonY + 2);
        }

        super.render(context, mouseX, mouseY, delta);
    }

    /**
     * 请求商店物品列表
     */
    private void requestShopItems() {
        // 发送请求商店物品列表的数据包
        ClientPlayNetworking.send(new NetWorking.ShopItemsRequestPacket());
    }

    /**
     * 设置商店物品列表（由网络回调调用）
     */
    public void setShopItems(List<ShopItem> items) {
        this.shopItems = items;
        // 刷新界面
        this.clearAndInit();
    }

    /**
     * 购买物品
     */
    private void purchaseItem(ShopItem item) {
        // 直接发送购买命令
        String itemName = item.id.replace("minecraft:", "");
        MinecraftClient client = MinecraftClient.getInstance();

        if (client.player != null) {
            // 使用/shop buy命令进行购买
            client.player.sendMessage(Text.literal("/shop buy " + itemName), false);

        }

        // 关闭屏幕
        this.close();
    }

    /**
     * 更新商店物品并保存到缓存
     */
    public void updateShopItemsFromData(List<NetWorking.ShopItemData> dataList) {
        List<ShopItem> items = new ArrayList<>();
        for (NetWorking.ShopItemData data : dataList) {
            items.add(new ShopItem(data.id(), data.price()));
        }
        // 更新缓存
        cachedShopItems = new ArrayList<>(items);
    }
}