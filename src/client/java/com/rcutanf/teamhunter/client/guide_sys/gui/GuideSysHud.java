package com.rcutanf.teamhunter.client.guide_sys.gui;

import com.rcutanf.teamhunter.client.config.GuideSysConfig;
import com.rcutanf.teamhunter.client.guide_sys.gui.data.GuideCondition;
import com.rcutanf.teamhunter.client.guide_sys.gui.data.GuideData;
import com.rcutanf.teamhunter.client.guide_sys.gui.widget.ScrollingCarouselText;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.OrderedText;
import net.minecraft.text.StringVisitable;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

import java.util.List;

public class GuideSysHud {
    private static final int ITEM_HEIGHT = 20;
    private static final int ITEM_PADDING = 3;
    private static final int DESCRIPTION_PADDING = 8;
    private static final int ICON_SIZE = 16;

    private static ScrollingCarouselText hintTextWidget;
    private static boolean showAllDescriptions = false;

    // 进度图标资源
    private static final Identifier PROGRESS_FULL_PROGRESSED = Identifier.of("teamhunter", "textures/ui/guide/progress_full_progressed.png");
    private static final Identifier PROGRESS_IN_PROGRESS = Identifier.of("teamhunter", "textures/ui/guide/progress_in_progress.png");
    private static final Identifier PROGRESS_COMPLETE = Identifier.of("teamhunter", "textures/ui/guide/progress_complete.png");

    private static final GuideSystemDataManager guideManager = GuideSystemDataManager.getInstance();

    private GuideSysHud() {}

    private static void initializeHintWidget() {
        GuideSysConfig config = GuideSysConfig.getInstance();
        hintTextWidget = new ScrollingCarouselText(
                "按下TAB展开进度描述",
                "按下Tab+Ctrl可打开详情界面"
        ).setTextColor(config.getTextColor())
         .setBackgroundColor(config.getBackgroundColor())
         .setSwitchInterval(config.getHintSwitchInterval())
         .setScrollSpeed(config.getHintScrollSpeed());
    }

    public static void render(DrawContext context, RenderTickCounter tickDelta) {
        GuideSysConfig config = GuideSysConfig.getInstance();

        // 如果未启用，直接返回
        if (!config.isEnabled()) return;

        // 初始化提示组件（如果需要）
        if (hintTextWidget == null) {
            initializeHintWidget();
        }

        MinecraftClient client = MinecraftClient.getInstance();
        List<GuideData> guides = guideManager.getIncompleteGuides();

        // 限制显示的项目数量
        if (guides.size() > config.getMaxItems()) {
            guides = guides.subList(0, config.getMaxItems());
        }

        TextRenderer textRenderer = client.textRenderer;
        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();

        // 尝试使用配置的宽度
        int maxWidth = Math.max(config.getWidth(), 140);
        if (!guides.isEmpty()) {
            for (GuideData guide : guides) {
                int titleWidth = (int) (textRenderer.getWidth(guide.getTitle()) * config.getTextScale());
                String progressText = guide.getProgress() + "%";
                int progressTextWidth = (int) (textRenderer.getWidth(progressText) * config.getTextScale());
                int itemWidth = (int) (ICON_SIZE * config.getTextScale()) + ITEM_PADDING * 3 + titleWidth + progressTextWidth + (int) (ICON_SIZE * config.getTextScale());
                maxWidth = Math.max(maxWidth, itemWidth);
            }
        }

        // 确保表头文本也能适应
        String headerText = "进度列表";
        int headerTextWidth = (int) (textRenderer.getWidth(headerText) * config.getTextScale()) + ITEM_PADDING * 6;
        maxWidth = Math.max(maxWidth, headerTextWidth);

        int itemWidth = Math.min(maxWidth, config.getWidth());

        // 使用配置中的位置
        int x, y;
        if (config.getPosX() + itemWidth > screenWidth) {
            x = screenWidth - itemWidth - 8; // 防止超出屏幕
        } else {
            x = config.getPosX();
        }

        if (config.getPosY() > screenHeight - 100) {
            y = screenHeight - 100; // 防止超出屏幕
        } else {
            y = config.getPosY();
        }

        // 绘制表头
        int headerHeight = (int) (20 * config.getTextScale());
        int headerX = x;

        // 表头背景 - 根据配置决定是否显示
        if (config.isShowBackground()) {
            context.fill(headerX, y, x + itemWidth, y + headerHeight, 0xA0204080);
            // 表头边框
            context.fill(headerX, y + headerHeight - 1, x + itemWidth, y + headerHeight, 0xFF4080FF);
        }

        // 表头文本 - 应用文字缩放
        context.getMatrices().pushMatrix();
        context.getMatrices().scale(config.getTextScale(), config.getTextScale());

        int scaledHeaderX = (int) ((headerX + (itemWidth - textRenderer.getWidth(headerText) * config.getTextScale()) / 2) / config.getTextScale());
        int scaledHeaderY = (int) ((y + (headerHeight - textRenderer.fontHeight * config.getTextScale()) / 2) / config.getTextScale());

        context.drawText(textRenderer, headerText, scaledHeaderX, scaledHeaderY, 0xFFFFFFFF, true);
        context.getMatrices().popMatrix();

        y += headerHeight;

        // 提示文本区域
        if (config.isEnableHintCarousel()) {
            int hintHeight = (int) ((textRenderer.fontHeight + 2) * config.getTextScale());

            // 更新提示组件的配置
            hintTextWidget.setTextColor(config.getTextColor())
                          .setBackgroundColor(config.getBackgroundColor())
                          .setSwitchInterval(config.getHintSwitchInterval())
                          .setScrollSpeed(config.getHintScrollSpeed())
                          .setDrawBackground(config.isShowBackground());

            // 渲染提示文本
            context.getMatrices().pushMatrix();
            context.getMatrices().scale(config.getTextScale(), config.getTextScale());

            int scaledHintX = (int) (headerX / config.getTextScale());
            int scaledHintY = (int) (y / config.getTextScale());
            int scaledHintWidth = (int) (itemWidth / config.getTextScale());
            int scaledHintHeight = (int) (hintHeight / config.getTextScale());

            hintTextWidget.render(context, textRenderer, scaledHintX, scaledHintY, scaledHintWidth, scaledHintHeight);
            context.getMatrices().popMatrix();

            y += hintHeight;
        }

        // 如果没有指南项，显示提示信息
        if (guides.isEmpty()) {
            String emptyText = "暂无进度";
            context.getMatrices().pushMatrix();
            context.getMatrices().scale(config.getTextScale(), config.getTextScale());

            int scaledEmptyX = (int) ((headerX + (itemWidth - textRenderer.getWidth(emptyText) * config.getTextScale()) / 2) / config.getTextScale());
            int scaledEmptyY = (int) (y / config.getTextScale());

            context.drawText(textRenderer, emptyText, scaledEmptyX, scaledEmptyY, 0xFFAAAAAA, true);
            context.getMatrices().popMatrix();
            return;
        }

        int itemIndex = 0;
        int scaledItemHeight = (int) (ITEM_HEIGHT * config.getTextScale());

        for (GuideData guide : guides) {
            // 交替背景色增强可读性
            int backgroundColor = (itemIndex % 2 == 0) ?
                (config.getBackgroundColor() & 0x00FFFFFF) | 0x90202020 :
                (config.getBackgroundColor() & 0x00FFFFFF) | 0x90303030;

            // 绘制背景（如果启用）
            if (config.isShowBackground()) {
                context.fill(x, y, x + itemWidth, y + scaledItemHeight, backgroundColor);
                // 绘制边框线
                context.fill(x, y + scaledItemHeight - 1, x + itemWidth, y + scaledItemHeight, 0x40FFFFFF);
            }

            // 应用文字缩放
            context.getMatrices().pushMatrix();
            context.getMatrices().scale(config.getTextScale(), config.getTextScale());

            // 绘制成就图标
            int scaledIconX = (int) ((x + ITEM_PADDING) / config.getTextScale());
            int scaledIconY = (int) ((y + (scaledItemHeight - ICON_SIZE * config.getTextScale()) / 2) / config.getTextScale());
            context.drawItem(guide.getIcon(), scaledIconX, scaledIconY);

            // 绘制标题
            int scaledTitleX = (int) ((x + ICON_SIZE * config.getTextScale() + ITEM_PADDING * 2) / config.getTextScale());
            int scaledTitleY = (int) ((y + (scaledItemHeight - textRenderer.fontHeight * config.getTextScale()) / 2 + 1) / config.getTextScale());
            context.drawText(textRenderer, guide.getTitle(), scaledTitleX, scaledTitleY, config.getTextColor(), true);

            // 绘制进度百分比文本
            String progressText = guide.getProgress() + "%";
            int progressTextWidth = textRenderer.getWidth(progressText);
            int scaledProgressTextX = (int) ((x + itemWidth - ICON_SIZE * config.getTextScale() - ITEM_PADDING * 2) / config.getTextScale()) - progressTextWidth;
            int scaledProgressTextY = scaledTitleY;
            int progressColor = guide.isCompleted() ? 0xFF55FF55 : (guide.getProgress() > 0 ? 0xFF55AAFF : 0xFFAAAAAA);
            context.drawText(textRenderer, progressText, scaledProgressTextX, scaledProgressTextY, progressColor, true);

            context.getMatrices().popMatrix();

            // 绘制状态图标
            int statusX = x + itemWidth - (int) (ICON_SIZE * config.getTextScale()) - ITEM_PADDING;
            int statusY = y + (scaledItemHeight - (int) (ICON_SIZE * config.getTextScale())) / 2;

            Identifier statusIcon;
            if (guide.isCompleted()) {
                statusIcon = PROGRESS_COMPLETE;
            } else if (guide.getProgress() >= 0 && guide.getProgress() < 100) {
                statusIcon = PROGRESS_IN_PROGRESS;
            } else if (guide.getProgress() == 100) {
                statusIcon = PROGRESS_FULL_PROGRESSED;
            } else {
                statusIcon = PROGRESS_IN_PROGRESS;
            }

            int scaledIconSize = (int) (ICON_SIZE * config.getTextScale());
            context.drawTexture(RenderPipelines.GUI_TEXTURED, statusIcon, statusX, statusY, 0, 0,
                               scaledIconSize, scaledIconSize, scaledIconSize, scaledIconSize);

            // 绘制进度条（2像素高，按缩放调整）
            int progressBarHeight = Math.max(1, (int) (2 * config.getTextScale()));
            int progressBarY = y + scaledItemHeight - progressBarHeight;

            if (config.isShowBackground()) {
                // 进度条背景
                context.fill(x, progressBarY, x + itemWidth, progressBarY + progressBarHeight, 0xFF404040);
                // 进度条填充
                int fillWidth = (int) (itemWidth * (MathHelper.clamp(guide.getProgress(), 0, 100) / 100.0f));
                if (fillWidth > 0) {
                    context.fill(x, progressBarY, x + fillWidth, progressBarY + progressBarHeight, progressColor);
                }
            }

            // 处理描述显示（如果启用且有描述）
            if (showAllDescriptions && guide.getDescription() != null) {
                y += scaledItemHeight;

                context.getMatrices().pushMatrix();
                context.getMatrices().scale(config.getTextScale(), config.getTextScale());

                // 自动换行处理
                int scaledWidth = (int) ((itemWidth - 6) / config.getTextScale());
                List<OrderedText> lines = textRenderer.wrapLines(guide.getDescription(), scaledWidth);
                int descriptionHeight = (int) ((lines.size() * (textRenderer.fontHeight + 1) + 4) * config.getTextScale());

                // 描述背景
                if (config.isShowBackground()) {
                    context.fill(x, y, x + itemWidth, y + descriptionHeight, config.getBackgroundColor());
                }

                // 描述文本
                int scaledTextY = (int) ((y + 2) / config.getTextScale());
                for (OrderedText line : lines) {
                    context.drawText(textRenderer, line, (int) ((x + 3) / config.getTextScale()), scaledTextY, 0xFFAA8800, true);
                    scaledTextY += textRenderer.fontHeight + 1;
                }

                context.getMatrices().popMatrix();
                y += descriptionHeight;

                // 条件描述处理
                List<GuideCondition> guideConditions = guide.getConditions();
                if (guideConditions != null) {
                    for (int i = 0; i < guideConditions.size(); i++) {
                        var condition = guideConditions.get(i);
                        String conditionDesc = condition.getDescription();
                        if (conditionDesc != null && !conditionDesc.isEmpty()) {
                            context.getMatrices().pushMatrix();
                            context.getMatrices().scale(config.getTextScale(), config.getTextScale());

                            List<OrderedText> condLines = textRenderer.wrapLines(StringVisitable.plain(conditionDesc), (int) ((itemWidth - 6 - 20) / config.getTextScale()));
                            int condHeight = (int) ((condLines.size() * (textRenderer.fontHeight + 1) + 4) * config.getTextScale());

                            // 使用条件的内部进度来渲染整个背景
                            float insideProgress = condition.getProgress();
                            int condFillWidth = (int) (itemWidth * (MathHelper.clamp(insideProgress, 0, 100) / 100.0f));

                            // 背景颜色 - 完成时为亮绿色，未完成时为淡蓝色
                            int bgColor = condition.isCompleted() ? 0xA0005500 : 0xA03366AA;

                            // 绘制整个条件区域的背景
                            if (config.isShowBackground()) {
                                context.fill(x, y, x + condFillWidth, y + condHeight, bgColor);
                                // 未填充部分使用较暗的颜色
                                if (condFillWidth < itemWidth) {
                                    context.fill(x + condFillWidth, y, x + itemWidth, y + condHeight, 0x60202020);
                                }
                            }

                            // 绘制复选框边框
                            int checkboxSize = (int) (10 * config.getTextScale());
                            int checkboxX = x + 3;
                            int checkboxY = y + 2;
                            context.fill(checkboxX, checkboxY, checkboxX + checkboxSize, checkboxY + checkboxSize, 0xFF808080);
                            context.fill(checkboxX + 1, checkboxY + 1, checkboxX + checkboxSize - 1, checkboxY + checkboxSize - 1,
                                        condition.isCompleted() ? 0xFF005500 : 0xFF202020);

                            // 如果完成，绘制勾号
                            if (condition.isCompleted()) {
                                String checkMark = "✔";
                                int scaledCheckMarkX = (int) ((checkboxX + 1) / config.getTextScale());
                                int scaledCheckMarkY = (int) ((checkboxY + 1) / config.getTextScale());
                                context.drawText(textRenderer, checkMark, scaledCheckMarkX, scaledCheckMarkY, 0xFF55FF55, true);
                            }

                            // 条件文本
                            int condTextColor = condition.isCompleted() ? 0xFF55FF55 : 0xFFAAAAAA;
                            int scaledCondTextY = (int) ((y + 2) / config.getTextScale());
                            for (OrderedText line : condLines) {
                                context.drawText(textRenderer, line, (int) ((x + 3 + checkboxSize + 2) / config.getTextScale()),
                                               scaledCondTextY, condTextColor, true);
                                scaledCondTextY += textRenderer.fontHeight + 1;
                            }

                            context.getMatrices().popMatrix();
                            y += condHeight;
                        }
                    }
                }
            } else {
                y += scaledItemHeight;
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

    /**
     * 动画操作：设置进度图标为progress_complete
     */
    public static void animateProgressComplete() {
        // 动画逻辑
    }

    /**
     * 重新加载配置时调用，重新初始化组件
     */
    public static void reloadConfig() {
        hintTextWidget = null; // 强制重新初始化
    }
}