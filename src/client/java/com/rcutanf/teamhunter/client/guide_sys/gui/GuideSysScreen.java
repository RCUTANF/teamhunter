package com.rcutanf.teamhunter.client.guide_sys.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ScrollableWidget;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.screen.narration.NarrationPart;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public class GuideSysScreen extends Screen {
    private static final int ITEM_HEIGHT = 30;
    private static final int ITEM_WIDTH = 350;
    private static final int PADDING = 10;
    private static final int SCROLL_SPEED = 20;

    private final Screen parent;
    private GuideListWidget leftList;  // 未完成列表
    private GuideListWidget rightList; // 已完成列表

    public GuideSysScreen(Screen parent) {
        super(Text.of("指南系统"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();

        int listWidth = (this.width - PADDING * 3) / 2;
        int listHeight = this.height - 40 - PADDING * 2; // 留出按钮空间

        // 分离指南项
        List<GuideSysGuiManager.AdvancementGuideItem> allGuides = GuideSysGuiManager.getAdvancementGuides();
        List<GuideSysGuiManager.AdvancementGuideItem> incompleteGuides = new ArrayList<>();
        List<GuideSysGuiManager.AdvancementGuideItem> completeGuides = new ArrayList<>();

        for (GuideSysGuiManager.AdvancementGuideItem guide : allGuides) {
            if (guide.isCompleted()) {
                completeGuides.add(guide);
            } else {
                incompleteGuides.add(guide);
            }
        }

        // 创建左右列表
        this.leftList = new GuideListWidget(
                this.client,
                listWidth,
                listHeight,
                PADDING,
                PADDING + 30,
                ITEM_HEIGHT,
                incompleteGuides,
                false // 未完成列表
        );
        this.addDrawableChild(this.leftList);

        this.rightList = new GuideListWidget(
                this.client,
                listWidth,
                listHeight,
                this.width - listWidth - PADDING,
                PADDING + 30,
                ITEM_HEIGHT,
                completeGuides,
                true // 已完成列表
        );
        this.addDrawableChild(this.rightList);

        // 返回按钮
        this.addDrawableChild(ButtonWidget.builder(ScreenTexts.DONE, button -> {
                    assert this.client != null;
                    this.client.setScreen(this.parent);
                })
                .dimensions(this.width / 2 - 75, this.height - 30, 150, 20)
                .build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        // 绘制标题
        context.drawCenteredTextWithShadow(this.textRenderer, "指南系统", this.width / 2, 10, 0xFFFFFF);
        context.drawCenteredTextWithShadow(this.textRenderer, "未完成", PADDING + (this.leftList.getWidth()) / 2, 35, 0xFFAAAAAA);
        context.drawCenteredTextWithShadow(this.textRenderer, "已完成", this.width - (this.rightList.getWidth()) / 2, 35, 0x55FF55);
    }

    @Override
    public void close() {
        assert this.client != null;
        this.client.setScreen(this.parent);
    }

    // 自定义滚动列表
    private static class GuideListWidget extends ScrollableWidget {
        private final List<GuideEntry> entries = new ArrayList<>();
        private final boolean isCompletedList;
        private final List<GuideSysGuiManager.AdvancementGuideItem> guides;
        //private double scrollAmount = 0;

        public GuideListWidget(MinecraftClient client, int width, int height, int x, int y, int itemHeight,
                               List<GuideSysGuiManager.AdvancementGuideItem> guides, boolean isCompletedList) {
            super(x, y, width, height, Text.empty());
            this.guides = guides;
            this.isCompletedList = isCompletedList;
            this.updateEntries();
        }

        private void updateEntries() {
            this.entries.clear();
            for (GuideSysGuiManager.AdvancementGuideItem guide : this.guides) {
                this.entries.add(new GuideEntry(guide, !this.isCompletedList)); // 只有未完成列表的项目需要展开功能
            }
        }

        protected int getContentsHeight() {
            int height = 0;
            for (GuideEntry entry : this.entries) {
                height += entry.getHeight();
            }
            return height;
        }

        @Override
        protected int getContentsHeightWithPadding() {
            return getContentsHeight() + 20;
        }

        @Override
        protected double getDeltaYPerScroll() {
            return SCROLL_SPEED;
        }

        @Override
        public void appendClickableNarrations(NarrationMessageBuilder builder) {
            // 为无障碍功能提供叙述信息
            builder.put(NarrationPart.TITLE, this.isCompletedList ? "已完成指南列表" : "未完成指南列表");
        }

        protected void renderBackground(DrawContext context) {
            // 绘制背景
            context.fill(this.getX(), this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight(), 0x80000000);
        }

        @Override
        protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
            this.renderBackground(context);
            int currentY = (int) (this.getY() - this.getScrollY());

            // 启用裁剪以防止内容绘制到边界外
            context.enableScissor(this.getX(), this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight());

            for (GuideEntry entry : this.entries) {
                if (currentY + entry.getHeight() >= this.getY() && currentY <= this.getY() + this.getHeight()) {
                    entry.render(context, this.getX(), currentY, this.getWidth(), mouseX, mouseY, delta);
                }
                currentY += entry.getHeight();
            }

            context.disableScissor();
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            int currentY = (int) (this.getY() - this.getScrollY());
            for (GuideEntry entry : this.entries) {
                if (mouseX >= this.getX() && mouseX < this.getX() + this.getWidth() &&
                        mouseY >= currentY && mouseY < currentY + entry.getHeight()) {
                    return entry.mouseClicked(mouseX, mouseY, button, currentY);
                }
                currentY += entry.getHeight();
            }
            return super.mouseClicked(mouseX, mouseY, button);
        }

        private class GuideEntry {
            private final GuideSysGuiManager.AdvancementGuideItem guide;
            private final boolean needsExpandToggle;
            private boolean expanded = false;

            public GuideEntry(GuideSysGuiManager.AdvancementGuideItem guide, boolean needsExpandToggle) {
                this.guide = guide;
                this.needsExpandToggle = needsExpandToggle;
            }

            public int getHeight() {
                int baseHeight = ITEM_HEIGHT;
                if (this.expanded && this.needsExpandToggle) {
                    baseHeight += getDescriptionHeight();
                    if (this.guide.getAchievementChecker() != null) {
                        for (var condition : this.guide.getAchievementChecker().conditions) {
                            String conditionDesc = condition.description;
                            if (conditionDesc != null && !conditionDesc.isEmpty()) {
                                baseHeight += getWrappedLinesHeight(conditionDesc);
                            }
                        }
                    }
                }
                return baseHeight;
            }

            private int getDescriptionHeight() {
                if (this.guide.getDescription() == null) return 0;
                return getWrappedLinesHeight(this.guide.getDescription().getString());
            }

            private int getWrappedLinesHeight(String text) {
                if (text == null || text.isEmpty()) return 0;
                TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
                List<String> lines = wrapText(text, ITEM_WIDTH - 20); // 预留边距
                return lines.size() * (textRenderer.fontHeight + 2) + 4; // 加上内边距
            }

            private List<String> wrapText(String text, int maxWidth) {
                TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
                List<String> lines = new ArrayList<>();
                String[] words = text.split(" ");
                StringBuilder currentLine = new StringBuilder();

                for (String word : words) {
                    String testLine = currentLine.length() > 0 ? currentLine + " " + word : word;
                    if (textRenderer.getWidth(testLine) <= maxWidth) {
                        currentLine = new StringBuilder(testLine);
                    } else {
                        if (currentLine.length() > 0) {
                            lines.add(currentLine.toString());
                            currentLine = new StringBuilder(word);
                        } else {
                            // Word too long, force split
                            lines.add(word);
                        }
                    }
                }
                if (currentLine.length() > 0) {
                    lines.add(currentLine.toString());
                }
                return lines;
            }

            public boolean mouseClicked(double mouseX, double mouseY, int button, int entryY) {
                if (this.needsExpandToggle) {
                    int toggleX = GuideListWidget.this.getX() + 5;
                    int toggleY = entryY;
                    if (mouseX >= toggleX && mouseX < toggleX + 15 &&
                            mouseY >= toggleY && mouseY < toggleY + ITEM_HEIGHT) {
                        this.expanded = !this.expanded;
                        return true;
                    }
                }
                return false;
            }

            public void render(DrawContext context, int x, int y, int width, int mouseX, int mouseY, float delta) {
                TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;

                // 绘制背景
                int bgColor = this.guide.isCompleted() ? 0x60005500 : 0x60202020;
                context.fill(x, y, x + width, y + ITEM_HEIGHT, bgColor);

                // 绘制展开/收起按钮（仅对未完成项）
                if (this.needsExpandToggle) {
                    String toggleSymbol = this.expanded ? "-" : "+";
                    context.drawText(textRenderer, toggleSymbol, x + 5, y + (ITEM_HEIGHT - textRenderer.fontHeight) / 2, 0xFFFFFFFF, true);
                }

                // 绘制标题
                int titleColor = this.guide.isCompleted() ? 0x55FF55 : 0xFFFFFF;
                String title = this.guide.getTitle().getString();
                if (textRenderer.getWidth(title) > width - 40) {
                    title = textRenderer.trimToWidth(title, width - 40 - textRenderer.getWidth("...")) + "...";
                }
                context.drawText(textRenderer, title, x + (this.needsExpandToggle ? 20 : 10), y + (ITEM_HEIGHT - textRenderer.fontHeight) / 2, titleColor, true);

                // 绘制进度
                String progressText = (int) this.guide.getProgress() + "%";
                int progressTextWidth = textRenderer.getWidth(progressText);
                context.drawText(textRenderer, progressText, x + width - progressTextWidth - 5, y + (ITEM_HEIGHT - textRenderer.fontHeight) / 2, titleColor, true);

                // 如果展开且是未完成项，则绘制描述和条件
                if (this.expanded && this.needsExpandToggle) {
                    int currentY = y + ITEM_HEIGHT;

                    // 绘制描述
                    if (this.guide.getDescription() != null) {
                        List<String> descLines = wrapText(this.guide.getDescription().getString(), width - 20);
                        int descBgY = currentY;
                        int descHeight = descLines.size() * (textRenderer.fontHeight + 2) + 4;

                        context.fill(x, descBgY, x + width, descBgY + descHeight, 0x80000000);

                        int textY = descBgY + 2;
                        for (String line : descLines) {
                            context.drawText(textRenderer, line, x + 10, textY, 0xFFAA8800, true);
                            textY += textRenderer.fontHeight + 2;
                        }
                        currentY += descHeight;
                    }

                    // 绘制条件描述
                    if (this.guide.getAchievementChecker() != null) {
                        for (var condition : this.guide.getAchievementChecker().conditions) {
                            String conditionDesc = condition.description;
                            if (conditionDesc != null && !conditionDesc.isEmpty()) {
                                List<String> condLines = wrapText(conditionDesc, width - 20);
                                int condBgY = currentY;
                                int condHeight = condLines.size() * (textRenderer.fontHeight + 2) + 4;

                                // 条件背景 - 根据完成状态
                                int condBgColor = condition.isCompleted() ? 0x80005500 : 0x80333333;
                                context.fill(x, condBgY, x + width, condBgY + condHeight, condBgColor);

                                // 绘制复选框
                                int checkboxSize = 10;
                                int checkboxX = x + 5;
                                int checkboxY = condBgY + 2;
                                context.fill(checkboxX, checkboxY, checkboxX + checkboxSize, checkboxY + checkboxSize, 0xFF808080);
                                context.fill(checkboxX + 1, checkboxY + 1, checkboxX + checkboxSize - 1, checkboxY + checkboxSize - 1, condition.isCompleted() ? 0xFF005500 : 0xFF202020);

                                // 绘制勾号
                                if (condition.isCompleted()) {
                                    String checkMark = "✔";
                                    context.drawText(textRenderer, checkMark, checkboxX + 1, checkboxY + 1, 0xFF55FF55, true);
                                }

                                // 条件文本
                                int condTextColor = condition.isCompleted() ? 0xFF55FF55 : 0xFFAAAAAA;
                                int textY = condBgY + 2;
                                // 在描述末尾添加进度信息
                                String conditionTextWithProgress = conditionDesc + " (" + condition.getInsideProgress() + "% of " + condition.getMaxProgress() + "%)";
                                List<String> condLinesWithProgress = wrapText(conditionTextWithProgress, width - 20);

                                for (String line : condLinesWithProgress) {
                                    context.drawText(textRenderer, line, x + 5 + checkboxSize + 2, textY, condTextColor, true);
                                    textY += textRenderer.fontHeight + 2;
                                }

                                currentY += condHeight;
                            }
                        }
                    }
                }
            }
        }
    }
}