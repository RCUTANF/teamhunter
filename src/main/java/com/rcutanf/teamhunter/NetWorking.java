package com.rcutanf.teamhunter;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public class NetWorking {
    private static final Identifier Toggle = Identifier.of(Teamhunter.MOD_ID, "start");
    private static final Identifier Sync = Identifier.of(Teamhunter.MOD_ID, "sync");

    public record CounterTogglePacket(int countDownTicks) implements CustomPayload {

        public static final Id<CounterTogglePacket> ID = new Id<>(Toggle);
        public static final PacketCodec<PacketByteBuf, CounterTogglePacket> CODEC = CustomPayload.codecOf(CounterTogglePacket::write, CounterTogglePacket::new);

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }

        public CounterTogglePacket(PacketByteBuf buf) {
            this(buf.readVarInt());
        }

        public void write(PacketByteBuf buf) {
            buf.writeVarInt(countDownTicks);
        }
    }

    public record CounterSyncPacket(int countDownTicks) implements CustomPayload {

        public static final Id<CounterSyncPacket> ID = new Id<>(Sync);
        public static final PacketCodec<PacketByteBuf, CounterSyncPacket> CODEC = CustomPayload.codecOf(CounterSyncPacket::write, CounterSyncPacket::new);

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }

        public CounterSyncPacket(PacketByteBuf buf) {
            this(buf.readVarInt());
        }

        public void write(PacketByteBuf buf) {
            buf.writeVarInt(countDownTicks);
        }
    }

}
