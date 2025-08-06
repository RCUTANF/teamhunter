package com.rcutanf.teamhunter.client.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.rcutanf.teamhunter.client.guide_sys.advancementListener.AdvancementEventManager;
import net.minecraft.advancement.AdvancementEntry;
import net.minecraft.advancement.AdvancementProgress;
import net.minecraft.advancement.PlacedAdvancement;
import net.minecraft.client.network.ClientAdvancementManager;
import net.minecraft.network.packet.s2c.play.AdvancementUpdateS2CPacket;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.Iterator;
import java.util.Map;

@Mixin(ClientAdvancementManager.class)
public class ClientAdvancementManagerCompletionMixin {

    /**
     * 当玩家完成一个成就时调用
     */
    @Inject(
            method = "onAdvancements",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/session/telemetry/WorldSession;onAdvancementMade(Lnet/minecraft/world/World;Lnet/minecraft/advancement/AdvancementEntry;)V",
                    shift = At.Shift.AFTER
            )
    )
    private void onAdvancementCompleted(
            AdvancementUpdateS2CPacket packet,
            CallbackInfo ci,
            @Local PlacedAdvancement placedAdvancement,
            @Local AdvancementProgress advancementProgress
            ) {
        // 获取成就条目并触发事件
        AdvancementEntry advancementEntry = placedAdvancement.getAdvancementEntry();
        AdvancementEventManager.getInstance().fireAdvancementCompleted(advancementEntry, advancementProgress);
    }
}