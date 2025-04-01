package com.rcutanf.teamhunter;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

public final class Ticker<C> {
    private final BiConsumer<C, Duration> onTick;
    private Duration remaining;
    boolean paused = false;

    public Ticker(BiConsumer<C, Duration> onTick) {
        this.onTick = onTick;
    }

    public CompletableFuture<Void> start(C context, Duration duration, Duration interval) {
        if (counterTask != null) counterTask.cancel(false);
        remaining = duration;

        CompletableFuture<Void> future = new CompletableFuture<>();


        counterTask = executor.scheduleAtFixedRate(() -> {
            if (paused) return;
            onTick.accept(context, remaining);
            remaining = remaining.minus(interval);
            if (remaining.isNegative()) {
                remaining = Duration.ZERO;
                if (counterTask != null) {
                    counterTask.cancel(false);
                }
                future.complete(null);
            }
        }, 0, interval.toMillis(), TimeUnit.MILLISECONDS);

        return future;
    }

    public void pause() {
        paused = true;
    }

    public void resume() {
        paused = false;
    }

    public boolean isPaused() {
        return paused;
    }

    public void clear() {
        if (counterTask != null)
            counterTask.cancel(false);
    }

    private ScheduledFuture<?> counterTask;

    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
}
