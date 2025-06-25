package com.rcutanf.teamhunter.client.ui;

import com.rcutanf.teamhunter.Phase;
import com.rcutanf.teamhunter.client.TeamhunterClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public class TeamScoreHud {
    private static int huntersScore = 0;
    private static int runnersScore = 0;
    private static int huntersAddedScore = 0;
    private static int runnersAddedScore = 0;
    private static long lastUpdateTime = 0;

    // 动画参数
    private static final int ANIMATION_DURATION = 2000;
    private static final float BOUNCE_SCALE = 1.2f;
    private static final float BOUNCE_DURATION = 300; // 弹性动画持续时间(毫秒)

    // UI参数
    private static final int BAR_WIDTH = 150; // 进度条总宽度
    private static final int BAR_HEIGHT = 2; // 进度条高度
    private static final int BAR_Y = 7; // 进度条Y位置
    private static final int TEXT_Y = BAR_Y-3; // 分数文本Y位置
    private static final int SWORD_WIDTH = 16; // 剑图标宽度
    private static final int SWORD_HEIGHT = 16; // 剑图标高度
    private static final int PADDING = 5; // 内边距

    // 颜色
    private static final int HUNTERS_COLOR = 0xFFFF3333; // 猎人队红色 (Alpha, R, G, B)
    private static final int RUNNERS_COLOR = 0xFF33FF33; // 逃亡者队绿色
    private static final int ADDED_SCORE_COLOR = 0xFF00FF00; // 加分绿色
    private static final int BAR_BG_COLOR = 0x80404040; // 进度条背景(半透灰)

    // 剑图标纹理
    private static final Identifier SWORD_ICON = Identifier.of("teamhunter", "textures/ui/sword.png");

    /**
     * 从网络包更新分数数据
     */
    public static void updateScores(int huntersScore, int runnersScore,
                                    int huntersAddedScore, int runnersAddedScore) {
        TeamScoreHud.huntersScore = huntersScore;
        TeamScoreHud.runnersScore = runnersScore;
        TeamScoreHud.huntersAddedScore = huntersAddedScore;
        TeamScoreHud.runnersAddedScore = runnersAddedScore;
        lastUpdateTime = System.currentTimeMillis();
    }

    /**
     * 渲染UI
     */
    public static void render(DrawContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || TeamhunterClient.phase != Phase.MATCH) return;

        int width = context.getScaledWindowWidth();
        int centerX = width / 2;

        TextRenderer textRenderer = client.textRenderer;
        int barWidth = BAR_WIDTH;

        // 计算进度条位置
        int barLeft = centerX - barWidth / 2;
        int barRight = barLeft + barWidth;

        // 计算分数比例
        int totalScore = Math.max(1, huntersScore + runnersScore);
        float huntersRatio = (float) huntersScore / totalScore;
        float runnersRatio = 1 - huntersRatio;

        // 计算剑的位置（基于比例）
        int swordX = barLeft + (int) (huntersRatio * barWidth) - SWORD_WIDTH / 2;
        int swordY = BAR_Y - SWORD_HEIGHT / 2;

        // 1. 绘制进度条背景
        context.fill(barLeft, BAR_Y, barRight, BAR_Y + BAR_HEIGHT, BAR_BG_COLOR);

        // 2. 绘制猎人队进度条（红色）
        if (huntersScore > 0) {
            int huntersBarWidth = (int) (huntersRatio * barWidth);
            context.fill(barLeft, BAR_Y, barLeft + huntersBarWidth, BAR_Y + BAR_HEIGHT, HUNTERS_COLOR);
        }

        // 3. 绘制逃亡者队进度条（绿色）
        if (runnersScore > 0) {
            int runnersBarStart = barLeft + (int) (huntersRatio * barWidth);
            int runnersBarWidth = (int) (runnersRatio * barWidth);
            context.fill(runnersBarStart, BAR_Y, runnersBarStart + runnersBarWidth, BAR_Y + BAR_HEIGHT, RUNNERS_COLOR);
        }

        // 4. 绘制剑图标（使用正确的draw方法）
        context.draw(vertexConsumerProvider -> {
            RenderLayer renderLayer = RenderLayer.getText(SWORD_ICON);
            VertexConsumer vertexConsumer = vertexConsumerProvider.getBuffer(renderLayer);

            MatrixStack matrixStack = context.getMatrices();
            matrixStack.push();

            // 直接获取矩阵
            Matrix4f positionMatrix = matrixStack.peek().getPositionMatrix();
            Matrix3f normalMatrix = matrixStack.peek().getNormalMatrix();

            // 左下角顶点
            vertexConsumer.vertex(positionMatrix, swordX, swordY + SWORD_HEIGHT, 0);
            vertexConsumer.color(255, 255, 255, 255);
            vertexConsumer.texture(0, 1);
            vertexConsumer.overlay(OverlayTexture.DEFAULT_UV);
            vertexConsumer.light(LightmapTextureManager.MAX_LIGHT_COORDINATE);
            vertexConsumer.normal(normalMatrix.m00(), normalMatrix.m01(), normalMatrix.m02());

            // 右下角顶点
            vertexConsumer.vertex(positionMatrix, swordX + SWORD_WIDTH, swordY + SWORD_HEIGHT, 0);
            vertexConsumer.color(255, 255, 255, 255);
            vertexConsumer.texture(1, 1);
            vertexConsumer.overlay(OverlayTexture.DEFAULT_UV);
            vertexConsumer.light(LightmapTextureManager.MAX_LIGHT_COORDINATE);
            vertexConsumer.normal(normalMatrix.m00(), normalMatrix.m01(), normalMatrix.m02());

            // 右上角顶点
            vertexConsumer.vertex(positionMatrix, swordX + SWORD_WIDTH, swordY, 0);
            vertexConsumer.color(255, 255, 255, 255);
            vertexConsumer.texture(1, 0);
            vertexConsumer.overlay(OverlayTexture.DEFAULT_UV);
            vertexConsumer.light(LightmapTextureManager.MAX_LIGHT_COORDINATE);
            vertexConsumer.normal(normalMatrix.m00(), normalMatrix.m01(), normalMatrix.m02());

            // 左上角顶点
            vertexConsumer.vertex(positionMatrix, swordX, swordY, 0);
            vertexConsumer.color(255, 255, 255, 255);
            vertexConsumer.texture(0, 0);
            vertexConsumer.overlay(OverlayTexture.DEFAULT_UV);
            vertexConsumer.light(LightmapTextureManager.MAX_LIGHT_COORDINATE);
            vertexConsumer.normal(normalMatrix.m00(), normalMatrix.m01(), normalMatrix.m02());

            matrixStack.pop();
        });

        // 5. 获取动画进度
        float animationProgress = Math.min(1.0f,
                (System.currentTimeMillis() - lastUpdateTime) / (float)ANIMATION_DURATION);

        float bounceProgress = Math.min(1.0f, animationProgress * ANIMATION_DURATION / BOUNCE_DURATION);

        // 6. 绘制猎人队分数（在进度条左侧）
        String huntersText = String.valueOf(huntersScore);
        int huntersTextWidth = textRenderer.getWidth(huntersText);
        int huntersTextX = barLeft - huntersTextWidth - PADDING;

        drawTextWithBounce(context, textRenderer, huntersText, huntersTextX, TEXT_Y, HUNTERS_COLOR,
                huntersAddedScore > 0 ? bounceProgress : 1.0f);

        // 7. 绘制逃亡者队分数（在进度条右侧）
        String runnersText = String.valueOf(runnersScore);
        int runnersTextX = barRight + PADDING;

        drawTextWithBounce(context, textRenderer, runnersText, runnersTextX, TEXT_Y, RUNNERS_COLOR,
                runnersAddedScore > 0 ? bounceProgress : 1.0f);

        // 8. 绘制加分动画 - 修改为在总分下方跳出
        if (animationProgress < 1.0f) {
            // 缩短动画持续时间，使其更快速
            float fastAnimProgress = Math.min(1.0f, animationProgress * 2.5f);

            // 只在动画没有完全淡出前显示
            float fadeStart = 0.7f;
            if (fastAnimProgress <= 1.0f) {  // 确保只在动画进行中显示
                // 猎人队加分 - 使用猎人队颜色(红色)
                if (huntersAddedScore > 0) {
                    String addText = "+" + huntersAddedScore;
                    // 从下往上移动的动画
                    int startY = TEXT_Y + textRenderer.fontHeight * 2;
                    int targetY = TEXT_Y;
                    int offsetY = startY - (int)((startY - targetY) * fastAnimProgress);

                    // 计算透明度
                    int alpha = 255;
                    if (fastAnimProgress > fadeStart) {
                        alpha = (int)((1 - (fastAnimProgress - fadeStart) / (1 - fadeStart)) * 255);
                        if (fastAnimProgress >= 1.0f) alpha = 0;
                    }

                    // 只在有可见度时绘制，使用猎人队颜色
                    if (alpha > 0) {
                        int color = (alpha << 24) | (HUNTERS_COLOR & 0x00FFFFFF);
                        context.drawText(textRenderer, addText, huntersTextX, offsetY, color, false);
                    }
                }

                // 逃亡者队加分 - 使用逃亡者队颜色(绿色)
                if (runnersAddedScore > 0) {
                    String addText = "+" + runnersAddedScore;
                    int startY = TEXT_Y + textRenderer.fontHeight * 2;
                    int targetY = TEXT_Y;
                    int offsetY = startY - (int)((startY - targetY) * fastAnimProgress);

                    int alpha = 255;
                    if (fastAnimProgress > fadeStart) {
                        alpha = (int)((1 - (fastAnimProgress - fadeStart) / (1 - fadeStart)) * 255);
                        if (fastAnimProgress >= 1.0f) alpha = 0;
                    }

                    if (alpha > 0) {
                        int color = (alpha << 24) | (RUNNERS_COLOR & 0x00FFFFFF);
                        context.drawText(textRenderer, addText, runnersTextX, offsetY, color, false);
                    }
                }
            }
        }
    }

    /**
     * 带弹跳效果的文本渲染
     */
    private static void drawTextWithBounce(DrawContext context, TextRenderer textRenderer,
                                           String text, int x, int y, int color,
                                           float bounceProgress) {
        // 计算缩放
        float scale = 1.0f;
        if (bounceProgress < 1.0f) {
            scale = 1.0f + (BOUNCE_SCALE - 1.0f) * (1.0f - bounceProgress);
        }

        int textWidth = textRenderer.getWidth(text);
        int textHeight = textRenderer.fontHeight;

        context.getMatrices().push();
        context.getMatrices().translate(x + textWidth / 2, y + textHeight / 2, 0);
        context.getMatrices().scale(scale, scale, 1.0f);
        context.getMatrices().translate(-(x + textWidth / 2), -(y + textHeight / 2), 0);

        context.drawText(textRenderer, text, x, y, color, false);

        context.getMatrices().pop();
    }
}