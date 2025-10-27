package com.rcutanf.teamhunter.client.guide_sys.gui;

import com.rcutanf.teamhunter.client.guide_sys.def.AchievementChecker;
import net.minecraft.advancement.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
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
        private AchievementChecker achievementChecker;

        public Identifier getId() {
            return id;
        }

        public ItemStack getIcon() {
            return icon;
        }

        public Text getTitle() {
            return title;
        }

        public Text getDescription() {
            return description;
        }

        public int getProgress() {
            return progress;
        }

        public boolean isCompleted() {
            return completed;
        }

        public AchievementChecker getAchievementChecker() {
            return achievementChecker;
        }
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

    public static void clearAdvancementGuides() {advancementGuides.clear();}
    
    public static void addAdvancementGuide(AchievementChecker achievementChecker, int progress) {
        try {
            MinecraftClient client = MinecraftClient.getInstance();

            if (client.getNetworkHandler() == null) {
                System.out.println("客户端网络处理器未初始化");
                return;
            }

            // 直接通过Identifier获取PlacedAdvancement
            MinecraftServer server = client.getServer();
            AdvancementManager advancementManager;
            if(server == null) {
                //TODO:这里应该是联机状态，获取服务器的进度管理器，但是现在没空处理数据包构造和收发
                advancementManager = client.getNetworkHandler().getAdvancementHandler().getManager();
            }
            else{
                advancementManager = server.getAdvancementLoader().getManager();
            }
            var placedAdvancement = advancementManager.get(achievementChecker.advancementId);

            if (placedAdvancement == null) {
                System.out.println("GuideSysGuiManager 找不到进度: " + achievementChecker.advancementId.toString());
                return;
            }

            // 从PlacedAdvancement获取AdvancementEntry和Advancement
            AdvancementEntry advancementEntry = placedAdvancement.getAdvancementEntry();
            var advancement = placedAdvancement.getAdvancement();

            // 获取进度显示信息
            var displayOpt = advancement.display();
            if (displayOpt.isEmpty()) {
                System.out.println("进度没有显示信息: " + achievementChecker.advancementId.toString());
                return;
            }

            var display = displayOpt.get();

            // 创建并填充AdvancementGuideItem
            AdvancementGuideItem item = new AdvancementGuideItem();
            item.id = achievementChecker.advancementId;
            item.icon = display.getIcon();
            item.title = display.getTitle();
            item.description = display.getDescription();
            item.completed = false;
            item.progress = progress;
            item.achievementChecker = achievementChecker;

            // 添加到列表开头
            advancementGuides.add(0, item);

        } catch (Exception e) {
            System.out.println("添加进度到指南时出错: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 用于直接移除一个成就的引导项，设计为在引导系统认为暂时无法完成时调用
     * @param advancementID
     */
    public static void removeAdvancementGuide(Identifier advancementID) {
        //动画预留
        advancementGuides.removeIf(item -> item.id.equals(advancementID));
    }

    public static void updateAdvancementGuide(AchievementChecker achievementChecker, int progress) {
        for (int i = 0; i < advancementGuides.size(); i++) {
            AdvancementGuideItem item = advancementGuides.get(i);
            if (item.id.equals(achievementChecker.advancementId)) {
                item.progress = progress;

                if (progress >= 100) {
                    //预留动画调用
                }

                // 将更新的item移动到顶部
                advancementGuides.remove(i);
                advancementGuides.add(0, item);
                return;
            }
        }
        if (progress > 0) addAdvancementGuide(achievementChecker, progress);
    }

    /**
     * 这是用来触发完成动画的
     * @param advancementID
     */
    public static void markAdvancementComplete(Identifier advancementID) {
        for (AdvancementGuideItem item : advancementGuides) {
            if (item.id.equals(advancementID)) {
                item.completed = true;

                return;
            }
        }
        System.out.println("未找到成就引导项: " + advancementID.toString());
    }
}
