package com.rcutanf.teamhunter.client.ui;

import com.rcutanf.teamhunter.Phase;
import com.rcutanf.teamhunter.client.config.RadarConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

import java.time.Duration;

public class PhaseCountdownHud {
    private static Phase phase = Phase.NONE;
    private static Duration countDown = Duration.ZERO;

    public static void setPhase(Phase newPhase) {
        phase = newPhase;
    }

    public static void setCountDown(Duration newCountDown) {
        countDown = newCountDown;
    }

    public static Phase getPhase() {
        return phase;
    }

    public static Duration getCountDown() {
        return countDown;
    }

    public static boolean shouldShowCountDown() {
        return phase.showCountDown;
    }

    public static void draw(DrawContext ctx, RenderTickCounter counter) {
        if (!shouldShowCountDown()) return;
        var textRenderer = MinecraftClient.getInstance().textRenderer;
        var windowWidth = ctx.getScaledWindowWidth();
        var textHeight = textRenderer.fontHeight;

        var text = String.valueOf(countDown.toSeconds());
        if(phase == Phase.MATCH) {
            if(countDown.toSeconds() > 0) {
                // 将总秒数转换为MM:SS格式
                long totalSeconds = countDown.toSeconds();
                long minutes = totalSeconds / 60;
                long seconds = totalSeconds % 60;
                String timeText = String.format("%02d:%02d", minutes, seconds);

                // 获取雷达配置
                RadarConfig radarConfig = RadarConfig.getInstance();
                int radarX = radarConfig.getRadarX();
                int radarY = radarConfig.getRadarY();
                int radarSize = radarConfig.getRadarSize();

                // 计算雷达中心
                int centerX = radarX + radarSize / 2;
                int centerY = radarY + radarSize / 2;
                int textWidth = textRenderer.getWidth(timeText);
                int margin = 6; // 雷达和文本之间的间距
                int textY = radarY + radarSize + margin;
                int textX = centerX - textWidth / 2;

                // 在雷达下方居中绘制倒计时
                ctx.drawTextWithShadow(textRenderer, timeText, textX, textY, 0xFFFFFFFF);
            }
        }
        else {
            ctx.drawCenteredTextWithShadow(textRenderer, phase.name(), windowWidth / 2, 10, 0xFFFFFFFF);
            ctx.drawCenteredTextWithShadow(textRenderer, text, windowWidth / 2, 10 + textHeight + 4, 0xFFFFFFFF);
        }
    }

    // 预留：右上角渲染倒计时的方法
    public static void drawCountdownTopRight(DrawContext ctx, String timeText) {
        var textRenderer = MinecraftClient.getInstance().textRenderer;
        var windowWidth = ctx.getScaledWindowWidth();
        int rightMargin = 10;
        int topMargin = 10;
        int textWidth = textRenderer.getWidth(timeText);
        int x = windowWidth - textWidth - rightMargin;
        ctx.drawTextWithShadow(textRenderer, timeText, x, topMargin, 0xFFFFFFFF);
    }
}