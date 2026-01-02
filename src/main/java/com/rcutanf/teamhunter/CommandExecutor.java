package com.rcutanf.teamhunter;

import net.minecraft.command.permission.LeveledPermissionPredicate;
import net.minecraft.server.MinecraftServer;

public class CommandExecutor {
    public static void executeCommand(MinecraftServer server, String command) {
        // 使用新版本的 parseAndExecute 方法
        server.getCommandManager().parseAndExecute(
                server.getCommandSource().withPermissions(LeveledPermissionPredicate.OWNERS),
                command
        );
    }
}
