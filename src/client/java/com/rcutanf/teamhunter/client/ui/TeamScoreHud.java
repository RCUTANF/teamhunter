package com.rcutanf.teamhunter.client.ui;

import com.rcutanf.teamhunter.Phase;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

public class TeamScoreHud {
    private static int huntersScore = 0;
    private static int runnersScore = 0;
    private static int huntersAddedScore = 0;
    private static int runnersAddedScore = 0;
    private static long lastUpdateTime = 0;

    // Buff 相关
    private static final List<TeamBuff> huntersBuffs = new ArrayList<>();
    private static final List<TeamBuff> runnersBuffs = new ArrayList<>();
    private static final int BUFF_ICON_SIZE = 12; // Buff 图标尺寸
    private static final int BUFF_SPACING = 2; // Buff 图标之间的间距

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

    // 地狱劣势buff图标 - 新增
    private static final Identifier NETHER_DEBUFF_ICON = Identifier.of("teamhunter", "textures/ui/nether_debuff.png");
    // 领先优势buff图标
    private static final Identifier ADVANTAGE_BUFF_ICON = Identifier.of("teamhunter", "textures/ui/advantage_buff.png");

    public static int getBAR_HEIGHT() {return BAR_HEIGHT;}

    public static int getBAR_Y() {return BAR_Y;}

    /**
     * 表示一个队伍效果（Buff/Debuff）
     */
    public static class TeamBuff {
        private final Identifier icon;
        private final String tooltip;
        private boolean isActive = true;
        private long addedTime;

        public TeamBuff(Identifier icon, String tooltip) {
            this.icon = icon;
            this.tooltip = tooltip;
            this.addedTime = System.currentTimeMillis();
        }

        public Identifier getIcon() {
            return icon;
        }

        public String getTooltip() {
            return tooltip;
        }

        public boolean isActive() {
            return isActive;
        }

        public void setActive(boolean active) {
            this.isActive = active;
        }

        public long getAddedTime() {
            return addedTime;
        }
    }

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
     * 添加地狱劣势Buff给指定队伍
     */
    public static void addNetherDebuff(boolean isHunterTeam) {
        TeamBuff netherDebuff = new TeamBuff(NETHER_DEBUFF_ICON, "地狱劣势");

        if (isHunterTeam) {
            // 移除之前的相同buff
            huntersBuffs.removeIf(buff -> buff.getIcon().equals(NETHER_DEBUFF_ICON));
            huntersBuffs.add(netherDebuff);
        } else {
            // 移除之前的相同buff
            runnersBuffs.removeIf(buff -> buff.getIcon().equals(NETHER_DEBUFF_ICON));
            runnersBuffs.add(netherDebuff);
        }
    }

    /**
     * 移除地狱劣势Buff
     */
    public static void removeNetherDebuff(boolean isHunterTeam) {
        if (isHunterTeam) {
            huntersBuffs.removeIf(buff -> buff.getIcon().equals(NETHER_DEBUFF_ICON));
        } else {
            runnersBuffs.removeIf(buff -> buff.getIcon().equals(NETHER_DEBUFF_ICON));
        }
    }

    /**
     * 添加自定义Buff
     */
    public static void addBuff(boolean isHunterTeam, Identifier icon, String tooltip) {
        TeamBuff buff = new TeamBuff(icon, tooltip);

        if (isHunterTeam) {
            huntersBuffs.add(buff);
        } else {
            runnersBuffs.add(buff);
        }
    }

    /**
     * 清空所有Buff
     */
    public static void clearAllBuffs() {
        huntersBuffs.clear();
        runnersBuffs.clear();
    }

    /**
     * 渲染UI
     */
    public static void render(DrawContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if ((client.player == null) ||
                PhaseCountdownHud.getPhase() != Phase.MATCH && PhaseCountdownHud.getPhase() != Phase.END ) return;

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
        context.drawTexture(RenderPipelines.GUI_TEXTURED,SWORD_ICON, swordX, swordY, 0, 0, SWORD_WIDTH, SWORD_HEIGHT, SWORD_WIDTH, SWORD_HEIGHT);

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

        // 7. 绘制猎人队的Buff (在分数左侧)
        int hunterBuffX = huntersTextX - BUFF_ICON_SIZE - PADDING;
        int buffY = TEXT_Y - 1; // 轻微调整，使buff图标与文本垂直居中

        for (int i = 0; i < huntersBuffs.size(); i++) {
            TeamBuff buff = huntersBuffs.get(i);
            if (buff.isActive()) {
                context.drawTexture(RenderPipelines.GUI_TEXTURED, buff.getIcon(), hunterBuffX - (BUFF_ICON_SIZE + BUFF_SPACING) * i,
                                buffY, 0, 0, BUFF_ICON_SIZE, BUFF_ICON_SIZE, BUFF_ICON_SIZE, BUFF_ICON_SIZE);
            }
        }

        // 8. 绘制逃亡者队分数（在进度条右侧）
        String runnersText = String.valueOf(runnersScore);
        int runnersTextX = barRight + PADDING;

        drawTextWithBounce(context, textRenderer, runnersText, runnersTextX, TEXT_Y, RUNNERS_COLOR,
                runnersAddedScore > 0 ? bounceProgress : 1.0f);

        // 9. 绘制逃亡者队的Buff (在分数右侧)
        int runnersTextWidth = textRenderer.getWidth(runnersText);
        int runnerBuffX = runnersTextX + runnersTextWidth + PADDING;

        for (int i = 0; i < runnersBuffs.size(); i++) {
            TeamBuff buff = runnersBuffs.get(i);
            if (buff.isActive()) {
                context.drawTexture(RenderPipelines.GUI_TEXTURED, buff.getIcon(), runnerBuffX + (BUFF_ICON_SIZE + BUFF_SPACING) * i,
                                buffY, 0, 0, BUFF_ICON_SIZE, BUFF_ICON_SIZE, BUFF_ICON_SIZE, BUFF_ICON_SIZE);
            }
        }

        // 10. 绘制分数变化动画 - 支持加分和减分
        if (animationProgress < 1.0f) {
            // 缩短动画持续时间，使其更快速
            float fastAnimProgress = Math.min(1.0f, animationProgress * 2.5f);

            // 只在动画没有完全淡出前显示
            float fadeStart = 0.7f;
            if (fastAnimProgress <= 1.0f) {  // 确保只在动画进行中显示
                // 猎人队分数变化
                if (huntersAddedScore != 0) {
                    // 确定是加分还是减分，符号会自动显示
                    String changeText = huntersAddedScore > 0 ?
                            "+" + huntersAddedScore : String.valueOf(huntersAddedScore);
                    // 始终使用队伍颜色
                    int textColor = HUNTERS_COLOR;

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

                    // 只在有可见度时绘制
                    if (alpha > 0) {
                        int color = (alpha << 24) | (textColor & 0x00FFFFFF);
                        context.drawText(textRenderer, changeText, huntersTextX, offsetY, color, false);
                    }
                }

                // 逃亡者队分数变化
                if (runnersAddedScore != 0) {
                    // 确定是加分还是减分，符号会自动显示
                    String changeText = runnersAddedScore > 0 ?
                            "+" + runnersAddedScore : String.valueOf(runnersAddedScore);
                    // 始终使用队伍颜色
                    int textColor = RUNNERS_COLOR;

                    int startY = TEXT_Y + textRenderer.fontHeight * 2;
                    int targetY = TEXT_Y;
                    int offsetY = startY - (int)((startY - targetY) * fastAnimProgress);

                    int alpha = 255;
                    if (fastAnimProgress > fadeStart) {
                        alpha = (int)((1 - (fastAnimProgress - fadeStart) / (1 - fadeStart)) * 255);
                        if (fastAnimProgress >= 1.0f) alpha = 0;
                    }

                    if (alpha > 0) {
                        int color = (alpha << 24) | (textColor & 0x00FFFFFF);
                        context.drawText(textRenderer, changeText, runnersTextX, offsetY, color, false);
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

        context.getMatrices().pushMatrix();
        context.getMatrices().translate(x + (float) textWidth / 2, y + (float) textHeight / 2);
        context.getMatrices().scale(scale, scale);
        context.getMatrices().translate(-(x + (float) textWidth / 2), -(y + (float) textHeight / 2));

        context.drawText(textRenderer, text, x, y, color, false);

        context.getMatrices().popMatrix();
    }

    /**
     * 添加领先优势Buff给指定队伍
     */
    public static void addAdvantageBuff(boolean isHunterTeam) {
        TeamBuff advantageBuff = new TeamBuff(ADVANTAGE_BUFF_ICON, "领先优势：指南针追踪");

        if (isHunterTeam) {
            // 移除之前的相同buff
            huntersBuffs.removeIf(buff -> buff.getIcon().equals(ADVANTAGE_BUFF_ICON));
            huntersBuffs.add(advantageBuff);
        } else {
            // 移除之前的相同buff
            runnersBuffs.removeIf(buff -> buff.getIcon().equals(ADVANTAGE_BUFF_ICON));
            runnersBuffs.add(advantageBuff);
        }
    }

    /**
     * 移除领先优势Buff
     */
    public static void removeAdvantageBuff(boolean isHunterTeam) {
        if (isHunterTeam) {
            huntersBuffs.removeIf(buff -> buff.getIcon().equals(ADVANTAGE_BUFF_ICON));
        } else {
            runnersBuffs.removeIf(buff -> buff.getIcon().equals(ADVANTAGE_BUFF_ICON));
        }
    }

    /**
     * 获取猎人队分数
     */
    public static int getHuntersScore() {
        return huntersScore;
    }
    /**
     * 获取逃亡者队分数
     */
    public static int getRunnersScore() {
        return runnersScore;
    }
}