package com.rcutanf.teamhunter.client.mixin;

import com.rcutanf.teamhunter.client.guide_sys.AbstractGuideSysChecker;
import com.rcutanf.teamhunter.client.guide_sys.GuideSysCheckerManager;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class WorldLoadFinishMixin {
    @Inject(method = "joinWorld", at = @At("TAIL"))
    private void onJoinWorld(CallbackInfo ci) {
        //预留，暂时不用
    }
}
