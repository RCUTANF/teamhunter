package com.rcutanf.teamhunter.advancement;

import net.minecraft.util.Identifier;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

public class AdvancementScoreLoader {
    private final Map<Identifier, Integer> scores = new HashMap<>();
    private static final String DEFAULT_NAMESPACE = "minecraft";

    public AdvancementScoreLoader() {
        loadScores();
    }

    public int getScore(Identifier advancementId) {
        return scores.getOrDefault(advancementId, 0);
    }

    private void loadScores() {
        Path scoresFile = getScoresFilePath();

        // 创建默认文件如果不存在
        if (!Files.exists(scoresFile)) {
            createDefaultFile(scoresFile);
            return;
        }

        // 快速加载文件
        try {
            List<String> lines = Files.readAllLines(scoresFile);

            // 跳过标题行
            for (int i = 1; i < lines.size(); i++) {
                String[] parts = lines.get(i).split(",");
                if (parts.length < 2) continue;

                try {
                    String path = parts[0].trim();
                    int score = Integer.parseInt(parts[1].trim());

                    // 解析为完整的成就ID (默认使用 minecraft 命名空间)
                    Identifier id = Identifier.of(path.contains(":")
                            ? path // 完整ID如 "minecraft:husbandry/breed_an_animal"
                            : "minecraft:" + path); // 路径如 "husbandry/breed_an_animal"

                    scores.put(id, score);
                } catch (Exception e) {
                    // 简单忽略无效行
                }
            }
        } catch (IOException e) {
            // 简单记录错误
            System.err.println("无法读取成就分数文件: " + e.getMessage());
        }
    }

    private Path getScoresFilePath() {
        return Paths.get("config", "teamhunter", "advancement_scores.csv");
    }

    private void createDefaultFile(Path file) {
        try {
            Files.createDirectories(file.getParent());
            Files.write(file, Arrays.asList(
                    "advancement_path,score",
                    "husbandry/breed_an_animal,5",
                    "story/mine_stone,1",
                    "story/smelt_iron,3"
            ));
        } catch (IOException e) {
            System.err.println("无法创建默认成就分数文件: " + e.getMessage());
        }
    }
}