package com.rcutanf.teamhunter.client.ui;

import com.rcutanf.teamhunter.client.TeamhunterClient;
import com.rcutanf.teamhunter.client.TeamhunterClient.PlayerPositionInfo;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.scoreboard.Team;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

public class PlayerRadarHud {
    // 雷达配置
    private static final int RADAR_SIZE = 60;
    private static final int RADAR_X = 5;
    private static final int RADAR_Y = 5;
    private static final int RADAR_BG_COLOR = 0x80000000;
    private static final int RADAR_BORDER_COLOR = 0xFFFFFFFF;

    // 玩家标记配置
    private static final int PLAYER_DOT_SIZE = 4;
    private static final int HUNTER_COLOR = 0xFFFF0000; // 红色为猎人
    private static final int RUNNER_COLOR = 0xFF00FF00; // 绿色为逃亡者
    private static final int UNKNOWN_TEAM_COLOR = 0xFFFFFF00; // 黄色为未知队伍

    private static final Map<String, Identifier> CIRCLE_TEXTURES = new HashMap<>();//纹理资源缓存



    private static boolean shouldRender = false;

    public static void render(DrawContext context, RenderTickCounter tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        PlayerEntity player = client.player;

        if (player == null) return;

        // 获取玩家渲染距离（方块单位）
        int renderDistance = client.options.getViewDistance().getValue() * 16;

        // 绘制雷达背景
        drawRadarBackground(context);

        // 获取所有玩家位置信息
        Map<UUID, PlayerPositionInfo> playerPositions = TeamhunterClient.getPlayerPositions();

        // 玩家当前位置和朝向
        BlockPos playerPos = player.getBlockPos();
        float playerRotation = player.getYaw();

        // 绘制其他玩家位置
        for (Map.Entry<UUID, PlayerPositionInfo> entry : playerPositions.entrySet()) {
            UUID otherPlayerId = entry.getKey();
            PlayerPositionInfo posInfo = entry.getValue();

            // 跳过当前玩家自己
            if (otherPlayerId.equals(player.getUuid())) continue;

            BlockPos otherPlayerPos = posInfo.getPosition();


            // 队伍颜色
            String playerName = posInfo.getPlayerName();
            Formatting teamFormatting = null;

            // 正确获取玩家队伍信息
            if (playerName != null) {
                Team team = client.world.getScoreboard().getScoreHolderTeam(playerName);
                if (team != null) {
                    teamFormatting = team.getColor();
                }
            }

            // 将Formatting颜色转换为RGB整数
            int dotColor = UNKNOWN_TEAM_COLOR; // 默认颜色
            if (teamFormatting != null) {
                dotColor = teamFormatting.getColorValue() | 0xFF000000; // 添加不透明度
            }

            drawPlayerOnRadar(
                    context,
                    playerPos,
                    playerRotation,
                    otherPlayerPos,
                    renderDistance,
                    dotColor,
                    posInfo.getPlayerName()  // 添加玩家名称参数
            );
        }
    }

    private static void drawRadarBackground(DrawContext context) {
        int centerX = RADAR_X + RADAR_SIZE / 2;
        int centerY = RADAR_Y + RADAR_SIZE / 2;
        int radius = RADAR_SIZE / 2;

        // 绘制圆形背景（而非矩形）
        fillCircle(context, centerX, centerY, radius, RADAR_BG_COLOR);

        // 绘制高精度圆形边框
        drawSmoothCircle(context, centerX, centerY, radius, RADAR_BORDER_COLOR);

        // 绘制中心白点
        int centerDotSize = 3;
        context.fill(centerX - centerDotSize/2, centerY - centerDotSize/2,
                centerX + centerDotSize/2, centerY + centerDotSize/2,
                0xFFFFFFFF); // 纯白色


    }

    private static void drawPlayerOnRadar(DrawContext context, BlockPos playerPos, float playerRotation,
                                          BlockPos otherPlayerPos, int renderDistance, int dotColor,
                                          String playerName) {
        int centerX = RADAR_X + RADAR_SIZE / 2;
        int centerY = RADAR_Y + RADAR_SIZE / 2;
        int radius = RADAR_SIZE / 2;

        // 计算相对位置，考虑玩家朝向
        double dx = otherPlayerPos.getX() - playerPos.getX();
        double dz = otherPlayerPos.getZ() - playerPos.getZ();

        // 计算实际距离
        double distance = Math.sqrt(dx * dx + dz * dz);

        // 旋转坐标系，使玩家朝向始终向上
        double angle = Math.toRadians(-playerRotation + 180);
        double rotatedDx = dx * Math.cos(angle) - dz * Math.sin(angle);
        double rotatedDz = dx * Math.sin(angle) + dz * Math.cos(angle);

        // 计算雷达上的位置
        double scaleFactor = (double) radius / renderDistance;

        boolean isOutOfRange = distance > renderDistance;
        int dotX, dotY;

        if (isOutOfRange) {
            // 计算方向角度
            double directionAngle = Math.atan2(rotatedDz, rotatedDx);

            // 将点放在雷达边缘
            dotX = centerX + (int)(Math.cos(directionAngle) * radius);
            dotY = centerY + (int)(Math.sin(directionAngle) * radius);
        } else {
            // 在雷达范围内，正常计算位置
            dotX = centerX + (int)(rotatedDx * scaleFactor);
            dotY = centerY + (int)(rotatedDz * scaleFactor);
        }



        // 绘制客户端玩家中心点
        context.fill(dotX - PLAYER_DOT_SIZE/2, dotY - PLAYER_DOT_SIZE/2,
                dotX + PLAYER_DOT_SIZE/2, dotY + PLAYER_DOT_SIZE/2,
                dotColor);

        // 如果超出范围则显示距离
        String displayText = playerName;
        if (isOutOfRange) {
            int distanceInBlocks = (int)distance;
            displayText = playerName + " (" + distanceInBlocks + "m)";
        }

        // 绘制玩家名称（缩小字体）
        float scale = 0.4f; // 调整为需要的缩放比例
        int textWidth = MinecraftClient.getInstance().textRenderer.getWidth(playerName);
        int scaledWidth = (int)(textWidth * scale);

        // 保存当前变换矩阵
        context.getMatrices().push();
        // 移动到文本绘制位置（减小垂直偏移，使文字更靠近点）
        //TODO：能不能动态调整啊
        context.getMatrices().translate(dotX - (float) scaledWidth / 2, dotY - (float) PLAYER_DOT_SIZE /2 - 6, 0);
        // 应用缩放
        context.getMatrices().scale(scale, scale, 1.0f);
        // 使用与点相同的颜色渲染文字
        context.drawText(
                MinecraftClient.getInstance().textRenderer,
                displayText,
                0,
                0,
                dotColor, // 使用与点相同的颜色
                true  // 带阴影，提高可读性
        );
        // 恢复变换矩阵
        context.getMatrices().pop();
    }

    // 绘制圆形边框
    private static void drawSmoothCircle(DrawContext context, int centerX, int centerY, int radius, int color) {
        // 获取或生成纹理
        String cacheKey = "border_" + radius + "_" + Integer.toHexString(color);
        Identifier textureId = CIRCLE_TEXTURES.computeIfAbsent(cacheKey,
                k -> generateCircleTexture(radius, color, false));

        // 使用纹理绘制圆形
        int size = radius * 2;
        context.drawTexture(
                RenderLayer::getGuiTextured,  // 渲染层函数
                textureId,                               // 纹理标识符
                centerX - radius, centerY - radius,      // 位置
                0, 0,                                    // 纹理UV起点
                size, size,                              // 绘制宽高
                size, size                               // 纹理总尺寸
        );
    }

    // 填充圆形 - 也使用动态生成的纹理
    private static void fillCircle(DrawContext context, int centerX, int centerY, int radius, int color) {
        // 获取或生成纹理
        String cacheKey = "filled_" + radius + "_" + Integer.toHexString(color);
        Identifier textureId = CIRCLE_TEXTURES.computeIfAbsent(cacheKey,
                k -> generateCircleTexture(radius, color, true));

        // 使用纹理绘制填充圆形
        int size = radius * 2;
        context.drawTexture(
                RenderLayer::getGuiTextured,  // 渲染层函数
                textureId,                               // 纹理标识符
                centerX - radius, centerY - radius,      // 位置
                0, 0,                                    // 纹理UV起点
                size, size,                              // 绘制宽高
                size, size                               // 纹理总尺寸
        );
    }

    // 生成圆形纹理
    private static Identifier generateCircleTexture(int radius, int color, boolean filled) {
        // 增加纹理分辨率来提高精度 - 使用4倍分辨率
        int resolution = 4;
        int size = (radius * 2 + 8) * resolution;
        int centerOffset = size / 2;

        // 创建纹理图像
        NativeImage image = new NativeImage(NativeImage.Format.RGBA, size, size, false);

        // 先将所有像素设为完全透明
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                image.setColor(x, y, 0x00000000); // 完全透明黑色
            }
        }

        // 提取颜色通道
        int alpha = (color >> 24) & 0xFF;
        int red = (color >> 16) & 0xFF;
        int green = (color >> 8) & 0xFF;
        int blue = color & 0xFF;

        // 边框粗细(像素)，增加边框厚度适应更高分辨率
        int borderThickness = filled ? radius * resolution : resolution;

        // 填充图像
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                // 计算到中心的距离
                double dx = x - centerOffset;
                double dy = y - centerOffset;
                double distance = Math.sqrt(dx * dx + dy * dy);

                // 高分辨率下的半径
                double scaledRadius = radius * resolution;

                if (filled) {
                    // 填充圆
                    if (distance <= scaledRadius) {
                        image.setColor(x, y, color);
                    } else if (distance < scaledRadius + resolution) {
                        // 边缘抗锯齿 - 更平滑的过渡
                        double factor = 1.0 - (distance - scaledRadius) / resolution;
                        factor = Math.max(0, Math.min(1, factor)); // 确保因子在0-1范围内
                        int newAlpha = (int)(alpha * factor);
                        int newColor = (newAlpha << 24) | (red << 16) | (green << 8) | blue;
                        image.setColor(x, y, newColor);
                    }
                } else {
                    // 圆形边框
                    double innerRadius = scaledRadius - borderThickness;
                    if (distance >= innerRadius && distance <= scaledRadius) {
                        image.setColor(x, y, color);
                    } else if (distance < innerRadius + (double) resolution /2 && distance > innerRadius - resolution/2) {
                        // 内边缘抗锯齿
                        image.setColor(x, y, calculateAntiAliasedColor(distance, innerRadius, (double) resolution/2, color));
                    } else if (distance < scaledRadius + (double) resolution /2 && distance > scaledRadius - resolution/2) {
                        // 外边缘抗锯齿
                        image.setColor(x, y, calculateAntiAliasedColor(distance, scaledRadius, (double) resolution/2, color));
                    }
                }
            }
        }

        // 生成唯一标识符
        String uniqueId = (filled ? "filled" : "border") + "_circle_" + radius + "_" + Integer.toHexString(color);
        Identifier id = Identifier.of("teamhunter", uniqueId);

        // 创建纹理名称供应器
        String textureName = "teamhunter:circle_" + uniqueId;
        Supplier<String> nameSupplier = () -> textureName;

        // 使用正确的构造函数创建纹理
        NativeImageBackedTexture texture = new NativeImageBackedTexture(nameSupplier, image);
        MinecraftClient.getInstance().getTextureManager().registerTexture(id, texture);

        // 释放资源
        image.close();

        return id;
    }

    // 辅助方法，处理抗锯齿
    private static int calculateAntiAliasedColor(double distance, double targetRadius, double halfResolution, int baseColor) {
        double factor = 1.0 - Math.abs(distance - targetRadius) / halfResolution;
        factor = Math.max(0, Math.min(1, factor));

        int alpha = (baseColor >> 24) & 0xFF;
        int red = (baseColor >> 16) & 0xFF;
        int green = (baseColor >> 8) & 0xFF;
        int blue = baseColor & 0xFF;

        int newAlpha = (int)(alpha * factor);
        return (newAlpha << 24) | (red << 16) | (green << 8) | blue;
    }

    // 纹理清理方法
    public static void dispose() {
        // 清理纹理资源
        TextureManager textureManager = MinecraftClient.getInstance().getTextureManager();
        for (Identifier id : CIRCLE_TEXTURES.values()) {
            textureManager.destroyTexture(id);
        }
        CIRCLE_TEXTURES.clear();
    }



}