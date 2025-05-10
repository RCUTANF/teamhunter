package com.rcutanf.teamhunter.client;

import com.rcutanf.teamhunter.NetWorking;
import com.rcutanf.teamhunter.NetWorking.CounterSyncPacket;
import com.rcutanf.teamhunter.Phase;
import com.rcutanf.teamhunter.Teamhunter;
import io.netty.buffer.Unpooled;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientLoginNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudLayerRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.IdentifiedLayer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

public class TeamhunterClient implements ClientModInitializer {

    private static final Identifier countDownLayer = Identifier.of(Teamhunter.MOD_ID, "count-down");

    public static int teamAdvantage = 0; // 0=无优势, 1=猎人, 2=逃亡者
    private static final Identifier advantageLayer = Identifier.of(Teamhunter.MOD_ID, "team-advantage");



    @Override
    public void onInitializeClient() {

        ClientPlayNetworking.registerGlobalReceiver(Phase.ID, (payload, context) -> {
            phase = payload;
        });

        ClientPlayNetworking.registerGlobalReceiver(CounterSyncPacket.ID, (payload, context) -> {
            countDown = Duration.ofMillis(payload.countDownMilliseconds());
        });

        HudLayerRegistrationCallback.EVENT.register(r ->
                r.attachLayerBefore(IdentifiedLayer.MISC_OVERLAYS, countDownLayer, TeamhunterClient::draw)
        );
        ClientLoginNetworking.registerGlobalReceiver(NetWorking.CHECK_CLIENT_MOD, (payload, context, buf, consumer) -> CompletableFuture.completedFuture(new PacketByteBuf(Unpooled.buffer())));

        ClientPlayNetworking.registerGlobalReceiver(NetWorking.TeamAdvantagePacket.ID, (payload, context) -> {
            teamAdvantage = payload.advantageOrdinal();
        });

        HudLayerRegistrationCallback.EVENT.register(r ->
                r.attachLayerBefore(IdentifiedLayer.MISC_OVERLAYS, advantageLayer, TeamhunterClient::drawAdvantageBar)
        );
    }

    public static Phase phase = Phase.WAITING;
    public static Duration countDown = Duration.ZERO;

    public static boolean shouldShowCountDown() {
        return phase.showCountDown;
    }

    private static void draw(DrawContext ctx, RenderTickCounter counter) {
        if (!shouldShowCountDown()) return;
        var textRenderer = MinecraftClient.getInstance().textRenderer;
        var windowWidth = ctx.getScaledWindowWidth();
        var textHeight = textRenderer.fontHeight;

        var text = String.valueOf(countDown.toSeconds());

        ctx.drawCenteredTextWithShadow(textRenderer, phase.name(), windowWidth / 2, 10, 0xFFFFFFFF);
        ctx.drawCenteredTextWithShadow(textRenderer, text, windowWidth / 2, 10 + textHeight + 4, 0xFFFFFFFF);
    }

    //渲染地狱优势队伍
    private static void drawAdvantageBar(DrawContext ctx, RenderTickCounter counter) {
        if (teamAdvantage == 0) return; // 无优势不显示

        int windowWidth = ctx.getScaledWindowWidth();
        int lineWidth = 100; // 横线宽度
        int lineHeight = 3;  // 横线高度
        int y = 5;          // 距离顶部的距离

        // 根据优势队伍设置颜色（猎人红色，逃亡者绿色）
        int color = teamAdvantage == 1 ? 0xFFFF0000 : 0xFF00FF00;

        // 绘制横线
        ctx.fill((windowWidth - lineWidth) / 2, y, (windowWidth + lineWidth) / 2, y + lineHeight, color);
    }
}
