package com.rcutanf.teamhunter;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public class NetWorking {

    private static final Identifier SYNC_ID = Identifier.of(Teamhunter.MOD_ID, "sync");
    public static final Identifier CHECK_CLIENT_MOD = Identifier.of(Teamhunter.MOD_ID, "sync");




    public record CounterSyncPacket(long countDownMilliseconds) implements CustomPayload {

        public static final Id<CounterSyncPacket> ID = new Id<>(SYNC_ID);
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

    // 必须确保TeamAdvantagePacket与服务器期望的类型兼容
    public record TeamAdvantagePacket(int advantageOrdinal) implements CustomPayload {
        public static final Identifier TEAM_ADVANTAGE_ID = Identifier.of(Teamhunter.MOD_ID, "team_advantage");
        public static final Id<TeamAdvantagePacket> ID = new Id<>(TEAM_ADVANTAGE_ID);
        public static final PacketCodec<PacketByteBuf, TeamAdvantagePacket> CODEC = CustomPayload.codecOf(TeamAdvantagePacket::write, TeamAdvantagePacket::new);

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }

        public TeamAdvantagePacket(PacketByteBuf buf) {
            this(buf.readInt());
        }

        public void write(PacketByteBuf buf) {
            buf.writeInt(advantageOrdinal);
        }
    }

    public record TeamScorePacket(int huntersScore, int runnersScore, int huntersAddedScore, int runnersAddedScore) implements CustomPayload {
        public static final Identifier TEAM_SCORE_ID = Identifier.of(Teamhunter.MOD_ID, "team_score");
        public static final Id<TeamScorePacket> ID = new Id<>(TEAM_SCORE_ID);
        public static final PacketCodec<PacketByteBuf, TeamScorePacket> CODEC = CustomPayload.codecOf(TeamScorePacket::write, TeamScorePacket::new);

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }

        public TeamScorePacket(PacketByteBuf buf) {
            this(buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt());
        }

        public void write(PacketByteBuf buf) {
            buf.writeInt(huntersScore);
            buf.writeInt(runnersScore);
            buf.writeInt(huntersAddedScore);
            buf.writeInt(runnersAddedScore);
        }
    }

    public record AdvantageBuffPacket(boolean isHunterTeam, boolean hasAdvantage) implements CustomPayload {
        public static final Identifier ADVANTAGE_BUFF_ID = Identifier.of(Teamhunter.MOD_ID, "advantage_buff");
        public static final Id<AdvantageBuffPacket> ID = new Id<>(ADVANTAGE_BUFF_ID);
        public static final PacketCodec<PacketByteBuf, AdvantageBuffPacket> CODEC = CustomPayload.codecOf(AdvantageBuffPacket::write, AdvantageBuffPacket::new);

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }

        public AdvantageBuffPacket(PacketByteBuf buf) {
            this(buf.readBoolean(), buf.readBoolean());
        }

        public void write(PacketByteBuf buf) {
            buf.writeBoolean(isHunterTeam);
            buf.writeBoolean(hasAdvantage);
        }
    }
}