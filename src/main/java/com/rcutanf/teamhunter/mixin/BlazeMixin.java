package com.rcutanf.teamhunter.mixin;

import com.rcutanf.teamhunter.EnvironmentController;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.BlazeEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlazeEntity.class)
public abstract class BlazeMixin extends LivingEntity {

    protected BlazeMixin(EntityType<? extends LivingEntity> entityType, World world) {
        super(entityType, world);
    }

    // 在烈焰人被杀死时记录杀死它的玩家
    @Inject(method = "onDeath(Lnet/minecraft/entity/damage/DamageSource;)V", at = @At("HEAD"))
    private void onBlazeKilled(DamageSource source, CallbackInfo ci) {
        Entity attacker = source.getAttacker();
        if (attacker instanceof PlayerEntity player) {
            // 只在地狱维度处理
            if (this.getWorld().getRegistryKey() == World.NETHER) {
                EnvironmentController.registerBlazeKiller((BlazeEntity)(Object)this, player);
            }
        }
    }

    // 控制烈焰人掉落物
    @Inject(method = "dropLoot(Lnet/minecraft/entity/damage/DamageSource;Z)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/mob/HostileEntity;dropLoot(Lnet/minecraft/entity/damage/DamageSource;Z)V"), cancellable = true)
    private void onDropLoot(DamageSource source, boolean causedByPlayer, CallbackInfo ci) {
        if (this.getWorld().getRegistryKey() == World.NETHER) {
            MinecraftServer server = this.getWorld().getServer();
            if (server != null) {
                boolean shouldDrop = EnvironmentController.shouldBlazeDropItems((BlazeEntity)(Object)this, server);
                if (!shouldDrop) {
                    ci.cancel(); // 取消掉落物生成
                }
            }
        }
    }
}