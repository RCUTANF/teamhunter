package com.rcutanf.teamhunter.client.guide_sys.impl;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.rcutanf.teamhunter.client.guide_sys.AbstractGuideSysChecker;
import com.rcutanf.teamhunter.client.guide_sys.def.AchievementDefinition;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class AchievementLoader {

    private static final Gson GSON = new Gson();

    // ✅ 模组ID（请替换为你的实际 modid）
    private static final String MOD_ID = "teamhunter";

    // ✅ 成就配置在 JAR 中的路径（和资源包一致，便于管理）
    private static final String ACHIEVEMENTS_PATH = "data/" + MOD_ID + "/achievements/";

    private static final List<AchievementDefinition> loadedDefinitions = new ArrayList<>();

    public static void loadAllAchievements() {
        List<String> resourcePaths = findAchievementResources();
        int loadedCount = 0;

        for (String resourcePath : resourcePaths) {
            try (InputStream stream = AchievementLoader.class.getClassLoader()
                    .getResourceAsStream(resourcePath);
                 InputStreamReader reader = new InputStreamReader(stream)) {

                if (stream == null) {
                    System.err.println("⚠ 资源未找到: " + resourcePath);
                    continue;
                }

                AchievementDefinition definition = GSON.fromJson(reader, AchievementDefinition.class);
                if (definition == null || definition.id == null || definition.advancementId == null) {
                    System.err.println("⚠ 无效成就配置: " + resourcePath);
                    continue;
                }

                loadedDefinitions.add(definition);
                loadedCount++;
                System.out.println("✔ 成就已加载（JAR内）: " + definition.id + " (" + resourcePath + ")");

            } catch (JsonSyntaxException e) {
                System.err.println("❌ JSON 格式错误: " + resourcePath + " - " + e.getMessage());
            } catch (IOException e) {
                System.err.println("❌ 读取失败: " + resourcePath + " - " + e.getMessage());
            }
        }

        System.out.println("✅ 已从 JAR 内加载 " + loadedCount + " 个成就");
    }

    // ✅ 查找所有 data/teamhunter/achievements/*.json 文件
    private static List<String> findAchievementResources() {
        List<String> paths = new ArrayList<>();
        try {
            Enumeration<URL> resources = AchievementLoader.class.getClassLoader()
                    .getResources(ACHIEVEMENTS_PATH);

            while (resources.hasMoreElements()) {
                URL url = resources.nextElement();
                String protocol = url.getProtocol();

                if ("file".equals(protocol)) {
                    // 处理文件系统中的资源
                    File directory = new File(url.toURI());
                    if (directory.isDirectory()) {
                        File[] files = directory.listFiles((dir, name) -> name.endsWith(".json"));
                        if (files != null) {
                            for (File file : files) {
                                paths.add(ACHIEVEMENTS_PATH + file.getName());
                            }
                        }
                    }
                } else if ("jar".equals(protocol)) {
                    // 处理 JAR 包中的资源
                    String jarPath = url.getPath().substring(5, url.getPath().indexOf("!"));
                    try (JarFile jarFile = new JarFile(jarPath)) {
                        Enumeration<JarEntry> entries = jarFile.entries();
                        while (entries.hasMoreElements()) {
                            JarEntry entry = entries.nextElement();
                            String entryName = entry.getName();
                            if (entryName.startsWith(ACHIEVEMENTS_PATH) && entryName.endsWith(".json")) {
                                paths.add(entryName);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("❌ 查找成就资源失败: " + e.getMessage());
        }

        return paths;
    }

    //在checker初始化调用，属于运行时数据
    public static void convertDefinitionsToCheckers() {
        System.out.println("开始创建成就检查器...");
        int count = 0;

        for (AchievementDefinition definition : loadedDefinitions) {
            new AbstractGuideSysChecker(definition);
            count++;
        }

        System.out.println("✅ 已创建 " + count + " 个成就检查器");
    }
}