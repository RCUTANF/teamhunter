package com.rcutanf.teamhunter;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.damage.DamageSource;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityCombatEvents;
import net.minecraft.server.world.ServerWorld;

public class MatchEndListener {

    public MatchEndListener() {
        // 注册末影龙死亡事件监听器
        ServerEntityCombatEvents.AFTER_KILLED_OTHER_ENTITY.register((world, entity, killed) -> {
            // 检查被杀死的实体是否为末影龙
            if (killed instanceof EnderDragonEntity) {
                CommandExecutor.executeCommand(
                        world.getServer(),
                        "/say source:"+entity
                );
                matchEnd();
            }
        });
    }

    public static void matchEnd() {
        if (Teamhunter.phaseManager.Phase() == Phase.MATCH) {
            Teamhunter.phaseManager.clear();
            Teamhunter.phaseManager.then(Phase.END);
        }
    }
}