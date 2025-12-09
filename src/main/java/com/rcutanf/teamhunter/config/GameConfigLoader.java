package com.rcutanf.teamhunter.config;

import com.rcutanf.teamhunter.Teamhunter;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class GameConfigLoader {
    private static final String CONFIG_DIR = "config/teamhunter";
    private static final String CONFIG_FILE = "teamhunter.yaml";
    private static final String DEFAULT_GAMEMODE = "train";
    private static final int DEFAULT_MATCH_DURATION = 40;

    private static final Yaml YAML = new Yaml();
    private static GameConfigDefinition config = new GameConfigDefinition();

    public static void loadConfig() {
        try {
            // 确保配置目录存在
            Path configDirPath = Paths.get(CONFIG_DIR);
            if (!Files.exists(configDirPath)) {
                Files.createDirectories(configDirPath);
            }

            Path configFilePath = Paths.get(CONFIG_DIR, CONFIG_FILE);

            if (!Files.exists(configFilePath)) {
                createDefaultConfig();
                saveConfig();
            } else {
                loadFromFile(configFilePath);
            }

            Teamhunter.LOGGER.info("已加载配置，当前游戏模式: {}", config.currentGamemode);
        } catch (Exception e) {
            Teamhunter.LOGGER.error("加载配置失败: {}", e.getMessage());
            createDefaultConfig();
        }
    }

    private static void loadFromFile(Path configFilePath) {
        try (InputStream inputStream = new FileInputStream(configFilePath.toFile())) {
            Map<String, Object> yamlData = YAML.load(inputStream);
            if (yamlData == null) yamlData = new HashMap<>();

            // 转换为我们的配置结构
            config.currentGamemode = (String) yamlData.getOrDefault("current_gamemode", DEFAULT_GAMEMODE);
            config.gamemodes = new HashMap<>();

            Map<String, Object> gamemodes = (Map<String, Object>) yamlData.get("gamemodes");
            if (gamemodes != null) {
                for (Map.Entry<String, Object> entry : gamemodes.entrySet()) {
                    String gamemode = entry.getKey();
                    Map<String, Object> gamemodeData = (Map<String, Object>) entry.getValue();

                    GameConfigDefinition.GamemodeConfig gamemodeConfig = new GameConfigDefinition.GamemodeConfig();
                    gamemodeConfig.matchDuration = getIntValue(gamemodeData.get("match_duration"), DEFAULT_MATCH_DURATION);
                    gamemodeConfig.phases = loadPhaseCommands(gamemode);

                    config.gamemodes.put(gamemode, gamemodeConfig);
                }
            }
        } catch (Exception e) {
            Teamhunter.LOGGER.error("读取配置文件失败: {}", e.getMessage());
            createDefaultConfig();
        }
    }

    private static GameConfigDefinition.GamemodeConfig.PhaseCommands loadPhaseCommands(String gamemode) {
        GameConfigDefinition.GamemodeConfig.PhaseCommands phases = new GameConfigDefinition.GamemodeConfig.PhaseCommands();

        try {
            Path commandsFile = Paths.get(CONFIG_DIR, "commands_" + gamemode + ".yaml");
            if (Files.exists(commandsFile)) {
                try (InputStream inputStream = new FileInputStream(commandsFile.toFile())) {
                    Map<String, Object> commandsData = YAML.load(inputStream);
                    if (commandsData != null) {
                        Map<String, Object> phasesData = (Map<String, Object>) commandsData.get("phases");
                        if (phasesData != null) {
                            phases.warmup = (List<String>) phasesData.get("warmup");
                            phases.prepare = (List<String>) phasesData.get("prepare");
                            phases.match = (List<String>) phasesData.get("match");
                            phases.end = (List<String>) phasesData.get("end");
                        }
                    }
                }
            }
        } catch (Exception e) {
            Teamhunter.LOGGER.error("读取命令配置失败: {}", gamemode);
        }

        // 确保列表不为null
        if (phases.warmup == null) phases.warmup = new ArrayList<>();
        if (phases.prepare == null) phases.prepare = new ArrayList<>();
        if (phases.match == null) phases.match = new ArrayList<>();
        if (phases.end == null) phases.end = new ArrayList<>();

        return phases;
    }

    private static void createDefaultConfig() {
        config = new GameConfigDefinition();
        config.currentGamemode = DEFAULT_GAMEMODE;
        config.gamemodes = new HashMap<>();

        // 创建默认训练模式配置
        GameConfigDefinition.GamemodeConfig trainConfig = new GameConfigDefinition.GamemodeConfig();
        trainConfig.matchDuration = DEFAULT_MATCH_DURATION;
        trainConfig.phases = new GameConfigDefinition.GamemodeConfig.PhaseCommands();
        trainConfig.phases.warmup = Arrays.asList("/say 训练模式热身阶段开始");
        trainConfig.phases.prepare = Arrays.asList("/say 训练模式准备阶段开始");
        trainConfig.phases.match = Arrays.asList("/say 训练模式比赛开始！");
        trainConfig.phases.end = Arrays.asList("/say 训练模式比赛结束！");

        config.gamemodes.put(DEFAULT_GAMEMODE, trainConfig);
    }

    public static void saveConfig() {
        try {
            Path configFilePath = Paths.get(CONFIG_DIR, CONFIG_FILE);

            // 构建要保存的数据结构
            Map<String, Object> yamlData = new HashMap<>();
            yamlData.put("current_gamemode", config.currentGamemode);

            Map<String, Object> gamemodes = new HashMap<>();
            for (Map.Entry<String, GameConfigDefinition.GamemodeConfig> entry : config.gamemodes.entrySet()) {
                String gamemode = entry.getKey();
                GameConfigDefinition.GamemodeConfig gamemodeConfig = entry.getValue();

                Map<String, Object> gamemodeData = new HashMap<>();
                gamemodeData.put("match_duration", gamemodeConfig.matchDuration);
                gamemodes.put(gamemode, gamemodeData);

                // 保存命令配置到单独文件
                savePhaseCommands(gamemode, gamemodeConfig.phases);
            }
            yamlData.put("gamemodes", gamemodes);

            try (FileWriter writer = new FileWriter(configFilePath.toFile())) {
                YAML.dump(yamlData, writer);
            }
        } catch (Exception e) {
            Teamhunter.LOGGER.error("保存配置失败: {}", e.getMessage());
        }
    }

    private static void savePhaseCommands(String gamemode, GameConfigDefinition.GamemodeConfig.PhaseCommands phases) {
        try {
            Path commandsFile = Paths.get(CONFIG_DIR, "commands_" + gamemode + ".yaml");

            Map<String, Object> commandsData = new HashMap<>();
            Map<String, Object> phasesData = new HashMap<>();
            phasesData.put("warmup", phases.warmup);
            phasesData.put("prepare", phases.prepare);
            phasesData.put("match", phases.match);
            phasesData.put("end", phases.end);
            commandsData.put("phases", phasesData);

            try (FileWriter writer = new FileWriter(commandsFile.toFile())) {
                YAML.dump(commandsData, writer);
            }
        } catch (Exception e) {
            Teamhunter.LOGGER.error("保存命令配置失败: {}", gamemode);
        }
    }

    // 简单的访问方法
    public static String getCurrentGamemode() {
        return config.currentGamemode;
    }

    public static void setCurrentGamemode(String gamemode) {
        config.currentGamemode = gamemode;
        saveConfig();
    }

    public static int getCurrentMatchDuration() {
        GameConfigDefinition.GamemodeConfig gamemodeConfig = config.gamemodes.get(config.currentGamemode);
        return gamemodeConfig != null ? gamemodeConfig.matchDuration : DEFAULT_MATCH_DURATION;
    }

    public static List<String> getCommandsForPhase(String phase) {
        GameConfigDefinition.GamemodeConfig gamemodeConfig = config.gamemodes.get(config.currentGamemode);
        if (gamemodeConfig == null || gamemodeConfig.phases == null) return new ArrayList<>();

        switch (phase) {
            case "warmup": return gamemodeConfig.phases.warmup;
            case "prepare": return gamemodeConfig.phases.prepare;
            case "match": return gamemodeConfig.phases.match;
            case "end": return gamemodeConfig.phases.end;
            default: return new ArrayList<>();
        }
    }

    public static List<String> getAvailableGamemodes() {
        return new ArrayList<>(config.gamemodes.keySet());
    }

    public static void setMatchDuration(String gamemode, int duration) {
        config.gamemodes.computeIfAbsent(gamemode, k -> new GameConfigDefinition.GamemodeConfig()).matchDuration = duration;
        saveConfig();
    }

    private static int getIntValue(Object value, int defaultValue) {
        if (value instanceof Integer) return (Integer) value;
        if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    public static boolean switchGamemode(String gamemode) {
        if (!config.gamemodes.containsKey(gamemode)) {
            Teamhunter.LOGGER.error("游戏模式不存在: {}", gamemode);
            return false;
        }

        setCurrentGamemode(gamemode);
        Teamhunter.LOGGER.info("已切换到游戏模式: {}", gamemode);
        return true;
    }
}