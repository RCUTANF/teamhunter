package com.rcutanf.teamhunter;

import com.rcutanf.teamhunter.advancement.AdvancementListener;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.RaycastContext;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerVisibilityTracker {
    private final MinecraftServer server;
    private final Map<UUID, Map<UUID, Boolean>> playerVisibility = new HashMap<>();
    private int tickCounter = 0;
    private static final int VISIBILITY_CHECK_INTERVAL = 2; // 每2tick检查一次

    public PlayerVisibilityTracker(MinecraftServer server) {
        this.server = server;
    }

    public void tick() {
        tickCounter++;

        // 每2tick执行一次可见性检查
        if (tickCounter >= VISIBILITY_CHECK_INTERVAL) {
            tickCounter = 0;
            checkPlayerVisibility();
        }
    }

    // 检查所有玩家的可见性
    private void checkPlayerVisibility() {
        for (ServerPlayerEntity observer : server.getPlayerManager().getPlayerList()) {
            UUID observerId = observer.getUuid();

            // 获取或创建观察者的可见性映射
            Map<UUID, Boolean> visibilityMap = playerVisibility.computeIfAbsent(observerId, k -> new HashMap<>());

            // 获取观察者的维度
            Identifier observerDimension = observer.getServerWorld().getRegistryKey().getValue();

            for (ServerPlayerEntity target : server.getPlayerManager().getPlayerList()) {
                UUID targetId = target.getUuid();

                // 跳过自己
                if (observerId.equals(targetId)) continue;

                // 检查是否在同一维度
                Identifier targetDimension = target.getServerWorld().getRegistryKey().getValue();
                if (!observerDimension.equals(targetDimension)) {
                    visibilityMap.put(targetId, false);
                    continue;
                }

                // 检查可见性
                boolean isVisible = checkIfPlayerIsVisible(observer, target);

                // 更新可见性状态
                boolean previousVisibility = visibilityMap.getOrDefault(targetId, false);
                visibilityMap.put(targetId, isVisible);

                // 如果可见性状态发生变化，发送更新
                if (isVisible != previousVisibility) {
                    sendVisibilityUpdate(observer, target, isVisible);
                }
            }
        }
    }

    // 检查一个玩家是否对另一个玩家可见
    private boolean checkIfPlayerIsVisible(ServerPlayerEntity observer, ServerPlayerEntity target) {
        //检查buff，存在buff则始终可见
        //TODO：这是个临时解决方案，后续要改到buff系统
        if (TeamUtils.getPlayerTeam(target).getName().equals("hunters")) {
            if (AdvancementListener.getLastAdvantageState() == AdvancementListener.AdvantageState.RUNNERS_ADVANTAGE) {
                return true; // 如果逃者队有优势，则始终可见
            }
        }
        if(TeamUtils.getPlayerTeam(observer).getName().equals("runners")){
            if (AdvancementListener.getLastAdvantageState() == AdvancementListener.AdvantageState.HUNTERS_ADVANTAGE) {
                return true; // 如果猎人队有优势，则始终可见
            }
        }

        // 检查距离 - 如果超出渲染距离则不可见
        int viewDistance = server.getPlayerManager().getViewDistance();
        double renderDistance = viewDistance * 16; // 块数转为坐标距离
        double distanceSquared = observer.squaredDistanceTo(target);
        if (distanceSquared > renderDistance * renderDistance) {
            return false;
        }

        // 检查视角 - 目标是否在观察者的视野内
        if (!isInFieldOfView(observer, target)) {
            return false;
        }

        // 射线检测 - 检查从观察者到目标的三条射线
        return isRayTraceVisible(observer, target);
    }

    // 检查目标是否在观察者的视野范围内
    private boolean isInFieldOfView(ServerPlayerEntity observer, ServerPlayerEntity target) {
        // 获取观察者的视线向量
        Vec3d observerPos = observer.getEyePos();
        Vec3d observerLook = Vec3d.fromPolar(observer.getPitch(), observer.getYaw());

        // 获取从观察者到目标的向量
        Vec3d targetPos = target.getPos().add(0, target.getEyeHeight(target.getPose()) / 2, 0); // 使用目标的中点
        Vec3d toTarget = targetPos.subtract(observerPos).normalize();

        // 计算夹角的余弦值（点积）
        double dotProduct = observerLook.dotProduct(toTarget);

        // 视野范围约为140度，cos(70°) ≈ 0.342
        return dotProduct > 0.342;
    }

    // 执行射线检测，检查从观察者到目标的三条射线是否被阻挡
    private boolean isRayTraceVisible(ServerPlayerEntity observer, ServerPlayerEntity target) {
        ServerWorld world = observer.getServerWorld();
        Vec3d observerEyes = observer.getEyePos();

        // 目标玩家的两个关键点（头部和脚部）
        Vec3d targetHead = target.getEyePos();
        Vec3d targetFeet = target.getPos();

        // 如果任何一条射线未被阻挡，则认为目标可见
        boolean headVisible = !isRayBlocked(world, observerEyes, targetHead);
        boolean feetVisible = !isRayBlocked(world, observerEyes, targetFeet);

        return headVisible || feetVisible;
    }

    /**
     * 检查从起点到终点的射线是否被方块阻挡
     * @param world 服务器世界
     * @param start 射线起点
     * @param end 射线终点
     * @return 如果射线被方块阻挡则返回true
     */
    private boolean isRayBlocked(ServerWorld world, Vec3d start, Vec3d end) {
        // 计算射线方向向量
        Vec3d direction = end.subtract(start).normalize();

        // 计算射线总长度
        double length = start.distanceTo(end);

        // 射线步长 (较小的步长提高精度，但会增加计算量)
        double stepSize = 0.5;
        int steps = (int) Math.ceil(length / stepSize);

        // 沿射线进行迭代检查
        for (int i = 1; i < steps; i++) {
            // 计算当前检查点 (跳过起点附近，避免检测到玩家自身所在方块)
            double progress = i * stepSize / length;
            if (progress >= 1.0) break; // 确保不超过终点

            Vec3d checkPos = start.add(direction.multiply(i * stepSize));

            // 获取该位置的方块状态
            BlockPos blockPos = new BlockPos(
                (int) Math.floor(checkPos.getX()),
                (int) Math.floor(checkPos.getY()),
                (int) Math.floor(checkPos.getZ())
            );

            // 检查该位置的方块是否为固体 (阻挡视线)
            if (world.getBlockState(blockPos).isOpaque()) {
                return true; // 射线被阻挡
            }
        }

        // 射线没有被任何方块阻挡
        return false;
    }

    // 发送可见性更新到客户端
    private void sendVisibilityUpdate(ServerPlayerEntity observer, ServerPlayerEntity target, boolean isVisible) {
        // 创建一个可见性更新数据包
        NetWorking.PlayerVisibilityUpdatePacket packet = new NetWorking.PlayerVisibilityUpdatePacket(
            target.getUuid(), target.getName().getString(), isVisible);

        //  向所有玩家发送可见性更新数据包
        for(ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            ServerPlayNetworking.send(player, packet);
        }
    }

    /**
     * 向新登录的玩家发送所有其他玩家的可见性信息
     * @param newPlayer 新登录的玩家
     */
    public void sendAllVisibilityToPlayer(ServerPlayerEntity newPlayer) {
        UUID newPlayerId = newPlayer.getUuid();

        // 获取新玩家的可见性映射
        Map<UUID, Boolean> visibilityMap = playerVisibility.computeIfAbsent(newPlayerId, k -> new HashMap<>());

        // 遍历所有其他玩家
        for (ServerPlayerEntity target : server.getPlayerManager().getPlayerList()) {
            UUID targetId = target.getUuid();

            boolean isVisible = false;

            // 从可见性映射中获取状态，如果不存在则执行可见性检查
            isVisible = visibilityMap.computeIfAbsent(targetId,
                    k -> checkIfPlayerIsVisible(newPlayer, target));

            // 创建数据包并发送给新玩家
            NetWorking.PlayerVisibilityUpdatePacket packet = new NetWorking.PlayerVisibilityUpdatePacket(
                targetId, target.getName().getString(), isVisible);

            ServerPlayNetworking.send(newPlayer, packet);
        }
    }
}