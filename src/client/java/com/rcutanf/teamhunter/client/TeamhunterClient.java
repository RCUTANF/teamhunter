package com.rcutanf.teamhunter.client;

import com.rcutanf.teamhunter.NetWorking;
import com.rcutanf.teamhunter.NetWorking.CounterSyncPacket;
import com.rcutanf.teamhunter.Phase;
import com.rcutanf.teamhunter.Teamhunter;
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
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.InputUtil;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class TeamhunterClient implements ClientModInitializer {

    private static final Identifier countDownLayer = Identifier.of(Teamhunter.MOD_ID, "count-down");
    private static final Identifier teamScoreLayer = Identifier.of(Teamhunter.MOD_ID, "team-score");

    // 移除优势层标识，因为现在使用buff系统
    // private static final Identifier advantageLayer = Identifier.of(Teamhunter.MOD_ID, "team-advantage");

    // 上一次的队伍劣势状态
    private static int lastTeamAdvantage = 0;
    public static int teamAdvantage = 0; // 0=无优势, 1=猎人, 2=逃亡者

    private static KeyBinding shopKeyBinding;

    @Override
    public void onInitializeClient() {

        PayloadTypeRegistry.playC2S().register(NetWorking.TeamScorePacket.ID, NetWorking.TeamScorePacket.CODEC);
        ClientPlayNetworking.registerGlobalReceiver(Phase.ID, (payload, context) -> {
            phase = payload;
            // 阶段变化时清空所有buff
            if (payload != Phase.MATCH) {
                TeamScoreHud.clearAllBuffs();
            }
        });

        ClientPlayNetworking.registerGlobalReceiver(CounterSyncPacket.ID, (payload, context) -> {
            countDown = Duration.ofMillis(payload.countDownMilliseconds());
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
                r.attachLayerBefore(IdentifiedLayer.MISC_OVERLAYS, countDownLayer, TeamhunterClient::draw)
        );
        ClientLoginNetworking.registerGlobalReceiver(NetWorking.CHECK_CLIENT_MOD, (payload, context, buf, consumer) -> CompletableFuture.completedFuture(new PacketByteBuf(Unpooled.buffer())));

        // 接收团队优势信息(这个是地狱的包用的)
        ClientPlayNetworking.registerGlobalReceiver(NetWorking.TeamAdvantagePacket.ID, (payload, context) -> {
            teamAdvantage = payload.advantageOrdinal();

            // 在游戏主线程中更新Buff状态
            MinecraftClient.getInstance().execute(() -> {
                updateNetherDisadvantageBuff();
            });
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
            List<NetWorking.ShopItemData> itemDataList = payload.items();

            // 在游戏主线程中处理UI更新
            MinecraftClient.getInstance().execute(() -> {

                ShopScreen.getInstance().updateShopItemsFromData(itemDataList);

            });
        });

        // 注册商店键绑定 - 使用T键
        shopKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.teamhunter.shop", // 翻译键
                InputUtil.Type.KEYSYM,  // 键盘输入类型
                GLFW.GLFW_KEY_O,        // T键的GLFW键值
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


    }

    public static Phase phase = Phase.WAITING;
    public static Duration countDown = Duration.ZERO;

    public static boolean shouldShowCountDown() {
        return phase.showCountDown;
    }

    private static void draw(DrawContext ctx, RenderTickCounter counter) {
        if (!shouldShowCountDown()) return;
        var textRenderer = MinecraftClient.getInstance().textRenderer;
        var windowWidth = ctx.getScaledWindowWidth();
        var textHeight = textRenderer.fontHeight;

        var text = String.valueOf(countDown.toSeconds());

        ctx.drawCenteredTextWithShadow(textRenderer, phase.name(), windowWidth / 2, 10, 0xFFFFFFFF);
        ctx.drawCenteredTextWithShadow(textRenderer, text, windowWidth / 2, 10 + textHeight + 4, 0xFFFFFFFF);
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
}