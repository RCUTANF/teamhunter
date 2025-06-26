package com.rcutanf.teamhunter.mixin;

import com.rcutanf.teamhunter.advancement.PlayerAdvancementCallback;
import net.minecraft.advancement.AdvancementEntry;
import net.minecraft.advancement.PlayerAdvancementTracker;
import net.minecraft.advancement.AdvancementDisplay;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerAdvancementTracker.class)
public abstract class PlayerAdvancementTrackerMixin {

    @Shadow
    private ServerPlayerEntity owner;  // 直接引用 owner 字段

    @Inject(
            method = "grantCriterion",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/advancement/AdvancementRewards;apply(Lnet/minecraft/server/network/ServerPlayerEntity;)V",
                    shift = At.Shift.AFTER  // 在奖励应用后执行
            )
    )
    private void onAdvancementGranted(
            AdvancementEntry advancementEntry,
            String criterionName,
            CallbackInfoReturnable<Boolean> cir
    ) {
        Text title = null;

        // 安全获取显示标题
        if (advancementEntry.value().display().isPresent()) {
            AdvancementDisplay display = advancementEntry.value().display().get();
            title = display.getTitle();
        }

        // 触发事件
        PlayerAdvancementCallback.EVENT.invoker().onAdvancementGet(
                this.owner,
                title,
                advancementEntry
        );
    }
}