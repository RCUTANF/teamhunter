package com.rcutanf.teamhunter;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

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

    public record ShopItemsRequestPacket() implements CustomPayload {
        public static final Identifier SHOP_ITEMS_REQUEST_ID = Identifier.of(Teamhunter.MOD_ID, "shop_items_request");
        public static final Id<ShopItemsRequestPacket> ID = new Id<>(SHOP_ITEMS_REQUEST_ID);
        public static final PacketCodec<PacketByteBuf, ShopItemsRequestPacket> CODEC = CustomPayload.codecOf(
                (packet, buf) -> {}, // 无需写入数据，但需要两个参数
                buf -> new ShopItemsRequestPacket()
        );

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record ShopItemData(String id, int price) {}

    public record ShopItemsResponsePacket(List<ShopItemData> items) implements CustomPayload {
        public static final Identifier SHOP_ITEMS_RESPONSE_ID = Identifier.of(Teamhunter.MOD_ID, "shop_items_response");
        public static final Id<ShopItemsResponsePacket> ID = new Id<>(SHOP_ITEMS_RESPONSE_ID);
        public static final PacketCodec<PacketByteBuf, ShopItemsResponsePacket> CODEC = CustomPayload.codecOf(
                (packet, buf) -> {
                    buf.writeInt(packet.items.size());
                    for (ShopItemData item : packet.items) {
                        buf.writeString(item.id());
                        buf.writeInt(item.price());
                    }
                },
                buf -> {
                    int size = buf.readInt();
                    List<ShopItemData> items = new ArrayList<>(size);
                    for (int i = 0; i < size; i++) {
                        String id = buf.readString();
                        int price = buf.readInt();
                        items.add(new ShopItemData(id, price));
                    }
                    return new ShopItemsResponsePacket(items);
                }
        );

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }
}