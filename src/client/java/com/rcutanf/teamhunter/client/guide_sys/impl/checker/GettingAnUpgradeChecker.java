package com.rcutanf.teamhunter.client.guide_sys.impl.checker;

import com.rcutanf.teamhunter.client.guide_sys.AbstractGuideSysChecker;
import com.rcutanf.teamhunter.client.guide_sys.TriggerType;
import com.rcutanf.teamhunter.client.guide_sys.GuideSysCheckerManager;
import com.rcutanf.teamhunter.client.guide_sys.advancementListener.AdvancementCompletionListener;
import com.rcutanf.teamhunter.client.guide_sys.advancementListener.AdvancementEventManager;
import net.minecraft.advancement.AdvancementEntry;
import net.minecraft.advancement.AdvancementProgress;
import net.minecraft.item.ItemStack;

/**
 * 石器时代成就检查器
 * 检查玩家是否获得了石头类物品
 */
public class GettingAnUpgradeChecker extends AbstractGuideSysChecker implements AdvancementCompletionListener {

    public GettingAnUpgradeChecker() {
        // 设置成就ID和使用物品栏触发器
        super("getting_an_upgrade", TriggerType.inventory);

        // 注册到检查器管理器
        GuideSysCheckerManager.getInstance().registerChecker(this);

        // 注册成就监听器
        AdvancementEventManager.getInstance().registerListener(this);

        if(super.checkADinGameStatus("minecraft", "story/upgrade_tools")){
            markAsCompleted();
        }


    }

    @Override
    public void onEvent(Object eventData) {
        // 如果成就已完成，不再处理
        if (isCompleted()) {
            return;
        }
        ItemStack itemStack = (ItemStack)eventData;
        // 检查传入的物品是否为圆石
        String itemId = itemStack.getItem().toString();
        if (itemId.equals("minecraft:cobblestone")) {
            setActive(true);
        }
    }

    @Override
    public void onAdvancementCompleted(AdvancementEntry advancement, AdvancementProgress progress) {
        // 检查是否是石器时代成就
        if (advancement.id().getNamespace().equals("minecraft") && advancement.id().getPath().equals("story/upgrade_tools")) {
            markAsCompleted();
        }
    }

    @Override
    public  void onAdvancementRemoved(AdvancementEntry advancement) {
        // 检查是否是石器时代成就
        if (advancement.id().getNamespace().equals("minecraft") && advancement.id().getPath().equals("story/upgrade_tools")) {
            setActive(false);
            completed = false; // 成就被移除，重置状态
            //重新侦听触发器
            register();
        }
    }
}