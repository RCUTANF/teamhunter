package com.rcutanf.teamhunter;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public class NetWorking {
    private static final Identifier Sync = Identifier.of(Teamhunter.MOD_ID, "sync");

    public record CounterSyncPacket(long countDownMilliseconds) implements CustomPayload {

        public static final Id<CounterSyncPacket> ID = new Id<>(Sync);
        public static final PacketCodec<PacketByteBuf, CounterSyncPacket> CODEC = CustomPayload.codecOf(CounterSyncPacket::write, CounterSyncPacket::new);

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }

        public CounterSyncPacket(PacketByteBuf buf) {
            this(buf.readLong());
        }

        public void write(PacketByteBuf buf) {
            buf.writeLong(countDownMilliseconds);
        }
    }

}
