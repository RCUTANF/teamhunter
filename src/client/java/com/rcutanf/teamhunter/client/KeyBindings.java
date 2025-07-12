package com.rcutanf.teamhunter.client;

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

    /** 团队切换状态标记，true 表示下次将选择猎人队伍 */
    private static boolean isHunterCommand = true;

    public static void register() {
        registerConfigKey();
        registerShopKey();
        registerTeamSwitchKey();
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
                String command = isHunterCommand ? "trigger mh.join.hunters" : "trigger mh.join.runners";
                client.player.networkHandler.sendChatCommand(command);
                isHunterCommand = !isHunterCommand;
            }
        });
    }
}