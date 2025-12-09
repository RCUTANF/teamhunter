package com.rcutanf.teamhunter.client.guide_sys.gui.data;
import com.rcutanf.teamhunter.Teamhunter;
import com.rcutanf.teamhunter.client.guide_sys.def.AchievementChecker;
import net.minecraft.advancement.AdvancementEntry;
import net.minecraft.advancement.PlacedAdvancement;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

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

            PlacedAdvancement placedAdvancement = getPlacedAdvancement(checker.advancementId);

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
            Teamhunter.LOGGER.info("从AchievementChecker创建GuideData时出错: {}", e.getMessage());
            return null;
        }
    }

    private static PlacedAdvancement getPlacedAdvancement(Identifier id) {
        MinecraftClient client = MinecraftClient.getInstance();
        MinecraftServer server = client.getServer();

        if (server == null) {
            // 联机模式，从缓存获取数据
            AdvancementEntry cachedAdvancementEntry = AdvancementDataCache.getInstance().getCachedAdvancementEntry(id);
            if (cachedAdvancementEntry != null) {
                // 创建一个模拟的 PlacedAdvancement 用于显示
                return new PlacedAdvancement(cachedAdvancementEntry, null);
            }
            return null;
        } else {
            return server.getAdvancementLoader().getManager().get(id);
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