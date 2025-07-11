package com.rcutanf.teamhunter.client;

import com.rcutanf.teamhunter.client.ui.RadarConfigScreen;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class KeyBindings {
    private static KeyBinding configKey;

    public static void register() {
        // 注册打开配置界面的键绑定
        configKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.teamhunter.config",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_F7, // 默认为F7键
                "category.teamhunter.keys"
        ));

        // 监听按键事件
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (configKey.wasPressed() && client.player != null) {
                client.setScreen(new RadarConfigScreen(client.currentScreen));
            }
        });
    }
}