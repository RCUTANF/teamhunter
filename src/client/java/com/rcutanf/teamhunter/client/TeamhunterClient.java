package com.rcutanf.teamhunter.client;

import com.rcutanf.teamhunter.NetWorking;
import com.rcutanf.teamhunter.NetWorking.CounterSyncPacket;
import com.rcutanf.teamhunter.Phase;
import com.rcutanf.teamhunter.Teamhunter;
import com.rcutanf.teamhunter.client.guide_sys.GuideSysCheckerManager;
import com.rcutanf.teamhunter.client.guide_sys.GuideSysTriggerManager;
import com.rcutanf.teamhunter.client.guide_sys.advancementListener.AdvancementEventManager;
import com.rcutanf.teamhunter.client.guide_sys.gui.GuideSysHud;
import com.rcutanf.teamhunter.client.guide_sys.gui.data.AdvancementDataCache;
import com.rcutanf.teamhunter.client.guide_sys.gui.data.StructureCache;
import com.rcutanf.teamhunter.client.guide_sys.impl.AchievementLoader;
import com.rcutanf.teamhunter.client.ui.PhaseCountdownHud;
import com.rcutanf.teamhunter.client.ui.PlayerRadarHud;
import com.rcutanf.teamhunter.client.ui.ShopScreen;
import com.rcutanf.teamhunter.client.ui.TeamScoreHud;
import io.netty.buffer.Unpooled;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientLoginNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.advancement.AdvancementEntry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientAdvancementManager;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;


public class TeamhunterClient implements ClientModInitializer {

    // ========== 常量定义 ==========

    /** 倒计时 HUD 层标识符 */
    private static final Identifier COUNTDOWN_LAYER = Identifier.of(Teamhunter.MOD_ID, "count-down");

    /** 团队分数 HUD 层标识符 */
    private static final Identifier TEAM_SCORE_LAYER = Identifier.of(Teamhunter.MOD_ID, "team-score");

    /** 玩家雷达 HUD 层标识符 */
    private static final Identifier RADAR_LAYER = Identifier.of(Teamhunter.MOD_ID, "player-radar");

    /** 成就指南 HUD 层标识符 */
    public static final Identifier GUIDE_SYS_LAYER = Identifier.of(Teamhunter.MOD_ID, "guide-sys");

    // ========== 游戏状态变量 ==========

    /** 团队优势状态: 0=无优势, 1=猎人优势, 2=逃亡者优势 */
    public static int teamAdvantage = 0;

    /** 游戏当前阶段 */
    public static Phase phase = Phase.WAITING;


    /** 上一次的团队优势状态，用于状态变化检测 */
    private static int lastTeamAdvantage = 0;


    // ========== 玩家位置追踪 ==========

    /** 存储其他玩家位置信息的映射表 */
    private static final Map<UUID, PlayerPositionInfo> playerPositions = new HashMap<>();

    /** 成就指南检查器管理器 */
    private static GuideSysCheckerManager guideCheckerManager;
    private static GuideSysTriggerManager guideSysTriggerManager;
    private static AdvancementEventManager advancementEventManager;

    @Override
    public void onInitializeClient() {
        registerNetworkHandlers();
        registerHudLayers();
        registerTickEvents();
        registerAdvancementGuide();

        // 注册客户端命令
        ClientCommands.register();
        KeyBindings.register();
    }

    /**
     * 注册网络数据包处理器
     */
    private void registerNetworkHandlers() {
        // 注册数据包编解码器
        PayloadTypeRegistry.playC2S().register(NetWorking.TeamScorePacket.ID, NetWorking.TeamScorePacket.CODEC);

        // 注册阶段同步数据包处理
        ClientPlayNetworking.registerGlobalReceiver(Phase.ID, (payload, context) -> {
            PhaseCountdownHud.setPhase(payload);
            // 阶段变化时清空所有buff
            if (payload != Phase.MATCH) {
                TeamScoreHud.clearAllBuffs();
            }
        });

        // 注册倒计时同步数据包处理
        ClientPlayNetworking.registerGlobalReceiver(CounterSyncPacket.ID, (payload, context) -> {
            PhaseCountdownHud.setCountDown(Duration.ofMillis(payload.countDownMilliseconds()));
        });

        // 注册团队分数数据包处理
        ClientPlayNetworking.registerGlobalReceiver(NetWorking.TeamScorePacket.ID, (payload, context) -> {
            TeamScoreHud.updateScores(
                    payload.huntersScore(),
                    payload.runnersScore(),
                    payload.huntersAddedScore(),
                    payload.runnersAddedScore()
            );
        });

        // 注册优势Buff数据包处理
        ClientPlayNetworking.registerGlobalReceiver(NetWorking.AdvantageBuffPacket.ID, (payload, context) -> {
            boolean isHunterTeam = payload.isHunterTeam();
            boolean hasAdvantage = payload.hasAdvantage();

            if (hasAdvantage) {
                TeamScoreHud.addAdvantageBuff(isHunterTeam);
            } else {
                TeamScoreHud.removeAdvantageBuff(isHunterTeam);
            }
        });

        // 注册团队优势信息数据包处理（地狱维度使用）
        ClientPlayNetworking.registerGlobalReceiver(NetWorking.TeamAdvantagePacket.ID, (payload, context) -> {
            teamAdvantage = payload.advantageOrdinal();
            updateNetherDisadvantageBuff();
        });

        // 注册商店物品列表响应处理
        ClientPlayNetworking.registerGlobalReceiver(NetWorking.ShopItemsResponsePacket.ID, (payload, context) -> {
            ShopScreen.getInstance().shopItems = payload.items();
        });

        // 注册玩家位置更新数据包处理
        ClientPlayNetworking.registerGlobalReceiver(NetWorking.PlayerPositionUpdatePacket.ID, (payload, context) -> {
            UUID playerId = payload.playerId();
            String playerName = payload.playerName();
            BlockPos position = payload.position();
            Identifier dimension = payload.dimension(); // 接收维度信息

            if (playerPositions.containsKey(playerId)) {
                PlayerPositionInfo info = playerPositions.get(playerId);
                info.updatePosition(position);
                info.updateDimension(dimension);
            } else {
                playerPositions.put(playerId, new PlayerPositionInfo(playerName, position, dimension));
            }
        });

        // 注册玩家可见性更新数据包处理
        ClientPlayNetworking.registerGlobalReceiver(NetWorking.PlayerVisibilityUpdatePacket.ID, (payload, context) -> {
            UUID playerId = payload.playerId();
            String playerName = payload.playerName();
            boolean isVisible = payload.isVisible();

            if (playerPositions.containsKey(playerId)) {
                PlayerPositionInfo info = playerPositions.get(playerId);
                info.updateVisible(isVisible);
            }
            // TODO:如果玩家尚未在位置映射中，我们将等待位置更新数据包
        });

        // 注册成就数据响应处理
        ClientPlayNetworking.registerGlobalReceiver(NetWorking.AdvancementDataResponsePacket.ID, (payload, context) -> {
            // 直接使用 AdvancementEntry，不需要额外缓存
            AdvancementDataCache cache = AdvancementDataCache.getInstance();

            for (AdvancementEntry entry : payload.advancements()) {
                cache.cacheAdvancement(entry.id(), entry);
            }

            // 完成请求 Future，触发后续初始化
            CompletableFuture<Void> currentRequest = cache.getCurrentRequest();
            if (currentRequest != null) {
                currentRequest.complete(null);
            }

            Teamhunter.LOGGER.info("已接收并缓存 {} 个成就数据", payload.advancements().size());
        });

        // 注册结构数据响应处理
        ClientPlayNetworking.registerGlobalReceiver(NetWorking.StructureDataResponsePacket.ID, (payload, context) -> {
            StructureCache.getInstance().handleServerResponse(payload.chunkPos(), payload.structures());
        });

        // 注册登录阶段网络处理
        ClientLoginNetworking.registerGlobalReceiver(NetWorking.CHECK_CLIENT_MOD,
            (payload, context, buf, consumer) -> CompletableFuture.completedFuture(new PacketByteBuf(Unpooled.buffer())));

        // 注册客户端连接事件
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            playerPositions.clear();
            //PlayerRadarHud.dispose();
            guideCheckerManager.clearAllCheckers();
            StructureCache.getInstance().clearCache();
        });

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            playerPositions.clear();

            // 异步等待成就数据加载完成
            new Thread(() -> {
                try {
                    int attempts = 0;
                    while (attempts < 500) { // 最多等待25秒
                        Thread.sleep(50);

                        // 检查是否是游戏画面（不是菜单、加载界面等）
                        if (client.player != null &&
                                client.world != null &&
                                client.currentScreen == null &&
                                client.getCameraEntity() != null) {

                            client.execute(() -> {
                                guideCheckerManager.initializeAllCheckers();
                            });
                            break;
                        }
                        attempts++;
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }, "AdvancementInit").start();
        });
    }

    /**
     * 注册HUD层
     */
    private void registerHudLayers() {
        // 注册倒计时HUD
        HudRenderCallback.EVENT.register(PhaseCountdownHud::draw);

        // 注册团队分数HUD
        HudRenderCallback.EVENT.register((ctx, tickCounter) -> {
            TeamScoreHud.render(ctx);
        });

        // 注册玩家雷达HUD
        HudRenderCallback.EVENT.register(PlayerRadarHud::render);

        //注册成就指南HUD
        HudRenderCallback.EVENT.register(GuideSysHud::render);
    }

    /**
     * 注册Tick事件
     */
    private void registerTickEvents() {

        // 维度检测和Buff更新
        HudRenderCallback.EVENT.register((drawContext, tickDelta) -> {
            checkDimensionAndUpdateBuffs();
        });
    }

    /**
     * 注册成就guide系统
     */
    private void registerAdvancementGuide() {
        guideSysTriggerManager = GuideSysTriggerManager.getInstance();
        guideCheckerManager = GuideSysCheckerManager.getInstance();
        advancementEventManager = AdvancementEventManager.getInstance();
        AchievementLoader.loadAllAchievements();


    }

    /**
     * 检测玩家维度并更新相应的Buff
     */
    private static void checkDimensionAndUpdateBuffs() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || phase != Phase.MATCH) return;

        boolean isInNether = client.player.getEntityWorld().getRegistryKey().getValue().toString().equals("minecraft:the_nether");

        // 只在地狱内并且有优势状态时显示Buff
        if (isInNether && teamAdvantage != 0) {
            updateNetherDisadvantageBuff();
        } else if (!isInNether) {
            // 不在地狱时，清除所有地狱劣势buff
            TeamScoreHud.removeNetherDebuff(true);
            TeamScoreHud.removeNetherDebuff(false);
        }
    }

    /**
     * 更新地狱维度劣势Buff
     */
    private static void updateNetherDisadvantageBuff() {
        // 优势队伍意味着对方是劣势
        if (teamAdvantage == 1) {
            // 猎人有优势，逃亡者有地狱劣势
            TeamScoreHud.removeNetherDebuff(true);
            TeamScoreHud.addNetherDebuff(false);
        } else if (teamAdvantage == 2) {
            // 逃亡者有优势，猎人有地狱劣势
            TeamScoreHud.removeNetherDebuff(false);
            TeamScoreHud.addNetherDebuff(true);
        } else {
            // 无优势状态，清除所有地狱劣势buff
            TeamScoreHud.removeNetherDebuff(true);
            TeamScoreHud.removeNetherDebuff(false);
        }
    }

    /**
     * 获取所有跟踪的玩家位置信息
     * @return 玩家位置信息映射表
     */
    public static Map<UUID, PlayerPositionInfo> getPlayerPositions() {
        return playerPositions;
    }

}