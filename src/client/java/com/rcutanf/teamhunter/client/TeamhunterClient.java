package com.rcutanf.teamhunter.client;

import com.rcutanf.teamhunter.NetWorking;
import com.rcutanf.teamhunter.NetWorking.CounterSyncPacket;
import com.rcutanf.teamhunter.Phase;
import com.rcutanf.teamhunter.Teamhunter;
import com.rcutanf.teamhunter.client.ui.PhaseCountdownHud;
import com.rcutanf.teamhunter.client.ui.PlayerRadarHud;
import com.rcutanf.teamhunter.client.ui.ShopScreen;
import com.rcutanf.teamhunter.client.ui.TeamScoreHud;
import io.netty.buffer.Unpooled;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientLoginNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudLayerRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.IdentifiedLayer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.InputUtil;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import org.lwjgl.glfw.GLFW;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;


public class TeamhunterClient implements ClientModInitializer {

    private static final Identifier countDownLayer = Identifier.of(Teamhunter.MOD_ID, "count-down");
    private static final Identifier teamScoreLayer = Identifier.of(Teamhunter.MOD_ID, "team-score");
    private static final Identifier radarLayer = Identifier.of(Teamhunter.MOD_ID, "player-radar");

    public static int teamAdvantage = 0; // 0=无优势, 1=猎人, 2=逃亡者
    public static Phase phase = Phase.WAITING;
    public static Duration countDown = Duration.ZERO;

    // 上一次的队伍劣势状态
    private static int lastTeamAdvantage = 0;//用来记忆上次切换执行的命令
    private static KeyBinding shopKeyBinding;

    // 添加类变量，用于记录当前队伍选择状态
    private static boolean isHunterCommand = true;
    private static KeyBinding teamSwitchKeyBinding;

    // 添加一个Map存储其他玩家的位置信息
    private static final Map<UUID, PlayerPositionInfo> playerPositions = new HashMap<>();

    // 添加一个记录玩家位置的类
    public static class PlayerPositionInfo {
        private final String playerName;
        private BlockPos position;
        private final String teamName; // 新增队伍名称字段

        public PlayerPositionInfo(String playerName, BlockPos position, String teamName) {
            this.playerName = playerName;
            this.position = position;
            this.teamName = teamName;
        }

        public PlayerPositionInfo(String playerName, BlockPos position) {
            this(playerName, position, getTeamNameForPlayer(playerName));
        }

        public void updatePosition(BlockPos newPosition) {
            this.position = newPosition;
        }

        public String getPlayerName() { return playerName; }
        public BlockPos getPosition() { return position; }
        public String getTeamName() { return teamName; }

        private static String getTeamNameForPlayer(String playerName) {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.world != null) {
                Team playerTeam = client.world.getScoreboard().getTeam(playerName);
                return playerTeam != null ? playerTeam.getName() : null;
            }
            return null;
        }
    }


    // 检测玩家维度并更新Buff
    private static void checkDimensionAndUpdateBuffs() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || phase != Phase.MATCH) return;

        boolean isInNether = client.player.getWorld().getRegistryKey().getValue().toString().equals("minecraft:the_nether");

        // 只在地狱内并且有优势状态时显示Buff
        if (isInNether && teamAdvantage != 0) {
            updateNetherDisadvantageBuff();
        } else if (!isInNether) {
            // 不在地狱时，清除所有地狱劣势buff
            TeamScoreHud.removeNetherDebuff(true);
            TeamScoreHud.removeNetherDebuff(false);
        }
    }

    // 更新地狱劣势Buff
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

    @Override
    public void onInitializeClient() {

        PayloadTypeRegistry.playC2S().register(NetWorking.TeamScorePacket.ID, NetWorking.TeamScorePacket.CODEC);
        ClientPlayNetworking.registerGlobalReceiver(Phase.ID, (payload, context) -> {
            PhaseCountdownHud.setPhase(payload); // 更新到新类
            // 阶段变化时清空所有buff
            if (payload != Phase.MATCH) {
                TeamScoreHud.clearAllBuffs();
            }
        });

        ClientPlayNetworking.registerGlobalReceiver(CounterSyncPacket.ID, (payload, context) -> {
            PhaseCountdownHud.setCountDown(Duration.ofMillis(payload.countDownMilliseconds())); // 更新到新类
        });


        // 注册网络监听器
        ClientPlayNetworking.registerGlobalReceiver(NetWorking.TeamScorePacket.ID, (payload, context) -> {
            // 获取分数数据
            int huntersScore = payload.huntersScore();
            int runnersScore = payload.runnersScore();
            int huntersAddedScore = payload.huntersAddedScore();
            int runnersAddedScore = payload.runnersAddedScore();

            // 在游戏主线程中更新UI数据
            MinecraftClient.getInstance().execute(() -> {
                TeamScoreHud.updateScores(
                        huntersScore,
                        runnersScore,
                        huntersAddedScore,
                        runnersAddedScore
                );
            });
        });

        // 注册优势Buff数据包接收器
        ClientPlayNetworking.registerGlobalReceiver(NetWorking.AdvantageBuffPacket.ID, (payload, context) -> {
            boolean isHunterTeam = payload.isHunterTeam();
            boolean hasAdvantage = payload.hasAdvantage();

            // 在游戏主线程中执行UI更新
            MinecraftClient.getInstance().execute(() -> {
                if (hasAdvantage) {
                    TeamScoreHud.addAdvantageBuff(isHunterTeam);
                } else {
                    TeamScoreHud.removeAdvantageBuff(isHunterTeam);
                }
            });
        });

        HudLayerRegistrationCallback.EVENT.register(r ->
                r.attachLayerBefore(IdentifiedLayer.MISC_OVERLAYS, countDownLayer, PhaseCountdownHud::draw) // 使用新类的方法
        );
        ClientLoginNetworking.registerGlobalReceiver(NetWorking.CHECK_CLIENT_MOD, (payload, context, buf, consumer) -> CompletableFuture.completedFuture(new PacketByteBuf(Unpooled.buffer())));

        // 接收团队优势信息(这个是地狱的包用的)
        ClientPlayNetworking.registerGlobalReceiver(NetWorking.TeamAdvantagePacket.ID, (payload, context) -> {
            teamAdvantage = payload.advantageOrdinal();

            // 在游戏主线程中更新Buff状态
            MinecraftClient.getInstance().execute(TeamhunterClient::updateNetherDisadvantageBuff);
        });

        // 添加：注册团队分数 HUD 渲染
        HudLayerRegistrationCallback.EVENT.register(r ->
                r.attachLayerBefore(IdentifiedLayer.MISC_OVERLAYS, teamScoreLayer, (ctx, tickCounter) -> {
                    TeamScoreHud.render(ctx);
                })
        );

        // 添加tick事件监听器，用于检测维度变化
        HudRenderCallback.EVENT.register((drawContext, tickDelta) -> {
            checkDimensionAndUpdateBuffs();
        });

        ClientCommands.register();


        // 注册商店物品列表响应接收器
        ClientPlayNetworking.registerGlobalReceiver(NetWorking.ShopItemsResponsePacket.ID, (payload, context) -> {
            var itemDataList = payload.items();

            // 在游戏主线程中处理UI更新
            MinecraftClient.getInstance().execute(() -> {

                ShopScreen.getInstance().shopItems = itemDataList;

            });
        });

        // 注册商店键绑定 - 使用O键
        shopKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.teamhunter.shop", // 翻译键
                InputUtil.Type.KEYSYM,  // 键盘输入类型
                GLFW.GLFW_KEY_O,        // O键的GLFW键值
                "category.teamhunter.keys" // 分类
        ));

        // 注册按键处理事件
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // 检查商店键是否被按下
            //TODO:不知道怎么去通过打开的按键关闭商店按钮，可能是screen本身机制的拦截？
            if (shopKeyBinding.wasPressed() && client.player != null) {
                if (client.currentScreen instanceof ShopScreen) {
                    // 如果当前已经打开商店界面，则关闭它
                    client.setScreen(null);
                } else {
                    // 如果当前没有打开商店界面，则打开它
                    ShopScreen.open();
                }
            }
        });

        // 注册队伍切换键绑定 - 使用F8键
        teamSwitchKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.teamhunter.teamswitch", // 翻译键
                InputUtil.Type.KEYSYM,       // 键盘输入类型
                GLFW.GLFW_KEY_F8,            // F8键的GLFW键值
                "category.teamhunter.keys"   // 分类
        ));

        // 注册按键处理事件
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // 检查队伍切换键是否被按下
            if (teamSwitchKeyBinding.wasPressed() && client.player != null) {
                // 确定要执行的命令
                String command = isHunterCommand ? "trigger mh.join.hunters" : "trigger mh.join.runners";
                // 发送命令
                client.player.networkHandler.sendChatCommand(command);
                // 切换状态，下次按键时执行另一个命令
                isHunterCommand = !isHunterCommand;
            }
        });

        // 注册位置更新数据包接收器
        ClientPlayNetworking.registerGlobalReceiver(NetWorking.PlayerPositionUpdatePacket.ID, (payload, context) -> {
            UUID playerId = payload.playerId();
            String playerName = payload.playerName();
            BlockPos position = payload.position();

            // 在游戏主线程中更新位置信息
            MinecraftClient.getInstance().execute(() -> {
                if (playerPositions.containsKey(playerId)) {
                    playerPositions.get(playerId).updatePosition(position);
                } else {
                    playerPositions.put(playerId, new PlayerPositionInfo(playerName, position));
                }

                // 可以在这里添加额外逻辑，如显示在HUD或小地图上
            });
        });


        HudLayerRegistrationCallback.EVENT.register(r ->
                r.attachLayerBefore(IdentifiedLayer.MISC_OVERLAYS, radarLayer, PlayerRadarHud::render)
        );


        // 监听客户端断开连接事件
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            // 清空玩家位置缓存
            playerPositions.clear();
        });

        // 监听客户端连接到服务器事件
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            // 在新连接建立时也清空玩家位置缓存，确保不会有旧数据
            playerPositions.clear();
        });


    }

    public static Map<UUID, PlayerPositionInfo> getPlayerPositions() {
        return playerPositions;
    }



}