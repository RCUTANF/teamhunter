package com.rcutanf.teamhunter;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public enum Phase implements CustomPayload {
    WAITING(true),
    WARMUP(true),
    PREPARE(true),
    MATCH(false),
    PAUSED(false),
    END(false);

    Phase(boolean countDown) {
        this.countDown = countDown;
    }

    public final boolean countDown;

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
