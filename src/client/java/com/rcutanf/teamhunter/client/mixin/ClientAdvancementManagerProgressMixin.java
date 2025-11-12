package com.rcutanf.teamhunter.client.mixin;

import com.rcutanf.teamhunter.client.guide_sys.impl.trigger.AdvancementProgressTrigger;
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

import java.util.Map;

@Mixin(ClientAdvancementManager.class)
public class ClientAdvancementManagerProgressMixin {

    @Inject(method = "onAdvancements", at = @At("TAIL"))
    private void onAdvancementsProgressTrigger(AdvancementUpdateS2CPacket packet, CallbackInfo ci) {
        AdvancementProgressTrigger trigger = AdvancementProgressTrigger.getInstance();
        if (trigger != null) {
            ClientAdvancementManager manager = (ClientAdvancementManager) (Object) this;

            // 遍历所有进度更新
            for (Map.Entry<Identifier, AdvancementProgress> entry : packet.getAdvancementsToProgress().entrySet()) {
                PlacedAdvancement placedAdvancement = manager.getManager().get(entry.getKey());
                if (placedAdvancement != null) {
                    AdvancementProgress progress = entry.getValue();
                    trigger.onAdvancementProgress(placedAdvancement, progress);
                }
            }
        }
    }
}