package com.rcutanf.teamhunter.client.mixin;

import com.rcutanf.teamhunter.client.guide_sys.GuideSysTriggerManager;
import com.rcutanf.teamhunter.client.guide_sys.TriggerType;
import com.rcutanf.teamhunter.client.guide_sys.impl.trigger.InventoryTrigger;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.Map;

@Mixin(PlayerInventory.class)
public class PlayerInventoryMixin {

    @Inject(method = "addStack(Lnet/minecraft/item/ItemStack;)I", at = @At("RETURN"))
    private void onAddStack(ItemStack stack, CallbackInfoReturnable<Integer> cir) {
        // 传递添加的物品
        triggerInventoryChange(stack,true);
    }

    @Inject(method = "removeStack(II)Lnet/minecraft/item/ItemStack;", at = @At("RETURN"))
    private void onRemoveStack(int slot, int amount, CallbackInfoReturnable<ItemStack> cir) {
        // 传递被移除的物品
        ItemStack removedStack = cir.getReturnValue();
        if (!removedStack.isEmpty()) {
            triggerInventoryChange(removedStack,false);
        }
    }

    @Inject(method = "setStack", at = @At("RETURN"))
    private void onSetStack(int slot, ItemStack stack, CallbackInfo ci) {
        // 传递设置的新物品
        triggerInventoryChange(stack,true);
    }

    private void triggerInventoryChange(ItemStack changedStack, boolean isAdded) {
        InventoryTrigger trigger = (InventoryTrigger) GuideSysTriggerManager.getInstance()
            .getTrigger(TriggerType.inventory);
        if (trigger != null && !changedStack.isEmpty()) {
            // 创建一个Map作为键值对，包含物品和操作类型
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("itemStack", changedStack);
            eventData.put("isAdded", isAdded);// true表示添加，false表示移除
            trigger.fire(eventData);
        }
    }
}