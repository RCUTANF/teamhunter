package com.rcutanf.teamhunter.client.guide_sys.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.client.render.RenderLayer;

import java.util.List;

public class GuideSysHud {
    private static final int MARGIN_RIGHT = 10;
    private static final int MARGIN_TOP = 10;
    private static final int ITEM_HEIGHT = 24;
    private static final int ITEM_PADDING = 4;
    private static final int DESCRIPTION_PADDING = 8;
    private static final int ICON_SIZE = 16;

    private static boolean showAllDescriptions = false;

    // 进度图标资源
    private static final Identifier PROGRESS_PENDING = Identifier.of("teamhunter", "textures/gui/guide/progress_pending.png");
    private static final Identifier PROGRESS_IN_PROGRESS = Identifier.of("teamhunter", "textures/gui/guide/progress_in_progress.png");
    private static final Identifier PROGRESS_COMPLETE = Identifier.of("teamhunter", "textures/gui/guide/progress_complete.png");

    private GuideSysHud() {}

    public static void render(DrawContext context, RenderTickCounter tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        List<GuideSysGuiManager.AdvancementGuideItem> guides = GuideSysGuiManager.getAdvancementGuides();
        TextRenderer textRenderer = client.textRenderer;
        int screenWidth = client.getWindow().getScaledWidth();

        // 绘制表头
        int headerHeight = 20;
        String headerText = "成就指南";
        int headerWidth = textRenderer.getWidth(headerText) + ITEM_PADDING * 4;
        int headerX = screenWidth - MARGIN_RIGHT - headerWidth;

        // 表头背景
        context.fill(headerX, MARGIN_TOP, screenWidth - MARGIN_RIGHT, MARGIN_TOP + headerHeight, 0xA0000080); // 半透明深蓝

        // 表头文本
        context.drawText(textRenderer, headerText,
                headerX + (headerWidth - textRenderer.getWidth(headerText)) / 2,
                MARGIN_TOP + (headerHeight - textRenderer.fontHeight) / 2,
                0xFFFFFFFF, true);

        // 说明文本 (显示按Tab可展开详情)
        String hintText = "按 TAB 键可展开详情";
        int hintX = headerX + (headerWidth - textRenderer.getWidth(hintText)) / 2;
        context.drawText(textRenderer, hintText, hintX,
                MARGIN_TOP + headerHeight + 2, 0xFFAAAAFF, true);

        // 如果没有指南项，显示提示信息
        if (guides.isEmpty()) {
            String emptyText = "暂无可完成进度";
            int emptyX = headerX + (headerWidth - textRenderer.getWidth(emptyText)) / 2;
            context.drawText(textRenderer, emptyText, emptyX,
                    MARGIN_TOP + headerHeight + textRenderer.fontHeight + 6,
                    0xFFAAAAAA, true);
            return;
        }

        int y = MARGIN_TOP + headerHeight + textRenderer.fontHeight + 6;

        for (GuideSysGuiManager.AdvancementGuideItem guide : guides) {
            // 修改：计算项的宽度 (图标 + 标题 + 进度百分比 + 状态图标)
            int titleWidth = textRenderer.getWidth(guide.getTitle());
            String progressText = (int) guide.getProgress() + "%";
            int progressTextWidth = textRenderer.getWidth(progressText);
            int itemWidth = ICON_SIZE + ITEM_PADDING + titleWidth + ITEM_PADDING + progressTextWidth + ITEM_PADDING + ICON_SIZE;

            // 计算x起始位置（右对齐）
            int x = screenWidth - MARGIN_RIGHT - itemWidth;

            // 绘制半透明背景
            int backgroundColor = 0x80000000; // 半透明黑色
            context.fill(x, y, screenWidth - MARGIN_RIGHT, y + ITEM_HEIGHT, backgroundColor);

            // 绘制成就图标
            context.drawItem(guide.getIcon(), x, y + (ITEM_HEIGHT - ICON_SIZE) / 2);

            // 绘制标题
            int titleX = x + ICON_SIZE + ITEM_PADDING;
            int titleY = y + (ITEM_HEIGHT - textRenderer.fontHeight) / 2;
            context.drawText(textRenderer, guide.getTitle(), titleX, titleY, 0xFFFFFFFF, true);

            // 修改：绘制进度百分比文本
            int progressTextX = titleX + titleWidth + ITEM_PADDING;
            int progressTextY = y + (ITEM_HEIGHT - textRenderer.fontHeight) / 2;
            int progressColor = guide.isCompleted() ? 0xFF55FF55 : 0xFF5555FF; // 完成绿色，进行中蓝色
            context.drawText(textRenderer, progressText, progressTextX, progressTextY, progressColor, true);

            // 绘制状态图标
            int statusX = progressTextX + progressTextWidth + ITEM_PADDING;
            int statusY = y + (ITEM_HEIGHT - ICON_SIZE) / 2;

            Identifier statusIcon;
            if (guide.isCompleted()) {
                statusIcon = PROGRESS_COMPLETE;
            } else if (guide.getProgress() > 0) {
                statusIcon = PROGRESS_IN_PROGRESS;
            } else {
                statusIcon = PROGRESS_PENDING;
            }

            context.drawTexture(RenderLayer::getGuiTextured, statusIcon, statusX, statusY, 0, 0, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);

            // 修改：在列表项底部绘制1像素高的进度条
            int progressBarY = y + ITEM_HEIGHT - 1;
            int progressBarWidth = screenWidth - MARGIN_RIGHT - x;

            // 进度条背景
            context.fill(x, progressBarY, screenWidth - MARGIN_RIGHT, progressBarY + 1, 0xFF555555);

            // 进度条填充
            int fillWidth = (int) (progressBarWidth * (MathHelper.clamp(guide.getProgress(), 0, 100) / 100.0f));
            context.fill(x, progressBarY, x + fillWidth, progressBarY + 1, progressColor);

            // 如果显示所有描述且该项有描述，则绘制描述
            if (showAllDescriptions && guide.getDescription() != null) {
                int descriptionWidth = Math.min(200, textRenderer.getWidth(guide.getDescription()));
                int descriptionHeight = textRenderer.wrapLines(guide.getDescription(), descriptionWidth).size() * textRenderer.fontHeight;

                y += ITEM_HEIGHT;

                // 描述背景
                context.fill(x + DESCRIPTION_PADDING, y, screenWidth - MARGIN_RIGHT - DESCRIPTION_PADDING, y + descriptionHeight + ITEM_PADDING * 2, 0xA0000000);

                // 描述文本
                context.drawText(textRenderer, guide.getDescription(), x + ITEM_PADDING * 2, y + ITEM_PADDING, 0xFFFFFFFF, true);

                y += descriptionHeight + ITEM_PADDING * 2;
            } else {
                y += ITEM_HEIGHT;
            }

            // 项之间的间隙
            y += 2;
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