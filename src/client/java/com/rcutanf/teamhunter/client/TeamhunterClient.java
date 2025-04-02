package com.rcutanf.teamhunter.client;

import com.rcutanf.teamhunter.NetWorking.CounterSyncPacket;
import com.rcutanf.teamhunter.Phase;
import com.rcutanf.teamhunter.Teamhunter;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudLayerRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.IdentifiedLayer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.util.Identifier;

import java.time.Duration;

public class TeamhunterClient implements ClientModInitializer {

    private static final Identifier countDownLayer = Identifier.of(Teamhunter.MOD_ID, "count-down");


    @Override
    public void onInitializeClient() {
        PayloadTypeRegistry.playS2C().register(Phase.ID, Phase.CODEC);
        PayloadTypeRegistry.playS2C().register(CounterSyncPacket.ID, CounterSyncPacket.CODEC);

        ClientPlayNetworking.registerGlobalReceiver(Phase.ID, (payload, context) -> {
            phase = payload;
        });

        ClientPlayNetworking.registerGlobalReceiver(CounterSyncPacket.ID, (payload, context) -> {
            countDown = Duration.ofMillis(payload.countDownMilliseconds());
        });

        HudLayerRegistrationCallback.EVENT.register(r ->
                r.attachLayerBefore(IdentifiedLayer.MISC_OVERLAYS, countDownLayer, TeamhunterClient::draw)
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
}
