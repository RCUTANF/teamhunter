package com.rcutanf.teamhunter.buff;

import com.rcutanf.teamhunter.EnvironmentController;
import com.rcutanf.teamhunter.TeamUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;

/**
 * 烈焰人无掉落Debuff - 阻止特定队伍从烈焰人身上获得烈焰棒
 */
public class NoBlazeDroppingDebuff extends Buff {
    private static final int BUFF_ID = 1001;
    private static final String BUFF_NAME = "烈焰人无掉落";
    private static final String BUFF_DESCRIPTION = "阻止队伍成员从烈焰人身上获得烈焰棒";

    // 受影响的队伍名称
    private final String teamName;

    /**
     * 构造函数
     * @param server Minecraft服务器实例
     * @param teamName 受影响的队伍名称
     */
    public NoBlazeDroppingDebuff(MinecraftServer server, String teamName) {
        super(server);
        this.teamName = teamName;
    }

    @Override
    protected int getId() {
        return BUFF_ID;
    }

    @Override
    protected String getName() {
        return BUFF_NAME;
    }

    @Override
    protected String getDescription() {
        return BUFF_DESCRIPTION;
    }

    @Override
    protected int getDuration() {
        return -1; // 永久效果，由外部控制
    }

    @Override
    protected boolean isPositive() {
        return false; // 负面效果
    }

    /**
     * 根据队伍优势更新此Buff状态
     */
    public void updateFromAdvantage(EnvironmentController.TeamAdvantage advantage) {
        boolean shouldBeActive = false;

        if ("hunters".equals(teamName) && advantage == EnvironmentController.TeamAdvantage.RUNNERS) {
            shouldBeActive = true; // 逃亡者占优势时，猎人队受到debuff
        } else if ("runners".equals(teamName) && advantage == EnvironmentController.TeamAdvantage.HUNTERS) {
            shouldBeActive = true; // 猎人占优势时，逃亡者队受到debuff
        }

        // 更新buff状态
        if (shouldBeActive && !isActive) {
            initialize();
            applyEffect();
        } else if (!shouldBeActive && isActive) {
            removeEffect();
            dispel();
        }
    }

    @Override
    public void applyEffect() {
        if (!isActive) return;

        // 移除队伍中所有在地狱玩家的烈焰棒掉落权限
        for (String playerName : TeamUtils.getTeamPlayerNames(server, teamName)) {
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(playerName);
            if (player != null && player.getWorld().getRegistryKey() == World.NETHER) {
                player.removeCommandTag("can_drop_blaze_rod");
            }
        }
    }

    @Override
    public void removeEffect() {
        // 恢复队伍中所有在地狱玩家的烈焰棒掉落权限
        for (String playerName : TeamUtils.getTeamPlayerNames(server, teamName)) {
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(playerName);
            if (player != null && player.getWorld().getRegistryKey() == World.NETHER) {
                player.addCommandTag("can_drop_blaze_rod");
            }
        }
    }

    /**
     * 对新进入地狱的玩家应用效果
     */
    public void applyToPlayer(ServerPlayerEntity player) {
        if (!isActive) return;

        if (player != null && TeamUtils.isPlayerInTeam(player, teamName)) {
            player.removeCommandTag("can_drop_blaze_rod");
        }
    }

    /**
     * 获取受影响的队伍名称
     */
    public String getTeamName() {
        return teamName;
    }
}