package com.rcutanf.teamhunter.client.ui;

import com.rcutanf.teamhunter.client.PlayerPositionInfo;
import com.rcutanf.teamhunter.client.TeamhunterClient;
import com.rcutanf.teamhunter.client.config.RadarConfig;
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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

public class PlayerRadarHud {
    // 雷达配置
    private static final int RADAR_BG_COLOR = 0x80000000;
    private static final int RADAR_BORDER_COLOR = 0xFFCCCCCC;
    // 玩家标记配置
    private static final int PLAYER_DOT_SIZE = 4;
    private static final int HUNTER_COLOR = 0xFFFF0000; // 红色为猎人
    private static final int RUNNER_COLOR = 0xFF00FF00; // 绿色为逃亡者
    private static final int UNKNOWN_TEAM_COLOR = 0xFFFFFF00; // 黄色为未知队伍
    private static final Map<String, Identifier> CIRCLE_TEXTURES = new HashMap<>();//纹理资源缓存
    private static final Identifier exposureIcon = Identifier.of("teamhunter", "textures/ui/pos_exposure.png");
    private static int RADAR_SIZE = 60; // 默认雷达大小
    private static int RADAR_X = 5; // 默认X位置
    private static int RADAR_Y = 5; // 默认Y位置

    public static void setRadarSize(int size) {
        RADAR_SIZE = Math.max(30, Math.min(200, size));
    }

    public static void setRadarPosition(int x, int y) {
        RADAR_X = Math.max(0, x);
        RADAR_Y = Math.max(0, y);
    }

    public static void render(DrawContext context, RenderTickCounter tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        PlayerEntity player = client.player;
        RadarConfig config = RadarConfig.getInstance();

        if (player == null || !config.isEnabled()) return;

        // 获取配置的雷达大小和位置
        setRadarSize(config.getRadarSize());
        setRadarPosition(config.getRadarX(), config.getRadarY());

        // 获取玩家当前维度
        Identifier playerDimension = client.world.getRegistryKey().getValue();

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
            //加入一个玩家自己可见时展示可见性图片的逻辑
            if (otherPlayerId.equals(player.getUuid())){
                if (posInfo.isVisible()){
                    // 加载位置暴露指示器图片

                    int iconSize = 64; // 图标大小

                    // 计算图标位置
                    int screenWidth = client.getWindow().getScaledWidth();
                    int iconX = screenWidth / 2 - iconSize / 2; // 屏幕中心
                    int iconY = TeamScoreHud.getBAR_HEIGHT() + TeamScoreHud.getBAR_Y() + 3; // 顶部往下偏移10像素

                    // 使用与类中其他纹理绘制相同的方法
                    context.drawTexture(
                            RenderLayer::getGuiTextured, // 渲染层函数
                            exposureIcon,                // 纹理标识符
                            iconX, iconY,                // 位置
                            0, 0,                        // 纹理UV起点
                            iconSize, iconSize,          // 绘制宽高
                            iconSize, iconSize           // 纹理总尺寸
                    );
                }
                continue;
            };

            // 跳过不在同一维度的玩家
            if (posInfo.getDimension() != null && !playerDimension.equals(posInfo.getDimension())) continue;

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
                    posInfo.getPlayerName(),
                    posInfo.isVisible()
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
        fillPlayerDot(context, centerX, centerY, centerDotSize / 2, 0xFFFFFFFF); // 纯白色圆点


    }

    private static void drawPlayerOnRadar(DrawContext context, BlockPos playerPos, float playerRotation,
                                          BlockPos otherPlayerPos, int renderDistance, int dotColor,
                                          String playerName, boolean isVisible) {
        int centerX = RADAR_X + RADAR_SIZE / 2;
        int centerY = RADAR_Y + RADAR_SIZE / 2;
        int radius = RADAR_SIZE / 2;
        int dotRadius = PLAYER_DOT_SIZE / 2;

        // 计算相对位置，考虑玩家朝向
        double dx = otherPlayerPos.getX() - playerPos.getX();
        double dz = otherPlayerPos.getZ() - playerPos.getZ();
        double dy = otherPlayerPos.getY() - playerPos.getY(); // 计算Y轴高度差

        // 计算实际距离
        double distance = Math.sqrt(dx * dx + dz * dz);

        // 旋转坐标系，使玩家朝向始终向上
        double angle = Math.toRadians(-playerRotation + 180);
        double rotatedDx = dx * Math.cos(angle) - dz * Math.sin(angle);
        double rotatedDz = dx * Math.sin(angle) + dz * Math.cos(angle);

        // 计算方向角度（用于射线）
        double directionAngle = Math.atan2(rotatedDz, rotatedDx);

        //点渲染的修正参数
        int adjustedRadius = radius - dotRadius - 1; // 额外减1像素作为安全边距
        // 计算雷达上的位置
        double scaleFactor = (double) adjustedRadius / renderDistance;

        boolean isOutOfRange = distance >= renderDistance;
        int dotX, dotY;

        // 检查是否为队友（同一队伍的玩家）
        boolean isTeammate = false;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null && client.world != null) {
            Team playerTeam = client.world.getScoreboard().getScoreHolderTeam(client.player.getName().getString());
            Team otherTeam = client.world.getScoreboard().getScoreHolderTeam(playerName);
            isTeammate = (playerTeam != null && otherTeam != null && playerTeam == otherTeam);
        }

        if (!isVisible && !isTeammate) {
            // 绘制指向不可见玩家方向的射线
            drawDirectionLine(context, centerX, centerY, adjustedRadius, directionAngle, dotColor, playerName);
            return; // 跳过下方的正常绘制逻辑
        }

        if (isOutOfRange) {


            // 将点放在雷达边缘
            dotX = centerX + Math.round((float) (Math.cos(directionAngle) * (adjustedRadius - 2)));
            dotY = centerY + Math.round((float) (Math.sin(directionAngle) * (adjustedRadius - 2)));
        } else {
            // 正常计算位置
            dotX = centerX + Math.round((float) (rotatedDx * scaleFactor)) - 2;
            dotY = centerY + Math.round((float) (rotatedDz * scaleFactor)) - 2;
        }


        // 绘制玩家圆点(不再使用矩形)

        fillPlayerDot(context, dotX, dotY, dotRadius, dotColor);

        // 如果高度差超过1格，绘制高度指示箭头
        if (Math.abs(dy) > 1) {
            drawHeightIndicator(context, dotX, dotY, dy, dotColor);
        }

        // 如果超出范围则显示距离
        String displayText = playerName;
        if (isOutOfRange) {
            int distanceInBlocks = (int) distance;
            displayText = playerName + " (" + distanceInBlocks + "m)";
        }

        // 绘制玩家名称
        float scale = RadarConfig.getInstance().getNameScale();; // 调整为需要的缩放比例
        int textWidth = MinecraftClient.getInstance().textRenderer.getWidth(playerName);
        int scaledWidth = (int) (textWidth * scale);

        // 保存当前变换矩阵
        context.getMatrices().push();
        // 移动到文本绘制位置（减小垂直偏移，使文字更靠近点）
        //TODO：能不能动态调整啊
        context.getMatrices().translate(dotX - (float) scaledWidth / 2, dotY - (float) PLAYER_DOT_SIZE / 2 - 6, 0);
        // 应用缩放
        context.getMatrices().scale(scale, scale, 1.0f);
        // 使用与点相同的颜色渲染文字
        context.drawText(
                MinecraftClient.getInstance().textRenderer,
                displayText,
                0,
                0,
                dotColor, // 使用与点相同的颜色
                false
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
        int resolution = 2;
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

        // 转换为ABGR格式（交换红蓝通道）
        int abgrColor = (alpha << 24) | (blue << 16) | (green << 8) | red;

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
                        image.setColor(x, y, abgrColor);
                    } else if (distance < scaledRadius + resolution) {
                        // 边缘抗锯齿 - 更平滑的过渡
                        double factor = 1.0 - (distance - scaledRadius) / resolution;
                        factor = Math.max(0, Math.min(1, factor)); // 确保因子在0-1范围内
                        int newAlpha = (int) (alpha * factor);
                        int newColor = (newAlpha << 24) | (blue << 16) | (green << 8) | red;
                        image.setColor(x, y, newColor);
                    }
                } else {
                    // 圆形边框
                    double innerRadius = scaledRadius - borderThickness;
                    if (distance >= innerRadius && distance <= scaledRadius) {
                        image.setColor(x, y, abgrColor);
                    } else if (distance < innerRadius + (double) resolution / 2 && distance > innerRadius - (double) resolution / 2) {
                        // 内边缘抗锯齿
                        image.setColor(x, y, calculateAntiAliasedColor(distance, innerRadius, (double) resolution / 2, abgrColor));
                    } else if (distance < scaledRadius + (double) resolution / 2 && distance > scaledRadius - (double) resolution / 2) {
                        // 外边缘抗锯齿
                        image.setColor(x, y, calculateAntiAliasedColor(distance, scaledRadius, (double) resolution / 2, abgrColor));
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

        int newAlpha = (int) (alpha * factor);
        return (newAlpha << 24) | (red << 16) | (green << 8) | blue;
    }

    // 绘制玩家圆点
    private static void fillPlayerDot(DrawContext context, int centerX, int centerY, int radius, int color) {
        // 确保颜色完全格式化为8位十六进制（包含透明度）
        String colorHex = String.format("%08X", color);
        String cacheKey = "player_dot_" + radius + "_" + colorHex;

        // 检查缓存中是否有这个颜色的纹理，如果没有则生成
        Identifier textureId = CIRCLE_TEXTURES.computeIfAbsent(cacheKey,
                k -> generateCircleTexture(radius, color, true));

        int size = radius * 2;
        context.drawTexture(
                RenderLayer::getGuiTextured,
                textureId,
                centerX - radius, centerY - radius,
                0, 0,
                size, size,
                size, size
        );
    }

    // 绘制高度指示箭头 (使用^字符，下箭头通过旋转实现)
    private static void drawHeightIndicator(DrawContext context, int x, int y, double dy, int color) {
        int yOffset = 1;     // 箭头与圆点的距离
        String arrowChar = "§l^";  // 统一使用^字符
        float scale = 0.35f; // 缩放比例

        // 保存当前变换状态
        context.getMatrices().push();

        // 获取文本尺寸
        int textWidth = MinecraftClient.getInstance().textRenderer.getWidth(arrowChar);
        int textHeight = MinecraftClient.getInstance().textRenderer.fontHeight;

        if (dy > 0) {
            // 向上箭头 - 直接绘制在圆点上方
            float posY = y - yOffset;

            // 移动到位置，应用缩放，然后绘制
            context.getMatrices().translate(x, posY, 0);
            context.getMatrices().scale(scale, scale, 1.0f);

            // 调整偏移以保持居中
            context.drawText(
                    MinecraftClient.getInstance().textRenderer,
                    arrowChar,
                    -textWidth / 2,
                    -textHeight / 2,
                    color,
                    false
            );
        } else {
            // 向下箭头 - 旋转180度后绘制在圆点下方
            float posY = y + yOffset;

            // 移动到文本中心点
            context.getMatrices().translate(x, posY, 0);
            // 应用缩放
            context.getMatrices().scale(scale, scale, 1.0f);
            // 旋转180度
            context.getMatrices().multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Z.rotationDegrees(180));
            // 调整偏移以保持居中
            context.drawText(
                    MinecraftClient.getInstance().textRenderer,
                    arrowChar,
                    -textWidth / 2,
                    -textHeight / 2,
                    color,
                    true
            );
        }

        // 恢复变换状态
        context.getMatrices().pop();
    }

    // 绘制指向不可见玩家方向的射线
    private static void drawDirectionLine(DrawContext context, int centerX, int centerY, int radius,
                                          double angle, int color, String playerName) {
        // 计算射线终点
        int endX = centerX + Math.round((float) (Math.cos(angle) * (radius - 2)));
        int endY = centerY + Math.round((float) (Math.sin(angle) * (radius - 2)));

        // 绘制射线
        drawLine(context, centerX, centerY, endX, endY, color);

        // 在射线终点绘制玩家名称
        float scale = RadarConfig.getInstance().getNameScale();
        int textWidth = MinecraftClient.getInstance().textRenderer.getWidth(playerName);
        int scaledWidth = (int) (textWidth * scale);

        context.getMatrices().push();
        context.getMatrices().translate(endX - (float) scaledWidth / 2, endY - 6, 0);
        context.getMatrices().scale(scale, scale, 1.0f);
        context.drawText(
                MinecraftClient.getInstance().textRenderer,
                playerName,
                0,
                0,
                color,
                true
        );
        context.getMatrices().pop();
    }

    // 通过矩阵旋转绘制线段
    private static void drawLine(DrawContext context, int x1, int y1, int x2, int y2, int color) {
        // 计算线段长度和角度
        int length = (int) Math.round(Math.hypot(x2 - x1, y2 - y1));
        double angle = Math.atan2(y2 - y1, x2 - x1);

        // 保存当前变换
        context.getMatrices().push();

        // 移动到起点位置
        context.getMatrices().translate(x1, y1, 0);
        // 旋转到线段角度
        context.getMatrices().multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Z.rotation((float) angle));

        // 绘制水平线
        context.fill(0, 0, length, 1, color);

        // 恢复变换
        context.getMatrices().pop();
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