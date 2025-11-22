package com.rcutanf.teamhunter.client.guide_sys.gui;

import com.rcutanf.teamhunter.client.guide_sys.advancementListener.AdvancementEventManager;
import com.rcutanf.teamhunter.client.guide_sys.advancementListener.AdvancementRecord;
import net.minecraft.advancement.PlacedAdvancement;
import net.minecraft.advancement.AdvancementEntry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ScrollableWidget;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.screen.narration.NarrationPart;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public class GuideSysScreen extends Screen {
    private static final int ITEM_HEIGHT = 30;
    private static final int ITEM_WIDTH = 350;
    private static final int PADDING = 10;
    private static final int SCROLL_SPEED = 20;

    private final Screen parent;
    private GuideListWidget leftList;  // 未完成列表
    private ScrollableWidget rightList; // 已完成列表

    public GuideSysScreen(Screen parent) {
        super(Text.of("指南系统"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();

        int listWidth = (this.width - PADDING * 3) / 2;
        int listHeight = this.height - 40 - PADDING * 2;

        // 获取未完成的指南项（保持原来的方式）
        List<GuideSysGuiManager.AdvancementGuideItem> allGuides = GuideSysGuiManager.getAdvancementGuides();
        List<GuideSysGuiManager.AdvancementGuideItem> incompleteGuides = new ArrayList<>();

        for (GuideSysGuiManager.AdvancementGuideItem guide : allGuides) {
            if (!guide.isCompleted()) {
                incompleteGuides.add(guide);
            }
        }

        // 获取已完成的成就记录（新的数据源）
        List<AdvancementRecord> completedRecords = AdvancementEventManager.getInstance().getAdvancementRecords();
        List<CompletedGuideItem> completeGuides = new ArrayList<>();

        for (AdvancementRecord record : completedRecords) {
            // 直接从游戏成就管理器获取成就信息
            PlacedAdvancement placedAdvancement = getPlacedAdvancement(record.getAdvancementId().toString());
            if (placedAdvancement != null) {
                completeGuides.add(new CompletedGuideItem(placedAdvancement.getAdvancementEntry(), record));
            }
        }

        // 按完成时间排序（游戏内时间）
        completeGuides.sort((a, b) -> Long.compare(a.record.getGameTime(), b.record.getGameTime()));

        // 创建左右列表
        this.leftList = new GuideListWidget(
                this.client,
                listWidth,
                listHeight,
                PADDING,
                PADDING + 30,
                ITEM_HEIGHT,
                incompleteGuides,
                false
        );
        this.addDrawableChild(this.leftList);

        this.rightList = new CompletedGuideListWidget(
                this.client,
                listWidth,
                listHeight,
                this.width - listWidth - PADDING,
                PADDING + 30,
                ITEM_HEIGHT,
                completeGuides
        );
        this.addDrawableChild(this.rightList);

        // 右上角关闭按钮
        int closeButtonSize = 20;
        this.addDrawableChild(ButtonWidget.builder(Text.of("×"), button -> {
                    assert this.client != null;
                    this.client.setScreen(this.parent);
                })
                .dimensions(this.width - closeButtonSize - 5, 5, closeButtonSize, closeButtonSize)
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
                this.entries.add(new GuideEntry(guide, !this.isCompletedList));
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
            builder.put(NarrationPart.TITLE, this.isCompletedList ? "已完成指南列表" : "未完成指南列表");
        }

        protected void renderBackground(DrawContext context) {
            context.fill(this.getX(), this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight(), 0x80000000);
        }

        @Override
        protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
            this.renderBackground(context);
            int currentY = (int) (this.getY() - this.getScrollY());

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
        public boolean mouseClicked(Click click, boolean doubled) {
            // 先处理列表项的点击
            double mouseX = click.x();
            double mouseY = click.y();
            int button = click.buttonInfo().button();

            // 检查点击是否在列表区域内
            if (mouseX >= this.getX() && mouseX < this.getX() + this.getWidth() &&
                    mouseY >= this.getY() && mouseY < this.getY() + this.getHeight()) {

                // 计算相对于列表内容的Y坐标
                int relativeY = (int) (mouseY - this.getY() + this.getScrollY());
                int currentY = 0;

                for (GuideEntry entry : this.entries) {
                    int entryHeight = entry.getHeight();

                    // 检查点击是否在当前条目内
                    if (relativeY >= currentY && relativeY < currentY + entryHeight) {
                        // 将坐标转换为条目内的相对坐标
                        int entryRelativeY = relativeY - currentY;

                        // 调用条目的点击处理，传递条目内的相对坐标
                        if (entry.mouseClicked(mouseX - this.getX(), entryRelativeY, button)) {
                            return true; // 如果条目处理了点击，直接返回
                        }
                    }
                    currentY += entryHeight;
                }
            }

            // 如果条目没有处理点击，再交给父类处理（滚动条等）
            return super.mouseClicked(click, doubled);
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
                List<String> lines = wrapText(text, ITEM_WIDTH - 20);
                return lines.size() * (textRenderer.fontHeight + 2) + 4;
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
                            lines.add(word);
                        }
                    }
                }
                if (currentLine.length() > 0) {
                    lines.add(currentLine.toString());
                }
                return lines;
            }

            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                if (this.needsExpandToggle) {
                    // 现在 mouseX 和 mouseY 都是相对于条目左上角的坐标
                    int toggleX = 5;  // toggle 按钮相对于条目的X位置
                    int toggleY = 0;  // toggle 按钮相对于条目的Y位置
                    int toggleWidth = 15;
                    int toggleHeight = ITEM_HEIGHT;

                    if (mouseX >= toggleX && mouseX < toggleX + toggleWidth &&
                            mouseY >= toggleY && mouseY < toggleY + toggleHeight) {
                        this.expanded = !this.expanded;
                        return true;
                    }
                }
                return false;
            }


            public void render(DrawContext context, int x, int y, int width, int mouseX, int mouseY, float delta) {
                TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;

                int bgColor = this.guide.isCompleted() ? 0x60005500 : 0x60202020;
                context.fill(x, y, x + width, y + ITEM_HEIGHT, bgColor);

                if (this.needsExpandToggle) {
                    String toggleSymbol = this.expanded ? "-" : "+";
                    context.drawText(textRenderer, toggleSymbol, x + 5, y + (ITEM_HEIGHT - textRenderer.fontHeight) / 2, 0xFFFFFFFF, true);
                }

                int titleColor = this.guide.isCompleted() ? 0xFF55FF55 : 0xFFFFFFFF;
                String title = this.guide.getTitle().getString();
                if (textRenderer.getWidth(title) > width - 40) {
                    title = textRenderer.trimToWidth(title, width - 40 - textRenderer.getWidth("...")) + "...";
                }
                context.drawText(textRenderer, title, x + (this.needsExpandToggle ? 20 : 10), y + (ITEM_HEIGHT - textRenderer.fontHeight) / 2, titleColor, true);

                String progressText = (int) this.guide.getProgress() + "%";
                int progressTextWidth = textRenderer.getWidth(progressText);
                context.drawText(textRenderer, progressText, x + width - progressTextWidth - 5, y + (ITEM_HEIGHT - textRenderer.fontHeight) / 2, titleColor, true);

                if (this.expanded && this.needsExpandToggle) {
                    int currentY = y + ITEM_HEIGHT;

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

                    if (this.guide.getAchievementChecker() != null) {
                        for (var condition : this.guide.getAchievementChecker().conditions) {
                            String conditionDesc = condition.description;
                            if (conditionDesc != null && !conditionDesc.isEmpty()) {
                                List<String> condLines = wrapText(conditionDesc, width - 20);
                                int condBgY = currentY;
                                int condHeight = condLines.size() * (textRenderer.fontHeight + 2) + 4;

                                int condBgColor = condition.isCompleted() ? 0x80005500 : 0x80333333;
                                context.fill(x, condBgY, x + width, condBgY + condHeight, condBgColor);

                                int checkboxSize = 10;
                                int checkboxX = x + 5;
                                int checkboxY = condBgY + 2;
                                context.fill(checkboxX, checkboxY, checkboxX + checkboxSize, checkboxY + checkboxSize, 0xFF808080);
                                context.fill(checkboxX + 1, checkboxY + 1, checkboxX + checkboxSize - 1, checkboxY + checkboxSize - 1, condition.isCompleted() ? 0xFF005500 : 0xFF202020);

                                if (condition.isCompleted()) {
                                    String checkMark = "✔";
                                    context.drawText(textRenderer, checkMark, checkboxX + 1, checkboxY + 1, 0xFF55FF55, true);
                                }

                                int condTextColor = condition.isCompleted() ? 0xFF55FF55 : 0xFFAAAAAA;
                                int textY = condBgY + 2;
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

    private static class CompletedGuideItem {
        final AdvancementEntry advancement;
        final AdvancementRecord record;

        CompletedGuideItem(AdvancementEntry advancement, AdvancementRecord record) {
            this.advancement = advancement;
            this.record = record;
        }
    }

    private static class CompletedGuideListWidget extends ScrollableWidget {
        private final List<CompletedGuideEntry> entries = new ArrayList<>();
        private final List<CompletedGuideItem> guides;

        public CompletedGuideListWidget(MinecraftClient client, int width, int height, int x, int y, int itemHeight,
                                       List<CompletedGuideItem> guides) {
            super(x, y, width, height, Text.empty());
            this.guides = guides;
            this.updateEntries();
        }

        private void updateEntries() {
            this.entries.clear();
            for (CompletedGuideItem guide : this.guides) {
                this.entries.add(new CompletedGuideEntry(guide));
            }
        }

        protected int getContentsHeight() {
            return this.entries.size() * ITEM_HEIGHT;
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
            builder.put(NarrationPart.TITLE, "已完成指南列表");
        }

        protected void renderBackground(DrawContext context) {
            context.fill(this.getX(), this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight(), 0x80000000);
        }

        @Override
        protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
            this.renderBackground(context);
            int currentY = (int) (this.getY() - this.getScrollY());

            context.enableScissor(this.getX(), this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight());

            for (CompletedGuideEntry entry : this.entries) {
                if (currentY + ITEM_HEIGHT >= this.getY() && currentY <= this.getY() + this.getHeight()) {
                    entry.render(context, this.getX(), currentY, this.getWidth(), mouseX, mouseY, delta);
                }
                currentY += ITEM_HEIGHT;
            }

            context.disableScissor();
        }

        private class CompletedGuideEntry {
            private final CompletedGuideItem completedGuide;

            public CompletedGuideEntry(CompletedGuideItem completedGuide) {
                this.completedGuide = completedGuide;
            }

            public void render(DrawContext context, int x, int y, int width, int mouseX, int mouseY, float delta) {
                TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;

                context.fill(x, y, x + width, y + ITEM_HEIGHT, 0x60005500);

                String title = this.completedGuide.advancement.value().display()
                        .map(display -> display.getTitle().getString())
                        .orElse("未知成就");

                String timeText = "";
                int timeTextWidth = 0;
                if (this.completedGuide.record != null) {
                    timeText = formatGameTime(this.completedGuide.record);
                    timeTextWidth = textRenderer.getWidth(timeText);
                }

                String progressText = "100%";
                int progressTextWidth = textRenderer.getWidth(progressText);

                // 计算标题可用宽度（减去时间文本、进度文本和间距）
                int availableWidth = width - timeTextWidth - progressTextWidth - 30;

                if (textRenderer.getWidth(title) > availableWidth) {
                    title = textRenderer.trimToWidth(title, availableWidth - textRenderer.getWidth("...")) + "...";
                }

                // 渲染标题
                context.drawText(textRenderer, title, x + 10, y + (ITEM_HEIGHT - textRenderer.fontHeight) / 2, 0xFF55FF55, true);

                // 渲染时间（在标题右侧）
                if (!timeText.isEmpty()) {
                    int timeX = x + 10 + textRenderer.getWidth(title) + 10;
                    context.drawText(textRenderer, timeText, timeX, y + (ITEM_HEIGHT - textRenderer.fontHeight) / 2, 0xFFAAFFAA, false);
                }

                // 渲染进度（在最右侧）
                context.drawText(textRenderer, progressText, x + width - progressTextWidth - 5, y + (ITEM_HEIGHT - textRenderer.fontHeight) / 2, 0xFF55FF55, true);
            }

            private String formatGameTime(AdvancementRecord record) {
                long gameTime = record.getGameTime();
                // 将游戏刻转换为现实毫秒 (1秒 = 20游戏刻)
                long realTimeMillis = gameTime * 50; // 50毫秒 = 1000ms / 20ticks

                long totalSeconds = realTimeMillis / 1000;
                long hours = totalSeconds / 3600;
                long minutes = (totalSeconds % 3600) / 60;
                long seconds = totalSeconds % 60;
                long milliseconds = realTimeMillis % 1000;

                return String.format("%02d:%02d:%02d.%03d", hours, minutes, seconds, milliseconds);
            }



        }
    }

    private PlacedAdvancement getPlacedAdvancement(String advancementId) {
        if (MinecraftClient.getInstance().player == null) {
            return null;
        }

        Identifier id = Identifier.tryParse(advancementId);
        if (id == null) {
            return null;
        }

        return MinecraftClient.getInstance().player.networkHandler
                .getAdvancementHandler().getManager().get(id);
    }
}