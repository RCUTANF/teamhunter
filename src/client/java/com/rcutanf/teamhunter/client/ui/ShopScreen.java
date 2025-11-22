package com.rcutanf.teamhunter.client.ui;

import com.rcutanf.teamhunter.NetWorking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

import static com.rcutanf.teamhunter.shop.ShopComponentTypes.PRICE;

public class ShopScreen extends Screen {
    // 界面纹理
    private static final Identifier WINDOW_TEXTURE = Identifier.ofVanilla("textures/gui/advancements/window.png");
    // 商品框纹理参数
    private static final int ITEM_FRAME_U = 0;
    private static final int ITEM_FRAME_V = 166;
    private static final int ITEM_FRAME_SIZE = 26;

    // 设置固定窗口大小
    private static final int WINDOW_WIDTH = 252;
    private static final int WINDOW_HEIGHT = 166;
    private static final int ICON_SIZE = 32;
    private static final int GRID_SPACING = 8;  //列间距
    private static ShopScreen INSTANCE;
    public List<ItemStack> shopItems = new ArrayList<>();
    // 窗口位置
    private int guiLeft;
    private int guiTop;
    // 滚动相关
    private double scrollX;
    private double scrollY;
    private boolean isDragging;
    private int lastMouseX;
    private int lastMouseY;
    private int teamScore = 0;
    private boolean isHunterTeam = false;
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
        ClientPlayNetworking.send(NetWorking.ShopItemsRequestPacket.INSTANCE);

        MinecraftClient.getInstance().setScreen(screen);
    }

    @Override
    protected void init() {
        super.init();

        // 设置团队和分数
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
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null && client.player.getScoreboardTeam() != null) {
            String teamName = client.player.getScoreboardTeam().getName();
            isHunterTeam = "hunters".equals(teamName);
            teamScore = isHunterTeam ? TeamScoreHud.getHuntersScore() : TeamScoreHud.getRunnersScore();
        }

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
                guiTop + 5,
                0xFFFFFFFF);

        String teamText = (isHunterTeam ? "猎人队" : "逃亡者队") + " 分数: " + teamScore;
        context.drawCenteredTextWithShadow(
                this.textRenderer,
                Text.literal(teamText),
                guiLeft + WINDOW_WIDTH / 2,
                guiTop + 21,
                isHunterTeam ? 0xFFFF3333 : 0xFF33FF33);

        // 渲染按钮
        super.render(context, mouseX, mouseY, delta);

        // 渲染悬停提示
        if (hoveredItemIndex >= 0 && hoveredItemIndex < shopItems.size()) {
            renderTooltip(context, mouseX, mouseY, shopItems.get(hoveredItemIndex));
        }


    }

    private void renderWindowBackground(DrawContext context) {
        // 绘制进度界面风格的窗口背景和边框
        context.drawTexture(
                RenderPipelines.GUI_TEXTURED,
                WINDOW_TEXTURE,
                guiLeft, guiTop,
                0, 0,
                WINDOW_WIDTH, WINDOW_HEIGHT,
                256, 256 // window.png 的原始尺寸
        );
    }

    private void renderItemGrid(DrawContext context, int mouseX, int mouseY) {
        hoveredItemIndex = -1;

        // 设置网格与窗口边缘的边距
        final int MARGIN_LEFT = 30;
        final int MARGIN_TOP = 40;


        // 直接从窗口左上角开始计算，而不是从中心点
        int startX = guiLeft + MARGIN_LEFT;
        int startY = guiTop + MARGIN_TOP;

        // 每行显示5个物品
        int itemsPerRow = 5;


        // 创建一个裁剪区域以防止物品渲染超出窗口
        context.enableScissor(
                guiLeft + 10,
                guiTop + 40,
                guiLeft + WINDOW_WIDTH - 10,
                guiTop + WINDOW_HEIGHT - 40
        );

        for (int i = 0; i < shopItems.size(); i++) {
            ItemStack item = shopItems.get(i);
            var price = item.get(PRICE);

            int row = i / itemsPerRow;
            int col = i % itemsPerRow;

            int x = startX + col * (ICON_SIZE + GRID_SPACING) + (int) scrollX;
            int y = startY + row * (ICON_SIZE + GRID_SPACING) + (int) scrollY;

            // 判断是否在可视区域内
            if (x < guiLeft || x > guiLeft + WINDOW_WIDTH - ICON_SIZE ||
                y < guiTop + 40 || y > guiTop + WINDOW_HEIGHT - 30) {
                continue;
            }

            // 绘制白色边框
            context.fill(x - 2, y - 2, x + ICON_SIZE + 2, y + ICON_SIZE + 2, 0xFFFFFFFF);
            // 绘制背景框
            int bgColor = teamScore >= price ? 0x80FFFFFF : 0x80FF5555;
            context.fill(x, y, x + ICON_SIZE, y + ICON_SIZE, bgColor);

            // 绘制物品
            context.drawItem(item, x + ICON_SIZE / 2 - 8, y + ICON_SIZE / 2 - 10);

            // 绘制价格
            context.drawText(textRenderer, String.valueOf(price),
                    x + ICON_SIZE / 2 - textRenderer.getWidth(String.valueOf(price)) / 2,
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

    private void renderTooltip(DrawContext context, int mouseX, int mouseY, ItemStack item) {
        List<Text> tooltip = new ArrayList<>();
        tooltip.add(item.getName());
        tooltip.add(Text.literal("价格: " + item.get(PRICE) + " 分"));

        if (teamScore < item.get(PRICE)) {
            tooltip.add(Text.literal("§c积分不足!"));
        } else {
            tooltip.add(Text.literal("§a点击购买"));
        }

        context.drawTooltip(textRenderer, tooltip, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (super.mouseClicked(click, doubled)) {
            return true;
        }

        double mouseX = click.x();
        double mouseY = click.y();
        int button = click.buttonInfo().button();

        // 确保点击在窗口内
        if (mouseX < guiLeft || mouseX > guiLeft + WINDOW_WIDTH ||
                mouseY < guiTop || mouseY > guiTop + WINDOW_HEIGHT) {
            return false;
        }

        if (button == 0 && hoveredItemIndex >= 0 && hoveredItemIndex < shopItems.size()) {
            purchaseItem(hoveredItemIndex);
            return true;
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
    public boolean mouseReleased(Click click) {
        int button = click.buttonInfo().button();
        if (button == 0) {
            isDragging = false;
        }
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseDragged(Click click, double offsetX, double offsetY) {
        if (isDragging) {
            double mouseX = click.x();
            double mouseY = click.y();

            scrollX += offsetX;
            scrollY += offsetY;
            lastMouseX = (int) mouseX;
            lastMouseY = (int) mouseY;
            return true;
        }
        return super.mouseDragged(click, offsetX, offsetY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        // 滚轮滚动
        scrollY += verticalAmount * 16;
        return true;
    }

    private void purchaseItem(int index) {
        ClientPlayNetworking.send(
                new NetWorking.ShopPurchasePacket(index)
        );
    }

    @Override
    public boolean shouldPause() {
        return false;  // 防止游戏暂停，同时也会禁用背景模糊
    }
}