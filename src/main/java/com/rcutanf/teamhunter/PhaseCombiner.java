package com.rcutanf.teamhunter;

import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

public final class PhaseCombiner implements AutoCloseable {
    private final MinecraftServer server;
    private final PhaseHandler phaseHandler;
    private @Nullable CompletableFuture<Void> future;
    private Phase _phase = Phase.NONE;

    public PhaseCombiner(MinecraftServer server, PhaseHandler phaseHandler) {
        this.server = server;
        this.phaseHandler = phaseHandler;
    }

    /**
     * 链式调用以设置下一个阶段，并指定倒计时时长。
     *
     * @param phase     下一个阶段
     * @param countDown 倒计时时长
     * @return 当前 PhaseCombiner 实例
     */
    public PhaseCombiner then(Phase phase, Duration countDown) {
        if (future == null) {
            future = setPhase(phase, countDown);
        } else
            future = future.thenCompose(v -> setPhase(phase, countDown));
        return this;
    }

    /**
     * @return 当前阶段
     */
    public Phase Phase() {
        return _phase;
    }

    /**
     * 链式调用的结束，设置最终阶段，持续时间无限。
     *
     * @param phase 最终阶段
     */
    public void then(Phase phase) {
        if (future != null)
            future = future.thenAccept(v -> setPhase(phase));
        else
            setPhase(phase);
    }

    private void setPhase(Phase phase) {
        _phase = phase;
        Teamhunter.broadcastPacket(server, phase);
        server.execute(() -> {
            switch (phase) {
                case WARMUP -> phaseHandler.onWarmupStart();
                case PREPARE -> phaseHandler.onPrepareStart();
                case MATCH -> phaseHandler.onMatchStart();
                case END ->  phaseHandler.onMatchEnd();
            }
        });
    }

    private CompletableFuture<Void> setPhase(Phase phase, Duration countDown) {
        setPhase(phase);
        return ticker.start(server, countDown, Duration.ofMillis(500));
    }

    /**
     * 停止正在运行的倒计时。
     *
     * @return 当前 PhaseCombiner 实例
     */
    public PhaseCombiner clear() {
        ticker.clear();
        future = null;
        return this;
    }

    public void pause() {
        ticker.pause();
    }

    public void resume() {
        ticker.resume();
    }

    public boolean isPaused() {
        return ticker.isPaused();
    }

    private final Ticker<MinecraftServer> ticker = new Ticker<>((c, d) -> {
        Teamhunter.broadcastPacket(c, new NetWorking.CounterSyncPacket(d.toMillis()));
    });

    @Override
    public void close() throws Exception {
        ticker.close();
    }
}
