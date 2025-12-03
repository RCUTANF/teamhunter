package com.rcutanf.teamhunter.client.guide_sys.gui.data;

import com.rcutanf.teamhunter.client.guide_sys.advancementListener.AdvancementRecord;
import net.minecraft.advancement.PlacedAdvancement;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

public class CompletedGuideData {
    private final Identifier id;
    private final String title;
    private final long completionTime;
    private final ItemStack icon;

    public CompletedGuideData(Identifier id, String title, long completionTime, ItemStack icon) {
        this.id = id;
        this.title = title;
        this.completionTime = completionTime;
        this.icon = icon;
    }

    public static CompletedGuideData fromAdvancementRecord(AdvancementRecord record) {
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player == null) return null;

            Identifier id = Identifier.tryParse(record.getAdvancementId().toString());
            if (id == null) return null;

            PlacedAdvancement placedAdvancement = client.player.networkHandler
                    .getAdvancementHandler().getManager().get(id);

            if (placedAdvancement == null) return null;

            String title = placedAdvancement.getAdvancementEntry().value().display()
                    .map(display -> display.getTitle().getString())
                    .orElse("未知成就");

            ItemStack icon = placedAdvancement.getAdvancementEntry().value().display()
                    .map(display -> display.getIcon())
                    .orElse(ItemStack.EMPTY);

            return new CompletedGuideData(id, title, record.getGameTime(), icon);
        } catch (Exception e) {
            System.out.println("从AdvancementRecord创建CompletedGuideData时出错: " + e.getMessage());
            return null;
        }
    }

    public String getFormattedTime() {
        long realTimeMillis = completionTime * 50;
        long totalSeconds = realTimeMillis / 1000;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        long milliseconds = realTimeMillis % 1000;

        return String.format("%02d:%02d:%02d.%03d", hours, minutes, seconds, milliseconds);
    }

    // Getters
    public Identifier getId() { return id; }
    public String getTitle() { return title; }
    public long getCompletionTime() { return completionTime; }
    public ItemStack getIcon() { return icon; }
}
