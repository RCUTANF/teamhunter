package com.rcutanf.teamhunter.advancement;

import com.rcutanf.teamhunter.NetWorking;
import com.rcutanf.teamhunter.Phase;
import com.rcutanf.teamhunter.Teamhunter;
import com.rcutanf.teamhunter.CommandExecutor;
import net.minecraft.advancement.AdvancementEntry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.network.packet.s2c.common.CustomPayloadS2CPacket;

public class AdvancementListener {
    private final MinecraftServer server;
    private final AdvancementScoreLoader scoreLoader;

    // 直接使用变量存储队伍分数
    private static int huntersScore = 0;
    private static int runnersScore = 0;

    private static boolean huntersHadAdvantage = false;
    private static boolean runnersHadAdvantage = false;

    public AdvancementListener(MinecraftServer server) {
        this.server = server;
        this.scoreLoader = new AdvancementScoreLoader();
        PlayerAdvancementCallback.EVENT.register(this::onAdvancement);
    }

    private void onAdvancement(ServerPlayerEntity player, Text title, AdvancementEntry advancementEntry) {
        // 跳过非比赛阶段
        if (Teamhunter.phaseManager.Phase() != Phase.MATCH) return;

        Identifier id = advancementEntry.id();
        int score = scoreLoader.getScore(id);
        if (score <= 0) return;

        // 只处理指定队伍的玩家
        String teamName = player.getScoreboardTeam() != null
                ? player.getScoreboardTeam().getName()
                : "";

        if (!teamName.equals("hunters") && !teamName.equals("runners")) return;

        // 添加队伍分数（不再使用计分板，而是直接更新变量）
        int huntersAddedScore = 0;
        int runnersAddedScore = 0;

        if (teamName.equals("hunters")) {
            huntersScore += score;
            huntersAddedScore = score;
        } else {
            runnersScore += score;
            runnersAddedScore = score;
        }

        // 调试信息（直接在控制台打印）
        String playerName = player.getName().getString();
        String achievement = title != null ? title.getString() : id.toString();
        CommandExecutor.executeCommand(server,
                String.format("say %s 为 %s 队获得成就 %s (+%d分)",
                        playerName, teamName, achievement, score));



        // 替换原有代码
        applyTeamAdvantageEffects(server, huntersScore, runnersScore);
        // 发送分数更新包
        sendTeamScoreUpdate(player.getServerWorld(), huntersScore, runnersScore, huntersAddedScore, runnersAddedScore);
    }

    // 获取当前猎人队伍分数
    public static int getHuntersScore() {
        return huntersScore;
    }

    // 获取当前逃亡者队伍分数
    public static int getRunnersScore() {
        return runnersScore;
    }

    // 重置分数（比赛开始时调用）
    public static void resetScores() {
        huntersScore = 0;
        runnersScore = 0;
    }

    public static void sendTeamScoreUpdate(ServerWorld world, int huntersScore, int runnersScore,
                                           int huntersAddedScore, int runnersAddedScore) {
        NetWorking.TeamScorePacket packet = new NetWorking.TeamScorePacket(huntersScore, runnersScore, huntersAddedScore, runnersAddedScore);

        for (ServerPlayerEntity player : world.getPlayers()) {
            player.networkHandler.sendPacket(new CustomPayloadS2CPacket(packet));
        }
    }

    /**
     * 减少指定队伍的分数
     * @param teamName 队伍名称 ("hunters" 或 "runners")
     * @param amount 要减少的分数值
     * @param world 服务器世界实例，用于发送分数更新包
     * @param message 可选的消息，解释为什么减少分数
     * @return 减少后的新分数
     */
    public static int reduceTeamScore(String teamName, int amount, ServerWorld world, String message) {
        // 验证参数
        if (amount <= 0) {
            return teamName.equals("hunters") ? huntersScore : runnersScore;
        }

        // 减少对应队伍的分数
        int huntersReducedScore = 0;
        int runnersReducedScore = 0;

        if (teamName.equals("hunters")) {
            huntersScore = Math.max(0, huntersScore - amount); // 确保分数不会变为负数
            huntersReducedScore = amount;
        } else if (teamName.equals("runners")) {
            runnersScore = Math.max(0, runnersScore - amount); // 确保分数不会变为负数
            runnersReducedScore = amount;
        } else {
            // 如果队伍名称无效，返回0
            return 0;
        }

        // 更新计分板
        MinecraftServer server = world.getServer();
        CommandExecutor.executeCommand(server,
                "scoreboard players set hunters TeamScore " + huntersScore
        );
        CommandExecutor.executeCommand(server,
                "scoreboard players set runners TeamScore " + runnersScore
        );

        // 如果提供了消息，显示减分通知
        if (message != null && !message.isEmpty()) {
            CommandExecutor.executeCommand(server,
                    String.format("say %s 队扣除 %d 分: %s",
                            teamName, amount, message));
        }

        // 发送分数更新包，使用负值表示减少的分数
        sendTeamScoreUpdate(world, huntersScore, runnersScore, -huntersReducedScore, -runnersReducedScore);

        // 返回当前队伍的新分数
        return teamName.equals("hunters") ? huntersScore : runnersScore;
    }

    private void applyTeamAdvantageEffects(MinecraftServer server, int huntersScore, int runnersScore) {
        // 存储上一次的领先状态


        boolean huntersHaveAdvantage = huntersScore - runnersScore > 100;
        boolean runnersHaveAdvantage = runnersScore - huntersScore > 100;

        // 设置领先后的效果
        if (huntersHaveAdvantage) {
            // 猎人领先100分，启用猎人的指南针追踪功能
            CommandExecutor.executeCommand(server,
                    "execute as @a[team=hunters] run scoreboard players set 猎人追踪器:显示距离 mh.settings 1");
            // 关闭逃亡者的指南针追踪功能
            CommandExecutor.executeCommand(server,
                    "execute as @a[team=runners] run scoreboard players set 逃者追踪器:显示距离 mh.settings 0");

            // 发送网络包通知客户端更新UI
            if (!huntersHadAdvantage) {
                sendAdvantageBuffUpdate(server.getOverworld(), true, true);
                sendAdvantageBuffUpdate(server.getOverworld(), false, false);
                huntersHadAdvantage = true;
                runnersHadAdvantage = false;
            }
        } else if (runnersHaveAdvantage) {
            // 逃亡者领先100分，启用逃亡者的指南针追踪功能
            CommandExecutor.executeCommand(server,
                    "execute as @a[team=runners] run scoreboard players set 逃者追踪器:显示距离 mh.settings 1");
            // 关闭猎人的指南针追踪功能
            CommandExecutor.executeCommand(server,
                    "execute as @a[team=hunters] run scoreboard players set 猎人追踪器:显示距离 mh.settings 0");

            // 发送网络包通知客户端更新UI
            if (!runnersHadAdvantage) {
                sendAdvantageBuffUpdate(server.getOverworld(), false, true);
                sendAdvantageBuffUpdate(server.getOverworld(), true, false);
                runnersHadAdvantage = true;
                huntersHadAdvantage = false;
            }
        } else {
            // 无领先优势，关闭所有人的指南针追踪功能
            CommandExecutor.executeCommand(server,
                    "execute as @a[team=hunters] run scoreboard players set 猎人追踪器:显示距离 mh.settings 0");
            CommandExecutor.executeCommand(server,
                    "execute as @a[team=runners] run scoreboard players set 逃者追踪器:显示距离 mh.settings 0");

            // 如果之前有队伍有优势，现在取消
            if (huntersHadAdvantage || runnersHadAdvantage) {
                sendAdvantageBuffUpdate(server.getOverworld(), true, false);
                sendAdvantageBuffUpdate(server.getOverworld(), false, false);
                huntersHadAdvantage = false;
                runnersHadAdvantage = false;
            }
        }
    }

    // 添加一个新方法发送优势Buff更新包
    private void sendAdvantageBuffUpdate(ServerWorld world, boolean isHunterTeam, boolean hasAdvantage) {
        // 创建一个新的优势Buff数据包
        NetWorking.AdvantageBuffPacket packet =
                new NetWorking.AdvantageBuffPacket(isHunterTeam, hasAdvantage);

        // 使用与其他数据包一致的发送方式
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            player.networkHandler.sendPacket(new CustomPayloadS2CPacket(packet));
        }
    }
}