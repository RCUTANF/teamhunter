package com.rcutanf.teamhunter.client.ui;

import com.rcutanf.teamhunter.NetWorking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.StringNbtReader;
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

    // 设置固定窗口大小
    private static final int WINDOW_WIDTH = 252;
    private static final int WINDOW_HEIGHT = 166;
    private static final int ICON_SIZE = 32;
    private static final int GRID_SPACING = 16;

    // 窗口位置
    private int guiLeft;
    private int guiTop;

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
        final String name;
        final int price;
        final String nbt;
        final ItemStack stack;

        ShopItem(String id, String name, int price, String nbt) {
            this.id = id;
            this.name = name;
            this.price = price;
            this.nbt = nbt;

            // 创建物品并应用NBT数据
            Item item = Registries.ITEM.get(Identifier.of(id));
            ItemStack itemStack = new ItemStack(item);

            // 如果有NBT数据，则应用
            if (nbt != null && !nbt.isEmpty()) {
                try {
                    NbtCompound nbtData = StringNbtReader.parse(nbt);
                    itemStack.setTag(nbtData); // 使用setTag而不是setNbt
                } catch (Exception e) {
                    System.out.println("解析NBT数据错误: " + e.getMessage());
                }
            }

            // 如果有自定义名称，则应用
            if (name != null && !name.isEmpty()) {
                itemStack.setCustomName(Text.literal(name));
            }

            this.stack = itemStack;
        }
    }

    private void loadDefaultItems() {
        shopItems.add(new ShopItem("minecraft:diamond_sword", "钻石剑", 50, null));
        shopItems.add(new ShopItem("minecraft:diamond_helmet", "钻石头盔", 40, null));
        shopItems.add(new ShopItem("minecraft:diamond_chestplate", "钻石胸甲", 50, null));
        shopItems.add(new ShopItem("minecraft:diamond_leggings", "钻石护腿", 45, null));
        shopItems.add(new ShopItem("minecraft:diamond_boots", "钻石靴子", 35, null));
        shopItems.add(new ShopItem("minecraft:enchanted_golden_apple", "附魔金苹果", 100, null));
        shopItems.add(new ShopItem("minecraft:ender_pearl", "末影珍珠", 40, null));
        shopItems.add(new ShopItem("minecraft:arrow", "箭", 25, null));
        shopItems.add(new ShopItem("minecraft:golden_carrot", "金胡萝卜", 20, null));
        shopItems.add(new ShopItem("minecraft:iron_axe", "铁斧", 45, null));
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

        // 计算窗口位置（居中）
        this.guiLeft = (this.width - WINDOW_WIDTH) / 2;
        this.guiTop = (this.height - WINDOW_HEIGHT) / 2;

        // 重新定位关闭按钮
        this.addDrawableChild(ButtonWidget.builder(
                        Text.literal("关闭"),
                        btn -> this.close())
                .dimensions(guiLeft + WINDOW_WIDTH / 2 - 40, guiTop + WINDOW_HEIGHT - 25, 80, 20)
                .build());

        // 重置滚动位置
        scrollX = 0;
        scrollY = 0;
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        // 绘制一个半透明的背景以隔离游戏世界
        context.fillGradient(0, 0, this.width, this.height, 0x80000000, 0x90000000);

        // 不调用super.renderBackground避免模糊效果
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // 绘制背景
        renderBackground(context, mouseX, mouseY, delta);
        //super.render(context, mouseX, mouseY, delta);


        // 绘制窗口背景
        renderWindowBackground(context);

        // 绘制物品网格
        renderItemGrid(context, mouseX, mouseY);

        // 绘制标题和分数
        context.drawCenteredTextWithShadow(
                this.textRenderer,
                this.title,
                guiLeft + WINDOW_WIDTH / 2,
                guiTop + 10,
                0xFFFFFFFF);

        String teamText = (isHunterTeam ? "猎人队" : "逃亡者队") + " 分数: " + teamScore;
        context.drawCenteredTextWithShadow(
                this.textRenderer,
                Text.literal(teamText),
                guiLeft + WINDOW_WIDTH / 2,
                guiTop + 25,
                isHunterTeam ? 0xFFFF5555 : 0xFF55FFFF);

        // 渲染按钮
        super.render(context, mouseX, mouseY, delta);

        // 渲染悬停提示
        if (hoveredItemIndex >= 0 && hoveredItemIndex < shopItems.size()) {
            renderTooltip(context, mouseX, mouseY, shopItems.get(hoveredItemIndex));
        }


    }

    private void renderWindowBackground(DrawContext context) {
        // 绘制固定大小的背景
        context.drawTexture(
                identifier -> RenderLayer.getGuiTextured(identifier),
                BACKGROUND,
                guiLeft,
                guiTop,
                0,
                0,
                WINDOW_WIDTH,
                WINDOW_HEIGHT,
                16,
                16
        );
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

        // 每行显示4个物品
        int itemsPerRow = 4;


        // 创建一个裁剪区域以防止物品渲染超出窗口
        context.enableScissor(
                guiLeft + 10,
                guiTop + 40,
                guiLeft + WINDOW_WIDTH - 10,
                guiTop + WINDOW_HEIGHT - 30
        );

        for (int i = 0; i < shopItems.size(); i++) {
            ShopItem item = shopItems.get(i);

            int row = i / itemsPerRow;
            int col = i % itemsPerRow;

            int x = startX + col * (ICON_SIZE + GRID_SPACING) + (int)scrollX;
            int y = startY + row * (ICON_SIZE + GRID_SPACING) + (int)scrollY;

            // 判断是否在可视区域内
            if (x < guiLeft || x > guiLeft + WINDOW_WIDTH - ICON_SIZE ||
                    y < guiTop + 40 || y > guiTop + WINDOW_HEIGHT - 30) {
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

            // 检查鼠标悬停（需要在窗口内）
            if (mouseX >= x && mouseX <= x + ICON_SIZE && mouseY >= y && mouseY <= y + ICON_SIZE &&
                    mouseX >= guiLeft && mouseX <= guiLeft + WINDOW_WIDTH &&
                    mouseY >= guiTop && mouseY <= guiTop + WINDOW_HEIGHT) {
                // 高亮选中的物品
                context.fill(x, y, x + ICON_SIZE, y + ICON_SIZE, 0x80FFFFFF);
                hoveredItemIndex = i;
            }
        }

        context.disableScissor();// 关闭裁剪区域
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

        // 确保点击在窗口内
        if (mouseX < guiLeft || mouseX > guiLeft + WINDOW_WIDTH ||
                mouseY < guiTop || mouseY > guiTop + WINDOW_HEIGHT) {
            return false;
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
            // 检查是否在可拖动区域内
            boolean inDragArea = hoveredItemIndex < 0 &&
                    mouseY > guiTop + 40 && mouseY < guiTop + WINDOW_HEIGHT - 30;
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
            try {
                items.add(new ShopItem(data.id(), data.name(), data.price(), data.nbt()));
            } catch (Exception e) {
                System.out.println("处理商店物品数据错误: " + data.id() + ", 价格: " + data.price() + ", 错误: " + e.getMessage());
                // 可以在这里添加更详细的日志记录
            }
        }
        cachedShopItems = new ArrayList<>(items);

        // 检查当前界面是否为ShopScreen，如果是则刷新
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.currentScreen instanceof ShopScreen) {
            // 更新当前界面的商品列表并刷新
            ((ShopScreen) client.currentScreen).setShopItems(items);
        }
    }

    @Override
    public boolean shouldPause() {
        return false;  // 防止游戏暂停，同时也会禁用背景模糊
    }
}