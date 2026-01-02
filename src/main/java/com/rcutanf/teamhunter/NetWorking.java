package com.rcutanf.teamhunter;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.advancement.AdvancementEntry;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.text.Text;
import net.minecraft.util.math.ChunkPos;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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

    public record TeamScorePacket(int huntersScore, int runnersScore, int huntersAddedScore,
                                  int runnersAddedScore) implements CustomPayload {
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

    public static class ShopItemsRequestPacket implements CustomPayload {
        private ShopItemsRequestPacket() {
        }

        public static final ShopItemsRequestPacket INSTANCE = new ShopItemsRequestPacket();
        public static final Identifier SHOP_ITEMS_REQUEST_ID = Identifier.of(Teamhunter.MOD_ID, "shop_items_request");
        public static final Id<ShopItemsRequestPacket> ID = new Id<>(SHOP_ITEMS_REQUEST_ID);
        public static final PacketCodec<ByteBuf, ShopItemsRequestPacket> CODEC =
                PacketCodec.unit(ShopItemsRequestPacket.INSTANCE);

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record ShopPurchasePacket(int id) implements CustomPayload {
        public static final Identifier SHOP_PURCHASE_ID = Identifier.of(Teamhunter.MOD_ID, "shop_purchase");
        public static final Id<ShopPurchasePacket> ID = new Id<>(SHOP_PURCHASE_ID);
        public static final PacketCodec<RegistryByteBuf, ShopPurchasePacket> CODEC = PacketCodec.tuple(
                PacketCodecs.VAR_INT, ShopPurchasePacket::id, ShopPurchasePacket::new
        );

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }


    public record ShopItemsResponsePacket(List<ItemStack> items) implements CustomPayload {
        public static final Identifier SHOP_ITEMS_RESPONSE_ID = Identifier.of(Teamhunter.MOD_ID, "shop_items_response");
        public static final Id<ShopItemsResponsePacket> ID = new Id<>(SHOP_ITEMS_RESPONSE_ID);
        public static final PacketCodec<RegistryByteBuf, ShopItemsResponsePacket> CODEC = PacketCodec.tuple(
                ItemStack.OPTIONAL_LIST_PACKET_CODEC, ShopItemsResponsePacket::items, ShopItemsResponsePacket::new
        );

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record PlayerPositionUpdatePacket(UUID playerId, String playerName, BlockPos position,
                                             Identifier dimension) implements CustomPayload {
        public static final Identifier PLAYER_POSITION_ID = Identifier.of(Teamhunter.MOD_ID, "player_position");
        public static final Id<PlayerPositionUpdatePacket> ID = new Id<>(PLAYER_POSITION_ID);
        public static final PacketCodec<PacketByteBuf, PlayerPositionUpdatePacket> CODEC =
            CustomPayload.codecOf(PlayerPositionUpdatePacket::write, PlayerPositionUpdatePacket::new);

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }

        public PlayerPositionUpdatePacket(PacketByteBuf buf) {
            this(buf.readUuid(), buf.readString(), new BlockPos(buf.readInt(), buf.readInt(), buf.readInt()),
                 buf.readIdentifier());
        }

        public void write(PacketByteBuf buf) {
            buf.writeUuid(playerId);
            buf.writeString(playerName);
            buf.writeInt(position.getX());
            buf.writeInt(position.getY());
            buf.writeInt(position.getZ());
            buf.writeIdentifier(dimension);
        }
    }

    public record PlayerVisibilityUpdatePacket(UUID playerId, String playerName, boolean isVisible) implements CustomPayload {
        public static final Identifier PLAYER_VISIBILITY_ID = Identifier.of(Teamhunter.MOD_ID, "player_visibility");
        public static final Id<PlayerVisibilityUpdatePacket> ID = new Id<>(PLAYER_VISIBILITY_ID);
        public static final PacketCodec<PacketByteBuf, PlayerVisibilityUpdatePacket> CODEC =
            CustomPayload.codecOf(PlayerVisibilityUpdatePacket::write, PlayerVisibilityUpdatePacket::new);

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }

        public PlayerVisibilityUpdatePacket(PacketByteBuf buf) {
            this(buf.readUuid(), buf.readString(), buf.readBoolean());
        }

        public void write(PacketByteBuf buf) {
            buf.writeUuid(playerId);
            buf.writeString(playerName);
            buf.writeBoolean(isVisible);
        }
    }

    // 请求成就数据的数据包
    public record AdvancementDataRequestPacket(List<Identifier> advancementIds) implements CustomPayload {
        public static final Identifier ADVANCEMENT_DATA_REQUEST_ID = Identifier.of(Teamhunter.MOD_ID, "advancement_data_request");
        public static final Id<AdvancementDataRequestPacket> ID = new Id<>(ADVANCEMENT_DATA_REQUEST_ID);
        public static final PacketCodec<PacketByteBuf, AdvancementDataRequestPacket> CODEC =
            PacketCodec.tuple(
                Identifier.PACKET_CODEC.collect(PacketCodecs.toList()),
                AdvancementDataRequestPacket::advancementIds,
                AdvancementDataRequestPacket::new
            );

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    // 成就数据响应数据包 - 直接使用 AdvancementEntry
    public record AdvancementDataResponsePacket(List<AdvancementEntry> advancements) implements CustomPayload {
        public static final Identifier ADVANCEMENT_DATA_RESPONSE_ID = Identifier.of(Teamhunter.MOD_ID, "advancement_data_response");
        public static final Id<AdvancementDataResponsePacket> ID = new Id<>(ADVANCEMENT_DATA_RESPONSE_ID);
        public static final PacketCodec<RegistryByteBuf, AdvancementDataResponsePacket> CODEC =
            PacketCodec.tuple(
                AdvancementEntry.LIST_PACKET_CODEC,
                AdvancementDataResponsePacket::advancements,
                AdvancementDataResponsePacket::new
            );

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    // 结构数据请求数据包
    public record StructureDataRequestPacket(ChunkPos chunkPos, Identifier dimension) implements CustomPayload {
        public static final Identifier STRUCTURE_DATA_REQUEST_ID = Identifier.of(Teamhunter.MOD_ID, "structure_data_request");
        public static final Id<StructureDataRequestPacket> ID = new Id<>(STRUCTURE_DATA_REQUEST_ID);

        // 为ChunkPos创建PacketCodec
        private static final PacketCodec<PacketByteBuf, ChunkPos> CHUNK_POS_CODEC = PacketCodec.tuple(
                PacketCodecs.VAR_INT, chunkPos -> chunkPos.x,
                PacketCodecs.VAR_INT, chunkPos -> chunkPos.z,
                ChunkPos::new
        );

        public static final PacketCodec<PacketByteBuf, StructureDataRequestPacket> CODEC = PacketCodec.tuple(
                CHUNK_POS_CODEC, StructureDataRequestPacket::chunkPos,
                Identifier.PACKET_CODEC, StructureDataRequestPacket::dimension,
                StructureDataRequestPacket::new
        );

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    // 结构响应数据包
    public record StructureDataResponsePacket(ChunkPos chunkPos, List<StructureInfo> structures) implements CustomPayload {
        public static final Identifier STRUCTURE_DATA_RESPONSE_ID = Identifier.of(Teamhunter.MOD_ID, "structure_data_response");
        public static final Id<StructureDataResponsePacket> ID = new Id<>(STRUCTURE_DATA_RESPONSE_ID);

        public static final PacketCodec<PacketByteBuf, StructureDataResponsePacket> CODEC = PacketCodec.tuple(
                StructureDataRequestPacket.CHUNK_POS_CODEC, StructureDataResponsePacket::chunkPos,
                StructureInfo.PACKET_CODEC.collect(PacketCodecs.toList()), StructureDataResponsePacket::structures,
                StructureDataResponsePacket::new
        );

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    public record StructureInfo(String name, BlockBox boundingBox) {
        // 为BlockBox创建专用的PacketCodec
        public static final PacketCodec<PacketByteBuf, BlockBox> BLOCK_BOX_CODEC = PacketCodec.tuple(
                PacketCodecs.VAR_INT, BlockBox::getMinX,
                PacketCodecs.VAR_INT, BlockBox::getMinY,
                PacketCodecs.VAR_INT, BlockBox::getMinZ,
                PacketCodecs.VAR_INT, BlockBox::getMaxX,
                PacketCodecs.VAR_INT, BlockBox::getMaxY,
                PacketCodecs.VAR_INT, BlockBox::getMaxZ,
                BlockBox::new
        );

        // StructureInfo的PacketCodec
        public static final PacketCodec<PacketByteBuf, StructureInfo> PACKET_CODEC = PacketCodec.tuple(
                PacketCodecs.STRING, StructureInfo::name,
                BLOCK_BOX_CODEC, StructureInfo::boundingBox,
                StructureInfo::new
        );
    }
}