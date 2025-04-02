package com.rcutanf.teamhunter;

public enum Phase {
    WAITING(true, false),
    WARMUP(true),
    PREPARE(true),
    MATCH(false),
    PAUSED(false),
    END(false);

    Phase(boolean showCountDown, boolean showPhaseName) {
        this.showCountDown = showCountDown;
        this.showPhaseName = showPhaseName;
    }

    Phase(boolean showCountDown) {
        this(showCountDown, true);
    }

    public final boolean showCountDown;
    public final boolean showPhaseName;

}
