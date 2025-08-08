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
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.Map.Entry;

@Mixin(ClientAdvancementManager.class)
public class ClientAdvancementManagerRevokeMixin {

    @Shadow
    private Map<AdvancementEntry, AdvancementProgress> advancementProgresses;

    @Inject(method = "onAdvancements",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/advancement/AdvancementProgress;init(Lnet/minecraft/advancement/AdvancementRequirements;)V",
                    ordinal = 0,
                    shift = At.Shift.AFTER
            )
    )
    private void checkProgressRevocation(AdvancementUpdateS2CPacket packet,
                                         CallbackInfo ci,
                                         @Local Entry<Identifier, AdvancementProgress> entry,
                                         @Local PlacedAdvancement placedAdvancement,
                                         @Local AdvancementProgress advancementProgress) {
        // 获取成就条目
        AdvancementEntry advancementEntry = placedAdvancement.getAdvancementEntry();

        // 检查是否已经有这个成就的进度
        AdvancementProgress oldProgress = this.advancementProgresses.get(advancementEntry);

        if (oldProgress != null && oldProgress != advancementProgress) {
            // 检查是否有任何已完成的条件在新进度中变为未完成
            boolean progressRevoked = false;

            // 比较新旧进度中已获得的条件
            for (String criterion : oldProgress.getObtainedCriteria()) {
                if (!advancementProgress.getObtainedCriteria().toString().contains(criterion)) {
                    progressRevoked = true;
                    break;
                }
            }

            // 如果检测到进度被撤销，触发事件
            if (progressRevoked) {
                AdvancementEventManager.getInstance().fireAdvancementRevoked(advancementEntry);
            }
        }
    }
}