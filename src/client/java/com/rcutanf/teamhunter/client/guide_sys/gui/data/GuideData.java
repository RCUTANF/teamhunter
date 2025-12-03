package com.rcutanf.teamhunter.client.guide_sys.gui.data;
import com.rcutanf.teamhunter.client.guide_sys.def.AchievementChecker;
import net.minecraft.advancement.AdvancementManager;
import net.minecraft.advancement.PlacedAdvancement;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class GuideData {
    private final Identifier id;
    private final ItemStack icon;
    private final Text title;
    private final Text description;
    private final int progress;
    private final boolean completed;
    private final List<GuideCondition> conditions;

    public GuideData(Identifier id, ItemStack icon, Text title, Text description,
                     int progress, boolean completed, List<GuideCondition> conditions) {
        this.id = id;
        this.icon = icon;
        this.title = title;
        this.description = description;
        this.progress = progress;
        this.completed = completed;
        this.conditions = conditions != null ? conditions : new ArrayList<>();
    }

    public static GuideData fromAchievementChecker(AchievementChecker checker, int progress) {
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.getNetworkHandler() == null) return null;

            AdvancementManager advancementManager = getAdvancementManager();
            PlacedAdvancement placedAdvancement = advancementManager.get(checker.advancementId);

            if (placedAdvancement == null) return null;

            var advancement = placedAdvancement.getAdvancement();
            var displayOpt = advancement.display();
            if (displayOpt.isEmpty()) return null;

            var display = displayOpt.get();

            // 转换条件
            List<GuideCondition> conditions = new ArrayList<>();
            if (checker.conditions != null) {
                for (var condition : checker.conditions) {
                    conditions.add(new GuideCondition(
                        condition.description,
                        condition.isCompleted(),
                        condition.getInsideProgress(),
                        condition.getMaxProgress()
                    ));
                }
            }

            return new GuideData(
                checker.advancementId,
                display.getIcon(),
                display.getTitle(),
                display.getDescription(),
                progress,
                false,
                conditions
            );
        } catch (Exception e) {
            System.out.println("从AchievementChecker创建GuideData时出错: " + e.getMessage());
            return null;
        }
    }

    private static AdvancementManager getAdvancementManager() {
        MinecraftClient client = MinecraftClient.getInstance();
        MinecraftServer server = client.getServer();

        if (server == null) {
            //TODO:这里应该是联机状态，获取服务器的进度管理器，但是现在没空处理数据包构造和收发
            return Objects.requireNonNull(client.getNetworkHandler()).getAdvancementHandler().getManager();
        } else {
            return server.getAdvancementLoader().getManager();
        }
    }

    // Getters
    public Identifier getId() { return id; }
    public ItemStack getIcon() { return icon; }
    public Text getTitle() { return title; }
    public Text getDescription() { return description; }
    public int getProgress() { return progress; }
    public boolean isCompleted() { return completed; }
    public List<GuideCondition> getConditions() { return conditions; }
}