package com.rcutanf.teamhunter.client.guide_sys.impl.checker;

import com.rcutanf.teamhunter.client.guide_sys.AbstractGuideSysChecker;
import com.rcutanf.teamhunter.client.guide_sys.TriggerType;
import com.rcutanf.teamhunter.client.guide_sys.GuideSysCheckerManager;
import com.rcutanf.teamhunter.client.guide_sys.advancementListener.AdvancementCompletionListener;
import com.rcutanf.teamhunter.client.guide_sys.advancementListener.AdvancementEventManager;
import net.minecraft.advancement.AdvancementEntry;
import net.minecraft.advancement.AdvancementProgress;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

import java.util.Map;

/**
 * 铁器时代成就检查器
 * 检查玩家是否获得了铁矿石
 */
public class AcquireHardwareChecker extends AbstractGuideSysChecker {

    private static final int REQUIRED_IRON_ORE_COUNT = 1; // 需要的铁矿石数量

    public AcquireHardwareChecker() {
        // 设置成就ID和使用物品栏触发器
        super(Identifier.of("minecraft", "story/smelt_iron"), "acquire_hardware", TriggerType.inventory);

        // 注册到检查器管理器
        GuideSysCheckerManager.getInstance().registerChecker(this);

        // 注册成就监听器
        AdvancementEventManager.getInstance().registerListener(this);

        if(super.checkADinGameStatus()){
            markAsCompleted();
        }
    }

    @Override
    public void onEvent(Object eventData) {
        // 如果成就已完成，不再处理
        if (isCompleted()) {
            return;
        }
        // 将eventData转换为Map
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) eventData;
        ItemStack itemStack = (ItemStack) data.get("itemStack");
        boolean isAdded = (boolean) data.get("isAdded");
        if (isAdded) {
            // 检查传入的物品是否为铁矿石
            String itemId = itemStack.getItem().toString();
            if (itemId.equals("minecraft:iron_ore") || itemId.equals("minecraft:deepslate_iron_ore")) {
                if(!isActive()){setActive(true);}
                if(itemStack.getCount() / REQUIRED_IRON_ORE_COUNT >= 1){
                    setProgress(100);
                }
                else {
                    setProgress(itemStack.getCount() * 100 / REQUIRED_IRON_ORE_COUNT);
                }
            }
        }
    }
}