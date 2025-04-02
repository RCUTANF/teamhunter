package com.rcutanf.teamhunter;

import net.minecraft.server.MinecraftServer;

public class CommandExecutor {
    public static void executeCommand(MinecraftServer server, String command) {
        // 直接使用命令管理器的方法
        server.getCommandManager().executeWithPrefix(
                server.getCommandSource().withLevel(4),
                command
        );
    }
}
