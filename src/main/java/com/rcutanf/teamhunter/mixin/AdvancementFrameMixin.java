package com.rcutanf.teamhunter.mixin;

import com.rcutanf.teamhunter.Phase;
import com.rcutanf.teamhunter.Teamhunter;
import com.rcutanf.teamhunter.advancement.AdvancementScoreLoader;
import com.rcutanf.teamhunter.advancement.PlayerAdvancementCallback;
import net.minecraft.advancement.AdvancementEntry;
import net.minecraft.advancement.AdvancementFrame;
import net.minecraft.advancement.PlayerAdvancementTracker;
import net.minecraft.advancement.AdvancementDisplay;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


@Mixin(AdvancementFrame.class)
public class AdvancementFrameMixin {
    @Inject(method = "getChatAnnouncementText", at = @At("RETURN"), cancellable = true)
    private void appendScoreToAnnouncement(AdvancementEntry advancement, ServerPlayerEntity player, CallbackInfoReturnable<Text> cir) {
        // 检查是否在比赛阶段
        if (Teamhunter.phaseManager.Phase() != Phase.MATCH) return;

        // 获取玩家队伍
        String teamName = player.getScoreboardTeam() != null ? player.getScoreboardTeam().getName() : "";
        if (!teamName.equals("hunters") && !teamName.equals("runners")) return;

        // 获取成就分数
        AdvancementScoreLoader scoreLoader = new AdvancementScoreLoader();
        int score = scoreLoader.getScore(advancement.id());
        if (score <= 0) return;

        // 修改原始文本，添加分数信息
        Text originalText = cir.getReturnValue();
        Text modifiedText = Text.literal("")
            .append(originalText)
            .append(Text.literal(" (+"+score+"分)").formatted(Formatting.GOLD, Formatting.BOLD));

        cir.setReturnValue(modifiedText);
    }
}