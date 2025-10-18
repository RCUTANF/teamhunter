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
            Path recordsDir = Paths.get(MinecraftClient.getInstance().runDirectory.getAbsolutePath(), RECORDS_FOLDER, currentWorldName);
            Files.createDirectories(recordsDir);

            Path recordsPath = recordsDir.resolve(RECORDS_FILE);
            String json = GSON.toJson(records);
            Files.writeString(recordsPath, json);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void addRecord(AdvancementRecord record) {
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
        return Paths.get(MinecraftClient.getInstance().runDirectory.getAbsolutePath(),
                         RECORDS_FOLDER, currentWorldName, RECORDS_FILE);
    }
}