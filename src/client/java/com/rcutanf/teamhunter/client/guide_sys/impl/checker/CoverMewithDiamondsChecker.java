package com.rcutanf.teamhunter.client.guide_sys.impl.checker;

import com.rcutanf.teamhunter.client.guide_sys.AbstractGuideSysChecker;
import com.rcutanf.teamhunter.client.guide_sys.TriggerType;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

import java.util.List;
import java.util.Map;

public class CoverMewithDiamondsChecker extends AbstractGuideSysChecker {
    private static final int REQUIRED_DIAMOND_INGOT_COUNT = 6; // 需要的钻石数量

    public CoverMewithDiamondsChecker() {
        // 设置成就ID和使用物品栏触发器
        super(Identifier.of("minecraft", "story/shiny_gear"), "cover_me_with_diamonds", List.of(TriggerType.inventory));
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
            // 检查传入的物品是否为钻石
            String itemId = itemStack.getItem().toString();
            if (itemId.equals("minecraft:diamond")) {
                if(!isActive()){setActive(true);}
                if(itemStack.getCount() / REQUIRED_DIAMOND_INGOT_COUNT >= 1){
                    setProgress(100);
                }
                else {
                    setProgress(itemStack.getCount() * 100 / REQUIRED_DIAMOND_INGOT_COUNT);
                }
            }
        }
    }
}
