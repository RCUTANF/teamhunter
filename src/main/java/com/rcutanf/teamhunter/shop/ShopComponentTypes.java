package com.rcutanf.teamhunter.shop;

import com.mojang.serialization.Codec;
import com.rcutanf.teamhunter.Teamhunter;
import net.minecraft.component.ComponentType;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.dynamic.Codecs;

import java.util.function.UnaryOperator;

public class ShopComponentTypes {
    //    public static final ComponentType<Unit> FINITE = register("finite", b -> b.codec(Unit.CODEC).packetCodec(Unit.PACKET_CODEC));
    public static final ComponentType<Integer> PRICE = register("price", builder -> builder.codec(Codecs.POSITIVE_INT).packetCodec(PacketCodecs.VAR_INT));
    public static final ComponentType<String> ID = register("id", builder -> builder.codec(Codec.STRING).packetCodec(PacketCodecs.STRING));

    private static <T> ComponentType<T> register(String id, UnaryOperator<ComponentType.Builder<T>> builderOperator) {
        return Registry.register(Registries.DATA_COMPONENT_TYPE, Teamhunter.MOD_ID + ":" + id, (builderOperator.apply(ComponentType.builder())).build());
    }
}
