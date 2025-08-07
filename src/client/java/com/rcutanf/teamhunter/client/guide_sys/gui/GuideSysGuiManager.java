package com.rcutanf.teamhunter.client.guide_sys.gui;

import com.rcutanf.teamhunter.client.guide_sys.advancementListener.AdvancementEventManager;
import net.minecraft.advancement.AdvancementDisplay;
import net.minecraft.advancement.PlacedAdvancement;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientAdvancementManager;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public class GuideSysGuiManager {
    public static class AdvancementGuideItem{
        private Identifier id;
        private ItemStack icon;
        private Text title;
        private Text description;
        private int progress;
        private boolean completed;
    }

    private static List<AdvancementGuideItem> advancementGuides = new ArrayList<>();

    GuideSysGuiManager() {}
    
    public static void addAdvancementGuide(AdvancementGuideItem item) {
        advancementGuides.add(item);
    }
    
    public static void removeAdvancementGuide(AdvancementGuideItem item) {
        advancementGuides.remove(item);
    }
    
    public static List<AdvancementGuideItem> getAdvancementGuides() {
        return advancementGuides;
    }
    
    public static void addAdvancementGuide(Identifier advancementID, int progress) {
        PlacedAdvancement advancement = AdvancementEventManager.getInstance().fromId(advancementID.getNamespace(), advancementID.getPath());

        // 获取进度显示信息
        AdvancementDisplay display = advancement.getAdvancement().display().orElse(null);
        if (display == null) {
            System.out.println("进度没有显示信息: " + advancement.getAdvancementEntry().id().toString());
            return;
        }

        // 创建一个新的AdvancementGuideItem
        AdvancementGuideItem item = new AdvancementGuideItem();

        item.id = advancementID;

        // 设置图标
        item.icon = display.getIcon();

        // 设置标题和描述
        item.title = display.getTitle();
        item.description = display.getDescription();

        // 设置进度信息（这里需要根据实际情况计算进度）
        item.completed = false;
        item.progress = progress;


        advancementGuides.add(item);
        //动画预留
    }

    /**
     * 用于直接移除一个成就的引导项，设计为在引导系统认为暂时无法完成时调用
     * @param advancementID
     */
    public static void removeAdvancementGuide(Identifier advancementID) {
        //动画预留
        advancementGuides.removeIf(item -> item.id.equals(advancementID));
    }

    public static void updateAdvancementGuide(Identifier advancementID, int progress) {
        for (AdvancementGuideItem item : advancementGuides) {
            if (item.id.equals(advancementID)) {
                item.progress = progress;

                if (progress >= 100) {
                    //预留动画调用
                }
                return;
            }
        }
        addAdvancementGuide(advancementID, progress);
    }

    /**
     * 这是用来触发完成动画的
     * @param advancementID
     */
    public static void markAdvancementComplete(Identifier advancementID) {
        for (AdvancementGuideItem item : advancementGuides) {
            if (item.id.equals(advancementID)) {
                item.completed = true;
                // 这里可以添加动画触发逻辑
                return;
            }
        }
        System.out.println("未找到成就引导项: " + advancementID.toString());
    }
}
