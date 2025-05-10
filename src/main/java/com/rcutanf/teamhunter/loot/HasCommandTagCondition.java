package com.rcutanf.teamhunter.loot;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.mojang.serialization.*;
import com.rcutanf.teamhunter.Teamhunter;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.loot.condition.LootCondition;
import net.minecraft.loot.condition.LootConditionType;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.util.Identifier;


/**
 * 自定义战利品条件：检查击杀者是否有指定命令标签
 * 用于决定烈焰人是否掉落烈焰棒
 */
public class HasCommandTagCondition implements LootCondition {
    // 使用of方法创建标识符，因为构造函数是私有的
    public static final Identifier ID = Identifier.of(Teamhunter.MOD_ID, "has_command_tag");
    private final String tagName; // 需要检查的标签名

    /**
     * 构造函数
     * @param tagName 要检查的玩家命令标签
     */
    public HasCommandTagCondition(String tagName) {
        this.tagName = tagName;
    }

    @Override
    public LootConditionType getType() {
        // 返回已注册的条件类型
        return TeamhunterLootConditions.HAS_COMMAND_TAG;
    }

    @Override
    public boolean test(LootContext context) {
        Entity killer = context.get(LootContextParameters.LAST_DAMAGE_PLAYER);
        // 检查击杀者是否是玩家且拥有指定标签
        if (killer instanceof PlayerEntity player) {
            return player.getCommandTags().contains(this.tagName);
        }
        return false;
    }

    /**
     * 理论上这里要补上codec,但是没搞懂
     */
    public static class Serializer implements Codec<HasCommandTagCondition> {
        private static final String TAG_KEY = "tag";

        @Override
        public <T> DataResult<Pair<HasCommandTagCondition, T>> decode(T input, DynamicOps<T> ops) {
            return ops.getMap(input).flatMap(map -> {
                DataResult<String> tagResult = ops.getStringValue(map.get(TAG_KEY));
                return tagResult.map(tag -> Pair.of(new HasCommandTagCondition(tag), ops.empty()));
            });
        }

        @Override
        public <T> DataResult<T> encode(HasCommandTagCondition condition, DynamicOps<T> ops, T prefix) {
            return ops.mapBuilder()
                    .add(TAG_KEY, ops.createString(condition.tagName))
                    .build(prefix);
        }
    }
}