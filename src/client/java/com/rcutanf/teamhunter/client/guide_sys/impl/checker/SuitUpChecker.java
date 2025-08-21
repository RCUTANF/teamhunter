package com.rcutanf.teamhunter.client.guide_sys.impl.checker;

import com.rcutanf.teamhunter.client.guide_sys.AbstractGuideSysChecker;
import com.rcutanf.teamhunter.client.guide_sys.TriggerType;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class SuitUpChecker  extends AbstractGuideSysChecker {
    private static final int REQUIRED_IRON_INGOT_COUNT = 6; // 需要的铁锭数量

    public SuitUpChecker() {
        // 设置成就ID和使用物品栏触发器
        super(Identifier.of("minecraft", "story/obtain_armor"), "suit_up", List.of(TriggerType.inventory));
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
            // 检查传入的物品是否为铁锭
            String itemId = itemStack.getItem().toString();
            if (itemId.equals("minecraft:iron_ingot")) {
                if(!isActive()){setActive(true);}
                if(itemStack.getCount() / REQUIRED_IRON_INGOT_COUNT >= 1){
                    setProgress(100);
                }
                else {
                    setProgress(itemStack.getCount() * 100 / REQUIRED_IRON_INGOT_COUNT);
                }
            }
        }
    }
}
