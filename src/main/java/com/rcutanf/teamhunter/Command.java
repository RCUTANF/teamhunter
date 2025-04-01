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
                );

        dispatcher.register(cmd);

    }

    // TODO: inspect the start method
    private static int start(CommandContext<ServerCommandSource> context) {
        var server = context.getSource().getServer();
        var minutes = IntegerArgumentType.getInteger(context, MINUTES);
        Teamhunter.phaseManager.clear().then(Phase.WARMUP, Duration.ofMinutes(minutes))
                .then(Phase.PREPARE, Duration.ofSeconds(10))
                .then(Phase.MATCH);
        return SINGLE_SUCCESS;
    }

    // TODO: Implement the cancel method
    private static int cancel(CommandContext<ServerCommandSource> context) {
        return SINGLE_SUCCESS;
    }
}
