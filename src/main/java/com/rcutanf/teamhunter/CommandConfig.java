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
    private static final String CURRENT_GAMEMODE_FILE = "current_gamemode.yaml";
    private static final String DEFAULT_GAMEMODE = "default";

    private static String currentGamemode = DEFAULT_GAMEMODE;
    private static Map<String, List<String>> phaseCommands = new HashMap<>();

    public static void loadConfig() {
        try {
            // 确保配置目录存在
            Path configDirPath = Paths.get(CONFIG_DIR);
            if (!Files.exists(configDirPath)) {
                Files.createDirectories(configDirPath);
            }

            // 加载当前玩法设置
            loadCurrentGamemode();

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

    private static void loadCurrentGamemode() {
        Path gamemodeFilePath = Paths.get(CONFIG_DIR, CURRENT_GAMEMODE_FILE);

        if (!Files.exists(gamemodeFilePath)) {
            saveCurrentGamemode(DEFAULT_GAMEMODE);
            currentGamemode = DEFAULT_GAMEMODE;
            return;
        }

        try {
            Yaml yaml = new Yaml();
            try (InputStream inputStream = new FileInputStream(gamemodeFilePath.toFile())) {
                Map<String, Object> config = yaml.load(inputStream);
                currentGamemode = (String) config.get("current_gamemode");
                if (currentGamemode == null) {
                    currentGamemode = DEFAULT_GAMEMODE;
                    saveCurrentGamemode(DEFAULT_GAMEMODE);
                }
            }
        } catch (Exception e) {
            System.err.println("加载当前玩法配置失败");
            currentGamemode = DEFAULT_GAMEMODE;
            saveCurrentGamemode(DEFAULT_GAMEMODE);
        }
    }

    public static void saveCurrentGamemode(String gamemode) {
        try {
            Path gamemodeFilePath = Paths.get(CONFIG_DIR, CURRENT_GAMEMODE_FILE);
            Map<String, String> config = new HashMap<>();
            config.put("current_gamemode", gamemode);

            Yaml yaml = new Yaml();
            try (FileWriter writer = new FileWriter(gamemodeFilePath.toFile())) {
                yaml.dump(config, writer);
            }

            currentGamemode = gamemode;
        } catch (Exception e) {
            System.err.println("保存当前玩法配置失败");
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
               "    - \"/say " + gamemode + " 玩法比赛开始！\"\n";
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
            saveCurrentGamemode(gamemode);

            return true;
        } catch (Exception e) {
            System.err.println("切换玩法失败: " + gamemode);
            return false;
        }
    }
}