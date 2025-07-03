package com.rcutanf.teamhunter;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public enum Phase implements CustomPayload {
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
        String gamemode = CommandConfig.getCurrentGamemode();
        switch (gamemode) {
            case "as" -> this.showPhaseName = true;
            default -> this.showPhaseName = showCountDown;
        }
        this.showCountDown = showCountDown;
    }

    public final boolean showCountDown;
    public final boolean showPhaseName;

    private static final Identifier PhaseId = Identifier.of(Teamhunter.MOD_ID, "start");
    public static final Id<Phase> ID = new Id<>(PhaseId);
    public static final PacketCodec<PacketByteBuf, Phase> CODEC = CustomPayload.codecOf(Phase::write, p -> p.readEnumConstant(Phase.class));

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    public void write(PacketByteBuf buf) {
        buf.writeEnumConstant(this);
    }
}
