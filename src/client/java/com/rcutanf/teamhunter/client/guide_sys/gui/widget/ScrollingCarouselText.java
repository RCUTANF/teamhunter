package com.rcutanf.teamhunter.client.guide_sys.gui.widget;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

public class ScrollingCarouselText {
    private final String[] texts;
    private int currentIndex = 0;
    private long lastSwitchTime = 0;
    private int scrollOffset = 0;
    private long lastScrollTime = 0;
    private boolean scrollingRight = true;
    private boolean needsPause = false;
    private long pauseStartTime = 0;

    // 配置参数
    private int switchInterval = 3000; // 3秒切换
    private int scrollSpeed = 50; // 50ms滚动一次
    private int scrollStep = 1; // 每次滚动1像素
    private int pauseTime = 1000; // 滚动到端点暂停1秒
    private int textColor = 0xFFAAAAFF;
    private int backgroundColor = 0x90000000;
    private boolean drawBackground = true;

    public ScrollingCarouselText(String... texts) {
        this.texts = texts.clone();
    }

    public void render(DrawContext context, TextRenderer textRenderer, int x, int y, int width, int height) {
        long currentTime = System.currentTimeMillis();

        // 处理文本轮播
        handleCarousel(currentTime);

        String currentText = texts[currentIndex];
        int textWidth = textRenderer.getWidth(currentText);

        // 绘制背景
        if (drawBackground) {
            context.fill(x, y, x + width, y + height, backgroundColor);
        }

        // 创建裁剪区域
        context.enableScissor(x, y, x + width, y + height);

        // 处理滚动
        int displayX = x;
        if (textWidth > width) {
            displayX = x - handleScrolling(currentTime, textWidth, width);
        } else {
            // 文本居中显示
            displayX = x + (width - textWidth) / 2;
        }

        // 绘制文本
        int textY = y + (height - textRenderer.fontHeight) / 2;
        context.drawText(textRenderer, currentText, displayX, textY, textColor, true);

        // 禁用裁剪
        context.disableScissor();
    }

    private void handleCarousel(long currentTime) {
        if (texts.length <= 1) return;

        if (currentTime - lastSwitchTime > switchInterval) {
            currentIndex = (currentIndex + 1) % texts.length;
            lastSwitchTime = currentTime;
            resetScroll();
        }
    }

    private int handleScrolling(long currentTime, int textWidth, int maxWidth) {
        int maxOffset = textWidth - maxWidth;

        // 处理暂停
        if (needsPause) {
            if (currentTime - pauseStartTime > pauseTime) {
                needsPause = false;
                lastScrollTime = currentTime;
            } else {
                return scrollOffset; // 保持当前位置
            }
        }

        // 处理滚动
        if (currentTime - lastScrollTime > scrollSpeed) {
            if (scrollingRight) {
                if (scrollOffset < maxOffset) {
                    scrollOffset += scrollStep;
                    lastScrollTime = currentTime;
                } else {
                    // 到达右端，开始暂停
                    startPause(currentTime);
                    scrollingRight = false;
                }
            } else {
                if (scrollOffset > 0) {
                    scrollOffset -= scrollStep;
                    lastScrollTime = currentTime;
                } else {
                    // 到达左端，开始暂停
                    startPause(currentTime);
                    scrollingRight = true;
                }
            }
        }

        return scrollOffset;
    }

    private void startPause(long currentTime) {
        needsPause = true;
        pauseStartTime = currentTime;
    }

    private void resetScroll() {
        scrollOffset = 0;
        scrollingRight = true;
        needsPause = false;
    }

    // 配置方法
    public ScrollingCarouselText setSwitchInterval(int interval) {
        this.switchInterval = interval;
        return this;
    }

    public ScrollingCarouselText setScrollSpeed(int speed) {
        this.scrollSpeed = speed;
        return this;
    }

    public ScrollingCarouselText setScrollStep(int step) {
        this.scrollStep = step;
        return this;
    }

    public ScrollingCarouselText setPauseTime(int pause) {
        this.pauseTime = pause;
        return this;
    }

    public ScrollingCarouselText setTextColor(int color) {
        this.textColor = color;
        return this;
    }

    public ScrollingCarouselText setBackgroundColor(int color) {
        this.backgroundColor = color;
        return this;
    }

    public ScrollingCarouselText setDrawBackground(boolean draw) {
        this.drawBackground = draw;
        return this;
    }
}