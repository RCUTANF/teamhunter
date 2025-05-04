package com.rcutanf.teamhunter;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import java.time.Duration;
import java.util.Arrays;

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
    final static String SET_PHASE = "setphase";
    final static String SECONDS = "seconds";
    final static String RESURRECTION = "resurrection";
    final static String INVINCIBILITY = "invincibility";
    final static String INVISIBILITY = "invisibility";
    final static String SPEED = "speed";

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
                )
                .then(literal(SET_PHASE)
                        .then(argument("phase", StringArgumentType.word())
                                .suggests((context, builder) -> {
                                    Arrays.stream(Phase.values())
                                            .map(Phase::name)
                                            .forEach(builder::suggest);
                                    builder.suggest("NONE");
                                    return builder.buildFuture();
                                })
                                .executes(Command::setPhase)
                                .then(argument(SECONDS, IntegerArgumentType.integer(0))
                                        .executes(Command::setPhaseWithSeconds)
                                )
                        )
                )
                .then(literal(RESURRECTION)
                        .then(literal(INVINCIBILITY)
                                .then(argument("enabled", BoolArgumentType.bool())
                                        .executes(Command::setResurrectionInvincibility)
                                )
                        )
                        .then(literal(INVISIBILITY)
                                .then(argument("enabled", BoolArgumentType.bool())
                                        .executes(Command::setResurrectionInvisibility)
                                )
                        )
                        .then(literal(SPEED)
                                .then(argument("enabled", BoolArgumentType.bool())
                                        .executes(Command::setResurrectionSpeed)
                                )
                        )
                        .executes(Command::showResurrectionStatus)
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
            //清除无敌效果
            CommandExecutor.executeCommand(context.getSource().getServer(), "/effect clear @a[team=runners] minecraft:resistance");
            CommandExecutor.executeCommand(context.getSource().getServer(), "/effect clear @a[team=hunters] minecraft:resistance");
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
        PhaseHandler.matchEnd(context.getSource().getServer());
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
    /**
     * 设置当前phase
     *
     * @param context 命令上下文
     * @return 执行结果
     */
    private static int setPhase(CommandContext<ServerCommandSource> context) {
        String phaseName = StringArgumentType.getString(context, "phase");
        var server = context.getSource().getServer();

        if (phaseName.equalsIgnoreCase("NONE")) {
            Teamhunter.phaseManager.clear();
            context.getSource().sendFeedback(() -> Text.of("已清除当前阶段"), true);
            return SINGLE_SUCCESS;
        }

        try {
            Phase phase = Phase.valueOf(phaseName.toUpperCase());
            Teamhunter.phaseManager.clear().then(phase);
            context.getSource().sendFeedback(() -> Text.of("已设置当前阶段为: " + phase.name()), true);
            return SINGLE_SUCCESS;
        } catch (IllegalArgumentException e) {
            context.getSource().sendError(Text.of("无效的阶段名称: " + phaseName));
            return 0;
        }
    }

    /**
     * 设置当前phase及其计时器秒数
     *
     * @param context 命令上下文
     * @return 执行结果
     */
    private static int setPhaseWithSeconds(CommandContext<ServerCommandSource> context) {
        String phaseName = StringArgumentType.getString(context, "phase");
        int seconds = IntegerArgumentType.getInteger(context, SECONDS);
        var server = context.getSource().getServer();

        if (phaseName.equalsIgnoreCase("NONE")) {
            Teamhunter.phaseManager.clear();
            context.getSource().sendFeedback(() -> Text.of("已清除当前阶段"), true);
            return SINGLE_SUCCESS;
        }

        try {
            Phase phase = Phase.valueOf(phaseName.toUpperCase());
            Teamhunter.phaseManager.clear().then(phase, Duration.ofSeconds(seconds));
            context.getSource().sendFeedback(() -> Text.of("已设置当前阶段为: " + phase.name() + ", 计时器: " + seconds + "秒"), true);
            return SINGLE_SUCCESS;
        } catch (IllegalArgumentException e) {
            context.getSource().sendError(Text.of("无效的阶段名称: " + phaseName));
            return 0;
        }
    }

    /**
     * 设置复活保护无敌
     *
     * @param context 命令上下文
     * @return 执行结果
     */
    private static int setResurrectionInvincibility(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        boolean enabled = BoolArgumentType.getBool(context, "enabled");
        ResurrectionProtection.setInvincibilityEnabled(enabled);
        context.getSource().sendFeedback(() -> Text.of("复活无敌效果已" + (enabled ? "启用" : "禁用")), true);
        return SINGLE_SUCCESS;
    }

    private static int setResurrectionInvisibility(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        boolean enabled = BoolArgumentType.getBool(context, "enabled");
        ResurrectionProtection.setInvisibilityEnabled(enabled);
        context.getSource().sendFeedback(() -> Text.of("复活隐身效果已" + (enabled ? "启用" : "禁用")), true);
        return SINGLE_SUCCESS;
    }

    private static int setResurrectionSpeed(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        boolean enabled = BoolArgumentType.getBool(context, "enabled");
        ResurrectionProtection.setSpeedEnabled(enabled);
        context.getSource().sendFeedback(() -> Text.of("复活速度效果已" + (enabled ? "启用" : "禁用")), true);
        return SINGLE_SUCCESS;
    }

    private static int showResurrectionStatus(CommandContext<ServerCommandSource> context) {
        String status = "复活保护效果状态:\n" +
                "- 无敌: " + (ResurrectionProtection.isInvincibilityEnabled() ? "开启" : "关闭") + "\n" +
                "- 隐身: " + (ResurrectionProtection.isInvisibilityEnabled() ? "开启" : "关闭") + "\n" +
                "- 速度: " + (ResurrectionProtection.isSpeedEnabled() ? "开启" : "关闭") + "\n" +
                "持续时间: 10秒";
        context.getSource().sendFeedback(() -> Text.of(status), false);
        return SINGLE_SUCCESS;
    }
}
