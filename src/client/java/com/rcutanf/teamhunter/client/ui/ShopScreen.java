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
import net.minecraft.client.render.RenderLayer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import java.util.ArrayList;
import java.util.List;

public class ShopScreen extends Screen {
    // 背景纹理
    private static final Identifier BACKGROUND = Identifier.of("textures/gui/advancements/backgrounds/stone.png");
    private static final int ICON_SIZE = 32;
    private static final int GRID_SPACING = 16;

    // 滚动相关
    private double scrollX;
    private double scrollY;
    private boolean isDragging;
    private int lastMouseX;
    private int lastMouseY;

    private List<ShopItem> shopItems = new ArrayList<>();
    private int teamScore = 0;
    private boolean isHunterTeam = false;

    private static ShopScreen INSTANCE;
    private static List<ShopItem> cachedShopItems = null;

    // 悬停的物品索引
    private int hoveredItemIndex = -1;

    public ShopScreen() {
        super(Text.translatable("teamhunter.shop.title"));
    }

    public static ShopScreen getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new ShopScreen();
        }
        return INSTANCE;
    }

    public static void open() {
        ShopScreen screen = getInstance();

        if (cachedShopItems != null && !cachedShopItems.isEmpty()) {
            screen.shopItems = new ArrayList<>(cachedShopItems);
        } else {
            screen.loadDefaultItems();
        }

        screen.requestShopItems();
        MinecraftClient.getInstance().setScreen(screen);
    }

    static class ShopItem {
        final String id;
        final int price;
        final ItemStack stack;

        ShopItem(String id, int price) {
            this.id = id;
            this.price = price;
            Item item = Registries.ITEM.get(Identifier.of(id));
            this.stack = new ItemStack(item);
        }
    }

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

        // 设置团队和分数
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null && client.player.getScoreboardTeam() != null) {
            String teamName = client.player.getScoreboardTeam().getName();
            isHunterTeam = "hunters".equals(teamName);
            teamScore = isHunterTeam ? TeamScoreHud.getHuntersScore() : TeamScoreHud.getRunnersScore();
        }

        // 添加关闭按钮
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("关闭"),
                btn -> this.close())
            .dimensions(width / 2 - 40, height - 30, 80, 20)
            .build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // 绘制背景
        //renderBackground(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
        renderGridBackground(context);

        // 绘制物品网格
        renderItemGrid(context, mouseX, mouseY);

        // 绘制标题和分数
        context.drawCenteredTextWithShadow(
                this.textRenderer,
                this.title,
                width / 2,
                20,
                0xFFFFFFFF);

        String teamText = (isHunterTeam ? "猎人队" : "逃亡者队") + " 分数: " + teamScore;
        context.drawCenteredTextWithShadow(
                this.textRenderer,
                Text.literal(teamText),
                width / 2,
                35,
                isHunterTeam ? 0xFFFF5555 : 0xFF55FFFF);

        // 渲染悬停提示
        if (hoveredItemIndex >= 0 && hoveredItemIndex < shopItems.size()) {
            renderTooltip(context, mouseX, mouseY, shopItems.get(hoveredItemIndex));
        }


    }

    private void renderGridBackground(DrawContext context) {
        // 绘制类似进度界面的背景纹理
        int centerX = width / 2;
        int centerY = height / 2;
        int size = Math.max(width, height);

        // 使用进度界面的石头背景纹理
        context.drawTexture(
                identifier -> RenderLayer.getGuiTextured(identifier),  // 渲染层函数
                BACKGROUND,                                 // 纹理标识符
                centerX - size / 2 + (int)scrollX,          // x坐标
                centerY - size / 2 + (int)scrollY,          // y坐标
                0,                                          // u纹理坐标
                0,                                          // v纹理坐标
                size,                                       // 宽度
                size,                                       // 高度
                16,                                         // 纹理宽度
                16                                          // 纹理高度
        );
    }

    private void renderItemGrid(DrawContext context, int mouseX, int mouseY) {
        hoveredItemIndex = -1;

        // 计算网格开始位置
        int centerX = width / 2;
        int centerY = height / 2;
        int startX = centerX - (ICON_SIZE * 3) / 2;
        int startY = centerY - (ICON_SIZE * 3) / 2;

        // 每行显示5个物品
        int itemsPerRow = 5;

        for (int i = 0; i < shopItems.size(); i++) {
            ShopItem item = shopItems.get(i);

            int row = i / itemsPerRow;
            int col = i % itemsPerRow;

            int x = startX + col * (ICON_SIZE + GRID_SPACING) + (int)scrollX;
            int y = startY + row * (ICON_SIZE + GRID_SPACING) + (int)scrollY;

            // 判断是否超出屏幕，如果是则不渲染
            if (x < -ICON_SIZE || x > width || y < -ICON_SIZE || y > height) {
                continue;
            }

            // 绘制背景框
            int bgColor = teamScore >= item.price ? 0x80FFFFFF : 0x80FF5555;
            context.fill(x, y, x + ICON_SIZE, y + ICON_SIZE, bgColor);

            // 绘制物品
            context.drawItem(item.stack, x + ICON_SIZE/2 - 8, y + ICON_SIZE/2 - 8);

            // 绘制价格
            context.drawText(textRenderer, String.valueOf(item.price),
                    x + ICON_SIZE/2 - textRenderer.getWidth(String.valueOf(item.price))/2,
                    y + ICON_SIZE - 10, 0xFFFFFF, true);

            // 检查鼠标悬停
            if (mouseX >= x && mouseX <= x + ICON_SIZE && mouseY >= y && mouseY <= y + ICON_SIZE) {
                // 高亮选中的物品
                context.fill(x, y, x + ICON_SIZE, y + ICON_SIZE, 0x80FFFFFF);
                hoveredItemIndex = i;
            }
        }
    }

    private void renderTooltip(DrawContext context, int mouseX, int mouseY, ShopItem item) {
        List<Text> tooltip = new ArrayList<>();
        tooltip.add(item.stack.getName());
        tooltip.add(Text.literal("价格: " + item.price + " 分"));

        if (teamScore < item.price) {
            tooltip.add(Text.literal("§c积分不足!"));
        } else {
            tooltip.add(Text.literal("§a点击购买"));
        }

        context.drawTooltip(textRenderer, tooltip, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }

        if (button == 0 && hoveredItemIndex >= 0 && hoveredItemIndex < shopItems.size()) {
            // 尝试购买物品
            ShopItem item = shopItems.get(hoveredItemIndex);
            if (teamScore >= item.price) {
                purchaseItem(item);
                return true;
            }
        }

        if (button == 0) {
            // 检查是否在有效的拖动区域内（避开UI元素和物品）
            boolean inDragArea = hoveredItemIndex < 0 && mouseY > 50 && mouseY < height - 40;
            if (inDragArea) {
                isDragging = true;
                lastMouseX = (int) mouseX;
                lastMouseY = (int) mouseY;
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            isDragging = false;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (isDragging) {
            scrollX += mouseX - lastMouseX;
            scrollY += mouseY - lastMouseY;
            lastMouseX = (int) mouseX;
            lastMouseY = (int) mouseY;
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        // 滚轮滚动
        scrollY += verticalAmount * 16;
        return true;
    }

    private void requestShopItems() {
        ClientPlayNetworking.send(new NetWorking.ShopItemsRequestPacket());
    }

    public void setShopItems(List<ShopItem> items) {
        this.shopItems = items;
        this.clearAndInit();
    }

    private void purchaseItem(ShopItem item) {
        MinecraftClient client = MinecraftClient.getInstance();

        if (client.player != null) {
            client.player.networkHandler.sendChatCommand("shop buyid " + item.id);
        }

        //this.close();
    }

    public void updateShopItemsFromData(List<NetWorking.ShopItemData> dataList) {
        List<ShopItem> items = new ArrayList<>();
        for (NetWorking.ShopItemData data : dataList) {
            items.add(new ShopItem(data.id(), data.price()));
        }
        cachedShopItems = new ArrayList<>(items);
    }

    @Override
    public boolean shouldPause() {
        return false;  // 防止游戏暂停，同时也会禁用背景模糊
    }
}