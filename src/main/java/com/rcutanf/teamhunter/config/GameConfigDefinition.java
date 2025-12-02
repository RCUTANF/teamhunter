package com.rcutanf.teamhunter.config;

import java.util.List;
import java.util.Map;

/**
 * 游戏配置定义的数据结构
 * 对应 teamhunter.yaml 和 commands_*.yaml 文件
 */
public class GameConfigDefinition {
    public String currentGamemode; // 当前选中的游戏模式
    public Map<String, GamemodeConfig> gamemodes; // 各游戏模式的配置

    public static class GamemodeConfig {
        public int matchDuration; // 比赛时长（分钟）
        public PhaseCommands phases; // 各阶段命令配置

        public static class PhaseCommands {
            public List<String> warmup; // 热身阶段命令
            public List<String> prepare; // 准备阶段命令
            public List<String> match; // 比赛阶段命令
            public List<String> end; // 结束阶段命令
        }
    }
}