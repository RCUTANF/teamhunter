package com.rcutanf.teamhunter.loot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
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
 *
 * @param tag 需要检查的标签名
 */
public record HasCommandTagCondition(String tag) implements LootCondition {
    // 标签条件的类型
    public static final Identifier ID = Identifier.of(Teamhunter.MOD_ID, "has_command_tag");
    public static final MapCodec<HasCommandTagCondition> CODEC = RecordCodecBuilder.mapCodec(
            instance -> instance.group(
                    Codec.STRING.fieldOf("tag").forGetter(HasCommandTagCondition::tag)
            ).apply(instance, HasCommandTagCondition::new)
    );

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
            return player.getCommandTags().contains(this.tag);
        }
        return false;
    }
}
