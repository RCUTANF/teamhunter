package com.rcutanf.teamhunter.client.guide_sys.impl.trigger;

import com.rcutanf.teamhunter.client.guide_sys.AbstractGuideSysTrigger;
import com.rcutanf.teamhunter.client.guide_sys.TriggerType;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;

public abstract class ScanCore extends AbstractGuideSysTrigger {
    protected BlockPos lastPlayerPos = BlockPos.ORIGIN;
    protected int scanInterval;
    protected int tickCounter = 0;
    protected boolean isEnabled = true;

    public ScanCore(TriggerType type, int scanInterval) {
        super(type);
        this.scanInterval = scanInterval;
        registerEvents();
    }

    private void registerEvents() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!isEnabled || !shouldScan()) {
                return;
            }

            tickCounter++;
            if (tickCounter >= scanInterval) {
                tickCounter = 0;
                performScan();
            }
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> onDisconnect());
    }

    protected boolean shouldScan() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) {
            return false;
        }

        BlockPos currentPos = client.player.getBlockPos();
        double distanceSq = lastPlayerPos.getSquaredDistance(currentPos);

        if (distanceSq > getMovementThreshold()) {
            lastPlayerPos = currentPos;
            tickCounter = 0;
            return true;
        }

        return true;
    }

    public void enable() {
        this.isEnabled = true;
        this.tickCounter = 0;
    }

    public void disable() {
        this.isEnabled = false;
        onDisable();
    }

    public void setScanInterval(int interval) {
        this.scanInterval = Math.max(5, interval);
    }

    public void forceScan() {
        if (isEnabled) {
            tickCounter = scanInterval;
        }
    }

    protected abstract void performScan();

    protected abstract void onDisconnect();

    protected abstract void onDisable();

    protected abstract double getMovementThreshold();
}