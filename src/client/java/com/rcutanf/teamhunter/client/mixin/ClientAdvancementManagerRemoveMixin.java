package com.rcutanf.teamhunter.client.mixin;

import com.rcutanf.teamhunter.client.guide_sys.advancementListener.AdvancementEventManager;
import net.minecraft.advancement.AdvancementManager;
import net.minecraft.advancement.PlacedAdvancement;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AdvancementManager.class)
public class ClientAdvancementManagerRemoveMixin {
    @Inject(method = "remove",
            at = @At("HEAD")
    )
    private void onRemove(PlacedAdvancement advancement, CallbackInfo ci) {
        AdvancementEventManager.getInstance().fireAdvancementRemoved(advancement.getAdvancementEntry());
    }
}
