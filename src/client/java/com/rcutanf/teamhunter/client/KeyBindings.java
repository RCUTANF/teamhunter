package com.rcutanf.teamhunter.client;

import com.rcutanf.teamhunter.client.guide_sys.gui.GuideSysHud;
import com.rcutanf.teamhunter.client.guide_sys.gui.GuideSysScreen;
import com.rcutanf.teamhunter.client.ui.RadarConfigScreen;
import com.rcutanf.teamhunter.client.ui.ShopScreen;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

/**
 * 管理模组所有键位绑定
 */
public class KeyBindings {
    private static KeyBinding configKey;
    private static KeyBinding shopKeyBinding;
    private static KeyBinding teamSwitchKeyBinding;
    private static KeyBinding adGuideDescriptionKey;

    /** 团队切换状态标记，true 表示下次将选择猎人队伍 */
    private static boolean isHunterCommand = true;

    public static void register() {
        registerConfigKey();
        registerShopKey();
        registerTeamSwitchKey();
        registerADguideDescriptionKey();
        registerTickEvents();
    }

    private static void registerConfigKey() {
        // 注册打开配置界面的键绑定
        configKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.teamhunter.config",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_F7, // 默认为F7键
                "category.teamhunter.keys"
        ));
    }

    private static void registerShopKey() {
        // 注册商店键绑定
        shopKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.teamhunter.shop",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_O,
            "category.teamhunter.keys"
        ));
    }

    private static void registerTeamSwitchKey() {
        // 注册队伍切换键绑定
        teamSwitchKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.teamhunter.teamswitch",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_F8,
            "category.teamhunter.keys"
        ));
    }

    private static void registerADguideDescriptionKey(){
        adGuideDescriptionKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.teamhunter.adguide.description",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_TAB,
            "category.teamhunter.keys"
        ));
    }

    private static void registerTickEvents() {
        // 配置键处理
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (configKey.wasPressed() && client.player != null) {
                client.setScreen(new RadarConfigScreen(client.currentScreen));
            }
        });

        // 商店键处理
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (shopKeyBinding.wasPressed() && client.player != null) {
                if (client.currentScreen instanceof ShopScreen) {
                    client.setScreen(null);
                } else {
                    ShopScreen.open();
                }
            }
        });

        // 队伍切换键处理
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (teamSwitchKeyBinding.wasPressed() && client.player != null) {
                String command = isHunterCommand ? "team join hunters" : "team join runners";
                client.player.networkHandler.sendChatCommand(command);
                isHunterCommand = !isHunterCommand;
            }
        });

        // 成就指南成就描述展开键处理
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (adGuideDescriptionKey.isPressed()) {
                GuideSysHud.setShowAllDescriptions(true);
                // 检查是否同时按下了Ctrl键
                long windowHandle = client.getWindow().getHandle();
                boolean isCtrlPressed = InputUtil.isKeyPressed(windowHandle, GLFW.GLFW_KEY_LEFT_CONTROL) ||
                        InputUtil.isKeyPressed(windowHandle, GLFW.GLFW_KEY_RIGHT_CONTROL);

                if (isCtrlPressed) {
                    // 打开指南屏幕
                    if (client.player != null && client.currentScreen == null) {
                        client.setScreen(new GuideSysScreen(client.currentScreen));
                    }
                }
            } else {
                GuideSysHud.setShowAllDescriptions(false);
            }
        });
    }
}