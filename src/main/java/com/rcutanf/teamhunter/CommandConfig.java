package com.rcutanf.teamhunter;

import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CommandConfig {
    private static final String CONFIG_DIR = "config/teamhunter";
    private static final String CONFIG_FILE = "commands.yaml";
    private static Map<String, List<String>> phaseCommands = new HashMap<>();

    public static void loadConfig() {
        try {
            // 确保配置目录存在
            Path configDirPath = Paths.get(CONFIG_DIR);
            if (!Files.exists(configDirPath)) {
                Files.createDirectories(configDirPath);
            }

            // 配置文件完整路径
            Path configFilePath = configDirPath.resolve(CONFIG_FILE);

            // 如果配置文件不存在，从资源文件复制默认配置
            if (!Files.exists(configFilePath)) {
                createDefaultConfig(configFilePath);
            }

            // 从外部配置文件加载
            try (InputStream inputStream = new FileInputStream(configFilePath.toFile())) {
                Yaml yaml = new Yaml();
                Map<String, Object> config = yaml.load(inputStream);

                Map<String, Object> phases = (Map<String, Object>) config.get("phases");
                for (Map.Entry<String, Object> entry : phases.entrySet()) {
                    String phaseName = entry.getKey();
                    List<String> commands = (List<String>) entry.getValue();
                    phaseCommands.put(phaseName, commands);
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to load commands config");
            System.err.println(e);
            phaseCommands = new HashMap<>();
        }
    }

    private static void createDefaultConfig(Path configFilePath) throws IOException {
        // 从jar包内部的资源文件中复制默认配置
        try (InputStream defaultConfig = CommandConfig.class.getClassLoader().getResourceAsStream("commands.yaml")) {
            if (defaultConfig != null) {
                Files.copy(defaultConfig, configFilePath);
            } else {
                // 如果默认配置不存在，创建一个基础的配置文件
                Files.write(configFilePath, getDefaultConfigContent().getBytes());
            }
        }
    }

    private static String getDefaultConfigContent() {
        return "phases:\n" +
               "  warmup:\n" +
               "    # 这里是热身阶段的命令\n" +
               "    - \"/say 热身阶段开始\"\n" +
               "  prepare:\n" +
               "    # 这里是准备阶段的命令\n" +
               "    - \"/say 准备阶段开始\"\n" +
               "  match:\n" +
               "    # 这里是比赛阶段的命令\n" +
               "    - \"/say 比赛开始！\"\n";
    }

    public static List<String> getCommandsForPhase(String phase) {
        return phaseCommands.getOrDefault(phase, Collections.emptyList());
    }
}