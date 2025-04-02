package com.rcutanf.teamhunter;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import java.time.Duration;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

// /match start minutes:int 进入热身阶段。该阶段可以随时通过/matchCommand cancel 取消比赛进程
public class Command {

    final static String BASE_COMMAND = "match";
    final static String START = "start";
    final static String CANCEL = "cancel";
    final static String MINUTES = "minutes";
    final static String END = "end";
    final static String PARSE = "parse";
    final static String RESUME = "resume";


    private Command() {
    }

    static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {

        var cmd = literal(BASE_COMMAND)
                .then(literal(START)
                        .then(argument(MINUTES, IntegerArgumentType.integer(0))
                                .executes(Command::start)
                        )
                ).then(literal(CANCEL)
                        .executes(Command::cancel)
                )
                .then(literal(END)
                        .executes(Command::end)
                )
                .then(literal(PARSE)
                        .executes(Command::parse)
                )
                .then(literal(RESUME)
                        .executes(Command::resume)
                );

        dispatcher.register(cmd);

    }

    /**
     * 开始比赛
     *
     * @param context
     * @return
     */
    private static int start(CommandContext<ServerCommandSource> context) {
        var server = context.getSource().getServer();
        var minutes = IntegerArgumentType.getInteger(context, MINUTES);
        Teamhunter.phaseManager.clear().then(Phase.WARMUP, Duration.ofMinutes(minutes))
                .then(Phase.PREPARE, Duration.ofSeconds(10))
                .then(Phase.MATCH);
        return SINGLE_SUCCESS;
    }

    /**
     * 取消比赛
     *
     * @param context
     * @return
     */
    private static int cancel(CommandContext<ServerCommandSource> context) {
        //检查是否是热身阶段
        if (Teamhunter.phaseManager.Phase() == Phase.WARMUP) {
            CommandExecutor.executeCommand(context.getSource().getServer(), "/luckperms group default permission set minecraft.command.trigger.* false");
            CommandExecutor.executeCommand(context.getSource().getServer(), "/say §4比赛已取消");
            Teamhunter.phaseManager.clear();
            Teamhunter.phaseManager.then(Phase.WAITING);
            return SINGLE_SUCCESS;
        }
        else {
            CommandExecutor.executeCommand(context.getSource().getServer(), "/say §4比赛已开始，无法取消");
            return SINGLE_SUCCESS;
        }
    }
    /**
     * 结束比赛
     *
     * @param context
     * @return
     */
    private static int end(CommandContext<ServerCommandSource> context) {
        var server = context.getSource().getServer();
        CommandExecutor.executeCommand(server, "/say §4比赛已结束");
        CommandExecutor.executeCommand(server, "/luckperms group default permission set minecraft.command.trigger.* true");
        CommandExecutor.executeCommand(server, "/luckperms group default permission set minecraft.command.gamemode true");
        Teamhunter.phaseManager.clear();
        return SINGLE_SUCCESS;
    }
    /**
     * 暂停比赛
     *
     * @param context
     * @return
     */
    private static int parse(CommandContext<ServerCommandSource> context) {
        var server = context.getSource().getServer();
        PhaseHandler.FreezeAllPlayers(server);
        return SINGLE_SUCCESS;
    }
    /**
     * 恢复比赛
     *
     * @param context
     * @return
     */
    private static int resume(CommandContext<ServerCommandSource> context) {
        var server = context.getSource().getServer();
        PhaseHandler.unFreezeAllPlayers(server);
        return SINGLE_SUCCESS;
    }
}
