package com.rcutanf.teamhunter;

import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CommandConfig {
    private static final String CONFIG_DIR = "config/teamhunter";
    private static final String COMMANDS_FILE_PREFIX = "commands_";
    private static final String COMMANDS_FILE_SUFFIX = ".yaml";
    private static final String CONFIG_FILE = "teamhunter.yaml";
    private static final String DEFAULT_GAMEMODE = "train";

    private static String currentGamemode = DEFAULT_GAMEMODE;
    private static final int DEFAULT_MATCH_DURATION = 40; // 默认比赛时长40分钟
    private static Map<String, Map<String, Object>> gamemodeConfigs = new HashMap<>();
    private static Map<String, List<String>> phaseCommands = new HashMap<>();

    public static void loadConfig() {
        try {
            // 确保配置目录存在
            Path configDirPath = Paths.get(CONFIG_DIR);
            if (!Files.exists(configDirPath)) {
                Files.createDirectories(configDirPath);
            }

            // 加载当前玩法设置
            loadMainConfig();

            // 配置文件完整路径
            Path configFilePath = getCommandsFilePath(currentGamemode);

            // 如果配置文件不存在，创建默认配置
            if (!Files.exists(configFilePath)) {
                createDefaultConfig(configFilePath, currentGamemode);
            }

            // 从外部配置文件加载
            loadCommandsFile(configFilePath);

            System.out.println("已加载玩法配置: " + currentGamemode);
        } catch (Exception e) {
            System.err.println("加载命令配置失败");
            phaseCommands = new HashMap<>();
        }
    }

    private static void loadMainConfig() {
        Path configFilePath = Paths.get(CONFIG_DIR, CONFIG_FILE);

        if (!Files.exists(configFilePath)) {
            saveMainConfig(DEFAULT_GAMEMODE);
            currentGamemode = DEFAULT_GAMEMODE;
            return;
        }

        try {
            Yaml yaml = new Yaml();
            try (InputStream inputStream = new FileInputStream(configFilePath.toFile())) {
                Map<String, Object> config = yaml.load(inputStream);
                if (config == null) {
                    config = new HashMap<>();
                }

                // 加载当前玩法
                currentGamemode = (String) config.get("current_gamemode");
                if (currentGamemode == null) {
                    currentGamemode = DEFAULT_GAMEMODE;
                    saveMainConfig(DEFAULT_GAMEMODE);
                }

                // 加载玩法配置
                Map<String, Object> gamemodes = (Map<String, Object>) config.get("gamemodes");
                if (gamemodes != null) {
                    gamemodeConfigs.clear();
                    for (Map.Entry<String, Object> entry : gamemodes.entrySet()) {
                        gamemodeConfigs.put(entry.getKey(), (Map<String, Object>) entry.getValue());
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("加载游戏配置失败");
            currentGamemode = DEFAULT_GAMEMODE;
            saveMainConfig(DEFAULT_GAMEMODE);
        }
    }

    public static void saveMainConfig(String gamemode) {
        try {
            Path configFilePath = Paths.get(CONFIG_DIR, CONFIG_FILE);
            Map<String, Object> config = new HashMap<>();

            // 如果配置文件已存在，先读取现有配置
            if (Files.exists(configFilePath)) {
                try (InputStream inputStream = new FileInputStream(configFilePath.toFile())) {
                    Yaml yaml = new Yaml();
                    Map<String, Object> existingConfig = yaml.load(inputStream);
                    if (existingConfig != null) {
                        config.putAll(existingConfig);
                    }
                } catch (Exception e) {
                    System.err.println("读取现有配置失败");
                }
            }

            // 更新当前玩法设置
            config.put("current_gamemode", gamemode);

            // 确保gamemodes部分存在
            if (!config.containsKey("gamemodes")) {
                config.put("gamemodes", new HashMap<String, Object>());
            }

            Yaml yaml = new Yaml();
            try (FileWriter writer = new FileWriter(configFilePath.toFile())) {
                yaml.dump(config, writer);
            }

            currentGamemode = gamemode;
        } catch (Exception e) {
            System.err.println("保存游戏配置失败");
        }
    }

    // 获取指定玩法的match阶段时长
    public static int getMatchDuration(String gamemode) {
        Map<String, Object> gamemodeConfig = gamemodeConfigs.get(gamemode);
        if (gamemodeConfig != null && gamemodeConfig.containsKey("match_duration")) {
            Object duration = gamemodeConfig.get("match_duration");
            if (duration instanceof Integer) {
                return (Integer) duration;
            }
            if (duration instanceof String) {
                try {
                    return Integer.parseInt((String) duration);
                } catch (NumberFormatException e) {
                    // 忽略转换错误
                }
            }
        }
        return DEFAULT_MATCH_DURATION;
    }

    // 获取当前玩法的match阶段时长
    public static int getCurrentMatchDuration() {
        return getMatchDuration(currentGamemode);
    }

    // 设置指定玩法的match阶段时长
    public static void setMatchDuration(String gamemode, int minutes) {
        try {
            Path configFilePath = Paths.get(CONFIG_DIR, CONFIG_FILE);
            Map<String, Object> config = new HashMap<>();

            // 读取现有配置
            if (Files.exists(configFilePath)) {
                try (InputStream inputStream = new FileInputStream(configFilePath.toFile())) {
                    Yaml yaml = new Yaml();
                    Map<String, Object> existingConfig = yaml.load(inputStream);
                    if (existingConfig != null) {
                        config.putAll(existingConfig);
                    }
                }
            }

            // 确保gamemodes部分存在
            Map<String, Object> gamemodes;
            if (!config.containsKey("gamemodes")) {
                gamemodes = new HashMap<>();
                config.put("gamemodes", gamemodes);
            } else {
                gamemodes = (Map<String, Object>) config.get("gamemodes");
            }

            // 确保指定玩法的配置存在
            Map<String, Object> gamemodeConfig;
            if (!gamemodes.containsKey(gamemode)) {
                gamemodeConfig = new HashMap<>();
                gamemodes.put(gamemode, gamemodeConfig);
            } else {
                gamemodeConfig = (Map<String, Object>) gamemodes.get(gamemode);
            }

            // 设置match时长
            gamemodeConfig.put("match_duration", minutes);

            // 更新内存中的配置
            if (!gamemodeConfigs.containsKey(gamemode)) {
                gamemodeConfigs.put(gamemode, new HashMap<>());
            }
            gamemodeConfigs.get(gamemode).put("match_duration", minutes);

            // 保存到文件
            Yaml yaml = new Yaml();
            try (FileWriter writer = new FileWriter(configFilePath.toFile())) {
                yaml.dump(config, writer);
            }
        } catch (Exception e) {
            System.err.println("保存玩法时长配置失败: " + e.getMessage());
        }
    }

    private static Path getCommandsFilePath(String gamemode) {
        return Paths.get(CONFIG_DIR, COMMANDS_FILE_PREFIX + gamemode + COMMANDS_FILE_SUFFIX);
    }

    private static void loadCommandsFile(Path configFilePath) throws IOException {
        try (InputStream inputStream = new FileInputStream(configFilePath.toFile())) {
            Yaml yaml = new Yaml();
            Map<String, Object> config = yaml.load(inputStream);

            phaseCommands.clear();
            Map<String, Object> phases = (Map<String, Object>) config.get("phases");
            for (Map.Entry<String, Object> entry : phases.entrySet()) {
                String phaseName = entry.getKey();
                List<String> commands = (List<String>) entry.getValue();
                phaseCommands.put(phaseName, commands);
            }
        }
    }

    private static void createDefaultConfig(Path configFilePath, String gamemode) throws IOException {
        // 从jar包内部的资源文件中复制默认配置
        String resourceName = "commands" + (DEFAULT_GAMEMODE.equals(gamemode) ? "" : "_" + gamemode) + ".yaml";
        try (InputStream defaultConfig = CommandConfig.class.getClassLoader().getResourceAsStream(resourceName)) {
            if (defaultConfig != null) {
                Files.copy(defaultConfig, configFilePath);
            } else {
                // 如果默认配置不存在，创建一个基础的配置文件
                Files.write(configFilePath, getDefaultConfigContent(gamemode).getBytes());
            }
        }
    }

    private static String getDefaultConfigContent(String gamemode) {
        return "# " + gamemode + " 玩法的命令配置\n" +
               "phases:\n" +
               "  warmup:\n" +
               "    # 这里是热身阶段的命令\n" +
               "    - \"/say " + gamemode + " 玩法热身阶段开始\"\n" +
               "  prepare:\n" +
               "    # 这里是准备阶段的命令\n" +
               "    - \"/say " + gamemode + " 玩法准备阶段开始\"\n" +
               "  match:\n" +
               "    # 这里是比赛阶段的命令\n" +
               "    - \"/say " + gamemode + " 玩法比赛开始！\"\n" +
                "  end:\n" +
                "    - \"/say " + gamemode + " 玩法比赛结束！\"\n";
    }

    public static List<String> getCommandsForPhase(String phase) {
        return phaseCommands.getOrDefault(phase, Collections.emptyList());
    }

    public static String getCurrentGamemode() {
        return currentGamemode;
    }

    public static List<String> getAvailableGamemodes() {
        try {
            Path configDirPath = Paths.get(CONFIG_DIR);
            if (!Files.exists(configDirPath)) {
                return Collections.singletonList(DEFAULT_GAMEMODE);
            }

            try (Stream<Path> paths = Files.list(configDirPath)) {
                return paths
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .filter(name -> name.startsWith(COMMANDS_FILE_PREFIX) && name.endsWith(COMMANDS_FILE_SUFFIX))
                    .map(name -> name.substring(COMMANDS_FILE_PREFIX.length(), name.length() - COMMANDS_FILE_SUFFIX.length()))
                    .collect(Collectors.toList());
            }
        } catch (Exception e) {
            System.err.println("获取可用玩法列表失败");
            return Collections.singletonList(DEFAULT_GAMEMODE);
        }
    }

    public static boolean switchGamemode(String gamemode) {
        Path configFilePath = getCommandsFilePath(gamemode);

        // 检查指定的玩法配置是否存在
        if (!Files.exists(configFilePath)) {
            try {
                // 如果不存在，尝试创建默认配置
                createDefaultConfig(configFilePath, gamemode);
            } catch (IOException e) {
                System.err.println("创建玩法配置失败: " + gamemode);
                return false;
            }
        }

        try {
            // 加载指定的玩法配置
            loadCommandsFile(configFilePath);

            // 保存当前玩法设置
            saveMainConfig(gamemode);

            return true;
        } catch (Exception e) {
            System.err.println("切换玩法失败: " + gamemode);
            return false;
        }
    }
}