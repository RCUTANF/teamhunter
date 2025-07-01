package com.rcutanf.teamhunter.client;

import com.rcutanf.teamhunter.client.ui.ShopScreen;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.client.MinecraftClient;

public class ClientCommands {
    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(
                ClientCommandManager.literal("shopui")
                    .executes(context -> {
                        // 在主线程中打开商店界面
                        MinecraftClient.getInstance().execute(() -> {
                            ShopScreen.open();
                        });
                        return 1;
                    })
            );
        });
    }
}