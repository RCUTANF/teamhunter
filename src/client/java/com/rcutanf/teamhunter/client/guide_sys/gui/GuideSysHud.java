package com.rcutanf.teamhunter.client.guide_sys.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.OrderedText;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.client.render.RenderLayer;

import java.util.List;

public class GuideSysHud {
    private static final int MARGIN_RIGHT = 8;
    private static final int MARGIN_TOP = 8;
    private static final int ITEM_HEIGHT = 20;
    private static final int ITEM_PADDING = 3;
    private static final int DESCRIPTION_PADDING = 8;
    private static final int ICON_SIZE = 16;

    private static boolean showAllDescriptions = false;

    // 进度图标资源
    private static final Identifier PROGRESS_FULL_PROGRESSED = Identifier.of("teamhunter", "textures/ui/guide/progress_full_progressed.png");
    private static final Identifier PROGRESS_IN_PROGRESS = Identifier.of("teamhunter", "textures/ui/guide/progress_in_progress.png");
    private static final Identifier PROGRESS_COMPLETE = Identifier.of("teamhunter", "textures/ui/guide/progress_complete.png");

    private GuideSysHud() {}

    public static void render(DrawContext context, RenderTickCounter tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        List<GuideSysGuiManager.AdvancementGuideItem> guides = GuideSysGuiManager.getAdvancementGuides();
        TextRenderer textRenderer = client.textRenderer;
        int screenWidth = client.getWindow().getScaledWidth();

        // 计算统一宽度
        int maxWidth = 140; // 最小宽度
        if (!guides.isEmpty()) {
            for (GuideSysGuiManager.AdvancementGuideItem guide : guides) {
                int titleWidth = textRenderer.getWidth(guide.getTitle());
                String progressText = (int) guide.getProgress() + "%";
                int progressTextWidth = textRenderer.getWidth(progressText);
                int itemWidth = ICON_SIZE + ITEM_PADDING * 3 + titleWidth + progressTextWidth + ICON_SIZE;
                maxWidth = Math.max(maxWidth, itemWidth);
            }
        }
        // 确保表头文本也能适应
        String headerText = "进度列表";
        int headerTextWidth = textRenderer.getWidth(headerText) + ITEM_PADDING * 6;
        maxWidth = Math.max(maxWidth, headerTextWidth);

        int itemWidth = maxWidth;
        int x = screenWidth - MARGIN_RIGHT - itemWidth;

        // 绘制表头
        int headerHeight = 20;
        int headerX = x;

        // 表头背景 - 深蓝色
        context.fill(headerX, MARGIN_TOP, screenWidth - MARGIN_RIGHT, MARGIN_TOP + headerHeight, 0xA0204080);
        // 表头边框
        context.fill(headerX, MARGIN_TOP + headerHeight - 1, screenWidth - MARGIN_RIGHT, MARGIN_TOP + headerHeight, 0xFF4080FF);

        // 表头文本
        context.drawText(textRenderer, headerText,
                headerX + (itemWidth - textRenderer.getWidth(headerText)) / 2,
                MARGIN_TOP + (headerHeight - textRenderer.fontHeight) / 2,
                0xFFFFFFFF, true);

        // 说明文本 (显示按Tab可展开详情)
        String hintText = "按下TAB展开进度描述";
        int hintY = MARGIN_TOP + headerHeight;
        int hintHeight = textRenderer.fontHeight + 2;

        // 为说明文本添加背景框
        context.fill(headerX, hintY, screenWidth - MARGIN_RIGHT, hintY + hintHeight, 0x90000000);
        // 说明文本
        context.drawText(textRenderer, hintText,
                headerX + (itemWidth - textRenderer.getWidth(hintText)) / 2,
                hintY + 1,
                0xFFAAAAFF, true);

        // 如果没有指南项，显示提示信息
        if (guides.isEmpty()) {
            String emptyText = "暂无进度";
            int emptyY = hintY + hintHeight;
            int emptyX = headerX + (itemWidth - textRenderer.getWidth(emptyText)) / 2;
            context.drawText(textRenderer, emptyText, emptyX, emptyY, 0xFFAAAAAA, true);
            return;
        }

        int y = hintY + hintHeight;
        int itemIndex = 0;

        for (GuideSysGuiManager.AdvancementGuideItem guide : guides) {
            // 交替背景色增强可读性
            int backgroundColor = (itemIndex % 2 == 0) ? 0x90202020 : 0x90303030;

            // 绘制背景（无间距）
            context.fill(x, y, screenWidth - MARGIN_RIGHT, y + ITEM_HEIGHT, backgroundColor);

            // 绘制边框线
            context.fill(x, y + ITEM_HEIGHT - 1, screenWidth - MARGIN_RIGHT, y + ITEM_HEIGHT, 0x40FFFFFF);

            // 绘制成就图标
            context.drawItem(guide.getIcon(), x + ITEM_PADDING, y + (ITEM_HEIGHT - ICON_SIZE) / 2);

            // 绘制标题
            int titleX = x + ICON_SIZE + ITEM_PADDING * 2;
            int titleY = y + (ITEM_HEIGHT - textRenderer.fontHeight) / 2 + 1;
            context.drawText(textRenderer, guide.getTitle(), titleX, titleY, 0xFFFFFFFF, true);

            // 绘制进度百分比文本
            String progressText = (int) guide.getProgress() + "%";
            int progressTextWidth = textRenderer.getWidth(progressText);
            int progressTextX = screenWidth - MARGIN_RIGHT - ICON_SIZE - ITEM_PADDING * 2 - progressTextWidth;
            int progressTextY = y + (ITEM_HEIGHT - textRenderer.fontHeight) / 2 + 1;
            int progressColor = guide.isCompleted() ? 0xFF55FF55 : (guide.getProgress() > 0 ? 0xFF55AAFF : 0xFFAAAAAA);
            context.drawText(textRenderer, progressText, progressTextX, progressTextY, progressColor, true);

            // 绘制状态图标
            int statusX = screenWidth - MARGIN_RIGHT - ICON_SIZE - ITEM_PADDING;
            int statusY = y + (ITEM_HEIGHT - ICON_SIZE) / 2;

            Identifier statusIcon;
            if (guide.isCompleted()) {
                statusIcon = PROGRESS_COMPLETE;
            } else if (guide.getProgress() >= 0) {
                statusIcon = PROGRESS_IN_PROGRESS;
            } else if (guide.getProgress() == 100){
                statusIcon = PROGRESS_FULL_PROGRESSED;
            } else {
                statusIcon = PROGRESS_IN_PROGRESS;
            }

            context.drawTexture(RenderLayer::getGuiTextured, statusIcon, statusX, statusY, 0, 0, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);

            // 绘制进度条（2像素高）
            int progressBarY = y + ITEM_HEIGHT - 2;
            // 进度条背景
            context.fill(x, progressBarY, x + itemWidth, progressBarY + 2, 0xFF404040);
            // 进度条填充
            int fillWidth = (int) (itemWidth * (MathHelper.clamp(guide.getProgress(), 0, 100) / 100.0f));
            if (fillWidth > 0) {
                context.fill(x, progressBarY, x + fillWidth, progressBarY + 2, progressColor);
            }

            // 如果显示所有描述且该项有描述，则绘制描述
            if (showAllDescriptions && guide.getDescription() != null) {

                // 自动换行处理
                List<OrderedText> lines = textRenderer.wrapLines(guide.getDescription(), itemWidth - 6); // 留一些内边距
                int descriptionHeight = lines.size() * (textRenderer.fontHeight + 1) + 4;

                y += ITEM_HEIGHT; // 紧接当前项

                // 描述背景
                int descBgColor = 0xA0000000;
                context.fill(x, y, screenWidth - MARGIN_RIGHT, y + descriptionHeight, descBgColor);

                // 描述文本
                int textY = y + 2;
                for (OrderedText line : lines) {
                    context.drawText(textRenderer, line, x + 3, textY,  0xFFAA8800, true); // 浅灰色，比白色柔和
                    textY += textRenderer.fontHeight + 1;
                }

                y += descriptionHeight; // 描述结束后继续
            } else {
                y += ITEM_HEIGHT; // 直接累加，无间距
            }

            itemIndex++;
        }
    }

    /**
     * 设置是否显示所有描述
     */
    public static void setShowAllDescriptions(boolean show) {
        showAllDescriptions = show;
    }

    /**
     * 获取当前是否显示所有描述
     */
    public static boolean isShowingAllDescriptions() {
        return showAllDescriptions;
    }
}