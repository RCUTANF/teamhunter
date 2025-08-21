package com.rcutanf.teamhunter.client.mixin;

import com.rcutanf.teamhunter.client.guide_sys.GuideSysCheckerManager;
import net.minecraft.client.network.ClientConfigurationNetworkHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientConfigurationNetworkHandler.class)
public class GameOnReadyMixin {
    @Inject(method = "onReady", at = @At("TAIL"))
    private void onGameReady(CallbackInfo ci) {
    }
}
