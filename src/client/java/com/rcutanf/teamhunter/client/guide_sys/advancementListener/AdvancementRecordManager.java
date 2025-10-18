package com.rcutanf.teamhunter.client.guide_sys.advancementListener;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class AdvancementRecordManager {
    private static final Gson GSON = new Gson();
    private static final String RECORDS_FOLDER = "teamhunter_advancement_records";
    private static final String RECORDS_FILE = "advancement_records.json";

    private List<AdvancementRecord> records;
    private String currentWorldName;

    public AdvancementRecordManager() {
        this.records = new ArrayList<>();
    }

    public void loadRecords() {
        World world = MinecraftClient.getInstance().world;
        if (world == null) return;

        String worldName = getWorldName();
        if (worldName.equals(currentWorldName)) return;

        currentWorldName = worldName;
        records.clear();

        try {
            Path recordsPath = getRecordsPath();
            if (Files.exists(recordsPath)) {
                String json = Files.readString(recordsPath);
                Type listType = new TypeToken<List<AdvancementRecord>>(){}.getType();
                List<AdvancementRecord> loadedRecords = GSON.fromJson(json, listType);
                if (loadedRecords != null) {
                    records.addAll(loadedRecords);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void saveRecords() {
        if (currentWorldName == null) return;

        try {
            Path recordsPath = getRecordsPath();
            Files.createDirectories(recordsPath.getParent());

            String json = GSON.toJson(records);
            Files.writeString(recordsPath, json);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void addRecord(AdvancementRecord record) {
        // 拒绝 minecraft:recipes 开头的成就
        if (record.getAdvancementId().toString().startsWith("minecraft:recipes")) {
            return;
        }
        records.removeIf(r -> r.getAdvancementId().equals(record.getAdvancementId()));
        records.add(record);
        records.sort((a, b) -> Long.compare(a.getGameTime(), b.getGameTime()));
        saveRecords();
    }

    public void removeRecord(Identifier advancementId) {
        records.removeIf(r -> r.getAdvancementId().equals(advancementId));
        saveRecords();
    }

    public List<AdvancementRecord> getRecords() {
        return new ArrayList<>(records);
    }

    private String getWorldName() {
        World world = MinecraftClient.getInstance().world;
        if (world != null && world.getRegistryKey() != null) {
            return world.getRegistryKey().getValue().toString().replace(":", "_");
        }
        return "unknown";
    }

    private Path getRecordsPath() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getServer() != null) {
            // 本地单人世界
            return client.getServer().getSavePath(net.minecraft.util.WorldSavePath.ROOT)
                    .resolve("teamhunter_advancement_records.json");
        } else if (client.getCurrentServerEntry() != null) {
            // 多人服务器，存储在服务器特定目录
            String serverName = client.getCurrentServerEntry().name.replaceAll("[^a-zA-Z0-9._-]", "_");
            return Paths.get(client.runDirectory.getAbsolutePath(), "servers", serverName, "teamhunter_advancement_records.json");
        } else {
            // 回退到原来的方式
            return Paths.get(client.runDirectory.getAbsolutePath(), RECORDS_FOLDER, "default", RECORDS_FILE);
        }
    }
}