package com.rcutanf.teamhunter.client.mixin;

import com.rcutanf.teamhunter.client.guide_sys.impl.trigger.WeatherTrigger;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.GameStateChangeS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class WeatherChangeMixin {

    @Inject(method = "onGameStateChange", at = @At("TAIL"))
    private void onGameStateChangeWeatherTrigger(GameStateChangeS2CPacket packet, CallbackInfo ci) {
        WeatherTrigger trigger = WeatherTrigger.getInstance();
        if (trigger != null) {
            GameStateChangeS2CPacket.Reason reason = packet.getReason();

            // 监听雨水开始/停止和雷暴梯度变化
            if (reason == GameStateChangeS2CPacket.RAIN_STARTED ||
                reason == GameStateChangeS2CPacket.RAIN_STOPPED ||
                reason == GameStateChangeS2CPacket.RAIN_GRADIENT_CHANGED ||
                reason == GameStateChangeS2CPacket.THUNDER_GRADIENT_CHANGED) {

                ClientPlayNetworkHandler handler = (ClientPlayNetworkHandler)(Object)this;
                if (handler.getWorld() != null) {
                    trigger.onWeatherChanged(handler.getWorld());
                }
            }
        }
    }
}